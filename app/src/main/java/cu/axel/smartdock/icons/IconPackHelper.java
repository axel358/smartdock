package cu.axel.smartdock.icons;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconPackHelper implements OnDismissListener {
    private static final String ICON_MASK_TAG = "iconmask";
    private static final String ICON_BACK_TAG = "iconback";
    private static final String ICON_UPON_TAG = "iconupon";
    private static final String ICON_SCALE_TAG = "scale";

    // Holds package/class -> drawable
    private Map<String, String> mIconPackResources;
    private Context mContext;
    private String mLoadedIconPackName;
    private Resources mLoadedIconPackResource;
    Drawable mIconUpon, mIconMask;
    private final List<Drawable> mIconBackList;
    private final List<String> mIconBackStrings;
    float mIconScale;
    private String mCurrentIconPack = "";
    private boolean mLoading;
    private AlertDialog mDialog;

    @SuppressLint("StaticFieldLeak")
    private static IconPackHelper sInstance;

    public static IconPackHelper getInstance(Context context) {
        if (sInstance == null){
            sInstance = new IconPackHelper();
            sInstance.setContext(context);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            sInstance.init(prefs);
        }
        return sInstance;
    }

    public IconPackHelper() {
        mIconPackResources = new HashMap<String, String>();
        mIconBackList = new ArrayList<Drawable>();
        mIconBackStrings = new ArrayList<String>();
    }

    private void setContext(Context context) {
        mContext = context;
    }

    private Drawable getDrawableForName(String name) {
        if (isIconPackLoaded()) {
            String item = mIconPackResources.get(name);
            if (!TextUtils.isEmpty(item)) {
                int id = getResourceIdForDrawable(item);
                if (id != 0) {
                    return mLoadedIconPackResource.getDrawable(id);
                }
            }
        }
        return null;
    }

    private Drawable getDrawableWithName(String name) {
        if (isIconPackLoaded()) {
            int id = getResourceIdForDrawable(name);
            if (id != 0) {
                return mLoadedIconPackResource.getDrawable(id);
            }
        }
        return null;
    }

    private void loadResourcesFromXmlParser(XmlPullParser parser,
            Map<String, String> iconPackResources) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("item")) {
                String component = parser.getAttributeValue(null, "component");
                String drawable = parser.getAttributeValue(null, "drawable");
                // Validate component/drawable exist

                if (TextUtils.isEmpty(component) || TextUtils.isEmpty(drawable)) {
                    continue;
                }

                // Validate format/length of component
                if (!component.startsWith("ComponentInfo{") || !component.endsWith("}")
                        || component.length() < 16) {
                    continue;
                }

                // Sanitize stored value
                component = component.substring(14, component.length() - 1);

                if (!component.contains("/")) {
                    // Package icon reference
                    iconPackResources.put(component, drawable);
                } else {
                    ComponentName componentName = ComponentName.unflattenFromString(component);
                    if (componentName != null) {
                        iconPackResources.put(componentName.getPackageName(), drawable);
                        iconPackResources.put(component, drawable);
                    }
                }
                continue;
            }

            if (name.equalsIgnoreCase(ICON_BACK_TAG)) {
                String icon = parser.getAttributeValue(null, "img");
                if (icon == null) {
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        mIconBackStrings.add(parser.getAttributeValue(i));
                    }
                }
                continue;
            }

            if (name.equalsIgnoreCase(ICON_MASK_TAG) ||
                    name.equalsIgnoreCase(ICON_UPON_TAG)) {
                String icon = parser.getAttributeValue(null, "img");
                if (icon == null) {
                    if (parser.getAttributeCount() > 0) {
                        icon = parser.getAttributeValue(0);
                    }
                }
                iconPackResources.put(parser.getName().toLowerCase(), icon);
                continue;
            }

            if (name.equalsIgnoreCase(ICON_SCALE_TAG)) {
                String factor = parser.getAttributeValue(null, "factor");
                if (factor == null) {
                    if (parser.getAttributeCount() > 0) {
                        factor = parser.getAttributeValue(0);
                    }
                }
                if (factor != null) {
                    iconPackResources.put(parser.getName().toLowerCase(), factor);
                }
            }
        }
    }

    private void loadIconPack() {
        String packageName = mCurrentIconPack;
        mIconBackList.clear();
        mIconBackStrings.clear();
        if (TextUtils.isEmpty(packageName)){
            return;
        }
        mLoading = true;
        mIconPackResources = getIconPackResourcesNew(mContext, packageName);
        Resources res = null;
        try {
            res = mContext.getPackageManager().getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            mLoading = false;
            return;
        }
        mLoadedIconPackResource = res;
        mLoadedIconPackName = packageName;
        mIconMask = getDrawableForName(ICON_MASK_TAG);
        mIconUpon = getDrawableForName(ICON_UPON_TAG);
        for (int i = 0; i < mIconBackStrings.size(); i++) {
            String backIconString = mIconBackStrings.get(i);
            Drawable backIcon = getDrawableWithName(backIconString);
            if (backIcon != null) {
                mIconBackList.add(backIcon);
            }
        }
        String scale = mIconPackResources.get(ICON_SCALE_TAG);
        if (scale != null) {
            try {
                mIconScale = Float.parseFloat(scale);
            } catch (NumberFormatException ignored) {
            }
        }
        mLoading = false;
    }

    //new method from trebuchet
    private Map<String, String> getIconPackResourcesNew(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }

        Resources res = null;
        try {
            res = context.getPackageManager().getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        XmlPullParser parser = null;
        InputStream inputStream = null;
        Map<String, String> iconPackResources = new HashMap<String, String>();

        try {
            inputStream = res.getAssets().open("appfilter.xml");
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
            parser.setInput(inputStream, "UTF-8");
        } catch (Exception e) {
            // Catch any exception since we want to fall back to parsing the xml/
            // resource in all cases
            int resId = res.getIdentifier("appfilter", "xml", packageName);
            if (resId != 0) {
                parser = res.getXml(resId);
            }
        }

        if (parser != null) {
            try {
                loadResourcesFromXmlParser(parser, iconPackResources);
                return iconPackResources;
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            } finally {
                // Cleanup resources
                if (parser instanceof XmlResourceParser) {
                    ((XmlResourceParser) parser).close();
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        // Application uses a different theme format (most likely launcher pro)
        int arrayId = res.getIdentifier("theme_iconpack", "array", packageName);
        if (arrayId == 0) {
            arrayId = res.getIdentifier("icon_pack", "array", packageName);
        }

        if (arrayId != 0) {
            String[] iconPack = res.getStringArray(arrayId);
            for (String entry : iconPack) {

                if (TextUtils.isEmpty(entry)) {
                    continue;
                }

                String icon = entry.toLowerCase();
                entry = entry.replaceAll("_", ".");

                iconPackResources.put(entry, icon);

                int activityIndex = entry.lastIndexOf(".");
                if (activityIndex <= 0 || activityIndex == entry.length() - 1) {
                    continue;
                }

                String iconPackage = entry.substring(0, activityIndex);
                if (TextUtils.isEmpty(iconPackage)) {
                    continue;
                }
                iconPackResources.put(iconPackage, icon);

                String iconActivity = entry.substring(activityIndex + 1);
                if (TextUtils.isEmpty(iconActivity)) {
                    continue;
                }
                iconPackResources.put(iconPackage + "." + iconActivity, icon);
            }
        } else {
            loadApplicationResources(context, iconPackResources, packageName);
        }
        return iconPackResources;
    }

    private void loadApplicationResources(Context context,
                                                 Map<String, String> iconPackResources, String packageName) {
        Field[] drawableItems = null;
        try {
            Context appContext = context.createPackageContext(packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            drawableItems = Class.forName(packageName+".R$drawable",
                    true, appContext.getClassLoader()).getFields();
        } catch (Exception e){
            return;
        }

        for (Field f : drawableItems) {
            String name = f.getName();

            String icon = name.toLowerCase();
            name = name.replaceAll("_", ".");

            iconPackResources.put(name, icon);

            int activityIndex = name.lastIndexOf(".");
            if (activityIndex <= 0 || activityIndex == name.length() - 1) {
                continue;
            }

            String iconPackage = name.substring(0, activityIndex);
            if (TextUtils.isEmpty(iconPackage)) {
                continue;
            }
            iconPackResources.put(iconPackage, icon);

            String iconActivity = name.substring(activityIndex + 1);
            if (TextUtils.isEmpty(iconActivity)) {
                continue;
            }
            iconPackResources.put(iconPackage + "." + iconActivity, icon);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mDialog != null) {
            mDialog = null;
        }
    }

    public boolean isIconPackLoaded() {
        return mLoadedIconPackResource != null &&
                mLoadedIconPackName != null &&
                mIconPackResources != null;
    }

    private int getResourceIdForDrawable(String resource) {
        return mLoadedIconPackResource.getIdentifier(resource, "drawable", mLoadedIconPackName);
    }

    public Drawable getIconPackResources(int id, Context mContext) {
        return mLoadedIconPackResource.getDrawable(id, mContext.getTheme());
    }

    public int getResourceIdForActivityIcon(ActivityInfo info) {
        // TODO since we are loading in background block access until load ready
        if (!isIconPackLoaded() || mLoading){
            return 0;
        }
        //Try to match icon class by lower case, if not fallback to exact string
        //Catch added for lower case exceptions
        String drawable;
        try {
            drawable = mIconPackResources.get(info.packageName.toLowerCase()
                    + "." + info.name.toLowerCase());
        } catch (NullPointerException e) {
            drawable = mIconPackResources.get(info.packageName
                    + "." + info.name);
        }
        if (drawable == null) {
            // Icon pack doesn't have an icon for the activity, fallback to package icon
            //Catch added for lower case exceptions
            try {
                drawable = mIconPackResources.get(info.packageName.toLowerCase());
            }catch (NullPointerException e) {
                drawable = mIconPackResources.get(info.packageName);
            }
            if (drawable == null) {
                return 0;
            }
        }
        return getResourceIdForDrawable(drawable);
    }

    public String getIconPack(SharedPreferences prefs){
        return mCurrentIconPack = prefs.getString("icon_pack", "");
    }

    private void init(SharedPreferences prefs) {
        mCurrentIconPack = prefs.getString("icon_pack", "");
        if (!TextUtils.isEmpty(mCurrentIconPack)){
            try {
                loadIconPack();
            }catch (NullPointerException i){
                Log.d("Icon Pack Error", "Loading Error : " + i);
                //Icon Pack is not supported so wipe the icon pack data
                prefs.edit().putString("icon_pack", "").apply();
                Toast.makeText(mContext, "Unsupported Icon Pack", Toast.LENGTH_LONG).show();
            }
        }
    }

    //for loading all the icons in a package
    public static ArrayList<IconData> getCustomIconPackResources(
            Context context, String packageName) {
        Resources res;
        try {
            res = context.getPackageManager().getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        XmlResourceParser parser = null;
        ArrayList<IconData> iconPackResources = new ArrayList<>();

        try {
            parser = res.getAssets().openXmlResourceParser("drawable.xml");
        } catch (IOException e) {
            int resId = res.getIdentifier("drawable", "xml", packageName);
            if (resId != 0) {
                parser = res.getXml(resId);
            }
        }

        if (parser != null) {
            try {
                loadCustomResourcesFromXmlParser(parser, iconPackResources);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            } finally {
                parser.close();
            }
        }
        return iconPackResources;
    }

    private static void loadCustomResourcesFromXmlParser(
            XmlPullParser parser, ArrayList<IconData> iconPackResources)
            throws XmlPullParserException, IOException {

        int eventType = parser.getEventType();
        do {
            if (eventType != XmlPullParser.START_TAG) {
                continue;
            }

            if (parser.getName().equalsIgnoreCase("item")) {
                String drawable = parser.getAttributeValue(null, "drawable");
                if (TextUtils.isEmpty(drawable) || drawable.length() == 0) {
                    continue;
                }
                IconData item = new IconData();
                item.isIcon = true;
                item.title = drawable;
                iconPackResources.add(item);
            } else if (parser.getName().equalsIgnoreCase("category")) {
                String title = parser.getAttributeValue(null, "title");
                if (TextUtils.isEmpty(title) || title.length() == 0) {
                    continue;
                }
                IconData item = new IconData();
                item.isHeader = true;
                item.title = title;
                iconPackResources.add(item);
            }
        } while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT);

    }
}
