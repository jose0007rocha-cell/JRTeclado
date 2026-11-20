package com.jrocha.teclado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Alca de canto (arco) para redimensionar o teclado flutuante. */
public class CantoView extends View {
    final int canto; // 0 TL, 1 TR, 2 BL, 3 BR
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();

    public CantoView(Context ctx, int canto, int cor) {
        super(ctx);
        this.canto = canto;
        float d = ctx.getResources().getDisplayMetrics().density;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(4 * d);
        p.setColor(cor);
    }

    @Override protected void onDraw(Canvas c) {
        float pd = 3 * getResources().getDisplayMetrics().density;
        r.set(pd, pd, getWidth() - pd, getHeight() - pd);
        float start = canto == 0 ? 180 : canto == 1 ? 270 : canto == 2 ? 90 : 0;
        c.drawArc(r, start, 90, false, p);
    }
}
