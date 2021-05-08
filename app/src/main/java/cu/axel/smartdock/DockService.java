package cu.axel.smartdock;

import android.accessibilityservice.*;
import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.preference.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.view.View.*;
import android.view.accessibility.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.AdapterView.*;
import java.io.*;
import java.util.*;
import android.app.ActivityManager.*;


public class DockService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private PackageManager pm;
	private SharedPreferences sp;
	private ActivityManager am;
	private ImageView backBtn,homeBtn,recentBtn,toggleBtn,splitBtn,powerBtn;
	private ListView recentsLv;
	private Button topRightCorner,bottomRightCorner;
	private LinearLayout dockLayout;
	private WindowManager wm;
	private boolean menuVisible;
	private LinearLayout menu;
	private WindowManager.LayoutParams layoutParams;
	private EditText searchEt;
	private ArrayAdapter<App> appAdapter;
	private ListView appsLv;
	private int launcherTaskId;

	@Override
	public void onAccessibilityEvent(AccessibilityEvent p1)
	{}

	@Override
	public void onInterrupt()
	{}

	//Handle keyboard shortcuts
	@Override
	protected boolean onKeyEvent(KeyEvent event)
	{
		if (event.getAction() == KeyEvent.ACTION_UP && event.isAltPressed())
		{
			if (event.getKeyCode() == KeyEvent.KEYCODE_L)
			{
				if (sp.getBoolean("pref_enable_lock_desktop", true))
					lockScreen();
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_P)
			{
				if (sp.getBoolean("pref_enable_open_settings", true))
					startActivity(new Intent(Settings.ACTION_SETTINGS));
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_T)
			{
				if (sp.getBoolean("pref_enable_open_terminal", false))
				{
					try
					{
						startActivity(new Intent(pm.getLaunchIntentForPackage(sp.getString("pref_terminal_package", "com.termux"))));
					}
					catch (Exception e)
					{
						Toast.makeText(this, "Oops can't launch terminal. Is it installed ?", 5000).show();
					}
				}

			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_A)
			{
				if (sp.getBoolean("pref_enable_expand_notifications", true))
					performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_K)
			{
				sendKeyEvent(KeyEvent.KEYCODE_SYSRQ);
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_M)
			{
				if (sp.getBoolean("pref_enable_open_music", true))
					sendKeyEvent(KeyEvent.KEYCODE_MUSIC);
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_B)
			{
				if (sp.getBoolean("pref_enable_open_browser", true))
					sendKeyEvent(KeyEvent.KEYCODE_EXPLORER);
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_D)
			{
				am.moveTaskToFront(launcherTaskId, 0);
			}
		}
		else if (event.getAction() == KeyEvent.ACTION_UP)
		{
			if (event.getKeyCode() == KeyEvent.KEYCODE_CTRL_RIGHT)
			{
				if (sp.getBoolean("pref_enable_ctrl_back", true))
				{
					performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
					return true;
				}
			}

			if (event.getKeyCode() == KeyEvent.KEYCODE_HOME)
			{
				if (sp.getBoolean("pref_enable_app_menu", false))
				{
					if (menuVisible)
					{
						hideMenu();
					}
					else
					{
						showMenu();
					}
					return true;
				}
			}

		}

		return super.onKeyEvent(event);
	}


	@Override
	public void onCreate()
	{
		super.onCreate();

		Toast.makeText(this, "Smart Dock started", 5000).show();

		pm = getPackageManager();
		am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(this);
		wm = (WindowManager) getSystemService(WINDOW_SERVICE);

		//Create the dock
		final HoverInterceptorLayout dock = (HoverInterceptorLayout) LayoutInflater.from(this).inflate(R.layout.dock, null);
		recentsLv = dock.findViewById(R.id.apps_lv);
		dockLayout = dock.findViewById(R.id.dock_layout);
		powerBtn = dock.findViewById(R.id.power_btn);


		backBtn = dock.findViewById(R.id.back_btn);
		homeBtn = dock.findViewById(R.id.home_btn);
		recentBtn = dock.findViewById(R.id.recents_btn);
		toggleBtn = dock.findViewById(R.id.toggle_btn);
		splitBtn = dock.findViewById(R.id.split_btn);


		dock.setOnHoverListener(new OnHoverListener(){

				@Override
				public boolean onHover(View p1, MotionEvent p2)
				{
					if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER)
					{
						showDock();
					}
					else if (p2.getAction() == MotionEvent.ACTION_HOVER_EXIT)
					{
						hideDock(500);
					}

					return false;
				}


			});


		recentsLv.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
				{
					AppTask appTask = (AppTask) p1.getItemAtPosition(p3);

					am.moveTaskToFront(appTask.getId(), 0);
				}


			});

		powerBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
				}


			});

		backBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
				}


			});

		homeBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
				}


			});
		recentBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
				}


			});

		splitBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
				}


			});
		toggleBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
				}


			});

		layoutParams = new WindowManager.LayoutParams();
		layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT; 
		layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		layoutParams.gravity = Gravity.LEFT;
		layoutParams.format = PixelFormat.TRANSLUCENT;
		layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
		}
		else
			layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;

		wm.addView(dock, layoutParams);

		//Hot corners
	    topRightCorner = new Button(this);
		topRightCorner.setBackgroundResource(R.drawable.corner_background);

		bottomRightCorner = new Button(this);
		bottomRightCorner.setBackgroundResource(R.drawable.corner_background);


		topRightCorner.setOnHoverListener(new OnHoverListener(){

				@Override
				public boolean onHover(View p1, MotionEvent p2)
				{
					if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER)
					{
						Handler handler=new Handler();
						handler.postDelayed(
							new Runnable(){

								@Override
								public void run()
								{
									if (topRightCorner.isHovered())
										performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
								}


							}, 500);

					}

					return false;
				}


			});

		bottomRightCorner.setOnHoverListener(new OnHoverListener(){

				@Override
				public boolean onHover(View p1, MotionEvent p2)
				{
					if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER)
					{
						Handler handler=new Handler();
						handler.postDelayed(
							new Runnable(){

								@Override
								public void run()
								{
									if (bottomRightCorner.isHovered())
										lockScreen();
								}


							}, 500);

					}
					return false;
				}


			});

		layoutParams.width = 2;
		layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
		wm.addView(topRightCorner, layoutParams);

		layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		wm.addView(bottomRightCorner, layoutParams);


		//App menu
		layoutParams.flags =  WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
		layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
		menu = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.apps_menu, null);
		appsLv = menu.findViewById(R.id.menu_applist_lv);


		appsLv.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
				{
					App app = (App) p1.getItemAtPosition(p3);
					try
					{
						startActivity(pm.getLaunchIntentForPackage(app.getPackagename()));
						hideMenu();
					}
					catch (Exception e)
					{

					}
				}


			});
		searchEt = menu.findViewById(R.id.menu_et);
		searchEt.addTextChangedListener(new TextWatcher(){

				@Override
				public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4)
				{}

				@Override
				public void onTextChanged(CharSequence p1, int p2, int p3, int p4)
				{}

				@Override
				public void afterTextChanged(Editable p1)
				{
					appAdapter.getFilter().filter(p1.toString());
				}

			});

		layoutParams.width = 300;
		layoutParams.height = 500;
		layoutParams.x = 10;
		layoutParams.y = 10;

		new UpdateAppMenuTask().execute();

		updateNavigationBar();

		showDock();
		hideDock(2000);

	}

	public void showDock()
	{
		updateRunningTasks();
		dockLayout.setVisibility(View.VISIBLE);
		dockLayout.animate().translationX(0).setInterpolator(new DecelerateInterpolator(2));
	}

	public void hideDock(int delay)
	{
		new Handler().postDelayed(new Runnable(){

				@Override
				public void run()
				{
					dockLayout.animate().translationX(-dockLayout.getWidth()).setInterpolator(new AccelerateInterpolator(2)).withEndAction(new Runnable(){

							@Override
							public void run()
							{
								dockLayout.setVisibility(View.GONE);
							}


						});
				}
			}, delay);
	}

	public void showMenu()
	{

		wm.addView(menu, layoutParams);
		new UpdateAppMenuTask().execute();
		menu.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
		menuVisible = true;

	}

	public void hideMenu()
	{

		menu.animate().translationY(-dockLayout.getHeight()).setInterpolator(new AccelerateInterpolator(2)).withEndAction(new Runnable(){

				@Override
				public void run()
				{
					searchEt.setText("");
					wm.removeView(menu);
				}


			});
		menuVisible = false;
	}	

	@Override
	public void onSharedPreferenceChanged(SharedPreferences p1, String p2)
	{
		updateNavigationBar();
		updateCorners();
	}

	@Override
	public void onDestroy()
	{
		sp.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	public ArrayList<App> getInstalledApps()
	{
		ArrayList<App> apps = new ArrayList<App>();

		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> appsInfo = pm.queryIntentActivities(intent, 0);

		for (ResolveInfo appInfo : appsInfo)
		{
			String label = pm.getApplicationLabel(appInfo.activityInfo.applicationInfo).toString();
			Drawable icon = pm.getApplicationIcon(appInfo.activityInfo.applicationInfo);
			String packageName = appInfo.activityInfo.packageName;

			apps.add(new App(label, packageName, icon));
		}

		Collections.sort(apps, new Comparator<App>(){

				@Override
				public int compare(App p1, App p2)
				{
					return p1.getName().compareToIgnoreCase(p2.getName());
				}


			});

		return apps;
	}

	public void updateRunningTasks()
	{
		List<RunningTaskInfo> tasksInfo = am.getRunningTasks(10);

		ArrayList<AppTask> appTasks = new ArrayList<AppTask>();

		for (ActivityManager.RunningTaskInfo taskInfo : tasksInfo)
		{
			try
			{
                //Exclude systemui, current launcher and other system apps from the tasklist
				if (taskInfo.baseActivity.getPackageName().contains("com.android.systemui") 
					|| taskInfo.baseActivity.getPackageName().contains("com.google.android.packageinstaller"))
					continue;

				if (taskInfo.baseActivity.getPackageName().contains(getDefaultLauncher()))
				{
					launcherTaskId = taskInfo.id;
					continue;
				}

				appTasks.add(new AppTask(taskInfo.id, taskInfo.topActivity.getShortClassName(), taskInfo.topActivity.getPackageName(), pm.getActivityIcon(taskInfo.topActivity)));
			}
			catch (PackageManager.NameNotFoundException e)
			{}
		}
		recentsLv.setAdapter(new AppTaskAdapter(DockService.this, appTasks));

	}
	public void updateNavigationBar()
	{
		if (sp.getBoolean("pref_enable_back", false))
		{
			backBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			backBtn.setVisibility(View.GONE);
		}
		if (sp.getBoolean("pref_enable_home", false))
		{
			homeBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			homeBtn.setVisibility(View.GONE);
		}
		if (sp.getBoolean("pref_enable_recents", false))
		{
			recentBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			recentBtn.setVisibility(View.GONE);
		}
		if (sp.getBoolean("pref_enable_split", false))
		{
			splitBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			splitBtn.setVisibility(View.GONE);
		}
		if (sp.getBoolean("pref_enable_toggle", false))
		{
			toggleBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			toggleBtn.setVisibility(View.GONE);
		}
	}


	public void updateCorners()
	{
		if (sp.getBoolean("pref_enable_top_right", false))
		{
			topRightCorner.setVisibility(View.VISIBLE);
		}
		else
		{
			topRightCorner.setVisibility(View.GONE);
		}

		if (sp.getBoolean("pref_enable_bottom_right", false))
		{
			bottomRightCorner.setVisibility(View.VISIBLE);
		}
		else
		{
			bottomRightCorner.setVisibility(View.GONE);
		}

	}

	public String getDefaultLauncher()
	{
		Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
		return pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName;
	}

	public void lockScreen()
	{

		DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		try
		{
			dpm.lockNow();
		}
		catch (SecurityException e)
		{
			Toast.makeText(this, "Device administrator permission required", 5000).show();
			startActivity(new Intent(this, MainActivity.class));
		}

	}

	public void sendKeyEvent(int keycode)
	{
		try
		{
			java.lang.Process proccess = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(proccess.getOutputStream());
			os.writeBytes("input keyevent " + keycode + "\n");
			os.flush();
			os.close();
		}
		catch (IOException e)
		{
			Toast.makeText(this, e.toString(), 5000).show();
		}
	}


	class UpdateAppMenuTask extends AsyncTask<Void, Void, ArrayList<App>>
	{
		@Override
		protected ArrayList<App> doInBackground(Void[] p1)
		{
			return getInstalledApps();
		}

		@Override
		protected void onPostExecute(ArrayList<App> result)
		{
			super.onPostExecute(result);

			if (appAdapter == null)
			{
				appAdapter = new AppAdapter(DockService.this, result);
				appsLv.setAdapter(appAdapter);
			}
			else
			{
				appAdapter.clear();
				appAdapter.addAll(result);
			}
		}




	}


}
