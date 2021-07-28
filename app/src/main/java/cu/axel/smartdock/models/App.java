package cu.axel.smartdock.models;
import android.graphics.drawable.Drawable;

public class App
{
	private String name,packagename;
	private Drawable icon;

	public App(String name, String packagename, Drawable icon)
	{
		this.name = name;
		this.packagename = packagename;
		this.icon = icon;
	}

	public String getName()
	{
		return name;
	}


	public String getPackagename()
	{
		return packagename;
	}
	

	public Drawable getIcon()
	{
		return icon;
	}

	@Override
	public String toString()
	{
		return name;
	}

}
