package com.jrocha.teclado;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class TemasActivity extends Activity {

    @Override protected void onResume() {
        super.onResume();
        montar();
    }

    void editorNovo(Tema base) {
        Tema n = base.copia();
        n.id = "c" + System.currentTimeMillis();
        n.nome = "Meu tema";
        try {
            Intent i = new Intent(this, EditorTemaActivity.class);
            i.putExtra("json", n.toJson().toString());
            startActivity(i);
        } catch (Exception e) { }
    }

    void montar() {
        Ui.Tela t = Ui.tela(this, "Temas", null);
        TextView novo = Ui.txt(this, "＋  Novo tema", 17, Ui.ACENTO);
        novo.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF3A3450);
        g.setCornerRadius(Ui.dp(this, 16));
        novo.setBackground(g);
        novo.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        novo.setOnClickListener(v -> editorNovo(TemaStore.atual(this)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, Ui.dp(this, 8), 0, 0);
        t.col.addView(novo, lp);

        Ui.secao(t.col, "MEUS TEMAS");
        List<Tema> l = TemaStore.todos(this);
        String atual = TemaStore.atual(this).id;
        LinearLayout linha = null;
        for (int i = 0; i < l.size(); i++) {
            if (i % 2 == 0) {
                linha = new LinearLayout(this);
                linha.setOrientation(LinearLayout.HORIZONTAL);
                t.col.addView(linha, new LinearLayout.LayoutParams(-1, -2));
            }
            linha.addView(cartao(l.get(i), atual.equals(l.get(i).id)));
        }
        if (l.size() % 2 == 1 && linha != null) {
            View sp = new View(this);
            sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
            linha.addView(sp);
        }
        TextView dica = Ui.txt(this, "Toque no tema para usar. Toque e segure um tema criado por você para excluir.", 13, Ui.SUB);
        dica.setPadding(Ui.dp(this, 4), Ui.dp(this, 16), 0, 0);
        t.col.addView(dica);
    }

    View cartao(final Tema tema, boolean sel) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Ui.CARD);
        bg.setCornerRadius(Ui.dp(this, 16));
        if (sel) bg.setStroke(Ui.dp(this, 2), Ui.ACENTO);
        c.setBackground(bg);
        int p = Ui.dp(this, 8);
        c.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        c.setLayoutParams(lp);

        c.addView(Ui.preview(this, tema), new LinearLayout.LayoutParams(-1, -2));
        TextView nome = Ui.txt(this, (sel ? "✓ " : "") + tema.nome, 15, sel ? Ui.ACENTO : Ui.TEXTO);
        nome.setPadding(Ui.dp(this, 2), Ui.dp(this, 8), 0, Ui.dp(this, 4));
        nome.setSingleLine(true);
        c.addView(nome);
        final boolean custom = TemaStore.ehCustom(tema);
        TextView acao = Ui.txt(this, custom ? "editar" : "duplicar", 14, Ui.ACENTO);
        acao.setGravity(Gravity.CENTER);
        GradientDrawable ab = new GradientDrawable();
        ab.setColor(0xFF2C2935);
        ab.setCornerRadius(Ui.dp(this, 20));
        acao.setBackground(ab);
        acao.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        acao.setOnClickListener(v -> {
            if (custom) {
                Intent i = new Intent(this, EditorTemaActivity.class);
                i.putExtra("id", tema.id);
                startActivity(i);
            } else {
                editorNovo(tema);
            }
        });
        c.addView(acao, new LinearLayout.LayoutParams(-1, -2));

        c.setOnClickListener(v -> { TemaStore.selecionar(this, tema.id); montar(); });
        if (custom) {
            c.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle("Excluir \"" + tema.nome + "\"?")
                    .setPositiveButton("Excluir", (d, w) -> { TemaStore.excluir(this, tema.id); montar(); })
                    .setNegativeButton("Cancelar", null)
                    .show();
                return true;
            });
        }
        return c;
    }
}
