package cu.axel.smartdock.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import cu.axel.smartdock.R;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.ColorUtils;

public class AppearancePreferences extends PreferenceFragmentCompat {
    private Preference mainColorPref;

    @Override
    public void onCreatePreferences(Bundle arg0, String arg1) {
        setPreferencesFromResource(R.xml.preferences_appearance, arg1);
        mainColorPref = findPreference("theme_main_color");
        mainColorPref.setOnPreferenceClickListener((Preference p1) -> {
            showColorPickerDialog(getActivity());
            return false;
        });

        findPreference("theme").setOnPreferenceChangeListener((Preference p1, Object p2) -> {
            mainColorPref.setVisible(p2.toString().equals("custom"));
            return true;
        });

        mainColorPref.setVisible(mainColorPref.getSharedPreferences().getString("theme", "dark").equals("custom"));
        findPreference("tint_indicators")
                .setVisible(AppUtils.isSystemApp(getActivity(), getActivity().getPackageName()));
    }

    public void showColorPickerDialog(Context context) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getActivity());
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        final View colorPreview = view.findViewById(R.id.color_preview);
        final TextInputEditText colorHexEt = view.findViewById(R.id.color_hex_et);
        final Slider alphaSb = view.findViewById(R.id.color_alpha_sb);
        final Slider redSb = view.findViewById(R.id.color_red_sb);
        final Slider greenSb = view.findViewById(R.id.color_green_sb);
        final Slider blueSb = view.findViewById(R.id.color_blue_sb);
        final ViewSwitcher viewSwitcher = view.findViewById(R.id.colors_view_switcher);
        final MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.colors_btn_toggle);

        colorHexEt.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void afterTextChanged(Editable p1) {
                String hexColor = p1.toString();
                int color;

                if (hexColor.length() == 7 && (color = ColorUtils.toColor(hexColor)) != -1) {
                    colorPreview.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    redSb.setValue(Color.red(color));
                    greenSb.setValue(Color.green(color));
                    blueSb.setValue(Color.blue(color));
                } else
                    colorHexEt.setError(getString(R.string.invalid_color));
            }
        });

        alphaSb.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider arg0, float arg1, boolean arg2) {
                colorPreview.getBackground().setAlpha((int) arg1);
            }

        });

        Slider.OnChangeListener onChangeListener = new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                if (fromUser)
                    colorHexEt.setText(ColorUtils.toHexColor(
                            Color.rgb((int) redSb.getValue(), (int) greenSb.getValue(), (int) blueSb.getValue())));
            }

        };
        redSb.addOnChangeListener(onChangeListener);
        greenSb.addOnChangeListener(onChangeListener);
        blueSb.addOnChangeListener(onChangeListener);
        dialog.setNegativeButton(R.string.cancel, null);
        dialog.setPositiveButton(R.string.ok, (DialogInterface p1, int p2) -> {
            String color = colorHexEt.getText().toString();
            if (ColorUtils.toColor(color) != -1) {
                mainColorPref.getSharedPreferences().edit().putString(mainColorPref.getKey(), color).commit();
                mainColorPref.getSharedPreferences().edit().putInt("theme_main_alpha", (int) alphaSb.getValue())
                        .commit();
            }
        });

        alphaSb.setValue(mainColorPref.getSharedPreferences().getInt("theme_main_alpha", 255));
        String hexColor = mainColorPref.getSharedPreferences().getString(mainColorPref.getKey(), "#212121");
        colorHexEt.setText(hexColor);

        GridView presetsGv = view.findViewById(R.id.presets_gv);
        presetsGv.setAdapter(
                new HexColorAdapter(context, context.getResources().getStringArray(R.array.default_color_values)));

        presetsGv.setOnItemClickListener((AdapterView<?> p1, View p2, int p3, long p4) -> {
            colorHexEt.setText(p1.getItemAtPosition(p3).toString());
            toggleGroup.check(R.id.custom_button);
            viewSwitcher.showNext();
        });

        view.findViewById(R.id.custom_button).setOnClickListener(v -> viewSwitcher.showPrevious());
        view.findViewById(R.id.presets_button).setOnClickListener(v -> viewSwitcher.showNext());

        dialog.setView(view);
        dialog.show();

    }

    class HexColorAdapter extends ArrayAdapter<String> {
        private Context context;

        public HexColorAdapter(Context context, String[] colors) {
            super(context, R.layout.color_entry, colors);
            this.context = context;
        }

        public HexColorAdapter(Context context, ArrayList<String> colors) {
            super(context, R.layout.color_entry, colors);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.color_entry, null);

            ((ImageView) convertView.findViewById(R.id.color_entry_iv)).getBackground()
                    .setColorFilter(Color.parseColor(getItem(position)), PorterDuff.Mode.SRC_ATOP);

            return convertView;
        }

    }
}
