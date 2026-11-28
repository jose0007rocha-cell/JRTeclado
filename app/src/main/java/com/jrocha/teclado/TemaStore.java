package com.jrocha.teclado;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TemaStore {

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("jrteclado", Context.MODE_PRIVATE);
    }

    public static List<Tema> custom(Context c) {
        List<Tema> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString("temas_custom", "[]"));
            for (int i = 0; i < a.length(); i++) l.add(Tema.fromJson(a.getJSONObject(i)));
        } catch (Exception e) { }
        return l;
    }

    public static List<Tema> todos(Context c) {
        List<Tema> l = new ArrayList<>(Tema.presets());
        l.addAll(custom(c));
        return l;
    }

    public static boolean ehCustom(Tema t) { return t.id.startsWith("c"); }

    public static Tema buscar(Context c, String id) {
        for (Tema t : todos(c)) if (t.id.equals(id)) return t;
        return null;
    }

    public static Tema atual(Context c) {
        Tema t = buscar(c, sp(c).getString("tema_id", "b0"));
        return t != null ? t : Tema.presets().get(0);
    }

    public static void selecionar(Context c, String id) {
        sp(c).edit().putString("tema_id", id).apply();
    }

    public static Tema proximo(Context c) {
        List<Tema> l = todos(c);
        String id = sp(c).getString("tema_id", "b0");
        int idx = 0;
        for (int i = 0; i < l.size(); i++) if (l.get(i).id.equals(id)) idx = i;
        Tema n = l.get((idx + 1) % l.size());
        selecionar(c, n.id);
        return n;
    }

    public static void salvar(Context c, Tema t) {
        List<Tema> l = custom(c);
        boolean achou = false;
        for (int i = 0; i < l.size(); i++) if (l.get(i).id.equals(t.id)) { l.set(i, t); achou = true; }
        if (!achou) l.add(t);
        grava(c, l);
    }

    public static void excluir(Context c, String id) {
        List<Tema> l = custom(c);
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).id.equals(id)) l.remove(i);
        grava(c, l);
        new File(c.getFilesDir(), "timg_" + id).delete();
        if (sp(c).getString("tema_id", "b0").equals(id)) selecionar(c, "b0");
    }

    static void grava(Context c, List<Tema> l) {
        try {
            JSONArray a = new JSONArray();
            for (Tema t : l) a.put(t.toJson());
            sp(c).edit().putString("temas_custom", a.toString()).apply();
        } catch (Exception e) { }
    }
}
