package com.zerokg2004.gravitygun.gravitygun.packet;

import com.zerokg2004.gravitygun.gravitygun.client.render.LightningFX;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketThrowEffectFX {

    private final float dist;
    private final boolean supercharged;

    public PacketThrowEffectFX(float dist, boolean supercharged) {
        this.dist = dist;
        this.supercharged = supercharged;
    }

    public static void encode(PacketThrowEffectFX msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.dist);
        buf.writeBoolean(msg.supercharged);
    }

    public static PacketThrowEffectFX decode(FriendlyByteBuf buf) {
        return new PacketThrowEffectFX(buf.readFloat(), buf.readBoolean());
    }

    public static void handle(PacketThrowEffectFX msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Guarda estado 2 ticks en cliente
            LightningFX.ThrowFXState.trigger(msg.dist, msg.supercharged);
        });
        ctx.get().setPacketHandled(true);
    }
}