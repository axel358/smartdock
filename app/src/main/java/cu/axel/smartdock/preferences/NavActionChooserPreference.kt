package cu.axel.smartdock.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import cu.axel.smartdock.R
import androidx.core.content.edit

val NAV_LONG_ACTIONS = listOf("none", "notifications", "assistant", "lock", "split")

class NavActionChooserPreference(private val context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {

    init {
        setupPreference()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val configureButton = holder.findViewById(R.id.configurable_switch_button) as MaterialButton
        val switch = holder.findViewById(R.id.configurable_switch) as MaterialSwitch
        switch.isChecked = sharedPreferences!!.getBoolean(key, false)
        switch.setOnCheckedChangeListener { button, state ->
            setState(state)
        }
        configureButton.setOnClickListener { setAction() }
        holder.itemView.setOnClickListener { setAction() }
    }

    private fun setupPreference() {
        widgetLayoutResource = R.layout.preference_configurable_switch
        summary = context.getString(R.string.tap_to_set_long_action)
    }

    fun setAction() {
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.long_press_action)
        val item = NAV_LONG_ACTIONS.indexOf(getAction())
        dialogBuilder.setSingleChoiceItems(
            R.array.nav_long_actions,
            item
        ) { dialogInterface, position ->
            sharedPreferences!!.edit {
                putString("${key}_long_action", NAV_LONG_ACTIONS[position])
            }
            dialogInterface.dismiss()
        }
        dialogBuilder.show()
    }

    fun setState(state: Boolean) {
        persistBoolean(state)
    }

    fun getAction() = sharedPreferences!!.getString("${key}_long_action", "none")
}