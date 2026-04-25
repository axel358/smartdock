package cu.axel.smartdock.wrappers

import android.net.wifi.IWifiManager
import android.os.IBinder
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class WifiManagerWrapper {


    private var binder: ShizukuBinderWrapper? = null
    private var wifiManager: IWifiManager? = null

    private val deathRecipient = IBinder.DeathRecipient {
        synchronized(this) {
            wifiManager = null
            binder = null
        }
    }

    init {
        init()
    }

    fun init() {
        synchronized(this) {
            if (wifiManager != null)
                return
            binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("wifi"))
            binder?.linkToDeath(deathRecipient, 0)
            wifiManager = IWifiManager.Stub.asInterface(binder)
        }
    }

    fun isAlive() = binder?.isBinderAlive == true && wifiManager != null

    fun setWifiEnabled(enabled: Boolean): Boolean {
        return wifiManager?.setWifiEnabled("com.android.shell", enabled) ?: false
    }
}