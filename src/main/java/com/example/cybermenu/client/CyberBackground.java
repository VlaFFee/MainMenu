package com.example.cybermenu.client;

import com.example.cybermenu.CyberConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Живой фон: бесконечный туннель из светящихся синих точек. Точки лежат на кольцах, которые летят к краям экрана,
 * закручиваются по спирали, растягиваются в штрихи и мерцают. Время общее для всех экранов, поэтому при переходах
 * между меню фон не перезапускается, а продолжает течь.
 */
public final class CyberBackground {
    private static final long T0 = Util.getMillis();
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static ResourceLocation dotTex;
    private static float parX, parY;
    private static long lastMs = T0;

    private static float[] cosT = new float[0];
    private static float[] sinT = new float[0];

    private CyberBackground() {}

    /** Секунды с запуска с учётом настройки скорости. */
    public static float time() {
        return (Util.getMillis() - T0) / 1000f * (float) CyberConfig.SPEED.get().doubleValue();
    }

    private static void ensureTexture() {
        if (dotTex != null) return;
        int n = 32;
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, n, n, false);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float dx = (x + 0.5f - n / 2f) / (n / 2f);
                float dy = (y + 0.5f - n / 2f) / (n / 2f);
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Fx.clamp01(1f - d);
                a = (float) Math.pow(a, 1.35);
                int al = (int) (a * 255f);
                img.setPixelRGBA(x, y, (al << 24) | 0x00FFFFFF);
            }
        }
        DynamicTexture tex = new DynamicTexture(img);
        tex.setFilter(true, false);
        dotTex = Minecraft.getInstance().getTextureManager().register("cybermenu_dot", tex);
    }

    private static void ensureTables(int per) {
        if (cosT.length == per) return;
        cosT = new float[per];
        sinT = new float[per];
        for (int j = 0; j < per; j++) {
            double a = j * Math.PI * 2.0 / per;
            cosT[j] = (float) Math.cos(a);
            sinT[j] = (float) Math.sin(a);
        }
    }

    /**
     * Рисует фон.
     *
     * @param opaque    true: полностью закрыть экран (меню без мира); false: полупрозрачно поверх мира
     * @param intensity общая яркость точек, 0..1
     */
    public static void render(GuiGraphics g, int w, int h, boolean opaque, float intensity) {
        long now = Util.getMillis();
        float t = time();
        float dt = Math.min(0.1f, (now - lastMs) / 1000f);
        lastMs = now;

        // мягкое следование центра туннеля за мышью
        float tx = (Fx.mouseX() / w - 0.5f) * 2f;
        float ty = (Fx.mouseY() / h - 0.5f) * 2f;
        float k = Math.min(1f, dt * 2.5f);
        parX += (tx - parX) * k;
        parY += (ty - parY) * k;

        if (opaque) {
            g.fillGradient(0, 0, w, h, 0xFF01030F, 0xFF020A2A);
        } else {
            g.fill(0, 0, w, h, Fx.argb(0x020718, 0.64f));
        }

        g.flush();
        ensureTexture();
        drawDots(g, w, h, t, intensity);
        drawDigits(g, w, h, t, intensity);
        drawOverlay(g, w, h, t, opaque);
    }

    private static void drawDots(GuiGraphics g, int w, int h, float t, float intensity) {
        float dens = (float) CyberConfig.DENSITY.get().doubleValue();
        int rings = Math.max(18, Math.min(120, Math.round(54 * dens)));
        int per = Math.max(36, Math.min(240, Math.round(120 * dens)));
        ensureTables(per);

        float cx = w * 0.5f + parX * w * 0.035f;
        float cy = h * 0.5f + parY * h * 0.035f;
        float rMin = Math.min(w, h) * 0.012f;
        float rMax = (float) Math.hypot(w, h) * 0.56f;
        float logRatio = (float) Math.log(rMax / rMin);
        float flow = t * 0.055f;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, dotTex);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f m = g.pose().last().pose();

        // свечение в центре туннеля
        float pulse = 0.85f + 0.15f * (float) Math.sin(t * 1.3f);
        soft(bb, m, cx, cy, h * 0.62f, 0.10f, 0.28f, 1.00f, 0.26f * pulse * intensity);
        soft(bb, m, cx, cy, h * 0.22f, 0.25f, 0.65f, 1.00f, 0.22f * pulse * intensity);
        soft(bb, m, cx, cy, h * 0.06f, 0.80f, 0.95f, 1.00f, 0.30f * intensity);

        for (int i = 0; i < rings; i++) {
            float u = Fx.frac(i / (float) rings + flow);
            float fade = Fx.smooth(0f, 0.10f, u) * (1f - Fx.smooth(0.93f, 1f, u));
            if (fade < 0.01f) continue;
            float r = rMin * (float) Math.exp(logRatio * u);
            float twist = i * 0.61f + t * 0.06f + u * 1.1f;
            float ct = (float) Math.cos(twist), st = (float) Math.sin(twist);
            float size = Math.min(9f, 1.3f + r * 0.026f);
            float elong = 1f + 2.4f * u * u * u;
            float wave = (float) Math.pow(Math.max(0f, Math.sin(u * 30f - t * 2.2f)), 8) * 0.9f;
            float hl = size * elong * 1.15f;
            float hw = size * 1.15f;
            for (int j = 0; j < per; j++) {
                long key = i * 977L + j * 131L;
                float h1 = Fx.hash(key);
                if (h1 < 0.10f) continue;
                float dx = cosT[j] * ct - sinT[j] * st;
                float dy = sinT[j] * ct + cosT[j] * st;
                float x = cx + dx * r;
                float y = cy + dy * r;
                if (x < -24f || x > w + 24f || y < -24f || y > h + 24f) continue;
                float h2 = Fx.hash(key + 7L);
                float flick = 0.55f + 0.45f * (float) Math.sin(t * (1.2f + h1 * 2.6f) + h1 * 40f);
                float b = (0.30f + 0.70f * h2) * flick + wave;
                float a = Fx.clamp01(b * fade * intensity * 0.9f);
                if (a < 0.02f) continue;
                float cr = 0.10f, cg = 0.38f, cb = 1.00f;
                float bright = Fx.clamp01((b - 0.8f) * 2f);
                cr = Fx.lerp(cr, 0.45f, bright);
                cg = Fx.lerp(cg, 0.90f, bright);
                if (h2 > 0.985f) {
                    cr = 0.25f;
                    cg = 1.0f;
                    cb = 0.75f;
                    a = Fx.clamp01(a * 1.4f);
                }
                float ex = dx * hl, ey = dy * hl, fx = -dy * hw, fy = dx * hw;
                bb.vertex(m, x - ex - fx, y - ey - fy, 0f).uv(0f, 0f).color(cr, cg, cb, a).endVertex();
                bb.vertex(m, x + ex - fx, y + ey - fy, 0f).uv(1f, 0f).color(cr, cg, cb, a).endVertex();
                bb.vertex(m, x + ex + fx, y + ey + fy, 0f).uv(1f, 1f).color(cr, cg, cb, a).endVertex();
                bb.vertex(m, x - ex + fx, y - ey + fy, 0f).uv(0f, 1f).color(cr, cg, cb, a).endVertex();
            }
        }
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    private static void soft(BufferBuilder bb, Matrix4f m, float cx, float cy, float r, float cr, float cg, float cb, float a) {
        bb.vertex(m, cx - r, cy - r, 0f).uv(0f, 0f).color(cr, cg, cb, a).endVertex();
        bb.vertex(m, cx + r, cy - r, 0f).uv(1f, 0f).color(cr, cg, cb, a).endVertex();
        bb.vertex(m, cx + r, cy + r, 0f).uv(1f, 1f).color(cr, cg, cb, a).endVertex();
        bb.vertex(m, cx - r, cy + r, 0f).uv(0f, 1f).color(cr, cg, cb, a).endVertex();
    }


    private static final String HEX = "0123456789ABCDEF";

    private static char hexChar(long key) {
        return HEX.charAt((int) (Fx.hash(key) * 16f) & 15);
    }

    /** Мелькающие цифры: дождь из символов по колонкам и всплывающие числа (hex, двоичные, десятичные). */
    private static void drawDigits(GuiGraphics g, int w, int h, float t, float intensity) {
        if (!CyberConfig.DIGITS.get()) return;
        Font f = Minecraft.getInstance().font;
        g.drawManaged(() -> {
            // дождь
            int cols = Math.max(6, w / 90);
            int trail = 9;
            for (int c = 0; c < cols; c++) {
                float x = (Fx.hash(c * 31L + 5L) * 0.96f + 0.02f) * w;
                float speed = 45f + Fx.hash(c * 17L + 1L) * 110f;
                float period = h + trail * 9f + 80f;
                float head = ((t * speed + Fx.hash(c * 53L) * period) % period) - 40f;
                for (int k = 0; k < trail; k++) {
                    float y = head - k * 9f;
                    if (y < -9f || y > h) continue;
                    float a = 1f - k / (float) trail;
                    a *= a;
                    long bucket = (long) (t * (3f + (k % 3)));
                    String s = String.valueOf(hexChar(c * 911L + k * 37L + bucket * 7919L));
                    int col = k == 0 ? 0xBFEFFF : 0x2F8CFF;
                    g.pose().pushPose();
                    g.pose().translate(x, y, 0f);
                    g.pose().scale(0.75f, 0.75f, 1f);
                    g.drawString(f, s, 0, 0, Fx.argb(col, a * 0.55f * intensity), false);
                    g.pose().popPose();
                }
            }
            // всплывающие числа
            int clusters = 26;
            for (int i = 0; i < clusters; i++) {
                float life = 3.2f + Fx.hash(i * 13L) * 3.5f;
                float tt = t + Fx.hash(i * 29L) * life;
                long epoch = (long) Math.floor(tt / life);
                float ph = tt / life - epoch;
                float env = Fx.smooth(0f, 0.12f, ph) * (1f - Fx.smooth(0.7f, 1f, ph));
                if (env < 0.02f) continue;
                float flick = Fx.hash(i * 7L + (long) (t * 14f)) > 0.86f ? 0.25f : 1f;
                float x = Fx.hash(i * 101L + epoch * 977L) * (w - 100) + 10f;
                float y = Fx.hash(i * 131L + epoch * 313L) * (h - 24) + 10f;
                long bucket = (long) (t * 7f);
                int type = (int) (Fx.hash(i * 3L + epoch * 11L) * 3f);
                StringBuilder sb = new StringBuilder();
                if (type == 0) {
                    sb.append("0x");
                    for (int k = 0; k < 4; k++) sb.append(hexChar(i * 53L + k * 17L + bucket * 31L));
                } else if (type == 1) {
                    for (int k = 0; k < 8; k++) sb.append(Fx.hash(i * 61L + k * 19L + bucket * 29L) > 0.5f ? '1' : '0');
                } else {
                    for (int k = 0; k < 6; k++) {
                        if (k == 4) sb.append('.');
                        sb.append((int) (Fx.hash(i * 67L + k * 23L + bucket * 37L) * 10f) % 10);
                    }
                }
                float sc = 0.8f + Fx.hash(i * 5L) * 0.3f;
                g.pose().pushPose();
                g.pose().translate(x, y, 0f);
                g.pose().scale(sc, sc, 1f);
                g.drawString(f, sb.toString(), 0, 0, Fx.argb(0x4FB4FF, env * 0.5f * flick * intensity), false);
                g.pose().popPose();
            }
        });
    }

    /** Строка из групп цифр, которые по очереди меняются: заменяет текстовую подпись в углу. */
    private static String digitLabel() {
        long now = Util.getMillis();
        StringBuilder sb = new StringBuilder();
        for (int grp = 0; grp < 3; grp++) {
            if (grp > 0) sb.append(' ');
            long bucket = now / (140L + grp * 90L);
            for (int k = 0; k < 4; k++) sb.append((int) (Fx.hash(grp * 1000L + k * 17L + bucket * 7L) * 10f) % 10);
        }
        return sb.toString();
    }

    /** Виньетка, строчная развёртка и бегущая светлая полоса. */
    private static void drawOverlay(GuiGraphics g, int w, int h, float t, boolean opaque) {
        Fx.begin(g);
        int steps = 10;
        float depth = Math.min(w, h) * 0.34f;
        int c = Fx.argb(0x000008, 0.10f);
        for (int i = 0; i < steps; i++) {
            float th = depth * (i + 1) / steps;
            Fx.rect(0, 0, w, th, c);
            Fx.rect(0, h - th, w, h, c);
            Fx.rect(0, th, th, h - th, c);
            Fx.rect(w - th, th, w, h - th, c);
        }
        int line = Fx.argb(0x000000, opaque ? 0.14f : 0.10f);
        for (int y = 0; y < h; y += 3) Fx.rect(0, y, w, y + 1, line);
        float by = ((t * 0.11f) % 1.4f - 0.2f) * h;
        Fx.rectV(0, by - 40, w, by, Fx.argb(0x3FA9FF, 0f), Fx.argb(0x3FA9FF, 0.05f));
        Fx.rectV(0, by, w, by + 40, Fx.argb(0x3FA9FF, 0.05f), Fx.argb(0x3FA9FF, 0f));
        Fx.end();
    }

    /** Рамки по углам и системные подписи. Рисуется поверх фона на всех экранах, кроме главного. */
    public static void decorate(GuiGraphics g, int w, int h, boolean labels) {
        float t = time();
        float a = 0.45f + 0.15f * (float) Math.sin(t * 2f);
        int col = Fx.argb(0x3FD0FF, a);
        int m = 8, len = 22;
        Fx.begin(g);
        // верхний левый
        Fx.rect(m, m, m + len, m + 1, col);
        Fx.rect(m, m, m + 1, m + len, col);
        // верхний правый
        Fx.rect(w - m - len, m, w - m, m + 1, col);
        Fx.rect(w - m - 1, m, w - m, m + len, col);
        // нижний левый
        Fx.rect(m, h - m - 1, m + len, h - m, col);
        Fx.rect(m, h - m - len, m + 1, h - m, col);
        // нижний правый
        Fx.rect(w - m - len, h - m - 1, w - m, h - m, col);
        Fx.rect(w - m - 1, h - m - len, w - m, h - m, col);
        Fx.end();

        if (!labels) return;
        Font f = Minecraft.getInstance().font;
        String left = digitLabel();
        String right = LocalTime.now().format(CLOCK);
        int tc = Fx.argb(0x5CC8FF, 0.55f);
        g.pose().pushPose();
        g.pose().translate(m + 4, m + 5, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(f, left, 0, 0, tc, false);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(w - m - 4 - f.width(right) * 0.75f, m + 5, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(f, right, 0, 0, tc, false);
        g.pose().popPose();
    }
}
