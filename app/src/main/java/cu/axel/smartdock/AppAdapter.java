package cu.axel.smartdock;

import android.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class AppAdapter extends ArrayAdapter<App>
{
	private Context context;
	public AppAdapter(Context context, ArrayList<App> apps)
	{
		super(context, R.layout.app_entry, apps);
		this.context = context;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = LayoutInflater.from(context).inflate(R.layout.app_entry, null);
		ImageView iconIv = view.findViewById(R.id.menu_app_icon_iv);
		TextView nameTv=view.findViewById(R.id.menu_app_name_tv);
		App app = getItem(position);
		nameTv.setText(app.getName());
		iconIv.setImageDrawable(app.getIcon());

		return view;
	}

}
