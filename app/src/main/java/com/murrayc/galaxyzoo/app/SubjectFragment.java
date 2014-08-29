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
import android.widget.ImageView;

import com.murrayc.galaxyzoo.app.provider.Item;

/**
 * A fragment representing a single subject.
 * This fragment is either contained in a {@link ListActivity}
 * in two-pane mode (on tablets) or a {@link ClassifyActivity}
 * on handsets.
 */
public class SubjectFragment extends ItemFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {


    private static final int URL_LOADER = 0;
    private static final String ARG_INVERTED = "inverted";
    private Cursor mCursor;

    private View mRootView;
    private ImageView mImageView;

    private final String[] mColumns = { Item.Columns._ID, Item.Columns.SUBJECT_ID, Item.Columns.LOCATION_STANDARD_URI, Item.Columns.LOCATION_INVERTED_URI};

    // We have to hard-code the indices - we can't use getColumnIndex because the Cursor
    // (actually a SQliteDatabase cursor returned
    // from our ContentProvider) only knows about the underlying SQLite database column names,
    // not our ContentProvider's column names. That seems like a design error in the Android API.
    //TODO: Use org.apache.commons.lang.ArrayUtils.indexOf() instead?
    private static final int COLUMN_INDEX_ID = 0;
    static final int COLUMN_INDEX_SUBJECT_ID = 1;
    static final int COLUMN_INDEX_LOCATION_STANDARD_URI = 2;
    static final int COLUMN_INDEX_LOCATION_INVERTED_URI = 3;
    private boolean mInverted = false;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubjectFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            setInverted(savedInstanceState.getBoolean(ARG_INVERTED));
        }
        setHasOptionsMenu(true);
    }

    private void setInverted(boolean inverted) {
        mInverted = inverted;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_subject, container, false);
        assert mRootView != null;

        mImageView = (ImageView)mRootView.findViewById(R.id.imageView);
        if (mImageView == null) {
            Log.error("mImageView is null.");
        }

        setHasOptionsMenu(true);

        update();

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        //Save extra state to be used later by onCreate().
        outState.putBoolean(ARG_INVERTED, getInverted());

        //TODO: Allow use of ARG_INVERTED in the intent's arguments too?
        //For instance, see QuestionFragment.onSaveInstanceState().

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final MenuItem menuItem = menu.add(Menu.NONE, R.id.option_menu_item_invert, Menu.NONE, R.string.action_invert);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.option_menu_item_invert:
                doInvert();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void doInvert() {
        setInverted(!getInverted());
        showImage();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void update() {
        final Activity activity = getActivity();
        if (activity == null)
            return;

        /*
         * Initializes the CursorLoader. The URL_LOADER value is eventually passed
         * to onCreateLoader().
         * We use restartLoader(), instead of initLoader(),
         * so we can refresh this fragment to show a different subject,
         * even when using the same query ("next") to do that.
         */
        getLoaderManager().restartLoader(URL_LOADER, null, this);
    }

    private void updateFromCursor() {
        if (mCursor == null) {
            Log.error("mCursor is null.");
            return;
        }

        if (mCursor.getCount() <= 0) { //In case the query returned no rows.
            Log.error("The ContentProvider query returned no rows.");

            //Check for this possible cause.
            // TODO: Is there any simpler way to just catch the
            // ItemsContentProvider.NoNetworkConnection exception in the CursorLoader?
            final Activity activity = getActivity();
            if (!Utils.getNetworkIsConnected(activity)) {
                UiUtils.warnAboutNoNetworkConnection(activity);
            }

            return;
        }

        mCursor.moveToFirst(); //There should only be one anyway.

        //Look at each group in the layout:
        if (mRootView == null) {
            Log.error("mRootView is null.");
            return;
        }

        showImage();
    }

    private void showImage() {
        final Activity activity = getActivity();
        if (activity == null)
            return;

        final boolean inverted = getInverted();
        String imageUriStr;
        if (inverted) {
            imageUriStr = mCursor.getString(COLUMN_INDEX_LOCATION_INVERTED_URI);
        } else {
            imageUriStr = mCursor.getString(COLUMN_INDEX_LOCATION_STANDARD_URI);
        }

        UiUtils.fillImageViewFromContentUri(activity, imageUriStr, mImageView);
    }

    private boolean getInverted() {
        return mInverted;
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
