package cu.axel.smartdock.utils;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.preference.PreferenceManager;
import cu.axel.smartdock.models.DockApp;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cu.axel.smartdock.models.App;
import cu.axel.smartdock.models.AppTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUtils {
	public static final String PINNED_LIST = "pinned.lst";
	public static final String DOCK_PINNED_LIST = "dock_pinned.lst";
	public static final String DESKTOP_LIST = "desktop.lst";
	public static String currentApp = "";

	public enum WindowMode {
		STANDARD, PORTRAIT, MAXIMIZED, FULLSCREEN, TILED_TOP, TILED_LEFT, TILED_RIGHT, TILED_BOTTOM
	}

	public static ArrayList<App> getInstalledApps(PackageManager pm) {
		ArrayList<App> apps = new ArrayList<>();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> appsInfo = pm.queryIntentActivities(intent, 0);

		//TODO: Filter Google App
		for (ResolveInfo appInfo : appsInfo) {
			String label = appInfo.activityInfo.loadLabel(pm).toString();
			Drawable icon = appInfo.activityInfo.loadIcon(pm);
			String packageName = appInfo.activityInfo.packageName;

			apps.add(new App(label, packageName, icon));
		}

		Collections.sort(apps, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

		return apps;
	}

	public static ArrayList<App> getPinnedApps(Context context, PackageManager pm, String type) {
		ArrayList<App> apps = new ArrayList<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(context.getFilesDir(), type)));
			String applist;
			try {
				if ((applist = br.readLine()) != null) {
					String[] applist2 = applist.split(" ");
					for (String app : applist2) {
						try {
							ApplicationInfo appInfo = pm.getApplicationInfo(app, 0);
							apps.add(new App(pm.getApplicationLabel(appInfo).toString(), app,
									pm.getApplicationIcon(app)));
						} catch (PackageManager.NameNotFoundException e) {
							//app is no longer available, lets unpin it
							unpinApp(context, app, type);
						}
					}
				}

			} catch (IOException e) {
			}
		} catch (FileNotFoundException e) {
		}

		return apps;

	}

	public static void pinApp(Context context, String app, String type) {
		try {
			File file = new File(context.getFilesDir(), type);
			FileWriter fw = new FileWriter(file, true);
			fw.write(app + " ");
			fw.close();
		} catch (IOException e) {
		}

	}

	public static void unpinApp(Context context, String app, String type) {
		try {
			File file = new File(context.getFilesDir(), type);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String applist;

			if ((applist = br.readLine()) != null) {
				applist = applist.replace(app + " ", "");
				FileWriter fw = new FileWriter(file, false);
				fw.write(applist);
				fw.close();
			}

		} catch (IOException e) {
		}
	}

	public static void moveApp(Context context, String app, String type, int direction) {
		try {
			File file = new File(context.getFilesDir(), type);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String applist, what = "", with = "";

			if ((applist = br.readLine()) != null) {
				String[] apps = applist.split(" ");
				int pos = findInArray(app, apps);
				if (direction == 0 && pos > 0) {
					what = apps[pos - 1] + " " + app;
					with = app + " " + apps[pos - 1];
				} else if (direction == 1 && pos < apps.length - 1) {
					what = app + " " + apps[pos + 1];
					with = apps[pos + 1] + " " + app;
				}
				applist = applist.replace(what, with);
				FileWriter fw = new FileWriter(file, false);
				fw.write(applist);
				fw.close();
			}

		} catch (IOException e) {
		}
	}

	public static int findInArray(String key, String[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].contains(key))
				return i;
		}
		return -1;
	}

	public static boolean isPinned(Context context, String app, String type) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(context.getFilesDir(), type)));
			String applist;

			if ((applist = br.readLine()) != null) {
				return applist.contains(app);
			}

		} catch (IOException e) {
		}
		return false;
	}

	public static boolean isGame(PackageManager pm, String packageName) {
		try {
			ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				return info.category == ApplicationInfo.CATEGORY_GAME;
			} else {
				return (info.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME;
			}
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	public static String getCurrentLauncher(PackageManager pm) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return resolveInfo.activityInfo.packageName;
	}

	public static void setWindowMode(ActivityManager am, int taskId, int mode) {
		try {
			Method setWindowMode = am.getClass().getMethod("setTaskWindowingMode", int.class, int.class, boolean.class);
			setWindowMode.invoke(am, taskId, mode, false);
		} catch (Exception e) {
		}
	}

	public static ArrayList<AppTask> getRunningTasks(ActivityManager am, PackageManager pm, int max) {
		List<ActivityManager.RunningTaskInfo> tasksInfo = am.getRunningTasks(max);
		currentApp = tasksInfo.get(0).baseActivity.getPackageName();

		ArrayList<AppTask> appTasks = new ArrayList<>();
		for (ActivityManager.RunningTaskInfo taskInfo : tasksInfo) {
			try {
				//Exclude systemui, launcher and other system apps from the tasklist
				if (taskInfo.baseActivity.getPackageName().contains("com.android.systemui")
						|| taskInfo.baseActivity.getPackageName().contains("com.google.android.packageinstaller"))
					continue;

				//Hack to save Dock settings activity ftom being excluded
				if (!(taskInfo.topActivity.getClassName().equals("cu.axel.smartdock.activities.MainActivity")
						|| taskInfo.topActivity.getClassName().equals("cu.axel.smartdock.activities.DebugActivity"))
						&& taskInfo.topActivity.getPackageName().equals(AppUtils.getCurrentLauncher(pm)))
					continue;

				if (Build.VERSION.SDK_INT > 29) {
					try {
						Field isRunning = taskInfo.getClass().getField("isRunning");
						boolean running = isRunning.getBoolean(taskInfo);
						if (!running)
							continue;
					} catch (Exception e) {
					}
				}

				appTasks.add(
						new AppTask(taskInfo.id, pm.getActivityInfo(taskInfo.topActivity, 0).loadLabel(pm).toString(),
								taskInfo.topActivity.getPackageName(), pm.getActivityIcon(taskInfo.topActivity)));
			} catch (PackageManager.NameNotFoundException e) {
			}
		}

		return appTasks;
	}

	public static ArrayList<AppTask> getRecentTasks(Context context, int max) {
		UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
		long start = System.currentTimeMillis() - SystemClock.elapsedRealtime();
		List<UsageStats> usageStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start,
				System.currentTimeMillis());
		ArrayList<AppTask> appTasks = new ArrayList<>();

		Collections.sort(usageStats, (p1, p2) -> Long.compare(p2.getLastTimeUsed(), p1.getLastTimeUsed()));

		for (UsageStats stat : usageStats) {
			String app = stat.getPackageName();
			try {
				if (isLaunchable(context, app) && !app.equals(AppUtils.getCurrentLauncher(context.getPackageManager())))
					appTasks.add(new AppTask(-1, getPackageLabel(context, app), app,
							context.getPackageManager().getApplicationIcon(app)));
			} catch (PackageManager.NameNotFoundException e) {
			}

			if (appTasks.size() >= max)
				break;
		}

		return appTasks;
	}

	public static boolean isSystemApp(Context context, String app) {
		try {
			ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(app, 0);
			return (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	private static boolean isLaunchable(Context context, String app) {
		List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentActivities(
				new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app), 0);
		return resolveInfo.size() > 0;
	}

	public static void removeTask(ActivityManager am, int id) {
		try {
			Method removeTask = am.getClass().getMethod("removeTask", int.class);
			removeTask.invoke(am, id);
		} catch (Exception e) {
			Log.e("Dock", e + e.getCause().toString());
		}
	}

	public static String getPackageLabel(Context context, String packageName) {
		try {
			PackageManager pm = context.getPackageManager();
			ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
			return pm.getApplicationLabel(appInfo).toString();
		} catch (PackageManager.NameNotFoundException e) {
		}
		return "";
	}

	public static Drawable getAppIcon(Context context, String app) {
		try {
			return context.getPackageManager().getApplicationIcon(app);
		} catch (PackageManager.NameNotFoundException e) {
		}
		return context.getDrawable(android.R.drawable.sym_def_app_icon);
	}

	public static Rect makeLaunchBounds(Context context, String mode, int dockHeight, boolean secondary) {

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		int left = 0, top = 0, right = 0, bottom = 0;
		int deviceWidth = DeviceUtils.getDisplayMetrics(context, secondary).widthPixels;
		int deviceHeight = DeviceUtils.getDisplayMetrics(context, secondary).heightPixels;
		int statusHeight = DeviceUtils.getStatusBarHeight(context);
		int usableHeight = deviceHeight - dockHeight - statusHeight;
		float scaleFactor = Float.parseFloat(sp.getString("scale_factor", "1.0"));

		switch (mode) {
		case "standard":
			left = (int) (deviceWidth / (5 * scaleFactor));
			top = (int) ((usableHeight + statusHeight) / (7 * scaleFactor));
			right = deviceWidth - left;
			bottom = usableHeight + dockHeight - top;
			break;
		case "maximized":
			right = deviceWidth;
			bottom = usableHeight;
			break;
		case "portrait":
			left = deviceWidth / 3;
			top = (usableHeight) / 15;
			right = deviceWidth - left;
			bottom = usableHeight + dockHeight - top;
			break;
		case "tiled-left":
			right = deviceWidth / 2;
			bottom = usableHeight;
			break;
		case "tiled-top":
			right = deviceWidth;
			bottom = (usableHeight + statusHeight) / 2;
			break;
		case "tiled-right":
			left = deviceWidth / 2;
			right = deviceWidth;
			bottom = usableHeight;
			break;
		case "tiled-bottom":
			right = deviceWidth;
			top = (usableHeight + statusHeight) / 2;
			bottom = usableHeight + statusHeight;
			break;
		}

		Rect launchBounds = new Rect(left, top, right, bottom);

		return launchBounds;
	}

	public static void resizeTask(Context context, String mode, int taskId, int dockHeight, boolean secondary) {
		if (taskId < 0)
			return;
		Rect bounds = makeLaunchBounds(context, mode, dockHeight, secondary);
		DeviceUtils.runAsRoot("am task resize " + taskId + " " + bounds.left + " " + bounds.top + " " + bounds.right
				+ " " + bounds.bottom);
	}

	public static int containsTask(ArrayList<DockApp> apps, AppTask task) {
		for (int i = 0; i < apps.size(); i++) {
			if (apps.get(i).getPackageName().equals(task.getPackageName()))
				return i;
		}
		return -1;
	}

	public static String findStackId(Context context, int taskId) {
		String stackInfo = DeviceUtils.runAsRoot("am stack list");
		String regexPattern = "(?s)Stack id=(\\d+).*?taskId=" + taskId;
		Pattern pattern = Pattern.compile(regexPattern);
		Matcher matcher = pattern.matcher(stackInfo);

		if (matcher.find())
			return matcher.group(1);

		return null;
	}

	public static void removeStack(String stackId) {
		DeviceUtils.runAsRoot("am stack remove " + stackId);
	}

}
