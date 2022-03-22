package cu.axel.smartdock.adapters;
import android.widget.ArrayAdapter;
import android.content.Context;
import java.util.ArrayList;
import cu.axel.smartdock.models.AppTask;
import cu.axel.smartdock.R;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;
import cu.axel.smartdock.utils.ColorUtils;

public class AppTaskAdaper extends ArrayAdapter<AppTask> {
    private Context context;

    public AppTaskAdaper(Context context, ArrayList<AppTask> tasks) {
        super(context, R.layout.pin_entry, tasks);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.pin_entry, null);

        ImageView icon = convertView.findViewById(R.id.pin_entry_iv);
        TextView text=convertView.findViewById(R.id.pin_entry_tv);

        AppTask task = getItem(position);

        icon.setImageDrawable(task.getIcon());
        text.setText(task.getName());
        
        ColorUtils.applyColor(icon, ColorUtils.getDrawableDominantColor(icon.getDrawable()));

        return convertView;


    } 

}
