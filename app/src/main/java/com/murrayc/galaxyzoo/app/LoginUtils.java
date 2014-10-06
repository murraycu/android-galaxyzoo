package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by murrayc on 10/6/14.
 */
public class LoginUtils {
    //TODO: Ask the provider instead of using this hack which uses too much internal knowledge.
    public static boolean getLoggedIn(final Context context) {
        final LoginDetails loginDetails = getPrefsAuth(context);
        return (loginDetails != null) && !(TextUtils.isEmpty(loginDetails.authApiKey));
    }

    public static void saveAuthToPreferences(final Context context, final String name, final String apiKey) {
        final SharedPreferences prefs = Utils.getPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_key_auth_name), name);
        editor.putString(context.getString(R.string.pref_key_auth_api_key), apiKey);
        editor.apply();
    }

    public static LoginDetails getPrefsAuth(final Context context) {
        final LoginDetails result = new LoginDetails();
        final SharedPreferences prefs = Utils.getPreferences(context);
        result.authName = prefs.getString(context.getString(R.string.pref_key_auth_name), null);
        result.authApiKey = prefs.getString(context.getString(R.string.pref_key_auth_api_key), null);
        return result;
    }

    public static class LoginDetails {
        public String authName;
        public String authApiKey;
    }
}
