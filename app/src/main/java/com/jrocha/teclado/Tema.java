package com.jrocha.teclado;

public class Tema {
    public final String nome;
    public final int bg, key, keyPressed, special, text, hint, accent, stroke;
    public final boolean pill;

    Tema(String nome, int bg, int key, int keyPressed, int special, int text, int hint, int accent, int stroke, boolean pill) {
        this.nome = nome; this.bg = bg; this.key = key; this.keyPressed = keyPressed; this.special = special;
        this.text = text; this.hint = hint; this.accent = accent; this.stroke = stroke; this.pill = pill;
    }

    public static final Tema[] TODOS = {
        new Tema("Neon Roxo",     0xFF2B2738, 0xFF191722, 0xFF3A3450, 0xFF1F1C2B, 0xFFFFFFFF, 0xFFB8B2CC, 0xFFB584FF, 0xFF4B4560, false),
        new Tema("Claro Suave",   0xFFF4F4FC, 0xFFE8E9F8, 0xFFD3D5F0, 0xFFE2E4F6, 0xFF4B4F8F, 0xFF7A7DB0, 0xFFE8793A, 0xFFFFFFFF, false),
        new Tema("Escuro Pílula", 0xFF2A2A2A, 0xFF3B3B3D, 0xFF55555A, 0xFF343436, 0xFFFFFFFF, 0xFFCCCCCC, 0xFFFFA000, 0x00000000, true)
    };

    public static Tema get(int i) {
        return TODOS[Math.max(0, Math.min(i, TODOS.length - 1))];
    }
}
