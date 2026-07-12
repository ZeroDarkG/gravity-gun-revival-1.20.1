package com.zerokg2004.gravitygun.gravitygun.registry;

import com.zerokg2004.gravitygun.gravitygun.Gravitygun;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SoundEventsRegistry {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Gravitygun.MODID);

    public static final RegistryObject<SoundEvent> AMMO_PICKUP =
            register("ammo_pickup");

    public static final RegistryObject<SoundEvent> PHYSCANNON_DROP =
            register("physcannon_drop");

    public static final RegistryObject<SoundEvent> PHYSCANNON_PICKUP =
            register("physcannon_pickup");

    public static final RegistryObject<SoundEvent> HOLD_LOOP =
            register("hold_loop");

    public static final RegistryObject<SoundEvent> SUPERPHYS_HOLD_LOOP =
            register("superphys_hold_loop");

    public static final RegistryObject<SoundEvent> PHYSCANNON_LAUNCH1 =
            register("physcannon_launch1");

    public static final RegistryObject<SoundEvent> PHYSCANNON_LAUNCH2 =
            register("physcannon_launch2");

    public static final RegistryObject<SoundEvent> SUPERPHYS_LAUNCH1 =
            register("superphys_launch1");

    public static final RegistryObject<SoundEvent> SUPERPHYS_LAUNCH2 =
            register("superphys_launch2");

    public static final RegistryObject<SoundEvent> CLAWS_OPEN =
            SOUND_EVENTS.register("physcannon_claws_open",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Gravitygun.MODID, "physcannon_claws_open")
                    ));

    public static final RegistryObject<SoundEvent> CLAWS_CLOSE =
            SOUND_EVENTS.register("physcannon_claws_close",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Gravitygun.MODID, "physcannon_claws_close")
                    ));

    public static final RegistryObject<SoundEvent> PHYSCANNON_CHARGE =
            SOUND_EVENTS.register("physcannon_charge",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Gravitygun.MODID, "physcannon_charge")
                    ));

    // ✅ faltaban (zaps + dryfire + tooheavy) - exactos al JAR
    public static final RegistryObject<SoundEvent> SUPERPHYS_ZAP1 =
            register("superphys_small_zap1");
    public static final RegistryObject<SoundEvent> SUPERPHYS_ZAP2 =
            register("superphys_small_zap2");
    public static final RegistryObject<SoundEvent> SUPERPHYS_ZAP3 =
            register("superphys_small_zap3");
    public static final RegistryObject<SoundEvent> SUPERPHYS_ZAP4 =
            register("superphys_small_zap4");

    public static final RegistryObject<SoundEvent> PHYSCANNON_DRYFIRE =
            register("physcannon.dryfire");

    public static final RegistryObject<SoundEvent> PHYSCANNON_TOOHEAVY =
            register("physcannon.tooheavy");

    public static final RegistryObject<SoundEvent> BUTTON_SOUND = SOUND_EVENTS.register("button_sound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("gravitygun", "button_sound")));

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(Gravitygun.MODID, name))
        );
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}