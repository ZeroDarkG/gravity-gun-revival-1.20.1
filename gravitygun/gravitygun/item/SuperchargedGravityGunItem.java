package com.zerokg2004.gravitygun.gravitygun.item;

import com.zerokg2004.gravitygun.gravitygun.handler.HeldObjectTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SuperchargedGravityGunItem extends GravityGunItem {

    public SuperchargedGravityGunItem(Properties properties) {
        super(properties);
    }

    // ✅ igual que el jar
    @Override
    protected float getThrowStrength(ItemStack stack) {
        return 3.0F;
    }

    // ✅ igual que el jar
    @Override
    protected boolean isSupercharged(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    // ✅ igual que el jar (color 58862)
    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Supercharged Gravity Gun")
                .withStyle(style -> style.withColor(58862));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public GravityGunState getVisualState(Player player, ItemStack stack) {
        if (!ItemStack.isSameItemSameTags(player.getMainHandItem(), stack)) {
            return GravityGunState.CLOSED;
        }

        if (HeldObjectTracker.isHolding(player.getUUID())) {
            return GravityGunState.HOLDING;
        }

        HitResult result = Minecraft.getInstance().hitResult;

        if (result instanceof EntityHitResult ehr) {
            Entity target = ehr.getEntity();
            return (target instanceof LivingEntity living && living.isAlive())
                    ? GravityGunState.AIMING
                    : GravityGunState.CLOSED;
        }

        if (result instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = player.level().getBlockState(pos);

            boolean canGrab =
                    !state.isAir()
                            && state.getDestroySpeed(player.level(), pos) >= 0.0F
                            && state.canOcclude()
                            && player.level().getBlockEntity(pos) == null;

            return canGrab ? GravityGunState.AIMING : GravityGunState.CLOSED;
        }

        return GravityGunState.CLOSED;
    }
}