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

package com.murrayc.galaxyzoo.app.syncadapter;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.LoginActivity;
import com.murrayc.galaxyzoo.app.LoginUtils;

/**
 * Created by murrayc on 10/4/14.
 */
public class Authenticator extends AbstractAccountAuthenticator {

    private final Context mContext;

    public Authenticator(final Context context) {
        super(context);

        mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        //This is called when the user tries to add an account via the Accounts UI in the system's Settings.
        //Currently the LoginActivity always starts with any existing account name,
        //so doing this just gives you a chance to try the password for the existing account.
        final Intent intent = generateLoginIntent(response);
        return generateIntentBundle(intent);
    }

    private Bundle generateIntentBundle(final Intent intent) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;

        /*
        if ((options != null) && options.containsKey(AccountManager.KEY_PASSWORD)) {
            final String password =
                    options.getString(AccountManager.KEY_PASSWORD);
            final LoginResult loginResult = loginSync(account.name, password);

            boolean verified = false;
            if (loginResult != null) {
                if (loginResult.getSuccess()) {
                    verified = true;
                    saveAuthToPreferences(mContext, loginResult.getName(), loginResult.getApiKey());
                } else {
                    //Make sure that the auth key is wiped, so we know we are not logged in.
                    //This is an unofficial way to log out, though that is only useful for debugging.
                    saveAuthToPreferences(mContext, account.name, "");
                }
            }

            final Bundle result = new Bundle();
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
            return result;
        }

        return null;
        */
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(LoginUtils.ACCOUNT_AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        //Return the existing auth token, if we have it:
        final AccountManager am = AccountManager.get(mContext);
        final String authToken = am.peekAuthToken(account, authTokenType);
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        //We don't store the password, so we don't try to get it to then try to use it to
        //get the auth token. We always ask for the password form the user and get the
        //auth token then in the LoginActivity.

        //TODO: Check if the server complains when we provide an invalid auth_key
        //so we can invalidate it.

        //TODO: Encrypt the auth token. Or does the AccountManager take care of this already?

        // If we get here, then we couldn't access the user's auth key or password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our LoginActivity panel.
        final Intent intent = generateLoginIntent(response);
        intent.putExtra(LoginActivity.ARG_USERNAME, account.name);
        return generateIntentBundle(intent);
    }

    private Intent generateLoginIntent(final AccountAuthenticatorResponse response) {
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        //Our LoginActivity only handles one type of account and one type of auth token:
        //intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, account.type);
        //intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);

        return intent;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        // null means we don't support multiple authToken types
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        // This call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }


}
