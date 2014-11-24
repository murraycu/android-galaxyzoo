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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.murrayc.galaxyzoo.app.provider.Item;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 *
 */
public class Utils {

    public static final String STRING_ENCODING = "UTF-8";

    public static boolean getUseWifiOnly(final Context context) {
        return getBooleanPref(context, R.string.pref_key_wifi_only);
    }

    public static File getExternalCacheDir(final Context context) {
        try {
            return context.getExternalCacheDir();
        }  catch (final UnsupportedOperationException e) {
            //This happens while running under ProviderTestCase2.
            //so we just catch it and provide a useful value,
            //so at least the other functionality can be tested.
            //TODO: Find a way to let it succeed.
            Log.error("getExternalCacheDir(): Unsupported operation from Context.getExternalCacheDir()", e);
            return null;
        }
    }

    public final static class NetworkConnected {
        public final boolean connected;
        public final boolean notConnectedBecauseNotOnWifi;

        NetworkConnected(boolean connected, boolean notConnectedBecauseNotOnWifi) {
            this.connected = connected;
            this.notConnectedBecauseNotOnWifi = notConnectedBecauseNotOnWifi;
        }
    }

    public static NetworkConnected getNetworkIsConnected(final Context context, final boolean wifiOnly) {
        boolean connected = false;
        ConnectivityManager connMgr = (ConnectivityManager)
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

    private static boolean getBooleanPref(final Context context, final int prefKeyResId) {
        final String value = getStringPref(context, prefKeyResId);
        if (value == null) {
            return false;
        }

        return Boolean.parseBoolean(value);
    }

    public static int getIntPref(final Context context, final int prefKeyResId) {
        final String value = getStringPref(context, prefKeyResId);
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    private static String getStringPref(Context context, int prefKeyResId) {
        final AccountManager mgr = AccountManager.get(context);
        final Account account = getAccount(mgr);
        if (account == null) {
            return null;
        }

        return mgr.getUserData(account, context.getString(prefKeyResId));
    }

    private static Account getAccount(final AccountManager mgr) {
        final Account[] accts = mgr.getAccountsByType(LoginUtils.ACCOUNT_TYPE);
        if((accts == null) || (accts.length < 1)) {
            //Log.error("getAccountLoginDetails(): getAccountsByType() returned no account.");
            return null;
        }

        return accts[0];
    }

    static InputStream openAsset(final Context context, final String filename) {
        try {
            return context.getAssets().open(filename);
        } catch (final IOException e) {
            //Don't log this because we expect the file to not exist sometimes,
            //and the caller will just check for a null result to know that.
            return null;
        }
    }

    public static void abandonItem(final Context context, final String itemId) {
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

    static void copyPrefToAccount(final Context context, final String key, final String value) {
        //Copy the preference to the Account:
        final AccountManager mgr = AccountManager.get(context);
        final Account[] accts = mgr.getAccountsByType(LoginUtils.ACCOUNT_TYPE);
        if((accts == null) || (accts.length < 1)) {
            //Log.error("getAccountLoginDetails(): getAccountsByType() returned no account.");
            return;
        }

        final Account account = accts[0];
        if (account == null) {
            return;
        }

        copyPrefToAccount(mgr, account, key, value);
    }

    private static void copyPrefToAccount(final AccountManager mgr, final Account account, final String key, final String value) {
        mgr.setUserData(account, key, value);
    }

    static void copyPrefsToAccount(final Context context, final AccountManager accountManager, final Account account) {
        //Copy the preferences into the account.
        //See also SettingsFragment.onSharedPreferenceChanged()
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final Map<String, ?> keys = prefs.getAll();
        for(final Map.Entry<String, ?> entry : keys.entrySet()) {
            final Object value =  entry.getValue();
            if ((value == null) || !(value instanceof String)) {
                continue;
            }

            copyPrefToAccount(accountManager, account, entry.getKey(), (String) value);
        }
    }

    static void initDefaultPrefs(final Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    }

 
}
