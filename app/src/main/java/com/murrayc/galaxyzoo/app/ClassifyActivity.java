/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.view.MenuItem;

import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

import java.lang.ref.WeakReference;

/**
 * An activity showing a single subject. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link com.murrayc.galaxyzoo.app.ListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link com.murrayc.galaxyzoo.app.ClassifyFragment}.
 */
public class ClassifyActivity extends ItemActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
            ClassifyFragment.Callbacks, QuestionFragment.Callbacks{
    private boolean mIsStateAlreadySaved = false;
    private boolean mPendingClassificationFinished = false;
    private boolean mPendingWarnAboutNetworkProblemWithRetry = false;

    private AlertDialog mAlertDialog = null;


//    public class ItemsContentProviderObserver extends ContentObserver {
//
//        /**
//         * Creates a content observer.
//         *
//         * @param handler The handler to run {@link #onChange} on, or null if none.
//         */
//        public ItemsContentProviderObserver(final Handler handler) {
//            super(handler);
//        }
//
//        @Override
//        public void onChange(boolean selfChange) {
//            super.onChange(selfChange);
//
//            requestSync();
//        }
//
//        @Override
//        public void onChange(boolean selfChange, final Uri changeUri) {
//            //super.onChange(selfChange, changeUri);
//
//            requestSync();
//        }
//    }


    /**
      * Asynchronously discovers if we are logged in and offers a login if not.
      */
     public static class CheckLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final WeakReference<Context> mContextReference;

        CheckLoginTask(final Context context) {
            mContextReference = new WeakReference<>(context);
        }

        @Override
        protected Boolean doInBackground(final Void... params) {

            if (mContextReference == null) {
                return false;
            }

            final Context context = mContextReference.get();
            if (context == null) {
                return false;
            }

            return LoginUtils.getLoggedIn(context);
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            if (mContextReference == null) {
                return;
            }

            final Context context = mContextReference.get();
            if (context == null) {
                return;
            }

            if (!result) {
                final Intent intent = new Intent(context, LoginActivity.class);
                context.startActivity(intent);
            }

        }
    }

    /**
     * Asynchronously gets the account and tells the SyncAdapter to sync it now:
     */
    public static class RequestSyncTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Context> mContextReference;

        RequestSyncTask(final Context context) {
            mContextReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(final Void... params) {

            if (mContextReference == null) {
                return null;
            }

            final Context context = mContextReference.get();
            if (context == null) {
                return null;
            }

            final AccountManager mgr = AccountManager.get(context);
            final Account[] accts = mgr.getAccountsByType(LoginUtils.ACCOUNT_TYPE);
            if((accts == null) || (accts.length < 1)) {
                //Log.error("getAccountLoginDetails(): getAccountsByType() returned no account.");
                return null;
            }

            final Account account = accts[0];

            final Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);


            //Ask the framework to run our SyncAdapter.
            ContentResolver.requestSync(account, Item.AUTHORITY, extras);

            return null;
        }

        @Override
        protected void onCancelled() {
        }
    }

    private void requestSync() {
        final RequestSyncTask task = new RequestSyncTask(this);
        task.execute();
    }

    private int mClassificationsDoneInSession = 0;

    // The authority for the sync adapter's content provider
    //public static final String AUTHORITY = Item.AUTHORITY;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(getItemId())) {
            setItemId(ItemsContentProvider.URI_PART_ITEM_ID_NEXT);
        }

        //Make the SyncAdapter respond to preferences changes.
        //The SyncAdapter can't do this itself, because SharedPreferences
        //doesn't work across processes.
        try {
            PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        } catch (final UnsupportedOperationException e) {
            //This happens during our test case, because the MockContext doesn't support this,
            //so ignore this.
            Log.info("ClassifyActivity.onCreate(): Ignoring UnsupportedOperationException from getDefaultSharedPreferences().");
        }

        /**
         * Ask the SyncProvider to update whenever anything in the ItemContentProvider's
         * Items table changes. This seems excessive, but maybe we can trust
         * the SyncAdapter framework to not try to do too much work.
         *
         * Register the observer for the data table. The table's path
         * and any of its subpaths trigger the observer.
         */
        /* Disabled for now. Instead the ItemsContentProvider calls its requestSync()
         * when it is necessary.
        final ItemsContentProviderObserver observer = new ItemsContentProviderObserver(new Handler());
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(Item.ITEMS_URI, true, observer);
        */

        //Make sure that the SyncAdapter starts to download items as soon as possible:
        requestSync();


        setContentView(R.layout.activity_classify);

        UiUtils.showToolbar(this);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            final FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager != null) {
                //We check to see if the fragment exists already,
                //because apparently it sometimes does exist already when the app has been
                //in the background for some time,
                //at least on Android 5.0 (Lollipop)

                // Create the detail fragment and add it to the activity
                // using a fragment transaction.
                final Bundle arguments = new Bundle();
                arguments.putString(ItemFragment.ARG_ITEM_ID,
                        getItemId());

                ClassifyFragment fragment = (ClassifyFragment) fragmentManager.findFragmentById(R.id.container);
                if (fragment == null) {
                    // TODO: Find a simpler way to just pass this through to the fragment.
                    // For instance, pass the intent.getExtras() as the bundle?.
                    fragment = new ClassifyFragment();
                    fragment.setArguments(arguments);
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, fragment)
                            .commit();
                } else {
                    Log.info("ClassifyActivity.onCreate(): The ClassifyFragment already existed.");

                    fragment.setItemId(getItemId());
                    fragment.update();
                }
            }
        }

        /*
        // Show the Up button in the action bar.
        final ActionBar actionBar = getActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);
        */

        final LoginUtils.GetExistingLogin task = new LoginUtils.GetExistingLogin(this) {
            @Override
            protected void onPostExecute(final LoginUtils.LoginDetails loginDetails) {
                super.onPostExecute(loginDetails);

                onExistingLoginRetrieved(loginDetails);
            }
        };
        task.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsStateAlreadySaved = true;
    }

    /** This would ideally be in ClassifyFragment.onResume() or similar,
     * but we need to do it here to avoid this exception sometimes:
     *   "java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState".
     * as suggested here:
     * http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
     */
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        mIsStateAlreadySaved = false;

        //See onClassificationFinished().
        if(mPendingClassificationFinished) {
            mPendingClassificationFinished = false;
            doClassificationFinished();
            return;
        }

        //See warnAboutNetworkProblemWithRetry().
        if(mPendingWarnAboutNetworkProblemWithRetry) {
            mPendingWarnAboutNetworkProblemWithRetry = false;
            doWarnAboutNetworkProblemWithRetry();
            return;
        }

        final ClassifyFragment fragmentClassify = getChildFragment();
        if (fragmentClassify != null) {

            if(TextUtils.equals(fragmentClassify.getItemId(), ItemsContentProvider.URI_PART_ITEM_ID_NEXT)) {
                //We are probably resuming again after a previous failure to get new items
                //from the network, so try again:
                fragmentClassify.update();
            }
        }
    }

    private void onExistingLoginRetrieved(final LoginUtils.LoginDetails loginDetails) {
        if (loginDetails != null && !TextUtils.isEmpty(loginDetails.authApiKey)) {
            //Tell the user that we are logged in,
            //reassuring them that their classifications will be part of their profile:
            //TODO: Is there a better way to do this?
            //Hiding the login option menu item would still leave some doubt,
            //and graying it out would be against the Android design guidelines.
            UiUtils.showLoggedInToast(this);
        }

        //Request a sync in case sync has not happened before because there was not yet
        //an anonymous account (which is added in GetExistingLogin).
        requestSync();
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle presses on the action bar items
        final int id = item.getItemId();

        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            final Intent intent = new Intent(this, ListActivity.class);
            NavUtils.navigateUpTo(this, intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClassificationFinished() {
        // Avoid causing this exception when ClassifyFragment tries to show() an AlertDialog:
        // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        if(mIsStateAlreadySaved) {
            //Do it later instead.
            this.mPendingClassificationFinished = true;
            return;
        }

        doClassificationFinished();
    }

    private void doClassificationFinished() {
        //Suggest registering or logging in after a certain number of classifications,
        //as the web UI does, but don't ask again.
        if (mClassificationsDoneInSession == 3) {
            checkForLoginAsync();
        }
        mClassificationsDoneInSession++;

        // Careful: This can cause an AlertDialog.show() if there are no more items and if the
        // network is not working properly.
        // That's a problem because this method (onClassificationFinished()) can result from an AsyncTask.onPostExecute(),
        // which is generally discouraged:
        // See http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
        startNextClassification();
    }

    private void checkForLoginAsync() {
        final CheckLoginTask task = new CheckLoginTask(this);
        task.execute();
    }

    private void startNextClassification() {
        //Start another classification:
        setItemId(ItemsContentProvider.URI_PART_ITEM_ID_NEXT);
        final ClassifyFragment fragmentClassify = getChildFragment();
        if (fragmentClassify != null) {
            fragmentClassify.setItemId(ItemsContentProvider.URI_PART_ITEM_ID_NEXT);
            fragmentClassify.update();
        }
    }

    private ClassifyFragment getChildFragment() {
        return (ClassifyFragment) getSupportFragmentManager().findFragmentById(R.id.container);
    }

    @Override
    public void abandonItem() {
        startNextClassification();
    }


    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        Log.info("ClassifyActivity: onSharedPreferenceChanged().");

        //Changes to these preferences would need us to do some work:
        //TODO: Do we need this check, or will we only be notified about the app's own preferences?
        if (TextUtils.equals(key, getString(R.string.pref_key_cache_size)) ||
                TextUtils.equals(key, getString(R.string.pref_key_keep_count)) ||
                TextUtils.equals(key, getString(R.string.pref_key_wifi_only))) {
            requestSync();
        }
    }

    @Override
    public void warnAboutNetworkProblemWithRetry() {
        // Avoid causing this exception when we try to show() an AlertDialog:
        // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        if (mIsStateAlreadySaved) {
            //Do it later instead.
            this.mPendingWarnAboutNetworkProblemWithRetry = true;
            return;
        }

        doWarnAboutNetworkProblemWithRetry();
    }

    private void doWarnAboutNetworkProblemWithRetry() {
        //Dismiss any existing dialog:
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }

        //Show the new one:
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // http://developer.android.com/design/building-blocks/dialogs.html
        // says "Most alerts don't need titles.":
        // builder.setTitle(activity.getString(R.string.error_title_connection_problem));

        builder.setMessage(getString(R.string.error_no_subjects));

        builder.setPositiveButton(getString(R.string.error_button_retry), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                onClickListenerRetry();
            }
        });

        builder.setNegativeButton(getString(R.string.error_button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.cancel();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(final DialogInterface dialog) {
                dialog.dismiss();
                mAlertDialog = null;
            }
        });

        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    private void onClickListenerRetry() {
        //Try to get the next item again.
        //It should succeed if we have a working network connection,
        //or fail again with the same message.
        final ClassifyFragment fragmentClassify = getChildFragment();
        if (fragmentClassify != null) {
            fragmentClassify.update();
        }
    }
}
