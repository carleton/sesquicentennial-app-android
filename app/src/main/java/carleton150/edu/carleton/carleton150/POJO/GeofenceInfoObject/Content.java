package carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject;

/**
 * Created by haleyhinze on 11/9/15.
 */
public class Content
{
    private String summary;

    private String _id;

    private String data;

    private String[] geofences;

    private String type;

    public String getSummary ()
    {
        return summary;
    }

    public void setSummary (String summary)
    {
        this.summary = summary;
    }

    public String get_id ()
    {
        return _id;
    }

    public void set_id (String _id)
    {
        this._id = _id;
    }

    public String getData ()
    {
        return data;
    }

    public void setData (String data)
    {
        this.data = data;
    }

    public String[] getGeofences ()
    {
        return geofences;
    }

    public void setGeofences (String[] geofences)
    {
        this.geofences = geofences;
    }

    public String getType ()
    {
        return type;
    }

    public void setType (String type)
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        try {
            return "Content [summary = " + summary + ", _id = " + _id + ", data = " + data + ", geofences = " + geofences.toString() + ", type = " + type + "]";
        } catch(NullPointerException e){
            e.printStackTrace();
            return "null content";
        }
    }
}