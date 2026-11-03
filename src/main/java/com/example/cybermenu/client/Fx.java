package com.example.cybermenu.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Общие помощники: плавность, шум, цвета и пакетная отрисовка цветных прямоугольников одним вызовом. */
public final class Fx {
    private static BufferBuilder bb;
    private static Matrix4f mat;

    private Fx() {}

    // ----------------------------------------------------------- математика

    public static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float smooth(float e0, float e1, float x) {
        float t = clamp01((x - e0) / (e1 - e0));
        return t * t * (3f - 2f * t);
    }

    public static float easeOutCubic(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    public static float easeInOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    public static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    /** Детерминированный шум 0..1 по целому числу. */
    public static float hash(long n) {
        long x = n * 0x9E3779B97F4A7C15L;
        x ^= x >>> 29;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 32;
        return (x & 0xFFFFFF) / (float) 0x1000000;
    }

    // ---------------------------------------------------------------- цвет

    public static int argb(int rgb, float a) {
        int al = (int) (clamp01(a) * 255f);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerpRgb(int a, int b, float t) {
        t = clamp01(t);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------------- мышь

    /** Позиция курсора в координатах интерфейса. */
    public static float mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return (float) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth());
    }

    public static float mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return (float) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
    }

    // -------------------------------------------- пакетная отрисовка прямоугольников

    /** Начинает пакет цветных прямоугольников: сотни квадов уйдут в видеокарту одним вызовом. */
    public static void begin(GuiGraphics g) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        mat = g.pose().last().pose();
    }

    public static void end() {
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableCull();
    }

    private static void v(float x, float y, int argb) {
        bb.vertex(mat, x, y, 0f).color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24).endVertex();
    }

    public static void rect(float x0, float y0, float x1, float y1, int argb) {
        if ((argb >>> 24) < 2) return;
        v(x0, y0, argb);
        v(x0, y1, argb);
        v(x1, y1, argb);
        v(x1, y0, argb);
    }

    /** Горизонтальный градиент: слева c0, справа c1. */
    public static void rectH(float x0, float y0, float x1, float y1, int c0, int c1) {
        v(x0, y0, c0);
        v(x0, y1, c0);
        v(x1, y1, c1);
        v(x1, y0, c1);
    }

    /** Вертикальный градиент: сверху c0, снизу c1. */
    public static void rectV(float x0, float y0, float x1, float y1, int c0, int c1) {
        v(x0, y0, c0);
        v(x0, y1, c1);
        v(x1, y1, c1);
        v(x1, y0, c0);
    }
}
