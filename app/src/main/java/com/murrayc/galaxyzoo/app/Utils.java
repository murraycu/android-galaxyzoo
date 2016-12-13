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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.test.mock.MockContentResolver;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.provider.Item;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public final class Utils {

    /**
     * For use, for instance with String.toLowerCase()
     */
    public static final String STRING_LANGUAGE = "en";

    /**
     * For use when parsing and building raw binary data:
     */
    public static final String STRING_ENCODING = "UTF-8";
    private static final String ASSET_PATH_DECISION_TREE_DIR = "decision_tree/";
    private static final String JSON_FILE_EXTENSION = ".json";

    /**
     * Ideally you would use LoginUtils.getUseWifiOnly() instead of the copy that is in
     * the SharedPreferences, but this is useful when you don't want to call from the main thread.
     *
     * @param context
     * @return
     */
    public static boolean getUseWifiOnlyFromSharedPrefs(final Context context) {
        //We use the SharedPreferences rather than the copy of our prefs in the Account,
        //to avoid using the Account in the main thread.
        final SharedPreferences prefs = getPreferences(context);

        if (prefs == null) {
            //This should never happen, but there was at least one crash with a backtrace
            //suggesting that it can happen:
            //See https://github.com/murraycu/android-galaxyzoo/issues/16
            Log.error("getShowDiscussQuestionFromSharedPrefs(): prefs is null.");
            return true; //A suitable dfault.
        }

        final String key =  context.getString(R.string.pref_key_wifi_only);
        return prefs.getBoolean(key, false /* default */);
    }

    /**
     * Ideally you would use LoginUtils.getShowDiscussQuestion() instead of the copy that is in
     * the SharedPreferences, but this is useful when you don't want to call from the main thread.
     *
     * @param context
     * @return
     */
    public static boolean getShowDiscussQuestionFromSharedPrefs(final Context context) {
        //We use the SharedPreferences rather than the copy of our prefs in the Account,
        //to avoid using the Account in the main thread.
        final SharedPreferences prefs = getPreferences(context);

        if (prefs == null) {
            //This should never happen, but there was at least one crash with a backtrace
            //suggesting that it can happen:
            //See https://github.com/murraycu/android-galaxyzoo/issues/16
            Log.error("getShowDiscussQuestionFromSharedPrefs(): prefs is null.");
            return true; //A suitable dfault.
        }

        final String key =  context.getString(R.string.pref_key_show_discuss_question);
        return prefs.getBoolean(key, true /* default */);
    }

    static SharedPreferences getPreferences(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Nullable
    public static File getExternalCacheDir(final Context context) {
        try {
            final File result = context.getExternalCacheDir();
            if (result == null) {
                Log.error("getExternalCacheDir(): null result without an exception from Context.getExternalCacheDir()");

                final String state = android.os.Environment.getExternalStorageState();
                Log.error("  getExternalCacheDir(): getExternalStorageState() returned: " + state);
            }
            return result;
        }  catch (final SecurityException e) {
            Log.error("getExternalCacheDir(): SecurityException from Context.getExternalCacheDir()", e);
            return null;
        } catch (final UnsupportedOperationException e) {
            //This happens while running under ProviderTestCase2.
            //so we just catch it and provide a useful value,
            //so at least the other functionality can be tested.
            //TODO: Find a way to let it succeed.
            if (context.getContentResolver() instanceof MockContentResolver) {
                return new File("testExternalCacheDirForAndroidGalaxyZooWithMockContentResolver");
            }

            Log.error("getExternalCacheDir(): Unsupported operation from Context.getExternalCacheDir()", e);
            return null;
        }
    }

    public static String getDecisionTreeFilepath(final String filename) {
        return ASSET_PATH_DECISION_TREE_DIR + filename;
    }

    public static String getTranslationFilePath(final String language, final String countryCode) {
        if (!TextUtils.isEmpty(countryCode)) {
            return ASSET_PATH_DECISION_TREE_DIR + language + "_" + countryCode + JSON_FILE_EXTENSION;
        } else {
            return ASSET_PATH_DECISION_TREE_DIR + language + JSON_FILE_EXTENSION;
        }
    }

    @NonNull
    public static String getTalkUri(final String zooniverseId) {
        return Config.TALK_URI + zooniverseId;
    }

    public static final class NetworkConnected {
        public final boolean connected;
        public final boolean notConnectedBecauseNotOnWifi;

        NetworkConnected(final boolean connected, final boolean notConnectedBecauseNotOnWifi) {
            super();
            this.connected = connected;
            this.notConnectedBecauseNotOnWifi = notConnectedBecauseNotOnWifi;
        }
    }

    public static NetworkConnected getNetworkIsConnected(final Context context, final boolean wifiOnly) {
        boolean connected = false;
        final ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) {
            //This happens during our test case, probably because the MockContext doesn't support
            //this, so let's ignore it.
            return new NetworkConnected(false, false);
        }

        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            connected = true;
        }

        if (!connected) {
            return new NetworkConnected(false, false);
        }

        //Consider us not connected if we should use only wi-fi but don't have wi-fi:
        if (wifiOnly && (networkInfo.getType() != ConnectivityManager.TYPE_WIFI)) {
            return new NetworkConnected(false, true);
        }

        return new NetworkConnected(true, false);
    }

    @Nullable
    public static InputStream openAsset(final Context context, final String filename) {
        try {
            return context.getAssets().open(filename);
        } catch (final IOException e) {
            //Don't log this because we expect the file to not exist sometimes,
            //and the caller will just check for a null result to know that.
            return null;
        }
    }

    public static void abandonItem(final Context context, final String itemId) {
        Log.info("abandonItem(): Abandoning item with itemId=" + itemId);

        final Uri itemUri = ContentUris.withAppendedId(
                Item.ITEMS_URI, Integer.parseInt(itemId));

        final ContentResolver resolver = context.getContentResolver();
        final int affected = resolver.delete(itemUri, null, null);
        if (affected != 1) {
            Log.error("abandonItem(): Unexpected number of rows affected: " + affected);
        }
    }

    public static Uri getItemUri(final String itemId) {
        final Uri.Builder uriBuilder = Item.ITEMS_URI.buildUpon();
        uriBuilder.appendPath(itemId);
        return uriBuilder.build();
    }

    static void initDefaultPrefs(final Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    }


}
