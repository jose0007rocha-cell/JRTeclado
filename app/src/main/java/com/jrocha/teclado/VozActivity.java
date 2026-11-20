package com.jrocha.teclado;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import java.util.ArrayList;

/** Activity transparente que abre o reconhecimento de voz do Android e devolve o texto ao teclado. */
public class VozActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        try {
            startActivityForResult(i, 1);
        } catch (Exception e) {
            Toast.makeText(this, "Reconhecimento de voz não disponível neste aparelho", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_OK && data != null) {
            ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (r != null && !r.isEmpty() && TecladoService.instancia != null)
                TecladoService.instancia.inserirTexto(r.get(0));
        }
        finish();
    }
}
