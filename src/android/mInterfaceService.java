package com.selfservit.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.BufferedReader;
import java.io.File;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class mInterfaceService extends Service {
	public Timer setQueueInterval,
			setTimerIntervel,
			setChecksumTimerInterval;

	@ Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@ Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		TimerTask setQueueIntervalObj,
				setTimerIntervelObj,
				setChecksumTimerIntervalObj;
		// ****Queue Manager Interval Timer ***** //
		setQueueInterval = new Timer();
		setQueueIntervalObj = new TimerTask() {
			public void run() {
				if (isConnected()) {
					new DespatchQueue().execute();
				}
			}
		};
		setQueueInterval.schedule(setQueueIntervalObj, 1000, 1000);

		// **** TimeReader Interval Timer ***** //
		setTimerIntervel = new Timer();
		setTimerIntervelObj = new TimerTask() {
			public void run() {
				try {
					timeReader();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		setTimerIntervel.schedule(setTimerIntervelObj, 60000, 60000);

		// *** CheckSum value Indicator Timer *** //
		setChecksumTimerInterval = new Timer();
		setChecksumTimerIntervalObj = new TimerTask() {
			public void run() {
				try {
					if (isConnected()) {
						new CheckSumIndicatorResult().execute();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		setChecksumTimerInterval.schedule(setChecksumTimerIntervalObj, 180000, 180000);
		LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new MyLocationListener(locationManager);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 200, locationListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 200, locationListener);

		return START_NOT_STICKY;
	}
	private void timeReader()throws Exception {
		String serverTimeObj;
		SimpleDateFormat simpleDateFormat;
		StringBuilder timeObj;
		String currentLine;
		BufferedReader readerObj;
		JSONObject serverDateObj;
		timeObj = new StringBuilder();
		BufferedWriter writerObj;
		File baseDirectory = Environment.getExternalStorageDirectory();
		readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/time_profile.txt")));
		while ((currentLine = readerObj.readLine()) != null) {
			timeObj.append(currentLine);
		}
		readerObj.close();
		serverDateObj = new JSONObject(timeObj.toString());
		serverTimeObj = serverDateObj.optString("serverDate").toString();

		// ******SERVER TIME ******//
		simpleDateFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss");
		Date date = simpleDateFormat.parse(serverTimeObj);
		long a = date.getTime() + 60000;
		date.setTime(a);
		serverTimeObj = simpleDateFormat.format(date);
		serverDateObj.put("serverDate", serverTimeObj);
		writerObj = new BufferedWriter(new FileWriter(new File(baseDirectory, "mservice/time_profile.txt")));
		writerObj.write(serverDateObj.toString());
		writerObj.flush();
		writerObj.close();
	}
	private class CheckSumIndicatorResult extends AsyncTask < String,
			Void,
			String > {
		@ Override
		protected String doInBackground(String...strings) {
			File baseDirectory,
					checksumFile,
					userProfileFile;

			BufferedReader checksumFileReader,
					userProfileFileReader,
					responseReader;

			String currentLine,
					checksumFileData,
					checksumValue,
					refreshIndValue,
					userProfileFileData,
					serverResponseData;

			JSONObject checksumObj,
					userProfileObj,
					serverResponseObj;

			JSONArray serverResponseArray;

			URL requestPath;

			HttpURLConnection urlConObj;

			OutputStreamWriter oStreamObj;

			FileWriter writerObj;

			try {

				baseDirectory = Environment.getExternalStorageDirectory();
				checksumFileData = "";
				checksumValue = "";
				refreshIndValue = "";
				userProfileFileData = "";
				serverResponseData = "";

				/* READ THE CHECKSUM VALUE AND GET CHECKSUM AND REFRESH INDICATOR VALUE*/
				checksumFile = new File(baseDirectory, "/mservice/database/checksum_value.txt");
				if (checksumFile.exists()) {
					checksumFileReader = new BufferedReader(new FileReader(checksumFile));
					while ((currentLine = checksumFileReader.readLine()) != null) {
						checksumFileData += currentLine;
					}
					checksumFileReader.close();
					checksumObj = new JSONObject(checksumFileData);
					checksumValue = checksumObj.optString("checksum_value").toString();
					refreshIndValue = checksumObj.optString("refresh_ind").toString();
				}

				if (refreshIndValue.matches("") || refreshIndValue.matches("false")) {

					/* READ THE USER PROFILE VALUE */
					userProfileFile = new File(baseDirectory, "/mservice/user_profile.txt");
					if (userProfileFile.exists()) {
						userProfileFileReader = new BufferedReader(new FileReader(userProfileFile));
						while ((currentLine = userProfileFileReader.readLine()) != null) {
							userProfileFileData += currentLine;
						}
						userProfileFileReader.close();
						userProfileObj = new JSONObject(userProfileFileData);
						userProfileObj = userProfileObj.optJSONObject("login_profile");

						/* SEND VALIDATE CHECKSUM REQUEST TO SERVER */
						requestPath = new URL(userProfileObj.optString("protocol").toString() + "//" + userProfileObj.optString("domain_name").toString() + ":" + userProfileObj.optString("portno").toString() + "/JSONServiceEndpoint.aspx?appName=common_modules&serviceName=retrieve_listof_values_for_searchcondition&path=context/outputparam");
						urlConObj = (HttpURLConnection)requestPath.openConnection();
						urlConObj.setDoOutput(true);
						urlConObj.setRequestMethod("POST");
						urlConObj.setRequestProperty("CONTENT-TYPE", "application/json");
						urlConObj.connect();

						oStreamObj = new OutputStreamWriter(urlConObj.getOutputStream());
						oStreamObj.write("{\"context\":{\"sessionId\":" + "\"" + userProfileObj.optString("guid_val").toString() + "\"" + ",\"userId\":" + "\"" + userProfileObj.optString("user_id").toString() + "\"" + ",\"client_id\":" + "\"" + userProfileObj.optString("client_id").toString() + "\"" + ",\"locale_id\":" + "\"" + userProfileObj.optString("locale_id").toString() + "\"" + ",\"country_code\":" + "\"" + userProfileObj.optString("country_code").toString() + "\"" + ",\"inputparam\":{\"p_inputparam_xml\":\"<inputparam><lov_code_type>VALIDATE_CHECKSUM</lov_code_type><search_field_1>" + checksumValue + "</search_field_1><search_field_2>" + userProfileObj.optString("emp_id").toString() + "</search_field_2><search_field_3>MOBILE</search_field_3></inputparam>\"}}}");
						oStreamObj.flush();
						oStreamObj.close();

						/* READ THE SERVER RESPONSE AND WRITE TO THE FILE */
						responseReader = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
						while ((currentLine = responseReader.readLine()) != null) {
							serverResponseData += currentLine;
						}
						responseReader.close();
						urlConObj.disconnect();

						serverResponseArray = new JSONArray(serverResponseData);
						serverResponseObj = serverResponseArray.optJSONObject(0);
						writerObj = new FileWriter(checksumFile);
						writerObj.write(serverResponseObj.toString());
						writerObj.flush();
						writerObj.close();
						String date,hour,minute;
						date = serverResponseObj.optString("serverDate").toString();
						hour = serverResponseObj.optString("serverHour").toString();
						minute = serverResponseObj.optString("serverMinute").toString();
						new mInterfaceUtil().refreshTimeProfile(date,hour,minute);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	private class MyLocationListener implements LocationListener {
		public MyLocationListener(LocationManager locationManager) {}

		@ Override
		public void onLocationChanged(Location location) {
			new UpdateLocation(Double.toString(location.getLatitude()), Double.toString(location.getLongitude())).execute("");
			if (isConnected()) {
				new SendLocation().execute();
			}
		}
		@ Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@ Override
		public void onProviderEnabled(String provider) {}

		@ Override
		public void onProviderDisabled(String provider) {}
	}

	private class SendLocation extends AsyncTask < String,
			Void,
			String > {
		@ Override
		protected String doInBackground(String...urls) {
			String clientID,
					countryCode,
					deviceID;
			DocumentBuilderFactory dbfObj;
			DocumentBuilder dbObj;
			Document docObj;
			OutputStreamWriter oStreamObj;
			StringBuilder locationData,
					userData;
			BufferedReader readerObj;
			BufferedWriter writerObj;
			String currentLine,
					serverData,
					requesturl;
			JSONObject userObj;
			HttpURLConnection urlConObj;
			URL requestPath;

			File baseDirectory = Environment.getExternalStorageDirectory();

			try {
				/* GETTING THE LOCATION POINTS TO BE SENT TO THE SERVER */
				locationData = new StringBuilder();
				readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/MyLocation.txt")));
				while ((currentLine = readerObj.readLine()) != null) {
					locationData.append(currentLine + "\n");
					//lastKnownLocation = currentLine + "\n";
				}
				readerObj.close();
				serverData = locationData.toString();
				/* CLEARING THE LOCATION POINTS */
				if (serverData != "") {
					writerObj = new BufferedWriter(new FileWriter(new File(baseDirectory, "mservice/MyLocation.txt")));
					writerObj.write("");
					writerObj.flush();
					writerObj.close();
				}
				/* GETTING THE USER INFO */
				userData = new StringBuilder();
				readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/user.txt")));
				while ((currentLine = readerObj.readLine()) != null) {
					userData.append(currentLine);
				}
				readerObj.close();
				userObj = new JSONObject(userData.toString());
				clientID = userObj.optString("client_id").toString();
				countryCode = userObj.optString("country_code").toString();
				deviceID = userObj.optString("device_id").toString();
				readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/client_functional_access_package" + "/" + clientID + "/" + countryCode + "/client_functional_access.xml")));
				dbfObj = DocumentBuilderFactory.newInstance();
				dbObj = dbfObj.newDocumentBuilder();
				docObj = dbObj.parse(new InputSource(readerObj));
				docObj.getDocumentElement().normalize();
				requesturl = docObj.getElementsByTagName("protocol_type").item(0).getTextContent() + "//" + docObj.getElementsByTagName("domain_name").item(0).getTextContent() + ":" + docObj.getElementsByTagName("port_no").item(0).getTextContent() + "/common/components/GeoLocation/update_device_location_offline.aspx";

				/* SEND LOCATION  */
				requestPath = new URL(requesturl);
				urlConObj = (HttpURLConnection)requestPath.openConnection();
				urlConObj.setDoOutput(true);
				urlConObj.setRequestMethod("POST");
				urlConObj.setRequestProperty("CONTENT-TYPE", "text/xml");
				urlConObj.connect();
				oStreamObj = new OutputStreamWriter(urlConObj.getOutputStream());
				oStreamObj.write("<location_xml><client_id>" + clientID + "</client_id><country_code>" + countryCode + "</country_code><device_id>" + deviceID + "</device_id><location>" + serverData + "</location></location_xml>");
				oStreamObj.flush();
				oStreamObj.close();
				urlConObj.getResponseCode();
				urlConObj.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	private class UpdateLocation extends AsyncTask < String,
			Void,
			String > {
		private String objLat,
				objLon;

		public UpdateLocation(String lat, String lon) {
			this.objLat = lat;
			this.objLon = lon;
		}

		@ Override
		protected String doInBackground(String...urls) {
			File appDirectory,
					baseDirectory = Environment.getExternalStorageDirectory();
			FileWriter fileWriterObj;
			appDirectory = new File(baseDirectory.getAbsolutePath() + "/mservice");
			if (appDirectory.exists()) {
				try {
					fileWriterObj = new FileWriter(new File(baseDirectory, "mservice/MyLocation.txt"), true);
					fileWriterObj.write(this.objLat + "," + this.objLon + "," + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "\n");
					fileWriterObj.flush();
					fileWriterObj.close();

					fileWriterObj = new FileWriter(new File(baseDirectory, "mservice/LastKnownLocation.txt"), false);
					fileWriterObj.write(this.objLat + "," + this.objLon + "," + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
					fileWriterObj.flush();
					fileWriterObj.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	private class DespatchQueue extends AsyncTask < String,
			Void,
			String > {
		@ Override
		protected String doInBackground(String...params) {
			String currentRequest = null,
					currentLine,
					sendData,
					fileType,
					sendFileName,
					requestFilepath,
					sendFileBasePath,
					method,
					keyValue,
					subKeyValue,
					requesturl = "",
					receiveData = "";
			StringBuilder queueData,
					serverResponseObj,
					backupFileData;
			BufferedReader readerObj;
			BufferedWriter writerObj;
			File responseFileName,
					backUpFilePath,
					appDirectory,
					baseDirectory = Environment.getExternalStorageDirectory();
			FileInputStream fileInputStream;
			int bytesRead,
					bytesAvailable,
					bufferSize;
			byte[]buffer;
			int maxBufferSize = 1 * 1024 * 1024;
			DataOutputStream dos;
			String lineEnd = "\r\n";
			String twoHyphens = "--";
			String boundary = "*****";
			JSONObject queueObject,
					backupDataObj;
			URL requestPath;
			HttpURLConnection urlConObj;
			OutputStreamWriter oStreamObj;
			FileWriter fileWriterObj;

			try {
				/* FETCH CURRENT REQUEST FROM QUEUE MANAGER */
				if (new File(baseDirectory, "mservice/database/queue_mgr.txt").exists()) {
					readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/database/queue_mgr.txt")));
					while ((currentLine = readerObj.readLine()) != null) {
						if (currentRequest == null) {
							currentRequest = currentLine;
						}
					}
					readerObj.close();

					if (isConnected() && currentRequest != null) {
						/* FROM REQUEST OBJECT */
						queueObject = new JSONObject(currentRequest);
						requesturl = queueObject.optString("url").toString();
						sendData = queueObject.optString("input").toString();
						fileType = queueObject.optString("type").toString();
						sendFileBasePath = queueObject.optString("filepath").toString();
						sendFileName = queueObject.optString("filename").toString();
						method = queueObject.optString("method").toString();
						keyValue = queueObject.optString("key").toString();
						subKeyValue = queueObject.optString("subkey").toString();

						/* UPLOAD FILE TO SERVER */
						if (method.equals("read")) {
							backUpFilePath = new File(baseDirectory.getAbsolutePath() + "/mservice/database/" + "bckp_" + keyValue + ".txt");
							requestPath = new URL(requesturl);
							urlConObj = (HttpURLConnection)requestPath.openConnection();
							urlConObj.setDoOutput(true);
							urlConObj.setRequestMethod("POST");
							urlConObj.setRequestProperty("CONTENT-TYPE", "application/json");
							urlConObj.connect();
							oStreamObj = new OutputStreamWriter(urlConObj.getOutputStream());
							oStreamObj.write(sendData);
							oStreamObj.flush();
							oStreamObj.close();

							/* GET RESPONSE FROM SERVER*/
							serverResponseObj = new StringBuilder();
							readerObj = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
							while ((currentLine = readerObj.readLine()) != null) {
								serverResponseObj.append(currentLine);
							}
							readerObj.close();
							urlConObj.disconnect();
							backupFileData = new StringBuilder();
							try {
								if (backUpFilePath.exists()) {
									readerObj = new BufferedReader(new FileReader(backUpFilePath));
									while ((currentLine = readerObj.readLine()) != null) {
										backupFileData.append(currentLine + "\n");
									}
									readerObj.close();
									backupDataObj = new JSONObject(backupFileData.toString());
								} else {
									backUpFilePath.createNewFile();
									backupDataObj = new JSONObject();
								}
								backupDataObj.put(subKeyValue, new JSONArray(serverResponseObj.toString()));
								writerObj = new BufferedWriter(new FileWriter(backUpFilePath));
								writerObj.write(backupDataObj.toString());
								writerObj.flush();
								writerObj.close();
							} catch (Exception e) {
								e.printStackTrace();
								/*try {
									errorLogData += "Time:" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n";
									errorLogData += "url:" + requesturl + "\n";
									errorLogData += "data:" + e.getMessage() + "\n";
									errorLogData += "------------------\n";
									errorLogDirectory = new File(baseDirectory.getAbsolutePath() + "/mservice/database/Log");
									errorLogFilename = new File(errorLogDirectory, new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".txt");
									if (!errorLogDirectory.exists()) {
										errorLogDirectory.mkdirs();
									}
									errorFileWriterObj = new FileWriter(errorLogFilename, true);
									errorFileWriterObj.write(errorLogData);
									errorFileWriterObj.flush();
									errorFileWriterObj.close();
								} catch (Exception ex) {
									ex.printStackTrace();
								}*/
							}
						} else {
							if (fileType.equals("file")) {
								requestFilepath = baseDirectory + "/" + sendFileBasePath + "/" + sendFileName;
								fileInputStream = new FileInputStream(new File(requestFilepath));
								requestPath = new URL((requesturl + "&filename=" + sendFileName).replaceAll(" ", "%20"));
								urlConObj = (HttpURLConnection)requestPath.openConnection();
								urlConObj.setDoInput(true); // Allow Inputs
								urlConObj.setDoOutput(true); // Allow Outputs
								urlConObj.setUseCaches(false); // Don't use a Cached Copy
								urlConObj.setRequestMethod("POST");
								urlConObj.setRequestProperty("Connection", "Keep-Alive");
								urlConObj.setRequestProperty("ENCTYPE", "multipart/form-data");
								urlConObj.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
								urlConObj.setRequestProperty("uploaded_file", sendFileName);

								dos = new DataOutputStream(urlConObj.getOutputStream());
								dos.writeBytes(twoHyphens + boundary + lineEnd);
								dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
										+ sendFileName + "\"" + lineEnd);
								dos.writeBytes(lineEnd);
								// create a buffer of  maximum size
								bytesAvailable = fileInputStream.available();

								bufferSize = Math.min(bytesAvailable, maxBufferSize);
								buffer = new byte[bufferSize];

								// read file and write it into form...
								bytesRead = fileInputStream.read(buffer, 0, bufferSize);
								while (bytesRead > 0) {
									dos.write(buffer, 0, bufferSize);
									bytesAvailable = fileInputStream.available();
									bufferSize = Math.min(bytesAvailable, maxBufferSize);
									bytesRead = fileInputStream.read(buffer, 0, bufferSize);

								}
								// send multipart form data necesssary after file data...
								dos.writeBytes(lineEnd);
								dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
								urlConObj.getResponseCode();
								//close the streams //
								fileInputStream.close();
								dos.flush();
								dos.close();
								serverResponseObj = new StringBuilder();
								readerObj = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
								while ((currentLine = readerObj.readLine()) != null) {
									serverResponseObj.append(currentLine + "\n");
								}
								readerObj.close();
								receiveData += "Time:" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n";
								receiveData += "url:" + requestPath + "\n";
								receiveData += "------------------\n";
								urlConObj.disconnect();
							} else {
								/* SEND JSON DATA TO SERVER*/
								requestPath = new URL(requesturl);
								urlConObj = (HttpURLConnection)requestPath.openConnection();
								urlConObj.setDoOutput(true);
								urlConObj.setRequestMethod("POST");
								urlConObj.setRequestProperty("CONTENT-TYPE", "application/json");
								urlConObj.connect();
								oStreamObj = new OutputStreamWriter(urlConObj.getOutputStream());
								oStreamObj.write(sendData);
								oStreamObj.flush();
								oStreamObj.close();

								/* GET RESPONSE FROM SERVER*/
								serverResponseObj = new StringBuilder();
								readerObj = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
								while ((currentLine = readerObj.readLine()) != null) {
									serverResponseObj.append(currentLine + "\n");
								}
								readerObj.close();
								receiveData += "Time:" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n";
								receiveData += "url:" + requesturl + "\n";
								receiveData += "data:" + sendData + "\n";
								receiveData += "response:" + serverResponseObj.toString() + "\n";
								receiveData += "------------------\n";
								urlConObj.disconnect();
							}
						}

						/* WRITE RESPONSE DATA TO INTERNAL STORAGE */
						appDirectory = new File(baseDirectory.getAbsolutePath() + "/mservice/database/process");
						responseFileName = new File(appDirectory, new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".txt");
						if (!appDirectory.exists()) {
							appDirectory.mkdirs();
						}
						fileWriterObj = new FileWriter(responseFileName, true);
						fileWriterObj.write(receiveData);
						fileWriterObj.flush();
						fileWriterObj.close();

						currentRequest = null;
						queueData = new StringBuilder();
						readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/database/queue_mgr.txt")));
						while ((currentLine = readerObj.readLine()) != null) {
							if (currentRequest == null) {
								currentRequest = currentLine;
							} else {
								queueData.append(currentLine + "\n");
							}
						}
						readerObj.close();

						writerObj = new BufferedWriter(new FileWriter(new File(baseDirectory, "mservice/database/queue_mgr.txt")));
						writerObj.write(queueData.toString());
						writerObj.flush();
						writerObj.close();

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	@ Override
	public void onDestroy() {
		super.onDestroy();
		if (setChecksumTimerInterval != null) {
			setChecksumTimerInterval.cancel();
		}

		if (setQueueInterval != null) {
			setQueueInterval.cancel();
		}

		if (setTimerIntervel != null) {
			setTimerIntervel.cancel();
		}

		startService(new Intent(getApplicationContext(), mInterfaceService.class));
	}
	@ Override
	public void onLowMemory() {
		super.onLowMemory();
		if (setChecksumTimerInterval != null) {
			setChecksumTimerInterval.cancel();
		}

		if (setQueueInterval != null) {
			setQueueInterval.cancel();
		}

		if (setTimerIntervel != null) {
			setTimerIntervel.cancel();
		}

		startService(new Intent(getApplicationContext(), mInterfaceService.class));
	}
	@ Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
		if (setChecksumTimerInterval != null) {
			setChecksumTimerInterval.cancel();
		}

		if (setQueueInterval != null) {
			setQueueInterval.cancel();
		}

		if (setTimerIntervel != null) {
			setTimerIntervel.cancel();
		}

		Intent restartService = new Intent(getApplicationContext(),
				this.getClass());
		restartService.setPackage(getPackageName());
		PendingIntent restartServicePI = PendingIntent.getService(
				getApplicationContext(), 1, restartService,
				PendingIntent.FLAG_ONE_SHOT);
		AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000, restartServicePI);
	}
	public boolean isConnected() {
		ConnectivityManager online = (ConnectivityManager)getSystemService(this.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = online.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}
}
