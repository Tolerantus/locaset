package ru.inventions.tolerantus.locaset.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by Aleksandr on 09.02.2017.
 */

public class NetworkUtils {

    public static boolean isAnyNetwork(Context context) {
        return isConnected(context, ConnectivityManager.TYPE_WIFI) || isConnected(context, ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean isWifi(Context context) {
        return isConnected(context, ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isConnected(Context context, int networkType) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetwork = connManager.getNetworkInfo(networkType);
        return mNetwork.isAvailable() && mNetwork.isConnected();
    }


    public static Network[] getAllNetworks(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getAllNetworks();
    }
}
