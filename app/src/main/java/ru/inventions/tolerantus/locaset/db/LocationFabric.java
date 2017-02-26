package ru.inventions.tolerantus.locaset.db;

/**
 * Created by Aleksandr on 19.02.2017.
 */

public class LocationFabric {
    public static Location createLocation() {
        Location location = new Location();
        location.setName("New location");
        location.setRadius(50);
        return location;
    }
}
