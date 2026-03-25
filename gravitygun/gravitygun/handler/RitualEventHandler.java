package com.zerokg2004.gravitygun.gravitygun.handler;

import com.zerokg2004.gravitygun.gravitygun.Gravitygun;
import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gravitygun.MODID)
public class RitualEventHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        BlockPos clickedPos = event.getPos();

        if (!stack.is(ModItems.GRAVITY_GUN.get())) return;

        // Solo si hace clic derecho sobre el centro de la estructura (capa 3)
        if (!level.getBlockState(clickedPos).is(Blocks.COPPER_BLOCK)) return;

        if (isValidRitualStructure(level, clickedPos)) {
            performRitual(level, clickedPos, player, stack);
            destroyStructure(level, clickedPos);
            event.setCanceled(true); // Evita usar el ítem en el bloque
        }
    }

    private static void performRitual(Level level, BlockPos pos, Player player, ItemStack usedItem) {
        System.out.println("⚡ Ritual activado por clic con Gravity Gun!");

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            lightning.setCause(null); // ⚠️ Esto evita que asocie el rayo a un atacante
            lightning.setVisualOnly(true); // ✅ Esto lo vuelve solo decorativo
            level.addFreshEntity(lightning);
        }

        level.playSound(null, pos, SoundEventsRegistry.PHYSCANNON_CHARGE.get(), SoundSource.BLOCKS, 1.5F, 1.0F);

        usedItem.shrink(1); // Quita la Gravity Gun

        ItemStack upgraded = new ItemStack(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
        if (!player.addItem(upgraded)) {
            // Si no entra en el inventario, lo lanza
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, upgraded));
        }

        System.out.println("✅ Ritual completado.");
    }

    private static boolean isValidRitualStructure(Level level, BlockPos posCapa3) {
        BlockPos centerYneg1 = posCapa3.below();      // capa 2
        BlockPos centerYneg2 = centerYneg1.below();   // capa 1

        // Capa 2
        if (!level.getBlockState(centerYneg1).is(Blocks.REDSTONE_BLOCK)) return false;
        if (!level.getBlockState(centerYneg1.north()).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg1.south()).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg1.east()).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg1.west()).is(Blocks.COPPER_BLOCK)) return false;

        // Capa 1
        if (!level.getBlockState(centerYneg2).is(Blocks.DIAMOND_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.north()).is(Blocks.REDSTONE_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.south()).is(Blocks.REDSTONE_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.east()).is(Blocks.REDSTONE_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.west()).is(Blocks.REDSTONE_BLOCK)) return false;

        if (!level.getBlockState(centerYneg2.north().east()).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.north().west()).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.south().east()).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.south().west()).is(Blocks.COPPER_BLOCK)) return false;

        if (!level.getBlockState(centerYneg2.north(2)).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.south(2)).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.east(2)).is(Blocks.COPPER_BLOCK)) return false;
        if (!level.getBlockState(centerYneg2.west(2)).is(Blocks.COPPER_BLOCK)) return false;

        return true;
    }

    private static void destroyStructure(Level level, BlockPos posCapa3) {
        BlockPos centerYneg1 = posCapa3.below();
        BlockPos centerYneg2 = centerYneg1.below();

        BlockPos[] blocksToDestroy = {
                posCapa3,

                centerYneg1,
                centerYneg1.north(), centerYneg1.south(),
                centerYneg1.east(), centerYneg1.west(),

                centerYneg2,
                centerYneg2.north(), centerYneg2.north(2),
                centerYneg2.south(), centerYneg2.south(2),
                centerYneg2.east(), centerYneg2.east(2),
                centerYneg2.west(), centerYneg2.west(2),

                centerYneg2.north().east(),
                centerYneg2.north().west(),
                centerYneg2.south().east(),
                centerYneg2.south().west()
        };

        for (BlockPos block : blocksToDestroy) {
            level.destroyBlock(block, false);
        }
    }
}