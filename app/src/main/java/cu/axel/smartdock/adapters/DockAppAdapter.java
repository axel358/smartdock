package cu.axel.smartdock.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cu.axel.smartdock.R;
import cu.axel.smartdock.icons.IconParserUtilities;
import cu.axel.smartdock.models.DockApp;
import cu.axel.smartdock.utils.ColorUtils;
import cu.axel.smartdock.utils.Utils;
import java.util.ArrayList;
import cu.axel.smartdock.utils.AppUtils;

public class DockAppAdapter extends ArrayAdapter<DockApp> {
	private final Context context;
	private int iconBackground;
	private final int iconPadding;
	private boolean iconTheming, tintIndicators;
	private TaskRightClickListener rightClickListener;

	public DockAppAdapter(Context context, TaskRightClickListener listener, ArrayList<DockApp> apps) {
		super(context, R.layout.app_task_entry, apps);
		this.context = context;
		rightClickListener = listener;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		iconTheming = sp.getBoolean("icon_theming", false);
		iconPadding = Utils.dpToPx(context, Integer.parseInt(sp.getString("icon_padding", "5")));
		tintIndicators = sp.getBoolean("tint_indicators", false);
		switch (sp.getString("icon_shape", "circle")) {
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
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.app_task_entry, null);
			holder = new ViewHolder();
			holder.iconIv = convertView.findViewById(R.id.icon_iv);
			holder.runningIndicator = convertView.findViewById(R.id.running_indicator);
			holder.taskCounter = convertView.findViewById(R.id.task_count_badge);
			convertView.setTag(holder);
		} else
			holder = (ViewHolder) convertView.getTag();

		final DockApp app = getItem(position);

		int size = app.getTasks().size();

		if (size > 0) {
			if (tintIndicators)
				ColorUtils.applyColor(holder.runningIndicator,
						ColorUtils.manipulateColor(ColorUtils.getDrawableDominantColor(app.getIcon()), 2f));
			if (app.getTasks().get(0).getID() != -1)
				holder.runningIndicator.setAlpha(1f);

			if (app.getPackageName().equals(AppUtils.currentApp)) {
				holder.runningIndicator.getLayoutParams().width = Utils.dpToPx(context, 16);
			}

			if (size > 1) {
				holder.taskCounter.setText("" + size);
				holder.taskCounter.setAlpha(1f);
			}
		}

		IconParserUtilities iconParserUtilities = new IconParserUtilities(context);

		if (iconTheming || size > 1)
			holder.iconIv.setImageDrawable(iconParserUtilities.getPackageThemedIcon(app.getPackageName()));
		else
			holder.iconIv.setImageDrawable(app.getIcon());

		if (iconBackground != -1) {
			holder.iconIv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
			holder.iconIv.setBackgroundResource(iconBackground);
			ColorUtils.applyColor(holder.iconIv, ColorUtils.getDrawableDominantColor(holder.iconIv.getDrawable()));
		}

		convertView.setOnTouchListener((View p1, MotionEvent p2) -> {
			if (p2.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
				rightClickListener.onTaskRightClick(app.getPackageName(), p1);
				return true;
			}
			return false;
		});
		return convertView;
	}

	private class ViewHolder {
		ImageView iconIv;
		TextView taskCounter;
		View runningIndicator;
	}

	public interface TaskRightClickListener {
		public abstract void onTaskRightClick(String app, View view);
	}
}
