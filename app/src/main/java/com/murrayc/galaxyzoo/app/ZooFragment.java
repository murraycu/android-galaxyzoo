package com.murrayc.galaxyzoo.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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
            case R.id.option_menu_item_about:
                showAbout();
                return true;
            case R.id.option_menu_item_settings:
                showSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
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
    */

    void requestLogin() {
        final Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
    }

    void showAbout() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.about, null);
        builder.setView(view);

        //This voodoo makes the textviews' HTML links clickable:
        //See http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable/20647011#20647011
        final TextView textView = (TextView) view.findViewById(R.id.textViewAbout);
        if (textView != null) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        final TextView textViewVersion = (TextView) view.findViewById(R.id.textViewVersion);
        if (textViewVersion != null) {
            final String versionText =
                    String.format(getString(R.string.about_version_text_format), BuildConfig.VERSION_NAME);
            textViewVersion.setText(versionText);
        }

        final AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.app_name);
        dialog.setIcon(R.drawable.ic_launcher);

        dialog.show();
    }

    void showSettings() {
        final Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }
}
