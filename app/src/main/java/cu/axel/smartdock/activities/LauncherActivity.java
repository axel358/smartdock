package cu.axel.smartdock.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import cu.axel.smartdock.R;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.Utils;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LauncherActivity extends Activity {
	private LinearLayout backgroundLayout;
    private Button serviceBtn;
    private String state;
    private TextView dateT;
    private GridView appsG;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		backgroundLayout = findViewById(R.id.ll_background);
        serviceBtn = findViewById(R.id.service_btn);

        dateT = findViewById(R.id.tv_date);

        appsG = findViewById(R.id.desktop_apps_gv);

        serviceBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    DeviceUtils.enableAccessibility(LauncherActivity.this);
                }
            });

		backgroundLayout.setOnLongClickListener(new OnLongClickListener(){

				@Override
				public boolean onLongClick(View p1) {
					AlertDialog.Builder dialog =new AlertDialog.Builder(LauncherActivity.this);
                    dialog.setAdapter(new ArrayAdapter<String>(LauncherActivity.this, android.R.layout.simple_list_item_1, new String[]{"Change wallpaper"})
                        , new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface p1, int p2) {
                                switch (p2) {
                                    case 0:
                                        startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
                                }
                            }
                        });
                    dialog.show();
					return true;
				}
			});

        appsG.setOnItemClickListener(new OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
                    final App app =(App) p1.getItemAtPosition(p3);
                    launchApp(null, app.getPackagename());
                }
            });

        appsG.setOnItemLongClickListener(new OnItemLongClickListener(){

                @Override
                public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4) {
                    showAppContextMenu(((App) p1.getItemAtPosition(p3)).getPackagename(), p2);
                    return true;
                }
            });

        registerReceiver(new BroadcastReceiver(){

                @Override
                public void onReceive(Context p1, Intent p2) {
                    String action=p2.getStringExtra("action");
                    if (action.equals("CONNECTED")) {
                        sendBroadcastToService(state);
                        serviceBtn.setVisibility(View.GONE);
                    } else if (action.equals("PINNED")) {
                        loadDesktopApps();
                    }
                }
            }, new IntentFilter(getPackageName() + ".SERVICE"){});
	}

    public void loadDesktopApps() {
        appsG.setAdapter(new AppAdapterDesktop(this, AppUtils.getPinnedApps(getPackageManager(), AppUtils.DESKTOP_LIST)));
    }

	@Override
	protected void onResume() {
		super.onResume();
		state = "resume";
        sendBroadcastToService(state);

        if (DeviceUtils.isAccessibilityServiceEnabled(this))
            serviceBtn.setVisibility(View.GONE);
        else
            serviceBtn.setVisibility(View.VISIBLE);

        dateT.setText(DateFormat.getDateInstance(DateFormat.LONG).format(new Date()));
        loadDesktopApps();
	}

	@Override
	protected void onPause() {
		super.onPause();
        state = "pause";
        sendBroadcastToService(state);

	}

    @Override
    public void onBackPressed() {
    }

    public void sendBroadcastToService(String action) {
        sendBroadcast(new Intent(getPackageName() + ".HOME").putExtra("action", action));
    }

    public void launchApp(String mode, String app) {
        sendBroadcast(new Intent(getPackageName() + ".HOME").putExtra("action", "launch").putExtra("mode", mode).putExtra("app", app));
    }

    private void showAppContextMenu(final String app, View p1) {
        PopupMenu pmenu=new PopupMenu(new ContextThemeWrapper(LauncherActivity.this, R.style.PopupMenuTheme), p1);

        Utils.setForceShowIcon(pmenu);

        pmenu.inflate(R.menu.app_menu);
        pmenu.getMenu().add(0, 4, 0, "Remove").setIcon(R.drawable.ic_unpin);

        pmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

                @Override
                public boolean onMenuItemClick(MenuItem p1) {
                    switch (p1.getItemId()) {
                        case R.id.action_appinfo:
                            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + app)));

                            break;
                        case R.id.action_uninstall:
                            startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + app)));
                            break;
                        case 4:
                            AppUtils.unpinApp(app, AppUtils.DESKTOP_LIST);
                            loadDesktopApps();
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
                    }
                    return false;
                }
            });

        pmenu.show();

    }

    public class AppAdapterDesktop extends ArrayAdapter<App> {
        private Context context;
        private int iconBackground,iconPadding;
        public AppAdapterDesktop(Context context, ArrayList<App> apps) {
            super(context, R.layout.app_entry_desktop, apps);
            this.context = context;
            SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(context);
            iconPadding = Utils.dpToPx(context, Integer.parseInt(sp.getString("pref_icon_padding", "4")));
            switch (sp.getString("pref_icon_shape", "pref_icon_shape_circle")) {
                case "pref_icon_shape_circle":
                    iconBackground = R.drawable.circle_solid_white;
                    break;
                case "pref_icon_shape_round_rect":
                    iconBackground = R.drawable.round_rect_solid_white;
                    break;
                case "pref_icon_shape_legacy":
                    iconBackground = -1;
                    break;
            }

        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView =   LayoutInflater.from(context).inflate(R.layout.app_entry_desktop, null);
            ImageView iconIv = convertView.findViewById(R.id.desktop_app_icon_iv);
            TextView nameTv=convertView.findViewById(R.id.desktop_app_name_tv);
            final App app = getItem(position);
            nameTv.setText(app.getName());
            if (iconBackground != -1) {
                iconIv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
                iconIv.setBackgroundResource(iconBackground);
            }iconIv.setImageDrawable(app.getIcon());

            convertView.setOnTouchListener(new OnTouchListener(){

                    @Override
                    public boolean onTouch(View p1, MotionEvent p2) {
                        if (p2.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                            showAppContextMenu(app.getPackagename(), p1);
                            return true;
                        }
                        return false;
                    }


                });


            return convertView;
        }
    }
}
