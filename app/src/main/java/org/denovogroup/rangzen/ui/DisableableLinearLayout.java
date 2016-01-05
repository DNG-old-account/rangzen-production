package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by Liran on 1/4/2016.
 *
 * A linear layout which support setEnabled calls
 */
public class DisableableLinearLayout extends LinearLayout {

    boolean enabled = true;

    public DisableableLinearLayout(Context context) {
        super(context);
    }

    public DisableableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisableableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setEnabled(boolean enabled){
        this.enabled = enabled;
        handleEnabledStateChange();
    }


    private void handleEnabledStateChange(){
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if(!enabled) {
            canvas.drawColor(Color.parseColor("#DDFFFFFF"));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !enabled;
    }
}
