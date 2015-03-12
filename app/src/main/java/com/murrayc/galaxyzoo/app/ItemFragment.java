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

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by murrayc on 7/31/14.
 */
public class ItemFragment extends ZooFragment {
    /**
     * The fragment argument representing the item that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item-id";

    /**
     * A dummy implementation of the {@link com.murrayc.galaxyzoo.app.ListFragment.Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        public void navigateToList() {
        }

        @Override
        public void abandonItem() {
        }
    };

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;
    private Singleton mSingleton = null;
    private String mItemId = null;

    String getItemId() {
        return mItemId;
    } //TODO: Should this be a long?

    void setItemId(final String itemId) {
        mItemId = itemId;
    }

    Singleton getSingleton() {
        return mSingleton;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //The item ID in savedInstanceState (from onSaveInstanceState())
        //overrules the item ID in the intent's arguments,
        //because the fragment may have been created with the virtual "next" ID,
        //but we replace that with the actual ID,
        //and we don't want to lost that actual ID when the fragment is recreated after
        //rotation.
        if (savedInstanceState != null) {
            setItemId(savedInstanceState.getString(ARG_ITEM_ID));
        } else {
            final Bundle bundle = getArguments();
            if (bundle != null) {
                setItemId(bundle.getString(ARG_ITEM_ID));
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(final Activity activity) {
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

    /**
     * Call this from a derived Fragment's onCreateOptionsMenu() override,
     * if you want this fragment to provide the common menu.
     * <p/>
     * This menu isn't added by default because then we could have
     * duplicate menu items if there are two ItemFragment-derived
     * child fragments in an activity, or as child fragments.
     *
     * @param menu
     * @param inflater
     */
    protected void createCommonOptionsMenu(final Menu menu, final MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.actionbar_menu_item_common, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.option_menu_item_list:
                mCallbacks.navigateToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        //Save state to be used later by onCreate().
        //If we don't do this then we we will lose the actual ID that we are using.
        //This way we can get the actual ID back again in onCreate().
        //Otherwise, on rotation, onCreateView() will just get the "next" ID that was first used
        //to create the fragment.
        outState.putString(ARG_ITEM_ID, getItemId());

        super.onSaveInstanceState(outState);
    }

    void onSingletonInitialized() {
        this.mSingleton = Singleton.getInstance();
    }

    void initializeSingleton() {
        Singleton.init(getActivity(), new Singleton.Callbacks() {
            @Override
            public void onInitialized() {
                onSingletonInitialized();
            }
        });
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
    interface Callbacks {
        void navigateToList();

        /** This is called when the fragment found something about the item
         * that means we should just give up and try another one.
         */
        void abandonItem();
    }

    void abandonItem() {
        Log.error("ItemFragment.abandonItem(): Abandoning item with itemId=" + getItemId());

        Utils.abandonItem(getActivity(), getItemId());
        mCallbacks.abandonItem();
    }
}
