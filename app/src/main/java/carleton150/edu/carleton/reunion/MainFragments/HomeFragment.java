package carleton150.edu.carleton.reunion.MainFragments;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import carleton150.edu.carleton.reunion.Constants;
import carleton150.edu.carleton.reunion.MainActivity;
import carleton150.edu.carleton.reunion.Models.ConnectionBroadcastReceiver;
import carleton150.edu.carleton.reunion.R;

import static carleton150.edu.carleton.reunion.R.id.btn_refresh;

/**
 * Fragment to display a webview that contains information about the app
 */
public class HomeFragment extends MainFragment {

    String curURL;
    URL secondURL = null;
    View v;
    public static WebView myWebView;
    private RelativeLayout myLoadingLayout;
    private ImageView myLoadingAnim;
    private AnimationDrawable myAnimationDrawable;
    IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    CheckRedirectURLAsyncTask runner = new CheckRedirectURLAsyncTask();
    private ConnectionBroadcastReceiver connectionBroadcastReceiver = new ConnectionBroadcastReceiver(this);
    private boolean connectionBroadcastReceiverRegisterred = false;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerConnectionBroadcastReceiver();

    }

    /**
     * opens the WebView, displays loading animation
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
        final MainActivity mainActivity = (MainActivity) getActivity();

        if (mainActivity.isConnectedToNetwork()) {
            runner.execute(curURL);
        }

        myWebView = (WebView) v.findViewById(R.id.web_view);
        myLoadingLayout = (RelativeLayout) v.findViewById(R.id.layout_loading);
        myLoadingAnim = (ImageView) v.findViewById(R.id.anim_web_view_loading);
        myAnimationDrawable = (AnimationDrawable) myLoadingAnim.getDrawable();
        showLoadingAnim();

        myWebView.getSettings().setUserAgentString(Constants.USER_AGENT_STRING);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.setWebChromeClient(new WebChromeClient() {

            /**
             * If the web view is loaded, makes it visible and hides the loading animation
             * @param view
             * @param newProgress
             */
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress == 100) {
                    myWebView.setVisibility(View.VISIBLE);
                    mainActivity.setWebViewLoaded(true);
                    mainActivity.requestAll();
                    hideLoadingAnim();
                }
            }
        });


        ImageButton btnRefresh = (ImageButton) v.findViewById(btn_refresh);
        btnRefresh.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        loadWebView();
                    }
                });
//        myWebView.setWebViewClient(new WebViewClient());
//        loadWebView();
        setTimeOfLastRefresh();
        return v;
    }

    /**
     * Loads the URL in the web view, displays the loading animation
     * until the web view is successfully loaded. If there is no
     * internet connection, shows the web view with an error message
     * stating that there is no internet
     */

    public void loadWebView() {

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity.isConnectedToNetwork()) {
            myWebView.setWebViewClient(new WebViewClient());
            myWebView.loadUrl(curURL);
            myWebView.setVisibility(View.GONE);
            showLoadingAnim();
        } else {
            myWebView.loadData(Constants.NO_INTERNET_HTML, "text/html", null);
        }
        setTimeOfLastRefresh();
    }

    /**
     * Makes loading screen visible and starts the animation
     */
    private void showLoadingAnim() {

        if (!myAnimationDrawable.isRunning()) {
            myAnimationDrawable.start();
        }

        myLoadingLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Makes loading screen invisible and stops the animation
     */
    private void hideLoadingAnim() {

        if (myAnimationDrawable.isRunning()) {
            myAnimationDrawable.stop();
        }
        myLoadingLayout.setVisibility(View.GONE);
    }

    /**
     * Function that monitors link clicks on the WebView and opens anything but the home page
     * in a separate browser window. This method is to be called from the OnPostExecute() method
     * of the CheckRedirectURLAsyncTask, because it is necessary to get the redirected URL so
     * that the app doesn't open the home page in a browser window as well.
     */
    private void startMonitoringLinkClicks() {
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (secondURL != null) {
                    if (url.equals(secondURL.toString())) {
                        return false;
                    }
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                loadWebView();
                return true;
            }


        });
    }


    /**
     * If we have no obtained the redirect URL and started listening
     * for link clicks, do so. Load the url for the home page.
     * <p/>
     * This method is called from the ConnectionBroadcastReceiver
     */
    public void loadWebContent() {
        if (secondURL == null) {
            try {
                runner.execute(curURL);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        loadWebView();
    }


    /**
     * When fragment is resumed, checks if it is connected to network. If not
     * shows alert dialog prompting user to connect
     */
    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainActivity = (MainActivity) getActivity();
        registerConnectionBroadcastReceiver();
        if (getUserVisibleHint()) {
            refreshWebViewIfNecessary();
            if (!mainActivity.isConnectedToNetwork()) {
                mainActivity.showNetworkNotConnectedDialog();
            }
        }
    }

    /**
     * Registers the ConnectionBroadcastReceiver to receive a broadcast
     * if the internet is connected
     */
    private void registerConnectionBroadcastReceiver() {
        if (!connectionBroadcastReceiverRegisterred) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.registerReceiver(connectionBroadcastReceiver, intentFilter);
            connectionBroadcastReceiverRegisterred = true;
        }
    }

    /**
     * Unregisters the ConnectionBroadcastReceiver
     */
    private void unregisterConnectionBroadcastReceiver() {
        if (connectionBroadcastReceiverRegisterred) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.unregisterReceiver(connectionBroadcastReceiver);
            connectionBroadcastReceiverRegisterred = false;
        }
    }

    /**
     * When fragment comes into view, checks if it is connected to network. If not
     * shows alert dialog prompting user to connect
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            refreshWebViewIfNecessary();
            MainActivity mainActivity = (MainActivity) getActivity();
            if (!mainActivity.isConnectedToNetwork()) {
                mainActivity.showNetworkNotConnectedDialog();
            }

        }
    }

    /**
     * If over five minutes have passed since the last refresh, refreshes
     * the web view
     */
    private void refreshWebViewIfNecessary() {
        if (checkElapsedTime(timeOfLastRefresh) > 1000) {
            loadWebView();
        }
    }

    /**
     * Goes back in webview if it is possible to do so
     *
     * @return true if web view was able to go back, false otherwise
     */
    public boolean backPressed() {
        if (myWebView != null) {
            if (myWebView.canGoBack()) {
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

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (secondURL == null) {

                return null;
            }
            return secondURL.toString();
        }

        /**
         * Starts monitoring for link clicks on the webview
         *
         * @param result
         */
        @Override
        protected void onPostExecute(String result) {

            startMonitoringLinkClicks();
        }
    }

    @Override
    public void onPause() {

        unregisterConnectionBroadcastReceiver();
        super.onPause();

    }
}
