package cu.axel.smartdock.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import cu.axel.smartdock.services.DockService;
import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.Utils;
import android.content.SharedPreferences;

public class SoundEventsReceiver extends BroadcastReceiver {
	private SharedPreferences sp;

	public SoundEventsReceiver(SharedPreferences sp) {
		this.sp = sp;
	}

	@Override
	public void onReceive(Context p1, Intent p2) {
		switch (p2.getAction()) {
		case UsbManager.ACTION_USB_DEVICE_ATTACHED:
			DeviceUtils.playEventSound(p1, "usb_sound");
			break;
		case Intent.ACTION_POWER_CONNECTED:
			Utils.shouldPlayChargeComplete = true;
			DeviceUtils.playEventSound(p1, "charge_sound");
			break;
		}
	}

}
