package com.example.covid_symptoms_tracker;

import androidx.room.TypeConverter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeConversion {

    static DateFormat df;

    static {
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        df.setTimeZone(TimeZone.getTimeZone("America/Phoenix"));
    }

    @TypeConverter
    public static String fromTimestamp(Date value) {
        if (!(value == null)) return df.format(value);
        else return null;
    }

    @TypeConverter
    public static Date dateToTimestamp(String value) {
        Date result = null;
        if (value == null) {
            return result;
        }
        try {
            result = df.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }
}
