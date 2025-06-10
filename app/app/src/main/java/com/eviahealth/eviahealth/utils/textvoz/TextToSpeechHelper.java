package com.eviahealth.eviahealth.utils.textvoz;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TextToSpeechHelper implements TextToSpeech.OnInitListener {

    final String TAG = "TextToSpeechHelper";

    private TextToSpeech textToSpeech;
    private Context context;
    private int id;
    private List<String> textos;
    private volatile boolean textstart = false;

    public TextToSpeechHelper(Context context) {
        this.context = context;
        textToSpeech = new TextToSpeech(context, this);
        textos = new ArrayList<>();
    }

    @Override
    public void onInit(int status) {

        Log.e(TAG,"onInit()");
        if(status != TextToSpeech.ERROR) {
            Locale locSpanish = new Locale("es", "ES"); //es-ES
            textToSpeech.setLanguage(locSpanish);

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
//                    Log.e(TAG,"TextToSpeech: On Start");
                    textstart = true;
                }

                @Override
                public void onDone(String utteranceId) {
//                    Log.e(TAG,"TextToSpeech: On Done");
                    id += 1;
                    if (id < textos.size()) {
                        textToSpeech.speak(textos.get(id), TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
                    } else { textstart = false; }
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG,"TextToSpeech: On Error");
                }
            });
        }
    }

    public void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void speak(List<String> text) {
        this.id = 0;

        if (text == null || text.isEmpty()) {
            Log.e(TAG, "Lista de texto vacía o nula");
            return;
        }

        this.textos.clear();
        this.textos.addAll(text);

        // Establece la velocidad de reproducción a un valor más lento
        textToSpeech.setSpeechRate(0.8f);
        textToSpeech.speak(textos.get(id), TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        textstart = false;
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textstart = false;
        }
    }

    public boolean isStart() {
        return textstart;
    }
}
