package edu.carleton.app.reunion.MainFragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.lsjwzh.widget.recyclerviewpager.RecyclerViewPager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.carleton.app.reunion.Adapters.EventDateCardAdapter;
import edu.carleton.app.reunion.Adapters.EventsListAdapter;
import edu.carleton.app.reunion.MainActivity;
import edu.carleton.app.reunion.POJO.EventObject.EventContent;
import edu.carleton.app.reunion.R;

import static edu.carleton.app.reunion.R.id.btn_refresh;
import static edu.carleton.app.reunion.R.id.txt_request_events;

/**
 * The main fragment for the Events portion of the app. Displays
 * an events calendar
 *
 */
public class EventsFragment extends MainFragment {

    private Button btnTryAgain;
    private TextView txtTryAgain;
    private ArrayList<EventContent> eventsList = new ArrayList<>();
    ArrayList<String> dateInfo = new ArrayList<>();

    private EventsListAdapter eventsListAdapter;
    RecyclerView eventsListView;
    private LinearLayoutManager eventsLayoutManager;

    // RecyclerView Pager
    private static View v;
    private RecyclerViewPager dates;
    private LinkedHashMap<String, Integer> eventsMapByDate;
    private LinearLayoutManager dateLayoutManager;
    private EventDateCardAdapter eventDateCardAdapter;

    public EventsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_events, container, false);
        btnTryAgain = (Button) v.findViewById(R.id.btn_try_getting_events);
        txtTryAgain = (TextView) v.findViewById(R.id.txt_request_events);

        // Before buildRecyclerViews is called, we need to grab all events
        MainActivity mainActivity = (MainActivity) getActivity();
        eventsMapByDate = mainActivity.getEventsMapByDate();
        ArrayList<EventContent> eventContents = mainActivity.getAllEvents();



        /*If no events were retrieved, displays this button so the user can click
        to try again once the network is connected
         */
        btnTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtTryAgain.setText(getString(R.string.requesting_events));
                btnTryAgain.setVisibility(View.GONE);
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.requestEvents();
            }
        });

        // Build RecyclerViews to display date tabs
        buildRecyclerViewsDates();
        buildRecyclerViewEvents();

        ImageButton btnRefresh = (ImageButton) v.findViewById(btn_refresh);
        btnRefresh.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        refreshEvents();
                    }
                });

        if(eventsMapByDate == null){
            mainActivity.requestEvents();
        }else{
            mainActivity.requestEvents();
            handleNewEvents(eventsMapByDate, eventContents);
        }

        return v;
    }

    /**
     * Builds the recycler view for the dates
     */
    private void buildRecyclerViewsDates(){
        Log.i("EVENTS BUG", "Building date recycler views");

        dates = (RecyclerViewPager) v.findViewById(R.id.lst_event_dates);
        dateLayoutManager = new LinearLayoutManager(getActivity());
        dateLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        dates.setLayoutManager(dateLayoutManager);
        eventDateCardAdapter = new EventDateCardAdapter(dateInfo);
        dates.setAdapter(eventDateCardAdapter);
        /*
        updates the events to show from the date of the day selected on
         */
        dates.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrollStateChanged(RecyclerView rv, int state) {
                Log.i("EVENTS BUG", "scroll state changed, state = " + state + ", scroll state idle = " + RecyclerView.SCROLL_STATE_IDLE);
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    int pos = dateLayoutManager.findFirstVisibleItemPosition();
                    Log.i("EVENTS BUG", "uncorrected pos is : " + pos);
                    //if user over-scrolls, correct to index 0
                    if (pos < 0) {
                        pos = 0;
                    }

                    int firstCompleteleyVisibleItemPos = eventsLayoutManager.findFirstVisibleItemPosition();
                    if (firstCompleteleyVisibleItemPos < 0) {
                        firstCompleteleyVisibleItemPos = 0;
                    }

                    Log.i("EVENTS BUG", "pos = " + pos + "firstCompletelyVisibleItemPos = " + firstCompleteleyVisibleItemPos);

                    String dateByDayDateScroller = dateInfo.get(pos);
                    String startTimeString = eventsList.get(firstCompleteleyVisibleItemPos).getStartTime();
                    String[] completeDateArray = startTimeString.split("T");
                    String dateByDay = completeDateArray[0];
                    Log.i("EVENTS BUG", "check if date is right");
                    Log.i("EVENTS BUG", "dateByDay is : " + dateByDay + " dateByDayDateScrollerIs : " + dateByDayDateScroller);

                    if (!dateByDay.equals(dateByDayDateScroller)) {
                        Log.i("EVENTS BUG", "date not right");

                        int index = 0;
                        for (int i = 0; i < eventsList.size(); i++) {
                            startTimeString = eventsList.get(i).getStartTime();
                            completeDateArray = startTimeString.split("T");
                            dateByDay = completeDateArray[0];
                            if (dateByDay.equals(dateByDayDateScroller)) {
                                index = i;
                                break;
                            }
                        }
                        Log.i("EVENTS BUG", "updating events list to index : " + index);
                        updateEventsList(index);
                    }
                    else{
                        Log.i("EVENTS BUG", "date is right");

                    }
                }
            }
        });
        eventDateCardAdapter.notifyDataSetChanged();
    }

    /**
     * Builds the recycler view to display events
     */
    private void buildRecyclerViewEvents(){
        eventsListView = (RecyclerView) v.findViewById(R.id.lst_events);
        eventsLayoutManager = new LinearLayoutManager(getActivity());
        eventsLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        eventsListView.setLayoutManager(eventsLayoutManager);
        eventsListAdapter = new EventsListAdapter(((MainActivity)getActivity()), eventsList);
        eventsListView.setAdapter(eventsListAdapter);

        //if events are scrolled to a new day, updates the date recyclerview to display that day
        eventsListView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                int pos = eventsLayoutManager.findFirstVisibleItemPosition();
                int datePos = dateLayoutManager.findFirstVisibleItemPosition();
                if (datePos < 0) {
                    datePos = 0;
                }
                if(pos < 0){
                    pos = 0;
                }

                String startTimeString = eventsList.get(pos).getStartTime();
                String[] completeDateArray = startTimeString.split("T");
                String dateByDay = completeDateArray[0];
                if (!dateByDay.equals(dateInfo.get(datePos))) {
                    int index = 0;
                    for (int i = 0; i < dateInfo.size(); i++) {
                        if (dateByDay.equals(dateInfo.get(i))) {
                            index = i;
                            break;
                        }
                    }
                    updateDateScroller(index);
                }
            }
        });
        eventsListAdapter.notifyDataSetChanged();
    }


    /**
     * Handles new events
     * @param eventsMapByDate
     */
    @Override
    public void handleNewEvents(LinkedHashMap<String, Integer> eventsMapByDate, ArrayList<EventContent> events) {
        this.eventsMapByDate = eventsMapByDate;
        if(eventsMapByDate == null) {
            showUnableToRetrieveEvents();
        }else if(eventsMapByDate.size() == 0){
            showNoEventsHappening();
        }else {
            this.eventsList.clear();
            for(int i = 0; i< events.size(); i++){
                this.eventsList.add(events.get(i));
            }
            dateInfo.clear();
            for (Map.Entry<String, Integer> entry : eventsMapByDate.entrySet()) {
                dateInfo.add(entry.getKey());
            }
            eventDateCardAdapter.notifyDataSetChanged();
            hideUnableToRetrieveEvents();
        }
        eventsListAdapter.notifyDataSetChanged();
    }

    /**
     * When a recycler view date selector is scrolled to, shows events for that day
     *
     * @param pos
     */
    private void updateEventsList(int pos){
        eventsLayoutManager.scrollToPositionWithOffset(pos, 0);
        eventsListAdapter.notifyDataSetChanged();
    }

    /**
     * When an event on a new day is scrolled to, updates date scroller to
     * show that day
     * @param pos
     */
    private void updateDateScroller(int pos){
        dates.scrollToPosition(pos);
        eventDateCardAdapter.notifyDataSetChanged();

    }

    /**
     * shows a message saying the app couldn't retrieve events. Prompts user
     * to connect to internet and try again
     */
    public void showUnableToRetrieveEvents(){
        final TextView txtRequestGeofences = (TextView) v.findViewById(txt_request_events);
        final Button btnRequestGeofences = (Button) v.findViewById(R.id.btn_try_getting_events);
        txtRequestGeofences.setText(getResources().getString(R.string.no_events_retrieved));
        txtRequestGeofences.setVisibility(View.VISIBLE);
        btnRequestGeofences.setVisibility(View.VISIBLE);
        eventsListView.setVisibility(View.GONE);
    }

    /**
     * Shows a message telling the user there are no future events happening for whatever
     * event the app is being used for
     */
    private void showNoEventsHappening(){
        final TextView txtRequestGeofences = (TextView) v.findViewById(txt_request_events);
        final Button btnRequestGeofences = (Button) v.findViewById(R.id.btn_try_getting_events);
        txtRequestGeofences.setText(getResources().getString(R.string.no_events_listed));
        txtRequestGeofences.setVisibility(View.VISIBLE);
        btnRequestGeofences.setVisibility(View.GONE);
        eventsListView.setVisibility(View.GONE);
    }

    /**
     * hides the message and button that display the unable to retrieve events button
     */
    private void hideUnableToRetrieveEvents(){
        eventsListView.setVisibility(View.VISIBLE);
        final TextView txtRequestGeofences = (TextView) v.findViewById(txt_request_events);
        final Button btnRequestGeofences = (Button) v.findViewById(R.id.btn_try_getting_events);
        txtRequestGeofences.setVisibility(View.GONE);
        btnRequestGeofences.setVisibility(View.GONE);
    }

    /**
     * requests events if there are no events when the fragment comes into view
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && isResumed()) {
            MainActivity mainActivity = (MainActivity) getActivity();
            eventsMapByDate = mainActivity.getEventsMapByDate();
            ArrayList<EventContent> eventsArray = mainActivity.getAllEvents();
            if (eventsMapByDate == null || eventsList == null) {
                mainActivity.requestEvents();
            } else {
                mainActivity.requestEvents();
                handleNewEvents(eventsMapByDate, eventsArray);
            }
        }
    }

    public void refreshEvents() {
        MainActivity mainActivity = (MainActivity) getActivity();
        eventsMapByDate = mainActivity.getEventsMapByDate();
        ArrayList<EventContent> eventsArray = mainActivity.getAllEvents();
        if (eventsMapByDate == null || eventsList == null) {
            mainActivity.requestEvents();
        } else {
            mainActivity.requestEvents();
            handleNewEvents(eventsMapByDate, eventsArray);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if(isVisible()){
           refreshEvents();
        }
    }


}
