package com.jrocha.teclado;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EditorTemaActivity extends Activity {
    Tema t;
    boolean salvoAntes = false;
    Ui.Tela tela;
    EditText nomeEt;
    FrameLayout previewBox;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            String js = getIntent().getStringExtra("json");
            if (js != null) {
                t = Tema.fromJson(new JSONObject(js));
            } else {
                t = TemaStore.buscar(this, getIntent().getStringExtra("id"));
                salvoAntes = true;
            }
        } catch (Exception e) { t = null; }
        if (t == null) { finish(); return; }
        tela = Ui.tela(this, salvoAntes ? "Editar tema" : "Novo tema", this::salvar);
        previewBox = tela.extra;
        previewBox.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 4));
        construir();
        atualizarPreview();
    }

    void atualizarPreview() {
        previewBox.removeAllViews();
        previewBox.addView(Ui.preview(this, t), new FrameLayout.LayoutParams(-1, -2));
    }

    void salvar() {
        String n = nomeEt.getText().toString().trim();
        t.nome = n.isEmpty() ? "Meu tema" : n;
        TemaStore.salvar(this, t);
        TemaStore.selecionar(this, t.id);
        Toast.makeText(this, "Tema salvo", Toast.LENGTH_SHORT).show();
        finish();
    }

    void construir() {
        LinearLayout col = tela.col;

        LinearLayout cn = Ui.card(col);
        nomeEt = new EditText(this);
        nomeEt.setText(t.nome);
        nomeEt.setHint("Nome do tema");
        nomeEt.setSingleLine(true);
        nomeEt.setTextColor(Ui.TEXTO);
        nomeEt.setHintTextColor(Ui.SUB);
        cn.addView(nomeEt);

        // FUNDO
        Ui.secao(col, "FUNDO");
        LinearLayout cf = Ui.card(col);
        Ui.add(cf, Ui.cor(this, "Cor de fundo", t.bg, v -> { t.bg = v; atualizarPreview(); }));
        final View corFim = Ui.cor(this, "Cor final do gradiente", t.bg2, v -> { t.bg2 = v; atualizarPreview(); });
        corFim.setVisibility(t.bgGrad ? View.VISIBLE : View.GONE);
        Ui.add(cf, Ui.toggle(this, "Fazer gradiente de fundo", t.bgGrad, on -> {
            t.bgGrad = on;
            corFim.setVisibility(on ? View.VISIBLE : View.GONE);
            atualizarPreview();
        }));
        Ui.add(cf, corFim);
        Ui.add(cf, Ui.item(this, "Adicionar uma imagem ao fundo", "Substitui a cor (teclado fixo)", v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i, 10);
        }));
        Ui.add(cf, Ui.item(this, "Remover imagem de fundo", null, v -> {
            new File(getFilesDir(), "timg_" + t.id).delete();
            Toast.makeText(this, "Imagem removida", Toast.LENGTH_SHORT).show();
        }));

        // GRUPOS DE TECLAS
        grupo("TECLAS PRINCIPAIS", 0);
        grupo("TECLAS DE FUNÇÃO", 1);
        grupo("TECLA ENTER", 2);
        grupo("BARRA DE ESPAÇO", 3);

        // ASPECTO
        Ui.secao(col, "ASPECTO DAS TECLAS");
        LinearLayout ca = Ui.card(col);
        ca.addView(Ui.radio(this, new String[]{"Neon (brilho e luz)", "Suave (relevo)", "Pílula 3D", "Plano"}, t.efeito,
            v -> { t.efeito = v; atualizarPreview(); }));
        LinearLayout cr = Ui.card(col);
        final int[] rv = {t.raio >= 0 ? t.raio : 12};
        Ui.add(cr, Ui.toggle(this, "Raio dos cantos personalizado", t.raio >= 0, on -> {
            t.raio = on ? rv[0] : -1; atualizarPreview();
        }));
        Ui.add(cr, Ui.slider(this, "Raio dos cantos (dp)", 0, 40, rv[0], v -> {
            rv[0] = v;
            if (t.raio >= 0) t.raio = v;
            atualizarPreview();
        }));
        final View corContorno = Ui.cor(this, "Cor do contorno", t.stroke, v -> { t.stroke = v; atualizarPreview(); });
        corContorno.setVisibility(t.contorno ? View.VISIBLE : View.GONE);
        Ui.add(cr, Ui.toggle(this, "Contorno da tecla", t.contorno, on -> {
            t.contorno = on;
            corContorno.setVisibility(on ? View.VISIBLE : View.GONE);
            atualizarPreview();
        }));
        Ui.add(cr, corContorno);
        Ui.add(cr, Ui.cor(this, "Cor de destaque (brilho, linha)", t.accent, v -> { t.accent = v; atualizarPreview(); }));

        // BARRA SUPERIOR
        Ui.secao(col, "BARRA SUPERIOR");
        LinearLayout cb = Ui.card(col);
        Ui.add(cb, Ui.cor(this, "Cor de fundo do painel superior", t.topBg, v -> t.topBg = v));
        Ui.add(cb, Ui.slider(this, "Opacidade do fundo do painel superior (%)", 0, 100, t.topAlpha * 100 / 255,
            v -> t.topAlpha = v * 255 / 100));
        Ui.add(cb, Ui.cor(this, "Cor do texto do painel superior", t.topText, v -> t.topText = v));

        // EFEITOS
        Ui.secao(col, "EFEITOS");
        LinearLayout ce = Ui.card(col);
        ce.addView(Ui.radio(this, new String[]{"Sem efeito", "Neve", "Queda de folhas"}, t.fx, v -> t.fx = v));

        if (salvoAntes && TemaStore.ehCustom(t)) {
            LinearLayout cx = Ui.card(col);
            Ui.add(cx, Ui.item(this, "Excluir este tema", null, v ->
                new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle("Excluir \"" + t.nome + "\"?")
                    .setPositiveButton("Excluir", (d, w) -> { TemaStore.excluir(this, t.id); finish(); })
                    .setNegativeButton("Cancelar", null)
                    .show()));
        }
    }

    void grupo(String titulo, int idx) {
        final Tema.Grupo g = t.g[idx];
        Ui.secao(tela.col, titulo);
        LinearLayout c = Ui.card(tela.col);
        Ui.add(c, Ui.slider(this, "Opacidade da tecla (%)", 0, 100, g.alpha * 100 / 255, v -> {
            g.alpha = v * 255 / 100; atualizarPreview();
        }));
        Ui.add(c, Ui.cor(this, "Cor de fundo", g.bg, v -> { g.bg = v; atualizarPreview(); }));
        final View fim = Ui.cor(this, "Cor final do gradiente", g.bg2, v -> { g.bg2 = v; atualizarPreview(); });
        fim.setVisibility(g.grad ? View.VISIBLE : View.GONE);
        Ui.add(c, Ui.toggle(this, "Fazer gradiente de fundo", g.grad, on -> {
            g.grad = on;
            fim.setVisibility(on ? View.VISIBLE : View.GONE);
            atualizarPreview();
        }));
        Ui.add(c, fim);
        Ui.add(c, Ui.toggle(this, "Sombras e brilho das teclas", g.sombra, on -> { g.sombra = on; atualizarPreview(); }));
        Ui.add(c, Ui.cor(this, "Cor do texto", g.text, v -> { g.text = v; atualizarPreview(); }));
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 10 || res != RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream in = getContentResolver().openInputStream(data.getData());
             FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "timg_" + t.id))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            Toast.makeText(this, "Imagem aplicada ao fundo", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao copiar a imagem", Toast.LENGTH_LONG).show();
        }
    }
}
