package cu.axel.smartdock.dialogs

import android.content.Context
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.PreferenceManager
import cu.axel.smartdock.R
import cu.axel.smartdock.utils.ColorUtils

class DockDialog(context: Context, asOverlay: Boolean = false) : AlertDialog(
    context,
    if (asOverlay) R.style.AppTheme_Dock_Dialog else R.style.MaterialAlertDialog_Material3
) {
    init {
        setCanceledOnTouchOutside(true)
        if (asOverlay) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val background = AppCompatResources.getDrawable(context, R.drawable.round_rect)
            val colors = ColorUtils.getMainColors(sharedPreferences, context)
            background?.setColorFilter(colors[0], PorterDuff.Mode.SRC_ATOP)
            window?.setBackgroundDrawable(background)
        }
    }

    fun setMessage(resId: Int) {
        setMessage(context.getString(resId))
    }

    fun setPositiveButton(resId: Int, listener: DialogInterface.OnClickListener) {
        setButton(BUTTON_POSITIVE, context.getString(resId), listener)
    }

    fun setNeutralButton(resId: Int, listener: DialogInterface.OnClickListener) {
        setButton(BUTTON_NEUTRAL, context.getString(resId), listener)
    }

    fun setNegativeButton(resId: Int, listener: DialogInterface.OnClickListener) {
        setButton(BUTTON_NEGATIVE, context.getString(resId), listener)
    }
}