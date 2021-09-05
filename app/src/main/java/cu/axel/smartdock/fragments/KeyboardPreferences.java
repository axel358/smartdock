package cu.axel.smartdock.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import cu.axel.smartdock.R;

public class KeyboardPreferences extends PreferenceFragment 
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_keyboard);
	}
	
}
