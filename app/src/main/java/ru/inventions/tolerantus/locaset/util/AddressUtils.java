package ru.inventions.tolerantus.locaset.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static ru.inventions.tolerantus.locaset.util.LogUtils.debug;
import static ru.inventions.tolerantus.locaset.util.LogUtils.error;

/**
 * Created by Aleksandr on 06.02.2017.
 */

public class AddressUtils {

    private static String tag = "AddressUtils";

    public static String getStringAddress(double latitude, double longitude, Geocoder geocoder) {
        debug("Starting getting address for coordinates (" + latitude + ", " + longitude + ")");
        String address = "";
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            error("Something has gone wrong during address recognizing");
        }
        if (addresses != null && !addresses.isEmpty()) {
            address = composeAddress(addresses.get(0));
        }
        if (address.isEmpty()) {
            error("Can't get address");
        }
        return address;
    }

    private static String composeAddress(Address address) {
        String result = "";
        if (address != null) {
            List<String> addressFragments = new ArrayList<>();
            for (int i=0; i<address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            String countryName = address.getCountryName();
            String city = address.getAdminArea();
            String street = address.getThoroughfare();
            String houseNr = address.getSubThoroughfare();
            String postal = address.getPostalCode();
            result = (countryName != null ? countryName + ", " : "") + (city != null ? city + ", " : "") + (street != null ? street + ", " : "") + (houseNr != null ? houseNr + ", " : "") + (postal != null ? postal : "");
        }
        return result;
    }
}
