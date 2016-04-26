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

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.Interfaces.RetrievedFileListener;

/**
 * Background Async Task to download file
 * */
public class DownloadFileFromURL extends AsyncTask<String, String, String> {

    RetrievedFileListener retrievedFileListener;
    String fileNameWithExtension;

    public DownloadFileFromURL(RetrievedFileListener retrievedFileListener, String fileNameWithExtension){
        this.retrievedFileListener = retrievedFileListener;
        this.fileNameWithExtension = fileNameWithExtension;
    }

    /**
     * Downloading file in background thread
     * */
    @Override
    public String doInBackground(String... f_url) {
        int count;
        try {
            URL url = new URL(f_url[0]);
            URLConnection connection = url.openConnection();
            connection.connect();

            // download the file
            InputStream input = new BufferedInputStream(connection.getInputStream());
            Constants constants = new Constants();
            // Output stream
            OutputStream output = new FileOutputStream(Environment
                    .getExternalStorageDirectory().toString()
                    + "/" +fileNameWithExtension);

            byte data[] = new byte[1024];

            while ((count = input.read(data)) != -1) {
                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();
            // closing streams
            output.close();
            input.close();


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
        if(retrievedFileListener != null){
            retrievedFileListener.retrievedFile(true, fileNameWithExtension);
        }
        super.onPostExecute(s);
    }

    @Override
    protected void onCancelled(String s) {
        if(retrievedFileListener != null){
            retrievedFileListener.retrievedFile(false, fileNameWithExtension);
        }
        super.onCancelled(s);
    }
}