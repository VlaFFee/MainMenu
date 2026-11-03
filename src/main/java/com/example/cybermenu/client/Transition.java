package com.example.cybermenu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Переходы между экранами в компьютерном стиле. Для каждого открытия случайно выбирается один из трёх:
 * развёртка линией, «расшифровка» блоками с цифрами или сдвигающиеся полосы данных.
 */
public final class Transition {
    public static final long DURATION_MS = 700;

    private static final String HEX = "0123456789ABCDEF";
    private static final int CELL = 20;

    private Transition() {}

    public static void draw(GuiGraphics g, int w, int h, float t, long seed) {
        if (t >= 1f) return;
        switch ((int) (Fx.hash(seed) * 3f) % 3) {
            case 0 -> scan(g, w, h, t);
            case 1 -> decrypt(g, w, h, t, seed);
            default -> bars(g, w, h, t, seed);
        }
    }

    // ------------------------------------------------ 0: развёртка линией

    private static void scan(GuiGraphics g, int w, int h, float t) {
        float p = Fx.easeInOutCubic(t);
        float lineY = p * (h + 24f) - 12f;

        Fx.begin(g);
        float flash = 1f - Fx.smooth(0f, 0.18f, t);
        Fx.rect(0, 0, w, h, Fx.argb(0x3FD0FF, 0.22f * flash));

        float cover = 0.96f * (1f - Fx.smooth(0.85f, 1f, t));
        if (lineY < h) Fx.rect(0, Math.max(0f, lineY), w, h, Fx.argb(0x020617, cover));

        int cell = 14;
        float band = 110f;
        int y0 = (int) Math.max(0f, lineY - 8f) / cell;
        int y1 = (int) Math.min(h, lineY + band) / cell + 1;
        long tick = (long) (t * 40f);
        for (int cy = y0; cy <= y1; cy++) {
            float py = cy * cell;
            float dist = (py - lineY) / band;
            if (dist < -0.1f || dist > 1f) continue;
            for (int cx = 0; cx * cell < w; cx++) {
                float hs = Fx.hash(cx * 7919L + cy * 104729L + tick * 31L);
                if (hs < 0.55f + dist * 0.45f) continue;
                float a = (1f - Math.max(0f, dist)) * 0.55f * (0.4f + 0.6f * Fx.hash(cx * 31L + cy * 17L + tick));
                int col = hs > 0.93f ? 0xBFF3FF : 0x2F8CFF;
                Fx.rect(cx * cell, py, cx * cell + cell - 2, py + cell - 2, Fx.argb(col, a));
            }
        }

        if (lineY > -12f && lineY < h + 12f) {
            Fx.rectV(0, lineY - 46f, w, lineY, Fx.argb(0x3FD0FF, 0f), Fx.argb(0x3FD0FF, 0.22f));
            Fx.rectV(0, lineY + 2f, w, lineY + 30f, Fx.argb(0x3FD0FF, 0.16f), Fx.argb(0x3FD0FF, 0f));
            Fx.rect(0, lineY, w, lineY + 2f, Fx.argb(0xE8FBFF, 0.95f));
        }
        Fx.end();
    }

    // ---------------------------------- 1: «расшифровка» блоками с цифрами

    private static void decrypt(GuiGraphics g, int w, int h, float t, long seed) {
        int cols = w / CELL + 1, rows = h / CELL + 1;
        float tt = t * 1.5f;
        long sd = seed % 1000L;
        int maxDigits = 140;
        int[] dx = new int[maxDigits];
        int[] dy = new int[maxDigits];
        float[] da = new float[maxDigits];
        int[] dc = new int[maxDigits];
        int n = 0;

        Fx.begin(g);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float s = Fx.hash(c * 7919L + r * 104729L + sd * 13L);
                float p = tt - s * 0.75f;
                int x = c * CELL, y = r * CELL;
                if (p >= 0.3f) continue;
                if (p < 0f) {
                    Fx.rect(x, y, x + CELL, y + CELL, Fx.argb(0x020617, 0.96f));
                } else {
                    float k = 1f - p / 0.3f;
                    Fx.rect(x, y, x + CELL, y + CELL, Fx.argb(0x020617, 0.96f * k));
                    Fx.rect(x + 1, y + 1, x + CELL - 1, y + CELL - 1, Fx.argb(0x3FD0FF, 0.35f * k));
                    if (n < maxDigits) {
                        dx[n] = x + 6;
                        dy[n] = y + 6;
                        da[n] = k;
                        dc[n] = (int) (Fx.hash(c * 31L + r * 17L + (long) (t * 30f)) * 16f) & 15;
                        n++;
                    }
                }
            }
        }
        Fx.end();

        if (n > 0) {
            final int count = n;
            Font f = Minecraft.getInstance().font;
            g.drawManaged(() -> {
                for (int i = 0; i < count; i++) {
                    g.drawString(f, String.valueOf(HEX.charAt(dc[i])), dx[i], dy[i], Fx.argb(0xE8FBFF, da[i]), false);
                }
            });
        }
    }

    // ------------------------------- 2: полосы данных, уезжающие в стороны

    private static void bars(GuiGraphics g, int w, int h, float t, long seed) {
        int count = 16;
        float bh = h / (float) count;
        long sd = seed % 1000L;
        Fx.begin(g);
        for (int i = 0; i < count; i++) {
            float delay = Fx.hash(i * 37L + sd) * 0.35f;
            float pr = Fx.easeInOutCubic((t - delay) / 0.55f);
            if (pr >= 1f) continue;
            boolean right = Fx.hash(i * 53L + sd * 3L) > 0.5f;
            float y0 = i * bh, y1 = y0 + bh - 1f;
            float front = right ? pr * w : w - pr * w;
            if (right) Fx.rect(front, y0, w, y1, Fx.argb(0x020617, 0.96f));
            else Fx.rect(0, y0, front, y1, Fx.argb(0x020617, 0.96f));
            // светящийся край и блоки «данных» у фронта
            float dir = right ? -1f : 1f;
            Fx.rectH(front + (right ? -18f : 0f), y0, front + (right ? 0f : 18f), y1,
                    Fx.argb(0x3FD0FF, right ? 0f : 0.28f), Fx.argb(0x3FD0FF, right ? 0.28f : 0f));
            Fx.rect(front - 1f, y0, front + 1f, y1, Fx.argb(0xE8FBFF, 0.95f));
            for (int k = 0; k < 3; k++) {
                float hs = Fx.hash(i * 11L + k * 5L + (long) (t * 25f));
                if (hs < 0.5f) continue;
                float bx = front + dir * (6f + hs * 90f);
                Fx.rect(bx, y0 + 2f, bx + 8f + hs * 14f, y1 - 2f, Fx.argb(0x2F8CFF, 0.5f * (1f - pr)));
            }
        }
        float flash = 1f - Fx.smooth(0f, 0.12f, t);
        Fx.rect(0, 0, w, h, Fx.argb(0x3FD0FF, 0.16f * flash));
        Fx.end();
    }
}
