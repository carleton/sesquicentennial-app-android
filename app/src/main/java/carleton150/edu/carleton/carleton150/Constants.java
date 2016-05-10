package carleton150.edu.carleton.carleton150;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by haleyhinze on 3/3/16.
 */
public class Constants {

    //Constants to control frequency and accuracy of location updates
    public final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    // Location updates intervals in milliseconds
    public static int UPDATE_INTERVAL = 5000; // 5 sec
    public static int FASTEST_INTERVAL = 1000; // 1 sec
    public static int DISPLACEMENT = 5; // 5 meters

    //To see whether the user has already explored features so we can show tutorial if they haven't
    public static final String IS_FIRST_HISTORY_RUN_STRING = "isFirstHistoryRun";
    public static final String IS_FIRST_QUEST_RUN_STRING = "isFirstQuestRun";

    //To see if the quest was already started and if so how far the user is in it
    public final static String QUEST_PREFERENCES_KEY = "QuestPreferences";

    //Coordinates for setting map zoom and not allowing user to scroll too far off campus
    public static final LatLng CENTER_CAMPUS = new LatLng(44.460421, -93.152749);

    public static final double MAX_LONGITUDE = -93.141134;
    public static final double MIN_LONGITUDE = -93.161333;
    public static final double MAX_LATITUDE = 44.488045;
    public static final double MIN_LATITUDE = 44.458869;
    public static final int PROVIDER_NUMBER = 256;
    //Default zoom and bearing for camera on map
    public static final int DEFAULT_ZOOM = 15;
    public static final int DEFAULT_BEARING = 0;
    public static final int DEFAULT_MAX_ZOOM = 13;

    //Used in MyScaleInAnimationAdapter to set the size the image is scaled from
    public static final float DEFAULT_SCALE_FROM = .5f;


    public static final String NEW_GEOFENCES_ENDPOINT = "https://go.carleton.edu/apphistory";
    public static final String QUESTS_FEED_URL = "https://go.carleton.edu/appquests";

    //TODO: URL for actual calendar : https://go.carleton.edu/appevents
    public static final String ICAL_FEED_URL = "https://apps.carleton.edu/calendar";
    public static final String ICAL_FEED_DATE_REQUEST = "/?start_date=";
    public static final String ICAL_FEED_FORMAT_REQUEST = "&format=ical";
    public static final String INFO_URL = "https://go.carleton.edu/appinfo";
    public static final String USER_AGENT_STRING = "CarletonSesquicentennialApp 1.0";

    public static final String ICAL_FILE_NAME_WITH_EXTENSION = "carleton150Events.ics";
    public static final String GEOFENCES_FILE_NAME_WITH_EXTENSION = "carleton150Geofences.txt";
    public static final String QUESTS_FILE_NAME_WITH_EXTENSION = "carleton150Quests.txt";

    //urls for map tiling
    public static final String BASE_URL_STRING = " https://www.carleton.edu/global_stock/images/campus_map/tiles/base/%d_%d_%d.png";
    public static final String LABEL_URL_STRING = " https://www.carleton.edu/global_stock/images/campus_map/tiles/labels/%d_%d_%d.png";

}
