package cu.axel.smartdock;

import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import android.preference.*;

public class AppTaskAdapter extends ArrayAdapter<AppTask>
{
	private Context context;
	private SharedPreferences sp;
	public AppTaskAdapter(Context context, ArrayList<AppTask> appTasks)
	{
		super(context, R.layout.app_task_entry, appTasks);
		this.context = context;
		sp = PreferenceManager.getDefaultSharedPreferences(context);
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = LayoutInflater.from(context).inflate(R.layout.app_task_entry, null);
		ImageView iconIv = view.findViewById(R.id.icon_iv);
		LinearLayout iconContainer = view.findViewById(R.id.icon_container);

		AppTask task = getItem(position);
		if (sp.getBoolean("pref_enable_circular_icons", true))
			iconContainer.setBackgroundResource(R.drawable.circle_solid);
		iconIv.setImageDrawable(task.getIcon());

		return view;
	}


}

