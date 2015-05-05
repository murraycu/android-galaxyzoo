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
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;


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
 * {@link ListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ListActivity extends BaseActivity
        implements ListFragment.Callbacks {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        UiUtils.showToolbar(this);

        showUpButton();
    }

    /**
     * Callback method from {@link ListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(final String itemId, final boolean done, final View sharedElementView) {
        navigate(itemId, done, sharedElementView);
    }

    @Override
    public void navigateToNextAvailable() {
        navigate(null, false /* not done */, null); //null means next for itemId
    }

    /**
     *
     * @param itemId
     * @param done
     * @param sharedElementView A shared element for use with a transition animation.
     */
    private void navigate(final String itemId, final boolean done, final View sharedElementView) {
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
            ActivityCompat.startActivity(this, intent, UiUtils.getTransitionOptionsBundle(this, sharedElementView));
        } else {
            startActivity(intent);
        }
    }
}
