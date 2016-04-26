package carleton150.edu.carleton.carleton150.Models;

import android.os.Environment;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.ValidationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.ExtraFragments.AddMemoryFragment;
import carleton150.edu.carleton.carleton150.ExtraFragments.RecyclerViewPopoverFragment;
import carleton150.edu.carleton.carleton150.LogMessages;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.MainFragments.MainFragment;
import carleton150.edu.carleton.carleton150.MyApplication;
import carleton150.edu.carleton.carleton150.POJO.EventObject.Events;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoObject;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.MemoriesContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoRequestObject.GeofenceInfoRequestObject;
import carleton150.edu.carleton.carleton150.POJO.GeofenceObject.GeofenceObject;
import carleton150.edu.carleton.carleton150.POJO.GeofenceObject.GeofenceObjectContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceRequestObject.Geofence;
import carleton150.edu.carleton.carleton150.POJO.GeofenceRequestObject.GeofenceRequestObject;
import carleton150.edu.carleton.carleton150.POJO.GeofenceRequestObject.Location;
import carleton150.edu.carleton.carleton150.POJO.NewGeofenceInfo.AllGeofences;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;

/**
 * Created by haleyhinze on 10/28/15.
 *
 * Class to make server requests
 */
public class VolleyRequester {

    private LogMessages logMessages = new LogMessages();
    private Constants constants = new Constants();

    public VolleyRequester(){
    }



    /**
     * Requests Quests from server
     * @param callerActivity the activity that is to be notified on the result
     */
    public void requestQuests(final MainActivity callerActivity){
        final Gson gson = new Gson();
        JSONObject emptyRequest = new JSONObject();
        JsonObjectRequest request = new JsonObjectRequest(constants.QUESTS_ENDPOINT, emptyRequest,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        String responseString = response.toString();
                        ArrayList<Quest> quests = new ArrayList<>();
                        try {
                            JSONArray responseArr = response.getJSONArray("content");
                            createFile(constants.QUESTS_FILE_NAME_WITH_EXTENSION, responseArr.toString());
                            Log.i(logMessages.VOLLEY, "requestQuests : length of responseArr is: " + responseArr.length());
                            for (int i = 0; i < responseArr.length(); i++) {
                                try {
                                    Quest responseQuest = gson.fromJson(responseArr.getString(i), Quest.class);
                                    Log.i(logMessages.VOLLEY, "requestQuests : quest response string = : " + responseArr.getString(i));
                                    quests.add(responseQuest);
                                }
                                catch (Exception e) {
                                    Log.i(logMessages.VOLLEY, "requestQuests : quest response string = : " + responseArr.getString(i));
                                    Log.i(logMessages.VOLLEY, "requestQuests : unable to parse result");
                                    e.getMessage();
                                    e.printStackTrace();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.i(logMessages.VOLLEY, "requestQuests : response string = : " + responseString);
                        Log.i(logMessages.VOLLEY, "requestQuests : length of quests is: " + quests.size());
                        callerActivity.handleNewQuests(quests);
                    }
                },

                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(logMessages.VOLLEY, "requestQuests : error : " + error.toString());
                        if(callerActivity!=null) {
                            callerActivity.handleNewQuests(null);
                        }
                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        MyApplication.getInstance().getRequestQueue().add(request);
    }


    /**
     * Method to request new geofences to monitor. When it receives the new geofences,
     * calls a method in the mainActivity to handle the new geofences
     *
     * @param latitude user's latitude
     * @param longitude user's longitude
     * @param callerFragment
     */
    public void requestMemories(double latitude, double longitude, double radius, final RecyclerViewPopoverFragment callerFragment) {
        final Gson gson = new Gson();
        //Creates request object
        JSONObject memoriesRequest = new JSONObject();

        try {
//            memoriesRequest.put("lat", 44.461319);
//            memoriesRequest.put("lng", -93.156094);
//            memoriesRequest.put("rad", 0.1);
            memoriesRequest.put("lat", latitude);
            memoriesRequest.put("lng", longitude);
            memoriesRequest.put("rad", radius);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(logMessages.MEMORY_MONITORING, "requestMemories: JSON request : " + memoriesRequest);
        JsonObjectRequest request = new JsonObjectRequest(constants.MEMORIES_ENDPOINT, memoriesRequest,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        String responseString = response.toString();
                        Log.i(logMessages.VOLLEY, "requestMemories : response string = : " + responseString);
                        try {
                            MemoriesContent responseObject = gson.fromJson(responseString, MemoriesContent.class);
                            callerFragment.handleNewMemories(responseObject);
                        }catch (Exception e){
                            Log.i(logMessages.VOLLEY, "requestMemories : unable to parse result");
                        }
                    }
                },

                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(logMessages.VOLLEY, "requestMemories : error : " + error.toString());
                        if(callerFragment!=null) {
                            callerFragment.handleNewMemories(null);
                        }
                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MyApplication.getInstance().getRequestQueue().add(request);
    }

    public void addMemory(String image, String title, String uploader, String desc, String timestamp, double lat, double lng, final AddMemoryFragment callerFragment){
        final Gson gson = new Gson();
        //Creates request object
        JSONObject addMemoryRequest = new JSONObject();
        JSONObject location = new JSONObject();




        try {
            addMemoryRequest.put("title", title);
            addMemoryRequest.put("desc", desc);
            addMemoryRequest.put("timestamp", timestamp);
            addMemoryRequest.put("uploader", uploader);
            location.put("lat", lat);
            location.put("lng", lng);
            addMemoryRequest.put("location", location);
            addMemoryRequest.put("image", image);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(logMessages.MEMORY_MONITORING, "addMemory: JsonObject is: " + addMemoryRequest.toString());

        //createFile("memoryRequest1", addMemoryRequest.toString());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, constants.ADD_MEMORY_ENDPOINT, addMemoryRequest,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        String responseString = response.toString();
                        callerFragment.addMemorySuccess();
                        Log.i(logMessages.MEMORY_MONITORING, "addMemory : response string = : " + responseString);
                    }
                },

                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callerFragment.addMemoryError();
                        Log.i(logMessages.MEMORY_MONITORING, "addMemory : error : " + error.toString());

                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MyApplication.getInstance().getRequestQueue().add(request);
    }

    public void createFile(String fileNameWithExtension, String sBody){
        FileOutputStream fop = null;
        File file;

        try {

            file = new File(Environment.getExternalStorageDirectory().toString() + "/" + fileNameWithExtension);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // get the content in bytes
            byte[] contentInBytes = sBody.getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

            System.out.println("Done");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}



