package com.jrocha.teclado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Random;

/** Efeito de fundo animado: 1 = neve, 2 = queda de folhas. */
public class FxView extends View {
    final int tipo, n;
    final float[] x, y, v, ph, r, rot;
    final int[] cor;
    final Random rnd = new Random();
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF rc = new RectF();
    long ult = 0;
    boolean ativo = false;
    static final int[] FOLHAS = {0xFFE8793A, 0xFFC0502B, 0xFFD9A441, 0xFF7A9A3B};

    public FxView(Context ctx, int tipo) {
        super(ctx);
        this.tipo = tipo;
        n = tipo == 1 ? 36 : 16;
        x = new float[n]; y = new float[n]; v = new float[n]; ph = new float[n]; r = new float[n]; rot = new float[n];
        cor = new int[n];
        for (int i = 0; i < n; i++) init(i, true);
    }

    void init(int i, boolean aleatorioY) {
        x[i] = rnd.nextFloat();
        y[i] = aleatorioY ? rnd.nextFloat() : -0.05f;
        v[i] = tipo == 1 ? .06f + rnd.nextFloat() * .12f : .10f + rnd.nextFloat() * .12f;
        ph[i] = rnd.nextFloat() * 6.28f;
        r[i] = tipo == 1 ? 1.5f + rnd.nextFloat() * 2.5f : 4f + rnd.nextFloat() * 3f;
        rot[i] = rnd.nextFloat() * 360f;
        cor[i] = FOLHAS[rnd.nextInt(FOLHAS.length)];
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(resolveSize(0, wSpec), resolveSize(0, hSpec));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ativo = true; ult = 0; invalidate();
    }

    @Override protected void onDetachedFromWindow() {
        ativo = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        long now = System.nanoTime();
        float dt = ult == 0 ? 0.016f : Math.min(0.05f, (now - ult) / 1e9f);
        ult = now;
        float d = getResources().getDisplayMetrics().density;
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            y[i] += v[i] * dt;
            ph[i] += dt * (tipo == 1 ? 1.2f : 1.8f);
            rot[i] += dt * 80f;
            if (y[i] > 1.05f) init(i, false);
            float px = (x[i] + (float) Math.sin(ph[i]) * 0.03f) * w;
            float py = y[i] * h;
            if (tipo == 1) {
                p.setColor(0xCCFFFFFF);
                c.drawCircle(px, py, r[i] * d, p);
            } else {
                c.save();
                c.translate(px, py);
                c.rotate(rot[i]);
                p.setColor(cor[i]);
                rc.set(-r[i] * 1.5f * d, -r[i] * 0.75f * d, r[i] * 1.5f * d, r[i] * 0.75f * d);
                c.drawOval(rc, p);
                c.restore();
            }
        }
        if (ativo) postInvalidateOnAnimation();
    }
}
