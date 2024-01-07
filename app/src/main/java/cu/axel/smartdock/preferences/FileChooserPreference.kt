package cu.axel.smartdock.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButton
import cu.axel.smartdock.R

class FileChooserPreference(private val context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    init {
        setupPreference()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val resetButton = holder.findViewById(R.id.fs_preference_reset_btn) as MaterialButton
        resetButton.setOnClickListener { setFile("default") }
    }

    private fun setupPreference() {
        widgetLayoutResource = R.layout.preference_file_chooser
    }

    fun setFile(file: String) {
        persistString(file)
        updateSummary()
    }

    override fun onAttached() {
        super.onAttached()
        updateSummary()
    }

    private fun updateSummary() {
        val file = sharedPreferences!!.getString(key, "default")
        summary = if (file == "default") context.getString(R.string.tap_to_set) else ""
    }
}