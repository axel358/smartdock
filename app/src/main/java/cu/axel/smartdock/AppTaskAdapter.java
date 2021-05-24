package cu.axel.smartdock;

import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class AppTaskAdapter extends ArrayAdapter<AppTask>
{
	private Context context;
	public AppTaskAdapter(Context context, ArrayList<AppTask> appTasks)
	{
		super(context, R.layout.app_task_entry, appTasks);
		this.context = context;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = LayoutInflater.from(context).inflate(R.layout.app_task_entry, null);
		ImageView iconIv = view.findViewById(R.id.icon_iv);
		AppTask task = getItem(position);
		
		iconIv.setImageDrawable(task.getIcon());

		return view;
	}


}

