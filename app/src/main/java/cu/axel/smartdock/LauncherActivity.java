package cu.axel.smartdock;
import android.app.*;
import android.os.*;
import android.content.*;
import android.widget.*;
import android.view.View.*;
import android.view.*;

public class LauncherActivity extends Activity
{
	private LinearLayout backgroundLayout;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		backgroundLayout = findViewById(R.id.ll_background);

		backgroundLayout.setOnLongClickListener(new OnLongClickListener(){

				@Override
				public boolean onLongClick(View p1)
				{
					PopupMenu pmenu=new PopupMenu(LauncherActivity.this, p1);
					pmenu.setGravity(Gravity.CENTER);

					Utils.setForceShowIcon(pmenu);

					pmenu.inflate(R.menu.menu_desktop);

					pmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

							@Override
							public boolean onMenuItemClick(MenuItem p1)
							{
								switch (p1.getItemId())
								{
									case R.id.action_change_wallpaper:
										break;
									case R.id.action_settings:
										break;
								}
								return false;
							}
						});

					pmenu.show();
					
					return true;
				}
			});
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		sendBroadcast(new Intent(getPackageName() + ".HOME").putExtra("action", "resume"));
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		sendBroadcast(new Intent(getPackageName() + ".HOME").putExtra("action", "pause"));
	}

	@Override
	public void onBackPressed()
	{
		// TODO: Implement this method
		//super.onBackPressed();
	}



}
