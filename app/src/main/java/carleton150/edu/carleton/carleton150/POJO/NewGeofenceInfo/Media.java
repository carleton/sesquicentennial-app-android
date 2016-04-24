package carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Media {

    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("caption")
    @Expose
    private String caption;
    @SerializedName("credit")
    @Expose
    private String credit;

    /**
     *
     * @return
     * The url
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     * @param url
     * The url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     *
     * @return
     * The caption
     */
    public String getCaption() {
        return caption;
    }

    /**
     *
     * @param caption
     * The caption
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     *
     * @return
     * The credit
     */
    public String getCredit() {
        return credit;
    }

    /**
     *
     * @param credit
     * The credit
     */
    public void setCredit(String credit) {
        this.credit = credit;
    }

}
