package com.zerokg2004.gravitygun.gravitygun.client;

import net.minecraft.client.Minecraft;

public final class ThrowFXState {
    private ThrowFXState() {}

    private static long untilGameTime = -1L;
    private static float dist = 0f;
    private static boolean supercharged = false;

    public static void trigger(float d, boolean isSupercharged) {
        Minecraft mc = Minecraft.getInstance();
        long t = (mc.level != null) ? mc.level.getGameTime() : 0L;
        untilGameTime = t + 2L;
        dist = d;
        supercharged = isSupercharged;
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        long t = (mc.level != null) ? mc.level.getGameTime() : 0L;
        return t <= untilGameTime;
    }

    public static float dist() { return dist; }
    public static boolean supercharged() { return supercharged; }
}