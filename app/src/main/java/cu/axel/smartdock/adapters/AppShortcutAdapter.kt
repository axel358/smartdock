package cu.axel.smartdock.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ShortcutInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import cu.axel.smartdock.R
import cu.axel.smartdock.utils.DeepShortcutManager

class AppShortcutAdapter(private val context: Context, shortcuts: List<ShortcutInfo>) : ArrayAdapter<ShortcutInfo>(context, R.layout.context_menu_entry, shortcuts) {
    @SuppressLint("NewApi")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.context_menu_entry, null)
        val icon = convertView!!.findViewById<ImageView>(R.id.menu_entry_iv)
        val text = convertView.findViewById<TextView>(R.id.menu_entry_tv)
        val shortcut = getItem(position)!!
        icon!!.setImageDrawable(DeepShortcutManager.getShortcutIcon(shortcut, context))
        text!!.text = shortcut.shortLabel
        return convertView
    }
}
