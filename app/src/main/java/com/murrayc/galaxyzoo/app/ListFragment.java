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
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.murrayc.galaxyzoo.app.provider.Item;

//TODO: Why doesn't this need a layout resource?

/**
 * A list fragment representing a list of Tables. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ClassifyFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link ListFragment.Callbacks}
 * interface.
 */
public class ListFragment extends ZooFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // We have to hard-code the indices - we can't use getColumnIndex because the Cursor
    // (actually a SQliteDatabase cursor returned
    // from our ContentProvider) only knows about the underlying SQLite database column names,
    // not our ContentProvider's column names. That seems like a design error in the Android API.
    //TODO: Use org.apache.commons.lang.ArrayUtils.indexOf() instead?
    static final int COLUMN_INDEX_ID = 0;
    static final int COLUMN_INDEX_LOCATION_THUMBNAIL_URI = 1;
    static final int COLUMN_INDEX_LOCATION_THUMBNAIL_DOWNLOADED = 2;
    static final int COLUMN_INDEX_DONE = 3;
    static final int COLUMN_INDEX_UPLOADED = 4;
    static final int COLUMN_INDEX_FAVOURITE = 5;
    private static final int URL_LOADER = 0;

    /**
     * A dummy implementation of the {@link ListFragment.Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(final String itemId, final boolean done, final View sharedElementView) {
        }

        @Override
        public void navigateToNextAvailable() {
        }
    };

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;
    private final String[] mColumns = {Item.Columns._ID,
            Item.Columns.LOCATION_THUMBNAIL_URI, Item.Columns.LOCATION_THUMBNAIL_DOWNLOADED,
            Item.Columns.DONE, Item.Columns.UPLOADED, Item.Columns.FAVORITE};
    private View mRootView = null;
    private ListCursorAdapter mAdapter = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ListFragment() {
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int loaderId, final Bundle bundle) {
        if (loaderId != URL_LOADER) {
            return null;
        }

        final Activity activity = getActivity();

        final Uri uriItems = Item.ITEMS_URI;

        return new CursorLoader(
                activity,
                uriItems,
                mColumns,
                null, // No where clause, return all records.
                null, // No where clause, therefore no where column values.
                null // Use the default sort order.
        );
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {
        /*
         * Moves the query results into the adapter, causing the
         * ListView fronting this adapter to re-display.
         */
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> cursorLoader) {
        /*
         * Clears out the adapter's reference to the Cursor.
         * This prevents memory leaks.
         */
        mAdapter.changeCursor(null);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TableNavActivity will call update() when document loading has finished.
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // We would only call the base class's onCreateView if we wanted the default layout:
        // super.onCreateView(inflater, container, savedInstanceState);
        mRootView = inflater.inflate(R.layout.fragment_list, container, false);
        assert mRootView != null;

        setHasOptionsMenu(true);

        update();

        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.actionbar_menu_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            /*
            case R.id.option_menu_item_more:
                requestMoreItems();
                return true;
            */
            case R.id.option_menu_item_next:
                mCallbacks.navigateToNextAvailable();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void update() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mAdapter = new ListCursorAdapter(
                activity,
                /* No cursor yet */
                new ListCursorAdapter.OnItemClickedListener() {
                    @Override
                    public void onItemClicked(final int position, final View sharedElementView) {
                        onGridItemClicked(position, sharedElementView);
                    }
                });

        //TODO: Can we specify the layout manager in the layout XML?
        int gridSpan = 3; //A suitable default for the error case.
        final Resources resources = getResources();
        if (resources != null) {
            //This is different depending on the screen width:
            final int resourcesGridSpan = resources.getInteger(R.integer.list_grid_span);

            //A sanity check:
            if (resourcesGridSpan > 0) {
                gridSpan = resourcesGridSpan;
            }
        }

        final RecyclerView gridView = getGridView();
        if (gridView == null) {
            Log.error("update(): gridView is null.");
            return;
        }

        gridView.setLayoutManager(
                new GridLayoutManager(activity, gridSpan));


        //This is apparently already the default:
        //gridView.setItemAnimator(new DefaultItemAnimator());

        //For performance, because all our items are the same size:
        gridView.setHasFixedSize(true);

        gridView.setAdapter(mAdapter);

        /*
         * Initializes the CursorLoader. The URL_LOADER value is eventually passed
         * to onCreateLoader().
         */
        getLoaderManager().initLoader(URL_LOADER, null, this);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        final Activity activity = getActivity();

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

    private void onGridItemClicked(final int position, final View sharedElementView) {
        final RecyclerView gridView = getGridView();
        if (gridView == null) {
            Log.error("onGridItemClicked(): gridView is null.");
            return;
        }

        final RecyclerView.Adapter adapter = gridView.getAdapter();

        /*
        //When a ListView has header views, our adaptor will be wrapped by HeaderViewListAdapter:
        if (adapter instanceof HeaderViewListAdapter) {
            final HeaderViewListAdapter parentAdapter = (HeaderViewListAdapter) adapter;
            adapter = parentAdapter.getWrappedAdapter();
        }
        */

        if (!(adapter instanceof ListCursorAdapter)) {
            Log.error("Unexpected Adapter class: " + adapter.getClass());
            return;
        }

        // CursorAdapter.getItem() returns a  Cursor but that seems to be completely undocumented:
        // https://code.google.com/p/android/issues/detail?id=69973&thanks=69973&ts=1400841331
        final ListCursorAdapter cursorAdapter = (ListCursorAdapter) adapter;
        final Cursor cursor =  cursorAdapter.getItem(position /* -1 if we we have a header */);
        if (cursor == null) {
            Log.error("cursorAdapter.getItem() returned null.");
            return;
        }

        final boolean imageDownloaded = (cursor.getInt(COLUMN_INDEX_LOCATION_THUMBNAIL_DOWNLOADED) == 1);
        if (!imageDownloaded) {
            //Just ignore clicks on items that are still downloading.
            //TODO: We don't check tha the full image has been downloaded,
            //so make sure that the activities can show them when they are ready.
            return;
        }

        final String itemId = cursor.getString(COLUMN_INDEX_ID);
        final boolean done = (cursor.getInt(COLUMN_INDEX_DONE) == 1);

        //Disable the ability to classify a not-yet-done item by selecting it from
        //the list.
        //See also ListCursorAdapter.onBindViewHolder(), where we prevent clicks anyway.
        //TODO: Remove the done parameter from onItemSelected if we keep this disabled.
        if (done) {
            mCallbacks.onItemSelected(itemId, done, sharedElementView);
        }
    }

    private RecyclerView getGridView() {
        final RecyclerView gridView = (RecyclerView) mRootView.findViewById(R.id.gridView);
        if (gridView == null) {
            Log.error("gridView is null.");
        }

        return gridView;
    }

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
    interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        void onItemSelected(final String itemId, boolean done, final View sharedElementView);

        void navigateToNextAvailable();
    }
}
