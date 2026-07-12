package com.zerokg2004.gravitygun.gravitygun.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "gravitygun", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GravityGunThrownHandler {

    private static final String GG_THROWN = "ggThrown";
    private static final String GG_EXPLOSIVE = "ggExplosive";
    private static final String GG_THROWER = "ggThrower";
    private static final String GG_AGE = "ggAge";
    private static final String GG_EXPLODED = "ggCreeperExploded";

    private static final String GG_PX = "ggPx";
    private static final String GG_PY = "ggPy";
    private static final String GG_PZ = "ggPz";

    private static final String GG_STILL = "ggStillTicks";
    private static final String GG_LAST_HIT = "ggLastHit";
    private static final String GG_FAST_GRAV_K = "ggFastGravK";
    private static final String GG_LAUNCH_KEEP_TICKS = "ggLaunchKeepTicks";
    private static final String GG_LAUNCH_VX = "ggLaunchVx";
    private static final String GG_LAUNCH_VY = "ggLaunchVy";
    private static final String GG_LAUNCH_VZ = "ggLaunchVz";

    private static final double MIN_MAG_FOR_DAMAGE = 0.35D;
    private static final double STILL_MAG = 0.01D;
    private static final int HIT_COOLDOWN = 4;
    private static final int STILL_TICKS_TO_DROP = 12;
    private static final int MAX_LIFETIME_TICKS = 600;
    private static final int WATER_ANIMAL_KEEP_TICKS = 12;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        Level level = e.level();
        if (level.isClientSide) return;

        CompoundTag pd = e.getPersistentData();
        if (!pd.getBoolean(GG_THROWN)) return;

        float k = pd.contains(GG_FAST_GRAV_K) ? pd.getFloat(GG_FAST_GRAV_K) : 1.0f;
        if (k < 1.0f) k = 1.0f;

        if (!e.isNoGravity() && !e.isInWaterOrBubble()) {
            double extraG = 0.08D * (k - 1.0f);
            if (!e.onGround()) {
                Vec3 dm = e.getDeltaMovement();
                e.setDeltaMovement(dm.x, dm.y - extraG, dm.z);
            }
        }

        int age = pd.getInt(GG_AGE) + 1;
        pd.putInt(GG_AGE, age);
        if (age > MAX_LIFETIME_TICKS) {
            clearThrown(pd);
            return;
        }

        if (HeldObjectTracker.isHeld(e)) {
            clearThrown(pd);
            return;
        }

        if (pd.hasUUID(GG_THROWER)) {
            UUID throwerId = pd.getUUID(GG_THROWER);
            if (HeldObjectTracker.isHolding(throwerId)) {
                Entity held = HeldObjectTracker.getHeld(throwerId);
                if (held != null && held.getUUID().equals(e.getUUID())) {
                    clearThrown(pd);
                    return;
                }
            }
        }

        boolean isWaterAnimal = e instanceof WaterAnimal;
        if (isWaterAnimal) {
            keepWaterAnimalLaunch(e, pd);
        }

        Vec3 curr = e.position();
        Vec3 prev = getPrevPos(pd, curr);

        pd.putDouble(GG_PX, curr.x);
        pd.putDouble(GG_PY, curr.y);
        pd.putDouble(GG_PZ, curr.z);

        Vec3 delta = curr.subtract(prev);
        double mag = delta.length();

        boolean isInWater = e.isInWaterOrBubble();
        boolean hitSolidBlock = e.horizontalCollision || e.verticalCollision;

        if (isInWater) {
            e.fallDistance = 0.0F;
        }

        HitResult blockHit = ((ServerLevel) level).clip(
                new ClipContext(prev, curr, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)
        );

        Entity hitEntity = findEntityHit((ServerLevel) level, e, prev, curr);
        boolean hitSomething = hitEntity != null || blockHit.getType() != HitResult.Type.MISS || hitSolidBlock;

        if (e instanceof Creeper creeper) {
            boolean explosive = pd.getBoolean(GG_EXPLOSIVE);
            boolean already = pd.getBoolean(GG_EXPLODED);

            if (explosive && !already && hitSomething) {
                pd.putBoolean(GG_EXPLODED, true);

                ServerLevel sl = (ServerLevel) level;
                sl.explode(
                        creeper,
                        creeper.getX(), creeper.getY(), creeper.getZ(),
                        3.0F,
                        ExplosionInteraction.MOB
                );

                creeper.discard();
                return;
            }
        }

        int lastHit = pd.contains(GG_LAST_HIT) ? pd.getInt(GG_LAST_HIT) : -999999;
        if (hitSomething && !isInWater && mag >= MIN_MAG_FOR_DAMAGE && (e.tickCount - lastHit) >= HIT_COOLDOWN) {
            pd.putInt(GG_LAST_HIT, e.tickCount);

            ServerPlayer thrower = null;
            if (pd.hasUUID(GG_THROWER)) {
                thrower = ((ServerLevel) level).getServer().getPlayerList().getPlayer(pd.getUUID(GG_THROWER));
            }

            DamageSource src = (thrower != null) ? level.damageSources().playerAttack(thrower) : level.damageSources().generic();
            float dmg = computeDamage(mag, e, hitEntity);

            if (hitEntity instanceof LivingEntity livingHit) {
                livingHit.hurt(src, dmg);
            }

            if (blockHit.getType() != HitResult.Type.MISS || hitSolidBlock) {
                e.hurt(src, Math.max(1.0F, dmg * 0.6F));
            }

            clearThrown(pd);
            return;
        }

        if (isInWater) {
            if (!isWaterAnimal || pd.getInt(GG_LAUNCH_KEEP_TICKS) <= 0) {
                e.setDeltaMovement(e.getDeltaMovement().scale(0.9D));
            }
            if (mag < STILL_MAG) {
                clearThrown(pd);
            }
        } else if (mag < STILL_MAG && hitSolidBlock) {
            int still = pd.getInt(GG_STILL) + 1;
            pd.putInt(GG_STILL, still);
            if (still >= STILL_TICKS_TO_DROP) {
                clearThrown(pd);
            }
        } else {
            pd.putInt(GG_STILL, 0);
        }
    }

    public static void markThrown(Entity entity, ServerPlayer thrower, boolean explosive) {
        CompoundTag pd = entity.getPersistentData();

        pd.putBoolean(GG_THROWN, true);
        pd.putBoolean(GG_EXPLOSIVE, explosive);
        pd.putUUID(GG_THROWER, thrower.getUUID());
        pd.putInt(GG_AGE, 0);
        pd.putBoolean(GG_EXPLODED, false);

        Vec3 p = entity.position();
        pd.putDouble(GG_PX, p.x);
        pd.putDouble(GG_PY, p.y);
        pd.putDouble(GG_PZ, p.z);

        pd.putInt(GG_STILL, 0);
        pd.putInt(GG_LAST_HIT, -999999);

        if (!pd.contains(GG_FAST_GRAV_K)) {
            pd.putFloat(GG_FAST_GRAV_K, 1.0f);
        }

        if (entity instanceof WaterAnimal) {
            pd.putInt(GG_LAUNCH_KEEP_TICKS, WATER_ANIMAL_KEEP_TICKS);
            storeLaunchVelocity(pd, entity.getDeltaMovement());
            entity.hurtMarked = true;
            entity.hasImpulse = true;
        }
    }

    private static void keepWaterAnimalLaunch(LivingEntity entity, CompoundTag pd) {
        int keepTicks = pd.getInt(GG_LAUNCH_KEEP_TICKS);
        if (keepTicks <= 0) {
            return;
        }

        Vec3 forced = getStoredLaunchVelocity(pd, entity.getDeltaMovement());

        if (!entity.isInWaterOrBubble()) {
            forced = new Vec3(forced.x, forced.y - 0.08D, forced.z);
        } else {
            forced = forced.scale(0.985D);
        }

        entity.setDeltaMovement(forced);
        entity.hurtMarked = true;
        entity.hasImpulse = true;
        entity.fallDistance = 0.0F;

        storeLaunchVelocity(pd, forced);
        pd.putInt(GG_LAUNCH_KEEP_TICKS, keepTicks - 1);
    }

    private static Vec3 getPrevPos(CompoundTag pd, Vec3 fallback) {
        if (pd.contains(GG_PX) && pd.contains(GG_PY) && pd.contains(GG_PZ)) {
            return new Vec3(pd.getDouble(GG_PX), pd.getDouble(GG_PY), pd.getDouble(GG_PZ));
        }
        return fallback;
    }

    private static Vec3 getStoredLaunchVelocity(CompoundTag pd, Vec3 fallback) {
        if (pd.contains(GG_LAUNCH_VX) && pd.contains(GG_LAUNCH_VY) && pd.contains(GG_LAUNCH_VZ)) {
            return new Vec3(pd.getDouble(GG_LAUNCH_VX), pd.getDouble(GG_LAUNCH_VY), pd.getDouble(GG_LAUNCH_VZ));
        }
        return fallback;
    }

    private static void storeLaunchVelocity(CompoundTag pd, Vec3 velocity) {
        pd.putDouble(GG_LAUNCH_VX, velocity.x);
        pd.putDouble(GG_LAUNCH_VY, velocity.y);
        pd.putDouble(GG_LAUNCH_VZ, velocity.z);
    }

    private static void clearThrown(CompoundTag pd) {
        pd.putBoolean(GG_THROWN, false);
        pd.putInt(GG_AGE, 0);
        pd.putInt(GG_STILL, 0);
        pd.putInt(GG_LAST_HIT, 0);
        pd.remove(GG_FAST_GRAV_K);
        pd.remove(GG_LAUNCH_KEEP_TICKS);
        pd.remove(GG_LAUNCH_VX);
        pd.remove(GG_LAUNCH_VY);
        pd.remove(GG_LAUNCH_VZ);
    }

    private static float computeDamage(double mag, Entity thrown, Entity hitEntity) {
        double base = 2.3D * mag;
        base = Math.max(1.0D, Math.min(base, 30.0D));

        double volumeThrown = Math.cbrt(thrown.getBbHeight() * thrown.getBbWidth() * thrown.getBbWidth());
        double volumeHit = (hitEntity != null)
                ? Math.cbrt(hitEntity.getBbHeight() * hitEntity.getBbWidth() * hitEntity.getBbWidth())
                : volumeThrown;

        double scale = (volumeHit > 1.0E-4) ? (volumeThrown / volumeHit / 0.925D) : 1.0D;

        double dmg = base * scale;
        dmg = Math.max(1.0D, Math.min(dmg, 40.0D));
        return (float) dmg;
    }

    private static Entity findEntityHit(ServerLevel level, Entity thrown, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);

        AABB sweep = thrown.getBoundingBox()
                .move(delta)
                .inflate(1.0D);

        List<Entity> list = level.getEntities(
                thrown,
                sweep,
                entx -> entx.isPickable()
                        && entx.isAlive()
                        && entx != thrown
                        && !entx.isPassengerOfSameVehicle(thrown)
        );

        Entity best = null;
        double bestDist = 0.0D;

        for (Entity ent : list) {
            AABB box = ent.getBoundingBox().inflate(0.3D);
            Optional<Vec3> hit = box.clip(start, end);

            if (hit.isPresent()) {
                double d = start.distanceTo(hit.get());
                if (best == null || d < bestDist || bestDist == 0.0D) {
                    best = ent;
                    bestDist = d;
                }
            }
        }

        return best;
    }
}
