package com.jrocha.teclado;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.File;

public class TecladoService extends InputMethodService {

    static final String[] ESTILOS = {"Normal", "BIG", "Random", "Bold", "Italic", "Cursivo", "Mono", "Largo", "Círculo"};
    static final String[] EMOJIS = {
        "😀","😂","🤣","😊","😍","😘","😎","🤔",
        "😢","😭","😡","👍","👎","👏","🙏","💪",
        "🔥","❤️","💔","🎉","✨","🌟","💯","✅",
        "❌","😴","🤝","👀","🙄","😅","😉","🥰"};

    SharedPreferences sp;
    Tema tema;
    Typeface tf = Typeface.SANS_SERIF;
    float escW = 1f, escH = 1f;
    int estilo = 0, modo = 0; // modo: 0 letras, 1 simbolos, 2 simbolos2, 3 emoji
    boolean shift = false, caps = false, flutuante = false;
    long ultimoShift = 0;
    FrameLayout host, holder;
    PopupWindow popup;
    EditorInfo ei;
    SoundPool pool;
    int soundId = 0;
    long soundStamp = 0;
    AudioManager am;
    Bitmap imgBg;
    String imgKey = "";
    Handler h = new Handler();
    Runnable repApagar = new Runnable() {
        @Override public void run() { apagar(); h.postDelayed(this, 60); }
    };

    int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    float dens() { return getResources().getDisplayMetrics().density; }

    @Override public void onCreate() {
        super.onCreate();
        sp = getSharedPreferences("jrteclado", Context.MODE_PRIVATE);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        pool = new SoundPool.Builder().setMaxStreams(4)
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build();
        carregarPrefs();
    }

    @Override public void onDestroy() {
        fecharFlutuante();
        if (pool != null) pool.release();
        super.onDestroy();
    }

    void carregarPrefs() {
        tema = TemaStore.atual(this);
        flutuante = sp.getBoolean("flutuante", false);
        estilo = sp.getInt("estilo", 0);
        tf = carregarFonte();
        int sw0 = getResources().getDisplayMetrics().widthPixels;
        escH = flutuante ? sp.getFloat("fesc", 1f) : 1f;
        escW = flutuante ? Math.max(.7f, Math.min(1.5f, larguraFlutuante() / (sw0 * .9f))) : 1f;
        File f = new File(getFilesDir(), "som_custom");
        if (f.exists() && f.lastModified() != soundStamp) {
            if (soundId != 0) pool.unload(soundId);
            soundId = pool.load(f.getAbsolutePath(), 1);
            soundStamp = f.lastModified();
        }
    }

    Typeface carregarFonte() {
        switch (sp.getInt("fonte", 0)) {
            case 1: return Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
            case 2: return Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC);
            case 3: return Typeface.SERIF;
            case 4: return Typeface.MONOSPACE;
            case 5: return Typeface.create("cursive", Typeface.NORMAL);
            case 6:
                try {
                    File ff = new File(getFilesDir(), "fonte_custom");
                    if (ff.exists()) return Typeface.createFromFile(ff);
                } catch (Exception e) { }
                return Typeface.SANS_SERIF;
            default: return Typeface.SANS_SERIF;
        }
    }

    // ---------- janela ----------
    @Override public View onCreateInputView() {
        carregarPrefs();
        holder = new FrameLayout(this);
        host = new FrameLayout(this);
        if (flutuante) host.addView(new View(this), new FrameLayout.LayoutParams(1, 1));
        else host.addView(holder, new FrameLayout.LayoutParams(-1, -2));
        render();
        return host;
    }

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        ei = info;
        boolean antes = flutuante;
        carregarPrefs();
        if (host == null || antes != flutuante) setInputView(onCreateInputView());
        else render();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && ei != null && !caps) {
            boolean ns = ic.getCursorCapsMode(ei.inputType) != 0;
            if (ns != shift) { shift = ns; render(); }
        }
        if (flutuante) host.post(this::mostrarFlutuante);
    }

    @Override public void onFinishInputView(boolean finishingInput) {
        fecharFlutuante();
        super.onFinishInputView(finishingInput);
    }

    void fecharFlutuante() {
        if (popup != null) { try { popup.dismiss(); } catch (Exception e) { } popup = null; }
        if (holder != null && holder.getParent() != null && flutuante)
            ((ViewGroup) holder.getParent()).removeView(holder);
    }

    int larguraFlutuante() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int w = sp.getInt("fw", (int) (sw * 0.9f));
        return Math.max((int) (sw * 0.5f), Math.min(sw, w));
    }

    GradientDrawable fundoGrad() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{tema.bg, tema.bgGrad ? tema.bg2 : tema.bg});
    }

    Bitmap imagem(File f) {
        String k = f.getAbsolutePath() + f.lastModified();
        if (!k.equals(imgKey)) {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(f.getAbsolutePath(), o);
                int s = 1;
                while (o.outWidth / s > 1080) s *= 2;
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = s;
                imgBg = BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
            } catch (Throwable e) { imgBg = null; }
            imgKey = k;
        }
        return imgBg;
    }

    Drawable fundoGeral() {
        File f = new File(getFilesDir(), "timg_" + tema.id);
        if (f.exists()) {
            Bitmap bm = imagem(f);
            if (bm != null) return new CoverDrawable(bm);
        }
        return fundoGrad();
    }

    void mostrarFlutuante() {
        fecharFlutuante();
        if (host == null || host.getWindowToken() == null) return;
        final int sw = getResources().getDisplayMetrics().widthPixels;
        final FrameLayout box = new FrameLayout(this);
        GradientDrawable bgd = fundoGrad();
        bgd.setCornerRadius(dp(18));
        bgd.setStroke(dp(1), tema.accent);
        box.setBackground(bgd);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), dp(4), dp(8), dp(8));
        TextView alca = new TextView(this);
        alca.setText("≡   arraste   ≡");
        alca.setGravity(Gravity.CENTER);
        alca.setTextColor(tema.hint());
        alca.setPadding(0, dp(6), 0, dp(6));
        final int[] pos = {sp.getInt("fx", dp(20)), sp.getInt("fy", dp(80))};
        final float[] last = new float[2];
        alca.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    last[0] = e.getRawX(); last[1] = e.getRawY(); return true;
                case MotionEvent.ACTION_MOVE:
                    pos[0] += (int) (e.getRawX() - last[0]);
                    pos[1] -= (int) (e.getRawY() - last[1]);
                    last[0] = e.getRawX(); last[1] = e.getRawY();
                    if (popup != null) popup.update(pos[0], pos[1], -1, -1);
                    return true;
                case MotionEvent.ACTION_UP:
                    sp.edit().putInt("fx", pos[0]).putInt("fy", pos[1]).apply();
                    return true;
            }
            return true;
        });
        col.addView(alca);
        if (holder.getParent() != null) ((ViewGroup) holder.getParent()).removeView(holder);
        col.addView(holder, new LinearLayout.LayoutParams(-1, -2));
        box.addView(col, new FrameLayout.LayoutParams(-1, -2));
        final int[] larg = {larguraFlutuante()};
        for (int c = 0; c < 4; c++) box.addView(canto(c, box, pos, larg, sw));
        popup = new PopupWindow(box, larg[0], ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.setClippingEnabled(false);
        try {
            popup.showAtLocation(host, Gravity.BOTTOM | Gravity.START, pos[0], pos[1]);
        } catch (Exception e) { }
    }

    View canto(final int c, final FrameLayout box, final int[] pos, final int[] larg, final int sw) {
        CantoView v = new CantoView(this, c, tema.accent);
        int g = c == 0 ? (Gravity.TOP | Gravity.START)
              : c == 1 ? (Gravity.TOP | Gravity.END)
              : c == 2 ? (Gravity.BOTTOM | Gravity.START)
              : (Gravity.BOTTOM | Gravity.END);
        v.setLayoutParams(new FrameLayout.LayoutParams(dp(30), dp(30), g));
        final boolean dir = (c == 1 || c == 3), baixo = (c == 2 || c == 3);
        final float[] last = new float[2];
        final long[] t0 = new long[1];
        v.setOnTouchListener((x, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    last[0] = e.getRawX(); last[1] = e.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - last[0], dy = e.getRawY() - last[1];
                    last[0] = e.getRawX(); last[1] = e.getRawY();
                    int novo = Math.max((int) (sw * 0.5f), Math.min(sw, larg[0] + (int) (dir ? dx : -dx)));
                    int cresc = novo - larg[0];
                    larg[0] = novo;
                    if (!dir) pos[0] -= cresc;
                    pos[0] = Math.max(0, Math.min(sw - larg[0], pos[0]));
                    float H = Math.max(1f, box.getHeight());
                    float dh = baixo ? dy : -dy;
                    if (baixo) pos[1] = Math.max(0, pos[1] - (int) dy);
                    escH = Math.max(.7f, Math.min(1.8f, escH * (1f + dh / (0.75f * H))));
                    escW = Math.max(.7f, Math.min(1.5f, larg[0] / (sw * .9f)));
                    if (popup != null) popup.update(pos[0], pos[1], larg[0], -1);
                    long now = System.currentTimeMillis();
                    if (now - t0[0] > 60) { t0[0] = now; render(); }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    sp.edit().putInt("fx", pos[0]).putInt("fy", pos[1])
                        .putInt("fw", larg[0]).putFloat("fesc", escH).apply();
                    render();
                    return true;
            }
            return true;
        });
        return v;
    }

    void alternarFlutuante() {
        sp.edit().putBoolean("flutuante", !flutuante).apply();
        fecharFlutuante();
        setInputView(onCreateInputView());
        if (flutuante) host.post(this::mostrarFlutuante);
    }

    void pressAnim(View v, MotionEvent e) {
        if (!sp.getBoolean("efeitos", true)) return;
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) v.animate().scaleX(.93f).scaleY(.93f).setDuration(50).start();
        else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
    }

    // ---------- desenho ----------
    void render() {
        if (holder == null) return;
        holder.removeAllViews();
        holder.addView(construirTeclado(), new FrameLayout.LayoutParams(-1, -2));
    }

    View construirTeclado() {
        FrameLayout wrap = new FrameLayout(this);
        if (!flutuante) wrap.setBackground(fundoGeral());
        if (tema.fx != 0) wrap.addView(new FxView(this, tema.fx), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(3), dp(3), dp(3), dp(6));
        root.addView(barraEstilos());
        root.addView(barraFerramentas());
        if (modo == 0) letras(root);
        else if (modo == 3) emojis(root);
        else simbolos(root);
        wrap.addView(root, new FrameLayout.LayoutParams(-1, -2));
        return wrap;
    }

    View barraEstilos() {
        HorizontalScrollView sv = new HorizontalScrollView(this);
        sv.setHorizontalScrollBarEnabled(false);
        sv.setBackgroundColor(Tema.escalaAlpha(tema.topBg, tema.topAlpha));
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < ESTILOS.length; i++) {
            final int idx = i;
            TextView c = new TextView(this);
            c.setText(ESTILOS[i]);
            c.setTypeface(tf);
            c.setTextSize(15 * escW);
            c.setPadding(dp(14), dp(6), dp(14), dp(6));
            boolean sel = (i == estilo);
            c.setTextColor(sel ? tema.accent : tema.topText);
            GradientDrawable g = new GradientDrawable();
            g.setColor(Tema.escalaAlpha(tema.g[0].bg, Math.max(120, tema.g[0].alpha)));
            g.setCornerRadius(dp(20));
            if (sel) g.setStroke(dp(1), tema.accent);
            c.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(dp(4), dp(2), dp(4), dp(2));
            c.setLayoutParams(lp);
            c.setOnClickListener(v -> { sp.edit().putInt("estilo", idx).apply(); estilo = idx; render(); });
            l.addView(c);
        }
        sv.addView(l);
        return sv;
    }

    View barraFerramentas() {
        LinearLayout l = linha();
        l.setBackgroundColor(Tema.escalaAlpha(tema.topBg, tema.topAlpha));
        l.addView(ferramenta("😀", v -> { modo = 3; render(); }));
        l.addView(ferramenta("👕", v -> {
            TemaStore.proximo(this);
            carregarPrefs(); render();
            if (flutuante) mostrarFlutuante();
        }));
        l.addView(ferramenta("⚙", v -> {
            Intent i = new Intent(this, SettingsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }));
        l.addView(ferramenta("⧉", v -> alternarFlutuante()));
        l.addView(ferramenta("⌄", v -> requestHideSelf(0)));
        return l;
    }

    TextView ferramenta(String s, View.OnClickListener ck) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(18);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(tema.topText);
        t.setPadding(0, dp(6), 0, dp(6));
        t.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        t.setOnClickListener(v -> { feedback(v); ck.onClick(v); });
        return t;
    }

    LinearLayout linha() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return l;
    }

    View espaco(float p) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, p));
        return v;
    }

    GradientDrawable forma(int cor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(cor);
        d.setCornerRadius(tema.raioPx(dens()));
        if (tema.contorno) d.setStroke(dp(1), tema.stroke);
        return d;
    }

    /** grupo: 0 principais, 1 funcao, 2 enter, 3 espaco */
    View tecla(String rotulo, String dica, float peso, int grupo, View.OnClickListener ck) {
        Tema.Grupo gr = tema.g[grupo];
        FrameLayout f = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (dp(sp.getInt("altura", 48)) * escH), peso);
        lp.setMargins(dp(2), dp(3), dp(2), dp(3));
        f.setLayoutParams(lp);
        if (sp.getBoolean("efeitos", true)) {
            f.setBackground(new TeclaDrawable(tema, grupo, dens()));
        } else {
            StateListDrawable s = new StateListDrawable();
            s.addState(new int[]{android.R.attr.state_pressed}, forma(Tema.escalaAlpha(tema.pressed(grupo), gr.alpha)));
            s.addState(new int[]{}, forma(Tema.escalaAlpha(gr.bg, gr.alpha)));
            f.setBackground(s);
        }
        TextView t = new TextView(this);
        t.setText(rotulo);
        t.setTextColor(gr.text);
        t.setTypeface(tf);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, (rotulo.length() > 2 ? 13 : 20) * escW);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        f.addView(t, new FrameLayout.LayoutParams(-1, -1));
        if (dica != null) {
            TextView d = new TextView(this);
            d.setText(dica);
            d.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            d.setTextColor(tema.hint());
            d.setGravity(Gravity.CENTER_HORIZONTAL);
            d.setPadding(0, dp(2), 0, 0);
            f.addView(d, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        }
        f.setOnTouchListener((v, e) -> { pressAnim(v, e); return false; });
        if (ck != null) f.setOnClickListener(v -> { feedback(v); ck.onClick(v); });
        return f;
    }

    View letra(char c, String dica) {
        boolean up = shift || caps;
        String rot = String.valueOf(up ? Character.toUpperCase(c) : c);
        View k = tecla(rot, dica, 1f, 0, v -> digitar(String.valueOf(c)));
        if (dica != null) k.setOnLongClickListener(v -> { feedback(v); digitar(dica); return true; });
        return k;
    }

    View teclaEnter() {
        return tecla("↵", null, 1.4f, 2, v -> enter());
    }

    View teclaEspaco() {
        View k = tecla("Português (Brasil)", null, 4f, 3, v -> digitar(" "));
        k.setOnLongClickListener(v -> {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
            return true;
        });
        return k;
    }

    View teclaApagar() {
        View k = tecla("⌫", null, 1.5f, 1, null);
        k.setClickable(true);
        k.setOnTouchListener((v, e) -> {
            pressAnim(v, e);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    feedback(v); apagar(); h.postDelayed(repApagar, 400); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    h.removeCallbacks(repApagar); break;
            }
            return false;
        });
        return k;
    }

    void letras(LinearLayout root) {
        LinearLayout l1 = linha();
        String r1 = "qwertyuiop";
        for (int i = 0; i < r1.length(); i++) l1.addView(letra(r1.charAt(i), String.valueOf((i + 1) % 10)));
        LinearLayout l2 = linha();
        l2.addView(espaco(0.5f));
        String r2 = "asdfghjkl";
        for (int i = 0; i < r2.length(); i++) l2.addView(letra(r2.charAt(i), null));
        l2.addView(espaco(0.5f));
        LinearLayout l3 = linha();
        View sh = tecla(caps ? "⇪" : "⇧", null, 1.5f, 1, v -> {
            long t = System.currentTimeMillis();
            if (caps) { caps = false; shift = false; }
            else if (shift && t - ultimoShift < 400) caps = true;
            else shift = !shift;
            ultimoShift = t;
            render();
        });
        l3.addView(sh);
        String r3 = "zxcvbnm";
        for (int i = 0; i < r3.length(); i++) l3.addView(letra(r3.charAt(i), null));
        l3.addView(teclaApagar());
        LinearLayout l4 = linha();
        l4.addView(tecla("?123", null, 1.4f, 1, v -> { modo = 1; render(); }));
        l4.addView(tecla("☺", null, 1f, 0, v -> { modo = 3; render(); }));
        l4.addView(tecla(",", null, 1f, 0, v -> digitar(",")));
        l4.addView(teclaEspaco());
        l4.addView(tecla(".", "!?", 1f, 0, v -> digitar(".")));
        l4.addView(teclaEnter());
        root.addView(l1); root.addView(l2); root.addView(l3); root.addView(l4);
    }

    void simbolos(LinearLayout root) {
        String r1 = modo == 1 ? "1234567890" : "~`|•√π÷×¶∆";
        String r2 = modo == 1 ? "@#$_&-+()/" : "£¢€¥^°={}\\";
        String r3 = modo == 1 ? "*\"':;!?" : "%©®™✓[]";
        LinearLayout l1 = linha();
        for (int i = 0; i < r1.length(); i++) l1.addView(simb(r1.charAt(i)));
        LinearLayout l2 = linha();
        for (int i = 0; i < r2.length(); i++) l2.addView(simb(r2.charAt(i)));
        LinearLayout l3 = linha();
        l3.addView(tecla(modo == 1 ? "=\\<" : "123", null, 1.5f, 1, v -> { modo = (modo == 1) ? 2 : 1; render(); }));
        for (int i = 0; i < r3.length(); i++) l3.addView(simb(r3.charAt(i)));
        l3.addView(teclaApagar());
        LinearLayout l4 = linha();
        l4.addView(tecla("ABC", null, 1.4f, 1, v -> { modo = 0; render(); }));
        l4.addView(tecla(",", null, 1f, 0, v -> digitar(",")));
        l4.addView(teclaEspaco());
        l4.addView(tecla(".", null, 1f, 0, v -> digitar(".")));
        l4.addView(teclaEnter());
        root.addView(l1); root.addView(l2); root.addView(l3); root.addView(l4);
    }

    View simb(char c) {
        return tecla(String.valueOf(c), null, 1f, 0, v -> digitar(String.valueOf(c)));
    }

    void emojis(LinearLayout root) {
        for (int r = 0; r < 4; r++) {
            LinearLayout l = linha();
            for (int c = 0; c < 8; c++) {
                final String e = EMOJIS[r * 8 + c];
                l.addView(tecla(e, null, 1f, 0, v -> {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) ic.commitText(e, 1);
                }));
            }
            root.addView(l);
        }
        LinearLayout l = linha();
        l.addView(tecla("ABC", null, 2f, 1, v -> { modo = 0; render(); }));
        l.addView(tecla("espaço", null, 4f, 3, v -> digitar(" ")));
        l.addView(teclaApagar());
        root.addView(l);
    }

    // ---------- entrada ----------
    void digitar(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (s.length() == 1) {
            char c = s.charAt(0);
            if (Character.isLetter(c)) {
                if (shift || caps) c = Character.toUpperCase(c);
                s = estilizar(c);
            }
        }
        ic.commitText(s, 1);
        if (ei != null && !caps) {
            boolean ns = ic.getCursorCapsMode(ei.inputType) != 0;
            if (ns != shift) { shift = ns; render(); }
        }
    }

    void apagar() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) ic.commitText("", 1);
        else ic.deleteSurroundingTextInCodePoints(1, 0);
    }

    void enter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (ei != null) {
            int a = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
            if ((ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
                && a != EditorInfo.IME_ACTION_NONE && a != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(a);
                return;
            }
        }
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
    }

    String estilizar(char c) {
        if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) return String.valueOf(c);
        boolean mai = Character.isUpperCase(c);
        int i = Character.toLowerCase(c) - 'a';
        switch (estilo) {
            case 1: return String.valueOf(Character.toUpperCase(c));
            case 2: return String.valueOf(Math.random() < 0.5 ? Character.toUpperCase(c) : Character.toLowerCase(c));
            case 3: return cp(mai ? 0x1D5D4 : 0x1D5EE, i);
            case 4: return cp(mai ? 0x1D608 : 0x1D622, i);
            case 5: return cp(mai ? 0x1D4D0 : 0x1D4EA, i);
            case 6: return cp(mai ? 0x1D670 : 0x1D68A, i);
            case 7: return cp(mai ? 0xFF21 : 0xFF41, i);
            case 8: return cp(mai ? 0x24B6 : 0x24D0, i);
            default: return String.valueOf(c);
        }
    }

    String cp(int base, int i) {
        return new String(Character.toChars(base + i));
    }

    void feedback(View v) {
        String s = sp.getString("som", "sistema");
        float vol = sp.getInt("vol", 70) / 100f;
        if (s.equals("sistema")) am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, vol);
        else if (s.equals("custom") && soundId != 0) pool.play(soundId, vol, vol, 1, 0, 1f);
        if (sp.getBoolean("vibrar", true)) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }
}
