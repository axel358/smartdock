package cu.axel.smartdock.dialogs

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.axel.smartdock.R

class DockLayoutDialog(context: Context) : MaterialAlertDialogBuilder(context) {
    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        setTitle(R.string.choose_dock_layout)
        val layout = sharedPreferences.getInt("dock_layout", -1)
        setSingleChoiceItems(R.array.layouts, layout) { _, which ->
            editor.putBoolean("enable_nav_back", which != 0)
            editor.putBoolean("enable_nav_home", which != 0)
            editor.putBoolean("enable_nav_recents", which != 0)
            editor.putBoolean("enable_qs_wifi", which != 0)
            editor.putBoolean("enable_qs_vol", which != 0)
            editor.putBoolean("enable_qs_date", which != 0)
            editor.putBoolean("enable_qs_notif", which != 0)
            editor.putBoolean("app_menu_fullscreen", which != 2)
            editor.putString("launch_mode", if (which != 2) "fullscreen" else "standard")
            editor.putString(
                "max_running_apps", when (which) {
                    0 -> "4"
                    1 -> "10"
                    else -> "15"
                }
            )
            editor.putString("dock_activation_area", if (which == 2) "5" else "25")
            editor.putInt("dock_layout", which)
            editor.putString("activation_method", if (which != 2) "handle" else "swipe")
            editor.putBoolean("show_notifications", which != 0)
            editor.apply()
        }
        setPositiveButton(R.string.ok, null)
        show()
    }
}