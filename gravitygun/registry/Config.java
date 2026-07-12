package com.zerokg2004.gravitygun.gravitygun.registry;

import com.zerokg2004.gravitygun.gravitygun.Gravitygun;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Gravitygun.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue CROSSHAIR_ENABLED = BUILDER
            .comment("Enable the custom Gravity Gun crosshair.")
            .define("crosshairEnabled", true);

    public static final ForgeConfigSpec.BooleanValue SIMPLE_MODEL_FAST_GRAPHICS = BUILDER
            .comment("Allow the simple Gravity Gun model when graphics are set to Fast.")
            .define("simpleModelFastGraphics", false);

    public static final ForgeConfigSpec.IntValue PUSH_FACTOR = BUILDER
            .comment("Base push factor used by the Gravity Gun launch formula.")
            .defineInRange("pushFactor", 10, 5, 100);

    public static final ForgeConfigSpec.IntValue SUPERCHARGED_PUSH_FACTOR = BUILDER
            .comment("Supercharged push amplification in percent.")
            .defineInRange("superchargedAmplifyFactorPush", 130, 100, 300);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean crosshairEnabled = true;
    public static boolean simpleModelFastGraphics = false;
    public static int pushFactor = 10;
    public static int superchargedPushFactor = 130;

    public static void setCrosshairEnabled(boolean enabled) {
        crosshairEnabled = enabled;
        CROSSHAIR_ENABLED.set(enabled);
    }

    public static void setSimpleModelFastGraphics(boolean enabled) {
        simpleModelFastGraphics = enabled;
        SIMPLE_MODEL_FAST_GRAPHICS.set(enabled);
    }

    public static void setPushFactor(int value) {
        pushFactor = value;
        PUSH_FACTOR.set(value);
    }

    public static void setSuperchargedPushFactor(int value) {
        superchargedPushFactor = value;
        SUPERCHARGED_PUSH_FACTOR.set(value);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        crosshairEnabled = CROSSHAIR_ENABLED.get();
        simpleModelFastGraphics = SIMPLE_MODEL_FAST_GRAPHICS.get();
        pushFactor = PUSH_FACTOR.get();
        superchargedPushFactor = SUPERCHARGED_PUSH_FACTOR.get();
    }
}
