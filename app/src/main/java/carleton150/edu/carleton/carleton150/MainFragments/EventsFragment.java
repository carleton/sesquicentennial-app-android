package carleton150.edu.carleton.carleton150.MainFragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import carleton150.edu.carleton.carleton150.Adapters.EventDateCardAdapter;
import carleton150.edu.carleton.carleton150.Adapters.EventsListAdapter;
import com.lsjwzh.widget.recyclerviewpager.RecyclerViewPager;

import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.POJO.EventObject.EventContent;
import carleton150.edu.carleton.carleton150.R;

import static carleton150.edu.carleton.carleton150.R.id.txt_request_events;

/**
 * The main fragment for the Events portion of the app. Displays
 * and events calendar
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
    private int screenWidth;
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
        buildRecyclerViews();
        buildEventRecyclerView();

        //TODO: can have zero events
        if(eventsMapByDate == null){
            Log.i("EVENTS DEBUGGING", "EventsFragment : onCreateView: eventsMapByDate is null, requesting events");
            mainActivity.requestEvents();
        }else{
            Log.i("EVENTS DEBUGGING", "EventsFragment : onCreateView: eventsMapByDate is not null, handling new events");
            Log.i("EVENT DEBUGGING",
                    "EventsFragment: onCreateView : eventsList size is: "+ eventsList.size());
            handleNewEvents(eventsMapByDate, eventContents);
        }

        return v;
    }

    /**
     * Builds the views for the dates
     */
    private void buildRecyclerViews(){
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        dates = (RecyclerViewPager) v.findViewById(R.id.lst_event_dates);
        //dates.setScreenWidth(screenWidth);
        dateLayoutManager = new LinearLayoutManager(getActivity());

        dateLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        dates.setLayoutManager(dateLayoutManager);

        eventDateCardAdapter = new EventDateCardAdapter(dateInfo, screenWidth);
        dates.setAdapter(eventDateCardAdapter);

        /*
        updates the events to show from the date of the middle day selected on
         */
        dates.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrollStateChanged(RecyclerView rv, int state) {
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    int pos = dateLayoutManager.findFirstCompletelyVisibleItemPosition();
                    /*
                    There is a blank view at position 0 that is the first completely visible item if the view is scrolled
                    all the way left. In this case, we want the date at position 1 to be the date we use
                     */
                    if (pos < 0) {
                        pos = 0;
                    }

                    String dateByDayDateScroller = dateInfo.get(pos);

                    String startTimeString = eventsList.get(eventsLayoutManager.findFirstCompletelyVisibleItemPosition()).getStartTime();
                    String[] completeDateArray = startTimeString.split("T");
                    String dateByDay = completeDateArray[0];
                    if (!dateByDay.equals(dateByDayDateScroller)) {
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

                        updateEventsList(index);

                    }

                }
            }
        });

        eventDateCardAdapter.notifyDataSetChanged();
    }

    private void buildEventRecyclerView(){
        eventsListView = (RecyclerView) v.findViewById(R.id.lst_events);
        eventsLayoutManager = new LinearLayoutManager(getActivity());
        eventsLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        eventsListView.setLayoutManager(eventsLayoutManager);
        eventsListAdapter = new EventsListAdapter(((MainActivity)getActivity()), eventsList);
        eventsListView.setAdapter(eventsListAdapter);

        eventsListView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                int pos = eventsLayoutManager.findFirstCompletelyVisibleItemPosition();
                int datePos = dateLayoutManager.findFirstCompletelyVisibleItemPosition();
                if (datePos <= 0) {
                    datePos = 1;
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
     * Called from VolleyRequester. Handles new events from server
     * @param eventsMapByDate
     */
    @Override
    public void handleNewEvents(LinkedHashMap<String, Integer> eventsMapByDate, ArrayList<EventContent> events) {
        Log.i("EVENTS DEBUGGING", "EventsFragment : handleNewEvents");
        this.eventsMapByDate = eventsMapByDate;
        if(eventsMapByDate == null) {
            Log.i("EVENTS DEBUGGING", "EventsFragment : handleNewEvents : eventsMapByDate is null");
            showUnableToRetrieveEvents();
        }else if(eventsMapByDate.size() == 0){
            Log.i("EVENTS DEBUGGING", "EventsFragment : handleNewEvents : eventsMapByDate size is 0");
            showNoEventsHappening();
        }else {
            this.eventsList.clear();

            Log.i("EVENTS DEBUGGING", "EventsFragment : handleNewEvents : events size is : " + events.size());

            for(int i = 0; i< events.size(); i++){
                this.eventsList.add(events.get(i));
            }
            dateInfo.clear();
            Log.i("EVENTS DEBUGGING", "EventsFragment : handleNewEvents : eventsMapByDate size is : " + eventsMapByDate.entrySet().size());
            Log.i("EVENTS DEBUGGING", "EventsFragment : handleNewEvents : eventsList size is : " + eventsList.size());


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
    }

    private void updateDateScroller(int pos){
        dates.scrollToPosition(pos);

    }


    public void showUnableToRetrieveEvents(){
        final TextView txtRequestGeofences = (TextView) v.findViewById(txt_request_events);
        final Button btnRequestGeofences = (Button) v.findViewById(R.id.btn_try_getting_events);
        txtRequestGeofences.setText(getResources().getString(R.string.no_events_retrieved));
        txtRequestGeofences.setVisibility(View.VISIBLE);
        btnRequestGeofences.setVisibility(View.VISIBLE);
        eventsListView.setVisibility(View.GONE);
    }

    private void showNoEventsHappening(){
        final TextView txtRequestGeofences = (TextView) v.findViewById(txt_request_events);
        final Button btnRequestGeofences = (Button) v.findViewById(R.id.btn_try_getting_events);
        txtRequestGeofences.setText(getResources().getString(R.string.no_events_listed));
        txtRequestGeofences.setVisibility(View.VISIBLE);
        btnRequestGeofences.setVisibility(View.GONE);
        eventsListView.setVisibility(View.GONE);
    }

    private void hideUnableToRetrieveEvents(){
        eventsListView.setVisibility(View.VISIBLE);
        final TextView txtRequestGeofences = (TextView) v.findViewById(txt_request_events);
        final Button btnRequestGeofences = (Button) v.findViewById(R.id.btn_try_getting_events);
        txtRequestGeofences.setVisibility(View.GONE);
        btnRequestGeofences.setVisibility(View.GONE);
    }

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
                Log.i("EVENT DEBUGGING",
                        "EventsFragment: setUserVisibleHint : eventsList size is: "+ eventsList.size());
                handleNewEvents(eventsMapByDate, eventsArray);
            }
        }
    }
}
