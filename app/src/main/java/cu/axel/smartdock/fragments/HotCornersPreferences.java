package cu.axel.smartdock.fragments;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import cu.axel.smartdock.R;

public class HotCornersPreferences extends PreferenceFragmentCompat
{

	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		setPreferencesFromResource(R.xml.preferences_hot_corners, arg1);
	}

}
