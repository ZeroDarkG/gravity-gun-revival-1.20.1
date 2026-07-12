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

        // ✅ AHORA: Verificamos si es CUALQUIER Gravity Gun (Normal o de Color)
        // Usamos una pequeña función auxiliar para no repetir código
        if (!isAnyNormalGravityGun(stack)) return;

        if (!level.getBlockState(clickedPos).is(Blocks.COPPER_BLOCK)) return;

        if (isValidRitualStructure(level, clickedPos)) {
            performRitual(level, clickedPos, player, stack);
            destroyStructure(level, clickedPos);
            event.setCanceled(true);
        }
    }

    // ✅ Función auxiliar para detectar todas las variantes normales
    private static boolean isAnyNormalGravityGun(ItemStack stack) {
        return stack.is(ModItems.GRAVITY_GUN.get()) ||
                stack.is(ModItems.GRAVITY_GUN_RED.get()) ||
                stack.is(ModItems.GRAVITY_GUN_BLUE.get()) ||
                stack.is(ModItems.GRAVITY_GUN_GREEN.get()) ||
                stack.is(ModItems.GRAVITY_GUN_ORANGE.get()) ||
                stack.is(ModItems.GRAVITY_GUN_YELLOW.get()) ||
                stack.is(ModItems.GRAVITY_GUN_PURPLE.get());
    }

    private static void performRitual(Level level, BlockPos pos, Player player, ItemStack usedItem) {
        System.out.println("⚡ Ritual activado!");

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }

        level.playSound(null, pos, SoundEventsRegistry.PHYSCANNON_CHARGE.get(), SoundSource.BLOCKS, 1.5F, 1.0F);

        // ✅ LÓGICA DE MEJORA DINÁMICA:
        // Decidimos qué versión Supercharged dar basándonos en el ítem usado
        ItemStack upgraded;
        if (usedItem.is(ModItems.GRAVITY_GUN_RED.get())) {
            upgraded = new ItemStack(ModItems.RED_SUPERCHARGED_GRAVITY_GUN.get());
        } else if (usedItem.is(ModItems.GRAVITY_GUN_BLUE.get())) {
            upgraded = new ItemStack(ModItems.BLUE_SUPERCHARGED_GRAVITY_GUN.get());
        } else if (usedItem.is(ModItems.GRAVITY_GUN_GREEN.get())) {
            upgraded = new ItemStack(ModItems.GREEN_SUPERCHARGED_GRAVITY_GUN.get());
        } else if (usedItem.is(ModItems.GRAVITY_GUN_ORANGE.get())) {
            upgraded = new ItemStack(ModItems.ORANGE_SUPERCHARGED_GRAVITY_GUN.get());
        } else if (usedItem.is(ModItems.GRAVITY_GUN_YELLOW.get())) {
            upgraded = new ItemStack(ModItems.YELLOW_SUPERCHARGED_GRAVITY_GUN.get());
        } else if (usedItem.is(ModItems.GRAVITY_GUN_PURPLE.get())) {
            upgraded = new ItemStack(ModItems.PURPLE_SUPERCHARGED_GRAVITY_GUN.get());
        } else {
            // Por defecto la azul clásica
            upgraded = new ItemStack(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
        }

        usedItem.shrink(1); // Consumimos la normal

        if (!player.addItem(upgraded)) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, upgraded));
        }

        System.out.println("✅ Ritual completado para la variante: " + upgraded.getDescriptionId());
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