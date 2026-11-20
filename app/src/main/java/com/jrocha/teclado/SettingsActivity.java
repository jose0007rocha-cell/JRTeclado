package com.jrocha.teclado;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

public class SettingsActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
    }

    @Override protected void onResume() {
        super.onResume();
        montar();
    }

    void abrir(String tela) {
        Intent i = new Intent(this, TelaActivity.class);
        i.putExtra("tela", tela);
        startActivity(i);
    }

    void montar() {
        Ui.Tela t = Ui.tela(this, "JR Teclado", null);
        LinearLayout c1 = Ui.card(t.col);
        Ui.add(c1, Ui.item(this, "1. Ativar teclado", "Ativar nas configurações do Android",
            v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))));
        Ui.add(c1, Ui.item(this, "2. Selecionar teclado", "Escolher o JR Teclado agora",
            v -> ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker()));

        Ui.secao(t.col, "PERSONALIZAÇÃO");
        LinearLayout c2 = Ui.card(t.col);
        Ui.add(c2, Ui.item(this, "Temas", TemaStore.atual(this).nome,
            v -> startActivity(new Intent(this, TemasActivity.class))));
        Ui.add(c2, Ui.item(this, "Aparência", "Altura, efeitos, teclado flutuante", v -> abrir("aparencia")));
        Ui.add(c2, Ui.item(this, "Fontes", "Fonte das teclas, arquivo .ttf/.otf", v -> abrir("fontes")));
        Ui.add(c2, Ui.item(this, "Som e vibração", "Som das teclas, volume, vibração", v -> abrir("som")));

        Ui.secao(t.col, "TESTE");
        LinearLayout c3 = Ui.card(t.col);
        EditText et = new EditText(this);
        et.setHint("Toque aqui para testar o teclado");
        et.setHintTextColor(Ui.SUB);
        et.setTextColor(Ui.TEXTO);
        c3.addView(et);
    }
}
