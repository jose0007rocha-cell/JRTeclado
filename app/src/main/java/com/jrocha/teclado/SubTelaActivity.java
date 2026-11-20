package com.jrocha.teclado;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SubTelaActivity extends BaseTela {
    String tela = "aparencia";

    @Override protected void onCreate(Bundle b) {
        String t = getIntent().getStringExtra("tela");
        if (t != null) tela = t;
        super.onCreate(b);
    }

    @Override protected String titulo() {
        switch (tela) {
            case "som": return "Som e vibração";
            case "fonte": return "Teclas e fontes";
            case "flutuante": return "Teclado flutuante";
            default: return "Aparência";
        }
    }

    void escolher(int req) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, req);
    }

    @Override protected void construir() {
        LinearLayout c = Ui.cartao(this);
        switch (tela) {
            case "som": {
                String[] vals = {"sistema", "nenhum", "custom"};
                String atual = sp.getString("som", "sistema");
                int idx = 0;
                for (int i = 0; i < vals.length; i++) if (vals[i].equals(atual)) idx = i;
                Ui.add(c, Ui.radio(this, new String[]{"Som do sistema", "Sem som", "Personalizado (arquivo)"}, idx,
                    x -> sp.edit().putString("som", vals[x]).apply()));
                Ui.add(c, Ui.item(this, "Escolher arquivo de som", ".ogg, .wav ou .mp3", v -> escolher(2)));
                Ui.add(c, Ui.seekItem(this, "Volume", 0, 100, sp.getInt("vol", 70), "%",
                    x -> sp.edit().putInt("vol", x).apply()));
                Ui.add(c, Ui.switchItem(this, "Vibração ao tocar", null, sp.getBoolean("vibrar", true),
                    on -> sp.edit().putBoolean("vibrar", on).apply()));
                break;
            }
            case "fonte": {
                Ui.add(c, Ui.radio(this, new String[]{"Padrão", "Negrito", "Itálico", "Serifada", "Mono", "Cursiva",
                    "Personalizada (arquivo)"}, sp.getInt("fonte", 0), x -> sp.edit().putInt("fonte", x).apply()));
                Ui.add(c, Ui.item(this, "Escolher arquivo de fonte", ".ttf ou .otf", v -> escolher(1)));
                col.addView(c);
                col.addView(Ui.secao(this, "Estilo do texto digitado"));
                c = Ui.cartao(this);
                Ui.add(c, Ui.radio(this, TecladoService.ESTILOS, sp.getInt("estilo", 0),
                    x -> sp.edit().putInt("estilo", x).apply()));
                break;
            }
            case "flutuante": {
                Ui.add(c, Ui.switchItem(this, "Teclado flutuante", "Arraste pela alça; toque em \"arraste\" para ver os cantos de redimensionar",
                    sp.getBoolean("flutuante", false), on -> sp.edit().putBoolean("flutuante", on).apply()));
                Ui.add(c, Ui.item(this, "Resetar tamanho e posição", null, v -> {
                    sp.edit().remove("fw").remove("fesc").remove("fx").remove("fy").apply();
                    Toast.makeText(this, "Resetado", Toast.LENGTH_SHORT).show();
                }));
                break;
            }
            default: {
                Ui.add(c, Ui.seekItem(this, "Altura das teclas", 36, 64, sp.getInt("altura", 48), " dp",
                    x -> sp.edit().putInt("altura", x).apply()));
                Ui.add(c, Ui.seekItem(this, "Espaço entre as teclas", 1, 6, sp.getInt("espac", 2), " dp",
                    x -> sp.edit().putInt("espac", x).apply()));
                Ui.add(c, Ui.seekItem(this, "Margem inferior", 0, 40, sp.getInt("margem", 6), " dp",
                    x -> sp.edit().putInt("margem", x).apply()));
                Ui.add(c, Ui.switchItem(this, "Animação ao tocar", "A tecla encolhe um pouco ao ser tocada",
                    sp.getBoolean("efeitos", true), on -> sp.edit().putBoolean("efeitos", on).apply()));
            }
        }
        col.addView(c);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String nome = (req == 1) ? "fonte_custom" : "som_custom";
        try (InputStream in = getContentResolver().openInputStream(uri);
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
    }
}
