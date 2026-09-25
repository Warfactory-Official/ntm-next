// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleFoam extends Particle {

    public static final ParticleRenderType FOAM_GROUP = new ParticleRenderType("hbm:foam", "HFM");

    public static final int MAX_BUBBLES = 8;

    public static final int BUBBLE_STRIDE = 5;

    private static final int TRAIL_LENGTH = 15;
    private static final float BUOYANCY = 0.05F;
    private static final float JITTER = 0.15F;
    private static final float DRAG = 0.96F;

    private static int seedSeq;

    private final Deque<Point> trail = new ArrayDeque<>(TRAIL_LENGTH + 1);
    private final int id;
    private final float baseScale = 1.0F;
    private final float maxScale = 1.5F;
    private float scale;
    private float alpha;
    private int phase;

    public ParticleFoam(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.id = seedSeq++;
        this.lifetime = 60 + this.random.nextInt(60);
        this.gravity = 0.005F + this.random.nextFloat() * 0.015F;

        this.yd = 2.0F + this.random.nextFloat() * 3.0F;
        double angle = this.random.nextDouble() * Math.PI * 2D;
        double strength = this.random.nextDouble() * 0.5D;
        this.xd = Math.cos(angle) * strength;
        this.zd = Math.sin(angle) * strength;

        this.scale = 0.3F + this.random.nextFloat() * 0.7F;
        this.alpha = 0.8F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.trail.addFirst(new Point(this.x, this.y, this.z, bubbles(this.x, this.y, this.z)));
        while (this.trail.size() > TRAIL_LENGTH) this.trail.removeLast();

        this.age++;
        if (this.age == this.lifetime) {
            this.remove();
            return;
        }

        float t = (float) this.age / (float) this.lifetime;
        if (t < 0.3F) {
            this.phase = 0;
            if (t < 0.15F) this.yd += BUOYANCY * 6.0F;
            else this.yd += BUOYANCY * (1.0F - (t / 0.3F)) * 2.0F;
            this.scale = this.baseScale + (this.maxScale - this.baseScale) * (t / 0.3F);
        } else if (t < 0.6F) {
            this.phase = 1;
            this.yd *= 0.98F;
            this.scale = this.maxScale;
        } else {
            this.phase = 2;
            this.yd -= this.gravity;
            this.scale = this.maxScale * (1.0F - ((t - 0.6F) / 0.4F) * 0.7F);
        }

        this.alpha = 0.8F * (1.0F - t * t);

        this.xd += (this.random.nextFloat() - 0.5F) * JITTER;
        this.zd += (this.random.nextFloat() - 0.5F) * JITTER;

        this.xd *= DRAG;
        this.yd *= DRAG;
        this.zd *= DRAG;

        this.move(this.xd, this.yd, this.zd);

        if (this.onGround
                || this.level
                        .getBlockStates(this.getBoundingBox().deflate(0.001D))
                        .anyMatch(WebCollision::isWeb)) this.remove();
    }

    private float[] bubbles(double x, double y, double z) {
        Random urandom = new Random(this.id + (long) (x * 100D) + (long) (y * 10D) + (long) z);
        float[] out = new float[MAX_BUBBLES * BUBBLE_STRIDE];
        for (int i = 0; i < MAX_BUBBLES; i++) {
            out[i * BUBBLE_STRIDE + 4] = 0.9F + urandom.nextFloat() * 0.1F;
            out[i * BUBBLE_STRIDE + 3] = urandom.nextFloat() * 0.5F + 0.75F;
            out[i * BUBBLE_STRIDE] = (float) urandom.nextGaussian();
            out[i * BUBBLE_STRIDE + 1] = (float) urandom.nextGaussian();
            out[i * BUBBLE_STRIDE + 2] = (float) urandom.nextGaussian();
        }
        return out;
    }

    public Deque<Point> trail() {
        return this.trail;
    }

    public int trailLength() {
        return TRAIL_LENGTH;
    }

    public int bubbleCount() {
        return this.phase == 0 ? 8 : (this.phase == 1 ? 6 : 4);
    }

    public float spread() {
        return this.phase == 0 ? 0.4F : (this.phase == 1 ? 0.6F : 0.9F);
    }

    public float scale() {
        return this.scale;
    }

    public float alpha() {
        return this.alpha;
    }

    public double headX() {
        return this.x;
    }

    public double headY() {
        return this.y;
    }

    public double headZ() {
        return this.z;
    }

    public float[] headBubbles() {
        return bubbles(this.x, this.y, this.z);
    }

    public int light(float pt) {
        return getLightCoords(pt);
    }

    @Override
    public ParticleRenderType getGroup() {
        return FOAM_GROUP;
    }

    public record Point(double x, double y, double z, float[] bubbles) {}
}
