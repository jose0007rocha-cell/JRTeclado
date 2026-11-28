package com.jrocha.teclado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Linha de entrada continua (rastro do dedo). Fica sempre no teclado; so desenha durante o gesto. */
public class TrilhaView extends View {
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final List<float[]> pts = new ArrayList<>();
    final Path path = new Path();
    float ox, oy;

    public TrilhaView(Context ctx, int cor) {
        super(ctx);
        float d = ctx.getResources().getDisplayMetrics().density;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(5 * d);
        p.setColor(Tema.comAlpha(cor, 230));
    }

    /** Define a origem (posicao desta view na tela) e limpa o rastro. */
    public void iniciar() {
        int[] l = new int[2];
        getLocationOnScreen(l);
        ox = l[0];
        oy = l[1];
        pts.clear();
        invalidate();
    }

    public void limpar() {
        pts.clear();
        invalidate();
    }

    public void add(float rawX, float rawY) {
        pts.add(new float[]{rawX - ox, rawY - oy});
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        if (pts.size() < 2) return;
        path.reset();
        boolean primeiro = true;
        for (float[] q : pts) {
            if (primeiro) { path.moveTo(q[0], q[1]); primeiro = false; } else path.lineTo(q[0], q[1]);
        }
        c.drawPath(path, p);
    }
}
