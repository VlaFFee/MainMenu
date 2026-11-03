package com.example.cybermenu.client;

import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Главное меню в виде кибер-терминала: логотип «расшифровывается», пункты выезжают, фон живой. */
public class CyberTitleScreen extends Screen {
    private static final String LOGO = "OUTSIDE";
    private static final String SCRAMBLE = "01#$%&@ABCDEF<>/\\|=+*";
    private static final int ACCENT = 0x3FE0FF;

    private static final class Entry {
        final String label;
        final boolean small;
        final Runnable action;
        float hover;
        int x, y, w, h;
        long delay;

        Entry(String label, boolean small, Runnable action) {
            this.label = label;
            this.small = small;
            this.action = action;
        }
    }

    private static final String QUIT_TEXT = "Я ещё увижу твой код";
    /** Идёт анимация выхода: гул затихает, ввод заблокирован. */
    static boolean quitting;

    private long quitMs;
    private boolean quitDone;

    private final List<Entry> entries = new ArrayList<>();
    private int selected = -1;
    private int lastHover = -1;
    private long lastFrame;
    private long pressMs = -100000;
    private int pressIdx = -1;

    public CyberTitleScreen() {
        super(Component.translatable("cybermenu.title"));
    }

    @Override
    protected void init() {
        entries.clear();
        Minecraft mc = this.minecraft;
        entries.add(new Entry(tr("cybermenu.menu.singleplayer"), false, () -> mc.setScreen(new SelectWorldScreen(this))));
        entries.add(new Entry(tr("cybermenu.menu.multiplayer"), false, () -> mc.setScreen(new JoinMultiplayerScreen(this))));
        entries.add(new Entry(tr("cybermenu.menu.mods"), false, () -> mc.setScreen(new ModListScreen(this))));
        entries.add(new Entry(tr("cybermenu.menu.options"), false, () -> mc.setScreen(new OptionsScreen(this, mc.options))));
        entries.add(new Entry(tr("cybermenu.menu.quit"), false, this::startQuit));
        entries.add(new Entry(tr("cybermenu.menu.language"), true,
                () -> mc.setScreen(new LanguageSelectScreen(this, mc.options, mc.getLanguageManager()))));
        entries.add(new Entry(tr("cybermenu.menu.access"), true,
                () -> mc.setScreen(new AccessibilityOptionsScreen(this, mc.options))));
        for (int i = 0; i < entries.size(); i++) entries.get(i).delay = 750 + i * 85L;
        lastFrame = Util.getMillis();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString().toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        CyberBackground.render(g, this.width, this.height, true, 1f);
    }

    // ------------------------------------------------------------ раскладка

    private void layout() {
        int colX = Math.max(26, (int) (this.width * 0.07f));
        int itemW = 214;
        int itemH = 24;
        int gap = 5;
        int bigCount = 5;
        int blockH = bigCount * (itemH + gap);
        int startY = Math.max(110, this.height / 2 - blockH / 2 + 28);
        for (int i = 0; i < bigCount; i++) {
            Entry e = entries.get(i);
            e.x = colX;
            e.y = startY + i * (itemH + gap);
            e.w = itemW;
            e.h = itemH;
        }
        int sy = startY + bigCount * (itemH + gap) + 8;
        for (int i = bigCount; i < entries.size(); i++) {
            Entry e = entries.get(i);
            e.x = colX + (i - bigCount) * 110;
            e.y = sy;
            e.w = 104;
            e.h = 16;
        }
    }

    // ------------------------------------------------------------- рендер

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = Util.getMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        long el = now - ScreenHooks.enterMs(this);

        renderBackground(g);
        CyberBackground.decorate(g, this.width, this.height, false);
        layout();

        int colX = entries.get(0).x;
        int logoY = Math.max(24, entries.get(0).y - 118);
        drawLogo(g, colX, logoY, el, now);
        drawSubtitle(g, colX, logoY + 50, el);

        int hovered = -1;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (el > e.delay && mx >= e.x && mx < e.x + e.w && my >= e.y && my < e.y + e.h) hovered = i;
        }
        if (hovered >= 0 && hovered != lastHover) {
            CyberSounds.play(CyberSounds.HOVER, 1f, 0.5f);
        }
        if (hovered >= 0) selected = hovered;
        lastHover = hovered;

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            boolean hot = i == selected;
            e.hover += ((hot ? 1f : 0f) - e.hover) * Math.min(1f, dt * 14f);
            drawEntry(g, e, i, el, now);
        }

        drawBootLog(g, el);
        drawFooter(g);
        if (quitMs != 0) drawQuit(g, now);
    }

    private void startQuit() {
        if (quitMs != 0) return;
        quitMs = Util.getMillis();
        quitting = true;
        CyberSounds.play(CyberSounds.SHUTDOWN, 1f, 0.8f);
    }

    /**
     * Выход: интерфейс гаснет, по центру печатается фраза с глитчем, затем экран схлопывается как ЭЛТ-монитор
     * в светящуюся линию, линия сжимается в точку, и только после этого игра закрывается.
     */
    private void drawQuit(GuiGraphics g, long now) {
        float q = (now - quitMs) / 1000f;
        int w = this.width, h = this.height;

        float dim = Fx.smooth(0f, 0.7f, q);
        float close = Fx.easeInOutCubic((q - 2.5f) / 0.6f);
        float collapse = Fx.easeInOutCubic((q - 3.1f) / 0.35f);
        float barH = h / 2f * close;

        Fx.begin(g);
        Fx.rect(0, 0, w, h, Fx.argb(0x000208, 0.9f * dim));
        if (close > 0f) {
            Fx.rect(0, 0, w, barH, 0xFF000000);
            Fx.rect(0, h - barH, w, h, 0xFF000000);
        }
        if (close >= 1f && collapse < 1f) {
            float half = w / 2f * (1f - collapse);
            float cy = h / 2f;
            Fx.rect(w / 2f - half, cy - 5f, w / 2f + half, cy + 5f, Fx.argb(0x3FD0FF, 0.18f));
            Fx.rect(w / 2f - half, cy - 1f, w / 2f + half, cy + 1f, Fx.argb(0xE8FBFF, 1f));
        }
        if (q > 3.45f) Fx.rect(0, 0, w, h, 0xFF000000);
        Fx.end();

        // печатающаяся фраза
        float fade = 1f - Fx.smooth(2.25f, 2.55f, q);
        if (fade > 0.01f) {
            float sc = Math.min(2.4f, w * 0.8f / this.font.width(QUIT_TEXT));
            int shown = (int) Math.min(QUIT_TEXT.length(), Math.max(0, (q - 0.8f) / 0.05f));
            String s = QUIT_TEXT.substring(0, shown) + (shown < QUIT_TEXT.length() || (int) (q * 3f) % 2 == 0 ? "_" : "");
            float x0 = w / 2f - this.font.width(QUIT_TEXT) * sc / 2f;
            float y0 = h / 2f - 4f * sc;
            boolean gl = q > 1.2f && Fx.hash(now / 70L) > 0.9f;
            float d = gl ? 3f * (Fx.hash(now / 70L + 5L) - 0.3f) * 2f : 1f;
            g.pose().pushPose();
            g.pose().translate(x0, y0, 0f);
            g.pose().scale(sc, sc, 1f);
            g.drawString(this.font, s, Math.round(-d), 0, Fx.argb(0xFF2E63, 0.45f * fade), false);
            g.drawString(this.font, s, Math.round(d), 0, Fx.argb(0x2EE6FF, 0.55f * fade), false);
            g.drawString(this.font, s, 0, 0, Fx.argb(0xEAF6FF, fade), false);
            g.pose().popPose();
        }

        if (q >= 3.55f && !quitDone) {
            quitDone = true;
            this.minecraft.stop();
        }
    }

    private void drawLogo(GuiGraphics g, int x, int y, long el, long now) {
        Font f = this.font;
        float scale = 5f;
        // раскладка по финальным символам, чтобы буквы не прыгали
        int[] adv = new int[LOGO.length()];
        int total = 0;
        for (int i = 0; i < LOGO.length(); i++) {
            adv[i] = f.width(String.valueOf(LOGO.charAt(i))) + 1;
            total += adv[i];
        }
        float totalW = total * scale;

        // периодический глитч: смещённые полосы
        float cyc = (now % 3700L) / 1000f;
        boolean glitch = cyc < 0.20f && el > 1500;
        int bands = glitch ? 4 : 1;
        float bandH = 8f * scale / bands;
        for (int b = 0; b < bands; b++) {
            float dx = glitch ? (Fx.hash(now / 40L * 13L + b) - 0.5f) * 26f : 0f;
            int sy = Math.round(y + b * bandH);
            int ey = Math.round(y + (b + 1) * bandH);
            if (glitch) g.enableScissor(0, sy, this.width, ey);
            float cx = x + dx;
            for (int i = 0; i < LOGO.length(); i++) {
                long reveal = 180 + i * 80L;
                char ch = LOGO.charAt(i);
                float a = Fx.clamp01((el - i * 45f) / 120f);
                if (a <= 0f) {
                    cx += adv[i] * scale;
                    continue;
                }
                boolean done = el >= reveal;
                String s;
                int rgb;
                if (done) {
                    s = String.valueOf(ch);
                    rgb = 0xEAF6FF;
                } else {
                    s = String.valueOf(SCRAMBLE.charAt((int) (Fx.hash(i * 31L + el / 45L) * SCRAMBLE.length())));
                    rgb = 0x2F8CFF;
                }
                float flash = done ? 1f - Fx.clamp01((el - reveal) / 260f) : 0f;
                g.pose().pushPose();
                g.pose().translate(cx, y, 0f);
                g.pose().scale(scale, scale, 1f);
                // хроматическая аберрация + свечение
                g.drawString(f, s, -1, 0, Fx.argb(0xFF2E63, 0.40f * a), false);
                g.drawString(f, s, 1, 0, Fx.argb(0x2EE6FF, 0.55f * a), false);
                g.drawString(f, s, 0, 0, Fx.argb(Fx.lerpRgb(rgb, 0xFFFFFF, flash), a), false);
                g.pose().popPose();
                cx += adv[i] * scale;
            }
            if (glitch) g.disableScissor();
        }

        // линия под логотипом: выдвигается слева направо
        float lp = Fx.easeOutCubic((el - 900f) / 600f);
        int lineW = Math.round(totalW * lp);
        int ly = Math.round(y + 8f * scale + 6f);
        g.fill(x, ly, x + lineW, ly + 1, Fx.argb(ACCENT, 0.9f));
        g.fill(x, ly + 1, x + lineW, ly + 3, Fx.argb(ACCENT, 0.12f));
        if (lp > 0f && lp < 1f) g.fill(x + lineW - 3, ly - 1, x + lineW, ly + 2, Fx.argb(0xFFFFFF, 0.95f));
    }

    private void drawSubtitle(GuiGraphics g, int x, int y, long el) {
        String full = ">_ VlaFFi corp.";
        int typed = (int) Math.min(full.length(), Math.max(0, (el - 1000) / 32));
        String s = full.substring(0, typed);
        boolean cursor = ((el / 450) % 2 == 0) || typed < full.length();
        g.pose().pushPose();
        g.pose().translate(x, y + 12, 0f);
        g.pose().scale(1.0f, 1.0f, 1f);
        g.drawString(this.font, s + (cursor ? "_" : ""), 0, 0, Fx.argb(0x6FD8FF, 0.95f), false);
        g.pose().popPose();
    }

    private void drawEntry(GuiGraphics g, Entry e, int idx, long el, long now) {
        float p = Fx.clamp01((el - e.delay) / 480f);
        if (p <= 0f) return;
        float ep = Fx.easeOutCubic(p);
        float A = ep;
        int x = Math.round(e.x - 60f * (1f - ep));
        float hv = Fx.easeOutCubic(e.hover);
        int y = e.y, w = e.w, h = e.h;

        // основание-линия
        g.fill(x, y + h - 1, x + w, y + h, Fx.argb(0x2F6BFF, 0.35f * A));
        // подсветка наведения
        if (hv > 0.01f) {
            int fillW = Math.round(w * hv);
            int steps = 14;
            for (int i = 0; i < steps; i++) {
                int x0 = x + fillW * i / steps;
                int x1 = x + fillW * (i + 1) / steps;
                g.fill(x0, y, x1, y + h - 1, Fx.argb(ACCENT, 0.30f * (1f - i / (float) steps) * A));
            }
            g.fill(x, y, x + 3, y + h - 1, Fx.argb(ACCENT, A));
            g.fill(x, y + h - 1, x + fillW, y + h, Fx.argb(ACCENT, 0.95f * A));
        } else {
            g.fill(x, y + 3, x + 2, y + h - 4, Fx.argb(0x2F6BFF, 0.55f * A));
        }
        // вспышка при нажатии
        float pf = 1f - (now - pressMs) / 220f;
        if (pf > 0f && pressIdx == idx) g.fill(x, y, x + w, y + h, Fx.argb(0xFFFFFF, 0.35f * pf * A));

        int tc = Fx.lerpRgb(0x9DB8F5, 0xFFFFFF, hv);
        if (e.small) {
            g.drawString(this.font, e.label, x + 8 + Math.round(hv * 3f), y + 4, Fx.argb(tc, A), false);
            return;
        }
        String num = String.format(Locale.ROOT, "%02d", idx + 1);
        g.drawString(this.font, num, x + 10, y + 8, Fx.argb(0x3F78D8, (0.55f + 0.45f * hv) * A), false);
        float tx = x + 34 + hv * 6f;
        g.pose().pushPose();
        g.pose().translate(tx, y + 4.5f, 0f);
        g.pose().scale(1.25f, 1.25f, 1f);
        g.drawString(this.font, e.label, 0, 0, Fx.argb(tc, A), false);
        g.pose().popPose();
        if (hv > 0.05f) g.drawString(this.font, ">", x + w - 16 - Math.round((1f - hv) * 8f), y + 8, Fx.argb(ACCENT, hv * A), false);
    }

    private void drawBootLog(GuiGraphics g, long el) {
        int mods = ModList.get().size();
        String[] lines = {
                "CORE.KERNEL ............ ONLINE",
                "RENDER.PIPELINE ........ READY",
                "NET.UPLINK ............. STANDBY",
                "MODS.LOADED ............ " + mods,
                "AWAITING USER INPUT"
        };
        int lh = 10;
        int baseY = this.height - 14 - lines.length * lh;
        int x = Math.max(26, (int) (this.width * 0.07f));
        for (int i = 0; i < lines.length; i++) {
            long t0 = 1700 + i * 260L;
            if (el < t0) continue;
            boolean last = i == lines.length - 1;
            boolean ok = el > t0 + 200 || last;
            String tag = last ? "[ >> ] " : ok ? "[ OK ] " : "[ .. ] ";
            int tagCol = last ? 0x3FE0FF : ok ? 0x4CFF9A : 0x5C7AA8;
            float a = Fx.clamp01((el - t0) / 150f) * 0.8f;
            g.pose().pushPose();
            g.pose().translate(x, baseY + i * lh, 0f);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.drawString(this.font, tag, 0, 0, Fx.argb(tagCol, a), false);
            String body = lines[i] + (last && (el / 400) % 2 == 0 ? "_" : "");
            g.drawString(this.font, body, this.font.width(tag), 0, Fx.argb(0x7FA6E8, a), false);
            g.pose().popPose();
        }
    }

    private void drawFooter(GuiGraphics g) {
        String ver = "OUTSIDE // MC " + SharedConstants.getCurrentVersion().getName() + " // FORGE";
        String copy = "Copyright Mojang AB. Do not distribute!";
        float sc = 0.75f;
        int c = Fx.argb(0x6C8CC8, 0.7f);
        g.pose().pushPose();
        g.pose().translate(this.width - 14 - this.font.width(ver) * sc, this.height - 24, 0f);
        g.pose().scale(sc, sc, 1f);
        g.drawString(this.font, ver, 0, 0, c, false);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(this.width - 14 - this.font.width(copy) * sc, this.height - 14, 0f);
        g.pose().scale(sc, sc, 1f);
        g.drawString(this.font, copy, 0, 0, Fx.argb(0x4F6A9C, 0.7f), false);
        g.pose().popPose();
    }

    // --------------------------------------------------------------- ввод

    private void activate(int i) {
        if (i < 0 || i >= entries.size()) return;
        pressMs = Util.getMillis();
        pressIdx = i;
        CyberSounds.play(CyberSounds.CLICK, 1f, 0.7f);
        entries.get(i).action.run();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (quitMs != 0) return true;
        if (button == 0) {
            long el = Util.getMillis() - ScreenHooks.enterMs(this);
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                if (el > e.delay && mx >= e.x && mx < e.x + e.w && my >= e.y && my < e.y + e.h) {
                    activate(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (quitMs != 0) return true;
        int n = entries.size();
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_TAB) {
            selected = (selected + 1 + n) % n;
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            selected = selected <= 0 ? n - 1 : selected - 1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            if (selected >= 0) {
                activate(selected);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }
}
