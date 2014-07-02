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

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuItem;

/**
 * An activity representing a single record of a single Table. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link DetailFragment}.
 */
public class DetailActivity extends BaseActivity implements DetailFragment.Callbacks {

    private String mItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

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
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            final Bundle arguments = new Bundle();
            arguments.putString(ARG_USER_ID,
                    getUserId()); //Obtained in the super class.
            arguments.putString(DetailFragment.ARG_ITEM_ID,
                    getItemId());

            // TODO: Find a simpler way to just pass this through to the fragment.
            // For instance, pass the intent.getExtras() as the bundle?.
            final Intent intent = getIntent();

            final DetailFragment fragment = new DetailFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .add(R.id.table_data_container, fragment)
                    .commit();
        }

        // Show the Up button in the action bar.
        final ActionBar actionBar = getActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private DetailFragment getTableDetailFragment() {
        return ((DetailFragment) getFragmentManager()
                .findFragmentById(R.id.table_data_container));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
            intent.putExtra(ARG_USER_ID, getUserId());
            navigateUpTo(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String getItemId() {
        return mItemId;
    }
}
