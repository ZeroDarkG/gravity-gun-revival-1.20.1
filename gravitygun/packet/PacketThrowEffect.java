package com.zerokg2004.gravitygun.gravitygun.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientOnly.handle(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void handle(PacketThrowEffect msg) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;

            com.zerokg2004.gravitygun.gravitygun.client.render.LightningFX.ThrowFXState.trigger(msg.dist, msg.supercharged);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
