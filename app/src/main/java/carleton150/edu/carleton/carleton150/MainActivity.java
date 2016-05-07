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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.util.CompatibilityHints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import carleton150.edu.carleton.carleton150.Adapters.MyFragmentPagerAdapter;
import carleton150.edu.carleton.carleton150.ExtraFragments.QuestCompletedFragment;
import carleton150.edu.carleton.carleton150.Interfaces.RetrievedFileListener;
import carleton150.edu.carleton.carleton150.MainFragments.EventsFragment;
import carleton150.edu.carleton.carleton150.MainFragments.HistoryFragment;
import carleton150.edu.carleton.carleton150.MainFragments.InfoFragment;
import carleton150.edu.carleton.carleton150.MainFragments.MainFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestInProgressFragment;
import carleton150.edu.carleton.carleton150.Models.DownloadFileFromURL;
import carleton150.edu.carleton.carleton150.Models.GeofenceErrorMessages;
import carleton150.edu.carleton.carleton150.POJO.EventObject.EventContent;
import carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo.AllGeofences;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;

/**
 * Monitors location and geofence information and calls methods in the main view fragments
 * to handle geofence and location changes. Also controls which fragment is in view. Requests events
 * and quests using VolleyRequester and stores them for the fragments.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status>,
        RetrievedFileListener {

    //things for location

    public Location mLastLocation = null;
    // Google client to interact with Google API
    public GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;
    private boolean shouldBeRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;
    private static Constants constants = new Constants();
    private LogMessages logMessages = new LogMessages();
    public boolean needToShowGPSAlert = true;

    private Quest questInProgress;

    MyFragmentPagerAdapter adapter;

    private Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Log.i(logMessages.GEOFENCE_MONITORING, "MainActivity: trying to connect mGoogleApiClient");
            mGoogleApiClient.connect();
        }
    };

    AlertDialog.Builder networkAlertDialogBuilder;
    AlertDialog.Builder playServicesConnectivityAlertDialogBuilder;
    AlertDialog.Builder noGpsAlertDialogBuilder;
    AlertDialog.Builder onCampusFeatureAlertDialogBuilder;

    private boolean requestingQuests = false;
    private ArrayList<Quest> questInfo = null;
    private java.util.Date lastQuestUpdate;

    private LinkedHashMap<String, Integer> eventsMapByDate;
    private ArrayList<EventContent> eventContentList;
    private boolean requestingEvents = false;
    private java.util.Date lastEventsUpdate;
    private AllGeofences allGeofencesNew = null;
    private boolean requestingAllGeofencesNew = false;
    private java.util.Date lastGeofenceUpdate;



    /**
     * Sets up Google Play Services and builds a Google API Client. Sets up the tabs in the tab
     * views.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
        Log.i("GEOFENCE MONITORING", "onCreate in MainActivity called");

        networkAlertDialogBuilder = new AlertDialog.Builder(this);
        playServicesConnectivityAlertDialogBuilder = new AlertDialog.Builder(this);
        noGpsAlertDialogBuilder = new AlertDialog.Builder(this);
        onCampusFeatureAlertDialogBuilder = new AlertDialog.Builder(this);

        // check availability of play services for location data and geofencing
        if (checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
            mGoogleApiClient.connect();
        } else {
            showGooglePlayServicesUnavailableDialog();
        }

        //managing fragments an UI
        final FragmentManager manager=getSupportFragmentManager();
        adapter=new MyFragmentPagerAdapter(manager);
        adapter.initialize(this, manager);

        final NoSwipeViewPager viewPager = (NoSwipeViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setTabsFromPagerAdapter(adapter);



        if(isConnectedToNetwork()) {
            requestGeofencesNewer();
            requestEvents();
            requestQuests();
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


    /**
     * Overridden lifecycle method to start location updates if possible
     * and necessary, and connect mGoogleApiClient if possible and necessary
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        } else {
                // check availability of play services for location data and geofencing
                if (checkPlayServices()) {
                    mGoogleApiClient.connect();
                } else {
                    showGooglePlayServicesUnavailableDialog();
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

        Log.i("GOOGLE PLAY", "connected!");
        if(checkIfGPSEnabled()) {
            mLastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
            tellFragmentLocationChanged();
        }
        startLocationUpdates();
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
        AlertDialog dialog = playServicesConnectivityAlertDialogBuilder.create();
        if(!dialog.isShowing()) {
            showAlertDialog("Connection to play services failed with message: " +
                            connectionResult.getErrorMessage() + "\nCode: " + connectionResult.getErrorCode(),
                    dialog);
        }
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


    //TODO: only need to listen to location for QuestInProgress fragment.
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
    }

    /**
     * Calls a method in the current fragment to handle a location change.
     */
    private void tellFragmentLocationChanged() {
        if (adapter.getCurrentFragment() != null) {
            if(adapter.getCurrentFragment() instanceof MainFragment) {
                ((MainFragment)adapter.getCurrentFragment()).handleLocationChange(mLastLocation);
            }
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
    private void startLocationUpdates() {

        if(checkIfGPSEnabled()) {
            if (mGoogleApiClient.isConnected()) {
                if (shouldBeRequestingLocationUpdates && !mRequestingLocationUpdates) {
                    Log.i(LogMessages.LOCATION, "Location updates started");
                    mRequestingLocationUpdates = true;
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleApiClient, mLocationRequest, this);
                }
            }else{
                Log.i(LogMessages.LOCATION, "Location updates unable to start. Connecting API client");
                mGoogleApiClient.connect();
            }
        }
    }

    /**
     * Stopping location updates
     */
    private void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            Log.i(logMessages.LOCATION, "Location updates stopped");
            mRequestingLocationUpdates = false;
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    public void startLocationUpdatesIfPossible(){
        Log.i(logMessages.LOCATION, "starting location updates if possible");
        shouldBeRequestingLocationUpdates = true;
        startLocationUpdates();
    }

    public void stopLocationUpdatesIfPossible(){
        Log.i(logMessages.LOCATION, "stopping location updates if possible");
        shouldBeRequestingLocationUpdates = false;
        stopLocationUpdates();
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
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * displays a dialog requesting that the user connect to a network
     */
    public void showNetworkNotConnectedDialog() {
        AlertDialog dialog = networkAlertDialogBuilder.create();
        if(!dialog.isShowing()) {
            showAlertDialog(getResources().getString(R.string.no_network_connection),
                    dialog);
        }
    }

    /**
     * Shows a dialog to tell user google play services is unavailable
     */
    private void showGooglePlayServicesUnavailableDialog() {
        AlertDialog dialog = playServicesConnectivityAlertDialogBuilder.create();
        if(!dialog.isShowing()) {
            showAlertDialog(getResources().getString(R.string.no_google_services), dialog);
        }
    }

    public void showOnCampusFeatureAlertDialogQuests() {
        AlertDialog dialog = onCampusFeatureAlertDialogBuilder.create();
        if(!dialog.isShowing()){
            showAlertDialog(getString(R.string.quests_unuseable_off_campus), dialog);
        }
    }

    public void showOnCampusFeatureAlertDialogQuestInProgress() {
        AlertDialog dialog = onCampusFeatureAlertDialogBuilder.create();
        if(!dialog.isShowing()){
            showAlertDialog(getString(R.string.quest_in_progress_unuseable_off_campus), dialog);
        }
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
        if (!dialog.isShowing()) {
            dialog.show();
        }
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
     * If QuestInProgressFragment or QuestCompletedFragment is the current fragment,
     * overrides back button to replaces the QuestInProgressFragment
     * with a new QuestFragment
     */
    @Override
    public void onBackPressed() {
        if (adapter.getCurrentFragment() instanceof QuestInProgressFragment ||
                adapter.getCurrentFragment() instanceof QuestCompletedFragment) {
            adapter.backButtonPressed();
        }else if(adapter.getCurrentFragment() instanceof InfoFragment){

            boolean wentBack = ((InfoFragment)adapter.getCurrentFragment()).backPressed();
            if(!wentBack){
                super.onBackPressed();
            }

        }else{
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

    public void showEventInfoInBrowser(String url){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    /**
     * Checks if gps is enabled on the device
     * @return true if enabled, false otherwise
     */
    public boolean checkIfGPSEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            return false;
        }return true;
    }

    /**
     * Alerts the user that their GPS is not enabled and gives them option to enable it
     */
    public void buildAlertMessageNoGps() {
            needToShowGPSAlert = false;
            noGpsAlertDialogBuilder.setMessage(getString(R.string.feature_requires_gps))
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
            final AlertDialog alert = noGpsAlertDialogBuilder.create();

        if(!alert.isShowing()) {
            alert.show();
        }
    }

    /**
     * Called by VolleyRequester, handles new quests from the server
     * @param newQuests
     */
    public void handleNewQuests(ArrayList<Quest> newQuests, boolean newInfo) {
        /*This is a call from the VolleyRequester, so this check prevents the app from
        crashing if the user leaves the tab while the app is trying
        to get quests from the server
         */

        requestingQuests = false;

        if(newQuests != null && newInfo) {
            lastQuestUpdate = Calendar.getInstance().getTime();
        }

        if(newQuests != null) {
            questInfo = newQuests;
        }

        if(adapter.getCurrentFragment() instanceof QuestFragment){
            ((QuestFragment)adapter.getCurrentFragment()).handleNewQuests(questInfo);
        }

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
    public LinkedHashMap<String, Integer> getEventsMapByDate(){
        return this.eventsMapByDate;
    }

    /**
     * Method called when new geofences are successfully retrieved.
     * If the current fragment is a HistoryFragment, notifies the
     * fragment of the new geofences.
     * Saves the geofences.
     * @param geofences
     */
    public void handleGeofencesNewMethod(AllGeofences geofences, boolean isNewInfo){
        requestingAllGeofencesNew = false;
        Log.i("GEOFENCE MONITORING", "MainActivity: handleGeofencesNEwMethod: called");


        if(geofences != null && isNewInfo) {
            lastGeofenceUpdate = Calendar.getInstance().getTime();
        }

        if(adapter.getCurrentFragment() instanceof HistoryFragment){
            Log.i("GEOFENCE MONITORING", "MainActivity: handleGeofencesNewMethod: current fragment is historyfragment");

            ((HistoryFragment) adapter.getCurrentFragment()).addNewGeofenceInfoNew(geofences);
        }else if(adapter.getCurrentFragment() == null) {
            Log.i("GEOFENCE MONITORING", "MainActivity: handleGeofencesNewMethod: current fragment is null");

        }else{
                Log.i("GEOFENCE MONITORING", "MainActivity: handleGeofencesNewMethod: current fragment is NOT historyfragment");

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

    public void requestGeofencesNewer(){
        if(isConnectedToNetwork()) {
            if (!requestingAllGeofencesNew && lastGeofenceUpdate == null) {
                Log.i("GEOFENCE MONITORING", "MainActivity: requestGeofencesNewer: not requesting new geofences, last update null. About to request");
                DownloadFileFromURL downloadFileFromURLGeofences = new DownloadFileFromURL(this, constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, this);
                downloadFileFromURLGeofences.execute(constants.NEW_GEOFENCES_ENDPOINT);
                requestingAllGeofencesNew = true;
            } else if (!requestingAllGeofencesNew && lastGeofenceUpdate != null) {
                Log.i("GEOFENCE MONITORING", "MainActivity: requestGeofencesNewer: not requesting new geofences, last update not null");

                long hoursSinceUpdate = checkElapsedTime(lastGeofenceUpdate.getTime());
                if (hoursSinceUpdate > 5) {
                    Log.i("GEOFENCE MONITORING", "MainActivity: requestGeofencesNewer: more than five hours since last update, requesting...");
                    DownloadFileFromURL downloadFileFromURLGeofences = new DownloadFileFromURL(this, constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, this);
                    downloadFileFromURLGeofences.execute(constants.NEW_GEOFENCES_ENDPOINT);
                    requestingAllGeofencesNew = true;
                }
            }
        }else if(fileExists(constants.GEOFENCES_FILE_NAME_WITH_EXTENSION)){
            Log.i("GEOFENCE MONITORING", "MainActivity: requestGeofencesNewer: no internet, parsing existing data");
            Log.i("INTERNAL STORAGE DEBUG", "MainActivity: requestGeofencesNewer: file does NOT exist, returning false");

            parseGeofences(constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, false);
        }else{
            showNetworkNotConnectedDialog();
            if(adapter.getCurrentFragment() instanceof HistoryFragment) {
                Log.i("GEOFENCE MONITORING", "MainActivity: requestGeofencesNewer: no internet, no existing info");

                ((HistoryFragment) adapter.getCurrentFragment()).addNewGeofenceInfoNew(null);
            }
        }
    }

    /**
     * Gets ical feed from Constants.ICAL_FEED_URL
     */
    public void requestEvents(){
        if(isConnectedToNetwork()) {
            if (!requestingEvents && lastEventsUpdate == null) {
                String url = buildEventsRequestURL();
                Log.i("EVENTS", "MainActivity: requestEvents: url is: " + url);
                DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, constants.ICAL_FILE_NAME_WITH_EXTENSION, this);
                downloadFileFromURL.execute(url);
                requestingEvents = true;
            }else if(!requestingEvents && lastEventsUpdate !=null){
                long hoursSinceUpdate = checkElapsedTime(lastEventsUpdate.getTime());
                if (hoursSinceUpdate > 5) {
                    String url = buildEventsRequestURL();
                    DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, constants.ICAL_FILE_NAME_WITH_EXTENSION, this);
                    downloadFileFromURL.execute(url);
                    requestingEvents = true;
                }
            }
        }else{
            if(fileExists(constants.ICAL_FILE_NAME_WITH_EXTENSION)){
                Log.i("INTERNAL STORAGE DEBUG", "MainActivity: requestEvents: file does exist");
                parseIcalFeed(constants.ICAL_FILE_NAME_WITH_EXTENSION, false);
            }else{
                showNetworkNotConnectedDialog();
                if(adapter.getCurrentFragment() instanceof EventsFragment) {
                    ((EventsFragment)adapter.getCurrentFragment()).handleNewEvents(null, null);
                }
            }
        }
    }

    /**
     *
     * @param previousTime
     * @return elapsed time between previousTime and current time in hours
     */
    private long checkElapsedTime(long previousTime){
        Calendar currentTime = Calendar.getInstance();
        java.util.Date currentDate = currentTime.getTime();
        long time = currentDate.getTime();
        long hoursSinceUpdate = (time - previousTime) / (1000 * 60 * 60);
        return hoursSinceUpdate;
    }

    private String buildEventsRequestURL(){
        String url = constants.ICAL_FEED_URL;
        Calendar c = Calendar.getInstance();
        java.util.Date currentDate = c.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(currentDate);
        url = url + constants.ICAL_FEED_DATE_REQUEST + formattedDate + constants.ICAL_FEED_FORMAT_REQUEST;
        return url;
    }

    /**
     * uses the volleyRequester to retrieve all quests from the server. Results are handled
     * in handleNewQuests()
     */
    public void requestQuests(){
        if(isConnectedToNetwork()) {
            if (lastQuestUpdate == null && !requestingQuests) {
                DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, constants.QUESTS_FILE_NAME_WITH_EXTENSION, this);
                downloadFileFromURL.execute(constants.QUESTS_FEED_URL);
                requestingQuests = true;
            }else if(!requestingQuests && lastQuestUpdate != null){
                long hoursSinceLastUpdate = checkElapsedTime(lastQuestUpdate.getTime());
                if(hoursSinceLastUpdate > 5){
                    DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, constants.QUESTS_FILE_NAME_WITH_EXTENSION, this);
                    downloadFileFromURL.execute(constants.QUESTS_FEED_URL);
                    requestingQuests = true;
                }
            }
        }else{
            if(fileExists(constants.QUESTS_FILE_NAME_WITH_EXTENSION)){
                Log.i("NEWQUESTS", "MainActivity: requestQuests : file does exist");
                Log.i("INTERNAL STORAGE DEBUG", "MainActivity: requestQuests: file does exist");

                parseQuests(constants.QUESTS_FILE_NAME_WITH_EXTENSION, false);
            }else{
                showNetworkNotConnectedDialog();
                Log.i("NEWQUESTS", "MainActivity: requestQuests : file does not exist");
                if(adapter.getCurrentFragment()instanceof QuestFragment){
                    ((QuestFragment)adapter.getCurrentFragment()).handleNewQuests(null);
                }
            }
        }
    }

    private boolean fileExists(String fileNameWithExtension){
        try {
            InputStream inputStream = openFileInput(fileNameWithExtension);
            Log.i("INTERNAL STORAGE DEBUG", "MainActivity: fileExists: file does exist, returning true");

            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("INTERNAL STORAGE DEBUG", "MainActivity: fileExists: file does NOT exist, returning false");

            return false;
        }
    }


    /**
     * Accesses the Ical feed that was saved into a file by DownloadFileFromURL, then uses
     * it to create a Calendar object. Once that is completed, calls buildEventContent() to use
     * that Calendar object to build and ArrayList of EventContent
     */
    private void parseIcalFeed(String fileNameWithExtension, boolean newInfo){

        if(newInfo){
            lastEventsUpdate = Calendar.getInstance().getTime();
        }
        FileInputStream fin = null;
        try {
            fin = openFileInput(fileNameWithExtension);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CalendarBuilder builder = new CalendarBuilder();
        net.fortuna.ical4j.model.Calendar calendar = null;
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);

        if(fin != null) {
            try {
                calendar = builder.build(fin);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserException e) {
                e.printStackTrace();
            }
        }
        if (calendar != null) {
            Log.i("EVENTS", "MainActivity: parseIcalFeed : calendar not null ");
            PropertyList plist = calendar.getProperties();
            buildEventContent(calendar, newInfo);
        }

        if(fin != null){
            try {
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void parseGeofences(String fileNameWithExtension, boolean isNewInfo){

        String jsonString = readFromFile(fileNameWithExtension);

        Log.i("NEWGEOFENCES", "MainActivity : parseGeofences : " + jsonString);

        Log.i("GEOFENCE MONITORING", "MainActivity: parseGeofences");


        final Gson gson = new Gson();

        allGeofencesNew = gson.fromJson(jsonString, AllGeofences.class);

        handleGeofencesNewMethod(allGeofencesNew, isNewInfo);

    }

    private void parseQuests(String fileNameWithExtension, boolean isNewInfo){

        questInfo = new ArrayList<>();

        if(isNewInfo){
            lastQuestUpdate = Calendar.getInstance().getTime();
        }

        String jsonString = readFromFile(fileNameWithExtension);

        Log.i("NEWGEOFENCES", "MainActivity : parseQuests : " + jsonString);
        final Gson gson = new Gson();
        try {
            JSONObject questsObject = new JSONObject(jsonString);
            JSONArray responseArr = questsObject.getJSONArray("content");

            for (int i = 0; i < responseArr.length(); i++) {
                Log.i("NEWGEOFENCES", "MainActivity : parseQuests : i = " + i);

                try {
                    Log.i("NEWGEOFENCES", "MainActivity : parseQuests : in inner try block ");

                    Quest responseQuest = gson.fromJson(responseArr.getString(i), Quest.class);
                    Log.i(logMessages.VOLLEY, "requestQuests : quest response string = : " + responseArr.getString(i));
                    questInfo.add(responseQuest);
                }
                catch (Exception e) {
                    Log.i("NEWGEOFENCES", "MainActivity : parseQuests : unable to parse: " + responseArr.getString(i));
                    Log.i("NEWGEOFENCES", "MainActivity : parseQuests : unable to parse: error message: " + e.getMessage());

                    e.getMessage();
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            Log.i("NEWGEOFENCES", "MainActivity : parseGeofences : outer catch block : error : " + e.getMessage());
            handleNewQuests(null,false);
            e.printStackTrace();
        }
        handleNewQuests(questInfo, isNewInfo);
    }

    private String readFromFile(String fileNameWithExtension) {

        String ret = "";

        try {
            InputStream inputStream = openFileInput(fileNameWithExtension);

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
     * Builds an ArrayList of EventContents and passes them to the handleNewEvents method.
     * Note that this method does not add events to EventContents unless they have a start time.
     * @param calendar
     */
    private void buildEventContent(net.fortuna.ical4j.model.Calendar calendar, boolean isNewInfo){

        Log.i("EVENT DEBUGGING", "MainActivity: buildEventContent: calendar component size is: " + calendar.getComponents().size());
        ComponentList componentList = calendar.getComponents();
        ArrayList<EventContent> eventContents = new ArrayList<>();
        boolean addComponent = true;
        for(int i = 0; i<componentList.size(); i++){
            Component component = (Component) componentList.get(i);
            PropertyList propertyList = component.getProperties();
            EventContent eventContent = new EventContent();
            if(propertyList.getProperty(Property.LOCATION) != null) {
                eventContent.setLocation(propertyList.getProperty(Property.LOCATION).getValue());
            }else {
                eventContent.setLocation(getResources().getString(R.string.no_location));
            }if(propertyList.getProperty(Property.URL) != null){
                eventContent.setUrl(propertyList.getProperty(Property.URL).getValue());
            }
            if(propertyList.getProperty(Property.SUMMARY) != null) {
                    eventContent.setTitle(propertyList.getProperty(Property.SUMMARY).getValue());
            }else{
                eventContent.setTitle(getResources().getString(R.string.no_title));
            }if(propertyList.getProperty(Property.DTSTART) != null) {
                addComponent = true;
                DtStart start = (DtStart) propertyList.getProperty(Property.DTSTART);
                Date startDate = start.getDate();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
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
            }else{
                Log.i("EVENT DEBUGGING", "MainActivity: buildEventContent: NOT adding component");

            }
        }

       handleNewEvents(eventContents, isNewInfo);
    }


    /**
     * Called from BuildEventContent, adds events to a hashmap where the key is the date
     * and the value is an ArrayList of EventContent for events happening that day.
     * Notifies the fragment of the new events if the fragment in view is the EventsFragment
     * @param events
     */
    public void handleNewEvents(ArrayList<EventContent> events, boolean isNewInfo) {


        //TODO: events same for every day now. Hashmap should now contain the positions of
        //the start of the new dates (although doing a linear search is probably fine...)

        Log.i("EVENT DEBUGGING", "MainActivity: handleNewEvents");

        requestingEvents = false;
        String completeDate;
        String[] completeDateArray;
        String dateByDay;



        if(events == null){
            if(adapter.getCurrentFragment()instanceof EventsFragment){
                Log.i("EVENT DEBUGGING", "MainActivity: handleNewEvents : current fragment is eventsfragment, events are null");
                ((EventsFragment)adapter.getCurrentFragment()).handleNewEvents(null, null);
            }
        }else {
            Log.i("EVENT DEBUGGING", "MainActivity: handleNewEvents : events are not null");

            if(eventsMapByDate == null) {
                eventsMapByDate = new LinkedHashMap<>();
            }
            eventsMapByDate.clear();

            if(eventContentList == null){
                eventContentList = new ArrayList<>();
            }
            eventContentList.clear();


            Calendar c = Calendar.getInstance();

            for (int i = 0; i < events.size(); i++) {

                // Add new date values to hashmap if not already there
                completeDate = events.get(i).getStartTime();
                completeDateArray = completeDate.split("T");
                dateByDay = completeDateArray[0];

                String[] dateArray = dateByDay.split("-");

                java.util.Calendar eventCalendar = java.util.Calendar.getInstance();
                eventCalendar.set(Integer.parseInt(dateArray[0]), Integer.parseInt(dateArray[1]) - 1, Integer.parseInt(dateArray[2]), 23, 59, 59);

                if (eventCalendar.getTimeInMillis() >= c.getTimeInMillis()) {

                    eventContentList.add(events.get(i));
                    Log.i("EVENT DEBUGGING", "adding item to eventContentList from events");
                    Log.i("EVENT DEBUGGING", "item is: " + events.get(i).getDescription());


                    // If key already there, add + update new values
                    if (!eventsMapByDate.containsKey(dateByDay)) {
                        eventsMapByDate.put(dateByDay, i);
                    }

                }
            }

            Log.i("EVENT DEBUGGING",
                    "MainActivity: handleNewEvents : about to check current fragment : eventsMapByDate size is: "+eventsMapByDate.size());

            Log.i("EVENT DEBUGGING",
                    "MainActivity: handleNewEvents : eventContentList size is: "+eventContentList.size());

            Log.i("EVENT DEBUGGING",
                    "MainActivity: handleNewEvents : events size is: "+events.size());


            if (adapter.getCurrentFragment() instanceof EventsFragment) {
                Log.i("EVENT DEBUGGING", "MainActivity: handleNewEvents : current fragment is events fragment");
                ((EventsFragment)adapter.getCurrentFragment()).handleNewEvents(eventsMapByDate, eventContentList);
            }
        }

        if(events.size() != 0 && isNewInfo) {
            lastEventsUpdate = Calendar.getInstance().getTime();
        }
    }

    public ArrayList<EventContent> getAllEvents(){
        return this.eventContentList;
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
                parseIcalFeed(fileNameWithExtension, true);
            }if(fileNameWithExtension.equals(constants.GEOFENCES_FILE_NAME_WITH_EXTENSION)){
                parseGeofences(fileNameWithExtension, true);
            }if(fileNameWithExtension.equals(constants.QUESTS_FILE_NAME_WITH_EXTENSION)){
                parseQuests(fileNameWithExtension, true);
            }
        }else{
            if(fileNameWithExtension.equals(constants.ICAL_FILE_NAME_WITH_EXTENSION)) {
                handleNewEvents(null, false);
                requestingEvents = false;
            }if(fileNameWithExtension.equals(constants.GEOFENCES_FILE_NAME_WITH_EXTENSION)){
                handleGeofencesNewMethod(null, false);
                requestingAllGeofencesNew = false;
            }if(fileNameWithExtension.equals(constants.QUESTS_FILE_NAME_WITH_EXTENSION)){
                handleNewQuests(null, false);
                requestingQuests = false;
            }
        }
    }

    public Quest getQuestInProgress() {
        return questInProgress;
    }

    public void setQuestInProgress(Quest questInProgress) {
        this.questInProgress = questInProgress;
    }

    @Override
    protected void onPause() {
        stopLocationUpdates();
        super.onPause();
    }



    //TODO: now that we no longer need a location sent in to request geofences, call that request
    //TODO: earlier, before getting location updates, then we only need to get location updates
    //TODO: for the quest in progress screen

}
