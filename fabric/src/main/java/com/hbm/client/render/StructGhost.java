// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Block;

public record StructGhost(float x, float y, float z, TextureAtlasSprite sprite) {

    private static final float LO = 11F / 16F / 2F;
    private static final float HI = 1F - LO;
    private static final int ALPHA = 191;

    public static TextureAtlasSprite sprite(Block block) {
        return Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .getParticleMaterial(block.defaultBlockState())
                .sprite();
    }

    public static void cube(PoseStack.Pose pose, VertexConsumer buf, int light, StructGhost g) {
        TextureAtlasSprite sp = g.sprite;
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        float x = g.x, y = g.y, z = g.z;
        float lo = LO, hi = HI;

        v(pose, buf, light, x + hi, y + hi, z + hi, u1, v0);
        v(pose, buf, light, x + lo, y + hi, z + hi, u0, v0);
        v(pose, buf, light, x + lo, y + lo, z + hi, u0, v1);
        v(pose, buf, light, x + hi, y + lo, z + hi, u1, v1);

        v(pose, buf, light, x + hi, y + hi, z + lo, u1, v0);
        v(pose, buf, light, x + hi, y + hi, z + hi, u0, v0);
        v(pose, buf, light, x + hi, y + lo, z + hi, u0, v1);
        v(pose, buf, light, x + hi, y + lo, z + lo, u1, v1);

        v(pose, buf, light, x + lo, y + hi, z + lo, u1, v0);
        v(pose, buf, light, x + hi, y + hi, z + lo, u0, v0);
        v(pose, buf, light, x + hi, y + lo, z + lo, u0, v1);
        v(pose, buf, light, x + lo, y + lo, z + lo, u1, v1);

        v(pose, buf, light, x + lo, y + hi, z + hi, u1, v0);
        v(pose, buf, light, x + lo, y + hi, z + lo, u0, v0);
        v(pose, buf, light, x + lo, y + lo, z + lo, u0, v1);
        v(pose, buf, light, x + lo, y + lo, z + hi, u1, v1);

        v(pose, buf, light, x + hi, y + hi, z + lo, u1, v0);
        v(pose, buf, light, x + lo, y + hi, z + lo, u0, v0);
        v(pose, buf, light, x + lo, y + hi, z + hi, u0, v1);
        v(pose, buf, light, x + hi, y + hi, z + hi, u1, v1);

        v(pose, buf, light, x + lo, y + lo, z + lo, u1, v0);
        v(pose, buf, light, x + hi, y + lo, z + lo, u0, v0);
        v(pose, buf, light, x + hi, y + lo, z + hi, u0, v1);
        v(pose, buf, light, x + lo, y + lo, z + hi, u1, v1);
    }

    private static void v(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int light,
            float x,
            float y,
            float z,
            float u,
            float vv) {
        Vertices.emit(
                buf, pose, x, y, z, ARGB.color(ALPHA, 255, 255, 255), u, vv, light, 0F, 1F, 0F);
    }
}
