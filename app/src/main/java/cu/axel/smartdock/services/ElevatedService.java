package cu.axel.smartdock.services;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import cu.axel.smartdock.IElevatedService;
import android.os.RemoteException;
import android.widget.Toast;
import java.lang.reflect.Method;
import cu.axel.smartdock.utils.AppUtils;
import android.app.ActivityManager;
//import android.os.ServiceManager;

public class ElevatedService extends Service {
    
    private ActivityManager am;
    private final IElevatedService.Stub binder = new IElevatedService.Stub() {

        @Override
        public void showToast(String message) throws RemoteException {
            Toast.makeText(ElevatedService.this,message, 5000).show();
        }
        
    };
    
    public ElevatedService(String[] args){
        super();
        try {
            Class ServiceManager = Class.forName("android.os.ServiceManager");
            Method addService = ServiceManager.getMethod("addService", String.class, IBinder.class);
            addService.invoke(ServiceManager, "sdelevated", binder);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println(args[0]);
        
        
    }
    
    public void removeTask(int id){
        try {
            Method removeTask = am.getClass().getMethod("removeTask", int.class);
            removeTask.invoke(am, id);
        } catch (Exception e) {
        }
    }

    @Override
    public IBinder onBind(Intent p1) {
        return binder;
    }
    
    public static void main(String[] args){
        new ElevatedService(args);
    }
    
}
