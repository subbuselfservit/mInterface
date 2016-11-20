package com.selfservit.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.apache.cordova. * ;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class mInterface extends CordovaPlugin {
	private static final int PICK_FILE_REQUEST = 1;
	CallbackContext callback;
	@ Override
	public boolean execute(final String action, JSONArray args, CallbackContext callbackContext)throws JSONException {
		if (action.equals("StartService")) {
			if (!isServiceRunning(mInterfaceService.class)) {
				Intent serviceIntent = new Intent(cordova.getActivity().getApplicationContext(), mInterfaceService.class);
				cordova.getActivity().startService(serviceIntent);
				callbackContext.success("Success");
			}
		} else if (action.equals("GetLocation")) {
			callbackContext.success(new mInterfaceUtil().getLocation(cordova.getActivity().getApplicationContext()));
		} else if (action.equals("CheckLocation")) {
			callbackContext.success(new mInterfaceUtil().checkLocation(cordova.getActivity().getApplicationContext()));
		} else if (action.equals("FileChooser")) {
			chooseFile(callbackContext);
			return true;
		} else if (action.equals("CopyFile")) {
			JSONObject fileObj;
			String srcPath,
					srcFile,
					desPath,
					desFile;
			File srcPathObj,
					desPathObj,
					sourceFile,
					destinationFile;
			fileObj = args.getJSONObject(0);
			srcPath = fileObj.optString("srcPath").toString();
			srcFile = fileObj.optString("srcFile").toString();
			desPath = fileObj.optString("desPath").toString();
			desFile = fileObj.optString("desFile").toString();
			srcPathObj = new File(srcPath);
			desPathObj = new File(desPath);
			sourceFile = new File(srcPath + "/" + srcFile);
			destinationFile = new File(desPath + "/" + desFile);
			try {
				if (!srcPathObj.exists()) {
					callbackContext.success("failure");
				}
				if (!desPathObj.exists()) {
					desPathObj.mkdirs();
				}
				if (sourceFile.exists()) {
					InputStream in = new FileInputStream(sourceFile);
					OutputStream out = new FileOutputStream(destinationFile);
					byte[]buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					in.close();
					out.close();
					callbackContext.success("success");
				} else {
					callbackContext.success("failure");
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	public void chooseFile(CallbackContext callbackContext) {
		if(Build.MANUFACTURER.equals("samsang")){
			Toast.makeText(cordova.getActivity().getApplicationContext(),Build.MANUFACTURER,Toast.LENGTH_LONG).show();
			Intent intent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
			intent.putExtra("CONTENT_TYPE", "*/*");
			cordova.getActivity().startActivityForResult(intent, PICK_FILE_REQUEST);
		}else {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("*/*");
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
			Intent chooser = Intent.createChooser(intent, "Select File");
			cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);
		}
		PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
		pluginResult.setKeepCallback(true);
		callback = callbackContext;
		callbackContext.sendPluginResult(pluginResult);
	}
	@ Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_FILE_REQUEST && callback != null) {
			if (resultCode == Activity.RESULT_OK) {
				String fileName = null,
						getfilePath,
						filePath = null,
						fileExtension = null,
						fileType,
						fileSize = null;
				Cursor cursor;
				long getSize;
				Uri uri = data.getData();
				fileType = this.cordova.getActivity().getContentResolver().getType(uri);
				File getFileName;
				if (Build.VERSION.SDK_INT > 19 && !Build.MANUFACTURER.equals("samsung")) {
					try {
						String[]projection = {
								MediaStore.Images.Media.DATA,
						};
						cursor = cordova.getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null, null);
						cursor.moveToFirst();

						getfilePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
						filePath = getfilePath.substring(0, getfilePath.lastIndexOf(File.separator));
					} catch (Exception e) {
						callback.error("failer Exception");
					}
					cursor = cordova.getActivity().getContentResolver()
							.query(uri, null, null, null, null, null);

					try {
						if (cursor != null && cursor.moveToFirst()) {
							fileName = cursor.getString(
									cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
							fileExtension = "." + fileName.substring(fileName.lastIndexOf(".") + 1);
							int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
							if (!cursor.isNull(sizeIndex)) {
								fileSize = cursor.getString(sizeIndex);
							} else {
								fileSize = "Unknown";
							}
						}
					}
					finally {
						cursor.close();
					}
				} else {
					if (fileType != null) {
						String[]projection = {
								MediaStore.Images.Media.DATA,
								MediaStore.Images.Media.SIZE,
								MediaStore.Images.Media.DISPLAY_NAME
						};
						cursor = this.cordova.getActivity().getContentResolver().query(uri, projection, null, null, null);
						if (cursor.moveToFirst()) {
							int index1 = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
							int name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
							int actual_image_column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
							getfilePath = cursor.getString(actual_image_column_index);
							filePath = getfilePath.substring(0, getfilePath.lastIndexOf(File.separator));
							fileName = cursor.getString(name);
							fileExtension = "." + fileName.substring(fileName.lastIndexOf(".") + 1);
							fileSize = cursor.getString(index1);
						}
						cursor.close();

					} else {
						getfilePath = data.getData().getPath();
						filePath = getfilePath.substring(0, getfilePath.lastIndexOf(File.separator));
						getFileName = new File(getfilePath);
						fileName = getFileName.getName();
						fileExtension = "." + getfilePath.substring(getfilePath.lastIndexOf(".") + 1);
						getSize = getFileName.length();
						fileSize = String.valueOf(getSize);

					}
				}
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.put("filePath", filePath);
					jsonObject.put("fileName", fileName);
					jsonObject.put("fileSize", fileSize);
					jsonObject.put("fileExtension", fileExtension);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (filePath != null) {
					callback.success(jsonObject);
				} else {
					callback.error("File path was null");
				}
			} else if (resultCode == Activity.RESULT_CANCELED) {
				// TODO NO_RESULT or error callback?
				callback.error("failure");
			} else {
				callback.error(resultCode);
			}
		}
	}

	private boolean isServiceRunning(Class <  ?  > serviceClass) {
		ActivityManager manager = (ActivityManager)cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
