package com.zerokg2004.gravitygun.gravitygun.client;

import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "gravitygun", value = Dist.CLIENT)
public class BlockBreakCancelHandler {

    @SubscribeEvent
    public static void onLeftClickBlock(InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (held.is(ModItems.GRAVITY_GUN.get()) || held.is(ModItems.GRAVITY_GUN_SUPERCHARGED.get())) {
            event.setSwingHand(false);  // Cancela animación de ataque
            event.setCanceled(true);    // Cancela evento en sí (evita partículas y daño)
        }
    }
}