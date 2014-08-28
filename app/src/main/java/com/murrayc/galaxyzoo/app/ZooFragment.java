package com.murrayc.galaxyzoo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

/**
 * Created by murrayc on 8/7/14.
 */
public class ZooFragment extends Fragment {
    protected void requestMoreItems() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final ContentResolver contentResolver = activity.getContentResolver();
        if (contentResolver == null) {
            return;
        }

        try {
            contentResolver.call(Item.ITEMS_URI, ItemsContentProvider.METHOD_REQUEST_ITEMS, null, null);
        } catch (final ItemsContentProvider.NoNetworkException e) {
            UiUtils.warnAboutNoNetworkConnection(activity);
        }
    }

    protected void requestUpload() {
        if(!getLoggedIn()) {
            final Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST_BEFORE_UPLOAD);
            //onActivityResult() will then be called later.
            return;
        }

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final ContentResolver contentResolver = activity.getContentResolver();
        if (contentResolver == null) {
            return;
        }

        try {
            contentResolver.call(Item.ITEMS_URI, ItemsContentProvider.METHOD_UPLOAD_CLASSIFICATIONS, null, null);
        } catch (final ItemsContentProvider.NoNetworkException e) {
            UiUtils.warnAboutNoNetworkConnection(activity);
        }
    }

    private boolean getLoggedIn() {
        final SharedPreferences prefs = Utils.getPreferences(getActivity());
        final String apiKey = prefs.getString(ItemsContentProvider.PREF_KEY_AUTH_NAME, null);
        return !(TextUtils.isEmpty(apiKey));
    }

    private static final int LOGIN_REQUEST_BEFORE_UPLOAD = 1;  // The request code

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_BEFORE_UPLOAD) {
            if (resultCode == Activity.RESULT_OK) {
                requestUpload();
            }
        }
    }
}
