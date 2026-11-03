package com.example.cybermenu.client;

import com.example.cybermenu.CyberConfig;
import com.example.cybermenu.CyberMenuMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Включает гул, пока открыто стилизованное меню без мира (главное меню, настройки, выбор мира). */
@Mod.EventBusSubscriber(modid = CyberMenuMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HumPlayer {
    private static HumSound inst;
    private static boolean wanted;

    private HumPlayer() {}

    static float target() {
        return wanted ? (float) CyberConfig.HUM_VOLUME.get().doubleValue() : 0f;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            wanted = !CyberTitleScreen.quitting && mc.level == null && mc.screen != null && CyberConfig.THEME_SCREENS.get()
                    && CyberConfig.HUM_VOLUME.get() > 0.001 && ScreenHooks.isThemed(mc.screen);
            if (wanted && (inst == null || inst.isStopped())) {
                inst = new HumSound();
                mc.getSoundManager().play(inst);
            }
            if (inst != null && inst.isStopped()) inst = null;
        } catch (Throwable t) {
            ScreenHooks.fail("hum", t);
        }
    }
}
