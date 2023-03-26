package cu.axel.smartdock.preferences;

import android.content.Context;
import androidx.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import cu.axel.smartdock.R;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.models.AppTask;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.ColorUtils;
import java.util.ArrayList;
import android.content.DialogInterface;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.graphics.Color;

public class AppChooserPreference extends Preference {
	private Context context;
	private SharedPreferences sp;

	public AppChooserPreference(Context context) {
		super(context);
		setupPreference(context);
	}

	public AppChooserPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupPreference(context);
	}

	public AppChooserPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
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
		dialog.setAdapter(new AppAdapter(context, apps), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface p1, int p2) {
				App app = apps.get(p2);
				sp.edit().putString(getKey(), app.getPackageName()).commit();
				setSummary(app.getName());
			}
		});
		dialog.show();
	}

	class AppAdapter extends ArrayAdapter<App> {
		private Context context;

		public AppAdapter(Context context, ArrayList<App> apps) {
			super(context, R.layout.pin_entry, apps);
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null)
				convertView = LayoutInflater.from(context).inflate(R.layout.pin_entry, null);

			ImageView icon = convertView.findViewById(R.id.pin_entry_iv);
			TextView text = convertView.findViewById(R.id.pin_entry_tv);

			App app = getItem(position);

			icon.setImageDrawable(app.getIcon());
			text.setText(app.getName());
			text.setTextColor(Color.BLACK);

			ColorUtils.applyColor(icon, ColorUtils.getDrawableDominantColor(icon.getDrawable()));

			return convertView;

		}

	}

}
