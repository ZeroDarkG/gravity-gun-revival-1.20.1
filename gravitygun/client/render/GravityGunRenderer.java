package com.zerokg2004.gravitygun.gravitygun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zerokg2004.gravitygun.gravitygun.client.RenderModeHandler;
import com.zerokg2004.gravitygun.gravitygun.client.model.ModelGravityGunDetailed;
import com.zerokg2004.gravitygun.gravitygun.client.model.ModelGravityGunSimple;
import com.zerokg2004.gravitygun.gravitygun.handler.HeldObjectTracker;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@ParametersAreNonnullByDefault
public class GravityGunRenderer extends BlockEntityWithoutLevelRenderer {

    private static final float CLAW_OPEN_POSE = 1.0F;
    private static final float CLAW_CLOSED_POSE = 0.0F;
    private static final float CLAW_OPEN_SPEED = 0.082F;
    private static final float CLAW_CLOSE_SPEED = 0.074F;
    private final ModelGravityGunDetailed modelDetailed;
    private final ModelGravityGunSimple modelSimple;
    private final Map<String, Float> clawAnimMap = new HashMap<>();
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
            {30F, -50F, 0.0F, 0.75F, 0.2F, -0.05F, 0.5F, 0.5F, 0.5F},
            {-25F, 122.5F, 0.0F, 0.35F, 0.35F, 0.4F, 0.25F, 0.25F, 0.25F},
            {-180F, 180F, 150F, 0.8F, 0.25F, 0.5F, 0.5F, 0.5F, 0.5F}
    };

    private static final float[][] TRANSFORMATIONS_SIMPLE = {
            {-280F, -50F, 80F, 0.55F, 1.7F, 0.4F, 0.8F, 0.8F, 0.8F},
            {-280F, -50F, 80F, 0.55F, 1.7F, 0.4F, 0.8F, 0.8F, 0.8F},
            {-10.0F, -90F, -5.0F, 1.8F, 2.5F, -0.8F, 2F, 2F, 2F},
            {-10.1F, -90F, -5.0F, -0.5F, 2.5F, -0.8F, 2F, 2F, 2F},
            {0.0F, -90F, 0.0F, 0.0F, -0.1F, -0.05F, 0.7F, 0.7F, 0.7F},
            {30F, -50F, 0.0F, 0.6F, 1.3F, -0.05F, 0.8F, 0.8F, 0.8F},
            {-25F, 122.5F, 0.0F, 0.35F, 1F, 0.2F, 0.45F, 0.45F, 0.45F},
            {-180F, 180F, 150F, 1.3F, 1.5F, 0.4F, 0.9F, 0.9F, 0.9F}
    };

    public GravityGunRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.modelDetailed = new ModelGravityGunDetailed(Minecraft.getInstance().getEntityModels().bakeLayer(ModelGravityGunDetailed.LAYER_LOCATION));
        this.modelSimple = new ModelGravityGunSimple(Minecraft.getInstance().getEntityModels().bakeLayer(ModelGravityGunSimple.LAYER_LOCATION));
    }

    private static boolean isDualWieldBlockedClient(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        return main.getItem() instanceof GravityGunItem && off.getItem() instanceof GravityGunItem;
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext context, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (RenderModeHandler.isForce2D()) return;

        Minecraft mc = Minecraft.getInstance();
        boolean dualBlocked = mc.player != null && isDualWieldBlockedClient(mc);
        boolean useDetailed = RenderModeHandler.isDetailed();
        boolean isSupercharged = stack.is(ModItems.GRAVITY_GUN_SUPERCHARGED.get());

        // --- LÓGICA DE TEXTURAS REPARADA Y COMPATIBLE ---
        var regKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = (regKey != null) ? regKey.getPath() : "unknown";

// Forzamos la detección de Supercharged por clase
        isSupercharged = stack.getItem() instanceof com.zerokg2004.gravitygun.gravitygun.item.SuperchargedGravityGunItem;

        String suffix = useDetailed ? "detailed" : "simple";
        String texturePath;

        if (isSupercharged) {
            if (itemId.equals("gravity_gun_supercharged")) {
                // La azul original (ID: gravity_gun_supercharged)
                // Ajusta este nombre si tu archivo se llama distinto, ej: "gravity_gun_supercharged_detailed"
                texturePath = "gravity_gun_" + suffix + "_supercharged";
            } else {
                // Las nuevas de colores (ID: red_supercharged_gravity_gun)
                // Tus archivos van PRIMERO Supercharged, LUEGO color: "supercharged_red_gravity_gun_detailed"
                String color = itemId.split("_")[0];
                texturePath = "supercharged_" + color + "_gravity_gun_" + suffix;
            }
        } else {
            // Las normales (ID: red_gravity_gun)
            // Tus archivos son: "red_gravity_gun_detailed"
            texturePath = itemId + "_" + suffix;
        }

// Ruta final del recurso
        ResourceLocation texture = new ResourceLocation("gravitygun", "textures/item/" + texturePath + ".png");

// Capa de Brillo (Glow)
        ResourceLocation glow = null;
        if (useDetailed) {
            glow = isSupercharged ? SUPERCHARGED_GLOW_DETAILED : NORMAL_GLOW_DETAILED;
        }

        poseStack.pushPose();

        // 1. APLICAR TRANSFORMACIONES BÁSICAS
        applyTransform(poseStack, context, useDetailed ? TRANSFORMATIONS_DETAILED : TRANSFORMATIONS_SIMPLE);
        applyThrowRecoil(poseStack, context, useDetailed, false, mc);

        if (isSupercharged && useDetailed && context != ItemDisplayContext.GUI && context != ItemDisplayContext.FIXED && mc.player != null) {
            float animTime = mc.player.tickCount + mc.getFrameTime();
            applySmoothSuperchargedJitter(poseStack, animTime);
        }

        // --- Render del Modelo ---
        VertexConsumer baseConsumer = buffer.getBuffer(RenderType.entitySolid(texture));
        if (!useDetailed) {
            modelSimple.renderToBuffer(poseStack, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        } else {
            if (stack.getItem() instanceof GravityGunItem gun && mc.player != null) {
                if (isSupercharged) {
                    modelDetailed.setClawOpen(1.0F);
                } else {
                    String animKey = getClawAnimKey(context, false);
                    GravityGunState visualState = gun.getVisualState(mc.player, stack);
                    float targetClaw = (visualState == GravityGunState.CLOSED) ? CLAW_CLOSED_POSE : CLAW_OPEN_POSE;
                    float currentClaw = clawAnimMap.getOrDefault(animKey, targetClaw);
                    currentClaw = animateClawPose(currentClaw, targetClaw, mc.getFrameTime());

                    clawAnimMap.put(animKey, currentClaw);
                    modelDetailed.setClawOpen(currentClaw);
                }
            } else {
                modelDetailed.setClawOpen(0.0F);
            }

            modelDetailed.renderToBuffer(poseStack, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            if (glow != null) {
                float animTime = mc.player != null ? mc.player.tickCount + mc.getFrameTime() : 0.0F;

                float glowAlpha;
                if (isSupercharged) {
                    glowAlpha = 0.35F + 0.65F * (0.5F + 0.5F * Mth.sin(animTime * 0.12F));
                } else {
                    glowAlpha = 0.20F + 0.55F * (0.5F + 0.5F * Mth.sin(animTime * 0.08F));
                }

                VertexConsumer glowConsumer = buffer.getBuffer(RenderType.entityTranslucent(glow));
                modelDetailed.renderToBuffer(
                        poseStack,
                        glowConsumer,
                        0xF000F0,
                        OverlayTexture.NO_OVERLAY,
                        1.0F,
                        1.0F,
                        1.0F,
                        glowAlpha
                );
            }
        }

        // --- FX Section Restaurada ---
        renderFX(stack, context, poseStack, buffer, mc, isSupercharged, dualBlocked, useDetailed);

        poseStack.popPose(); // FIN ARMA (Cierra el pushPose del arma)

        // --- ✅ BLOQUE DEL BRAZO DERECHO (CORREGIDO) ---
        // Solo entramos si es Primera Persona, es la Mano Derecha y no hay Dual Wield bloqueado
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && mc.player != null && !dualBlocked) {
            poseStack.pushPose();

            if (useDetailed) {
                // --- COORDENADAS MODO DETALLADO ---
                poseStack.translate(0.08F, 0.7F, 0.8F);
                poseStack.mulPose(Axis.XP.rotationDegrees(0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-120F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
            } else {
                // --- COORDENADAS MODO SIMPLE ---
                poseStack.translate(0.2F, 0.7F, 0.8F);
                poseStack.mulPose(Axis.XP.rotationDegrees(0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-120F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
            }

            applyThrowRecoil(poseStack, context, useDetailed, true, mc);

            // Renderizado seguro del brazo
            var entityRenderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
            if (entityRenderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                playerRenderer.renderRightHand(poseStack, buffer, packedLight, mc.player);
            }

            poseStack.popPose();
        }
    } // Fin

    private void applySmoothSuperchargedJitter(PoseStack poseStack, float animTime) {
        float pulse = 0.55F + 0.45F * (0.5F + 0.5F * Mth.sin(animTime * 0.42F + 0.35F));

        float rx = (Mth.sin(animTime * 0.95F) * 0.38F
                + Mth.sin(animTime * 1.85F + 0.70F) * 0.12F) * pulse;

        float ry = (Mth.sin(animTime * 0.72F + 1.10F) * 0.14F
                + Mth.sin(animTime * 1.55F + 2.20F) * 0.05F) * pulse;

        float rz = (Mth.sin(animTime * 1.18F + 2.00F) * 0.48F
                + Mth.sin(animTime * 2.25F + 0.45F) * 0.16F) * pulse;

        float offsetX = Mth.sin(animTime * 0.85F + 0.25F) * 0.0008F * pulse;
        float offsetY = Mth.sin(animTime * 0.92F + 0.50F) * 0.0022F * pulse;
        float offsetZ = Mth.sin(animTime * 1.08F + 1.20F) * 0.0018F * pulse;

        poseStack.translate(offsetX, offsetY, offsetZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(rx));
        poseStack.mulPose(Axis.YP.rotationDegrees(ry));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rz));
    }

    private void applyThrowRecoil(PoseStack poseStack, ItemDisplayContext context, boolean detailed, boolean armPass, Minecraft mc) {
        if (mc.player == null) return;
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) return;

        float tickTime = mc.player.tickCount + mc.getFrameTime();
        float recoil = LightningFX.ThrowFXState.recoilStrength(tickTime);
        if (recoil <= 0.0F) return;

        float handed = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0F : 1.0F;
        float strength = recoil * (LightningFX.ThrowFXState.supercharged() ? 1.18F : 1.0F);

        if (armPass) {
            poseStack.translate(0.020F * handed * strength, -0.018F * strength, 0.120F * strength);
            poseStack.mulPose(Axis.XP.rotationDegrees(5.5F * strength));
            poseStack.mulPose(Axis.YP.rotationDegrees(-4.0F * handed * strength));
            poseStack.mulPose(Axis.ZP.rotationDegrees(2.5F * handed * strength));
            return;
        }

        float zPush = detailed ? 0.165F : 0.185F;
        poseStack.translate(0.030F * handed * strength, -0.022F * strength, zPush * strength);
        poseStack.mulPose(Axis.XP.rotationDegrees(7.0F * strength));
        poseStack.mulPose(Axis.YP.rotationDegrees(-5.5F * handed * strength));
        poseStack.mulPose(Axis.ZP.rotationDegrees(3.5F * handed * strength));
    }

    private float animateClawPose(float current, float target, float partialTick) {
        float speed = target > current ? CLAW_OPEN_SPEED : CLAW_CLOSE_SPEED;
        float frameFactor = Mth.clamp(partialTick + 1.0F, 1.0F, 2.25F);
        float alpha = 1.0F - (float) Math.pow(1.0F - speed, frameFactor);
        float next = Mth.lerp(alpha, current, target);
        if (Math.abs(target - next) < 0.0025F) {
            return target;
        }
        return next;
    }

    private String getClawAnimKey(ItemDisplayContext context, boolean isSupercharged) {
        return switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> isSupercharged ? "fp_right_super" : "fp_right";
            case FIRST_PERSON_LEFT_HAND -> isSupercharged ? "fp_left_super" : "fp_left";
            case THIRD_PERSON_RIGHT_HAND -> isSupercharged ? "tp_right_super" : "tp_right";
            case THIRD_PERSON_LEFT_HAND -> isSupercharged ? "tp_left_super" : "tp_left";
            case GUI -> isSupercharged ? "gui_super" : "gui";
            case FIXED -> isSupercharged ? "fixed_super" : "fixed";
            case GROUND -> isSupercharged ? "ground_super" : "ground";
            default -> isSupercharged ? "other_super" : "other";
        };
    }

    private void renderFX(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, Minecraft mc, boolean isSupercharged, boolean dualBlocked, boolean useDetailed) {
        // 1. FILTRO DE SALIDA: Si es el modelo simple (FAST graphics), no renderizamos rayos ni sonidos.
        // También evitamos el render en inventario (GUI) o marcos (FIXED).
        if (!useDetailed || context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED || mc.player == null) {
            return;
        }

        int tick = mc.player.tickCount;
        CompoundTag gg = stack.getOrCreateTagElement("GravityGun");

        // Inicialización de timers de efectos si no existen
        if (!gg.contains("nextZapTick")) {
            gg.putInt("nextZapTick", tick + 40 + rand.nextInt(60));
        }
        sanitizeFxTimers(gg, tick);

        int colorRGB = isSupercharged ? 9561070 : 0xFF9A32;
        boolean isClientPlayerHold = mc.player.getMainHandItem() == stack || mc.player.getOffhandItem() == stack;

        boolean l1 = false, l2 = false, l3 = false;
        boolean throwBeamActive = LightningFX.ThrowFXState.active(tick);
        boolean isActuallyHolding = HeldObjectTracker.isHoldingClient(mc.player.getUUID());

        // 2. LÓGICA DE GARRAS Y ZAPS ALEATORIOS (Solo Modo Detallado)
        if (stack.getItem() instanceof GravityGunItem gun) {
            if (isActuallyHolding && !throwBeamActive) {
                // Si sostiene algo, garras activas al 100%
                l1 = l2 = l3 = true;
            } else if (isSupercharged) {
                // Si está supercargado y libre, zaps aleatorios
                int nextZap = gg.getInt("nextZapTick");
                if (tick >= nextZap) {
                    if (rand.nextFloat() < 0.30F) {
                        gg.putInt("lastZap", tick);
                        gg.putInt("lastZapClaw", rand.nextInt(3) + 1);
                        if (isClientPlayerHold) playRandomZapSound(mc);
                        gg.putInt("nextZapTick", tick + 60 + rand.nextInt(100));
                    } else {
                        gg.putInt("nextZapTick", tick + 20 + rand.nextInt(20));
                    }
                }

                // Duración del rayo aleatorio (4 ticks)
                int lastZapTick = gg.getInt("lastZap");
                if (tick - lastZapTick >= 0 && tick - lastZapTick < 4) {
                    int claw = gg.getInt("lastZapClaw");
                    if (claw == 1) l1 = true;
                    else if (claw == 2) l2 = true;
                    else if (claw == 3) l3 = true;
                }
            }
        }

        // 3. RENDERIZADO DE RAYOS DE LAS GARRAS
        int bends = 8;
        int spread = 1;
        double heightOfBend = 2.0D;
        int layerCount = 6;
        double layerSize = 0.2D;
        float intensity = 0.4F;

        if (l1) {
            poseStack.pushPose();
            poseStack.scale(0.025F, 0.025F, 0.025F);
            poseStack.translate(-73.0D, -0.5D, -14.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(120.0F));
            LightningFX.renderClassicLocal(poseStack, buffer, (long) tick * 1000L, tick, 0.0D, 0.0D, 0.0D, bends, spread, heightOfBend, layerCount, layerSize, intensity, colorRGB, 1.0F);
            LightningFX.renderClassicLocal(poseStack, buffer, (long) tick * 3000L, tick, 0.0D, -2.0D, 0.0D, 1, 0, 2.0D, layerCount - 1, 0.7D, intensity * 2.0F, colorRGB, 1.0F);
            poseStack.popPose();
        }

        if (l2) {
            poseStack.pushPose();
            poseStack.scale(0.025F, 0.025F, 0.025F);
            poseStack.translate(-73.0D, -26.0D, -0.5D);
            LightningFX.renderClassicLocal(poseStack, buffer, (long) tick * 3000L, tick, 0.0D, 0.0D, 0.0D, bends, spread, heightOfBend, layerCount, layerSize, intensity, colorRGB, 1.0F);
            LightningFX.renderClassicLocal(poseStack, buffer, (long) tick * 3000L, tick, 1.0D, -2.0D, 0.0D, 1, 0, 2.0D, layerCount - 1, 0.7D, intensity * 2.0F, colorRGB, 1.0F);
            poseStack.popPose();
        }

        if (l3) {
            poseStack.pushPose();
            poseStack.scale(0.025F, 0.025F, 0.025F);
            poseStack.translate(-73.0D, -1.5D, 14.5D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-120.0F));
            LightningFX.renderClassicLocal(poseStack, buffer, (long) tick * 3000L, tick, 0.0D, 0.0D, 0.0D, bends, spread, heightOfBend, layerCount, layerSize, intensity, colorRGB, 1.0F);
            LightningFX.renderClassicLocal(poseStack, buffer, (long) tick * 3000L, tick, 0.0D, -2.0D, 0.0D, 1, 0, 2.0D, layerCount - 1, 0.7D, intensity * 2.0F, colorRGB, 1.0F);
            poseStack.popPose();
        }

        if (l1 && l2 && l3) {
            poseStack.pushPose();
            poseStack.scale(0.025F, 0.025F, 0.025F);
            poseStack.translate(-72.2D, -8.2D, -0.5D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            LightningFX.renderClassicLocal(poseStack, buffer, (long) tick * 3000L, tick, 0.0D, -2.0D, 0.0D, 1, 0, 2.0D, layerCount - 1, 1.0D, intensity * 4.0F, colorRGB, 1.0F);
            poseStack.popPose();
        }

        // 4. RAYO DE DISPARO (Sincronizado al 100% con las garras)
        if (!dualBlocked && throwBeamActive) {
            float distVal = LightningFX.ThrowFXState.dist();
            float finalLen = Math.max(10.0F, distVal) * 19.5F;
            long beamSeed = (long) tick * 3000L;
            double throwLayerSize = layerSize * 1.3D;
            float throwIntensity = intensity * 1.15F;

            poseStack.pushPose();

            poseStack.scale(0.1F, 0.1F, 0.1F);
            poseStack.translate(-15.5D, -3.0D, -0.5D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

            if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                Vector3f atkAxis = new Vector3f(1.0F, 0.0F, 1.0F).normalize();
                float angleRad = (float) Math.toRadians(95.0F / finalLen);
                Quaternionf qar = new Quaternionf().rotateAxis(angleRad, atkAxis.x, atkAxis.y, atkAxis.z);
                poseStack.mulPose(qar);
                poseStack.mulPose(Axis.XP.rotationDegrees(-950.0F / finalLen));
            }

            LightningFX.renderClassicLocal(
                    poseStack,
                    buffer,
                    beamSeed,
                    tick,
                    0.0D, 0.0D, 0.0D,
                    bends,
                    1,
                    finalLen / bends,
                    layerCount,
                    throwLayerSize,
                    throwIntensity,
                    colorRGB,
                    1.0F
            );

            poseStack.popPose();
        }
    }

    private static void sanitizeFxTimers(CompoundTag gg, int tick) {
        // Evita que los timers se queden colgados si el mundo se reinicia o el tick cambia bruscamente
        if (gg.getInt("lastZap") > tick) gg.putInt("lastZap", tick - 10);
        if (gg.getInt("nextZapTick") > tick + 1000) gg.putInt("nextZapTick", tick + 10);
    }

    private void playRandomZapSound(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        // Selección aleatoria del sonido de la carpeta de sonidos registrados
        SoundEvent sound = switch (rand.nextInt(4)) {
            case 0 -> SoundEventsRegistry.SUPERPHYS_ZAP1.get();
            case 1 -> SoundEventsRegistry.SUPERPHYS_ZAP2.get();
            case 2 -> SoundEventsRegistry.SUPERPHYS_ZAP3.get();
            default -> SoundEventsRegistry.SUPERPHYS_ZAP4.get();
        };

        // Reproducción local (solo para el cliente que renderiza)
        mc.level.playLocalSound(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                sound, SoundSource.PLAYERS,
                0.5F, // Volumen
                0.9F + rand.nextFloat() * 0.2F, // Pitch aleatorio para que no canse
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
