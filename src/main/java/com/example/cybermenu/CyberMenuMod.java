package com.example.cybermenu;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/** CyberMenu: клиентский мод, полностью меняющий внешний вид меню. Весь код отрисовки живёт в пакете client. */
@Mod(CyberMenuMod.MODID)
public final class CyberMenuMod {
    public static final String MODID = "cybermenu";

    public CyberMenuMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CyberConfig.SPEC);
    }
}
