package cu.axel.smartdock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import cu.axel.smartdock.utils.DeviceUtils
import cu.axel.smartdock.utils.Utils

class BatteryStatsReceiver(private val batteryBtn: ImageView) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.extras!!.getInt("level")
        if (intent.extras!!.getInt("plugged") == 0)
            batteryBtn.setImageResource(Utils.getBatteryDrawable(level, false))
        else {
            batteryBtn.setImageResource(Utils.getBatteryDrawable(level, true))
            if (level == 100) {
                if (Utils.shouldPlayChargeComplete)
                    DeviceUtils.playEventSound(context, "charge_complete_sound")
                Utils.shouldPlayChargeComplete = false
            }
        }
    }
}
