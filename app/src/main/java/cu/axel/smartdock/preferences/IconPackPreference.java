package cu.axel.smartdock.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import androidx.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import java.util.ArrayList;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cu.axel.smartdock.icons.IconPackHelper;
import cu.axel.smartdock.icons.IconParserUtilities;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.R;

public class IconPackPreference extends Preference {

	private Context mContext;
	private IconParserUtilities iconParserUtilities;
	private SharedPreferences sp;
	private IconPackHelper iconPackHelper;

	/*
	These are all icon pack intents to date
	It could change in the future
	but by default, I don't think we even use these any more in icon packs
	but we support all icon packs to date (Long live Ander Web)
	 */
	private final String[] LAUNCHER_INTENTS = new String[] { "com.fede.launcher.THEME_ICONPACK",
			"com.anddoes.launcher.THEME", "com.teslacoilsw.launcher.THEME", "com.gau.go.launcherex.theme",
			"org.adw.launcher.THEMES", "org.adw.launcher.icons.ACTION_PICK_ICON",
			"net.oneplus.launcher.icons.ACTION_PICK_ICON" };

	public IconPackPreference(Context context) {
		super(context);
		setupPreference(context);
	}

	public IconPackPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupPreference(context);
	}

	public IconPackPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setupPreference(context);
	}

	private void setupPreference(Context context) {
		mContext = context;
		iconParserUtilities = new IconParserUtilities(mContext);
		iconPackHelper = new IconPackHelper();

		sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		setTitle(R.string.icon_pack);

		if (iconPackHelper.getIconPack(sp).equals("")) {
			setSummary(R.string.system);
		} else {
			setSummary(AppUtils.getPackageLabel(context, iconPackHelper.getIconPack(sp)));
		}

	}

	@Override
	protected void onClick() {
		PackageManager pm = mContext.getPackageManager();

		/*
		We manually add Smart Dock context as a default item so Smart Dock has a default item to rely on
		 */
		ArrayList<String> iconPackageList = new ArrayList<String>();
		ArrayList<String> iconNameList = new ArrayList<String>();
		iconPackageList.add(mContext.getPackageName());
		iconNameList.add(mContext.getString(R.string.system));

		List<ResolveInfo> launcherActivities = new ArrayList<>();
		/*
		Gather all the apps installed on the device
		filter all the icon pack packages to the list
		 */
		for (String i : LAUNCHER_INTENTS) {
			launcherActivities.addAll(pm.queryIntentActivities(new Intent(i), PackageManager.GET_META_DATA));
		}
		for (ResolveInfo ri : launcherActivities) {
			iconPackageList.add(ri.activityInfo.packageName);
			iconNameList.add(AppUtils.getPackageLabel(mContext, ri.activityInfo.packageName));
		}

		Set<String> cleanedNameList = new LinkedHashSet<>(iconNameList);
		String[] newNameList = cleanedNameList.toArray(new String[0]);

		MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(mContext);
		dialog.setTitle(R.string.select_icon_pack);
		dialog.setItems(newNameList, (DialogInterface d1, int item) -> {
			if (iconPackageList.get(item).equals(mContext.getPackageName())) {
				sp.edit().putString("icon_pack", "").commit();
				setSummary(R.string.system);
			} else {
				sp.edit().putString("icon_pack", iconPackageList.get(item)).commit();
				setSummary(AppUtils.getPackageLabel(mContext, iconPackHelper.getIconPack(sp)));
			}
		});
		dialog.show();
	}

}
