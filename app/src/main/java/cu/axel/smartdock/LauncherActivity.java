package cu.axel.smartdock;
import android.app.*;
import android.os.*;
import android.content.*;

public class LauncherActivity extends Activity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		sendBroadcast(new Intent(getPackageName()+".HOME").putExtra("action","resume"));
	}

	@Override
	protected void onPause()
	{
		// TODO: Implement this method
		super.onPause();
		sendBroadcast(new Intent(getPackageName()+".HOME").putExtra("action","pause"));
	}

	@Override
	public void onBackPressed()
	{
		// TODO: Implement this method
       //super.onBackPressed();
	}
	

	
}
