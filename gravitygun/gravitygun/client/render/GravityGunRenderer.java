package com.zerokg2004.gravitygun.gravitygun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zerokg2004.gravitygun.gravitygun.client.RenderModeHandler;
import com.zerokg2004.gravitygun.gravitygun.client.model.ModelGravityGunDetailed;
import com.zerokg2004.gravitygun.gravitygun.client.model.ModelGravityGunSimple;
import com.zerokg2004.gravitygun.gravitygun.item.GravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.item.GravityGunItem.GravityGunState;
import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@ParametersAreNonnullByDefault
public class GravityGunRenderer extends BlockEntityWithoutLevelRenderer {

    private final ModelGravityGunDetailed modelDetailed;
    private final ModelGravityGunSimple modelSimple;
    private final Map<Integer, Float> clawAnimMap = new HashMap<>();
    private final Random rand = new Random();

    private static final ResourceLocation NORMAL_GLOW_DETAILED =
            new ResourceLocation("gravitygun", "textures/item/gravity_gun_detailed_glow.png");

    private static final ResourceLocation SUPERCHARGED_GLOW_DETAILED =
            new ResourceLocation("gravitygun", "textures/item/gravity_gun_detailed_supercharged_glow.png");

    private static final float[][] TRANSFORMATIONS_DETAILED = {
            {-280F, -50F, 80F, 0.5F, 0.5F, 0.6F, 0.5F, 0.5F, 0.5F},
            {-280F, -50F, 80F, 0.5F, 0.5F, 0.6F, 0.5F, 0.5F, 0.5F},
            {-10.0F, -90F, -5.0F, 1F, 0.1F, 0.5F, 1F, 1F, 1F},
            {-10.1F, -90F, -5.0F, 0.0F, 0.1F, 0.5F, 1F, 1F, 1F},
            {0.0F, -90F, 0.0F, 0.0F, -0.1F, -0.05F, 0.7F, 0.7F, 0.7F},
            {30F, 225F, 0.0F, 0.2F, 0.2F, -0.05F, 0.5F, 0.5F, 0.5F},
            {-25F, 122.5F, 0.0F, 0.35F, 0.35F, 0.4F, 0.25F, 0.25F, 0.25F},
            {-180F, 180F, 150F, 0.8F, 0.25F, 0.5F, 0.5F, 0.5F, 0.5F}
    };

    private static final float[][] TRANSFORMATIONS_SIMPLE = {
            {-280F, -50F, 80F, 0.55F, 1.7F, 0.4F, 0.8F, 0.8F, 0.8F},
            {-280F, -50F, 80F, 0.55F, 1.7F, 0.4F, 0.8F, 0.8F, 0.8F},
            {-10.0F, -90F, -5.0F, 1.8F, 2.5F, -1F, 2F, 2F, 2F},
            {-10.1F, -90F, -5.0F, -0.5F, 2.5F, -1F, 2F, 2F, 2F},
            {0.0F, -90F, 0.0F, 0.0F, -0.1F, -0.05F, 0.7F, 0.7F, 0.7F},
            {30F, 225F, 0.0F, 0.4F, 1.3F, -0.05F, 0.8F, 0.8F, 0.8F},
            {-25F, 122.5F, 0.0F, 0.35F, 1F, 0.2F, 0.45F, 0.45F, 0.45F},
            {-180F, 180F, 150F, 1.3F, 1.5F, 0.4F, 0.9F, 0.9F, 0.9F}
    };

    public GravityGunRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.modelDetailed = new ModelGravityGunDetailed(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelGravityGunDetailed.LAYER_LOCATION)
        );
        this.modelSimple = new ModelGravityGunSimple(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelGravityGunSimple.LAYER_LOCATION)
        );
    }

    private static boolean isDualWieldBlockedClient(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off  = mc.player.getOffhandItem();
        return main.getItem() instanceof GravityGunItem && off.getItem() instanceof GravityGunItem;
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack,
                             @Nonnull ItemDisplayContext context,
                             @Nonnull PoseStack poseStack,
                             @Nonnull MultiBufferSource buffer,
                             int packedLight,
                             int packedOverlay) {

        if (RenderModeHandler.isForce2D()) return;

        Minecraft mc = Minecraft.getInstance();
        boolean dualBlocked = mc.player != null && isDualWieldBlockedClient(mc);

        boolean useDetailed = mc.options.graphicsMode().get() != GraphicsStatus.FAST;

        // ✅ “es supercharged” por item (mejor que comparar solo el registry object)
        boolean isSupercharged = stack.is(ModItems.GRAVITY_GUN_SUPERCHARGED.get());

        var regKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = (regKey != null) ? regKey.getPath() : "unknown";
        boolean isColoredGun = itemId.endsWith("_gravity_gun") && !itemId.equals("gravity_gun");

        ResourceLocation texture;
        ResourceLocation glow = null;

        if (isSupercharged) {
            texture = new ResourceLocation("gravitygun",
                    useDetailed ? "textures/item/gravity_gun_detailed_supercharged.png"
                            : "textures/item/gravity_gun_simple_supercharged.png");
            if (useDetailed) glow = SUPERCHARGED_GLOW_DETAILED;

        } else if (isColoredGun) {
            String color = itemId.substring(0, itemId.length() - "_gravity_gun".length());
            texture = new ResourceLocation("gravitygun",
                    "textures/item/" + color + "_gravity_gun_" + (useDetailed ? "detailed" : "simple") + ".png");
            if (useDetailed) glow = NORMAL_GLOW_DETAILED;

        } else {
            texture = new ResourceLocation("gravitygun",
                    useDetailed ? "textures/item/gravity_gun_detailed.png"
                            : "textures/item/gravity_gun_simple.png");
            if (useDetailed) glow = NORMAL_GLOW_DETAILED;
        }

        // ✅ saber si este stack corresponde a la mano main/off del jugador (para autoridad de sonido)
        boolean inMain = false;
        boolean inOff = false;
        boolean mainIsSuper = false;
        boolean offIsSuper = false;

        if (mc.player != null) {
            ItemStack main = mc.player.getMainHandItem();
            ItemStack off  = mc.player.getOffhandItem();
            inMain = ItemStack.isSameItemSameTags(main, stack);
            inOff  = ItemStack.isSameItemSameTags(off, stack);
            mainIsSuper = main.is(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
            offIsSuper  = off.is(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
        }

        // ✅ autoridad de sonido para zaps: normalmente mainhand.
        // si main NO es supercharged y off SÍ, entonces offhand manda.
        boolean zapSoundAuthority = (mc.player != null) && (
                (mainIsSuper && inMain) ||
                        (!mainIsSuper && offIsSuper && inOff)
        );

        poseStack.pushPose();
        applyTransform(poseStack, context, useDetailed ? TRANSFORMATIONS_DETAILED : TRANSFORMATIONS_SIMPLE);

        // ✅ “bajado de brazo” en 1ª persona cuando dualBlocked
        if (dualBlocked && (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)) {
            poseStack.translate(0.0F, 0.12F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(4.0F));
        }

        // ✅ Vibración SUPERCHARGED (solo DETAILED). Si dualBlocked, igual vibra (como efecto)
        if (isSupercharged && useDetailed
                && context != ItemDisplayContext.GUI
                && context != ItemDisplayContext.FIXED
                && mc.player != null) {

            int tick = mc.player.tickCount;
            rand.setSeed((long) tick * 10000L);

            float rot = 1.4F;
            float rx = (rand.nextFloat() * 2.0F - 1.0F) * rot;
            float rz = (rand.nextFloat() * 2.0F - 1.0F) * rot;

            poseStack.mulPose(Axis.XP.rotationDegrees(rx));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rz));
        }

        VertexConsumer baseConsumer = buffer.getBuffer(RenderType.entitySolid(texture));

        if (!useDetailed) {
            modelSimple.renderToBuffer(poseStack, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            poseStack.popPose();
            return;
        }

        // ✅ Animación garras (normal). Ahora funciona también en offhand gracias al patch de getVisualState.
        if (!isSupercharged && stack.getItem() instanceof GravityGunItem gun && mc.player != null) {
            GravityGunState state = gun.getVisualState(mc.player, stack);
            float targetClaw = switch (state) {
                case CLOSED -> 0.0F;
                case AIMING, HOLDING -> 1.0F;
            };

            int key = System.identityHashCode(stack);
            float currentClaw = clawAnimMap.getOrDefault(key, 0.0F);
            float smoothClaw = Mth.lerp(1.0F, currentClaw, targetClaw);
            clawAnimMap.put(key, smoothClaw);
            modelDetailed.setClawOpen(smoothClaw);
        } else if (!isSupercharged) {
            modelDetailed.setClawOpen(0.0F);
        }

        // Render base
        modelDetailed.renderToBuffer(poseStack, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        // Render glow
        if (glow != null) {
            VertexConsumer glowConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(glow));
            modelDetailed.renderToBuffer(poseStack, glowConsumer, 0xF000F0, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        }

        // ===== FX =====
        if (context != ItemDisplayContext.GUI && context != ItemDisplayContext.FIXED && mc.player != null) {
            int tick = mc.player.tickCount;

            CompoundTag root = stack.getOrCreateTag();
            CompoundTag gg = root.getCompound("GravityGun");
            root.put("GravityGun", gg);

            if (!gg.getBoolean("fxInit")) {
                gg.putBoolean("fxInit", true);
                gg.putInt("lastZap", -9999);
                gg.putInt("lastZapClaw", 0);
                gg.putInt("nextZapTick", tick + 20 + rand.nextInt(40));
                gg.putInt("attackLightning", -9999);
                gg.putDouble("attackDistance", 0.0D);
            }

            sanitizeFxTimers(gg, tick);

            int colorRGB = isSupercharged ? 9561070 : 16757575;
            float baseAlpha = 0.9F;

            boolean renderClawLightning1 = false;
            boolean renderClawLightning2 = false;
            boolean renderClawLightning3 = false;

            // ✅ NORMAL: rayos SOLO cuando HOLDING (y ahora también en offhand funciona)
            if (!isSupercharged && stack.getItem() instanceof GravityGunItem gun) {
                GravityGunState st = gun.getVisualState(mc.player, stack);
                boolean holding = st == GravityGunState.HOLDING;
                renderClawLightning1 = holding;
                renderClawLightning2 = holding;
                renderClawLightning3 = holding;
            }

            // ✅ SUPERCHARGED:
            // - si dualBlocked: queremos rayos ALEATORIOS igualmente (sin forceAll)
            // - si NO dualBlocked: tu lógica original (forceAll si HOLDING, etc.)
            if (isSupercharged) {

                boolean allowForceAll = !dualBlocked;
                boolean forceAll = false;

                if (allowForceAll && stack.getItem() instanceof GravityGunItem gun) {
                    GravityGunState st = gun.getVisualState(mc.player, stack);
                    forceAll = st == GravityGunState.HOLDING;
                }

                GravityGunState stNow = GravityGunState.CLOSED;
                if (stack.getItem() instanceof GravityGunItem gun) {
                    stNow = gun.getVisualState(mc.player, stack);
                }
                boolean holdingNow = stNow == GravityGunState.HOLDING;

                // ✅ scheduler de random zaps:
                // en dualBlocked lo dejamos SIEMPRE (para que haya rayos aleatorios incluso sin usar)
                // pero el SONIDO solo lo dispara la mano "autoridad" para evitar doble zap.
                boolean doRandom = (!forceAll && (!mc.options.keyUse.isDown() || !holdingNow)) || dualBlocked;

                if (doRandom) {
                    int nextZapTick = gg.getInt("nextZapTick");
                    if (nextZapTick <= 0) {
                        nextZapTick = tick + 15 + rand.nextInt(40);
                        gg.putInt("nextZapTick", nextZapTick);
                    }

                    if (tick >= nextZapTick) {
                        if (rand.nextFloat() < 0.45F) {
                            gg.putInt("lastZap", tick);
                            gg.putInt("lastZapClaw", rand.nextInt(3) + 1);

                            // ✅ SOLO sonido en una mano para evitar duplicado
                            if (zapSoundAuthority) {
                                playRandomZapSound(mc);
                            }
                        }
                        gg.putInt("nextZapTick", tick + 20 + rand.nextInt(60));
                    }

                    int claw = gg.getInt("lastZapClaw");
                    int lastZap = gg.getInt("lastZap");
                    if (claw > 0) {
                        int delta = tick - lastZap;
                        if (delta >= 0 && delta < 4) {
                            if (claw == 1) renderClawLightning1 = true;
                            else if (claw == 2) renderClawLightning2 = true;
                            else if (claw == 3) renderClawLightning3 = true;
                        }
                    }
                } else {
                    // forceAll (solo si NO dualBlocked)
                    renderClawLightning1 = true;
                    renderClawLightning2 = true;
                    renderClawLightning3 = true;
                }
            }

            // ✅ beams
            int segments = 8;
            float jitter = isSupercharged ? 2.2F : 1.8F;
            float baseThickness = 0.45F;

            poseStack.pushPose();
            poseStack.scale(0.025F, 0.025F, 0.025F);

            float cx = -71.5F, cy = -8.5F, cz = -0.5F;

            if (renderClawLightning1) {
                LightningFX.renderBeamBetween6(
                        poseStack, buffer, (long) tick * 3000L + 1L, tick,
                        segments, jitter, baseThickness, colorRGB, baseAlpha,
                        -73.0F, -0.5F, -14.0F, cx, cy, cz
                );
            }
            if (renderClawLightning2) {
                LightningFX.renderBeamBetween6(
                        poseStack, buffer, (long) tick * 3000L + 2L, tick,
                        segments, jitter, baseThickness, colorRGB, baseAlpha,
                        -73.0F, -26.0F, -0.5F, cx, cy, cz
                );
            }
            if (renderClawLightning3) {
                LightningFX.renderBeamBetween6(
                        poseStack, buffer, (long) tick * 3000L + 3L, tick,
                        segments, jitter, baseThickness, colorRGB, baseAlpha,
                        -73.0F, -1.5F, 14.5F, cx, cy, cz
                );
            }

            poseStack.popPose();

            // ✅ attack lightning SOLO si NO dualBlocked (porque es “uso/ataque”)
            if (!dualBlocked) {
                int attackLightning = gg.getInt("attackLightning");
                if (attackLightning < tick - 100 || attackLightning > tick) {
                    gg.putInt("attackLightning", tick - 10);
                    attackLightning = tick - 10;
                }

                double attackDist = gg.getDouble("attackDistance");
                double dist = attackDist * 19.0D;

                if (tick - attackLightning < 2 && dist > 0.0D) {
                    poseStack.pushPose();
                    poseStack.scale(0.1F, 0.1F, 0.1F);
                    poseStack.translate(-15.5, -3.0, -0.5);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

                    float rr = ((colorRGB >> 16) & 255) / 255.0F;
                    float ggCol = ((colorRGB >> 8) & 255) / 255.0F;
                    float bbCol = (colorRGB & 255) / 255.0F;

                    LightningFX.renderLightning(
                            poseStack, buffer, (long) tick * 3000L + 99L,
                            10, (float) dist, 1.6F, 0.5F,
                            rr, ggCol, bbCol, 0.9F
                    );

                    poseStack.popPose();
                }
            }

            root.put("GravityGun", gg);
        }

        poseStack.popPose();
    }

    private static void sanitizeFxTimers(CompoundTag gg, int tick) {
        int lastZap = gg.getInt("lastZap");
        int lastZapClaw = gg.getInt("lastZapClaw");
        int nextZapTick = gg.getInt("nextZapTick");

        if (lastZap > tick || lastZap < tick - 20000) {
            gg.putInt("lastZap", -9999);
            gg.putInt("lastZapClaw", 0);
        }

        if (lastZapClaw < 0 || lastZapClaw > 3) {
            gg.putInt("lastZapClaw", 0);
        }

        if (nextZapTick > tick + 20000 || nextZapTick < 0) {
            gg.putInt("nextZapTick", tick + 20);
        }
    }

    private void playRandomZapSound(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        int i = rand.nextInt(4);
        SoundEvent sound = switch (i) {
            case 0 -> SoundEventsRegistry.SUPERPHYS_ZAP1.get();
            case 1 -> SoundEventsRegistry.SUPERPHYS_ZAP2.get();
            case 2 -> SoundEventsRegistry.SUPERPHYS_ZAP3.get();
            default -> SoundEventsRegistry.SUPERPHYS_ZAP4.get();
        };

        mc.level.playLocalSound(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                sound, SoundSource.PLAYERS,
                0.4F, 0.9F + rand.nextFloat() * 0.2F,
                false
        );
    }

    private void applyTransform(PoseStack poseStack, ItemDisplayContext context, float[][] transforms) {
        int index = switch (context) {
            case THIRD_PERSON_RIGHT_HAND -> 0;
            case THIRD_PERSON_LEFT_HAND -> 1;
            case FIRST_PERSON_RIGHT_HAND -> 2;
            case FIRST_PERSON_LEFT_HAND -> 3;
            case HEAD -> 4;
            case GUI -> 5;
            case GROUND -> 6;
            case FIXED -> 7;
            default -> -1;
        };

        if (index >= 0 && index < transforms.length) {
            float[] t = transforms[index];

            poseStack.translate(t[3], t[4], t[5]);
            poseStack.mulPose(Axis.XP.rotationDegrees(t[0]));
            poseStack.mulPose(Axis.YP.rotationDegrees(t[1]));
            poseStack.mulPose(Axis.ZP.rotationDegrees(t[2]));
            poseStack.scale(t[6], -t[7], -t[8]);

            if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-40.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(10.0F));
            }
        }
    }
}