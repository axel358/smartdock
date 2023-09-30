package cu.axel.smartdock.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import cu.axel.smartdock.R;

public class DefaultAppsPreferences extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle arg0, String arg1) {
        setPreferencesFromResource(R.xml.preferences_default_apps, arg1);
    }
}
