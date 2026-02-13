package cu.axel.smartdock.dialogs

import android.content.Context
import android.graphics.PorterDuff
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.axel.smartdock.R
import cu.axel.smartdock.utils.ColorUtils

class DockDialog(context: Context, val asOverlay: Boolean = false) : MaterialAlertDialogBuilder(
    context,
    if (asOverlay) R.style.AppTheme_Dock_Dialog else R.style.MaterialAlertDialog_Material3
) {

    override fun show(): AlertDialog? {
        val dialog = create()
        if (asOverlay) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val background = AppCompatResources.getDrawable(context, R.drawable.round_rect)
            val colors = ColorUtils.getMainColors(sharedPreferences, context)
            background?.setColorFilter(colors[0], PorterDuff.Mode.SRC_ATOP)
            dialog.window?.setBackgroundDrawable(background)
        }
        return dialog.also { it.show() }
    }
}