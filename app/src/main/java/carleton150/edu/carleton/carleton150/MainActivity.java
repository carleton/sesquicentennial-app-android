package carleton150.edu.carleton.carleton150;

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
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;

import carleton150.edu.carleton.carleton150.Adapters.MyFragmentPagerAdapter;
import carleton150.edu.carleton.carleton150.ExtraFragments.QuestCompletedFragment;
import carleton150.edu.carleton.carleton150.Interfaces.RetrievedFileListener;
import carleton150.edu.carleton.carleton150.MainFragments.EventsFragment;
import carleton150.edu.carleton.carleton150.MainFragments.HistoryFragment;
import carleton150.edu.carleton.carleton150.MainFragments.HomeFragment;
import carleton150.edu.carleton.carleton150.MainFragments.MainFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestInProgressFragment;
import carleton150.edu.carleton.carleton150.Models.DownloadFileFromURL;
import carleton150.edu.carleton.carleton150.POJO.EventObject.EventContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfo.AllGeofences;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;

/**
 * Monitors location information and calls methods in the main view fragments
 * to handle location changes. Controls which fragment is in view. Requests all the events,
 * history information, and quests and stores them for the fragments.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener,
        RetrievedFileListener {

    //things for location
    public Location mLastLocation = null;
    // Google client to interact with Google API
    public GoogleApiClient mGoogleApiClient;
    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;
    private boolean shouldBeRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;
    public boolean needToShowGPSAlert = true;
    private Handler handler = new Handler();
    //runnable to try connecting to google API client. Used if the connection fails
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mGoogleApiClient.connect();
        }
    };

    private Quest questInProgress;
    private boolean requestingQuests = false;
    private ArrayList<Quest> questInfo = null;
    private java.util.Date lastQuestUpdate;

    //For retrieving and handling event info
    private LinkedHashMap<String, Integer> eventsMapByDate;
    private ArrayList<EventContent> eventContentList;
    private boolean requestingEvents = false;
    private java.util.Date lastEventsUpdate;

    //For retrieving and handling point of interest info
    private AllGeofences allGeofencesNew = null;
    private boolean requestingAllGeofencesNew = false;
    private java.util.Date lastGeofenceUpdate;

    MyFragmentPagerAdapter adapter;

    //Alert Dialog builders for different app alerts
    AlertDialog.Builder networkAlertDialogBuilder;
    AlertDialog.Builder playServicesConnectivityAlertDialogBuilder;
    AlertDialog.Builder noGpsAlertDialogBuilder;
    AlertDialog.Builder onCampusFeatureAlertDialogBuilder;



    /**
     * Sets up Google Play Services and builds a Google API Client. Sets up the tabs in the tab
     * views.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sets exception handler to log exceptions not caught by app
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());

        //Builds AlertDialog builders
        networkAlertDialogBuilder = new AlertDialog.Builder(this);
        playServicesConnectivityAlertDialogBuilder = new AlertDialog.Builder(this);
        noGpsAlertDialogBuilder = new AlertDialog.Builder(this);
        onCampusFeatureAlertDialogBuilder = new AlertDialog.Builder(this);

        //check availability of play services for location data
        if (checkPlayServices()) {
            //Builds and connect google API client. Creates a location request
            //for the client
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

        //Requests information
        if(isConnectedToNetwork()) {
            requestGeofences();
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
     * Checks if GPS is enabled and starts location updates if necessary
     * and possible
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
     * using a constant delay time between attempts
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        handler.postDelayed(runnable, Constants.GOOGLE_PLAY_CONNECTION_RETRY_DELAY);
    }

    /**
     * Displays an alert dialog if unable to connect to the GoogleApiClient
     *
     * @param connectionResult ConnectionResult that contains the connection error
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        AlertDialog dialog = playServicesConnectivityAlertDialogBuilder.create();
        if(!dialog.isShowing()) {
            showAlertDialog(getString(R.string.connection_to_google_client_failed_with_message) +
                            connectionResult.getErrorMessage() + "\n" + getString(R.string.google_play_code)
                            + connectionResult.getErrorCode(),
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
     *
     * @return true if play services is available, false otherwise
     */
    public boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        Constants.PLAY_SERVICES_RESOLUTION_REQUEST).show();
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
     * @param location new location
     */
    @Override
    public void onLocationChanged(Location location) {
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
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(Constants.DISPLACEMENT);
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
            Log.i(LogMessages.LOCATION, "Location updates stopped");
            mRequestingLocationUpdates = false;
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    /**
     * Starts location updates if it is possible to do so (if Google API client
     * is connected and GPS is turned on)
     */
    public void startLocationUpdatesIfPossible(){
        Log.i(LogMessages.LOCATION, "starting location updates if possible");
        shouldBeRequestingLocationUpdates = true;
        startLocationUpdates();
    }

    /**
     * Stops location updates if it is possible to do so (if location updates
     * are currently being requested)
     */
    public void stopLocationUpdatesIfPossible(){
        Log.i(LogMessages.LOCATION, "stopping location updates if possible");
        shouldBeRequestingLocationUpdates = false;
        stopLocationUpdates();
    }


    /**
     * checks whether phone has network connection.
     *
     * @return true if connected, false otherwise
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

    /**
     * Shows a dialog alerting the user that the feature they are attempting to use
     * is intended to be used on campus
     */
    public void showOnCampusFeatureAlertDialogQuestInProgress() {
        AlertDialog dialog = onCampusFeatureAlertDialogBuilder.create();
        if(!dialog.isShowing()){
            showAlertDialog(getString(R.string.quest_in_progress_unuseable_off_campus), dialog);
        }
    }

    /**
     * shows an alert dialog with a specified message
     *
     * @param message String with the message to display
     * @param dialog AlertDialog to display
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
     * @param dialog AlertDialog to display
     */
    public void showAlertDialogNoNeutralButton(AlertDialog dialog) {
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }


    /**
     * If QuestInProgressFragment or QuestCompletedFragment is the current fragment,
     * overrides back button to replaces the QuestInProgressFragment
     * with a new QuestFragment. If HomeFragment is the current fragment, goes back
     * in webview if possible, otherwise does default back behavior
     */
    @Override
    public void onBackPressed() {
        if (adapter.getCurrentFragment() instanceof QuestInProgressFragment ||
                adapter.getCurrentFragment() instanceof QuestCompletedFragment) {
            adapter.backButtonPressed();
        }else if(adapter.getCurrentFragment() instanceof HomeFragment){
            boolean wentBack = ((HomeFragment)adapter.getCurrentFragment()).backPressed();
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
     *
     * @return SharedPreferences for the quest preferences
     */
    public SharedPreferences getPersistentQuestStorage() {
        return getSharedPreferences(Constants.QUEST_PREFERENCES_KEY, 0);

    }

    /**
     * Opens a url in the browser. Used to display events in browser
     * if user clicks on the link
     * @param url String of the url to display in the browser
     */
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
     * Registers that app is no longer requesting quests, records the last quest update,
     * and passes quests to current fragment if it is a QuestFragment
     * @param newQuests ArrayList of Quests retrieved
     * @param newInfo boolean flag where true means the info was retrieved from the
     *                web, false means it was retrieved from phone's internal storage
     */
    public void handleNewQuests(ArrayList<Quest> newQuests, boolean newInfo) {
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
    public void handleGeofences(AllGeofences geofences, boolean isNewInfo){
        requestingAllGeofencesNew = false;
        if(geofences != null && isNewInfo) {
            lastGeofenceUpdate = Calendar.getInstance().getTime();
        }
        if(adapter.getCurrentFragment() instanceof HistoryFragment){
            ((HistoryFragment) adapter.getCurrentFragment()).addNewGeofenceInfoNew(geofences);
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
     * Requests geofences if it is possible to do so. Updates the hours since the last
     * geofence update. If there is no internet, retrieves geofences from storage and parses
     * them
     */
    public void requestGeofences(){
        if(isConnectedToNetwork()) {
            if (!requestingAllGeofencesNew && lastGeofenceUpdate == null) {
                DownloadFileFromURL downloadFileFromURLGeofences = new DownloadFileFromURL(this, Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, this);
                downloadFileFromURLGeofences.execute(Constants.NEW_GEOFENCES_ENDPOINT);
                requestingAllGeofencesNew = true;
            } else if (!requestingAllGeofencesNew && lastGeofenceUpdate != null) {
                long hoursSinceUpdate = checkElapsedTime(lastGeofenceUpdate.getTime());
                if (hoursSinceUpdate > 5) {
                    DownloadFileFromURL downloadFileFromURLGeofences = new DownloadFileFromURL(this, Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, this);
                    downloadFileFromURLGeofences.execute(Constants.NEW_GEOFENCES_ENDPOINT);
                    requestingAllGeofencesNew = true;
                }
            }
        }else if(fileExists(Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION)){
            parseGeofences(Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, false);
        }else{
            showNetworkNotConnectedDialog();
            if(adapter.getCurrentFragment() instanceof HistoryFragment) {
                ((HistoryFragment) adapter.getCurrentFragment()).addNewGeofenceInfoNew(null);
            }
        }
    }

    /**
     * Gets ical feed from Constants.ICAL_FEED_URL. If there is no internet,
     * gets the feed from app internal storage and parses it
     */
    public void requestEvents(){
        if(isConnectedToNetwork()) {
            if (!requestingEvents && lastEventsUpdate == null) {
                String url = buildEventsRequestURL();
                DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, Constants.ICAL_FILE_NAME_WITH_EXTENSION, this);
                downloadFileFromURL.execute(url);
                requestingEvents = true;
            }else if(!requestingEvents && lastEventsUpdate !=null){
                long hoursSinceUpdate = checkElapsedTime(lastEventsUpdate.getTime());
                if (hoursSinceUpdate > 5) {
                    String url = buildEventsRequestURL();
                    DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, Constants.ICAL_FILE_NAME_WITH_EXTENSION, this);
                    downloadFileFromURL.execute(url);
                    requestingEvents = true;
                }
            }
        }else{
            if(fileExists(Constants.ICAL_FILE_NAME_WITH_EXTENSION)){
                parseIcalFeed(Constants.ICAL_FILE_NAME_WITH_EXTENSION, false);
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
     * @param previousTime long representation of a time
     * @return elapsed time between previousTime and current time in hours
     */
    private long checkElapsedTime(long previousTime){
        Calendar currentTime = Calendar.getInstance();
        java.util.Date currentDate = currentTime.getTime();
        long time = currentDate.getTime();
        //converting ms to hours
        long hoursSinceUpdate = (time - previousTime) / (1000 * 60 * 60);
        return hoursSinceUpdate;
    }

    /**
     * builds the URL for requesting events because it uses the date
     * @return
     */
    private String buildEventsRequestURL(){
        String url = Constants.ICAL_FEED_URL;
        Calendar c = Calendar.getInstance();
        java.util.Date currentDate = c.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(currentDate);
        url = url + Constants.ICAL_FEED_DATE_REQUEST + formattedDate + Constants.ICAL_FEED_FORMAT_REQUEST;
        return url;
    }

    /**
     * requests Quests if connected to network. Otherwise, parses quests stored in app
     * internal storage
     */
    public void requestQuests(){
        if(isConnectedToNetwork()) {
            if (lastQuestUpdate == null && !requestingQuests) {
                DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, Constants.QUESTS_FILE_NAME_WITH_EXTENSION, this);
                downloadFileFromURL.execute(Constants.QUESTS_FEED_URL);
                requestingQuests = true;
            }else if(!requestingQuests && lastQuestUpdate != null){
                long hoursSinceLastUpdate = checkElapsedTime(lastQuestUpdate.getTime());
                if(hoursSinceLastUpdate > 5){
                    DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, Constants.QUESTS_FILE_NAME_WITH_EXTENSION, this);
                    downloadFileFromURL.execute(Constants.QUESTS_FEED_URL);
                    requestingQuests = true;
                }
            }
        }else{
            if(fileExists(Constants.QUESTS_FILE_NAME_WITH_EXTENSION)){
                Log.i("NEWQUESTS", "MainActivity: requestQuests : file does exist");
                Log.i("INTERNAL STORAGE DEBUG", "MainActivity: requestQuests: file does exist");

                parseQuests(Constants.QUESTS_FILE_NAME_WITH_EXTENSION, false);
            }else{
                showNetworkNotConnectedDialog();
                Log.i("NEWQUESTS", "MainActivity: requestQuests : file does not exist");
                if(adapter.getCurrentFragment()instanceof QuestFragment){
                    ((QuestFragment)adapter.getCurrentFragment()).handleNewQuests(null);
                }
            }
        }
    }

    /**
     * Checks if a file exists in internal storage
     * @param fileNameWithExtension name of file
     * @return true if file exists, false otherwise
     */
    private boolean fileExists(String fileNameWithExtension){
        try {
            openFileInput(fileNameWithExtension);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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

    /**
     * Parses JSON that contains geofences into AllGeofences object
     * @param fileNameWithExtension name of file where JSON is stored
     * @param isNewInfo true if info was retrieved from internet, false if it was
     *                  retrieved from app's internal storage
     */
    private void parseGeofences(String fileNameWithExtension, boolean isNewInfo){

        String jsonString = readFromFile(fileNameWithExtension);
        final Gson gson = new Gson();
        allGeofencesNew = gson.fromJson(jsonString, AllGeofences.class);
        handleGeofences(allGeofencesNew, isNewInfo);
    }

    /**
     * Parses quest JSONS into an array of Quest objects
     * @param fileNameWithExtension file where JSON is located in internal storage
     * @param isNewInfo true if JSON was retrieved from URL, false if JSON
     *                  was retrieved from app's internal storage
     */
    private void parseQuests(String fileNameWithExtension, boolean isNewInfo){
        questInfo = new ArrayList<>();
        if(isNewInfo){
            lastQuestUpdate = Calendar.getInstance().getTime();
        }
        String jsonString = readFromFile(fileNameWithExtension);
        final Gson gson = new Gson();
        try {
            JSONObject questsObject = new JSONObject(jsonString);
            JSONArray responseArr = questsObject.getJSONArray("content");

            for (int i = 0; i < responseArr.length(); i++) {
                try {
                    Quest responseQuest = gson.fromJson(responseArr.getString(i), Quest.class);
                    questInfo.add(responseQuest);
                }
                catch (Exception e) {
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

    /**
     * Reads a string from a file
     * @param fileNameWithExtension the name of the file to read from in app internal storage
     * @return the String representing the file contents
     */
    private String readFromFile(String fileNameWithExtension) {

        String ret = "";

        if(fileNameWithExtension.equals(Constants.QUESTS_FILE_NAME_WITH_EXTENSION)){
            return "{\"content\":[{\"name\":\"Alumni Quest\",\"desc\": \"Visits the various attractions on campus relevant to Carleton Alumni\",\"compMsg\": \"\", \"creator\": \"\",\"image\": \"\",\n" +
                    "            \"difficulty\": 1,\n" +
                    "            \"audience\": \"Alumni, Students\",\n" +
                    "            \"waypoints\":[\n" +
                    "                {\n" +
                    "                    \"lat\":44.45945,\n" +
                    "                    \"lng\":-93.14475,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\":{\n" +
                    "                        \"text\": \"It's a field of dreams, without the bases\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"Check out the Arb Map online and you’ll find this field bordered by Wall Street\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\": {\n" +
                    "\t\t\t\t\t\"text\" : \"Alumni Field is in the southeast corner of the Upper Arb (H3-4 and I3-4 on the Arb map). The area used to be farmed, but was removed from agriculture and planted to brome in 1986. Trees have been planted here in various years starting in 1990.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"lat\":44.45909,\n" +
                    "                     \"lng\":-93.15586,\n" +
                    "                     \"rad\":5000,\n" +
                    "                     \"clue\": {\n" +
                    "                         \"text\": \"The closest you'll get to a Carleton B and B\"\n" +
                    "                     },\n" +
                    "                     \"hint\": {\n" +
                    "                         \"text\": \"Admissions is also located in this building\"\n" +
                    "                       },\n" +
                    "\t\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\t\"text\" : \"Since 1993, Alumni Guest House has provided lodging for alumni, parents, friends, guest speakers, job candidates, and other Carleton visitors.\"\n" +
                    "\t\t\t\t\t}\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"lat\":44.45947,\n" +
                    "                    \"lng\":-93.15514,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"This Sterling kit house is named for a Carleton romance languages prof\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"\\\"Tweet\\\" at us when you reach it!\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"Named for its original owner, James Pyper Bird, a Carleton romance languages professor from 1915 to the early 1940's,  Alumni Relations has officed in Bird House since 2001.  It is a Sterling Kit Home built in the 1920s, and was once the residence of philosophy professor Martin Eshleman.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "{\n" +
                    "                    \"lat\":44.45969,\n" +
                    "                     \"lng\":-93.15528,\n" +
                    "                     \"rad\":5000,\n" +
                    "                     \"clue\": {\n" +
                    "                         \"text\": \"Stop and take a selfie at this much-photographed landmark given to Carleton by the Class of 1930\"\n" +
                    "                     },\n" +
                    "                     \"hint\": {\n" +
                    "                         \"text\": \"A place on campus know for sharing greetings--from dancing cows during New Student Week to iconic status on postcards and publications, passing this landmark is a \\\"sign\\\" you've arrived at Carleton.\"\n" +
                    "                       },\n" +
                    "\t\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\t\"text\" : \"Members of the Class of 1930, looking for an appropriate way to commemorate the 50th anniversary of their graduation from Carleton and to serve the College, raised funds to support an integrated sign project to welcome and direct visitors toand around Carleton's campus. In addition to the handsome Indiana limestone Carleton College marker on First and College streets, the project involved installing uniform stone markers on 19 campus buildings.\"\n" +
                    "\t\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "\t\t\t\t{\n" +
                    "                    \"lat\":44.46086,\n" +
                    "                    \"lng\":-93.15574,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"Where news gets shared thanks to the Class of 1923 \"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"It doesn't take a degree in Political Science or Economics to know this excellent spot is a great place to share campus events.\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"The  May 19, 1923 Carletonian reports \\\"New Bulletin Board to\u2028Be Senior Memorial.\\\" According to the article, the large bulletin board was built in front of Willis hall as a memorial by the Class of 1923 with $160 in contributions from the class. The sign board was unveiled during the class' commencement ceremony.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "{\n" +
                    "                    \"lat\":44.46395,\n" +
                    "                    \"lng\":-93.15192,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"No strobe light \\\"lightning,\\\" foam rock \\\"avalanches,\\\" or flying glitter \\\"snow\\\" here, Carleton's \\\"Aggro Crag\\\" is made from 15 tons of native Minnesota limestone and 4 tons of New York bluestone.\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"Create your own \\\"council\\\" or \\\"ring\\\" in a new day around a fire here.\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"Drawing from both Nordic and Native American traditions, the council ring serves as a meeting place for conversation, song, dance, storytelling, poetry, and campfires, linking humanity and nature.\u2028\u2028As part of their 50th Reunion gift, the Class of 1954 funded the creation of a council ring at Carleton as a gathering place for students. Members of the class say the council ring is a strong symbol of their experience at the College, and they hope to foster and enhance that same sense of community for future generations. Council rings can be found on other campuses, including the University of Wisconsin at Madison and the University of Massachusetts Dartmouth. \u2028\u2028The council ring design was created in the early 1900s by landscape architect Jens Jensen, a designer and conservationist who worked closely with Frank Lloyd Wright and the Prairie School of design. A Danish immigrant who developed many of Chicago’s parks, gardens, and estates, Jensen’s designs feature local building materials and native plants, working in harmony with nature and the site. \\\"Here there is no social caste,\\\" he once said of the council ring. \\\"All are on the same level, looking each other in the face. A ring speaks of strength and friendship, and is one of the great symbols of mankind.\\\"\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "{\n" +
                    "                    \"lat\":44.46425,\n" +
                    "                     \"lng\":-93.15163,\n" +
                    "                     \"rad\":5000,\n" +
                    "                     \"clue\": {\n" +
                    "                         \"text\": \"A gift from Faribault's A.M. Brand and a crew of student workers helped this emerge from a gravel pit and dump in the 1930s\"\n" +
                    "                     },\n" +
                    "                     \"hint\": {\n" +
                    "                         \"text\": \"Today this location's name is misleading, but for the 70s alumni who strolled on this \\\"hill\\\", everything was coming up \\\"roses\\\" (or a sister flower!).\"\n" +
                    "                       },\n" +
                    "\t\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\t\"text\" : \"Prior to 1934, \\\"Lilac Hill,\\\" north of the lower lakes, was a gravel pit, \\\"a receptor of rubbish, tin cans, junk and last year's hat,\\\" at best an eyesore. In that year, Mr. A. M. Brand, an outstanding lilac expert and owner of a peony farm in Faribault, gave to the College some 1,500 \\\"pedigreed\\\" lilac bushes, representing more than 90 varieties. Planting was done by a crew of students under Mr. [D. Blake \\\"Stewsie\\\"] Stewart's direction, with results which year after year at lilac time enthrall and fascinate seekers after beauty.' -From Carleton: The First Century\"\n" +
                    "\t\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "\t\t\t\t{\n" +
                    "                    \"lat\":44.45859,\n" +
                    "                    \"lng\":-93.15792,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"Before this building was renovated for student housing, it was home to Carleton's Alumni Annual Fund\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"Named for Carleton Geology Professor Eiler Henrickson '43\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"It is unclear when this building was built – a two-story house stood on the property in 1894, but this may have been a different building which was razed or moved to make room for the current one. At any rate, the house which sits there now was in place by 1901, when Carleton bought it from Miron W. Skinner, a college trustee at the time, and later the namesake of Skinner Memorial Chapel.  Carleton used the house as a faculty rental for more than 50 years, until it was purchased by Eiler Henrickson ’43 in 1953. Professor Henrickson taught geology at Carleton from 1946 to 1987, and sold the house back to the College a year after his retirement.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "{\n" +
                    "                    \"lat\":44.45863,\n" +
                    "                    \"lng\":-93.14806,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"For many years, Carleton's \\\"Distinguished Visitors-In-Residence\\\" would come here to rest their \\\"head.\\\"\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"Catch a glimpse of the tennis courts and Bell Field from this house's back yard.\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"Both Headley House and Headley Cottage were owned by the family of Leal Headley ’07, Carleton Professor of Philosophy, Psychology and Education from 1911 to 1952. For many years, Headley House served as the lodging place for Carleton's \\\"Distinguished Visitor-In-Residence\\\" program, which sought to bring prominent individuals to campus, either academic or non-academic, for short-term residential visits.\u2028\u2028Headley House was donated to the College by Leal and Harriet’s son Marston and his wife Margaret in 1988.  The same year, Carleton bought the adjacent property, now called Headley Cottage, when Harriet put it up for sale. \u2028\u2028Many don't realize this, but Professor Headley was in the middle of the enormous task of writing \\\"Carleton: The First Century\\\" when he died in 1965. The book was completed by Merrill \\\"Casey\\\" Jarchow P '80 H '88. \"\n" +
                    "\t\t\t\t}\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        },\n" +
                    "\n" +
                    "{\"name\":\"Campus Inscriptions\",\"desc\": \"Given ten inscriptions found on plaques or other public markers at various spots on the Carleton campus, can you locate each site? A second hint is provided to accompany each inscription, as may prove needful.\",\"compMsg\": \"\", \"creator\": \"Content by Senior Associate in the Carleton Archives Eric Hillemann\",\"image\": \"\",\n" +
                    "            \"difficulty\": 2,\n" +
                    "            \"audience\": \"Students, Adventurers\",\n" +
                    "            \"waypoints\":[\n" +
                    "                {\n" +
                    "                    \"lat\":44.4616,\n" +
                    "                    \"lng\":-93.15498,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"BY HORSE AND BY HAND\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"That inscription continues: \\\"MEMBERS OF THE CLASS OF 1881 PLACED THIS STONE HERE UPON GRADUATION.\\\"\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\": {\n" +
                    "\t\t\t\t\t\"text\" : \"In the spring of 1881 the fifteen members of Carleton's senior class, troubled by a prophecy that none of them would ever make any particular mark in the world, determined that at any rate they would permanently mark their college campus by moving, at great expense of human and equine effort, a large granite boulder from its original home along Northfield's Division Street to a new location up the hill and onto the college grounds where it would be set down so as to be everlastingly \\\"right square in the way.\\\" And there the Class of 1881 stone still sits, though now considerably less inconvenient in its siting than was first intended.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"lat\":44.46111,\n" +
                    "                     \"lng\":-93.15592,\n" +
                    "                     \"rad\":5000,\n" +
                    "                     \"clue\": {\n" +
                    "                         \"text\": \"\\\"AD MCMIX\\\" - which does not, as wise sophomores used to tell gullible freshmen, refer to Mr. A. D. McMix, the Scottish architect said to have been responsible for the building in question.\"\n" +
                    "                     },\n" +
                    "                     \"hint\": {\n" +
                    "                         \"text\": \"A nearby inscription commemorates a 1979 rededication in memory of the original two donors, Deborah and Fred.\"\n" +
                    "                       },\n" +
                    "\t\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\t\"text\" : \"Fred B. Hill, a Carleton graduate of 1900, returned to his alma mater after a few years as a teacher of Biblical Literature. His wife, the former Deborah Sayles, was an heiress whose family wealth provided the means for the couple in 1909 to make a gift to the college of \\\"Sayles-Hill,\\\" originally a gymnasium housing both a basketball court and swimming pool. At the end of the 1970s the former gym was remodeled into the Sayles-Hill Campus Center, home to, among other things, the college bookstore, post office, and \\\"Great Space.\\\"\"\n" +
                    "\t\t\t\t\t}\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"lat\":44.45967,\n" +
                    "                    \"lng\":-93.1547,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"GLORIAE DEI DEDICATA ANTE DIEM VITI IDUS OCTOBRES ANNO DOMINI MCMXVI\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"The inscription also informs that this building was erected by Emily in memory of her husband Miron, \\\"A PIONEER SETTLER IN NORTHFIELD\\\" and \\\"ONE OF THE FOUNDERS OF CARLETON COLLEGE\\\"\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"Modeled after an English parish church, Skinner Memorial Chapel was completed in 1916 and dedicated as the College celebrated the 50th anniversary of its founding. The building was made possible by the generosity of Mrs. Emily Skinner, in memory of her husband Miron W. Skinner, a Northfielder and one of the school's original trustees, who had died in 1909. Mrs. Skinner directed that the Chapel's main entrance should face south, toward the town, to symbolize the close and cordial connections that have usually characterized the \\\"town-gown\\\" relationship in Northfield.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "{\n" +
                    "                    \"lat\":44.46188,\n" +
                    "                     \"lng\":-93.15395,\n" +
                    "                     \"rad\":5000,\n" +
                    "                     \"clue\": {\n" +
                    "                         \"text\": \"\\\"LOVE ALTERS NOT NOR LIGHT WITH TIMES SWIFT FLIGHT\\\" and \\\"SHE HAD ONLY TO TOUCH AN IDEA TO MAKE IT LIVE\\\"\"\n" +
                    "                     },\n" +
                    "                     \"hint\": {\n" +
                    "                         \"text\": \"Those inscriptions also include, respectively, \\\"ANNA T. LINCOLN\\\" and \\\"IN MEMORY OF LAUDIE PORTER\\\"\"\n" +
                    "                       },\n" +
                    "\t\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\t\"text\" : \"The base of the sundial now situated in front of Laird Hall belonged originally to the Anna T. Lincoln Memorial Sundial, dedicated on the lawn just north of Skinner Chapel in 1921, honoring the memory of a beloved college employee who had served the college's women for 30 years (1879-1909) as \\\"Matron\\\" or \\\"Superintendent of Gridley Hall.\\\" Moved 40-some years later, the pedestal eventually went into storage, and then was put into new service in its present location in 1991, topped now by a new sundial memorializing Assistant Professor of Flute Laudie Porter, the late wife of Carleton professor and president David Porter.\"\n" +
                    "\t\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "\t\t\t\t{\n" +
                    "                    \"lat\":44.4608,\n" +
                    "                    \"lng\":-93.15582,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"\\\"THEY SHALL GROW NOT OLD, AS WE THAT ARE LEFT GROW OLD; AGE SHALL NOT WEARY THEM NOR THE YEARS CONDEMN\\\"\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"Next to this inscription is a plaque inscribed (at the time of the building's 1953 rededication) \\\"DEDICATED TO THE MEMORY OF CARLETON ALUMNI WHO DIED IN THE SERVICE OF THEIR COUNTRY\\\"\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"Willis Hall, the first building constructed for the college's use, was erected (intermittently, as funds allowed) between 1869 and 1872, and then rebuilt following a damaging fire in 1879. It was named for an early benefactress, Miss Susan Willis. The center of collegiate life for much of its history, in the early 1950s Willis Hall was remodeled into the Willis Memorial Union, honoring the memory of Carleton alumni who lost their lives in American military service. Since the transfer of \\\"union\\\" functions to the Sayles-Hill Campus Center in 1979, the building has reverted to Willis Hall, primarily housing classrooms and departmental offices.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "{\n" +
                    "                    \"lat\":44.46207,\n" +
                    "                    \"lng\":-93.15477,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"\\\"THE HISTORY OF THE HUMAN RACE IS A CONTINUAL STRUGGLE FROM DARKNESS TOWARD LIGHT. IT IS THEREFORE TO NO PURPOSE TO DISCUSS THE USE OF KNOWLEDGE. MAN WANTS TO KNOW, AND WHEN HE CEASES TO  DO SO HE IS NO LONGER MAN.\\\"\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"This quotation, from the Norwegian Fridtjof Nansen, was personally selected by President Larry Gould for the building he opened in 1956. Where in the building is it now? Someone on the staff might help you to answer such a question. (That's a clue...)\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"When the present library building was first dedicated in 1956 the Nansen quotation was placed on the wall above the checkout area, near the main entrance. Following extensive remodeling and expansion the library was rededicated in 1984, and Nansen's words moved deeper into the building's interior. In 1995 the Carleton Library was renamed the Laurence McKinley Gould Library to honor the College's popular fifth president (1945-62) who, like Nansen, had earlier gained international stature as a polar explorer.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "{\n" +
                    "                    \"lat\":44.46005,\n" +
                    "                     \"lng\":-93.1497,\n" +
                    "                     \"rad\":5000,\n" +
                    "                     \"clue\": {\n" +
                    "                         \"text\": \"\\\"THIS BUILDING HAS BEEN MADE POSSIBLE THROUGH THE SUPPORT OF MANY ALUMNAE ALUMNI AND FRIENDS OF CARLETON COLLEGE AND ESPECIALLY BY THE GENEROSITY OF THE TOWSLEY FAMILY OF ANN ARBOR MICHIGAN AND OF MARGARET BELL CAMERON A TRUSTEE OF THE COLLEGE\\\"\"\n" +
                    "                     },\n" +
                    "                     \"hint\": {\n" +
                    "                         \"text\": \"This inscription begins: \\\"NAMED IN HONOR OF [BLANK] WIFE OF [BLANK] PRESIDENT OF CARLETON COLLEGE 1909 TO 1945\\\"\"\n" +
                    "                       },\n" +
                    "\t\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\t\"text\" : \"A gymnasium for Carleton women was identified as a pressing need as early as the 1909 inaugural address of President Donald J. Cowling, but this remained for decades a dream deferred, until the Elizabeth Stehman Cowling Recreation Center for Women (named for President Cowling's wife) was at last dedicated in 1965. The fourth of five campus buildings designed by architect Minoru Yamasaki, the Cowling Rec Center helped make possible a subsequent explosion of growth in women's athletics at Carleton. The building, as with all Carleton athletic and recreational facilities, has now long been utilized by both sexes.\"\n" +
                    "\t\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "\t\t\t\t{\n" +
                    "                    \"lat\":44.4623,\n" +
                    "                    \"lng\":-93.15304,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"\\\"SUCCISA VIRESCIT. RAYMOND JACOBSON SCULPTOR/DESIGNER\\\"\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"That inscription begins;  \\\"[BLANK] GARDEN INCORPORATES THE CARLETON CENTENNIAL FOUNTAIN 1966 WITH THE LANDSCAPE ADDITION 1989 MADE POSSIBLE BY FAMILY AND FRIENDS OF JENNIFER BONNER\\\"\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"Added to the campus during Carleton's centennial year of 1966, the phosphorus copper fountain near the entrance to the Boliou Hall art building was designed by Professor of Art Ray Jacobson with the assistance of instrument maker Russell Ferlen. The sculpture was built to rotate imperceptibly at the rate of one complete turn every two weeks. The Jennifer Bonner Memorial Rock Garden was added to the \\\"Boliou Garden\\\" site in 1989 to honor the memory of a Carleton art student, the daughter of Professor Robert Bonner and Carleton Bookstore employee Barbara Bonner, who died in 1988 following a heart transplant.\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "{\n" +
                    "                    \"lat\":44.45933,\n" +
                    "                    \"lng\":-93.14988,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"\\\"JO RYO EN\\\"  [There ought properly to be bar lines over the top of each letter \\\"O\\\" here.]\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"That inscription translates as: \\\"The Garden of Quiet Listening\\\"\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"A Japanese garden \\\"embodies a philosophy, at the heart of which is fulfillment found in harmony and tranquillity of body, mind and spirit.\\\" The driving force behind the creation of Carleton's dry-landscape Japanese garden was Professor of Asian Studies Bardwell Smith, who, acting upon the advice of a visiting professor from Kyoto, commissioned American landscape architect David Slawson to design and realize the project between 1974 and 1976. Slawson had been a student of Kyoto master Kinsaku Nakane. Incorporated into the design of the garden is the Japanese character translated variously as \\\"heart,\\\" \\\"mind,\\\" or \\\"inner self.\\\"\"\n" +
                    "\t\t\t\t}\n" +
                    "                },\n" +
                    "\n" +
                    "\n" +
                    "{\n" +
                    "                    \"lat\":44.46336,\n" +
                    "                    \"lng\":-93.14726,\n" +
                    "                    \"rad\":5000,\n" +
                    "                    \"clue\": {\n" +
                    "                        \"text\": \"\\\"ON THE OCCASION OF THE 25TH AND 15TH ANNIVERSARIES OF THE FOUNDING OF THE MEN'S AND WOMEN'S CARLETON RUGBY FOOTBALL CLUBS, MAY 15, 1993. PLAY ON.\\\"\"\n" +
                    "                    },\n" +
                    "                    \"hint\": {\n" +
                    "                        \"text\": \"That inscription begins: \\\"COMMEMORATING THE RESTORATION OF THE THIRD OAK TREE …\\\"\"\n" +
                    "                    },\n" +
                    "\t\t\t\t\"completion\" : {\n" +
                    "\t\t\t\t\t\"text\" : \"Named for a cluster of large oak trees, the Hill of Three Oaks overlooks playing fields associated over the years with Carleton's intercollegiate club rugby, lacrosse, and ultimate frisbee teams, as well as annual marathon games of \\\"Rotblatt\\\" softball. The hill itself has hosted innumerable picnics and dozens of iterations of May Spring Concerts. It was also an early \\\"holy site\\\" for Carleton's Reformed Druids of North America, a student group first formed in 1963. The actual number of oaks on the hill has fluctuated in recent memory due to storm loss and replanting.\"\n" +
                    "\t\t\t\t}\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        }\n" +
                    "\n" +
                    "    ]\n" +
                    "}";
        }

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
            Log.e("NEWGEOFENCES", "readFromFile: File not found: " + e.toString());
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
     * @param events events to add to hashMap
     * @param isNewInfo true if the info was retrieved from a URL, false if the info
     *                  was retrieved from internal app storage
     */
    public void handleNewEvents(ArrayList<EventContent> events, boolean isNewInfo) {

        requestingEvents = false;
        String completeDate;
        String[] completeDateArray;
        String dateByDay;
        if(events == null){
            if(adapter.getCurrentFragment()instanceof EventsFragment){
                ((EventsFragment)adapter.getCurrentFragment()).handleNewEvents(null, null);
            }
        }else {
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
                    // If key already there, add + update new values
                    if (!eventsMapByDate.containsKey(dateByDay)) {
                        eventsMapByDate.put(dateByDay, i);
                    }

                }
            }
            if (adapter.getCurrentFragment() instanceof EventsFragment) {
                ((EventsFragment)adapter.getCurrentFragment()).handleNewEvents(eventsMapByDate, eventContentList);
            }
        }
        if(events.size() != 0 && isNewInfo) {
            lastEventsUpdate = Calendar.getInstance().getTime();
        }
    }

    /**
     *
     * @return eventContentList
     */
    public ArrayList<EventContent> getAllEvents(){
        return this.eventContentList;
    }


    /**
     * Called by DownloadFileFromURL when a file is retrieved. If the
     * retrieval was successful, calls a function to parse the data the was
     * retrieved. If no data was retrieved, passes null to fragment in view
     * @param retrievalSucceeded true if retrieval was successful, false otherwise
     */
    @Override
    public void retrievedFile(boolean retrievalSucceeded, String fileNameWithExtension) {
        if(retrievalSucceeded){
            if(fileNameWithExtension.equals(Constants.ICAL_FILE_NAME_WITH_EXTENSION)) {
                parseIcalFeed(fileNameWithExtension, true);
            }if(fileNameWithExtension.equals(Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION)){
                parseGeofences(fileNameWithExtension, true);
            }if(fileNameWithExtension.equals(Constants.QUESTS_FILE_NAME_WITH_EXTENSION)){
                parseQuests(fileNameWithExtension, true);
            }
        }else{
            if(fileNameWithExtension.equals(Constants.ICAL_FILE_NAME_WITH_EXTENSION)) {
                handleNewEvents(null, false);
                requestingEvents = false;
            }if(fileNameWithExtension.equals(Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION)){
                handleGeofences(null, false);
                requestingAllGeofencesNew = false;
            }if(fileNameWithExtension.equals(Constants.QUESTS_FILE_NAME_WITH_EXTENSION)){
                handleNewQuests(null, false);
                requestingQuests = false;
            }
        }
    }

    /**
     *
     * @return the quest the user is currently working on
     */
    public Quest getQuestInProgress() {
        return questInProgress;
    }

    /**
     *
     * @param questInProgress quest user is currently working on
     */
    public void setQuestInProgress(Quest questInProgress) {
        this.questInProgress = questInProgress;
    }

    /**
     * Overridden lifecycle method to stop location updates when the activity is paused
     */
    @Override
    protected void onPause() {
        stopLocationUpdates();
        super.onPause();
    }
}
