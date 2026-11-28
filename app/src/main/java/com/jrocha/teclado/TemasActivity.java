package com.jrocha.teclado;

import android.content.Intent;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TemasActivity extends BaseTela {

    @Override protected String titulo() { return "Temas"; }

    void abrirEditor(String id) {
        Intent i = new Intent(this, EditorTemaActivity.class);
        i.putExtra("id", id);
        startActivity(i);
    }

    @Override protected void construir() {
        LinearLayout topo = new LinearLayout(this);
        topo.addView(Ui.botaoPilula(this, "＋  Novo tema", v -> {
            Tema n = Tema.selecionado(sp).copia("Meu tema " + (Tema.custom(sp).size() + 1));
            Tema.salvar(sp, n);
            sp.edit().putString("tema_id", n.id).apply();
            abrirEditor(n.id);
        }));
        col.addView(topo);
        col.addView(Ui.secao(this, "Meus temas"));
        String sel = sp.getString("tema_id", "b0");
        for (final Tema t : Tema.todos(sp)) {
            LinearLayout card = Ui.cartao(this);
            int p = Ui.dp(this, 12);
            card.setPadding(p, p, p, p);
            card.addView(Ui.preview(this, t));
            TextView nome = new TextView(this);
            nome.setText(t.nome + (sel.equals(t.id) ? "   ✓ em uso" : ""));
            nome.setTextColor(sel.equals(t.id) ? Ui.ACC : Ui.TXT);
            nome.setTextSize(17);
            nome.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), 0, Ui.dp(this, 2));
            card.addView(nome);
            LinearLayout bt = new LinearLayout(this);
            bt.setGravity(Gravity.START);
            if (!sel.equals(t.id))
                bt.addView(Ui.botaoPilula(this, "Usar", v -> {
                    sp.edit().putString("tema_id", t.id).apply();
                    recriar();
                }));
            if (!t.ePronto())
                bt.addView(Ui.botaoPilula(this, "Editar", v -> abrirEditor(t.id)));
            bt.addView(Ui.botaoPilula(this, "Duplicar", v -> {
                Tema n = t.copia(t.nome + " (cópia)");
                Tema.salvar(sp, n);
                recriar();
            }));
            if (!t.ePronto())
                bt.addView(Ui.botaoPilula(this, "Excluir", v -> {
                    Tema.excluir(sp, t.id);
                    recriar();
                }));
            card.addView(bt);
            col.addView(card);
        }
    }

    void recriar() {
        col.removeAllViews();
        construir();
    }
}
