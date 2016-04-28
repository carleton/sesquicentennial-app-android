package carleton150.edu.carleton.carleton150.MainFragments;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewFragment;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class InfoFragment extends MainFragment {

    String curURL;
    View v;
    public static WebView myWebView;


    public InfoFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_info, container, false);
        Constants constants = new Constants();

        curURL = constants.INFO_URL;
        myWebView = (WebView) v.findViewById(R.id.web_view);
        myWebView.getSettings().setUserAgentString(constants.USER_AGENT_STRING);


        myWebView.getSettings().setJavaScriptEnabled(true);

        myWebView.setWebViewClient(new WebClient());

        myWebView.loadUrl(curURL);

        return v;
    }

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
