package com.zerokg2004.gravitygun.gravitygun.packet;

import com.zerokg2004.gravitygun.gravitygun.client.render.LightningFX;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketThrowEffect {

    private final float dist;
    private final boolean supercharged;

    public PacketThrowEffect(float dist, boolean supercharged) {
        this.dist = dist;
        this.supercharged = supercharged;
    }

    public static void encode(PacketThrowEffect msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.dist);
        buf.writeBoolean(msg.supercharged);
    }

    public static PacketThrowEffect decode(FriendlyByteBuf buf) {
        return new PacketThrowEffect(buf.readFloat(), buf.readBoolean());
    }

    public static void handle(PacketThrowEffect msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // ✅ ACTIVA EL RAYO 2 TICKS EN CLIENTE
            LightningFX.ThrowFXState.trigger(msg.dist, msg.supercharged);
        });
        ctx.get().setPacketHandled(true);
    }
}