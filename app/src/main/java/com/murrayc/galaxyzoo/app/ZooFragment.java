package com.murrayc.galaxyzoo.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.view.MenuItem;

import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

/**
 * Created by murrayc on 8/7/14.
 */
public class ZooFragment extends Fragment {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.option_menu_item_login:
                requestLogin();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void requestMoreItems() {
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

    void requestLogin() {

        final Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
    }
}
