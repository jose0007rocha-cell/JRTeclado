package com.jrocha.teclado;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Fundo da tecla com efeitos (neon, relevo suave, pilula 3D). */
public class TeclaDrawable extends Drawable {
    final Tema t;
    final boolean esp;
    final float d;
    boolean pressed = false;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    final RectF r2 = new RectF();
    final Path clip = new Path();

    public TeclaDrawable(Tema t, boolean esp, float dens) {
        this.t = t; this.esp = esp; this.d = dens;
    }

    @Override public boolean isStateful() { return true; }

    @Override protected boolean onStateChange(int[] s) {
        boolean pr = false;
        for (int i : s) if (i == android.R.attr.state_pressed) pr = true;
        if (pr != pressed) { pressed = pr; invalidateSelf(); return true; }
        return false;
    }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        float rad = Math.min(t.pill ? 40 * d : 12 * d, Math.min(w, h) / 2f);
        int base = esp ? t.special : t.key;
        if (t.efeito == 1) soft(c, b, w, h, rad, base);
        else if (t.efeito == 2) pilula(c, b, w, h, rad, base);
        else neon(c, b, w, h, rad, base);
    }

    void neon(Canvas c, Rect b, float w, float h, float rad, int base) {
        r.set(b.left + d, b.top + d, b.right - d, b.bottom - d);
        int topC = pressed ? Tema.clarear(base, .35f) : Tema.clarear(base, .10f);
        int botC = pressed ? Tema.clarear(base, .15f) : Tema.escurecer(base, .25f);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, r.top, 0, r.bottom, topC, botC, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, p);
        p.setShader(null);
        c.save();
        clip.reset();
        clip.addRoundRect(r, rad, rad, Path.Direction.CW);
        c.clipPath(clip);
        float cx = r.centerX();
        float gr = w * 0.42f;
        p.setShader(new RadialGradient(cx, r.bottom, gr, Tema.comAlpha(t.accent, pressed ? 210 : 110),
            Tema.comAlpha(t.accent, 0), Shader.TileMode.CLAMP));
        c.drawCircle(cx, r.bottom, gr, p);
        p.setShader(new LinearGradient(0, r.top, 0, r.top + h * 0.45f, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(r.left, r.top, r.right, r.top + h * 0.45f, p);
        p.setShader(null);
        c.restore();
        p.setColor(t.accent);
        float bw = Math.min(w * 0.28f, 14 * d);
        c.drawRoundRect(cx - bw / 2, r.bottom - 3.5f * d, cx + bw / 2, r.bottom - 2f * d, d, d, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.2f * d);
        p.setColor(pressed ? t.accent : t.stroke);
        c.drawRoundRect(r, rad, rad, p);
        p.setStyle(Paint.Style.FILL);
    }

    void soft(Canvas c, Rect b, float w, float h, float rad, int base) {
        float m = 4 * d;
        r.set(b.left + m, b.top + m, b.right - m, b.bottom - m);
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        if (!pressed) {
            r2.set(r.left + 3 * d, r.top + 3 * d, r.right + 3 * d, r.bottom + 3 * d);
            p.setColor(0x185A5F9A);
            c.drawRoundRect(r2, rad, rad, p);
            r2.set(r.left + 2 * d, r.top + 2 * d, r.right + 2 * d, r.bottom + 2 * d);
            p.setColor(0x405A5F9A);
            c.drawRoundRect(r2, rad, rad, p);
            r2.set(r.left - 2 * d, r.top - 2 * d, r.right - 2 * d, r.bottom - 2 * d);
            p.setColor(0xFFFFFFFF);
            c.drawRoundRect(r2, rad, rad, p);
        }
        if (pressed) {
            p.setColor(t.keyPressed);
        } else {
            p.setShader(new LinearGradient(0, r.top, 0, r.bottom, Tema.clarear(base, .55f), base, Shader.TileMode.CLAMP));
        }
        c.drawRoundRect(r, rad, rad, p);
        p.setShader(null);
        if (pressed) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f * d);
            p.setColor(Tema.comAlpha(t.accent, 160));
            c.drawRoundRect(r, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    void pilula(Canvas c, Rect b, float w, float h, float rad, int base) {
        float m = 1.5f * d;
        r.set(b.left + m, b.top + m, b.right - m, b.bottom - m - 2 * d);
        p.setStyle(Paint.Style.FILL);
        r2.set(r.left, r.top + 2 * d, r.right, r.bottom + 2 * d);
        p.setShader(null);
        p.setColor(0x66000000);
        c.drawRoundRect(r2, rad, rad, p);
        int topC = pressed ? Tema.clarear(base, .30f) : Tema.clarear(base, .18f);
        int botC = pressed ? Tema.clarear(base, .05f) : Tema.escurecer(base, .18f);
        p.setShader(new LinearGradient(0, r.top, 0, r.bottom, topC, botC, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, p);
        c.save();
        clip.reset();
        clip.addRoundRect(r, rad, rad, Path.Direction.CW);
        c.clipPath(clip);
        p.setShader(new LinearGradient(0, r.top, 0, r.top + h * 0.5f, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(r.left, r.top, r.right, r.top + h * 0.5f, p);
        c.restore();
        p.setShader(null);
        if (pressed) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f * d);
            p.setColor(t.accent);
            c.drawRoundRect(r, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
