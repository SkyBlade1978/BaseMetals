package com.mcmoddev.basemetals.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BaseMetalsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SPECIAL_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue STARSTEEL_REGENERATION;
    public static final ForgeConfigSpec.BooleanValue MERCURY_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue VILLAGER_TRADES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Base Metals gameplay options. Registries, recipes, and world generation are data-driven and are never config-gated.");
        SPECIAL_EFFECTS = builder.comment("Enable special material armor and melee effects.")
                .define("specialEffects", true);
        STARSTEEL_REGENERATION = builder.comment("Repair held Starsteel equipment once every 200 server ticks.")
                .define("starsteelRegeneration", true);
        MERCURY_EFFECTS = builder.comment("Enable the nausea chance while touching molten mercury.")
                .define("mercuryImmersionEffects", true);
        VILLAGER_TRADES = builder.comment("Add Base Metals smithing trades.")
                .define("villagerTrades", true);
        SPEC = builder.build();
    }

    private BaseMetalsConfig() {}
}
