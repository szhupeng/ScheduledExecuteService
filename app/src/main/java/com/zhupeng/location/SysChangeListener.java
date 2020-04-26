package com.zhupeng.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

/**
 * 系统通知监听
 * 主要监听事件包括：
 * 1.息屏和开屏
 * 2.网络断开和连接
 */
public class SysChangeListener {

    final BroadcastReceiver receiver = new SysChangeReceiver();

    public BroadcastReceiver getReceiver() {
        return receiver;
    }

    public void listen(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(receiver, filter);
    }

    public void disregard(Context context) {
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
    }

    public final static class SysChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_SCREEN_OFF:
                case Intent.ACTION_SCREEN_ON:
                    log("屏幕：" + action);

                    break;
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    final int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                    if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                        log("WIFI_STATE_CHANGED_ACTION：网络不可用");

                    } else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        log("WIFI_STATE_CHANGED_ACTION：网络可用");

                    }

                    break;
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    final Parcelable extra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (extra != null) {
                        NetworkInfo networkInfo = (NetworkInfo) extra;
                        NetworkInfo.State state = networkInfo.getState();
                        boolean isConnected = state == NetworkInfo.State.CONNECTED;
                        if (isConnected) {
                            log("NETWORK_STATE_CHANGED_ACTION：网络可用");

                        } else {
                            log("NETWORK_STATE_CHANGED_ACTION：网络不可用");

                        }
                    }
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                    if (info != null) {
                        if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
                            log("CONNECTIVITY_ACTION：网络可用");

                        } else {
                            log("NETWORK_STATE_CHANGED_ACTION：网络不可用");

                        }
                    }
                    break;
                default:
                    return;
            }
        }

        private void log(String msg) {
            Log.d("MY-TAG", msg);
        }
    }
}
