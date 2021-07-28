package cu.axel.smartdock.widgets;
import android.widget.LinearLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

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
