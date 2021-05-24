package cu.axel.smartdock;
import android.widget.*;
import java.lang.reflect.*;

public class Utils
{
	public static void setForceShowIcon(PopupMenu popupMenu)
	{
		try
		{
            Field[] declaredFields = popupMenu.getClass().getDeclaredFields();
            for (Field field : declaredFields)
			{
                if ("mPopup".equals(field.getName()))
				{
                    field.setAccessible(true);
                    Object obj = field.get(popupMenu);
                    Class.forName(obj.getClass().getName()).getMethod("setForceShowIcon", Boolean.TYPE).invoke(obj, new Boolean(true));
                    return;
                }
            }
        }
		catch (Throwable th)
		{
            th.printStackTrace();
        }
    }

	public static boolean isBlackListed(String packageName)
	{
		return packageName.equals("com.aide.ui")
			|| packageName.equals("android");
	}
}
