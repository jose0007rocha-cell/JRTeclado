package com.jrocha.teclado;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Tema {

    public static class Grupo {
        public int cor, texto, alpha = 255;
        public boolean fundo = true, grad = true, sombra = true;

        public Grupo(int cor, int texto) { this.cor = cor; this.texto = texto; }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("cor", cor); o.put("texto", texto); o.put("alpha", alpha);
                o.put("fundo", fundo); o.put("grad", grad); o.put("sombra", sombra);
            } catch (JSONException e) { }
            return o;
        }

        static Grupo fromJson(JSONObject o) {
            Grupo g = new Grupo(o.optInt("cor", 0xFF333333), o.optInt("texto", 0xFFFFFFFF));
            g.alpha = o.optInt("alpha", 255);
            g.fundo = o.optBoolean("fundo", true);
            g.grad = o.optBoolean("grad", true);
            g.sombra = o.optBoolean("sombra", true);
            return g;
        }
    }

    public String id, nome;
    public int bg, accent, estilo, raio = 12, corContorno, barraCor, barraAlpha = 0, barraTexto, efeito = 0;
    public boolean bgGrad, contorno;
    public Grupo principal, funcao, enter, espaco;

    // estilo: 0 plano, 1 neon, 2 relevo suave, 3 pilula 3D
    // efeito: 0 nenhum, 1 neve, 2 folhas

    public GradientDrawable fundo() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{bg, bgGrad ? escurecer(bg, 0.3f) : bg});
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id); o.put("nome", nome); o.put("bg", bg); o.put("bgGrad", bgGrad);
            o.put("accent", accent); o.put("estilo", estilo); o.put("raio", raio);
            o.put("contorno", contorno); o.put("corContorno", corContorno);
            o.put("barraCor", barraCor); o.put("barraAlpha", barraAlpha); o.put("barraTexto", barraTexto);
            o.put("efeito", efeito);
            o.put("p", principal.toJson()); o.put("f", funcao.toJson());
            o.put("e", enter.toJson()); o.put("s", espaco.toJson());
        } catch (JSONException e) { }
        return o;
    }

    public static Tema fromJson(JSONObject o) {
        Tema t = new Tema();
        t.id = o.optString("id", "c0"); t.nome = o.optString("nome", "Tema");
        t.bg = o.optInt("bg", 0xFF222222); t.bgGrad = o.optBoolean("bgGrad", false);
        t.accent = o.optInt("accent", 0xFFB584FF); t.estilo = o.optInt("estilo", 0);
        t.raio = o.optInt("raio", 12); t.contorno = o.optBoolean("contorno", false);
        t.corContorno = o.optInt("corContorno", 0xFF555555);
        t.barraCor = o.optInt("barraCor", t.bg); t.barraAlpha = o.optInt("barraAlpha", 0);
        t.barraTexto = o.optInt("barraTexto", 0xFFFFFFFF); t.efeito = o.optInt("efeito", 0);
        t.principal = grupo(o, "p", 0xFF3A3A3A, 0xFFFFFFFF);
        t.funcao = grupo(o, "f", 0xFF2E2E2E, t.accent);
        t.enter = grupo(o, "e", 0xFF2E2E2E, t.accent);
        t.espaco = grupo(o, "s", 0xFF3A3A3A, 0xFFFFFFFF);
        return t;
    }

    static Grupo grupo(JSONObject o, String k, int cor, int texto) {
        JSONObject g = o.optJSONObject(k);
        return g == null ? new Grupo(cor, texto) : Grupo.fromJson(g);
    }

    public Tema copia(String novoNome) {
        Tema t = fromJson(toJson());
        t.id = "c" + System.currentTimeMillis();
        t.nome = novoNome;
        return t;
    }

    // ---------- temas prontos ----------
    static Tema pronto(String id, String nome, int bg, boolean bgGrad, int key, int special, int text, int accent,
                       boolean contorno, int corContorno, int raio, int estilo) {
        Tema t = new Tema();
        t.id = id; t.nome = nome; t.bg = bg; t.bgGrad = bgGrad; t.accent = accent;
        t.contorno = contorno; t.corContorno = corContorno; t.raio = raio; t.estilo = estilo;
        t.barraCor = bg; t.barraAlpha = 0; t.barraTexto = text;
        t.principal = new Grupo(key, text);
        t.funcao = new Grupo(special, accent);
        t.enter = new Grupo(special, accent);
        t.espaco = new Grupo(key, text);
        return t;
    }

    public static final Tema[] PRONTOS = {
        pronto("b0", "Neon Roxo", 0xFF2B2738, true, 0xFF1B1824, 0xFF1F1C2B, 0xFFFFFFFF, 0xFFB584FF, true, 0xFF4B4560, 12, 1),
        pronto("b1", "Claro Suave", 0xFFF4F4FC, false, 0xFFE8E9F8, 0xFFE2E4F6, 0xFF4B4F8F, 0xFFE8793A, false, 0xFFFFFFFF, 12, 2),
        pronto("b2", "Escuro Pílula", 0xFF2A2A2A, true, 0xFF3B3B3D, 0xFF343436, 0xFFFFFFFF, 0xFFFFA000, false, 0x00000000, 40, 3)
    };

    public boolean ePronto() { return id.startsWith("b"); }

    // ---------- armazenamento ----------
    public static List<Tema> custom(SharedPreferences sp) {
        List<Tema> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString("temas_custom", "[]"));
            for (int i = 0; i < a.length(); i++) l.add(fromJson(a.getJSONObject(i)));
        } catch (Exception e) { }
        return l;
    }

    static void gravar(SharedPreferences sp, List<Tema> l) {
        JSONArray a = new JSONArray();
        for (Tema t : l) a.put(t.toJson());
        sp.edit().putString("temas_custom", a.toString()).apply();
    }

    public static void salvar(SharedPreferences sp, Tema t) {
        List<Tema> l = custom(sp);
        boolean achou = false;
        for (int i = 0; i < l.size(); i++) if (l.get(i).id.equals(t.id)) { l.set(i, t); achou = true; }
        if (!achou) l.add(t);
        gravar(sp, l);
    }

    public static void excluir(SharedPreferences sp, String id) {
        List<Tema> l = custom(sp);
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).id.equals(id)) l.remove(i);
        gravar(sp, l);
        if (id.equals(sp.getString("tema_id", "b0"))) sp.edit().putString("tema_id", "b0").apply();
    }

    public static List<Tema> todos(SharedPreferences sp) {
        List<Tema> l = new ArrayList<>();
        for (Tema t : PRONTOS) l.add(t);
        l.addAll(custom(sp));
        return l;
    }

    public static Tema porId(SharedPreferences sp, String id) {
        for (Tema t : todos(sp)) if (t.id.equals(id)) return t;
        return null;
    }

    public static Tema selecionado(SharedPreferences sp) {
        Tema t = porId(sp, sp.getString("tema_id", "b0"));
        return t != null ? t : PRONTOS[0];
    }

    // ---------- cores ----------
    public static int clarear(int c, float f) {
        return Color.argb(Color.alpha(c),
            (int) (Color.red(c) + (255 - Color.red(c)) * f),
            (int) (Color.green(c) + (255 - Color.green(c)) * f),
            (int) (Color.blue(c) + (255 - Color.blue(c)) * f));
    }

    public static int escurecer(int c, float f) {
        return Color.argb(Color.alpha(c),
            (int) (Color.red(c) * (1 - f)), (int) (Color.green(c) * (1 - f)), (int) (Color.blue(c) * (1 - f)));
    }

    public static int comAlpha(int c, int a) {
        return Color.argb(Math.max(0, Math.min(255, a)), Color.red(c), Color.green(c), Color.blue(c));
    }

    public static int destacar(int c) {
        double lum = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
        return lum > 0.6 ? escurecer(c, 0.12f) : clarear(c, 0.30f);
    }
}
