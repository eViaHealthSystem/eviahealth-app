package com.eviahealth.eviahealth.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    public static final int PERMISSION_CODE = 30001;
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static boolean hasTip = false;
    private static boolean hasRequest = false;

    private PermissionUtils() {
    }

    public static String[] getAllPermissions() {
        return new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.USE_FULL_SCREEN_INTENT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
    }

    public static void handleRequestFailed(Activity activity, String permission) {
        if (hasTip) {
            return;
        }
        hasTip = true;
        if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Toast.makeText(activity, "Failed to obtain location information permission", Toast.LENGTH_SHORT).show();
            Log.e("PERMISSION", "Failed to obtain location information permission");
        }
        else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(activity, "Failed to obtain read permission", Toast.LENGTH_SHORT).show();
            Log.e("PERMISSION", "Failed to obtain read permission");
        }
        else if (permission.equals(Manifest.permission.CALL_PHONE)) {
            Toast.makeText(activity, "Failed to obtain call notification permission", Toast.LENGTH_SHORT).show();
            Log.e("PERMISSION", "Failed to obtain call notification permission");
        }
        else if (permission.equals(Manifest.permission.READ_SMS)) {
            Toast.makeText(activity, "Failed to obtain SMS reading permission", Toast.LENGTH_SHORT).show();
            Log.e("PERMISSION", "Failed to obtain SMS reading permission");
        }
        else if (permission.equals(Manifest.permission.READ_CONTACTS)) {
            Toast.makeText(activity, "Failed to obtain address book permissions", Toast.LENGTH_SHORT).show();
            Log.e("PERMISSION", "Failed to obtain address book permissions");
        }
        else if (permission.equals(Manifest.permission.BLUETOOTH_ADMIN)) {
            Toast.makeText(activity, "Failed to obtain Bluetooth permission", Toast.LENGTH_SHORT).show();
            Log.e("PERMISSION", "Failed to obtain Bluetooth permission");
        }
    }

    public static void checkAll(Activity activity) {
        for (String s : getAllPermissions()) {
            if (!checkPermission(activity, s)) {
                handleRequestFailed(activity, s);
            }
        }
    }

    public static void requestAll(Activity activity) {
        if (hasRequest) {
            checkAll(activity);
            return;
        }
        hasRequest = true;
        List<String> permissions = new ArrayList<>();
        for (String s : getAllPermissions()) {
            if (!checkPermission(activity, s)) {
                permissions.add(s);
            }
        }
        if (!permissions.isEmpty()) {
            String[] array = new String[permissions.size()];
            permissions.toArray(array);
            requestPermission(activity, array, PERMISSION_CODE);
        }

    }

    @SuppressLint("NewApi")
    public static void checkPermission_MANAGE_APP_ALL_FILES(Activity activity) {
        final int REQUEST_PERMISSIONS = 0;
        StringBuilder tempRequest = new StringBuilder();

        if (30 >= Build.VERSION_CODES.R) {

            if (!Environment.isExternalStorageManager()) {
                //  "Allow permission for storage access!"
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s",activity.getApplicationContext().getPackageName())));
                    activity.startActivityForResult(intent, 2296);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, 2296);
                }
            } else {
                // perform action when allow permission success
//                Toast.makeText(this, "perform action when allow permission success", Toast.LENGTH_SHORT).show();
            }
        }

        if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            tempRequest.append(Manifest.permission.MANAGE_EXTERNAL_STORAGE + ",");
        }

        if (tempRequest.length() > 0) {
            tempRequest.deleteCharAt(tempRequest.length() - 1);
            ActivityCompat.requestPermissions(activity, tempRequest.toString().split(","), REQUEST_PERMISSIONS);
        }
    }

    /**
     * Solicitar permiso
     */
    public static void requestPermission(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * Solicitar permiso
     */
    public static void requestPermission(Activity activity, String permissions, int requestCode) {
        requestPermission(activity, new String[]{permissions}, requestCode);
    }

    private static boolean checkPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

}
