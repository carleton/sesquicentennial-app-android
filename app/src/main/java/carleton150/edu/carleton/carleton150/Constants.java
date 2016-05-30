package carleton150.edu.carleton.carleton150;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by haleyhinze on 3/3/16.
 * File that contains constants for the app
 */
public class Constants {

    //Constants to control frequency and accuracy of location updates
    public final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    // Location updates intervals in milliseconds
    public static int UPDATE_INTERVAL = 3000; // 3 sec
    public static int FASTEST_INTERVAL = 1000; // 1 sec
    public static int DISPLACEMENT = 3; // 3 meters

    public static int GOOGLE_PLAY_CONNECTION_RETRY_DELAY = 1000; //1 sec

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

    //Used in MyScaleInAnimationAdapter to set the size the image is scaled from
    public static final float DEFAULT_SCALE_FROM = .5f;


    public static final String NEW_GEOFENCES_ENDPOINT = "https://go.carleton.edu/apphistory";
    public static final String QUESTS_FEED_URL = "https://go.carleton.edu/appquests";

    //TODO: URL for events calendar : https://apps.carleton.edu/calendar
    public static final String ICAL_FEED_URL = "https://go.carleton.edu/appevents";
    public static final String ICAL_FEED_DATE_REQUEST = "/?start_date=";
    public static final String ICAL_FEED_FORMAT_REQUEST = "&format=ical";
    public static final String INFO_URL = "http://go.carleton.edu/appinfo";
    public static final String USER_AGENT_STRING = "CarletonSesquicentennialApp 1.0";

    public static final String ICAL_FILE_NAME_WITH_EXTENSION = "carleton150Events.ics";
    public static final String GEOFENCES_FILE_NAME_WITH_EXTENSION = "carleton150Geofences.txt";
    public static final String QUESTS_FILE_NAME_WITH_EXTENSION = "carleton150Quests.txt";

    //urls for map tiling
    public static final String BASE_URL_STRING = " https://www.carleton.edu/global_stock/images/campus_map/tiles/base/%d_%d_%d.png";
    public static final String LABEL_URL_STRING = " https://www.carleton.edu/global_stock/images/campus_map/tiles/labels/%d_%d_%d.png";

    public static final int FLIP_ANIMATION_DURATION = 300;

    public static final String NO_INTERNET_HTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "<head>\n" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
            "<title>No Connection</title>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, minimum-scale=1.0, maximum-scale=1.0\" />\n" +
            "<style>\n" +
            "body {\n" +
            "\tbackground:#fff;\n" +
            "\tcolor:#222;\n" +
            "\tpadding:0;\n" +
            "\tmargin:0;\n" +
            "\ttext-align:center;\n" +
            "}\n" +
            ".wrap {\n" +
            "\tposition: fixed;\n" +
            "\twidth:75%;\n" +
            "\ttop: 50%;\n" +
            "\tleft: 50%;\n" +
            "\t-webkit-transform: translate(-50%, -50%);\n" +
            "\ttransform: translate(-50%, -50%);\n" +
            "\tfont-family: Roboto, \"San Francisco\", \"Helvetica Neue\", Helvetica, sans-serif;\n" +
            "}\n" +
            "h1 {\n" +
            "\topacity:0.5;\n" +
            "\tline-height:1;\n" +
            "}\n" +
            "body.dark {\n" +
            "\tbackground:#444;\n" +
            "\tcolor:#fff;\n" +
            "}\n" +
            "</style>\n" +
            "</head>\n" +
            "<body class=\"dark\"><!-- change body class to \"light\" for a white background -->\n" +
            "<div class=\"wrap\">\n" +
            "<h1>No Connection</h1>\n" +
            "<p>This part of the app requires an internet connection. Please connect to a wifi or mobile network and try again.</p>\n" +
            "</div>\n" +
            "<body>\n" +
            "</html>";

}
