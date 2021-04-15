package cu.axel.smartdock;
import android.accessibilityservice.*;
import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.view.accessibility.*;
import android.widget.*;
import android.widget.AdapterView.*;
import java.util.*;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.preference.*;
import android.provider.*;
import java.io.*;

public class DockService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener
{


	private PackageManager pm;
	private SharedPreferences sp;
	private ActivityManager am;
	private List<ActivityManager.RunningTaskInfo> tasksInfo;
	private ImageView backBtn,homeBtn,recentBtn,toggleBtn,splitBtn,powerBtn;
	private ListView recentsLv;
	private Button topRightCorner,bottomRightCorner;

	@Override
	public void onAccessibilityEvent(AccessibilityEvent p1)
	{
	}

	@Override
	public void onInterrupt()
	{}


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
				sendKeyEvent(KeyEvent.KEYCODE_MUSIC);
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_E)
			{
				sendKeyEvent(KeyEvent.KEYCODE_EXPLORER);
			}
		}
		else if (event.getAction() == KeyEvent.ACTION_UP)
		{
			if (event.getKeyCode() == KeyEvent.KEYCODE_CTRL_RIGHT)
			{
				if (sp.getBoolean("pref_enable_ctrl_back", true))
					performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
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
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

		final HoverInterceptorLayout dock = (HoverInterceptorLayout) LayoutInflater.from(this).inflate(R.layout.dock, null);
		recentsLv = dock.findViewById(R.id.apps_lv);
		final LinearLayout dockLayout = dock.findViewById(R.id.dock_layout);
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
						dockLayout.setVisibility(View.VISIBLE);
						updateRunningTasks();
					}
					else if (p2.getAction() == MotionEvent.ACTION_HOVER_EXIT)
					{
						new Handler().postDelayed(new Runnable(){

								@Override
								public void run()
								{
									dockLayout.setVisibility(View.GONE);
								}
							}, 500);
					}

					return false;
				}


			});


		recentsLv.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
				{
					AppTask appTask = (AppTask) p1.getItemAtPosition(p3);

					am.moveTaskToFront(appTask.id, 0);
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

		WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
		layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT; 
		layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		layoutParams.gravity = Gravity.LEFT;
		layoutParams.format = PixelFormat.TRANSLUCENT;
		layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
		layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
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
						performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
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
						lockScreen();
					}
					return false;
				}


			});

		layoutParams.width = 2;
		layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
		wm.addView(topRightCorner, layoutParams);

		layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		wm.addView(bottomRightCorner, layoutParams);

		updateNavigationBar();
		updateCorners();

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



	public void updateRunningTasks()
	{
		tasksInfo = am.getRunningTasks(10);
		ArrayList<AppTask> appTasks = new ArrayList<AppTask>();

		for (ActivityManager.RunningTaskInfo taskInfo : tasksInfo)
		{
			try
			{
				if (taskInfo.baseActivity.getPackageName().contains("com.android.systemui") 
					|| taskInfo.baseActivity.getPackageName().contains("com.google.android.packageinstaller")
					|| taskInfo.baseActivity.getPackageName().contains(getDefaultLauncher()))
					continue;
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

	class AppTaskAdapter extends ArrayAdapter<AppTask>
	{
		private Context context;
		public AppTaskAdapter(Context context, ArrayList<AppTask> appTasks)
		{
			super(context, R.layout.app_entry, appTasks);
			this.context = context;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = LayoutInflater.from(context).inflate(R.layout.app_entry, null);
			ImageView iconIv = view.findViewById(R.id.icon_iv);
			LinearLayout iconContainer = view.findViewById(R.id.icon_container);

			AppTask task = getItem(position);
			if (sp.getBoolean("pref_enable_circular_icons", true))
				iconContainer.setBackgroundResource(R.drawable.circle_solid);
			iconIv.setImageDrawable(task.getIcon());

			return view;
		}


	}


	class AppTask
	{
		private String className,packageName;
		private Drawable icon;
		private int id;

		public AppTask(int id, String className, String packageName, Drawable icon)
		{
			this.className = className;
			this.packageName = packageName;
			this.icon = icon;
			this.id = id;
		}

		public void setId(int id)
		{
			this.id = id;
		}

		public int getId()
		{
			return id;
		}

		public void setClassName(String className)
		{
			this.className = className;
		}

		public String getClassName()
		{
			return className;
		}

		public void setPackageName(String packageName)
		{
			this.packageName = packageName;
		}

		public String getPackageName()
		{
			return packageName;
		}

		public void setIcon(Drawable icon)
		{
			this.icon = icon;
		}

		public Drawable getIcon()
		{
			return icon;
		}
	}

}
