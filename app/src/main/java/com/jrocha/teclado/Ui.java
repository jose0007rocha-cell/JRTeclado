package com.jrocha.teclado;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Componentes de interface (estilo escuro parecido com o Yandex Keyboard). */
public class Ui {
    // paleta do app (muda conforme o modo escuro / claro / do sistema)
    public static int BG = 0xFF1E1E24, CARD = 0xFF121216, TXT = 0xFFEDE7F6, SUB = 0xFFB8B2CC,
        ACC = 0xFFD0BCFF, LINE = 0xFF2C2C34, PILULA = 0xFF4A4458;
    public static boolean escuro = true;

    /** modo_app: 0 = usar do sistema, 1 = claro, 2 = escuro. */
    public static boolean calcularEscuro(Context c) {
        int m = c.getSharedPreferences("jrteclado", Context.MODE_PRIVATE).getInt("modo_app", 0);
        if (m == 1) return false;
        if (m == 2) return true;
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void aplicarPaleta(Context c) {
        escuro = calcularEscuro(c);
        if (escuro) {
            BG = 0xFF1E1E24; CARD = 0xFF121216; TXT = 0xFFEDE7F6; SUB = 0xFFB8B2CC;
            ACC = 0xFFD0BCFF; LINE = 0xFF2C2C34; PILULA = 0xFF4A4458;
        } else {
            BG = 0xFFF3F0F8; CARD = 0xFFFFFFFF; TXT = 0xFF1D1B20; SUB = 0xFF5F5A6B;
            ACC = 0xFF6750A4; LINE = 0xFFE6E0EC; PILULA = 0xFFE6DEF7;
        }
    }

    public interface OnInt { void v(int x); }
    public interface OnBool { void v(boolean b); }

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static LinearLayout cartao(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(c, 22));
        l.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(c, 6), 0, dp(c, 10));
        l.setLayoutParams(lp);
        l.setPadding(dp(c, 16), dp(c, 4), dp(c, 16), dp(c, 4));
        return l;
    }

    public static TextView secao(Context c, String t) {
        TextView v = new TextView(c);
        v.setText(t.toUpperCase());
        v.setTextColor(TXT);
        v.setTextSize(13);
        v.setPadding(dp(c, 6), dp(c, 14), 0, dp(c, 2));
        return v;
    }

    static View divisor(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
        return v;
    }

    public static void add(LinearLayout cartao, View v) {
        if (cartao.getChildCount() > 0) cartao.addView(divisor(cartao.getContext()));
        cartao.addView(v);
    }

    static LinearLayout linhaBase(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(0, dp(c, 14), 0, dp(c, 14));
        return l;
    }

    static LinearLayout textos(Context c, String t, String s) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        TextView a = new TextView(c);
        a.setText(t);
        a.setTextColor(TXT);
        a.setTextSize(17);
        v.addView(a);
        if (s != null) {
            TextView b = new TextView(c);
            b.setText(s);
            b.setTextColor(SUB);
            b.setTextSize(14);
            v.addView(b);
        }
        return v;
    }

    public static View item(Context c, String titulo, String sub, View.OnClickListener ck) {
        LinearLayout l = linhaBase(c);
        l.addView(textos(c, titulo, sub), new LinearLayout.LayoutParams(0, -2, 1f));
        if (ck != null) {
            TextView seta = new TextView(c);
            seta.setText("›");
            seta.setTextColor(SUB);
            seta.setTextSize(24);
            l.addView(seta);
            l.setOnClickListener(ck);
        }
        return l;
    }

    public static View switchItem(Context c, String titulo, String sub, boolean on, final OnBool ob) {
        LinearLayout l = linhaBase(c);
        l.addView(textos(c, titulo, sub), new LinearLayout.LayoutParams(0, -2, 1f));
        final Switch sw = new Switch(c);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener((b, x) -> ob.v(x));
        l.addView(sw);
        l.setOnClickListener(v -> sw.toggle());
        return l;
    }

    public static View seekItem(Context c, final String titulo, final int min, int max, int val, final String suf, final OnInt ol) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(0, dp(c, 12), 0, dp(c, 8));
        final TextView t = new TextView(c);
        t.setTextColor(TXT);
        t.setTextSize(17);
        t.setText(titulo + ": " + val + suf);
        SeekBar s = new SeekBar(c);
        s.setMax(max - min);
        s.setProgress(val - min);
        s.setProgressTintList(ColorStateList.valueOf(ACC));
        s.setThumbTintList(ColorStateList.valueOf(ACC));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                t.setText(titulo + ": " + (p + min) + suf);
                ol.v(p + min);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        v.addView(t);
        v.addView(s);
        return v;
    }

    static void pintar(View dot, int cor, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(cor);
        g.setStroke(dp(c, 1), SUB);
        dot.setBackground(g);
    }

    public static View corItem(final Context c, String titulo, int cor, final OnInt ol) {
        LinearLayout r = linhaBase(c);
        TextView t = new TextView(c);
        t.setText(titulo);
        t.setTextColor(TXT);
        t.setTextSize(17);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        final View dot = new View(c);
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 30), dp(c, 30)));
        final int[] atual = {cor};
        pintar(dot, cor, c);
        r.addView(dot);
        r.setOnClickListener(v -> dialogoCor(c, atual[0], nc -> {
            atual[0] = nc;
            pintar(dot, nc, c);
            ol.v(nc);
        }));
        return r;
    }

    public static View radio(Context c, String[] ops, int atual, final OnInt ol) {
        RadioGroup rg = new RadioGroup(c);
        rg.setPadding(0, dp(c, 6), 0, dp(c, 6));
        for (int i = 0; i < ops.length; i++) {
            RadioButton rb = new RadioButton(c);
            rb.setText(ops[i]);
            rb.setTextColor(TXT);
            rb.setTextSize(16);
            rb.setButtonTintList(ColorStateList.valueOf(ACC));
            rb.setId(100 + i);
            rb.setPadding(dp(c, 8), dp(c, 10), 0, dp(c, 10));
            rg.addView(rb);
            if (i == atual) rb.setChecked(true);
        }
        rg.setOnCheckedChangeListener((g, id) -> ol.v(id - 100));
        return rg;
    }

    public static TextView botaoPilula(Context c, String texto, View.OnClickListener ck) {
        TextView t = new TextView(c);
        t.setText(texto);
        t.setTextColor(TXT);
        t.setTextSize(15);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(PILULA);
        g.setCornerRadius(dp(c, 22));
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, dp(c, 4), dp(c, 8), dp(c, 4));
        t.setLayoutParams(lp);
        t.setOnClickListener(ck);
        return t;
    }

    // ---------- seletor de cor ----------
    static GradientDrawable grad(Context c, int a, int b) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{a, b});
        g.setCornerRadius(dp(c, 10));
        return g;
    }

    static SeekBar barra(Context c, int max, int p) {
        SeekBar sb = new SeekBar(c);
        sb.setMax(max);
        sb.setProgress(p);
        sb.setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
        sb.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        sb.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(c, 34));
        lp.setMargins(0, dp(c, 4), 0, dp(c, 8));
        sb.setLayoutParams(lp);
        return sb;
    }

    static TextView rotulo(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(SUB);
        t.setTextSize(13);
        t.setPadding(0, dp(c, 8), 0, 0);
        return t;
    }

    public static void dialogoCor(final Context c, int inicial, final OnInt res) {
        float[] hsv = new float[3];
        Color.colorToHSV(inicial, hsv);
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 20), dp(c, 16), dp(c, 20), dp(c, 8));
        final View prev = new View(c);
        l.addView(prev, new LinearLayout.LayoutParams(-1, dp(c, 56)));
        final SeekBar h = barra(c, 360, (int) hsv[0]);
        final SeekBar s = barra(c, 100, (int) (hsv[1] * 100));
        final SeekBar v = barra(c, 100, (int) (hsv[2] * 100));
        h.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000}));
        final EditText hex = new EditText(c);
        hex.setTextColor(TXT);
        hex.setHintTextColor(SUB);
        hex.setHint("#RRGGBB");
        hex.setSingleLine(true);
        hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        l.addView(rotulo(c, "Matiz"));
        l.addView(h);
        l.addView(rotulo(c, "Saturação"));
        l.addView(s);
        l.addView(rotulo(c, "Brilho"));
        l.addView(v);
        l.addView(rotulo(c, "Código HEX"));
        l.addView(hex);
        final boolean[] semHex = {false};
        final int[] atual = {inicial | 0xFF000000};
        final Runnable atualizar = new Runnable() {
            @Override public void run() {
                int cor = Color.HSVToColor(new float[]{h.getProgress(), s.getProgress() / 100f, v.getProgress() / 100f});
                atual[0] = cor;
                prev.setBackgroundColor(cor);
                int puro = Color.HSVToColor(new float[]{h.getProgress(), 1f, 1f});
                s.setBackground(grad(c, Color.WHITE, puro));
                v.setBackground(grad(c, Color.BLACK, puro));
                if (!semHex[0]) hex.setText(String.format("#%06X", cor & 0xFFFFFF));
            }
        };
        SeekBar.OnSeekBarChangeListener ls = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) { atualizar.run(); }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        };
        h.setOnSeekBarChangeListener(ls);
        s.setOnSeekBarChangeListener(ls);
        v.setOnSeekBarChangeListener(ls);
        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c2, int d) { }
            @Override public void onTextChanged(CharSequence a, int b, int c2, int d) { }
            @Override public void afterTextChanged(Editable e) {
                String t = e.toString().trim();
                if (t.startsWith("#")) t = t.substring(1);
                if (t.length() != 6) return;
                try {
                    int cor = 0xFF000000 | Integer.parseInt(t, 16);
                    float[] f = new float[3];
                    Color.colorToHSV(cor, f);
                    semHex[0] = true;
                    h.setProgress((int) f[0]);
                    s.setProgress((int) (f[1] * 100));
                    v.setProgress((int) (f[2] * 100));
                    semHex[0] = false;
                    atual[0] = cor;
                    prev.setBackgroundColor(cor);
                } catch (Exception ex) {
                    semHex[0] = false;
                }
            }
        });
        atualizar.run();
        ScrollView sv = new ScrollView(c);
        sv.addView(l);
        new AlertDialog.Builder(c, escuro ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle("Escolher cor")
            .setView(sv)
            .setPositiveButton("OK", (dlg, w) -> res.v(atual[0]))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    // ---------- previa do tema ----------
    static LinearLayout linhaMini(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    static View mini(Context c, Tema t, Tema.Grupo g, String s, float peso) {
        FrameLayout f = new FrameLayout(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 34), peso);
        lp.setMargins(dp(c, 1), dp(c, 1), dp(c, 1), dp(c, 1));
        f.setLayoutParams(lp);
        f.setBackground(new TeclaDrawable(t, g, c.getResources().getDisplayMetrics().density));
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextColor(g.texto);
        tv.setTextSize(s.length() > 2 ? 10 : 14);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        f.addView(tv, new FrameLayout.LayoutParams(-1, -1));
        return f;
    }

    public static View preview(Context c, Tema t) {
        TeclaDrawable.larg = Math.max(0.6f, Math.min(1f,
            c.getSharedPreferences("jrteclado", Context.MODE_PRIVATE).getInt("larg_teclas", 100) / 100f));
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 6));
        GradientDrawable bgp = t.fundo();
        bgp.setCornerRadius(dp(c, 14));
        root.setBackground(bgp);
        LinearLayout bar = new LinearLayout(c);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Tema.comAlpha(t.barraCor, t.barraAlpha));
        int[] tipos = {0, 1, 2, 3, 8, 6};
        for (int tp : tipos) {
            IconeView iv = new IconeView(c, tp, t.barraTexto, false);
            iv.setEscala(0.8f);
            iv.setLayoutParams(new LinearLayout.LayoutParams(0, dp(c, 30), 1f));
            bar.addView(iv);
        }
        root.addView(bar);
        LinearLayout l1 = linhaMini(c);
        for (String s : "qwertyui".split("(?!^)")) l1.addView(mini(c, t, t.principal, s, 1f));
        LinearLayout l2 = linhaMini(c);
        l2.addView(mini(c, t, t.funcao, "⇧", 1.4f));
        for (String s : "zxcv".split("(?!^)")) l2.addView(mini(c, t, t.principal, s, 1f));
        l2.addView(mini(c, t, t.funcao, "⌫", 1.4f));
        LinearLayout l3 = linhaMini(c);
        l3.addView(mini(c, t, t.funcao, "?123", 1.4f));
        l3.addView(mini(c, t, t.principal, ",", 1f));
        l3.addView(mini(c, t, t.espaco, "espaço", 4f));
        l3.addView(mini(c, t, t.principal, ".", 1f));
        l3.addView(mini(c, t, t.enter, "↵", 1.4f));
        root.addView(l1);
        root.addView(l2);
        root.addView(l3);
        return root;
    }
}
