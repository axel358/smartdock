package cu.axel.smartdock.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import cu.axel.smartdock.R;
import android.preference.Preference;
import android.content.Intent;
import android.app.Activity;
import android.net.Uri;
import android.app.AlertDialog;
import android.view.View;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.SeekBar;
import android.text.TextWatcher;
import android.text.Editable;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.content.DialogInterface;
import cu.axel.smartdock.utils.Utils;
import android.widget.TextView;

public class AppearancePreferences extends PreferenceFragment {
    private final int OPEN_REQUEST_CODE=4;
    private Preference menuIconPref, mainColorPref, secondaryColorPref;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_appearance);

        menuIconPref = findPreference("menu_icon_uri");
        menuIconPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), OPEN_REQUEST_CODE);

                    return false;
                }
            });
        findPreference("restore_menu_icon").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    menuIconPref.getSharedPreferences().edit().putString(menuIconPref.getKey(), "default").commit();
                    return false;
                }
            });

        mainColorPref = findPreference("theme_main_color");
        mainColorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    showColorPickerDialog(getActivity(), "main");
                    return false;
                }
            });

        secondaryColorPref = findPreference("theme_secondary_color");
        secondaryColorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

                @Override
                public boolean onPreferenceClick(Preference p1) {
                    showColorPickerDialog(getActivity(), "secondary");
                    return false;
                }
            });
            
        findPreference("theme").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference p1, Object p2) {
                    mainColorPref.setEnabled(p2.toString().equals("custom"));
                    secondaryColorPref.setEnabled(p2.toString().equals("custom"));
                    return true;
                }
            });
            
        mainColorPref.setEnabled(mainColorPref.getSharedPreferences().getString("theme","dark").equals("custom"));
        secondaryColorPref.setEnabled(mainColorPref.getSharedPreferences().getString("theme","dark").equals("custom"));

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
    public void showColorPickerDialog(Context context, final String type) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        dialog.setTitle("Choose color");
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        final View colorPreview = view.findViewById(R.id.color_preview);
        final EditText colorHexEt = view.findViewById(R.id.color_hex_et);
        final SeekBar alphaSb = view.findViewById(R.id.color_alpha_sb);
        final TextView alphaTv = view.findViewById(R.id.color_alpha_tv);
        alphaSb.setMax(255);
        colorHexEt.addTextChangedListener(new TextWatcher(){

                @Override
                public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {}
                @Override
                public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {}

                @Override
                public void afterTextChanged(Editable p1) {
                    String color = p1.toString();
                    if (color.length() == 7 && Utils.isValidColor(color)) 
                        colorPreview.getBackground().setColorFilter(Color.parseColor(p1.toString()), PorterDuff.Mode.SRC_ATOP);
                    else
                        colorHexEt.setError("Invalid color");

                }
            });
        alphaSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

                @Override
                public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
                    alphaTv.setText(p2 + "/255");
                    colorPreview.getBackground().setAlpha(p2);
                }

                @Override
                public void onStartTrackingTouch(SeekBar p1) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar p1) {
                }
            });
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    String color = colorHexEt.getText().toString();
                    if (Utils.isValidColor(color)) {
                        if (type.equals("main")) {
                            mainColorPref.getSharedPreferences().edit().putString(mainColorPref.getKey(), color).commit();
                            mainColorPref.getSharedPreferences().edit().putInt("theme_main_alpha", alphaSb.getProgress()).commit();
                        } else {
                            secondaryColorPref.getSharedPreferences().edit().putString(secondaryColorPref.getKey(), color).commit();
                            secondaryColorPref.getSharedPreferences().edit().putInt("theme_secondary_alpha", alphaSb.getProgress()).commit();
                        }
                    }


                }
            });
        dialog.setView(view);

        if (type.equals("main")) {
            colorHexEt.setText(mainColorPref.getSharedPreferences().getString(mainColorPref.getKey(), "#212121"));
            alphaSb.setProgress(mainColorPref.getSharedPreferences().getInt("theme_main_alpha", 255));
        } else {
            colorHexEt.setText(secondaryColorPref.getSharedPreferences().getString(secondaryColorPref.getKey(), "#292929"));
            alphaSb.setProgress(mainColorPref.getSharedPreferences().getInt("theme_secondary_alpha", 255));

        }

        dialog.show();
    }
}
