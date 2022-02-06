package cu.axel.smartdock.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cu.axel.smartdock.icons.IconPackHelper;
import cu.axel.smartdock.icons.IconParserUtilities;

public class IconPackSwitchPreference extends SwitchPreference {

    private Context mContext;
    private IconParserUtilities iconParserUtilities;
    private SharedPreferences sharedPreferences;
    private Switch aSwitch;

    /*
    Create 2 list
    One list holds all the icon pack packages
    One list holds all the icon pack labels
     */
    public static ArrayList<String> iconPackageList = new ArrayList<>();
    public static ArrayList<String> iconNameList = new ArrayList<>();


    /*
    These are all icon pack intents to date
    It could change in the future
    but by default, I don't think we even use these any more in icon packs
    but we support all icon packs to date (Long live Ander Web)
     */
    private final String[] LAUNCHER_INTENTS = new String[] {
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.gau.go.launcherex.theme",
            "org.adw.launcher.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
            "net.oneplus.launcher.icons.ACTION_PICK_ICON"
    };

    private SharedPreferences.Editor myEdit;
    private IconPackHelper iconPackHelper;

    public IconPackSwitchPreference(Context context) {
        super(context);
        setupPreference(context);
    }

    public IconPackSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPreference(context);
    }

    public IconPackSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupPreference(context);
    }

    private void setupPreference(Context context){
        mContext = context;
        iconParserUtilities = new IconParserUtilities(mContext);
        iconPackHelper = new IconPackHelper();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        setTitle("Icon Theming");

        if(iconPackHelper.getIconPack(sharedPreferences).equals("")){
            setSummary("Default");
            setIcon(iconParserUtilities.getPackageIcon(mContext.getPackageName()));
        }else{
            setSummary(iconParserUtilities.getPackageLabel(iconPackHelper.getIconPack(sharedPreferences)));
            setIcon(iconParserUtilities.getPackageIcon(iconPackHelper.getIconPack(sharedPreferences)));
        }

        PackageManager pm = mContext.getPackageManager();

        /*
        We manually add Smart Dock context as a default item so Smart Dock has a default item to rely on
         */
        iconPackageList.add(mContext.getPackageName());
        iconNameList.add("Default");

        myEdit = sharedPreferences.edit();

        List<ResolveInfo> launcherActivities = new ArrayList<>();
        /*
        Gather all the apps installed on the device
        filter all the icon pack packages to the list
         */
        for (String i : LAUNCHER_INTENTS) {
            launcherActivities.addAll(pm.queryIntentActivities(
                    new Intent(i), PackageManager.GET_META_DATA));
        }
        for (ResolveInfo ri : launcherActivities) {
            iconPackageList.add(ri.activityInfo.packageName);
            iconNameList.add(iconParserUtilities.getPackageLabel(ri.activityInfo.packageName));
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        /*
        Find the view ID of the switch and adjust its state based on icon pack status
         */
        aSwitch = findSwitchInChildviews((ViewGroup) view);

        if(aSwitch != null){
            aSwitch.setChecked(!iconPackHelper.getIconPack(sharedPreferences).equals(""));
        }
    }

    private Switch findSwitchInChildviews(ViewGroup view) {
        for (int i = 0; i < view.getChildCount(); i++) {
            View thisChildview = view.getChildAt(i);
            if (thisChildview instanceof Switch) {
                return (Switch) thisChildview;
            } else if (thisChildview instanceof ViewGroup) {
                Switch theSwitch = findSwitchInChildviews((ViewGroup) thisChildview);
                if (theSwitch != null) return theSwitch;
            }
        }
        return null;
    }

    @Override
    protected void onClick() {
        /*
        Disable the preference click listener from binding to the Switch change listener
        Default icon pack will turn the switch off
        Selected icon pack will turn the switch on
        The switch will tell Smart Dock Preference Listener to reload the icons
         */
       // super.onClick();

        Set<String> cleanedNameList = new LinkedHashSet<>(iconNameList);
        String[] newNameList = cleanedNameList.toArray(new String[0]);

        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle("Select Icon Pack");
        dialog.setItems(newNameList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(iconPackageList.get(item).equals(mContext.getPackageName())){
                    myEdit.putString("icon_pack", "").apply();
                    setSummary("Default");
                    setIcon(iconParserUtilities.getPackageIcon(mContext.getPackageName()));
                    aSwitch.setChecked(false);
                }else{
                    myEdit.putString("icon_pack", iconPackageList.get(item)).apply();
                    setSummary(iconParserUtilities.getPackageLabel(iconPackHelper.getIconPack(sharedPreferences)));
                    setIcon(iconParserUtilities.getPackageIcon(iconPackHelper.getIconPack(sharedPreferences)));
                    aSwitch.setChecked(true);
                }
                dialog.dismiss();
            }
        });
        AlertDialog alert = dialog.create();
        alert.show();
    }

}
