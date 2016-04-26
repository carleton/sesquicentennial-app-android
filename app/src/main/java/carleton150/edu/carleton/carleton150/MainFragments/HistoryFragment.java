package carleton150.edu.carleton.carleton150.MainFragments;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
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

import carleton150.edu.carleton.carleton150.ExtraFragments.AddMemoryFragment;
import carleton150.edu.carleton.carleton150.ExtraFragments.RecyclerViewPopoverFragment;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceObject.GeofenceObjectContent;
import carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo.AllGeofences;
import carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo.Event;
import carleton150.edu.carleton.carleton150.R;

import static carleton150.edu.carleton.carleton150.R.id.txt_try_getting_geofences;

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

    //A Map of the info retrieved from the server for geofences the user is currently in
    //TODO: for old version
    HashMap<String, GeofenceInfoContent[]> currentGeofencesInfoMap = new HashMap<>();

    //A Map of the info retrieved from the server where the string is the latitude
    //and longitude of the location where the events occur
    //TODO: for new version
    HashMap<String, ArrayList<Event>> newCurrentGeofencesInfoMap = new HashMap<>();

    //A Map of geofences the user is in that are currently being queried for using VolleyRequester
    HashMap<String, Integer> geofenceNamesBeingQueriedForInfo = new HashMap<>();


    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            Log.i("GEOFENCE MONITORING", "onCreateView: container is null..");

            return null;
        }
        zoomToUserLocation = false;
        Log.i("GEOFENCE MONITORING", "onCreateView: called");

        view = inflater.inflate(R.layout.fragment_history, container, false);

        //Managing UI
        final TextView txtRequestGeofences = (TextView) view.findViewById(txt_try_getting_geofences);
        final Button btnRequestGeofences = (Button) view.findViewById(R.id.btn_request_geofences);
        final Button btnGetNearbyMemories = (Button) view.findViewById(R.id.btn_get_nearby_memories);
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

        btnGetNearbyMemories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //TODO: open webview with user stories -- need URL

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
                startActivity(browserIntent);

                //Shows a popover displaying nearby memories, disabled because we are no longer using this feature
                //showMemoriesPopover();
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


        if(mainActivity.isConnectedToNetwork()) {
            setUpMapIfNeeded(); // For setting up the MapFragment
        }

        // Toggle tutorial if first time using app
        if (checkFirstHistoryRun()) {
            toggleTutorial();
        }

        return view;
    }


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
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if(mainActivity.NEW_VERSION){
                        showPopupNew(getContentFromMarkerNew(marker), marker.getTitle());
                    }else {
                        showPopup(getContentFromMarker(marker), marker.getTitle());
                    }
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
     * Lifecycle method overridden to set up the map and check for internet connectivity
     * when the fragment comes into focus. If fragment is not already monitoring geofences,
     * begins monitoring geofences
     */
    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity.isConnectedToNetwork()) {
            setUpMapIfNeeded();
        }

        mMap.setMyLocationEnabled(mainActivity.checkIfGPSEnabled());
    }

    /**
     * Adds a marker to the map for each item in geofenceToAdd
     *
     * @param geofenceToAdd
     */
    private void addMarker(HashMap<String, GeofenceInfoContent[]> geofenceToAdd){
        System.gc();
        MainActivity mainActivity = (MainActivity) getActivity();
        Log.i("HistoryFragment", "addMarker");

        for(Map.Entry<String, GeofenceInfoContent[]> e : geofenceToAdd.entrySet()){

            if(!currentGeofencesInfoMap.containsKey(e.getKey())) {
                currentGeofencesInfoMap.put(e.getKey(), e.getValue());
                geofenceNamesBeingQueriedForInfo.remove(e.getKey());
                String curGeofenceName = e.getKey();
                GeofenceObjectContent geofence = mainActivity.getAllGeofencesMap().get(curGeofenceName);
                Bitmap markerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.basic_map_marker);
                LatLng position = new LatLng(geofence.getGeofence().getLocation().getLat(), geofence.getGeofence().getLocation().getLng());
                BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(markerIcon);
                Log.i("HistoryFragment", "addMarker : adding a marker!");
                MarkerOptions geofenceMarkerOptions = new MarkerOptions()
                        .position(position)
                        .title(curGeofenceName)
                        .icon(icon);
                Marker curGeofenceMarker = mMap.addMarker(geofenceMarkerOptions);
                currentGeofenceMarkers.add(curGeofenceMarker);
            }
        }
    }

    /**
     * Adds a marker to the map for each item in geofenceToAdd
     *
     * @param geofencesToAdd
     */
    private void addMarkerNew(HashMap<String, ArrayList<Event>> geofencesToAdd){
        System.gc();
        Log.i("HistoryFragment", "addMarkerNew");

        for(Map.Entry<String, ArrayList<Event>> e : geofencesToAdd.entrySet()){
                String curGeofenceName = e.getKey();
                ArrayList<Event> geofence = e.getValue();
                Bitmap markerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.basic_map_marker);
                double lat = geofence.get(0).getGeo().getLat();
                double lon = geofence.get(0).getGeo().getLon();
                LatLng position = new LatLng(lat, lon);
                BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(markerIcon);
                Log.i("HistoryFragment", "addMarker : adding a marker!");
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
    private GeofenceInfoContent[] getContentFromMarker(Marker marker){
        return currentGeofencesInfoMap.get(marker.getTitle());
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
     * Shows the history popover for a given marker on the map
     *
     * @param geofenceInfoObject
     */
    public void showPopup(GeofenceInfoContent[] geofenceInfoObject, String name){
        RelativeLayout relLayoutTutorial = (RelativeLayout) view.findViewById(R.id.tutorial);
        relLayoutTutorial.setVisibility(View.GONE);
        GeofenceInfoContent[] sortedContent = sortByDate(geofenceInfoObject);
        FragmentManager fm = getActivity().getSupportFragmentManager();
        RecyclerViewPopoverFragment recyclerViewPopoverFragment = RecyclerViewPopoverFragment.newInstance(sortedContent, name);
        // Transaction start
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom,
                R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
        fragmentTransaction.add(R.id.fragment_container, recyclerViewPopoverFragment, "RecyclerViewPopoverFragment");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void showPopupNew(ArrayList<Event> events, String name){
        Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, "showPopupNew: events size is: " + events.size());
        RelativeLayout relLayoutTutorial = (RelativeLayout) view.findViewById(R.id.tutorial);
        relLayoutTutorial.setVisibility(View.GONE);
        ArrayList<Event> sortedEvents = sortByDateNew(events);
        Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, "showPopupNew: sortedEvents size is: " + sortedEvents.size());


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
     * Uses bubble sort to sort geofenceInfoContents by date
     *
     * @param geofenceInfoContents
     * @return
     */
    private GeofenceInfoContent[] sortByDate(GeofenceInfoContent[] geofenceInfoContents){
        GeofenceInfoContent infoContent = null;
        for(int j = 0; j < geofenceInfoContents.length; j++) {
            for (int i = 0; i < geofenceInfoContents.length; i++) {
                infoContent = geofenceInfoContents[i];
                if (i < geofenceInfoContents.length - 1) {
                    String year1 = infoContent.getYear();
                    String year2 = geofenceInfoContents[i + 1].getYear();
                    int year1Int = Integer.parseInt(year1);
                    int year2Int = Integer.parseInt(year2);
                    if (year1Int < year2Int) {
                        geofenceInfoContents[i] = geofenceInfoContents[i + 1];
                        geofenceInfoContents[i + 1] = infoContent;
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
        if(!mainActivity.NEW_VERSION) {
            mainActivity.requestAllGeofences();
        }else{

            //TODO: switch to newest version
            //mainActivity.requestGeofencesNewer();
            mainActivity.requestGeofencesNew();
        }
    }

    /**
     * Shows a popover to display nearby memories
     * This will no longer be used because the memories feature was removed
     */
    private void showMemoriesPopover(){

        RelativeLayout relLayoutTutorial = (RelativeLayout) view.findViewById(R.id.tutorial);
        relLayoutTutorial.setVisibility(View.GONE);

        FragmentManager fm = getActivity().getSupportFragmentManager();
        RecyclerViewPopoverFragment recyclerViewPopoverFragment = RecyclerViewPopoverFragment.newInstance(this);

        // Transaction start
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        fragmentTransaction.setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom,
                R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
        fragmentTransaction.add(R.id.fragment_container, recyclerViewPopoverFragment, "RecyclerViewPopoverFragment");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * shows a popover for user to add a memory
     * This method will not be used because the memories functionality was removed
     */
    public void showAddMemoriesPopover(){
        Log.i(logMessages.MEMORY_MONITORING, "HistoryFragment : showAddMemoriesPopover called");
        FragmentManager fm = getActivity().getSupportFragmentManager();
        AddMemoryFragment addMemoryFragment = AddMemoryFragment.newInstance();

        // Transaction start
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        fragmentTransaction.setCustomAnimations(R.anim.abc_slide_in_top, R.anim.abc_slide_out_bottom,
                R.anim.abc_slide_in_top, R.anim.abc_slide_out_bottom);
        fragmentTransaction.replace(R.id.fragment_container, addMemoryFragment, "AddMemoriesFragment");

        fragmentTransaction.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view = null;
        currentGeofenceMarkers = null;

    }

    /**
     * Shows a popup containing information about the goefences that was clicked
     * @param infoForGeofenceClicked
     * @param geofenceName
     *//*
    @Override
    public void geofenceClicked(GeofenceInfoContent[] infoForGeofenceClicked, String geofenceName) {
        showPopup(infoForGeofenceClicked, geofenceName);
    }*/

    /**
     * Adds a new marker to the map for each new geofences in the HashMap
     * @param geofenceToAdd
     */
    @Override
    public void addNewGeofenceInfo(HashMap<String, GeofenceInfoContent[]> geofenceToAdd) {
        if(geofenceToAdd == null){
            showUnableToRetrieveGeofences();
        }else if(geofenceToAdd.size() == 0){
            showUnableToRetrieveGeofences();
        }else {
            addMarker(geofenceToAdd);
            hideUnableToRetrieveGeofences();
        }
    }

    public void addNewGeofenceInfoNew(AllGeofences allGeofences){
        if(allGeofences == null){
            showUnableToRetrieveGeofences();
            Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, " allGeofences null");
        }else if(allGeofences.getEvents() == null){
            Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, " allGeofences.getEvents() null");

            if(allGeofences.getTitle() == null){
                Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, " allGeofences.getTitle() null");
            }else if(allGeofences.getTitle().getText() == null){
                Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, " allGeofences.getTitle().getText() null");

            }

            showUnableToRetrieveGeofences();
        }else if(allGeofences.getEvents().length == 0){
            Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, " allGeofences events length is 0");
            showUnableToRetrieveGeofences();
        }else {
            Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, " allGeofences events length is: " + allGeofences.getEvents().length);
            hideUnableToRetrieveGeofences();
            for (int i = 0; i < allGeofences.getEvents().length; i++) {
                Event event = allGeofences.getEvents()[i];
                if(event.getGeo() != null) {
                    Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, " geo is not null");

                    String name = event.getGeo().getName();
                    ArrayList<Event> eventsAtPoint = new ArrayList<>();
                    if (newCurrentGeofencesInfoMap.containsKey(name)) {
                        eventsAtPoint = newCurrentGeofencesInfoMap.get(name);
                    }
                    eventsAtPoint.add(event);
                    newCurrentGeofencesInfoMap.put(name, eventsAtPoint);
                }else {
                    Log.i(logMessages.NEW_GEOPOINTS_DEBUGGING, " geo IS null");

                }
            }
            addMarkerNew(newCurrentGeofencesInfoMap);
        }
    }

    /**
     * Draws geofence markers for every goefence
     */
    private void drawAllGeofenceMarkers(){
        MainActivity mainActivity = (MainActivity) getActivity();
        if(!mainActivity.NEW_VERSION) {
            if (mainActivity.getAllGeofenceInfo() != null) {
                addMarker(mainActivity.getAllGeofenceInfo());
            }
        }else{
            if(mainActivity.getAllGeofencesNew() != null) {
                addNewGeofenceInfoNew(mainActivity.getAllGeofencesNew());
            }
        }
    }

    public void showUnableToRetrieveGeofences(){
        final TextView txtRequestGeofences = (TextView) view.findViewById(txt_try_getting_geofences);
        final Button btnRequestGeofences = (Button) view.findViewById(R.id.btn_request_geofences);
        txtRequestGeofences.setText(getResources().getString(R.string.no_geofences_retrieved));
        txtRequestGeofences.setVisibility(View.VISIBLE);
        btnRequestGeofences.setVisibility(View.VISIBLE);
    }

    private void hideUnableToRetrieveGeofences(){
        final TextView txtRequestGeofences = (TextView) view.findViewById(txt_try_getting_geofences);
        final Button btnRequestGeofences = (Button) view.findViewById(R.id.btn_request_geofences);
        txtRequestGeofences.setVisibility(View.GONE);
        btnRequestGeofences.setVisibility(View.GONE);
    }
}
