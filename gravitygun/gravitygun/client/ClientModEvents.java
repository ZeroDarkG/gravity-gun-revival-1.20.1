package com.zerokg2004.gravitygun.gravitygun.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zerokg2004.gravitygun.gravitygun.client.model.ModelGravityGunDetailed;
import com.zerokg2004.gravitygun.gravitygun.client.model.ModelGravityGunSimple;
import com.zerokg2004.gravitygun.gravitygun.client.render.RenderLiftedBlock;
import com.zerokg2004.gravitygun.gravitygun.client.sound.HeldSoundInstance;
import com.zerokg2004.gravitygun.gravitygun.handler.HeldObjectTracker;
import com.zerokg2004.gravitygun.gravitygun.handler.NetworkHandler;
import com.zerokg2004.gravitygun.gravitygun.packet.PacketThrowEntity;
import com.zerokg2004.gravitygun.gravitygun.registry.ModEntityTypes;
import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "gravitygun", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    private static final ResourceLocation RENDER_MODE =
            new ResourceLocation("gravitygun", "gravitygun_render_mode");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerRenderModePredicate(ModItems.GRAVITY_GUN.get());
            registerRenderModePredicate(ModItems.GRAVITY_GUN_SUPERCHARGED.get());

            registerRenderModePredicate(ModItems.GRAVITY_GUN_GREEN.get());
            registerRenderModePredicate(ModItems.GRAVITY_GUN_RED.get());
            registerRenderModePredicate(ModItems.GRAVITY_GUN_BLUE.get());
            registerRenderModePredicate(ModItems.GRAVITY_GUN_YELLOW.get());
            registerRenderModePredicate(ModItems.GRAVITY_GUN_ORANGE.get());
            registerRenderModePredicate(ModItems.GRAVITY_GUN_PURPLE.get());
        });
    }

    private static void registerRenderModePredicate(net.minecraft.world.item.Item item) {
        ItemProperties.register(item, RENDER_MODE, (stack, world, entity, seed) -> {
            if (RenderModeHandler.isForce2D()) return 0f;
            return Minecraft.getInstance().options.graphicsMode().get().ordinal() > 0 ? 2f : 1f;
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModelGravityGunDetailed.LAYER_LOCATION, ModelGravityGunDetailed::createLayer);
        event.registerLayerDefinition(ModelGravityGunSimple.LAYER_LOCATION, ModelGravityGunSimple::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.LIFTED_BLOCK.get(), RenderLiftedBlock::new);
    }

    // ============================================================
    // FORGE (runtime)
    // ============================================================
    @Mod.EventBusSubscriber(modid = "gravitygun", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

        private static HeldSoundInstance LOOP_SOUND;

        // ✅ ahora guardamos “última gun activa” (main si hay, si no off)
        private static ItemStack lastGunStack = ItemStack.EMPTY;
        private static long lastAmmoTick = -9999;

        private static boolean lastAttackDown = false;
        private static boolean lastUseDown = false;

        private static boolean wasHoldingLastTick = false;
        private static int suppressTooHeavyTicks = 0;

        private static final ResourceLocation PHYSGUN_CROSSHAIR_TEX =
                new ResourceLocation("gravitygun", "textures/gui/physgun_crosshair.png");

        private static final int PHYSGUN_CROSSHAIR_W = 160;
        private static final int PHYSGUN_CROSSHAIR_H = 90;

        private static boolean isGravityGunStack(ItemStack stack) {
            if (stack.isEmpty()) return false;

            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key == null) return false;

            return "gravitygun".equals(key.getNamespace())
                    && key.getPath().contains("gravity_gun");
        }

        // ✅ “gun activa” para sonidos/estado: main si es gun, si no off si es gun
        private static ItemStack getActiveGunStack(LocalPlayer player) {
            ItemStack main = player.getMainHandItem();
            if (isGravityGunStack(main)) return main;
            ItemStack off = player.getOffhandItem();
            if (isGravityGunStack(off)) return off;
            return ItemStack.EMPTY;
        }

        private static boolean isDualWield(LocalPlayer player) {
            return isGravityGunStack(player.getMainHandItem()) && isGravityGunStack(player.getOffhandItem());
        }

        @SubscribeEvent
        public static void onRenderCrosshairOverlay(RenderGuiOverlayEvent.Post event) {
            if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ItemStack main = mc.player.getMainHandItem();
            ItemStack off = mc.player.getOffhandItem();

            if (!isGravityGunStack(main) && !isGravityGunStack(off)) return;

            GuiGraphics gg = event.getGuiGraphics();

            int screenW = gg.guiWidth();
            int screenH = gg.guiHeight();

            int x = (screenW - PHYSGUN_CROSSHAIR_W) / 2;
            int y = (screenH - PHYSGUN_CROSSHAIR_H) / 2;

            RenderSystem.enableBlend();
            gg.blit(
                    PHYSGUN_CROSSHAIR_TEX,
                    x, y,
                    0, 0,
                    PHYSGUN_CROSSHAIR_W,
                    PHYSGUN_CROSSHAIR_H,
                    PHYSGUN_CROSSHAIR_W,
                    PHYSGUN_CROSSHAIR_H
            );
            RenderSystem.disableBlend();
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {

            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player == null || mc.level == null) return;

            long now = mc.level.getGameTime();

            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();

            boolean hasGunMain = isGravityGunStack(main);
            boolean hasGunOff = isGravityGunStack(off);
            boolean hasGun = hasGunMain || hasGunOff;
            boolean dual = hasGunMain && hasGunOff;

            // ✅ stack “activo” (para sonidos/UI). NO afecta a render de rayos (eso lo hace el renderer)
            ItemStack currentGun = getActiveGunStack(player);
            boolean currentIsGun = !currentGun.isEmpty();

            boolean lastWasGun = isGravityGunStack(lastGunStack);
            boolean changed = !ItemStack.isSameItemSameTags(currentGun, lastGunStack);

            // =========================
            // AMMO PICKUP SOUND
            // - se dispara si “entras” a tener gun activa (main u off)
            // =========================
            if (changed && currentIsGun && !lastWasGun) {
                if ((now - lastAmmoTick) > 5) {
                    if (SoundEventsRegistry.AMMO_PICKUP.isPresent()) {
                        player.playSound(SoundEventsRegistry.AMMO_PICKUP.get(), 1.0F, 1.0F);
                    }
                    lastAmmoTick = now;
                }
            }

            lastGunStack = currentGun.copy();

            // holding state
            boolean holding = HeldObjectTracker.isHolding(player.getUUID());

            if (wasHoldingLastTick && !holding) {
                suppressTooHeavyTicks = 4;
            }
            wasHoldingLastTick = holding;

            if (suppressTooHeavyTicks > 0) suppressTooHeavyTicks--;

            // =========================
            // CLICK IZQUIERDO (THROW O PUSH)
            // - con dual wield NO mandamos packet (porque no se usa)
            // =========================
            boolean attackDown = mc.options.keyAttack.isDown();

            if (attackDown && !lastAttackDown) {

                if (!hasGun) {
                    lastAttackDown = true;
                    return;
                }

                if (!dual) {
                    // - holding -> throwHeld true
                    // - no holding -> push false
                    NetworkHandler.INSTANCE.sendToServer(new PacketThrowEntity(holding));
                }
            }

            lastAttackDown = attackDown;

            // =========================
            // CLICK DERECHO -> TOO HEAVY (1 vez por pulsación)
            // SOLO cuando NO estás holding
            // - con dual wield NO (porque no se usa)
            // =========================
            boolean useDown = mc.options.keyUse.isDown();

            if (useDown && !lastUseDown && hasGun && !dual && !holding && suppressTooHeavyTicks == 0) {

                boolean tooHeavy = false;
                HitResult hit = mc.hitResult;

                if (hit == null || hit.getType() == HitResult.Type.MISS) {
                    tooHeavy = true;

                } else if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) hit;
                    if (mc.level.getBlockState(bhr.getBlockPos()).is(Blocks.BEDROCK)) {
                        tooHeavy = true;
                    }

                } else if (hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult ehr = (EntityHitResult) hit;
                    if (ehr.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                        tooHeavy = true;
                    }
                }

                if (tooHeavy && SoundEventsRegistry.PHYSCANNON_TOOHEAVY.isPresent()) {
                    player.playSound(SoundEventsRegistry.PHYSCANNON_TOOHEAVY.get(), 1.0F, 1.0F);
                }
            }

            lastUseDown = useDown;

            // =========================
            // HOLD LOOP SOUND
            // - con dual wield: NO loop (porque no se usa)
            // - usa currentGun (main u off)
            // =========================
            if (!dual && holding) {

                if (LOOP_SOUND == null || LOOP_SOUND.isStopped()) {

                    if (currentGun.is(ModItems.GRAVITY_GUN_SUPERCHARGED.get())) {

                        LOOP_SOUND = new HeldSoundInstance(
                                player,
                                SoundEventsRegistry.SUPERPHYS_HOLD_LOOP.get()
                        );
                        mc.getSoundManager().play(LOOP_SOUND);

                    } else if (currentIsGun) {

                        LOOP_SOUND = new HeldSoundInstance(
                                player,
                                SoundEventsRegistry.HOLD_LOOP.get()
                        );
                        mc.getSoundManager().play(LOOP_SOUND);
                    }
                }

            } else {

                if (LOOP_SOUND != null && !LOOP_SOUND.isStopped()) {
                    LOOP_SOUND.stopManually();
                    mc.getSoundManager().stop(LOOP_SOUND);
                    LOOP_SOUND = null;
                }
            }
        }

        // =========================
        // PLAYER ARM POSE
        // =========================
        @SubscribeEvent
        public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {

            if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
            if (!(event.getRenderer().getModel() instanceof PlayerModel<?> baseModel)) return;

            @SuppressWarnings("unchecked")
            PlayerModel<AbstractClientPlayer> model =
                    (PlayerModel<AbstractClientPlayer>) baseModel;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            boolean hasGunMain = isGravityGunStack(mainHand);
            boolean hasGunOff = isGravityGunStack(offHand);

            boolean dual = hasGunMain && hasGunOff;

            if (dual) {
                model.rightArmPose = PlayerModel.ArmPose.EMPTY;
                model.leftArmPose = PlayerModel.ArmPose.EMPTY;
            } else {
                model.rightArmPose = hasGunMain ? PlayerModel.ArmPose.BOW_AND_ARROW : PlayerModel.ArmPose.EMPTY;
                model.leftArmPose = hasGunOff ? PlayerModel.ArmPose.BOW_AND_ARROW : PlayerModel.ArmPose.EMPTY;
            }
        }
    }
}