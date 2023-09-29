package cu.axel.smartdock.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

import cu.axel.smartdock.R;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.ColorUtils;

public class AppChooserPreference extends Preference {
    private Context context;
    private SharedPreferences sp;

    public AppChooserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPreference(context);
    }

    public void setupPreference(Context context) {
        this.context = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String packageName = sp.getString(getKey(), "");
        setSummary(packageName.isEmpty() ? context.getString(R.string.tap_to_set)
                : AppUtils.getPackageLabel(context, packageName));
    }

    @Override
    protected void onClick() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.choose_app);
        final ArrayList<App> apps = AppUtils.getInstalledApps(context.getPackageManager());
        dialog.setAdapter(new AppAdapter(context, apps), (DialogInterface p1, int p2) -> {
            App app = apps.get(p2);
            sp.edit().putString(getKey(), app.getPackageName()).apply();
            setSummary(app.getName());
        });
        dialog.show();
    }

    static class AppAdapter extends ArrayAdapter<App> {
        private final Context context;

        public AppAdapter(Context context, ArrayList<App> apps) {
            super(context, R.layout.pin_entry, apps);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.app_chooser_entry, null);

            ImageView icon = convertView.findViewById(R.id.app_chooser_entry_iv);
            TextView text = convertView.findViewById(R.id.app_chooser_entry_tv);

            App app = getItem(position);

            icon.setImageDrawable(app.getIcon());
            text.setText(app.getName());

            ColorUtils.applyColor(icon, ColorUtils.getDrawableDominantColor(icon.getDrawable()));

            return convertView;

        }

    }

}
