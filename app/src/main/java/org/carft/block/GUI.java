package org.carft.block;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

import static org.carft.block.CraftEngine.*;

public class GUI {

    public static void drawActionButton(Canvas c, Paint paint, float cx, float cy, float r, String label, boolean pressed) {
        float outR = r * 1.1f;
        RectF out = new RectF(cx - outR, cy - outR, cx + outR, cy + outR);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x55000000);
        c.drawRoundRect(out, outR, outR, paint);

        RectF in = new RectF(out.left + 4, out.top + 4, out.right - 4, out.bottom - 4);
        float inR = outR - 4;

        if (pressed) {
            paint.setColor(0x99FFFFFF);
            c.drawRoundRect(in, inR, inR, paint);
        } else {
            paint.setColor(0x44000000);
            c.drawRoundRect(in, inR, inR, paint);
            
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(0x77FFFFFF);
            c.drawRoundRect(in, inR, inR, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(outR * 0.45f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setColor(0x99000000);
        c.drawText(label, cx + outR * 0.04f, cy + outR * 0.15f + outR * 0.04f, paint);
        paint.setColor(pressed ? 0xFFFFFFA0 : WHITE);
        c.drawText(label, cx, cy + outR * 0.15f, paint);
        paint.setFakeBoldText(false);
    }

    public static void drawDPad(Canvas c, Paint paint, float x, float y, float r, float jx, float jy, boolean active) {
        float s = r * 0.35f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66222222);
        
        c.drawRoundRect(new RectF(x - s, y - r, x + s, y - s - 4), 16, 16, paint);
        c.drawRoundRect(new RectF(x - s, y + s + 4, x + s, y + r), 16, 16, paint);
        c.drawRoundRect(new RectF(x - r, y - s, x - s - 4, y + s), 16, 16, paint);
        c.drawRoundRect(new RectF(x + s + 4, y - s, x + r, y + s), 16, 16, paint);
        c.drawRoundRect(new RectF(x - s, y - s, x + s, y + s), 16, 16, paint);

        if (active) {
            paint.setColor(0x88DDDDDD);
            if (jy < -0.3f) c.drawRoundRect(new RectF(x - s, y - r, x + s, y - s - 4), 16, 16, paint);
            if (jy > 0.3f) c.drawRoundRect(new RectF(x - s, y + s + 4, x + s, y + r), 16, 16, paint);
            if (jx < -0.3f) c.drawRoundRect(new RectF(x - r, y - s, x - s - 4, y + s), 16, 16, paint);
            if (jx > 0.3f) c.drawRoundRect(new RectF(x + s + 4, y - s, x + r, y + s), 16, 16, paint);
        }
    }

    public static class MenuView extends View {
        final MainActivity owner;
        public int screen = 0;
        public boolean ticking = true;
        float t = 0;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF[] mainBtn = {new RectF(), new RectF(), new RectF()};
        final RectF[] setBtn = {new RectF(), new RectF(), new RectF()};
        
        int[] rdOptions = {12, 16, 20, 24};
        int rdIndex = 2;

        final Runnable ticker = new Runnable() {
            public void run() {
                if (owner.game != null) {
                    ticking = false;
                    return;
                }
                t += 0.02f;
                invalidate();
                postDelayed(this, 33);
            }
        };

        public MenuView(MainActivity o) {
            super(o);
            owner = o;
            paint.setFilterBitmap(false);
            paint.setAntiAlias(false);
            
            for (int i = 0; i < rdOptions.length; i++) {
                if (rdOptions[i] == Game.renderDist) {
                    rdIndex = i;
                    break;
                }
            }
            postDelayed(ticker, 33);
        }

        void layout(int w, int h) {
            float bw = w * 0.38f, bh = h * 0.12f, cx = w / 2f;
            for (int i = 0; i < 3; i++) {
                float top = h * (0.4f + i * 0.15f);
                mainBtn[i].set(cx - bw / 2, top, cx + bw / 2, top + bh);
                setBtn[i].set(cx - bw / 2, top, cx + bw / 2, top + bh);
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            layout(w, h);

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, 0, h * 0.8f,
                    0xFF2E7BE0, 0xFFBFE6FF, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            paint.setColor(0xDDFFFFFF);
            for (int i = 0; i < 6; i++) {
                float cx = ((i * 0.23f + t * 0.03f) % 1.3f) * w - w * 0.1f;
                float cy = h * (0.08f + 0.06f * (i % 3));
                c.drawOval(cx, cy, cx + w * 0.14f, cy + h * 0.05f, paint);
            }

            float bs = h * 0.1f;
            float off = (t * 30) % bs;
            for (int col = -1; col * bs < w + bs; col++) {
                for (int row = 0; row < 4; row++) {
                    int color;
                    if (row == 0) color = ((col + 100) & 1) == 0 ? 0xFF5DAA3A : 0xFF4E9A2E;
                    else color = ((col * 7 + row * 3) & 3) == 0 ? 0xFF7F7F7F : 0xFF8B5A2B;
                    paint.setColor(color);
                    float x = col * bs - off, y = h * 0.8f + row * bs;
                    c.drawRect(x, y, x + bs, y + bs, paint);
                }
            }

            if (screen == 0) {
                drawTextShadow(c, "GoBlock", w / 2f, h * 0.26f, h * 0.17f, 0xFFFFE066, Paint.Align.CENTER, true);
                button(c, mainBtn[0], "MULAI");
                button(c, mainBtn[1], "PENGATURAN");
                button(c, mainBtn[2], "KELUAR");
            } else {
                drawTextShadow(c, "PENGATURAN", w / 2f, h * 0.26f, h * 0.11f, 0xFFFFE066, Paint.Align.CENTER, true);
                button(c, setBtn[0], "SENSITIVITAS: " + SENS_NAMES[Game.sensIndex]);
                button(c, setBtn[1], "JARAK PANDANG: " + Game.renderDist + " CHUNK");
                button(c, setBtn[2], "KEMBALI");
            }

            drawTextShadow(c, "v0.3 HD", w - 20, h - 16, h * 0.05f, 0xFFFFFFFF, Paint.Align.RIGHT, false);
            drawTextShadow(c, "By IqwanoIno", 20, h - 16, h * 0.05f, 0xFFFFFFFF, Paint.Align.LEFT, false);
        }

        void button(Canvas c, RectF r, String label) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF111111);
            c.drawRoundRect(r, 8, 8, paint);
            
            RectF in = new RectF(r.left + 4, r.top + 4, r.right - 4, r.bottom - 4);
            paint.setColor(0xFFC6C6C6);
            c.drawRoundRect(in, 6, 6, paint);
            
            paint.setColor(0xFFFFFFFF);
            c.drawRect(in.left, in.top, in.right, in.top + 8, paint);
            c.drawRect(in.left, in.top, in.left + 8, in.bottom, paint);
            
            paint.setColor(0xFF555555);
            c.drawRect(in.left, in.bottom - 8, in.right, in.bottom, paint);
            c.drawRect(in.right - 8, in.top, in.right, in.bottom, paint);
            
            drawTextShadow(c, label, r.centerX(), r.centerY() + r.height() * 0.15f, r.height() * 0.45f,
                    0xFFFFFFFF, Paint.Align.CENTER, true);
        }

        void drawTextShadow(Canvas c, String s, float x, float y, float size, int color, Paint.Align al, boolean bold) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(size);
            paint.setTextAlign(al);
            paint.setFakeBoldText(bold);
            paint.setColor(0x99000000);
            c.drawText(s, x + size * 0.06f, y + size * 0.06f, paint);
            paint.setColor(color);
            c.drawText(s, x, y, paint);
            paint.setFakeBoldText(false);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() != MotionEvent.ACTION_UP) return true;
            float x = e.getX(), y = e.getY();
            layout(getWidth(), getHeight());
            if (screen == 0) {
                if (mainBtn[0].contains(x, y)) owner.startGame();
                else if (mainBtn[1].contains(x, y)) screen = 1;
                else if (mainBtn[2].contains(x, y)) owner.finish();
            } else {
                if (setBtn[0].contains(x, y)) {
                    Game.sensIndex = (Game.sensIndex + 1) % SENS_NAMES.length;
                    Game.lookSens = SENS_VALUES[Game.sensIndex];
                    owner.saveSettings();
                } else if (setBtn[1].contains(x, y)) {
                    rdIndex = (rdIndex + 1) % rdOptions.length;
                    Game.renderDist = rdOptions[rdIndex];
                    owner.saveSettings();
                } else if (setBtn[2].contains(x, y)) {
                    screen = 0;
                }
            }
            invalidate();
            return true;
        }
    }
}