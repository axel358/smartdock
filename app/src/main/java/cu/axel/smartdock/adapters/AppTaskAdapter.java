package cu.axel.smartdock.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import cu.axel.smartdock.R;
import cu.axel.smartdock.models.AppTask;
import java.util.ArrayList;

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

