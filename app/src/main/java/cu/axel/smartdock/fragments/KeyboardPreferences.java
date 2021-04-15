package cu.axel.smartdock.fragments;
import android.preference.*;
import android.os.*;
import cu.axel.smartdock.*;


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
