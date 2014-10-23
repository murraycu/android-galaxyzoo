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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
//import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

/**
 * Created by murrayc on 2/7/14.
 */
@SuppressLint("Registered")
//This is a base class for other Activities.
class BaseActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
        */


        //This lets us know what MIME Type to mention in the intent filter in the manifest file,
        //as long as we cannot register a more specific MIME type.
        //String type = intent.getType();
        //Log.v("glomdebug", "type=" + type);
    }

    /**
     *
     * @param itemId
     * @param done
     * @param sharedElementView A shared element for use with a transition animation.
     */
    void navigate(final String itemId, final boolean done, final View sharedElementView) {
        // Start the detail activity
        // for the selected item ID.
        final Intent intent = new Intent(this,
                done ? SubjectViewerActivity.class : ClassifyActivity.class);
        if (!TextUtils.isEmpty(itemId)) {
            intent.putExtra(ItemFragment.ARG_ITEM_ID, itemId);
        }

        // get the common element for the transition in this activity
        if (sharedElementView != null) {

            //"subjectImageTransition" is also specified as transitionName="subjectImageTransition"
            //on the ImageView in both gridview_cell_fragment_list.xml and fragment_subject.xml.
            //TODO: Why do we need to specify it again here?
            final ActivityOptionsCompat options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, sharedElementView, SubjectFragment.TRANSITION_NAME_SUBJECT_IMAGE);
            ActivityCompat.startActivity(this, intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        final int id = item.getItemId();

        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void showUpButton() {
        // Show the Up button in the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
