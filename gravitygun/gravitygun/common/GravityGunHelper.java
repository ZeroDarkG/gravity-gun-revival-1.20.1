package com.zerokg2004.gravitygun.gravitygun.common;

import com.zerokg2004.gravitygun.gravitygun.handler.NetworkHandler;
import com.zerokg2004.gravitygun.gravitygun.item.GravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.item.SuperchargedGravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.packet.PacketThrowEffect;
import com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class GravityGunHelper {

    private static final double PUSH_REACH = 5.0D;
    private static final double MIN_DIST   = 1.5D;

    public static boolean tryPush(LivingEntity living) {
        ItemStack stack = getGravityGun(living);
        if (stack.isEmpty()) return false;

        Level level = living.level();
        if (level.isClientSide()) return false;

        Vec3 eye = living.getEyePosition();
        Vec3 look = living.getLookAngle();
        Vec3 end = eye.add(look.scale(PUSH_REACH));

        AABB box = new AABB(eye, end).inflate(1.0D);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && e != living
        );

        if (targets.isEmpty()) return false;

        LivingEntity target = targets.stream()
                .min(Comparator.comparingDouble(e -> e.distanceTo(living)))
                .orElse(null);

        if (target == null) return false;

        double dist = Math.max(MIN_DIST, living.position().distanceTo(target.position()));

        boolean supercharged = stack.getItem() instanceof SuperchargedGravityGunItem;

        // ✅ Sonido (igual lógica que jar)
        SoundEvent sound = null;
        if (supercharged) {
            if (SoundEventsRegistry.SUPERPHYS_LAUNCH1.isPresent() && SoundEventsRegistry.SUPERPHYS_LAUNCH2.isPresent()) {
                sound = living.getRandom().nextBoolean()
                        ? SoundEventsRegistry.SUPERPHYS_LAUNCH1.get()
                        : SoundEventsRegistry.SUPERPHYS_LAUNCH2.get();
            }
        } else {
            if (SoundEventsRegistry.PHYSCANNON_LAUNCH1.isPresent() && SoundEventsRegistry.PHYSCANNON_LAUNCH2.isPresent()) {
                sound = living.getRandom().nextBoolean()
                        ? SoundEventsRegistry.PHYSCANNON_LAUNCH1.get()
                        : SoundEventsRegistry.PHYSCANNON_LAUNCH2.get();
            }
        }

        if (sound != null) {
            level.playSound((Player) null, living.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // ✅ Fuerza (igual jar)
        double base   = supercharged ? 2.2D : 1.6D;
        double scale  = supercharged ? 5.0D : 3.2D;
        double factor = base + scale / (dist + 0.25D);

        double yBoost = supercharged ? 0.14D : 0.10D;

        Vec3 motion = look.scale(factor).add(0.0D, yBoost, 0.0D);

        target.setDeltaMovement(motion);
        target.hurtMarked = true;
        target.hasImpulse = true;

        // ✅ Sync si es jugador
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(target.getId(), target.getDeltaMovement()));
        }

        // ✅ FX (throw beam 2 ticks): para el que dispara + tracking
        float fxDist = supercharged ? 19.0F : 12.0F;
        if (living instanceof ServerPlayer serverPlayer) {
            PacketThrowEffect fx = new PacketThrowEffect(fxDist, supercharged);
            NetworkHandler.sendToClient(fx, serverPlayer);
            NetworkHandler.sendToTracking(serverPlayer, fx);
        }

        // ✅ Swing (jar lo hacía MAIN_HAND)
        living.swing(InteractionHand.MAIN_HAND, true);

        return true;
    }

    private static ItemStack getGravityGun(LivingEntity living) {
        ItemStack main = living.getMainHandItem();
        if (main.getItem() instanceof GravityGunItem) return main;

        ItemStack off = living.getOffhandItem();
        if (off.getItem() instanceof GravityGunItem) return off;

        return ItemStack.EMPTY;
    }

    public static boolean tryGrab(LivingEntity living, int chargeTime) {
        return false;
    }
}