package com.jrocha.teclado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Icones desenhados em codigo (assim seguem a cor do tema). */
public class IconeView extends View {
    // 0 grade, 1 prancheta, 2 traduzir, 3 engrenagem, 4 microfone, 5 seta baixo, 6 flutuante
    final int tipo, cor;
    final boolean fundo;
    final float d;
    boolean sel = false;

    public void setSel(boolean b) { sel = b; invalidate(); }
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    final Path path = new Path();

    public IconeView(Context ctx, int tipo, int cor, boolean fundo) {
        super(ctx);
        this.tipo = tipo; this.cor = cor; this.fundo = fundo;
        d = ctx.getResources().getDisplayMetrics().density;
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    float escala = 1f;

    public void setEscala(float e) { escala = e; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        c.save();
        c.scale(escala, escala, getWidth() / 2f, getHeight() / 2f);
        desenhar(c);
        c.restore();
    }

    void desenhar(Canvas c) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        if (fundo) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Tema.comAlpha(cor, 45));
            c.drawCircle(cx, cy, Math.min(getHeight() / 2f - 2 * d, 19 * d), p);
        }
        p.setColor(sel ? Ui.ACC : cor);
        switch (tipo) {
            case 0: {
                p.setStyle(Paint.Style.FILL);
                float s = 7 * d, g = 3 * d, x0 = cx - s - g / 2, y0 = cy - s - g / 2;
                for (int i = 0; i < 2; i++)
                    for (int j = 0; j < 2; j++) {
                        r.set(x0 + i * (s + g), y0 + j * (s + g), x0 + i * (s + g) + s, y0 + j * (s + g) + s);
                        c.drawRoundRect(r, 2 * d, 2 * d, p);
                    }
                break;
            }
            case 1: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * d);
                r.set(cx - 8 * d, cy - 9 * d, cx + 8 * d, cy + 10 * d);
                c.drawRoundRect(r, 3 * d, 3 * d, p);
                p.setStyle(Paint.Style.FILL);
                r.set(cx - 4 * d, cy - 12 * d, cx + 4 * d, cy - 7 * d);
                c.drawRoundRect(r, 2 * d, 2 * d, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - 4 * d, cy - 1 * d, cx + 4 * d, cy - 1 * d, p);
                c.drawLine(cx - 4 * d, cy + 4 * d, cx + 4 * d, cy + 4 * d, p);
                break;
            }
            case 2: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.8f * d);
                r.set(cx - 12 * d, cy - 10 * d, cx + 12 * d, cy + 10 * d);
                c.drawRoundRect(r, 4 * d, 4 * d, p);
                p.setStyle(Paint.Style.FILL);
                p.setTextSize(12 * d);
                p.setTextAlign(Paint.Align.CENTER);
                p.setFakeBoldText(true);
                c.drawText("G文", cx, cy + 4.5f * d, p);
                p.setFakeBoldText(false);
                break;
            }
            case 3: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2.2f * d);
                c.drawCircle(cx, cy, 6.5f * d, p);
                c.drawCircle(cx, cy, 2.2f * d, p);
                p.setStrokeWidth(3.6f * d);
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45);
                    float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
                    c.drawLine(cx + cs * 8.5f * d, cy + sn * 8.5f * d, cx + cs * 11f * d, cy + sn * 11f * d, p);
                }
                break;
            }
            case 4: {
                p.setStyle(Paint.Style.FILL);
                r.set(cx - 3.5f * d, cy - 10 * d, cx + 3.5f * d, cy + 2 * d);
                c.drawRoundRect(r, 3.5f * d, 3.5f * d, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * d);
                r.set(cx - 7 * d, cy - 6 * d, cx + 7 * d, cy + 8 * d);
                c.drawArc(r, 0, 180, false, p);
                c.drawLine(cx, cy + 8 * d, cx, cy + 11 * d, p);
                c.drawLine(cx - 4 * d, cy + 11 * d, cx + 4 * d, cy + 11 * d, p);
                break;
            }
            case 5: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2.5f * d);
                path.reset();
                path.moveTo(cx - 7 * d, cy - 3 * d);
                path.lineTo(cx, cy + 4 * d);
                path.lineTo(cx + 7 * d, cy - 3 * d);
                c.drawPath(path, p);
                break;
            }
            case 9: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * d);
                c.drawLine(cx - 8 * d, cy - 6 * d, cx + 8 * d, cy - 6 * d, p);
                c.drawLine(cx - 3 * d, cy - 9 * d, cx + 3 * d, cy - 9 * d, p);
                path.reset();
                path.moveTo(cx - 6 * d, cy - 6 * d);
                path.lineTo(cx - 5 * d, cy + 9 * d);
                path.lineTo(cx + 5 * d, cy + 9 * d);
                path.lineTo(cx + 6 * d, cy - 6 * d);
                c.drawPath(path, p);
                c.drawLine(cx - 2 * d, cy - 2 * d, cx - 2 * d, cy + 5 * d, p);
                c.drawLine(cx + 2 * d, cy - 2 * d, cx + 2 * d, cy + 5 * d, p);
                break;
            }
            case 8: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * d);
                c.drawCircle(cx, cy, 9 * d, p);
                p.setStyle(Paint.Style.FILL);
                r.set(cx - 9 * d, cy - 9 * d, cx + 9 * d, cy + 9 * d);
                c.drawArc(r, 90, 180, true, p);
                break;
            }
            case 7: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * d);
                r.set(cx - 11 * d, cy - 8 * d, cx + 11 * d, cy + 8 * d);
                c.drawRoundRect(r, 3 * d, 3 * d, p);
                p.setStyle(Paint.Style.FILL);
                for (int i = 0; i < 5; i++) c.drawCircle(cx - 7 * d + i * 3.5f * d, cy - 3.5f * d, 1.1f * d, p);
                for (int i = 0; i < 4; i++) c.drawCircle(cx - 5.2f * d + i * 3.5f * d, cy, 1.1f * d, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.8f * d);
                c.drawLine(cx - 5 * d, cy + 4 * d, cx + 5 * d, cy + 4 * d, p);
                break;
            }
            default: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * d);
                r.set(cx - 9 * d, cy - 9 * d, cx + 3 * d, cy + 3 * d);
                c.drawRoundRect(r, 3 * d, 3 * d, p);
                r.set(cx - 3 * d, cy - 3 * d, cx + 9 * d, cy + 9 * d);
                c.drawRoundRect(r, 3 * d, 3 * d, p);
            }
        }
    }
}
