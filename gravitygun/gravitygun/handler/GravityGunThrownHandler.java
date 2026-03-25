package com.zerokg2004.gravitygun.gravitygun.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

    // ✅ nuevo: multiplicador de “gravedad rápida” (lo setea PacketThrowEntity)
    private static final String GG_FAST_GRAV_K = "ggFastGravK";

    private static final double MIN_MAG_FOR_DAMAGE = 0.35D;
    private static final double STILL_MAG = 0.01D;
    private static final int HIT_COOLDOWN = 4;
    private static final int STILL_TICKS_TO_DROP = 12;
    private static final int MAX_LIFETIME_TICKS = 600;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        Level level = e.level();
        if (level.isClientSide) return;

        CompoundTag pd = e.getPersistentData();
        if (!pd.getBoolean(GG_THROWN)) return;

        // =========================================================
        // ✅ GRAVEDAD “RÁPIDA” mientras está ggThrown
        // - Vanilla gravity ~0.08 por tick (aprox).
        // - Si k=1.7 => extra = 0.08*(1.7-1)=0.056
        // - Si k=2.2 => extra = 0.096
        // =========================================================
        float k = pd.contains(GG_FAST_GRAV_K) ? pd.getFloat(GG_FAST_GRAV_K) : 1.0f;
        if (k < 1.0f) k = 1.0f;

        // solo si realmente le afecta la gravedad y no está en agua/burbujas
        if (!e.isNoGravity() && !e.isInWaterOrBubble()) {
            double extraG = 0.08D * (k - 1.0f);

            // no le metas gravedad si está en suelo (evita jitter raro)
            if (!e.onGround()) {
                Vec3 dm = e.getDeltaMovement();
                e.setDeltaMovement(dm.x, dm.y - extraG, dm.z);
            }
        }

        // age
        int age = pd.getInt(GG_AGE) + 1;
        pd.putInt(GG_AGE, age);

        if (age > MAX_LIFETIME_TICKS) {
            clearThrown(pd);
            return;
        }

        // si el thrower lo volvió a agarrar, corta
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

        // prev/curr
        Vec3 curr = e.position();
        Vec3 prev = getPrevPos(pd, curr);

        pd.putDouble(GG_PX, curr.x);
        pd.putDouble(GG_PY, curr.y);
        pd.putDouble(GG_PZ, curr.z);

        Vec3 delta = curr.subtract(prev);
        double mag = delta.length();

        // choque por colisión “física”
        boolean hitBlockByPhysics = e.horizontalCollision || e.verticalCollision || e.isInWaterOrBubble();

        // raytrace bloque
        HitResult blockHit = ((ServerLevel) level).clip(
                new ClipContext(prev, curr, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)
        );

        // sweep entidad
        Entity hitEntity = findEntityHit((ServerLevel) level, e, prev, curr);

        boolean hitSomething =
                hitEntity != null ||
                        blockHit.getType() != HitResult.Type.MISS ||
                        hitBlockByPhysics;

        // Creeper explosive
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

        // daño al impactar
        if (hitSomething && mag >= MIN_MAG_FOR_DAMAGE && (e.tickCount - lastHit) >= HIT_COOLDOWN) {
            pd.putInt(GG_LAST_HIT, e.tickCount);

            ServerPlayer thrower = null;
            if (pd.hasUUID(GG_THROWER)) {
                thrower = ((ServerLevel) level).getServer()
                        .getPlayerList()
                        .getPlayer(pd.getUUID(GG_THROWER));
            }

            DamageSource src = (thrower != null)
                    ? level.damageSources().playerAttack(thrower)
                    : level.damageSources().generic();

            float dmg = computeDamage(mag, e, hitEntity);

            if (hitEntity instanceof LivingEntity livingHit) {
                livingHit.hurt(src, dmg);
            }

            if (blockHit.getType() != HitResult.Type.MISS || hitBlockByPhysics) {
                e.hurt(src, Math.max(1.0F, dmg * 0.6F));
            }

            clearThrown(pd);
            return;
        }

        // si se quedó casi quieto y está colisionando, suéltalo tras X ticks
        if (mag < STILL_MAG && hitBlockByPhysics) {
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

        // ✅ si no vino seteado desde PacketThrowEntity, default 1
        if (!pd.contains(GG_FAST_GRAV_K)) {
            pd.putFloat(GG_FAST_GRAV_K, 1.0f);
        }
    }

    private static Vec3 getPrevPos(CompoundTag pd, Vec3 fallback) {
        if (pd.contains(GG_PX) && pd.contains(GG_PY) && pd.contains(GG_PZ)) {
            return new Vec3(pd.getDouble(GG_PX), pd.getDouble(GG_PY), pd.getDouble(GG_PZ));
        }
        return fallback;
    }

    private static void clearThrown(CompoundTag pd) {
        pd.putBoolean(GG_THROWN, false);
        pd.putInt(GG_AGE, 0);
        pd.putInt(GG_STILL, 0);
        pd.putInt(GG_LAST_HIT, 0);

        // ✅ limpiar para que no contamine futuros movimientos
        pd.remove(GG_FAST_GRAV_K);
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