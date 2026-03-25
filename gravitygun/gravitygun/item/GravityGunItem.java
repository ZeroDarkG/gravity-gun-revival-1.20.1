package com.zerokg2004.gravitygun.gravitygun.item;

import com.zerokg2004.gravitygun.gravitygun.client.render.GravityGunRenderer;
import com.zerokg2004.gravitygun.gravitygun.entity.EntityLiftedBlock;
import com.zerokg2004.gravitygun.gravitygun.handler.HeldObjectTracker;
import com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class GravityGunItem extends Item {

    public enum GravityGunState { CLOSED, AIMING, HOLDING }

    private static final String TAG_STATE = "GunState";
    private static final int TOOHEAVY_COOLDOWN_TICKS = 6;

    private static final Map<UUID, GravityGunState> pendingStateMap = new HashMap<>();
    private static final Map<UUID, Integer> pendingTickMap = new HashMap<>();

    @OnlyIn(Dist.CLIENT)
    private static final Map<UUID, GravityGunState> lastSoundStateMap = new HashMap<>();

    // =========================
    // RANGOS (normal vs supercharged)
    // =========================
    private static final double RANGE_NORMAL = 5.0D;
    private static final double RANGE_SUPER  = 12.0D;

    public GravityGunItem(Properties properties) {
        super(properties);
    }

    // =========================
    // DUAL WIELD BLOCK
    // =========================
    private static boolean isAnyGravityGun(ItemStack s) {
        return s.getItem() instanceof GravityGunItem;
    }

    private static boolean isDualWieldBlocked(Player player) {
        if (player == null) return false;
        return isAnyGravityGun(player.getMainHandItem()) && isAnyGravityGun(player.getOffhandItem());
    }

    protected float getThrowStrength(ItemStack stack) {
        return 1.75F;
    }

    protected boolean isSupercharged(ItemStack stack) {
        return this instanceof SuperchargedGravityGunItem;
    }

    private double getRange(ItemStack stack) {
        return isSupercharged(stack) ? RANGE_SUPER : RANGE_NORMAL;
    }

    private static int getNameColor(ItemStack stack) {
        String id = stack.getDescriptionId();
        if (id.endsWith("red_gravity_gun")) return 0xFF0000;
        if (id.endsWith("blue_gravity_gun")) return 0x0000FF;
        if (id.endsWith("green_gravity_gun")) return 0x00FF00;
        if (id.endsWith("orange_gravity_gun")) return 0xFFA500;
        if (id.endsWith("yellow_gravity_gun")) return 0xFFFF00;
        if (id.endsWith("purple_gravity_gun")) return 0x800080;
        return 0xFFA500;
    }

    public static void setGunState(ItemStack stack, GravityGunState state) {
        stack.getOrCreateTag().putString(TAG_STATE, state.name());
    }

    public static GravityGunState getGunState(ItemStack stack) {
        try {
            return GravityGunState.valueOf(stack.getOrCreateTag().getString(TAG_STATE));
        } catch (Exception e) {
            return GravityGunState.CLOSED;
        }
    }

    // ✅ filtro para evitar que “apunte” a hojas/flores/plantas
    private static boolean isLiftableBlock(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;

        if (state.getCollisionShape(level, pos).isEmpty()) return false;
        if (state.canBeReplaced()) return false;

        return true;
    }

    // ✅ distancia “base” al agarrar el bloque (depende de dónde lo agarraste)
    private static double computeBaseHoldDistance(ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 center = Vec3.atCenterOf(pos);
        return eye.distanceTo(center);
    }

    // ============================================================
    // AGARRAR BLOQUES DESDE LEJOS (sin selección/outline)
    // ============================================================
    private boolean tryGrabBlockFromCrosshair(ServerPlayer player, ItemStack stack) {
        Level level = player.level();
        double range = getRange(stack);

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();

        final double step = 0.25D;

        for (double t = 0.5D; t <= range; t += step) {
            Vec3 p = eye.add(look.scale(t));
            BlockPos pos = BlockPos.containing(p);
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) continue;

            if (isLiftableBlock(level, pos, state) && state.canOcclude()) {

                double baseDist = computeBaseHoldDistance(player, pos);

                EntityLiftedBlock liftedBlock = new EntityLiftedBlock(level, player, state, pos, baseDist);
                liftedBlock.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);

                level.addFreshEntity(liftedBlock);
                level.removeBlock(pos, false);

                HeldObjectTracker.hold(player.getUUID(), liftedBlock);
                player.getCooldowns().addCooldown(this, 10);
                return true;
            }

            if (!state.getCollisionShape(level, pos).isEmpty() && !state.canBeReplaced()) {
                return false;
            }
        }

        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public GravityGunState getVisualState(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof GravityGunItem)) return GravityGunState.CLOSED;

        // ✅ dual wield: cerrado siempre (y sin sonidos)
        if (isDualWieldBlocked(player)) {
            if (player != null) {
                UUID uuid = player.getUUID();
                lastSoundStateMap.put(uuid, GravityGunState.CLOSED);
                pendingStateMap.remove(uuid);
                pendingTickMap.remove(uuid);
            }
            return GravityGunState.CLOSED;
        }

        // ✅ ahora aceptamos mainhand O offhand
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand  = player.getOffhandItem();

        boolean inMain = ItemStack.isSameItemSameTags(mainHand, stack);
        boolean inOff  = ItemStack.isSameItemSameTags(offHand, stack);

        if (!inMain && !inOff) return GravityGunState.CLOSED;

        UUID uuid = player.getUUID();

        GravityGunState detected;
        if (HeldObjectTracker.isHolding(uuid)) {
            detected = GravityGunState.HOLDING;
        } else {
            double range = getRange(stack);
            HitResult result = player.pick(range, 0.0F, false);

            if (result instanceof EntityHitResult ehr) {
                Entity e = ehr.getEntity();
                detected = (e instanceof LivingEntity l && l.isAlive()) ? GravityGunState.AIMING : GravityGunState.CLOSED;
            } else if (result instanceof BlockHitResult bhr) {
                BlockPos pos = bhr.getBlockPos();
                BlockState state = player.level().getBlockState(pos);
                detected = isLiftableBlock(player.level(), pos, state) ? GravityGunState.AIMING : GravityGunState.CLOSED;
            } else {
                detected = GravityGunState.CLOSED;
            }
        }

        // ✅ IMPORTANTÍSIMO:
        // sonidos open/close SOLO desde mainhand para evitar duplicación.
        if (!inMain) {
            return detected;
        }

        GravityGunState current = lastSoundStateMap.getOrDefault(uuid, GravityGunState.CLOSED);
        GravityGunState pending = pendingStateMap.get(uuid);
        int ticks = pendingTickMap.getOrDefault(uuid, -1);

        if (pending != null) {
            if (detected != pending) {
                pendingStateMap.remove(uuid);
                pendingTickMap.remove(uuid);
            } else {
                if (ticks <= 0) {
                    if (!(stack.getItem() instanceof SuperchargedGravityGunItem)) {
                        if (pending == GravityGunState.AIMING && SoundEventsRegistry.CLAWS_OPEN.isPresent()) {
                            player.playSound((SoundEvent) SoundEventsRegistry.CLAWS_OPEN.get(), 1.0F, 1.0F);
                        } else if (pending == GravityGunState.CLOSED && SoundEventsRegistry.CLAWS_CLOSE.isPresent()) {
                            player.playSound((SoundEvent) SoundEventsRegistry.CLAWS_CLOSE.get(), 1.0F, 1.0F);
                        }
                    }
                    lastSoundStateMap.put(uuid, pending);
                    pendingStateMap.remove(uuid);
                    pendingTickMap.remove(uuid);
                    return pending;
                } else {
                    pendingTickMap.put(uuid, ticks - 1);
                }
            }
            return current;
        }

        if (detected != current) {
            pendingStateMap.put(uuid, detected);
            pendingTickMap.put(uuid, 20);
        }

        return current;
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
        return Component.translatable(this.getDescriptionId(stack))
                .withStyle(style -> style.withColor(getNameColor(stack)));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // intencionalmente vacío
    }

    // ======================
    // CLICK DERECHO (aire) = agarrar / soltar
    // ======================
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ✅ Si hay gravity gun en ambas manos: NO se puede usar ninguna
        if (isDualWieldBlocked(player)) {
            return InteractionResultHolder.fail(stack);
        }

        UUID playerId = player.getUUID();

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.consume(stack);
        }

        if (HeldObjectTracker.isHolding(playerId)) {
            HeldObjectTracker.release(playerId);
            player.getCooldowns().addCooldown(this, 2);
            return InteractionResultHolder.success(stack);
        }

        ServerPlayer sp = (ServerPlayer) player;

        boolean grabbedEntity = HeldObjectTracker.tryGrabFromCrosshair(sp);
        if (grabbedEntity) {
            return InteractionResultHolder.success(stack);
        }

        boolean grabbedBlock = tryGrabBlockFromCrosshair(sp, stack);
        if (grabbedBlock) {
            return InteractionResultHolder.success(stack);
        }

        player.getCooldowns().addCooldown(this, TOOHEAVY_COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    // ======================
    // CLICK DERECHO (bloque) = SOLO agarrar / soltar
    // ======================
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // ✅ Si hay gravity gun en ambas manos: NO se puede usar ninguna
        if (player != null && isDualWieldBlocked(player)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide && level.getBlockState(pos).is(Blocks.COPPER_BLOCK)) {
            BlockPos below = pos.below();
            BlockPos below2 = below.below();
            if (level.getBlockState(below).is(Blocks.REDSTONE_BLOCK)
                    && level.getBlockState(below2).is(Blocks.DIAMOND_BLOCK)) {
                return InteractionResult.PASS;
            }
        }

        if (level.isClientSide || player == null) return InteractionResult.PASS;

        UUID playerId = player.getUUID();

        if (HeldObjectTracker.isHolding(playerId)) {
            HeldObjectTracker.release(playerId);
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResult.SUCCESS;
        }

        BlockState state = level.getBlockState(pos);

        if (!isLiftableBlock(level, pos, state)) {
            player.getCooldowns().addCooldown(this, TOOHEAVY_COOLDOWN_TICKS);
            return InteractionResult.FAIL;
        }

        if (!state.canOcclude()) {
            player.getCooldowns().addCooldown(this, TOOHEAVY_COOLDOWN_TICKS);
            return InteractionResult.FAIL;
        }

        double baseDist = 2.5D;
        if (player instanceof ServerPlayer sp) {
            baseDist = computeBaseHoldDistance(sp, pos);
        }

        EntityLiftedBlock liftedBlock = new EntityLiftedBlock(level, player, state, pos, baseDist);
        liftedBlock.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);

        level.addFreshEntity(liftedBlock);
        level.removeBlock(pos, false);

        HeldObjectTracker.hold(playerId, liftedBlock);
        player.getCooldowns().addCooldown(this, 10);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new GravityGunRenderer();
            }
        });
    }
}