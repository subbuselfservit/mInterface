package com.selfservit.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class mInterfaceUtil {
	LocationManager locationManager;
	LocationListener locationListener;
	/*FOR GETTING CURRENT LOCATION */
	public String getLocation(Context context) {
		String latitude,
		longitude;

		/* REGISTER THE LOCATION MANAGER */
		locationManager = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);

		//*** IMPLEMENT LOCATION LISTENER CLASS METHODS ****//
		locationListener = new LocationListener() {
			 @ Override
			public void onLocationChanged(Location location) {}

			 @ Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}

			 @ Override
			public void onProviderEnabled(String provider) {}

			 @ Override
			public void onProviderDisabled(String provider) {}
		};

		//*** REQUEST LOCATION UPDATES FROM LOCATION MANAGER ***//
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

		//*** CONVERT DOUBLE VALUE INTO TO STRING ***//
		latitude = String.valueOf(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude());
		longitude = String.valueOf(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude());

		//*** SUBSTRING LATITUDE,LONGITUDE IF GREATER THAN 10 POINTS ***//
		if (latitude.length() > 10) {
			latitude = latitude.substring(0, 10);
		}
		if (longitude.length() > 10) {
			longitude = longitude.substring(0, 10);
		}
		//*** RETURN LATITUDE,LONGITUDE VALUE IN JSON OBJECT FORMAT ***//
		return "{\"lat\":" + "\"" + latitude + "\"" + ",\"lon\":" + "\"" + longitude + "\"}";
	}

	/*FOR CHECK LOCATION SERVICE SETTING IS ENABLED OR NOT */
	public String checkLocation(Context context) {
		//*** REGISTER THE LOCATION MANAGER *** //
		locationManager = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
		//*** CHECK LOCATION SETTING ENABLED OR NOT ***//
		return String.valueOf(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
	}

	/* COPY A FILE FROM ONE LOCATION TO ANOTHER LOCATION */
	public void copyFile(CallbackContext callbackContext, JSONArray arguments) {
		JSONObject inputFileObj;
		String srcFilePath,
		srcFileName,
		desFilePath,
		desFileName;
		InputStream srcFileReader;
		OutputStream desFileWriter;
		int srcFileSize;
		try {
			//*** GET USER INPUT FOR SOURCE FILE AND DESTINATION PATH ***//
			inputFileObj = arguments.getJSONObject(0);
			srcFilePath = inputFileObj.optString("srcPath").toString();
			srcFileName = inputFileObj.optString("srcFile").toString();
			desFilePath = inputFileObj.optString("desPath").toString();
			desFileName = inputFileObj.optString("desFile").toString();
			//*** CHECK SOURCE FILE PATH EXITS OR NOT ***//
			if (new File(srcFilePath).exists()) {
				//*** CHECK DESTINATION FILE PATH EXITS OR NOT ***//
				if (!new File(desFilePath).exists()) {
					new File(desFilePath).mkdirs();
				}
				//*** CHECK SOURCE FILE EXITS OR NOT ***//
				if (new File(srcFilePath + "/" + srcFileName).exists()) {
					srcFileReader = new FileInputStream(new File(srcFilePath + "/" + srcFileName));
					desFileWriter = new FileOutputStream(new File(desFilePath + "/" + desFileName));

					//*** WRITE SOURCE FILE INTO DESTINATION PATH ***//
					while ((srcFileSize = srcFileReader.read(new byte[1024])) > 0) {
						desFileWriter.write(new byte[1024], 0, srcFileSize);
					}
					srcFileReader.close();
					desFileWriter.close();
					callbackContext.success("success");
				} else {
					callbackContext.error("failure");
				}
			} else {
				callbackContext.error("srcPath not found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*GETTING A FILEPATH OF API LEVEL 19 AND ABOVE */
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
