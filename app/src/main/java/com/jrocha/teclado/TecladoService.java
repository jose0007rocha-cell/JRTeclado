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
import android.os.Build;
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
import android.graphics.RectF;
import android.text.InputType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;
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
import java.util.HashMap;
import java.util.HashSet;
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
    Runnable spellInitRun = new Runnable() {
        @Override public void run() { if (spell == null) iniciarSpell(); }
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

    /** Impede o "modo escuro forcado" do Android de escurecer as cores do teclado (so a janela do teclado era afetada). */
    void semForceDark(View v) {
        if (Build.VERSION.SDK_INT >= 29) {
            try { v.setForceDarkAllowed(false); } catch (Throwable t) { }
        }
    }

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
                @Override public void onPrimaryClipChanged() { clipsIgnorados.clear(); capturarClip(); }
            });
        } catch (Exception e) { }
        carregarPrefs();
        new Thread(new Runnable() {
            @Override public void run() { carregarDic(); }
        }).start();
        prefL = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override public void onSharedPreferenceChanged(SharedPreferences s, String key) {
                if (key == null || !ativo) return;
                switch (key) {
                    case "temas_custom": case "tema_id": case "tema_previa": case "flutuante_previa":
                    case "linhanum": case "larg_teclas": case "altura": case "espac": case "margem": case "fonte":
                    case "especiais": case "popup":
                        h.removeCallbacks(refreshRun);
                        h.postDelayed(refreshRun, 40);
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

    String textoDoClipSistema() {
        try {
            ClipData d = cm.getPrimaryClip();
            if (d == null || d.getItemCount() == 0) return "";
            CharSequence cs = d.getItemAt(0).coerceToText(this);
            return cs == null ? "" : cs.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Limpa a area de transferencia do proprio Android (para o texto excluido nao voltar de la). */
    void limparClipSistema() {
        try {
            if (Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip();
            else cm.setPrimaryClip(ClipData.newPlainText("", ""));
        } catch (Exception e) { }
    }

    // textos que o usuario excluiu: nao voltam enquanto nao houver uma nova copia
    final java.util.HashSet<String> clipsIgnorados = new java.util.HashSet<>();

    void capturarClip() {
        try {
            ClipData d = cm.getPrimaryClip();
            if (d == null || d.getItemCount() == 0) return;
            CharSequence cs = d.getItemAt(0).coerceToText(this);
            if (cs == null) return;
            String t = cs.toString();
            if (t.trim().isEmpty()) return;
            if (clipsIgnorados.contains(t)) return;
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
        TeclaDrawable.larg = Math.max(0.6f, Math.min(1f, sp.getInt("larg_teclas", 100) / 100f));
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
        semForceDark(host);
        semForceDark(holder);
        if (flutuante) host.addView(new View(this), new FrameLayout.LayoutParams(1, 1));
        else host.addView(holder, new FrameLayout.LayoutParams(-1, -2));
        render();
        return host;
    }

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        ei = info;
        ativo = true;
        if (spell == null) h.postDelayed(spellInitRun, 1500);
        modoSug = false;
        ultimaCorrecao = null;
        campoCorrigivel = calcularCampoCorrigivel();
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
        semForceDark(box);
        GradientDrawable bgd = fundoGeral();
        bgd.setCornerRadius(dp(18));
        box.setBackground(bgd);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), dp(4), dp(8), dp(6));
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

    // ---------- area de transferencia em balao ----------
    int abaClip = 0; // 0 recentes, 1 salvos
    FrameLayout clipOverlay;

    List<String> salvos() {
        List<String> l = new ArrayList<>();
        try {
            org.json.JSONArray a = new org.json.JSONArray(sp.getString("clip_salvos", "[]"));
            for (int i = 0; i < a.length(); i++) l.add(a.getString(i));
        } catch (Exception e) { }
        return l;
    }

    void gravarSalvos(List<String> l) {
        org.json.JSONArray a = new org.json.JSONArray();
        for (String s : l) a.put(s);
        sp.edit().putString("clip_salvos", a.toString()).apply();
    }

    FrameLayout camadaLivre() {
        return new FrameLayout(this) {
            @Override protected void onMeasure(int w, int h) {
                if (View.MeasureSpec.getMode(w) == View.MeasureSpec.EXACTLY
                    && View.MeasureSpec.getMode(h) == View.MeasureSpec.EXACTLY) {
                    super.onMeasure(w, h);
                } else {
                    setMeasuredDimension(0, 0);
                }
            }
        };
    }

    TextView abaClipView(String nome, final int idx) {
        TextView t = new TextView(this);
        t.setText(nome);
        t.setTextSize(15);
        t.setTextColor(tema.barraTexto);
        t.setPadding(dp(14), dp(8), dp(14), dp(8));
        if (abaClip == idx) {
            t.setBackground(new TeclaDrawable(tema, tema.funcao, getResources().getDisplayMetrics().density).maxRaio(18).fixa());
            t.setTextColor(tema.funcao.texto);
        }
        t.setOnClickListener(v -> { feedback(v); abaClip = idx; render(); });
        return t;
    }

    View cartaoVazioClip(String texto) {
        TextView t = new TextView(this);
        t.setText(texto);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(14);
        t.setTextColor(tema.principal.texto);
        t.setPadding(dp(14), dp(18), dp(14), dp(18));
        t.setBackground(new TeclaDrawable(tema, tema.principal, getResources().getDisplayMetrics().density).maxRaio(16).fixa());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        t.setLayoutParams(lp);
        return t;
    }

    View cartaoClip(final String texto, final boolean salvo) {
        FrameLayout f = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(96), 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        f.setLayoutParams(lp);
        f.setBackground(new TeclaDrawable(tema, tema.principal, getResources().getDisplayMetrics().density).maxRaio(16).fixa());
        TextView t = new TextView(this);
        t.setText(texto);
        t.setMaxLines(4);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setTextColor(tema.principal.texto);
        t.setTextSize(14);
        t.setPadding(dp(10), dp(10), dp(26), dp(8));
        f.addView(t, new FrameLayout.LayoutParams(-1, -1));
        TextView mais = new TextView(this);
        mais.setText("⋮");
        mais.setTextSize(20);
        mais.setTextColor(tema.principal.texto);
        mais.setPadding(dp(8), dp(2), dp(8), dp(6));
        f.addView(mais, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END));
        f.setOnClickListener(v -> {
            feedback(v);
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.commitText(texto, 1);
            modo = 0;
            render();
        });
        mais.setOnClickListener(v -> menuCartaoClip(v, texto, salvo));
        return f;
    }

    void adicionarGradeClip(LinearLayout lista, List<String> itens, boolean salvo) {
        for (int i = 0; i < itens.size(); i += 2) {
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < 2; j++) {
                if (i + j < itens.size()) {
                    linha.addView(cartaoClip(itens.get(i + j), salvo));
                } else {
                    View vazio = new View(this);
                    vazio.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
                    linha.addView(vazio);
                }
            }
            lista.addView(linha, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    TextView itemMenuClip(String s, View.OnClickListener ck) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setTextColor(tema.principal.texto);
        t.setPadding(dp(16), dp(10), dp(16), dp(10));
        t.setOnClickListener(ck);
        return t;
    }

    void menuCartaoClip(View ancora, final String texto, final boolean salvo) {
        if (clipOverlay == null) return;
        clipOverlay.removeAllViews();
        LinearLayout m = new LinearLayout(this);
        m.setOrientation(LinearLayout.VERTICAL);
        m.setBackground(new TeclaDrawable(tema, tema.funcao, getResources().getDisplayMetrics().density).maxRaio(14).fixa());
        m.setPadding(dp(2), dp(4), dp(2), dp(4));
        if (!salvo) {
            m.addView(itemMenuClip("Salvar", v -> {
                List<String> l = salvos();
                l.remove(texto);
                l.add(0, texto);
                while (l.size() > 30) l.remove(l.size() - 1);
                gravarSalvos(l);
                render();
            }));
        }
        m.addView(itemMenuClip("Editar", v -> {
            clipOverlay.removeAllViews();
            clipOverlay.setClickable(false);
            Intent i = new Intent(this, EditarClipActivity.class);
            i.putExtra("texto", texto);
            i.putExtra("salvo", salvo);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }));
        m.addView(itemMenuClip("Excluir", v -> {
            if (salvo) {
                List<String> l = salvos();
                l.remove(texto);
                gravarSalvos(l);
            } else {
                clips.remove(texto);
                clipsIgnorados.add(texto);
                if (texto.equals(textoDoClipSistema())) limparClipSistema();
            }
            render();
        }));
        m.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int w = m.getMeasuredWidth();
        int[] a = new int[2], b = new int[2];
        ancora.getLocationInWindow(a);
        clipOverlay.getLocationInWindow(b);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2);
        lp.leftMargin = Math.max(0, Math.min(Math.max(0, clipOverlay.getWidth() - w), a[0] - b[0] - w + ancora.getWidth()));
        lp.topMargin = Math.max(0, a[1] - b[1] + ancora.getHeight());
        clipOverlay.addView(m, lp);
        clipOverlay.setClickable(true);
        clipOverlay.setOnClickListener(v -> { clipOverlay.removeAllViews(); clipOverlay.setClickable(false); });
    }

    /** Chamado pela tela de edicao: troca o texto de um item recente. */
    public void trocarClip(String antigo, String novo) {
        int i = clips.indexOf(antigo);
        clipsIgnorados.add(antigo);
        if (i >= 0) clips.set(i, novo); else clips.add(0, novo);
        if (ativo && modo == 4) h.post(new Runnable() {
            @Override public void run() { render(); }
        });
    }

    void salvarEntradaClip() {
        InputConnection ic = getCurrentInputConnection();
        String txt = "";
        if (ic != null) {
            CharSequence a = ic.getTextBeforeCursor(500, 0);
            CharSequence b = ic.getTextAfterCursor(500, 0);
            txt = (a == null ? "" : a.toString()) + (b == null ? "" : b.toString());
        }
        if (txt.trim().isEmpty() && !clips.isEmpty()) txt = clips.get(0);
        if (txt.trim().isEmpty()) {
            Toast.makeText(this, "Digite um texto ou copie algo para salvar", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> l = salvos();
        l.remove(txt);
        l.add(0, txt);
        while (l.size() > 30) l.remove(l.size() - 1);
        gravarSalvos(l);
        Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show();
        render();
    }

    FrameLayout construirPainelClipboard() {
        capturarClip();
        overlay = null;
        trilhaView = null;
        bolha = null;
        opcoesView = null;
        sug[0] = null; sug[1] = null; sug[2] = null;
        int alt = (int) (dp(sp.getInt("altura", 48)) * escH * 5.6f) + dp(40);
        FrameLayout wrap = new FrameLayout(this);
        if (!flutuante) wrap.setPadding(dp(4), dp(2), dp(4), dp(sp.getInt("margem", 6)));
        LinearLayout painel = new LinearLayout(this);
        painel.setOrientation(LinearLayout.VERTICAL);
        if (flutuante) {
            painel.setBackgroundColor(Color.TRANSPARENT);
        } else {
            GradientDrawable bgp = tema.fundo();
            bgp.setCornerRadius(dp(22));
            painel.setBackground(bgp);
        }
        painel.setPadding(dp(10), dp(8), dp(10), dp(4));

        LinearLayout topo = new LinearLayout(this);
        topo.setOrientation(LinearLayout.HORIZONTAL);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        TextView voltar = new TextView(this);
        voltar.setText("←");
        voltar.setTextSize(24);
        voltar.setTextColor(tema.barraTexto);
        voltar.setPadding(dp(8), dp(4), dp(14), dp(4));
        voltar.setOnClickListener(v -> { feedback(v); modo = 0; render(); });
        topo.addView(voltar);
        topo.addView(abaClipView("recentes", 0));
        topo.addView(abaClipView("salvos", 1));
        topo.addView(espaco(1f));
        IconeView lixo = new IconeView(this, 9, tema.barraTexto, false);
        lixo.setEscala(0.85f);
        lixo.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(40)));
        lixo.setOnClickListener(v -> {
            feedback(v);
            clipsIgnorados.addAll(clips);
            clips.clear(); // apaga so os recentes; os salvos nao sao apagados aqui
            limparClipSistema();
            render();
        });
        lixo.setVisibility(abaClip == 0 ? View.VISIBLE : View.INVISIBLE);
        topo.addView(lixo);
        painel.addView(topo, new LinearLayout.LayoutParams(-1, dp(46)));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        List<String> sal = salvos();
        if (abaClip == 0) {
            if (clips.isEmpty()) lista.addView(cartaoVazioClip("Nada copiado ainda. Copie um texto e ele aparece aqui."));
            else adicionarGradeClip(lista, clips, false);
            TextView rot = new TextView(this);
            rot.setText("SALVOS");
            rot.setTextSize(13);
            rot.setTextColor(tema.barraTexto);
            rot.setPadding(dp(6), dp(14), 0, dp(4));
            lista.addView(rot);
            if (sal.isEmpty()) lista.addView(cartaoVazioClip("Adicione suas entradas ou salve textos recentes aqui"));
            else adicionarGradeClip(lista, sal, true);
        } else {
            if (sal.isEmpty()) lista.addView(cartaoVazioClip("Adicione suas entradas ou salve textos recentes aqui"));
            else adicionarGradeClip(lista, sal, true);
        }
        lista.setPadding(0, 0, 0, dp(64));
        sv.addView(lista);
        painel.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout baixo = new LinearLayout(this);
        baixo.setOrientation(LinearLayout.HORIZONTAL);
        baixo.setGravity(Gravity.CENTER_VERTICAL);
        baixo.addView(icone(5, false, 0f, v -> requestHideSelf(0)));
        baixo.addView(espaco(1f));
        baixo.addView(icone(0, false, 0f, v -> { modo = 0; render(); }));
        baixo.addView(espaco(1f));
        View reserva = new View(this);
        reserva.setLayoutParams(new LinearLayout.LayoutParams(dp(38), 1));
        baixo.addView(reserva);
        painel.addView(baixo, new LinearLayout.LayoutParams(-1, dp(38)));

        wrap.addView(painel, new FrameLayout.LayoutParams(-1, alt));

        TextView mais = new TextView(this);
        mais.setText("+");
        mais.setTextSize(30);
        mais.setGravity(Gravity.CENTER);
        mais.setTextColor(tema.enter.texto);
        mais.setBackground(new TeclaDrawable(tema, tema.enter, getResources().getDisplayMetrics().density).circulo().fixa());
        mais.setOnClickListener(v -> { feedback(v); salvarEntradaClip(); });
        FrameLayout.LayoutParams lpf = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.BOTTOM | Gravity.END);
        lpf.rightMargin = dp(18);
        lpf.bottomMargin = dp(48);
        wrap.addView(mais, lpf);

        clipOverlay = camadaLivre();
        wrap.addView(clipOverlay, new FrameLayout.LayoutParams(-1, -1));
        return wrap;
    }

    FrameLayout construirTeclado() {
        if (modo == 4) return construirPainelClipboard();
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
        overlay = new FrameLayout(this) {
            // A camada nao pode aumentar a altura do teclado: so ocupa o espaco depois que o tamanho final ja foi definido.
            @Override protected void onMeasure(int w, int h) {
                if (View.MeasureSpec.getMode(w) == View.MeasureSpec.EXACTLY
                    && View.MeasureSpec.getMode(h) == View.MeasureSpec.EXACTLY) {
                    super.onMeasure(w, h);
                } else {
                    setMeasuredDimension(0, 0);
                }
            }
        };
        bolha = null;
        opcoesView = null;
        wrap.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        trilhaView = new TrilhaView(this, tema.linhaCor);
        overlay.addView(trilhaView, new FrameLayout.LayoutParams(-1, -1));
        return wrap;
    }

    View icone(int tipo, boolean fundo, float peso, final View.OnClickListener ck) {
        IconeView v = new IconeView(this, tipo, tema.barraTexto, fundo);
        v.setEscala(0.8f);
        v.setLayoutParams(peso > 0 ? new LinearLayout.LayoutParams(0, dp(34), peso) : new LinearLayout.LayoutParams(dp(38), dp(34)));
        v.setOnClickListener(x -> { feedback(x); ck.onClick(x); });
        return v;
    }

    View barraTopo() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.HORIZONTAL);
        p.setGravity(Gravity.CENTER_VERTICAL);
        p.setBackgroundColor(Tema.comAlpha(tema.barraCor, tema.barraAlpha));
        p.addView(icone(0, false, 0f, v -> {
            if (!sp.getBoolean("sug_palavras", true)) {
                Toast.makeText(this, "Sugestões desativadas em Configurações > Barra superior", Toast.LENGTH_SHORT).show();
                return;
            }
            modoSug = !modoSug;
            aplicarModoBarra();
            if (modoSug) atualizarSugestoes();
        }));
        // as duas versoes da barra existem sempre; so alternamos qual aparece (troca instantanea)
        barraIcones = new LinearLayout(this);
        barraIcones.setOrientation(LinearLayout.HORIZONTAL);
        barraIcones.setGravity(Gravity.CENTER_VERTICAL);
        barraIcones.addView(espaco(1f));
        barraIcones.addView(icone(1, false, 0f, v -> { modo = (modo == 4) ? 0 : 4; render(); }));
        barraIcones.addView(espaco(1f));
        barraIcones.addView(icone(2, false, 0f, v -> traduzir()));
        barraIcones.addView(espaco(1f));
        barraIcones.addView(icone(3, false, 0f, v -> abrirConfig()));
        barraIcones.addView(espaco(1f));
        barraIcones.addView(icone(8, false, 0f, v -> proximoTema()));
        barraIcones.addView(espaco(1f));
        p.addView(barraIcones, new LinearLayout.LayoutParams(0, dp(40), 1f));
        barraSug = new LinearLayout(this);
        barraSug.setOrientation(LinearLayout.HORIZONTAL);
        barraSug.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                View sep = new View(this);
                sep.setBackgroundColor(Tema.comAlpha(tema.barraTexto, 70));
                sep.setLayoutParams(new LinearLayout.LayoutParams(1, dp(20)));
                barraSug.addView(sep);
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
            t.setText(sugTxt[i]);
            final int idx = i;
            t.setOnClickListener(v -> aplicarSugestao(idx));
            barraSug.addView(t, new LinearLayout.LayoutParams(0, dp(40), 1f));
        }
        p.addView(barraSug, new LinearLayout.LayoutParams(0, dp(40), 1f));
        p.addView(icone(6, false, 0f, v -> alternarFlutuante()));
        aplicarModoBarra();
        return p;
    }

    void proximoTema() {
        List<Tema> todos = Tema.todos(sp);
        String atual = sp.getString("tema_id", "b0");
        int idx = 0;
        for (int i = 0; i < todos.size(); i++) if (todos.get(i).id.equals(atual)) idx = i;
        sp.edit().putString("tema_id", todos.get((idx + 1) % todos.size()).id).apply();
    }

    View barraInferior() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        View esconder = icone(5, false, 1f, v -> requestHideSelf(0));
        esconder.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(34)));
        l.addView(esconder);
        return l;
    }

    View textoBotao(String s, float peso, final View.OnClickListener ck) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Tema.comAlpha(tema.barraTexto, 200));
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setPadding(dp(12), 0, dp(12), 0);
        t.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(34)));
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
        final String rot0 = String.valueOf(up ? Character.toUpperCase(c) : c);
        final boolean pop = sp.getBoolean("popup", true);
        final String dicaF = dica;
        View k = tecla(rot0, dica, 1f, tema.principal, v -> digitar(String.valueOf(c)));
        if (pop) {
            if (acentosDe(c).length() > 0 || dica != null)
                k.setOnLongClickListener(v -> { feedback(v); abrirOpcoes(v, opcoes(c, dicaF)); return true; });
        } else if (dica != null) {
            k.setOnLongClickListener(v -> { feedback(v); digitar(dicaF); return true; });
        }
        k.setTag(Character.valueOf(Character.toLowerCase(c)));
        letraViews.add(k);
        k.setOnTouchListener((v, e) -> {
            if (deslizar(v, e)) return true;
            pressAnim(v, e);
            toqueLetra(v, e, ((TextView) ((FrameLayout) v).getChildAt(0)).getText().toString(), pop);
            return false;
        });
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
        letraViews.clear();
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
        View shiftKey = tecla(caps ? "⇪" : "⇧", null, 1.5f, tema.funcao, v -> {
            long t = System.currentTimeMillis();
            if (caps) { caps = false; shift = false; }
            else if (shift && t - ultimoShift < 400) caps = true;
            else shift = !shift;
            ultimoShift = t;
            atualizarShiftVisual();
        });
        shiftTv = (TextView) ((FrameLayout) shiftKey).getChildAt(0);
        l3.addView(shiftKey);
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

    // ---------- sugestoes (palavras, emoji, e-mail) ----------
    final String[] sugTxt = {"", "", ""};
    final int[] sugApagar = new int[3];
    final boolean[] sugEsp = new boolean[3];
    static final String[] DOMINIOS = {"gmail.com", "hotmail.com", "outlook.com", "yahoo.com.br", "icloud.com"};
    static final String[][] EMOJI_MAP = {
        {"oi", "👋"}, {"ola", "👋"}, {"tchau", "👋"}, {"feliz", "😀"}, {"alegre", "😄"}, {"rir", "😂"},
        {"kkk", "😂"}, {"risada", "🤣"}, {"triste", "😢"}, {"chorar", "😭"}, {"choro", "😭"}, {"amor", "😍"},
        {"amo", "😍"}, {"coracao", "❤️"}, {"beijo", "😘"}, {"legal", "👍"}, {"ok", "👌"}, {"joia", "👍"},
        {"parabens", "🎉"}, {"festa", "🎉"}, {"fogo", "🔥"}, {"quente", "🔥"}, {"frio", "🥶"}, {"sono", "😴"},
        {"dormir", "😴"}, {"raiva", "😡"}, {"medo", "😱"}, {"pensando", "🤔"}, {"duvida", "🤔"},
        {"obrigado", "🙏"}, {"obrigada", "🙏"}, {"deus", "🙏"}, {"paz", "✌️"}, {"forca", "💪"}, {"musica", "🎵"},
        {"sol", "☀️"}, {"lua", "🌙"}, {"chuva", "🌧️"}, {"pizza", "🍕"}, {"cafe", "☕"}, {"cerveja", "🍺"},
        {"carro", "🚗"}, {"casa", "🏠"}, {"dinheiro", "💰"}, {"estrela", "⭐"}, {"flor", "🌸"}, {"gato", "🐱"},
        {"cachorro", "🐶"}, {"bebe", "👶"}, {"sim", "✅"}, {"nao", "❌"}
    };

    HashMap<String, String> emojiMapa;
    int spellSeq = 0;
    String[] ultimaCorrecao;
    boolean campoCorrigivel = true;
    LinearLayout barraIcones, barraSug;
    TextView shiftTv;

    final SpellCheckerSession.SpellCheckerSessionListener spellL = new SpellCheckerSession.SpellCheckerSessionListener() {
        @Override public void onGetSuggestions(SuggestionsInfo[] r) {
            final List<String> l = new ArrayList<>();
            int seq = -1;
            if (r != null) {
                for (SuggestionsInfo si : r) {
                    seq = si.getSequence();
                    for (int i = 0; i < si.getSuggestionsCount() && l.size() < 2; i++) {
                        String s = si.getSuggestionAt(i);
                        if (!s.equalsIgnoreCase(palavraPedida) && !l.contains(s)) l.add(s);
                    }
                }
            }
            final int fseq = seq;
            h.post(new Runnable() {
                @Override public void run() { if (fseq == spellSeq) mostrarSugestoes(l); }
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

    boolean campoEmail() {
        if (ei == null) return false;
        int cls = ei.inputType & InputType.TYPE_MASK_CLASS;
        int vr = ei.inputType & InputType.TYPE_MASK_VARIATION;
        return cls == InputType.TYPE_CLASS_TEXT
            && (vr == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || vr == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
    }

    void defSug(int i, String txt, int apagar, boolean esp) {
        sugTxt[i] = txt;
        sugApagar[i] = apagar;
        sugEsp[i] = esp;
        if (sug[i] != null) sug[i].setText(txt);
    }

    void limparSug() {
        for (int i = 0; i < 3; i++) defSug(i, "", 0, false);
    }

    String emojiPara(String w) {
        if (emojiMapa == null) {
            emojiMapa = new HashMap<>();
            for (String[] m : EMOJI_MAP) emojiMapa.put(baseDe(m[0]), m[1]);
        }
        return emojiMapa.get(baseDe(w));
    }

    List<String> completar(String w, int max) {
        carregarDic();
        List<String> l = new ArrayList<>();
        String n = semAcento(w);
        if (n.isEmpty()) return l;
        for (int i = 0; i < aprNorm.size() && l.size() < max; i++) {
            String c = aprNorm.get(i);
            if (c.length() > n.length() && c.startsWith(n)) {
                String x = ajustarCaixa(w, aprPal.get(i));
                if (!l.contains(x)) l.add(x);
            }
        }
        for (int i = 0; i < dicNorm.length && l.size() < max; i++) {
            String c = dicNorm[i];
            if (c.length() > n.length() && c.startsWith(n)) {
                String x = ajustarCaixa(w, dicPal[i]);
                if (!l.contains(x)) l.add(x);
            }
        }
        return l;
    }

    void montarSugestoes(String w, List<String> doCorretor) {
        carregarDic();
        String n = semAcento(w);
        boolean valida = dicExato.contains(w.toLowerCase()) || aprNormSet.contains(n);
        List<String> c = new ArrayList<>();
        if (!valida && w.length() >= 2) c.addAll(sugerirCorrecoes(w, 2));
        for (String x : completar(w, 3)) if (!c.contains(x)) c.add(x);
        for (String x : doCorretor) if (!c.contains(x) && !x.equalsIgnoreCase(w)) c.add(x);
        int len = w.length();
        String em = sp.getBoolean("sug_emoji", true) ? emojiPara(w) : null;
        defSug(0, w, len, true);
        defSug(1, c.size() > 0 ? c.get(0) : "", len, true);
        if (em != null) defSug(2, em, len, true);
        else defSug(2, c.size() > 1 ? c.get(1) : "", len, true);
    }

    void atualizarSugestoes() {
        if (!modoSug || sug[0] == null) return;
        if (!sp.getBoolean("sug_palavras", true)) { limparSug(); return; }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence cs = ic.getTextBeforeCursor(60, 0);
        String antes = cs == null ? "" : cs.toString();
        int i = antes.length();
        while (i > 0 && !Character.isWhitespace(antes.charAt(i - 1))) i--;
        String tok = antes.substring(i);
        if (sp.getBoolean("sug_email", true) && !tok.isEmpty() && (tok.indexOf('@') >= 0 || campoEmail())) {
            palavraPedida = "";
            int at = tok.indexOf('@');
            if (at >= 0) {
                String parcial = tok.substring(at + 1).toLowerCase();
                int n = 0;
                for (String d : DOMINIOS) {
                    if (n < 3 && d.startsWith(parcial) && !d.equals(parcial)) defSug(n++, d, parcial.length(), false);
                }
                for (; n < 3; n++) defSug(n, "", 0, false);
            } else {
                for (int k = 0; k < 3; k++) defSug(k, "@" + DOMINIOS[k], 0, false);
            }
            return;
        }
        int j = antes.length();
        while (j > 0 && Character.isLetterOrDigit(antes.charAt(j - 1))) j--;
        String w = antes.substring(j);
        if (w.isEmpty()) { palavraPedida = ""; limparSug(); return; }
        palavraPedida = w;
        montarSugestoes(w, new ArrayList<String>());
        if (spell != null) {
            try { spell.getSuggestions(new TextInfo(w, 0, ++spellSeq), 3); } catch (Exception e) { }
        }
    }

    void mostrarSugestoes(List<String> l) {
        if (!modoSug || palavraPedida.isEmpty()) return;
        montarSugestoes(palavraPedida, l);
    }

    void aplicarSugestao(int i) {
        String s = sugTxt[i];
        if (s == null || s.isEmpty()) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ultimaCorrecao = null;
        if (i == 0 && sugEsp[0]) aprenderPalavra(s, false);
        if (sugApagar[i] > 0) ic.deleteSurroundingText(sugApagar[i], 0);
        ic.commitText(s + (sugEsp[i] ? " " : ""), 1);
        pedirSugestoes();
    }

    void pedirSugestoes() {
        if (!modoSug) return;
        h.removeCallbacks(rodaSug);
        h.post(rodaSug);
    }

    // ---------- dicionario, corretor ortografico e digitacao deslizante ----------
    static final java.util.regex.Pattern MARCAS = java.util.regex.Pattern.compile("\\p{M}");
    volatile boolean dicOk = false;
    static final int LIMITE_AUTO = 1273;
    List<View> letraViews = new ArrayList<>();
    boolean swAtivo = false, swTemUlt = false;
    float swX, swY, swKeyW;
    List<float[]> swPts = new ArrayList<>();
    List<RectF> swRects = new ArrayList<>();
    List<Character> swChars = new ArrayList<>();
    HashMap<Character, float[]> swCenters = new HashMap<>();
    TrilhaView trilhaView;
    String[] dicPal, dicBase, dicNorm;
    HashSet<String> dicExato = new HashSet<>();
    HashMap<String, Integer> mapaNorm = new HashMap<>();
    List<String> aprPal = new ArrayList<>(), aprNorm = new ArrayList<>();
    HashSet<String> aprExato = new HashSet<>(), aprNormSet = new HashSet<>();

    static String semAcento(String w) {
        return MARCAS.matcher(Normalizer.normalize(w.toLowerCase(), Normalizer.Form.NFD)).replaceAll("");
    }

    static String baseDe(String w) {
        String n = semAcento(w);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c >= 'a' && c <= 'z' && (sb.length() == 0 || sb.charAt(sb.length() - 1) != c)) sb.append(c);
        }
        return sb.toString();
    }

    void carregarDic() {
        if (dicOk) return;
        synchronized (this) {
            if (dicOk) return;
            List<String> l = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.palavras), "UTF-8"))) {
                String ln;
                while ((ln = r.readLine()) != null) {
                    ln = ln.trim();
                    if (!ln.isEmpty()) l.add(ln);
                }
            } catch (Exception e) { }
            String[] pal = l.toArray(new String[0]);
            int n = pal.length;
            String[] base = new String[n], norm = new String[n];
            HashSet<String> exato = new HashSet<>();
            HashMap<String, Integer> mapa = new HashMap<>();
            for (int i = 0; i < n; i++) {
                base[i] = baseDe(pal[i]);
                norm[i] = semAcento(pal[i]);
                exato.add(pal[i].toLowerCase());
                if (!mapa.containsKey(norm[i])) mapa.put(norm[i], i);
            }
            dicBase = base; dicNorm = norm; dicExato = exato; mapaNorm = mapa; dicPal = pal;
            carregarAprendidas();
            dicOk = true;
        }
    }

    void carregarAprendidas() {
        aprPal.clear(); aprNorm.clear(); aprExato.clear(); aprNormSet.clear();
        String s = sp.getString("aprendidas", "");
        if (s.isEmpty()) return;
        for (String w : s.split("\n")) {
            if (w.isEmpty()) continue;
            String nn = semAcento(w);
            aprPal.add(w); aprNorm.add(nn); aprExato.add(w.toLowerCase()); aprNormSet.add(nn);
        }
    }

    /** Guarda uma palavra nova (ex.: um nome) para sugerir, completar e nao corrigir depois. */
    void aprenderPalavra(String w, boolean forcar) {
        if (w == null || w.length() < 2 || w.length() > 24) return;
        if (!forcar && !sp.getBoolean("aprender", true)) return;
        for (int i = 0; i < w.length(); i++) if (!Character.isLetter(w.charAt(i))) return;
        carregarDic();
        String low = w.toLowerCase();
        if (dicExato.contains(low) || aprExato.contains(low)) return;
        String nn = semAcento(w);
        aprPal.add(0, w); aprNorm.add(0, nn); aprExato.add(low); aprNormSet.add(nn);
        StringBuilder sb = new StringBuilder();
        int max = Math.min(aprPal.size(), 600);
        for (int i = 0; i < max; i++) {
            if (i > 0) sb.append('\n');
            sb.append(aprPal.get(i));
        }
        sp.edit().putString("aprendidas", sb.toString()).apply();
    }

    static boolean dist1(String a, String b) {
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > 1) return false;
        int i = 0, m = Math.min(la, lb);
        while (i < m && a.charAt(i) == b.charAt(i)) i++;
        if (la == lb) {
            if (i == la) return true;
            if (a.regionMatches(i + 1, b, i + 1, la - i - 1)) return true;
            return i + 1 < la && a.charAt(i) == b.charAt(i + 1) && a.charAt(i + 1) == b.charAt(i)
                && a.regionMatches(i + 2, b, i + 2, la - i - 2);
        }
        String s = la < lb ? a : b, l = la < lb ? b : a;
        return s.regionMatches(i, l, i + 1, s.length() - i);
    }

    static int lev(String a, String b, int max) {
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > max) return max + 1;
        int[] prev = new int[lb + 1], cur = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;
        for (int i = 1; i <= la; i++) {
            cur[0] = i;
            int min = cur[0];
            for (int j = 1; j <= lb; j++) {
                int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + c);
                if (cur[j] < min) min = cur[j];
            }
            if (min > max) return max + 1;
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[lb];
    }

    static String ajustarCaixa(String orig, String cand) {
        if (orig.isEmpty() || cand.isEmpty()) return cand;
        if (orig.length() > 1 && orig.equals(orig.toUpperCase()) && !orig.equals(orig.toLowerCase())) return cand.toUpperCase();
        if (Character.isUpperCase(orig.charAt(0))) return Character.toUpperCase(cand.charAt(0)) + cand.substring(1);
        return cand;
    }

    /** Sugestoes de correcao (acentos, troca/falta/sobra de uma letra, ate duas letras de diferenca). */
    List<String> sugerirCorrecoes(String w, int max) {
        carregarDic();
        List<String> l = new ArrayList<>();
        String n = semAcento(w);
        if (n.length() < 2) return l;
        Integer ix = mapaNorm.get(n);
        if (ix != null && !dicExato.contains(w.toLowerCase())) l.add(ajustarCaixa(w, dicPal[ix]));
        for (int i = 0; i < aprNorm.size() && l.size() < max; i++) {
            String c = aprNorm.get(i);
            if (!c.equals(n) && Math.abs(c.length() - n.length()) <= 1 && dist1(n, c)) {
                String x = ajustarCaixa(w, aprPal.get(i));
                if (!l.contains(x)) l.add(x);
            }
        }
        for (int d = 1; d <= 2 && l.size() < max; d++) {
            for (int i = 0; i < dicNorm.length && l.size() < max; i++) {
                String c = dicNorm[i];
                if (c.isEmpty() || c.equals(n) || c.charAt(0) != n.charAt(0) || Math.abs(c.length() - n.length()) > d) continue;
                boolean ok = (d == 1) ? dist1(n, c) : lev(n, c, 2) == 2;
                if (ok) {
                    String x = ajustarCaixa(w, dicPal[i]);
                    if (!l.contains(x)) l.add(x);
                }
            }
        }
        return l;
    }

    /** Correcao automatica ao digitar espaco/pontuacao. Retorna null quando nao deve corrigir. */
    String corrigir(String w) {
        if (w.length() < 3 || w.length() > 20) return null;
        for (int i = 0; i < w.length(); i++) if (!Character.isLetter(w.charAt(i))) return null;
        if (w.equals(w.toUpperCase())) return null;
        carregarDic();
        String low = w.toLowerCase();
        if (dicExato.contains(low) || aprExato.contains(low)) return null;
        String n = semAcento(w);
        if (aprNormSet.contains(n)) return null;
        Integer ix = mapaNorm.get(n);
        if (ix != null) return ajustarCaixa(w, dicPal[ix]);
        if (Character.isUpperCase(w.charAt(0)) || w.length() < 4) return null;
        // correcao automatica so para as palavras mais comuns (evita trocar palavras validas que nao estao no dicionario)
        for (int i = 0; i < Math.min(dicNorm.length, LIMITE_AUTO); i++) {
            String c = dicNorm[i];
            if (c.isEmpty() || c.charAt(0) != n.charAt(0) || Math.abs(c.length() - n.length()) > 1) continue;
            if (!c.equals(n) && dist1(n, c)) return ajustarCaixa(w, dicPal[i]);
        }
        return null;
    }

    // ----- gesto -----
    static float[][] reamostrar(List<float[]> pts, int n) {
        int m = pts.size();
        float[][] out = new float[n][2];
        if (m == 0) return out;
        float[] acum = new float[m];
        for (int i = 1; i < m; i++)
            acum[i] = acum[i - 1] + (float) Math.hypot(pts.get(i)[0] - pts.get(i - 1)[0], pts.get(i)[1] - pts.get(i - 1)[1]);
        float total = acum[m - 1];
        if (m < 2 || total < 1e-3f) {
            for (int k = 0; k < n; k++) { out[k][0] = pts.get(0)[0]; out[k][1] = pts.get(0)[1]; }
            return out;
        }
        int j = 0;
        for (int k = 0; k < n; k++) {
            float alvo = total * k / (n - 1);
            while (j < m - 2 && acum[j + 1] < alvo) j++;
            float seg = acum[j + 1] - acum[j];
            float t = seg < 1e-6f ? 0f : (alvo - acum[j]) / seg;
            out[k][0] = pts.get(j)[0] + (pts.get(j + 1)[0] - pts.get(j)[0]) * t;
            out[k][1] = pts.get(j)[1] + (pts.get(j + 1)[1] - pts.get(j)[1]) * t;
        }
        return out;
    }

    static float comprimento(List<float[]> pts) {
        float t = 0;
        for (int i = 1; i < pts.size(); i++)
            t += (float) Math.hypot(pts.get(i)[0] - pts.get(i - 1)[0], pts.get(i)[1] - pts.get(i - 1)[1]);
        return t;
    }

    double pontuarPalavra(String base, double penalidade, float[][] u, float totalUser, float[] ini, float[] fim) {
        if (base.length() < 2) return 1e9;
        float[] c0 = swCenters.get(base.charAt(0));
        float[] c1 = swCenters.get(base.charAt(base.length() - 1));
        if (c0 == null || c1 == null) return 1e9;
        float lim = swKeyW * 1.6f;
        if (Math.hypot(c0[0] - ini[0], c0[1] - ini[1]) > lim || Math.hypot(c1[0] - fim[0], c1[1] - fim[1]) > lim) return 1e9;
        List<float[]> poly = new ArrayList<>();
        for (int i = 0; i < base.length(); i++) {
            float[] c = swCenters.get(base.charAt(i));
            if (c == null) return 1e9;
            poly.add(c);
        }
        float[][] w = reamostrar(poly, u.length);
        float s = 0;
        for (int k = 0; k < u.length; k++) s += (float) Math.hypot(u[k][0] - w[k][0], u[k][1] - w[k][1]);
        double score = s / u.length / swKeyW;
        float pl = comprimento(poly);
        score += Math.abs(pl - totalUser) / Math.max(1f, Math.max(pl, totalUser)) * 0.4;
        return score + penalidade;
    }

    /** Escolhe a palavra cujo caminho ideal (pelo centro das teclas) mais se parece com o traco do dedo. */
    String melhorPalavraGeo() {
        carregarDic();
        if (swPts.size() < 2 || swCenters.isEmpty() || swKeyW <= 0) return null;
        float total = comprimento(swPts);
        if (total < swKeyW * 1.2f) return null;
        float[][] u = reamostrar(swPts, 32);
        float[] ini = swPts.get(0), fim = swPts.get(swPts.size() - 1);
        double melhor = 1e9;
        String res = null;
        for (int i = 0; i < aprPal.size(); i++) {
            double sc = pontuarPalavra(baseDe(aprPal.get(i)), 0.0, u, total, ini, fim);
            if (sc < melhor) { melhor = sc; res = aprPal.get(i); }
        }
        double den = Math.log(1.0 + dicBase.length);
        for (int i = 0; i < dicBase.length; i++) {
            String b = dicBase[i];
            if (b.length() < 2) continue;
            double sc = pontuarPalavra(b, Math.log(1.0 + i) / den * 0.25, u, total, ini, fim);
            if (sc < melhor) { melhor = sc; res = dicPal[i]; }
        }
        return melhor < 1.0 ? res : null;
    }

    boolean deslizar(View v, MotionEvent e) {
        try {
            return deslizarInterno(v, e);
        } catch (Throwable t) {
            abortarDeslize();
            return false;
        }
    }

    void abortarDeslize() {
        swAtivo = false;
        if (trilhaView != null) trilhaView.limpar();
    }

    boolean deslizarInterno(View v, MotionEvent e) {
        if (!sp.getBoolean("deslizar", true)) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                swX = e.getRawX(); swY = e.getRawY(); swAtivo = false;
                return false;
            case MotionEvent.ACTION_MOVE: {
                if (!swAtivo) {
                    float dx = e.getRawX() - swX, dy = e.getRawY() - swY;
                    float lim = dp(16);
                    if (dx * dx + dy * dy < lim * lim) return false;
                    if (opcoesView != null) {
                        int[] kp = new int[2];
                        v.getLocationOnScreen(kp);
                        if (e.getRawY() < kp[1] - dp(2)) return false;
                        fecharOpcoes();
                    }
                    iniciarDeslize(v);
                }
                acompanharDeslize(e);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (swAtivo) {
                    acompanharDeslize(e);
                    finalizarDeslize(e.getActionMasked() == MotionEvent.ACTION_UP);
                    v.setPressed(false);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(60).start();
                    return true;
                }
                return false;
        }
        return false;
    }

    void iniciarDeslize(View v) {
        swAtivo = true;
        v.cancelLongPress();
        if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(true);
        esconderBolha();
        fecharOpcoes();
        swRects.clear();
        swChars.clear();
        swCenters.clear();
        swPts.clear();
        float somaW = 0;
        for (View k : letraViews) {
            if (!(k.getTag() instanceof Character) || k.getWidth() == 0) continue;
            int[] p = new int[2];
            k.getLocationOnScreen(p);
            RectF r = new RectF(p[0], p[1], p[0] + k.getWidth(), p[1] + k.getHeight());
            char c = ((Character) k.getTag()).charValue();
            swRects.add(r);
            swChars.add(Character.valueOf(c));
            swCenters.put(Character.valueOf(c), new float[]{r.centerX(), r.centerY()});
            somaW += r.width();
        }
        swKeyW = swRects.isEmpty() ? 0 : somaW / swRects.size();
        if (trilhaView != null) trilhaView.iniciar();
        pontoDeslize(swX, swY);
    }

    void pontoDeslize(float x, float y) {
        if (!swPts.isEmpty()) {
            float[] ult = swPts.get(swPts.size() - 1);
            if (Math.abs(ult[0] - x) < 0.5f && Math.abs(ult[1] - y) < 0.5f) return;
        }
        swPts.add(new float[]{x, y});
        if (trilhaView != null) trilhaView.add(x, y);
    }

    void acompanharDeslize(MotionEvent e) {
        float ox = e.getRawX() - e.getX(), oy = e.getRawY() - e.getY();
        int n = e.getHistorySize();
        for (int i = 0; i < n; i++) pontoDeslize(e.getHistoricalX(i) + ox, e.getHistoricalY(i) + oy);
        pontoDeslize(e.getRawX(), e.getRawY());
    }

    void finalizarDeslize(boolean ok) {
        swAtivo = false;
        final TrilhaView tv = trilhaView;
        if (tv != null) {
            h.postDelayed(new Runnable() {
                @Override public void run() { tv.limpar(); }
            }, 200);
        }
        if (!ok) return;
        String w = melhorPalavraGeo();
        if (w == null) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ultimaCorrecao = null;
        if (caps) w = w.toUpperCase();
        else if (shift) w = Character.toUpperCase(w.charAt(0)) + w.substring(1);
        ic.commitText(w + " ", 1);
        if (!caps && shift) { shift = false; atualizarShiftVisual(); }
        pedirSugestoes();
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
            // reconstroi o teclado do zero para garantir que fique igual a previa do editor
            if (flutuante) fecharFlutuante();
            setInputView(onCreateInputView());
            if (flutuante) host.post(this::mostrarFlutuante);
        }
    }

    // ---------- entrada ----------
    static boolean ehSeparador(char c) {
        return c == ' ' || c == '.' || c == ',' || c == '!' || c == '?' || c == ';' || c == ':';
    }

    boolean calcularCampoCorrigivel() {
        if (ei == null) return true;
        if ((ei.inputType & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false;
        int vr = ei.inputType & InputType.TYPE_MASK_VARIATION;
        if (vr == InputType.TYPE_TEXT_VARIATION_PASSWORD || vr == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            || vr == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD || vr == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            || vr == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS || vr == InputType.TYPE_TEXT_VARIATION_URI
            || vr == InputType.TYPE_TEXT_VARIATION_PERSON_NAME) return false;
        return (ei.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0;
    }

    /** Atualiza maiusculas/minusculas das letras sem reconstruir o teclado (instantaneo). */
    void atualizarShiftVisual() {
        boolean up = shift || caps;
        for (View k : letraViews) {
            if (!(k.getTag() instanceof Character) || !(k instanceof FrameLayout)) continue;
            char c = ((Character) k.getTag()).charValue();
            View t = ((FrameLayout) k).getChildAt(0);
            if (t instanceof TextView) ((TextView) t).setText(String.valueOf(up ? Character.toUpperCase(c) : c));
        }
        if (shiftTv != null) shiftTv.setText(caps ? "⇪" : "⇧");
    }

    void aplicarModoBarra() {
        if (barraIcones == null || barraSug == null) return;
        barraIcones.setVisibility(modoSug ? View.GONE : View.VISIBLE);
        barraSug.setVisibility(modoSug ? View.VISIBLE : View.GONE);
    }

    void digitar(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        boolean letra = false;
        if (s.length() == 1) {
            char c = s.charAt(0);
            if (Character.isLetter(c)) {
                if (shift || caps) c = Character.toUpperCase(c);
                s = estilizar(c);
                letra = true;
            }
        }
        boolean sep = !letra && s.length() == 1 && ehSeparador(s.charAt(0));
        if (sep && campoCorrigivel) {
            boolean corrige = sp.getBoolean("corretor", true);
            boolean aprende = sp.getBoolean("aprender", true);
            String w = (corrige || aprende) ? palavraAtual() : "";
            String troca = (!w.isEmpty() && corrige) ? corrigir(w) : null;
            if (troca != null) {
                ic.deleteSurroundingText(w.length(), 0);
                ic.commitText(troca + s, 1);
                ultimaCorrecao = new String[]{w, troca, s};
            } else {
                ic.commitText(s, 1);
                ultimaCorrecao = null;
                if (!w.isEmpty() && aprende) aprenderPalavra(w, false);
            }
        } else {
            ic.commitText(s, 1);
            ultimaCorrecao = null;
        }
        if (!caps) {
            boolean mudou = false;
            if (letra && shift) {
                shift = false;
                mudou = true;
            } else if ((sep || s.equals("\n")) && ei != null) {
                boolean ns = ic.getCursorCapsMode(ei.inputType) != 0;
                if (ns != shift) { shift = ns; mudou = true; }
            }
            if (mudou) atualizarShiftVisual();
        }
        if (letra && !modoSug && sp.getBoolean("autosug", true) && sp.getBoolean("sug_palavras", true)) {
            modoSug = true;
            aplicarModoBarra();
        }
        pedirSugestoes();
    }

    void apagar() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) {
            ic.commitText("", 1);
            ultimaCorrecao = null;
        } else if (ultimaCorrecao != null) {
            desfazerCorrecao(ic);
        } else {
            ic.deleteSurroundingTextInCodePoints(1, 0);
        }
        pedirSugestoes();
    }

    /** Backspace logo depois de uma correcao automatica: volta a palavra original e passa a respeita-la. */
    void desfazerCorrecao(InputConnection ic) {
        String[] u = ultimaCorrecao;
        ultimaCorrecao = null;
        int n = u[1].length() + u[2].length();
        CharSequence antes = ic.getTextBeforeCursor(n, 0);
        if (antes != null && antes.toString().equals(u[1] + u[2])) {
            ic.deleteSurroundingText(n, 0);
            ic.commitText(u[0], 1);
            aprenderPalavra(u[0], true);
        } else {
            ic.deleteSurroundingTextInCodePoints(1, 0);
        }
    }

    void enter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ultimaCorrecao = null;
        if (modoSug && sp.getBoolean("autosug", true)) { modoSug = false; aplicarModoBarra(); }
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
