package carleton150.edu.carleton.carleton150.MainFragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.Map;

import carleton150.edu.carleton.carleton150.Adapters.HistoryAdapter;
import carleton150.edu.carleton.carleton150.Adapters.MyScaleInAnimationAdapter;
import carleton150.edu.carleton.carleton150.Adapters.OffCampusViewAdapter;
import carleton150.edu.carleton.carleton150.Interfaces.RecyclerViewClickListener;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoObject;
import carleton150.edu.carleton.carleton150.R;

/**
 * Created by haleyhinze on 4/4/16.
 */
public class OffCampusHistoryFragment extends Fragment implements RecyclerViewClickListener {

    public ArrayList<GeofenceInfoContent[]> allGeofenceInfo = new ArrayList<>();
    public ArrayList<GeofenceInfoContent> recyclerViewGeofenceInfo = new ArrayList<>();
    private View view;
    private OffCampusViewAdapter viewAdapter = null;

    public OffCampusHistoryFragment(){
       //required empty public constructor
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
                allGeofenceInfo.add(e.getValue());
                boolean isImage = false;
                GeofenceInfoContent firstImageContent = null;
                int i = 0;
                while (!isImage && i<e.getValue().length) {
                    firstImageContent = e.getValue()[i];
                    i++;
                    if (firstImageContent.getType().equals(firstImageContent.TYPE_IMAGE)) {
                        isImage = true;
                    }
                }
                if (firstImageContent != null) {
                    recyclerViewGeofenceInfo.add(firstImageContent);
                } else {
                    recyclerViewGeofenceInfo.add(e.getValue()[0]);
                }
                if(viewAdapter != null){
                    viewAdapter.notifyDataSetChanged();
                }

            }
        }
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
        //TODO: show popover
    }
}
