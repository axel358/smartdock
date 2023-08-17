package cu.axel.smartdock.preferences;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.google.android.material.button.MaterialButton;
import cu.axel.smartdock.R;

public class FileChooserPreference extends Preference {
	private Context context;

	public FileChooserPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		setupPreference();
	}

	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		MaterialButton resetButton = (MaterialButton) holder.findViewById(R.id.fs_preference_reset_btn);
		resetButton.setOnClickListener(v -> setFile("default"));
	}

	private void setupPreference() {
		setWidgetLayoutResource(R.layout.preference_file_chooser);
	}

	public void setFile(String file) {
		persistString(file);
		updateSummary();
	}
	
	@Override
	public void onAttached() {
		super.onAttached();
		updateSummary();
	}

	private void updateSummary() {
		String file = getSharedPreferences().getString(getKey(), "default");
		setSummary(file.equals("default") ? context.getString(R.string.tap_to_set) : "");
	}
}