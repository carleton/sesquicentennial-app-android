package carleton150.edu.carleton.reunion.Adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import carleton150.edu.carleton.reunion.MainActivity;
import carleton150.edu.carleton.reunion.MainFragments.EventsFragment;
import carleton150.edu.carleton.reunion.MainFragments.HomeFragment;
import carleton150.edu.carleton.reunion.MainFragments.MainFragment;
import carleton150.edu.carleton.reunion.MainFragments.MapFragment;
import carleton150.edu.carleton.reunion.R;

/**
 * Created by haleyhinze on 4/27/16.
 * FragmentStatePagerAdapter for main app fragments
 */
public class MyFragmentPagerAdapter extends FragmentStatePagerAdapter {

    private MainActivity mainActivity;
    private Fragment currentFragment = new HomeFragment();
    private FragmentManager fm;
    private MainFragment mFragmentAtPos2 = null;


    public MyFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void initialize(MainActivity mainActivity, FragmentManager fm){
        this.mainActivity = mainActivity;
        this.fm = fm;
    }

    @Override
    public Fragment getItem(int position) {
        MainFragment fragment = null;
        switch(position){
            case 0:
                Log.d("Adapter", "Instantiating Home Fragment");
                fragment = new HomeFragment();
                break;
            case 1:
                fragment = new EventsFragment();
                break;
            case 2:
                fragment = new MapFragment();
                break;
        }
        return fragment;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        currentFragment = (MainFragment) object;
        super.setPrimaryItem(container, position, object);
    }

    @Override
    public int getItemPosition(Object object) {

        return POSITION_UNCHANGED;
    }



    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = "";
        switch (position){
            case 0:
                title = mainActivity.getResources().getString(R.string.home);
                break;
            case 1:
                title = mainActivity.getResources().getString(R.string.events);
                break;
            case 2:
                title = mainActivity.getResources().getString(R.string.history);
                break;
        }
        return title;
    }

    /**
     * @return fragment currently in view
     */
    public Fragment getCurrentFragment(){
        return this.currentFragment;
    }



    /**
     * If back button is pressed while QuestInProgressFragment is in view,
     * goes back to QuestFragment
     */
    public void backButtonPressed(){
    }

}


