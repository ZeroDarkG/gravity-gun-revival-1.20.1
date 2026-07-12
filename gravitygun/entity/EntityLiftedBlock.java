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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
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

    private static final EntityDataAccessor<Float> ROT_YAW =
            SynchedEntityData.defineId(EntityLiftedBlock.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROT_PITCH =
            SynchedEntityData.defineId(EntityLiftedBlock.class, EntityDataSerializers.FLOAT);

    private static final double MIN_EFFECTIVE_DIST = 0.75D;
    private static final double MAX_EFFECTIVE_DIST_NORMAL = 10.0D;
    private static final double MAX_EFFECTIVE_DIST_SUPER = 14.0D;

    private static final double FOLLOW_SPEED = 0.45D;
    private static final double BACKOFF = 0.05D;
    private static final double HOLDER_PADDING = 0.06D;
    private static final double MIN_HORIZ_EPS = 0.02D;

    private static final double TELEPORT_DIST = 2.25D;
    private static final double TELEPORT_FALL_DIST = 1.35D;
    private static final double SNAP_FACTOR = 0.65D;
    private static final double CARRY_TOP_EPS = 0.08D;
    private static final int LAUNCH_GRACE_TICKS = 6;
    private static final int RELEASE_SETTLE_GRACE_TICKS = 8;
    private static final double MAX_STEP_PER_TICK = 1.2D;

    private static final int MAX_FREE_TICKS = 600;

    private BlockPos originalPos;
    private boolean placed = false;
    private int stillTicks = 0;
    private int freeTicks = 0;
    private int launchGraceTicks = 0;
    private int releaseSettleGraceTicks = 0;
    private boolean restoreOnLoad = false;
    private boolean suppressFreeSpin = false;
    private Vec3 launchVelocity = Vec3.ZERO;
    private int launchVelocityTicks = 0;

    public float rotYaw;
    public float rotPitch;
    public float prevRotYaw;
    public float prevRotPitch;

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
        this.entityData.define(ROT_YAW, 0.0F);
        this.entityData.define(ROT_PITCH, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(BLOCK_ID, tag.getInt("blockId"));
        this.restoreOnLoad = tag.hasUUID("holder");
        this.clearHolder();
        if (tag.contains("baseHoldDist")) this.setBaseHoldDist(tag.getFloat("baseHoldDist"));

        if (tag.contains("origX") && tag.contains("origY") && tag.contains("origZ")) {
            originalPos = new BlockPos(tag.getInt("origX"), tag.getInt("origY"), tag.getInt("origZ"));
        }
        this.placed = tag.getBoolean("placed");
        this.freeTicks = 0;
        this.launchGraceTicks = tag.getInt("launchGraceTicks");
        this.releaseSettleGraceTicks = tag.getInt("releaseSettleGraceTicks");
        this.suppressFreeSpin = tag.getBoolean("suppressFreeSpin");
        if (tag.contains("launchVelX") && tag.contains("launchVelY") && tag.contains("launchVelZ")) {
            this.launchVelocity = new Vec3(tag.getDouble("launchVelX"), tag.getDouble("launchVelY"), tag.getDouble("launchVelZ"));
        } else {
            this.launchVelocity = Vec3.ZERO;
        }
        this.launchVelocityTicks = tag.getInt("launchVelocityTicks");
        this.setNoGravity(false);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("blockId", this.entityData.get(BLOCK_ID));

        tag.putFloat("baseHoldDist", this.getBaseHoldDist());

        if (originalPos != null) {
            tag.putInt("origX", originalPos.getX());
            tag.putInt("origY", originalPos.getY());
            tag.putInt("origZ", originalPos.getZ());
        }

        tag.putBoolean("placed", this.placed);
        tag.putInt("freeTicks", this.freeTicks);
        tag.putInt("launchGraceTicks", this.launchGraceTicks);
        tag.putInt("releaseSettleGraceTicks", this.releaseSettleGraceTicks);
        tag.putBoolean("suppressFreeSpin", this.suppressFreeSpin);
        tag.putDouble("launchVelX", this.launchVelocity.x);
        tag.putDouble("launchVelY", this.launchVelocity.y);
        tag.putDouble("launchVelZ", this.launchVelocity.z);
        tag.putInt("launchVelocityTicks", this.launchVelocityTicks);
    }

    public BlockPos getOriginalPos() {
        return originalPos;
    }

    public void launch(Vec3 velocity) {
        clearHolder();
        this.placed = false;
        this.stillTicks = 0;
        this.freeTicks = 0;
        this.launchGraceTicks = LAUNCH_GRACE_TICKS;
        this.releaseSettleGraceTicks = 0;
        this.suppressFreeSpin = false;
        this.launchVelocity = velocity;
        this.launchVelocityTicks = LAUNCH_GRACE_TICKS;
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setDeltaMovement(velocity);
        this.fallDistance = 0.0F;
        this.hurtMarked = true;
        this.hasImpulse = true;
    }

    public void applyReleaseSpin(Vec3 velocity) {
        float speed = (float) velocity.length();
        if (speed < 0.02F) {
            return;
        }

        float spin = Math.min(24.0F, speed * 30.0F);
        this.rotYaw += (velocity.z >= 0.0D ? 1.0F : -1.0F) * spin;
        this.rotPitch += (velocity.x >= 0.0D ? 1.0F : -1.0F) * spin;
        this.entityData.set(ROT_YAW, this.rotYaw);
        this.entityData.set(ROT_PITCH, this.rotPitch);
    }

    public void releaseWithoutSpin() {
        this.clearHolder();
        this.placed = false;
        this.stillTicks = 0;
        this.freeTicks = 0;
        this.launchGraceTicks = 0;
        this.releaseSettleGraceTicks = RELEASE_SETTLE_GRACE_TICKS;
        this.suppressFreeSpin = true;
        this.launchVelocity = Vec3.ZERO;
        this.launchVelocityTicks = 0;
        this.noPhysics = false;
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
        this.hurtMarked = true;
        this.hasImpulse = true;
    }

    private static boolean isSupercharged(Player player) {
        return player.getMainHandItem().is(ModItems.GRAVITY_GUN_SUPERCHARGED.get())
                || player.getOffhandItem().is(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        UUID holder = getHolderUUID();
        if (holder != null && other instanceof Player p && holder.equals(p.getUUID())) return false;
        return super.canCollideWith(other);
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
        this.prevRotYaw = this.rotYaw;
        this.prevRotPitch = this.rotPitch;
        super.tick();

        if (this.level().isClientSide) {
            this.rotYaw = this.entityData.get(ROT_YAW);
            this.rotPitch = this.entityData.get(ROT_PITCH);
            return;
        }

        if (this.restoreOnLoad) {
            this.restoreOnLoad = false;

            BlockPos placePos = BlockPos.containing(this.getX(), this.getY() + 0.2D, this.getZ());
            if (tryPlace(placePos)) return;
            if (tryPlace(placePos.below())) return;
            if (this.originalPos != null && tryPlace(this.originalPos)) return;
            if (this.originalPos != null && tryPlace(this.originalPos.above())) return;
        }

        Player player = getHolderServerOnly();

        if (player != null && !player.isDeadOrDying()) {
            this.noPhysics = false;
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            this.stillTicks = 0;
            this.freeTicks = 0;
            this.fallDistance = 0.0F;

            boolean supercharged = isSupercharged(player);
            double holdDist = Mth.clamp(
                    this.getBaseHoldDist(),
                    MIN_EFFECTIVE_DIST,
                    supercharged ? MAX_EFFECTIVE_DIST_SUPER : MAX_EFFECTIVE_DIST_NORMAL
            );
            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 holdDir = stableHoldDir(player, player.getLookAngle());
            Vec3 desiredEnd = eyePos.add(holdDir.scale(holdDist));

            HitResult wallHit = this.level().clip(new ClipContext(
                    eyePos,
                    desiredEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            double effectiveDist = holdDist;
            if (wallHit.getType() != HitResult.Type.MISS) {
                effectiveDist = Mth.clamp(
                        wallHit.getLocation().distanceTo(eyePos) - (this.getBbWidth() * 0.5D + 0.12D),
                        MIN_EFFECTIVE_DIST,
                        holdDist
                );
            }

            Vec3 targetPos = eyePos.add(holdDir.scale(effectiveDist)).subtract(0.0D, this.getBbHeight() * 0.5D, 0.0D);
            Vec3 current = this.position();
            Vec3 toTarget = targetPos.subtract(current);

            Vec3 playerVel = player.getDeltaMovement();
            boolean fallingHard = playerVel.y < -0.25D;
            double threshold = fallingHard ? TELEPORT_FALL_DIST : TELEPORT_DIST;

            Vec3 beforeMove = current;
            if (toTarget.length() > threshold) {
                Vec3 snap = toTarget.scale(SNAP_FACTOR);
                this.setPos(current.x + snap.x, current.y + snap.y, current.z + snap.z);
                this.hurtMarked = true;
                this.hasImpulse = true;
            } else {
                Vec3 delta = toTarget.scale(FOLLOW_SPEED);

                if (playerVel.lengthSqr() > 1.0E-6D) {
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
                double deltaLen = delta.length();
                if (deltaLen > maxStepDynamic) {
                    delta = delta.scale(maxStepDynamic / deltaLen);
                }

                Vec3 from = current;
                Vec3 to = current.add(delta);
                HitResult moveHit = this.level().clip(new ClipContext(
                        from,
                        to,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        this
                ));

                if (moveHit.getType() != HitResult.Type.MISS) {
                    Vec3 hitLoc = moveHit.getLocation();
                    Vec3 backOff = delta.lengthSqr() > 1.0E-7D ? delta.normalize().scale(BACKOFF) : Vec3.ZERO;
                    Vec3 safe = hitLoc.subtract(backOff);
                    delta = safe.subtract(from);
                }

                Vec3 horiz = new Vec3(delta.x, 0.0D, delta.z);
                this.move(MoverType.SELF, horiz);

                Vec3 vert = new Vec3(0.0D, delta.y, 0.0D);
                this.move(MoverType.SELF, vert);

                this.hurtMarked = true;
                this.hasImpulse = true;
            }

            this.fallDistance = 0.0F;
            carryEntitiesOnTop(this.position().subtract(beforeMove));
        } else {
            this.setNoGravity(false);
            this.noPhysics = false;
            this.freeTicks++;
            if (this.launchGraceTicks > 0) {
                this.launchGraceTicks--;
            }
            if (this.releaseSettleGraceTicks > 0) {
                this.releaseSettleGraceTicks--;
            }

            if (this.freeTicks > MAX_FREE_TICKS) {
                this.spawnAtLocation(this.getBlockState().getBlock());
                this.discard();
                return;
            }

            Vec3 baseVelocity = this.getDeltaMovement();
            if (this.launchVelocityTicks > 0 && baseVelocity.lengthSqr() < 1.0E-6D) {
                baseVelocity = this.launchVelocity;
                this.setDeltaMovement(baseVelocity);
                this.hurtMarked = true;
                this.hasImpulse = true;
            }

            Vec3 motion = baseVelocity.add(0.0D, -0.05D, 0.0D);
            boolean isFlyingFast = motion.horizontalDistanceSqr() > 0.1D;

            if (this.releaseSettleGraceTicks <= 0 && !isFlyingFast) {
                BlockPos belowPos = BlockPos.containing(this.getX(), this.getY() - 0.5D, this.getZ());
                if (canReplace(this.level().getBlockState(belowPos))) {
                    double centerX = belowPos.getX() + 0.5D;
                    double centerZ = belowPos.getZ() + 0.5D;
                    double dx = centerX - this.getX();
                    double dz = centerZ - this.getZ();

                    if (Math.abs(dx) < 0.7D && Math.abs(dz) < 0.7D) {
                        motion = new Vec3(motion.x + dx * 0.2D, motion.y, motion.z + dz * 0.2D);

                        if (Math.abs(dx) < 0.15D && Math.abs(dz) < 0.15D) {
                            this.setPos(centerX, this.getY(), centerZ);
                            motion = new Vec3(0.0D, motion.y, 0.0D);
                        }
                    }
                }
            }

            final Vec3 currentMotion = motion;
            final double speedSqr = currentMotion.lengthSqr();
            if (speedSqr > 0.01D) {
                final float damage = (float) (Math.sqrt(speedSqr) * 12.0D);
                AABB scanBox = this.getBoundingBox().inflate(0.1D);
                this.level().getEntitiesOfClass(LivingEntity.class, scanBox, e -> !e.getUUID().equals(this.getUUID()))
                        .forEach(target -> target.hurt(this.damageSources().fallingBlock(this), damage));
            }

            Vec3 nextPos = this.position().add(motion);
            HitResult hit = this.level().clip(new ClipContext(
                    this.position(),
                    nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockState hitState = this.level().getBlockState(((BlockHitResult) hit).getBlockPos());
                if (!canReplace(hitState) && !hitState.is(net.minecraft.tags.BlockTags.LEAVES)) {
                    net.minecraft.core.Direction face = ((BlockHitResult) hit).getDirection();
                    double bounce = isFlyingFast ? 0.3D : 0.1D;
                    if (face.getAxis() == net.minecraft.core.Direction.Axis.Y) {
                        motion = new Vec3(motion.x * 0.5D, -motion.y * bounce, motion.z * 0.5D);
                    } else {
                        motion = new Vec3(-motion.x * bounce, motion.y, -motion.z * bounce);
                    }
                }
            }

            double friction = this.onGround() ? 0.6D : (isFlyingFast ? 0.99D : 0.95D);
            motion = motion.multiply(friction, 1.0D, friction);

            this.setDeltaMovement(motion);
            if (this.launchVelocityTicks > 0) {
                this.launchVelocity = motion;
                this.launchVelocityTicks--;
            } else {
                this.launchVelocity = Vec3.ZERO;
            }
            this.move(MoverType.SELF, motion);

            if (!placed && this.launchGraceTicks <= 0 && this.releaseSettleGraceTicks <= 0) {
                if (motion.lengthSqr() < 0.001D) {
                    stillTicks++;
                } else {
                    stillTicks = 0;
                }

                if (stillTicks > 3 || (this.onGround() && motion.lengthSqr() < 0.0005D)) {
                    BlockPos placePos = BlockPos.containing(this.getX(), this.getY() + 0.2D, this.getZ());
                    if (tryPlace(placePos)) return;
                    if (tryPlace(placePos.below())) return;
                }
            }

            if (!this.suppressFreeSpin && motion.lengthSqr() > 0.0005D) {
                float s = (float) motion.length();
                this.rotYaw += s * 20.0F;
                this.rotPitch += s * 20.0F;
            } else if (!this.suppressFreeSpin) {
                this.rotYaw *= 0.85F;
                this.rotPitch *= 0.85F;
            }
        }

        this.entityData.set(ROT_YAW, this.rotYaw);
        this.entityData.set(ROT_PITCH, this.rotPitch);
    }

    private boolean tryPlace(BlockPos pos) {
        if (canReplace(this.level().getBlockState(pos))) {
            this.level().setBlockAndUpdate(pos, getBlockState());
            this.placed = true;
            this.discard();
            return true;
        }
        return false;
    }

    private boolean canReplace(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
