package com.zerokg2004.gravitygun.gravitygun.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

public final class LightningFX {
    private LightningFX() {}

    // ============================================================
    // ✅ Estado del “4º rayo” (throw) - CLIENTE
    // ============================================================
    public static final class ThrowFXState {
        private ThrowFXState() {}

        private static int startTick = -1;
        private static int untilTick = -1;
        private static float dist = 0f;
        private static boolean supercharged = false;

        public static void trigger(float d, boolean isSupercharged) {
            // tick del cliente
            var p = Minecraft.getInstance().player;
            int t = (p != null) ? p.tickCount : 0;

            startTick = t;
            untilTick = t + 4;
            dist = d;
            supercharged = isSupercharged;
        }

        public static boolean active(int tick) {
            return tick <= untilTick;
        }

        public static float dist() {
            return dist;
        }

        public static boolean supercharged() {
            return supercharged;
        }

        public static float recoilStrength(float tickTime) {
            if (startTick < 0 || tickTime > untilTick) {
                return 0.0F;
            }

            float duration = Math.max(1.0F, untilTick - startTick);
            float progress = Mth.clamp((tickTime - startTick) / duration, 0.0F, 1.0F);

            if (progress < 0.18F) {
                return Mth.clamp(progress / 0.18F, 0.0F, 1.0F);
            }

            float settle = 1.0F - ((progress - 0.18F) / 0.82F);
            return settle * settle;
        }
    }

    /**
     * ✅ Helper: dibuja el 4º rayo SOLO si está activo (2 ticks).
     *
     * IMPORTANTÍSIMO:
     * - LLÁMALO cuando tu poseStack ya esté posicionado/orientado en el arma,
     *   idealmente en el cañón.
     * - Este método asume que el rayo sale en local hacia +X.
     */
    public static void renderThrowBeamIfActive(
            PoseStack poseStack,
            MultiBufferSource buffer,
            long seed,
            int tick,
            int segments,
            float jitter,
            float baseThickness,
            float baseAlpha
    ) {
        if (!ThrowFXState.active(tick)) return;

        int color = ThrowFXState.supercharged() ? 0x91E0AE : 0xFFF3D9;
        float dist = ThrowFXState.dist();

        // rayo largo hacia delante del arma (local +X)
        renderBeamBetween6(
                poseStack, buffer, seed ^ 0xBEEFL, tick,
                segments, jitter, baseThickness,
                color, baseAlpha,
                0f, 0f, 0f,
                dist, 0f, 0f
        );
    }

    /**
     * ✅ API principal: rayo entre dos puntos, formado por 6 capas coherentes (todas comparten la MISMA ruta).
     * - En el espacio local, el rayo avanza por +X.
     * - Genera una polilínea base y luego la dibuja 6 veces (distinto grosor/tono/alpha).
     */
    public static void renderBeamBetween6(
            PoseStack poseStack,
            MultiBufferSource buffer,
            long seed,
            int tick,
            int segments,
            float jitter,
            float baseThickness,
            int baseColorRGB,      // 0xRRGGBB
            float baseAlpha,
            float sx, float sy, float sz,
            float ex, float ey, float ez
    ) {
        float br = ((baseColorRGB >> 16) & 255) / 255f;
        float bg = ((baseColorRGB >> 8) & 255) / 255f;
        float bb = (baseColorRGB & 255) / 255f;

        float dx = ex - sx, dy = ey - sy, dz = ez - sz;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4f) return;

        dx /= len; dy /= len; dz /= len;

        float ax = 0.0f;
        float ay = -dz;
        float az = dy;
        float axisLen = Mth.sqrt(ax * ax + ay * ay + az * az);

        poseStack.pushPose();
        poseStack.translate(sx, sy, sz);

        if (axisLen > 1.0e-6f) {
            ax /= axisLen; ay /= axisLen; az /= axisLen;
            float dot = Mth.clamp(dx, -1.0f, 1.0f);
            float angle = (float) Math.acos(dot);
            poseStack.mulPose(new Quaternionf().fromAxisAngleRad(ax, ay, az, angle));
        } else if (dx < 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        }

        Vector3f[] pts = buildBoltPoints(seed, tick, segments, len, jitter);

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        Matrix4f mat = poseStack.last().pose();

        for (int layer = 1; layer <= 6; layer++) {
            float t = (layer - 1) / 5.0f;

            float thick = baseThickness * (0.65f + 1.70f * t);

            float lr = Mth.clamp(br * (1.05f - 0.18f * t), 0f, 1f);
            float lg = Mth.clamp(bg * (1.00f - 0.28f * t), 0f, 1f);
            float lb = Mth.clamp(bb * (0.95f - 0.40f * t), 0f, 1f);

            float alpha = baseAlpha * (0.70f - 0.08f * layer);

            renderBoltFromPoints(mat, vc, pts, thick, lr, lg, lb, alpha);
        }

        poseStack.popPose();
    }

    public static void renderClassicBeamBetween(
            PoseStack poseStack,
            MultiBufferSource buffer,
            long seed,
            int tick,
            int bends,
            int spread,
            float layerSize,
            int layerCount,
            float intensity,
            int baseColorRGB,
            float baseAlpha,
            float sx, float sy, float sz,
            float ex, float ey, float ez
    ) {
        float br = ((baseColorRGB >> 16) & 255) / 255f;
        float bg = ((baseColorRGB >> 8) & 255) / 255f;
        float bb = (baseColorRGB & 255) / 255f;

        float dx = ex - sx;
        float dy = ey - sy;
        float dz = ez - sz;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4f || bends < 2) return;

        poseStack.pushPose();
        poseStack.translate(sx, sy, sz);
        orientLocalYToVector(poseStack, dx / len, dy / len, dz / len);
        Matrix4f mat = poseStack.last().pose();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        double[] xs = new double[bends];
        double[] zs = new double[bends];
        double accX = 0.0D;
        double accZ = 0.0D;
        Random lightningRand = new Random(seed);

        for (int i = bends - 1; i >= 0; i--) {
            xs[i] = accX;
            zs[i] = accZ;
            accX += spread > 0 ? lightningRand.nextInt(spread * 2 + 1) - spread : 0;
            accZ += spread > 0 ? lightningRand.nextInt(spread * 2 + 1) - spread : 0;
        }

        double heightOfBend = len / bends;

        for (int layer = 0; layer < layerCount; layer++) {
            Random layerRand = new Random(seed);
            double d5 = 0.0D;
            double d6 = 0.0D;

            for (int i = bends - 1; i >= 0; i--) {
                double d7 = d5;
                double d8 = d6;
                d5 += spread > 0 ? layerRand.nextInt(spread * 2 + 1) - spread : 0;
                d6 += spread > 0 ? layerRand.nextInt(spread * 2 + 1) - spread : 0;
                if (i == 0) {
                    d5 = 0.0D;
                    d6 = 0.0D;
                }

                Tesselator tessellator = Tesselator.getInstance();
                BufferBuilder builder = tessellator.getBuilder();
                builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

                double d9 = 0.1D + layer * layerSize;
                d9 *= i * 0.1D + 0.5D;
                double d10 = 0.1D + layer * layerSize;
                d10 *= (i - 1) * 0.1D + 0.5D;

                float alpha = baseAlpha * (0.34F - 0.025F * layer);
                float rr = Mth.clamp(br * intensity, 0.0F, 1.0F);
                float gg = Mth.clamp(bg * intensity, 0.0F, 1.0F);
                float bbv = Mth.clamp(bb * intensity, 0.0F, 1.0F);

                for (int j = 0; j < 5; j++) {
                    double x1 = 0.5D - d9;
                    double z1 = 0.5D - d9;
                    if (j == 1 || j == 2) x1 += d9 * 2.0D;
                    if (j == 2 || j == 3) z1 += d9 * 2.0D;

                    double x2 = 0.5D - d10;
                    double z2 = 0.5D - d10;
                    if (j == 1 || j == 2) x2 += d10 * 2.0D;
                    if (j == 2 || j == 3) z2 += d10 * 2.0D;

                    builder.vertex(mat, (float) (x2 + d5), (float) (i * heightOfBend), (float) (z2 + d6))
                            .color(rr, gg, bbv, alpha)
                            .endVertex();
                    builder.vertex(mat, (float) (x1 + d7), (float) ((i + 1) * heightOfBend), (float) (z1 + d8))
                            .color(rr, gg, bbv, alpha)
                            .endVertex();
                }

                BufferUploader.drawWithShader(builder.end());
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);

        poseStack.popPose();
    }

    public static void renderClassicLocal(
            PoseStack poseStack,
            MultiBufferSource buffer,
            long seed,
            int tick,
            double x,
            double y,
            double z,
            int bends,
            int spread,
            double heightOfBend,
            int layerCount,
            double layerSize,
            float intensity,
            int baseColorRGB,
            float baseAlpha
    ) {
        float br = ((baseColorRGB >> 16) & 255) / 255f;
        float bg = ((baseColorRGB >> 8) & 255) / 255f;
        float bb = (baseColorRGB & 255) / 255f;
        Matrix4f mat = poseStack.last().pose();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        double[] xs = new double[bends];
        double[] zs = new double[bends];
        double accX = 0.0D;
        double accZ = 0.0D;
        Random lightningRand = new Random(seed);

        for (int i = bends - 1; i >= 0; i--) {
            xs[i] = accX;
            zs[i] = accZ;
            accX += spread > 0 ? lightningRand.nextInt(spread * 2 + 1) - spread : 0;
            accZ += spread > 0 ? lightningRand.nextInt(spread * 2 + 1) - spread : 0;
        }

        for (int layer = 0; layer < layerCount; layer++) {
            Random layerRand = new Random(seed);
            double d5 = 0.0D;
            double d6 = 0.0D;

            for (int i = bends - 1; i >= 0; i--) {
                double d7 = d5;
                double d8 = d6;
                d5 += spread > 0 ? layerRand.nextInt(spread * 2 + 1) - spread : 0;
                d6 += spread > 0 ? layerRand.nextInt(spread * 2 + 1) - spread : 0;
                if (i == 0) {
                    d5 = 0.0D;
                    d6 = 0.0D;
                }

                Tesselator tessellator = Tesselator.getInstance();
                BufferBuilder builder = tessellator.getBuilder();
                builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

                double d9 = 0.1D + layer * layerSize;
                d9 *= i * 0.1D + 0.5D;
                double d10 = 0.1D + layer * layerSize;
                d10 *= (i - 1) * 0.1D + 0.5D;

                float alpha = baseAlpha * (0.3F);
                float rr = Mth.clamp(br * intensity, 0.0F, 1.0F);
                float gg = Mth.clamp(bg * intensity, 0.0F, 1.0F);
                float bbv = Mth.clamp(bb * intensity, 0.0F, 1.0F);

                for (int j = 0; j < 5; j++) {
                    double x1 = x + 0.5D - d9;
                    double z1 = z + 0.5D - d9;
                    if (j == 1 || j == 2) x1 += d9 * 2.0D;
                    if (j == 2 || j == 3) z1 += d9 * 2.0D;

                    double x2 = x + 0.5D - d10;
                    double z2 = z + 0.5D - d10;
                    if (j == 1 || j == 2) x2 += d10 * 2.0D;
                    if (j == 2 || j == 3) z2 += d10 * 2.0D;

                    builder.vertex(mat, (float) (x2 + d5), (float) (y + i * heightOfBend), (float) (z2 + d6))
                            .color(rr, gg, bbv, alpha)
                            .endVertex();
                    builder.vertex(mat, (float) (x1 + d7), (float) (y + (i + 1) * heightOfBend), (float) (z1 + d8))
                            .color(rr, gg, bbv, alpha)
                            .endVertex();
                }

                BufferUploader.drawWithShader(builder.end());
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    public static void renderLightning(
            PoseStack poseStack,
            MultiBufferSource buffer,
            long seed,
            int segments,
            float length,
            float jitter,
            float thickness,
            float r, float g, float b, float a
    ) {
        Random rand = new Random(seed);

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        Matrix4f mat = poseStack.last().pose();

        Vector3f prev = new Vector3f(0, 0, 0);
        Vector3f cur = new Vector3f();

        float segLen = length / (float) segments;
        float thick = thickness * 1.15f;

        for (int i = 1; i <= segments; i++) {
            float x = segLen * i;
            float y = (rand.nextFloat() - 0.5f) * jitter;
            float z = (rand.nextFloat() - 0.5f) * jitter;

            cur.set(x, y, z);

            addQuad(mat, vc, prev, cur, thick, 0, 1, 0, r, g, b, a);
            addQuad(mat, vc, prev, cur, thick, 0, 0, 1, r, g, b, a);

            prev.set(cur);
        }
    }

    private static Vector3f[] buildBoltPoints(long seed, int tick, int segments, float length, float jitter) {
        long animSeed = seed ^ (tick * 31L);
        Random rand = new Random(animSeed);

        Vector3f[] pts = new Vector3f[segments + 1];
        pts[0] = new Vector3f(0, 0, 0);

        float segLen = length / segments;
        float phase = (tick % 360) * 0.017453292f;

        for (int i = 1; i <= segments; i++) {
            float x = segLen * i;

            float ry = (rand.nextFloat() - 0.5f) * jitter;
            float rz = (rand.nextFloat() - 0.5f) * jitter;

            float wave  = (float) Math.sin(phase + i * 0.65f) * (jitter * 0.30f);
            float wave2 = (float) Math.cos(phase * 1.3f + i * 0.55f) * (jitter * 0.30f);

            pts[i] = new Vector3f(x, ry + wave, rz + wave2);
        }
        return pts;
    }

    private static void orientLocalYToVector(PoseStack poseStack, float dx, float dy, float dz) {
        Vector3f from = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f to = new Vector3f(dx, dy, dz).normalize();

        float dot = Mth.clamp(from.dot(to), -1.0f, 1.0f);
        if (dot > 0.9999f) {
            return;
        }
        if (dot < -0.9999f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            return;
        }

        Vector3f axis = from.cross(to, new Vector3f()).normalize();
        float angle = (float) Math.acos(dot);
        poseStack.mulPose(new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, angle));
    }

    private static void renderBoltFromPoints(
            Matrix4f mat,
            VertexConsumer vc,
            Vector3f[] pts,
            float thickness,
            float r, float g, float b, float a
    ) {
        for (int i = 0; i < pts.length - 1; i++) {
            Vector3f from = pts[i];
            Vector3f to = pts[i + 1];

            addQuad(mat, vc, from, to, thickness, 0, 1, 0, r, g, b, a);
            addQuad(mat, vc, from, to, thickness, 0, 0, 1, r, g, b, a);
        }
    }

    private static void addQuad(
            Matrix4f mat,
            VertexConsumer vc,
            Vector3f from,
            Vector3f to,
            float halfWidth,
            float nx, float ny, float nz,
            float r, float g, float b, float a
    ) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float dz = to.z - from.z;

        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4f) return;

        dx /= len; dy /= len; dz /= len;

        float px = dy * nz - dz * ny;
        float py = dz * nx - dx * nz;
        float pz = dx * ny - dy * nx;

        float plen = Mth.sqrt(px * px + py * py + pz * pz);
        if (plen < 1.0e-4f) {
            px = 0; py = 1; pz = 0;
            plen = 1;
        }

        px = (px / plen) * halfWidth;
        py = (py / plen) * halfWidth;
        pz = (pz / plen) * halfWidth;

        float x1 = from.x + px, y1 = from.y + py, z1 = from.z + pz;
        float x2 = from.x - px, y2 = from.y - py, z2 = from.z - pz;
        float x3 = to.x   - px, y3 = to.y   - py, z3 = to.z   - pz;
        float x4 = to.x   + px, y4 = to.y   + py, z4 = to.z   + pz;

        vc.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        vc.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        vc.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
        vc.vertex(mat, x4, y4, z4).color(r, g, b, a).endVertex();
    }

}
