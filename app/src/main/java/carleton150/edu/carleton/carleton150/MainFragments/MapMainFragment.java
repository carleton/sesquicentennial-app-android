package carleton150.edu.carleton.carleton150.MainFragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.R;

/**
 * MainFragment that contains a map. Extended by HistoryFragment and QuestInProgressFragment
 */
public class MapMainFragment extends MainFragment {


    protected boolean zoomCamera = true;
    protected boolean zoomToUserLocation = true;
    public GoogleMap mMap; // Might be null if Google Play services APK is not available.


    //TileProvider for Carleton map tiling
    public TileProvider baseTileProvider = new UrlTileProvider(Constants.PROVIDER_NUMBER, Constants.PROVIDER_NUMBER) {
        @Override
        public URL getTileUrl(int x, int y, int zoom) {

         /* Define the URL pattern for the tile images */
            String s = String.format(Constants.BASE_URL_STRING,
                    zoom, x, y);
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }
        }
    };


    //TileProvider for Carleton label tiling
    public TileProvider labelTileProvider = new UrlTileProvider(Constants.PROVIDER_NUMBER, Constants.PROVIDER_NUMBER) {
        @Override
        public URL getTileUrl(int x, int y, int zoom) {

        /* Define the URL pattern for the tile images */
            String s = String.format(Constants.LABEL_URL_STRING,
                    zoom, x, y);
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }
        }
    };


    /***** Sets up the map if it is possible to do so *****/
    public boolean setUpMapIfNeeded() {
        MainActivity mainActivity = (MainActivity) getActivity();
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.my_map);
            if(mapFragment != null){
                mMap = mapFragment.getMap();
            }

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
                return true;
            } else {
                mainActivity.showAlertDialog(getResources().getString(R.string.unable_to_set_up_map),
                        new AlertDialog.Builder(mainActivity).create());
                return false;
            }
        }
        return true;
    }



    /**
     * Sets up the map (should only be called if mMap is null)
     */
    protected void setUpMap() {
        MainActivity mainActivity = (MainActivity) getActivity();
        final android.location.Location location = mainActivity.getLastLocation();
        setCamera(location, zoomToUserLocation);
    }


    /**
     * Sets the camera for the map.
     * The initial camera target is the center of campus.
     */
    protected void setCamera(android.location.Location location, boolean zoomOnUserLocation){

        if (location != null && zoomCamera && zoomOnUserLocation) {
            zoomCamera = false;
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                    .zoom(Constants.DEFAULT_ZOOM)
                    .bearing(Constants.DEFAULT_BEARING)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        else if(zoomCamera) {
            zoomCamera = false;
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(Constants.CENTER_CAMPUS.latitude, Constants.CENTER_CAMPUS.longitude))
                    .zoom(Constants.DEFAULT_ZOOM)
                    .bearing(Constants.DEFAULT_BEARING)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    /**
     * draws map tiling
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        drawTiles();

    }

    /**
     * Draws map tiling for campus
     */
    public void drawTiles(){
        if (mMap != null) {
            setUpMap();
        }
        setUpMapIfNeeded();
        if (mMap != null) {
            TileOverlay baseTileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                    .tileProvider(baseTileProvider));
            baseTileOverlay.setZIndex(0);
            TileOverlay labelTileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                    .tileProvider(labelTileProvider));
            labelTileOverlay.setZIndex(2);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMap = null;
    }
}
