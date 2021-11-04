package cu.axel.smartdock.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import cu.axel.smartdock.R;
import android.preference.PreferenceManager;

public class SoundsPreferences extends PreferenceFragment {

    private Preference startSoundPref,usbSoundPref,notifSoundPref,chargeSoundPref,chargeCompleteSoundPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_sounds);
        startSoundPref = findPreference("pref_startup_sound");
        startSoundPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    openSound(0);
                    return false;
                }
            });
        usbSoundPref = findPreference("pref_usb_sound");
        usbSoundPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    openSound(1);
                    return false;
                }
            });
        notifSoundPref = findPreference("pref_notification_sound");
        notifSoundPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    openSound(2);
                    return false;
                }
            });
        chargeSoundPref = findPreference("pref_charge_sound");
        chargeSoundPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    openSound(3);
                    return false;
                }
            });
        chargeCompleteSoundPref = findPreference("pref_charge_complete_sound");
        chargeCompleteSoundPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    openSound(4);
                    return false;
                }
            });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri openUri = data.getData();
            getActivity().getContentResolver().takePersistableUriPermission(openUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (requestCode == 0) {
                saveSound(startSoundPref.getKey(), openUri.toString());
            } else if (requestCode == 1) {
                saveSound(usbSoundPref.getKey(), openUri.toString());
            } else if (requestCode == 2) {
                saveSound(notifSoundPref.getKey(), openUri.toString());
            } else if (requestCode == 3) {
                saveSound(chargeSoundPref.getKey(), openUri.toString());
            } else if (requestCode == 4) {
                saveSound(chargeCompleteSoundPref.getKey(), openUri.toString());
            }
        }

    }
    public void openSound(int code) {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*"), code);
    }
    public void saveSound(String key, String uri) {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(key, uri).commit();
    }
}
