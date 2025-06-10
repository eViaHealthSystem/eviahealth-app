package com.eviahealth.eviahealth.ui.formacion;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;


public class VideoActivity extends BaseActivity {
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private Button salirButton;
    private SeekBar volumeSeekBar;
    private AudioManager audioManager;

    private float volume = 1.0f;
    private String videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        playerView = findViewById(R.id.player_view);
        salirButton = findViewById(R.id.button_salir);
        volumeSeekBar = findViewById(R.id.seekbar_volume);

        salirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVideo();
                finish();
            }
        });

        String url = getIntent().getStringExtra("url");
        if (url != null) { videoUrl = url; }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No se requiere ninguna acción al comenzar a mover la barra de búsqueda.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No se requiere ninguna acción al detener el movimiento de la barra de búsqueda.
            }
        });


    }

    private void playVideo() {
        // Crear el reproductor
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);


        // Configurar la URL del video
        Uri uri = Uri.parse(videoUrl);
        MediaSource mediaSource = buildMediaSource(uri);

        // Preparar el reproductor
        player.setPlayWhenReady(true);
        player.prepare(mediaSource);

        // Agregar el Listener para mostrar el progreso del video
        player.addListener(new Player.Listener() {
            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                updateProgress();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_READY && playWhenReady) {
                    // El video ha comenzado a reproducirse, inicia la actualización del progreso
                    updateProgress();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                // Manejar el error de reproducción
                Throwable cause = error.getCause();
                // Realizar acciones según el tipo de error
                if (cause instanceof IOException) {
                    // Error de red o de archivo
                    Toast.makeText(getApplicationContext(), "Error de red o de archivo", Toast.LENGTH_SHORT).show();
                } else if (cause instanceof UnsupportedDrmException) {
                    // Error de DRM no compatible
                    Toast.makeText(getApplicationContext(), "Error de DRM no compatible", Toast.LENGTH_SHORT).show();
                } else if (cause instanceof ExoPlaybackException) {
                    // Error de reproducción general
                    Toast.makeText(getApplicationContext(), "Error de reproducción general", Toast.LENGTH_SHORT).show();
                } else {
                    // Otros errores
                    Toast.makeText(getApplicationContext(), "Otros errores", Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        });

    }


    // Método para actualizar el progreso del video
    private void updateProgress() {
        if (player != null) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            // Actualiza la interfaz de usuario con el progreso actual
            // (por ejemplo, muestra el progreso en un TextView o ProgressBar)
        }
    }

    private void stopVideo() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    private MediaSource buildMediaSource(Uri videoUri, DataSource.Factory dataSourceFactory) {
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
    }

    // Método para construir el MediaSource
    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "ExoPlayer"));
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            adjustVolume(AudioManager.ADJUST_RAISE);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            adjustVolume(AudioManager.ADJUST_LOWER);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void adjustVolume(int direction) {
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int newVolume = currentVolume;

        if (direction == AudioManager.ADJUST_RAISE && currentVolume < maxVolume) {
            newVolume++;
        } else if (direction == AudioManager.ADJUST_LOWER && currentVolume > 0) {
            newVolume--;
        }

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        volumeSeekBar.setProgress(newVolume);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("VideoActivity","onResume()");
        playVideo();
    }
}