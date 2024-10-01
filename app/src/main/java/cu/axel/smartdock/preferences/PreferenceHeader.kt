package cu.axel.smartdock.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButton
import cu.axel.smartdock.R

class PreferenceHeader(private val context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    init {
        layoutResource = R.layout.preference_header
    }
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val iconBtn = holder.findViewById(R.id.preference_icon_btn) as MaterialButton
        iconBtn.icon = icon
    }
}