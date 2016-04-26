package carleton150.edu.carleton.carleton150.Models;

/**
 * Created by haleyhinze on 4/25/16.
 */

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import carleton150.edu.carleton.carleton150.Interfaces.RetrievedEventsListener;

/**
 * Background Async Task to download file
 * */
public class DownloadFileFromURL extends AsyncTask<String, String, String> {

    RetrievedEventsListener retrievedEventsListener;

    public DownloadFileFromURL(RetrievedEventsListener retrievedEventsListener){
        this.retrievedEventsListener = retrievedEventsListener;
    }

    /**
     * Downloading file in background thread
     * */
    @Override
    public String doInBackground(String... f_url) {
        int count;
        try {
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : beginning of try block");
            URL url = new URL(f_url[0]);
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : 1");

            URLConnection connection = url.openConnection();
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : 2");

            connection.connect();

            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : 3");

            // download the file
            InputStream input = new BufferedInputStream(connection.getInputStream());
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : 5");


            // Output stream
            OutputStream output = new FileOutputStream(Environment
                    .getExternalStorageDirectory().toString()
                    + "/carleton150Events.ics");
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : 6");


            byte data[] = new byte[1024];
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : 7");


            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : 8");


            while ((count = input.read(data)) != -1) {
                // writing data to file
                output.write(data, 0, count);
            }
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : beginning of try block");


            // flushing output
            output.flush();

            // closing streams
            output.close();
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : beginning of try block");

            input.close();
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : end of try block");


        } catch (Exception e) {
            Log.e(" Error: ", e.toString());
            Log.i("EVENTS", "DownloadFileFromURL : doInBackground : catch block : error: " + e.toString());
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        Log.i("EVENTS", "DownloadFileFromURL: onPostExecute : String is : " + s);
        if(retrievedEventsListener != null){
            retrievedEventsListener.retrievedEvents(true);
        }
        super.onPostExecute(s);
    }

    @Override
    protected void onCancelled(String s) {
        if(retrievedEventsListener != null){
            retrievedEventsListener.retrievedEvents(false);
        }
        super.onCancelled(s);
    }
}