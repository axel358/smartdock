package cu.axel.smartdock.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import cu.axel.smartdock.R
import cu.axel.smartdock.models.AppTask
import cu.axel.smartdock.utils.ColorUtils

class AppTaskAdapter(private val context: Context, tasks: ArrayList<AppTask>) : ArrayAdapter<AppTask>(context, R.layout.context_menu_entry, tasks) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) convertView = LayoutInflater.from(context).inflate(R.layout.context_menu_entry, null)
        val icon = convertView!!.findViewById<ImageView>(R.id.menu_entry_iv)
        val text = convertView.findViewById<TextView>(R.id.menu_entry_tv)
        val task = getItem(position)
        icon.setImageDrawable(task!!.icon)
        text.text = task.name
        ColorUtils.applyColor(icon, ColorUtils.getDrawableDominantColor(icon.drawable))
        return convertView
    }
}
