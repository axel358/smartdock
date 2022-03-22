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
import cu.axel.smartdock.adapters.AppTaskAdapter;
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

public class DockService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnTouchListener  
{
    private PackageManager pm;
    private SharedPreferences sp;
    private ActivityManager am;
    private ImageView appsBtn,backBtn,homeBtn,recentBtn,assistBtn,powerBtn,bluetoothBtn,wifiBtn,batteryBtn,volBtn,pinBtn,avatarIv;
    private TextView notificationBtn,searchTv,userNameTv;
    private TextClock dateTv;
    private Button topRightCorner,bottomRightCorner;
    private LinearLayout appMenu,searchLayout,powerMenu;
    private RelativeLayout dockLayout;
    private WindowManager wm;
    private View appsSeparator;
    private boolean appMenuVisible,powerMenuVisible,shouldHide = true,isPinned,reflectionAllowed;
    private WindowManager.LayoutParams dockLayoutParams;
    private EditText searchEt;
    private AppAdapter appAdapter;
    private GridView appsGv,favoritesGv,tasksGv;
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
    
    @Override
    public void onCreate()
    {
        super.onCreate();
        
        reflectionAllowed = Build.VERSION.SDK_INT < 28 || Utils.allowReflection();

        db = new DBHelper(this);
        pm = getPackageManager();
        am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        dockHandler = new Handler();

    }

    @Override
    protected void onServiceConnected()
    {
        super.onServiceConnected();

        //Create the dock
        dock = (HoverInterceptorLayout) LayoutInflater.from(this).inflate(R.layout.dock, null);
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


        dock.setOnHoverListener(new OnHoverListener(){

                @Override
                public boolean onHover(View p1, MotionEvent p2)
                {
                    if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER){
                        if(dockLayout.getVisibility() == View.GONE)
                            showDock();
                        }
                    else if (p2.getAction() == MotionEvent.ACTION_HOVER_EXIT)
                        hideDock(Integer.parseInt(sp.getString("dock_hide_delay", "500")));
                   
                     return false;
                }


            });
        gestureDetector = new GestureDetector(this, new OnSwipeListener(){
                @Override
                public boolean onSwipe(Direction direction)
                {
                    if (direction == Direction.up)
                    {
                        showDock();
                        pinDock();
                    }
                    return true;
                }
            });
        dock.setOnTouchListener(this);
        appsBtn.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1) {
                    launchApp("standard", (new Intent(Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
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

        recentBtn.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1) {
                    DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                    return true;
                }
            });
            

        tasksGv.setOnItemClickListener(new OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    AppTask appTask = (AppTask) p1.getItemAtPosition(p3);
                    ArrayList<Integer> taskIDs = appTask.getIds();
                    
                    if(taskIDs.size()>0)
                        am.moveTaskToFront(taskIDs.get(0), 0);
                    else
                        launchApp(getDefaultLaunchMode(appTask.getPackageName()),appTask.getPackageName()); 

                    if (getDefaultLaunchMode(appTask.getPackageName()).equals("fullscreen"))
                    {
                        if (isPinned) 
                            unpinDock();
                    }
                    else
                    {
                        if (!isPinned)
                        {
                            showDock();
                            pinDock();
                        } 
                    }
                }


            });
            
            tasksGv.setOnItemLongClickListener(new OnItemLongClickListener(){

                @Override
                public boolean onItemLongClick(AdapterView<?> p1, View anchor, int p3, long p4) {

                    final String app = ((AppTask) p1.getItemAtPosition(p3)).getPackageName();
                    
                    final View view = LayoutInflater.from(DockService.this).inflate(R.layout.pin_entry, null);
                    WindowManager.LayoutParams lp = Utils.makeWindowParams(-2,-2);
                    Utils.applyMainColor(sp, view);
                    lp.gravity=Gravity.BOTTOM|Gravity.LEFT;
                    lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                    
                    lp.y = Utils.dpToPx(DockService.this, Integer.parseInt(sp.getString("app_menu_y", "2"))) + dockLayout.getMeasuredHeight();
                    
                    Rect rect = new Rect();
                    anchor.getGlobalVisibleRect(rect);
                    
                    lp.x = rect.left;
                    view.setOnTouchListener(new OnTouchListener(){

                            @Override
                            public boolean onTouch(View p1, MotionEvent p2)
                            {
                                if (p2.getAction() == MotionEvent.ACTION_OUTSIDE)
                                {
                                    wm.removeView(view);
                                }
                                return false;
                            }
                        });
                        
                    ImageView icon = view.findViewById(R.id.pin_entry_iv);
                    Utils.applySecondaryColor(sp, icon);
                    TextView text = view.findViewById(R.id.pin_entry_tv);
                    
                    if(AppUtils.isPinned(DockService.this,app, AppUtils.DOCK_PINNED_LIST)){
                        icon.setImageResource(R.drawable.ic_unpin);
                        text.setText(R.string.unpin);
                    }
                    
                    view.setOnClickListener(new OnClickListener(){

                            @Override
                            public void onClick(View p1) {
                                if(AppUtils.isPinned(DockService.this,app, AppUtils.DOCK_PINNED_LIST))
                                    AppUtils.unpinApp(DockService.this,app, AppUtils.DOCK_PINNED_LIST);
                                 else
                                    AppUtils.pinApp(DockService.this,app, AppUtils.DOCK_PINNED_LIST);
                                    
                                 loadPinnedApps();
                                 updateRunningTasks();
                                 wm.removeView(view);
                            }
                        });

                    wm.addView(view, lp);


                return true;
            }
        });
            
        

        notificationBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View p1)
                {
                    //performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
                    if(Utils.notificationPanelVisible)
                        sendBroadcast(new Intent(getPackageName()+".NOTIFICATION_PANEL").putExtra("action","hide"));
                    else
                        sendBroadcast(new Intent(getPackageName()+".NOTIFICATION_PANEL").putExtra("action","show"));                        
                }
            });
        pinBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    if (isPinned)
                        unpinDock();
                    else
                        pinDock();
                }

            });
        pinBtn.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1) {
                    if(sp.getBoolean("tablet_mode", false)){
                        Utils.toggleBuiltinNavigation(sp.edit(), false);
                        sp.edit().putBoolean("app_menu_fullscreen", false).commit();
                        sp.edit().putBoolean("tablet_mode", false).commit();
                        Toast.makeText(DockService.this, R.string.tablet_mode_off, Toast.LENGTH_SHORT).show();
                    }else{
                        Utils.toggleBuiltinNavigation(sp.edit(), true);
                        sp.edit().putBoolean("app_menu_fullscreen", true).commit();
                        sp.edit().putBoolean("tablet_mode", true).commit();
                        Toast.makeText(DockService.this, R.string.tablet_mode_on, Toast.LENGTH_SHORT).show();
                    }
                    if(appMenuVisible)
                        hideAppMenu();
                    return true;
                }
            });
        bluetoothBtn.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1)
                {
                    launchApp("standard", (new Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                    return true;
                }
            });
        wifiBtn.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1)
                {
                    launchApp("standard", (new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                    return true;
                }
            });

        volBtn.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1)
                {
                    launchApp("standard", (new Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                    return true;
                }
            });
            
        if(Build.VERSION.SDK_INT>21){
            batteryBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    launchApp("standard", (new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                }
            });
            }

       
        dateTv.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    launchApp("standard", pm.getLaunchIntentForPackage("com.android.deskclock"));
                }


            });
        dateTv.setOnLongClickListener(new OnLongClickListener(){

                @Override
                public boolean onLongClick(View p1)
                {
                    launchApp("standard", (new Intent(Settings.ACTION_DATE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                    return true;
                }
            });


        dockLayoutParams = Utils.makeWindowParams(-1, -2);
        dockLayoutParams.screenOrientation = sp.getBoolean("lock_landscape",false) ? 0 : -1;
        dockLayoutParams.gravity = Gravity.BOTTOM;
        
        wm.addView(dock, dockLayoutParams);

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

                            }, Integer.parseInt(sp.getString("hot_corners_delay", "300")));
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
                                        DeviceUtils. lockScreen(DockService.this);
                                }

                            }, Integer.parseInt(sp.getString("hot_corners_delay", "300")));

                    }
                    return false;
                }

            });

        updateCorners();
        
        WindowManager.LayoutParams cornersLayoutParams = Utils.makeWindowParams(Utils.dpToPx(this, 2), -2);
        cornersLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        wm.addView(topRightCorner, cornersLayoutParams);

        cornersLayoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        wm.addView(bottomRightCorner, cornersLayoutParams);


        //App menu
        appMenu = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.apps_menu, null);
        searchEt = appMenu.findViewById(R.id.menu_et);
        powerBtn = appMenu.findViewById(R.id.power_btn);
        appsGv = appMenu.findViewById(R.id.menu_applist_lv);
        favoritesGv = appMenu.findViewById(R.id.fav_applist_lv);
        searchLayout = appMenu.findViewById(R.id.search_layout);
        searchTv = appMenu.findViewById(R.id.search_tv);
        appsSeparator = appMenu.findViewById(R.id.apps_separator);

        avatarIv = appMenu.findViewById(R.id.avatar_iv);
        userNameTv = appMenu.findViewById(R.id.user_name_tv);

        avatarIv.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    showUserContextMenu(p1);
                }
            });
            
        powerBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {

                    if(sp.getBoolean("enable_power_menu",false)){
                        if(powerMenuVisible)
                            hidePowerMenu();
                        else
                            showPowerMenu();
                    }
                    else
                        DockService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
                        
                   hideAppMenu();
                }


            });
        

        appsGv.setOnItemClickListener(new OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    App app = (App) p1.getItemAtPosition(p3);
                    if(app.getPackageName().equals(getPackageName()+".calc")){
                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("results", app.getName()));
                        Toast.makeText(DockService.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }
                    else
                    launchApp(null, app.getPackageName());
                }
                
            });

        appsGv.setOnItemLongClickListener(new OnItemLongClickListener(){

                @Override
                public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    App app = (App) p1.getItemAtPosition(p3);
                    if(!app.getPackageName().equals(getPackageName()+".calc"))                        
                        showAppContextMenu(((App) p1.getItemAtPosition(p3)).getPackageName(), p2);
                    return true;
                }
            });

        favoritesGv.setOnItemClickListener(new OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    App app = (App) p1.getItemAtPosition(p3);
                    launchApp(null, app.getPackageName());
                }


            });

        favoritesGv.setOnItemLongClickListener(new OnItemLongClickListener(){

                @Override
                public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    showAppContextMenu(((App) p1.getItemAtPosition(p3)).getPackageName(), p2);
                    return true;
                }
            });

        searchTv.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    launchApp("standard", new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + URLEncoder.encode(searchEt.getText().toString()))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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
                {    if (appAdapter != null)
                        appAdapter.getFilter().filter(p1.toString());
                    
                    if (p1.length() > 1)
                    {
                        searchLayout.setVisibility(View.VISIBLE);
                        searchTv.setText(getString(R.string.search_for)+" \"" + p1 + "\" "+getString(R.string.on_google));
                        toggleFavorites(false);
                    }
                    else
                    {
                        searchLayout.setVisibility(View.GONE);
                        toggleFavorites(AppUtils.getPinnedApps(DockService.this,pm, AppUtils.PINNED_LIST).size() > 0);
                    }
                }

            });
        searchEt.setOnKeyListener(new OnKeyListener(){

                @Override
                public boolean onKey(View p1, int p2, KeyEvent p3)
                {
                    if (p3.getAction() == KeyEvent.ACTION_DOWN)
                    {
                        if (p2 == KeyEvent.KEYCODE_ENTER && searchEt.getText().toString().length() > 1)
						{
                            launchApp("standard", new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + URLEncoder.encode(searchEt.getText().toString()))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            return true;
                        }
                    }
                    return false;
                }
            });

        new UpdateAppMenuTask().execute();

        //TODO: Filter app button menu click only
        appMenu.setOnTouchListener(new OnTouchListener(){

                @Override
                public boolean onTouch(View p1, MotionEvent p2)
                {
                    if (p2.getAction() == MotionEvent.ACTION_OUTSIDE && (p2.getY() < appMenu.getMeasuredHeight() || p2.getX() > appMenu.getMeasuredWidth()))
                    {
                        hideAppMenu();
                    }
                    return false;
                }
            });

        //Listen for launcher messages
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
                            break;
                        case "launch":
                            launchApp(p2.getStringExtra("mode"), p2.getStringExtra("app"));
                    }
                }
            }, new IntentFilter(getPackageName() + ".HOME"){});

        //Tell the launcher the service has connected
        sendBroadcast(new Intent(getPackageName() + ".SERVICE").putExtra("action", "CONNECTED"));

        //Register receivers
        registerReceiver(new BroadcastReceiver(){
                @Override
                public void onReceive(Context p1, Intent p2)
                {
                    int count = p2.getIntExtra("count", 0);
                    if(count > 0){
                        notificationBtn.setVisibility(View.VISIBLE);
                        notificationBtn.setText(count + "");
                    }else{
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
        if(sp.getBoolean("allow_broadcasts", false)){
        registerReceiver(new BroadcastReceiver(){

                @Override
                public void onReceive(Context p1, Intent p2) {
                    toggleAppMenu(null);
                }
                
        }, new IntentFilter(getPackageName()+".MENU"));
       }
        //Run startup script
        if(sp.getBoolean("run_autostart",false))
            Utils.doAutostart(this);
            
        //Play startup sound
        if(sp.getBoolean("enable_startup_sound",false))
           DeviceUtils.playEventSound(this,"startup_sound");

        updateNavigationBar();
        updateQuickSettings();
        applyTheme();
        updateMenuIcon();
        loadPinnedApps();
        placeRunningApps();
        updateDockTrigger();
        
        Toast.makeText(this, R.string.start_message, 5000).show();
         
        showDock();
        if (sp.getBoolean("pin_dock", false))
            pinDock();
        else
            hideDock(2000);

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent p1)
    {
        if (p1.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        {
            //getSource() might throw an exception
            try{
            if (p1.getSource() != null)
            {
                //Refresh the app list when the window state changes
                //TODO: Filter events that also trigger window state change other than app switching
                updateRunningTasks();   
            }
            }catch(Exception e){}

        }
    }

    @Override
    public void onInterrupt()
    {}

    //Handle keyboard shortcuts
    @Override
    protected boolean onKeyEvent(KeyEvent event)
    {
        boolean isModifierPressed = false;
        switch (sp.getString("shortcut_key", "alt"))
        {
            case "57":
                isModifierPressed = event.isAltPressed();
                break;
            case "113":
                isModifierPressed = event.isCtrlPressed();
                break;
            case "3":
                isModifierPressed = event.isMetaPressed();
        }
        if (event.getAction() == KeyEvent.ACTION_UP && isModifierPressed)
        {
            if (event.getKeyCode() == KeyEvent.KEYCODE_L && sp.getBoolean("enable_lock_desktop", true))
                    DeviceUtils.lockScreen(DockService.this);
            else if (event.getKeyCode() == KeyEvent.KEYCODE_P && sp.getBoolean("enable_open_settings", true))
                    launchApp("standard", new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            else if (event.getKeyCode() == KeyEvent.KEYCODE_T && sp.getBoolean("enable_open_terminal", false))
                    launchApp("standard", sp.getString("terminal_package", "com.termux"));                 
            else if (event.getKeyCode() == KeyEvent.KEYCODE_A && sp.getBoolean("enable_expand_notifications", true))
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
            else if (event.getKeyCode() == KeyEvent.KEYCODE_K)
                DeviceUtils.sendKeyEvent(KeyEvent.KEYCODE_SYSRQ);
            else if (event.getKeyCode() == KeyEvent.KEYCODE_M && sp.getBoolean("enable_open_music", true))
                    DeviceUtils.    sendKeyEvent(KeyEvent.KEYCODE_MUSIC);
            else if (event.getKeyCode() == KeyEvent.KEYCODE_B && sp.getBoolean("enable_open_browser", true))
                    DeviceUtils.    sendKeyEvent(KeyEvent.KEYCODE_EXPLORER);
            else if (event.getKeyCode() == KeyEvent.KEYCODE_D)
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            else if (event.getKeyCode() == KeyEvent.KEYCODE_O)
            {
                InputMethodManager im=(InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                im.showInputMethodPicker();
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_F12)
                DeviceUtils.sotfReboot();
        }
        else if (event.getAction() == KeyEvent.ACTION_UP)
        {
            if (event.getKeyCode() == KeyEvent.KEYCODE_CTRL_RIGHT && sp.getBoolean("enable_ctrl_back", false))
            {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    return true;
            }
            
            int menuKey=Integer.parseInt(sp.getString("menu_key", "3"));

            if (event.getKeyCode() == menuKey && sp.getBoolean("enable_app_menu", true))
            {
                    toggleAppMenu(null);
                    return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_F10  && sp.getBoolean("enable_f10", true))
            {
                    if (shouldHide)
                    { 
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                        return true;
                    }
            }
            //Still working on this
            /*else if (event.getKeyCode() == KeyEvent.KEYCODE_F9 && sp.getBoolean("enable_f9",false))
            {
                AppUtils.setWindowMode(am,getRunningTasks().get(0).getId(),5);
                return true;
            }*/
        }

        return super.onKeyEvent(event);
    }

    public void showDock()
    {
        dockHandler.removeCallbacksAndMessages(null);
        updateRunningTasks();
        dockLayout.setVisibility(View.VISIBLE);
    }

    public void pinDock()
    {
        isPinned = true;
        pinBtn.setImageResource(R.drawable.ic_pin);

    }
    private void unpinDock()
    {
        pinBtn.setImageResource(R.drawable.ic_unpin);
        isPinned = false;
        hideDock(500);
    }

    private void launchApp(String mode, String packagename)
    {
        if (mode == null)
            mode = getDefaultLaunchMode(packagename);
        else
        {
            if (sp.getBoolean("remember_launch_mode", true))
                db.saveLaunchMode(packagename, mode);
        }
        launchApp(mode, pm.getLaunchIntentForPackage(packagename));
    }

    public String getDefaultLaunchMode(String packagename)
    {
        if (Build.VERSION.SDK_INT < 24) 
            return "fullscreen";
        String mode;
        if (sp.getBoolean("remember_launch_mode", true) && (mode = db.getLaunchMode(packagename)) != null)
            return mode;
        else if (AppUtils.isGame(pm, packagename)&&sp.getBoolean("launch_games_fullscreen", true))
            return "fullscreen";
        else
            return sp.getString("launch_mode", "standard");
    }

    private void launchApp(String mode, Intent intent)
    {
        if(sp.getBoolean("disable_animations",false))
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        
        ActivityOptions options=null;
        int animResId=0;
        if(sp.getBoolean("enable_custom_animations",false) && !sp.getBoolean("disable_animations",false)){
            switch(sp.getString("custom_animation","fade")){
                case "fade":
                    animResId=R.anim.fade_in;
                    break;
                case "slide_up":
                    animResId=R.anim.slide_up;
                    break;
                case "slide_left":
                    animResId = R.anim.slide_left;
            }
            options = ActivityOptions.makeCustomAnimation(this,animResId,R.anim.fade_out);
        }
        else{
            if(Build.VERSION.SDK_INT >= 24)
                options=ActivityOptions.makeBasic();
            else
                options=null;
        }
        
        if (Build.VERSION.SDK_INT < 24)
        {
            try
            {
                if(options==null)
                    startActivity(intent);
                else
                    startActivity(intent,options.toBundle());
                if (appMenuVisible)
                    hideAppMenu();
                unpinDock();
            }
            catch (Exception e)
            {}
            return;
        }
                        
        try
        {
            if (!reflectionAllowed) Utils.allowReflection();
            String methodName = Build.VERSION.SDK_INT >= 28 ?"setLaunchWindowingMode": "setLaunchStackId";
            int windowMode;
            if (mode.equals("fullscreen"))
                windowMode = 1;    
            else
            {
                int width=0,height=0,x=0,y=0;
                int deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
                int deviceHeight  = Resources.getSystem().getDisplayMetrics().heightPixels;
                
                windowMode = Build.VERSION.SDK_INT >= 28 ? 5: 2;
                if (mode.equals("standard"))
                {
                    x = deviceWidth / 5;
                    y = deviceHeight / 6;
                    width = deviceWidth - x;
                    height = deviceHeight - y;

                }
                else if (mode.equals("maximized"))
                {
                    width = deviceWidth;
                    height = deviceHeight - (dockLayout.getMeasuredHeight() + DeviceUtils.getStatusBarHeight(this));
                }
                else if (mode.equals("portrait"))
                {
                    x = deviceWidth / 3;
                    y = deviceHeight / 15;
                    width = deviceWidth - x;
                    height = deviceHeight - y;
                }
                options.setLaunchBounds(new Rect(x, y, width, height));
            }

            Method method=ActivityOptions.class.getMethod(methodName, int.class);
            method.invoke(options, windowMode);
            startActivity(intent, options.toBundle());
            if (appMenuVisible)
                hideAppMenu();
            if (!mode.equals("fullscreen"))
            {
                showDock();
                pinDock();   
            }
            else
                unpinDock();
        }
        catch (Exception e)
        {
            Toast.makeText(this,e.toString(),5000).show();
            //Utils.saveLog(this,"app_launch",e.toString());
        }
    }

    public void hideDock(int delay)
    {
        dockHandler.postDelayed(new Runnable(){

                @Override
                public void run()
                {
                    if (shouldHide && !isPinned)
                        dockLayout.setVisibility(View.GONE);

                }
            }, delay);
    }
            
    public void setOrientation(){
        dockLayoutParams.screenOrientation = sp.getBoolean("lock_landscape",false) ? 0 : -1;
        wm.updateViewLayout(dock, dockLayoutParams);
    }

    public void toggleAppMenu(View v)
    {
        if (appMenuVisible)
            hideAppMenu();
        else
            showAppMenu();
    }

    public void showAppMenu()
    {
        WindowManager.LayoutParams appMenuLayoutParams = null;
        if(sp.getBoolean("app_menu_fullscreen", false)){
            int deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            int deviceHeight  = Resources.getSystem().getDisplayMetrics().heightPixels;
            appMenuLayoutParams = Utils.makeWindowParams(deviceWidth - Utils.dpToPx(this, 4), deviceHeight - Utils.dpToPx(this, 60) - DeviceUtils.getStatusBarHeight(this));
            appMenuLayoutParams.x = Utils.dpToPx(this, 2);
            appMenuLayoutParams.y = Utils.dpToPx(this, 2) + dockLayout.getMeasuredHeight();

            favoritesGv.setNumColumns(10);
            appsGv.setNumColumns(10);
            
        }else{
            appMenuLayoutParams = Utils.makeWindowParams(Utils.dpToPx(this, Integer.parseInt(sp.getString("app_menu_width", "650"))),Utils.dpToPx(this, Integer.parseInt(sp.getString("app_menu_height", "540"))));
            appMenuLayoutParams.x = Utils.dpToPx(this, Integer.parseInt(sp.getString("app_menu_x", "2")));
            appMenuLayoutParams.y = Utils.dpToPx(this, Integer.parseInt(sp.getString("app_menu_y", "2"))) + dockLayout.getMeasuredHeight();
            favoritesGv.setNumColumns(Integer.parseInt(sp.getString("num_columns", "5")));
            appsGv.setNumColumns(Integer.parseInt(sp.getString("num_columns", "5")));
            
        }
        
        appMenuLayoutParams.flags =  WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        appMenuLayoutParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        

        wm.addView(appMenu, appMenuLayoutParams);

        //Load apps
        new UpdateAppMenuTask().execute();
        loadFavoriteApps();

        //Load user info
        String name=DeviceUtils.getUserName(this);
        if (name != null)
            userNameTv.setText(name);
        Bitmap icon=DeviceUtils.getUserIcon(this);
        if (icon != null)
            avatarIv.setImageBitmap(icon);

        appMenu.setAlpha(0);
        appMenu.animate()
            .alpha(1)
            .setDuration(200)
            .setInterpolator(new AccelerateDecelerateInterpolator());
            
        searchEt.requestFocus();

        appMenuVisible = true;

    }

    public void hideAppMenu()
    {
        searchEt.setText("");
        wm.removeView(appMenu);
        appMenuVisible = false;
    }


    public void showAppContextMenu(final String app, View anchor)
    {
        PopupMenu pmenu = new PopupMenu(new ContextThemeWrapper(DockService.this, R.style.PopupMenuTheme), anchor);

        Utils.setForceShowIcon(pmenu);

        final DeepShortcutManager shortcutManager = new DeepShortcutManager(anchor.getContext());

        if(shortcutManager.hasHostPermission()) {
            new DeepShortcutManager(anchor.getContext()).addAppShortcutsToMenu(pmenu, app);
        }
        
        pmenu.inflate(R.menu.app_menu);
        
        if(sp.getBoolean("allow_app_freeze", false)){
            MenuItem manageMenu = pmenu.getMenu().findItem(R.id.action_manage);
            manageMenu.getSubMenu().add(0, 8, 0, R.string.freeze).setIcon(R.drawable.ic_freeze);
        }
        
        if (AppUtils.isPinned(this,app, AppUtils.PINNED_LIST))
            pmenu.getMenu().add(0, 4, 0, R.string.unpin).setIcon(R.drawable.ic_unpin);
        else
            pmenu.getMenu().add(0, 3, 0, R.string.pin).setIcon(R.drawable.ic_pin);
      
        if (!AppUtils.isPinned(this,app, AppUtils.DESKTOP_LIST))
            pmenu.getMenu().add(0, 5, 0, R.string.to_desktop).setIcon(R.drawable.ic_add_to_desktop);
            

        pmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

                @Override
                public boolean onMenuItemClick(MenuItem p1)
                {
                    switch (p1.getItemId())
                    {
                        case R.id.action_appinfo:
                            launchApp("standard", new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + app)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            break;
                        case R.id.action_uninstall:
                            startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + app)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            hideAppMenu();
                            break;
                        case 3:
                            AppUtils.pinApp(DockService.this, app, AppUtils.PINNED_LIST);
                            loadFavoriteApps();
                            break;
                        case 4:
                            AppUtils.unpinApp(DockService.this,app, AppUtils.PINNED_LIST);
                            loadFavoriteApps();
                            break;
                        case 5:
                            AppUtils.pinApp(DockService.this, app, AppUtils.DESKTOP_LIST);
                            sendBroadcast(new Intent(getPackageName() + ".SERVICE").putExtra("action", "PINNED"));
                            break;
                        case 7:
                            //do nothing
                            break;
                        case 8:
                            String status = DeviceUtils.runAsRoot("pm disable "+app);
                            if(!status.equals("error"))
                                Toast.makeText(DockService.this, R.string.app_frozen, Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(DockService.this, R.string.something_wrong, Toast.LENGTH_SHORT).show();
                            hideAppMenu();
                            break;
                        case R.id.action_launch_modes:
                            //do nothing
                            break;
                        case R.id.action_manage:
                            //do nothing
                            break;
                        case R.id.action_launch_standard:
                            launchApp("standard", app);
                            break;
                        case R.id.action_launch_maximized:
                            launchApp("maximized", app);
                            break;
                        case R.id.action_launch_portrait:
                            launchApp("portrait", app);
                            break;
                        case R.id.action_launch_fullscreen:
                            launchApp("fullscreen", app);
                            break;
                        default:
                            try {
                                ShortcutInfo shortcut = DeepShortcutManager.shortcutInfoMap.get(p1.getItemId());
                                if (shortcut != null) {
                                    shortcutManager.startShortcut(shortcut, shortcut.getId(), null);
                                }
                            } catch (Exception ignored) {
                                Toast.makeText(DockService.this, ignored +ignored.getMessage(),5000).show();
                            }
                            hideAppMenu();
                    }
                    return false;
                }
            });

        pmenu.show();

    }

    public void showUserContextMenu(View anchor)
    {
        PopupMenu pmenu=new PopupMenu(new ContextThemeWrapper(DockService.this, R.style.PopupMenuTheme), anchor);

        Utils.setForceShowIcon(pmenu);

        pmenu.inflate(R.menu.menu_user);
        pmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

                @Override
                public boolean onMenuItemClick(MenuItem p1)
                {
                    switch (p1.getItemId())
                    {
                        case R.id.action_users:
                            launchApp("standard", new Intent("android.settings.USER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            break;
                        case R.id.action_files:
                            launchApp("standard", "com.android.documentsui");
                            break;
                        case R.id.action_system_settings:
                            launchApp("standard", new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            break;
                        case R.id.action_dock_settings:
                            launchApp("standard", new Intent(DockService.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));    

                    }
                    return false;
                }
            });
        pmenu.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p1, String p2)
    {
        if (p2.startsWith("theme"))
            applyTheme();
        else if (p2.equals("menu_icon_uri"))
            updateMenuIcon();
        else if (p2.startsWith("icon_"))
        {
            updateRunningTasks();
            loadFavoriteApps();
        }
        else if (p2.equals("lock_landscape"))
            setOrientation();
        else if (p2.equals("center_running_apps"))
            placeRunningApps();
        else if(p2.equals("dock_activation_area"))
            updateDockTrigger();
        else if(p2.startsWith("enable_corner_"))
            updateCorners();
        else if(p2.startsWith("enable_nav_"))
            updateNavigationBar();   
        else if(p2.startsWith("enable_qs_"))
            updateQuickSettings();
    }
    
    private void updateDockTrigger(){
      int height = Integer.parseInt(sp.getString("dock_activation_area", "10"));
      if(height < 1) height = 1;
      dockTrigger.getLayoutParams().height = Utils.dpToPx(this, height);
    }

    private void placeRunningApps() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if(sp.getBoolean("center_running_apps", false)){
             lp.addRule(RelativeLayout.CENTER_IN_PARENT);
             lp.leftMargin=-120;
               }
        else{
            lp.addRule(RelativeLayout.END_OF, R.id.nav_panel);
            lp.addRule(RelativeLayout.START_OF, R.id.notifications_btn);
        }
        tasksGv.setLayoutParams(lp);
        updateRunningTasks();
    }
    
    private void loadPinnedApps(){
        pinnedApps = AppUtils.getPinnedApps(DockService.this,pm, AppUtils.DOCK_PINNED_LIST);
    }

    private void updateRunningTasks()
    {
        //TODO: This is shitty needs opt asap
        ArrayList<AppTask> apps = new ArrayList<AppTask>();
        
        for(App pinnedApp : pinnedApps){
            apps.add(new AppTask(-1, pinnedApp.getPackageName(), pinnedApp.getIcon()));
        }
        
        ArrayList<AppTask> runningTasks = AppUtils.getRunningTasks(am, pm);
        ArrayList<AppTask> runningTasksLeft = new ArrayList(runningTasks);
        
        for(AppTask app : apps){
            for(AppTask appTask : runningTasks){
                if(appTask.getPackageName().equals(app.getPackageName())){
                    app.addTask(appTask.getIds().get(0));
                    runningTasksLeft.remove(appTask);
                }
            }
        }
        
        apps.addAll(runningTasksLeft);
        
        tasksGv.getLayoutParams().width = Utils.dpToPx(this, 60) * apps.size();
        tasksGv.setAdapter(new AppTaskAdapter(DockService.this, apps));
        
        //TODO: Move this outta here
        wifiBtn.setImageResource(wifiManager.isWifiEnabled()? R.drawable.ic_wifi_on : R.drawable.ic_wifi_off);
        bluetoothBtn.setImageResource(bm.getAdapter().isEnabled()? R.drawable.ic_bluetooth : R.drawable.ic_bluetooth_off);
     }
    public void updateNavigationBar()
    {
        backBtn.setVisibility(sp.getBoolean("enable_nav_back", true)? View.VISIBLE : View.GONE);
        homeBtn.setVisibility(sp.getBoolean("enable_nav_home", true)? View.VISIBLE : View.GONE);
        recentBtn.setVisibility(sp.getBoolean("enable_nav_recents", true)? View.VISIBLE : View.GONE);
        assistBtn.setVisibility(sp.getBoolean("enable_nav_assist", false)? View.VISIBLE : View.GONE);   
     }
    
    public void updateQuickSettings(){
        bluetoothBtn.setVisibility(sp.getBoolean("enable_qs_bluetooth", false)? View.VISIBLE : View.GONE);
        batteryBtn.setVisibility(sp.getBoolean("enable_qs_battery", false)? View.VISIBLE : View.GONE);   
     }
    
    public void launchAssistant(View v){
        String assistant=sp.getString("custom_assist","");
        if(!assistant.isEmpty())
            launchApp("standard", assistant); 
        else{
            try{
                startActivity(new Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }catch(ActivityNotFoundException e){}
        }
    }
    
    public void toggleBluetooth(View v)
    {
        try{
        if(bm.getAdapter().isEnabled()){
            bluetoothBtn.setImageResource(R.drawable.ic_bluetooth_off);
            bm.getAdapter().disable();
        }else{
            bluetoothBtn.setImageResource(R.drawable.ic_bluetooth);
            bm.getAdapter().enable();
        }
        }catch(Exception e){}

    }

    public void toggleWifi(View v)
    {
        boolean enabled = wifiManager.isWifiEnabled();
        int icon= !enabled ?R.drawable.ic_wifi_on: R.drawable.ic_wifi_off;
        wifiBtn.setImageResource(icon);
        wifiManager.setWifiEnabled(!enabled);

    }

    public void toggleVolume(View v)
    {
        DeviceUtils.toggleVolume(this);
    }
    
    public void showPowerMenu(){
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = Utils.dpToPx(this,120); 
        layoutParams.height = Utils.dpToPx(this,400);
        layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.RIGHT;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.x = Utils.dpToPx(this,10);
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        layoutParams.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        
        powerMenu = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.power_menu,null);
        
        powerMenu.setOnTouchListener(new OnTouchListener(){

                @Override
                public boolean onTouch(View p1, MotionEvent p2)
                {
                    if (p2.getAction() == MotionEvent.ACTION_OUTSIDE)
                    {
                        hidePowerMenu();   
                    }
                    return false;
                }
            });
            
        ImageButton powerOffBtn = powerMenu.findViewById(R.id.power_off_btn);
        ImageButton restartBtn = powerMenu.findViewById(R.id.restart_btn);
        ImageButton softRestartBtn = powerMenu.findViewById(R.id.soft_restart_btn);
        ImageButton screenshotBtn = powerMenu.findViewById(R.id.screenshot_btn);
        
        powerOffBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    hidePowerMenu();
                    DeviceUtils.shutdown();
                }
            });
        
        restartBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    hidePowerMenu();
                    DeviceUtils.reboot();
                }
            });
        
        softRestartBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    hidePowerMenu();
                    DeviceUtils.sotfReboot();
                }
            });
        screenshotBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    hidePowerMenu();
                    DeviceUtils.sendKeyEvent(KeyEvent.KEYCODE_SYSRQ);
                }
            });
            Utils.applyMainColor(sp, powerMenu);
            wm.addView(powerMenu, layoutParams);
        topRightCorner.setVisibility( sp.getBoolean("enable_corner_top_right", false)? View.VISIBLE : View.GONE);
        powerMenuVisible = true;
    }
    
    public void hidePowerMenu(){
        wm.removeView(powerMenu);
        powerMenuVisible = false;
    }
    
    
    public void applyTheme()
    {       
        Utils.applyMainColor(sp,dockLayout);
        Utils.applyMainColor(sp,appMenu);
        Utils.applySecondaryColor(sp, searchEt);
        Utils.applySecondaryColor(sp, backBtn);
        Utils.applySecondaryColor(sp, homeBtn);
        Utils.applySecondaryColor(sp, recentBtn);
        Utils.applySecondaryColor(sp, assistBtn);
        Utils.applySecondaryColor(sp, pinBtn);
        Utils.applySecondaryColor(sp, bluetoothBtn);
        Utils.applySecondaryColor(sp, wifiBtn);
        Utils.applySecondaryColor(sp, volBtn);
        Utils.applySecondaryColor(sp, powerBtn);
        Utils.applySecondaryColor(sp, batteryBtn);
      }

    public void updateCorners()
    {
        topRightCorner.setVisibility( sp.getBoolean("enable_corner_top_right", false)? View.VISIBLE : View.GONE);
        bottomRightCorner.setVisibility( sp.getBoolean("enable_corner_bottom_right", false)? View.VISIBLE : View.GONE);    
    }

    public void updateMenuIcon()
    { 
        String iconUri=sp.getString("menu_icon_uri", "default");
        if (iconUri.equals("default"))
            appsBtn.setImageResource(R.drawable.ic_apps);
        else
        {
            try{
                Uri icon = Uri.parse(iconUri);
                if (icon != null)
                    appsBtn.setImageURI(icon);
                }catch(Exception e){}
        }

    }

    public void toggleFavorites(boolean visible)
    {
        favoritesGv.setVisibility(visible? View.VISIBLE : View.GONE);
        appsSeparator.setVisibility(visible? View.VISIBLE : View.GONE);
    }

    public void loadFavoriteApps()
    {
        ArrayList<App> apps = AppUtils.getPinnedApps(DockService.this,pm, AppUtils.PINNED_LIST);
        toggleFavorites(apps.size() > 0);
        favoritesGv.setAdapter(new AppAdapter(this, apps));

    }
    
    @Override
    public boolean onTouch(View p1, MotionEvent p2)
    {
        gestureDetector.onTouchEvent(p2);
        return true;
    }

    @Override
    public void onDestroy()
    {
        //TODO: Unregister all receivers
        sp.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(batteryReceiver);
        unregisterReceiver(soundEventsReceiver);
        super.onDestroy();
    }

    class UpdateAppMenuTask extends AsyncTask<Void, Void, ArrayList<App>>
    {
        @Override
        protected ArrayList<App> doInBackground(Void[] p1)
        {
            return AppUtils.getInstalledApps(pm);
        }

        @Override
        protected void onPostExecute(ArrayList<App> result)
        {
            super.onPostExecute(result);

            //TODO: Implement efficent adapter
            appAdapter = new AppAdapter(DockService.this, result);
            appsGv.setAdapter(appAdapter);

        }
    }
}
