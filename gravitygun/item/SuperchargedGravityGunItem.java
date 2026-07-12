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

    @Override
    public Component getName(ItemStack stack) {
        // Usamos translatable para que use los nombres del archivo .lang
        // y aplicamos el color de nuestro nuevo método getNameColor
        return Component.translatable(this.getDescriptionId(stack))
                .withStyle(style -> style.withColor(getNameColor(stack)));
    }

    @Override
    public GravityGunState getVisualState(Player player, ItemStack stack) {
        // Lógica segura (se puede ejecutar en cualquier lado)
        if (!ItemStack.isSameItemSameTags(player.getMainHandItem(), stack)) {
            return GravityGunState.CLOSED;
        }

        boolean isHolding = player.level().isClientSide
                ? HeldObjectTracker.isHoldingClient(player.getUUID())
                : HeldObjectTracker.isHolding(player.getUUID());

        if (isHolding) {
            return GravityGunState.HOLDING;
        }

        if (player.level().isClientSide) {
            return getClientSideState();
        }

        return GravityGunState.CLOSED;
    }

    @OnlyIn(Dist.CLIENT)
    private GravityGunState getClientSideState() {
        HitResult result = Minecraft.getInstance().hitResult;

        if (result instanceof EntityHitResult ehr) {
            Entity target = ehr.getEntity();
            return (target instanceof LivingEntity living && living.isAlive())
                    ? GravityGunState.AIMING
                    : GravityGunState.CLOSED;
        }

        if (result instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            Level level = Minecraft.getInstance().level;
            if (level == null) return GravityGunState.CLOSED;

            BlockState state = level.getBlockState(pos);

            // Verificamos si se puede agarrar el bloque
            boolean canGrab = !state.isAir()
                    && state.getDestroySpeed(level, pos) >= 0.0F
                    && state.canOcclude()
                    && level.getBlockEntity(pos) == null;

            return canGrab ? GravityGunState.AIMING : GravityGunState.CLOSED;
        }

        return GravityGunState.CLOSED;
    }

    private static int getNameColor(ItemStack stack) {
        String id = stack.getDescriptionId();

        // Buscamos los IDs exactos que registramos
        if (id.endsWith("red_supercharged_gravity_gun")) return 0xFF0000;
        if (id.endsWith("blue_supercharged_gravity_gun")) return 0x0000FF;
        if (id.endsWith("green_supercharged_gravity_gun")) return 0x00FF00;
        if (id.endsWith("orange_supercharged_gravity_gun")) return 0xFFA500;
        if (id.endsWith("yellow_supercharged_gravity_gun")) return 0xFFFF00;
        if (id.endsWith("purple_supercharged_gravity_gun")) return 0x800080;
        if (id.endsWith("white_supercharged_gravity_gun")) return 0xFFFFFF;
        if (id.endsWith("cyan_supercharged_gravity_gun")) return 0x66FFCC;

        // Color Cyan por defecto para la Supercharged normal
        return 0x55FFFF;
    }
}
