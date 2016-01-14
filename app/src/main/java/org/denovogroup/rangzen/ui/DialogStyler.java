package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

/**
 * Created by Liran on 1/14/2016.
 */
public abstract class DialogStyler {

    public static void styleAndShow(Context context, AlertDialog dialog){
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        dialog.show();
        int titleDividerId = context.getResources().getIdentifier("titleDivider", "id", "android");
        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null)
            titleDivider.setBackgroundColor(Color.WHITE);
    }
}
