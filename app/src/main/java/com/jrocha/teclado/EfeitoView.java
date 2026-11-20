package com.jrocha.teclado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Random;

/** Efeito animado de fundo: neve (1) ou queda de folhas (2). Nao captura toques. */
public class EfeitoView extends View {
    static final int N = 28;
    static final int[] FOLHAS = {0xFFE67E22, 0xFFD35400, 0xFFC0392B, 0xFFF1C40F, 0xFF8E5B2B};
    final int tipo;
    final float d;
    final float[] x = new float[N], y = new float[N], v = new float[N], sz = new float[N],
        ph = new float[N], rot = new float[N], rv = new float[N];
    final int[] cor = new int[N];
    final Random rd = new Random();
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF oval = new RectF();
    boolean pronto = false;

    public EfeitoView(Context ctx, int tipo) {
        super(ctx);
        this.tipo = tipo;
        d = ctx.getResources().getDisplayMetrics().density;
    }

    void resetar(int i, boolean topo) {
        x[i] = rd.nextFloat() * Math.max(1, getWidth());
        y[i] = topo ? -10 * d : rd.nextFloat() * Math.max(1, getHeight());
        v[i] = (tipo == 1 ? 0.5f + rd.nextFloat() * 0.8f : 0.9f + rd.nextFloat() * 1.1f) * d;
        sz[i] = (tipo == 1 ? 1.5f + rd.nextFloat() * 2.5f : 5 + rd.nextFloat() * 4) * d;
        ph[i] = rd.nextFloat() * 6.28f;
        rot[i] = rd.nextFloat() * 360f;
        rv[i] = (rd.nextFloat() - 0.5f) * 5f;
        cor[i] = tipo == 1 ? 0xE6FFFFFF : FOLHAS[rd.nextInt(FOLHAS.length)];
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        for (int i = 0; i < N; i++) resetar(i, false);
        pronto = true;
    }

    @Override protected void onDraw(Canvas c) {
        if (!pronto) return;
        for (int i = 0; i < N; i++) {
            y[i] += v[i];
            ph[i] += 0.03f;
            if (y[i] > getHeight() + sz[i]) resetar(i, true);
            float xx = x[i] + (float) Math.sin(ph[i]) * 12 * d;
            p.setColor(cor[i]);
            if (tipo == 1) {
                c.drawCircle(xx, y[i], sz[i], p);
            } else {
                c.save();
                c.translate(xx, y[i]);
                c.rotate(rot[i]);
                oval.set(-sz[i], -sz[i] / 2f, sz[i], sz[i] / 2f);
                c.drawOval(oval, p);
                c.restore();
                rot[i] += rv[i];
            }
        }
        postInvalidateOnAnimation();
    }
}
