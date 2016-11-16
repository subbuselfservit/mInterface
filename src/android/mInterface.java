package com.selfservit.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.apache.cordova.*;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class mInterface extends CordovaPlugin{
	private static final int PICK_FILE_REQUEST= 1;
	CallbackContext callback;
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public boolean execute(final String action, JSONArray args,  CallbackContext callbackContext) throws JSONException {
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
		}else if(action.equals("CopyFile")){
			String source =args.getString(0);
			String destination =args.getString(1);
			File sourcePath = new File(source);
			File destinationPath = new File (destination);
			try {
				if(sourcePath.exists()){
					InputStream in = new FileInputStream(sourcePath);
					OutputStream out = new FileOutputStream(destinationPath);
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					in.close();
					out.close();
					callbackContext.success("success");
				}else{
					callbackContext.success("failure");
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	public void chooseFile(CallbackContext callbackContext) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("file/*");
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
				String fileName = null,getfilePath,filePath = null,fileExtension = null,fileType,fileSize=null;
				long getSize;
				Uri uri = data.getData();
				fileType =this.cordova.getActivity().getContentResolver().getType(uri);
				File getFileName;
				if(fileType != null) {
					String[] projection = {MediaStore.Images.Media.DATA,MediaStore.Images.Media.SIZE};
					Cursor cursor = this.cordova.getActivity().getContentResolver().query(uri, projection, null, null, null);
					if (cursor.moveToFirst()) {
						int index1 = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
						int actual_image_column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
						getfilePath = cursor.getString(actual_image_column_index);
						filePath =getfilePath.substring(0,getfilePath.lastIndexOf(File.separator));
						fileName = getfilePath.substring(getfilePath.lastIndexOf("/")+1);
						fileExtension = "."+fileName.substring(fileName.lastIndexOf(".")+1);
						fileSize = cursor.getString(index1);
					}
				}else{
						getfilePath =data.getData().getPath();
						filePath = getfilePath.substring(0, getfilePath.lastIndexOf(File.separator));
						getFileName = new File(getfilePath);
						fileName =getFileName.getName();
						fileExtension ="."+ getfilePath.substring(getfilePath.lastIndexOf(".") + 1);
						getSize =getFileName.length();
						fileSize = String.valueOf(getSize);

				}
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.put("filePath",filePath);
					jsonObject.put("fileName",fileName);
					jsonObject.put("fileSize",fileSize);
					jsonObject.put("fileExtension",fileExtension);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (filePath != null) {
					callback.success(jsonObject);
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

