package ru.inventions.tolerantus.locaset.service;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.db.Dao;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class LocationPreferencesSavingTask extends AsyncTask<Void, Double, Void> {

    private Dao dao;
    private long locationId;
    private double latitude;
    private double longitude;
    private double altitude;
    private Activity activity;

    public LocationPreferencesSavingTask(Activity activity, Dao dao, long locationId, double latitude, double longitude) {
        this.activity = activity;
        this.dao = dao;
        this.locationId = locationId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... doubles) {
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "http://maps.googleapis.com/maps/api/elevation/" + "xml?locations="
                + latitude + "," + longitude + "&sensor=true";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(this.getClass().getSimpleName(), "Response was received, starting parsing");
                        publishProgress(parseXmlResponse(response));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(this.getClass().getSimpleName(), "Error occurred during elevation calculation! ",error);
                publishProgress(0d);
            }
        });
        queue.add(stringRequest);
        return null;
    }

    private double parseXmlResponse(String xmlResponse) {
        double elevation = 0d;
        try {
            Log.d(this.getClass().getSimpleName(), "Start parsing XML response for elevation");
            XmlPullParser xpp = prepareXpp(xmlResponse);
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    case XmlPullParser.START_TAG:
                        if (xpp.getName().equals(activity.getString(R.string.elavation_tag))) {
                            elevation = Double.parseDouble(xpp.nextText());
                        }
                        break;
                    default:
                        break;
                }
                xpp.next();
            }
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error during parsing XML response from http://maps.googleapis.com/maps/api/elevation/", e);
        }
        Log.d(this.getClass().getSimpleName(), "elevation = " + elevation);
        return elevation;
    }

    private XmlPullParser prepareXpp(String input) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(input));
        return xpp;
    }

    @Override
    protected void onProgressUpdate(Double... values) {
        super.onProgressUpdate(values);
        altitude = values[0];
        dao.updateLocation(locationId, latitude, longitude, altitude);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(this.getClass().getSimpleName(), "LocationPreferencesSavingTask finished her work");
        super.onPostExecute(aVoid);
    }
}
