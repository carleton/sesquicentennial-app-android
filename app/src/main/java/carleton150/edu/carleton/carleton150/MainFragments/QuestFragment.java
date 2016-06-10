package carleton150.edu.carleton.carleton150.MainFragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lsjwzh.widget.recyclerviewpager.RecyclerViewPager;

import java.util.ArrayList;

import carleton150.edu.carleton.carleton150.Adapters.QuestAdapter;
import carleton150.edu.carleton.carleton150.ExtraFragments.QuestCompletedFragment;
import carleton150.edu.carleton.carleton150.Interfaces.QuestStartedListener;
import carleton150.edu.carleton.carleton150.Interfaces.RecyclerViewClickListener;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;
import carleton150.edu.carleton.carleton150.POJO.Quests.Waypoint;
import carleton150.edu.carleton.carleton150.R;

import static carleton150.edu.carleton.carleton150.R.id.txt_request_quests;

/**
 * Class to display quests and allow a user to select a quest to start or resume it
 */
public class QuestFragment extends MainFragment implements RecyclerViewClickListener {

    private ArrayList<Quest> questInfo;
    private LinearLayoutManager questLayoutManager;
    private QuestAdapter questAdapter;
    private int screenWidth;
    private View view;
    private QuestStartedListener questStartedListener;


    public QuestFragment() {
        // Required empty public constructor
    }

    public void initialize(QuestStartedListener questStartedListener){
        this.questStartedListener = questStartedListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final TextView txtInfo;
        final Button btnTryAgain;
        view =  inflater.inflate(R.layout.fragment_quest, container, false);
        final MainActivity mainActivity = (MainActivity) getActivity();

        /*Button for user to try getting quests again if the app was unable
        to get them from the server
         */
        btnTryAgain = (Button) view.findViewById(R.id.btn_try_getting_quests);
        txtInfo = (TextView) view.findViewById(R.id.txt_request_quests);
        btnTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnTryAgain.setVisibility(View.GONE);
                txtInfo.setText(getString(R.string.retrieving_quests));
                if(mainActivity.getQuests() == null){
                    mainActivity.requestQuests();
                }else{
                    handleNewQuests(mainActivity.getQuests());
                }

            }
        });

        ImageView imgQuestion = (ImageView) view.findViewById(R.id.img_question);
        imgQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTutorial();
            }
        });

        //builds RecyclerViews to display quests
        buildRecyclerViews();

        // Toggle tutorial if first time using app
        if (checkFirstQuestRun()) {
            toggleTutorial();
        }

        //requests quests if mainActivity doesn't already have them
        questInfo = mainActivity.getQuests();
        if(questInfo == null){
            mainActivity.requestQuests();
        }else if(questInfo.size() == 0){
            mainActivity.requestQuests();
        }
        else{
            handleNewQuests(questInfo);
        }

        return view;
    }

    public void setQuests(ArrayList<Quest> allQuests){
        this.questInfo = allQuests;
    }

    /**
     * Handles when a quest was clicked beginning the quest
     * @param v
     * @param position
     */
    @Override
    public void recyclerViewListClicked(View v, final int position) {
        beginQuest(questAdapter.getQuestList().get(position));
    }

    /**
     * If the quest was not already started at some point, starts it. Otherwise,
     * checkIfQuestStarted() displays a dialog asking if the user wants to resume
     * the quest or start over
     * @param quest quest to begin
     */
    private void beginQuest(Quest quest){
        if(!checkIfQuestStarted(quest)){
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setQuestInProgress(quest);
            QuestInProgressFragment fr=new QuestInProgressFragment();
            fr.initialize(quest, false);
            if(questStartedListener != null) {
                questStartedListener.questStarted(fr);
            }else{
                questStartedListener = mainActivity.getQuestStartedListener();
                questStartedListener.questStarted(fr);
            }
        }
    }

    /**
     * Checks if a quest was already started. If so, gives user the option of resuming
     * the quest or starting over
     * @return true if quest was started, false otherwise
     */
    private boolean checkIfQuestStarted(Quest quest){
        MainActivity mainActivity = (MainActivity) getActivity();
        SharedPreferences sharedPreferences = mainActivity.getPersistentQuestStorage();
        int curClue = sharedPreferences.getInt(quest.getName(), 0);
        if(curClue != 0){
            showOptionToResumeQuest(quest);
            return true;
        }else{
            return false;
        }
    }

    /**
     * Shows user a dialog asking if they want to start over or resume the current quest.
     * If they choose to resume, calls a method to resume the quest. Otherwise, starts quest over
     */
    private void showOptionToResumeQuest(final Quest quest){
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.showAlertDialogNoNeutralButton(new AlertDialog.Builder(mainActivity)
                .setMessage(getString(R.string.quest_started))
                .setPositiveButton(getString(R.string.resume), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.setQuestInProgress(quest);
                        mainActivity.setResume(true);
                        boolean questCompleted = checkIfQuestCompleted(quest);
                        if(questCompleted){
                            showQuestCompletedFragment(quest);
                        }else {
                            QuestInProgressFragment fr = new QuestInProgressFragment();

                            fr.initialize(quest, true);
                            if (questStartedListener == null) {
                                questStartedListener = mainActivity.getQuestStartedListener();
                            }
                            questStartedListener.questStarted(fr);
                        }
                        dialog.cancel();
                    }
                })
                .setNegativeButton(getString(R.string.start_over), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.setQuestInProgress(quest);
                        mainActivity.setResume(false);
                        QuestInProgressFragment fr = new QuestInProgressFragment();
                        fr.initialize(quest, false);
                        if (questStartedListener == null) {
                            questStartedListener = mainActivity.getQuestStartedListener();
                        }
                        questStartedListener.questStarted(fr);
                        dialog.cancel();
                    }
                })
                .create());
    }

    private boolean checkIfQuestCompleted(Quest quest){
        Waypoint[] waypoints = quest.getWaypoints();
        MainActivity mainActivity = (MainActivity) getActivity();
        int curClue = mainActivity.getPersistentQuestStorage().getInt(quest.getName(), 0);

        if(curClue == waypoints.length){
            return true;
        }
        return false;
    }

    private void showQuestCompletedFragment(Quest quest){
        QuestCompletedFragment fr = new QuestCompletedFragment();
        fr.initialize(quest);
        if(questStartedListener == null){
            MainActivity mainActivity = (MainActivity) getActivity();
            questStartedListener = mainActivity.getQuestStartedListener();
        }
        questStartedListener.questCompleted(fr);
        fr.setQuestStartedListener(questStartedListener);
    }

    /**
     * Builds the views for the quests
     */
    private void buildRecyclerViews(){
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        RecyclerViewPager quests = (RecyclerViewPager) view.findViewById(R.id.lst_quests);
        questLayoutManager = new LinearLayoutManager(getActivity());
        questLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        quests.setLayoutManager(questLayoutManager);
        MainActivity mainActivity = (MainActivity) getActivity();
        questAdapter = new QuestAdapter(questInfo, this,
                getResources(), mainActivity.getPersistentQuestStorage());
        quests.setAdapter(questAdapter);
    }

    /**
     * Sets view items to null when view is destroyed to avoid
     * memory leaks
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view = null;
        questAdapter = null;
        questLayoutManager = null;
        questInfo = null;
    }

    /**
     * If the tutorial is in view, hides it. Otherwise, shows it
     */
    private void toggleTutorial(){
        final RelativeLayout relLayoutTutorial = (RelativeLayout) view.findViewById(R.id.tutorial);
        if(relLayoutTutorial.getVisibility() == View.VISIBLE){
            relLayoutTutorial.setVisibility(View.GONE);
        }else{
            relLayoutTutorial.setVisibility(View.VISIBLE);
        }
        Button btnCloseTutorial = (Button) view.findViewById(R.id.btn_close_tutorial);
        btnCloseTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                relLayoutTutorial.setVisibility(View.GONE);
            }
        });
    }


    /**
     * Called by MainActivity, handles new quests from the server
     * @param newQuests
     */
    @Override
    public void handleNewQuests(ArrayList<Quest> newQuests) {
        if(newQuests == null){
            showUnableToRetrieveQuests();
        }else if(newQuests.size() == 0){
            showUnableToRetrieveQuests();
        }else {
            hideUnableToRetrieveQuests();
            try {
                RecyclerViewPager quests = (RecyclerViewPager) view.findViewById(R.id.lst_quests);

                super.handleNewQuests(newQuests);
                if (newQuests == null) {
                    if (quests != null) {
                        quests.setVisibility(View.GONE);
                    }
                    return;
                } else if (newQuests != null) {
                    questInfo = newQuests;
                    questAdapter.updateQuestList(questInfo);
                    questAdapter.notifyDataSetChanged();
                    quests.setVisibility(View.VISIBLE);
                }
                if (questInfo == null) {
                    if (quests != null) {
                        quests.setVisibility(View.GONE);
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * shows text saying the app was unable to retrieve quests. Prompts user to connect
     * to network and try again
     */
    public void showUnableToRetrieveQuests(){
        final TextView txtRequestGeofences = (TextView) view.findViewById(txt_request_quests);
        final Button btnRequestGeofences = (Button) view.findViewById(R.id.btn_try_getting_quests);
        txtRequestGeofences.setText(getResources().getString(R.string.no_quests_retrieved));
        txtRequestGeofences.setVisibility(View.VISIBLE);
        btnRequestGeofences.setVisibility(View.VISIBLE);
    }

    /**
     * hides text saying the app was unable to retrieve quests. Prompts user to connect
     * to network and try again
     */
    private void hideUnableToRetrieveQuests(){
        final TextView txtRequestGeofences = (TextView) view.findViewById(txt_request_quests);
        final Button btnRequestGeofences = (Button) view.findViewById(R.id.btn_try_getting_quests);
        txtRequestGeofences.setVisibility(View.GONE);
        btnRequestGeofences.setVisibility(View.GONE);
    }

    /**
     * gets quests from MainActivity when fragment comes into view. If MainActivity
     * has no quests, requests them
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && isResumed()) {
            MainActivity mainActivity = (MainActivity) getActivity();
            questInfo = mainActivity.getQuests();
            if (questInfo == null) {
                mainActivity.requestQuests();
            } else {
                mainActivity.requestQuests();
                handleNewQuests(questInfo);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isVisible()){
            MainActivity mainActivity = (MainActivity) getActivity();
            questInfo = mainActivity.getQuests();
            if (questInfo == null) {
                mainActivity.requestQuests();
            } else {
                mainActivity.requestQuests();
                handleNewQuests(questInfo);
            }
        }
    }
}
