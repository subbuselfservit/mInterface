package com.selfservit.util;

import org.apache.cordova.CordovaInterface;
import android.util.Log;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import org.apache.cordova.*;

public class mInterface extends CordovaPlugin{
	public mInterface(){
		//constructor
	}
	
	@Override
	public boolean execute(final String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if(action.equals("StartService")){
			if(!isServiceRunning(mInterfaceService.class)) {
				Intent serviceIntent = new Intent(cordova.getActivity().getApplicationContext(), mInterfaceService.class);
				cordova.getActivity().startService(serviceIntent);
				callbackContext.success("Success");
			}
		}else if(action.equals("GetLocation")){
			LocationManager locationManager = (LocationManager) cordova.getActivity().getApplicationContext().getSystemService(cordova.getActivity().getApplicationContext().LOCATION_SERVICE);
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
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, locationListener);

	       		double latitude = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude();
    	   	        double longitude=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude();
			callbackContext.success("{\"lat\":"+"\""+latitude+"\""+",\"lon\":"+"\""+longitude+"\"}");
		}else if(action.equals("CheckLocation")){
			boolean location_enabled=false;
			LocationManager locationManager = (LocationManager) cordova.getActivity().getApplicationContext().getSystemService(cordova.getActivity().getApplicationContext().LOCATION_SERVICE);
			location_enabled=locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			if(location_enabled) {
				callbackContext.success("true");
			} else {
				callbackContext.success("false");
			}
			
		}
		return true;
	}	
	private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager)this.cordova.getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
