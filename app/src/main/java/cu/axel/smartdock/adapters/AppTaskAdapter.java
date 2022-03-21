package cu.axel.smartdock.adapters;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import cu.axel.smartdock.R;
import cu.axel.smartdock.icons.IconParserUtilities;
import cu.axel.smartdock.models.AppTask;
import cu.axel.smartdock.utils.Utils;
import java.util.ArrayList;
import android.content.SharedPreferences;

public class AppTaskAdapter extends ArrayAdapter<AppTask> 
{
    private final Context context;
    private int iconBackground;
    private final int iconPadding;
    public AppTaskAdapter(Context context, ArrayList<AppTask> appTasks)
    {
        super(context, R.layout.app_task_entry, appTasks);
        this.context = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        iconPadding = Utils.dpToPx(context, Integer.parseInt(sp.getString("icon_padding", "4")));
        switch (sp.getString("icon_shape", "circle"))
        {
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
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.app_task_entry, null);
            holder = new ViewHolder();
            holder.iconIv = convertView.findViewById(R.id.icon_iv);
            holder.runningIndicator = convertView.findViewById(R.id.running_indicator);
            convertView.setTag(holder);
        }else
            holder = (ViewHolder) convertView.getTag();

        AppTask task = getItem(position);
        
        holder.runningIndicator.setAlpha(task.getIds().size() > 0? 1f : 0);


        if (iconBackground != -1)
        {
            holder.iconIv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
            holder.iconIv.setBackgroundResource(iconBackground);
        }

        IconParserUtilities iconParserUtilities = new IconParserUtilities(context);

        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("icon_theming",false))
            holder.iconIv.setImageDrawable(iconParserUtilities.getPackageThemedIcon(task.getPackageName()));
        else
            holder.iconIv.setImageDrawable(task.getIcon());

        return convertView;
    }

    private class ViewHolder
    {
        ImageView iconIv;
        View runningIndicator;
    }
}
