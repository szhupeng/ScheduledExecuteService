package com.zhupeng.location;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

public class BDLocationStrategy extends Strategy {

    private LocationClient client;

    @Override
    public void execute(Context context) {
        if (null == client) {
            client = new LocationClient(context);
            client.registerLocationListener(new LocationListenerBridge());

            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setCoorType("bd09ll");
            option.setOpenGps(true);
            option.setIgnoreKillProcess(true);
            client.setLocOption(option);
        }

        if (!client.isStarted()) {
            client.start();
        }
    }

    @Override
    public void finish(Context context) {
        if (client != null) {
            client.stop();
            client = null;
        }
    }

    private void notifyResult(double longitude, double latitude, String address) {
        final int count = mResultListeners.beginBroadcast();
        for (int i = 0; i < count; i++) {
            IResultListener listener = mResultListeners.getBroadcastItem(i);
            if (null == listener) continue;
            try {
                Bundle args = new Bundle();
                args.putDouble("longitude", longitude);
                args.putDouble("latitude", latitude);
                args.putString("address", address);
                listener.onResult(args);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    class LocationListenerBridge extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            final double latitude = bdLocation.getLatitude();
            final double longitude = bdLocation.getLongitude();
            notifyResult(longitude, latitude, bdLocation.getProvince());
        }
    }
}
