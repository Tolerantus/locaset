package ru.inventions.tolerantus.locaset.service;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class AudioPreferences {

    private long preferenceId;
    private double volume;
    private boolean vibro;



    public long getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(long preferenceId) {
        this.preferenceId = preferenceId;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public boolean isVibro() {
        return vibro;
    }

    public void setVibro(boolean vibro) {
        this.vibro = vibro;
    }

    @Override
    public String toString() {
        return "AudioPreferences{" +
                "preferenceId=" + preferenceId +
                ", volume=" + volume +
                ", vibro=" + vibro +
                '}';
    }
}
