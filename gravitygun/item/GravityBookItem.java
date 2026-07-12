package com.zerokg2004.gravitygun.gravitygun.item;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

public class GravityBookItem extends Item {

    public GravityBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            // ✅ Llamada segura que no crashea el servidor
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProxy.openGravityBook());
        } else {
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN,
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

    private boolean isSpanishLang() {
        // Ojo: Minecraft.getInstance() aquí puede dar error en servidor,
        // pero lo dejamos como pediste para probar.
        Minecraft mc = Minecraft.getInstance();
        if (mc.getLanguageManager() == null) return false;
        String langCode = mc.getLanguageManager().getSelected();
        if (langCode == null) return false;
        return langCode.toLowerCase().startsWith("es");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean spanish = isSpanishLang();

        String line1 = spanish ? "Haz clic derecho para abrir el libro." : "Right-click to open the book.";
        String line2 = spanish
                ? "Este libro muestra la receta de la Gravity Gun, la obtención de la Supercharged Gravity Gun, permite cambiar entre los modelos 2D y 3D de la Gravity Gun y tiene info."
                : "This book shows the Gravity Gun recipe, how to get the Supercharged version, allows switching between 2D and 3D models, and contains info.";

        tooltip.add(Component.literal(line1)
                .withStyle(Style.EMPTY.withItalic(true).withColor(TextColor.fromRgb(0xAAAAAA))));
        tooltip.add(Component.literal(line2)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))));
    }

    // ✅ LA CLASE DEBE IR AQUÍ (FUERA DE LOS MÉTODOS)
    private static class ClientProxy {
        @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
        public static void openGravityBook() {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.zerokg2004.gravitygun.gravitygun.client.gui.GravityBook()
            );
        }
    }
} //