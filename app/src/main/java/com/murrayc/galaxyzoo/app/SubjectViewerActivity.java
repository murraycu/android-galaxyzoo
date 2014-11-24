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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.view.MenuItem;

import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

/**
 * An activity showing a single subject. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link ClassifyFragment}.
 */
public class SubjectViewerActivity extends ItemActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(getItemId())) {
            setItemId(ItemsContentProvider.URI_PART_ITEM_ID_NEXT);
        }

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
                SubjectViewerFragment fragment = (SubjectViewerFragment) fragmentManager.findFragmentById(R.id.container);
                if (fragment == null) {
                    // Create the detail fragment and add it to the activity
                    // using a fragment transaction.
                    final Bundle arguments = new Bundle();
                    arguments.putString(ItemFragment.ARG_ITEM_ID,
                            getItemId());

                    fragment = new SubjectViewerFragment();
                    fragment.setArguments(arguments);
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, fragment)
                            .commit();
                } else {
                    Log.info("SubjectViewerActivity.onCreate(): The SubjectViewerFragment already existed.");

                    fragment.setItemId(getItemId());
                    fragment.update();
                }
            }
        }

        showUpButton();
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
            NavUtils.navigateUpTo(this, intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
