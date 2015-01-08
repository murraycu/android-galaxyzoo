/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;


public class ExampleViewerActivity extends BaseActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example_viewer);

        UiUtils.showToolbar(this);

        final Intent intent = getIntent();
        final String uriStr = intent.getStringExtra(ExampleViewerFragment.ARG_EXAMPLE_URL);

        if (savedInstanceState == null) {

            final FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager != null) {
                //We check to see if the fragment exists already,
                //because apparently it sometimes does exist already when the app has been
                //in the background for some time,
                //at least on Android 5.0 (Lollipop)
                ExampleViewerFragment fragment = (ExampleViewerFragment) fragmentManager.findFragmentById(R.id.container);
                if (fragment == null) {
                    final Bundle arguments = new Bundle();
                    arguments.putString(ExampleViewerFragment.ARG_EXAMPLE_URL, uriStr);

                    fragment = new ExampleViewerFragment();
                    fragment.setArguments(arguments);
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, fragment)
                            .commit();
                } else {
                    Log.info("ExampleViewerActivity.onCreate(): The ExampleViewerFragment already existed.");

                    fragment.setExampleUrl(uriStr);
                    fragment.update();
                }
            }
        }

        showUpButton();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle presses on the action bar items
        final int id = item.getItemId();

        if (id == android.R.id.home) {
            //The base class just uses NavUtils.navigateUpFromSameTask() but we want to make sure
            //that the parent activity will be resumed instead of restarted,
            //to make sure we go back to the correct help for the correct question,
            //so we use FLAG_ACTIVITY_CLEAR_TOP.
            //Alternatively, we could just mark QuestionHelpActivity as
            //android:launchMode="singleTop" in the AndroidManifest.xml but it seems reasonable to
            //use QuestionHelpActivity from other places one day.

            //We can use this instead of more complex code, checking NavUtils.shouldUpRecreateTask(),
            //because we know that our activities will never be opened from another app.
            //See http://developer.android.com/training/implementing-navigation/ancestral.html.
            final Intent upIntent = NavUtils.getParentActivityIntent(this);
            if (upIntent != null) {
                upIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }

            NavUtils.navigateUpTo(this, upIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
