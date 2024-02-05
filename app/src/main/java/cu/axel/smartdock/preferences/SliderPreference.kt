package cu.axel.smartdock.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import cu.axel.smartdock.R

class SliderPreference(private val context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private lateinit var listener: OnDialogShownListener
     lateinit var slider: Slider
    override fun onClick() {
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setTitle(title)
        val view = LayoutInflater.from(context).inflate(R.layout.preference_slider, null)
        slider = view.findViewById(R.id.preference_slider)
        dialog.setPositiveButton(R.string.ok, null)
        dialog.setView(view)
        dialog.show()
        if (::listener.isInitialized)
            listener.onDialogShown()
    }

    fun setOnDialogShownListener(listener: OnDialogShownListener) {
        this.listener = listener
    }

    interface OnDialogShownListener {
        fun onDialogShown()
    }
}