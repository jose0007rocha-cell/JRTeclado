package com.jrocha.teclado;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Tema {

    /** Estilo de um grupo de teclas: 0 principais, 1 funcao, 2 enter, 3 espaco. */
    public static class Grupo {
        public int bg = 0xFF333333, bg2 = 0xFF111111, text = 0xFFFFFFFF, alpha = 255;
        public boolean grad = false, sombra = true;
        public Grupo() { }
        public Grupo(int bg, int text) { this.bg = bg; this.bg2 = escurecer(bg, .3f); this.text = text; }
    }

    public String id = "", nome = "Meu tema";
    public int bg = 0xFF2A2A2A, bg2 = 0xFF1A1A1A;
    public boolean bgGrad = false;
    public int accent = 0xFFB584FF, stroke = 0xFF4B4560;
    public boolean contorno = false;
    public int raio = -1;     // dp; -1 = automatico
    public int efeito = 0;    // 0 neon, 1 suave, 2 pilula 3D, 3 plano
    public int fx = 0;        // 0 nenhum, 1 neve, 2 folhas
    public int topBg = 0xFF000000, topAlpha = 0, topText = 0xFFFFFFFF;
    public Grupo[] g = new Grupo[4];

    public Tema() { for (int i = 0; i < 4; i++) g[i] = new Grupo(); }

    public int hint() { return comAlpha(g[0].text, 170); }

    public int pressed(int grupo) { return clarear(g[grupo].bg, .3f); }

    public float raioPx(float d) {
        if (raio >= 0) return raio * d;
        return (efeito == 2 ? 40 : efeito == 3 ? 10 : 12) * d;
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("nome", nome);
        o.put("bg", bg); o.put("bg2", bg2); o.put("bgGrad", bgGrad);
        o.put("accent", accent); o.put("stroke", stroke); o.put("contorno", contorno);
        o.put("raio", raio); o.put("efeito", efeito); o.put("fx", fx);
        o.put("topBg", topBg); o.put("topAlpha", topAlpha); o.put("topText", topText);
        JSONArray ga = new JSONArray();
        for (Grupo x : g) {
            JSONObject j = new JSONObject();
            j.put("bg", x.bg); j.put("bg2", x.bg2); j.put("text", x.text); j.put("alpha", x.alpha);
            j.put("grad", x.grad); j.put("sombra", x.sombra);
            ga.put(j);
        }
        o.put("g", ga);
        return o;
    }

    public static Tema fromJson(JSONObject o) throws Exception {
        Tema t = new Tema();
        t.id = o.optString("id", ""); t.nome = o.optString("nome", "Meu tema");
        t.bg = o.optInt("bg", t.bg); t.bg2 = o.optInt("bg2", t.bg2); t.bgGrad = o.optBoolean("bgGrad", false);
        t.accent = o.optInt("accent", t.accent); t.stroke = o.optInt("stroke", t.stroke);
        t.contorno = o.optBoolean("contorno", false);
        t.raio = o.optInt("raio", -1); t.efeito = o.optInt("efeito", 0); t.fx = o.optInt("fx", 0);
        t.topBg = o.optInt("topBg", t.topBg); t.topAlpha = o.optInt("topAlpha", 0); t.topText = o.optInt("topText", t.topText);
        JSONArray ga = o.optJSONArray("g");
        if (ga != null) {
            for (int i = 0; i < 4 && i < ga.length(); i++) {
                JSONObject j = ga.getJSONObject(i);
                Grupo x = t.g[i];
                x.bg = j.optInt("bg", x.bg); x.bg2 = j.optInt("bg2", x.bg2); x.text = j.optInt("text", x.text);
                x.alpha = j.optInt("alpha", 255); x.grad = j.optBoolean("grad", false); x.sombra = j.optBoolean("sombra", true);
            }
        }
        return t;
    }

    public Tema copia() {
        try { return fromJson(toJson()); } catch (Exception e) { return new Tema(); }
    }

    public static List<Tema> presets() {
        List<Tema> l = new ArrayList<>();

        Tema a = new Tema();
        a.id = "b0"; a.nome = "Neon Roxo";
        a.bg = 0xFF2B2738; a.bgGrad = true; a.bg2 = escurecer(a.bg, .25f);
        a.accent = 0xFFB584FF; a.stroke = 0xFF4B4560; a.contorno = true; a.efeito = 0;
        a.g[0] = new Grupo(0xFF1B1824, 0xFFFFFFFF);
        a.g[1] = new Grupo(0xFF1F1C2B, a.accent);
        a.g[2] = new Grupo(0xFF1F1C2B, a.accent);
        a.g[3] = new Grupo(0xFF1B1824, 0xFFFFFFFF);
        l.add(a);

        Tema b = new Tema();
        b.id = "b1"; b.nome = "Claro Suave";
        b.bg = 0xFFF4F4FC; b.bgGrad = true; b.bg2 = escurecer(b.bg, .04f);
        b.accent = 0xFFE8793A; b.stroke = 0xFFFFFFFF; b.contorno = false; b.efeito = 1;
        b.topText = 0xFF4B4F8F;
        b.g[0] = new Grupo(0xFFE8E9F8, 0xFF4B4F8F);
        b.g[1] = new Grupo(0xFFE2E4F6, b.accent);
        b.g[2] = new Grupo(0xFFE2E4F6, b.accent);
        b.g[3] = new Grupo(0xFFE8E9F8, 0xFF4B4F8F);
        l.add(b);

        Tema c = new Tema();
        c.id = "b2"; c.nome = "Escuro Pílula";
        c.bg = 0xFF2A2A2A; c.bgGrad = false; c.bg2 = 0xFF1A1A1A;
        c.accent = 0xFFFFA000; c.stroke = 0x00000000; c.contorno = false; c.efeito = 2;
        c.g[0] = new Grupo(0xFF3B3B3D, 0xFFFFFFFF);
        c.g[1] = new Grupo(0xFF343436, c.accent);
        c.g[2] = new Grupo(0xFF343436, c.accent);
        c.g[3] = new Grupo(0xFF3B3B3D, 0xFFFFFFFF);
        l.add(c);
        return l;
    }

    public static int clarear(int c, float f) {
        return Color.argb(Color.alpha(c),
            (int) (Color.red(c) + (255 - Color.red(c)) * f),
            (int) (Color.green(c) + (255 - Color.green(c)) * f),
            (int) (Color.blue(c) + (255 - Color.blue(c)) * f));
    }

    public static int escurecer(int c, float f) {
        return Color.argb(Color.alpha(c),
            (int) (Color.red(c) * (1 - f)),
            (int) (Color.green(c) * (1 - f)),
            (int) (Color.blue(c) * (1 - f)));
    }

    public static int comAlpha(int c, int a) {
        return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c));
    }

    /** Multiplica o alpha da cor por a/255. */
    public static int escalaAlpha(int c, int a) {
        return Color.argb(Color.alpha(c) * a / 255, Color.red(c), Color.green(c), Color.blue(c));
    }
}
