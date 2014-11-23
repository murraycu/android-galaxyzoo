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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

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
    // We have to hard-code the indices - we can't use getColumnIndex because the Cursor
    // (actually a SQliteDatabase cursor returned
    // from our ContentProvider) only knows about the underlying SQLite database column names,
    // not our ContentProvider's column names. That seems like a design error in the Android API.
    //TODO: Use org.apache.commons.lang.ArrayUtils.indexOf() instead?
    /* private static final int COLUMN_INDEX_ID = 0; */
    private static final int COLUMN_INDEX_LOCATION_STANDARD_URI = 1;
    private static final int COLUMN_INDEX_LOCATION_STANDARD_DOWNLOADED = 2;
    private static final int COLUMN_INDEX_LOCATION_INVERTED_URI = 3;
    private static final int COLUMN_INDEX_LOCATION_INVERTED_DOWNLOADED = 4;
    private final String[] mColumns = {Item.Columns._ID,
            Item.Columns.LOCATION_STANDARD_URI, Item.Columns.LOCATION_STANDARD_DOWNLOADED,
            Item.Columns.LOCATION_INVERTED_URI, Item.Columns.LOCATION_INVERTED_DOWNLOADED};
    private Cursor mCursor;
    private View mRootView;
    private ImageView mImageView;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_subject, container, false);
        assert mRootView != null;

        mImageView = (ImageView) mRootView.findViewById(R.id.imageView);
        if (mImageView == null) {
            Log.error("mImageView is null.");
        }

        //Make the image invert when clicked,
        //like in the web UI:
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doInvert();
            }
        });

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
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.actionbar_menu_subject, menu);

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

        //If the item is the next ID, then wait for the parent (ClassifyFragment) fragment
        //to discover the real ID, after which it will update this fragment with the real ID.
        //Otherwise, we will quickly ask for two next items at almost the same time,
        //resulting in a longer wait for the first item (at first app start) to be ready.
        if(!TextUtils.equals(getItemId(), ItemsContentProvider.URI_PART_ITEM_ID_NEXT)) {
            /*
             * Initializes the CursorLoader. The URL_LOADER value is eventually passed
             * to onCreateLoader().
             * We use restartLoader(), instead of initLoader(),
             * so we can refresh this fragment to show a different subject,
             * even when using the same query ("next") to do that.
             */
            getLoaderManager().restartLoader(URL_LOADER, null, this);
        }
    }

    private void updateFromCursor() {
        if (mCursor == null) {
            Log.error("mCursor is null.");
            return;
        }

        if (mCursor.getCount() <= 0) { //In case the query returned no rows.
            Log.error("SubjectFragment.updateFromCursor(): The ContentProvider query returned no rows.");

            //Wipe the image instead of keeping whatever might be showing already:
            mImageView.setImageDrawable(null);

            UiUtils.warnAboutMissingNetwork(getActivity());

            return;
        }

        mCursor.moveToFirst(); //There should only be one anyway.

        //Look at each group in the layout:
        if (mRootView == null) {
            Log.error("SubjectFragment.updateFromCursor(): mRootView is null.");
            return;
        }

        showImage();
    }

    private void showImage() {
        final Activity activity = getActivity();
        if (activity == null)
            return;

        //Avoid a crash in the unusual case that the ContentProvider
        //didn't provide an item.
        if (mCursor.getCount() < 1 || mCursor.getColumnCount() < 1) {
            return;
        }

        //Avoid a crash in the unusual case that the ContentProvider
        //didn't provide an item.
        if (mCursor.getCount() < 1 || mCursor.getColumnCount() < 1) {
            return;
        }

        final boolean inverted = getInverted();
        String imageUriStr = null;
        if (inverted) {
            if (mCursor.getInt(COLUMN_INDEX_LOCATION_INVERTED_DOWNLOADED) == 1) {
                imageUriStr = mCursor.getString(COLUMN_INDEX_LOCATION_INVERTED_URI);
            }
        } else {
            if (mCursor.getInt(COLUMN_INDEX_LOCATION_STANDARD_DOWNLOADED) == 1) {
                imageUriStr = mCursor.getString(COLUMN_INDEX_LOCATION_STANDARD_URI);
            }
        }

        if (!TextUtils.isEmpty(imageUriStr)) {
            final UiUtils.ShowImageFromContentProviderTask task = new UiUtils.ShowImageFromContentProviderTask(mImageView, activity) {
                @Override
                protected void onPostExecute(final Bitmap bitmap) {
                    super.onPostExecute(bitmap);

                    if (bitmap == null) {
                        //Something was wrong with the (cached) image,
                        //so just abandon this whole item.
                        //That seems safer and simpler than trying to recover just one of the 3 images.
                        SubjectFragment.this.abandonItem();
                    }
                }
            };
            task.execute(imageUriStr);
        }
    }

    private boolean getInverted() {
        return mInverted;
    }

    void setInverted(boolean inverted) {
        mInverted = inverted;
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
