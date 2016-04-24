package carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class StartDate {

    @SerializedName("year")
    @Expose
    private String year;

    /**
     *
     * @return
     * The year
     */
    public String getYear() {
        return year;
    }

    /**
     *
     * @param year
     * The year
     */
    public void setYear(String year) {
        this.year = year;
    }

}
