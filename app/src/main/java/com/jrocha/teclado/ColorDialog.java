package com.jrocha.teclado;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ColorDialog {

    public static void mostrar(Context c, int atual, final Ui.IntCb cb) {
        final float[] hsv = new float[3];
        Color.colorToHSV(atual, hsv);
        final int[] cur = {atual | 0xFF000000};
        final boolean[] lock = {false};

        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int pd = Ui.dp(c, 20);
        l.setPadding(pd, pd, pd, pd);

        final View prev = new View(c);
        prev.setBackgroundColor(cur[0]);
        l.addView(prev, new LinearLayout.LayoutParams(-1, Ui.dp(c, 48)));

        final SeekBar sh = new SeekBar(c), ss = new SeekBar(c), sv = new SeekBar(c);
        sh.setMax(360); ss.setMax(100); sv.setMax(100);
        sh.setProgress((int) hsv[0]); ss.setProgress((int) (hsv[1] * 100)); sv.setProgress((int) (hsv[2] * 100));
        l.addView(Ui.txt(c, "Matiz", 13, Ui.SUB)); l.addView(sh);
        l.addView(Ui.txt(c, "Saturação", 13, Ui.SUB)); l.addView(ss);
        l.addView(Ui.txt(c, "Brilho", 13, Ui.SUB)); l.addView(sv);

        final EditText hex = new EditText(c);
        hex.setSingleLine(true);
        hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hex.setText(String.format("#%06X", atual & 0xFFFFFF));
        l.addView(hex);

        SeekBar.OnSeekBarChangeListener ch = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (lock[0] || !user) return;
                hsv[0] = sh.getProgress(); hsv[1] = ss.getProgress() / 100f; hsv[2] = sv.getProgress() / 100f;
                cur[0] = Color.HSVToColor(hsv);
                prev.setBackgroundColor(cur[0]);
                lock[0] = true;
                hex.setText(String.format("#%06X", cur[0] & 0xFFFFFF));
                lock[0] = false;
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        };
        sh.setOnSeekBarChangeListener(ch); ss.setOnSeekBarChangeListener(ch); sv.setOnSeekBarChangeListener(ch);

        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c2) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c2) { }
            @Override public void afterTextChanged(Editable e) {
                if (lock[0]) return;
                String t = e.toString().trim();
                if (!t.startsWith("#")) t = "#" + t;
                if (t.length() != 7) return;
                try {
                    int col = Color.parseColor(t) | 0xFF000000;
                    cur[0] = col;
                    Color.colorToHSV(col, hsv);
                    lock[0] = true;
                    sh.setProgress((int) hsv[0]); ss.setProgress((int) (hsv[1] * 100)); sv.setProgress((int) (hsv[2] * 100));
                    lock[0] = false;
                    prev.setBackgroundColor(col);
                } catch (IllegalArgumentException ex) { }
            }
        });

        new AlertDialog.Builder(c, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Escolher cor")
            .setView(l)
            .setPositiveButton("OK", (d, w) -> cb.on(cur[0]))
            .setNegativeButton("Cancelar", null)
            .show();
    }
}
