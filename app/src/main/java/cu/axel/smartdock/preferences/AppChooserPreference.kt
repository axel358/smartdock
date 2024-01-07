package cu.axel.smartdock.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.axel.smartdock.R
import cu.axel.smartdock.models.App
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils


class AppChooserPreference(private val context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    init {
        setupPreference()
    }

    private fun setupPreference() {
        val packageName = sharedPreferences!!.getString(key, "")!!
        summary = if (packageName.isEmpty()) context.getString(R.string.tap_to_set) else AppUtils.getPackageLabel(context, packageName)
    }

    override fun onClick() {
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setTitle(R.string.choose_app)
        val apps = AppUtils.getInstalledApps(context.packageManager)
        dialog.setAdapter(AppAdapter(context, apps)) { _, position ->
            val app = apps[position]
            sharedPreferences!!.edit().putString(key, app.packageName).apply()
            summary = app.name
        }
        dialog.show()
    }

    internal class AppAdapter(private val context: Context, apps: ArrayList<App>) : ArrayAdapter<App>(context, R.layout.pin_entry, apps) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) convertView = LayoutInflater.from(context).inflate(R.layout.app_chooser_entry, null)
            val icon = convertView!!.findViewById<ImageView>(R.id.app_chooser_entry_iv)
            val text = convertView.findViewById<TextView>(R.id.app_chooser_entry_tv)
            val app = getItem(position)!!
            icon.setImageDrawable(app.icon)
            text.text = app.name
            ColorUtils.applyColor(icon, ColorUtils.getDrawableDominantColor(icon.drawable))
            return convertView
        }
    }
}
