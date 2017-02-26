package ru.inventions.tolerantus.locaset.service.media;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class AudioPreferences {

    private long preferenceId;
    private String locationName;
    private double ringtoneVolume;
    private double musicVolume;
    private double notificationVolume;
    private boolean vibro;



    public long getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(long preferenceId) {
        this.preferenceId = preferenceId;
    }

    public double getRingtoneVolume() {
        return ringtoneVolume;
    }

    public void setRingtoneVolume(double ringtoneVolume) {
        this.ringtoneVolume = ringtoneVolume;
    }

    public boolean isVibro() {
        return vibro;
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(double musicVolume) {
        this.musicVolume = musicVolume;
    }

    public double getNotificationVolume() {
        return notificationVolume;
    }

    public void setNotificationVolume(double notificationVolume) {
        this.notificationVolume = notificationVolume;
    }

    public void setVibro(boolean vibro) {
        this.vibro = vibro;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    @Override
    public String toString() {
        return "AudioPreferences{" +
                "locationName='" + locationName + '\'' +
                ", preferenceId=" + preferenceId +
                ", ringtoneVolume=" + ringtoneVolume +
                ", musicVolume=" + musicVolume +
                ", notificationVolume=" + notificationVolume +
                ", vibro=" + vibro +
                '}';
    }
}
