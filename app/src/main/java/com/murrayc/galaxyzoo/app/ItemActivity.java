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
public class ItemActivity extends BaseActivity {
    private String mItemId;

    String getItemId() {
        return mItemId;
    }

    void setItemId(final String itemId) {
        mItemId = itemId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        //Show a requested item, or just show the next available item:
        String itemId = intent.getStringExtra(ItemFragment.ARG_ITEM_ID);
        if(TextUtils.isEmpty(itemId)) {
            itemId = ItemsContentProvider.URI_PART_ITEM_ID_NEXT;
        }
        setItemId(itemId);
    }
}
