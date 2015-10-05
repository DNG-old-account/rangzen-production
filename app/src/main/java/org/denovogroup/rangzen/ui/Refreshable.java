package org.denovogroup.rangzen.ui;

import java.util.List;

/**
 * Created by Liran on 9/12/2015.
 *
 * A simple interface used to supply uniform refresh view functionality across multiple objects
 */
public interface Refreshable  {

    public void refreshView(List<?> items);
}
