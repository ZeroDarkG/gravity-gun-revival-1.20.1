package com.zerokg2004.gravitygun.gravitygun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zerokg2004.gravitygun.gravitygun.entity.EntityLiftedBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class RenderLiftedBlock extends EntityRenderer<EntityLiftedBlock> {

    public RenderLiftedBlock(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityLiftedBlock entity,
                       float entityYaw,
                       float partialTicks,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight) {

        BlockState state = entity.getBlockState();
        // Evitar NPEs y no renderizar aire
        if (state == null || state.isAir()) {
            return;
        }

        poseStack.pushPose();

        // Centrar el bloque en la entidad (modelo de bloque va de 0..1)
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        // Opcional: llamar al super (por si se añaden cosas en el futuro)
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityLiftedBlock entity) {
        // El bloque se renderiza con sus propias texturas
        return null;
    }
}