
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

public class RandomColors extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_RANDOM_COLOR_ONE = "color_one";
    private static final String PREF_RANDOM_COLOR_TWO = "color_two";
    private static final String PREF_RANDOM_COLOR_THREE = "color_three";
    private static final String PREF_RANDOM_COLOR_FOUR = "color_four";
    private static final String PREF_RANDOM_COLOR_FIVE = "color_five";
    private static final String PREF_RANDOM_COLOR_SIX = "color_six";

    private ColorPickerPreference mOne;
    private ColorPickerPreference mTwo;
    private ColorPickerPreference mThree;
    private ColorPickerPreference mFour;
    private ColorPickerPreference mFive;
    private ColorPickerPreference mSix;

    private ContentResolver mContentResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.random_colors);
        mContentResolver = getContentResolver();

        mOne = (ColorPickerPreference) findPreference(PREF_RANDOM_COLOR_ONE);
        mTwo = (ColorPickerPreference) findPreference(PREF_RANDOM_COLOR_TWO);
        mThree = (ColorPickerPreference) findPreference(PREF_RANDOM_COLOR_THREE);
        mFour = (ColorPickerPreference) findPreference(PREF_RANDOM_COLOR_FOUR);
        mFive = (ColorPickerPreference) findPreference(PREF_RANDOM_COLOR_FIVE);
        mSix = (ColorPickerPreference) findPreference(PREF_RANDOM_COLOR_SIX);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerListeners();
        updateSummaries();
    }

    private void registerListeners() {
        mOne.setOnPreferenceChangeListener(this);
        mTwo.setOnPreferenceChangeListener(this);
        mThree.setOnPreferenceChangeListener(this);
        mFour.setOnPreferenceChangeListener(this);
        mFive.setOnPreferenceChangeListener(this);
        mSix.setOnPreferenceChangeListener(this);
    }

    private void updateSummaries() {
        mOne.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                mContentResolver, Settings.System.RANDOM_COLOR_ONE,
                com.android.internal.R.color.holo_blue_dark)));
        mTwo.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                mContentResolver, Settings.System.RANDOM_COLOR_TWO,
                com.android.internal.R.color.holo_green_dark)));
        mThree.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                mContentResolver, Settings.System.RANDOM_COLOR_THREE,
                com.android.internal.R.color.holo_red_dark)));
        mFour.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                mContentResolver, Settings.System.RANDOM_COLOR_FOUR,
                com.android.internal.R.color.holo_orange_dark)));
        mFive.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                mContentResolver, Settings.System.RANDOM_COLOR_FIVE,
                com.android.internal.R.color.holo_purple)));
        mSix.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                mContentResolver, Settings.System.RANDOM_COLOR_SIX,
                com.android.internal.R.color.holo_blue_bright)));
    }

    private void setDefaultValues() {
        Settings.System.putInt(mContentResolver, Settings.System.RANDOM_COLOR_ONE,
                com.android.internal.R.color.holo_blue_dark);
        Settings.System.putInt(mContentResolver, Settings.System.RANDOM_COLOR_TWO,
                com.android.internal.R.color.holo_green_dark);
        Settings.System.putInt(mContentResolver, Settings.System.RANDOM_COLOR_THREE,
                com.android.internal.R.color.holo_red_dark);
        Settings.System.putInt(mContentResolver, Settings.System.RANDOM_COLOR_FOUR,
                com.android.internal.R.color.holo_orange_dark);
        Settings.System.putInt(mContentResolver, Settings.System.RANDOM_COLOR_FIVE,
                com.android.internal.R.color.holo_purple);
        Settings.System.putInt(mContentResolver, Settings.System.RANDOM_COLOR_SIX,
                com.android.internal.R.color.holo_blue_bright);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mOne) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentResolver,
                    Settings.System.RANDOM_COLOR_ONE, intHex);
            return true;
        } else if (preference == mTwo) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentResolver,
                    Settings.System.RANDOM_COLOR_TWO, intHex);
            return true;
        } else if (preference == mThree) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentResolver,
                    Settings.System.RANDOM_COLOR_THREE, intHex);
            return true;
        } else if (preference == mFour) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentResolver,
                    Settings.System.RANDOM_COLOR_FOUR, intHex);
            return true;
        } else if (preference == mFive) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentResolver,
                    Settings.System.RANDOM_COLOR_FIVE, intHex);
            return true;
        } else if (preference == mSix) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentResolver,
                    Settings.System.RANDOM_COLOR_SIX, intHex);
            return true;
        }
        return false;
    }
}
