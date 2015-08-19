/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.JsonReader;

import com.murrayc.galaxyzoo.app.provider.Item;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Created by murrayc on 10/5/14.
 */
public final class LoginUtils {
    // An account type, in the form of a domain name
    // This must match the android:accountType in authenticator.xml
    public static final String ACCOUNT_TYPE = "galaxyzoo.com";

    //This is an arbitrary string, because Accountmanager.setAuthToken() needs something non-null
    public static final String ACCOUNT_AUTHTOKEN_TYPE = "authApiKey";

    //Because the Account must have a name.
    //TODO: Stop this string from appearing in the general Settings->Accounts UI,
    //or at least show a translatable "Anonymous" there instead.
    private static final String ACCOUNT_NAME_ANONYMOUS = "anonymous";

    /**
     * Returns true if we have a real account that has logged into the server,
     * or false if we are using the anonymous account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     *
     * @param context
     * @return
     */
    public static boolean getLoggedIn(final Context context) {
        final LoginDetails loginDetails = getAccountLoginDetails(context);
        return getLoggedIn(loginDetails);
    }

    /**
     * This is a just a utility method that examines the LoginDetails.
     * Unlike getLoggedIn(Context), this can be called from any thread.
     *
     * @param loginDetails
     * @return
     */
    public static boolean getLoggedIn(final LoginDetails loginDetails) {
        if (loginDetails == null) {
            return false;
        }

        return !(TextUtils.isEmpty(loginDetails.authApiKey));
    }

    public static LoginResult parseLoginResponseContent(final InputStream content) throws IOException {
        //A failure by default.
        LoginResult result = new LoginResult(false, null, null);

        final InputStreamReader streamReader = new InputStreamReader(content, Utils.STRING_ENCODING);
        final JsonReader reader = new JsonReader(streamReader);
        reader.beginObject();
        boolean success = false;
        String apiKey = null;
        String userName = null;
        String message = null;
        while (reader.hasNext()) {
            final String name = reader.nextName();
            switch (name) {
                case "success":
                    success = reader.nextBoolean();
                    break;
                case "api_key":
                    apiKey = reader.nextString();
                    break;
                case "name":
                    userName = reader.nextString();
                    break;
                case "message":
                    message = reader.nextString();
                    break;
                default:
                    reader.skipValue();
            }
        }

        if (success) {
            result = new LoginResult(true, userName, apiKey);
        } else {
            Log.info("Login failed.");
            Log.info("Login failure message: " + message);
        }

        reader.endObject();
        reader.close();

        streamReader.close();

        return result;
    }

    /**
     * This returns null if there is no account (not even an anonymous account).
     * Don't call this from the main thread - use an AsyncTask, for instance.
     *
     * @param context
     * @return
     */
    @Nullable
    public static LoginDetails getAccountLoginDetails(final Context context) {
        final AccountManager mgr = AccountManager.get(context);
        if (mgr == null) {
            Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed because AccountManager.get() returned null.");
            return null;
        }

        final Account account = getAccount(mgr);
        if (account == null) {
            Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed because getAccount() returned null. ");
            return null;
        }

        //Make sure that this has not been unset somehow:
        setAutomaticAccountSync(context, account);

        final LoginDetails result = new LoginDetails();


        //Avoid showing our anonymous account name in the UI.
        //Also, an anonymous account never has an auth_api_key.
        result.isAnonymous = TextUtils.equals(account.name, ACCOUNT_NAME_ANONYMOUS);
        if (result.isAnonymous) {
            return result; //Return a mostly-empty empty (but not null) LoginDetails.
        }

        result.name = account.name;

        //Note that this requires the USE_CREDENTIALS permission on
        //SDK <=22.
        final AccountManagerFuture<Bundle> response = mgr.getAuthToken(account, ACCOUNT_AUTHTOKEN_TYPE, null, null, null, null);
        try {
            final Bundle bundle = response.getResult();
            if (bundle == null) {
                //TODO: Let the caller catch this?
                Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed because getAuthToken() returned a null response result bundle.");
                return null;
            }

            result.authApiKey = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            return result;
        } catch (final OperationCanceledException e) {
            //TODO: Let the caller catch this?
            Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed", e);
            return null;
        } catch (final AuthenticatorException e) {
            //TODO: Let the caller catch this?
            Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed", e);
            return null;
        } catch (final IOException e) {
            //TODO: Let the caller catch this?
            Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed", e);
            return null;
        }
    }

    public static void logOut(final ZooFragment fragment) {
        final Activity activity = fragment.getActivity();
        final AccountRemoveTask task = new AccountRemoveTask(activity) {
            @Override
            protected void onPostExecute(final Void result) {
                super.onPostExecute(result);

                //Make sure that the currently-shown menu will update:
                fragment.setCachedLoggedIn(false);

                //TODO: This doesn't actually seem to cause the (various) child fragments'
                //onPrepareOptionsMenu() methods to be called. Maybe it doesn't work with
                //nested child fragments.
                if (activity instanceof FragmentActivity) {
                    final FragmentActivity fragmentActivity = (FragmentActivity) activity;
                    fragmentActivity.supportInvalidateOptionsMenu();
                } else {
                    activity.invalidateOptionsMenu();
                }
            }
        };
        task.execute();
    }

    /**
     * Add the anonymous Account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     * @param context
     */
    private static void addAnonymousAccount(final Context context) {
        final AccountManager accountManager = AccountManager.get(context);
        final Account account = new Account(ACCOUNT_NAME_ANONYMOUS, LoginUtils.ACCOUNT_TYPE);
        //Note that this requires the AUTHENTICATE_ACCOUNTS permission on
        //SDK <=22:
        accountManager.addAccountExplicitly(account, null, null);

        //In case it has not been called yet.
        //This has no effect the second time.
        Utils.initDefaultPrefs(context);

        //Give the new account the existing (probably default) preferences,
        //so the SyncAdapter can use them.
        //See SettingsFragment.onSharedPreferenceChanged().
        copyPrefsToAccount(context, accountManager, account);

        //Tell the SyncAdapter to sync whenever the network is reconnected:
        setAutomaticAccountSync(context, account);
    }

    static void setAutomaticAccountSync(final Context context, final Account account) {
        final ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            return;
        }

        ContentResolver.setSyncAutomatically(account, Item.AUTHORITY, true);
    }

    /** Don't call this from the main UI thread.
     *
     * @param context
     */
    public static void removeAnonymousAccount(final Context context) {
        removeAccount(context, ACCOUNT_NAME_ANONYMOUS);
    }

    /** Don't call this from the main UI thread.
     *
     * @param context
     */
    public static void removeAccount(final Context context, final String accountName) {
        final AccountManager accountManager = AccountManager.get(context);
        final Account account = new Account(accountName, LoginUtils.ACCOUNT_TYPE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            //Trying to call this on an older Android version results in a
            //NoSuchMethodError exception.
            //There is no AppCompat version of the AccountManager API to
            //avoid the need for this version check at runtime.
            accountManager.removeAccount(account, null, null, null);
        } else {
            //noinspection deprecation
            //Note that this needs the MANAGE_ACCOUNT permission on
            //SDK <=22.
            //noinspection deprecation
            accountManager.removeAccount(account, null, null);
        }
    }

    /**
     * Get a preference from the Account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     *
     * @param context
     * @param prefKeyResId
     * @return
     */
    private static boolean getBooleanPref(final Context context, final int prefKeyResId) {
        final String value = getStringPref(context, prefKeyResId);
        if (value == null) {
            return false;
        }

        return Boolean.parseBoolean(value);
    }

    /**
     * Get a preference from the Account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     *
     * @param context
     * @param prefKeyResId
     * @return
     */
    public static int getIntPref(final Context context, final int prefKeyResId) {
        final String value = getStringPref(context, prefKeyResId);
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            //NumberFormatException is an unchecked exception but
            //it would not be a programmer error to try to parse
            //an input string (the stored preference in this case)
            //as an Integer, as long as there's no way for us
            //to check its validity before calling Integer.parseInt().
            //Therefore we catch it.
            return 0;
        }
    }

    /**
     * Get a preference from the Account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     *
     * @param context
     * @param prefKeyResId
     * @return
     */
    @Nullable
    private static String getStringPref(final Context context, final int prefKeyResId) {
        final AccountManager mgr = AccountManager.get(context);
        final Account account = getAccount(context);
        if (account == null) {
            return null;
        }

        //Note that this requires the AUTHENTICATE_ACCOUNTS permission on
        //SDK <=22.
        return mgr.getUserData(account, context.getString(prefKeyResId));
    }

    /**
     * Get the Account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     *
     * @param mgr
     * @return
     */
    @Nullable
    private static Account getAccount(final AccountManager mgr) {
        //Note this needs the GET_ACCOUNTS permission on
        //SDK <=22
        final Account[] accts = mgr.getAccountsByType(ACCOUNT_TYPE);
        if((accts == null) || (accts.length < 1)) {
            //Log.error("getAccountLoginDetails(): getAccountsByType() returned no account.");
            return null;
        }

        return accts[0];
    }

    /**
     * Get the Account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     *
     * @param context
     * @return
     */
    private static Account getAccount(final Context context) {
        final AccountManager mgr = AccountManager.get(context);
        return getAccount(mgr);
    }

    static void copyPrefToAccount(final Context context, final String key, final String value) {
        //Copy the preference to the Account:
        final AccountManager mgr = AccountManager.get(context);
        final Account account = getAccount(context);
        if (account == null) {
            return;
        }

        copyPrefToAccount(mgr, account, key, value);
    }

    private static void copyPrefToAccount(final AccountManager mgr, final Account account, final String key, final String value) {
        //Note that this requires the AUTHENTICATE_ACCOUNTS permission on
        //SDK <=22.
        mgr.setUserData(account, key, value);
    }

    static void copyPrefsToAccount(final Context context, final AccountManager accountManager, final Account account) {
        //Copy the preferences into the account.
        //See also SettingsFragment.onSharedPreferenceChanged()
        final SharedPreferences prefs = Utils.getPreferences(context);
        final Map<String, ?> keys = prefs.getAll();
        for(final Map.Entry<String, ?> entry : keys.entrySet()) {
            final Object value =  entry.getValue();
            if (value instanceof String) {
                copyPrefToAccount(accountManager, account, entry.getKey(), (String) value);
            } else if (value instanceof Integer) {
                copyPrefToAccount(accountManager, account, entry.getKey(), Integer.toString((Integer) value));
            } else if (value instanceof Boolean) {
                copyPrefToAccount(accountManager, account, entry.getKey(), Boolean.toString((Boolean) value));
            }
        }
    }

    /**
     * Get the "use-wifi only" setting from the account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     * Or use Utils.getUseWifiOnlyFromSharedPrefs().
     *
     * @param context
     * @return
     */
    public static boolean getUseWifiOnly(final Context context) {
        return getBooleanPref(context, R.string.pref_key_wifi_only);
    }

    public static class LoginDetails {
        public String name = null;
        public String authApiKey = null;
        public boolean isAnonymous = false;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public static class GetExistingLogin extends AsyncTask<Void, Void, LoginDetails> {

        private final WeakReference<Context> mContextReference;
        Exception mException;

        GetExistingLogin(final Context context) {
            mContextReference = new WeakReference<>(context);
        }

        @Override
        protected LoginDetails doInBackground(final Void... params) {

            if (mContextReference == null) {
                return null;
            }

            final Context context = mContextReference.get();
            if (context == null) {
                return null;
            }

            LoginDetails result = null;

            try {
                result = getAccountLoginDetails(context);
                if (result == null) {
                    //Add an anonymous Account,
                    //because our SyncAdapter will not run if there is no associated Account,
                    //and we want it to run to get the items to classify, and to upload
                    //anonymous classifications.
                    LoginUtils.addAnonymousAccount(context);

                    return getAccountLoginDetails(context);
                }
            } catch (final SecurityException ex) {
                mException = ex;
            }

            return result;
        }

        @Override
        protected void onCancelled() {
        }
    }

    /** Run this to log out.
     */
    private static class AccountRemoveTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Context> contextReference;

        AccountRemoveTask(final Context context) {
            this.contextReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(final Void... params) {

            if (contextReference == null) {
                return null;
            }

            final Context context = contextReference.get();
            if (context == null) {
                return null;
            }

            final LoginUtils.LoginDetails loginDetails = LoginUtils.getAccountLoginDetails(context);
            if(!LoginUtils.getLoggedIn(loginDetails)) {
                return null;
            }

            final String accountName = loginDetails.name;
            if (TextUtils.isEmpty(accountName)) {
                return null;
            }

            LoginUtils.removeAccount(context, accountName);

            LoginUtils.addAnonymousAccount(context);

            return null;
        }
    }


    public static class LoginResult {
        private final boolean success;
        private final String name;
        private final String apiKey;

        public LoginResult(final boolean success, final String name, final String apiKey) {
            this.success = success;
            this.name = name;
            this.apiKey = apiKey;
        }

        public String getApiKey() {
            return apiKey;
        }

        public boolean getSuccess() {
            return success;
        }

        public String getName() {
            return name;
        }
    }
}
