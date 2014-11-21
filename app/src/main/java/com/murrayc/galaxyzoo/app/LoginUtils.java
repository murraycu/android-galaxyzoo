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
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.JsonReader;

import com.murrayc.galaxyzoo.app.provider.Item;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;

/**
 * Created by murrayc on 10/5/14.
 */
public class LoginUtils {
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
        if (loginDetails == null) {
            return false;
        }

        return !(TextUtils.isEmpty(loginDetails.authApiKey));
    }

    public static LoginResult parseLoginResponseContent(final InputStream content) {
        //A failure by default.
        LoginResult result = new LoginResult(false, null, null);

        final JsonReader reader;
        try {
            reader = new JsonReader(new InputStreamReader(content, Utils.STRING_ENCODING));
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
                Log.info("Login failure message", message);
            }

            reader.endObject();
            reader.close();
        } catch (final UnsupportedEncodingException e) {
            Log.info("parseLoginResponseContent: UnsupportedEncodingException parsing JSON", e);
        } catch (final IOException e) {
            Log.info("parseLoginResponseContent: IOException parsing JSON", e);
        } catch (final IllegalStateException e) {
            Log.info("parseLoginResponseContent: IllegalStateException parsing JSON", e);
        }

        return result;
    }

    /**
     * This returns null if there is no account (not even an anonymous account).
     * Don't call this from the main thread - use an AsyncTask, for instance.
     *
     * @param context
     * @return
     */
    public static LoginDetails getAccountLoginDetails(final Context context) {
        final AccountManager mgr = AccountManager.get(context);
        final Account[] accts = mgr.getAccountsByType(ACCOUNT_TYPE);
        if((accts == null) || (accts.length < 1)) {
            //Log.error("getAccountLoginDetails(): getAccountsByType() returned no account.");
            return null;
        }

        final Account account = accts[0];

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

        final AccountManagerFuture<Bundle> response = mgr.getAuthToken(account, ACCOUNT_AUTHTOKEN_TYPE, null, null, null, null);
        try {
            final Bundle bundle = response.getResult();
            result.authApiKey = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            return result;
        } catch (OperationCanceledException e) {
            Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed", e);
            return null;
        } catch (AuthenticatorException e) {
            Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed", e);
            return null;
        } catch (IOException e) {
            Log.error("getAccountLoginDetails(): getAccountLoginDetails() failed", e);
            return null;
        }
    }

    /**
     * Add the anonymous Account.
     *
     * Don't call this from the main thread - use an AsyncTask, for instance.
     * @param context
     */
    public static void addAnonymousAccount(final Context context) {
        final AccountManager accountManager = AccountManager.get(context);
        final Account account = new Account(ACCOUNT_NAME_ANONYMOUS, LoginUtils.ACCOUNT_TYPE);
        accountManager.addAccountExplicitly(account, null, null);

        //In case it has not been called yet.
        //This has no effect the second time.
        Utils.initDefaultPrefs(context);

        //Give the new account the existing (probably default) preferences,
        //so the SyncAdapter can use them.
        //See SettingsFragment.onSharedPreferenceChanged().
        Utils.copyPrefsToAccount(context, accountManager, account);

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

    public static void removeAnonymousAccount(final Context context) {
        removeAccount(context, ACCOUNT_NAME_ANONYMOUS);
    }

    public static void removeAccount(final Context context, final String accountName) {
        final AccountManager accountManager = AccountManager.get(context);
        final Account account = new Account(accountName, LoginUtils.ACCOUNT_TYPE);
        accountManager.removeAccount(account, null, null);
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

        GetExistingLogin(final Context context) {
            mContextReference = new WeakReference<>(context);
        }

        @Override
        protected LoginDetails doInBackground(Void... params) {

            if (mContextReference == null) {
                return null;
            }

            final Context context = mContextReference.get();
            if (context == null) {
                return null;
            }

            return getAccountLoginDetails(context);
        }

        @Override
        protected void onCancelled() {
        }
    }

    public static class LoginResult {
        private final boolean success;
        private final String name;
        private final String apiKey;

        public LoginResult(boolean success, final String name, final String apiKey) {
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