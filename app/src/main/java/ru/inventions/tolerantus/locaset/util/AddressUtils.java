package ru.inventions.tolerantus.locaset.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Aleksandr on 06.02.2017.
 */

public class AddressUtils {

    public static String getStringAddress(double latitude, double longitude, Geocoder geocoder) {
        String address = "";
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            Log.e(AddressUtils.class.getSimpleName(), "Something has gone wrong during address recognizing", e);
        }
        if (addresses != null && !addresses.isEmpty()) {
            address = composeAddress(addresses.get(0));
        }
        return address;
    }

    private static String composeAddress(Address address) {
        String result = "";
        if (address != null) {
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
