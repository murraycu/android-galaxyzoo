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

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

/**
 * A fragment representing a single subject.
 * This fragment is either contained in a {@link com.murrayc.galaxyzoo.app.ListActivity}
 * in two-pane mode (on tablets) or a {@link com.murrayc.galaxyzoo.app.DetailActivity}
 * on handsets.
 */
public class ClassifyFragment extends ItemFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int URL_LOADER = 0;
    private Cursor mCursor;

    private final String[] mColumns = { Item.Columns._ID, Item.Columns.SUBJECT_ID };

    // We have to hard-code the indices - we can't use getColumnIndex because the Cursor
    // (actually a SQliteDatabase cursor returned
    // from our ContentProvider) only knows about the underlying SQLite database column names,
    // not our ContentProvider's column names. That seems like a design error in the Android API.
    //TODO: Use org.apache.commons.lang.ArrayUtils.indexOf() instead?
    private static final int COLUMN_INDEX_ID = 0;
    static final int COLUMN_INDEX_SUBJECT_ID = 1;


    /**
     * A dummy implementation of the {@link com.murrayc.galaxyzoo.app.ListFragment.Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        public void navigateToList() {

        }
    };

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;
    private View mLoadingView;

    /**
     * A callback interface that all activities containing some fragments must
     * implement. This mechanism allows activities to be notified of table
     * navigation selections.
     * <p/>
     * This is the recommended way for activities and fragments to communicate,
     * presumably because, unlike a direct function call, it still keeps the
     * fragment and activity implementations separate.
     * http://developer.android.com/guide/components/fragments.html#CommunicatingWithActivity
     */
    static interface Callbacks {
        public void navigateToList();
    }

    private View mRootView;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ClassifyFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_classify, container, false);
        assert mRootView != null;

        showLoadingView(false);

        setHasOptionsMenu(true);

        //TODO: Show the progress spinner while we are waiting for the subject to load,
        //particularly during first start when we are waiting to get the first data in our cache.
        update();

        return mRootView;
    }

    private void showLoadingView(boolean show) {
        if(mLoadingView == null) {
            mLoadingView = mRootView.findViewById(R.id.loading_spinner);
        }

        mLoadingView.setVisibility(show ? View.VISIBLE: View.GONE);
    }

    private void addOrUpdateChildFragments() {
        final Bundle arguments = new Bundle();
        //TODO? arguments.putString(ARG_USER_ID,
        //        getUserId()); //Obtained in the super class.
        arguments.putString(ItemFragment.ARG_ITEM_ID,
                getItemId());

        //Add, or update, the nested child fragments.
        //This can only be done programmatically, not in the layout XML.
        //See http://developer.android.com/about/versions/android-4.2.html#NestedFragments

        final FragmentManager fragmentManager = getChildFragmentManager();
        SubjectFragment fragmentSubject = (SubjectFragment)fragmentManager.findFragmentById(R.id.child_fragment_subject);
        if (fragmentSubject == null) {
            fragmentSubject = new SubjectFragment();
            fragmentSubject.setArguments(arguments);
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.child_fragment_subject, fragmentSubject).commit();
        } else {
            //TODO: Is there some more standard method to do this,
            //to trigger the Fragments' onCreate()?
            fragmentSubject.setItemId(getItemId());
            fragmentSubject.update();
        }


        QuestionFragment fragmentQuestion = (QuestionFragment)fragmentManager.findFragmentById(R.id.child_fragment_question);
        if (fragmentQuestion == null) {
            fragmentQuestion = new QuestionFragment();
            fragmentQuestion.setArguments(arguments);
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.child_fragment_question, fragmentQuestion).commit();
        } else {
            //TODO: Is there some more standard method to do this,
            //to trigger the Fragments' onCreate()?
            fragmentQuestion.setItemId(getItemId());
            fragmentQuestion.update();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.actionbar_menu_classify, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.option_menu_item_login:
                requestLogin();
                return true;
            case R.id.option_menu_item_list:
                mCallbacks.navigateToList();
                return true;
            case R.id.option_menu_item_upload:
                requestUpload();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    public void update() {
        final Activity activity = getActivity();
        if (activity == null)
            return;


        if (TextUtils.equals(getItemId(), ItemsContentProvider.URI_PART_ITEM_ID_NEXT)) {
            /*
             * Initializes the CursorLoader. The URL_LOADER value is eventually passed
             * to onCreateLoader().
             * We use restartLoader(), instead of initLoader(),
             * so we can refresh this fragment to show a different subject,
             * even when using the same query ("next") to do that.
             */
            getLoaderManager().restartLoader(URL_LOADER, null, this);
        } else {
            //Add, or update, the child fragments already, because we know the Item IDs:
            addOrUpdateChildFragments();
        }
    }

    private void updateFromCursor() {
        if (mCursor == null) {
            Log.error("mCursor is null.");
            return;
        }

        final Activity activity = getActivity();
        if (activity == null)
            return;

        if (mCursor.getCount() <= 0) { //In case the query returned no rows.
            Log.error("The ContentProvider query returned no rows.");
        }

        mCursor.moveToFirst(); //There should only be one anyway.

        if (mRootView == null) {
            Log.error("mRootView is null.");
            return;
        }

        //This will return the actual ID if we asked for the NEXT id.
        if(mCursor.getCount() > 0) {
            final String itemId = mCursor.getString(COLUMN_INDEX_ID);
            setItemId(itemId);
        }

        //TODO: Just update them.
        addOrUpdateChildFragments();
    }

    //We only bother using this when we have asked for the "next" item,
    //because we want to know its ID.
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        if (loaderId != URL_LOADER) {
            return null;
        }
        final String itemId = getItemId();
        if (TextUtils.isEmpty(itemId)) {
            return null;
        }

        //Asychronously get the actual ID,
        //because we have just asked for the "next" item.
        final Activity activity = getActivity();

        final Uri.Builder builder = Item.CONTENT_URI.buildUpon();
        builder.appendPath(itemId);

        showLoadingView(true);

        return new CursorLoader(
                activity,
                builder.build(),
                mColumns,
                null, // No where clause, return all records. We already specify just one via the itemId in the URI
                null, // No where clause, therefore no where column values.
                null // Use the default sort order.
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;

        showLoadingView(false);

        updateFromCursor();

        // Avoid this being called twice, which seems to be an Android bug,
        // and which could cause us to get a different item ID if our virtual "next" item changes to
        // another item:
        // See http://stackoverflow.com/questions/14719814/onloadfinished-called-twice
        // and https://code.google.com/p/android/issues/detail?id=63179
        getLoaderManager().destroyLoader(URL_LOADER);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        /*
         * Clears out our reference to the Cursor.
         * This prevents memory leaks.
         */
        mCursor = null;
    }
}
