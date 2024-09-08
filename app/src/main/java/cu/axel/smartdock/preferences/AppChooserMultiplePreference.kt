package cu.axel.smartdock.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.axel.smartdock.R
import cu.axel.smartdock.models.App
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils


class AppChooserMultiplePreference(private val context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {
    override fun onClick() {
        //val allApp = AppUtils.getInstalledApps(context)
        val allApp = AppUtils.getInstalledPackages(context)
        val apps = sharedPreferences!!.getStringSet(key, emptySet())
        val adapter = AppAdapter(context, allApp, apps!!.toList())

        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.choose_apps)
        val view = LayoutInflater.from(context).inflate(R.layout.app_chooser_list, null)
        val listView: ListView = view.findViewById(R.id.app_chooser_lv)
        dialogBuilder.setView(view)
        dialogBuilder.setPositiveButton("Ok") { _, _ ->
            persistStringSet(adapter.sApps.toSet())
        }
        val dialog = dialogBuilder.create()
        listView.adapter = adapter
        dialog.show()
    }

    class AppAdapter(
        private val context: Context,
        apps: List<App>,
        private val selectedApps: List<String>?
    ) :
        ArrayAdapter<App>(
            context, R.layout.app_chooser_entry, apps
        ) {
        val sApps = selectedApps?.toMutableList() ?: mutableListOf()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            val holder: Holder

            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.app_chooser_entry, null)
                holder = Holder()
                holder.icon = view.findViewById(R.id.app_chooser_entry_iv)
                holder.text = view.findViewById(R.id.app_chooser_entry_tv)
                holder.selection = view.findViewById(R.id.app_chooser_entry_chkbx)
                view.tag = holder
            } else {
                holder = view.tag as Holder
                holder.selection.setOnCheckedChangeListener(null)
            }

            val app: App = getItem(position)!!


            holder.icon.setImageDrawable(app.icon)
            holder.text.text = app.name
            ColorUtils.applyColor(
                holder.icon,
                ColorUtils.getDrawableDominantColor(holder.icon.drawable)
            )


            if (selectedApps != null) {
                holder.selection.visibility = View.VISIBLE
                holder.selection.isChecked = sApps.contains(app.packageName)
                holder.selection.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked)
                        sApps.add(app.packageName)
                    else
                        sApps.remove(app.packageName)
                }
            }
            return view!!
        }

        inner class Holder {
            lateinit var icon: ImageView
            lateinit var text: TextView
            lateinit var selection: CheckBox
        }
    }
}
