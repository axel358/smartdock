package cu.axel.smartdock.utils;

import android.widget.PopupMenu;
import java.lang.reflect.Field;
import android.content.Context;

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
    
    public static int dpToPx(Context context, int dp)
    {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
