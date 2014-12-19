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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

/**
 * Created by murrayc on 8/1/14.
 */
@SuppressLint("Registered") //It is not an actual activity - it is just a base class.
public class ItemActivity extends BaseActivity implements ItemFragment.Callbacks {
    private String mItemId = null;

    //This is not private, so we can use it in tests.
    public String getItemId() {
        return mItemId;
    }

    //This is not private, so we can use it in tests.
    public void setItemId(final String itemId) {
        mItemId = itemId;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        //Show a requested item, or just show the next available item:
        String itemId = intent.getStringExtra(ItemFragment.ARG_ITEM_ID);
        if (TextUtils.isEmpty(itemId)) {
            itemId = ItemsContentProvider.URI_PART_ITEM_ID_NEXT;
        }
        setItemId(itemId);
    }

    @Override
    public void navigateToList() {
        final Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    @Override
    public void abandonItem() {
       //Overriden in ClassifyActivity().
    }
}
