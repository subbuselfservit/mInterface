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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.io.BufferedReader;
import java.io.File;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class mInterfaceService extends Service {
    LocationManager locationManager;
    LocationListener locationListener;

    @ Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @ Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long timeInterval = 15000;
        Timer timerObj = new Timer();
        TimerTask timerTaskObj = new TimerTask() {
            public void run() {
                if (isConnected()) {
                    new DespatchQueue().execute();
                }
                readTimer();
            }
        };
        timerObj.schedule(timerTaskObj, 0, 15000);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener(locationManager);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, locationListener);
        return START_STICKY;
    }
    private void readTimer() {
        StringBuilder serverTimeFile = new StringBuilder();
        String line,
                serverTimeSync;
        try {
            BufferedReader readerServerTime = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(), "mservice/time_profile.txt")));
            while ((line = readerServerTime.readLine()) != null) {
                serverTimeFile.append(line);
            }
            serverTimeSync = serverTimeFile.toString();
            SimpleDateFormat serverTime = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss");
            Calendar c = Calendar.getInstance();
            c.setTime(serverTime.parse(serverTimeSync));
            c.add(Calendar.MILLISECOND, 15000);
            serverTimeSync = serverTime.format(c.getTime());
            readerServerTime.close();
            BufferedWriter writeServerTime = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "mservice/time_profile.txt")));
            writeServerTime.write(serverTimeSync);
            writeServerTime.flush();
            writeServerTime.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
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
            StringBuilder locationRequest,
                    userProfile;
            BufferedReader readerObj;
            BufferedWriter writerObj;
            String line,
                    clientID,
                    countryCode,
                    deviceID,
                    url;
            JSONObject userInfo;
            DocumentBuilderFactory dbfObj;
            DocumentBuilder dbObj;
            Document docObj;
            URL requestPath;
            HttpURLConnection urlConObj;
            OutputStreamWriter oStreamObj;

            try {
				/* GETTING THE LOCATION POINTS TO BE SENT TO THE SERVER */
                locationRequest = new StringBuilder();
                readerObj = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(), "mservice/MyLocation.txt")));
                while ((line = readerObj.readLine()) != null) {
                    locationRequest.append(line + "\n");
                }
                readerObj.close();

				/* CLEARING THE LOCATION POINTS */
                writerObj = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "mservice/MyLocation.txt")));
                writerObj.write("");
                writerObj.flush();
                writerObj.close();

				/* GETTING THE USER INFO */
                userProfile = new StringBuilder();
                readerObj = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(), "mservice/user.txt")));
                while ((line = readerObj.readLine()) != null) {
                    userProfile.append(line);
                }
                readerObj.close();
                userInfo = new JSONObject(userProfile.toString());
                clientID = userInfo.optString("client_id").toString();
                countryCode = userInfo.optString("country_code").toString();
                deviceID = userInfo.optString("device_id").toString();
                readerObj = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(), "mservice/client_functional_access_package" + "/" + clientID + "/" + countryCode + "/client_functional_access.xml")));
                dbfObj = DocumentBuilderFactory.newInstance();
                dbObj = dbfObj.newDocumentBuilder();
                docObj = dbObj.parse(new InputSource(readerObj));
                docObj.getDocumentElement().normalize();
                url = docObj.getElementsByTagName("protocol_type").item(0).getTextContent() + "//" + docObj.getElementsByTagName("domain_name").item(0).getTextContent() + ":" + docObj.getElementsByTagName("port_no").item(0).getTextContent() + "/common/components/GeoLocation/update_device_location_offline.aspx";

				/* SEND LOCATION  */
                requestPath = new URL(url);
                urlConObj = (HttpURLConnection)requestPath.openConnection();
                urlConObj.setDoOutput(true);
                urlConObj.setRequestMethod("POST");
                urlConObj.setRequestProperty("CONTENT-TYPE", "text/xml");
                urlConObj.connect();
                oStreamObj = new OutputStreamWriter(urlConObj.getOutputStream());
                oStreamObj.write("<location_xml><client_id>" + clientID + "</client_id><country_code>" + countryCode + "</country_code><device_id>" + deviceID + "</device_id><location>" + locationRequest.toString() + "</location></location_xml>");
                oStreamObj.flush();
                oStreamObj.close();
                urlConObj.getResponseCode();
                urlConObj.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            catch (SAXException e) {
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
            File baseDirectory,
                    appDirectory;
            FileWriter writerObj;

            baseDirectory = Environment.getExternalStorageDirectory();
            appDirectory = new File(baseDirectory.getAbsolutePath() + "/mservice");
            if (appDirectory.exists()) {
                try {
                    writerObj = new FileWriter(new File(baseDirectory, "mservice/MyLocation.txt"), true);
                    writerObj.write(this.objLat + "," + this.objLon + "," + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "\n");
                    writerObj.close();
                } catch (IOException e) {
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
            StringBuilder queueRequest,
                    queueResponse;
            BufferedReader readerObj,
                    readerresponseObj;
            BufferedWriter writerObj;
            JSONArray queueList;
            JSONObject queueObj;
            String line,
                    requesturl,
                    sendData,
                    responseline,
                    fileType,
                    sendFileName,
                    requestfilepath,
                    sendFileBasePath,
                    receiveData = "";
            int bytesRead,
                    bytesAvailable,
                    bufferSize;
            byte[]buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            URL requestPath;
            HttpURLConnection urlConObj;
            OutputStreamWriter oStreamObj;
            File baseDirectory,
                    appDirectory,
                    fileName;
            FileWriter responseObj;
            FileInputStream fileInputStream;
            DataOutputStream dos;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

			/* READING A QUEUE_MGR FILE FROM INTERNAL STORAGE */
            try {
                queueRequest = new StringBuilder();
                baseDirectory = Environment.getExternalStorageDirectory();
                readerObj = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(), "mservice/database/queue_mgr.txt")));
                while ((line = readerObj.readLine()) != null) {
                    queueRequest.append(line);
                }
                readerObj.close();

				/* CNVERT A QUEUE_MGR FILE DATA INTO JSONARRAY */
                queueList = new JSONArray(queueRequest.toString());
                writerObj = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "mservice/database/queue_mgr.txt")));
                writerObj.write("[]");
                writerObj.flush();
                writerObj.close();
                for (int i = 0; i < queueList.length(); i++) {
                    queueObj = queueList.getJSONObject(i);
                    requesturl = queueObj.optString("url").toString();
                    sendData = queueObj.optString("data").toString();
                    fileType = queueObj.optString("type").toString();
                    sendFileBasePath = queueObj.optString("filepath").toString();
                    sendFileName = queueObj.optString("filename").toString();

					/* UPLOAD FILE TO SERVER */
                    if (fileType.equals("file")) {
                        requestfilepath = baseDirectory + "/" + sendFileBasePath + "/" + sendFileName;

                        fileInputStream = new FileInputStream(new File(requestfilepath));
                        requestPath = new URL(requesturl + "&filename=" + sendFileName);
                        // Open a HTTP  connection to  the URL
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
                        queueResponse = new StringBuilder();
                        readerresponseObj = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
                        while ((responseline = readerresponseObj.readLine()) != null) {
                            queueResponse.append(responseline + "\n");
                        }
                        receiveData += "Time:" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n";
                        receiveData += "url:" + requestPath + "\n";
                        receiveData += "------------------\n";
                        readerresponseObj.close();
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
                        queueResponse = new StringBuilder();
                        readerresponseObj = new BufferedReader(new InputStreamReader(urlConObj.getInputStream()));
                        while ((responseline = readerresponseObj.readLine()) != null) {
                            queueResponse.append(responseline + "\n");
                        }
                        receiveData += "Time:" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n";
                        receiveData += "url:" + requesturl + "\n";
                        receiveData += "data:" + sendData + "\n";
                        receiveData += "response:" + queueResponse.toString() + "\n";
                        receiveData += "------------------\n";
                        readerresponseObj.close();
                        urlConObj.disconnect();
                    }
                }

				/* WRITE RESPONSE DATA TO INTERNAL STORAGE */
                baseDirectory = Environment.getExternalStorageDirectory();
                appDirectory = new File(baseDirectory.getAbsolutePath() + "/mservice/database/process");
                fileName = new File(appDirectory, new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".txt");
                if (appDirectory.exists()) {
                    try {
                        responseObj = new FileWriter(fileName, true);
                        responseObj.write(receiveData);
                        responseObj.flush();
                        responseObj.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    appDirectory.mkdir();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }
}


