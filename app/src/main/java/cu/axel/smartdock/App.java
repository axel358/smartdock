package cu.axel.smartdock;
import android.graphics.drawable.*;

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

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setPackagename(String packagename)
	{
		this.packagename = packagename;
	}

	public String getPackagename()
	{
		return packagename;
	}

	public void setIcon(Drawable icon)
	{
		this.icon = icon;
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
