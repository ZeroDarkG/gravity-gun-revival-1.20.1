package com.zerokg2004.gravitygun.gravitygun.registry;

import com.zerokg2004.gravitygun.gravitygun.Gravitygun;
import com.zerokg2004.gravitygun.gravitygun.entity.EntityLiftedBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Gravitygun.MODID);

    public static final RegistryObject<EntityType<EntityLiftedBlock>> LIFTED_BLOCK =
            ENTITY_TYPES.register("lifted_block", () ->
                    EntityType.Builder.<EntityLiftedBlock>of(EntityLiftedBlock::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(new ResourceLocation(Gravitygun.MODID, "lifted_block").toString()));
}