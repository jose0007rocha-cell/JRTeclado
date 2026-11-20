package com.jrocha.teclado;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class TelaActivity extends Activity {
    SharedPreferences sp;
    String tela;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("jrteclado", Context.MODE_PRIVATE);
        tela = getIntent().getStringExtra("tela");
        if (tela == null) tela = "aparencia";
        montar();
    }

    void montar() {
        String titulo = tela.equals("fontes") ? "Fontes" : tela.equals("som") ? "Som e vibração" : "Aparência";
        Ui.Tela t = Ui.tela(this, titulo, null);
        if (tela.equals("fontes")) fontes(t.col);
        else if (tela.equals("som")) som(t.col);
        else aparencia(t.col);
    }

    void aparencia(LinearLayout col) {
        LinearLayout c = Ui.card(col);
        Ui.add(c, Ui.slider(this, "Altura das teclas (dp)", 36, 64, sp.getInt("altura", 48),
            v -> sp.edit().putInt("altura", v).apply()));
        Ui.add(c, Ui.toggle(this, "Efeitos visuais nas teclas", sp.getBoolean("efeitos", true),
            on -> sp.edit().putBoolean("efeitos", on).apply()));
        LinearLayout c2 = Ui.card(col);
        Ui.add(c2, Ui.toggle(this, "Teclado flutuante", sp.getBoolean("flutuante", false),
            on -> sp.edit().putBoolean("flutuante", on).apply()));
        Ui.add(c2, Ui.item(this, "Resetar tamanho e posição do flutuante", "Volta ao padrão", v -> {
            sp.edit().remove("fw").remove("fesc").remove("fx").remove("fy").apply();
            Toast.makeText(this, "Resetado", Toast.LENGTH_SHORT).show();
        }));
    }

    void fontes(LinearLayout col) {
        LinearLayout c = Ui.card(col);
        c.addView(Ui.radio(this, new String[]{"Padrão", "Negrito", "Itálico", "Serifada", "Mono", "Cursiva", "Personalizada (arquivo)"},
            sp.getInt("fonte", 0), v -> sp.edit().putInt("fonte", v).apply()));
        LinearLayout c2 = Ui.card(col);
        Ui.add(c2, Ui.item(this, "Escolher arquivo de fonte", ".ttf ou .otf", v -> escolher(1)));
    }

    void som(LinearLayout col) {
        final String[] vals = {"sistema", "nenhum", "custom"};
        String atual = sp.getString("som", "sistema");
        int idx = 0;
        for (int i = 0; i < vals.length; i++) if (vals[i].equals(atual)) idx = i;
        LinearLayout c = Ui.card(col);
        c.addView(Ui.radio(this, new String[]{"Som do sistema", "Sem som", "Personalizado (arquivo)"}, idx,
            v -> sp.edit().putString("som", vals[v]).apply()));
        LinearLayout c2 = Ui.card(col);
        Ui.add(c2, Ui.item(this, "Escolher arquivo de som", ".ogg, .wav ou .mp3", v -> escolher(2)));
        Ui.add(c2, Ui.slider(this, "Volume do som", 0, 100, sp.getInt("vol", 70),
            v -> sp.edit().putInt("vol", v).apply()));
        Ui.add(c2, Ui.toggle(this, "Vibração ao tocar", sp.getBoolean("vibrar", true),
            on -> sp.edit().putBoolean("vibrar", on).apply()));
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
        montar();
    }
}
