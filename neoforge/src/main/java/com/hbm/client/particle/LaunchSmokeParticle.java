/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class LaunchSmokeParticle extends SingleQuadParticle implements VisualParticle {
    static final int WISPS = 6;
    private static final VarHandle NEXT_PARTICLE_ID;
    private static long nextParticleId;

    static {
        try {
            NEXT_PARTICLE_ID =
                    MethodHandles.lookup()
                            .findStaticVarHandle(
                                    LaunchSmokeParticle.class, "nextParticleId", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final long particleId = (long) NEXT_PARTICLE_ID.getAndAdd(1L) + 1L;
    private volatile float @Nullable [] wispPattern;
    private @Nullable VisualizationManager visualManager;

    private LaunchSmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.lifetime = 80 + this.random.nextInt(20);
        this.quadSize = .25F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
    }

    @Override
    public ClientLevel level() {
        return level;
    }

    @Override
    public @Nullable VisualizationManager visualManager() {
        return visualManager;
    }

    @Override
    public void visualManager(@Nullable VisualizationManager manager) {
        visualManager = manager;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext context, float partialTick) {
        return new LaunchSmokeVisual(context, this);
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        if (ownsVisual()) return;
        Vec3 cameraPosition = camera.position();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x);
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y);
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z);
        Quaternionf rotation = camera.rotation();
        float[] pattern = wispPattern();
        for (int i = 0; i < WISPS; i++) {
            int at = i * 4;
            float grey = pattern[at + 3];
            state.add(
                    ParticleLayers.TRANSLUCENT_SEPARATE,
                    x + pattern[at] * this.quadSize,
                    y + pattern[at + 1] * this.quadSize,
                    z + pattern[at + 2] * this.quadSize,
                    rotation.x,
                    rotation.y,
                    rotation.z,
                    rotation.w,
                    this.quadSize,
                    getU0(),
                    getU1(),
                    getV0(),
                    getV1(),
                    ARGB.colorFromFloat(this.alpha, grey, grey, grey),
                    LightCoordsUtil.pack(15, 0));
        }
    }

    float[] wispPattern() {
        float[] pattern = wispPattern;
        if (pattern == null) {
            pattern = createWispPattern(particleId);
            wispPattern = pattern;
        }
        return pattern;
    }

    private static float[] createWispPattern(long seed) {
        float[] pattern = new float[WISPS * 4];
        Random random = new Random(seed);
        for (int i = 0; i < WISPS; i++) {
            int at = i * 4;
            pattern[at + 3] = random.nextFloat() * .75F + .1F;
            pattern[at] = (float) random.nextGaussian() * .5F;
            pattern[at + 1] = (float) random.nextGaussian() * .5F;
            pattern[at + 2] = (float) random.nextGaussian() * .5F;
        }
        return pattern;
    }

    float visualU0() {
        return getU0();
    }

    float visualU1() {
        return getU1();
    }

    float visualV0() {
        return getV0();
    }

    float visualV1() {
        return getV1();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.alpha = 1F - (float) this.age / this.lifetime;
        float previousScale = this.quadSize;
        this.quadSize = .25F + (float) this.age / this.lifetime * 2F;
        if (++this.age == this.lifetime) this.remove();

        double speed = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        move(this.xd, this.yd + this.quadSize - previousScale, this.zd);
        if (this.onGround) this.yd = speed;
        this.xd *= .925D;
        this.yd *= .925D;
        this.zd *= .925D;
    }

    @Override
    public void move(double xd, double yd, double zd) {
        double requestedY = yd;
        Vec3 actual =
                Entity.collideBoundingBox(
                        CollisionContext.positionContext(this.y),
                        new Vec3(xd, yd, zd),
                        this.getBoundingBox(),
                        this.level,
                        List.of());
        this.setBoundingBox(this.getBoundingBox().move(actual));
        this.setPos(
                (this.getBoundingBox().minX + this.getBoundingBox().maxX) * .5D,
                this.getBoundingBox().minY,
                (this.getBoundingBox().minZ + this.getBoundingBox().maxZ) * .5D);
        this.onGround = requestedY != actual.y;
        if (xd != actual.x) this.xd = 0D;
        if (zd != actual.z) this.zd = 0D;
    }

    @Override
    protected int getLightCoords(float partialTicks) {
        return LightCoordsUtil.pack(15, 0);
    }

    @Override
    protected Layer getLayer() {
        return ParticleLayers.TRANSLUCENT_SEPARATE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
                SimpleParticleType options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                RandomSource random) {
            return new LaunchSmokeParticle(level, x, y, z, xd, yd, zd, this.sprites.get(random));
        }
    }
}
