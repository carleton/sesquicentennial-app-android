package carleton150.edu.carleton.reunion.ExtraFragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import carleton150.edu.carleton.reunion.Adapters.HistoryAdapter;
import carleton150.edu.carleton.reunion.Adapters.MyScaleInAnimationAdapter;
import carleton150.edu.carleton.reunion.POJO.GeofenceInfo.Event;
import carleton150.edu.carleton.reunion.POJO.Quests.Quest;
import carleton150.edu.carleton.reunion.POJO.Quests.Waypoint;
import carleton150.edu.carleton.reunion.R;

/**
 * Class to manage a RecyclerViewPopoverFragment. This is used to show the popover for
 * history and quest progress.
 */
public class RecyclerViewPopoverFragment extends Fragment{

    private View view;
    private RecyclerView historyInfoObjects;
    private LinearLayoutManager historyLayoutManager;
    private HistoryAdapter historyAdapter;
    private Button btnClose;
    private int screenWidth;
    private int screenHeight;
    private static boolean isQuestInProgress = false;
    private static String geofenceName;
    private static ArrayList<Event> geofenceInfoObjectNew;

    private static Quest quest;
    private static int progressThroughQuest;


    public RecyclerViewPopoverFragment()
    {
        //required empty public constructor
    }

    /**
     * Creates a new instance of the RecyclerViewPopoverFragment where the ArrayList of Events
     * to display in the RecyclerView is provided. This is called when creating a history popover
     *
     * @param object the GeofenceInfoContent[] for the RecyclerViewPopoverFragment to display
     * @return the RecyclerViewPopoverFragment that was created
     */
    public static RecyclerViewPopoverFragment newInstance(ArrayList<Event> object, String name) {
        RecyclerViewPopoverFragment f = new RecyclerViewPopoverFragment();
        geofenceInfoObjectNew = object;
        geofenceName = name;
        isQuestInProgress = false;
        return f;
    }

    /**
     * Creates a new instance of the RecyclerViewPopoverFragment where the array to be displayed is
     * the quest waypoints that were completed by the user. This method is to be called by the
     * QuestInProgress fragment or the QuestCompletedFragment
     *
     * @param mQuest the user's current quest
     * @param mProgress the user's progress through the quest
     * @return the RecyclerViewPopoverFragment that was created
     */
    public static RecyclerViewPopoverFragment newInstance(Quest mQuest, int mProgress){
        RecyclerViewPopoverFragment f = new RecyclerViewPopoverFragment();
        quest = mQuest;
        progressThroughQuest = mProgress;
        isQuestInProgress = true;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        //Manages view
        view = getActivity().getLayoutInflater().
                inflate(R.layout.fragment_history_popover, new LinearLayout(getActivity()), false);
        TextView txtTitle = (TextView) view.findViewById(R.id.txt_title);
        btnClose = (Button) view.findViewById(R.id.btn_exit_popup);


        //Closes the RecyclerViewPopoverFragment when the close button is pressed
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrentFragment();
            }
        });

        if(!isQuestInProgress) {
            //If this is being used to show the history, sets the title to the name of the geofence
            if(geofenceName != null) {
                txtTitle.setText(geofenceName);
            }else{
                txtTitle.setText("");
            }
        }else if(isQuestInProgress){
            txtTitle.setText(getString(R.string.progress_through_quest_title));
        }
        //builds RecyclerViews to display info
        buildRecyclerViews();

        return view;
    }

    /**
     * Removes this fragment from the view
     */
    private void removeCurrentFragment(){
        if(historyAdapter != null) {
            historyAdapter.closeAdapter();
        }
        FragmentTransaction fm = getActivity().getSupportFragmentManager().beginTransaction();
        fm.setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
        fm.detach(this).remove(this).commit();

    }


    /**
     * Determines what the fragment is being used for and builds the appropriate RecyclerView
     */
    private void buildRecyclerViews(){
            historyInfoObjects = (RecyclerView) view.findViewById(R.id.lst_history_items);
            historyLayoutManager = new LinearLayoutManager(getActivity());
            historyLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            historyInfoObjects.setLayoutManager(historyLayoutManager);
            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;

            if(!isQuestInProgress) {
                buildNewHistoryRecyclerView();
            }else if (isQuestInProgress){
                buildQuestProgressRecyclerView();
            }
    }


    /**
     * Builds a RecyclerView when the RecyclerViewPopoverFragment is being used to display
     * history info
     */
    private void buildNewHistoryRecyclerView(){
        Event[] events = new Event[geofenceInfoObjectNew.size()];
        for(int i = 0 ; i<geofenceInfoObjectNew.size(); i++){
            events[i] = geofenceInfoObjectNew.get(i);
        }
        historyAdapter = new HistoryAdapter(getActivity(), events, null, screenWidth,
                screenHeight, isQuestInProgress);
        //RecyclerView animation
        MyScaleInAnimationAdapter scaleInAnimationAdapter = new MyScaleInAnimationAdapter(historyAdapter);
        scaleInAnimationAdapter.setFirstOnly(false);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        historyInfoObjects.setAdapter(scaleInAnimationAdapter);
        historyAdapter.notifyDataSetChanged();
    }

    /**
     * Builds a RecyclerView when the RecyclerViewPopoverFragment is being used to display
     * quest progress
     */
    private void buildQuestProgressRecyclerView(){
        Waypoint[] waypoints = quest.getWaypoints();
        Waypoint[] completedWaypoints = new Waypoint[progressThroughQuest];
        for(int i = 0; i<progressThroughQuest; i++){
            completedWaypoints[i] = waypoints[i];
        }
        historyAdapter = new HistoryAdapter(getActivity(), null, completedWaypoints, screenWidth, screenHeight, true);
        MyScaleInAnimationAdapter scaleInAnimationAdapter = new MyScaleInAnimationAdapter(historyAdapter);
        scaleInAnimationAdapter.setFirstOnly(false);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        historyInfoObjects.setAdapter(scaleInAnimationAdapter);
        historyAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view = null;
        historyInfoObjects = null;
        historyLayoutManager = null;
        historyAdapter = null;
        btnClose = null;
    }

}
