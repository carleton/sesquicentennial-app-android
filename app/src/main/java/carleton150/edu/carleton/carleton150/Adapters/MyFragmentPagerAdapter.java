package carleton150.edu.carleton.carleton150.Adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import carleton150.edu.carleton.carleton150.ExtraFragments.QuestCompletedFragment;
import carleton150.edu.carleton.carleton150.Interfaces.QuestStartedListener;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.MainFragments.EventsFragment;
import carleton150.edu.carleton.carleton150.MainFragments.HistoryFragment;
import carleton150.edu.carleton.carleton150.MainFragments.MainFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestInProgressFragment;
import carleton150.edu.carleton.carleton150.R;

/**
 * Created by haleyhinze on 4/27/16.
 */
public class MyFragmentPagerAdapter extends FragmentStatePagerAdapter implements QuestStartedListener {

    private MainActivity mainActivity;
    private MainFragment currentFragment = new HistoryFragment();
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
                fragment = new HistoryFragment();
                break;
            case 1:
                fragment = new EventsFragment();
                break;
            case 2:
                if(mFragmentAtPos2 == null) {
                    mFragmentAtPos2 = new QuestFragment();
                    ((QuestFragment) mFragmentAtPos2).initialize(this);
                }
                return mFragmentAtPos2;
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

        if(object instanceof QuestInProgressFragment || object instanceof QuestCompletedFragment || object instanceof QuestFragment) {
            if(object instanceof QuestInProgressFragment && mFragmentAtPos2 instanceof QuestInProgressFragment) {
                return POSITION_UNCHANGED;
            }else if(object instanceof QuestFragment && mFragmentAtPos2 instanceof QuestFragment) {
                return POSITION_UNCHANGED;
            }else if(object instanceof QuestCompletedFragment && mFragmentAtPos2 instanceof QuestCompletedFragment) {
                return POSITION_UNCHANGED;
            }else {
                return POSITION_NONE;
            }
        }
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
                title = mainActivity.getResources().getString(R.string.history);
                break;
            case 1:
                title = mainActivity.getResources().getString(R.string.events);
                break;
            case 2:
                title = mainActivity.getResources().getString(R.string.quests);
                break;
        }
        return title;
    }

    public MainFragment getCurrentFragment(){
        return this.currentFragment;
    }


    @Override
    public void questStarted(MainFragment newFragment) {
        fm.beginTransaction().remove(mFragmentAtPos2).commit();
        mFragmentAtPos2 = newFragment;
        ((QuestInProgressFragment)newFragment).setQuestStartedListener(this);
        notifyDataSetChanged();
    }

    public void backButtonPressed(){
        if(mFragmentAtPos2 instanceof QuestInProgressFragment || mFragmentAtPos2 instanceof QuestCompletedFragment){
            fm.beginTransaction().remove(mFragmentAtPos2).commit();
            mFragmentAtPos2 = new QuestFragment();
            ((QuestFragment) mFragmentAtPos2).initialize(this);
            notifyDataSetChanged();
        }
    }

    @Override
    public void goBackToQuestScreen() {
        backButtonPressed();
    }

    @Override
    public void questCompleted(MainFragment fragment) {
        if(mFragmentAtPos2 instanceof QuestInProgressFragment){
            fm.beginTransaction().remove(mFragmentAtPos2).commit();
            mFragmentAtPos2 = fragment;
            ((QuestCompletedFragment)fragment).setQuestStartedListener(this);
            notifyDataSetChanged();
        }
    }
}


