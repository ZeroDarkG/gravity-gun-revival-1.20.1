package com.zerokg2004.gravitygun.gravitygun.entity;

import com.zerokg2004.gravitygun.gravitygun.registry.ModEntityTypes;
import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityLiftedBlock extends Entity {

    private static final EntityDataAccessor<Integer> BLOCK_ID =
            SynchedEntityData.defineId(EntityLiftedBlock.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Optional<UUID>> HOLDER_UUID =
            SynchedEntityData.defineId(EntityLiftedBlock.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final EntityDataAccessor<Float> BASE_HOLD_DIST =
            SynchedEntityData.defineId(EntityLiftedBlock.class, EntityDataSerializers.FLOAT);

    private BlockPos originalPos;
    private boolean placed = false;

    private static final double MIN_EFFECTIVE_DIST = 0.75D;
    private static final double MAX_EFFECTIVE_DIST_NORMAL = 10.0D;
    private static final double MAX_EFFECTIVE_DIST_SUPER  = 14.0D;

    private static final double FOLLOW_SPEED = 0.45D;
    private static final double MAX_STEP_PER_TICK = 1.2D;
    private static final double BACKOFF = 0.05D;

    private static final double HOLDER_PADDING = 0.06D;
    private static final double MIN_HORIZ_EPS = 0.02D;

    // ✅ iChun-like: tolerancia + snap parcial
    private static final double TELEPORT_DIST = 2.25D;
    private static final double TELEPORT_FALL_DIST = 1.35D;
    private static final double SNAP_FACTOR = 0.65D;

    // ✅ “plataforma”: si estás encima, te muevo con el delta del bloque
    private static final double CARRY_TOP_EPS = 0.08D;

    public EntityLiftedBlock(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public EntityLiftedBlock(Level level, Player holder, BlockState state, BlockPos pos, double baseHoldDist) {
        this(ModEntityTypes.LIFTED_BLOCK.get(), level);
        this.setHolder(holder.getUUID());
        this.setBlockState(state);
        this.originalPos = pos;
        this.setBaseHoldDist((float) baseHoldDist);
        this.setPos(holder.getX(), holder.getY(), holder.getZ());
    }

    public EntityLiftedBlock(Level level, Player holder, BlockState state, BlockPos pos) {
        this(level, holder, state, pos, 2.5D);
    }

    public void setHolder(UUID uuid) {
        this.entityData.set(HOLDER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getHolderUUID() {
        return this.entityData.get(HOLDER_UUID).orElse(null);
    }

    private Player getHolderServerOnly() {
        UUID uuid = getHolderUUID();
        if (uuid == null || level().isClientSide) return null;
        return level().getPlayerByUUID(uuid);
    }

    public void clearHolder() {
        this.setHolder(null);
    }

    public void setBaseHoldDist(float dist) {
        this.entityData.set(BASE_HOLD_DIST, dist);
    }

    public float getBaseHoldDist() {
        return this.entityData.get(BASE_HOLD_DIST);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(BLOCK_ID, Block.getId(state));
    }

    public BlockState getBlockState() {
        return Block.stateById(this.entityData.get(BLOCK_ID));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BLOCK_ID, 0);
        this.entityData.define(HOLDER_UUID, Optional.empty());
        this.entityData.define(BASE_HOLD_DIST, 2.5F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(BLOCK_ID, tag.getInt("blockId"));
        if (tag.hasUUID("holder")) this.setHolder(tag.getUUID("holder"));
        if (tag.contains("baseHoldDist")) this.setBaseHoldDist(tag.getFloat("baseHoldDist"));

        if (tag.contains("origX") && tag.contains("origY") && tag.contains("origZ")) {
            originalPos = new BlockPos(tag.getInt("origX"), tag.getInt("origY"), tag.getInt("origZ"));
        }
        this.placed = tag.getBoolean("placed");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("blockId", this.entityData.get(BLOCK_ID));

        UUID uuid = getHolderUUID();
        if (uuid != null) tag.putUUID("holder", uuid);

        tag.putFloat("baseHoldDist", this.getBaseHoldDist());

        if (originalPos != null) {
            tag.putInt("origX", originalPos.getX());
            tag.putInt("origY", originalPos.getY());
            tag.putInt("origZ", originalPos.getZ());
        }
        tag.putBoolean("placed", this.placed);
    }

    public BlockPos getOriginalPos() {
        return originalPos;
    }

    private static boolean isSupercharged(Player player) {
        return player.getMainHandItem().is(ModItems.GRAVITY_GUN_SUPERCHARGED.get())
                || player.getOffhandItem().is(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
    }

    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }

    @Override
    public boolean canCollideWith(Entity other) {
        UUID holder = getHolderUUID();
        if (holder != null && other instanceof Player p && holder.equals(p.getUUID())) return false;
        return super.canCollideWith(other);
    }

    private double minNoOverlap(Player player) {
        return (player.getBbWidth() * 0.5D) + (this.getBbWidth() * 0.5D) + HOLDER_PADDING;
    }

    private static Vec3 stableHoldDir(Player player, Vec3 look) {
        Vec3 dir = look.normalize();

        Vec3 horiz = new Vec3(dir.x, 0.0D, dir.z);
        if (horiz.lengthSqr() < (MIN_HORIZ_EPS * MIN_HORIZ_EPS)) {
            Vec3 yawHoriz = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
            dir = new Vec3(yawHoriz.x * MIN_HORIZ_EPS, dir.y, yawHoriz.z * MIN_HORIZ_EPS).normalize();
        }
        return dir;
    }

    // ✅ entidades “encima” del bloque
    private boolean isEntityOnTop(Entity e) {
        AABB tbb = this.getBoundingBox();
        AABB ebb = e.getBoundingBox();

        boolean xz = ebb.maxX > tbb.minX && ebb.minX < tbb.maxX
                && ebb.maxZ > tbb.minZ && ebb.minZ < tbb.maxZ;

        double dy = ebb.minY - tbb.maxY;
        boolean yOk = dy >= -CARRY_TOP_EPS && dy <= 0.35D;

        return xz && yOk;
    }

    private void carryEntitiesOnTop(Vec3 moved) {
        if (moved.lengthSqr() < 1.0E-10) return;

        // zona justo encima del bloque
        AABB area = this.getBoundingBox().inflate(0.02D, 0.30D, 0.02D).move(0.0D, 0.02D, 0.0D);
        List<Entity> list = this.level().getEntities(this, area, e -> e.isAlive() && !e.isRemoved());

        for (Entity e : list) {
            if (isEntityOnTop(e)) {
                e.move(MoverType.SELF, moved);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        Player player = getHolderServerOnly();

        if (player != null && !player.isDeadOrDying()) {
            this.noPhysics = false;
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);

            boolean supercharged = isSupercharged(player);

            double base = this.getBaseHoldDist();
            double max = supercharged ? MAX_EFFECTIVE_DIST_SUPER : MAX_EFFECTIVE_DIST_NORMAL;
            double holdDist = Mth.clamp(base, MIN_EFFECTIVE_DIST, max);

            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 look = player.getLookAngle();
            Vec3 holdDir = stableHoldDir(player, look);

            Vec3 desiredEnd = eyePos.add(holdDir.scale(holdDist));
            HitResult wallHit = this.level().clip(new ClipContext(
                    eyePos, desiredEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            double effectiveDist = holdDist;
            if (wallHit.getType() != HitResult.Type.MISS) {
                double hitDist = wallHit.getLocation().distanceTo(eyePos);
                double margin = (this.getBbWidth() * 0.5D) + 0.12D;
                effectiveDist = Mth.clamp(hitDist - margin, MIN_EFFECTIVE_DIST, max);
            }

            Vec3 targetCenter = eyePos.add(holdDir.scale(effectiveDist));
            Vec3 targetPos = new Vec3(
                    targetCenter.x,
                    targetCenter.y - (this.getBbHeight() * 0.5D),
                    targetCenter.z
            );

            Vec3 before = this.position();
            Vec3 toTarget = targetPos.subtract(before);

            Vec3 playerVel = player.getDeltaMovement();
            boolean fallingHard = playerVel.y < -0.25D;

            // ✅ snap parcial si se queda lejos (anti-jitter real)
            double threshold = fallingHard ? TELEPORT_FALL_DIST : TELEPORT_DIST;
            if (toTarget.length() > threshold) {
                Vec3 snap = toTarget.scale(SNAP_FACTOR);
                this.setPos(before.x + snap.x, before.y + snap.y, before.z + snap.z);

                Vec3 moved = this.position().subtract(before);
                carryEntitiesOnTop(moved);

                this.hurtMarked = true;
                this.hasImpulse = true;
                return;
            }

            // follow normal
            Vec3 delta = toTarget.scale(FOLLOW_SPEED);

            // heredar velocidad del jugador (moderado)
            if (playerVel.lengthSqr() > 1.0E-6) {
                double yFactor = 1.05D;
                double xzFactor = 0.60D;

                if (playerVel.y < -0.20D) {
                    yFactor = 1.10D;
                    xzFactor = 0.70D;
                }
                delta = delta.add(new Vec3(playerVel.x * xzFactor, playerVel.y * yFactor, playerVel.z * xzFactor));
            }

            // cap dinámico
            double fallBoost = Math.max(0.0D, -playerVel.y) * 1.2D;
            double maxStepDynamic = MAX_STEP_PER_TICK + playerVel.length() * 1.8D + fallBoost;

            double len = delta.length();
            if (len > maxStepDynamic) delta = delta.scale(maxStepDynamic / len);

            // ✅ anti-overlap solo si el jugador NO está encima (si está encima, lo queremos “carry”, no empujar)
            boolean playerOnTop = isEntityOnTop(player);
            if (!playerOnTop) {
                AABB playerBB = player.getBoundingBox();
                AABB movedBB = this.getBoundingBox().move(delta);

                if (movedBB.intersects(playerBB)) {
                    Vec3 pushDir = new Vec3(holdDir.x, 0.0D, holdDir.z);
                    if (pushDir.lengthSqr() < 1.0E-7) pushDir = Vec3.directionFromRotation(0.0F, player.getYRot());
                    pushDir = pushDir.normalize();

                    Vec3 c1 = new Vec3(movedBB.getCenter().x, 0.0D, movedBB.getCenter().z);
                    Vec3 c2 = new Vec3(playerBB.getCenter().x, 0.0D, playerBB.getCenter().z);
                    double dist = c1.distanceTo(c2);
                    double need = minNoOverlap(player);
                    double add = need - dist;
                    if (add > 0.0D) delta = delta.add(pushDir.scale(add));
                }
            }

            // ===== mover bloque (slide) =====
            Vec3 beforeMove = this.position();

            // horizontal
            Vec3 horizMove = new Vec3(delta.x, 0.0D, delta.z);
            if (horizMove.lengthSqr() > 1.0E-7) {
                Vec3 fromH = beforeMove;
                Vec3 toH = beforeMove.add(horizMove);

                HitResult hitH = this.level().clip(new ClipContext(
                        fromH, toH,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        this
                ));

                if (hitH.getType() != HitResult.Type.MISS) {
                    Vec3 hitLoc = hitH.getLocation();
                    Vec3 safe = hitLoc.subtract(horizMove.normalize().scale(BACKOFF));
                    horizMove = safe.subtract(fromH);
                }
            }
            this.move(MoverType.SELF, horizMove);

            // vertical
            double dy = delta.y;
            if (Math.abs(dy) > 1.0E-7) {
                Vec3 cur2 = this.position();
                Vec3 vert = new Vec3(0.0D, dy, 0.0D);

                HitResult hitV = this.level().clip(new ClipContext(
                        cur2, cur2.add(vert),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        this
                ));

                if (hitV.getType() != HitResult.Type.MISS) {
                    Vec3 hitLoc = hitV.getLocation();
                    Vec3 back = new Vec3(0.0D, dy > 0 ? 1.0D : -1.0D, 0.0D).scale(BACKOFF);
                    Vec3 safe = hitLoc.subtract(back);
                    vert = safe.subtract(cur2);
                }

                this.move(MoverType.SELF, vert);
            }

            // ✅ carry encima después del movimiento real
            Vec3 moved = this.position().subtract(beforeMove);
            carryEntitiesOnTop(moved);

            this.setDeltaMovement(Vec3.ZERO);
            this.hurtMarked = true;
            this.hasImpulse = true;

        } else {
            // suelto
            this.setNoGravity(false);
            this.noPhysics = false;

            Vec3 motion = this.getDeltaMovement().add(0, -0.04, 0);
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);

            if (!placed && this.onGround()) {
                BlockPos pos = this.blockPosition();

                if (level().getBlockState(pos).isAir()) {
                    level().setBlockAndUpdate(pos, getBlockState());
                } else {
                    for (int dy = 1; dy <= 2; dy++) {
                        BlockPos above = pos.above(dy);
                        if (level().getBlockState(above).isAir()) {
                            level().setBlockAndUpdate(above, getBlockState());
                            break;
                        }
                    }
                }

                this.placed = true;
                this.discard();
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}