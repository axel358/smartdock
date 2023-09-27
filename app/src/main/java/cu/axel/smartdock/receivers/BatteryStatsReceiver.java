package cu.axel.smartdock.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.ImageView;

import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.Utils;

public class BatteryStatsReceiver extends BroadcastReceiver {
    private ImageView batteryBtn;
    private SharedPreferences sp;

    public BatteryStatsReceiver(ImageView batteryBtn, SharedPreferences sp) {
        this.batteryBtn = batteryBtn;
        this.sp = sp;
    }

    @Override
    public void onReceive(Context p1, Intent intent) {

        int level = intent.getExtras().getInt("level");

        if (intent.getExtras().getInt("plugged") == 0)
            batteryBtn.setImageResource(Utils.getBatteryDrawable(level, false));
        else {
            batteryBtn.setImageResource(Utils.getBatteryDrawable(level, true));
            if (level == 100) {
                if (Utils.shouldPlayChargeComplete)
                    DeviceUtils.playEventSound(p1, "charge_complete_sound");

                Utils.shouldPlayChargeComplete = false;
            }
        }
    }
}
