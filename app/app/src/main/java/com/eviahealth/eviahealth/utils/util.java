package com.eviahealth.eviahealth.utils;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.util.Log;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

public class util {

    //region :: Utils Bytes[]
    public static int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    public static int byteTointnegative(byte value){
        int valor = (int)value;
        byte t = value;
        if ((t & 7) == 1){ valor = valor -256; }
        return valor;
    }

    public static byte[] fromHexString(String hexString) {
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    public static byte[] toHex2(String text) {
        try {
            int value = parseInt(text);
            int MSO = value / 256;
            int LSO = value % 256;

            byte[] myBytes = new byte[2];
            myBytes[0] = (byte)MSO;
            myBytes[1] = (byte)LSO;
            return myBytes;
        }
        catch (Exception e){
            return null;
        }
    }

    public static byte[] intToBytes( final int valor ) {
        BigInteger bigInt = BigInteger.valueOf(valor);
        return bigInt.toByteArray();
    }

    public static byte[] longToBytes( final long valor ) {
        BigInteger bigInt = BigInteger.valueOf(valor);
        return bigInt.toByteArray();
    }

    public static int BytesToInt(byte[] intBytes){
        int value = 0;
        for (byte b : intBytes) {
            value = (value << 8) + (b & 0xFF);
        }
        return value;
    }

    public static byte[] addAll(final byte[] array1, byte[] array2) {
        // Este método devuelve una copia de la matriz original, truncada o rellenada con ceros para
        // obtener la longitud especificada.
        int startbyte = array1.length;
        byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);

        for (int i=0; i < array2.length; i++){
            joinedArray[startbyte+i] = array2[i];
        }

        return joinedArray;
    }

    public static byte[] splitBytes(byte[] input, int startbyte, int endbyte) {

        ByteBuffer bb = ByteBuffer.wrap(input);
        int nbytes = endbyte-startbyte;
        byte[] newarray = new byte[nbytes];
        bb.position(startbyte);
        bb.get(newarray, 0, nbytes);

        return newarray;
    }

    public static byte[] bytesToString(String string) {
        byte[] stringBytes = new byte[0];
        try {
            stringBytes = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("UTILS", "Failed to convert message string to byte array");
        }

        return stringBytes;
    }

    /**
     * Convert String to Array Byte
     * @param data - String values type "8763C935EF1063C95E7564"
     * @return Bytes[]
     */
    public static byte[] toArrayByte(String data) {
        byte[] buffer = new byte[data.length() / 2];
        int pos = 0;
        for (int i=0; i<=data.length()-1;i+=2) {
            String result = data.substring(i,i+2);
            int hexInt = Integer.parseInt(result, 16);
            buffer[pos] = (byte)hexInt;
            pos++;
        }
        return buffer;
    }

    // Obtiene el string ContentData usado en la MAMBO6 para generar clases
    public static String getContentData(byte[] array) {
        String res = "";
        for (byte b: array) { res += byteToHex(b).toUpperCase(); }
        return res;
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
//        return new String(hexDigits);
        return String.format("0x%1$s%2$s", hexDigits[0], hexDigits[1]);
    }

    // Método para verificar si el bit en la posición indicada es 1
    public static boolean verificarBit(int numero, int posicion) {
        // Desplazar 1 a la posición deseada y realizar la operación lógica AND
        int mascara = 1 << posicion;
        return (numero & mascara) != 0;
    }

    public static String byteArrayInHexFormat(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ ");
        for (int i = 0; i < byteArray.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            String hexString = byteToHex(byteArray[i]);
            stringBuilder.append(hexString);
        }
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }
    // endregion

    //region :: Utils Fechas

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static Date toDate(String time){
        try {
            return dateFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Date toDate(long utc){
        try {
            return new Date(utc*1000L);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String toStrDateTime(long utc){
        try {
            return dateFormat.format(new Date(utc*1000L));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String toStrTime(long utc){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return dateFormat.format(new Date(utc * 1000L));
        } catch (Exception e) {
            e.printStackTrace();
            return "---";
        }
    }

    public static String toStrDate(long utc){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return dateFormat.format(new Date(utc * 1000L));
        } catch (Exception e) {
            e.printStackTrace();
            return "---";
        }
    }

    public static Date convertDate(String valor) {

        Date ydate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        try {
            ydate = format2.parse("2000-01-01 00:00:00"); // Por si da una exception
            Date ndate = format.parse(valor);
            String tmp = "" + format2.format(ndate);
            Date xdate = format.parse(tmp);
            return xdate;
        } catch (ParseException e) {
            e.printStackTrace();
            return ydate;
        }
    }

    public static Date convertDate(String valor, String formato) {

        Date ydate = new Date();
//        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        SimpleDateFormat format = new SimpleDateFormat(formato);
        try {
            Date ndate = format.parse(valor);
            return ndate;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String DateToString(Date fecha, String formato) {
        SimpleDateFormat format = new SimpleDateFormat(formato);
        return "" + format.format(fecha);
    }

    // Convert date to long value "yyyy-MM-dd HH:mm"
    public static long Timestamp(String valor) {

        long timeunix = 0;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            Date ndate = new Date();
            ndate = format.parse(valor);
            timeunix = ndate.getTime() / 1000L;

            return timeunix;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Convert date to long value "yyyy-MM-dd HH:mm:ss"
    public static long getTimestamp(String valor) {

        long timeunix = 0;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date ndate = new Date();
            ndate = format.parse(valor);
            timeunix = ndate.getTime() / 1000L;

            return timeunix;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String getDate(long time) {
        Date date = new Date(time*1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // the format of your date
//        sdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));

        return sdf.format(date);
    }

    public static String convertTime(int time) {
        String value = "00:00:00";

        Date ydate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        SimpleDateFormat format2 = new SimpleDateFormat("HH:mm:SS");
        try {
            ydate = format.parse("2000-01-01 00:00:00"); // Por si da una exception
            Date ndate = addMinutesFecha(ydate,time);
            value = format2.format(ndate);
            return value;
        } catch (ParseException e) {
            e.printStackTrace();
            return value;
        }
    }

    public static Date addMinutesFecha(Date fecha, int min){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha);                    // Configuramos la fecha que se recibe
        calendar.add(Calendar.MINUTE, min);         // numero minutos a añadir, o restar
        return calendar.getTime();
    }

    public static Date addHourFecha(Date fecha, int hour){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha);                    // Configuramos la fecha que se recibe
        calendar.add(Calendar.HOUR_OF_DAY, hour);         // numero minutos a añadir, o restar
        return calendar.getTime();
    }

    public static Date addDayFecha(Date fecha, int dias){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha);                    // Configuramos la fecha que se recibe
        calendar.add(Calendar.DAY_OF_YEAR, dias);   // numero de dias a añadir, o restar
        return calendar.getTime();
    }

    public static String dosDigitos (int valor){
        String _time = String.format("%02d",valor); //cadena ’00’, '01
        return _time;
    }

    public static Date convertDateHour(String date) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH");
        try {
            Date ndate = format.parse(date);
            String tmp = "" + format2.format(ndate) + ":00:00";
            ndate = format.parse(tmp);
            return ndate;

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Date convertDateHourMinute(String date) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            Date ndate = format.parse(date);
            String tmp = "" + format2.format(ndate) + ":00";
            ndate = format.parse(tmp);
            return ndate;

        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("UTIL", "convertDateHourMinute >> null");
            return null;
        }
    }

    public static String clearSecond(String date) {
        Date fecha = convertDateHourMinute(date);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        return "" + format.format(fecha);
    }

    public static int getMinuto(Date fecha){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha);                    // Configuramos la fecha que se recibe
        return calendar.get(Calendar.MINUTE);
    }

    public static int getHora(Date fecha){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha);                    // Configuramos la fecha que se recibe
        int h = calendar.get(Calendar.HOUR);
        if (calendar.get(Calendar.AM_PM) == Calendar.PM) {
            h += 12;
        }
        return h;
    }

    /**
    * Return Today, format("yyyy-MM-dd")
     */
    public static String getToday(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
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

    public static String getDateNow(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    public static String getCurrentDayOfWeek() {
        String[] days = new String[] {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        Calendar calendar = Calendar.getInstance();
        int dayIndex = calendar.get(Calendar.DAY_OF_WEEK);
        return days[dayIndex - 1]; // Los días de la semana en Calendar empiezan en 1 (Domingo), pero los arrays en Java empiezan en 0
    }

    //si endDate >= startDate => true, else false
    public static boolean isEndDateAfterOrEqualStartDate(Date startDate, Date endDate) {
        return endDate.compareTo(startDate) >= 0;
    }

    public static int calcularEdad(String fechaNacimiento) {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        try {
            Date fechaNac = formatoFecha.parse(fechaNacimiento);
            Calendar fechaNacimientoCal = Calendar.getInstance();
            fechaNacimientoCal.setTime(fechaNac);

            Calendar fechaActual = Calendar.getInstance();

            int edad = fechaActual.get(Calendar.YEAR) - fechaNacimientoCal.get(Calendar.YEAR);

            // Verifica si el cumpleaños ya pasó en el año actual
            if (fechaActual.get(Calendar.MONTH) < fechaNacimientoCal.get(Calendar.MONTH) ||
                    (fechaActual.get(Calendar.MONTH) == fechaNacimientoCal.get(Calendar.MONTH) &&
                            fechaActual.get(Calendar.DAY_OF_MONTH) < fechaNacimientoCal.get(Calendar.DAY_OF_MONTH))) {
                edad--;
            }

            return edad;
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // Retorna -1 si hay un error en la conversión
        }
    }
    public static String obtenerFechaNacimiento(int edad) {
        // Obtener la fecha actual
        Calendar calendario = Calendar.getInstance();

        // Restar la edad al año actual
        calendario.add(Calendar.YEAR, -edad);

        // Formatear la fecha como dd/MM/yyyy
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        return formatoFecha.format(calendario.getTime());
    }
    public static long obtenerTimeMillisDesdeFecha(String fecha) {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        try {
            Date fechaConvertida = formatoFecha.parse(fecha);
            return fechaConvertida.getTime(); // Retorna el tiempo en milisegundos
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // Retorna -1 si hay un error
        }
    }
    //endregion

    //region :: JSON
    public static Integer getIntValueJSON(String data, String reference) {

        try {
            JSONTokener jsonTokener = new JSONTokener(data);
            JSONObject jsonobject1;
            jsonobject1 = (JSONObject) jsonTokener.nextValue();
            int result = jsonobject1.getInt(reference); // {"battery":80}
            return result;

        }catch (Exception e){
//            Log.e("DATA_ACT","EXCEPTION getIntValueJSON()");
            return null;
        }
    }

    public static String getStringValueJSON(String data, String reference) {
        try {
            JSONTokener jsonTokener = new JSONTokener(data);
            JSONObject jsonobject1;
            jsonobject1 = (JSONObject) jsonTokener.nextValue();
            String result = jsonobject1.getString(reference); // {"reference":"resulr"}
            return result;

        }catch (Exception e){
//            Log.e("DATA_ACT","EXCEPTION getStringValueJSON()");
            return null;
        }
    }

    public static Double getDoubleValueJSON(String data, String reference) {
        try {
            JSONTokener jsonTokener = new JSONTokener(data);
            JSONObject jsonobject1;
            jsonobject1 = (JSONObject) jsonTokener.nextValue();
            Double result = jsonobject1.getDouble(reference); // {"battery":80.4}
//            Log.e("UTIL", "getDoubleValueJSON(): " + result);
            return result;

        }catch (Exception e){
//            Log.e("UTIL","EXCEPTION getDoubleValueJSON()");
            return null;
        }
    }

    public static Boolean getBoolValueJSON(String data, String reference) {

        try {
            JSONTokener jsonTokener = new JSONTokener(data);
            JSONObject jsonobject1;
            jsonobject1 = (JSONObject) jsonTokener.nextValue();
            Boolean result = jsonobject1.getBoolean(reference); // {"battery":80}
            return result;

        }catch (Exception e){
//            Log.e("DATA_ACT","EXCEPTION getIntValueJSON()");
            return null;
        }
    }
    //endregion

    //region :: Other
    public static String MontarMAC(String mac){

        String newMAC;
        char[] chars = mac.toCharArray();

        newMAC = "" + chars[0] + chars[1] + ":" + chars[2] + chars[3] + ":" + chars[4] + chars[5] + ":" +
                chars[6] + chars[7] + ":" + chars[8] + chars[9] + ":" + chars[10] + chars[11];

        return newMAC;
    }

    public static String RequestError(String typeDevice, int error, String description) {
        // ("{"type":" + TypeDevice + ","error":802,"description":"Error inesperado."}");
        try {
            JSONObject json = new JSONObject();
            json.put("type", typeDevice);
            json.put("error", error);
            json.put("description", description);
            return json.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public static Double roundDouble(double number) {
        BigDecimal bd = new BigDecimal(Double.toString(number));
        bd = bd.setScale(1, RoundingMode.HALF_UP);
//        Log.e(TAG,"roundDouble >> " + bd.doubleValue());
        return bd.doubleValue();
    }

    public static int getBatteryLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100); // Devuelve el porcentaje de batería
        }
        return -1; // Error al obtener el nivel de batería
    }

    //endregion

    //region FILES

    /**
     * Escribe un Fichero con el contenido dado
     * @param filename [File fichero = new File(path + name)]
     * @param contenido String
     * @return True = Ok
     */
    public static boolean writeFile(File filename, String contenido) {
        try {
            FileWriter myWriter = new FileWriter(filename);
            myWriter.write(contenido);
            myWriter.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Crea un fichero con el contenido en formato JSON
     * @param filename
     * @param json
     * @return
     */
    public static boolean writeFileJSON(File filename, JSONObject json){
        return writeFile(filename, json.toString());
    }

    /**
     * Crea un fichero con contedo en formato json
     * @param filename
     * @param contenido
     * @return
     */
    public static boolean writeFileJSON(File filename, String contenido){
        try {
            JSONObject json = new JSONObject(contenido);
            return writeFile(filename, json.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Lee el contenido de un fichero
     * @param file [File fichero = new File(path + name)]
     * @return String
     */
    public static String readFile(File file){
        try {
            Scanner sc = new Scanner(file);
            StringBuffer contenido = new StringBuffer();
            while (sc.hasNextLine()) {
                contenido.append(sc.nextLine());
                contenido.append("\n");
            }
            sc.close();
            return contenido.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Lee el contenido de un fichero en formato JSON
     * @param file
     * @return
     */
    public static JSONObject readFileJSON(File file) {
        try {
            return new JSONObject(readFile(file));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void createDirectoryIfNotExists(Context context, String dirName) {
        // Obtiene el directorio de archivos de la aplicación
//        File filesDir = context.getFilesDir();kl
        File filesDir = FileAccess.getPATH();

        // Crea un objeto File para el nuevo directorio
        File newDir = new File(filesDir, dirName);

        // Comprueba si el directorio ya existe
        if (!newDir.exists()) {
            // Si no existe, intenta crearlo
            boolean isCreated = newDir.mkdirs();
            if (isCreated) {
                System.out.println("Directorio creado: " + newDir.getAbsolutePath());
            } else {
                System.out.println("No se pudo crear el directorio: " + newDir.getAbsolutePath());
            }
        } else {
            System.out.println("El directorio ya existe: " + newDir.getAbsolutePath());
        }
    }

    //endregion
}
