package cu.axel.smartdock;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import cu.axel.smartdock.DockService;
import java.util.List;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.content.DialogInterface;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;

public class LauncherActivity extends Activity
{
	private LinearLayout backgroundLayout;
    private Button serviceBtn;
    private String action;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		backgroundLayout = findViewById(R.id.ll_background);
        serviceBtn = findViewById(R.id.service_btn);

        serviceBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    enableAccessibility();
                }
            });

		backgroundLayout.setOnLongClickListener(new OnLongClickListener(){

				@Override
				public boolean onLongClick(View p1)
				{
					AlertDialog.Builder dialog =new AlertDialog.Builder(LauncherActivity.this);
                    dialog.setAdapter(new ArrayAdapter<String>(LauncherActivity.this, android.R.layout.simple_list_item_1, new String[]{"Change wallpaper"})
                        , new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface p1, int p2)
                            {
                                switch (p2)
                                {
                                    case 0:
                                        startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
                                }
                            }
                        });
                    dialog.show();
					return true;
				}
			});

        registerReceiver(new BroadcastReceiver(){

                @Override
                public void onReceive(Context p1, Intent p2)
                {
                    sendBroadcastToService(action);
                    serviceBtn.setVisibility(View.GONE);
                }
            }, new IntentFilter(getPackageName() + ".CONNECTED"){});
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		action = "resume";
        sendBroadcastToService(action);

        if (isAccessibilityServiceEnabled())
            serviceBtn.setVisibility(View.GONE);
        else
            serviceBtn.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
        action = "pause";
        sendBroadcastToService(action);

	}

    public void enableAccessibility()
    {
        startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
    }

    public void sendBroadcastToService(String action)
    {
        sendBroadcast(new Intent(getPackageName() + ".HOME").putExtra("action", action));
    }

    public boolean isAccessibilityServiceEnabled()
    {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices)
        {
            ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
            if (serviceInfo.packageName.equals(getPackageName()) && serviceInfo.name.equals(DockService.class.getName()))
            {
                return true;
            }
        }

        return false;
	}

}
