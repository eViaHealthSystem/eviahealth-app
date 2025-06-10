package com.eviahealth.eviahealth.ui.admin.fichapaciente;

import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;
import com.eviahealth.eviahealth.api.BackendConector.ApiUrl;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CameraFoto extends BaseActivity {

    final String TAG = CameraFoto.class.getSimpleName();
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector;
    private Camera camera;
    private PreviewView viewFinder;
    private Boolean fgCapture = false;
    private String idpaciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_foto);
        Log.e(TAG,"onCreate()");
        viewFinder = findViewById(R.id.viewFinder);

        cameraExecutor = Executors.newSingleThreadExecutor();

        ImageButton tomarImagen = findViewById(R.id.capture);
        tomarImagen.setOnClickListener(v -> takePicture());

        ImageButton flipCamera = findViewById(R.id.flipCamera);
        flipCamera.setOnClickListener(v -> switchCamera());

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finalizar());

        Button buttonEnviar = findViewById(R.id.buttonEnviar);
        buttonEnviar.setOnClickListener(v -> enviarImagen());

        // Obtener el idpaciente del intent
        idpaciente = getIntent().getStringExtra("numeropaciente");
        Log.e(TAG,"PACIENTE: "+ idpaciente);

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                preview = new Preview.Builder().build();

                // Establece la resolución de la imagen en 1280x720
                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(new Size(480, 720))
                        .build();

                cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                // Modifica esta línea para conectar tu Preview con tu viewFinder
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                // Maneja cualquier error
                e.printStackTrace();
                Log.e(TAG,"Exception: " + e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePicture() {
        Log.e(TAG,"takePicture()");

        fgCapture = false;

//        File photoFile = new File(getFilesDir(), "photo.png");
        File photoFile = new File(FileAccess.getPATH_FILES(), "photo.png");
        Log.e(TAG,"FILE: " + photoFile.toString());

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                Log.e(TAG, "onImageSaved");
                Uri savedUri = Uri.fromFile(photoFile);
                String msg = "Photo capture succeeded: " + savedUri;
                Log.e(TAG, msg);

                // Añade este código para corregir la orientación de la imagen
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), savedUri);
                    if (cameraSelector.getLensFacing() == CameraSelector.LENS_FACING_FRONT) {
                        Matrix matrix = new Matrix();
                        matrix.preScale(-1.0f, 1.0f);
                        Bitmap mirroredBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                        bitmap.recycle();
                        FileOutputStream out = new FileOutputStream(photoFile);
                        mirroredBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.close();
                    }
                    fgCapture = true;
                    mensage("CAPTURA REALIZADA");
                } catch (IOException e) {
                    Log.e(TAG, "Error al corregir la orientación de la imagen: " + e.getMessage(), e);
                    fgCapture = false;
                }

                if (fgCapture) {
                    chargeImage(photoFile);
                }
                else {
                    ImageView myImage = (ImageView) findViewById(R.id.imagePreview);
                    myImage.setImageResource(R.drawable.sinfotos);
                }
            }

            @Override
            public void onError(ImageCaptureException error) {
                Log.e(TAG, "Photo capture failed: " + error.getMessage(), error);
                mensage("Photo capture failed: " + error.getMessage());
            }
        });
    }

    private void chargeImage(File imgFile) {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    ImageView myImage = (ImageView) findViewById(R.id.imagePreview);
                    myImage.setImageBitmap(myBitmap);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG,"EX: " + e);
                }
            }
        });

    }

    @SuppressLint("RestrictedApi")
    private void switchCamera() {
        if (cameraSelector.getLensFacing() == CameraSelector.LENS_FACING_BACK) {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        } else {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        }

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void enviarImagen() {
        if (fgCapture) {
            Log.e(TAG, "enviarImagen()");
            try {
//                final File photoFile = new File(getFilesDir(), "photo.png");

                final File photoFile = new File(FileAccess.getPATH_FILES(), "photo.png");
                Log.e(TAG,"FILE: " + photoFile.toString());

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<String> future = executor.submit(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return ApiConnector.subirArchivo(ApiUrl.FOTO_PACIENTE, photoFile, idpaciente);
                    }
                });

                try {
                    String result = future.get();  // Espera la ejecución
                    Log.e(TAG, "API: " + result);

                    Toast.makeText(getApplicationContext(), "FOTO SUBIDA CORRECTAMENTE", Toast.LENGTH_LONG).show();

                    finalizar();

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "EXCEPCION: Error al subir el archivo", Toast.LENGTH_LONG).show();
                }

                executor.shutdown();  // No olvides cerrar el executor

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "EXCEPCION: " + e.toString(), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e(TAG, "No hay imagen cargada");
            Toast.makeText(getApplicationContext(), "No hay imagen cargada!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void finalizar() {

        // Borra el archivo si existe
        File photoFile = new File(FileAccess.getPATH_FILES(), "photo.png");
        FileAccess.deleteFile(photoFile);

        finish();
        Intent intent = new Intent(CameraFoto.this, DispositivosPaciente.class);
        intent.putExtra("numeropaciente", idpaciente);
        startActivity(intent);
    }

    private void mensage(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"onDestroy()");
        cameraExecutor.shutdown();
        super.onDestroy();
    }
}