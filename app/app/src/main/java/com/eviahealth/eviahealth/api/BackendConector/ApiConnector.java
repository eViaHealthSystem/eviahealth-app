package com.eviahealth.eviahealth.api.BackendConector;

import android.os.StrictMode;
import android.util.Log;
import com.eviahealth.eviahealth.utils.log.EVLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import javax.net.ssl.HttpsURLConnection;

/**
 * Clase que permite la conexión con la API a través de peticiones HTTPS.
 * Proporciona métodos para enviar peticiones POST, obtener respuestas en formato JSON
 * y subir archivos en formato multipart/form-data.
 */
public class ApiConnector {

    private static String HOST = "";
    private static String TOKEN = "";

    /**
     * Establece el token de autenticación para las peticiones.
     *
     * @param tkn Token de autenticación.
     */
    public static void setToken(String tkn) {
        TOKEN = tkn;
        Log.i("ApiConector", "Token establecido: " + TOKEN);
    }

    /**
     * Solicita el token actual establecido
     * @return Respuesta del token actual
     */
    public static String getToken() {
        return TOKEN;
    }

    /**
     * Establece el host para las peticiones. Se asegura de que el host tenga el prefijo "https://".
     *
     * @param host Dirección del host.
     */
    public static void setHost(String host) {
        if (host.contains("https://")) {
            HOST = host;
        } else {
            HOST = "https://" + host;
        }
        Log.i("ApiConector", "Host establecido: " + HOST);
    }

    /**
     * Solicita el host actual establecido
     * @return Respuesta del host actual
     */
    public static String getHost() {
        return HOST;
    }

    /**
     * Construye la URL completa a partir del host y del endpoint definido en ApiUrl.
     *
     * @param url Objeto ApiUrl que contiene el endpoint.
     * @return URL completa en formato String.
     */
    public static String buildURL(ApiUrl url) {
        return HOST + url.getURL();
    }

    /**
     * Lee la respuesta del servidor a través de una conexión HTTPS.
     *
     * @param con Conexión HttpsURLConnection desde la que se leerá la respuesta.
     * @return Respuesta del servidor en formato String.
     * @throws IOException Si ocurre un error al leer la respuesta.
     */
    private static String readResponse(HttpsURLConnection con) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Escribe el cuerpo de la petición utilizando un Scanner para leer línea a línea.
     *
     * @param con  Conexión HttpsURLConnection a la que se enviará el cuerpo de la petición.
     * @param body Scanner que contiene el cuerpo de la petición.
     * @throws IOException Si ocurre un error al escribir el cuerpo.
     */
    private static void writeRequestBody(HttpsURLConnection con, Scanner body) throws IOException {
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            while (body.hasNextLine()) {
                String line = body.nextLine();
                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                wr.write(bytes);
                wr.write("\n".getBytes(StandardCharsets.UTF_8));
                wr.flush();
            }
        }
    }

    /**
     * Envía una petición POST con cuerpo en formato JSON utilizando un Scanner.
     *
     * @param url  Objeto ApiUrl que contiene el endpoint.
     * @param body Scanner que contiene el cuerpo de la petición.
     * @return Respuesta del servidor en formato String.
     * @throws IOException   Si ocurre un error en la conexión o lectura/escritura de datos.
     * @throws ApiException  Si la respuesta del servidor no tiene un código HTTP 200.
     */
    public static String peticion(ApiUrl url, Scanner body) throws IOException, ApiException {
        URL obj = new URL(buildURL(url));
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setConnectTimeout(10000);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setRequestProperty("Authorization", TOKEN);
        con.setDoOutput(true);

        writeRequestBody(con, body);

        int httpcode = con.getResponseCode();
        Log.e("-----", "httpcode: " + httpcode);
        if (httpcode != 200) {
            EVLog.log("API", "API ERROR HTTP CODE " + httpcode);
            throw new ApiException("API ERROR HTTP CODE " + httpcode, httpcode);
        }
        return readResponse(con);
    }

    /**
     * Sobrecarga del método peticion que recibe el cuerpo en formato String.
     *
     * @param url  Objeto ApiUrl que contiene el endpoint.
     * @param body Cuerpo de la petición en formato String.
     * @return Respuesta del servidor en formato String.
     * @throws IOException   Si ocurre un error en la conexión o lectura/escritura de datos.
     * @throws ApiException  Si la respuesta del servidor no tiene un código HTTP 200.
     */
    public static String peticion(ApiUrl url, String body) throws IOException, ApiException {
        Scanner sc = new Scanner(body);
        String res = peticion(url, sc);
        sc.close();
        return res;
    }

    /**
     * Sobrecarga del método peticion que recibe el cuerpo en formato JSONObject.
     *
     * @param url  Objeto ApiUrl que contiene el endpoint.
     * @param body Cuerpo de la petición en formato JSONObject.
     * @return Respuesta del servidor en formato String.
     * @throws IOException   Si ocurre un error en la conexión o lectura/escritura de datos.
     * @throws ApiException  Si la respuesta del servidor no tiene un código HTTP 200.
     */
    public static String peticion(ApiUrl url, JSONObject body) throws IOException, ApiException {
        return peticion(url, body.toString());
    }

    /**
     * Envía una petición POST y retorna la respuesta como un JSONObject.
     *
     * @param url  Objeto ApiUrl que contiene el endpoint.
     * @param body Cuerpo de la petición en formato JSONObject.
     * @return Respuesta del servidor en formato JSONObject.
     * @throws IOException   Si ocurre un error en la conexión o lectura/escritura de datos.
     * @throws JSONException Si ocurre un error al parsear el JSON.
     * @throws ApiException  Si la respuesta del servidor no tiene un código HTTP 200.
     */
    public static JSONObject peticionJSONObject(ApiUrl url, JSONObject body) throws IOException, JSONException, ApiException {
        return new JSONObject(peticion(url, body));
    }

    /**
     * Envía una petición POST y retorna la respuesta como un JSONArray.
     *
     * @param url  Objeto ApiUrl que contiene el endpoint.
     * @param body Cuerpo de la petición en formato JSONObject.
     * @return Respuesta del servidor en formato JSONArray.
     * @throws IOException   Si ocurre un error en la conexión o lectura/escritura de datos.
     * @throws JSONException Si ocurre un error al parsear el JSON.
     * @throws ApiException  Si la respuesta del servidor no tiene un código HTTP 200.
     */
    public static JSONArray peticionJSONArray(ApiUrl url, JSONObject body) throws IOException, JSONException, ApiException {
        return new JSONArray(peticion(url, body));
    }

    /**
     * Sube un log en formato de texto plano.
     * <p>
     * Este método envía el contenido del log (proporcionado mediante un Scanner) al endpoint definido
     * en {@link ApiUrl#SUBIR_LOG} concatenado con el idPaciente y el nombreArchivo. Se utilizan los métodos
     * auxiliares {@link #writeRequestBody(HttpsURLConnection, Scanner)} y {@link #readResponse(HttpsURLConnection)}
     * para escribir el cuerpo de la petición y leer la respuesta.
     * </p>
     *
     * @param idpaciente    Identificador del paciente.
     * @param nombre_archivo Nombre con el que se subirá el archivo log.
     * @param body          Scanner que contiene el contenido del log.
     * @throws IOException  Si ocurre un error durante la lectura/escritura o en la conexión.
     * @throws ApiException Si la respuesta del servidor no tiene un código HTTP 200.
     */
    public static void subirLog(String idpaciente, String nombre_archivo, Scanner body) throws ApiException, IOException {
        URL obj = new URL(buildURL(ApiUrl.SUBIR_LOG) + "/" + idpaciente + "/" + nombre_archivo);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setConnectTimeout(10000);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        con.setRequestProperty("Authorization", TOKEN);
        con.setDoOutput(true);

        // Escribir el cuerpo de la petición utilizando el método auxiliar
        writeRequestBody(con, body);

        // Obtener el código HTTP y, opcionalmente, leer la respuesta
        int httpcode = con.getResponseCode();
        if (httpcode != 200) {
            Log.e("ApiConector", "httpcode: " + httpcode + ", file: " + nombre_archivo);
            throw new ApiException("API ERROR HTTP CODE " + httpcode, httpcode);
        }
        // Se lee la respuesta para asegurar que se consume el InputStream (aunque no se utilice)
        readResponse(con);
    }

    /**
     * Sobrecarga privada de uploadFile que acepta un endpoint completo en formato String y
     * permite especificar el nombre del campo para el id del paciente.
     *
     * @param endpoint        Endpoint completo (incluyendo la parte definida en ApiUrl) al que se realizará la petición.
     * @param file            Archivo a enviar.
     * @param idPaciente      Identificador del paciente.
     * @param idFieldName     Nombre del campo para el id del paciente.
     * @param fileFieldName   Nombre del campo del formulario para el archivo.
     * @param fileContentType Tipo de contenido del archivo (puede ser null).
     * @return Respuesta del servidor en formato String.
     * @throws IOException  Si ocurre un error en la conexión o lectura/escritura de datos.
     * @throws ApiException Si la respuesta del servidor no tiene un código HTTP 200.
     */
    private static String uploadFile(String endpoint, File file, String idPaciente, String idFieldName,
                                     String fileFieldName, String fileContentType)
            throws IOException, ApiException {
        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";

        URL obj = new URL(HOST + endpoint);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setConnectTimeout(10000);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        con.setRequestProperty("Authorization", TOKEN);
        con.setRequestProperty("file", "true");
        con.setRequestProperty("X-Requested-With", "mi-fetch");
        con.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream());
             FileInputStream inputStream = new FileInputStream(file)) {
            // Añadir el archivo
            wr.writeBytes("--" + boundary + CRLF);
            wr.writeBytes("Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + file.getName() + "\"" + CRLF);
            if (fileContentType != null) {
                wr.writeBytes("Content-Type: " + fileContentType + CRLF);
            }
            wr.writeBytes(CRLF);

            // Leer y escribir el archivo en bloques de 4096 bytes
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                wr.write(buffer, 0, bytesRead);
            }
            wr.writeBytes(CRLF);

            // Añadir idPaciente con el nombre de campo configurable
            wr.writeBytes("--" + boundary + CRLF);
            wr.writeBytes("Content-Disposition: form-data; name=\"" + idFieldName + "\"" + CRLF);
            wr.writeBytes(CRLF);
            wr.writeBytes(idPaciente);
            wr.writeBytes(CRLF);

            // Finalizar el cuerpo multipart/form-data
            wr.writeBytes("--" + boundary + "--" + CRLF);
            wr.flush();
        }

        int httpcode = con.getResponseCode();
        if (httpcode != 200) {
            Log.e("API", "API ERROR HTTP CODE " + httpcode);
            throw new ApiException("API ERROR HTTP CODE " + httpcode, httpcode);
        }
        return readResponse(con);
    }

    /**
     * Sube un archivo genérico (por ejemplo, una imagen) utilizando una petición multipart/form-data.
     *
     * @param url        Objeto ApiUrl que contiene el endpoint.
     * @param file       Archivo a subir.
     * @param idPaciente Identificador del paciente.
     * @return Respuesta del servidor en formato String.
     * @throws IOException  Si ocurre un error en la conexión o lectura/escritura de datos.
     * @throws ApiException Si la respuesta del servidor no tiene un código HTTP 200.
     */
    public static String subirArchivo(ApiUrl url, File file, String idPaciente) throws IOException, ApiException {
        // Para imágenes se utiliza "idPaciente" (con P mayúscula) como nombre del campo
        return uploadFile(url.getURL(), file, idPaciente, "idPaciente", "image", null);
    }

    /**
     * Sube un archivo PDF utilizando una petición multipart/form-data.
     *
     * @param url        Objeto ApiUrl que contiene el endpoint.
     * @param file       Archivo PDF a subir.
     * @param idPaciente Identificador del paciente.
     * @return Respuesta del servidor en formato String.
     * @throws IOException  Si ocurre un error en la conexión o lectura/escritura de datos.
     * @throws ApiException Si la respuesta del servidor no tiene un código HTTP 200.
     */
    public static String subirArchivoPDF(ApiUrl url, File file, String idPaciente) throws IOException, ApiException {
        Log.e("*********", "Filename: " + file.getName());
        Log.e("*********", "idPaciente: " + idPaciente);
        // Para PDF se utiliza "idpaciente" (en minúsculas) como nombre del campo
        return uploadFile(url.getURL(), file, idPaciente, "idpaciente", "ecg", "application/pdf");
    }


    public static JSONObject postAPI_HTTPCODE(ApiUrl purl, JSONObject pBody) {

        HttpsURLConnection conn = null;
        JSONObject vOb = null;
        String vRespuesta = "";
        Integer responseCode = 0;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            URL url = new URL(buildURL(purl));
            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", TOKEN);

            if(pBody != null) {
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(pBody.toString());
                writer.flush();
                writer.close();
                os.flush();
                os.close();
            }
            conn.connect();

            String protocol = conn.getCipherSuite();
            Log.e("TLS_VERSION", "Protocolo en uso: " + protocol);

            responseCode = conn.getResponseCode();
            Log.e("ApiConnector", "responseCode: " + responseCode);

            if (responseCode == HttpsURLConnection.HTTP_OK || responseCode == HttpsURLConnection.HTTP_CREATED) {
                vRespuesta = "{ \"httpCode\": " + responseCode.toString() + " }";
            }
            else {
                vRespuesta = "";
            }

            conn.disconnect();

            Log.e("ApiConnector", "vRespuesta;: " + vRespuesta);

            try {
                if (vRespuesta == "" || vRespuesta == null) {
                    if ((responseCode == HttpsURLConnection.HTTP_INTERNAL_ERROR || responseCode == HttpsURLConnection.HTTP_NOT_FOUND || responseCode == HttpsURLConnection.HTTP_BAD_REQUEST)) {
                        vRespuesta = "{ \"httpCode\": " + responseCode.toString() + " }";
                    }
                    else { vRespuesta = "{ \"httpCode\": -1 }"; }
                }

                vOb = new JSONObject(vRespuesta);
                if (!vOb.has("httpCode")) {
                    vOb.put("httpCode", responseCode);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("postAPI_HTTPCODE()->" + e.getMessage());

            }
        }catch (IOException e) {
            Log.e("ApiConnector","Exception postAPI_HTTPCODE(IOException)->" + e.getMessage());
            try {
                vOb = new JSONObject("{ \"httpCode\": -999 }");
            } catch (JSONException err) {
                err.printStackTrace();
            }
        }catch (Exception ex){
            System.out.println("postAPI_HTTPCODE()->" + ex.getMessage());
        }
        finally {
            if(conn != null)
                conn.disconnect();
        }

        if (vOb == null) {
            try {
                vOb = new JSONObject("{ \"httpCode\": -2 }");
            }catch (JSONException e) {
            }
        }

        Log.e("ApiConnector","Response postAPI_HTTPCODE: " + vOb);
        return vOb;
    }
}
