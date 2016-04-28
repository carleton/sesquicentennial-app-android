package carleton150.edu.carleton.carleton150.Models;

/**
 * Created by haleyhinze on 4/27/16.
 *
 * Class from : http://stackoverflow.com/questions/30103743/webview-reload-after-connection-recovered
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.MainFragments.InfoFragment;

public class ConnectionBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            if(InfoFragment.myWebView != null) {
                InfoFragment.myWebView.reload();
            }
        }

    }
}
