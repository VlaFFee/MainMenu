package com.example.cybermenu.client;

import com.example.cybermenu.CyberConfig;
import com.example.cybermenu.CyberMenuMod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Подключение к событиям экранов: замена главного меню, фон, скин кнопок, анимации появления и переходы. */
@Mod.EventBusSubscriber(modid = CyberMenuMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ScreenHooks {
    /** Координата-метка: виджет списка, который в этом кадре не рисовался, остаётся на ней. */
    private static final int SENTINEL = -20000;
    private static final long ITEM_DELAY_MS = 32;
    private static final long ITEM_DUR_MS = 380;

    private static final Map<Screen, Long> ENTER = new WeakHashMap<>();
    private static final List<Rec> RECS = new ArrayList<>();

    private static final class Rec {
        final AbstractWidget w;
        final boolean nested;
        final int origY;
        final boolean origVisible;
        final Component origMsg;
        int index;
        boolean hidden;

        Rec(AbstractWidget w, boolean nested) {
            this.w = w;
            this.nested = nested;
            this.origY = w.getY();
            this.origVisible = w.visible;
            this.origMsg = w.getMessage();
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> REPORTED = new HashSet<>();
    /** После первой ошибки в отрисовке мод отключает себя, чтобы не ронять игру и не засорять лог. */
    private static boolean disabled;

    private ScreenHooks() {}

    static void fail(String where, Throwable t) {
        if (REPORTED.add(where)) {
            LOGGER.error("[CyberMenu] ошибка в '{}', оформление отключено до перезапуска. Пришлите этот лог автору.", where, t);
        }
        if (!where.equals("hum")) disabled = true;
    }

    /** Когда экран открыли в последний раз (мс). */
    public static long enterMs(Screen s) {
        Long v = ENTER.get(s);
        return v == null ? Util.getMillis() : v;
    }

    // ------------------------------------------------------------- фильтры

    static boolean isThemed(Screen s) {
        if (s == null) return false;
        if (s instanceof CyberTitleScreen) return true;
        if (s instanceof ChatScreen || s instanceof AbstractContainerScreen<?>) return false;
        String n = s.getClass().getName();
        // экраны оптимизаторов (Embeddium/Sodium и дополнения) рисуют свои виджеты, их не трогаем никогда
        if (n.startsWith("org.embeddedt.") || n.startsWith("me.jellysquid.") || n.startsWith("me.flashyreese.")
                || n.startsWith("net.caffeinemc.") || n.startsWith("org.vivecraft.")) return false;
        if (n.startsWith("net.minecraft.client.gui.screens.inventory")
                || n.startsWith("net.minecraft.client.gui.screens.advancements")
                || n.startsWith("net.minecraft.client.gui.screens.recipebook")) return false;
        if (n.startsWith("net.minecraft.client.gui.screens.")
                || n.startsWith("net.minecraftforge.client.gui.")
                || n.startsWith("net.minecraftforge.fml.")
                || n.startsWith("com.mojang.realmsclient")) return true;
        return CyberConfig.MODDED_SCREENS.get();
    }

    private static boolean skinnable(AbstractWidget w) {
        if (w instanceof ImageButton || w instanceof PlainTextButton) return false;
        return w instanceof Button || w instanceof CycleButton<?> || w instanceof AbstractSliderButton;
    }

    private static void collect(GuiEventListener l, boolean nested, List<Rec> out, int depth) {
        if (depth > 6) return;
        if (l instanceof AbstractWidget w && skinnable(w)) {
            out.add(new Rec(w, nested));
            return;
        }
        if (l instanceof ContainerEventHandler c) {
            boolean n = nested || l instanceof AbstractSelectionList<?>;
            for (GuiEventListener ch : c.children()) collect(ch, n, out, depth + 1);
        }
    }

    private static void restore() {
        for (Rec r : RECS) {
            if (r.w.getY() == SENTINEL) r.w.setY(r.origY);
            r.w.visible = r.origVisible;
            if (!(r.w instanceof AbstractSliderButton)) r.w.setMessage(r.origMsg);
        }
        RECS.clear();
    }

    // ------------------------------------------------------------- события

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOpening(ScreenEvent.Opening e) {
        if (disabled) return;
        try {
            Screen n = e.getNewScreen();
            if (n == null) return;
            if (CyberConfig.REPLACE_TITLE.get() && n.getClass() == TitleScreen.class) {
                n = new CyberTitleScreen();
                e.setNewScreen(n);
            }
            ENTER.put(n, Util.getMillis());
            if (CyberConfig.TRANSITIONS.get() && isThemed(n)) {
                CyberSounds.play(CyberSounds.TRANSITION, 0.95f + Fx.hash(Util.getMillis()) * 0.1f, 0.5f);
            }
        } catch (Throwable t) {
            fail("opening", t);
        }
    }

    @SubscribeEvent
    public static void onBackground(ScreenEvent.BackgroundRendered e) {
        if (disabled) return;
        Screen s = e.getScreen();
        if (s instanceof CyberTitleScreen || !CyberConfig.THEME_SCREENS.get() || !isThemed(s)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean inWorld = mc.level != null;
        if (inWorld && !CyberConfig.IN_WORLD.get()) return;
        GuiGraphics g = e.getGuiGraphics();
        try {
            CyberBackground.render(g, s.width, s.height, !inWorld, inWorld ? 0.5f : 1f);
            CyberBackground.decorate(g, s.width, s.height, true);
        } catch (Throwable t) {
            fail("background", t);
        }
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        Skin.registerClick(e.getMouseX(), e.getMouseY());
    }

    @SubscribeEvent
    public static void onRenderPre(ScreenEvent.Render.Pre e) {
        restore();
        if (disabled) return;
        Screen s = e.getScreen();
        if (!CyberConfig.SKIN_WIDGETS.get() || !isThemed(s)) return;
        try {
            prepare(s);
        } catch (Throwable t) {
            restore();
            fail("render-pre", t);
        }
    }

    private static void prepare(Screen s) {
        List<Rec> found = new ArrayList<>();
        for (GuiEventListener l : s.children()) collect(l, false, found, 0);
        long el = Util.getMillis() - enterMs(s);

        List<Rec> top = new ArrayList<>();
        for (Rec r : found) if (!r.nested) top.add(r);
        top.sort(Comparator.<Rec>comparingInt(r -> r.w.getY()).thenComparingInt(r -> r.w.getX()));
        for (int i = 0; i < top.size(); i++) top.get(i).index = i;

        RECS.addAll(found);
        for (Rec r : RECS) {
            // Ванильный текст убираем на время кадра: при пакетной отрисовке (ImmediatelyFast, Embeddium)
            // текст выводится поверх любых заливок, и перекрыть его подложкой нельзя. Свою подпись рисует Skin.
            if (!(r.w instanceof AbstractSliderButton)) r.w.setMessage(Component.empty());
            if (r.nested) {
                r.w.setY(SENTINEL);
            } else if (r.origVisible && !(r.w instanceof AbstractSliderButton)
                    && CyberConfig.TRANSITIONS.get() && el < r.index * ITEM_DELAY_MS + ITEM_DUR_MS) {
                // пока кнопка «въезжает», настоящую прячем, а сверху рисуем анимированную копию
                r.w.visible = false;
                r.hidden = true;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post e) {
        if (disabled) {
            restore();
            return;
        }
        try {
            post(e);
        } catch (Throwable t) {
            restore();
            fail("render-post", t);
        }
    }

    private static void post(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        GuiGraphics g = e.getGuiGraphics();
        long now = Util.getMillis();
        int mx = e.getMouseX(), my = e.getMouseY();

        try {
            if (CyberConfig.SKIN_WIDGETS.get() && isThemed(s)) {
                long el = now - enterMs(s);
                boolean clipped = false;
                for (Rec r : RECS) {
                    AbstractWidget w = r.w;
                    if (r.nested) {
                        if (w.getY() == SENTINEL) continue;      // эта строка списка сейчас не рисовалась
                        if (!r.origVisible || w.getWidth() <= 0) continue;
                        if (!clipped) {
                            g.enableScissor(0, 32, s.width, s.height - 32);
                            clipped = true;
                        }
                        drawOne(g, w, r.origMsg, 1f, 0f, mx, my, now);
                    }
                }
                if (clipped) g.disableScissor();

                for (Rec r : RECS) {
                    if (r.nested || !r.origVisible || r.w.getWidth() <= 0) continue;
                    float a = 1f, xo = 0f;
                    if (r.hidden) {
                        float p = Fx.clamp01((el - r.index * ITEM_DELAY_MS) / (float) ITEM_DUR_MS);
                        float ep = Fx.easeOutCubic(p);
                        a = ep;
                        xo = -34f * (1f - ep);
                    }
                    drawOne(g, r.w, r.origMsg, a, xo, mx, my, now);
                }
            }
        } finally {
            restore();
        }

        if (CyberConfig.TRANSITIONS.get() && isThemed(s)) {
            float t = (now - enterMs(s)) / (float) Transition.DURATION_MS;
            Transition.draw(g, s.width, s.height, t, enterMs(s));
        }
    }

    private static void drawOne(GuiGraphics g, AbstractWidget w, Component msg, float a, float xo, int mx, int my, long now) {
        if (w instanceof AbstractSliderButton) Skin.slider(g, w, a, mx, my, now);
        else Skin.button(g, w, msg, a, xo, mx, my, now);
    }
}
