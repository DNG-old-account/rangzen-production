package org.denovogroup.rangzen.ui;

import android.database.Cursor;

/**
 * Created by Liran on 9/12/2015.
 *
 * A simple interface used to supply uniform refresh view functionality across multiple objects
 */
public interface Refreshable  {

    public void refreshView(Cursor items);
}
