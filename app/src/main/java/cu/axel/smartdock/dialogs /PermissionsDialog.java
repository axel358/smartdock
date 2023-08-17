package cu.axel.smartdock.dialogs;

import android.app.Activity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PermissionsDialog extends MaterialAlertDialogBuilder {
	private MaterialButton overlayBtn, storageBtn, adminBtn, notificationsBtn, accessibilityBtn, locationBtn, usageBtn,
			secureBtn;

	public PermissionsDialog(Activity context) {
		super(context);
	}

}