package com.example.cybermenu;

import net.minecraftforge.common.ForgeConfigSpec;

/** Настройки клиента: config/cybermenu-client.toml. */
public final class CyberConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue REPLACE_TITLE;
    public static final ForgeConfigSpec.BooleanValue THEME_SCREENS;
    public static final ForgeConfigSpec.BooleanValue SKIN_WIDGETS;
    public static final ForgeConfigSpec.BooleanValue TRANSITIONS;
    public static final ForgeConfigSpec.BooleanValue IN_WORLD;
    public static final ForgeConfigSpec.BooleanValue MODDED_SCREENS;
    public static final ForgeConfigSpec.DoubleValue DENSITY;
    public static final ForgeConfigSpec.DoubleValue SPEED;
    public static final ForgeConfigSpec.DoubleValue HUM_VOLUME;
    public static final ForgeConfigSpec.BooleanValue DIGITS;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        REPLACE_TITLE = b.comment("Заменить главное меню на кибер-терминал").define("replaceTitle", true);
        THEME_SCREENS = b.comment("Живой фон на всех меню и настройках").define("themeScreens", true);
        SKIN_WIDGETS = b.comment("Кибер-скин для кнопок и ползунков").define("skinWidgets", true);
        TRANSITIONS = b.comment("Анимация перехода между экранами").define("transitions", true);
        IN_WORLD = b.comment("Стилизовать меню поверх мира (пауза, настройки в игре)").define("inWorld", true);
        MODDED_SCREENS = b.comment("Стилизовать и экраны других модов (может ломать их внешний вид)").define("moddedScreens", false);
        DENSITY = b.comment("Плотность точек фона. Меньше значение — выше FPS").defineInRange("density", 1.0, 0.25, 2.0);
        SPEED = b.comment("Скорость анимации фона").defineInRange("speed", 1.0, 0.2, 3.0);
        HUM_VOLUME = b.comment("Громкость компьютерного гула в меню. 0 — выключить").defineInRange("humVolume", 0.35, 0.0, 1.0);
        DIGITS = b.comment("Мелькающие цифры на фоне").define("digits", true);
        SOUND_VOLUME = b.comment("Громкость звуков интерфейса (наведение, клик, переходы). 0 — выключить").defineInRange("soundVolume", 0.6, 0.0, 1.0);
        SPEC = b.build();
    }

    private CyberConfig() {}
}
