package edu.carleton.app.reunion;

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
import android.support.v4.app.Fragment;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;

import edu.carleton.app.reunion.Adapters.MyFragmentPagerAdapter;
import edu.carleton.app.reunion.ExtraFragments.QuestCompletedFragment;
import edu.carleton.app.reunion.Interfaces.QuestStartedListener;
import edu.carleton.app.reunion.Interfaces.RetrievedFileListener;
import edu.carleton.app.reunion.MainFragments.EventsFragment;
import edu.carleton.app.reunion.MainFragments.HistoryFragment;
import edu.carleton.app.reunion.MainFragments.HomeFragment;
import edu.carleton.app.reunion.MainFragments.MainFragment;
import edu.carleton.app.reunion.MainFragments.QuestFragment;
import edu.carleton.app.reunion.MainFragments.QuestInProgressFragment;
import edu.carleton.app.reunion.Models.DownloadFileFromURL;
import edu.carleton.app.reunion.POJO.EventObject.EventContent;
import edu.carleton.app.reunion.POJO.GeofenceInfo.AllGeofences;
import edu.carleton.app.reunion.POJO.Quests.Quest;

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
    private boolean resumeQuest = false;

    private boolean webViewLoaded = false;

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
        //keeps all pages cached
        viewPager.setOffscreenPageLimit(4);
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                tabLayout.setupWithViewPager(viewPager);
            }
        });

        tabLayout.setOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        Fragment selected = adapter.getItem(tab.getPosition());
                        if (selected instanceof QuestInProgressFragment) {
                            ((QuestInProgressFragment) selected).inView();
                        } else if (selected instanceof QuestCompletedFragment) {
                            ((QuestCompletedFragment) selected).inView();
                        }
                    }
                });


        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));


        tabLayout.setTabsFromPagerAdapter(adapter);

        //Requests information
        if(isConnectedToNetwork()) {
           //requestGeofences();
           //requestEvents();
          // requestQuests();
        }
    }

    public void requestAll(){
        requestGeofences();
        requestEvents();
        requestQuests();
    }

    public void setWebViewLoaded(boolean loaded){
       this.webViewLoaded = loaded;
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

        if(mGoogleApiClient == null){
            buildGoogleApiClient();
        }
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

    public QuestStartedListener getQuestStartedListener(){
        return adapter;
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
        Log.i("GOOGLE API CLIENT", "building...");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        Log.i("GOOGLE API CLIENT", "built");

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

    public void questInProgressGoBack(){
        if(adapter.getCurrentFragment() instanceof QuestInProgressFragment ||
                adapter.getCurrentFragment() instanceof QuestCompletedFragment){
            adapter.backButtonPressed();
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
     * Alerts the user that their High Accuracy GPS is not enabled and gives them option to enable it
     * This avoids the chance that GPS is on but current location is null
     */
    public void buildAlertMessageNoHighAccuracyGps() {
        needToShowGPSAlert = false;
        noGpsAlertDialogBuilder.setMessage(getString(R.string.feature_requires_fine_gps))
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

            if (isConnectedToNetwork()) {
                if(webViewLoaded) {
                    if (!requestingAllGeofencesNew && lastGeofenceUpdate == null) {
                        DownloadFileFromURL downloadFileFromURLGeofences = new DownloadFileFromURL(this, Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, this);
                        downloadFileFromURLGeofences.execute(Constants.NEW_GEOFENCES_ENDPOINT);
                        requestingAllGeofencesNew = true;
                    } else if (!requestingAllGeofencesNew && lastGeofenceUpdate != null) {
                        long minutesSinceUpdate = checkElapsedTime(lastGeofenceUpdate.getTime());
                        if (minutesSinceUpdate > Constants.MINUTES_BETWEEN_REFRESH) {
                            DownloadFileFromURL downloadFileFromURLGeofences = new DownloadFileFromURL(this, Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, this);
                            downloadFileFromURLGeofences.execute(Constants.NEW_GEOFENCES_ENDPOINT);
                            requestingAllGeofencesNew = true;
                        }
                    }
                }
            } else if (fileExists(Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION)) {
                parseGeofences(Constants.GEOFENCES_FILE_NAME_WITH_EXTENSION, false);
            } else {
                showNetworkNotConnectedDialog();
                if (adapter.getCurrentFragment() instanceof HistoryFragment) {
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
            if(webViewLoaded) {
                if (!requestingEvents && lastEventsUpdate == null) {
                    String url = buildEventsRequestURL();
                    DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, Constants.ICAL_FILE_NAME_WITH_EXTENSION, this);
                    downloadFileFromURL.execute(url);
                    requestingEvents = true;
                } else if (!requestingEvents && lastEventsUpdate != null) {
                    long minutesSinceUpdate = checkElapsedTime(lastEventsUpdate.getTime());
                    if (minutesSinceUpdate > Constants.MINUTES_BETWEEN_REFRESH) {
                        String url = buildEventsRequestURL();
                        DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, Constants.ICAL_FILE_NAME_WITH_EXTENSION, this);
                        downloadFileFromURL.execute(url);
                        requestingEvents = true;
                    }
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
     * @return elapsed time between previousTime and current time in minutes
     */
    private long checkElapsedTime(long previousTime){
        Calendar currentTime = Calendar.getInstance();
        java.util.Date currentDate = currentTime.getTime();
        long time = currentDate.getTime();
        //converting ms to hours
        long minutesSinceUpdate = (time - previousTime) / (1000 * 60);
        return minutesSinceUpdate;
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
            if(webViewLoaded) {
                if (lastQuestUpdate == null && !requestingQuests) {
                    DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, Constants.QUESTS_FILE_NAME_WITH_EXTENSION, this);
                    downloadFileFromURL.execute(Constants.QUESTS_FEED_URL);
                    requestingQuests = true;
                } else if (!requestingQuests && lastQuestUpdate != null) {
                    long minutesSinceLastUpdate = checkElapsedTime(lastQuestUpdate.getTime());
                    if (minutesSinceLastUpdate > Constants.MINUTES_BETWEEN_REFRESH) {
                        DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(this, Constants.QUESTS_FILE_NAME_WITH_EXTENSION, this);
                        downloadFileFromURL.execute(Constants.QUESTS_FEED_URL);
                        requestingQuests = true;
                    }
                }
            }
        }else{
            if(fileExists(Constants.QUESTS_FILE_NAME_WITH_EXTENSION)){

                parseQuests(Constants.QUESTS_FILE_NAME_WITH_EXTENSION, false);
            }else{
                showNetworkNotConnectedDialog();
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

                if(startDate.toString().contains("T")) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    String date = df.format(startDate);
                    eventContent.setStartTime(date);
                }
                else{
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String date = df.format(startDate);
                    eventContent.setStartTime(date);
                }

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
     * @return true if the quest should be resumed, false otherwise
     */
    public boolean getResumed(){
        return resumeQuest;
    }

    /**
     *
     * @param questInProgress quest user is currently working on
     */
    public void setQuestInProgress(Quest questInProgress) {
        this.questInProgress = questInProgress;
    }

    /**
     * Sets boolean to indicate if questInProgress is resumed or started over
     * @param resume
     */
    public void setResume(boolean resume){
        this.resumeQuest = resume;
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
