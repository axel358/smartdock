package cu.axel.smartdock;

import android.accessibilityservice.*;
import android.app.*;
import android.app.ActivityManager.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
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

import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.animation.*;
import android.net.wifi.*;
import android.media.*;


public class DockService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private PackageManager pm;
	private SharedPreferences sp;
	private ActivityManager am;
	private ImageView backBtn,homeBtn,recentBtn,toggleBtn,splitBtn,powerBtn,wifiBtn,volBtn;
	private GridView recentsLv;
	private TextClock notificationTv;
	private Button topRightCorner,bottomRightCorner;
	private LinearLayout dockLayout;
	private WindowManager wm;
	private boolean menuVisible;
	private LinearLayout menu;
	private WindowManager.LayoutParams layoutParams;
	private EditText searchEt;
	private ArrayAdapter<App> appAdapter;
	private GridView appsLv;
	private boolean shouldHide=true;

	private WifiManager wifiManager;

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
				//am.moveTaskToFront(launcherTaskId, 0);
				startActivity(new Intent(this, LauncherActivity.class));
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
					toggleMenu(null);
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
		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);


		//Create the dock
		HoverInterceptorLayout dock = (HoverInterceptorLayout) LayoutInflater.from(this).inflate(R.layout.dock, null);
		recentsLv = dock.findViewById(R.id.apps_lv);
		dockLayout = dock.findViewById(R.id.dock_layout);
		powerBtn = dock.findViewById(R.id.power_btn);
		notificationTv = dock.findViewById(R.id.notif_btn);
		wifiBtn = dock.findViewById(R.id.wifi_btn);
		volBtn = dock.findViewById(R.id.volume_btn);


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

		notificationTv.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
				}
			});
		wifiBtn.setOnLongClickListener(new OnLongClickListener(){

				@Override
				public boolean onLongClick(View p1)
				{
					startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
					return true;
				}
			});

		volBtn.setOnLongClickListener(new OnLongClickListener(){

				@Override
				public boolean onLongClick(View p1)
				{
					startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
					return true;
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
		layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT; 
		layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		layoutParams.gravity = Gravity.BOTTOM;
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


							}, 300);

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


							}, 300);

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
		layoutParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
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

		appsLv.setOnItemLongClickListener(new OnItemLongClickListener(){

				@Override
				public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4)
				{
					final App app =(App) p1.getItemAtPosition(p3);
					PopupMenu pmenu=new PopupMenu(new ContextThemeWrapper(DockService.this, R.style.PopupMenuTheme), p2);

					Utils.setForceShowIcon(pmenu);

					pmenu.inflate(R.menu.app_menu);


					pmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

							@Override
							public boolean onMenuItemClick(MenuItem p1)
							{
								switch (p1.getItemId())
								{
									case R.id.action_appinfo:
										startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + app.getPackagename())));
										hideMenu();
										break;
									case R.id.action_uninstall:
										startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + app.getPackagename())));
										hideMenu();
										break;
								}
								return false;
							}
						});

					pmenu.show();
					return true;
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

		layoutParams.width = 600;
		layoutParams.height = 500;
		layoutParams.x = 2;
		layoutParams.y = 54;

		new UpdateAppMenuTask().execute();

		updateNavigationBar();

		registerReceiver(new BroadcastReceiver(){

				@Override
				public void onReceive(Context p1, Intent p2)
				{
					switch (p2.getStringExtra("action"))
					{
						case "pause":
							shouldHide = true;
							hideDock(500);
							break;
						case "resume":
							shouldHide = false;
							showDock();
					}
				}
			}, new IntentFilter(getPackageName() + ".HOME"){});

		showDock();
		hideDock(2000);

	}

	public void showDock()
	{
		//Toast.makeText(this, dockLayout.getHeight() + "", 5000).show();
		updateDock();
		dockLayout.setVisibility(View.VISIBLE);
	}

	public void hideDock(int delay)
	{
		if (shouldHide)
		{
			new Handler().postDelayed(new Runnable(){

					@Override
					public void run()
					{
						if (!dockLayout.isHovered())
							dockLayout.setVisibility(View.GONE);

					}
				}, delay);}
	}

	public void toggleMenu(View v)
	{
		if (menuVisible)
		{
			hideMenu();
		}
		else
		{
			showMenu();

		}

	}

	public void showMenu()
	{
		wm.addView(menu, layoutParams);
		new UpdateAppMenuTask().execute();
		menu.setAlpha(0);
		menu.animate()
			.alpha(1)
			.setDuration(200)
			.setInterpolator(new AccelerateDecelerateInterpolator());

		menuVisible = true;

	}

	public void hideMenu()
	{
		searchEt.setText("");
		wm.removeView(menu);
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

	public void openDockSettings(View v)
	{
		startActivity(new Intent(this, MainActivity.class));
		hideMenu();
	}

	public void toggleWifi(View v)
	{
		if (wifiManager.isWifiEnabled())
		{
			wifiManager.setWifiEnabled(false);
			wifiBtn.setImageResource(R.drawable.wifi_off);
		}
		else
		{
			wifiManager.setWifiEnabled(true);
			wifiBtn.setImageResource(R.drawable.wifi_on);
		}
	}

	public void toggleVolume(View v)
	{
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);

	}


	public void updateDock()
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

				if (taskInfo.topActivity.getClassName().equals(getPackageName() + ".LauncherActivity"))
				{
					continue;
				}

				appTasks.add(new AppTask(taskInfo.id, taskInfo.topActivity.getShortClassName(), taskInfo.topActivity.getPackageName(), pm.getActivityIcon(taskInfo.topActivity)));
			}
			catch (PackageManager.NameNotFoundException e)
			{}
		}
		recentsLv.setAdapter(new AppTaskAdapter(DockService.this, appTasks));

		if (wifiManager.isWifiEnabled())
		{
			wifiBtn.setImageResource(R.drawable.wifi_on);
		}
		else
		{
			wifiBtn.setImageResource(R.drawable.wifi_off);
		}



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
		{
			toggleBtn.setVisibility(View.GONE);
		}
		if (sp.getBoolean("pref_enable_transparency", false))
		{
			dockLayout.setBackgroundResource(R.drawable.round_rect_transparent);
			menu.setBackgroundResource(R.drawable.round_rect_transparent);
		}
		else
		{
			dockLayout.setBackgroundResource(R.drawable.round_rect_solid);
			menu.setBackgroundResource(R.drawable.round_rect_solid);
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
