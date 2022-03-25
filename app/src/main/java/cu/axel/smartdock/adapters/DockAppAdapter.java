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
import cu.axel.smartdock.models.DockApp;
import cu.axel.smartdock.utils.ColorUtils;
import android.widget.TextView;

public class DockAppAdapter extends ArrayAdapter<DockApp> 
{
    private final Context context;
    private int iconBackground;
    private final int iconPadding;
    private boolean iconTheming;
    public DockAppAdapter(Context context, ArrayList<DockApp> apps)
    {
        super(context, R.layout.app_task_entry, apps);
        this.context = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        iconTheming = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("icon_theming",false);
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
            holder.taskCounter=convertView.findViewById(R.id.task_count_badge);
            convertView.setTag(holder);
        }else
            holder = (ViewHolder) convertView.getTag();

        DockApp app = getItem(position);
 
        int size = app.getTasks().size();
        holder.runningIndicator.setAlpha(size > 0 ? 1f : 0);
        
        if(size>1){
            holder.taskCounter.setText(""+size);
        }else{
            holder.taskCounter.setAlpha(0);
        }


        if (iconBackground != -1)
        {
            holder.iconIv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
            holder.iconIv.setBackgroundResource(iconBackground);
        }

        IconParserUtilities iconParserUtilities = new IconParserUtilities(context);

        if(iconTheming || size > 1)
            holder.iconIv.setImageDrawable(iconParserUtilities.getPackageThemedIcon(app.getPackageName()));
        else
            holder.iconIv.setImageDrawable(app.getIcon());
        
        ColorUtils.applyColor(holder.iconIv, ColorUtils.getDrawableDominantColor(holder.iconIv.getDrawable()));
        
        return convertView;
    }

    private class ViewHolder
    {
        ImageView iconIv;
        TextView taskCounter;
        View runningIndicator;
    }
}
