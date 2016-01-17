package org.denovogroup.rangzen.ui;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.URLSpan;

import org.denovogroup.rangzen.R;

/**
 * Created by Liran on 1/17/2016.
 */
public class URLSpanNoUnderline extends URLSpan {
    public URLSpanNoUnderline(String p_Url) {
        super(p_Url);
    }

    public void updateDrawState(TextPaint p_DrawState) {
        super.updateDrawState(p_DrawState);
        p_DrawState.setUnderlineText(false);
        p_DrawState.setColor(Color.parseColor("#6734ba"));
    }
}
