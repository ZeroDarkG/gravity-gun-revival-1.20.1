package com.zerokg2004.gravitygun.gravitygun.client;

import com.zerokg2004.gravitygun.gravitygun.registry.Config;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;

public class RenderModeHandler {

    public enum Mode {
        AUTO,
        SIMPLE,
        DETAILED,
        PLANE
    }

    private static Mode currentMode = Mode.AUTO;

    public static Mode getMode() {
        return currentMode;
    }

    public static void setMode(Mode mode) {
        currentMode = mode;
    }

    public static boolean isForce2D() {
        return currentMode == Mode.PLANE;
    }

    public static boolean isDetailed() {
        if (currentMode == Mode.DETAILED) return true;
        if (currentMode == Mode.SIMPLE || currentMode == Mode.PLANE) return false;

        if (currentMode == Mode.AUTO) {
            GraphicsStatus graphicsStatus = Minecraft.getInstance().options.graphicsMode().get();
            return graphicsStatus != GraphicsStatus.FAST || !Config.simpleModelFastGraphics;
        }
        return false;
    }

    public static boolean isSimple() {
        if (currentMode == Mode.SIMPLE) return true;
        if (currentMode == Mode.AUTO) {
            return Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST
                    && Config.simpleModelFastGraphics;
        }
        return false;
    }

    public static void toggle() {
        currentMode = switch (currentMode) {
            case AUTO -> Mode.PLANE;
            case PLANE -> Mode.SIMPLE;
            case SIMPLE -> Mode.DETAILED;
            case DETAILED -> Mode.AUTO;
        };
    }
}
