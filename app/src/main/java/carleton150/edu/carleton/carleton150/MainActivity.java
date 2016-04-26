package carleton150.edu.carleton.carleton150;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.property.DtStart;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import carleton150.edu.carleton.carleton150.ExtraFragments.QuestCompletedFragment;
import carleton150.edu.carleton.carleton150.Interfaces.FragmentChangeListener;
import carleton150.edu.carleton.carleton150.Interfaces.RetrievedFileListener;
import carleton150.edu.carleton.carleton150.MainFragments.EventsFragment;
import carleton150.edu.carleton.carleton150.MainFragments.HistoryFragment;
import carleton150.edu.carleton.carleton150.MainFragments.MainFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestInProgressFragment;
import carleton150.edu.carleton.carleton150.Models.DownloadFileFromURL;
import carleton150.edu.carleton.carleton150.Models.GeofenceErrorMessages;
import carleton150.edu.carleton.carleton150.Models.VolleyRequester;
import carleton150.edu.carleton.carleton150.POJO.EventObject.EventContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoObject;
import carleton150.edu.carleton.carleton150.POJO.GeofenceObject.GeofenceObjectContent;
import carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo.AllGeofences;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;

/**
 * Monitors location and geofence information and calls methods in the main view fragments
 * to handle geofence and location changes. Also controls which fragment is in view. Requests events
 * and quests using VolleyRequester and stores them for the fragments.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status>, FragmentChangeListener,
        RetrievedFileListener {

    //things for location

    public Location mLastLocation = null;
    // Google client to interact with Google API
    public GoogleApiClient mGoogleApiClient;
    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = true;
    private LocationRequest mLocationRequest;
    private static Constants constants = new Constants();
    private LogMessages logMessages = new LogMessages();
    MainFragment curFragment = null;
    public boolean needToShowGPSAlert = true;

    private Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Log.i(logMessages.GEOFENCE_MONITORING, "MainActivity: trying to connect mGoogleApiClient");
            mGoogleApiClient.connect();
        }
    };

    public VolleyRequester mVolleyRequester = new VolleyRequester();
    AlertDialog networkAlertDialog;
    AlertDialog playServicesConnectivityAlertDialog;

    private boolean requestingQuests = false;
    private ArrayList<Quest> questInfo = null;

    private LinkedHashMap<String, ArrayList<EventContent>> eventsMapByDate = new LinkedHashMap<String, ArrayList<EventContent>>();
    private ArrayList<EventContent> tempEventContentLst = new ArrayList<EventContent>();
    private boolean requestingEvents = false;


    private boolean geofenceRetrievalSuccessful = false;
    private GeofenceInfoObject allGeofenceInfo = null;
    private GeofenceObjectContent[] allGeofences = null;
    private boolean requestingGeofences = false;
    private HashMap<String, GeofenceObjectContent> allGeofencesMap = new HashMap<>();


    //TODO: this should be gone by final version because we should only have one method
    //method to determine whether we are using old geofence retrieval method or new one
    public boolean NEW_VERSION = true;
    private AllGeofences allGeofencesNew = null;
    private boolean requestingAllGeofencesNew = false;
    DownloadFileFromURL downloadFileFromURLGeofences = new DownloadFileFromURL(this, constants.GEOFENCES_FILE_NAME_WITH_EXTENSION);



    /**
     * Sets up Google Play Services and builds a Google API Client. Sets up the tabs in the tab
     * views.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("GEOFENCE MONITORING", "onCreate in MainActivity called");

        networkAlertDialog = new AlertDialog.Builder(MainActivity.this).create();
        playServicesConnectivityAlertDialog = new AlertDialog.Builder(MainActivity.this).create();
        // check availability of play services for location data and geofencing
        if (checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
            if (isConnectedToNetwork()) {
                mGoogleApiClient.connect();
            }
        } else {
            showGooglePlayServicesUnavailableDialog();
        }

        //managing fragments and UI
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        }
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.history)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.events)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.quests)));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        curFragment = new HistoryFragment();
        int commit = getSupportFragmentManager()
                .beginTransaction().replace(R.id.containerLayout, curFragment).commit();

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    if (curFragment instanceof HistoryFragment == false) {
                        getSupportFragmentManager().beginTransaction().remove(curFragment).commit();
                        curFragment = new HistoryFragment();
                    }
                }
                if (tab.getPosition() == 1) {
                    if (curFragment instanceof EventsFragment == false) {
                        getSupportFragmentManager().beginTransaction().remove(curFragment).commit();
                        curFragment = new EventsFragment();
                    }
                }
                if (tab.getPosition() == 2) {
                    if (curFragment instanceof QuestFragment == false) {
                        getSupportFragmentManager().beginTransaction().remove(curFragment).commit();
                        curFragment = new QuestFragment();
                    }
                }
                int commit = getSupportFragmentManager()
                        .beginTransaction().replace(R.id.containerLayout, curFragment).commit();

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        if(NEW_VERSION) {
            //TODO: switch to newest version
            requestGeofencesNew();
            //requestGeofencesNewer();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //TODO: START and stop location updates from questInProgressFragment? That is the only time location is needed now...
    /**
     * Stops location updates to save battery
     */
    @Override
    protected void onPause() {
        super.onPause();
        needToShowGPSAlert = true;
        stopLocationUpdates();
    }

    /**
     * Overridden lifecycle method to start location updates if possible
     * and necessary, and connect mGoogleApiClient if possible and necessary
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected()) {
            isConnectedToNetwork();
            if (mRequestingLocationUpdates) {
                if(checkIfGPSEnabled()) {
                   startLocationUpdates();
                }
            }
        } else {
            checkIfGPSEnabled();
            if (isConnectedToNetwork()) {
                // check availability of play services for location data and geofencing
                if (checkPlayServices()) {
                    mGoogleApiClient.connect();
                } else {
                    showGooglePlayServicesUnavailableDialog();
                }
            }
        }
    }


    /**
     * Method that is called when google API Client is connected
     *
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        // Once connected with google api, get the location
        if(checkIfGPSEnabled()) {
            mLastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
            tellFragmentLocationChanged();
        }

        //starts periodic location updates
        if (mRequestingLocationUpdates) {
            if(checkIfGPSEnabled()) {
               startLocationUpdates();
            }
        }

    }

    /**
     * If google api client connection was suspended, keeps trying to connect
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        handler.postDelayed(runnable, 1000);
    }

    /**
     * Displays an alert dialog if unable to connect to the GoogleApiClient
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        showAlertDialog("Connection to play services failed with message: " +
                        connectionResult.getErrorMessage() + "\nCode: " + connectionResult.getErrorCode(),
                playServicesConnectivityAlertDialog);
    }

    /**
     * Builds a GoogleApiClient
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Method to verify google play services on the device
     */
    public boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        constants.PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }


    /**
     * Method called by the google location client when the user's
     * location changes. Records the location and passes the new
     * location information to the fragment
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {

        Log.i("GEOFENCE MONITORING", "onLocationChanged ");

        // Assign the new location
        mLastLocation = location;
        tellFragmentLocationChanged();
        if(!NEW_VERSION) {
            requestAllGeofences();
        }
    }

    /**
     * Calls a method in the current fragment to handle a location change.
     */
    private void tellFragmentLocationChanged() {
        if (curFragment != null) {
            curFragment.handleLocationChange(mLastLocation);
        }
    }

    /**
     *
     * @return last known location
     */
    public Location getLastLocation() {
        return mLastLocation;
    }



    /**
     * Creating location request object
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(constants.FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(constants.DISPLACEMENT);
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates() {
        if(checkIfGPSEnabled()) {
            if (mGoogleApiClient.isConnected()) {
                if (mRequestingLocationUpdates) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleApiClient, mLocationRequest, this);
                }
            }
        }
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            Log.i(logMessages.LOCATION, "stopLocationUpdates : location updates stopped");
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }


    /**
     * checks whether phone has network connection. If not, displays a dialog
     * requesting that the user connects to a network.
     *
     * @return
     */
    public boolean isConnectedToNetwork() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            if (activeNetworkInfo.isConnected()) {
                return true;
            } else {
                showNetworkNotConnectedDialog();
                return false;
            }
        } else {
            showNetworkNotConnectedDialog();
            return false;
        }
    }

    /**
     * displays a dialog requesting that the user connect to a network
     */
    public void showNetworkNotConnectedDialog() {
        showAlertDialog(getResources().getString(R.string.no_network_connection),
                networkAlertDialog);
    }

    /**
     * Shows a dialog to tell user google play services is unavailable
     */
    private void showGooglePlayServicesUnavailableDialog() {
        showAlertDialog(getResources().getString(R.string.no_google_services), playServicesConnectivityAlertDialog);
    }

    /**
     * shows an alert dialog with the specified message
     *
     * @param message
     */
    public void showAlertDialog(String message, AlertDialog dialog) {
        if (!dialog.isShowing()) {
            dialog.setTitle("Alert");
            dialog.setMessage(message);
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener()

                    {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }
            );
            dialog.show();
        }
    }

    /**
     * An alternative method to show an alert dialog without a neutral button
     * as the previous method adds a neutral button before displaying the dialog
     * @param dialog
     */
    public void showAlertDialogNoNeutralButton(AlertDialog dialog) {
        dialog.show();
    }


    public HashMap<String, GeofenceObjectContent> getAllGeofencesMap(){
        return allGeofencesMap;
    }


    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     * The activity implements ResultCallback, so this is a required method
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
        } else {
            // Get the status code and log it.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(logMessages.GEOFENCE_MONITORING, "onResult error: " + errorMessage);
        }
    }

    /**
     * Overridden from FragmentChangeListener interface to replace
     * the QuestFragment with a new QuestInProgressFragment
     * when a quest is started from the QuestFragment
     *
     * @param fragment
     */
    @Override
    public void replaceFragment(MainFragment fragment) {
        //adapter.replaceFragment(fragment);
        getSupportFragmentManager().beginTransaction().remove(curFragment).commit();

        curFragment = fragment;

        int commit = getSupportFragmentManager()
                .beginTransaction().replace(R.id.containerLayout, curFragment).commit();
    }


    /**
     * If QuestInProgressFragment is the current fragment,
     * overrides back button to replaces the QuestInProgressFragment
     * with a new QuestFragment
     */
    @Override
    public void onBackPressed() {
        if (curFragment instanceof QuestInProgressFragment || curFragment instanceof QuestCompletedFragment) {
            getSupportFragmentManager().beginTransaction().remove(curFragment).commit();
            curFragment = new QuestFragment();
            int commit = getSupportFragmentManager()
                    .beginTransaction().replace(R.id.containerLayout, curFragment).commit();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Reterns the Preferences for the information stored about the user's
     * progress in a quest. This method is so the user can resume quests even
     * after killing the app or going back to the quest selection screen
     */
    public SharedPreferences getPersistentQuestStorage() {
        return getSharedPreferences(constants.QUEST_PREFERENCES_KEY, 0);

    }

    /**
     * gets the memory class of the device
     *
     * @return
     */
    public int getMemoryClass() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        Log.v(logMessages.MEMORY_MONITORING, "memoryClass:" + Integer.toString(memoryClass));
        return memoryClass;
    }

    /**
     * Checks if gps is enabled on the device
     * @return true if enabled, false otherwise
     */
    public boolean checkIfGPSEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            if(needToShowGPSAlert) {
                needToShowGPSAlert = false;
                buildAlertMessageNoGps();
            }
            return false;
        }return true;
    }

    /**
     * Alerts the user that their GPS is not enabled and gives them option to enable it
     */
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.gps_not_enabled))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }



    /**
     * Using the information retrieved from the requestAllGeofences() method,
     * requests information about each geofence. Results are handled in handleGeofenceInfo()
     *
     * @param geofences
     */
    public void requestAllGeofenceInfo(GeofenceObjectContent[] geofences){
        Log.i("GEOFENCE MONITORING", "requestAllGeofenceInfo ");

        if(allGeofenceInfo == null){
            Log.i("GEOFENCE MONITORING", "requestAllGeofenceInfo : allGeofenceInfo is null ");
        }
        if(geofences != null){
            Log.i("GEOFENCE MONITORING", "requestAllGeofenceInfo : geofences not null ");

        }

        if(allGeofenceInfo == null && geofences != null) {
            Log.i("GEOFENCE MONITORING", "requestingAllGeofenceInfo: in if loop: ");

            GeofenceObjectContent[] singleGeofence = new GeofenceObjectContent[1];
            for(int i = 0; i<geofences.length; i++){
                Log.i("GEOFENCE MONITORING", "requestAllGeofenceInfo: in for loop");

                singleGeofence[0] = geofences[i];
                mVolleyRequester.request(this, singleGeofence);

            }
        }
    }

    /**
     * Method called from the VolleyRequester when it recieves info about a geofence
     * @param geofenceInfoObject
     */
    public void handleGeofenceInfo(GeofenceInfoObject geofenceInfoObject){
        Log.i("GEOFENCE MONITORING", "handleGeofenceInfo");
        requestingGeofences = false;

        if(allGeofenceInfo == null){
            Log.i("GEOFENCE MONITORING", "allGeofenceInfo was null");
            if(curFragment instanceof HistoryFragment){
                ((HistoryFragment) curFragment).showUnableToRetrieveGeofences();
            }
            allGeofenceInfo = geofenceInfoObject;
            geofenceRetrievalSuccessful = false;
        }else {
            geofenceRetrievalSuccessful = true;
            Log.i("GEOFENCE MONITORING", "allGeofenceInfo was not null");

            if (geofenceInfoObject != null) {
                Log.i("GEOFENCE MONITORING", "geofenceInfoObject was not null");

                HashMap<String, GeofenceInfoContent[]> newGeofence = new HashMap<>();
                for (HashMap.Entry<String, GeofenceInfoContent[]> e : geofenceInfoObject.getContent().entrySet()) {
                    Log.i("GEOFENCE MONITORING", "handleGeofenceInfo: in for loop");

                    this.allGeofenceInfo.getContent().put(e.getKey(), e.getValue());
                    newGeofence.clear();
                    newGeofence.put(e.getKey(), e.getValue());
                    curFragment.addNewGeofenceInfo(newGeofence);
                }
            }
        }

        if(eventsMapByDate.size() == 0){

            //TODO: switch to new version
            getEvents();
        }if(questInfo == null){
            requestQuests();
        }
    }

    /**
     * Getter method for getting information about geofences
     *
     * @return all the information about geofences as a HashMap where the key is the name
     * of the geofence and the value is a GeofenceInfoContent[] with info about that geofence
     */
    public HashMap<String, GeofenceInfoContent[]> getAllGeofenceInfo(){
        if(allGeofenceInfo != null) {
            return this.allGeofenceInfo.getContent();
        }else{
            return null;
        }
    }


    /**
     * Does checks to make sure requesting geofences is necessary, then calls a method to request
     * geofences and stores a boolean indicating that a geofence request is in progress
     */
    public void requestAllGeofences(){
        if(allGeofences == null && !requestingGeofences){
            Log.i("GEOFENCE MONITORING", "requestiongAllGeofences. about to request them ");

            requestingGeofences = true;
            if(geofenceRetrievalSuccessful != true) {
                Log.i("GEOFENCE MONITORING", "requestAllGeofences: geofenceRetrieval successful is not true ");
                requestingGeofences = getNewGeofences();
            }else{
                Log.i("GEOFENCE MONITORING", "requestAllGeofences: geofenceRetrieval successful is true ");
            }
        }
    }


    /**
     * Method called from VolleyRequester when new geofences are retrieved
     * from server. Calls a function on whatever fragment is currently in view to
     * handle the new geofences
     *
     * @param content
     */
    public void handleNewGeofences(GeofenceObjectContent[] content) {
        Log.i("GEOFENCE MONITORING", "handleNewGeofences ");

        if(content == null){
            geofenceRetrievalSuccessful = false;
            requestingGeofences = false;
            if(curFragment instanceof HistoryFragment){
                ((HistoryFragment) curFragment).showUnableToRetrieveGeofences();
            }
            Log.i("GEOFENCE MONITORING", "handleNewGeofences: content is null ");
        }else{
            geofenceRetrievalSuccessful = true;
            requestAllGeofenceInfo(content);
            for(int i = 0; i<content.length; i++){
                allGeofencesMap.put(content[i].getName(), content[i]);
            }
            allGeofences = content;
        }
    }


    /**
     * Requests all geofences from server using VolleyRequester. Results are handled in handleNewGeofences()
     */
    public boolean getNewGeofences(){
        if(mLastLocation == null){
            Log.i("GEOFENCE MONITORING", "getNewGeofences: mLastLocation is null ");
        }else {
            try {
                this.mVolleyRequester.requestGeofences(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude(), this);
                return true;
            } catch (Exception e) {
                Log.i("GEOFENCE MONITORING", "getNewGeofences: error is :   " + e.getMessage());

                e.printStackTrace();
                return false;
            }
        }
        return false;
    }



    /**
     * Called by VolleyRequester, handles new quests from the server
     * @param newQuests
     */
    public void handleNewQuests(ArrayList<Quest> newQuests) {
        /*This is a call from the VolleyRequester, so this check prevents the app from
        crashing if the user leaves the tab while the app is trying
        to get quests from the server
         */

        requestingQuests = false;

        if(newQuests != null) {
            questInfo = newQuests;
        }

        if(curFragment instanceof QuestFragment){
            curFragment.handleNewQuests(questInfo);
        }

    }

    /**
     * uses the volleyRequester to retrieve all quests from the server. Results are handled
     * in handleNewQuests()
     */
    public void requestQuests(){
        if(questInfo == null && !requestingQuests)
            mVolleyRequester.requestQuests(this);
        requestingQuests = true;
    }

    /**
     * @return an ArrayList of Quests
     */
    public ArrayList<Quest> getQuests(){
        return this.questInfo;
    }


    /**
     *
     * @return a LinkedHashMap where the key is the date of the event and the value
     * is an ArrayList of EventContent
     */
    public LinkedHashMap<String, ArrayList<EventContent>> getEventsMapByDate(){
        return this.eventsMapByDate;
    }

    /**
     * Method called when new geofences are successfully retrieved.
     * If the current fragment is a HistoryFragment, notifies the
     * fragment of the new geofences.
     * Saves the geofences.
     * @param geofences
     */
    public void handleGeofencesNewMethod(AllGeofences geofences){
        requestingAllGeofencesNew = false;
        if(curFragment instanceof HistoryFragment){
            ((HistoryFragment) curFragment).addNewGeofenceInfoNew(geofences);
        }
        this.allGeofencesNew = geofences;
    }

    /**
     *
     * @return all geofences
     */
    public AllGeofences getAllGeofencesNew(){
        return this.allGeofencesNew;
    }

    /**
     * Requests all geofences with the new request and response format
     */
    public void requestGeofencesNew(){
        if(!requestingAllGeofencesNew && allGeofencesNew == null) {
            mVolleyRequester.requestGeopointsNew(this);
            requestingAllGeofencesNew = true;
        }
    }

    public void requestGeofencesNewer(){
        downloadFileFromURLGeofences.execute(constants.NEW_GEOFENCES_ENDPOINT);
    }

    /**
     * Gets ical feed from Constatns.ICAL_FEED_URL
     */
    public void getEvents(){
        DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, constants.ICAL_FILE_NAME_WITH_EXTENSION);
        downloadFileFromURL.execute(constants.ICAL_FEED_URL);
    }


    /**
     * Accesses the Ical feed that was saved into a file by DownloadFileFromURL, then uses
     * it to create a Calendar object. Once that is completed, calls buildEventContent() to use
     * that Calendar object to build and ArrayList of EventContent
     */
    private void parseIcalFeed(String fileNameWithExtension){
        File file = new File(Environment.getExternalStorageDirectory().toString() + "/" + fileNameWithExtension);
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CalendarBuilder builder = new CalendarBuilder();
        net.fortuna.ical4j.model.Calendar calendar = null;
        try {
            calendar = builder.build(fin);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }
        if (calendar != null) {
            Log.i("EVENTS", "MainActivity: parseIcalFeed : calendar not null ");
            PropertyList plist = calendar.getProperties();
            buildEventContent(calendar);
        }

    }

    private void parseGeofences(String fileNameWithExtension){

        String jsonString = readFromFile(fileNameWithExtension);

        Log.i("NEWGEOFENCES", "MainActivity : parseGeofences : " + jsonString);
        final Gson gson = new Gson();

        AllGeofences allGeofences = gson.fromJson(jsonString, AllGeofences.class);

        handleGeofencesNewMethod(allGeofences);

    }

    private String readFromFile(String fileNameWithExtension) {

        String ret = "";

        File file = new File(Environment.getExternalStorageDirectory().toString() + "/" + fileNameWithExtension);

        try {
            InputStream inputStream = new FileInputStream(file);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.i("NEWGEOFENCES", "readFromFile: File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("NEWGEOFENCES", "readFromFile: " + e.toString());
        }

        return ret;
    }

    /**
     * Saves the calendar to an ical file in externalStorage for later use.
     * The filepath is <External Storage Directory>fileNameWithExtension.
     * @param fileNameWithExtension
     * @param calendar
     * @return
     */
    public boolean saveIcalToFile(String fileNameWithExtension, net.fortuna.ical4j.model.Calendar calendar) {

        for(int i = 0; i<calendar.getProperties().size(); i++){
            Log.i("EVENTS", "MainActivity: generateICAL : property :  " + calendar.getProperties().get(i).toString());
        }

        if(calendar.getProperties().size() == 0){
            Log.i("EVENTS", "MainActivity: generateICAL : property :  length of properties is 0");

        }

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/" + fileNameWithExtension);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            Log.i("EVENTS", "MainActivity: generateICAL : couldn't get FileOutputStream : path is:  " + Environment.getExternalStorageDirectory().toString() + fileNameWithExtension);

        }
        CalendarOutputter outputter;
        try {
            outputter = new CalendarOutputter();
            outputter.setValidating(false);
            outputter.output(calendar, fout);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ValidationException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Builds an ArrayList of EventContents and passes them to the handleNewEvents method.
     * Note that this method does not add events to EventContents unless they have a start time.
     * @param calendar
     */
    private void buildEventContent(net.fortuna.ical4j.model.Calendar calendar){
        ArrayList<EventContent> eventContents = new ArrayList<>();
        ComponentList componentList = calendar.getComponents();
        boolean addComponent = true;
        for(int i = 0; i<componentList.size(); i++){
            Component component = (Component) componentList.get(i);
            PropertyList propertyList = component.getProperties();
            EventContent eventContent = new EventContent();
            if(propertyList.getProperty(Property.LOCATION) != null) {
                eventContent.setLocation(propertyList.getProperty(Property.LOCATION).getValue());
            }else {
                eventContent.setLocation(getResources().getString(R.string.no_location));
            }if(propertyList.getProperty(Property.SUMMARY) != null) {
                    eventContent.setTitle(propertyList.getProperty(Property.SUMMARY).getValue());
            }else{
                eventContent.setTitle(getResources().getString(R.string.no_title));
            }if(propertyList.getProperty(Property.DTSTART) != null) {
                addComponent = true;
                DtStart start = (DtStart) propertyList.getProperty(Property.DTSTART);
                Date startDate = start.getDate();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", Locale.US);
                String date = df.format(startDate);
                eventContent.setStartTime(date);
            }else {
                addComponent = false;
            }if(propertyList.getProperty(Property.DESCRIPTION) != null) {
                    eventContent.setDescription(propertyList.getProperty(Property.DESCRIPTION).getValue());
            }else{
                    eventContent.setDescription(getResources().getString(R.string.no_description));
            }
            if(propertyList.getProperty(Property.DURATION) != null) {
                eventContent.setDuration(propertyList.getProperty(Property.DURATION).getValue());
            }
            if(addComponent) {
                eventContents.add(eventContent);
            }


        }

       handleNewEvents(eventContents);
    }


    /**
     * Called from BuildEventContent, adds events to a hashmap where the key is the date
     * and the value is an ArrayList of EventContent for events happening that day.
     * Notifies the fragment of the new events if the fragment in view is the EventsFragment
     * @param events
     */
    public void handleNewEvents(ArrayList<EventContent> events) {
        requestingEvents = false;
        String completeDate;
        String[] completeDateArray;
        String dateByDay;
        eventsMapByDate.clear();

        if(events == null){
            if(curFragment instanceof EventsFragment){
                curFragment.handleNewEvents(null);
            }
        }else {



            for (int i = 0; i < events.size(); i++) {

                // Add new date values to hashmap if not already there
                completeDate = events.get(i).getStartTime();
                completeDateArray = completeDate.split("T");
                dateByDay = completeDateArray[0];


                // If key already there, add + update new values
                if (!eventsMapByDate.containsKey(dateByDay)) {
                    tempEventContentLst.clear();
                    tempEventContentLst.add(events.get(i));
                    ArrayList<EventContent> eventContents1 = new ArrayList<>();
                    for (int k = 0; k < tempEventContentLst.size(); k++) {
                        eventContents1.add(tempEventContentLst.get(k));
                    }
                    eventsMapByDate.put(dateByDay, eventContents1);
                } else {
                    tempEventContentLst.add(events.get(i));
                    ArrayList<EventContent> eventContents1 = new ArrayList<>();
                    for (int k = 0; k < tempEventContentLst.size(); k++) {
                        eventContents1.add(tempEventContentLst.get(k));
                    }
                    eventsMapByDate.put(dateByDay, eventContents1);
                }

            }

            if (curFragment instanceof EventsFragment) {
                curFragment.handleNewEvents(eventsMapByDate);
            }
        }
    }

    /**
     * Called by DownloadFileFromURL when the events are retrieved. If the
     * retrieval was successful, calls a function to parse the ical feed that
     * was retrieved into a calendar
     * @param retrievalSucceeded
     */
    @Override
    public void retrievedFile(boolean retrievalSucceeded, String fileNameWithExtension) {
        if(retrievalSucceeded){
            if(fileNameWithExtension.equals(constants.ICAL_FILE_NAME_WITH_EXTENSION)) {
                parseIcalFeed(fileNameWithExtension);
            }if(fileNameWithExtension.equals(constants.GEOFENCES_FILE_NAME_WITH_EXTENSION)){
                parseGeofences(fileNameWithExtension);
            }
        }else{
            Log.i("EVENTS", "unable to retrieve events");
        }
    }

    //TODO: now that we no longer need a location sent in to request geofences, call that request
    //TODO: earlier, before getting location updates, then we only need to get location updates
    //TODO: for the quest in progress screen

}
