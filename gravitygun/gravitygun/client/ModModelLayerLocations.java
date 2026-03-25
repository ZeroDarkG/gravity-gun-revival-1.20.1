package com.zerokg2004.gravitygun.gravitygun.client;

import com.zerokg2004.gravitygun.gravitygun.Gravitygun;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModModelLayerLocations {
    public static final ModelLayerLocation GRAVITY_GUN_SIMPLE =
            new ModelLayerLocation(new ResourceLocation(Gravitygun.MODID, "gravity_gun_simple"), "main");

    public static final ModelLayerLocation GRAVITY_GUN_DETAILED =
            new ModelLayerLocation(new ResourceLocation(Gravitygun.MODID, "gravity_gun_detailed"), "main");
}