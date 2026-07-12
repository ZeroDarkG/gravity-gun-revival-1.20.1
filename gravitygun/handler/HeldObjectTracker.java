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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "gravitygun", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeldObjectTracker {

    private static final Map<UUID, Entity> heldObjects = new HashMap<>();
    private static final Map<UUID, Double> grabDistance = new HashMap<>();
    private static final Map<UUID, Vec3> lastHeldPositions = new HashMap<>();
    private static final Map<UUID, Vec3> recentHeldMotion = new HashMap<>();
    private static final Map<UUID, Vec3> lastLookDirs = new HashMap<>();
    private static final Map<UUID, Vec3> recentLookMotion = new HashMap<>();
    private static final Set<UUID> clientHeldPlayers = new HashSet<>();

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
    private static final double RELEASE_MOTION_MIN_SQR = 0.0040D;

    public static boolean isHolding(UUID playerId) {
        return heldObjects.containsKey(playerId);
    }

    public static boolean isHoldingClient(UUID playerId) {
        return clientHeldPlayers.contains(playerId);
    }

    public static void setClientHolding(UUID playerId, boolean holding) {
        if (holding) {
            clientHeldPlayers.add(playerId);
        } else {
            clientHeldPlayers.remove(playerId);
        }
    }

    public static Entity getHeld(UUID playerId) {
        return heldObjects.get(playerId);
    }

    public static boolean isHeld(Entity entity) {
        return entity != null && heldObjects.containsValue(entity);
    }

    public static void hold(UUID playerId, Entity entity) {
        heldObjects.put(playerId, entity);
        grabDistance.putIfAbsent(playerId, DEFAULT_DIST);
        lastHeldPositions.put(playerId, entity.position());
        recentHeldMotion.put(playerId, Vec3.ZERO);
        Vec3 look = entity.level().getPlayerByUUID(playerId) != null
                ? entity.level().getPlayerByUUID(playerId).getLookAngle().normalize()
                : Vec3.ZERO;
        lastLookDirs.put(playerId, look);
        recentLookMotion.put(playerId, Vec3.ZERO);

        if (entity instanceof EntityLiftedBlock liftedBlock) {
            liftedBlock.setHolder(playerId);
            liftedBlock.setDeltaMovement(Vec3.ZERO);
            liftedBlock.hurtMarked = true;
            liftedBlock.hasImpulse = true;
        }

        syncHoldingState(playerId, true);

        // MODIFICACIÓN: Aplicar gravedad cero a LivingEntity y PrimedTnt
        if (entity instanceof LivingEntity living) {
            living.setNoGravity(true);
            living.setDeltaMovement(Vec3.ZERO);
            living.setOnGround(false);
            living.fallDistance = 0.0F;
        } else if (entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt) {
            tnt.setNoGravity(true);
            tnt.setDeltaMovement(Vec3.ZERO);
            tnt.fallDistance = 0.0F;
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
        release(playerId, false);
    }

    public static void releaseWithMomentum(UUID playerId) {
        release(playerId, true);
    }

    private static void release(UUID playerId, boolean preserveMomentum) {
        Entity entity = heldObjects.remove(playerId);
        grabDistance.remove(playerId);
        Vec3 storedMotion = recentHeldMotion.remove(playerId);
        Vec3 storedLookMotion = recentLookMotion.remove(playerId);
        lastHeldPositions.remove(playerId);
        lastLookDirs.remove(playerId);
        syncHoldingState(playerId, false);
        if (entity == null) return;

        // MODIFICACIÓN: Restaurar gravedad a LivingEntity y PrimedTnt
        if (entity instanceof LivingEntity living) {
            living.setNoGravity(false);
            living.fallDistance = 0.0F;
        } else if (entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt) {
            tnt.setNoGravity(false);
            tnt.fallDistance = 0.0F;
        }

        if (entity instanceof EntityLiftedBlock liftedBlock) {
            if (preserveMomentum) {
                liftedBlock.releaseWithoutSpin();
            } else {
                liftedBlock.clearHolder();
            }
        }

        if (preserveMomentum) {
            applyReleaseMomentum(entity, storedMotion, storedLookMotion);
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
        lastHeldPositions.clear();
        recentHeldMotion.clear();
        lastLookDirs.clear();
        recentLookMotion.clear();
        clientHeldPlayers.clear();
    }

    private static void sampleHeldMotion(UUID playerId, Entity target) {
        Vec3 current = target.position();
        Vec3 previous = lastHeldPositions.put(playerId, current);
        if (previous == null) return;

        Vec3 instant = current.subtract(previous);
        if (instant.lengthSqr() > 6.25D) {
            instant = instant.normalize().scale(2.5D);
        }

        Vec3 smoothed = recentHeldMotion.getOrDefault(playerId, Vec3.ZERO).scale(0.45D)
                .add(instant.scale(0.55D));
        recentHeldMotion.put(playerId, smoothed);
    }

    private static void sampleLookMotion(UUID playerId, Player player) {
        Vec3 current = player.getLookAngle().normalize();
        Vec3 previous = lastLookDirs.put(playerId, current);
        if (previous == null) return;

        Vec3 instant = current.subtract(previous);
        Vec3 smoothed = recentLookMotion.getOrDefault(playerId, Vec3.ZERO).scale(0.50D)
                .add(instant.scale(0.50D));
        recentLookMotion.put(playerId, smoothed);
    }

    private static void applyReleaseMomentum(Entity entity, Vec3 storedMotion, Vec3 storedLookMotion) {
        if (entity == null) {
            return;
        }

        Vec3 motionPart = storedMotion == null ? Vec3.ZERO : storedMotion.scale(entity instanceof EntityLiftedBlock ? 0.60D : 0.55D);
        Vec3 lookPart = Vec3.ZERO;
        if (storedLookMotion != null && storedLookMotion.lengthSqr() > 1.0E-5D) {
            double lookScale = entity instanceof EntityLiftedBlock ? 1.85D : 1.55D;
            lookPart = new Vec3(storedLookMotion.x * lookScale, storedLookMotion.y * 0.35D, storedLookMotion.z * lookScale);
        }

        Vec3 boost = motionPart.add(lookPart);
        if (boost.lengthSqr() < RELEASE_MOTION_MIN_SQR) {
            return;
        }

        double maxSpeed = entity instanceof EntityLiftedBlock ? 0.72D : 0.90D;

        if (boost.length() > maxSpeed) {
            boost = boost.normalize().scale(maxSpeed);
        }

        entity.setDeltaMovement(boost);
        entity.hurtMarked = true;
        entity.hasImpulse = true;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        release(event.getEntity().getUUID());
    }

    private static void syncHoldingState(UUID playerId, boolean holding) {
        if (ServerLifecycleHooks.getCurrentServer() == null) return;

        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            NetworkHandler.sendToClient(new com.zerokg2004.gravitygun.gravitygun.packet.PacketHeldState(holding), player);
        }
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

        if (isDualWieldBlocked(player)) return false;
        if (isHolding(playerId)) return true;

        double range = getGrabRange(player);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));

        AABB aabb = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);

        EntityLiftedBlock liftedBlock = findLiftedBlockTarget(player, eye, end, aabb);
        if (liftedBlock != null) {
            hold(playerId, liftedBlock);
            return true;
        }

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

            // MODIFICACIÓN: Aceptar LivingEntity o PrimedTnt
            if (target instanceof EntityLiftedBlock || target instanceof LivingEntity || target instanceof net.minecraft.world.entity.item.PrimedTnt) {
                hold(playerId, target);
                return true;
            }
        }
        return false;
    }

    private static EntityLiftedBlock findLiftedBlockTarget(ServerPlayer player, Vec3 eye, Vec3 end, AABB searchBox) {
        UUID playerId = player.getUUID();
        EntityLiftedBlock best = null;
        double bestDistSqr = Double.MAX_VALUE;

        for (EntityLiftedBlock candidate : player.level().getEntitiesOfClass(EntityLiftedBlock.class, searchBox.inflate(0.35D))) {
            UUID holder = candidate.getHolderUUID();
            if (holder != null && !holder.equals(playerId)) {
                continue;
            }

            var clip = candidate.getBoundingBox().inflate(0.2D).clip(eye, end);
            if (clip.isEmpty()) {
                continue;
            }

            double distSqr = eye.distanceToSqr(clip.get());
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                best = candidate;
            }
        }

        return best;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        UUID playerId = player.getUUID();
        sampleLookMotion(playerId, player);

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
        if (target instanceof EntityLiftedBlock) {
            sampleHeldMotion(playerId, target);
            return;
        }

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
            sampleHeldMotion(playerId, target);
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
            living.fallDistance = 0.0F;
        }

        target.fallDistance = 0.0F;

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

        sampleHeldMotion(playerId, target);
    }
}
