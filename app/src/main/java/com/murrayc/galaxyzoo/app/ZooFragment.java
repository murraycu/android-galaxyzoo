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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by murrayc on 8/7/14.
 */
public class ZooFragment extends Fragment {
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        cacheLoggedInStatus();
    }

    /** Asynchronously cache the log in status in loggedIn.
     * Derived classes can call this in their onCreateView.
     **/
    private void cacheLoggedInStatus() {
        final LoginUtils.GetExistingLogin task = new LoginUtils.GetExistingLogin(getActivity()) {
            @Override
            protected void onPostExecute(final LoginUtils.LoginDetails loginDetails) {
                super.onPostExecute(loginDetails);

                if (mException != null) {
                    Log.error("ZooFragment.cacheLoggedInStatus(): GetExistingLogin asynctask failed, probably due to a missing permission:", mException);
                }

                ZooFragment.setCachedLoggedIn(LoginUtils.getLoggedIn(loginDetails));
            }
        };
        task.execute();
    }

    public static void setCachedLoggedIn(final boolean loggedIn) {
        final Singleton singleton = Singleton.getInstance();

        //This can happen before the Singleton has been initialized, during startup.
        //It doesn't matter at that point.
        if (singleton == null) {
            return;
        }

        singleton.setCachedLoggedIn(loggedIn);
    }

    private static boolean getCachedLoggedIn() {
        final Singleton singleton = Singleton.getInstance();

        //This can happen before the Singleton has been initialized, during startup.
        //It doesn't matter at that point.
        if (singleton == null) {
            return false;
        }

        return singleton.getCachedLoggedIn();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.option_menu_item_login:
                requestLogin();
                return true;
            case R.id.option_menu_item_logout:
                requestLogout();
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

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //Before the menu item is displayed,
        //show either "Log in" or "Log out":
        //This depends on cacheLoggedInStatus() having been called in onResume().
        final MenuItem itemLogin = menu.findItem(R.id.option_menu_item_login);
        final MenuItem itemLogout = menu.findItem(R.id.option_menu_item_logout);

        if ((itemLogin == null) || (itemLogout == null)) {
            Log.error("onPrepareOptionsMenu(): Could not find menu items.");
            return;
        }

        final boolean loggedIn = getCachedLoggedIn();
        itemLogin.setVisible(!loggedIn);
        itemLogout.setVisible(loggedIn);
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

    private void requestLogin() {
        final Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
    }

    private void requestLogout() {
        LoginUtils.logOut(this);
    }

    private void showAbout() {
        final Activity activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Get the layout inflater
        final LayoutInflater inflater = activity.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.about, null);
        builder.setView(view);


        final TextView textView = (TextView) view.findViewById(R.id.textViewAbout);
        if (textView == null) {
            Log.error("showAbout: textView was null.");
            return;
        }

        //This voodoo makes the textviews' HTML links clickable:
        //See http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable/20647011#20647011
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        final String versionText =
                String.format(getString(R.string.about_version_text_format), BuildConfig.VERSION_NAME);

        //The about dialog's text is split into multiple strings to make translation easier,
        //so we need to concatenate them here.
        //Note that we use getText(), not getString(),
        //so we don't lose the <a href=""> links.
        //Likewise, we use SpannableStringBuilder instead of StringBuilder,
        //because we lose the links when using StringBuilder.
        final SpannableStringBuilder strBuilder = new SpannableStringBuilder();
        final String PARAGRAPH_BREAK = "\n\n";
        strBuilder.append(versionText);
        strBuilder.append(PARAGRAPH_BREAK);
        strBuilder.append(getText(R.string.about_text1));
        strBuilder.append(PARAGRAPH_BREAK);
        strBuilder.append(getText(R.string.about_text2));
        strBuilder.append(PARAGRAPH_BREAK);
        strBuilder.append(getText(R.string.about_text3));
        strBuilder.append(PARAGRAPH_BREAK);
        strBuilder.append(getText(R.string.about_text3b));
        strBuilder.append(PARAGRAPH_BREAK);
        strBuilder.append(getText(R.string.about_text4));
        strBuilder.append(PARAGRAPH_BREAK);
        strBuilder.append(getText(R.string.about_text5));
        strBuilder.append(PARAGRAPH_BREAK);
        strBuilder.append(getText(R.string.about_text6));

        textView.setText(strBuilder);

        /* We used to put the version text into a separate TextView,
           but when the about text in textView is too long,
           the scroll never reaches this far.
           It does work when we add it to first regular textView.
         */
        /*
        final TextView textViewVersion = (TextView) view.findViewById(R.id.textViewVersion);
        if (textViewVersion != null) {
            textViewVersion.setText(versionText);
        }
        */

        final AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.app_name);
        dialog.setIcon(R.mipmap.ic_launcher);

        dialog.show();
    }

    private void showSettings() {
        final Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }
}
