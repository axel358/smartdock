package cu.axel.smartdock.preferences

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.axel.smartdock.R
import cu.axel.smartdock.utils.AppUtils
import androidx.core.content.edit

private val LAUNCHER_INTENTS = arrayOf(
    "com.fede.launcher.THEME_ICONPACK",
    "com.anddoes.launcher.THEME",
    "com.teslacoilsw.launcher.THEME",
    "com.gau.go.launcherex.theme",
    "org.adw.launcher.THEMES",
    "org.adw.launcher.icons.ACTION_PICK_ICON",
    "net.oneplus.launcher.icons.ACTION_PICK_ICON"
)

class IconPackPreference(private val context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    private var sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /*
    These are all icon pack intents to date
    It could change in the future
    but by default, I don't think we even use these any more in icon packs
    but we support all icon packs to date (Long live Ander Web)
     */

    init {
        setTitle(R.string.icon_pack)
        val iconPack = sp.getString("icon_pack", "")!!
        if (iconPack.isEmpty()) {
            setSummary(R.string.system)
        } else {
            summary = AppUtils.getPackageLabel(context, iconPack)
        }
    }

    override fun onClick() {
        val pm: PackageManager = context.packageManager

        /*
		We manually add Smart Dock context as a default item so Smart Dock has a default item to rely on
		 */
        val iconPackageList = ArrayList<String>()
        val iconNameList = ArrayList<String>()
        iconPackageList.add(context.packageName)
        iconNameList.add(context.getString(R.string.system))
        val launcherActivities: MutableList<ResolveInfo> = ArrayList()
        /*
		Gather all the apps installed on the device
		filter all the icon pack packages to the list
		 */for (i in LAUNCHER_INTENTS) {
            launcherActivities.addAll(
                pm.queryIntentActivities(
                    Intent(i),
                    PackageManager.GET_META_DATA
                )
            )
        }
        for (resolveInfo in launcherActivities) {
            iconPackageList.add(resolveInfo.activityInfo.packageName)
            iconNameList.add(
                AppUtils.getPackageLabel(
                    context,
                    resolveInfo.activityInfo.packageName
                )
            )
        }
        val cleanedNameList: Set<String> = LinkedHashSet(iconNameList)
        val newNameList = cleanedNameList.toTypedArray<String>()
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setTitle(R.string.icon_pack)
        dialog.setItems(
            newNameList
        ) { _: DialogInterface?, item: Int ->
            if (iconPackageList[item] == context.packageName) {
                sp.edit { putString("icon_pack", "") }
                setSummary(R.string.system)
            } else {
                val iconPack = iconPackageList[item]
                sp.edit { putString("icon_pack", iconPack) }
                summary = AppUtils.getPackageLabel(context, iconPack)
            }
        }
        dialog.show()
    }
}
