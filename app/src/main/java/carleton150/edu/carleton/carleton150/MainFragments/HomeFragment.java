package carleton150.edu.carleton.carleton150.MainFragments;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.xml.sax.XMLReader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.R;

/**
 * Fragment to display a webview that contains information about the app
 */
public class HomeFragment extends MainFragment {

    String curURL;
    URL secondURL = null;
    View v;
    public static WebView myWebView;
    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * opens the WebView
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_info, container, false);
        curURL = Constants.INFO_URL;

        CheckRedirectURLAsyncTask runner = new CheckRedirectURLAsyncTask();

        runner.execute(curURL);

        myWebView = (WebView) v.findViewById(R.id.web_view);
        myWebView.getSettings().setUserAgentString(Constants.USER_AGENT_STRING);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new WebViewClient());
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity.isConnectedToNetwork()) {
            myWebView.loadUrl(curURL);
        }else{
            myWebView.loadData(Constants.NO_INTERNET_HTML, "text/html", null);
        }
        return v;
    }

    /**
     * Function that monitors link clicks on the WebView and opens anything but the home page
     * in a separate browser window. This method is to be called from the OnPostExecute() method
     * of the CheckRedirectURLAsyncTask, because it is necessary to get the redirected URL so
     * that the app doesn't open the home page in a browser window as well.
     */
    private void startMonitoringLinkClicks(){
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(secondURL != null){
                    if(url.equals(secondURL.toString())) {
                        return false;
                    }
                }

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return true;
            }
        });
    }


    public static void loadWebContent(){
        myWebView.loadUrl(Constants.INFO_URL);
    }

    /**
     * When fragment is resumed, checks if it is connected to network. If not
     * shows alert dialog prompting user to connect
     */
    @Override
    public void onResume() {
        super.onResume();
        if(getUserVisibleHint()){
            MainActivity mainActivity = (MainActivity) getActivity();
            if(!mainActivity.isConnectedToNetwork()){
                mainActivity.showNetworkNotConnectedDialog();
            }
        }
    }

    /**
     * When fragment comes into view, checks if it is connected to network. If not
     * shows alert dialog prompting user to connect
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && isResumed()){
            MainActivity mainActivity = (MainActivity) getActivity();
            if(!mainActivity.isConnectedToNetwork()){
                mainActivity.showNetworkNotConnectedDialog();
            }
        }
    }

    /**
     * Goes back in webview if it is possible to do so
     * @return true if web view was able to go back, false otherwise
     */
    public boolean backPressed(){
        if(myWebView != null){
            if(myWebView.canGoBack()){
                myWebView.goBack();
                return true;
            }
        }
        return false;
    }



    /**
     * Class to find the redirected URL of a URL. Then calls a method to monitor link clicks
     * in the webview
     */
    private class CheckRedirectURLAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                URL redirectUrl = new URL(Constants.INFO_URL);
                HttpURLConnection ucon = null;
                try {
                    ucon = (HttpURLConnection) redirectUrl.openConnection();
                    ucon.setInstanceFollowRedirects(false);
                    secondURL = new URL(ucon.getHeaderField("Location"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if(secondURL == null){
                return null;
            }
            return secondURL.toString();
        }

        /**
         * Starts monitoring for link clicks on the webview
         * @param result
         */
        @Override
        protected void onPostExecute(String result) {
            startMonitoringLinkClicks();
        }
    }

}
