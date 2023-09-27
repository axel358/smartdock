package cu.axel.smartdock.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import cu.axel.smartdock.R;

import java.util.ArrayList;

import cu.axel.smartdock.utils.ColorUtils;

import android.preference.PreferenceManager;

import cu.axel.smartdock.models.Action;

public class AppActionsAdapter extends ArrayAdapter<Action> {
    private Context context;

    public AppActionsAdapter(Context context, ArrayList<Action> actions) {
        super(context, R.layout.pin_entry, actions);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Action action = getItem(position);

        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.pin_entry, null);

        ImageView icon = convertView.findViewById(R.id.pin_entry_iv);
        TextView text = convertView.findViewById(R.id.pin_entry_tv);

        ColorUtils.applySecondaryColor(context, PreferenceManager.getDefaultSharedPreferences(context), icon);

        text.setText(action.getText());
        icon.setImageResource(action.getIcon());

        return convertView;
    }


}
