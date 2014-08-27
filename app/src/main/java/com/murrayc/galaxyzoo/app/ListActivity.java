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

import android.os.Bundle;


/**
 * An activity representing a list of Tables. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link DetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ListFragment} and the item details
 * (if present) is a {@link ClassifyFragment}.
 * <p/>
 * This activity also implements the required
 * {@link com.murrayc.galaxyzoo.app.ListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ListActivity extends BaseActivity
        implements ListFragment.Callbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        //Don't use the action bar on this top-level activity,
        //though we have it via the shared base class.
        //TODO: ActionBar actionBar = getActionBar();
        //actionBar.hide();
    }

    /**
     * Callback method from {@link com.murrayc.galaxyzoo.app.ListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(final String itemId) {
        navigate(itemId);
    }

    @Override
    public void navigateToNextAvailable() {
        navigate(null); //null means next.
    }
}
