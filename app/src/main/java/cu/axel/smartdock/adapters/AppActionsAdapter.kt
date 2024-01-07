package cu.axel.smartdock.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import cu.axel.smartdock.R
import cu.axel.smartdock.models.Action
import cu.axel.smartdock.utils.ColorUtils


class AppActionsAdapter(private val context: Context, actions: ArrayList<Action>) : ArrayAdapter<Action>(context, R.layout.pin_entry, actions) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val action = getItem(position)
        if (convertView == null) convertView = LayoutInflater.from(context).inflate(R.layout.pin_entry, null)
        val icon = convertView!!.findViewById<ImageView>(R.id.pin_entry_iv)
        val text = convertView.findViewById<TextView>(R.id.pin_entry_tv)
        ColorUtils.applySecondaryColor(context, PreferenceManager.getDefaultSharedPreferences(context), icon)
        text.text = action!!.text
        icon.setImageResource(action.icon)
        return convertView
    }
}
