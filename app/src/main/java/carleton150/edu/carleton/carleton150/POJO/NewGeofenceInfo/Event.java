package carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Event {

    @SerializedName("media")
    @Expose
    private Media_ media;
    @SerializedName("start_date")
    @Expose
    private StartDate startDate;
    @SerializedName("geo")
    @Expose
    private Geo_ geo;
    @SerializedName("text")
    @Expose
    private Text_ text;

    private boolean isExpanded;

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setIsExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
    }

    /**
     *
     * @return
     * The media
     */
    public Media_ getMedia() {
        return media;
    }

    /**
     *
     * @param media
     * The media
     */
    public void setMedia(Media_ media) {
        this.media = media;
    }

    /**
     *
     * @return
     * The startDate
     */
    public StartDate getStartDate() {
        return startDate;
    }

    /**
     *
     * @param startDate
     * The start_date
     */
    public void setStartDate(StartDate startDate) {
        this.startDate = startDate;
    }

    /**
     *
     * @return
     * The geo
     */
    public Geo_ getGeo() {
        return geo;
    }

    /**
     *
     * @param geo
     * The geo
     */
    public void setGeo(Geo_ geo) {
        this.geo = geo;
    }

    /**
     *
     * @return
     * The text
     */
    public Text_ getText() {
        return text;
    }

    /**
     *
     * @param text
     * The text
     */
    public void setText(Text_ text) {
        this.text = text;
    }

}
