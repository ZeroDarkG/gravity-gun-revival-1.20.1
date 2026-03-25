package com.zerokg2004.gravitygun.gravitygun.client;

import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class RenderModeHandler {

    public enum Mode {
        AUTO,      // Usa el modo gráfico (rápido o detallado)
        SIMPLE,    // Forzar modelo 3D simple
        DETAILED,  // Forzar modelo 3D detallado
        PLANE      // Forzar modelo 2D clásico
    }

    private static Mode currentMode = Mode.AUTO;

    public static Mode getMode() {
        return currentMode;
    }

    public static void setMode(Mode mode) {
        currentMode = mode;
    }

    public static void toggle() {
        currentMode = switch (currentMode) {
            case AUTO -> Mode.PLANE;
            case PLANE -> Mode.AUTO;
            default -> Mode.AUTO;
        };
    }

    /**
     * Devuelve true si el modo actual es PLANE y el jugador tiene el Gravity Book.
     */
    public static boolean isForce2D() {
        if (currentMode != Mode.PLANE) return false;

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        return player.getInventory().items.stream()
                .anyMatch(stack -> stack.getItem() == ModItems.GRAVITY_BOOK.get());
    }

    /**
     * Devuelve true si se debe usar el modelo detallado.
     */
    public static boolean isDetailed() {
        return switch (currentMode) {
            case DETAILED -> true;
            case SIMPLE, PLANE -> false;
            case AUTO -> Minecraft.getInstance().options.graphicsMode().get().ordinal() > 0;
        };
    }

    /**
     * Devuelve true si se debe usar el modelo simple.
     */
    public static boolean isSimple() {
        return switch (currentMode) {
            case SIMPLE -> true;
            case DETAILED, PLANE -> false;
            case AUTO -> Minecraft.getInstance().options.graphicsMode().get().ordinal() == 0;
        };
    }
}