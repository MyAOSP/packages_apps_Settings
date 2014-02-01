package com.android.settings.baked;

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.ColorPickerPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NavBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String NAVBAR_BUTTON_TINT = "navbar_button_tint";

    private ColorPickerPreference mNavbarButtonTint;

    private ContentResolver mContentResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.navbar_settings);
        mContentResolver = getContentResolver();

        mNavbarButtonTint = (ColorPickerPreference) findPreference(NAVBAR_BUTTON_TINT);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerListeners();
        updateSummaries();
    }

    private void registerListeners() {
        mNavbarButtonTint.setOnPreferenceChangeListener(this);
    }

    private void updateSummaries() {
        mNavbarButtonTint.setSummary(ColorPickerPreference.convertToARGB(
                Settings.System.getInt(mContentResolver, Settings.System.NAVIGATION_BAR_TINT,
                com.android.internal.R.color.white)));
    }

    private void setDefaultValues() {
        Settings.System.putInt(mContentResolver, Settings.System.NAVIGATION_BAR_TINT,
                com.android.internal.R.color.white);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNavbarButtonTint) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentResolver,
                    Settings.System.NAVIGATION_BAR_TINT, intHex);
            return true;
        }
        return false;
    }
}
