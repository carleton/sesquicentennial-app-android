package carleton150.edu.carleton.carleton150.MainFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.R;

/**
 * Fragment to display a webview that contains information about the app
 */
public class HomeFragment extends MainFragment {

    String curURL;
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
        myWebView = (WebView) v.findViewById(R.id.web_view);
        myWebView.getSettings().setUserAgentString(Constants.USER_AGENT_STRING);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new WebClient());
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity.isConnectedToNetwork()) {
            myWebView.loadUrl(curURL);
        }else{
            myWebView.loadData(Constants.NO_INTERNET_HTML, "text/html", null);
        }
        return v;
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

    private class WebClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

    }

}
