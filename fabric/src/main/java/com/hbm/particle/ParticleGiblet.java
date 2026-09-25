// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.level.block.Blocks;

public class ParticleGiblet extends Particle implements IQuadParticle {

    public static final ParticleRenderType MEAT_GROUP =
            new ParticleRenderType("hbm:gib_meat", "HGM");
    public static final ParticleRenderType SLIME_GROUP =
            new ParticleRenderType("hbm:gib_slime", "HGS");
    public static final ParticleRenderType METAL_GROUP =
            new ParticleRenderType("hbm:gib_metal", "HGT");

    public static final int TYPE_MEAT = 0;
    public static final int TYPE_SLIME = 1;
    public static final int TYPE_METAL = 2;

    private final int gibType;
    private final float scale;

    public ParticleGiblet(
            ClientLevel level,
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            int gibType) {
        super(level, x, y, z);
        this.scale = (this.random.nextFloat() * 0.5F + 0.5F) * 2F;
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.lifetime = 140 + this.random.nextInt(20);
        this.gravity = gibType == TYPE_METAL ? 4F : 2F;
        this.gibType = gibType;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.onGround && this.gibType != TYPE_METAL) {
            Minecraft.getInstance()
                    .particleEngine
                    .add(
                            new ParticleGibletDust(
                                    this.level,
                                    this.x,
                                    this.y,
                                    this.z,
                                    (this.gibType == TYPE_SLIME
                                                    ? Blocks.MELON
                                                    : Blocks.REDSTONE_BLOCK)
                                            .defaultBlockState()));
        }
    }

    @Override
    public float size(float pt) {
        return this.scale * 0.1F;
    }

    @Override
    public int argb(float pt) {
        return 0xFFFFFFFF;
    }

    @Override
    public int light(float pt) {
        return getLightCoords(pt);
    }

    @Override
    public float u0() {
        return 1F;
    }

    @Override
    public float u1() {
        return 0F;
    }

    @Override
    public float v0() {
        return 1F;
    }

    @Override
    public float v1() {
        return 0F;
    }

    @Override
    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    @Override
    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    @Override
    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    @Override
    public ParticleRenderType getGroup() {
        return switch (this.gibType) {
            case TYPE_SLIME -> SLIME_GROUP;
            case TYPE_METAL -> METAL_GROUP;
            default -> MEAT_GROUP;
        };
    }
}
