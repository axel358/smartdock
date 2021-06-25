package cu.axel.smartdock;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DockService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private PackageManager pm;
	private SharedPreferences sp;
	private ActivityManager am;
	private ImageView appsBtn,backBtn,homeBtn,recentBtn,splitBtn,powerBtn,wifiBtn,batteryBtn,volBtn,settingsBtn;
    private TextView notificationBtn,searchTv;
	private TextClock dateTv;
	private Button topRightCorner,bottomRightCorner;
	private LinearLayout dockLayout,menu,searchLayout;
	private WindowManager wm;
    private View appsSeparator;
	private boolean menuVisible;
	private WindowManager.LayoutParams layoutParams;
	private EditText searchEt;
	private ArrayAdapter<App> appAdapter;
	private GridView appsGv,tasksGv,favoritesGv;
	private boolean shouldHide = true;
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
                if (sp.getBoolean("pref_enable_app_menu", true))
				{
					toggleMenu(null);
					return true;
				}
			}

		}

		return super.onKeyEvent(event);
	}

    @Override
    protected void onServiceConnected()
    {
        super.onServiceConnected();

        //Create the dock
        HoverInterceptorLayout dock = (HoverInterceptorLayout) LayoutInflater.from(this).inflate(R.layout.dock, null);
        dockLayout = dock.findViewById(R.id.dock_layout);

        appsBtn = dock.findViewById(R.id.apps_btn);
        tasksGv = dock.findViewById(R.id.apps_lv);

        backBtn = dock.findViewById(R.id.back_btn);
        homeBtn = dock.findViewById(R.id.home_btn);
        recentBtn = dock.findViewById(R.id.recents_btn);
        splitBtn = dock.findViewById(R.id.split_btn);

        notificationBtn = dock.findViewById(R.id.notifications_btn);
        wifiBtn = dock.findViewById(R.id.wifi_btn);
        volBtn = dock.findViewById(R.id.volume_btn);
        batteryBtn = dock.findViewById(R.id.battery_btn);
        powerBtn = dock.findViewById(R.id.power_btn);
        dateTv = dock.findViewById(R.id.date_btn);


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


        tasksGv.setOnItemClickListener(new OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    AppTask appTask = (AppTask) p1.getItemAtPosition(p3);

                    am.moveTaskToFront(appTask.getId(), 0);
                }


            });


        notificationBtn.setOnClickListener(new OnClickListener(){

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

        powerBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
                }


            });

        dateTv.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    startActivity(pm.getLaunchIntentForPackage("com.android.deskclock"));
                }


            });
        dateTv.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1)
                {
                    startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
                    return true;
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


                            }, Integer.parseInt(sp.getString("pref_hot_corners_delay", "300")));

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


                            }, Integer.parseInt(sp.getString("pref_hot_corners_delay", "300")));

                    }
                    return false;
                }


            });

        updateCorners();

        layoutParams.width = 2;
        layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        wm.addView(topRightCorner, layoutParams);

        layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        wm.addView(bottomRightCorner, layoutParams);


        //App menu
        layoutParams.flags =  WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        layoutParams.gravity = Gravity.BOTTOM | Gravity.LEFT;

        menu = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.apps_menu, null);
        searchEt = menu.findViewById(R.id.menu_et);
        appsGv = menu.findViewById(R.id.menu_applist_lv);
        favoritesGv = menu.findViewById(R.id.fav_applist_lv);
        settingsBtn = menu.findViewById(R.id.settings_btn);
        searchLayout = menu.findViewById(R.id.search_layout);
        searchTv = menu.findViewById(R.id.search_tv);
        appsSeparator = menu.findViewById(R.id.apps_separator);


        appsGv.setOnItemClickListener(new OnItemClickListener(){

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

        appsGv.setOnItemLongClickListener(new OnItemLongClickListener(){

                @Override
                public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    final App app =(App) p1.getItemAtPosition(p3);
                    PopupMenu pmenu=new PopupMenu(new ContextThemeWrapper(DockService.this, R.style.PopupMenuTheme), p2);

                    Utils.setForceShowIcon(pmenu);

                    pmenu.inflate(R.menu.app_menu);

                    if (isPinned(app.getPackagename()))
                    {
                        pmenu.getMenu().add(0, 4, 0, "Unpin").setIcon(R.drawable.remove);
                    }
                    else
                    {
                        pmenu.getMenu().add(0, 3, 0, "Pin").setIcon(R.drawable.pin);
                    }


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
                                    case 3:
                                        pinApp(app.getPackagename());
                                        break;
                                    case 4:
                                        unpinApp(app.getPackagename());
                                        break;
                                }
                                return false;
                            }
                        });

                    pmenu.show();
                    return true;
                }
            });

        favoritesGv.setOnItemClickListener(new OnItemClickListener(){

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

        favoritesGv.setOnItemLongClickListener(new OnItemLongClickListener(){

                @Override
                public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    final App app =(App) p1.getItemAtPosition(p3);
                    PopupMenu pmenu=new PopupMenu(new ContextThemeWrapper(DockService.this, R.style.PopupMenuTheme), p2);

                    Utils.setForceShowIcon(pmenu);

                    pmenu.inflate(R.menu.app_menu);
                    pmenu.getMenu().add(0, 4, 0, "Unpin").setIcon(R.drawable.remove);


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
                                    case 4:
                                        unpinApp(app.getPackagename());
                                        break;
                                }
                                return false;
                            }
                        });

                    pmenu.show();
                    return true;
                }
            });

        searchTv.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + URLEncoder.encode(searchEt.getText().toString()))));
                    hideMenu();
                }
            });

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
                    if (p1.length() > 0)
                    {
                        searchLayout.setVisibility(View.VISIBLE);
                        searchTv.setText("Search for \"" + p1.toString() + "\" on Google");
                        hideFavorites();
                    }
                    else
                    {
                        searchLayout.setVisibility(View.GONE);
                        if (getFavoriteApps().size() > 0)
                            showFavorites();

                    }
                }

            });



        new UpdateAppMenuTask().execute();

        loadFavoriteApps();

        //TODO: Filter app menu click
        menu.setOnTouchListener(new OnTouchListener(){

                @Override
                public boolean onTouch(View p1, MotionEvent p2)
                {
                    if (p2.getAction() == p2.ACTION_OUTSIDE)
                    {
                        hideMenu();   
                    }
                    return false;
                }
            });

        updateNavigationBar();
        applyTheme();
        updateMenuIcon();

        //Watch for launcher visibility
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

        //Tell the launcher the service has connected
        sendBroadcast(new Intent(getPackageName() + ".CONNECTED"));

        registerReceiver(new BroadcastReceiver(){

                @Override
                public void onReceive(Context p1, Intent p2)
                {
                    int count = p2.getIntExtra("count", 0);
                    notificationBtn.setText(count + "");
                }
            }, new IntentFilter(getPackageName() + ".NOTIFICATION_COUNT_CHANGED"));



        showDock();
        hideDock(2000);

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
	}

	public void showDock()
	{
		updateRunningTasks();
		dockLayout.setVisibility(View.VISIBLE);
	}

	public void hideDock(int delay)
	{
        new Handler().postDelayed(new Runnable(){

                @Override
                public void run()
                {
                    //Toast.makeText(DockService.this, shouldHide + "2", 5000).show();
                    if (shouldHide)
                        dockLayout.setVisibility(View.GONE);

                }
            }, delay);}


    public void openDockSettings(View v)
    {
        startActivity(new Intent(this, MainActivity.class));
        hideMenu();
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
        layoutParams.width = Integer.parseInt(sp.getString("pref_app_menu_width", "600"));
        layoutParams.height = Integer.parseInt(sp.getString("pref_app_menu_height", "500"));
        layoutParams.x = Integer.parseInt(sp.getString("pref_app_menu_x", "2"));
		layoutParams.y = Integer.parseInt(sp.getString("pref_app_menu_y", "54"));
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
        if (p2.equals("pref_theme"))
            applyTheme();
        else if (p2.equals("pref_menu_icon_uri"))
            updateMenuIcon();
        else
        {
            updateNavigationBar();
            updateCorners();   
        }
	}


	public ArrayList<App> getInstalledApps()
	{
		ArrayList<App> apps = new ArrayList<App>();
        int gCount=0;
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> appsInfo = pm.queryIntentActivities(intent, 0);

        //TODO: Filter Google App
		for (ResolveInfo appInfo : appsInfo)
		{
			String label = appInfo.activityInfo.loadLabel(pm).toString();
			Drawable icon = appInfo.activityInfo.loadIcon(pm);
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

                if (taskInfo.topActivity.getClassName().equals(getPackageName() + ".LauncherActivity"))
                {
                    continue;
                }

                appTasks.add(new AppTask(taskInfo.id, taskInfo.topActivity.getShortClassName(), taskInfo.topActivity.getPackageName(), pm.getActivityIcon(taskInfo.topActivity)));
            }
            catch (PackageManager.NameNotFoundException e)
            {}
        }

        tasksGv.setAdapter(new AppTaskAdapter(DockService.this, appTasks));

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

    public void applyTheme()
    {
        switch (sp.getString("pref_theme", "dark"))
        {
            case "pref_theme_dark":
                dockLayout.setBackgroundResource(R.drawable.round_rect_solid_dark);
                menu.setBackgroundResource(R.drawable.round_rect_solid_dark);
                searchEt.setBackgroundResource(R.drawable.search_background_dark);
                wifiBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                volBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                powerBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                batteryBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                settingsBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                backBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                homeBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                recentBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                splitBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                break;

            case "pref_theme_black":
                dockLayout.setBackgroundResource(R.drawable.round_rect_solid_black);
                menu.setBackgroundResource(R.drawable.round_rect_solid_black);
                searchEt.setBackgroundResource(R.drawable.search_background_black);
                wifiBtn.setBackgroundResource(R.drawable.circle_solid_black);
                volBtn.setBackgroundResource(R.drawable.circle_solid_black);
                powerBtn.setBackgroundResource(R.drawable.circle_solid_black);
                batteryBtn.setBackgroundResource(R.drawable.circle_solid_black);
                settingsBtn.setBackgroundResource(R.drawable.circle_solid_black);
                backBtn.setBackgroundResource(R.drawable.circle_solid_black);
                homeBtn.setBackgroundResource(R.drawable.circle_solid_black);
                recentBtn.setBackgroundResource(R.drawable.circle_solid_black);
                splitBtn.setBackgroundResource(R.drawable.circle_solid_black);

                break;
            case "pref_theme_transparent":
                dockLayout.setBackgroundResource(R.drawable.round_rect_transparent);
                menu.setBackgroundResource(R.drawable.round_rect_transparent);
                searchEt.setBackgroundResource(R.drawable.search_background_transparent);
                wifiBtn.setBackgroundResource(R.drawable.circle_transparent);
                volBtn.setBackgroundResource(R.drawable.circle_transparent);
                powerBtn.setBackgroundResource(R.drawable.circle_transparent);
                batteryBtn.setBackgroundResource(R.drawable.circle_transparent);
                settingsBtn.setBackgroundResource(R.drawable.circle_transparent);
                backBtn.setBackgroundResource(R.drawable.circle_transparent);
                homeBtn.setBackgroundResource(R.drawable.circle_transparent);
                recentBtn.setBackgroundResource(R.drawable.circle_transparent);
                splitBtn.setBackgroundResource(R.drawable.circle_transparent);
                break;


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

    public void updateMenuIcon()
    {
        File path = new File(sp.getString("pref_menu_icon_uri", ""));
        if (path.exists())
            appsBtn.setImageBitmap(BitmapFactory.decodeFile(path.getAbsolutePath()));
        else
            appsBtn.setImageResource(R.drawable.apps);

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

    public void hideFavorites()
    {
        favoritesGv.setVisibility(View.GONE);
        appsSeparator.setVisibility(View.GONE);
    }

    public void showFavorites()
    {
        favoritesGv.setVisibility(View.VISIBLE);
        appsSeparator.setVisibility(View.VISIBLE);
    }

    public ArrayList<App> getFavoriteApps()
    {
        ArrayList<App> apps = new ArrayList<App>();

        try
        {
            String path=getFilesDir() + "/pinned.lst";
            BufferedReader br = new BufferedReader(new FileReader(path));
            String applist="";
            try
            {
                if ((applist = br.readLine()) != null)
                {
                    String[] applist2 = applist.split(" ");
                    for (String app:applist2)
                    {
                        try
                        {
                            ApplicationInfo appInfo=pm.getApplicationInfo(app, 0);
                            apps.add(new App(pm.getApplicationLabel(appInfo).toString(), app, pm.getApplicationIcon(app)));
                        }
                        catch (PackageManager.NameNotFoundException e)
                        {
                            Toast.makeText(DockService.this, e.toString(), 5000).show();
                        }   
                    }
                }

            }
            catch (IOException e)
            {} 
        }
        catch (FileNotFoundException e)
        {}

        return apps;

    }

    public void pinApp(String app)
    {
        try
        {
            String path=getFilesDir() + "/pinned.lst";
            BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));
            bw.write(app + " ");
            bw.close();
        }
        catch (IOException e)
        {}
        loadFavoriteApps();

    }
    public void unpinApp(String app)
    {
        try
        {
            String path=getFilesDir() + "/pinned.lst";
            BufferedReader br = new BufferedReader(new FileReader(path));
            String applist="";

            if ((applist = br.readLine()) != null)
            {
                applist = applist.replace(app + " ", "");
                BufferedWriter bw = new BufferedWriter(new FileWriter(path, false));
                bw.write(applist);
                bw.close();
            }

        }
        catch (IOException e)
        {}
        loadFavoriteApps();   
    }

    public   boolean isPinned(String app)
    {
        try
        {
            String path=getFilesDir() + "/pinned.lst";
            BufferedReader br = new BufferedReader(new FileReader(path));
            String applist="";

            if ((applist = br.readLine()) != null)
            {
                return applist.contains(app);
            }

        }
        catch (IOException e)
        {}
        return    false; 
    }

    public void loadFavoriteApps()
    {
        ArrayList<App> apps = getFavoriteApps();
        if (apps.size() > 0)
        {
            showFavorites();
        }
        else
        {
            hideFavorites();
        }
        favoritesGv.setAdapter(new AppAdapter(this, apps));

    }

    @Override
    public void onDestroy()
    {
        //TODO: Unregister all receivers
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
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

            //TODO: Fix this shit
            appAdapter = new AppAdapter(DockService.this, result);
            appsGv.setAdapter(appAdapter);

			/*if (appAdapter == null)
             {

             }
             else
             {
             appAdapter.clear();
             appAdapter.addAll(result);
             appsGv.setAdapter(appAdapter);
             }*/
		}


	}


}
