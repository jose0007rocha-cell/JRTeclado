package com.jrocha.teclado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Linha de entrada continua (rastro do dedo na digitacao deslizante). Nao captura toques. */
public class TrilhaView extends View {
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final List<float[]> pts = new ArrayList<>();
    final Path path = new Path();

    public TrilhaView(Context ctx, int cor) {
        super(ctx);
        float d = ctx.getResources().getDisplayMetrics().density;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(5 * d);
        p.setColor(Tema.comAlpha(cor, 230));
    }

    public void add(float rawX, float rawY) {
        pts.add(new float[]{rawX, rawY});
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        if (pts.size() < 2) return;
        int[] l = new int[2];
        getLocationOnScreen(l);
        path.reset();
        boolean primeiro = true;
        for (float[] q : pts) {
            float x = q[0] - l[0], y = q[1] - l[1];
            if (primeiro) { path.moveTo(x, y); primeiro = false; } else path.lineTo(x, y);
        }
        c.drawPath(path, p);
    }
}
