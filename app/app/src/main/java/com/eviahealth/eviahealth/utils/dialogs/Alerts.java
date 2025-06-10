package com.eviahealth.eviahealth.utils.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class Alerts {
    private Alerts() {}

    public static void mostrarAlert(Activity ctx, String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setMessage(message);
        alert.setNegativeButton("Cerrar",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alert.show();
    }

    public static void mostrarAlert(Activity ctx, String message, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setMessage(message);
//        alert.setNegativeButton("Salir sin guardar",
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
        alert.setNegativeButton("Salir sin guardar",listener);
        alert.setPositiveButton("Guardar y salir", listener);
        alert.show();
    }

    public static void mostrarAlertFoto(Activity ctx, String message, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setMessage(message);
        alert.setNegativeButton("Cancelar",listener);
        alert.setPositiveButton("Guardar", listener);
        alert.show();
    }

    public static void mostrarAlertRemove(Activity ctx, String message, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setMessage(message);
        alert.setNegativeButton("Cancelar",listener);
        alert.setPositiveButton("Eliminar", listener);
        alert.show();
    }

    public static void mostrarAlertSalir(Activity ctx, String message, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setMessage(message);
        alert.setNegativeButton("Cancelar",listener);
        alert.setPositiveButton("Salir", listener);
        alert.show();
    }
}
