package com.eviahealth.eviahealth.meeting.models;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class zutils {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.getDefault());

    // yyyy-MM-ddTHH:mm:ssZ
    public static String getDateGMT_ZOOM(String fecha){

        SimpleDateFormat formatInt = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        SimpleDateFormat formatOut = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        TimeZone tz = TimeZone.getDefault();
        formatInt.setTimeZone(tz); // establece zona horaria actual

        // Pasamos zona horaria "GMT+0:00"
        formatOut.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

        try {
            Date ndate = formatInt.parse(fecha);
            return formatOut.format(ndate);

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getGMTToDate(String fechaGTM){

        SimpleDateFormat formatInt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        SimpleDateFormat formatOut = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");

        formatInt.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

        TimeZone tz = TimeZone.getDefault();
        formatOut.setTimeZone(tz); // establece zona horaria actual

        try {
            Date ndate = formatInt.parse(fechaGTM);
            return formatOut.format(ndate);

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Covert String to Date
     * @param time (String)
     * @return (Date)
     */
    public static Date toDate(String time){
        try {
            return dateFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Covert Date to String
     * @param fecha (Date)
     * @return (String) "yyyy-MM-dd HH:mm:SS"
     */
    public static String DateToString(Date fecha) {
        return "" + dateFormat.format(fecha);
    }

    /**
     * Suma/Resta minutos a un Date
     * @param fecha (String)
     * @param min
     * @return (Date)
     */
    public static Date addMinutesFecha(String fecha, int min) throws NullPointerException  {
        Date ndate = toDate(fecha);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ndate);                    // Configuramos la fecha que se recibe
        calendar.add(Calendar.MINUTE, min);         // numero minutos a añadir, o restar
        return calendar.getTime();
    }

    /**
     * Suma/Resta minutos a un Date
     * @param time (String)
     * @param min (int)
     * @return (Date)
     */
    public static String addMinutesFechaString(String time, int min){
        Date ndate = toDate(time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ndate);                    // Configuramos la fecha que se recibe
        calendar.add(Calendar.MINUTE, min);         // numero minutos a añadir, o restar
        return DateToString(calendar.getTime());
    }

    /**
     * <p>compareTo() método para comparar dos fechas en la clase Date de Java.</p>
     * <p>Compara dos fechas y devuelve diferentes valores basados en el resultado de la comparación.</p>
     * @param date1 (Date)
     * @param date2 (Date)
     * @return (int)
     * <p>Devuelve 0  si (date1== date2)</p>
     * <p>Devuelve >0 si (date1 > date2)</p>
     * <p>Devuelve <0 si (date1 < date2)</p>
     */
    public static int compareTo(Date date1, Date date2) {
        return date1.compareTo(date2);
    }

    /**
     * @param fecha "2022-01-01 10:10:10";
     * @return "2022-01-01"
     */
    public static String getDay(String fecha){

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date ndate = format.parse(fecha);
            return format2.format(ndate);

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retorna la fecha sin los segundo
     * @param date (String)
     * <p>format "yyyy-MM-dd HH:mm:SS"</p>
     * @return "dd/MM/yyyy HH:mm"
     */
    public static String clearSecond(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        SimpleDateFormat formatOut = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        try {
            Date ndate = format.parse(date);
            return "" + formatOut.format(ndate);

        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getDateNow(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }
}
