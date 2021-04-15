package cu.axel.smartdock.fragments;
import android.preference.*;
import android.os.*;
import cu.axel.smartdock.*;


public class HotCornersPreferences extends PreferenceFragment
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_hot_corners);
	}

}
