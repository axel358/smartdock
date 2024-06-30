package cu.axel.smartdock.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.ViewSwitcher
import androidx.core.widget.addTextChangedListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import cu.axel.smartdock.R
import cu.axel.smartdock.dialogs.DockLayoutDialog
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils

class AppearancePreferences : PreferenceFragmentCompat() {
    private lateinit var mainColorPref: Preference
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, arg1)
        mainColorPref = findPreference("theme_main_color")!!
        mainColorPref.setOnPreferenceClickListener {
            showColorPickerDialog(requireContext())
            false
        }
        findPreference<Preference>("theme")!!.setOnPreferenceChangeListener { _, newValue ->
            mainColorPref.isVisible = newValue.toString() == "custom"
            true
        }
        mainColorPref.isVisible = mainColorPref.sharedPreferences!!.getString("theme", "dark") == "custom"
        findPreference<Preference>("tint_indicators")!!.isVisible = AppUtils.isSystemApp(requireContext(), requireContext().packageName)

        findPreference<Preference>("dock_layout")!!.setOnPreferenceClickListener {
            DockLayoutDialog(requireContext())
            false
        }
    }

    private fun showColorPickerDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        val colorPreview = view.findViewById<View>(R.id.color_preview)
        val colorHexEt = view.findViewById<TextInputEditText>(R.id.color_hex_et)
        val alphaSb = view.findViewById<Slider>(R.id.color_alpha_sb)
        val redSb = view.findViewById<Slider>(R.id.color_red_sb)
        val greenSb = view.findViewById<Slider>(R.id.color_green_sb)
        val blueSb = view.findViewById<Slider>(R.id.color_blue_sb)
        val viewSwitcher = view.findViewById<ViewSwitcher>(R.id.colors_view_switcher)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.colors_btn_toggle)
        colorHexEt.addTextChangedListener { text ->
            val hexColor = text.toString()
            var color = -1
            if (hexColor.length == 7 && ColorUtils.toColor(hexColor).also { color = it } != -1) {
                colorPreview.background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                redSb.value = Color.red(color).toFloat()
                greenSb.value = Color.green(color).toFloat()
                blueSb.value = Color.blue(color).toFloat()
            } else colorHexEt.error = getString(R.string.invalid_color)

        }
        alphaSb.addOnChangeListener { _, value, _ -> colorPreview.background.alpha = value.toInt() }
        val onChangeListener = Slider.OnChangeListener { _, _, fromUser ->
            if (fromUser) colorHexEt.setText(ColorUtils.toHexColor(
                    Color.rgb(redSb.value.toInt(), greenSb.value.toInt(), blueSb.value.toInt())))
        }
        redSb.addOnChangeListener(onChangeListener)
        greenSb.addOnChangeListener(onChangeListener)
        blueSb.addOnChangeListener(onChangeListener)
        dialog.setNegativeButton(R.string.cancel, null)
        dialog.setPositiveButton(R.string.ok) { _, _ ->
            val color = colorHexEt.text.toString()
            if (ColorUtils.toColor(color) != -1) {
                mainColorPref.sharedPreferences!!.edit().putString(mainColorPref.key, color).apply()
                mainColorPref.sharedPreferences!!.edit().putInt("theme_main_alpha", alphaSb.value.toInt())
                        .apply()
            }
        }
        alphaSb.value = mainColorPref.sharedPreferences!!.getInt("theme_main_alpha", 255).toFloat()
        val hexColor = mainColorPref.sharedPreferences!!.getString(mainColorPref.key, "#212121")
        colorHexEt.setText(hexColor)
        val presetsGv = view.findViewById<GridView>(R.id.presets_gv)
        presetsGv.adapter = HexColorAdapter(context, context.resources.getStringArray(R.array.default_color_values))
        presetsGv.onItemClickListener = OnItemClickListener { adapterView, _, position, _ ->
            colorHexEt.setText(adapterView.getItemAtPosition(position).toString())
            toggleGroup.check(R.id.custom_button)
            viewSwitcher.showNext()
        }
        view.findViewById<View>(R.id.custom_button).setOnClickListener { viewSwitcher.showPrevious() }
        view.findViewById<View>(R.id.presets_button).setOnClickListener { viewSwitcher.showNext() }
        dialog.setView(view)
        dialog.show()
    }

    internal class HexColorAdapter(private val context: Context, colors: Array<String>) : ArrayAdapter<String>(context, R.layout.color_entry, colors) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) convertView = LayoutInflater.from(context).inflate(R.layout.color_entry, null)
            convertView!!.findViewById<View>(R.id.color_entry_iv).background
                    .setColorFilter(Color.parseColor(getItem(position)), PorterDuff.Mode.SRC_ATOP)
            return convertView
        }
    }
}
