package carleton150.edu.carleton.carleton150.ExtraFragments;


import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import carleton150.edu.carleton.carleton150.Interfaces.QuestStartedListener;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.MainFragments.MainFragment;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;
import carleton150.edu.carleton.carleton150.R;

/**
 * Fragment to display quest completed animation and message
 */
public class QuestCompletedFragment extends MainFragment {

    private Quest quest;
    private View v;
    private QuestStartedListener questStartedListener;

    public QuestCompletedFragment() {
        // Required empty public constructor
    }

    public void initialize(Quest quest){
        this.quest = quest;
    }

    public void setQuestStartedListener(QuestStartedListener questStartedListener){
        this.questStartedListener = questStartedListener;
    }

    /**
     * manages UI
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        System.gc();
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_quest_completed, container, false);
        ImageView imgQuestCompletedAnim = (ImageView) v.findViewById(R.id.img_animation_quest_completed);
        Button btnDone = (Button) v.findViewById(R.id.btn_done_with_quest);
        TextView txtNumCompleted = (TextView) v.findViewById(R.id.txt_clue_number_comp_window);
        TextView txtCompMsg = (TextView) v.findViewById(R.id.txt_completion_message);
        RelativeLayout relLayoutQuestCompleted = (RelativeLayout) v.findViewById(R.id.rel_layout_quest_completed);

        final MainActivity mainActivity = (MainActivity) getActivity();


        if(quest == null){
            quest = mainActivity.getQuestInProgress();
        }
        if(quest == null){
            mainActivity.questInProgressGoBack();
        }

        txtCompMsg.setText(quest.getCompMsg());
        txtNumCompleted.setText(quest.getWaypoints().length + "/" + quest.getWaypoints().length);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                imgQuestCompletedAnim.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.anim_quest_completed));
                ((AnimationDrawable) imgQuestCompletedAnim.getBackground()).start();

            } catch (OutOfMemoryError e) {
                imgQuestCompletedAnim.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.qanim25));
            }
        }else{
            imgQuestCompletedAnim.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.qanim25));
        }

        imgQuestCompletedAnim.setVisibility(View.VISIBLE);
        relLayoutQuestCompleted.setVisibility(View.VISIBLE);

        txtNumCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressPopup();
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                questStartedListener.goBackToQuestScreen();
            }
        });

        return v;
    }


    /**
     * Shows the history popover for a given marker on the map
     *
     * @param
     */
    private void showProgressPopup(){

        ImageView imgQuestCompleted = (ImageView) v.findViewById(R.id.img_animation_quest_completed);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            imgQuestCompleted.setBackground(getResources().getDrawable(R.drawable.bg_transparent));
        }
        imgQuestCompleted.setImageDrawable(getResources().getDrawable(R.drawable.qanim25));
        System.gc();
        FragmentManager fm = getActivity().getSupportFragmentManager();
        RecyclerViewPopoverFragment recyclerViewPopoverFragment = RecyclerViewPopoverFragment.newInstance(quest, quest.getWaypoints().length);
        // Transaction start
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom,
                R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
        fragmentTransaction.add(R.id.fragment_container_quest_completed, recyclerViewPopoverFragment, "QuestProgressPopoverFragment");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


    @Override
    public void onDestroyView() {
        ImageView imgQuestCompleted = (ImageView) v.findViewById(R.id.img_animation_quest_completed);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            imgQuestCompleted.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.bg_transparent));
            System.gc();
        }else{
            imgQuestCompleted.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.bg_transparent));
        }
        super.onDestroyView();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        MainActivity mainActivity = (MainActivity) getActivity();
        if(quest == null && isVisibleToUser && isResumed()){
            mainActivity.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainActivity = (MainActivity) getActivity();
        if(getUserVisibleHint() && quest == null){
            mainActivity.onBackPressed();
        }
    }

    public void inView(){
        MainActivity mainActivity = (MainActivity) getActivity();
        if(quest == null && getUserVisibleHint() && isResumed()){
            mainActivity.onBackPressed();
        }
    }
}
