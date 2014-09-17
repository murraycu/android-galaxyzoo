package com.murrayc.galaxyzoo.app;

import android.os.Bundle;

/**
 * Created by murrayc on 7/31/14.
 */
public class ItemFragment extends ZooFragment {
    /**
     * The fragment argument representing the item that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item-id";
    private String mItemId;
    Singleton mSingleton = null;

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
    public void onCreate(Bundle savedInstanceState) {
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
}
