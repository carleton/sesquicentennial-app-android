package carleton150.edu.carleton.carleton150.MainFragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.ExtraFragments.QuestCompletedFragment;
import carleton150.edu.carleton.carleton150.ExtraFragments.RecyclerViewPopoverFragment;
import carleton150.edu.carleton.carleton150.Interfaces.QuestStartedListener;
import carleton150.edu.carleton.carleton150.LogMessages;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;
import carleton150.edu.carleton.carleton150.POJO.Quests.Waypoint;
import carleton150.edu.carleton.carleton150.R;

import static carleton150.edu.carleton.carleton150.R.id.rel_layout_found_it_hint;

/**
 * Fragment for when the user is doing a quest. Shows hints, clues, a map, completion messages,
 * and checks for the user reaching the location
 */
public class QuestInProgressFragment extends MapMainFragment {
    private Quest quest = null;
    private int numClue = 0;
    private View cardFace;
    private View cardBack;
    private View v;
    private boolean resume;
    private boolean inView = false;
    private Marker curLocationMarker;
    private boolean needToShowOnCampusDialog = true;
    private boolean locationUpdatesRequested = false;
    private SupportMapFragment mapFragment;
    QuestStartedListener questStartedListener;

    public QuestInProgressFragment() {
        // Required empty public constructor
    }

    /**
     * This must be called after creating the QuestInProgressFragment in order to pass
     * it the current quest and a boolean indicating if the user would like to resume the quest
     * @param quest quest to be completed by user
     */
    public void initialize(Quest quest, boolean resume){
        this.quest = quest;
        this.resume = resume;

    }

    /**
     * Sets a listener to detect when quest is completed or when the user presses
     * the back button to go back to quest selection screen
     * @param questStartedListener
     */
    public void setQuestStartedListener(QuestStartedListener questStartedListener){
        this.questStartedListener = questStartedListener;
    }

    /**
     * Manages UI.
     * Sets OnClickListeners to register when hint button or found it button is clicked
     * and to show the hint or check if the user is within a valid radius of the waypoint
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        needToShowOnCampusDialog = true;
        v = inflater.inflate(R.layout.fragment_quest_in_progress, container, false);
        Button btnFoundIt = (Button) v.findViewById(R.id.btn_found_location);
        Button btnFoundItHint = (Button) v.findViewById(R.id.btn_found_location_hint);
        cardFace = v.findViewById(R.id.clue_view_front);
        cardBack = v.findViewById(R.id.clue_view_back);
        final ImageButton btnReturnToUserLocation = (ImageButton) v.findViewById(R.id.btn_return_to_my_location);
        final ImageButton btnReturnToCampus = (ImageButton) v.findViewById(R.id.btn_return_to_campus);

        final MainActivity mainActivity = (MainActivity) getActivity();
        if(quest == null){
            quest = mainActivity.getQuestInProgress();
        }
        ImageView imgQuestion = (ImageView) v.findViewById(R.id.img_question);
        /*
        Sets listeners to show the progress popover
         */
        imgQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTutorial();
            }
        });
        monitorQuestProgressButtons();
        monitorSlidingDrawers();
        monitorForCardFlips();
        setClueAndHintFields();
        btnFoundIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mainActivity.checkIfGPSEnabled()){
                    mainActivity.buildAlertMessageNoGps();
                }
                checkIfClueFound();

            }
        });
        btnFoundItHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mainActivity.checkIfGPSEnabled()){
                    mainActivity.buildAlertMessageNoGps();
                }
                checkIfClueFound();
            }
        });
        boolean completedQuest = updateCurrentWaypoint();
        if (completedQuest){
            showCompletedQuestMessage();
        }

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

        if(resume){
            resumeQuest();
        }

        return v;
    }

    /**
     * Sets text and images for the clue and hint based on the numClue
     */
    private void setClueAndHintFields(){
        TextView txtHint = (TextView) v.findViewById(R.id.txt_hint);
        SlidingDrawer slidingDrawerClue = (SlidingDrawer) v.findViewById(R.id.front_drawer);
        SlidingDrawer slidingDrawerHint = (SlidingDrawer) v.findViewById(R.id.back_drawer);
        ImageView imgHint = (ImageView) v.findViewById(R.id.img_hint_image_back);
        ImageView imgClue = (ImageView) v.findViewById(R.id.img_clue_image_front);
        final ScrollView scrollViewFront = (ScrollView) v.findViewById(R.id.scrollview_clue_view_front);
        final ScrollView scrollViewBack = (ScrollView) v.findViewById(R.id.scrollview_clue_view_back);
        scrollViewFront.post(new Runnable() {
            public void run() {
                scrollViewFront.fullScroll(View.FOCUS_UP);
            }
        });
        scrollViewBack.post(new Runnable() {
            public void run() {
                scrollViewBack.fullScroll(View.FOCUS_UP);
            }
        });
        Waypoint[] waypoints = quest.getWaypoints();
        if(numClue != waypoints.length) {
            String hint = waypoints[numClue].getHint().getText();
            String image = null;
            String hintImage = null;
            if (waypoints[numClue].getHint().getImage() != null) {
                hintImage = waypoints[numClue].getHint().getImage().getImage();
            }
            if (waypoints[numClue].getClue().getImage() != null) {
                image = waypoints[numClue].getClue().getImage().getImage();
            }
            if (hint == null || hint.equals("")) {
                txtHint.setText(getResources().getString(R.string.no_hint_available));
            } else {
                txtHint.setText(waypoints[numClue].getHint().getText());
            }
            RelativeLayout relLayoutFoundItHint = (RelativeLayout) v.findViewById(rel_layout_found_it_hint);
            RelativeLayout relLayoutFoundItClue = (RelativeLayout) v.findViewById(R.id.rel_layout_found_it_clue);

            //converts dp to pixels
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixelsSmallPadding = (int) (10*scale + 0.5f);
            int dpAsPixelsBigPadding = (int) (80*scale + 0.5f);

            //sets padding so drawer and found it layout don't overlap
            if (image != null) {
                slidingDrawerClue.setVisibility(View.VISIBLE);
                relLayoutFoundItClue.setPadding(0, 0, 0, dpAsPixelsBigPadding);
                setImage(image, imgClue);
            } else {
                slidingDrawerClue.setVisibility(View.GONE);
                relLayoutFoundItClue.setPadding(0, 0, 0, dpAsPixelsSmallPadding);
            }
            if (hintImage != null) {
                slidingDrawerHint.setVisibility(View.VISIBLE);
                relLayoutFoundItHint.setPadding(0, 0, 0, dpAsPixelsBigPadding);
                setImage(hintImage, imgHint);
            } else {
                slidingDrawerHint.setVisibility(View.GONE);
                relLayoutFoundItHint.setPadding(0, 0, 0, dpAsPixelsSmallPadding);
            }
        }
    }

    /**
     * Monitors the clue number views. If one is clicked, shows the quest progress
     * popover
     */
    private void monitorQuestProgressButtons(){
        TextView txtClueNumber = (TextView) v.findViewById(R.id.txt_clue_number);
        TextView txtClueNumberHint = (TextView) v.findViewById(R.id.txt_clue_number_hint);
        TextView txtClueNumberCompMessage= (TextView) v.findViewById(R.id.txt_clue_number_comp_window);
        txtClueNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressPopup();
            }
        });
        txtClueNumberHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressPopup();
            }
        });
        txtClueNumberCompMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressPopup();
            }
        });
    }

    /**
     * Sets listeners for drawer opens and changes the icon for opening or closing the drawer
     */
    private void monitorSlidingDrawers(){
        SlidingDrawer slidingDrawerClue = (SlidingDrawer) v.findViewById(R.id.front_drawer);
        SlidingDrawer slidingDrawerHint = (SlidingDrawer) v.findViewById(R.id.back_drawer);
        final ImageView imgExpandClue = (ImageView) v.findViewById(R.id.img_expand_clue);
        final ImageView imgExpandHint = (ImageView) v.findViewById(R.id.img_expand_hint);
        slidingDrawerClue.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            @Override
            public void onDrawerOpened() {
                imgExpandClue.setImageDrawable(getResources().getDrawable(R.drawable.ic_navigation_expand_more));
            }
        });
        slidingDrawerHint.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            @Override
            public void onDrawerOpened() {
                imgExpandHint.setImageDrawable(getResources().getDrawable(R.drawable.ic_navigation_expand_more));
            }
        });
        slidingDrawerClue.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                imgExpandClue.setImageDrawable(getResources().getDrawable(R.drawable.ic_navigation_expand_less));
            }
        });
        slidingDrawerHint.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                imgExpandHint.setImageDrawable(getResources().getDrawable(R.drawable.ic_navigation_expand_less));
            }
        });
    }

    /**
     * Watches for card flips and displays either the clue or hint
     * depending which was already in view
     */
    private void monitorForCardFlips(){
        Button btnFlipCardToClue = (Button) v.findViewById(R.id.btn_show_clue);
        Button btnFlipCardToHint = (Button) v.findViewById(R.id.btn_show_hint);
        btnFlipCardToHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipCard();
            }
        });

        btnFlipCardToClue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipCard();
            }
        });
    }


    /**
     * replaces the RelativeLayout named my_map with a SupportMapFragment
     *
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.my_map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.my_map, mapFragment).commit();
        }
    }

    /**
     * Lifecycle method overridden to set up the map if it
     * is currently null and to start locatin updates if possible
     */
    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainActivity = (MainActivity) getActivity();
        setUpMapIfNeeded();
        fragmentInView();
        needToShowOnCampusDialog = true;
        if(mainActivity.mLastLocation != null){
            drawLocationMarker(mainActivity.mLastLocation);
        }
        drawTiles();
        if(!locationUpdatesRequested && getUserVisibleHint()) {
            if(mainActivity.checkIfGPSEnabled()){
                mainActivity.startLocationUpdatesIfPossible();
                locationUpdatesRequested = true;
            }if(!mainActivity.checkIfGPSEnabled()){
                mainActivity.buildAlertMessageNoGps();
            }
        }
    }

    /**
     * starts location updates if possible when fragment comes into view.
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        needToShowOnCampusDialog = true;
        if(isVisibleToUser && isResumed()) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if(!locationUpdatesRequested) {
                if(mainActivity.checkIfGPSEnabled()){
                    mainActivity.startLocationUpdatesIfPossible();
                    locationUpdatesRequested = true;
                }if(!mainActivity.checkIfGPSEnabled()){
                    mainActivity.buildAlertMessageNoGps();
                }
            }
        }else if(!isVisibleToUser){
            MainActivity mainActivity = (MainActivity) getActivity();
            if(mainActivity != null && locationUpdatesRequested){
                mainActivity.stopLocationUpdatesIfPossible();
                locationUpdatesRequested = false;
            }
        }
    }

    /**
     * stops location updates if possible when fragment is paused
     */
    @Override
    public void onPause() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity != null && locationUpdatesRequested){
            mainActivity.stopLocationUpdatesIfPossible();
            locationUpdatesRequested = false;
        }
        super.onPause();
    }

    /**
     * Checks if the user's current location is within the radius of the waypoint
     * (both the radius and waypoint are specified in the quest object)
     */
    private void checkIfClueFound(){
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.needToShowGPSAlert = true;
        if(mainActivity.checkIfGPSEnabled()) {
            Location curLocation = mainActivity.mLastLocation;
            if (curLocation != null) {
                Waypoint curWaypoint = quest.getWaypoints()[numClue];
                double lat = Double.parseDouble(curWaypoint.getLat());
                double lon = Double.parseDouble(curWaypoint.getLng());
                double rad = Double.parseDouble(curWaypoint.getRad());
                float[] results = new float[1];

                Location.distanceBetween(curLocation.getLatitude(), curLocation.getLongitude(),
                        lat, lon,
                        results);
                if (results[0] <= rad) {
                    clueCompleted();
                } else {
                    //String to display if hint is not already showing
                    String alertString = getActivity().getResources().getString(R.string.location_not_found_hint);
                    TextView txtHint = (TextView) v.findViewById(R.id.txt_hint);
                    if (txtHint.getVisibility() == View.VISIBLE) {
                        //String to display if hint is already showing
                        alertString = getActivity().getResources().getString(R.string.location_not_found);
                    }
                    mainActivity.showAlertDialog(alertString,
                            new AlertDialog.Builder(mainActivity).create());
                }
            } else {
                Log.i(LogMessages.LOCATION, "QuestInProgressFragment: checkIfClueFound: location is null");
            }
        }else{
            mainActivity.buildAlertMessageNoGps();
        }
    }

    /**
     * Sets up the map
     * Monitors the zoom and target of the camera and changes them
     * if the user zooms out too much or scrolls map too far off campus.
     */
    @Override
    protected void setUpMap() {

        super.setUpMap();
        // to get rid of blue dot showing user's location
        mMap.setMyLocationEnabled(false);
    }


    /**
     * Updates map view to reflect user's new location
     *
     * @param newLocation
     */
    @Override
    public void handleLocationChange(Location newLocation) {
        super.handleLocationChange(newLocation);
        setCamera(newLocation, zoomToUserLocation);
        drawLocationMarker(newLocation);
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity != null) {
            if (mainActivity.mLastLocation != null) {
                setUpMapIfNeeded();
                if(!onCampus() && needToShowOnCampusDialog){
                    mainActivity.showOnCampusFeatureAlertDialogQuestInProgress();
                    needToShowOnCampusDialog = false;
                }
            }
        }
    }

    /**
     * draws a custom location marker for the user's current location
     * @param newLocation
     */
    private void drawLocationMarker(Location newLocation) {
        if(mMap != null) {
            if(curLocationMarker != null) {
                curLocationMarker.remove();
            }
            Bitmap knightIcon = BitmapFactory.decodeResource(getResources(), R.drawable.knight_horse_icon);
            LatLng position = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
            BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(knightIcon);
            curLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(getString(R.string.current_loaction))
                    .icon(icon));
        }else{
            Log.i(LogMessages.LOCATION, "QuestInProgressFragment : drawLocationMarker : mMap is null");
        }
    }



    /**
     * Checks if the quest is finished. If not, sets the text and images to show the next clue
     *
     * @return boolean, true if quest is finished, false otherwise
     */
    public boolean updateCurrentWaypoint(){
        boolean finished = false;
        Waypoint[] waypoints = quest.getWaypoints();
        TextView txtClueNumberCompMessage= (TextView) v.findViewById(R.id.txt_clue_number_comp_window);

        try {
            if(numClue == waypoints.length) {
                finished = true;
                txtClueNumberCompMessage.setText((numClue) + "/" + quest.getWaypoints().length);
                return finished;
            }

            RelativeLayout relLayoutFoundItHint = (RelativeLayout) v.findViewById(rel_layout_found_it_hint);
            RelativeLayout relLayoutFoundItClue = (RelativeLayout) v.findViewById(R.id.rel_layout_found_it_clue);

            //converts dp to pixels
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixelsSmallPadding = (int) (10*scale + 0.5f);
            int dpAsPixelsBigPadding = (int) (80*scale + 0.5f);

            TextView txtClue = (TextView) v.findViewById(R.id.txt_clue);
            TextView txtClueNumber = (TextView) v.findViewById(R.id.txt_clue_number);
            TextView txtClueNumberBack = (TextView) v.findViewById(R.id.txt_clue_number_hint);
            TextView txtHint = (TextView) v.findViewById(R.id.txt_hint);
            SlidingDrawer slidingDrawerClue = (SlidingDrawer) v.findViewById(R.id.front_drawer);
            SlidingDrawer slidingDrawerHint = (SlidingDrawer) v.findViewById(R.id.back_drawer);
            txtClue.setText(waypoints[numClue].getClue().getText());
            txtClueNumber.setText((numClue + 1) + "/" + quest.getWaypoints().length);
            txtClueNumberBack.setText((numClue + 1) + "/" + quest.getWaypoints().length);
            txtClueNumberCompMessage.setText((numClue + 1) + "/" + quest.getWaypoints().length);

            if(txtHint != null || !txtHint.equals("")){
                txtHint.setText(waypoints[numClue].getHint().getText());
            }else{
                txtHint.setText(getResources().getString(R.string.no_hint_available));
            }

            ImageView imgClue = (ImageView) v.findViewById(R.id.img_clue_image_front);
            ImageView imgHint = (ImageView) v.findViewById(R.id.img_hint_image_back);
            String image = null;
            String hintImage = null;
            if(waypoints[numClue].getHint().getImage() != null) {
                hintImage = waypoints[numClue].getHint().getImage().getImage();
            }if(waypoints[numClue].getClue().getImage() != null){
                image = waypoints[numClue].getClue().getImage().getImage();
            }

            //sets padding so sliding drawer and rest of clue or hint view don't overlap
            if (image != null){
                slidingDrawerClue.setVisibility(View.VISIBLE);
                setImage(image, imgClue);
                relLayoutFoundItClue.setPadding(0, 0, 0, dpAsPixelsBigPadding);
            }else{
                slidingDrawerClue.setVisibility(View.GONE);
                relLayoutFoundItClue.setPadding(0, 0, 0, dpAsPixelsSmallPadding);

            }
            if (hintImage != null){
                slidingDrawerHint.setVisibility(View.VISIBLE);
                setImage(hintImage, imgHint);
                relLayoutFoundItHint.setPadding(0, 0, 0, dpAsPixelsBigPadding);

            }else{
                slidingDrawerHint.setVisibility(View.GONE);
                relLayoutFoundItHint.setPadding(0, 0, 0, dpAsPixelsSmallPadding);

            }
            return finished;
        } catch (NullPointerException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handles when a clue has been completed by incrementing the clue
     * number, updating the current waypoint, and checking if the quest is completed
     */
    public void clueCompleted() {

        if(numClue + 1 != quest.getWaypoints().length){
            showClueCompletedMessage();
        }

        numClue += 1;

        //saves the quest progress into SharedPreferences
        MainActivity mainActivity = (MainActivity) getActivity();
        SharedPreferences.Editor sharedPrefsEditor = mainActivity.getPersistentQuestStorage().edit();
        sharedPrefsEditor.putInt(quest.getName(), numClue);
        sharedPrefsEditor.commit();
        boolean completedQuest = updateCurrentWaypoint();
        if (completedQuest){
            showCompletedQuestMessage();
        }



    }

    /**
     * Shows the message stored with the quest when the quest has been
     * completed by showing the QuestCompletedFragment
     */
    private void showCompletedQuestMessage(){

        goToQuestCompletionScreen();
    }

    /**
     * Shows the message stored with the clue when the clue has been
     * completed
     */
    private void showClueCompletedMessage(){
        ImageView imgQuestCompleted = (ImageView) v.findViewById(R.id.img_animation_quest_completed);
        TextView txtQuestCompleted = (TextView) v.findViewById(R.id.txt_completion_message);
        final RelativeLayout relLayoutQuestCompleted = (RelativeLayout) v.findViewById(R.id.rel_layout_quest_completed);
        Button btnDoneWithQuest = (Button) v.findViewById(R.id.btn_done_with_quest);
        txtQuestCompleted.setText(quest.getWaypoints()[numClue].getCompletion().getText());
        txtQuestCompleted.setMovementMethod(new ScrollingMovementMethod());
        txtQuestCompleted.scrollTo(0, 0);

        if(quest.getWaypoints()[numClue].getCompletion().getImage() != null){
            setImage(quest.getWaypoints()[numClue].getCompletion().getImage().getImage(), imgQuestCompleted);
        }else{
            imgQuestCompleted.setVisibility(View.GONE);
        }
        relLayoutQuestCompleted.setVisibility(View.VISIBLE);
        btnDoneWithQuest.setText(getString(R.string.continue_to_next_hint));
        btnDoneWithQuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                relLayoutQuestCompleted.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Called when the fragment comes into view
     *
     * updates the waypoints
     * and sets the map camera if necessary
     */
    public void fragmentInView() {
        updateCurrentWaypoint();
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity.mLastLocation != null){
            drawLocationMarker(mainActivity.mLastLocation);
        }
        if (this.isResumed()) {
            if (!inView) {
                inView = true;
            }
            drawTiles();
        }
        setCamera(mainActivity.getLastLocation(), zoomToUserLocation);
        if(isResumed()) {
            ImageView imgClue = (ImageView) v.findViewById(R.id.img_clue_image_front);
            ImageView imgHint = (ImageView) v.findViewById(R.id.img_hint_image_back);
            Waypoint[] waypoints = quest.getWaypoints();
            String image = null;
            String hintImage = null;
            if(waypoints.length == numClue){
                showCompletedQuestMessage();
                return;
            }
            if (waypoints[numClue].getHint().getImage() != null) {
                hintImage = waypoints[numClue].getHint().getImage().getImage();
                setImage(hintImage, imgHint);
            }
            if (waypoints[numClue].getClue().getImage() != null) {
                image = waypoints[numClue].getClue().getImage().getImage();
                setImage(image, imgClue);
            }
        }

    }

    /**
     * Map should be set to null in onDestroyView(), but then there is an error
     * because the FragmentManager has already called onSaveInstanceState, so variables
     * can no longer be changed. Therefore, it is necessary to make mMap = null before
     * saving the instance state.
     *
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        mMap = null;
        super.onSaveInstanceState(outState);
    }

    /**
     * Starts fragment to show animation and message on quest completion
     */
    private void goToQuestCompletionScreen(){
        System.gc();
        QuestCompletedFragment fr = new QuestCompletedFragment();
        fr.initialize(quest);
        questStartedListener.questCompleted(fr);
    }

    /**
     * flips the clue card to show the hint and vice versa
     */
    private void flipCard()
    {
        if (cardFace.getVisibility() == View.GONE)
        {
            cardBack.setVisibility(View.GONE);
            cardBack.animate().alpha(0f).setDuration(Constants.FLIP_ANIMATION_DURATION);
            cardFace.bringToFront();
            cardFace.setVisibility(View.VISIBLE);
            cardFace.animate().alpha(1f).setDuration(Constants.FLIP_ANIMATION_DURATION);

        }else{
            cardFace.setVisibility(View.GONE);
            cardFace.animate().alpha(0f).setDuration(Constants.FLIP_ANIMATION_DURATION);
            cardBack.bringToFront();
            cardBack.setVisibility(View.VISIBLE);
            cardBack.animate().alpha(1f).setDuration(Constants.FLIP_ANIMATION_DURATION);
        }
    }

    /**
     *displays image from URL in the imageView
     *
     * @param imageURL the URL for the image
     * @param imageView image view to display image
     */
    public void setImage(String imageURL, ImageView imageView) {
        Uri uri = Uri.parse(imageURL);
        Context imgContext = imageView.getContext();
        Picasso.with(imgContext).load(uri).into(imageView);
    }

    /**
     * Resumes quest at stored numClue
     */
    private void resumeQuest(){
        MainActivity mainActivity = (MainActivity) getActivity();
        int curClue = mainActivity.getPersistentQuestStorage().getInt(quest.getName(), 0);
        if(curClue != 0){
            numClue = curClue;
        }
        boolean completedQuest = updateCurrentWaypoint();
        if (completedQuest){
            showCompletedQuestMessage();
        }
    }

    @Override
    public void onDestroyView() {
        cardFace = null;
        cardBack = null;
        v = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        quest = null;
        super.onDestroy();
    }

    /**
     * Shows the tutorial if it is hidden, hides the tutorial if it is showing
     */
    private void toggleTutorial(){
        final RelativeLayout relLayoutTutorial = (RelativeLayout) v.findViewById(R.id.tutorial);
        if(relLayoutTutorial.getVisibility() == View.VISIBLE){
            relLayoutTutorial.setVisibility(View.GONE);
        }else{
            relLayoutTutorial.setVisibility(View.VISIBLE);
        }
        Button btnCloseTutorial = (Button) v.findViewById(R.id.btn_close_tutorial);
        btnCloseTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                relLayoutTutorial.setVisibility(View.GONE);
            }
        });
    }


    /**
     * Shows the history popover for a given marker on the map
     *
     * @param
     */
    private void showProgressPopup(){

        Log.i("QUEST PROGRESS POPUP", "function called");

        RelativeLayout relLayoutTutorial = (RelativeLayout) v.findViewById(R.id.tutorial);
        relLayoutTutorial.setVisibility(View.GONE);

        FragmentManager fm = getActivity().getSupportFragmentManager();
        RecyclerViewPopoverFragment recyclerViewPopoverFragment = RecyclerViewPopoverFragment.newInstance(quest, numClue);

        // Transaction start
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        fragmentTransaction.setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom,
                R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
        fragmentTransaction.add(R.id.fragment_container_quest, recyclerViewPopoverFragment, "QuestProgressPopoverFragment");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        //MainActivity mainActivity = (MainActivity) getActivity();
        //mainActivity.getMyFragmentPagerAdapter().notifyDataSetChanged();
    }

    /**
     * checks whether the user's location is on campus
     * @return true if the user is on campus, false otherwise
     */
    private boolean onCampus(){
        MainActivity mainActivity = (MainActivity) getActivity();
        Location location = mainActivity.getLastLocation();

        if(location == null){
            return true;
        }

        if(location.getLatitude() > Constants.MIN_LATITUDE
                && location.getLatitude() < Constants.MAX_LATITUDE
                && location.getLongitude() > Constants.MIN_LONGITUDE
                && location.getLongitude() < Constants.MAX_LONGITUDE){
            return true;
        }
        return false;

    }
}
