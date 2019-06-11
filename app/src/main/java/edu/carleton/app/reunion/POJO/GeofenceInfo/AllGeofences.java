package edu.carleton.app.reunion.POJO.GeofenceInfo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * Created by haleyhinze on 4/20/16.
 */

public class AllGeofences {

    @SerializedName("title")
    @Expose
    private Title title;
    @SerializedName("events")
    @Expose
    private Event[] events;

    /**
     *
     * @return
     * The title
     */
    public Title getTitle() {
        return title;
    }

    /**
     *
     * @param title
     * The title
     */
    public void setTitle(Title title) {
        this.title = title;
    }

    /**
     *
     * @return
     * The events
     */
    public Event[] getEvents() {
        return events;
    }

    /**
     *
     * @param events
     * The events
     */
    public void setEvents(Event[] events) {
        this.events = events;
    }

}