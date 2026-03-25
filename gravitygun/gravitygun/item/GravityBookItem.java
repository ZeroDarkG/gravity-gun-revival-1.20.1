package com.zerokg2004.gravitygun.gravitygun.item;

import com.zerokg2004.gravitygun.gravitygun.client.gui.GravityBook;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class GravityBookItem extends Item {

    public GravityBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new GravityBook());
        } else {
            level.playSound(
                    player,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, // más temático
                    SoundSource.PLAYERS,
                    1.0f, 1.0f
            );
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Gravity Book")
                .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFD700)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Haz clic derecho para abrir el libro.")
                .withStyle(Style.EMPTY.withItalic(true).withColor(TextColor.fromRgb(0xAAAAAA))));
        tooltip.add(Component.literal("Permite cambiar entre los modelos 2D y 3D de la Gravity Gun."));
    }
}