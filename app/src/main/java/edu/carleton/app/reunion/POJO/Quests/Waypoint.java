package edu.carleton.app.reunion.POJO.Quests;

/**
 * Created by haleyhinze on 1/19/16.
 */
public class Waypoint {

    private String lat;
    private String lng;
    private String rad;


    private Clue clue;

    private Hint hint;

    private Completion completion;


    public Clue getClue ()
    {
        return clue;
    }

    public void setClue (Clue clue)
    {
        this.clue = clue;
    }

    public Hint getHint ()
    {
        return hint;
    }

    public void setHint (Hint hint)
    {
        this.hint = hint;
    }



    public Completion getCompletion() {
        return completion;
    }

    public void setCompletion(Completion completion) {
        this.completion = completion;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getRad() {
        return rad;
    }

    public void setRad(String rad) {
        this.rad = rad;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [geofence = "+lat + lng+", clue = "+clue.getText()+", hint = "+hint.getText()+"]";
    }
}
