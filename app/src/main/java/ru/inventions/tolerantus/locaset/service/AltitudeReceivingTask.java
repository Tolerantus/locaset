package ru.inventions.tolerantus.locaset.service;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class AltitudeReceivingTask extends AsyncTask<Void, Double, Void> {

    private Dao dao;
    private long locationId;
    private String locationName;
    private double latitude;
    private double longitude;
    private int volume;
    private Activity activity;

    public AltitudeReceivingTask(Activity activity, Dao dao, long locationId, String locationName, double latitude, double longitude, int volume) {
        this.activity = activity;
        this.dao = dao;
        this.locationId = locationId;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.volume = volume;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        activity.findViewById(R.id.pb_saving).setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(Void... doubles) {
        double altitude = getAltitude(latitude, longitude);
        publishProgress(altitude);
        return null;
    }

    @Override
    protected void onProgressUpdate(Double... values) {
        super.onProgressUpdate(values);
        dao.updateLocation(locationId, locationName, latitude, longitude, values[0], volume);
        Toast.makeText(activity, "Location was successfully saved", Toast.LENGTH_SHORT).show();
    }

    private double getAltitude(double latitude, double longitude) {
        Log.v(this.getClass().getSimpleName(), "Looking up net altitude");
        double result = 0;
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
        HttpConnectionParams.setSoTimeout(httpParameters, 5000);

        HttpClient httpClient = new DefaultHttpClient(httpParameters);
        HttpContext localContext = new BasicHttpContext();
        String url = "http://maps.googleapis.com/maps/api/elevation/" + "xml?locations="
                + latitude + "," + longitude + "&sensor=true";
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet, localContext);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = instream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<elevation>";
                String tagClose = "</elevation>";
                if (respStr.indexOf(tagOpen) != -1) {
                    int start = respStr.indexOf(tagOpen) + tagOpen.length();
                    int end = respStr.indexOf(tagClose);
                    String value = respStr.substring(start, end);
                    result = (double) (Double.parseDouble(value));

                }
                instream.close();
            }
        } catch (ClientProtocolException e) {
            Log.w(this.getClass().getSimpleName(), "Looking up net altitude ClientProtocolException", e);
        } catch (IOException e) {
            Log.w(this.getClass().getSimpleName(), "Looking up net altitude IOException", e);
        }

        Log.i(this.getClass().getSimpleName(), "got net altitude " + (int) result);
        return result;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(this.getClass().getSimpleName(), "AltitudeReceivingTask finished her work");
        activity.findViewById(R.id.pb_saving).setVisibility(View.INVISIBLE);
        super.onPostExecute(aVoid);
    }
}
