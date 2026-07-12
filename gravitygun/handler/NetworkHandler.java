package com.zerokg2004.gravitygun.gravitygun.handler;

import com.zerokg2004.gravitygun.gravitygun.packet.PacketThrowEffect;
import com.zerokg2004.gravitygun.gravitygun.packet.PacketThrowEntity;
import com.zerokg2004.gravitygun.gravitygun.packet.PacketHeldState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("gravitygun", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {

        // ✅ Cliente -> Servidor (disparar/lanzar)
        INSTANCE.messageBuilder(PacketThrowEntity.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketThrowEntity::encode)
                .decoder(PacketThrowEntity::decode)
                .consumerMainThread(PacketThrowEntity::handle)
                .add();

        // ✅ Servidor -> Cliente (efecto visual/sonido si aplica)
        INSTANCE.messageBuilder(PacketThrowEffect.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketThrowEffect::encode)
                .decoder(PacketThrowEffect::decode)
                .consumerMainThread(PacketThrowEffect::handle)
                .add();

        INSTANCE.messageBuilder(PacketHeldState.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketHeldState::encode)
                .decoder(PacketHeldState::decode)
                .consumerMainThread(PacketHeldState::handle)
                .add();
    }

    public static void sendToClient(Object msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToTracking(Entity entity, Object msg) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }
}
