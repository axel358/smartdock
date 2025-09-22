package cu.axel.smartdock.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Field
import java.util.Locale
import androidx.core.content.edit

private const val ICON_MASK_TAG = "iconmask"
private const val ICON_BACK_TAG = "iconback"
private const val ICON_UPON_TAG = "iconupon"
private const val ICON_SCALE_TAG = "scale"

class IconPackUtils(val context: Context) {
    // Holds package/class -> drawable
    private var iconPackResources: Map<String, String?>? = HashMap()
    private var loadedIconPackName: String? = null
    private var loadedIconPackResource: Resources? = null
    private var mIconUpon: Drawable? = null
    private var iconMask: Drawable? = null
    private val iconBackList: MutableList<Drawable> = ArrayList()
    private val iconBackStrings: MutableList<String> = ArrayList()
    private var iconScale = 0f
    private var loading = false

    init {
        try {
            loadIconPack()
        } catch (e: Exception) {
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putString("icon_pack", "")
            }
            Log.e(context.packageName, e.stackTraceToString())
        }
    }

    private fun getDrawableForName(name: String): Drawable? {
        if (isIconPackLoaded) {
            val item = iconPackResources?.get(name)
            if (!item.isNullOrEmpty()) {
                val id = getResourceIdForDrawable(item)
                if (id != 0) {
                    return loadedIconPackResource!!.getDrawable(id)
                }
            }
        }
        return null
    }

    private fun getDrawableWithName(name: String): Drawable? {
        if (isIconPackLoaded) {
            val id = getResourceIdForDrawable(name)
            if (id != 0) {
                return loadedIconPackResource!!.getDrawable(id)
            }
        }
        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun loadResourcesFromXmlParser(
        parser: XmlPullParser,
        iconPackResources: MutableMap<String, String?>
    ) {
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == "item") {
                var component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                // Validate component/drawable exist
                if (TextUtils.isEmpty(component) || TextUtils.isEmpty(drawable)) {
                    continue
                }

                // Validate format/length of component
                if (!component.startsWith("ComponentInfo{") || !component.endsWith("}") || component.length < 16) {
                    continue
                }

                // Sanitize stored value
                component = component.substring(14, component.length - 1)
                if (!component.contains("/")) {
                    // Package icon reference
                    iconPackResources[component] = drawable
                } else {
                    val componentName = ComponentName.unflattenFromString(component)
                    if (componentName != null) {
                        iconPackResources[componentName.packageName] = drawable
                        iconPackResources[component] = drawable
                    }
                }
                continue
            }
            if (name.equals(ICON_BACK_TAG, ignoreCase = true)) {
                val icon = parser.getAttributeValue(null, "img")
                if (icon == null) {
                    for (i in 0 until parser.attributeCount) {
                        iconBackStrings.add(parser.getAttributeValue(i))
                    }
                }
                continue
            }
            if (name.equals(ICON_MASK_TAG, ignoreCase = true) || name.equals(
                    ICON_UPON_TAG,
                    ignoreCase = true
                )
            ) {
                var icon = parser.getAttributeValue(null, "img")
                if (icon == null) {
                    if (parser.attributeCount > 0) {
                        icon = parser.getAttributeValue(0)
                    }
                }
                iconPackResources[parser.name.lowercase(Locale.getDefault())] = icon
                continue
            }
            if (name.equals(ICON_SCALE_TAG, ignoreCase = true)) {
                var factor = parser.getAttributeValue(null, "factor")
                if (factor == null) {
                    if (parser.attributeCount > 0) {
                        factor = parser.getAttributeValue(0)
                    }
                }
                if (factor != null) {
                    iconPackResources[parser.name.lowercase(Locale.getDefault())] = factor
                }
            }
        }
    }

    private fun loadIconPack() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val currentIconPack = sharedPreferences.getString("icon_pack", "")
        iconBackList.clear()
        iconBackStrings.clear()
        if (currentIconPack.isNullOrEmpty())
            return

        loading = true
        iconPackResources = getIconPackResources(context, currentIconPack)
        val resources: Resources =
            context.packageManager.getResourcesForApplication(currentIconPack)

        loadedIconPackResource = resources
        loadedIconPackName = currentIconPack
        iconMask = getDrawableForName(ICON_MASK_TAG)
        mIconUpon = getDrawableForName(ICON_UPON_TAG)
        for (i in iconBackStrings.indices) {
            val backIconString = iconBackStrings[i]
            val backIcon = getDrawableWithName(backIconString)
            if (backIcon != null) {
                iconBackList.add(backIcon)
            }
        }
        val scale = iconPackResources!![ICON_SCALE_TAG]
        if (scale != null) {
            try {
                iconScale = scale.toFloat()
            } catch (_: NumberFormatException) {
            }
        }
        loading = false
    }

    //new method from trebuchet
    @SuppressLint("DiscouragedApi")
    private fun getIconPackResources(
        context: Context,
        packageName: String
    ): Map<String, String?>? {
        if (packageName.isEmpty())
            return null

        val resources: Resources = try {
            context.packageManager.getResourcesForApplication(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        var parser: XmlPullParser? = null
        var inputStream: InputStream? = null
        val iconPackResources: MutableMap<String, String?> = HashMap()
        try {
            inputStream = resources.assets.open("appfilter.xml")
            val factory = XmlPullParserFactory.newInstance()
            parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")
        } catch (_: Exception) {
            // Catch any exception since we want to fall back to parsing the xml/
            // resource in all cases
            val resId = resources.getIdentifier("appfilter", "xml", packageName)
            if (resId != 0) {
                parser = resources.getXml(resId)
            }
        }
        if (parser != null) {
            try {
                loadResourcesFromXmlParser(parser, iconPackResources)
                return iconPackResources
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                // Cleanup resources
                if (parser is XmlResourceParser) {
                    parser.close()
                }
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (_: IOException) {
                    }
                }
            }
        }

        // Application uses a different theme format (most likely launcher pro)
        var arrayId = resources.getIdentifier("theme_iconpack", "array", packageName)
        if (arrayId == 0) {
            arrayId = resources.getIdentifier("icon_pack", "array", packageName)
        }
        if (arrayId != 0) {
            val iconPack = resources.getStringArray(arrayId)
            for (entry in iconPack) {
                if (TextUtils.isEmpty(entry)) {
                    continue
                }
                val icon = entry.lowercase(Locale.getDefault())
                val entry = entry.replace("_".toRegex(), ".")
                iconPackResources[entry] = icon
                val activityIndex = entry.lastIndexOf(".")
                if (activityIndex <= 0 || activityIndex == entry.length - 1) {
                    continue
                }
                val iconPackage = entry.substring(0, activityIndex)
                if (TextUtils.isEmpty(iconPackage)) {
                    continue
                }
                iconPackResources[iconPackage] = icon
                val iconActivity = entry.substring(activityIndex + 1)
                if (TextUtils.isEmpty(iconActivity)) {
                    continue
                }
                iconPackResources["$iconPackage.$iconActivity"] = icon
            }
        } else {
            loadApplicationResources(context, iconPackResources, packageName)
        }
        return iconPackResources
    }

    private fun loadApplicationResources(
        context: Context?,
        iconPackResources: MutableMap<String, String?>,
        packageName: String?
    ) {
        val drawableItems: Array<Field> = try {
            val appContext = context!!.createPackageContext(
                packageName,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
            Class.forName("$packageName.R\$drawable", true, appContext.classLoader).fields
        } catch (_: Exception) {
            return
        }
        for (f in drawableItems) {
            var name = f.name
            val icon = name.lowercase(Locale.getDefault())
            name = name.replace("_".toRegex(), ".")
            iconPackResources[name] = icon
            val activityIndex = name.lastIndexOf(".")
            if (activityIndex <= 0 || activityIndex == name.length - 1) {
                continue
            }
            val iconPackage = name.substring(0, activityIndex)
            if (TextUtils.isEmpty(iconPackage)) {
                continue
            }
            iconPackResources[iconPackage] = icon
            val iconActivity = name.substring(activityIndex + 1)
            if (TextUtils.isEmpty(iconActivity)) {
                continue
            }
            iconPackResources["$iconPackage.$iconActivity"] = icon
        }
    }

    private val isIconPackLoaded: Boolean
        get() = loadedIconPackResource != null && loadedIconPackName != null && iconPackResources != null

    @SuppressLint("DiscouragedApi")
    private fun getResourceIdForDrawable(resource: String?): Int {
        return loadedIconPackResource!!.getIdentifier(resource, "drawable", loadedIconPackName)
    }

    private fun getIconPackResources(id: Int, mContext: Context): Drawable? {
        return ResourcesCompat.getDrawable(loadedIconPackResource!!, id, mContext.theme)
    }

    private fun getResourceIdForActivityIcon(info: ActivityInfo): Int {
        // TODO since we are loading in background block access until load ready
        if (!isIconPackLoaded || loading) {
            return 0
        }
        //Try to match icon class by lower case, if not fallback to exact string
        //Catch added for lower case exceptions
        var drawable: String?
        drawable = try {
            iconPackResources?.get(
                info.packageName.lowercase(Locale.getDefault()) + "." + info.name.lowercase(
                    Locale.getDefault()
                )
            )
        } catch (_: NullPointerException) {
            iconPackResources?.get(info.packageName + "." + info.name)
        }
        if (drawable == null) {
            // Icon pack doesn't have an icon for the activity, fallback to package icon
            //Catch added for lower case exceptions
            drawable = try {
                iconPackResources?.get(info.packageName.lowercase(Locale.getDefault()))
            } catch (_: NullPointerException) {
                iconPackResources?.get(info.packageName)
            }
            if (drawable == null) {
                return 0
            }
        }
        return getResourceIdForDrawable(drawable)
    }

    fun getAppThemedIcon(packageName: String): Drawable? {
        val activityInfo = ActivityInfo()
        activityInfo.packageName = packageName
        if (isIconPackLoaded) {
            val iconId = getResourceIdForActivityIcon(activityInfo)
            run {
                if (iconId != 0) {
                    return getIconPackResources(iconId, context)
                }
            }
        }
        return AppUtils.getAppIcon(context, packageName)
    }
}
