package com.jrocha.teclado;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Imagem de fundo em "cover" (preenche e corta o excesso). */
public class CoverDrawable extends Drawable {
    final Bitmap bm;
    final Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
    final Rect src = new Rect();

    public CoverDrawable(Bitmap bm) { this.bm = bm; }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        if (bm == null || b.width() == 0 || b.height() == 0) return;
        float sc = Math.max(b.width() / (float) bm.getWidth(), b.height() / (float) bm.getHeight());
        int sw = Math.min(bm.getWidth(), (int) (b.width() / sc));
        int sh = Math.min(bm.getHeight(), (int) (b.height() / sc));
        int sx = (bm.getWidth() - sw) / 2, sy = (bm.getHeight() - sh) / 2;
        src.set(sx, sy, sx + sw, sy + sh);
        c.drawBitmap(bm, src, b, p);
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { p.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
