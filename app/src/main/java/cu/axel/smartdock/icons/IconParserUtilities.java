package cu.axel.smartdock.icons;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

public class IconParserUtilities {

    /*
     Small utility for handling icon pack changes for apps
     Not exactly a clean method but it works like a charm
     */

    private final Context context;

    public IconParserUtilities(Context context) {
        this.context = context;
    }

    public Drawable getPackageIcon(String packageName) {
        /*
         Try to load an apps icon from package manager
         for whatever reason it fails
         fallback to package info for more in depth information
         */
        Drawable appIcon;
        PackageManager pm = context.getPackageManager();
        try {
            appIcon = pm.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                appIcon = packageInfo.applicationInfo.loadIcon(pm);
            } catch (PackageManager.NameNotFoundException e2) {
                appIcon = context.getDrawable(android.R.drawable.sym_def_app_icon);
            }
        }

        return appIcon;
    }

    public Drawable getPackageThemedIcon(String packageName) {
        final IconPackHelper iconPackHelper = IconPackHelper.getInstance(context);
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;
        if (iconPackHelper.isIconPackLoaded()) {
            /*
             if an icon pack has been set in the shared preferences
             load the respective icon based on its ID from the icon pack set
             */
            int iconId = iconPackHelper.getResourceIdForActivityIcon(activityInfo);
            {
                if (iconId != 0) {
                    return iconPackHelper.getIconPackResources(iconId, context);
                }
            }
        }
        /*
         if an icon pack is not set in the preference manager
         load the apps default icon
         */
        return getPackageIcon(packageName);
    }


}
