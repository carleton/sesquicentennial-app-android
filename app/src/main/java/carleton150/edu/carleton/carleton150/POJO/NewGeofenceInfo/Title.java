package carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Title {

    @SerializedName("display_date")
    @Expose
    private String displayDate;
    @SerializedName("media")
    @Expose
    private Media media;
    @SerializedName("geo")
    @Expose
    private Geo geo;
    @SerializedName("text")
    @Expose
    private Text text;

    /**
     *
     * @return
     * The displayDate
     */
    public String getDisplayDate() {
        return displayDate;
    }

    /**
     *
     * @param displayDate
     * The display_date
     */
    public void setDisplayDate(String displayDate) {
        this.displayDate = displayDate;
    }

    /**
     *
     * @return
     * The media
     */
    public Media getMedia() {
        return media;
    }

    /**
     *
     * @param media
     * The media
     */
    public void setMedia(Media media) {
        this.media = media;
    }

    /**
     *
     * @return
     * The geo
     */
    public Geo getGeo() {
        return geo;
    }

    /**
     *
     * @param geo
     * The geo
     */
    public void setGeo(Geo geo) {
        this.geo = geo;
    }

    /**
     *
     * @return
     * The text
     */
    public Text getText() {
        return text;
    }

    /**
     *
     * @param text
     * The text
     */
    public void setText(Text text) {
        this.text = text;
    }

}