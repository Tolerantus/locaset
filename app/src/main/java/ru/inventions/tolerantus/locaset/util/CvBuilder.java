package ru.inventions.tolerantus.locaset.util;

import android.content.ContentValues;

/**
 * Created by Aleksandr on 07.02.2017.
 */

public class CvBuilder {

    private ContentValues contentValues;

    private CvBuilder() {
        this.contentValues = new ContentValues();
    }

    public static CvBuilder create() {
        return new CvBuilder();
    }

    public CvBuilder append(String description, Object object) {
        if (object instanceof String) {
            contentValues.put(description, (String) object);
        } else if (object instanceof Integer) {
            contentValues.put(description, (Integer) object);
        } else if (object instanceof Byte) {
            contentValues.put(description, (Byte) object);
        } else if (object instanceof Double) {
            contentValues.put(description, (Double) object);
        } else if (object instanceof Float) {
            contentValues.put(description, (Float) object);
        } else if (object instanceof Long) {
            contentValues.put(description, (Long) object);
        } else if (object instanceof Boolean) {
            contentValues.put(description, (Boolean) object);
        } else if (object instanceof Short) {
            contentValues.put(description, (Short) object);
        }
        return this;
    }

    public ContentValues get() {
        return contentValues;
    }
}
