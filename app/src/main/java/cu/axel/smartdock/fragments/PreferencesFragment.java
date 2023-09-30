package cu.axel.smartdock.fragments;

import android.os.Bundle;

import cu.axel.smartdock.R;

import androidx.preference.PreferenceFragmentCompat;

public class PreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle arg0, String arg1) {
        setPreferencesFromResource(R.xml.preferences_main, arg1);
    }

}