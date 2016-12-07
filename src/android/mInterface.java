package com.selfservit.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
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
	private static final int PERMISSION_DENIED_ERROR = 1;
	CallbackContext callback;
	private static JSONArray arguments = null;
	 @ Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		if (!isServiceRunning(mInterfaceService.class)) {
			Intent serviceIntent = new Intent(cordova.getActivity().getApplicationContext(), mInterfaceService.class);
			cordova.getActivity().startService(serviceIntent);
		}
	}
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
			callback = callbackContext;
			if (cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
				chooseFile(callbackContext);
				return true;
			} else {
				cordova.requestPermission(this, 0, Manifest.permission.READ_EXTERNAL_STORAGE);
			}
		} else if (action.equals("CopyFile")) {
			arguments = args;
			if (cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				copyFile(callbackContext);
			} else {
				cordova.requestPermission(this, 1, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			}
		}else if (action.equals("PlayStoreUpdate")) {
			cordova.getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.selfservit.mservice")));
		}
		return true;
	}

	private void copyFile(CallbackContext callbackContext) {
		JSONObject fileObj = null;
		String srcPath,
		srcFile,
		desPath,
		desFile;
		File srcPathObj,
		desPathObj,
		sourceFile,
		destinationFile;
		try {
			fileObj = arguments.getJSONObject(0);
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
				callbackContext.success("srcPath not found");
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
			Toast.makeText(cordova.getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	public void chooseFile(CallbackContext callbackContext) {
		if (Build.MANUFACTURER.equals("samsung")) {
			Toast.makeText(cordova.getActivity().getApplicationContext(), Build.MANUFACTURER, Toast.LENGTH_LONG).show();
			Intent intent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
			intent.putExtra("CONTENT_TYPE", "*/*");
			Intent chooser = Intent.createChooser(intent, "Select File");
			cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);
		} else {
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
				if (Build.VERSION.SDK_INT >= 19 && !Build.MANUFACTURER.equals("samsung") && DocumentsContract.isDocumentUri(cordova.getActivity(), uri)) {
					try {
						if (isExternalStorageDocument(uri)) { // ExternalStorageProvider
							final String docId = DocumentsContract.getDocumentId(uri);
							final String[]split = docId.split(":");
							final String type = split[0];
							String storageDefinition;
							if ("primary".equalsIgnoreCase(type)) {
								getfilePath = Environment.getExternalStorageDirectory() + "/" + split[1];
								filePath = getfilePath.substring(0, getfilePath.lastIndexOf(File.separator));
							} else {
								if (Environment.isExternalStorageRemovable()) {
									storageDefinition = System.getenv("EXTERNAL_STORAGE");
								} else {
									final int splitIndex = docId.indexOf(':', 1);
									final String tag = docId.substring(0, splitIndex);
									final String path = docId.substring(splitIndex + 1);

									String nonPrimaryVolume = getPathToNonPrimaryVolume(cordova.getActivity(), tag);
									if (nonPrimaryVolume != null) {
										storageDefinition = nonPrimaryVolume + "/" + path;
										File file = new File(storageDefinition);
										if (file.exists() && file.canRead()) {
											getfilePath = storageDefinition;
											filePath = getfilePath.substring(0, getfilePath.lastIndexOf(File.separator));
										}
									}
								}
							}
						} else if (isDownloadsDocument(uri)) { // DownloadsProvider

							final String id = DocumentsContract.getDocumentId(uri);
							final Uri contentUri = ContentUris.withAppendedId(
									Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

							getfilePath = getDataColumn(cordova.getActivity(), contentUri, null, null);
							filePath = getfilePath.substring(0, getfilePath.lastIndexOf(File.separator));

						} else if (isMediaDocument(uri)) { // MediaProvider
							final String docId = DocumentsContract.getDocumentId(uri);
							final String[]split = docId.split(":");
							final String type = split[0];

							Uri contentUri = null;
							if ("image".equals(type)) {
								contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
							} else if ("video".equals(type)) {
								contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
							} else if ("audio".equals(type)) {
								contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
							}

							final String selection = "_id=?";
							final String[]selectionArgs = new String[]{
								split[1]
							};
							getfilePath = getDataColumn(cordova.getActivity(), contentUri, selection, selectionArgs);
							filePath = getfilePath.substring(0, getfilePath.lastIndexOf(File.separator));
						}
					} catch (Exception e) {
						e.printStackTrace();
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
						cursor = cordova.getActivity().getContentResolver().query(uri, projection, null, null, null);
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
	 @ Override
	public void onRequestPermissionResult(int requestCode, String[]permissions,
		int[]grantResults)throws JSONException{
		for (int r: grantResults) {
			if (r == PackageManager.PERMISSION_DENIED) {
				this.callback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
				return;
			}
		}
		switch (requestCode) {
		case 0:
			chooseFile(callback);
			break;
		case 1:
			copyFile(callback);
			break;
		}
	}
	public static String getDataColumn(Context context, Uri uri, String selection, String[]selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[]projection = {
			column
		};

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}
	 @ TargetApi(Build.VERSION_CODES.KITKAT)
	public static String getPathToNonPrimaryVolume(Context context, String tag) {
		File[]volumes = context.getExternalCacheDirs();
		if (volumes != null) {
			for (File volume: volumes) {
				if (volume != null) {
					String path = volume.getAbsolutePath();
					if (path != null) {
						int index = path.indexOf(tag);
						if (index != -1) {
							return path.substring(0, index) + tag;
						}
					}

				}
			}
		}

		return null;
	}
}
