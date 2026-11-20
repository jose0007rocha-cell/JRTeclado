package com.jrocha.teclado;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public abstract class BaseTela extends Activity {
    protected SharedPreferences sp;
    protected LinearLayout col;
    private boolean criado = false;

    protected abstract String titulo();
    protected abstract void construir();
    protected boolean recarregarAoVoltar() { return true; }
    protected View acaoTopo() { return null; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("jrteclado", Context.MODE_PRIVATE);
        getWindow().setStatusBarColor(Ui.BG);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(Ui.BG);
        LinearLayout topo = new LinearLayout(this);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        topo.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
        TextView volta = new TextView(this);
        volta.setText("←");
        volta.setTextSize(26);
        volta.setTextColor(Ui.TXT);
        volta.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 6));
        volta.setOnClickListener(v -> finish());
        TextView t = new TextView(this);
        t.setText(titulo());
        t.setTextSize(22);
        t.setTextColor(Ui.TXT);
        topo.addView(volta);
        topo.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        View a = acaoTopo();
        if (a != null) topo.addView(a);
        ScrollView sv = new ScrollView(this);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 24));
        sv.addView(col);
        raiz.addView(topo);
        raiz.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(raiz);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!criado || recarregarAoVoltar()) {
            col.removeAllViews();
            construir();
            criado = true;
        }
    }
}
