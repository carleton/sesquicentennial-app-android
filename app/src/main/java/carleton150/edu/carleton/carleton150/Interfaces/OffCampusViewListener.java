package carleton150.edu.carleton.carleton150.Interfaces;

import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoContent;

/**
 * Created by haleyhinze on 4/6/16.
 */
public interface OffCampusViewListener {

    public void geofenceClicked(GeofenceInfoContent[] infoForGeofenceClicked, String geofenceName);
}
