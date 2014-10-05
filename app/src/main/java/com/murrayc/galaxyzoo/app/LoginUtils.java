package com.murrayc.galaxyzoo.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 10/5/14.
 */
public class LoginUtils {
    // An account type, in the form of a domain name
    // This must match the android:accountType in authenticator.xml
    public static final String ACCOUNT_TYPE = "galaxyzoo.com";
    //This is an arbitrary string, because Accountmanager.setAuthToken() needs something non-null
    public static final String ACCOUNT_AUTHTOKEN_TYPE = "authApiKey";

    //TODO: Ask the provider instead of using this hack which uses too much internal knowledge.
    public static boolean getLoggedIn(final Context context) {
        final LoginDetails loginDetails = getAccountLoginDetails(context);
        if (loginDetails == null) {
            return false;
        }

        return !(TextUtils.isEmpty(loginDetails.authApiKey));
    }

    private static LoginResult parseLoginResponseContent(final InputStream content) throws IOException {
        final String str = HttpUtils.getStringFromInputStream(content);

        //A failure by default.
        LoginResult result = new LoginResult(false, null, null);

        JSONTokener tokener = new JSONTokener(str);
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(tokener);
        } catch (JSONException e) {
            Log.error("JSON parsing failed.", e);
            return result;
        }

        try {
            if (TextUtils.equals(jsonObject.getString("success"), "true")) {
                Log.info("Login succeeded.");

                //TODO: Store the name and api_key for later use when uploading classifications.
                final String apiKey = jsonObject.getString("api_key");;
                final String name = jsonObject.getString("name");

                return new LoginResult(true, name, apiKey);
            } else {
                Log.info("Login failed.");

                final String message = jsonObject.getString("message");
                Log.info("Login failure message", message);
                return result;
            }
        } catch (final JSONException e) {
            Log.error("parseLoginResponseContent(): Exception", e);
            return result;
        }
    }

    public static LoginResult loginSync(final String username, final String password) {
        final HttpURLConnection conn = HttpUtils.openConnection(Config.LOGIN_URI);
        if (conn == null) {
            return null;
        }

        final List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("username", username));
        nameValuePairs.add(new BasicNameValuePair("password", password));

        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            if (!Utils.writeParamsToHttpPost(conn, nameValuePairs)) {
                return null;
            }

            conn.connect();
        } catch (final IOException e) {
            Log.error("loginSync(): exception during HTTP connection", e);

            return null;
        }

        //Get the response:
        InputStream in = null;
        try {
            in = conn.getInputStream();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.error("loginSync(): response code: " + conn.getResponseCode());
                return null;
            }

            return parseLoginResponseContent(in);
        } catch (final IOException e) {
            Log.error("loginSync(): exception during HTTP connection", e);

            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    Log.error("loginSync(): exception while closing in", e);
                }
            }
        }
    }

    public static LoginDetails getAccountLoginDetails(final Context context) {
        final AccountManager mgr = AccountManager.get(context);
        final Account[] accts = mgr.getAccountsByType(ACCOUNT_TYPE);
        if((accts == null) || (accts.length < 1)) {
            Log.error("getAccountLoginDetails(): getAccountsByType() return no account.");
            return null;
        }

        final LoginDetails result = new LoginDetails();

        final Account account = accts[0];
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

    public static class LoginDetails {
        public String name = null;
        public String authApiKey = null;
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
