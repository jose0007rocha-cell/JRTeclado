package com.jrocha.teclado;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

public class SettingsActivity extends BaseTela {

    @Override protected String titulo() { return "JR Teclado"; }

    View.OnClickListener sub(final String tela) {
        return v -> {
            Intent i = new Intent(this, SubTelaActivity.class);
            i.putExtra("tela", tela);
            startActivity(i);
        };
    }

    @Override protected void construir() {
        LinearLayout c1 = Ui.cartao(this);
        Ui.add(c1, Ui.item(this, "Ativar teclado", "Habilitar o JR Teclado no Android",
            v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))));
        Ui.add(c1, Ui.item(this, "Selecionar teclado", "Escolher o JR Teclado agora",
            v -> ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker()));
        col.addView(c1);

        LinearLayout c2 = Ui.cartao(this);
        Ui.add(c2, Ui.item(this, "Temas", Tema.selecionado(sp).nome,
            v -> startActivity(new Intent(this, TemasActivity.class))));
        Ui.add(c2, Ui.item(this, "Aparência", "Altura, margens, animação", sub("aparencia")));
        Ui.add(c2, Ui.item(this, "Som e vibração", "Som das teclas, volume", sub("som")));
        Ui.add(c2, Ui.item(this, "Teclas", "Cursor, linha numérica, popup, ajuste automático", sub("teclas")));
        Ui.add(c2, Ui.item(this, "Barra superior", "Sugestões de palavras, emoji e e-mail", sub("barra")));
        Ui.add(c2, Ui.item(this, "Gestos", "Digitação deslizante", sub("gestos")));
        Ui.add(c2, Ui.item(this, "Fontes", "Fonte das teclas, estilo do texto", sub("fonte")));
        Ui.add(c2, Ui.item(this, "Teclado flutuante", "Ativar, tamanho e posição", sub("flutuante")));
        String[] nomesModo = {"Usar do sistema", "Claro", "Escuro"};
        Ui.add(c2, Ui.item(this, "Modo do aplicativo", nomesModo[Math.max(0, Math.min(2, sp.getInt("modo_app", 0)))], sub("modo")));
        col.addView(c2);

        col.addView(Ui.secao(this, "Teste"));
        LinearLayout c3 = Ui.cartao(this);
        EditText et = new EditText(this);
        et.setHint("Toque aqui para testar o teclado");
        et.setTextColor(Ui.TXT);
        et.setHintTextColor(Ui.SUB);
        c3.addView(et);
        col.addView(c3);
    }
}
