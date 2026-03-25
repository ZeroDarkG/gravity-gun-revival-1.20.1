package com.zerokg2004.gravitygun.gravitygun.packet;

import com.zerokg2004.gravitygun.gravitygun.common.GravityGunHelper;
import com.zerokg2004.gravitygun.gravitygun.entity.EntityLiftedBlock;
import com.zerokg2004.gravitygun.gravitygun.handler.HeldObjectTracker;
import com.zerokg2004.gravitygun.gravitygun.handler.NetworkHandler;
import com.zerokg2004.gravitygun.gravitygun.item.GravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.item.SuperchargedGravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry;
import com.zerokg2004.gravitygun.gravitygun.handler.GravityGunThrownHandler; // ✅ IMPORT CORRECTO

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketThrowEntity {

    private final boolean throwHeld;

    public PacketThrowEntity(boolean throwHeld) {
        this.throwHeld = throwHeld;
    }

    public static void encode(PacketThrowEntity msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.throwHeld);
    }

    public static PacketThrowEntity decode(FriendlyByteBuf buf) {
        return new PacketThrowEntity(buf.readBoolean());
    }

    public static void handle(PacketThrowEntity msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 1) Rama PUSH (cuando no estás sujetando o quieres empujar)
            if (!msg.throwHeld) {
                boolean pushed = GravityGunHelper.tryPush(player);
                if (!pushed && SoundEventsRegistry.PHYSCANNON_DRYFIRE.isPresent()) {
                    player.level().playSound(
                            null,
                            player.blockPosition(),
                            SoundEventsRegistry.PHYSCANNON_DRYFIRE.get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F
                    );
                }
                return;
            }

            // 2) Rama THROW (lanzar held)
            UUID uuid = player.getUUID();
            if (!HeldObjectTracker.isHolding(uuid)) return;

            Entity held = HeldObjectTracker.getHeld(uuid);
            if (held == null || !held.isAlive() || held.isRemoved()) return;
            if (held.level() != player.level()) return;

            ItemStack main = player.getMainHandItem();
            ItemStack off  = player.getOffhandItem();

            InteractionHand gunHand;
            ItemStack gunStack;

            if (main.getItem() instanceof GravityGunItem) {
                gunHand = InteractionHand.MAIN_HAND;
                gunStack = main;
            } else if (off.getItem() instanceof GravityGunItem) {
                gunHand = InteractionHand.OFF_HAND;
                gunStack = off;
            } else {
                return;
            }

            boolean supercharged = gunStack.getItem() instanceof SuperchargedGravityGunItem;

            float fxDist = supercharged ? 19.0f : 12.0f;

            // ✅ FUERZA SEPARADA: bloques vs entidades
            boolean isBlock = held instanceof EntityLiftedBlock;

            double force;
            double yBoost;

            if (isBlock) {
                // BLOQUES: mucho más suave (evita misilazos)
                force  = supercharged ? 2.8 : 1.8;
                yBoost = supercharged ? 0.10 : 0.07;
            } else {
                // ENTIDADES: un poco más fuerte que bloques, pero controlado
                force  = supercharged ? 3.6 : 2.2;
                yBoost = supercharged ? 0.14 : 0.10;
            }

            SoundEvent sound = null;
            if (supercharged) {
                if (SoundEventsRegistry.SUPERPHYS_LAUNCH1.isPresent() && SoundEventsRegistry.SUPERPHYS_LAUNCH2.isPresent()) {
                    sound = player.getRandom().nextBoolean()
                            ? SoundEventsRegistry.SUPERPHYS_LAUNCH1.get()
                            : SoundEventsRegistry.SUPERPHYS_LAUNCH2.get();
                }
            } else {
                if (SoundEventsRegistry.PHYSCANNON_LAUNCH1.isPresent() && SoundEventsRegistry.PHYSCANNON_LAUNCH2.isPresent()) {
                    sound = player.getRandom().nextBoolean()
                            ? SoundEventsRegistry.PHYSCANNON_LAUNCH1.get()
                            : SoundEventsRegistry.PHYSCANNON_LAUNCH2.get();
                }
            }

            if (sound != null) {
                player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            PacketThrowEffect fx = new PacketThrowEffect(fxDist, supercharged);
            NetworkHandler.sendToClient(fx, player);
            NetworkHandler.sendToTracking(player, fx);

            // Soltar flags del "held"
            if (held instanceof EntityLiftedBlock lb) {
                lb.clearHolder();
            }
            if (held instanceof LivingEntity le) {
                le.setNoGravity(false);
            }

            // Impulso
            Vec3 look = player.getLookAngle().normalize();
            Vec3 vel = look.scale(force).add(0.0, yBoost, 0.0);

            // ✅ cap opcional (seguridad anti-misil). Déjalo si quieres estabilidad.
            double max = supercharged ? (isBlock ? 3.6 : 4.2) : (isBlock ? 2.6 : 3.2);
            if (vel.length() > max) vel = vel.normalize().scale(max);

            held.setDeltaMovement(vel);
            held.hurtMarked = true;
            held.hasImpulse = true;

            // ✅ Marca centralizada (evita duplicar tags a mano)
            GravityGunThrownHandler.markThrown(held, player, supercharged);

            // Suelta del tracker
            HeldObjectTracker.release(uuid);
            player.swing(gunHand, true);
        });

        ctx.get().setPacketHandled(true);
    }
}