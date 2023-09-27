package cu.axel.smartdock.models;

import android.graphics.drawable.Drawable;

public class AppTask extends App {
    private int id;

    public AppTask(int id, String label, String packageName, Drawable icon) {
        super(label, packageName, icon);
        this.id = id;

    }

    public int getID() {
        return id;
    }

}
