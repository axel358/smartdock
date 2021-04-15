package cu.axel.smartdock;
import android.content.*;
import android.util.*;
import android.view.*;
import android.widget.*;

public class HoverInterceptorLayout extends LinearLayout
{
	public HoverInterceptorLayout(Context context, AttributeSet attrs)
	{
        super(context, attrs);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event)
	{
		return true;
    }
}
