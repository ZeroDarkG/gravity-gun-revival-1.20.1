package com.zerokg2004.gravitygun.gravitygun.handler;

import com.zerokg2004.gravitygun.gravitygun.entity.EntityLiftedBlock;
import com.zerokg2004.gravitygun.gravitygun.item.GravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "gravitygun", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeldObjectTracker {

    private static final Map<UUID, Entity> heldObjects = new HashMap<>();
    private static final Map<UUID, Double> grabDistance = new HashMap<>();

    private static final double MIN_DIST = 3.0D;
    private static final double MAX_DIST = 10.0D;
    private static final double DEFAULT_DIST = 4.5D;

    private static final double HOLD_Y_OFFSET = -0.05D;
    private static final double LIVING_EXTRA_DIST = 0.5D;

    private static final double GRAB_RANGE_NORMAL = 5.0D;
    private static final double GRAB_RANGE_SUPER  = 12.0D;

    private static final double FOLLOW_SPEED = 0.45D;
    private static final double MAX_STEP_PER_TICK = 1.2D;
    private static final double BACKOFF = 0.05D;
    private static final double MIN_EFFECTIVE_DIST = 0.75D;

    private static final double TELEPORT_DIST = 2.25D;
    private static final double TELEPORT_FALL_DIST = 1.35D;
    private static final double SNAP_FACTOR = 0.65D;

    private static final double CARRY_TOP_EPS = 0.08D;

    public static boolean isHolding(UUID playerId) {
        return heldObjects.containsKey(playerId);
    }

    public static Entity getHeld(UUID playerId) {
        return heldObjects.get(playerId);
    }

    public static void hold(UUID playerId, Entity entity) {
        heldObjects.put(playerId, entity);
        grabDistance.putIfAbsent(playerId, DEFAULT_DIST);

        if (entity instanceof LivingEntity living) {
            living.setNoGravity(true);
            living.setDeltaMovement(Vec3.ZERO);
            living.setOnGround(false);
        }

        if (entity.level().isLoaded(entity.blockPosition())
                && SoundEventsRegistry.PHYSCANNON_PICKUP.isPresent()) {
            entity.level().playSound(
                    null,
                    entity.blockPosition(),
                    (SoundEvent) SoundEventsRegistry.PHYSCANNON_PICKUP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    public static void release(UUID playerId) {
        Entity entity = heldObjects.remove(playerId);
        if (entity == null) return;

        if (entity instanceof LivingEntity living) {
            living.setNoGravity(false);
        }

        if (entity instanceof EntityLiftedBlock liftedBlock) {
            liftedBlock.clearHolder();
        }

        if (entity.level().isLoaded(entity.blockPosition())
                && SoundEventsRegistry.PHYSCANNON_DROP.isPresent()) {
            entity.level().playSound(
                    null,
                    entity.blockPosition(),
                    (SoundEvent) SoundEventsRegistry.PHYSCANNON_DROP.get(),
                    SoundSource.PLAYERS,
                    0.5F,
                    0.8F
            );
        }
    }

    public static void release(Entity entity) {
        heldObjects.entrySet().removeIf(entry -> entry.getValue().equals(entity));
    }

    public static void clear() {
        heldObjects.clear();
        grabDistance.clear();
    }

    private static boolean isGravityGunStack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return false;
        return "gravitygun".equals(key.getNamespace()) && key.getPath().contains("gravity_gun");
    }

    private static boolean hasAnyGravityGun(Player player) {
        return isGravityGunStack(player.getMainHandItem()) || isGravityGunStack(player.getOffhandItem());
    }

    // ✅ Bloqueo total si hay gravity gun en ambas manos (normal/super/colores/mixtas)
    private static boolean isDualWieldBlocked(Player player) {
        if (player == null) return false;
        return (player.getMainHandItem().getItem() instanceof GravityGunItem)
                && (player.getOffhandItem().getItem() instanceof GravityGunItem);
    }

    private static boolean hasSuperchargedGun(Player player) {
        return player.getMainHandItem().is(ModItems.GRAVITY_GUN_SUPERCHARGED.get())
                || player.getOffhandItem().is(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
    }

    private static double getGrabRange(Player player) {
        return hasSuperchargedGun(player) ? GRAB_RANGE_SUPER : GRAB_RANGE_NORMAL;
    }

    public static void adjustGrabDistance(UUID playerId, double delta) {
        double cur = grabDistance.getOrDefault(playerId, DEFAULT_DIST) + delta;

        if (cur < MIN_DIST) cur = MIN_DIST;
        if (cur > MAX_DIST) cur = MAX_DIST;

        grabDistance.put(playerId, cur);
    }

    public static boolean tryGrabFromCrosshair(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // ✅ Si dual-wield bloqueado: NO puede agarrar nada
        if (isDualWieldBlocked(player)) return false;

        if (isHolding(playerId)) return true;

        double range = getGrabRange(player);

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
            Entity target = entityHit.getEntity();

            if (target instanceof EnderDragon) return false;

            if (target instanceof LivingEntity living && living.isAlive() && living != player) {
                hold(playerId, living);
                return true;
            }
        }

        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        UUID playerId = player.getUUID();

        // ✅ Si está dual-wield bloqueado: suelta y NO empuja/mueve nada
        if (isDualWieldBlocked(player)) {
            release(playerId);
            return;
        }

        // ✅ Si ya no tiene ninguna gravity gun: suelta
        if (!hasAnyGravityGun(player)) {
            release(playerId);
            return;
        }

        Entity target = heldObjects.get(playerId);
        if (target == null || !target.isAlive() || target.isRemoved()) {
            release(playerId);
            return;
        }

        // ✅ Los bloques se mueven solos en su tick
        if (target instanceof EntityLiftedBlock) return;

        double dist = grabDistance.getOrDefault(playerId, DEFAULT_DIST);
        if (target instanceof LivingEntity) dist += LIVING_EXTRA_DIST;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        Vec3 desiredEnd = eye.add(look.scale(dist));
        HitResult wallHit = player.level().clip(new ClipContext(
                eye, desiredEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        double effectiveDist = dist;
        if (wallHit.getType() != HitResult.Type.MISS) {
            double hitDist = wallHit.getLocation().distanceTo(eye);
            double margin = (target.getBbWidth() * 0.5D) + 0.12D;
            effectiveDist = Math.max(MIN_EFFECTIVE_DIST, hitDist - margin);
        }

        Vec3 desiredCenter = eye.add(look.scale(effectiveDist)).add(0.0D, HOLD_Y_OFFSET, 0.0D);

        Vec3 targetPos = new Vec3(
                desiredCenter.x,
                desiredCenter.y - (target.getBbHeight() * 0.5D),
                desiredCenter.z
        );

        Vec3 current = target.position();
        Vec3 toTarget = targetPos.subtract(current);

        Vec3 playerVel = player.getDeltaMovement();
        boolean fallingHard = playerVel.y < -0.25D;

        double threshold = fallingHard ? TELEPORT_FALL_DIST : TELEPORT_DIST;
        if (toTarget.length() > threshold) {
            Vec3 snap = toTarget.scale(SNAP_FACTOR);
            target.setPos(current.x + snap.x, current.y + snap.y, current.z + snap.z);
            target.hurtMarked = true;
            target.hasImpulse = true;

            if (target instanceof LivingEntity living) living.setNoGravity(true);
            return;
        }

        Vec3 delta = toTarget.scale(FOLLOW_SPEED);

        if (playerVel.lengthSqr() > 1.0E-6) {
            double yFactor = 1.05D;
            double xzFactor = 0.60D;

            if (playerVel.y < -0.20D) {
                yFactor = 1.10D;
                xzFactor = 0.70D;
            }

            delta = delta.add(new Vec3(playerVel.x * xzFactor, playerVel.y * yFactor, playerVel.z * xzFactor));
        }

        double fallBoost = Math.max(0.0D, -playerVel.y) * 1.2D;
        double maxStepDynamic = MAX_STEP_PER_TICK + playerVel.length() * 1.8D + fallBoost;

        double len = delta.length();
        if (len > maxStepDynamic) {
            delta = delta.scale(maxStepDynamic / len);
        }

        if (target.onGround() && delta.y < 0.0D) {
            delta = new Vec3(delta.x, 0.0D, delta.z);
        }

        Vec3 from = current;
        Vec3 to = current.add(delta);

        HitResult hit = player.level().clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target
        ));

        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 hitLoc = hit.getLocation();
            Vec3 backOff = delta.lengthSqr() > 1.0E-7 ? delta.normalize().scale(BACKOFF) : Vec3.ZERO;
            Vec3 safe = hitLoc.subtract(backOff);
            delta = safe.subtract(from);
        }

        Vec3 beforeMove = target.position();

        Vec3 horiz = new Vec3(delta.x, 0.0D, delta.z);
        target.move(MoverType.SELF, horiz);

        Vec3 vert = new Vec3(0.0D, delta.y, 0.0D);
        target.move(MoverType.SELF, vert);

        target.setDeltaMovement(Vec3.ZERO);

        if (target instanceof LivingEntity living) {
            living.setNoGravity(true);
        }

        target.hurtMarked = true;
        target.hasImpulse = true;

        AABB tbb = target.getBoundingBox();
        AABB pbb = player.getBoundingBox();

        boolean xz = pbb.maxX > tbb.minX && pbb.minX < tbb.maxX
                && pbb.maxZ > tbb.minZ && pbb.minZ < tbb.maxZ;

        boolean onTop =
                xz
                        && (pbb.minY >= tbb.maxY - CARRY_TOP_EPS)
                        && (pbb.minY <= tbb.maxY + 0.35D);

        if (onTop) {
            Vec3 afterMove = target.position();
            Vec3 moved = afterMove.subtract(beforeMove);
            if (moved.lengthSqr() > 1.0E-8) {
                player.move(MoverType.SELF, moved);
            }
        }
    }
}