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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;


public class ExampleViewerActivity extends ActionBarActivity {

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

        // Show the Up button in the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
