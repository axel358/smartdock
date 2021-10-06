package cu.axel.smartdock.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class PackageRemovedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context p1, Intent p2) {
        Toast.makeText(p1,p2.getDataString(),Toast.LENGTH_LONG).show();
    }

}
