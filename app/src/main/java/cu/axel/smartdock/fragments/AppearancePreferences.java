package cu.axel.smartdock.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import cu.axel.smartdock.R;
import android.preference.Preference;
import android.content.Intent;
import android.app.Activity;
import android.net.Uri;

public class AppearancePreferences extends PreferenceFragment {
    private final int OPEN_REQUEST_CODE=4;
    private Preference menuIconPref;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_appearance);

        menuIconPref = findPreference("pref_menu_icon_uri");
        menuIconPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), OPEN_REQUEST_CODE);

                    return false;
                }
            });
        findPreference("pref_menu_icon_reset").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    menuIconPref.getSharedPreferences().edit().putString(menuIconPref.getKey(), "default").commit();
                    return false;
                }
            }); 

	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_REQUEST_CODE) {
                Uri openUri = data.getData();
                getActivity().getContentResolver().takePersistableUriPermission(openUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                menuIconPref.getSharedPreferences().edit().putString(menuIconPref.getKey(), openUri.toString()).commit();
            }
        }

    }
}
