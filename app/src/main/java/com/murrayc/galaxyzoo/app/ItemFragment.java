package com.murrayc.galaxyzoo.app;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by murrayc on 7/31/14.
 */
public class ItemFragment extends Fragment {
    /**
     * The fragment argument representing the item that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item-id";
    protected String mItemId;
    private long mUserId = -1;

    private long getUserId() {
        return mUserId;
    }

    protected String getItemId() {
        return mItemId;
    } //TODO: Should this be a long?

    protected void setItemId(final String itemId) {
        mItemId = itemId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getArguments();
        if (bundle != null) {
            setItemId(bundle.getString(ARG_ITEM_ID));
        }
    }
}
