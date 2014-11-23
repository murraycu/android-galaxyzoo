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
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * Created by murrayc on 5/21/14.
 */
class UiUtils {

    //See http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
    static class ShowImageFromContentProviderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<Context> contextReference;

        protected String getUri() {
            return strUri;
        }

        private String strUri = null;

        public ShowImageFromContentProviderTask(final ImageView imageView, final Context fragment) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);

            // Use a WeakReference to ensure the ImageView can be garbage collected
            contextReference = new WeakReference<>(fragment);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            strUri = params[0];

            if (contextReference != null) {
                final Context context = contextReference.get();
                if (context != null) {
                    return UiUtils.getBitmapFromContentUri(context, strUri);
                }
            }

            return null;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        // This avoids calling the ImageView methods in the non-main thread.
        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            //Callers should maybe use a derived class that overrides this
            //to call abandonItem() when the bitmap is null.

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {

                        imageView.setImageResource(android.R.color.transparent);
                    }
                }
            }
        }
    }

    /** Get a URI for an image in the ItemsContentProvider.
     *
     * Don't call this from a main (UI) thread.
     *
     * @param context
     * @param imageUriStr
     * @return
     */
    static Bitmap getBitmapFromContentUri(final Context context, final String imageUriStr) {
        if (imageUriStr == null) {
            Log.error("getBitmapFromContentUri(): imageUriStr is null.");
            return null;
        }

        final ContentResolver contentResolver = context.getContentResolver();

        Bitmap bMap = null;
        final Uri uri = Uri.parse(imageUriStr);

        InputStream stream = null;
        try {
            stream = contentResolver.openInputStream(uri);
            bMap = BitmapFactory.decodeStream(stream);
        } catch (final IOException e) {
            Log.error("getBitmapFromContentUri(): BitmapFactory.decodeStream() failed.", e);
            return null;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (final IOException e) {
                    Log.error("getBitmapFromContentUri(): Exception while closing stream.", e);
                }
            }
        }

        return bMap;
    }

    private static void warnAboutNoNetworkConnection(final Activity activity) {
        final Toast toast = Toast.makeText(activity, activity.getString(R.string.error_no_network), Toast.LENGTH_LONG);
        toast.show();
    }

    private static void warnAboutNoWifiNetworkConnection(final Activity activity) {
        final Toast toast = Toast.makeText(activity, activity.getString(R.string.error_no_wifi_network), Toast.LENGTH_LONG);
        toast.show();
    }

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

        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriTalk));
            context.startActivity(intent);
        } catch (final ActivityNotFoundException e) {
            Log.error("Could not open the discussion URI.", e);
        }
    }

    static void showLoggedInToast(final Context context) {
        final Toast toast = Toast.makeText(context, context.getString(R.string.message_logged_in), Toast.LENGTH_LONG);
        toast.show();
    }

    static int getPxForDpResource(final Context context, int resourceId) {
        final Resources r = context.getResources();
        return r.getDimensionPixelSize(resourceId);
    }

    static void showToolbar(final ActionBarActivity activity) {
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

    /**
     * Use this instead of the one with the wifiOnly parameter,
     * when we are sure that the request should work even on wi-fi even if
     * wifi-only is set.
     *
     * @param activity
     * @return
     */
    static boolean warnAboutMissingNetwork(final Activity activity) {
        return warnAboutMissingNetwork(activity, Utils.getUseWifiOnly(activity));
    }

    /**
     * Return true if a suitable warning was shown, if a possible cause was found.
     *
     * @param activity
     * @return
     */
    static boolean warnAboutMissingNetwork(final Activity activity, boolean wifiOnly) {
        //Check for this possible cause.
        // TODO: Avoid copy/pasting with QuestionFragment
        // TODO: Is there any simpler way to just catch the
        // ItemsContentProvider.NoNetworkConnection exception in the CursorLoader?
        final Utils.NetworkConnected networkConnected = Utils.getNetworkIsConnected(activity,
                wifiOnly);
        if (networkConnected.notConnectedBecauseNotOnWifi) {
            warnAboutNoWifiNetworkConnection(activity);
            return true;
        } else if (!networkConnected.connected) {
            warnAboutNoNetworkConnection(activity);
            return true;
        }

        return false;
    }
}
