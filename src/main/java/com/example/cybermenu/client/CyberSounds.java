package com.example.cybermenu.client;

import com.example.cybermenu.CyberConfig;
import com.example.cybermenu.CyberMenuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** Компьютерные звуки интерфейса. События объявлены в sounds.json и нигде не регистрируются в реестре. */
public final class CyberSounds {
    public static final SoundEvent HOVER = ev("ui.hover");
    public static final SoundEvent CLICK = ev("ui.click");
    public static final SoundEvent TRANSITION = ev("ui.transition");
    public static final SoundEvent SHUTDOWN = ev("ui.shutdown");

    private CyberSounds() {}

    private static SoundEvent ev(String path) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(CyberMenuMod.MODID, path));
    }

    /** Играет звук интерфейса; общая громкость берётся из конфига. */
    public static void play(SoundEvent e, float pitch, float volume) {
        float v = volume * (float) CyberConfig.SOUND_VOLUME.get().doubleValue();
        if (v <= 0.001f) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, Math.min(1f, v)));
    }
}
