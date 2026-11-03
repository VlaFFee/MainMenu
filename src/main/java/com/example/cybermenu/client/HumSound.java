package com.example.cybermenu.client;

import com.example.cybermenu.CyberMenuMod;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Зацикленный компьютерный гул: плавно нарастает при входе в меню и затихает при выходе. */
public final class HumSound extends AbstractTickableSoundInstance {
    HumSound() {
        super(SoundEvent.createVariableRangeEvent(new ResourceLocation(CyberMenuMod.MODID, "ui.hum")),
                SoundSource.MASTER, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        float target = HumPlayer.target();
        this.volume += (target - this.volume) * 0.07f;
        if (target <= 0.001f && this.volume < 0.01f) stop();
    }
}
