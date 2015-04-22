/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
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
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.murrayc.galaxyzoo.app.provider.HttpUtils;

/**
 * Created by murrayc on 5/21/14.
 */
final class UiUtils {

    /*
    static void warnAboutNoItemsToDo(final Activity activity) {
        final Toast toast = Toast.makeText(activity, activity.getString(R.string.error_no_subjects), Toast.LENGTH_LONG);
        toast.show();
    }
    */

    static void openDiscussionPage(final Context context, final String zooniverseId) {
        //Todo: Find a way to use Uri.Builder with a URI with # in it.
        //Using Uri.parse() (with Uri.Builder) removes the #.
        //Using Uri.Builder() leads to an ActivityNotFoundException.
        //final String encodedHash = Uri.encode("#"); //This just puts %23 in the URL instead of #.
        //final Uri.Builder uriBuilder = new Uri.Builder();
        //uriBuilder.path("http://talk.galaxyzoo.org/#/subjects/");
        //uriBuilder.appendPath(getZooniverseId());
        final String uriTalk = com.murrayc.galaxyzoo.app.Config.TALK_URI + zooniverseId;

        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriTalk));
        context.startActivity(intent);
    }

    static void showLoggedInToast(final Context context) {
        final Toast toast = Toast.makeText(context, context.getString(R.string.message_logged_in), Toast.LENGTH_LONG);
        toast.show();
    }

    static int getPxForDpResource(final Context context, final int resourceId) {
        final Resources r = context.getResources();
        return r.getDimensionPixelSize(resourceId);
    }

    static void showToolbar(final AppCompatActivity activity) {
        //The layout XML should include our toolbar.xml,
        //which we use instead of an ActionBar,
        //See also our use of <item name="windowActionBar">false</item> in styles.xml.
        final Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            activity.setSupportActionBar(toolbar);

            //Remove the title text from the app bar (toolbar/actionbar)
            //because we instead use an icon that shows the title.
            final ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false);
            }

            //TODO: Why can't we specify this via android:logo in the XML:
            toolbar.setLogo(R.drawable.ic_toolbar_icon);
        }
    }

    public static Bundle getTransitionOptionsBundle(final Activity activity, final View sharedElementView) {
        final ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElementView, activity.getString(R.string.transition_subject_image));
        return options.toBundle();
    }

    public static void warnAboutNoNetworkConnection(final Activity activity, final HttpUtils.NoNetworkException ex) {
        //This null check would be correct, but seems harsh because this code will only run
        //in response to an exception, so we cannot expect to test it completely.
        /*
        if (ex == null) {
            throw new IllegalArgumentException("ex is null.");
        }
        */

        if ((ex != null) && ex.getWifiOnly()) {
            warnAboutNoWifiNetworkConnection(activity);
        } else {
            warnAboutNoNetworkConnectionAtAll(activity);
        }
    }

    private static void warnAboutNoNetworkConnection(final Activity activity, final boolean notConnectedBecauseNotOnWifi) {
        if (notConnectedBecauseNotOnWifi) {
            warnAboutNoWifiNetworkConnection(activity);
        } else {
            warnAboutNoNetworkConnectionAtAll(activity);
        }
    }

    private static void warnAboutNoNetworkConnectionAtAll(final Activity activity) {
        final Toast toast = Toast.makeText(activity, activity.getString(R.string.error_no_network), Toast.LENGTH_LONG);
        toast.show();
    }

    private static void warnAboutNoWifiNetworkConnection(final Activity activity) {
        final Toast toast = Toast.makeText(activity, activity.getString(R.string.error_no_wifi_network), Toast.LENGTH_LONG);
        toast.show();
    }


    /**
     * Use this instead of the one with the wifiOnly parameter,
     * when we are sure that the request should work even on wi-fi even if
     * wifi-only is set.
     *
     * @param activity
     * @return
     */
    static boolean warnAboutMissingNetwork(final Activity activity) {
        return warnAboutMissingNetwork(activity, Utils.getUseWifiOnlyFromSharedPrefs(activity));
    }

    /**
     * Return true if a suitable warning was shown, if a possible cause was found.
     *
     * @param activity
     * @return
     */
    static boolean warnAboutMissingNetwork(final Activity activity, final boolean wifiOnly) {
        //Check for this possible cause.
        // TODO: Avoid copy/pasting with QuestionFragment
        // TODO: Is there any simpler way to just catch the
        // ItemsContentProvider.NoNetworkConnection exception in the CursorLoader?
        final Utils.NetworkConnected networkConnected = Utils.getNetworkIsConnected(activity,
                wifiOnly);
        if (!networkConnected.connected) {
            warnAboutNoNetworkConnection(activity, networkConnected.notConnectedBecauseNotOnWifi);
            return true;
        }

        return false;
    }
}
