package ru.inventions.tolerantus.locaset.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Aleksandr on 15.02.2017.
 */

@DatabaseTable(tableName = "location")
public class Location {

    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField
    private String name;

    @DatabaseField
    private double latitude;

    @DatabaseField
    private double longitude;

    @DatabaseField
    private double altitude;

    @DatabaseField
    private String address;

    @DatabaseField
    private float ringtoneVol;

    @DatabaseField
    private float musicVol;

    @DatabaseField
    private float notificationVol;

    @DatabaseField
    private boolean vibro;

    @DatabaseField
    private double radius;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public float getRingtoneVol() {
        return ringtoneVol;
    }

    public void setRingtoneVol(float ringtoneVol) {
        this.ringtoneVol = ringtoneVol;
    }

    public float getMusicVol() {
        return musicVol;
    }

    public void setMusicVol(float musicVol) {
        this.musicVol = musicVol;
    }

    public float getNotificationVol() {
        return notificationVol;
    }

    public void setNotificationVol(float notificationVol) {
        this.notificationVol = notificationVol;
    }

    public boolean isVibro() {
        return vibro;
    }

    public void setVibro(boolean vibro) {
        this.vibro = vibro;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}
