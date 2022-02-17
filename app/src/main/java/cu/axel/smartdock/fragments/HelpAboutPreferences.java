package cu.axel.smartdock.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import cu.axel.smartdock.R;
import android.preference.Preference;
import android.content.Intent;
import android.net.Uri;
import android.content.ActivityNotFoundException;

public class HelpAboutPreferences extends PreferenceFragment 
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_help_about);
        
        findPreference("join_telegram").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    try{
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("tg://resolve?domain=smartdock358")));
                    }catch(ActivityNotFoundException e){}
                    return false;
                }
            });
    }

}
