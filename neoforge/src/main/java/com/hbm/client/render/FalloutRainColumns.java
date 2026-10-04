// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class FalloutRainColumns {
    private static final int TABLE_SIZE = 32;
    private static final int TABLE_HALF = 16;
    private static final int TABLE_MASK = TABLE_SIZE - 1;
    private static final float[] RAIN_X = new float[TABLE_SIZE * TABLE_SIZE];
    private static final float[] RAIN_Z = new float[TABLE_SIZE * TABLE_SIZE];

    static {
        for (int z = 0; z < TABLE_SIZE; z++) {
            for (int x = 0; x < TABLE_SIZE; x++) {
                float dx = x - TABLE_HALF;
                float dz = z - TABLE_HALF;
                float len = Mth.length(dx, dz);
                RAIN_X[z << 5 | x] = -dz / len;
                RAIN_Z[z << 5 | x] = dx / len;
            }
        }
    }

    private FalloutRainColumns() {}

    public static Frame capture(
            ClientLevel level,
            double entityX,
            double entityY,
            double entityZ,
            Vec3 cameraPos,
            float ageInTicks) {
        int cameraX = Mth.floor(cameraPos.x);
        int cameraY = Mth.floor(cameraPos.y);
        int cameraZ = Mth.floor(cameraPos.z);
        int layerCount =
                Math.min(10, Math.max(5, Minecraft.getInstance().options.weatherRadius().get()));
        int side = layerCount * 2 + 1;
        float[] vertices = new float[side * side * 10];
        int[] colors = new int[side * side];
        int[] lights = new int[side * side];
        int wholeTick = Mth.floor(ageInTicks);
        float partialTick = ageInTicks - wholeTick;
        Random random = new Random();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 0;

        for (int z = cameraZ - layerCount; z <= cameraZ + layerCount; z++) {
            for (int x = cameraX - layerCount; x <= cameraX + layerCount; x++) {
                int tableIndex =
                        ((z - cameraZ + TABLE_HALF) & TABLE_MASK) * TABLE_SIZE
                                + ((x - cameraX + TABLE_HALF) & TABLE_MASK);
                float halfX = RAIN_X[tableIndex] * 0.5F;
                float halfZ = RAIN_Z[tableIndex] * 0.5F;

                int rainHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int minY = cameraY - layerCount;
                int maxY = cameraY + layerCount;
                if (minY < rainHeight) minY = rainHeight;
                if (maxY < rainHeight) maxY = rainHeight;
                if (minY == maxY) continue;

                int lightY = Math.max(rainHeight, cameraY);
                int sampled = LightCoordsUtil.getLightCoords(level, pos.set(x, lightY, z));
                int light =
                        LightCoordsUtil.pack(
                                (LightCoordsUtil.block(sampled) * 3 + 15) / 4,
                                (LightCoordsUtil.sky(sampled) * 3 + 15) / 4);
                random.setSeed(
                        (long) x * x * 3121L + (long) x * 45238971L
                                ^ (long) z * z * 418711L + (long) z * 13761L);

                float fallSpeed = 1.0F;
                float swayLoop = ((wholeTick & 511) + partialTick) / 512.0F;
                float fallVariation = 0.4F + random.nextFloat() * 0.2F;
                float swayVariation = random.nextFloat();
                double distX = x + 0.5D - cameraPos.x;
                double distZ = z + 0.5D - cameraPos.z;
                float intensityMod = Mth.sqrt((float) (distX * distX + distZ * distZ)) / layerCount;
                int color = ARGB.white(((1.0F - intensityMod * intensityMod) * 0.3F + 0.5F));

                int b = count * 10;
                vertices[b] = (float) (x - entityX - halfX + 0.5D);
                vertices[b + 1] = (float) (minY - entityY);
                vertices[b + 2] = (float) (z - entityZ - halfZ + 0.5D);
                vertices[b + 3] = (float) (x - entityX + halfX + 0.5D);
                vertices[b + 4] = (float) (maxY - entityY);
                vertices[b + 5] = (float) (z - entityZ + halfZ + 0.5D);
                vertices[b + 6] = fallVariation;
                vertices[b + 7] = minY * fallSpeed / 4.0F + swayLoop * fallSpeed + swayVariation;
                vertices[b + 8] = 1.0F + fallVariation;
                vertices[b + 9] = maxY * fallSpeed / 4.0F + swayLoop * fallSpeed + swayVariation;
                colors[count] = color;
                lights[count] = light;
                count++;
            }
        }
        return new Frame(entityX, entityY, entityZ, count, vertices, colors, lights);
    }

    public record Frame(
            double entityX,
            double entityY,
            double entityZ,
            int count,
            float[] vertices,
            int[] colors,
            int[] lights) {
        public void emit(PoseStack.Pose pose, VertexConsumer buffer) {
            for (int i = 0; i < count; i++) {
                int b = i * 10;
                float x0 = vertices[b], y0 = vertices[b + 1], z0 = vertices[b + 2];
                float x1 = vertices[b + 3], y1 = vertices[b + 4], z1 = vertices[b + 5];
                float u0 = vertices[b + 6], v0 = vertices[b + 7];
                float u1 = vertices[b + 8], v1 = vertices[b + 9];
                int color = colors[i], light = lights[i];
                vertex(buffer, pose, x0, y0, z0, u0, v0, color, light);
                vertex(buffer, pose, x1, y0, z1, u1, v0, color, light);
                vertex(buffer, pose, x1, y1, z1, u1, v1, color, light);
                vertex(buffer, pose, x0, y1, z0, u0, v1, color, light);
            }
        }

        private static void vertex(
                VertexConsumer buffer,
                PoseStack.Pose pose,
                float x,
                float y,
                float z,
                float u,
                float v,
                int color,
                int light) {
            Vertices.emit(buffer, pose, x, y, z, color, u, v, light, 0F, 1F, 0F);
        }
    }
}
