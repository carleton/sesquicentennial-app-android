package carleton150.edu.carleton.reunion.MainFragments;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

import carleton150.edu.carleton.reunion.Constants;
import carleton150.edu.carleton.reunion.POJO.EventObject.EventContent;

/**
 * Created on 10/28/15.
 * Super class for all of the main view fragments. Ensures that they have
 * some methods in common so that the MainActivity can call these methods
 * without checking which type of fragment is currently in view
 */
public class MainFragment extends Fragment{

    public static WebView myWebView;

    public void loadWebContent(){
        throw new UnsupportedOperationException("This is not a Webview");
    }

    public long timeOfLastRefresh = -1;

    /**
     * Required empty constructor
     */
    public MainFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Checks if this is app's first launch to display history tutorial
     */
    public boolean checkFirstHistoryRun() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor;

        boolean isFirstHistoryRun = sharedPreferences.getBoolean(Constants.IS_FIRST_HISTORY_RUN_STRING, true);
        if (isFirstHistoryRun) {
            editor = sharedPreferences.edit();
            editor.putBoolean(Constants.IS_FIRST_HISTORY_RUN_STRING, false);
            editor.commit();
        }
        return isFirstHistoryRun;
    }

    /**
     * Checks if this is app's first launch to display quest tutorial
     */
    public boolean checkFirstQuestRun() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isFirstQuestRun = sharedPreferences.getBoolean(Constants.IS_FIRST_QUEST_RUN_STRING, true);
        if (isFirstQuestRun) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(Constants.IS_FIRST_QUEST_RUN_STRING, false);
            editor.commit();
        }
        return isFirstQuestRun;
    }

    /**
     * handles when the user's location changes
     * @param newLocation
     */
    public void handleLocationChange(Location newLocation){

    }

    /**
     * handles new events
     * @param eventsMapByDate
     */
    public void handleNewEvents(LinkedHashMap<String, Integer> eventsMapByDate, ArrayList<EventContent> events){

    }



    /**
     *
     * @param previousTime long representation of a time
     * @return elapsed time between previousTime and current time in minutes or -1
     * if the page has not been refreshed
     */
    public long checkElapsedTime(long previousTime){
        if (previousTime == -1){
            return -1;
        }
        Calendar currentTime = Calendar.getInstance();
        java.util.Date currentDate = currentTime.getTime();
        long time = currentDate.getTime();
        //converting ms to hours
        long minutesSinceUpdate = (time - previousTime) / (1000 * 60);
        return minutesSinceUpdate;
    }

    public void setTimeOfLastRefresh(){
        Calendar currentTime = Calendar.getInstance();
        java.util.Date currentDate = currentTime.getTime();
        timeOfLastRefresh = currentDate.getTime();
    }
}


