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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.Item;

/**
 * A fragment representing a single subject.
 * This fragment is either contained in a {@link ListActivity}
 * in two-pane mode (on tablets) or a {@link ClassifyActivity}
 * on handsets.
 */
public class SubjectExtrasFragment extends ItemFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int URL_LOADER = 0;
    // We have to hard-code the indices - we can't use getColumnIndex because the Cursor
    // (actually a SQliteDatabase cursor returned
    // from our ContentProvider) only knows about the underlying SQLite database column names,
    // not our ContentProvider's column names. That seems like a design error in the Android API.
    //TODO: Use org.apache.commons.lang.ArrayUtils.indexOf() instead?
    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_ZOONIVERSE_ID = 1;
    private final String[] mColumns = {Item.Columns._ID, Item.Columns.ZOONIVERSE_ID};
    private Cursor mCursor;
    private String mZooniverseId; //Only used for the talk URI so far.

    // A map of checkbox IDs to buttons.
    private boolean mLoaderFinished = false;
    private View mRootView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubjectExtrasFragment() {
    }

    private String getZooniverseId() {
        return mZooniverseId;
    }

    private void setZooniverseId(final String zooniverseId) {
        mZooniverseId = zooniverseId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    protected void setItemId(String itemId) {
        super.setItemId(itemId);

        /*
         * Initializes the CursorLoader. The URL_LOADER value is eventually passed
         * to onCreateLoader().
         * This lets us get the Zooniverse ID for the item, for use in the discussion page's URI.
         * We use restartLoader(), instead of initLoader(),
         * so we can refresh this fragment to show a different subject,
         * even when using the same query ("next") to do that.
         */
        mLoaderFinished = false; //Don't update() until this is ready.
        getLoaderManager().restartLoader(URL_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_subject_extras, container, false);
        assert mRootView != null;

        initializeSingleton();

        //This will be called later by updateIfReady(): update();

        return mRootView;
    }


    @Override
    void onSingletonInitialized() {
        super.onSingletonInitialized();

        updateIfReady();
    }

    public void update() {
        final Activity activity = getActivity();
        if (activity == null)
            return;

        if (mRootView == null) {
            //This can happen when update() is called by the parent fragment
            //after this fragment has been instantiated after an orientation change,
            //but before onCreateView() has been called. It's not a problem
            //because onCreateView() will call this method again after setting mRootView.
            //Log.error("SubjectsExtraFragment.update(): mRootView is null.");
            return;
        }

        //Show the title:
        final TextView textViewTitle = (TextView) mRootView.findViewById(R.id.textViewZooniverseId);
        if (textViewTitle == null) {
            Log.error("textViewTitle is null.");
            return;
        }
        textViewTitle.setText(getZooniverseId());

        //Connect the buttons:
        final Button buttonExamine = (Button) mRootView.findViewById(R.id.buttonExamine);
        if (buttonExamine == null) {
            Log.error("buttonExamine is null.");
            return;
        }
        buttonExamine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doExamine();
            }
        });

        final Button buttonDiscuss = (Button) mRootView.findViewById(R.id.buttonDiscuss);
        if (buttonDiscuss == null) {
            Log.error("buttonDiscuss is null.");
            return;
        }
        buttonDiscuss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDiscuss();
            }
        });

    }

    private void doDiscuss() {
        //Open a link to the discussion page.
        UiUtils.openDiscussionPage(getActivity(), getZooniverseId());
    }

    private void doExamine() {
        final Activity activity = getActivity();

        //Open a link to the examine page:
        final String uriTalk = Config.EXAMINE_URI + getZooniverseId();

        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriTalk));
            activity.startActivity(intent);
        } catch (final ActivityNotFoundException e) {
            Log.error("Could not open the examine URI.", e);
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

            //Check for this possible cause.
            // TODO: Is there any simpler way to just catch the
            // ItemsContentProvider.NoNetworkConnection exception in the CursorLoader?
            if (!Utils.getNetworkIsConnected(activity)) {
                UiUtils.warnAboutNoNetworkConnection(activity);
            }

            return;
        }

        if (mCursor.getColumnCount() <= 0) { //In case the query returned no columns.
            Log.error("The ContentProvider query returned no columns.");
            return;
        }

        mCursor.moveToFirst(); //There should only be one anyway.

        //Look at each group in the layout:
        if (mRootView == null) {
            Log.error("SubjectsExtraFragment.update(): mRootView is null.");
            return;
        }

        final String zooniverseId = mCursor.getString(COLUMN_INDEX_ZOONIVERSE_ID);
        setZooniverseId(zooniverseId);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        if (loaderId != URL_LOADER) {
            return null;
        }

        final String itemId = getItemId();
        if (TextUtils.isEmpty(itemId)) {
            return null;
        }

        final Activity activity = getActivity();

        final Uri.Builder builder = Item.CONTENT_URI.buildUpon();
        builder.appendPath(itemId);

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
        updateFromCursor();

        mLoaderFinished = true;
        updateIfReady();

        // Avoid this being called twice (actually multiple times), which seems to be an Android bug:
        // See http://stackoverflow.com/questions/14719814/onloadfinished-called-twice
        // and https://code.google.com/p/android/issues/detail?id=63179
        getLoaderManager().destroyLoader(URL_LOADER);
    }

    private void updateIfReady() {
        if (mLoaderFinished && (getSingleton() != null)) {
            update();
        }
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
