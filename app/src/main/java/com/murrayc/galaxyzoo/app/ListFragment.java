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
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

//TODO: Why doesn't this need a layout resource?

/**
 * A list fragment representing a list of Tables. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link DetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link com.murrayc.galaxyzoo.app.ListFragment.Callbacks}
 * interface.
 */
public class ListFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

    private View mRootView;

    private static final int URL_LOADER = 0;
    private ListCursorAdapter mAdapter;
    private final String[] mColumns = { Item.Columns._ID, Item.Columns.SUBJECT_ID, Item.Columns.LOCATION_THUMBNAIL_URI};

    // We have to hard-code the indices - we can't use getColumnIndex because the Cursor
    // (actually a SQliteDatabase cursor returned
    // from our ContentProvider) only knows about the underlying SQLite database column names,
    // not our ContentProvider's column names. That seems like a design error in the Android API.
    //TODO: Use org.apache.commons.lang.ArrayUtils.indexOf() instead?
    private static final int COLUMN_INDEX_ID = 0;
    static final int COLUMN_INDEX_SUBJECT_ID = 1;
    static final int COLUMN_INDEX_LOCATION_THUMBNAIL_URI = 2;

    private void requestMoreItems() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final ContentResolver contentResolver = activity.getContentResolver();
        if (contentResolver == null) {
            return;
        }

        contentResolver.call(Item.ITEMS_URI, ItemsContentProvider.METHOD_REQUEST_ITEMS, null, null);
    }

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * A dummy implementation of the {@link com.murrayc.galaxyzoo.app.ListFragment.Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(final String itemId) {
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
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ListFragment() {
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
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
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        /*
         * Moves the query results into the adapter, causing the
         * ListView fronting this adapter to re-display.
         */
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        /*
         * Clears out the adapter's reference to the Cursor.
         * This prevents memory leaks.
         */
        mAdapter.changeCursor(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TableNavActivity will call update() when document loading has finished.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // We would only call the base class's onCreateView if we wanted the default layout:
        // super.onCreateView(inflater, container, savedInstanceState);

        mRootView = inflater.inflate(R.layout.fragment_list, container, false);
        assert mRootView != null;

        getGridView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                final GridView gridView = (GridView)parent;
                onGridItemClicked(gridView, position);
            }
        });


        setHasOptionsMenu(true);

        update();

        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.actionbar_menu_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.option_menu_item_more:
                requestMoreItems();
                return true;
            case R.id.option_menu_item_next:
                mCallbacks.navigateToNextAvailable();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void update() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mAdapter = new ListCursorAdapter(
                activity,
                null /* No cursor yet */);

        try {
            getGridView().setAdapter(mAdapter);
        } catch (final Exception e) {
            Log.error("glom", "setListAdapter() failed for query  with exception: " + e.getMessage());
        }

        /*
         * Initializes the CursorLoader. The URL_LOADER value is eventually passed
         * to onCreateLoader().
         */
        getLoaderManager().initLoader(URL_LOADER, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        final GridView gridView = getGridView();
        if (gridView == null)
            return;

        gridView.setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        final GridView gridView = getGridView();
        if (gridView == null)
            return;

        if (position == ListView.INVALID_POSITION) {
            gridView.setItemChecked(mActivatedPosition, false);
        } else {
            gridView.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    private void onGridItemClicked(GridView gridView, int position) {
        ListAdapter adapter = gridView.getAdapter();

        //When the ListView has header views, our adaptor will be wrapped by HeaderViewListAdapter:
        if (adapter instanceof HeaderViewListAdapter) {
            final HeaderViewListAdapter parentAdapter = (HeaderViewListAdapter) adapter;
            adapter = parentAdapter.getWrappedAdapter();
        }

        if (!(adapter instanceof CursorAdapter)) {
            Log.error("Unexpected Adapter class: " + adapter.getClass().toString());
            return;
        }

        // CursorAdapter.getItem() returns a  Cursor but that seems to be completely undocumented:
        // https://code.google.com/p/android/issues/detail?id=69973&thanks=69973&ts=1400841331
        final CursorAdapter cursorAdapter = (CursorAdapter) adapter;
        final Cursor cursor = (Cursor) cursorAdapter.getItem(position /* -1 if we we have a header */);
        if (cursor == null) {
            Log.error("cursorAdapter.getItem() returned null.");
            return;
        }

        final String itemId = cursor.getString(COLUMN_INDEX_ID);

        mCallbacks.onItemSelected(itemId);
    }

    public GridView getGridView() {
        final GridView gridView = (GridView)mRootView.findViewById(R.id.gridView);
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
    static interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(final String itemId);

        public void navigateToNextAvailable();
    }
}
