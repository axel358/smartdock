package cu.axel.smartdock.adapters;

import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import cu.axel.smartdock.R;
import cu.axel.smartdock.utils.DeepShortcutManager;

import java.util.List;

public class AppShortcutAdapter extends ArrayAdapter<ShortcutInfo> {
    private Context context;

    public AppShortcutAdapter(Context context, List<ShortcutInfo> shortcuts) {
        super(context, R.layout.pin_entry, shortcuts);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.pin_entry, null);

        ImageView icon = convertView.findViewById(R.id.pin_entry_iv);
        TextView text = convertView.findViewById(R.id.pin_entry_tv);

        ShortcutInfo shortcut = getItem(position);

        icon.setImageDrawable(DeepShortcutManager.getShortcutIcon(shortcut, context));

        //noinspection NewApi
        text.setText(shortcut.getShortLabel());

        return convertView;


    }

}
