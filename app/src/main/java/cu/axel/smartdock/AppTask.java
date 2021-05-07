package cu.axel.smartdock;
import android.graphics.drawable.*;

public class AppTask
{
	private String className,packageName;
	private Drawable icon;
	private int id;

	public AppTask(int id, String className, String packageName, Drawable icon)
	{
		this.className = className;
		this.packageName = packageName;
		this.icon = icon;
		this.id = id;
	}


	public int getId()
	{
		return id;
	}


	public String getClassName()
	{
		return className;
	}


	public String getPackageName()
	{
		return packageName;
	}

	public Drawable getIcon()
	{
		return icon;
	}

}
