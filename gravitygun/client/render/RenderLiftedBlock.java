package com.zerokg2004.gravitygun.gravitygun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zerokg2004.gravitygun.gravitygun.entity.EntityLiftedBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

public class RenderLiftedBlock extends EntityRenderer<EntityLiftedBlock> {
    private final BlockRenderDispatcher blockRenderer;

    public RenderLiftedBlock(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(EntityLiftedBlock entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockState state = entity.getBlockState();
        if (state == null || state.isAir()) return;

        poseStack.pushPose();

        // 1. EL TRUCO DE ICHUN: Centrado perfecto
        // Movemos el "pivote" al centro exacto del cubo para que rote sobre su eje
        double d0 = state.getShape(entity.level(), entity.blockPosition()).bounds().maxY;
        poseStack.translate(0.0D, 0.5D, 0.0D);

        // 2. ROTACIÓN DINÁMICA
        // Interpolamos el Yaw y Pitch para que no haya saltos visuales entre ticks
        // Nota: Asegúrate de tener ROT_YAW y ROT_PITCH en tu clase EntityLiftedBlock
        float renderYaw = Mth.lerp(partialTicks, entity.prevRotYaw, entity.rotYaw);
        float renderPitch = Mth.lerp(partialTicks, entity.prevRotPitch, entity.rotPitch);

        poseStack.mulPose(Axis.YP.rotationDegrees(renderYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(renderPitch));

        // Volvemos a bajar el bloque después de rotarlo para que el render coincida con la Hitbox
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        // 3. RENDERIZADO DEL BLOQUE
        this.blockRenderer.renderSingleBlock(
                state,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityLiftedBlock entity) {
        // En Minecraft moderno, las texturas de bloques usan el atlas de bloques
        return InventoryMenu.BLOCK_ATLAS;
    }
}