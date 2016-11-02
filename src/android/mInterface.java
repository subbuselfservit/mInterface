package com.selfservit.util;

import org.apache.cordova.CordovaInterface;
import android.util.Log;
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
		if(action == "TrackLocation"){
			if(!isServiceRunning(mInterfaceService.class)) {
				Intent serviceIntent = new Intent(cordova.getActivity().getApplicationContext(), mInterfaceService.class);
				cordova.getActivity().startService(serviceIntent);
				callbackContext.success("Success");
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
