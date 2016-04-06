package carleton150.edu.carleton.carleton150.MainFragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import carleton150.edu.carleton.carleton150.Adapters.HistoryAdapter;
import carleton150.edu.carleton.carleton150.Adapters.MyScaleInAnimationAdapter;
import carleton150.edu.carleton.carleton150.Adapters.OffCampusViewAdapter;
import carleton150.edu.carleton.carleton150.ExtraFragments.RecyclerViewPopoverFragment;
import carleton150.edu.carleton.carleton150.Interfaces.OffCampusViewListener;
import carleton150.edu.carleton.carleton150.Interfaces.RecyclerViewClickListener;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoObject;
import carleton150.edu.carleton.carleton150.R;

/**
 * Created by haleyhinze on 4/4/16.
 */
public class OffCampusHistoryFragment extends Fragment implements RecyclerViewClickListener {

    public ArrayList<GeofenceInfoContent[]> allGeofenceInfo = new ArrayList<>();
    public HashMap<String, GeofenceInfoContent[]> allGeofenceInfoMap = new HashMap<>();
    public ArrayList<GeofenceInfoContent> recyclerViewGeofenceInfo = new ArrayList<>();
    private View view;
    private OffCampusViewAdapter viewAdapter = null;
    private OffCampusViewListener offCampusViewListener;

    public OffCampusHistoryFragment(){
       //required empty public constructor
    }

    public void initialize(OffCampusViewListener offCampusViewListener){
        this.offCampusViewListener = offCampusViewListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_off_campus, container, false);
        buildRecyclerViews();
        return view;
    }

    public void setGeofenceInfoContent(GeofenceInfoObject geofenceInfoObject){
        if(geofenceInfoObject != null) {
            for (Map.Entry<String, GeofenceInfoContent[]> e : geofenceInfoObject.getContent().entrySet()) {
                /*String title = "";
                int i = 0;
                while(title.equals("") && i<e.getValue().length){
                    if(!e.getValue()[i].getName().equals("")){
                        title = e.getValue()[i].getName();
                    }
                }*/
                String title = e.getKey();
                if(!itemAlreadyAdded(title)) {
                    allGeofenceInfo.add(e.getValue());
                    allGeofenceInfoMap.put(title, e.getValue());

                    boolean isImage = false;
                    GeofenceInfoContent firstImageContent = null;
                    int i = 0;
                    while (!isImage && i < e.getValue().length) {
                        firstImageContent = e.getValue()[i];
                        i++;
                        if (firstImageContent.getType().equals(firstImageContent.TYPE_IMAGE)) {
                            isImage = true;
                        }
                    }
                    firstImageContent.setName(title);
                    if (firstImageContent != null) {
                        recyclerViewGeofenceInfo.add(firstImageContent);
                    } else {
                        e.getValue()[0].setName(title);
                        recyclerViewGeofenceInfo.add(e.getValue()[0]);
                    }
                    if (viewAdapter != null) {
                        viewAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    private boolean itemAlreadyAdded(String title){
        if(allGeofenceInfoMap.containsKey(title)){
            return true;
        }
        return false;
        /*if(title != null) {
            for (int i = 0; i < allGeofenceInfo.size(); i++) {
                if(allGeofenceInfo.get(i)[0].getName() != null) {
                    if (allGeofenceInfo.get(i)[0].getName().equals(title)) {
                        return true;
                    }
                }
            }
        }
        return false;*/
    }


    /**
     * Determines what the fragment is being used for and builds the appropriate RecyclerView
     */
    private void buildRecyclerViews(){
        RecyclerView poiRecyclerView = (RecyclerView) view.findViewById(R.id.lst_poi);
        LinearLayoutManager poiLayoutManager = new LinearLayoutManager(getActivity());
        poiLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        poiRecyclerView.setLayoutManager(poiLayoutManager);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;


        viewAdapter = new OffCampusViewAdapter(recyclerViewGeofenceInfo, screenWidth, screenHeight, this);
        poiRecyclerView.setAdapter(viewAdapter);
    }


    @Override
    public void recyclerViewListClicked(View v, int position) {
        GeofenceInfoContent geofenceClicked = recyclerViewGeofenceInfo.get(position);
        GeofenceInfoContent[] infoForGeofenceClicked = allGeofenceInfoMap.get(geofenceClicked.getName());
        offCampusViewListener.geofenceClicked(infoForGeofenceClicked, geofenceClicked.getName());
    }
}
