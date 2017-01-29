package ru.inventions.tolerantus.locaset.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import java.text.AttributedCharacterIterator;

import ru.inventions.tolerantus.locaset.R;
import ru.inventions.tolerantus.locaset.service.MediaService;

/**
 * Created by Aleksandr on 23.01.2017.
 */

public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener{

    private SeekBar mSeekBar;
    private int mProgress;
    private int mMax;


    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SeekBarPreference(Context context) {
        super(context);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        for (int i=0; i<attrs.getAttributeCount(); i++) {
            if (attrs.getAttributeName(i).equals("key")) {
                setKey(attrs.getAttributeValue(i));
            }
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LayoutInflater inflater = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        View view = inflater.inflate(R.layout.preference_widget_seekbar, null);
        mSeekBar = ((SeekBar) view.findViewById(R.id.seek_bar));
        mSeekBar.setOnSeekBarChangeListener(this);
        mProgress = getPersistedInt(mProgress);
        mMax = ((AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE)).getStreamMaxVolume(AudioManager.STREAM_RING);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mProgress);
        return view;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        Log.d(this.getClass().getSimpleName(), "progress = " + i);
        setValue(i);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
        }

        if (value != mProgress) {
            mProgress = value;
            mSeekBar.setProgress(mProgress);
        }
    }
}
