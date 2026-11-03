package com.example.cybermenu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Кибер-оформление кнопок и ползунков. Рисуется поверх ванильного виджета, ввод остаётся у оригинала. */
public final class Skin {
    private static final int BG = 0x050C24;
    private static final int EDGE = 0x1D4ED8;
    private static final int GLOW = 0x3FE0FF;

    private static final class State {
        float hover;
        long last;
    }

    private static final Map<AbstractWidget, State> STATES = new WeakHashMap<>();
    private static long clickMs = -100000;
    private static double clickX, clickY;

    private Skin() {}

    public static void registerClick(double x, double y) {
        clickMs = System.currentTimeMillis();
        clickX = x;
        clickY = y;
    }

    private static State state(AbstractWidget w, boolean target, long now) {
        State s = STATES.computeIfAbsent(w, k -> new State());
        float dt = s.last == 0 ? 0f : Math.min(0.1f, (now - s.last) / 1000f);
        s.last = now;
        float goal = target ? 1f : 0f;
        s.hover += (goal - s.hover) * Math.min(1f, dt * 14f);
        return s;
    }

    /**
     * Рисует кнопку.
     *
     * @param alpha общая прозрачность (анимация появления)
     * @param xOff  сдвиг по X (анимация появления)
     */
    public static void button(GuiGraphics g, AbstractWidget wd, Component msg, float alpha, float xOff, int mx, int my, long now) {
        Font font = Minecraft.getInstance().font;
        int x = Math.round(wd.getX() + xOff), y = wd.getY(), w = wd.getWidth(), h = wd.getHeight();
        boolean over = mx >= wd.getX() && mx < wd.getX() + w && my >= y && my < y + h;
        boolean hot = wd.active && (over || wd.isFocused());
        State s = state(wd, hot, now);
        float hv = Fx.easeOutCubic(s.hover);
        float A = alpha;

        // тело
        // подложка полностью непрозрачная, иначе сквозь неё виден родной текст ванильной кнопки
        g.fill(x, y, x + w, y + h, Fx.argb(BG, A));
        g.fill(x + 1, y + 1, x + w - 1, y + h / 2, Fx.argb(0xFFFFFF, 0.035f * A));

        // свечение вокруг при наведении
        if (hv > 0.02f) {
            g.fill(x - 1, y - 1, x + w + 1, y, Fx.argb(GLOW, 0.28f * hv * A));
            g.fill(x - 1, y + h, x + w + 1, y + h + 1, Fx.argb(GLOW, 0.28f * hv * A));
            g.fill(x - 1, y, x, y + h, Fx.argb(GLOW, 0.28f * hv * A));
            g.fill(x + w, y, x + w + 1, y + h, Fx.argb(GLOW, 0.28f * hv * A));
            // бегущая заливка слева направо ступеньками
            int steps = 12;
            int fillW = Math.round((w - 2) * hv);
            for (int i = 0; i < steps; i++) {
                int sx0 = x + 1 + fillW * i / steps;
                int sx1 = x + 1 + fillW * (i + 1) / steps;
                g.fill(sx0, y + 1, sx1, y + h - 1, Fx.argb(GLOW, (0.22f * (1f - i / (float) steps)) * A));
            }
        }

        // рамка
        int edge = wd.active ? Fx.lerpRgb(EDGE, GLOW, hv) : 0x1B2440;
        float ea = (wd.active ? 0.85f : 0.55f) * A;
        g.fill(x, y, x + w, y + 1, Fx.argb(edge, ea));
        g.fill(x, y + h - 1, x + w, y + h, Fx.argb(edge, ea));
        g.fill(x, y, x + 1, y + h, Fx.argb(edge, ea));
        g.fill(x + w - 1, y, x + w, y + h, Fx.argb(edge, ea));

        // левая акцентная планка и угловые скобки
        g.fill(x, y + 2, x + 2, y + h - 2, Fx.argb(wd.active ? GLOW : 0x2A3558, (0.30f + 0.70f * hv) * A));
        if (hv > 0.02f) {
            int cl = 5;
            int bc = Fx.argb(0xFFFFFF, 0.9f * hv * A);
            g.fill(x + w - cl, y + h - 1, x + w, y + h, bc);
            g.fill(x + w - 1, y + h - cl, x + w, y + h, bc);
            g.fill(x, y, x + cl, y + 1, bc);
        }

        // вспышка при клике
        float cf = 1f - (System.currentTimeMillis() - clickMs) / 200f;
        if (cf > 0f && clickX >= wd.getX() && clickX < wd.getX() + w && clickY >= y && clickY < y + h) {
            g.fill(x, y, x + w, y + h, Fx.argb(0xFFFFFF, 0.32f * cf * A));
        }

        // подпись
        String label = msg.getString().toUpperCase(Locale.ROOT);
        int fw = font.width(label);
        float sc = fw > w - 10 ? (w - 10f) / fw : 1f;
        int tc = wd.active ? Fx.lerpRgb(0xB8CCFF, 0xFFFFFF, hv) : 0x56627F;
        float tx = x + w / 2f - fw * sc / 2f + hv * 2f;
        float ty = y + (h - 8f * sc) / 2f;
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0f);
        g.pose().scale(sc, sc, 1f);
        g.drawString(font, label, 0, 0, Fx.argb(tc, A), false);
        g.pose().popPose();
    }

    /** Ползунок: ванильный остаётся (на нём ручка), сверху кладём синий тон и рамку. */
    public static void slider(GuiGraphics g, AbstractWidget wd, float alpha, int mx, int my, long now) {
        int x = wd.getX(), y = wd.getY(), w = wd.getWidth(), h = wd.getHeight();
        boolean over = mx >= x && mx < x + w && my >= y && my < y + h;
        State s = state(wd, wd.active && (over || wd.isFocused()), now);
        float hv = Fx.easeOutCubic(s.hover);
        g.fill(x, y, x + w, y + h, Fx.argb(0x06143C, 0.62f * alpha));
        int edge = Fx.lerpRgb(EDGE, GLOW, hv);
        int c = Fx.argb(edge, 0.9f * alpha);
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }
}
