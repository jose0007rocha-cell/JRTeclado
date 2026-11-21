package com.jrocha.teclado;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
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
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TecladoService extends InputMethodService {

    public static TecladoService instancia;

    static final String[] ESTILOS = {"Normal", "BIG", "Random", "Bold", "Italic", "Cursivo", "Mono", "Largo", "Círculo"};
    static final String H1 = "%\\|=[]<>{}", H2 = "@#$_&-+()", H3 = "*\"':;!?";
    static final String[] EMOJIS = {
        "😀","😂","🤣","😊","😍","😘","😎","🤔",
        "😢","😭","😡","👍","👎","👏","🙏","💪",
        "🔥","❤️","💔","🎉","✨","🌟","💯","✅",
        "❌","😴","🤝","👀","🙄","😅","😉","🥰"};

    SharedPreferences sp;
    Tema tema;
    Typeface tf = Typeface.SANS_SERIF;
    int estilo = 0, modo = 0; // modo: 0 letras, 1 simbolos, 2 simbolos2, 3 emoji, 4 area de transferencia
    boolean shift = false, caps = false, flutuante = false, modoSug = false;
    long ultimoShift = 0;
    float escW = 1f, escH = 1f;
    FrameLayout host, holder;
    PopupWindow popup;
    EditorInfo ei;
    SoundPool pool;
    int soundId = 0;
    long soundStamp = 0;
    AudioManager am;
    ClipboardManager cm;
    Handler h = new Handler();
    List<String> clips = new ArrayList<>();
    TextView[] sug = new TextView[3];
    SpellCheckerSession spell;
    String palavraPedida = "";
    List<View> cantos = new ArrayList<>();
    FrameLayout overlay;
    View bolha;
    LinearLayout opcoesView;
    int opcSel = -1;
    boolean ativo = false;
    SharedPreferences.OnSharedPreferenceChangeListener prefL;
    Runnable fecharOpcoesRun = new Runnable() {
        @Override public void run() { fecharOpcoes(); }
    };
    Runnable refreshRun = new Runnable() {
        @Override public void run() { atualizarPorPrefs(); }
    };

    Runnable repApagar = new Runnable() {
        @Override public void run() { apagar(); h.postDelayed(this, 60); }
    };
    Runnable rodaSug = new Runnable() {
        @Override public void run() { atualizarSugestoes(); }
    };
    Runnable ocultarCantos = new Runnable() {
        @Override public void run() {
            for (final View v : cantos) {
                v.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
                    @Override public void run() { v.setVisibility(View.INVISIBLE); }
                }).start();
            }
        }
    };

    int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    // ---------- ciclo de vida ----------
    @Override public void onCreate() {
        super.onCreate();
        instancia = this;
        sp = getSharedPreferences("jrteclado", Context.MODE_PRIVATE);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        pool = new SoundPool.Builder().setMaxStreams(4)
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build();
        cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            cm.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override public void onPrimaryClipChanged() { capturarClip(); }
            });
        } catch (Exception e) { }
        carregarPrefs();
        prefL = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override public void onSharedPreferenceChanged(SharedPreferences s, String key) {
                if (key == null || !ativo) return;
                switch (key) {
                    case "temas_custom": case "tema_id": case "tema_previa": case "flutuante_previa":
                    case "linhanum": case "altura": case "espac": case "margem": case "fonte":
                    case "especiais": case "popup":
                        h.removeCallbacks(refreshRun);
                        h.postDelayed(refreshRun, 90);
                }
            }
        };
        sp.registerOnSharedPreferenceChangeListener(prefL);
    }

    @Override public void onDestroy() {
        sp.unregisterOnSharedPreferenceChangeListener(prefL);
        instancia = null;
        fecharFlutuante();
        if (spell != null) { try { spell.close(); } catch (Exception e) { } }
        if (pool != null) pool.release();
        super.onDestroy();
    }

    void capturarClip() {
        try {
            ClipData d = cm.getPrimaryClip();
            if (d == null || d.getItemCount() == 0) return;
            CharSequence cs = d.getItemAt(0).coerceToText(this);
            if (cs == null) return;
            String t = cs.toString();
            if (t.trim().isEmpty()) return;
            clips.remove(t);
            clips.add(0, t);
            while (clips.size() > 15) clips.remove(clips.size() - 1);
        } catch (Exception e) { }
    }

    void carregarPrefs() {
        String pv = sp.getString("tema_previa", "");
        Tema tp = pv.isEmpty() ? null : Tema.porId(sp, pv);
        tema = tp != null ? tp : Tema.selecionado(sp);
        flutuante = sp.getBoolean("flutuante", false) || sp.getBoolean("flutuante_previa", false);
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
        ativo = true;
        modoSug = false;
        boolean antes = flutuante;
        carregarPrefs();
        capturarClip();
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
        ativo = false;
        fecharFlutuante();
        super.onFinishInputView(finishingInput);
    }

    void fecharFlutuante() {
        h.removeCallbacks(ocultarCantos);
        if (popup != null) { try { popup.dismiss(); } catch (Exception e) { } popup = null; }
        if (holder != null && holder.getParent() != null && flutuante)
            ((ViewGroup) holder.getParent()).removeView(holder);
    }

    int larguraFlutuante() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int w = sp.getInt("fw", (int) (sw * 0.9f));
        return Math.max((int) (sw * 0.5f), Math.min(sw, w));
    }

    GradientDrawable fundoGeral() { return tema.fundo(); }

    void mostrarCantos() {
        h.removeCallbacks(ocultarCantos);
        for (View v : cantos) {
            v.setVisibility(View.VISIBLE);
            v.animate().alpha(1f).setDuration(120).start();
        }
    }

    void agendarOcultar() {
        h.removeCallbacks(ocultarCantos);
        h.postDelayed(ocultarCantos, 2000);
    }

    void mostrarFlutuante() {
        fecharFlutuante();
        if (host == null || host.getWindowToken() == null) return;
        final int sw = getResources().getDisplayMetrics().widthPixels;
        final FrameLayout box = new FrameLayout(this);
        GradientDrawable bgd = fundoGeral();
        bgd.setCornerRadius(dp(18));
        box.setBackground(bgd);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), dp(12), dp(8), dp(12));
        final int[] pos = {sp.getInt("fx", dp(20)), sp.getInt("fy", dp(80))};
        final float[] last = new float[2];
        col.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    last[0] = e.getRawX(); last[1] = e.getRawY();
                    mostrarCantos();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    pos[0] += (int) (e.getRawX() - last[0]);
                    pos[1] -= (int) (e.getRawY() - last[1]);
                    last[0] = e.getRawX(); last[1] = e.getRawY();
                    if (popup != null) popup.update(pos[0], pos[1], -1, -1);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    sp.edit().putInt("fx", pos[0]).putInt("fy", pos[1]).apply();
                    agendarOcultar();
                    return true;
            }
            return true;
        });
        if (holder.getParent() != null) ((ViewGroup) holder.getParent()).removeView(holder);
        col.addView(holder, new LinearLayout.LayoutParams(-1, -2));
        box.addView(col, new FrameLayout.LayoutParams(-1, -2));
        final int[] larg = {larguraFlutuante()};
        cantos.clear();
        for (int c = 0; c < 4; c++) {
            View cv = canto(c, box, pos, larg, sw);
            cv.setAlpha(0f);
            cv.setVisibility(View.INVISIBLE);
            cantos.add(cv);
            box.addView(cv);
        }
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
                    h.removeCallbacks(ocultarCantos);
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
                    agendarOcultar();
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

    // ---------- desenho ----------
    void render() {
        if (holder == null) return;
        holder.removeAllViews();
        holder.addView(construirTeclado(), new FrameLayout.LayoutParams(-1, -2));
        if (modoSug) h.post(rodaSug);
    }

    FrameLayout construirTeclado() {
        FrameLayout wrap = new FrameLayout(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        if (flutuante) root.setBackgroundColor(Color.TRANSPARENT);
        else root.setBackground(fundoGeral());
        root.setPadding(dp(3), dp(3), dp(3), dp(sp.getInt("margem", 6)));
        root.addView(barraTopo());
        if (modo == 0) letras(root);
        else if (modo == 3) emojis(root);
        else if (modo == 4) areaTransferencia(root);
        else simbolos(root);
        root.addView(barraInferior());
        wrap.addView(root, new FrameLayout.LayoutParams(-1, -2));
        overlay = new FrameLayout(this);
        bolha = null;
        opcoesView = null;
        wrap.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        return wrap;
    }

    View icone(int tipo, boolean fundo, float peso, final View.OnClickListener ck) {
        IconeView v = new IconeView(this, tipo, tema.barraTexto, fundo);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, dp(40), peso));
        v.setOnClickListener(x -> { feedback(x); ck.onClick(x); });
        return v;
    }

    View barraTopo() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.HORIZONTAL);
        p.setGravity(Gravity.CENTER_VERTICAL);
        p.setBackgroundColor(Tema.comAlpha(tema.barraCor, tema.barraAlpha));
        p.addView(icone(0, false, 1f, v -> { modoSug = !modoSug; render(); }));
        if (!modoSug) {
            p.addView(icone(1, false, 1f, v -> { modo = (modo == 4) ? 0 : 4; render(); }));
            p.addView(icone(2, false, 1f, v -> traduzir()));
            p.addView(icone(3, false, 1f, v -> abrirConfig()));
        } else {
            for (int i = 0; i < 3; i++) {
                if (i > 0) {
                    View sep = new View(this);
                    sep.setBackgroundColor(Tema.comAlpha(tema.barraTexto, 70));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(1, dp(20));
                    sep.setLayoutParams(lp);
                    p.addView(sep);
                }
                TextView t = new TextView(this);
                t.setTextColor(tema.barraTexto);
                t.setTextSize(17);
                t.setTypeface(tf);
                t.setGravity(Gravity.CENTER);
                t.setSingleLine(true);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                t.setPadding(dp(4), 0, dp(4), 0);
                sug[i] = t;
                final int idx = i;
                t.setOnClickListener(v -> aplicarSugestao(idx));
                p.addView(t, new LinearLayout.LayoutParams(0, dp(40), 1.6f));
            }
        }
        p.addView(icone(6, false, 1f, v -> alternarFlutuante()));
        return p;
    }

    View barraInferior() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.addView(icone(5, false, 1f, v -> requestHideSelf(0)));
        l.addView(textoBotao("◐  tema", 3f, v -> {
            List<Tema> todos = Tema.todos(sp);
            String atual = sp.getString("tema_id", "b0");
            int idx = 0;
            for (int i = 0; i < todos.size(); i++) if (todos.get(i).id.equals(atual)) idx = i;
            sp.edit().putString("tema_id", todos.get((idx + 1) % todos.size()).id).apply();
            carregarPrefs();
            render();
            if (flutuante) mostrarFlutuante();
        }));
        return l;
    }

    View textoBotao(String s, float peso, final View.OnClickListener ck) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Tema.comAlpha(tema.barraTexto, 200));
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, dp(34), peso));
        t.setOnClickListener(x -> { feedback(x); ck.onClick(x); });
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

    View tecla(String rotulo, String dica, float peso, Tema.Grupo g, View.OnClickListener ck) {
        FrameLayout f = new FrameLayout(this);
        int esp = sp.getInt("espac", 2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (dp(sp.getInt("altura", 48)) * escH), peso);
        lp.setMargins(dp(esp), dp(esp + 1), dp(esp), dp(esp + 1));
        f.setLayoutParams(lp);
        f.setBackground(new TeclaDrawable(tema, g, getResources().getDisplayMetrics().density));
        TextView t = new TextView(this);
        t.setText(rotulo);
        t.setTextColor(g.texto);
        t.setTypeface(tf);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, (rotulo.length() > 2 ? 13 : 20) * escW);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        f.addView(t, new FrameLayout.LayoutParams(-1, -1));
        if (dica != null) {
            TextView d = new TextView(this);
            d.setText(dica);
            d.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10 * escW);
            d.setTextColor(Tema.comAlpha(tema.principal.texto, 170));
            d.setGravity(Gravity.END);
            d.setPadding(0, dp(2), dp(6), 0);
            f.addView(d, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.END));
        }
        f.setOnTouchListener((v, e) -> {
            pressAnim(v, e);
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) fecharOpcoes();
            return false;
        });
        if (ck != null) f.setOnClickListener(v -> { feedback(v); ck.onClick(v); });
        return f;
    }

    View letra(char c, String dica) {
        boolean up = shift || caps;
        final String rot = String.valueOf(up ? Character.toUpperCase(c) : c);
        final boolean pop = sp.getBoolean("popup", true);
        View k = tecla(rot, dica, 1f, tema.principal, v -> digitar(String.valueOf(c)));
        if (pop) {
            final List<String> ops = opcoes(c, dica);
            if (!ops.isEmpty()) k.setOnLongClickListener(v -> { feedback(v); abrirOpcoes(v, ops); return true; });
        } else if (dica != null) {
            k.setOnLongClickListener(v -> { feedback(v); digitar(dica); return true; });
        }
        k.setOnTouchListener((v, e) -> { pressAnim(v, e); toqueLetra(v, e, rot, pop); return false; });
        return k;
    }

    View teclaEnter() { return tecla("↵", null, 1.4f, tema.enter, v -> enter()); }

    View teclaEspaco() {
        View k = tecla("Português (Brasil)", null, 4f, tema.espaco, v -> digitar(" "));
        k.setOnLongClickListener(v -> {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
            return true;
        });
        final float[] ini = new float[1], ult = new float[1];
        final boolean[] mov = {false};
        k.setOnTouchListener((v, e) -> {
            pressAnim(v, e);
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) fecharOpcoes();
            if (!sp.getBoolean("cursor", true)) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    ini[0] = e.getRawX(); ult[0] = ini[0]; mov[0] = false;
                    return false;
                case MotionEvent.ACTION_MOVE: {
                    float x = e.getRawX();
                    if (!mov[0]) {
                        if (Math.abs(x - ini[0]) < dp(14)) return false;
                        mov[0] = true; ult[0] = x; v.cancelLongPress();
                        return true;
                    }
                    float passo = dp(11);
                    while (Math.abs(x - ult[0]) >= passo) {
                        boolean dir = x > ult[0];
                        sendDownUpKeyEvents(dir ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT);
                        ult[0] += dir ? passo : -passo;
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mov[0]) { mov[0] = false; v.setPressed(false); pedirSugestoes(); return true; }
                    return false;
            }
            return false;
        });
        return k;
    }

    View teclaApagar() {
        View k = tecla("⌫", null, 1.5f, tema.funcao, null);
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
        boolean numRow = sp.getBoolean("linhanum", true);
        boolean esp = sp.getBoolean("especiais", true);
        String r1 = "qwertyuiop";
        for (int i = 0; i < r1.length(); i++)
            l1.addView(letra(r1.charAt(i), numRow ? (esp ? String.valueOf(H1.charAt(i)) : null) : String.valueOf((i + 1) % 10)));
        LinearLayout l2 = linha();
        l2.addView(espaco(0.5f));
        String r2 = "asdfghjkl";
        for (int i = 0; i < r2.length(); i++) l2.addView(letra(r2.charAt(i), esp ? String.valueOf(H2.charAt(i)) : null));
        l2.addView(espaco(0.5f));
        LinearLayout l3 = linha();
        l3.addView(tecla(caps ? "⇪" : "⇧", null, 1.5f, tema.funcao, v -> {
            long t = System.currentTimeMillis();
            if (caps) { caps = false; shift = false; }
            else if (shift && t - ultimoShift < 400) caps = true;
            else shift = !shift;
            ultimoShift = t;
            render();
        }));
        String r3 = "zxcvbnm";
        for (int i = 0; i < r3.length(); i++) l3.addView(letra(r3.charAt(i), esp ? String.valueOf(H3.charAt(i)) : null));
        l3.addView(teclaApagar());
        LinearLayout l4 = linha();
        l4.addView(tecla("?123", null, 1.4f, tema.funcao, v -> { modo = 1; render(); }));
        l4.addView(tecla("☺", null, 1f, tema.principal, v -> { modo = 3; render(); }));
        l4.addView(tecla(",", null, 1f, tema.principal, v -> digitar(",")));
        l4.addView(teclaEspaco());
        l4.addView(tecla(".", "!?", 1f, tema.principal, v -> digitar(".")));
        l4.addView(teclaEnter());
        if (numRow) {
            LinearLayout l0 = linha();
            for (int i = 0; i < 10; i++) {
                final String dg = String.valueOf((i + 1) % 10);
                l0.addView(tecla(dg, null, 1f, tema.principal, v -> digitar(dg)));
            }
            root.addView(l0);
        }
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
        l3.addView(tecla(modo == 1 ? "=\\<" : "123", null, 1.5f, tema.funcao, v -> { modo = (modo == 1) ? 2 : 1; render(); }));
        for (int i = 0; i < r3.length(); i++) l3.addView(simb(r3.charAt(i)));
        l3.addView(teclaApagar());
        LinearLayout l4 = linha();
        l4.addView(tecla("ABC", null, 1.4f, tema.funcao, v -> { modo = 0; render(); }));
        l4.addView(tecla(",", null, 1f, tema.principal, v -> digitar(",")));
        l4.addView(teclaEspaco());
        l4.addView(tecla(".", null, 1f, tema.principal, v -> digitar(".")));
        l4.addView(teclaEnter());
        root.addView(l1); root.addView(l2); root.addView(l3); root.addView(l4);
    }

    View simb(char c) {
        return tecla(String.valueOf(c), null, 1f, tema.principal, v -> digitar(String.valueOf(c)));
    }

    void emojis(LinearLayout root) {
        for (int r = 0; r < 4; r++) {
            LinearLayout l = linha();
            for (int c = 0; c < 8; c++) {
                final String e = EMOJIS[r * 8 + c];
                l.addView(tecla(e, null, 1f, tema.principal, v -> {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) ic.commitText(e, 1);
                }));
            }
            root.addView(l);
        }
        LinearLayout l = linha();
        l.addView(tecla("ABC", null, 2f, tema.funcao, v -> { modo = 0; render(); }));
        l.addView(tecla("espaço", null, 4f, tema.espaco, v -> digitar(" ")));
        l.addView(teclaApagar());
        root.addView(l);
    }

    void areaTransferencia(LinearLayout root) {
        capturarClip();
        int alt = (int) (dp(sp.getInt("altura", 48)) * escH * 3.2f);
        ScrollView sv = new ScrollView(this);
        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        if (clips.isEmpty()) {
            TextView vazio = new TextView(this);
            vazio.setText("Nada copiado ainda.\nCopie um texto e ele aparece aqui.");
            vazio.setTextColor(Tema.comAlpha(tema.barraTexto, 200));
            vazio.setGravity(Gravity.CENTER);
            vazio.setPadding(dp(12), dp(24), dp(12), dp(24));
            lista.addView(vazio);
        }
        for (final String t : clips) {
            TextView item = new TextView(this);
            item.setText(t);
            item.setMaxLines(2);
            item.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.setTextColor(tema.principal.texto);
            item.setTextSize(15);
            item.setPadding(dp(14), dp(10), dp(14), dp(10));
            item.setBackground(new TeclaDrawable(tema, tema.principal, getResources().getDisplayMetrics().density));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(dp(2), dp(3), dp(2), dp(3));
            item.setLayoutParams(lp);
            item.setOnClickListener(v -> {
                feedback(v);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(t, 1);
            });
            lista.addView(item);
        }
        sv.addView(lista);
        root.addView(sv, new LinearLayout.LayoutParams(-1, alt));
        LinearLayout l = linha();
        l.addView(tecla("ABC", null, 2f, tema.funcao, v -> { modo = 0; render(); }));
        l.addView(tecla("Limpar", null, 2f, tema.funcao, v -> { clips.clear(); render(); }));
        l.addView(teclaApagar());
        root.addView(l);
    }

    // ---------- barra superior: acoes ----------
    void abrirConfig() {
        Intent i = new Intent(this, SettingsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }


    void traduzir() {
        InputConnection ic = getCurrentInputConnection();
        String txt = "";
        if (ic != null) {
            CharSequence a = ic.getTextBeforeCursor(1000, 0);
            CharSequence b = ic.getTextAfterCursor(1000, 0);
            txt = (a == null ? "" : a.toString()) + (b == null ? "" : b.toString());
        }
        if (txt.trim().isEmpty()) {
            Toast.makeText(this, "Digite um texto primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, txt);
        i.setPackage("com.google.android.apps.translate");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            Intent w = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=" + Uri.encode(txt) + "&op=translate"));
            w.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(w); } catch (Exception e2) {
                Toast.makeText(this, "Não foi possível abrir o tradutor", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------- sugestoes (autocorrecao) ----------
    final SpellCheckerSession.SpellCheckerSessionListener spellL = new SpellCheckerSession.SpellCheckerSessionListener() {
        @Override public void onGetSuggestions(SuggestionsInfo[] r) {
            final List<String> l = new ArrayList<>();
            if (r != null) {
                for (SuggestionsInfo si : r) {
                    for (int i = 0; i < si.getSuggestionsCount() && l.size() < 2; i++) {
                        String s = si.getSuggestionAt(i);
                        if (!s.equalsIgnoreCase(palavraPedida) && !l.contains(s)) l.add(s);
                    }
                }
            }
            h.post(new Runnable() {
                @Override public void run() { mostrarSugestoes(l); }
            });
        }
        @Override public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] r) { }
    };

    void iniciarSpell() {
        try {
            TextServicesManager tsm = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
            spell = tsm.newSpellCheckerSession(null, null, spellL, true);
        } catch (Exception e) {
            spell = null;
        }
    }

    String palavraAtual() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence cs = ic.getTextBeforeCursor(40, 0);
        if (cs == null) return "";
        String s = cs.toString();
        int i = s.length();
        while (i > 0 && Character.isLetterOrDigit(s.charAt(i - 1))) i--;
        return s.substring(i);
    }

    void setSug(int i, String s) {
        if (sug[i] != null) sug[i].setText(s);
    }

    void atualizarSugestoes() {
        if (!modoSug || sug[0] == null) return;
        String w = palavraAtual();
        if (w.isEmpty()) { for (int i = 0; i < 3; i++) setSug(i, ""); return; }
        palavraPedida = w;
        setSug(0, w); setSug(1, ""); setSug(2, "");
        if (spell == null) iniciarSpell();
        if (spell != null) {
            try { spell.getSuggestions(new TextInfo(w), 3); } catch (Exception e) { }
        }
    }

    void mostrarSugestoes(List<String> l) {
        if (!modoSug || !palavraPedida.equals(palavraAtual())) return;
        setSug(0, palavraPedida);
        setSug(1, l.size() > 0 ? l.get(0) : "");
        setSug(2, l.size() > 1 ? l.get(1) : "");
    }

    void aplicarSugestao(int i) {
        if (sug[i] == null) return;
        String s = sug[i].getText().toString();
        if (s.isEmpty()) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        String pa = palavraAtual();
        ic.deleteSurroundingText(pa.length(), 0);
        ic.commitText(s + " ", 1);
        h.removeCallbacks(rodaSug);
        h.postDelayed(rodaSug, 100);
    }

    void pedirSugestoes() {
        if (!modoSug) return;
        h.removeCallbacks(rodaSug);
        h.postDelayed(rodaSug, 120);
    }

    // ---------- popup ao pressionar (bolha e opcoes) ----------
    static String acentosDe(char c) {
        switch (Character.toLowerCase(c)) {
            case 'a': return "áàâãä";
            case 'e': return "éèêë";
            case 'i': return "íìîï";
            case 'o': return "óòôõö";
            case 'u': return "úùûü";
            case 'c': return "ç";
            case 'n': return "ñ";
            case 'y': return "ý";
            default: return "";
        }
    }

    List<String> opcoes(char c, String dica) {
        List<String> l = new ArrayList<>();
        boolean up = shift || caps;
        String ac = acentosDe(c);
        for (int i = 0; i < ac.length(); i++) {
            char x = ac.charAt(i);
            l.add(String.valueOf(up ? Character.toUpperCase(x) : x));
        }
        if (dica != null) l.add(dica);
        return l;
    }

    void esconderBolha() {
        if (bolha != null && overlay != null) overlay.removeView(bolha);
        bolha = null;
    }

    void mostrarBolha(View key, String s) {
        esconderBolha();
        if (overlay == null || overlay.getWidth() == 0) return;
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(tema.principal.texto);
        t.setTypeface(tf);
        t.setTextSize(26);
        t.setGravity(Gravity.CENTER);
        t.setBackground(new TeclaDrawable(tema, tema.principal, getResources().getDisplayMetrics().density));
        int w = Math.max(key.getWidth(), dp(40)) + dp(10);
        int hh = key.getHeight() + dp(6);
        int[] a = new int[2], b = new int[2];
        key.getLocationInWindow(a);
        overlay.getLocationInWindow(b);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, hh);
        lp.leftMargin = Math.max(0, Math.min(Math.max(0, overlay.getWidth() - w), a[0] - b[0] - (w - key.getWidth()) / 2));
        lp.topMargin = Math.max(0, a[1] - b[1] - hh - dp(4));
        overlay.addView(t, lp);
        bolha = t;
    }

    void fecharOpcoes() {
        h.removeCallbacks(fecharOpcoesRun);
        if (opcoesView != null && overlay != null) overlay.removeView(opcoesView);
        opcoesView = null;
        opcSel = -1;
    }

    void abrirOpcoes(View key, List<String> ops) {
        fecharOpcoes();
        esconderBolha();
        if (overlay == null) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(tema.bg | 0xFF000000);
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), tema.accent);
        row.setBackground(g);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (final String op : ops) {
            TextView t = new TextView(this);
            t.setText(op);
            t.setTextColor(tema.principal.texto);
            t.setTypeface(tf);
            t.setTextSize(22);
            t.setGravity(Gravity.CENTER);
            t.setMinWidth(dp(40));
            t.setPadding(dp(8), dp(8), dp(8), dp(8));
            t.setOnClickListener(v -> { feedback(v); fecharOpcoes(); digitar(op); });
            row.addView(t);
        }
        row.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int w = row.getMeasuredWidth(), hh = row.getMeasuredHeight();
        int[] a = new int[2], b = new int[2];
        key.getLocationInWindow(a);
        overlay.getLocationInWindow(b);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, hh);
        lp.leftMargin = Math.max(0, Math.min(Math.max(0, overlay.getWidth() - w), a[0] - b[0] + key.getWidth() / 2 - w / 2));
        lp.topMargin = Math.max(0, a[1] - b[1] - hh - dp(6));
        overlay.addView(row, lp);
        opcoesView = row;
        opcSel = -1;
        h.postDelayed(fecharOpcoesRun, 3500);
    }

    void deslizarOpcoes(View key, MotionEvent e) {
        if (opcoesView == null) return;
        int[] k = new int[2];
        key.getLocationOnScreen(k);
        int sel = -1;
        if (e.getRawY() < k[1] + dp(6)) {
            for (int i = 0; i < opcoesView.getChildCount(); i++) {
                View c = opcoesView.getChildAt(i);
                int[] p = new int[2];
                c.getLocationOnScreen(p);
                if (e.getRawX() >= p[0] && e.getRawX() < p[0] + c.getWidth()) { sel = i; break; }
            }
        }
        if (sel != opcSel) {
            opcSel = sel;
            for (int i = 0; i < opcoesView.getChildCount(); i++) {
                View c = opcoesView.getChildAt(i);
                if (i == sel) {
                    GradientDrawable gg = new GradientDrawable();
                    gg.setColor(Tema.comAlpha(tema.accent, 90));
                    gg.setCornerRadius(dp(10));
                    c.setBackground(gg);
                } else {
                    c.setBackground(null);
                }
            }
        }
    }

    void toqueLetra(View v, MotionEvent e, String rotulo, boolean pop) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                fecharOpcoes();
                if (pop) mostrarBolha(v, rotulo);
                break;
            case MotionEvent.ACTION_MOVE:
                deslizarOpcoes(v, e);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                esconderBolha();
                if (opcoesView != null && opcSel >= 0 && e.getActionMasked() == MotionEvent.ACTION_UP) {
                    TextView t = (TextView) opcoesView.getChildAt(opcSel);
                    String op = t.getText().toString();
                    fecharOpcoes();
                    digitar(op);
                }
                break;
        }
    }

    void atualizarPorPrefs() {
        if (!ativo || host == null) return;
        boolean antes = flutuante;
        carregarPrefs();
        if (antes != flutuante) {
            fecharFlutuante();
            setInputView(onCreateInputView());
            if (flutuante) host.post(this::mostrarFlutuante);
        } else {
            render();
            if (flutuante) mostrarFlutuante();
        }
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
        if (sp.getBoolean("autosug", true) && !modoSug && Character.isLetter(s.codePointAt(0))) {
            modoSug = true;
            render();
        } else {
            pedirSugestoes();
        }
    }

    void apagar() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) ic.commitText("", 1);
        else ic.deleteSurroundingTextInCodePoints(1, 0);
        pedirSugestoes();
    }

    void enter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (modoSug && sp.getBoolean("autosug", true)) { modoSug = false; h.post(() -> render()); }
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

    void pressAnim(View v, MotionEvent e) {
        if (!sp.getBoolean("efeitos", true)) return;
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) v.animate().scaleX(.93f).scaleY(.93f).setDuration(50).start();
        else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
    }

    void feedback(View v) {
        String s = sp.getString("som", "sistema");
        float vol = sp.getInt("vol", 70) / 100f;
        if (s.equals("sistema")) am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, vol);
        else if (s.equals("custom") && soundId != 0) pool.play(soundId, vol, vol, 1, 0, 1f);
        if (sp.getBoolean("vibrar", true)) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }
}
