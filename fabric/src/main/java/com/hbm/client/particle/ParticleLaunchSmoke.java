// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public final class ParticleLaunchSmoke extends SingleQuadParticle implements VisualParticle {

    private static final VarHandle NEXT_PARTICLE_ID;
    private static long nextParticleId;

    static {
        try {
            NEXT_PARTICLE_ID =
                    MethodHandles.lookup()
                            .findStaticVarHandle(
                                    ParticleLaunchSmoke.class, "nextParticleId", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final long particleId = (long) NEXT_PARTICLE_ID.getAndAdd(1L) + 1L;
    private @Nullable VisualizationManager visualManager;

    private ParticleLaunchSmoke(
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
        Quaternionf rotation = new Quaternionf(camera.rotation());
        Random random = new Random(this.particleId);
        for (int i = 0; i < 6; i++) {
            float grey = random.nextFloat() * .75F + .1F;
            state.add(
                    ParticleLayers.TRANSLUCENT_SEPARATE,
                    x + (float) random.nextGaussian() * .5F * this.quadSize,
                    y + (float) random.nextGaussian() * .5F * this.quadSize,
                    z + (float) random.nextGaussian() * .5F * this.quadSize,
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

    long visualParticleId() {
        return particleId;
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
            return new ParticleLaunchSmoke(level, x, y, z, xd, yd, zd, this.sprites.get(random));
        }
    }
}
