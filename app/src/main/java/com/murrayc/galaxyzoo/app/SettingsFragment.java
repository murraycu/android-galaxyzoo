/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * A fragment showing the preferences.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_PREF_CACHE_SIZE = "cache_size";
    private static final String KEY_PREF_KEEP_COUNT = "keep_count";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Change the summary when the value changes,
        //as suggested by the Android documentation,
        //though that documentation doesn't show how to actually do it:
        //https://code.google.com/p/android/issues/detail?id=76538&thanks=76538&ts=1411464975
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        //Set the initial summaries:
        Preference pref = findPreference(KEY_PREF_CACHE_SIZE);
        showUserDescriptionAsSummary(pref);
        pref = findPreference(KEY_PREF_KEEP_COUNT);
        showUserDescriptionAsSummary(pref);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                          final String key) {
        final Preference connectionPref = findPreference(key);
        if (connectionPref instanceof ListPreference) {
            showUserDescriptionAsSummary(connectionPref);
        }

        //Copy the preference to the Account:
        //This is an awful hack. Hopefully there is some other way to use preferences per-account.
        //If not, maybe we need to reimplement this fragment without using PreferencesFragment.
        String value = null;
        if (connectionPref instanceof ListPreference) {
            final ListPreference listPref = (ListPreference) connectionPref;
            value = listPref.getValue();
        } else if (connectionPref instanceof CheckBoxPreference) {
            final CheckBoxPreference checkboxPref = (CheckBoxPreference) connectionPref;
            value = checkboxPref.isChecked() ? "true" : ""; //See Boolean.parseBoolean().
        }

        Utils.copyPrefToAccount(getActivity(), key, value);
    }

    private void showUserDescriptionAsSummary(final Preference preference) {
        if (!(preference instanceof ListPreference)) {
            Log.error("showUserDescriptionAsSummary(): preference is not a ListPreference.");
            return;
        }

        final ListPreference listPref = (ListPreference) preference;
        preference.setSummary(listPref.getEntry());
    }
}
