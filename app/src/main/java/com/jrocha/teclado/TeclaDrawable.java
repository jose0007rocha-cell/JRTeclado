package com.jrocha.teclado;

import android.graphics.Canvas;
import android.graphics.Color;
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

/** Fundo da tecla: plano, neon, relevo suave ou pilula 3D, conforme o tema. */
public class TeclaDrawable extends Drawable {
    final Tema t;
    final Tema.Grupo g;
    final float d;
    boolean pressed = false;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    final RectF r2 = new RectF();
    final Path clip = new Path();

    public TeclaDrawable(Tema t, Tema.Grupo g, float dens) {
        this.t = t; this.g = g; this.d = dens;
    }

    @Override public boolean isStateful() { return true; }

    @Override protected boolean onStateChange(int[] s) {
        boolean pr = false;
        for (int i : s) if (i == android.R.attr.state_pressed) pr = true;
        if (pr != pressed) { pressed = pr; invalidateSelf(); return true; }
        return false;
    }

    int al(int cor) {
        return Color.argb(Color.alpha(cor) * g.alpha / 255, Color.red(cor), Color.green(cor), Color.blue(cor));
    }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        float rad = Math.min(t.raioEf() * d, Math.min(w, h) / 2f);
        float m = (t.estilo == 2 ? 4 : 1.5f) * d;
        float baixo = (t.estilo == 3 && g.sombra) ? 2 * d : 0;
        r.set(b.left + m, b.top + m, b.right - m, b.bottom - m - baixo);
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);

        // sombras externas
        if (g.sombra && g.fundo && !pressed) {
            if (t.estilo == 3) {
                r2.set(r.left, r.top + 2 * d, r.right, r.bottom + 2 * d);
                p.setColor(al(0x66000000));
                c.drawRoundRect(r2, rad, rad, p);
            } else if (t.estilo == 2) {
                r2.set(r.left + 3 * d, r.top + 3 * d, r.right + 3 * d, r.bottom + 3 * d);
                p.setColor(al(0x185A5F9A));
                c.drawRoundRect(r2, rad, rad, p);
                r2.set(r.left + 2 * d, r.top + 2 * d, r.right + 2 * d, r.bottom + 2 * d);
                p.setColor(al(0x405A5F9A));
                c.drawRoundRect(r2, rad, rad, p);
                r2.set(r.left - 2 * d, r.top - 2 * d, r.right - 2 * d, r.bottom - 2 * d);
                p.setColor(al(0xFFFFFFFF));
                c.drawRoundRect(r2, rad, rad, p);
            } else if (t.estilo == 0) {
                r2.set(r.left, r.top + 1.5f * d, r.right, r.bottom + 1.5f * d);
                p.setColor(al(0x55000000));
                c.drawRoundRect(r2, rad, rad, p);
            }
        }

        // preenchimento
        if (g.fundo) {
            int top, bot;
            if (pressed) { top = bot = Tema.destacar(g.cor); }
            else if (!g.grad) { top = bot = g.cor; }
            else if (t.estilo == 2) { top = Tema.clarear(g.cor, .5f); bot = g.cor; }
            else { top = Tema.clarear(g.cor, .12f); bot = Tema.escurecer(g.cor, .22f); }
            p.setShader(new LinearGradient(0, r.top, 0, r.bottom, al(top), al(bot), Shader.TileMode.CLAMP));
            c.drawRoundRect(r, rad, rad, p);
            p.setShader(null);
        } else if (pressed) {
            p.setColor(0x22FFFFFF);
            c.drawRoundRect(r, rad, rad, p);
        }

        // efeitos internos (neon e pilula)
        if (g.fundo && (t.estilo == 1 || t.estilo == 3)) {
            c.save();
            clip.reset();
            clip.addRoundRect(r, rad, rad, Path.Direction.CW);
            c.clipPath(clip);
            if (t.estilo == 1 && g.sombra) {
                float cx = r.centerX();
                float gr = r.width() * 0.42f;
                p.setShader(new RadialGradient(cx, r.bottom, gr, al(Tema.comAlpha(t.accent, pressed ? 210 : 110)),
                    Tema.comAlpha(t.accent, 0), Shader.TileMode.CLAMP));
                c.drawCircle(cx, r.bottom, gr, p);
            }
            int hl = t.estilo == 1 ? 0x40FFFFFF : 0x30FFFFFF;
            float hh = r.height() * (t.estilo == 1 ? .45f : .5f);
            p.setShader(new LinearGradient(0, r.top, 0, r.top + hh, hl, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(r.left, r.top, r.right, r.top + hh, p);
            p.setShader(null);
            c.restore();
            if (t.estilo == 1 && g.sombra) {
                p.setColor(t.accent);
                float bw = Math.min(r.width() * 0.28f, 14 * d);
                c.drawRoundRect(r.centerX() - bw / 2, r.bottom - 3.5f * d, r.centerX() + bw / 2, r.bottom - 2f * d, d, d, p);
            }
        }

        // contorno e destaque ao tocar
        if (t.contorno || pressed) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth((pressed ? Math.max(1.5f, t.contornoLargura) : t.contornoLargura) * d);
            p.setColor(pressed ? Tema.comAlpha(t.accent, 190) : t.corContorno);
            c.drawRoundRect(r, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
