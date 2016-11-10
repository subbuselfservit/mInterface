package com.selfservit.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.apache.cordova.*;

public class mInterface extends CordovaPlugin{
	private static final int PICK_FILE_REQUEST= 1;
	CallbackContext callback;
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
		}else if(action.equals("FileChooser")){
			chooseFile(callbackContext);
			return true;
		}
		return true;
	}
	public void chooseFile(CallbackContext callbackContext) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

		Intent chooser = Intent.createChooser(intent, "Select File");
		cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);

		PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
		pluginResult.setKeepCallback(true);
		callback = callbackContext;
		callbackContext.sendPluginResult(pluginResult);
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_FILE_REQUEST && callback != null) {
			if (resultCode == Activity.RESULT_OK) {
				String filePath = data.getData().getPath();
				if (filePath != null) {
					callback.success(filePath);
				} else {
					callback.error("File uri was null");
				}
			} else if (resultCode == Activity.RESULT_CANCELED) {
				// TODO NO_RESULT or error callback?
				callback.error("failure");
			} else {
				callback.error(resultCode);
			}
		}
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

