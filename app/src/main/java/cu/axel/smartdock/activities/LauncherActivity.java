package cu.axel.smartdock.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
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
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import cu.axel.smartdock.R;
import cu.axel.smartdock.adapters.AppActionsAdapter;
import cu.axel.smartdock.adapters.AppAdapter;
import cu.axel.smartdock.icons.IconParserUtilities;
import cu.axel.smartdock.models.Action;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.services.DockService;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.ColorUtils;
import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.Utils;
import cu.axel.smartdock.widgets.VisualizerView;
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

public class LauncherActivity extends AppCompatActivity implements AppAdapter.OnAppClickListener, Visualizer.OnDataCaptureListener {
	private LinearLayout backgroundLayout;
	private MaterialButton serviceBtn;
	private RecyclerView appsGv;
	private EditText notesEt;
	private SharedPreferences sp;
	private float x, y;
	private IconParserUtilities iconParserUtilities;
	private VisualizerView visualizerView;
	private Visualizer visualizer;
	private final int AUDIO_REQUEST_CODE = 57;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		backgroundLayout = findViewById(R.id.ll_background);
		serviceBtn = findViewById(R.id.service_btn);
		appsGv = findViewById(R.id.desktop_apps_gv);
		appsGv.setLayoutManager(new GridLayoutManager(this, 2));
		notesEt = findViewById(R.id.notes_et);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		iconParserUtilities = new IconParserUtilities(this);
		visualizerView = findViewById(R.id.visualizer_view);
		visualizerView.setBarAlpha(150);

		serviceBtn.setOnClickListener((View p1) -> {
			startActivity(new Intent(LauncherActivity.this, MainActivity.class));
		});

		backgroundLayout.setOnLongClickListener((View v0) -> {
			final View view = LayoutInflater.from(LauncherActivity.this).inflate(R.layout.task_list, null);
			WindowManager.LayoutParams lp = Utils.makeWindowParams(-2, -2, LauncherActivity.this, false);
			ColorUtils.applyMainColor(LauncherActivity.this, sp, view);
			lp.gravity = Gravity.TOP | Gravity.LEFT;
			lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
					| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
			lp.x = (int) x;
			lp.y = (int) y;

			final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

			view.setOnTouchListener((View v1, MotionEvent p2) -> {
				if (p2.getAction() == MotionEvent.ACTION_OUTSIDE) {
					wm.removeView(view);
				}
				return false;
			});
			final ListView actionsLv = view.findViewById(R.id.tasks_lv);
			ArrayList<Action> actions = new ArrayList<Action>();
			actions.add(new Action(R.drawable.ic_wallpaper, getString(R.string.change_wallpaper)));
			actions.add(new Action(R.drawable.ic_fullscreen, getString(R.string.display_settings)));

			actionsLv.setAdapter(new AppActionsAdapter(LauncherActivity.this, actions));

			actionsLv.setOnItemClickListener((AdapterView<?> a1, View v2, int p3, long p4) -> {
				Action action = (Action) a1.getItemAtPosition(p3);
				if (action.getText().equals(getString(R.string.change_wallpaper)))
					startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER),
							getString(R.string.change_wallpaper)), 18);
				else if (action.getText().equals(getString(R.string.display_settings)))
					startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));

				wm.removeView(view);
			});

			wm.addView(view, lp);
			return true;
		});

		backgroundLayout.setOnTouchListener((View p1, MotionEvent p2) -> {
			x = p2.getX();
			y = p2.getY();
			return false;
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
		appsGv.setAdapter(new AppAdapter(this, iconParserUtilities,
				AppUtils.getPinnedApps(this, getPackageManager(), AppUtils.DESKTOP_LIST), this, true));
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

		if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
			requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, AUDIO_REQUEST_CODE);
		else
			startVisualiser();

		appsGv.requestFocus();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (sp.getBoolean("show_notes", false))
			saveNotes();

		if (visualizer != null) {
			visualizer.setEnabled(false);
			visualizer.release();
			visualizer.setDataCaptureListener(null, 0, false, false);
		}
	}
	
	@Override
    public void onWaveFormDataCapture(Visualizer thisVisualiser, byte[] waveform, int samplingRate) {
        if (visualizerView != null) {
            visualizerView.setAudioData(waveform);
        }
    }

    @Override
    public void onFftDataCapture(Visualizer thisVisualiser, byte[] fft, int samplingRate) {
    }

	private void startVisualiser() {
		visualizer = new Visualizer(0);
		visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
		visualizer.setDataCaptureListener(this, Visualizer.getMaxCaptureRate() - Visualizer.getMaxCaptureRate() / 4,
				true, false);
		visualizer.setEnabled(true);
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
		WindowManager.LayoutParams lp = Utils.makeWindowParams(-2, -2, this, false);
		ColorUtils.applyMainColor(LauncherActivity.this, sp, view);
		lp.gravity = Gravity.TOP | Gravity.LEFT;
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

		int[] location = new int[2];
		anchor.getLocationOnScreen(location);

		lp.x = location[0];
		lp.y = location[1] + Utils.dpToPx(this, anchor.getMeasuredHeight() / 2);

		view.setOnTouchListener((View p1, MotionEvent p2) -> {
			if (p2.getAction() == MotionEvent.ACTION_OUTSIDE) {
				wm.removeView(view);
			}
			return false;
		});
		final ListView actionsLv = view.findViewById(R.id.tasks_lv);

		actionsLv.setAdapter(new AppActionsAdapter(this, getAppActions(app)));

		actionsLv.setOnItemClickListener((AdapterView<?> p1, View p2, int p3, long p4) -> {
			if (p1.getItemAtPosition(p3) instanceof Action) {
				Action action = (Action) p1.getItemAtPosition(p3);
				if (action.getText().equals(getString(R.string.manage))) {
					ArrayList<Action> actions = new ArrayList<Action>();
					actions.add(new Action(R.drawable.ic_arrow_back, ""));
					actions.add(new Action(R.drawable.ic_info, getString(R.string.app_info)));
					if (!AppUtils.isSystemApp(LauncherActivity.this, app)
							|| sp.getBoolean("allow_sysapp_uninstall", false))
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
					if (AppUtils.isSystemApp(LauncherActivity.this, app))
						DeviceUtils.runAsRoot("pm uninstall --user 0 " + app);
					else
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
			}
			//noinspection NewApi 
			else if (p1.getItemAtPosition(p3) instanceof ShortcutInfo) {
				ShortcutInfo shortcut = (ShortcutInfo) p1.getItemAtPosition(p3);
				wm.removeView(view);
				DeepShortcutManager.startShortcut(shortcut, LauncherActivity.this);
			}
		});

		wm.addView(view, lp);
	}

	@Override
	public void onAppClicked(App app, View item) {
		launchApp(null, app.getPackageName());
	}

	@Override
	public void onAppLongClicked(App app, View item) {
		showAppContextMenu(app.getPackageName(), item);

	}

}
