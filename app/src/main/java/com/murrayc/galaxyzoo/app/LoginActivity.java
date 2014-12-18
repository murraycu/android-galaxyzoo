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
//import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.murrayc.galaxyzoo.app.provider.HttpUtils;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;

import java.lang.ref.WeakReference;

//TODO: Use the toolbar, but we cannot derive from ActionBarActivity from AppCompat.
//Android's standard AccountAuthenticatorActivity doesn't let us use the toolbar,
//because it doesn't deried from ActionBarActivity,
//and it apparently will never have a usable version in AppCompat,
//so we have to copy the whole class to create ZooAccountAuthenticatorActivity
//and derive from that instead.
//See https://chris.banes.me/2014/10/17/appcompat-v21/#comment-1652981836

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends ZooAccountAuthenticatorActivity {

    /** The Intent extra to store username. */
    public static final String ARG_USERNAME = "username";

    private ZooniverseClient mClient = null;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUsernameView = null;
    private EditText mPasswordView = null;
    private View mProgressView = null;
    private View mLoginFormView = null;
    private String mExistingAccountName = null;
    private boolean mExistingAccountIsAnonymous = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClient = new ZooniverseClient(this, com.murrayc.galaxyzoo.app.provider.Config.SERVER);

        setContentView(R.layout.activity_login);

        UiUtils.showToolbar(this);

        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);

        //Get the name that was successful last time, if any:
        final Intent intent = getIntent();
        if (intent != null) {
            mExistingAccountName = intent.getStringExtra(ARG_USERNAME);
        }

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        setTextViewLink(R.id.textViewForgot, Config.FORGET_PASSWORD_URI, R.string.forgot_password_button_text);
        setTextViewLink(R.id.textViewRegister, Config.REGISTER_URI, R.string.register_button_text);

        Button mUsernameSignInButton = (Button) findViewById(R.id.username_sign_in_button);
        mUsernameSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        // Show the Up button in the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);

        //Get the existing logged-in username, if any:
        final LoginUtils.GetExistingLogin task = new LoginUtils.GetExistingLogin(this) {
            @Override
            protected void onPostExecute(final LoginUtils.LoginDetails loginDetails) {
                super.onPostExecute(loginDetails);

                onExistingLoginRetrieved(loginDetails);
            }
        };
        task.execute();
    }

    private void setTextViewLink(final int textViewResourceId, final String uri, final int strResourceId) {
        //We add the <a> link here rather than having it in the string resource,
        //to make life easier for translators.
        final String html = "<a href=\"" + uri + "\">" +
                getString(strResourceId) + "</a>";

        TextView textView = (TextView) findViewById(textViewResourceId);
        textView.setText(Html.fromHtml(html));

        //This setMovementMethod() voodoo makes the textviews' HTML links clickable:
        //See http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable/20647011#20647011
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            // This activity has no single possible parent activity.
            // In this case Up should be the same as Back.
            // See "Navigating to screens with multiple entry points":
            //   http://developer.android.com/design/patterns/navigation.html
            // Just closing the activity might be enough:
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void onExistingLoginRetrieved(final LoginUtils.LoginDetails loginDetails) {

        mExistingAccountName = null;
        if (loginDetails == null) {
            Log.error("LoginActivity.onExistingLoginRetrieved(): loginDetails is null.");
            return;
        } else {
            mExistingAccountName = loginDetails.name; //The anonymous name will never be here. Instead see LoginDetails.isAnonymous.
        }

        mUsernameView.setText(mExistingAccountName);
        mExistingAccountIsAnonymous = loginDetails.isAnonymous;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username:
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private static class AccountSaveTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Context> contextReference;
        private final LoginUtils.LoginResult loginResult;
        private final String existingAccountName;

        private final boolean existingAccountIsAnonymous;

        AccountSaveTask(final Context context, final LoginUtils.LoginResult loginResult, final String existingAccountName, boolean existingAccountIsAnonymous) {
            this.contextReference = new WeakReference<>(context);
            this.loginResult = loginResult;
            this.existingAccountName = existingAccountName;
            this.existingAccountIsAnonymous = existingAccountIsAnonymous;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (contextReference == null) {
                return null;
            }

            final Context context = contextReference.get();
            if (context == null) {
                return null;
            }

            final String accountName = loginResult.getName();

            final AccountManager accountManager = AccountManager.get(context);

            boolean addingAccount = false;
            if (existingAccountIsAnonymous) {
                //Remove the existing account so we can add the new one.
                //TODO: Find a way to just change the name,
                //though we don't lose any ItemsContentProvider data when we delete an Account.
                LoginUtils.removeAnonymousAccount(context);
                addingAccount = true;
            } else if(!TextUtils.equals(existingAccountName, accountName)) {
                //Remove any existing account so we can add the new one.
                //TODO: Find a way to just change the name,
                if (!TextUtils.isEmpty(existingAccountName)) {
                    LoginUtils.removeAccount(context, existingAccountName);
                }

                addingAccount = true;
            }


            final Account account = new Account(accountName, LoginUtils.ACCOUNT_TYPE);
            if (addingAccount) {
                accountManager.addAccountExplicitly(account, null, null);
                LoginUtils.copyPrefsToAccount(context, accountManager, account);

                //Tell the SyncAdapter to sync whenever the network is reconnected:
                LoginUtils.setAutomaticAccountSync(context, account);
            }

            //TODO? ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true)

            //This is apparently not necessary, when updating an existing account,
            //if this activity was launched from our Authenticator, for instance if our
            //Authenticator found that the accounts' existing auth token was invalid.
            //Presumably it is necessary if this activity is launched from our app.
            accountManager.setAuthToken(account, LoginUtils.ACCOUNT_AUTHTOKEN_TYPE, loginResult.getApiKey());

            return null;
        }
    }

    private void finishWithResult(final LoginUtils.LoginResult result) {
        boolean loggedIn = false;
        if ((result != null) && result.getSuccess()) {
            loggedIn = true;
        }

        if(loggedIn) {
            UiUtils.showLoggedInToast(this);
        }


        final Intent intent = new Intent();

        if (loggedIn) {
            final AccountSaveTask task = new AccountSaveTask(this, result, mExistingAccountName, mExistingAccountIsAnonymous);
            task.execute();

            //Set the accountName in the intent result:
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, result.getName());
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, LoginUtils.ACCOUNT_TYPE);
        }

        //This sets the AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE response,
        //for when this activity was launched by our Authenticator.
        setAccountAuthenticatorResult(intent.getExtras());

        setResult(loggedIn ? RESULT_OK : RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();

        //Let callers (via startActivityForResult() know that this was cancelled.
        finishWithResult(null);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, LoginUtils.LoginResult> {

        private final String mUsername;
        private final String mPassword;
        private Exception exceptionCaught = null;

        UserLoginTask(final String username, final String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected LoginUtils.LoginResult doInBackground(Void... params) {
            final ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                return null;
            }

            try {
                return mClient.loginSync(mUsername, mPassword);
            } catch (final HttpUtils.NoNetworkException ex) {
                Log.info("LoginActivity.UserLoginTask.doInBackground(): loginSync() threw a NoNetworkException.");
                exceptionCaught = ex;
            } catch (final ZooniverseClient.LoginException e) {
                Log.info("LoginActivity.UserLoginTask.doInBackground(): loginSync() threw a LoginException.");
                exceptionCaught = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(final LoginUtils.LoginResult result) {
            mAuthTask = null;
            LoginActivity.this.showProgress(false);

            // A null result means that we didn't even get a response from the server for some reason:
            if (result == null) {
                //Respond appropriately:
                if (exceptionCaught instanceof HttpUtils.NoNetworkException) {        ;
                    UiUtils.warnAboutNoNetworkConnection(LoginActivity.this, (HttpUtils.NoNetworkException)exceptionCaught);
                } else {
                    //There was some other connection error:
                    Log.error("UserLoginTask(): Exception from ZooniverseClient.loginSync()", exceptionCaught);

                    final Toast toast = Toast.makeText(LoginActivity.this, getString(R.string.error_could_not_connect), Toast.LENGTH_LONG);
                    toast.show();
                }

                return;
            }

            if (result.getSuccess()) {
                LoginActivity.this.finishWithResult(result);
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}



