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
import android.text.TextUtils;
import android.view.MenuItem;

import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

/**
 * An activity showing a single subject. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link com.murrayc.galaxyzoo.app.ListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link com.murrayc.galaxyzoo.app.ClassifyFragment}.
 */
public class ClassifyActivity extends ItemActivity implements ItemFragment.Callbacks, QuestionFragment.Callbacks {

    private int mClassificationsDoneInSession = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(getItemId())) {
            setItemId(ItemsContentProvider.URI_PART_ITEM_ID_NEXT);
        }

        setContentView(R.layout.activity_classify);

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
            arguments.putString(ItemFragment.ARG_ITEM_ID,
                    getItemId());

            // TODO: Find a simpler way to just pass this through to the fragment.
            // For instance, pass the intent.getExtras() as the bundle?.
            final ClassifyFragment fragment = new ClassifyFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }

        /*
        // Show the Up button in the action bar.
        final ActionBar actionBar = getActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);
        */
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
            navigateUpTo(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClassificationFinished() {
        //Suggest registering or logging in after a certain number of classifications,
        //as the web UI does, but don't ask again.
        if(mClassificationsDoneInSession == 3) {
            if (!Utils.getLoggedIn(this)) {
                final Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }
        }
        mClassificationsDoneInSession++;

        //Start another classification:
        setItemId(ItemsContentProvider.URI_PART_ITEM_ID_NEXT);
        final ClassifyFragment fragmentClassify = (ClassifyFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        fragmentClassify.setItemId(ItemsContentProvider.URI_PART_ITEM_ID_NEXT);
        fragmentClassify.update();
    }
}
