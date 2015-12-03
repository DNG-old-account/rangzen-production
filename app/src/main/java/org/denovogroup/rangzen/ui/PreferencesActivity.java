package org.denovogroup.rangzen.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import org.denovogroup.rangzen.R;

/**
 * Created by Liran on 12/2/2015.
 *
 * The main activity for all user prefernces related interactions
 */
public class PreferencesActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences_activity);

        ((ViewPager)findViewById(R.id.pref_pager)).setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()){

            int[] titles = new int[]{R.string.pager_title_security,R.string.pager_title_profile};

            @Override
            public Fragment getItem(int position) {
                Fragment page = null;

                switch(position){
                    case 0:
                        page = new SecuritySettingsFragment();
                        break;
                    case 1:
                        page = new ProfileSettingsFragment();
                        break;
                }
                return page;
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle (int position) {
                return getString(titles[position]);
            }
        });
    }
}
