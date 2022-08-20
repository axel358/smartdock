package cu.axel.smartdock.services;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.usb.UsbManager;
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
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import cu.axel.smartdock.R;
import cu.axel.smartdock.activities.MainActivity;
import cu.axel.smartdock.adapters.AppAdapter;
import cu.axel.smartdock.adapters.DockAppAdapter;
import cu.axel.smartdock.db.DBHelper;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.models.AppTask;
import cu.axel.smartdock.receivers.BatteryStatsReceiver;
import cu.axel.smartdock.receivers.SoundEventsReceiver;
import cu.axel.smartdock.services.DockService;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.DeepShortcutManager;
import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.OnSwipeListener;
import cu.axel.smartdock.utils.Utils;
import cu.axel.smartdock.widgets.HoverInterceptorLayout;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import android.graphics.Color;
import cu.axel.smartdock.models.DockApp;
import android.util.Log;
import android.widget.ListView;
import cu.axel.smartdock.adapters.AppTaskAdaper;
import android.widget.Adapter;
import cu.axel.smartdock.utils.ColorUtils;
import cu.axel.smartdock.adapters.AppActionsAdapter;
import cu.axel.smartdock.models.Action;
import cu.axel.smartdock.adapters.AppShortcutAdapter;
import android.widget.SeekBar;
import android.media.AudioManager;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.net.wifi.WifiInfo;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class DockService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener,
		View.OnTouchListener, AppAdapter.AppRightClickListener, DockAppAdapter.TaskRightClickListener {
	private PackageManager pm;
	private SharedPreferences sp;
	private ActivityManager am;
	private ImageView appsBtn, backBtn, homeBtn, recentBtn, assistBtn, powerBtn, bluetoothBtn, wifiBtn, batteryBtn,
			volBtn, pinBtn;
	private TextView notificationBtn, searchTv;
	private Button topRightCorner, bottomRightCorner;
	private LinearLayout appMenu, searchLayout, powerMenu, audioPanel, wifiPanel, searchEntry;
	private RelativeLayout dockLayout;
	private WindowManager wm;
	private View appsSeparator;
	private boolean appMenuVisible, powerMenuVisible, isPinned, reflectionAllowed, audioPanelVisible,
			wifiPanelVisible, systemApp;
	private WindowManager.LayoutParams dockLayoutParams;
	private EditText searchEt;
	private GridView appsGv, favoritesGv, tasksGv;
	private WifiManager wifiManager;
	private BatteryStatsReceiver batteryReceiver;
	private SoundEventsReceiver soundEventsReceiver;
	private GestureDetector gestureDetector;
	private DBHelper db;
	private Handler dockHandler;
	private HoverInterceptorLayout dock;
	private BluetoothManager bm;
	private View dockTrigger;
	private ArrayList<App> pinnedApps;
    private TextClock dateTv;
    private long lastUpdate;
    private int maxApps;
    private Context context;

	@Override
	public void onCreate() {
		super.onCreate();

		reflectionAllowed = Build.VERSION.SDK_INT < 28 || Utils.allowReflection();
        
		db = new DBHelper(this);
		pm = getPackageManager();
		am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(this);
        context = DeviceUtils.getDisplayContext(this, true);
		wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
		wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		dockHandler = new Handler();

	}

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
        
        Utils.startupTime = System.currentTimeMillis();
        systemApp = AppUtils.isSystemApp(context, getPackageName());
        maxApps = Integer.parseInt(sp.getString("max_running_apps", "15"));

		//Create the dock
		dock = (HoverInterceptorLayout) LayoutInflater.from(context).inflate(R.layout.dock, null);
		dockLayout = dock.findViewById(R.id.dock_layout);
		dockTrigger = dock.findViewById(R.id.dock_trigger);

		appsBtn = dock.findViewById(R.id.apps_btn);
		tasksGv = dock.findViewById(R.id.apps_lv);

		backBtn = dock.findViewById(R.id.back_btn);
		homeBtn = dock.findViewById(R.id.home_btn);
		recentBtn = dock.findViewById(R.id.recents_btn);
		assistBtn = dock.findViewById(R.id.assist_btn);

		notificationBtn = dock.findViewById(R.id.notifications_btn);
		pinBtn = dock.findViewById(R.id.pin_btn);
		bluetoothBtn = dock.findViewById(R.id.bluetooth_btn);
		wifiBtn = dock.findViewById(R.id.wifi_btn);
		volBtn = dock.findViewById(R.id.volume_btn);
		batteryBtn = dock.findViewById(R.id.battery_btn);
		dateTv = dock.findViewById(R.id.date_btn);

		dock.setOnHoverListener(new OnHoverListener() {

			@Override
			public boolean onHover(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
					if (dockLayout.getVisibility() == View.GONE)
						showDock();
				} else if (p2.getAction() == MotionEvent.ACTION_HOVER_EXIT)
                    if (dockLayout.getVisibility() == View.VISIBLE){
					hideDock(Integer.parseInt(sp.getString("dock_hide_delay", "500")));
                    }

				return false;
			}

		});
		gestureDetector = new GestureDetector(context, new OnSwipeListener() {
			@Override
			public boolean onSwipe(Direction direction) {
				if (direction == Direction.up) {
					pinDock();
				}
				return true;
			}
		});
		dock.setOnTouchListener(this);
        dockLayout.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1) {
                    togglePin();
                    return true;
                }
            });
            
        appsBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    toggleAppMenu();
                }
            });
		appsBtn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View p1) {
				launchApp("standard",
						(new Intent(Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
				return true;
			}
		});
        assistBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    launchAssistant();
                }
            });
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
			}

		});

		homeBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
			}

		});
		recentBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
			}

		});

		recentBtn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View p1) {
				performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
				return true;
			}
		});

		tasksGv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> p1, View anchor, int p3, long p4) {
				DockApp app = (DockApp) p1.getItemAtPosition(p3);
				ArrayList<AppTask> tasks = app.getTasks();

				if (tasks.size() == 1) {
                    int taskId =tasks.get(0).getID();
                    if(taskId == -1)
                        launchApp(getDefaultLaunchMode(app.getPackageName()), app.getPackageName());
                    else
                        am.moveTaskToFront(taskId, 0);
                    
				} else if (tasks.size() > 1) {
					final View view = LayoutInflater.from(context).inflate(R.layout.task_list, null);
					WindowManager.LayoutParams lp = Utils.makeWindowParams(-2, -2);
					ColorUtils.applyMainColor(context, sp, view);
					lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
					lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
							| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

					lp.y = Utils.dpToPx(context, Integer.parseInt(sp.getString("app_menu_y", "2")))
							+ dockLayout.getMeasuredHeight();

					Rect rect = new Rect();
					anchor.getGlobalVisibleRect(rect);

					lp.x = rect.left;
					view.setOnTouchListener(new OnTouchListener() {

						@Override
						public boolean onTouch(View p1, MotionEvent p2) {
							if (p2.getAction() == MotionEvent.ACTION_OUTSIDE) {
								wm.removeView(view);
							}
							return false;
						}
					});
					ListView tasksLv = view.findViewById(R.id.tasks_lv);
					tasksLv.setAdapter(new AppTaskAdaper(context, tasks));

					tasksLv.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
							am.moveTaskToFront(((AppTask) p1.getItemAtPosition(p3)).getID(), 0);
							wm.removeView(view);
						}
					});

					wm.addView(view, lp);
				} else
					launchApp(getDefaultLaunchMode(app.getPackageName()), app.getPackageName());

				if (getDefaultLaunchMode(app.getPackageName()).equals("fullscreen")) {
                    if (isPinned){
                        unpinDock();
                    }
                } else {
                    if (!isPinned) {
                        pinDock();
                    }
                }
			}

		});

		tasksGv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> p1, View anchor, int p3, long p4) {

				final String app = ((DockApp) p1.getItemAtPosition(p3)).getPackageName();
				showTaskContextMenu(app, anchor);

				return true;
			}

		});

		notificationBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View p1) {
                if(sp.getBoolean("enable_notif_panel", true)){
				if (Utils.notificationPanelVisible)
					sendBroadcast(new Intent(getPackageName() + ".NOTIFICATION_PANEL").putExtra("action", "hide"));
				else {

					if (audioPanelVisible)
						hideAudioPanel();

					if (wifiPanelVisible)
						hideWiFiPanel();

					sendBroadcast(new Intent(getPackageName() + ".NOTIFICATION_PANEL").putExtra("action", "show"));
				}
                }else
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
			}
		});
		pinBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				togglePin();
			}

		});
        bluetoothBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    toggleBluetooth();
                }
            });
		bluetoothBtn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View p1) {
				launchApp("standard",
						(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
				return true;
			}
		});
        wifiBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    toggleWifi();
                }
            });
		wifiBtn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View p1) {
				launchApp("standard",
						(new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
				return true;
			}
		});
        volBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    toggleVolume();
                }
            });

		volBtn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View p1) {
				launchApp("standard",
						(new Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
				return true;
			}
		});

		if (Build.VERSION.SDK_INT > 21) {
			batteryBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View p1) {
					launchApp("standard", (new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
				}
			});
		}

		dateTv.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
                
				launchApp("standard", pm.getLaunchIntentForPackage(sp.getString("app_clock", "com.android.deskclock")));
			}

		});
		dateTv.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View p1) {
				launchApp("standard",
						(new Intent(Settings.ACTION_DATE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
				return true;
			}
		});

		dockLayoutParams = Utils.makeWindowParams(-1, -2);
		dockLayoutParams.screenOrientation = sp.getBoolean("lock_landscape", false) ? 0 : -1;
		dockLayoutParams.gravity = Gravity.BOTTOM;

		wm.addView(dock, dockLayoutParams);

		//Hot corners
		topRightCorner = new Button(context);
		topRightCorner.setBackgroundResource(R.drawable.corner_background);

		bottomRightCorner = new Button(context);
		bottomRightCorner.setBackgroundResource(R.drawable.corner_background);

		topRightCorner.setOnHoverListener(new OnHoverListener() {

			@Override
			public boolean onHover(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {

						@Override
						public void run() {
							if (topRightCorner.isHovered())
								performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
						}

					}, Integer.parseInt(sp.getString("hot_corners_delay", "300")));
				}

				return false;
			}

		});

		bottomRightCorner.setOnHoverListener(new OnHoverListener() {

			@Override
			public boolean onHover(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {

						@Override
						public void run() {
							if (bottomRightCorner.isHovered())
								DeviceUtils.lockScreen(context);
						}

					}, Integer.parseInt(sp.getString("hot_corners_delay", "300")));

				}
				return false;
			}

		});

		updateCorners();

		WindowManager.LayoutParams cornersLayoutParams = Utils.makeWindowParams(Utils.dpToPx(context, 2), -2);
		cornersLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
		wm.addView(topRightCorner, cornersLayoutParams);

		cornersLayoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		wm.addView(bottomRightCorner, cornersLayoutParams);

		//App menu
		appMenu = (LinearLayout) LayoutInflater.from(new ContextThemeWrapper(context, R.style.AppTheme_Dock)).inflate(R.layout.apps_menu, null);
		searchEntry = appMenu.findViewById(R.id.search_entry);
        searchEt = appMenu.findViewById(R.id.menu_et);
		powerBtn = appMenu.findViewById(R.id.power_btn);
		appsGv = appMenu.findViewById(R.id.menu_applist_lv);
		favoritesGv = appMenu.findViewById(R.id.fav_applist_lv);
		searchLayout = appMenu.findViewById(R.id.search_layout);
		searchTv = appMenu.findViewById(R.id.search_tv);
		appsSeparator = appMenu.findViewById(R.id.apps_separator);

		powerBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {

				if (sp.getBoolean("enable_power_menu", false)) {
					if (powerMenuVisible)
						hidePowerMenu();
					else
						showPowerMenu();
				} else
					performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);

				hideAppMenu();
			}

		});

		appsGv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
				App app = (App) p1.getItemAtPosition(p3);
				if (app.getPackageName().equals(getPackageName() + ".calc")) {
					ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					cm.setPrimaryClip(ClipData.newPlainText("results", app.getName()));
					Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
				} else
					launchApp(null, app.getPackageName());
			}

		});

		appsGv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> p1, View anchor, int p3, long p4) {
				final String app = ((App) p1.getItemAtPosition(p3)).getPackageName();

				if (!app.equals(getPackageName() + ".calc")) {
					showAppContextMenu(((App) p1.getItemAtPosition(p3)).getPackageName(), anchor);
				}
				return true;
			}
		});

		favoritesGv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
				App app = (App) p1.getItemAtPosition(p3);
				launchApp(null, app.getPackageName());
			}

		});

		favoritesGv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4) {
				showAppContextMenu(((App) p1.getItemAtPosition(p3)).getPackageName(), p2);
				return true;
			}
		});

		searchTv.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				launchApp("standard",
						new Intent(Intent.ACTION_VIEW,
								Uri.parse("https://www.google.com/search?q="
										+ URLEncoder.encode(searchEt.getText().toString())))
												.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			}
		});

		searchEt.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
			}

			@Override
			public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
			}

			@Override
			public void afterTextChanged(Editable p1) {
				AppAdapter appAdapter = (AppAdapter) appsGv.getAdapter();

				if (appAdapter != null)
					appAdapter.getFilter().filter(p1.toString());

				if (p1.length() > 1) {
					searchLayout.setVisibility(View.VISIBLE);
					searchTv.setText(
							getString(R.string.search_for) + " \"" + p1 + "\" " + getString(R.string.on_google));
					toggleFavorites(false);
				} else {
					searchLayout.setVisibility(View.GONE);
					toggleFavorites(AppUtils.getPinnedApps(context, pm, AppUtils.PINNED_LIST).size() > 0);
				}
			}

		});
		searchEt.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View p1, int p2, KeyEvent p3) {
				if (p3.getAction() == KeyEvent.ACTION_DOWN) {
					if (p2 == KeyEvent.KEYCODE_ENTER && searchEt.getText().toString().length() > 1) {
						launchApp("standard",
								new Intent(Intent.ACTION_VIEW,
										Uri.parse("https://www.google.com/search?q="
												+ URLEncoder.encode(searchEt.getText().toString())))
														.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						return true;
					}
				}
				return false;
			}
		});

		new UpdateAppMenuTask().execute();

		//TODO: Filter app button menu click only
		appMenu.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_OUTSIDE
						&& (p2.getY() < appMenu.getMeasuredHeight() || p2.getX() > appMenu.getMeasuredWidth())) {
					hideAppMenu();
				}
				return false;
			}
		});

		//Listen for launcher messages
		registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context p1, Intent p2) {
				switch (p2.getStringExtra("action")) {
				case "resume":
					pinDock();
					break;
				case "launch":
					launchApp(p2.getStringExtra("mode"), p2.getStringExtra("app"));
				}
			}
		}, new IntentFilter(getPackageName() + ".HOME") {
		});

		//Tell the launcher the service has connected
		sendBroadcast(new Intent(getPackageName() + ".SERVICE").putExtra("action", "CONNECTED"));

		//Register receivers
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context p1, Intent p2) {
				int count = p2.getIntExtra("count", 0);
				if (count > 0) {
					notificationBtn.setVisibility(View.VISIBLE);
					notificationBtn.setText(count + "");
				} else {
					notificationBtn.setVisibility(View.GONE);
					notificationBtn.setText("");
				}
			}
		}, new IntentFilter(getPackageName() + ".NOTIFICATION_COUNT_CHANGED"));

		batteryReceiver = new BatteryStatsReceiver(batteryBtn, sp);
		registerReceiver(batteryReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));

		soundEventsReceiver = new SoundEventsReceiver(sp);
		IntentFilter soundEventsFilter = new IntentFilter();
		soundEventsFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		soundEventsFilter.addAction(Intent.ACTION_POWER_CONNECTED);
		registerReceiver(soundEventsReceiver, soundEventsFilter);
		if (sp.getBoolean("allow_broadcasts", false)) {
			registerReceiver(new BroadcastReceiver() {

				@Override
				public void onReceive(Context p1, Intent p2) {
					toggleAppMenu();
				}

			}, new IntentFilter(getPackageName() + ".MENU"));
		}
        
        registerReceiver(new BroadcastReceiver(){

                @Override
                public void onReceive(Context p1, Intent p2) {
                    applyTheme();
                }
            }, new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED));

		//Run startup script
		if (sp.getBoolean("run_autostart", false))
			Utils.doAutostart(context);

		//Play startup sound
		if (sp.getBoolean("enable_startup_sound", false))
			DeviceUtils.playEventSound(context, "startup_sound");

		updateNavigationBar();
		updateQuickSettings();
        updateDockShape();
		applyTheme();
		updateMenuIcon();
		placeRunningApps();
		updateDockTrigger();
        

        if(sp.getBoolean("pin_dock", true))
		    pinDock();
        else
            Toast.makeText(context, R.string.start_message, 5000).show();

	}

	public ArrayList<Action> getAppActions(String app) {
		ArrayList<Action> actions = new ArrayList<Action>();
		if (DeepShortcutManager.hasHostPermission(context)) {
			if (DeepShortcutManager.getShortcuts(app, context).size() > 0)
				actions.add(new Action(R.drawable.ic_shortcuts, getString(R.string.shortcuts)));
		}
		actions.add(new Action(R.drawable.ic_manage, getString(R.string.manage)));
		actions.add(new Action(R.drawable.ic_launch_mode, getString(R.string.open_in)));
		if (AppUtils.isPinned(context, app, AppUtils.PINNED_LIST))
			actions.add(new Action(R.drawable.ic_remove_favorite, getString(R.string.remove)));
		else
			actions.add(new Action(R.drawable.ic_add_favorite, getString(R.string.to_favorites)));

		if (!AppUtils.isPinned(context, app, AppUtils.DESKTOP_LIST))
			actions.add(new Action(R.drawable.ic_add_to_desktop, getString(R.string.to_desktop)));

		return actions;
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent p1) {
		if (p1.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
			//getSource() might throw an exception
			try {
				if (p1.getSource() != null) {
					//Refresh the app list when the window state changes only it has been at least a second since last update
					//TODO: Filter events that also trigger window state change other than app switching
                    if(System.currentTimeMillis() - lastUpdate > 700)
                         //Log.e("beaut", System.currentTimeMillis() -  lastUpdate + "");
					     updateRunningTasks();
				}
			} catch (Exception e) {
			}

		}
	}

	@Override
	public void onInterrupt() {
	}
    
    public void restart(){
        Toast.makeText(context, "Restarting...", 5000).show();
        context = DeviceUtils.getDisplayContext(this, true);
    }

	//Handle keyboard shortcuts
	@Override
	protected boolean onKeyEvent(KeyEvent event) {
		boolean isModifierPressed = false;
		switch (sp.getString("shortcut_key", "alt")) {
		case "57":
			isModifierPressed = event.isAltPressed();
			break;
		case "113":
			isModifierPressed = event.isCtrlPressed();
			break;
		case "3":
			isModifierPressed = event.isMetaPressed();
		}
		if (event.getAction() == KeyEvent.ACTION_UP && isModifierPressed) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_L && sp.getBoolean("enable_lock_desktop", true))
				DeviceUtils.lockScreen(context);
			else if (event.getKeyCode() == KeyEvent.KEYCODE_P && sp.getBoolean("enable_open_settings", true))
				launchApp("standard", new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			else if (event.getKeyCode() == KeyEvent.KEYCODE_T && sp.getBoolean("enable_open_terminal", false))
				launchApp("standard", sp.getString("app_terminal", "com.termux"));
			else if (event.getKeyCode() == KeyEvent.KEYCODE_N && sp.getBoolean("enable_expand_notifications", true))
				performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
			else if (event.getKeyCode() == KeyEvent.KEYCODE_K && sp.getBoolean("enable_take_screenshot", true))
				DeviceUtils.sendKeyEvent(KeyEvent.KEYCODE_SYSRQ);
            else if (event.getKeyCode() == KeyEvent.KEYCODE_W && sp.getBoolean("enable_toggle_pin", true))
                togglePin();
            else if (event.getKeyCode() == KeyEvent.KEYCODE_S)
                restart();
			else if (event.getKeyCode() == KeyEvent.KEYCODE_M && sp.getBoolean("enable_open_music", true))
				launchApp("standard", sp.getString("app_music", "com.termux"));
			else if (event.getKeyCode() == KeyEvent.KEYCODE_B && sp.getBoolean("enable_open_browser", true))
				launchApp("standard", sp.getString("app_browser", ""));
            else if (event.getKeyCode() == KeyEvent.KEYCODE_A && sp.getBoolean("enable_open_assist", true))
                launchApp("standard", sp.getString("app_assistant", ""));
            else if (event.getKeyCode() == KeyEvent.KEYCODE_R && sp.getBoolean("enable_open_rec", true))
                launchApp("standard", sp.getString("app_rec", ""));    
			else if (event.getKeyCode() == KeyEvent.KEYCODE_D)
				startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			else if (event.getKeyCode() == KeyEvent.KEYCODE_O) {
                if(Build.VERSION.SDK_INT < 24){
				InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				im.showInputMethodPicker();
                }else{
                    AccessibilityService.SoftKeyboardController kc = getSoftKeyboardController();
                    int mode = kc.getShowMode();
                }
			} else if (event.getKeyCode() == KeyEvent.KEYCODE_F12)
				DeviceUtils.sotfReboot();
			else if (event.getKeyCode() == KeyEvent.KEYCODE_F4)
				AppUtils.removeTask(am, AppUtils.getRunningTasks(am, pm, maxApps).get(0).getID());
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
            int menuKey = Integer.parseInt(sp.getString("menu_key", "3"));
            int menuAltKey = menuKey == 3 ? KeyEvent.KEYCODE_META_LEFT : menuKey;
            
			if (event.getKeyCode() == KeyEvent.KEYCODE_CTRL_RIGHT && sp.getBoolean("enable_ctrl_back", true)) {
				performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
				return true;
			}
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && sp.getBoolean("enable_menu_recents", false)) {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                return true;
			}
			else if ((event.getKeyCode() == menuKey || event.getKeyCode() == menuAltKey)
					&& sp.getBoolean("enable_app_menu", true)) {
				toggleAppMenu();
				return true;
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_F10 && sp.getBoolean("enable_f10", true)) {
				if (true) {
					performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
					return true;
				}
			}
			//Still working on context
			/*else if (event.getKeyCode() == KeyEvent.KEYCODE_F9 && sp.getBoolean("enable_f9",false))
			{
			    AppUtils.setWindowMode(am,getRunningTasks().get(0).getId(),5);
			    return true;
			}*/
		}

		return super.onKeyEvent(event);
	}

	@Override
	public void onAppRightClick(String app, View view) {
		showAppContextMenu(app, view);
	}
    
    public void togglePin(){
        if (isPinned){
            unpinDock();
        }
        else
            pinDock();
    }

	@Override
	public void onTaskRightClick(String app, View view) {
		showTaskContextMenu(app, view);
	}

	public void showDock() {
		dockHandler.removeCallbacksAndMessages(null);
        loadPinnedApps();
		updateRunningTasks();
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_up);
        dockLayout.setVisibility(View.VISIBLE);
		dockLayout.startAnimation(anim);
	}

	public void pinDock() {
		isPinned = true;
		pinBtn.setImageResource(R.drawable.ic_pin);
        if (dockLayout.getVisibility() == View.GONE)
            showDock();

	}

	private void unpinDock() {
		pinBtn.setImageResource(R.drawable.ic_unpin);
		isPinned = false;
        if (dockLayout.getVisibility() == View.VISIBLE)
            hideDock(500);
	}

	private void launchApp(String mode, String packagename) {
		if (mode == null)
			mode = getDefaultLaunchMode(packagename);
		else {
			if (sp.getBoolean("remember_launch_mode", true))
				db.saveLaunchMode(packagename, mode);
		}
		launchApp(mode, pm.getLaunchIntentForPackage(packagename));
	}

	public String getDefaultLaunchMode(String packagename) {
		if (Build.VERSION.SDK_INT < 24)
			return "fullscreen";
		String mode;
		if (sp.getBoolean("remember_launch_mode", true) && (mode = db.getLaunchMode(packagename)) != null)
			return mode;
		else if (AppUtils.isGame(pm, packagename) && sp.getBoolean("launch_games_fullscreen", true))
			return "fullscreen";
		else
			return sp.getString("launch_mode", "standard");
	}

	private void launchApp(String mode, Intent intent) {
        ActivityOptions options = null;
        String animation = sp.getString("custom_animation", "system");
		
        if (animation.equals("none") || animation.equals("system")){
            
            if (Build.VERSION.SDK_INT >= 24)
                options = ActivityOptions.makeBasic();
                
            if(animation.equals("none"))
			    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        else {
            int animResId = 0;
            switch (sp.getString("custom_animation", "fade")) {
                case "fade":
                    animResId = R.anim.fade_in;
                    break;
                case "slide_up":
                    animResId = R.anim.slide_up;
                    break;
                case "slide_left":
                    animResId = R.anim.slide_left;
            }
            options = ActivityOptions.makeCustomAnimation(context, animResId, R.anim.fade_out);
		}

		if (Build.VERSION.SDK_INT < 24) {
			try {
				if (options == null)
					startActivity(intent);
				else
					startActivity(intent, options.toBundle());
				if (appMenuVisible)
					hideAppMenu();
                
                if(isPinned)
				    unpinDock();
			} catch (Exception e) {}
			return;
		}

		try {
			if (!reflectionAllowed)
				Utils.allowReflection();
			String methodName = Build.VERSION.SDK_INT >= 28 ? "setLaunchWindowingMode" : "setLaunchStackId";
			int windowMode;
			if (mode.equals("fullscreen"))
				windowMode = 1;
			else {
				int width = 0, height = 0, x = 0, y = 0;
                int deviceWidth = DeviceUtils.getDisplayMetrics(context, true).widthPixels;
                int deviceHeight = DeviceUtils.getDisplayMetrics(context, true).heightPixels;
                

				windowMode = Build.VERSION.SDK_INT >= 28 ? 5 : 2;
				if (mode.equals("standard")) {
					float scaleFactor = Float.parseFloat(sp.getString("scale_factor", "1.0"));
					x = (int) (deviceWidth / (5 * scaleFactor));
					y = (int) (deviceHeight / (6 * scaleFactor));
					width = deviceWidth - x;
					height = deviceHeight - y;

				} else if (mode.equals("maximized")) {
					width = deviceWidth;
					int statusHeight = sp.getBoolean("hide_status_bar", false) ? 0
							: DeviceUtils.getStatusBarHeight(context);
					height = deviceHeight - (statusHeight + dockLayout.getMeasuredHeight());
				} else if (mode.equals("portrait")) {
					x = deviceWidth / 3;
					y = deviceHeight / 15;
					width = deviceWidth - x;
					height = deviceHeight - y;
				}
				options.setLaunchBounds(new Rect(x, y, width, height));
                
                if(Build.VERSION.SDK_INT > 28)
                    options.setLaunchDisplayId(DeviceUtils.getSecondaryDisplay(this).getDisplayId());
			}

			Method method = ActivityOptions.class.getMethod(methodName, int.class);
			method.invoke(options, windowMode);
			context.startActivity(intent, options.toBundle());
			if (appMenuVisible)
				hideAppMenu();
                
            if (mode.equals("fullscreen")) {
                if (isPinned){
                    unpinDock();
                }
            } else {
                if (!isPinned) {
                    pinDock();
                }
            }
            
		} catch (Exception e) {
			Toast.makeText(context, R.string.something_wrong, 5000).show();
		}
	}

	public void hideDock(int delay) {
        dockHandler.removeCallbacksAndMessages(null);
		dockHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (!isPinned){
                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_down);
                    anim.setAnimationListener(new Animation.AnimationListener(){

                            @Override
                            public void onAnimationStart(Animation p1) {
                            }

                            @Override
                            public void onAnimationEnd(Animation p1) {
                                dockLayout.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation p1) {
                            }
                        });
                      dockLayout.startAnimation(anim);
                    }

			}
		}, delay);
	}

	public void setOrientation() {
		dockLayoutParams.screenOrientation = sp.getBoolean("lock_landscape", false) ? 0 : -1;
		wm.updateViewLayout(dock, dockLayoutParams);
	}

	public void toggleAppMenu() {
		if (appMenuVisible)
			hideAppMenu();
		else
			showAppMenu();
	}

	public void showAppMenu() {
		WindowManager.LayoutParams lp = null;
		if (sp.getBoolean("app_menu_fullscreen", false)) {
			int deviceWidth = DeviceUtils.getDisplayMetrics(context, true).widthPixels;
            int deviceHeight = DeviceUtils.getDisplayMetrics(context, true).heightPixels;
            lp = Utils.makeWindowParams(-1,
                                        deviceHeight - Utils.dpToPx(context,60) - DeviceUtils.getStatusBarHeight(context));
			//lp.x = Utils.dpToPx(context, 2);
			lp.y = Utils.dpToPx(context, 2) + dockLayout.getMeasuredHeight();

			favoritesGv.setNumColumns(10);
            appsGv.setVerticalSpacing(Utils.dpToPx(context, 45));
            favoritesGv.setVerticalSpacing(Utils.dpToPx(context, 45));
            int padding = Utils.dpToPx(context, 30);
            appMenu.setPadding(padding,padding,padding,padding);
            searchEntry.setGravity(Gravity.CENTER);
            searchLayout.setGravity(Gravity.CENTER);
			appsGv.setNumColumns(10);
            //appMenu.setBackgroundResource(R.drawable.rect);
            //ColorUtils.applyMainColor(context, sp, appMenu);

		} else {
			lp = Utils.makeWindowParams(Utils.dpToPx(context, Integer.parseInt(sp.getString("app_menu_width", "650"))),
					Utils.dpToPx(context, Integer.parseInt(sp.getString("app_menu_height", "540"))));
			lp.x = Utils.dpToPx(context, Integer.parseInt(sp.getString("app_menu_x", "2")));
			lp.y = Utils.dpToPx(context, Integer.parseInt(sp.getString("app_menu_y", "2")))
					+ dockLayout.getMeasuredHeight();
			favoritesGv.setNumColumns(Integer.parseInt(sp.getString("num_columns", "5")));
			appsGv.setNumColumns(Integer.parseInt(sp.getString("num_columns", "5")));
            appsGv.setVerticalSpacing(Utils.dpToPx(context, 5));
            favoritesGv.setVerticalSpacing(Utils.dpToPx(context, 5));
            int padding = Utils.dpToPx(context, 10);
            appMenu.setPadding(padding,padding,padding,padding);
            searchEntry.setGravity(Gravity.START);
            searchLayout.setGravity(Gravity.START);
            appMenu.setBackgroundResource(R.drawable.round_rect);
            ColorUtils.applyMainColor(context, sp, appMenu);
		}

		lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
				| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
		lp.gravity = Gravity.BOTTOM | Gravity.LEFT;

		ImageView avatarIv = appMenu.findViewById(R.id.avatar_iv);
		TextView userNameTv = appMenu.findViewById(R.id.user_name_tv);
        ColorUtils.applyColor(appsSeparator, ColorUtils.getMainColors(sp, this)[4]);

		avatarIv.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				showUserContextMenu(p1);
			}
		});
        
		wm.addView(appMenu, lp);

		//Load apps
		new UpdateAppMenuTask().execute();
		loadFavoriteApps();

		//Load user info
		String name = DeviceUtils.getUserName(context);
		if (name != null)
			userNameTv.setText(name);
		Bitmap icon = DeviceUtils.getUserIcon(context);
		if (icon != null)
			avatarIv.setImageBitmap(icon);

		appMenu.setAlpha(0);
		appMenu.animate().alpha(1).setDuration(200).setInterpolator(new AccelerateDecelerateInterpolator());

		searchEt.requestFocus();

		appMenuVisible = true;

	}

	public void hideAppMenu() {
		searchEt.setText("");
		wm.removeView(appMenu);
		appMenuVisible = false;
	}

	public void showAppContextMenu(final String app, View anchor) {

		final View view = LayoutInflater.from(context).inflate(R.layout.task_list, null);
		WindowManager.LayoutParams lp = Utils.makeWindowParams(-2, -2);
		ColorUtils.applyMainColor(context, sp, view);
		lp.gravity = Gravity.TOP | Gravity.LEFT;
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

		Rect rect = new Rect();
		anchor.getGlobalVisibleRect(rect);

		lp.x = rect.left;
		lp.y = rect.centerY();

		view.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_OUTSIDE) {
					wm.removeView(view);
				}
				return false;
			}
		});
		final ListView actionsLv = view.findViewById(R.id.tasks_lv);

		actionsLv.setAdapter(new AppActionsAdapter(context, getAppActions(app)));

		actionsLv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {

				if (p1.getItemAtPosition(p3) instanceof Action) {
					Action action = (Action) p1.getItemAtPosition(p3);
					if (action.getText().equals(getString(R.string.manage))) {
						ArrayList<Action> actions = new ArrayList<Action>();
						actions.add(new Action(R.drawable.ic_arrow_back, ""));
						actions.add(new Action(R.drawable.ic_info, getString(R.string.app_info)));
                        if(!AppUtils.isSystemApp(context, app) || sp.getBoolean("allow_sysapp_uninstall", false))
						    actions.add(new Action(R.drawable.ic_uninstall, getString(R.string.uninstall)));
						if (sp.getBoolean("allow_app_freeze", false))
							actions.add(new Action(R.drawable.ic_freeze, getString(R.string.freeze)));

						actionsLv.setAdapter(new AppActionsAdapter(context, actions));
					} else if (action.getText().equals(getString(R.string.shortcuts))) {
						actionsLv.setAdapter(new AppShortcutAdapter(context,
								DeepShortcutManager.getShortcuts(app, context)));
					} else if (action.getText().equals("")) {
						actionsLv.setAdapter(new AppActionsAdapter(context, getAppActions(app)));
					} else if (action.getText().equals(getString(R.string.open_in))) {
						ArrayList<Action> actions = new ArrayList<Action>();
						actions.add(new Action(R.drawable.ic_arrow_back, ""));
						actions.add(new Action(R.drawable.ic_standard, getString(R.string.standard)));
						actions.add(new Action(R.drawable.ic_maximized, getString(R.string.maximized)));
						actions.add(new Action(R.drawable.ic_portrait, getString(R.string.portrait)));
						actions.add(new Action(R.drawable.ic_fullscreen, getString(R.string.fullscreen)));
						actionsLv.setAdapter(new AppActionsAdapter(context, actions));
					} else if (action.getText().equals(getString(R.string.app_info))) {
						launchApp("standard", new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
								.setData(Uri.parse("package:" + app)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						wm.removeView(view);
					} else if (action.getText().equals(getString(R.string.uninstall))) {
                        if(AppUtils.isSystemApp(context, app))
                            DeviceUtils.runAsRoot("pm uninstall --user 0 " + app);
                        else
						    startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + app))
								.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						if (appMenuVisible)
							hideAppMenu();
						wm.removeView(view);
					} else if (action.getText().equals(getString(R.string.freeze))) {
						String status = DeviceUtils.runAsRoot("pm disable " + app);
						if (!status.equals("error"))
							Toast.makeText(context, R.string.app_frozen, Toast.LENGTH_SHORT).show();
						else
							Toast.makeText(context, R.string.something_wrong, Toast.LENGTH_SHORT).show();
						wm.removeView(view);
						if (appMenuVisible)
							hideAppMenu();
					} else if (action.getText().equals(getString(R.string.to_favorites))) {
						AppUtils.pinApp(context, app, AppUtils.PINNED_LIST);
						wm.removeView(view);
						loadFavoriteApps();
					} else if (action.getText().equals(getString(R.string.remove))) {
						AppUtils.unpinApp(context, app, AppUtils.PINNED_LIST);
						wm.removeView(view);
						loadFavoriteApps();
					} else if (action.getText().equals(getString(R.string.to_desktop))) {
						AppUtils.pinApp(context, app, AppUtils.DESKTOP_LIST);
						sendBroadcast(new Intent(getPackageName() + ".SERVICE").putExtra("action", "PINNED"));
						wm.removeView(view);
					} else if (action.getText().equals(getString(R.string.standard))) {
						wm.removeView(view);
						launchApp("standard", app);
					} else if (action.getText().equals(getString(R.string.maximized))) {
						wm.removeView(view);
						launchApp("maximized", app);
					} else if (action.getText().equals(getString(R.string.portrait))) {
						wm.removeView(view);
						launchApp("portrait", app);
					} else if (action.getText().equals(getString(R.string.fullscreen))) {
						wm.removeView(view);
						launchApp("fullscreen", app);
					}
				} else if (p1.getItemAtPosition(p3) instanceof ShortcutInfo) {
					ShortcutInfo shortcut = (ShortcutInfo) p1.getItemAtPosition(p3);
					wm.removeView(view);
					DeepShortcutManager.startShortcut(shortcut, context);
				}
			}
		});

		wm.addView(view, lp);
	}

	private void showTaskContextMenu(final String app, View anchor) {
		final View view = LayoutInflater.from(context).inflate(R.layout.pin_entry, null);
		WindowManager.LayoutParams lp = Utils.makeWindowParams(-2, -2);
		view.setBackgroundResource(R.drawable.round_rect);
		ColorUtils.applyMainColor(context, sp, view);
		lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

		lp.y = Utils.dpToPx(context, Integer.parseInt(sp.getString("app_menu_y", "2")))
				+ dockLayout.getMeasuredHeight();

		Rect rect = new Rect();
		anchor.getGlobalVisibleRect(rect);

		lp.x = rect.left;
		view.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_OUTSIDE) {
					wm.removeView(view);
				}
				return false;
			}
		});

		ImageView icon = view.findViewById(R.id.pin_entry_iv);
		ColorUtils.applySecondaryColor(context, sp, icon);
		TextView text = view.findViewById(R.id.pin_entry_tv);

		if (AppUtils.isPinned(context, app, AppUtils.DOCK_PINNED_LIST)) {
			icon.setImageResource(R.drawable.ic_unpin);
			text.setText(R.string.unpin);
		}

		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				if (AppUtils.isPinned(context, app, AppUtils.DOCK_PINNED_LIST))
					AppUtils.unpinApp(context, app, AppUtils.DOCK_PINNED_LIST);
				else
					AppUtils.pinApp(context, app, AppUtils.DOCK_PINNED_LIST);

				loadPinnedApps();
				updateRunningTasks();
				wm.removeView(view);
			}
		});

		wm.addView(view, lp);
	}

	public void showUserContextMenu(View anchor) {
		final View view = LayoutInflater.from(context).inflate(R.layout.task_list, null);
		WindowManager.LayoutParams lp = Utils.makeWindowParams(-2, -2);
		ColorUtils.applyMainColor(context, sp, view);
		lp.gravity = Gravity.TOP | Gravity.LEFT;
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

		Rect rect = new Rect();
		anchor.getGlobalVisibleRect(rect);

		lp.x = rect.left;
		lp.y = rect.bottom;

		view.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_OUTSIDE) {
					wm.removeView(view);
				}
				return false;
			}
		});
		final ListView actionsLv = view.findViewById(R.id.tasks_lv);

		ArrayList<Action> actions = new ArrayList<Action>();
		actions.add(new Action(R.drawable.ic_users, getString(R.string.users)));
		actions.add(new Action(R.drawable.ic_user_folder, getString(R.string.files)));
		actions.add(new Action(R.drawable.ic_user_settings, getString(R.string.settings)));
		actions.add(new Action(R.drawable.ic_settings, getString(R.string.dock_settings)));

		actionsLv.setAdapter(new AppActionsAdapter(context, actions));

		actionsLv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
				Action action = (Action) p1.getItemAtPosition(p3);
				if (action.getText().equals(getString(R.string.users)))
					launchApp("standard",
							new Intent("android.settings.USER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				else if (action.getText().equals(getString(R.string.files)))
					launchApp("standard", sp.getString("app_files", "com.android.documentsui"));
				else if (action.getText().equals(getString(R.string.settings)))
					launchApp("standard", new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				else if (action.getText().equals(getString(R.string.dock_settings)))
					launchApp("standard", new Intent(context, MainActivity.class)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

				wm.removeView(view);
			}
		});
		wm.addView(view, lp);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences p1, String p2) {
        if (p2.startsWith("theme"))
			applyTheme();
		else if (p2.equals("menu_icon_uri"))
			updateMenuIcon();
		else if (p2.startsWith("icon_")|| p2.equals("tint_indicators")) {
			updateRunningTasks();
			loadFavoriteApps();
		} else if (p2.equals("lock_landscape"))
			setOrientation();
		else if (p2.equals("center_running_apps") ){
			placeRunningApps();
            updateRunningTasks();
            }
		else if (p2.equals("dock_activation_area"))
			updateDockTrigger();
		else if (p2.startsWith("enable_corner_"))
			updateCorners();
		else if (p2.startsWith("enable_nav_"))
			updateNavigationBar();
		else if (p2.startsWith("enable_qs_"))
			updateQuickSettings();
        else if(p2.equals("dock_square"))
            updateDockShape();
        else if(p2.equals("max_running_apps")){
            maxApps = Integer.parseInt(sp.getString("max_running_apps", "15"));
            updateRunningTasks();
        }
	}

	private void updateDockTrigger() {
		int height = Integer.parseInt(sp.getString("dock_activation_area", "10"));
		if (height < 1)
			height = 1;
		dockTrigger.getLayoutParams().height = Utils.dpToPx(context, height);
	}

	private void placeRunningApps() {
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		if (sp.getBoolean("center_running_apps", true)) {
			lp.addRule(RelativeLayout.CENTER_IN_PARENT);
		} else {
			lp.addRule(RelativeLayout.END_OF, R.id.nav_panel);
			lp.addRule(RelativeLayout.START_OF, R.id.notifications_btn);
		}
		tasksGv.setLayoutParams(lp);
	}

	private void loadPinnedApps() {
		pinnedApps = AppUtils.getPinnedApps(context, pm, AppUtils.DOCK_PINNED_LIST);
	}

	public int containsTask(ArrayList<DockApp> apps, AppTask task) {
		for (int i = 0; i < apps.size(); i++) {
			if (apps.get(i).getPackageName().equals(task.getPackageName()))
				return i;
		}
		return -1;
	}

	private void updateRunningTasks() {
        lastUpdate = System.currentTimeMillis();
		
        ArrayList<DockApp> apps = new ArrayList<DockApp>();

		for (App pinnedApp : pinnedApps) {
			apps.add(new DockApp(pinnedApp.getName(), pinnedApp.getPackageName(), pinnedApp.getIcon()));
		}

		//TODO: We can eliminate another for
        ArrayList<AppTask> tasks = systemApp? AppUtils.getRunningTasks(am, pm, maxApps) : AppUtils.getRecentTasks(context, maxApps);
        

		for (int j = 1; j <= tasks.size(); j++) {
			AppTask task = tasks.get(tasks.size() - j);
			int i = containsTask(apps, task);
			if (i != -1)
				apps.get(i).addTask(task);
			else
				apps.add(new DockApp(task));
		}

		tasksGv.getLayoutParams().width = Utils.dpToPx(context, 60) * apps.size();
		tasksGv.setAdapter(new DockAppAdapter(context, this, apps));

		//TODO: Move context outta here
		wifiBtn.setImageResource(wifiManager.isWifiEnabled() ? R.drawable.ic_wifi_on : R.drawable.ic_wifi_off);
		bluetoothBtn
				.setImageResource(bm.getAdapter().isEnabled() ? R.drawable.ic_bluetooth : R.drawable.ic_bluetooth_off);
	}
    
    private void updateDockShape(){
        dockLayout.setBackgroundResource(sp.getBoolean("dock_square", false)? R.drawable.rect : R.drawable.round_rect);
        ColorUtils.applyMainColor(context, sp, dockLayout);
    }

	public void updateNavigationBar() {
		backBtn.setVisibility(sp.getBoolean("enable_nav_back", true) ? View.VISIBLE : View.GONE);
		homeBtn.setVisibility(sp.getBoolean("enable_nav_home", true) ? View.VISIBLE : View.GONE);
		recentBtn.setVisibility(sp.getBoolean("enable_nav_recents", true) ? View.VISIBLE : View.GONE);
		assistBtn.setVisibility(sp.getBoolean("enable_nav_assist", false) ? View.VISIBLE : View.GONE);
	}

	public void updateQuickSettings() {
		bluetoothBtn.setVisibility(sp.getBoolean("enable_qs_bluetooth", false) ? View.VISIBLE : View.GONE);
		batteryBtn.setVisibility(sp.getBoolean("enable_qs_battery", false) ? View.VISIBLE : View.GONE);
        wifiBtn.setVisibility(sp.getBoolean("enable_qs_wifi", true) ? View.VISIBLE : View.GONE);
        pinBtn.setVisibility(sp.getBoolean("enable_qs_pin", true) ? View.VISIBLE : View.GONE);
        volBtn.setVisibility(sp.getBoolean("enable_qs_vol", true) ? View.VISIBLE : View.GONE);
        dateTv.setVisibility(sp.getBoolean("enable_qs_date", true) ? View.VISIBLE : View.GONE);
	}

	public void launchAssistant() {
		String assistant = sp.getString("app_assistant", "");
		if (!assistant.isEmpty())
			launchApp("standard", assistant);
		else {
			try {
				startActivity(new Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			} catch (ActivityNotFoundException e) {
			}
		}
	}


	public void toggleBluetooth() {
		try {
			if (bm.getAdapter().isEnabled()) {
				bluetoothBtn.setImageResource(R.drawable.ic_bluetooth_off);
				bm.getAdapter().disable();
			} else {
				bluetoothBtn.setImageResource(R.drawable.ic_bluetooth);
				bm.getAdapter().enable();
			}
		} catch (Exception e) {
		}

	}

	public void toggleWifi() {
		if (sp.getBoolean("enable_wifi_panel", false)) {
			if (wifiPanelVisible)
				hideWiFiPanel();
			else
				showWiFiPanel();
		} else {
			boolean enabled = wifiManager.isWifiEnabled();
			int icon = !enabled ? R.drawable.ic_wifi_on : R.drawable.ic_wifi_off;
			wifiBtn.setImageResource(icon);
			wifiManager.setWifiEnabled(!enabled);
		}

	}

	public void toggleVolume() {
		//DeviceUtils.toggleVolume(context);

		if (!audioPanelVisible)
			showAudioPanel();
		else
			hideAudioPanel();
	}

	public void hideAudioPanel() {
		wm.removeView(audioPanel);
		audioPanelVisible = false;
		audioPanel = null;
	}

	public void showAudioPanel() {

		if (Utils.notificationPanelVisible)
			sendBroadcast(new Intent(getPackageName() + ".NOTIFICATION_PANEL").putExtra("action", "hide"));

		if (wifiPanelVisible)
			hideWiFiPanel();

		final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		WindowManager.LayoutParams lp = Utils.makeWindowParams(Utils.dpToPx(context, 270), -2);
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
		lp.y = Utils.dpToPx(context, 2) + dockLayout.getMeasuredHeight();
		lp.x = Utils.dpToPx(context, 2);
		lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;

		audioPanel = (LinearLayout) LayoutInflater.from(new ContextThemeWrapper(context, R.style.AppTheme_Dock)).inflate(R.layout.audio_panel, null);

		audioPanel.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_OUTSIDE
						&& (p2.getY() < audioPanel.getMeasuredHeight() || p2.getX() < audioPanel.getX())) {
					hideAudioPanel();
				}
				return false;
			}
		});

		ImageView musicIcon = audioPanel.findViewById(R.id.ap_music_icon);
		SeekBar musicSb = audioPanel.findViewById(R.id.ap_music_sb);
		musicSb.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		musicSb.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
		musicSb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, p2, 0);
			}

			@Override
			public void onStartTrackingTouch(SeekBar p1) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar p1) {

			}
		});

		ColorUtils.applySecondaryColor(context, sp, musicIcon);
		ColorUtils.applyMainColor(context, sp, audioPanel);
		wm.addView(audioPanel, lp);
		audioPanelVisible = true;
	}

	public void hideWiFiPanel() {
		wm.removeView(wifiPanel);
		wifiPanelVisible = false;
		wifiPanel = null;
	}

	public void showWiFiPanel() {

		if (Utils.notificationPanelVisible)
			sendBroadcast(new Intent(getPackageName() + ".NOTIFICATION_PANEL").putExtra("action", "hide"));

		if (audioPanelVisible)
			hideAudioPanel();

		WindowManager.LayoutParams lp = Utils.makeWindowParams(Utils.dpToPx(context, 300), -2);
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
		lp.y = Utils.dpToPx(context, 2) + dockLayout.getMeasuredHeight();
		lp.x = Utils.dpToPx(context, 2);
		lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;

		wifiPanel = (LinearLayout) LayoutInflater.from(new ContextThemeWrapper(context, R.style.AppTheme_Dock)).inflate(R.layout.wifi_panel, null);

		wifiPanel.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_OUTSIDE
						&& (p2.getY() < wifiPanel.getMeasuredHeight() || p2.getX() < wifiPanel.getX())) {
					hideWiFiPanel();
				}
				return false;
			}
		});

		final TextView ssidTv = wifiPanel.findViewById(R.id.wp_ssid_tv);
		Switch wifiSwtch = wifiPanel.findViewById(R.id.wp_switch);
		Button selectBtn = wifiPanel.findViewById(R.id.wp_select_btn);
		final LinearLayout infoLayout = wifiPanel.findViewById(R.id.wp_info);

		selectBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				launchApp("standard",
						(new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
				hideWiFiPanel();
			}
		});

		wifiSwtch.setChecked(wifiManager.isWifiEnabled());

		wifiSwtch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton p1, boolean p2) {
				if (p2) {
					wifiManager.setWifiEnabled(true);
					wifiBtn.setImageResource(R.drawable.ic_wifi_on);
				} else {
					wifiManager.setWifiEnabled(false);
					ssidTv.setText(R.string.not_connected);
					wifiBtn.setImageResource(R.drawable.ic_wifi_off);
				}

			}
		});

		WifiInfo wi = wifiManager.getConnectionInfo();

		if (wifiManager.isWifiEnabled()) {
			infoLayout.setVisibility(View.VISIBLE);
			if (wi != null && wi.getNetworkId() != -1) {
				ssidTv.setText(wi.getSSID());
			}
		}

		registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context p1, Intent p2) {
				WifiInfo wi = wifiManager.getConnectionInfo();

				if (wifiManager.isWifiEnabled()) {
					infoLayout.setVisibility(View.VISIBLE);
					if (wi != null && wi.getNetworkId() != -1) {
						ssidTv.setText(wi.getSSID());
					} else
						ssidTv.setText(R.string.not_connected);
				} else
					infoLayout.setVisibility(View.GONE);

			}
		}, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

		ColorUtils.applyMainColor(context, sp, wifiPanel);

		wm.addView(wifiPanel, lp);
		wifiPanelVisible = true;

	}

	public void showPowerMenu() {
		WindowManager.LayoutParams layoutParams = Utils.makeWindowParams(Utils.dpToPx(context, 120),
				Utils.dpToPx(context, 400));
		layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
		layoutParams.x = Utils.dpToPx(context, 10);
		layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

		powerMenu = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.power_menu, null);

		powerMenu.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View p1, MotionEvent p2) {
				if (p2.getAction() == MotionEvent.ACTION_OUTSIDE) {
					hidePowerMenu();
				}
				return false;
			}
		});

		ImageButton powerOffBtn = powerMenu.findViewById(R.id.power_off_btn);
		ImageButton restartBtn = powerMenu.findViewById(R.id.restart_btn);
		ImageButton softRestartBtn = powerMenu.findViewById(R.id.soft_restart_btn);
		ImageButton lockBtn = powerMenu.findViewById(R.id.lock_btn);

		ColorUtils.applySecondaryColor(context, sp, powerOffBtn);
		ColorUtils.applySecondaryColor(context, sp, restartBtn);
		ColorUtils.applySecondaryColor(context, sp, softRestartBtn);
		ColorUtils.applySecondaryColor(context, sp, lockBtn);

		powerOffBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				hidePowerMenu();
				DeviceUtils.shutdown();
			}
		});

		restartBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				hidePowerMenu();
				DeviceUtils.reboot();
			}
		});

		softRestartBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				hidePowerMenu();
				DeviceUtils.sotfReboot();
			}
		});
		lockBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				hidePowerMenu();
				DeviceUtils.lockScreen(DockService.this);
			}
		});
		ColorUtils.applyMainColor(context, sp, powerMenu);
		wm.addView(powerMenu, layoutParams);
		topRightCorner.setVisibility(sp.getBoolean("enable_corner_top_right", false) ? View.VISIBLE : View.GONE);
		powerMenuVisible = true;
	}

	public void hidePowerMenu() {
		wm.removeView(powerMenu);
		powerMenuVisible = false;
		powerMenu = null;
	}

	public void applyTheme() {
		ColorUtils.applyMainColor(context, sp, dockLayout);
		ColorUtils.applyMainColor(context, sp, appMenu);
		ColorUtils.applySecondaryColor(context, sp, searchEntry);
		ColorUtils.applySecondaryColor(context, sp, backBtn);
		ColorUtils.applySecondaryColor(context, sp, homeBtn);
		ColorUtils.applySecondaryColor(context, sp, recentBtn);
		ColorUtils.applySecondaryColor(context, sp, assistBtn);
		ColorUtils.applySecondaryColor(context, sp, pinBtn);
		ColorUtils.applySecondaryColor(context, sp, bluetoothBtn);
		ColorUtils.applySecondaryColor(context, sp, wifiBtn);
		ColorUtils.applySecondaryColor(context, sp, volBtn);
		ColorUtils.applySecondaryColor(context, sp, powerBtn);
		ColorUtils.applySecondaryColor(context, sp, batteryBtn);
	}

	public void updateCorners() {
		topRightCorner.setVisibility(sp.getBoolean("enable_corner_top_right", false) ? View.VISIBLE : View.GONE);
		bottomRightCorner.setVisibility(sp.getBoolean("enable_corner_bottom_right", false) ? View.VISIBLE : View.GONE);
	}

	public void updateMenuIcon() {
		String iconUri = sp.getString("menu_icon_uri", "default");
		if (iconUri.equals("default"))
			appsBtn.setImageResource(R.drawable.ic_apps);
		else {
			try {
				Uri icon = Uri.parse(iconUri);
				if (icon != null)
					appsBtn.setImageURI(icon);
			} catch (Exception e) {
			}
		}

	}

	public void toggleFavorites(boolean visible) {
		favoritesGv.setVisibility(visible ? View.VISIBLE : View.GONE);
		appsSeparator.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	public void loadFavoriteApps() {
		ArrayList<App> apps = AppUtils.getPinnedApps(context, pm, AppUtils.PINNED_LIST);
		toggleFavorites(apps.size() > 0);
		favoritesGv.setAdapter(new AppAdapter(context, this, apps));

	}

	@Override
	public boolean onTouch(View p1, MotionEvent p2) {
		gestureDetector.onTouchEvent(p2);
		return true;
	}

	@Override
	public void onDestroy() {
		//TODO: Unregister all receivers
		sp.unregisterOnSharedPreferenceChangeListener(this);
		unregisterReceiver(batteryReceiver);
		unregisterReceiver(soundEventsReceiver);
		super.onDestroy();
	}

	class UpdateAppMenuTask extends AsyncTask<Void, Void, ArrayList<App>> {
		@Override
		protected ArrayList<App> doInBackground(Void[] p1) {
			return AppUtils.getInstalledApps(pm);
		}

		@Override
		protected void onPostExecute(ArrayList<App> result) {
			super.onPostExecute(result);

			//TODO: Implement efficent adapter
			appsGv.setAdapter(new AppAdapter(DockService.this, DockService.this, result));

		}
	}
}
