package cu.axel.smartdock.fragments;
import android.preference.*;
import android.os.*;
import cu.axel.smartdock.*;

public class AppearancePreferences extends PreferenceFragment
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_appearance);
	}

}
