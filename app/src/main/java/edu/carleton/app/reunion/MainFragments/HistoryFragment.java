package edu.carleton.app.reunion.MainFragments;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.carleton.app.reunion.ExtraFragments.RecyclerViewPopoverFragment;
import edu.carleton.app.reunion.MainActivity;
import edu.carleton.app.reunion.POJO.GeofenceInfo.AllGeofences;
import edu.carleton.app.reunion.POJO.GeofenceInfo.Event;
import edu.carleton.app.reunion.R;

import static edu.carleton.app.reunion.R.id.txt_try_getting_geofences;

/**
 * The main fragment for the History section of the app
 *
 * Displays a map with markers indicating nearby points of interest. When a marker is clicked,
 * creates a RecyclerViewPopoverFragment to display the info for that point
 */
public class HistoryFragment extends MapMainFragment{

    private View view;
    //The Markers for geofences that are currently being displayed
    ArrayList<Marker> currentGeofenceMarkers = new ArrayList<Marker>();
    AllGeofences allGeofences;

    //A Map of the info retrieved from the server where the string is the latitude
    //and longitude of the location where the events occur
    HashMap<String, ArrayList<Event>> newCurrentGeofencesInfoMap = new HashMap<>();


    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        zoomToUserLocation = false;
        view = inflater.inflate(R.layout.fragment_history, container, false);
        //Managing UI
        final TextView txtRequestGeofences = (TextView) view.findViewById(txt_try_getting_geofences);
        final Button btnRequestGeofences = (Button) view.findViewById(R.id.btn_request_geofences);
        final ImageButton btnReturnToUserLocation = (ImageButton) view.findViewById(R.id.btn_return_to_my_location);
        final ImageButton btnReturnToCampus = (ImageButton) view.findViewById(R.id.btn_return_to_campus);

        ImageView imgQuestion = (ImageView) view.findViewById(R.id.img_question);
        imgQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //displays or hides tutorial depending on whether it is in view or not
                toggleTutorial();
            }
        });
        /*If geofences weren't retrieved (likely due to network error), shows button for user
        to try requesting geofences again. If it is clicked, calls updateGeofences() to get new
        geofences and draw the necessary map markers
         */
        btnRequestGeofences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateGeofences();
                btnRequestGeofences.setVisibility(View.GONE);
                txtRequestGeofences.setText(getResources().getString(R.string.retrieving_geofences));
            }
        });
        final MainActivity mainActivity = (MainActivity) getActivity();

        btnReturnToUserLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomCamera = true;
                setCamera(mainActivity.getLastLocation(), true);
            }
        });

        btnReturnToCampus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomCamera = true;
                setCamera(null, false);
            }
        });
        setUpMapIfNeeded(); // For setting up the MapFragment

        // Toggle tutorial if first time using app
        if (checkFirstHistoryRun()) {
            toggleTutorial();
        }

        return view;
    }

    /**
     * draws markers for all the geofences on the map
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        drawAllGeofenceMarkers();
        super.onViewCreated(view, savedInstanceState);
    }



    /**
     * If the tutorial is visible, makes it invisible. Otherwise, makes it visible
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
     * Sets up the map if necessary and possible
     *
     * @return
     */
    @Override
    /***** Sets up the map if it is possible to do so *****/
    public boolean setUpMapIfNeeded() {
        super.setUpMapIfNeeded();
        if (mMap != null) {
            //Shows history popover on marker clicks
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    showPopupNew(getContentFromMarkerNew(marker), marker.getTitle());
                    return true;
                }
            });
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets up the map (should only be called if mMap is null)
     * Monitors the zoom and target of the camera and changes them
     * if the user zooms out too much or scrolls map too far off campus.
     */
    @Override
    protected void setUpMap() {
        super.setUpMap();
        // For showing a move to my location button and a blue
        // dot to show user's location
        MainActivity mainActivity = (MainActivity) getActivity();
        mMap.setMyLocationEnabled(mainActivity.checkIfGPSEnabled());
    }



    /**
     * Adds a marker to the map for each item in geofenceToAdd
     *
     * @param geofencesToAdd
     */
    private void addMarkerNew(HashMap<String, ArrayList<Event>> geofencesToAdd){
        System.gc();
        for(Map.Entry<String, ArrayList<Event>> e : geofencesToAdd.entrySet()){
                String curGeofenceName = e.getKey();
                ArrayList<Event> geofence = e.getValue();
                Bitmap markerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.basic_map_marker);
                double lat = geofence.get(0).getGeo().getLat();
                double lon = geofence.get(0).getGeo().getLon();
                LatLng position = new LatLng(lat, lon);
                BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(markerIcon);
                MarkerOptions geofenceMarkerOptions = new MarkerOptions()
                        .position(position)
                        .title(curGeofenceName)
                        .icon(icon);
                Marker curGeofenceMarker = mMap.addMarker(geofenceMarkerOptions);
                currentGeofenceMarkers.add(curGeofenceMarker);
            }
    }


    /**
     * Updates map view to reflect user's new location
     *
     * @param newLocation
     */
    @Override
    public void handleLocationChange(Location newLocation) {
        super.handleLocationChange(newLocation);
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity.mLastLocation != null) {
            setUpMapIfNeeded();
        }
    }


    /**
     * Returns the GeofenceInfoContent[] of info that each marker represents
     * @param marker
     * @return
     */
    private ArrayList<Event> getContentFromMarkerNew(Marker marker){
        return newCurrentGeofencesInfoMap.get(marker.getTitle());
    }

    /**
     * shows popup displaying information about a point of interest
     * @param events
     * @param name
     */
    public void showPopupNew(ArrayList<Event> events, String name){
        RelativeLayout relLayoutTutorial = (RelativeLayout) view.findViewById(R.id.tutorial);
        relLayoutTutorial.setVisibility(View.GONE);
        ArrayList<Event> sortedEvents = sortByDateNew(events);
        FragmentManager fm = getActivity().getSupportFragmentManager();
        RecyclerViewPopoverFragment recyclerViewPopoverFragment = RecyclerViewPopoverFragment.newInstance(sortedEvents, name);
        // Transaction start
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom,
                R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
        fragmentTransaction.add(R.id.fragment_container, recyclerViewPopoverFragment, "RecyclerViewPopoverFragment");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


    /**
     * Uses bubble sort to sort geofenceInfoContents by date
     *
     * @param geofenceInfoContents
     * @return
     */
    private ArrayList<Event> sortByDateNew(ArrayList<Event> geofenceInfoContents){
        Event infoContent = null;
        for(int j = 0; j < geofenceInfoContents.size(); j++) {
            for (int i = 0; i < geofenceInfoContents.size(); i++) {
                infoContent = geofenceInfoContents.get(i);
                if (i < geofenceInfoContents.size() - 1) {
                    String year1 = infoContent.getStartDate().getYear();
                    String year2 = geofenceInfoContents.get(i+1).getStartDate().getYear();
                    int year1Int = Integer.parseInt(year1);
                    int year2Int = Integer.parseInt(year2);
                    if (year1Int < year2Int) {
                        geofenceInfoContents.set(i, geofenceInfoContents.get(i+1));
                        geofenceInfoContents.set(i+1, infoContent);
                    }
                }
            }
        }
        return geofenceInfoContents;
    }

    /**
     * Gets new geofences
     * and draws markers on the map
     */
    public void updateGeofences() {

        MainActivity mainActivity = (MainActivity) getActivity();

        if(mainActivity.getAllGeofencesNew() == null) {
            mainActivity.requestGeofences();
        }else{
            allGeofences = mainActivity.getAllGeofencesNew();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view = null;
        currentGeofenceMarkers = null;

    }

    /**
     * Adds new geofence info and draws map markers for it
     * @param allGeofences
     */
    public void addNewGeofenceInfoNew(AllGeofences allGeofences){
        this.allGeofences = allGeofences;
        if(allGeofences == null){
            showUnableToRetrieveGeofences();
        }else if(allGeofences.getEvents() == null){
            showUnableToRetrieveGeofences();
        }else if(allGeofences.getEvents().length == 0){
            showUnableToRetrieveGeofences();
        }else {
            hideUnableToRetrieveGeofences();
            newCurrentGeofencesInfoMap.clear();
            for (int i = 0; i < allGeofences.getEvents().length; i++) {
                Event event = allGeofences.getEvents()[i];
                if(event.getGeo() != null) {
                    String name = event.getGeo().getName();
                    ArrayList<Event> eventsAtPoint = new ArrayList<>();
                    if (newCurrentGeofencesInfoMap.containsKey(name)) {
                        eventsAtPoint = newCurrentGeofencesInfoMap.get(name);
                    }
                    eventsAtPoint.add(event);
                    newCurrentGeofencesInfoMap.put(name, eventsAtPoint);
                }
            }
            addMarkerNew(newCurrentGeofencesInfoMap);
        }
    }

    /**
     * Draws geofence markers for every goefence. If there are no geofences,
     * requests them
     */
    private void drawAllGeofenceMarkers(){
        MainActivity mainActivity = (MainActivity) getActivity();
            if(mainActivity.getAllGeofencesNew() != null) {
                if(mainActivity.getAllGeofencesNew().getEvents().length == 0){
                    mainActivity.requestGeofences();
                }
                else{
                    allGeofences = mainActivity.getAllGeofencesNew();
                    addNewGeofenceInfoNew(allGeofences);
                }
        }else{
                mainActivity.requestGeofences();
            }
    }

    /**
     * shows text alerting the user that the app was unable to retrieve geofences. Prompts
     * them to connect to a network and try again
     */
    public void showUnableToRetrieveGeofences(){
        try {
            final TextView txtRequestGeofences = (TextView) view.findViewById(txt_try_getting_geofences);
            final Button btnRequestGeofences = (Button) view.findViewById(R.id.btn_request_geofences);
            txtRequestGeofences.setText(getResources().getString(R.string.no_geofences_retrieved));
            txtRequestGeofences.setVisibility(View.VISIBLE);
            btnRequestGeofences.setVisibility(View.VISIBLE);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    /**
     * hides text alerting the user that the app was unable to retrieve geofences.
     */
    private void hideUnableToRetrieveGeofences(){
        try {
            final TextView txtRequestGeofences = (TextView) view.findViewById(txt_try_getting_geofences);
            final Button btnRequestGeofences = (Button) view.findViewById(R.id.btn_request_geofences);
            txtRequestGeofences.setVisibility(View.GONE);
            btnRequestGeofences.setVisibility(View.GONE);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    /**
     * When fragment comes into view, gets geofences from mainActivity. If it
     * has no geofences, requests them
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        MainActivity mainActivity = (MainActivity) getActivity();
        if(isVisibleToUser && isResumed()) {
            allGeofences = mainActivity.getAllGeofencesNew();
            if (allGeofences == null) {
                mainActivity.requestGeofences();
            } else {
                mainActivity.requestGeofences();
                addNewGeofenceInfoNew(allGeofences);
            }
        }if(isVisibleToUser){
            if(mMap != null){
                mMap.setMyLocationEnabled(true);
            }
        }else{
            if(mMap != null){
                mMap.setMyLocationEnabled(false);
            }
        }
    }

    /**
     * Lifecycle method overridden to set up the map and check for internet connectivity
     * when the fragment comes into focus.
     */
    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainActivity = (MainActivity) getActivity();
        setUpMapIfNeeded();
        if(isVisible()){
            mMap.setMyLocationEnabled(mainActivity.checkIfGPSEnabled());
            allGeofences = mainActivity.getAllGeofencesNew();
            if (allGeofences == null) {
                mainActivity.requestGeofences();
            } else {
                mainActivity.requestGeofences();
                addNewGeofenceInfoNew(allGeofences);
            }
        }
    }

}
