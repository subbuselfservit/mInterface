package com.selfservit.util;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.apache.cordova.*;

public class mInterface extends CordovaPlugin{
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public boolean execute(final String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if(action.equals("StartService")){
			if(!isServiceRunning(mInterfaceService.class)) {
				Intent serviceIntent = new Intent(cordova.getActivity().getApplicationContext(), mInterfaceService.class);
				cordova.getActivity().startService(serviceIntent);
				callbackContext.success("Success");
			}
		}else if(action.equals("GetLocation")){
			callbackContext.success(new mInterfaceUtil().getLocation(cordova.getActivity().getApplicationContext()));
		}else if(action.equals("CheckLocation")){
			callbackContext.success(new mInterfaceUtil().checkLocation(cordova.getActivity().getApplicationContext()));
		}else if(action.equals("CheckDateTimeAccuracy")){
			try {
				if (Settings.Global.getInt(this.cordova.getActivity().getContentResolver(), Settings.Global.AUTO_TIME) == 1) {
					callbackContext.success("true");
				}else{
					callbackContext.success("false");
				}
			} catch (Settings.SettingNotFoundException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	private boolean isServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager)cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}

