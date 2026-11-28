package com.jrocha.teclado;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/** Janela pequena para editar um texto da area de transferencia (recente ou salvo). */
public class EditarClipActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        Ui.aplicarPaleta(this);
        setTheme(Ui.escuro ? android.R.style.Theme_DeviceDefault_Dialog
            : android.R.style.Theme_DeviceDefault_Light_Dialog);
        super.onCreate(b);
        final String antigo = getIntent().getStringExtra("texto") == null ? "" : getIntent().getStringExtra("texto");
        final boolean salvo = getIntent().getBooleanExtra("salvo", false);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 18);
        col.setPadding(p, p, p, p);
        col.setBackgroundColor(Ui.BG);

        TextView titulo = new TextView(this);
        titulo.setText("Editar texto");
        titulo.setTextSize(20);
        titulo.setTextColor(Ui.TXT);
        titulo.setPadding(0, 0, 0, Ui.dp(this, 10));
        col.addView(titulo);

        final EditText et = new EditText(this);
        et.setText(antigo);
        et.setSelection(antigo.length());
        et.setMinLines(4);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setTextColor(Ui.TXT);
        et.setHintTextColor(Ui.SUB);
        col.addView(et, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout botoes = new LinearLayout(this);
        botoes.setGravity(Gravity.END);
        botoes.addView(Ui.botaoPilula(this, "Cancelar", v -> finish()));
        botoes.addView(Ui.botaoPilula(this, "Salvar", v -> {
            String novo = et.getText().toString();
            if (novo.trim().isEmpty()) {
                Toast.makeText(this, "O texto não pode ficar vazio", Toast.LENGTH_SHORT).show();
                return;
            }
            aplicar(antigo, novo, salvo);
            finish();
        }));
        LinearLayout.LayoutParams lpb = new LinearLayout.LayoutParams(-1, -2);
        lpb.topMargin = Ui.dp(this, 10);
        col.addView(botoes, lpb);
        setContentView(col);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    void aplicar(String antigo, String novo, boolean salvo) {
        if (salvo) {
            SharedPreferences sp = getSharedPreferences("jrteclado", Context.MODE_PRIVATE);
            List<String> l = new ArrayList<>();
            try {
                JSONArray a = new JSONArray(sp.getString("clip_salvos", "[]"));
                for (int i = 0; i < a.length(); i++) l.add(a.getString(i));
            } catch (Exception e) { }
            int i = l.indexOf(antigo);
            if (i >= 0) l.set(i, novo); else l.add(0, novo);
            JSONArray a = new JSONArray();
            for (String s : l) a.put(s);
            sp.edit().putString("clip_salvos", a.toString()).apply();
        } else if (TecladoService.instancia != null) {
            TecladoService.instancia.trocarClip(antigo, novo);
        }
    }
}
