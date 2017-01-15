package ru.inventions.tolerantus.locaset.service;

/**
 * Created by Aleksandr on 13.01.2017.
 */

public class AudioPreferences {

    private int volume;
    private boolean vibro;

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public boolean isVibro() {
        return vibro;
    }

    public void setVibro(boolean vibro) {
        this.vibro = vibro;
    }
}
