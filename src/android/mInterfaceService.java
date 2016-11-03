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
import android.widget.Toast;

/*import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;*/
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class mInterfaceService extends Service {
    public mInterfaceService() {
//constructor
    }

    @Override
    public IBinder onBind(Intent intent) {
// TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timer timer=new Timer();
        TimerTask timerTask=new TimerTask() {
            public void run() {
                if(isConnected()) {
                    new PostJson().execute();
                }
            }
        };
        timer.schedule(timerTask,0,10000);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener mlocListener = new MyLocationListener(locationManager);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER , 30000, 0, mlocListener);
        return START_STICKY;
    }

    private class MyLocationListener implements LocationListener {
        public MyLocationListener(LocationManager locationManager) {
//constructor
        }

        @Override
        public void onLocationChanged(Location location) {
            new WriteGpsData(Double.toString(location.getLatitude()), Double.toString(location.getLongitude())).execute(" ");
//new PostGpsData(Double.toString(location.getLatitude()), Double.toString(location.getLongitude())).execute(" ");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
//Called when the provider status changes
        }

        @Override
        public void onProviderEnabled(String provider) {
//Called when the provider is enabled by the user
        }

        @Override
        public void onProviderDisabled(String provider) {
//Called when the provider is disabled by the user
        }
    }

/*private class PostGpsData extends AsyncTask<String, Void, String> {

private String objLon;
private String objLat;

public PostGpsData(String lat, String lon) {
this.objLon = lon;
this.objLat = lat;
}

@Override
protected String doInBackground(String... urls) {
File baseDirectory = Environment.getExternalStorageDirectory();
BufferedReader fileReader;
StringBuilder user_profile;
String ClientID = "";
String CountryCode = "";
String DeviceID = "";
try {
user_profile = new StringBuilder();
fileReader = new BufferedReader(new FileReader(new File(baseDirectory,"mservice/user.txt")));
String line;
while ((line = fileReader.readLine()) != null) {
user_profile.append(line);
}
fileReader.close();
if(user_profile.toString() != "") {
JSONArray args = new JSONArray("["+user_profile.toString()+"]");
JSONObject arg_object = args.getJSONObject(0);

ClientID = arg_object.getString("client_id");
CountryCode = arg_object.getString("country_code");
DeviceID = arg_object.getString("device_id");

fileReader = new BufferedReader(new FileReader(new File(baseDirectory,"mservice/client_functional_access_package" + "/" + ClientID + "/" + CountryCode + "/client_functional_access.xml" )));
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
DocumentBuilder db = dbf.newDocumentBuilder();
Document doc = db.parse(new InputSource(fileReader));
doc.getDocumentElement().normalize();

String Protocol = doc.getElementsByTagName("protocol_type").item(0).getTextContent();
String Domain = doc.getElementsByTagName("domain_name").item(0).getTextContent();
String Port = doc.getElementsByTagName("port_no").item(0).getTextContent();
String url = Protocol + "//" + Domain + ":" + Port + "/common/components/GeoLocation/update_device_location.aspx";

HttpClient httpclient = new DefaultHttpClient();
HttpPost httppost = new HttpPost(url);
StringEntity reqloc = new StringEntity("<location_xml><client_id>" + ClientID + "</client_id>" + "<country_code>" + CountryCode + "</country_code>" + "<device_id>" + DeviceID + "</device_id>" + "<latitude>" + this.objLat + "</latitude>" + "<longitude>" + this.objLon + "</longitude>" + "</location_xml>",HTTP.UTF_8);
reqloc.setContentType("text/xml");
httppost.setEntity(reqloc);
HttpResponse response = httpclient.execute(httppost);
HttpEntity httpEntity = response.getEntity();
}
}
catch (IOException e) {
e.printStackTrace();
} catch (JSONException e) {
e.printStackTrace();
} catch (ParserConfigurationException e) {
e.printStackTrace();
} catch (SAXException e) {
e.printStackTrace();
} catch (Exception e) {
e.printStackTrace();
}
return null;
}

@Override
protected void onPostExecute(String result) {
// onPostExecute displays the results of the AsyncTask.
}
}*/

    private class WriteGpsData extends AsyncTask<String, Void, String> {

        private String objLat, objLon;

        public WriteGpsData(String lat, String lon) {
            this.objLat = lat;
            this.objLon = lon;
        }

        @Override
        protected String doInBackground(String... urls) {
            File baseDirectory = Environment.getExternalStorageDirectory();
            File dir = new File (baseDirectory.getAbsolutePath() + "/mservice");
            if(dir.exists()) {
                try {
                    FileWriter out = new FileWriter(new File(baseDirectory, "mservice/MyLocation.txt"), true);
                    out.write(this.objLat + "," + this.objLon + "," + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+"/n");
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
// onPostExecute displays the results of the AsyncTask.
        }
    }
    private class PostJson extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder sb = new StringBuilder();
            BufferedReader br;
            String line;
            String outputfile = "";
            try {
                br = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(), "mservice/database/queue_mgr.txt")));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                JSONArray jsonArray = new JSONArray(sb.toString());
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "mservice/database/queue_mgr.txt")));
                bw.write("[]");
                bw.flush();
                bw.close();
//Iterate the jsonArray and print the info of JSONObjects
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String urlString = jsonObject.optString("url").toString();
                    String data = jsonObject.optString("data").toString();
                    try {
                        URL url = new URL(urlString);
                        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                        httpURLConnection.setDoOutput(true);
                        httpURLConnection.setRequestMethod("POST");
                        httpURLConnection.setRequestProperty("CONTENT-TYPE", "application/json");
                        httpURLConnection.connect();
                        try {
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream());
                            outputStreamWriter.write(data);
                            outputStreamWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
        public boolean isConnected(){
            ConnectivityManager cm=(ConnectivityManager)getSystemService(this.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo=cm.getActiveNetworkInfo();
            if(networkInfo !=null && networkInfo.isConnected()) {
                return true;
            } else {
                return false;
            }
        }
    }
