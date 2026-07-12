package com.zerokg2004.gravitygun.gravitygun.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ModelGravityGunSimple extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation("gravitygun", "gravity_gun_simple"), "main");

    private final ModelPart root;

    public ModelGravityGunSimple(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    /**
     * Este método lo busca Forge para registrar las capas del modelo.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("corething3", CubeListBuilder.create().texOffs(0, 14).addBox(-11.0F, 0.0F, 0.0F, 6, 1, 1), PartPose.offset(3.0F, 21.0F, 1.0F));
        root.addOrReplaceChild("frontsmall", CubeListBuilder.create().texOffs(0, 26).addBox(0.0F, 0.0F, 0.0F, 1, 3, 3), PartPose.offset(-10.0F, 20.0F, -2.0F));
        root.addOrReplaceChild("frontbig", CubeListBuilder.create().texOffs(38, 0).addBox(-18.0F, 0.0F, 0.0F, 1, 5, 5), PartPose.offset(9.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("gripback", CubeListBuilder.create().texOffs(28, 20).addBox(10.0F, 0.0F, 0.0F, 5, 2, 2), PartPose.offset(-7.0F, 22.0F, -1.5F));
        root.addOrReplaceChild("corething2", CubeListBuilder.create().texOffs(0, 10).addBox(-11.0F, 0.0F, 0.0F, 6, 1, 1), PartPose.offset(3.0F, 19.0F, 1.0F));
        root.addOrReplaceChild("claw1", CubeListBuilder.create().texOffs(0, 20).addBox(-21.0F, 0.0F, 0.0F, 3, 1, 1), PartPose.offset(9.0F, 17.5F, -1.0F));
        root.addOrReplaceChild("corething1", CubeListBuilder.create().texOffs(0, 12).addBox(-11.0F, 0.0F, 0.0F, 6, 1, 1), PartPose.offset(3.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("gripside1", CubeListBuilder.create().texOffs(28, 14).addBox(0.0F, 0.0F, -12.0F, 2, 2, 4), PartPose.offset(-0.5F, 20.5F, 3.0F));
        root.addOrReplaceChild("clawbottom3", CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, 0.0F, 1, 1, 1), PartPose.offset(-9.0F, 23.0F, 2.0F));
        root.addOrReplaceChild("clawbottom1", CubeListBuilder.create().texOffs(0, 18).addBox(-18.0F, 0.0F, 0.0F, 1, 1, 1), PartPose.offset(9.0F, 18.0F, -1.0F));
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 5, 5, 5), PartPose.offset(-2.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("claw2", CubeListBuilder.create().texOffs(0, 24).addBox(-21.0F, 0.0F, 0.0F, 3, 1, 1), PartPose.offset(9.0F, 23.5F, -4.5F));
        root.addOrReplaceChild("clawbottom2", CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, 0.0F, 1, 1, 1), PartPose.offset(-9.0F, 23.0F, -4.0F));
        root.addOrReplaceChild("baseleft_box", CubeListBuilder.create().texOffs(40, 15).addBox(0.0F, 0.0F, 0.0F, 3, 3, 2), PartPose.offset(-1.0F, 20.0F, -5.0F));
        root.addOrReplaceChild("corething4", CubeListBuilder.create().texOffs(0, 16).addBox(-11.0F, 0.0F, 0.0F, 6, 1, 1), PartPose.offset(3.0F, 21.0F, -3.0F));
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(10, 24).addBox(-11.0F, 0.0F, 0.0F, 6, 4, 4), PartPose.offset(3.0F, 19.5F, -2.5F));
        root.addOrReplaceChild("claw3", CubeListBuilder.create().texOffs(0, 22).addBox(-21.0F, 0.0F, 0.0F, 3, 1, 1), PartPose.offset(9.0F, 23.5F, 2.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}