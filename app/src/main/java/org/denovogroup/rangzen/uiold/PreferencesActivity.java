package org.denovogroup.rangzen.uiold;

import android.content.Intent;
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
public class PreferencesActivity extends ActionBarActivity implements FriendsFragment.FriendsFragmentLock{

    public static final String ACTION_ADD_FRIEND = "RangzenPref.promptAddFriend";

    public static final String PREF_FILE = "settings";

    public static final String WIFI_NAME = "wifiName";

    ViewPager pager;
    FriendsFragment friendsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preferences_activity);
        pager = ((ViewPager)findViewById(R.id.pref_pager));

        pager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {

            int[] titles = new int[]{R.string.pager_title_security, R.string.pager_title_profile, R.string.pager_title_friends,R.string.killswitch};

            @Override
            public Fragment getItem(int position) {
                Fragment page = null;

                switch (position) {
                    case 0:
                        page = new SecuritySettingsFragment();
                        break;
                    case 1:
                        page = new ProfileSettingsFragment();
                        break;
                    case 2:
                        page = new FriendsFragment();
                        break;
                    case 3:
                        page = new KillswitchFragment();
                        break;
                }
                return page;
            }

            @Override
            public int getCount() {
                return titles.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return getString(titles[position]);
            }
        });

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 2 && friendsFragment != null) {
                    friendsFragment.restoreViewState();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        if(ACTION_ADD_FRIEND.equals(getIntent().getAction())){
            pager.setCurrentItem(2, false);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onFriendsFragmentCreated(FriendsFragment fragment) {
        friendsFragment = fragment;

        if(ACTION_ADD_FRIEND.equals(getIntent().getAction())){
            if(pager.getCurrentItem() != 2) pager.setCurrentItem(2, false);
            getIntent().setAction("");
            friendsFragment.startAddFriend();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(pager != null && pager.getCurrentItem() == 2 && friendsFragment != null){
            friendsFragment.onActivityResult(requestCode,resultCode,data);
        }
    }
}
