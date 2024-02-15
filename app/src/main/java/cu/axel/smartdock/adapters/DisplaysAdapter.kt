package cu.axel.smartdock.adapters

import android.content.Context
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import cu.axel.smartdock.R
import cu.axel.smartdock.utils.ColorUtils

class DisplaysAdapter(private val context: Context, displays: Array<Display>) : ArrayAdapter<Display>(context, R.layout.pin_entry, displays) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val display = getItem(position)
        if (convertView == null) convertView = LayoutInflater.from(context).inflate(R.layout.pin_entry, null)
        val icon = convertView!!.findViewById<ImageView>(R.id.pin_entry_iv)
        val text = convertView.findViewById<TextView>(R.id.pin_entry_tv)
        ColorUtils.applySecondaryColor(context, PreferenceManager.getDefaultSharedPreferences(context), icon)
        text.text = display!!.name
        icon.setImageResource(R.drawable.ic_screen)
        return convertView
    }
}
