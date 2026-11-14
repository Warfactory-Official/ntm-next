/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.effect;

import com.hbm.client.VanishedEntities;
import com.hbm.config.GunVisualConfig;
import com.hbm.particle.AshesParticle;
import com.hbm.particle.CasingParticle;
import com.hbm.particle.EffectKinds.GibletType;
import com.hbm.particle.EffectKinds.VomitMode;
import com.hbm.particle.ExplosionSmallParticle;
import com.hbm.particle.GibletParticle;
import com.hbm.particle.HazeParticle;
import com.hbm.particle.PlasmaBlastParticle;
import com.hbm.particle.SkeletonBones;
import com.hbm.particle.SkeletonParticle;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingSmokeTrail;
import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class DetailEffects {

    private DetailEffects() {}

    public static void ashes(int entityId, int count, float scale) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;
        RandomSource rand = level.getRandom();

        VanishedEntities.vanish(entityId);
        float spread = entity.getBbWidth() + scale * 2;
        for (int i = 0; i < count; i++) {
            double px = entity.getX() + spread * (rand.nextDouble() - 0.5);
            double py = entity.getY() + entity.getBbHeight() * rand.nextDouble();
            double pz = entity.getZ() + spread * (rand.nextDouble() - 0.5);
            mc.particleEngine.add(new AshesParticle(level, px, py, pz, scale));
            level.addParticle(ParticleTypes.FLAME, px, py, pz, 0D, 0D, 0D);
        }
    }

    public static void skeletonize(int entityId, float brightness) {
        skeleton(entityId, brightness, 0F, false);
    }

    public static void skeletonGib(int entityId, float force) {
        skeleton(entityId, 1F, force, true);
    }

    private static void skeleton(int entityId, float brightness, float force, boolean gib) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        if (!(level.getEntity(entityId) instanceof LivingEntity living)) return;
        VanishedEntities.vanish(entityId);
        SkeletonBones.Bone[] bones = SkeletonBones.of(living);
        if (bones == null) return;
        RandomSource rand = level.getRandom();
        boolean skeletal = SkeletonBones.isSkeletal(living);

        for (SkeletonBones.Bone bone : bones) {
            if (gib && rand.nextBoolean() && !skeletal) continue;

            SkeletonParticle fx =
                    new SkeletonParticle(
                            level,
                            bone.x(),
                            bone.y(),
                            bone.z(),
                            brightness,
                            brightness,
                            brightness,
                            bone.part());
            fx.setInitialRotation(bone.yaw(), bone.pitch());
            if (gib) {
                if (skeletal) fx.makeGibKeepingTexture();
                else fx.makeGib();
                fx.setParticleSpeed(
                        rand.nextGaussian() * force,
                        (rand.nextGaussian() + 1) * force,
                        rand.nextGaussian() * force);
            }
            mc.particleEngine.add(fx);
        }
    }

    public static void vomit(int entityId, VomitMode mode, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        if (!(level.getEntity(entityId) instanceof LivingEntity entity)) return;
        RandomSource rand = level.getRandom();
        int reducedCount = count / (mc.options.particles().get().ordinal() + 1);

        double x = entity.getX(), y = entity.getEyeY(), z = entity.getZ();
        Vec3 look = entity.getLookAngle();
        for (int i = 0; i < reducedCount; i++) {
            if (mode == VomitMode.SMOKE) {
                Particle fx =
                        mc.particleEngine.createParticle(
                                ParticleTypes.SMOKE,
                                x,
                                y,
                                z,
                                (look.x + rand.nextGaussian() * 0.1) * 0.05,
                                (look.y + rand.nextGaussian() * 0.1) * 0.05,
                                (look.z + rand.nextGaussian() * 0.1) * 0.05);
                if (fx != null) {
                    fx.scale(0.2F);
                    fx.setLifetime(10 + rand.nextInt(10));
                }
                continue;
            }
            double mx = (look.x + rand.nextGaussian() * 0.2) * 0.2;
            double my = (look.y + rand.nextGaussian() * 0.2) * 0.2;
            double mz = (look.z + rand.nextGaussian() * 0.2) * 0.2;
            BlockState state =
                    mode == VomitMode.BLOOD
                            ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                            : rand.nextBoolean()
                                    ? Blocks.DYED_TERRACOTTA.lime().defaultBlockState()
                                    : Blocks.DYED_TERRACOTTA.green().defaultBlockState();
            Particle fx =
                    mc.particleEngine.createParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            x,
                            y,
                            z,
                            mx,
                            my,
                            mz);
            if (fx != null) {
                fx.setParticleSpeed(mx, my, mz);
                fx.setLifetime(150 + rand.nextInt(50));
            }
        }
    }

    public static void sweat(int entityId, int stateId, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        if (!(level.getEntity(entityId) instanceof LivingEntity entity)) return;
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(stateId);
        if (state == null) return;
        RandomSource rand = level.getRandom();
        AABB box = entity.getBoundingBox();

        for (int i = 0; i < count; i++) {
            double x = box.minX - 0.2 + (box.getXsize() + 0.4) * rand.nextDouble();
            double y = box.minY + (box.getYsize() + 0.2) * rand.nextDouble();
            double z = box.minZ - 0.2 + (box.getZsize() + 0.4) * rand.nextDouble();
            Particle fx =
                    mc.particleEngine.createParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            x,
                            y,
                            z,
                            0D,
                            0D,
                            0D);
            if (fx != null) {
                fx.setParticleSpeed(0D, 0D, 0D);
                fx.setLifetime(150 + rand.nextInt(50));
            }
        }
    }

    public static void properJolt(int time, int maxTime) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        player.animateHurt(0F);
        player.hurtTime = time;
        player.hurtDuration = maxTime;
    }

    public static void giblets(
            double x, double y, double z, int entityId, GibletType gibType, int countDivisor) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        VanishedEntities.vanish(entityId);
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;
        RandomSource rand = level.getRandom();

        int gW = (int) (entity.getBbWidth() / 0.25F);
        int gH = (int) (entity.getBbHeight() / 0.25F);
        int count = (int) (gW * 1.5 * gH);
        if (countDivisor > 0) count = (int) Math.ceil(count / (double) countDivisor);

        double mult = rand.nextInt(15) == 0 ? 10D : 1D;
        int particleType =
                switch (gibType) {
                    case MEAT -> GibletParticle.TYPE_MEAT;
                    case SLIME -> GibletParticle.TYPE_SLIME;
                    case METAL -> GibletParticle.TYPE_METAL;
                };
        for (int i = 0; i < count; i++) {
            mc.particleEngine.add(
                    new GibletParticle(
                            level,
                            x,
                            y,
                            z,
                            rand.nextGaussian() * 0.25 * mult,
                            rand.nextDouble() * mult,
                            rand.nextGaussian() * 0.25 * mult,
                            particleType));
        }
    }

    public static void casing(
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            float yaw,
            float pitch,
            float mPitch,
            float mYaw,
            String casing,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife) {
        spawnCasing(
                x, y, z, mX, mY, mZ, yaw, pitch, mPitch, mYaw, casing, smoking, smokeLife,
                smokeLift, nodeLife);
    }

    public static void gunCasing(
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            float yaw,
            float pitch,
            float mPitch,
            float mYaw,
            String casing,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife,
            boolean legacy) {
        if (GunVisualConfig.legacyAnimations != legacy) return;
        spawnCasing(
                x, y, z, mX, mY, mZ, yaw, pitch, mPitch, mYaw, casing, smoking, smokeLife,
                smokeLift, nodeLife);
    }

    private static void spawnCasing(
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            float yaw,
            float pitch,
            float mPitch,
            float mYaw,
            String casing,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        Casing declared = Casing.named(casing);
        if (declared == null) return;

        mc.particleEngine.add(
                new CasingParticle(
                        level,
                        x,
                        y,
                        z,
                        mX,
                        mY,
                        mZ,
                        yaw,
                        pitch,
                        mPitch,
                        mYaw,
                        declared,
                        smoking ? new CasingSmokeTrail(smokeLife, smokeLift, nodeLife) : null));
    }

    public static void plasmaBlast(
            double x,
            double y,
            double z,
            float r,
            float g,
            float b,
            float pitch,
            float yaw,
            float scale) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        PlasmaBlastParticle cloud = new PlasmaBlastParticle(level, x, y, z, r, g, b, pitch, yaw);
        cloud.setScale(scale);
        mc.particleEngine.add(cloud);
    }

    public static void haze(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        mc.particleEngine.add(new HazeParticle(level, x, y, z));
    }

    public static void muzzleBlast(double x, double y, double z, float size, int puffs) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        if (mc.particleEngine.createParticle(ParticleTypes.EXPLOSION, x, y, z, size, 0D, 0D)
                instanceof SingleQuadParticle flash) {
            float warm = 0.8F + rand.nextFloat() * 0.2F;
            flash.setColor(warm, 0.9F * warm, 0.5F * warm);
        }
        for (int i = 0; i < puffs; i++) {
            if (!(mc.particleEngine.createParticle(ParticleTypes.POOF, x, y, z, 0D, 0D, 0D)
                    instanceof SingleQuadParticle puff)) continue;
            float grey = 0.5F * (0.5F + rand.nextFloat() * 0.5F);
            puff.setColor(grey, grey, grey);
            puff.scale(i + 1);
        }
    }

    public static void explosionStandard(
            float x,
            float y,
            float z,
            float size,
            Object blocksStorage,
            long blocksAddress,
            int blocksCount) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        level.addParticle(
                size >= 2F ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION,
                x,
                y,
                z,
                1D,
                0D,
                0D);
        for (var index = 0; index < blocksCount; index++) {
            var packedPos = U.getLong(blocksStorage, blocksAddress + ((long) index << 3));
            double oX = BlockPos.getX(packedPos) + rand.nextFloat();
            double oY = BlockPos.getY(packedPos) + rand.nextFloat();
            double oZ = BlockPos.getZ(packedPos) + rand.nextFloat();
            double dX = oX - x, dY = oY - y, dZ = oZ - z;
            double delta = Mth.sqrt((float) (dX * dX + dY * dY + dZ * dZ));
            dX /= delta;
            dY /= delta;
            dZ /= delta;
            double mod = 0.5D / (delta / size + 0.1D);
            mod *= rand.nextFloat() * rand.nextFloat() + 0.3F;
            dX *= mod;
            dY *= mod;
            dZ *= mod;
            level.addParticle(
                    ParticleTypes.POOF, (oX + x) / 2D, (oY + y) / 2D, (oZ + z) / 2D, dX, dY, dZ);
            level.addParticle(ParticleTypes.SMOKE, oX, oY, oZ, dX, dY, dZ);
        }
    }

    public static void explosionSmall(
            double x,
            double y,
            double z,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMult,
            int debris) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        Player player = mc.player;
        if (player == null) return;
        RandomSource rand = level.getRandom();

        float dist = (float) player.getEyePosition().distanceTo(new Vec3(x, y, z));
        float soundRange = 200F;
        if (dist <= soundRange) {
            var sound =
                    dist <= soundRange * 0.4
                            ? ModSounds.WEAPON_EXPLOSION_SMALL_NEAR
                            : ModSounds.WEAPON_EXPLOSION_SMALL_FAR;
            mc.getSoundManager()
                    .playDelayed(
                            new SimpleSoundInstance(
                                    sound.get(),
                                    SoundSource.BLOCKS,
                                    100F,
                                    0.9F + rand.nextFloat() * 0.2F,
                                    RandomSource.create(),
                                    x,
                                    y,
                                    z),
                            (int) (dist / (17.15D * 0.5D)));
        }

        for (int i = 0; i < cloudCount; i++) {
            mc.particleEngine.add(
                    new ExplosionSmallParticle(level, x, y, z, cloudScale, cloudSpeedMult));
        }

        BlockState dustState = null;
        BlockPos center = BlockPos.containing(x, y, z);
        for (Direction dir : Direction.VALUES) {
            BlockState state = level.getBlockState(center.relative(dir));
            if (!state.isAir()) {
                dustState = state;
                break;
            }
        }

        if (dustState != null)
            for (int i = 0; i < debris; i++) {
                double mx = rand.nextGaussian() * 0.2;
                double my = 0.5F + rand.nextDouble() * 0.7;
                double mz = rand.nextGaussian() * 0.2;
                Particle fx =
                        mc.particleEngine.createParticle(
                                new BlockParticleOption(ParticleTypes.BLOCK, dustState),
                                x,
                                y + 0.1,
                                z,
                                mx,
                                my,
                                mz);
                if (fx != null) {
                    fx.setParticleSpeed(mx, my, mz);
                    fx.scale(2F);
                    fx.setLifetime(50 + rand.nextInt(20));
                }
            }
    }
}
