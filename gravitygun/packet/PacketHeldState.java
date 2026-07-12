package com.zerokg2004.gravitygun.gravitygun.packet;

import com.zerokg2004.gravitygun.gravitygun.handler.HeldObjectTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketHeldState {

    private final boolean holding;

    public PacketHeldState(boolean holding) {
        this.holding = holding;
    }

    public static void encode(PacketHeldState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.holding);
    }

    public static PacketHeldState decode(FriendlyByteBuf buf) {
        return new PacketHeldState(buf.readBoolean());
    }

    public static void handle(PacketHeldState msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientOnly.handle(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void handle(PacketHeldState msg) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                HeldObjectTracker.setClientHolding(mc.player.getUUID(), msg.holding);
            }
        }
    }
}
