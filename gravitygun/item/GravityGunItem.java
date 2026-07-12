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
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class GravityGunItem extends Item {

    public enum GravityGunState { CLOSED, AIMING, HOLDING }

    private static final String TAG_STATE = "GunState";
    private static final int TOOHEAVY_COOLDOWN_TICKS = 6;
    private static final int OPEN_DELAY_TICKS = 9;
    private static final int CLOSE_DELAY_TICKS = 9;
    private static final int EMERGENCY_OPEN_DELAY_TICKS = 3;

    private static final Map<UUID, GravityGunState> pendingStateMap = new HashMap<>();
    private static final Map<UUID, Integer> pendingTickMap = new HashMap<>();
    private static final Map<String, GravityGunState> interactionStateMap = new HashMap<>();
    private static final Map<String, GravityGunState> interactionPendingStateMap = new HashMap<>();
    private static final Map<String, Integer> interactionPendingTickMap = new HashMap<>();

    private static final double RANGE_NORMAL = 5.0D;
    private static final double RANGE_SUPER  = 12.0D;

    public GravityGunItem(Properties properties) {
        super(properties);
    }

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
        if (id.endsWith("white_gravity_gun")) return 0xFFFFFF;
        if (id.endsWith("cyan_gravity_gun")) return 0x66FFCC;
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

    private static boolean isLiftableBlock(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;

        // ❌ BLOQUEO DE HOJAS: Añadimos esta línea
        if (state.is(net.minecraft.tags.BlockTags.LEAVES)) return false;

        // Excepción para el Hielo (permitir agarrarlo aunque sea transparente)
        boolean isIce = state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
        if (isIce) return true;

        // Reglas originales para evitar agarrar flores/pasto/agua
        if (state.getCollisionShape(level, pos).isEmpty()) return false;
        if (state.canBeReplaced()) return false;

        return true;
    }

    private static double computeBaseHoldDistance(ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 center = Vec3.atCenterOf(pos);
        return eye.distanceTo(center);
    }

    // ✅ RECUPERADO: Lógica de agarre a distancia
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

            // Arreglado para Hielo: Quitamos el state.canOcclude() que lo bloqueaba
            if (isLiftableBlock(level, pos, state)) {
                double baseDist = computeBaseHoldDistance(player, pos);
                EntityLiftedBlock liftedBlock = new EntityLiftedBlock(level, player, state, pos, baseDist);
                liftedBlock.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                level.addFreshEntity(liftedBlock);
                level.removeBlock(pos, false);
                HeldObjectTracker.hold(player.getUUID(), liftedBlock);
                player.getCooldowns().addCooldown(this, 10);
                return true;
            }

            // No detener el rayo si es hielo (para poder agarrarlo)
            if (!state.getCollisionShape(level, pos).isEmpty() && !state.canBeReplaced() && !state.is(Blocks.ICE)) {
                return false;
            }
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public GravityGunState getVisualState(Player player, ItemStack stack) {
        GravityGunState detected = detectVisualState(player, stack);
        if (detected == GravityGunState.CLOSED && (!ItemStack.isSameItemSameTags(player.getMainHandItem(), stack))) {
            return GravityGunState.CLOSED;
        }

        if (!ItemStack.isSameItemSameTags(player.getMainHandItem(), stack)) {
            return detected;
        }

        UUID uuid = player.getUUID();
        GravityGunState current = ClientSoundCache.lastSoundStateMap.getOrDefault(uuid, GravityGunState.CLOSED);
        GravityGunState pending = pendingStateMap.get(uuid);
        int readyTick = pendingTickMap.getOrDefault(uuid, -1);

        if (pending != null) {
            if (detected != pending) {
                pendingStateMap.remove(uuid);
                pendingTickMap.remove(uuid);
            } else if (player.tickCount >= readyTick) {
                if (!(stack.getItem() instanceof SuperchargedGravityGunItem)) {
                    if (current == GravityGunState.CLOSED && pending != GravityGunState.CLOSED && SoundEventsRegistry.CLAWS_OPEN.isPresent()) {
                        player.playSound(SoundEventsRegistry.CLAWS_OPEN.get(), 1.0F, 1.0F);
                    } else if (current != GravityGunState.CLOSED && pending == GravityGunState.CLOSED && SoundEventsRegistry.CLAWS_CLOSE.isPresent()) {
                        player.playSound(SoundEventsRegistry.CLAWS_CLOSE.get(), 1.0F, 1.0F);
                    }
                }
                ClientSoundCache.lastSoundStateMap.put(uuid, pending);
                pendingStateMap.remove(uuid);
                pendingTickMap.remove(uuid);
                return pending;
            }
            return current;
        }

        if (detected != current) {
            pendingStateMap.put(uuid, detected);
            pendingTickMap.put(uuid, player.tickCount + getStateDelayTicks(player, stack, current, detected));
        }
        return current;
    }

    public GravityGunState detectVisualState(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof GravityGunItem)) return GravityGunState.CLOSED;

        if (isDualWieldBlocked(player)) {
            if (player != null) {
                UUID uuid = player.getUUID();
                ClientSoundCache.lastSoundStateMap.put(uuid, GravityGunState.CLOSED);
                pendingStateMap.remove(uuid);
                pendingTickMap.remove(uuid);
            }
            return GravityGunState.CLOSED;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand  = player.getOffhandItem();
        boolean inMain = ItemStack.isSameItemSameTags(mainHand, stack);
        boolean inOff  = ItemStack.isSameItemSameTags(offHand, stack);
        if (!inMain && !inOff) return GravityGunState.CLOSED;

        UUID uuid = player.getUUID();

        boolean isHolding = player.level().isClientSide
                ? HeldObjectTracker.isHoldingClient(uuid)
                : HeldObjectTracker.isHolding(uuid);

        if (isHolding) {
            return GravityGunState.HOLDING;
        } else {
            double range = getRange(stack);
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle().normalize();
            Vec3 end = eye.add(look.scale(range));

            AABB aabb = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    player.level(),
                    player,
                    eye,
                    end,
                    aabb,
                    e -> e.isAlive() && e != player && e.isPickable()
            );

            if (entityHit != null) {
                Entity e = entityHit.getEntity();
                if (e instanceof EntityLiftedBlock) {
                    return GravityGunState.AIMING;
                }
                return (e instanceof LivingEntity l && l.isAlive()) ? GravityGunState.AIMING : GravityGunState.CLOSED;
            }

            HitResult result = player.level().clip(new net.minecraft.world.level.ClipContext(
                    eye,
                    end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    player
            ));

            if (result instanceof BlockHitResult bhr) {
                BlockPos pos = bhr.getBlockPos();
                BlockState state = player.level().getBlockState(pos);
                return isLiftableBlock(player.level(), pos, state) ? GravityGunState.AIMING : GravityGunState.CLOSED;
            }
            return GravityGunState.CLOSED;
        }
    }

    protected GravityGunState advanceInteractionState(Player player, ItemStack stack) {
        GravityGunState detected = detectVisualState(player, stack);
        String key = getInteractionStateKey(player, stack);
        GravityGunState current = interactionStateMap.getOrDefault(key, GravityGunState.CLOSED);
        GravityGunState pending = interactionPendingStateMap.get(key);
        int readyTick = interactionPendingTickMap.getOrDefault(key, -1);

        if (pending != null) {
            if (detected != pending) {
                interactionPendingStateMap.remove(key);
                interactionPendingTickMap.remove(key);
            } else if (player.tickCount >= readyTick) {
                current = pending;
                interactionStateMap.put(key, current);
                interactionPendingStateMap.remove(key);
                interactionPendingTickMap.remove(key);
            }
            return current;
        }

        if (detected != current) {
            interactionPendingStateMap.put(key, detected);
            interactionPendingTickMap.put(key, player.tickCount + getStateDelayTicks(player, stack, current, detected));
        }

        interactionStateMap.putIfAbsent(key, current);
        return current;
    }

    protected boolean areClawsReady(Player player, ItemStack stack) {
        String key = getInteractionStateKey(player, stack);
        GravityGunState current = advanceInteractionState(player, stack);
        return current != GravityGunState.CLOSED && !interactionPendingStateMap.containsKey(key);
    }

    private static String getInteractionStateKey(Player player, ItemStack stack) {
        if (ItemStack.isSameItemSameTags(player.getMainHandItem(), stack)) {
            return player.getUUID() + ":main";
        }
        if (ItemStack.isSameItemSameTags(player.getOffhandItem(), stack)) {
            return player.getUUID() + ":off";
        }
        return player.getUUID() + ":other";
    }

    private static int getStateDelayTicks(Player player, ItemStack stack, GravityGunState current, GravityGunState next) {
        if (next == GravityGunState.CLOSED) {
            return CLOSE_DELAY_TICKS;
        }
        if (current == GravityGunState.CLOSED) {
            return hasEmergencyPickupTarget(player, stack) ? EMERGENCY_OPEN_DELAY_TICKS : OPEN_DELAY_TICKS;
        }
        return 0;
    }

    private static boolean hasEmergencyPickupTarget(Player player, ItemStack stack) {
        EntityHitResult entityHit = findCrosshairEntity(player, stack);
        if (entityHit == null) {
            return false;
        }

        Entity entity = entityHit.getEntity();
        if (entity instanceof EntityLiftedBlock) {
            return isActuallyFalling(entity);
        }

        if (entity instanceof LivingEntity living && living.isAlive()) {
            return isActuallyFalling(living);
        }

        return isActuallyFalling(entity);
    }

    private static boolean isActuallyFalling(Entity entity) {
        if (entity == null || entity.onGround()) {
            return false;
        }

        Vec3 motion = entity.getDeltaMovement();
        return motion.y < -0.08D;
    }

    private static EntityHitResult findCrosshairEntity(Player player, ItemStack stack) {
        if (player == null) {
            return null;
        }

        double range = stack.getItem() instanceof GravityGunItem gun ? gun.getRange(stack) : RANGE_NORMAL;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));

        AABB aabb = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eye,
                end,
                aabb,
                e -> e.isAlive() && e != player && e.isPickable()
        );
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) { return false; }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) { return true; }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack))
                .withStyle(style -> style.withColor(getNameColor(stack)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1. Bloqueo de dual wield (común)
        if (isDualWieldBlocked(player)) return InteractionResultHolder.fail(stack);

        UUID playerId = player.getUUID();

        // 2. Lógica de Cliente: Solo visual, no procesamos física aquí
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        // 3. Lógica de Servidor segura
        if (player instanceof ServerPlayer sp) {
            advanceInteractionState(player, stack);

            // Cooldowns
            if (sp.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.consume(stack);
            }

            // Si ya sostiene algo, soltarlo
            if (HeldObjectTracker.isHolding(playerId)) {
                HeldObjectTracker.releaseWithMomentum(playerId);
                sp.getCooldowns().addCooldown(this, 2);
                return InteractionResultHolder.success(stack);
            }

            // Intento de agarre (usando el ServerPlayer verificado)
            if (!areClawsReady(sp, stack)) {
                return InteractionResultHolder.consume(stack);
            }

            if (HeldObjectTracker.tryGrabFromCrosshair(sp) || tryGrabBlockFromCrosshair(sp, stack)) {
                return InteractionResultHolder.success(stack);
            }

            // Cooldown de fallo
            sp.getCooldowns().addCooldown(this, TOOHEAVY_COOLDOWN_TICKS);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player != null && isDualWieldBlocked(player)) return InteractionResult.FAIL;

        // ✅ RECUPERADO: Lógica del Ritual (Copper -> Redstone -> Diamond)
        if (!level.isClientSide && level.getBlockState(pos).is(Blocks.COPPER_BLOCK)) {
            BlockPos below = pos.below();
            BlockPos below2 = below.below();
            if (level.getBlockState(below).is(Blocks.REDSTONE_BLOCK) && level.getBlockState(below2).is(Blocks.DIAMOND_BLOCK)) {
                return InteractionResult.PASS;
            }
        }

        if (level.isClientSide || player == null) return InteractionResult.PASS;
        UUID playerId = player.getUUID();

        if (HeldObjectTracker.isHolding(playerId)) {
            HeldObjectTracker.releaseWithMomentum(playerId);
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResult.SUCCESS;
        }

        if (!areClawsReady(player, context.getItemInHand())) {
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer sp && HeldObjectTracker.tryGrabFromCrosshair(sp)) {
            player.getCooldowns().addCooldown(this, 2);
            return InteractionResult.SUCCESS;
        }

        BlockState state = level.getBlockState(pos);

        // Arreglado para Hielo: eliminada la condición state.canOcclude() que sobraba aquí
        if (!isLiftableBlock(level, pos, state)) {
            player.getCooldowns().addCooldown(this, TOOHEAVY_COOLDOWN_TICKS);
            return InteractionResult.FAIL;
        }

        double baseDist = (player instanceof ServerPlayer sp) ? computeBaseHoldDistance(sp, pos) : 2.5D;
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
            private GravityGunRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GravityGunRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        if (!ItemStack.isSameItemSameTags(player.getMainHandItem(), stack)
                && !ItemStack.isSameItemSameTags(player.getOffhandItem(), stack)) {
            return;
        }
        advanceInteractionState(player, stack);
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientSoundCache {
        private static final Map<UUID, GravityGunState> lastSoundStateMap = new HashMap<>();
    }
}
