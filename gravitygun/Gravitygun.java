package com.zerokg2004.gravitygun.gravitygun;

import com.mojang.logging.LogUtils;
import com.zerokg2004.gravitygun.gravitygun.handler.RitualEventHandler;
import com.zerokg2004.gravitygun.gravitygun.registry.ModEntityTypes;
import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import com.zerokg2004.gravitygun.gravitygun.handler.NetworkHandler;
import com.zerokg2004.gravitygun.gravitygun.registry.ModRecipes; // ✅ NUEVO IMPORT
import com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Gravitygun.MODID)
public class Gravitygun {

    public static final String MODID = "gravitygun";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Gravitygun() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModItems.register(modEventBus);
        SoundEventsRegistry.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus); // ✅ REGISTRO DE RECETAS AÑADIDO

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabItems);
        NetworkHandler.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, com.zerokg2004.gravitygun.gravitygun.registry.Config.SPEC);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(RitualEventHandler.class); // ✅ REGISTRO CRUCIAL
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Gravity Gun mod cargado correctamente.");
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.GRAVITY_GUN.get());
            event.accept(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
            event.accept(ModItems.GRAVITY_BOOK.get());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Servidor iniciando con Gravity Gun mod.");
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        private static final Logger LOGGER = LogUtils.getLogger();

        @SubscribeEvent
        public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            LOGGER.info("Cliente cargado para Gravity Gun. Usuario: {}", net.minecraft.client.Minecraft.getInstance().getUser().getName());
        }
    }
}
