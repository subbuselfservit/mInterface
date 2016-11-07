package com.selfservit.util;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class mInterfaceUtil {

    public String getLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        return "{\"lat\":"+"\""+locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude()+"\""+",\"lon\":"+"\""+locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude()+"\"}";
    }

    public String checkLocation(Context context){
        LocationManager locationManager = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        boolean location_enabled=locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(location_enabled) {
            return "true";
        } else {
            return "false";
        }
    }
}
