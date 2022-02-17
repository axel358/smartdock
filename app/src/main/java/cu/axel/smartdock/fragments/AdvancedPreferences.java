package cu.axel.smartdock.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import cu.axel.smartdock.R;
import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.Utils;

public class AdvancedPreferences extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_advanced);
        Preference editAutostart = findPreference("edit_autostart");
        editAutostart.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    showEditAutostartDialog(getActivity());
                    return false;
                }
            });
        Preference displayS = findPreference("custom_display_size");
        displayS.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference p1, Object p2) {
                    String n=p2.toString();
                    if (n.isEmpty())
                        DeviceUtils.setDisplaySize(0);
                    else
                        DeviceUtils.setDisplaySize(Integer.parseInt(n));

                    showRebootDialog(getActivity());
                    return true;
                }
            });
        Preference softReboot = findPreference("soft_reboot");
        softReboot.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    DeviceUtils.sotfReboot();
                    return false;
                }
            });
    }

    public void showEditAutostartDialog(final Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Edit autostart");
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_autostart, null);
        final EditText contentEt = view.findViewById(R.id.edit_autostart_et);
        contentEt.setText(Utils.readAutostart(context));
        dialog.setPositiveButton("Save", new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    String content=contentEt.getText().toString();
                    if (!content.isEmpty()) {
                        Utils.saveAutoStart(context, content);
                    }
                }
            });
        dialog.setNegativeButton("Cancel", null);
        dialog.setView(view);
        dialog.show();
    }

    public void showRebootDialog(Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.DialogTheme);
        dialog.setTitle("Reboot required");
        dialog.setMessage("A reboot is required for changes to take effect. Do you want to reboot now?");
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    DeviceUtils.reboot();
                }
            });
        dialog.setNegativeButton("Cancel", null);
        dialog.show();
    }
}
