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
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ModelGravityGunDetailed extends Model {

    // ✅ Agregamos el LAYER_LOCATION para usarlo en el renderizador.
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation("gravitygun", "gravity_gun_detailed"), "main");

    public ModelPart corefrontbottom;
    public ModelPart stock1;
    public ModelPart clawrightbase;
    public ModelPart corebackleft;
    public ModelPart stock4;
    public ModelPart stock3;
    public ModelPart corefrontmain;
    public ModelPart stock2;
    public ModelPart coreleft;
    public ModelPart frontleft;
    public ModelPart Mainbody;
    public ModelPart bodyrightback;
    public ModelPart gaugethingyL;
    public ModelPart frontright;
    public ModelPart clawtopthick;
    public ModelPart clawleftthick;
    public ModelPart coreright;
    public ModelPart corebottom;
    public ModelPart frontbottom;
    public ModelPart bodyleftback;
    public ModelPart clawrightthick;
    public ModelPart corebackmain;
    public ModelPart coretop;
    public ModelPart bodyrighttop;
    public ModelPart Lstick2;
    public ModelPart Lstick3;
    public ModelPart Lstick1;
    public ModelPart corebackright;
    public ModelPart bodyleftmain;
    public ModelPart frontmain;
    public ModelPart bodyrightmain;
    public ModelPart bodyrightbottom;
    public ModelPart bodyrightfront;
    public ModelPart bodylefttop;
    public ModelPart bodyleftfront;
    public ModelPart corefrontright;
    public ModelPart bodyleftbottom;
    public ModelPart sidehandleL;
    public ModelPart corefronttop;
    public ModelPart corefrontleft;
    public ModelPart fronttop;
    public ModelPart corebackbottom;
    public ModelPart Rstick1;
    public ModelPart Rstick2;
    public ModelPart Rstick3;
    public ModelPart corebacktop;
    public ModelPart coremain;
    public ModelPart clawleftbase;
    public ModelPart clawtopbase;
    public ModelPart clawtopthin;
    public ModelPart clawtopend;
    public ModelPart clawleftthin;
    public ModelPart clawleftend;
    public ModelPart clawrightend;
    public ModelPart clawrightthin;

    public ModelGravityGunDetailed(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.corefrontbottom = root.getChild("corefrontbottom");
        this.stock1 = root.getChild("stock1");

        // Bases de las garras:
        this.clawleftbase = root.getChild("clawleftbase");
        this.clawrightbase = root.getChild("clawrightbase");
        this.clawtopbase = root.getChild("clawtopbase");

        // Claw thick parts (obténlos desde las bases, NO desde el root)
        this.clawleftthick = root.getChild("clawleftthick");
        this.clawrightthick = root.getChild("clawrightthick");
        this.clawtopthick = root.getChild("clawtopthick");

        // Claw thin parts:
        this.clawleftthin = clawleftthick.getChild("clawleftthin");
        this.clawrightthin = clawrightthick.getChild("clawrightthin");
        this.clawtopthin = clawtopthick.getChild("clawtopthin");

        // Claw end parts:
        this.clawleftend = clawleftthick.getChild("clawleftend");
        this.clawrightend = clawrightthick.getChild("clawrightend");
        this.clawtopend = clawtopthick.getChild("clawtopend");

        // Otras partes:
        this.corebackleft = root.getChild("corebackleft");
        this.stock4 = root.getChild("stock4");
        this.stock3 = root.getChild("stock3");
        this.corefrontmain = root.getChild("corefrontmain");
        this.stock2 = root.getChild("stock2");
        this.coreleft = root.getChild("coreleft");
        this.frontleft = root.getChild("frontleft");
        this.Mainbody = root.getChild("Mainbody");
        this.bodyrightback = root.getChild("bodyrightback");
        this.gaugethingyL = root.getChild("gaugethingyL");
        this.frontright = root.getChild("frontright");
        this.coreright = root.getChild("coreright");
        this.corebottom = root.getChild("corebottom");
        this.frontbottom = root.getChild("frontbottom");
        this.bodyleftback = root.getChild("bodyleftback");
        this.corebackmain = root.getChild("corebackmain");
        this.coretop = root.getChild("coretop");
        this.bodyrighttop = root.getChild("bodyrighttop");
        this.Lstick2 = root.getChild("Lstick2");
        this.Lstick3 = root.getChild("Lstick3");
        this.Lstick1 = root.getChild("Lstick1");
        this.corebackright = root.getChild("corebackright");
        this.bodyleftmain = root.getChild("bodyleftmain");
        this.frontmain = root.getChild("frontmain");
        this.bodyrightmain = root.getChild("bodyrightmain");
        this.bodyrightbottom = root.getChild("bodyrightbottom");
        this.bodyrightfront = root.getChild("bodyrightfront");
        this.bodylefttop = root.getChild("bodylefttop");
        this.bodyleftfront = root.getChild("bodyleftfront");
        this.corefrontright = root.getChild("corefrontright");
        this.bodyleftbottom = root.getChild("bodyleftbottom");
        this.sidehandleL = root.getChild("sidehandleL");
        this.corefronttop = root.getChild("corefronttop");
        this.corefrontleft = root.getChild("corefrontleft");
        this.fronttop = root.getChild("fronttop");
        this.corebackbottom = root.getChild("corebackbottom");
        this.Rstick1 = root.getChild("Rstick1");
        this.Rstick2 = root.getChild("Rstick2");
        this.Rstick3 = root.getChild("Rstick3");
        this.corebacktop = root.getChild("corebacktop");
        this.coremain = root.getChild("coremain");

        // Ajustes de rotación:
        this.sidehandleL.xRot = 0.0872665F;
        this.stock2.zRot = 0.3839724F;
        this.bodyleftmain.xRot = 0.0872665F;
        this.bodyleftback.xRot = 0.0872665F;
        this.bodyleftbottom.xRot = 0.0872665F;
        this.bodyleftfront.xRot = 0.0872665F;
        this.bodylefttop.xRot = 0.0872665F;
        this.bodyrightmain.xRot = -0.0872665F;
        this.bodyrightback.xRot = -0.0872665F;
        this.bodyrightbottom.xRot = -0.0872665F;
        this.bodyrightfront.xRot = -0.0872665F;
        this.bodyrighttop.xRot = -0.0872665F;
        this.clawrightend.zRot = -(float) Math.PI / 2F;
        this.clawleftend.zRot = -(float) Math.PI / 2F;
        this.clawtopthick.zRot = -0.34906584F;
        this.clawrightthin.zRot = -(float) Math.PI / 2F;
        this.clawleftbase.xRot = -0.8726646F;  // ya estaba correcto
        this.clawrightbase.xRot = 0.8726646F;  // añadido para inclinar la garra derecha igual al modelo original
        this.clawleftthin.zRot = -(float) Math.PI / 2F;
    }

    @Override
    public void renderToBuffer(PoseStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        stock1.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        stock4.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        stock3.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        stock2.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        frontleft.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        Mainbody.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyrightback.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        gaugethingyL.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        frontright.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        frontbottom.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyleftback.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyrighttop.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        Lstick2.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        Lstick3.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        Lstick1.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyleftmain.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        frontmain.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyrightmain.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyrightbottom.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyrightfront.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodylefttop.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyleftfront.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bodyleftbottom.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        sidehandleL.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        fronttop.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        Rstick1.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        Rstick2.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        Rstick3.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        corefrontbottom.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corebackleft.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corefrontmain.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        coreleft.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        coreright.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corebottom.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corebackmain.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        coretop.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corebackright.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corebacktop.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        coremain.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corefronttop.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corefrontleft.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corefrontright.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        corebackbottom.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        // Renderiza solo las partes que forman la garra real (no la base)
        clawtopthick.render(matrixStack, buffer, packedLight, packedOverlay);
        clawleftthick.render(matrixStack, buffer, packedLight, packedOverlay);
        clawrightthick.render(matrixStack, buffer, packedLight, packedOverlay);
        clawtopbase.render(matrixStack, buffer, packedLight, packedOverlay);
        clawleftbase.render(matrixStack, buffer, packedLight, packedOverlay);
        clawrightbase.render(matrixStack, buffer, packedLight, packedOverlay);
    }

    public void setClawOpen(float prog) {
        float eased = Mth.clamp(prog, 0.0F, 1.0F);
        eased = eased * eased * (3.0F - 2.0F * eased);

        float closedAngle = 60.0F;
        float openAngle = 20.0F;
        float angle = Mth.lerp(eased, closedAngle, openAngle);

        this.clawtopthick.zRot = (float) Math.toRadians(-angle);
        this.clawleftthick.xRot = (float) Math.toRadians(angle);
        this.clawrightthick.xRot = (float) Math.toRadians(angle);
    }

    public void setRotateAngle(ModelPart part, float x, float y, float z) {
        part.xRot = x;
        part.yRot = y;
        part.zRot = z;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("stock1", CubeListBuilder.create().texOffs(26, 8).addBox(-2.0F, -6.5F, -2.5F, 2, 6, 5), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("stock2", CubeListBuilder.create().texOffs(28, 28).addBox(-3.0F, -2.5F, -1.0F, 5, 2, 2), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("stock3", CubeListBuilder.create().texOffs(18, 27).addBox(2.0F, -2.0F, -1.0F, 3, 3, 2), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("stock4", CubeListBuilder.create().texOffs(6, 26).addBox(5.0F, -2.0F, -1.0F, 4, 4, 2), PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("corefrontmain", CubeListBuilder.create().texOffs(78, 0).addBox(-23.0F, -7.0F, -3.0F, 1, 6, 6), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corefrontbottom", CubeListBuilder.create().texOffs(92, 0).addBox(-23.0F, -1.0F, -2.0F, 1, 1, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corefronttop", CubeListBuilder.create().texOffs(92, 0).addBox(-23.0F, -8.0F, -2.0F, 1, 1, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corefrontleft", CubeListBuilder.create().texOffs(102, 0).addBox(-23.0F, -6.0F, -4.0F, 1, 4, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corefrontright", CubeListBuilder.create().texOffs(102, 0).addBox(-23.0F, -6.0F, 3.0F, 1, 4, 1), PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("corebackmain", CubeListBuilder.create().texOffs(78, 20).addBox(-12.0F, -7.0F, -3.0F, 4, 6, 6), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corebackbottom", CubeListBuilder.create().texOffs(98, 27).addBox(-12.0F, -1.0F, -2.0F, 4, 1, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corebacktop", CubeListBuilder.create().texOffs(98, 27).addBox(-12.0F, -8.0F, -2.0F, 4, 1, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corebackleft", CubeListBuilder.create().texOffs(114, 27).addBox(-12.0F, -6.0F, -4.0F, 4, 4, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corebackright", CubeListBuilder.create().texOffs(114, 27).addBox(-12.0F, -6.0F, 3.0F, 4, 4, 1), PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("coremain", CubeListBuilder.create().texOffs(26, 0).addBox(-22.0F, -6.0F, -2.0F, 10, 4, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("coretop", CubeListBuilder.create().texOffs(54, 0).addBox(-22.0F, -7.0F, -1.0F, 10, 1, 2), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("corebottom", CubeListBuilder.create().texOffs(54, 0).addBox(-22.0F, -2.0F, -1.0F, 10, 1, 2), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("coreleft", CubeListBuilder.create().texOffs(54, 3).addBox(-22.0F, -5.0F, -3.0F, 10, 2, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("coreright", CubeListBuilder.create().texOffs(54, 3).addBox(-22.0F, -5.0F, 2.0F, 10, 2, 1), PartPose.offset(0.0F, 0.0F, 0.0F));

        // Bases
        PartDefinition clawLeftBase = root.addOrReplaceChild("clawleftbase",
                CubeListBuilder.create().texOffs(106, 0)
                        .addBox(-24.0F, -1.0F, -3.5F, 3, 3, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition clawRightBase = root.addOrReplaceChild("clawrightbase",
                CubeListBuilder.create().texOffs(106, 0)
                        .addBox(-24.0F, -1.0F, 2.5F, 3, 3, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition clawTopBase = root.addOrReplaceChild("clawtopbase",
                CubeListBuilder.create().texOffs(106, 0)
                        .addBox(-24.0F, -8.5F, -0.5F, 3, 2, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F));

// Claw thick parts (con rotaciones)
        PartDefinition clawLeftThick = root.addOrReplaceChild("clawleftthick",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-0.5F, -0.7F, -0.3F, 1, 1, 6),
                PartPose.offsetAndRotation(-21.5F, -1.3F, -3.0F,
                        0.34906584F, -2.443461F, -1.3962634F));

        PartDefinition clawRightThick = root.addOrReplaceChild("clawrightthick",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-0.5F, -0.7F, -0.3F, 1, 1, 6),
                PartPose.offsetAndRotation(-21.5F, -1.3F, 3.0F,
                        0.34906584F, -0.6981317F, -1.3962634F));

        PartDefinition clawTopThick = root.addOrReplaceChild("clawtopthick",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-1.0F, -4.8F, -0.5F, 1, 5, 1),
                PartPose.offsetAndRotation(-21.5F, -8.0F, 0.0F,
                        0.0F, 0.0F, -0.34906584F));

// Claw thin parts
        clawLeftThick.addOrReplaceChild("clawleftthin",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(1.2F, 0.0F, 0.0F, 1, 0, 6),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                        0.0F, 0.0F, -(float) Math.PI / 2F));

        clawRightThick.addOrReplaceChild("clawrightthin",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(1.2F, 0.0F, 0.0F, 1, 0, 6),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                        0.0F, 0.0F, -(float) Math.PI / 2F));

        clawTopThick.addOrReplaceChild("clawtopthin",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-2.5F, -4.8F, 0.0F, 1, 5, 0),
                PartPose.offset(0.0F, 0.0F, 0.0F));

// Claw ends
        clawLeftThick.addOrReplaceChild("clawleftend",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(0.7F, -0.5F, 5.7F, 5, 1, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                        0.0F, 0.0F, -(float) Math.PI / 2F));

        clawRightThick.addOrReplaceChild("clawrightend",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(0.7F, -0.5F, 5.7F, 5, 1, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                        0.0F, 0.0F, -(float) Math.PI / 2F));

        clawTopThick.addOrReplaceChild("clawtopend",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-6.0F, -5.8F, -0.5F, 5, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("Mainbody", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -7.5F, -3.5F, 6, 8, 7), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("frontmain", CubeListBuilder.create().texOffs(114, 0).addBox(-25.0F, -5.5F, -1.5F, 2, 3, 3), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("frontleft", CubeListBuilder.create().texOffs(134, 0).addBox(-25.0F, -5.5F, -2.5F, 2, 3, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("frontright", CubeListBuilder.create().texOffs(134, 0).addBox(-25.0F, -5.5F, 1.5F, 2, 3, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fronttop", CubeListBuilder.create().texOffs(124, 0).addBox(-25.0F, -6.5F, -1.5F, 2, 1, 3), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("frontbottom", CubeListBuilder.create().texOffs(124, 0).addBox(-25.0F, -2.5F, -1.5F, 2, 1, 3), PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("bodyleftmain", CubeListBuilder.create().texOffs(42, 23).addBox(-8.0F, -6.0F, -6.486667F, 5, 5, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodylefttop", CubeListBuilder.create().texOffs(42, 18).addBox(-7.0F, -7.0F, -6.486667F, 3, 1, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodyleftbottom", CubeListBuilder.create().texOffs(42, 18).addBox(-7.0F, -1.0F, -6.486667F, 3, 1, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodyleftfront", CubeListBuilder.create().texOffs(42, 11).addBox(-9.0F, -5.0F, -6.486667F, 1, 3, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodyleftback", CubeListBuilder.create().texOffs(42, 11).addBox(-3.0F, -5.0F, -6.486667F, 1, 3, 4), PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("bodyrightmain", CubeListBuilder.create().texOffs(42, 23).addBox(-8.0F, -6.0F, 2.5F, 5, 5, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodyrighttop", CubeListBuilder.create().texOffs(42, 18).addBox(-7.0F, -7.0F, 2.5F, 3, 1, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodyrightbottom", CubeListBuilder.create().texOffs(42, 18).addBox(-7.0F, -1.0F, 2.5F, 3, 1, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodyrightfront", CubeListBuilder.create().texOffs(42, 11).addBox(-9.0F, -5.0F, 2.5F, 1, 3, 4), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodyrightback", CubeListBuilder.create().texOffs(42, 11).addBox(-3.0F, -5.0F, 2.5F, 1, 3, 4), PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("gaugethingyL", CubeListBuilder.create().texOffs(0, 28).addBox(-3.5F, -7.0F, -6.0F, 1, 2, 2), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("sidehandleL", CubeListBuilder.create().texOffs(60, 23).addBox(-7.0F, -5.0F, -12.48667F, 3, 3, 6), PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("Lstick1", CubeListBuilder.create().texOffs(0, 15).addBox(-22.0F, -8.0F, -2.0F, 10, 1, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Lstick2", CubeListBuilder.create().texOffs(0, 15).addBox(-22.0F, -6.0F, -4.0F, 10, 1, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Lstick3", CubeListBuilder.create().texOffs(0, 15).addBox(-22.0F, -3.0F, -4.0F, 10, 1, 1), PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("Rstick1", CubeListBuilder.create().texOffs(0, 15).addBox(-22.0F, -8.0F, 1.0F, 10, 1, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Rstick2", CubeListBuilder.create().texOffs(0, 15).addBox(-22.0F, -6.0F, 3.0F, 10, 1, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Rstick3", CubeListBuilder.create().texOffs(0, 15).addBox(-22.0F, -3.0F, 3.0F, 10, 1, 1), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 256, 32);
    }
}
