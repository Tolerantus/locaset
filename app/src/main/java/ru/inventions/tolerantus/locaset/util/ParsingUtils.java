package ru.inventions.tolerantus.locaset.util;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import ru.inventions.tolerantus.locaset.R;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;

/**
 * Created by Aleksandr on 07.02.2017.
 */

public class ParsingUtils {

    public static double parseXmlResponseElevation(String xmlResponse, Context context) {
        double elevation = 0d;
        try {
            debug("Start parsing XML response for elevation");
            XmlPullParser xpp = prepareXpp(xmlResponse);
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    case XmlPullParser.START_TAG:
                        if (xpp.getName().equals(context.getString(R.string.elavation_tag))) {
                            elevation = Double.parseDouble(xpp.nextText());
                        }
                        break;
                    default:
                        break;
                }
                xpp.next();
            }
        } catch (Exception e) {
            error("Error during parsing XML response from http://maps.googleapis.com/maps/api/elevation/");
        }
        debug("elevation = " + elevation);
        return elevation;
    }

    private static XmlPullParser prepareXpp(String input) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(input));
        return xpp;
    }

}
