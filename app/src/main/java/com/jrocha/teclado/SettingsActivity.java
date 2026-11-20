package com.jrocha.teclado;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SettingsActivity extends Activity {

    SharedPreferences sp;
    LinearLayout col;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("jrteclado", Context.MODE_PRIVATE);
        ScrollView sv = new ScrollView(this);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        col.setPadding(p, p, p, p);
        sv.addView(col);
        setContentView(sv);

        titulo("JR Teclado", 24);
        botao("1. Ativar teclado", v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        botao("2. Selecionar teclado", v ->
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker());

        titulo("Tema", 18);
        String[] temas = new String[Tema.TODOS.length];
        for (int i = 0; i < temas.length; i++) temas[i] = Tema.TODOS[i].nome;
        radioInt(temas, "tema");

        titulo("Fonte das teclas", 18);
        radioInt(new String[]{"Padrão", "Negrito", "Itálico", "Serifada", "Mono", "Cursiva", "Personalizada (arquivo)"}, "fonte");
        botao("Escolher arquivo de fonte (.ttf / .otf)", v -> escolher(1));

        titulo("Som das teclas", 18);
        radioStr(new String[]{"Som do sistema", "Sem som", "Personalizado (arquivo)"},
                 new String[]{"sistema", "nenhum", "custom"}, "som", "sistema");
        botao("Escolher arquivo de som (.ogg / .wav / .mp3)", v -> escolher(2));
        titulo("Volume do som", 14);
        seek("vol", 0, 100, 70);

        Switch vib = new Switch(this);
        vib.setText("Vibração ao tocar");
        vib.setChecked(sp.getBoolean("vibrar", true));
        vib.setOnCheckedChangeListener((x, on) -> sp.edit().putBoolean("vibrar", on).apply());
        col.addView(vib);

        titulo("Altura das teclas (dp)", 14);
        seek("altura", 36, 64, 48);

        Switch fl = new Switch(this);
        fl.setText("Teclado flutuante (arraste pela alça)");
        fl.setChecked(sp.getBoolean("flutuante", false));
        fl.setOnCheckedChangeListener((x, on) -> sp.edit().putBoolean("flutuante", on).apply());
        col.addView(fl);

        Switch ef = new Switch(this);
        ef.setText("Efeitos visuais nas teclas (brilho, relevo)");
        ef.setChecked(sp.getBoolean("efeitos", true));
        ef.setOnCheckedChangeListener((x, on) -> sp.edit().putBoolean("efeitos", on).apply());
        col.addView(ef);
        botao("Resetar tamanho e posição do flutuante", v -> {
            sp.edit().remove("fw").remove("fesc").remove("fx").remove("fy").apply();
            Toast.makeText(this, "Resetado", Toast.LENGTH_SHORT).show();
        });

        titulo("Teste", 18);
        EditText et = new EditText(this);
        et.setHint("Toque aqui para testar o teclado");
        col.addView(et);
    }

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density); }

    void titulo(String s, int sz) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sz);
        t.setPadding(0, dp(14), 0, dp(6));
        col.addView(t);
    }

    void botao(String s, android.view.View.OnClickListener ck) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(ck);
        col.addView(b);
    }

    void radioInt(String[] ops, String key) {
        RadioGroup rg = new RadioGroup(this);
        int atual = sp.getInt(key, 0);
        for (int i = 0; i < ops.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(ops[i]);
            rb.setId(100 + i);
            rg.addView(rb);
            if (i == atual) rb.setChecked(true);
        }
        rg.setOnCheckedChangeListener((g, id) -> sp.edit().putInt(key, id - 100).apply());
        col.addView(rg);
    }

    void radioStr(String[] ops, String[] vals, String key, String def) {
        RadioGroup rg = new RadioGroup(this);
        String atual = sp.getString(key, def);
        for (int i = 0; i < ops.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(ops[i]);
            rb.setId(100 + i);
            rg.addView(rb);
            if (vals[i].equals(atual)) rb.setChecked(true);
        }
        rg.setOnCheckedChangeListener((g, id) -> sp.edit().putString(key, vals[id - 100]).apply());
        col.addView(rg);
    }

    void seek(String key, int min, int max, int def) {
        SeekBar s = new SeekBar(this);
        s.setMax(max - min);
        s.setProgress(sp.getInt(key, def) - min);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar x, int p, boolean u) { sp.edit().putInt(key, p + min).apply(); }
            public void onStartTrackingTouch(SeekBar x) { }
            public void onStopTrackingTouch(SeekBar x) { }
        });
        col.addView(s);
    }

    void escolher(int req) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, req);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        String nome = (req == 1) ? "fonte_custom" : "som_custom";
        try (InputStream in = getContentResolver().openInputStream(data.getData());
             FileOutputStream out = new FileOutputStream(new File(getFilesDir(), nome))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao copiar o arquivo", Toast.LENGTH_LONG).show();
            return;
        }
        if (req == 1) sp.edit().putInt("fonte", 6).apply();
        else sp.edit().putString("som", "custom").apply();
        Toast.makeText(this, "Arquivo aplicado", Toast.LENGTH_SHORT).show();
        recreate();
    }
}
