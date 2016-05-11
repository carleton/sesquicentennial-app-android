package carleton150.edu.carleton.carleton150.Models;

/**
 * Created by haleyhinze on 4/27/16.
 * Registers a reciever to be notified when phone is connected to a network.
 * Notifies the home fragment that network is connected so it can reload webview.
 *
 * Class from : http://stackoverflow.com/questions/30103743/webview-reload-after-connection-recovered
 */

//TODO: it would be nice if this would notify whichever fragment is in view as well so it can try requesting information
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import carleton150.edu.carleton.carleton150.MainFragments.HomeFragment;

public class ConnectionBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            if(HomeFragment.myWebView != null) {
                HomeFragment.loadWebContent();
            }
        }
    }
}
