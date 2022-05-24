package cu.axel.smartdock.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cu.axel.smartdock.R;
import cu.axel.smartdock.adapters.AppActionsAdapter;
import cu.axel.smartdock.icons.IconParserUtilities;
import cu.axel.smartdock.models.Action;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.services.DockService;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.ColorUtils;
import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import android.content.pm.ShortcutInfo;
import cu.axel.smartdock.utils.DeepShortcutManager;
import cu.axel.smartdock.adapters.AppShortcutAdapter;
import android.widget.Adapter;

public class LauncherActivity extends Activity {
	private LinearLayout backgroundLayout;
	private Button serviceBtn;
	private GridView appsGv;
	private EditText notesEt;
	private SharedPreferences sp;
	private float x, y;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		backgroundLayout = findViewById(R.id.ll_background);
		serviceBtn = findViewById(R.id.service_btn);
		appsGv = findViewById(R.id.desktop_apps_gv);
		notesEt = findViewById(R.id.notes_et);
		sp = PreferenceManager.getDefaultSharedPreferences(this);

		serviceBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				startActivity(new Intent(LauncherActivity.this, MainActivity.class));
			}
		});

		backgroundLayout.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View p1) {
				final View view = LayoutInflater.from(LauncherActivity.this).inflate(R.layout.task_list, null);
				WindowManager.LayoutParams lp = Utils.makeWindowParams(-2, -2);
				ColorUtils.applyMainColor(sp, view);
				lp.gravity = Gravity.TOP | Gravity.LEFT;
				lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
				lp.x = (int) x;
				lp.y = (int) y;

				final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

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
				actions.add(new Action(R.drawable.ic_wallpaper, getString(R.string.change_wallpaper)));
				actions.add(new Action(R.drawable.ic_fullscreen, getString(R.string.display_settings)));

				actionsLv.setAdapter(new AppActionsAdapter(LauncherActivity.this, actions));

				actionsLv.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
						Action action = (Action) p1.getItemAtPosition(p3);
						if (action.getText().equals(getString(R.string.change_wallpaper)))
							startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER),
									getString(R.string.change_wallpaper)));
						else if (action.getText().equals(getString(R.string.display_settings)))
							startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));

						wm.removeView(view);
					}
				});

				wm.addView(view, lp);
				return true;
			}
		});

		backgroundLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View p1, MotionEvent p2) {
				x = p2.getX();
				y = p2.getY();
				return false;
			}
		});

		appsGv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
				final App app = (App) p1.getItemAtPosition(p3);
				launchApp(null, app.getPackageName());
			}
		});

		appsGv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4) {
				showAppContextMenu(((App) p1.getItemAtPosition(p3)).getPackageName(), p2);
				return true;
			}
		});

		registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context p1, Intent p2) {
				String action = p2.getStringExtra("action");
				if (action.equals("CONNECTED")) {
					serviceBtn.setVisibility(View.GONE);
				} else if (action.equals("PINNED")) {
					loadDesktopApps();
				}
			}
		}, new IntentFilter(getPackageName() + ".SERVICE") {
		});
	}

	public void loadDesktopApps() {
		appsGv.setAdapter(
				new AppAdapterDesktop(this, AppUtils.getPinnedApps(this, getPackageManager(), AppUtils.DESKTOP_LIST)));
	}

	@Override
	protected void onResume() {
		super.onResume();
        
		sendBroadcastToService("resume");

		if (DeviceUtils.isAccessibilityServiceEnabled(this))
			serviceBtn.setVisibility(View.GONE);
		else
			serviceBtn.setVisibility(View.VISIBLE);
		loadDesktopApps();

		if (sp.getBoolean("show_notes", false)) {
			notesEt.setVisibility(View.VISIBLE);
			loadNotes();

		} else {
			notesEt.setVisibility(View.GONE);
		}
		appsGv.requestFocus();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (sp.getBoolean("show_notes", false))
			saveNotes();

	}

	@Override
	public void onBackPressed() {
	}

	public void loadNotes() {
		File notes = new File(getExternalFilesDir(null), "notes.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(notes));
			String line = "";
			String noteContent = "";
			while ((line = br.readLine()) != null) {
				noteContent += line + "\n";
			}
			br.close();
			notesEt.setText(noteContent);
		} catch (IOException e) {
		}

	}

	public void saveNotes() {
		String noteContent = notesEt.getText().toString();
		if (!noteContent.isEmpty()) {
			File notes = new File(getExternalFilesDir(null), "notes.txt");
			try {
				FileWriter fr = new FileWriter(notes);
				fr.write(noteContent);
				fr.close();
			} catch (IOException e) {
			}
		}
	}

	public void sendBroadcastToService(String action) {
		sendBroadcast(new Intent(getPackageName() + ".HOME").putExtra("action", action));
	}

	public void launchApp(String mode, String app) {
		sendBroadcast(new Intent(getPackageName() + ".HOME").putExtra("action", "launch").putExtra("mode", mode)
				.putExtra("app", app));
	}

	public ArrayList<Action> getAppActions(String app) {
		ArrayList<Action> actions = new ArrayList<Action>();
		if (DeepShortcutManager.hasHostPermission(this)) {
			if (DeepShortcutManager.getShortcuts(app, this).size() > 0)
				actions.add(new Action(R.drawable.ic_shortcuts, getString(R.string.shortcuts)));
		}
		actions.add(new Action(R.drawable.ic_manage, getString(R.string.manage)));
		actions.add(new Action(R.drawable.ic_launch_mode, getString(R.string.open_in)));
		actions.add(new Action(R.drawable.ic_remove_from_desktop, getString(R.string.remove)));

		return actions;
	}

	public void showAppContextMenu(final String app, View anchor) {
		final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		final View view = LayoutInflater.from(this).inflate(R.layout.task_list, null);
		WindowManager.LayoutParams lp = Utils.makeWindowParams(-2, -2);
		ColorUtils.applyMainColor(sp, view);
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

		actionsLv.setAdapter(new AppActionsAdapter(this, getAppActions(app)));

		actionsLv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
				if (p1.getItemAtPosition(p3) instanceof Action) {
					Action action = (Action) p1.getItemAtPosition(p3);
					if (action.getText().equals(getString(R.string.manage))) {
						ArrayList<Action> actions = new ArrayList<Action>();
						actions.add(new Action(R.drawable.ic_arrow_back, ""));
						actions.add(new Action(R.drawable.ic_info, getString(R.string.app_info)));
						actions.add(new Action(R.drawable.ic_uninstall, getString(R.string.uninstall)));
						if (sp.getBoolean("allow_app_freeze", false))
							actions.add(new Action(R.drawable.ic_freeze, getString(R.string.freeze)));

						actionsLv.setAdapter(new AppActionsAdapter(LauncherActivity.this, actions));
					} else if (action.getText().equals(getString(R.string.shortcuts))) {
						actionsLv.setAdapter(new AppShortcutAdapter(LauncherActivity.this,
								DeepShortcutManager.getShortcuts(app, LauncherActivity.this)));
					} else if (action.getText().equals("")) {
						actionsLv.setAdapter(new AppActionsAdapter(LauncherActivity.this, getAppActions(app)));
					} else if (action.getText().equals(getString(R.string.open_in))) {
						ArrayList<Action> actions = new ArrayList<Action>();
						actions.add(new Action(R.drawable.ic_arrow_back, ""));
						actions.add(new Action(R.drawable.ic_standard, getString(R.string.standard)));
						actions.add(new Action(R.drawable.ic_maximized, getString(R.string.maximized)));
						actions.add(new Action(R.drawable.ic_portrait, getString(R.string.portrait)));
						actions.add(new Action(R.drawable.ic_fullscreen, getString(R.string.fullscreen)));
						actionsLv.setAdapter(new AppActionsAdapter(LauncherActivity.this, actions));
					} else if (action.getText().equals(getString(R.string.app_info))) {
						startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
								.setData(Uri.parse("package:" + app)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						wm.removeView(view);
					} else if (action.getText().equals(getString(R.string.uninstall))) {
						startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + app))
								.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						wm.removeView(view);
					} else if (action.getText().equals(getString(R.string.freeze))) {
						String status = DeviceUtils.runAsRoot("pm disable " + app);
						if (!status.equals("error"))
							Toast.makeText(LauncherActivity.this, R.string.app_frozen, Toast.LENGTH_SHORT).show();
						else
							Toast.makeText(LauncherActivity.this, R.string.something_wrong, Toast.LENGTH_SHORT).show();
						wm.removeView(view);
						loadDesktopApps();
					} else if (action.getText().equals(getString(R.string.remove))) {
						AppUtils.unpinApp(LauncherActivity.this, app, AppUtils.DESKTOP_LIST);
						wm.removeView(view);
						loadDesktopApps();
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
					DeepShortcutManager.startShortcut(shortcut, LauncherActivity.this);
				}
			}
		});

		wm.addView(view, lp);
	}

	public class AppAdapterDesktop extends ArrayAdapter<App> {
		private Context context;
		private int iconBackground, iconPadding;

		public AppAdapterDesktop(Context context, ArrayList<App> apps) {
			super(context, R.layout.app_entry_desktop, apps);
			this.context = context;
			iconPadding = Utils.dpToPx(context, Integer.parseInt(sp.getString("icon_padding", "5")) + 2);
			switch (sp.getString("icon_shape", "circle")) {
			case "circle":
				iconBackground = R.drawable.circle;
				break;
			case "round_rect":
				iconBackground = R.drawable.round_square;
				break;
			case "default":
				iconBackground = -1;
				break;
			}

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = LayoutInflater.from(context).inflate(R.layout.app_entry_desktop, null);
			ImageView iconIv = convertView.findViewById(R.id.desktop_app_icon_iv);
			TextView nameTv = convertView.findViewById(R.id.desktop_app_name_tv);
			final App app = getItem(position);
			nameTv.setText(app.getName());

			IconParserUtilities iconParserUtilities = new IconParserUtilities(context);

			if (sp.getBoolean("icon_theming", false))
				iconIv.setImageDrawable(iconParserUtilities.getPackageThemedIcon(app.getPackageName()));
			else
				iconIv.setImageDrawable(app.getIcon());

			if (iconBackground != -1) {
				iconIv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
				iconIv.setBackgroundResource(iconBackground);
				ColorUtils.applyColor(iconIv, ColorUtils.getDrawableDominantColor(iconIv.getDrawable()));

			}

			convertView.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View p1, MotionEvent p2) {
					if (p2.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
						showAppContextMenu(app.getPackageName(), p1);
						return true;
					}
					return false;
				}

			});

			return convertView;
		}
	}
}
