package com.selfservit.util;

import android.app.Service; ;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class mInterfaceService extends Service {
	LocationManager locationManager;
	LocationListener locationListener;
	String currentLine,
	requesturl;
	StringBuilder stringBuilder,
	stringObj;
	BufferedReader readerObj;
	BufferedWriter writerObj;
	URL requestPath;
	HttpURLConnection urlConObj;
	JSONObject queueObject;
	FileWriter fileWriterObj;
	OutputStreamWriter oStreamObj;
	File appDirectory,
	baseDirectory = Environment.getExternalStorageDirectory();
	 @ Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	 @ Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Timer setInterval;
		TimerTask setIntervalObj;
		// ****Queue Manager Interval Timer ***** //
		setInterval = new Timer();
		setIntervalObj = new TimerTask() {
			public void run() {
				if (isConnected()) {
					new DespatchQueue().execute();
				}
			}
		};
		setInterval.schedule(setIntervalObj, 0, 1000);

		// **** TimeReader Interval Timer ***** //
		setInterval = new Timer();
		setIntervalObj = new TimerTask() {
			public void run() {
				try {
					timeReader();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		setInterval.schedule(setIntervalObj, 0, 60000);
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener(locationManager);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, locationListener);
		return START_STICKY;
	}
	private void timeReader()throws Exception {
		String serverTimeObj;
		Calendar calender;
		SimpleDateFormat simpleDateFormat;
		stringBuilder = new StringBuilder();
		readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/time_profile.txt")));
		while ((currentLine = readerObj.readLine()) != null) {
			stringBuilder.append(currentLine);
		}
		queueObject = new JSONObject(stringBuilder.toString());
		serverTimeObj = queueObject.optString("serverDate").toString();

		// ******SERVER TIME ******//
		simpleDateFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss");
		calender = Calendar.getInstance();
		calender.setTime(simpleDateFormat.parse(serverTimeObj));
		calender.add(Calendar.MILLISECOND, 60500);
		serverTimeObj = simpleDateFormat.format(calender.getTime());
		queueObject.put("serverDate", serverTimeObj);
		readerObj.close();
		writerObj = new BufferedWriter(new FileWriter(new File(baseDirectory, "mservice/time_profile.txt")));
		writerObj.write(queueObject.toString());
		writerObj.flush();
		writerObj.close();
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

			try {
				/* GETTING THE LOCATION POINTS TO BE SENT TO THE SERVER */
				stringBuilder = new StringBuilder();
				readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/MyLocation.txt")));
				while ((currentLine = readerObj.readLine()) != null) {
					stringBuilder.append(currentLine + "\n");
				}
				readerObj.close();

				/* CLEARING THE LOCATION POINTS */
				writerObj = new BufferedWriter(new FileWriter(new File(baseDirectory, "mservice/MyLocation.txt")));
				writerObj.write("");
				writerObj.flush();
				writerObj.close();

				/* GETTING THE USER INFO */
				stringObj = new StringBuilder();
				readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/user.txt")));
				while ((currentLine = readerObj.readLine()) != null) {
					stringObj.append(currentLine);
				}
				readerObj.close();
				queueObject = new JSONObject(stringObj.toString());
				clientID = queueObject.optString("client_id").toString();
				countryCode = queueObject.optString("country_code").toString();
				deviceID = queueObject.optString("device_id").toString();
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
				oStreamObj.write("<location_xml><client_id>" + clientID + "</client_id><country_code>" + countryCode + "</country_code><device_id>" + deviceID + "</device_id><location>" + stringBuilder.toString() + "</location></location_xml>");
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
			appDirectory = new File(baseDirectory.getAbsolutePath() + "/mservice");
			if (appDirectory.exists()) {
				try {
					fileWriterObj = new FileWriter(new File(baseDirectory, "mservice/MyLocation.txt"), true);
					fileWriterObj.write(this.objLat + "," + this.objLon + "," + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "\n");
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
			sendData,
			fileType,
			sendFileName,
			requestFilepath,
			sendFileBasePath,
			method,
			keyValue,
			subKeyValue,
			receiveData = "";
			File responseFileName,
			backUpFilePath;
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

			try {
				/* FETCH CURRENT REQUEST FROM QUEUE MANAGER */
				stringBuilder = new StringBuilder();
				readerObj = new BufferedReader(new FileReader(new File(baseDirectory, "mservice/database/queue_mgr.txt")));
				while ((currentLine = readerObj.readLine()) != null) {
					if (currentRequest == null) {
						currentRequest = currentLine;
					} else {
						stringBuilder.append(currentLine + "\n");
					}
				}
				readerObj.close();

				if (isConnected() && currentRequest != null) {
					writerObj = new BufferedWriter(new FileWriter(new File(baseDirectory, "mservice/database/queue_mgr.txt")));
					writerObj.write(stringBuilder.toString());
					writerObj.flush();
					writerObj.close();

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
						stringObj = new StringBuilder();
						readerObj = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
						while ((currentLine = readerObj.readLine()) != null) {
							stringObj.append(currentLine + "\n");
						}
						receiveData += "Time:" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n";
						receiveData += "url:" + requesturl + "\n";
						receiveData += "data:" + sendData + "\n";
						receiveData += "response:" + stringObj.toString() + "\n";
						receiveData += "------------------\n";
						readerObj.close();
						urlConObj.disconnect();
						stringBuilder = new StringBuilder();
						if (backUpFilePath.exists()) {
							readerObj = new BufferedReader(new FileReader(backUpFilePath));
							while ((currentLine = readerObj.readLine()) != null) {
								stringBuilder.append(currentLine + "\n");
							}
							readerObj.close();
						} else {
							backUpFilePath.createNewFile();
						}
						queueObject = new JSONObject(stringBuilder.toString());
						queueObject.put(subKeyValue, stringObj.toString());
						writerObj = new BufferedWriter(new FileWriter(backUpFilePath));
						writerObj.write(queueObject.toString());
						writerObj.flush();
						writerObj.close();
					} else {
						if (fileType.equals("file")) {
							requestFilepath = baseDirectory + "/" + sendFileBasePath + "/" + sendFileName;

							fileInputStream = new FileInputStream(new File(requestFilepath));
							requestPath = new URL(requesturl + "&filename=" + sendFileName);
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
							stringObj = new StringBuilder();
							readerObj = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
							while ((currentLine = readerObj.readLine()) != null) {
								stringObj.append(currentLine + "\n");
							}
							receiveData += "Time:" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n";
							receiveData += "url:" + requestPath + "\n";
							receiveData += "------------------\n";
							readerObj.close();
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
							stringObj = new StringBuilder();
							readerObj = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
							while ((currentLine = readerObj.readLine()) != null) {
								stringObj.append(currentLine + "\n");
							}
							receiveData += "Time:" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n";
							receiveData += "url:" + requesturl + "\n";
							receiveData += "data:" + sendData + "\n";
							receiveData += "response:" + stringObj.toString() + "\n";
							receiveData += "------------------\n";
							readerObj.close();
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
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
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
