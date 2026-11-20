package com.jrocha.teclado;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class Ui {
    public interface IntCb { void on(int v); }
    public interface BoolCb { void on(boolean v); }

    public static final int FUNDO = 0xFF121016, BARRA = 0xFF1F1D26, CARD = 0xFF1B1922,
        ACENTO = 0xFFCDB8FF, TEXTO = 0xFFEDE8F5, SUB = 0xFFA9A3B5, DIV = 0xFF2C2935;

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + .5f);
    }

    public static TextView txt(Context c, String s, float sp, int cor) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(cor);
        return t;
    }

    public static class Tela {
        public LinearLayout root, col;
        public FrameLayout extra;
    }

    public static Tela tela(Activity a, String titulo, Runnable onOk) {
        Tela t = new Tela();
        t.root = new LinearLayout(a);
        t.root.setOrientation(LinearLayout.VERTICAL);
        t.root.setBackgroundColor(FUNDO);
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(BARRA);
        TextView voltar = txt(a, "←", 24, TEXTO);
        voltar.setGravity(Gravity.CENTER);
        voltar.setPadding(dp(a, 16), 0, dp(a, 16), 0);
        voltar.setOnClickListener(v -> a.finish());
        bar.addView(voltar, new LinearLayout.LayoutParams(-2, -1));
        TextView tit = txt(a, titulo, 20, TEXTO);
        tit.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(tit, new LinearLayout.LayoutParams(0, -1, 1f));
        if (onOk != null) {
            TextView ok = txt(a, "✓", 26, ACENTO);
            ok.setGravity(Gravity.CENTER);
            ok.setPadding(dp(a, 18), 0, dp(a, 18), 0);
            ok.setOnClickListener(v -> onOk.run());
            bar.addView(ok, new LinearLayout.LayoutParams(-2, -1));
        }
        t.root.addView(bar, new LinearLayout.LayoutParams(-1, dp(a, 56)));
        t.extra = new FrameLayout(a);
        t.root.addView(t.extra, new LinearLayout.LayoutParams(-1, -2));
        ScrollView sv = new ScrollView(a);
        t.col = new LinearLayout(a);
        t.col.setOrientation(LinearLayout.VERTICAL);
        t.col.setPadding(dp(a, 14), dp(a, 6), dp(a, 14), dp(a, 24));
        sv.addView(t.col);
        t.root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        a.setContentView(t.root);
        return t;
    }

    public static void secao(LinearLayout col, String s) {
        TextView t = txt(col.getContext(), s, 13, TEXTO);
        t.setPadding(dp(col.getContext(), 4), dp(col.getContext(), 18), 0, dp(col.getContext(), 8));
        col.addView(t);
    }

    public static LinearLayout card(LinearLayout col) {
        Context c = col.getContext();
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(c, 16));
        l.setBackground(g);
        l.setPadding(dp(c, 16), dp(c, 2), dp(c, 16), dp(c, 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(c, 8));
        col.addView(l, lp);
        return l;
    }

    public static void add(LinearLayout card, View row) {
        if (card.getChildCount() > 0) {
            View d = new View(card.getContext());
            d.setBackgroundColor(DIV);
            card.addView(d, new LinearLayout.LayoutParams(-1, 1));
        }
        card.addView(row);
    }

    public static LinearLayout item(Context c, String titulo, String sub, View.OnClickListener ck) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, dp(c, 14), 0, dp(c, 14));
        l.addView(txt(c, titulo, 17, TEXTO));
        if (sub != null) l.addView(txt(c, sub, 13, SUB));
        if (ck != null) l.setOnClickListener(ck);
        return l;
    }

    public static LinearLayout toggle(Context c, String titulo, boolean val, BoolCb cb) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(0, dp(c, 10), 0, dp(c, 10));
        TextView t = txt(c, titulo, 17, TEXTO);
        l.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch sw = new Switch(c);
        sw.setChecked(val);
        sw.setOnCheckedChangeListener((x, on) -> cb.on(on));
        l.addView(sw);
        l.setOnClickListener(v -> sw.toggle());
        return l;
    }

    public static LinearLayout cor(Context c, String titulo, int valor, IntCb cb) {
        final int[] cur = {valor};
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(0, dp(c, 12), 0, dp(c, 12));
        l.addView(txt(c, titulo, 17, TEXTO), new LinearLayout.LayoutParams(0, -2, 1f));
        final View bola = new View(c);
        final GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(valor);
        g.setStroke(dp(c, 1), 0x66FFFFFF);
        bola.setBackground(g);
        l.addView(bola, new LinearLayout.LayoutParams(dp(c, 28), dp(c, 28)));
        l.setOnClickListener(v -> ColorDialog.mostrar(c, cur[0], n -> {
            cur[0] = n;
            g.setColor(n);
            bola.invalidate();
            cb.on(n);
        }));
        return l;
    }

    public static LinearLayout slider(Context c, String titulo, int min, int max, int val, IntCb cb) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, dp(c, 12), 0, dp(c, 8));
        l.addView(txt(c, titulo, 17, TEXTO));
        SeekBar s = new SeekBar(c);
        s.setMax(max - min);
        s.setProgress(Math.max(0, Math.min(max - min, val - min)));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar x, int p, boolean u) { if (u) cb.on(p + min); }
            @Override public void onStartTrackingTouch(SeekBar x) { }
            @Override public void onStopTrackingTouch(SeekBar x) { }
        });
        l.addView(s, new LinearLayout.LayoutParams(-1, -2));
        return l;
    }

    public static RadioGroup radio(Context c, String[] ops, int atual, IntCb cb) {
        RadioGroup rg = new RadioGroup(c);
        for (int i = 0; i < ops.length; i++) {
            RadioButton rb = new RadioButton(c);
            rb.setText(ops[i]);
            rb.setTextSize(17);
            rb.setTextColor(TEXTO);
            rb.setId(100 + i);
            rb.setPadding(dp(c, 8), dp(c, 12), 0, dp(c, 12));
            rg.addView(rb);
            if (i == atual) rb.setChecked(true);
        }
        rg.setOnCheckedChangeListener((g, id) -> cb.on(id - 100));
        return rg;
    }

    public static View preview(Context c, Tema t) {
        float d = c.getResources().getDisplayMetrics().density;
        FrameLayout f = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{t.bg, t.bgGrad ? t.bg2 : t.bg});
        bg.setCornerRadius(dp(c, 12));
        bg.setStroke(dp(c, 1), 0x55FFFFFF);
        f.setBackground(bg);
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(c, 6), dp(c, 6), dp(c, 6), dp(c, 6));
        LinearLayout r1 = new LinearLayout(c);
        String[] L = {"A", "B", "C", "D"};
        for (String s : L) r1.addView(tecla(c, t, 0, s, 1f, d));
        LinearLayout r2 = new LinearLayout(c);
        r2.addView(tecla(c, t, 1, "⇧", 1f, d));
        r2.addView(tecla(c, t, 3, "", 3f, d));
        r2.addView(tecla(c, t, 2, "↵", 1f, d));
        col.addView(r1, new LinearLayout.LayoutParams(-1, -2));
        col.addView(r2, new LinearLayout.LayoutParams(-1, -2));
        f.addView(col, new FrameLayout.LayoutParams(-1, -2));
        return f;
    }

    static View tecla(Context c, Tema t, int g, String s, float peso, float d) {
        TextView v = txt(c, s, 14, t.g[g].text);
        v.setGravity(Gravity.CENTER);
        v.setBackground(new TeclaDrawable(t, g, d));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 30), peso);
        lp.setMargins(dp(c, 1), dp(c, 1), dp(c, 1), dp(c, 1));
        v.setLayoutParams(lp);
        return v;
    }
}
