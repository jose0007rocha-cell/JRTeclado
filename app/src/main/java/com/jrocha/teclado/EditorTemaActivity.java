package com.jrocha.teclado;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class EditorTemaActivity extends BaseTela {
    Tema t;
    FrameLayout previewHolder;

    @Override protected String titulo() { return "Novo tema"; }
    @Override protected boolean recarregarAoVoltar() { return false; }

    @Override protected View acaoTopo() {
        TextView ok = new TextView(this);
        ok.setText("✓");
        ok.setTextSize(26);
        ok.setTextColor(Ui.TXT);
        ok.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 4), 0);
        ok.setOnClickListener(v -> {
            if (t != null) sp.edit().putString("tema_id", t.id).apply();
            finish();
        });
        return ok;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        t = Tema.porId(sp, getIntent().getStringExtra("id"));
        if (t == null || t.ePronto()) { t = null; finish(); }
    }

    void alterado(boolean preview) {
        Tema.salvar(sp, t);
        if (preview) atualizarPreview();
    }

    void atualizarPreview() {
        previewHolder.removeAllViews();
        previewHolder.addView(Ui.preview(this, t));
    }

    void grupo(String nome, final Tema.Grupo g) {
        col.addView(Ui.secao(this, nome));
        LinearLayout c = Ui.cartao(this);
        Ui.add(c, Ui.seekItem(this, "Opacidade da tecla", 0, 100, g.alpha * 100 / 255, "%", x -> { g.alpha = x * 255 / 100; alterado(true); }));
        Ui.add(c, Ui.switchItem(this, "Fundo da tecla", null, g.fundo, on -> { g.fundo = on; alterado(true); }));
        Ui.add(c, Ui.corItem(this, "Cor de fundo", g.cor, x -> { g.cor = x; alterado(true); }));
        Ui.add(c, Ui.switchItem(this, "Fazer gradiente", null, g.grad, on -> { g.grad = on; alterado(true); }));
        Ui.add(c, Ui.switchItem(this, "Sombras e brilho", null, g.sombra, on -> { g.sombra = on; alterado(true); }));
        Ui.add(c, Ui.corItem(this, "Cor do texto", g.texto, x -> { g.texto = x; alterado(true); }));
        col.addView(c);
    }

    @Override protected void construir() {
        if (t == null) return;
        previewHolder = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        col.addView(previewHolder, lp);
        atualizarPreview();

        LinearLayout cn = Ui.cartao(this);
        EditText en = new EditText(this);
        en.setText(t.nome);
        en.setHint("Nome do tema");
        en.setTextColor(Ui.TXT);
        en.setHintTextColor(Ui.SUB);
        en.setSingleLine(true);
        en.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { t.nome = e.toString(); alterado(false); }
        });
        cn.addView(en);
        col.addView(cn);

        col.addView(Ui.secao(this, "Estilo das teclas"));
        LinearLayout ce = Ui.cartao(this);
        Ui.add(ce, Ui.radio(this, new String[]{"Plano", "Neon (brilho)", "Relevo suave", "Pílula 3D"}, t.estilo,
            x -> { t.estilo = x; alterado(true); }));
        col.addView(ce);

        col.addView(Ui.secao(this, "Fundo"));
        LinearLayout cf = Ui.cartao(this);
        Ui.add(cf, Ui.corItem(this, "Cor de fundo", t.bg, x -> { t.bg = x; alterado(true); }));
        Ui.add(cf, Ui.switchItem(this, "Fazer gradiente de fundo", null, t.bgGrad, on -> { t.bgGrad = on; alterado(true); }));
        col.addView(cf);

        grupo("Teclas principais", t.principal);
        grupo("Teclas de função", t.funcao);
        grupo("Tecla Enter", t.enter);
        grupo("Barra de espaço", t.espaco);

        col.addView(Ui.secao(this, "Aparência das teclas"));
        LinearLayout ca = Ui.cartao(this);
        Ui.add(ca, Ui.seekItem(this, "Raio dos cantos", 0, 40, t.raio, " dp", x -> { t.raio = x; alterado(true); }));
        Ui.add(ca, Ui.switchItem(this, "Contorno da tecla", null, t.contorno, on -> { t.contorno = on; alterado(true); }));
        Ui.add(ca, Ui.corItem(this, "Cor do contorno", t.corContorno, x -> { t.corContorno = x; alterado(true); }));
        col.addView(ca);

        col.addView(Ui.secao(this, "Barra superior"));
        LinearLayout cb = Ui.cartao(this);
        Ui.add(cb, Ui.corItem(this, "Cor de fundo do painel superior", t.barraCor, x -> { t.barraCor = x; alterado(true); }));
        Ui.add(cb, Ui.seekItem(this, "Opacidade do fundo do painel", 0, 100, t.barraAlpha * 100 / 255, "%",
            x -> { t.barraAlpha = x * 255 / 100; alterado(true); }));
        Ui.add(cb, Ui.corItem(this, "Cor dos ícones e do texto", t.barraTexto, x -> { t.barraTexto = x; alterado(true); }));
        col.addView(cb);

        col.addView(Ui.secao(this, "Cor de destaque"));
        LinearLayout cd = Ui.cartao(this);
        Ui.add(cd, Ui.corItem(this, "Brilho, contorno ao tocar e alça", t.accent, x -> { t.accent = x; alterado(true); }));
        col.addView(cd);

        col.addView(Ui.secao(this, "Efeitos"));
        LinearLayout cx = Ui.cartao(this);
        Ui.add(cx, Ui.radio(this, new String[]{"Sem efeito", "Neve", "Queda de folhas"}, t.efeito,
            x -> { t.efeito = x; alterado(false); }));
        col.addView(cx);

        col.addView(Ui.secao(this, "Teste"));
        LinearLayout ct = Ui.cartao(this);
        EditText teste = new EditText(this);
        teste.setHint("Toque para testar (use ✓ para aplicar o tema)");
        teste.setTextColor(Ui.TXT);
        teste.setHintTextColor(Ui.SUB);
        ct.addView(teste);
        col.addView(ct);
    }
}
