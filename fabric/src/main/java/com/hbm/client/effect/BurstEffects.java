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

import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.client.particle.HadronParticle;
import com.hbm.items.ModItems;
import com.hbm.particle.AmatFlashParticle;
import com.hbm.particle.BlackPowderSmokeParticle;
import com.hbm.particle.DebugLineParticle;
import com.hbm.particle.ExSmokeParticle;
import com.hbm.particle.FoamParticle;
import com.hbm.particle.HbmParticles;
import com.hbm.particle.LetterParticle;
import com.hbm.particle.MukeCloudParticle;
import com.hbm.particle.MukeFlashParticle;
import com.hbm.particle.MukeWaveParticle;
import com.hbm.particle.MushroomCloudEmitter;
import com.hbm.particle.RBMKFlameParticle;
import com.hbm.particle.RBMKSteamParticle;
import com.hbm.particle.SparkParticle;
import com.hbm.particle.TextParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BurstEffects {

    private BurstEffects() {}

    public static void amatFlash(double x, double y, double z, float scale) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        mc.particleEngine.add(new AmatFlashParticle(level, x, y, z, scale));
    }

    public static void blockDustBurst(
            int stateId, double x, double y, double z, int count, double motion) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(stateId);
        if (state == null) return;
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            ClientEffects.spawnBlockDust(
                    level,
                    x,
                    y,
                    z,
                    random.nextGaussian() * motion,
                    random.nextGaussian() * motion,
                    random.nextGaussian() * motion,
                    state);
        }
    }

    public static void blockDestroy(int x, int y, int z, int stateId) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(stateId);
        if (state == null) return;
        level.addDestroyBlockEffect(new BlockPos(x, y, z), state);
    }

    public static void rbmkFlame(double x, double y, double z, int maxAge) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        mc.particleEngine.add(new RBMKFlameParticle(level, x, y, z, maxAge));
    }

    public static void rbmkSteam(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        mc.particleEngine.add(new RBMKSteamParticle(level, x, y, z));
    }

    public static void blackPowder(
            double x,
            double y,
            double z,
            double hx,
            double hy,
            double hz,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMult,
            int sparkCount,
            float sparkSpeedMult) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource random = level.getRandom();
        Vec3 heading = new Vec3(hx, hy, hz).normalize();

        for (int i = 0; i < cloudCount; i++) {
            BlackPowderSmokeParticle smoke =
                    new BlackPowderSmokeParticle(level, x, y, z, cloudScale);
            double speedMult = 0.85 + random.nextDouble() * 0.3;
            smoke.setParticleSpeed(
                    heading.x * cloudSpeedMult * speedMult + random.nextGaussian() * 0.05,
                    heading.y * cloudSpeedMult * speedMult + random.nextGaussian() * 0.05,
                    heading.z * cloudSpeedMult * speedMult + random.nextGaussian() * 0.05);
            mc.particleEngine.add(smoke);
        }

        for (int i = 0; i < sparkCount; i++) {
            double speedMult = 0.85 + random.nextDouble() * 0.3;
            level.addParticle(
                    HbmParticles.BLACK_POWDER_SPARK.get(),
                    true,
                    false,
                    x,
                    y,
                    z,
                    heading.x * sparkSpeedMult * speedMult + random.nextGaussian() * 0.02,
                    heading.y * sparkSpeedMult * speedMult + random.nextGaussian() * 0.02,
                    heading.z * sparkSpeedMult * speedMult + random.nextGaussian() * 0.02);
        }
    }

    public static void sparkBurst(double x, double y, double z, int count, boolean small) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            mc.particleEngine.add(
                    new SparkParticle(
                                    level,
                                    x,
                                    y,
                                    z,
                                    random.nextGaussian() * 0.05,
                                    0.05,
                                    random.nextGaussian() * 0.05)
                            .makeSmall(small));
        }
        if (mc.particleEngine.createParticle(HbmParticles.HADRON.get(), x, y, z, 0D, 0D, 0D)
                instanceof HadronParticle hadron) hadron.makeSmall(small);
    }

    public static void debugText(
            double x, double y, double z, int color, float scale, String text) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        mc.particleEngine.add(new TextParticle(level, x, y, z, color, text).setScale(scale));
    }

    public static void debugLetter(double x, double y, double z, int color, char glyph) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        for (int i = 0; i < 50; i++) {
            Particle spark =
                    mc.particleEngine.createParticle(
                            ParticleTypes.FIREWORK,
                            x,
                            y,
                            z,
                            0.4 * level.getRandom().nextGaussian(),
                            0.4 * level.getRandom().nextGaussian(),
                            0.4 * level.getRandom().nextGaussian());
            if (spark instanceof SimpleAnimatedParticle animated) animated.setColor(color);
        }
        mc.particleEngine.add(new LetterParticle(level, x, y, z, color, glyph));
    }

    public static void droneLine(
            double x, double y, double z, double mx, double my, double mz, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level) || !showsDroneNetwork(mc)) return;
        mc.particleEngine.add(new DebugLineParticle(level, x, y, z, mx, my, mz, color));
    }

    public static void smokeCloud(double x, double y, double z, int packetCount) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource random = level.getRandom();
        int count = Math.max(1, packetCount);
        for (int i = 0; i < count; i++) {
            ExSmokeParticle smoke = new ExSmokeParticle(level, x, y, z);
            double motionY = random.nextGaussian() * (1 + (count / 100));
            double motionX = random.nextGaussian() * (1 + (count / 150));
            if (random.nextBoolean()) motionY = Math.abs(motionY);
            smoke.setParticleSpeed(motionX, motionY, random.nextGaussian() * (1 + (count / 150)));
            mc.particleEngine.add(smoke);
        }
    }

    public static void smokeRadial(double x, double y, double z, int packetCount) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource random = level.getRandom();
        int count = Math.max(1, packetCount);
        double spread = 1 + count / 50;
        for (int i = 0; i < count; i++) {
            ExSmokeParticle smoke = new ExSmokeParticle(level, x, y, z);
            double motionY = random.nextGaussian() * spread;
            double motionX = random.nextGaussian() * spread;
            smoke.setParticleSpeed(motionX, motionY, random.nextGaussian() * spread);
            mc.particleEngine.add(smoke);
        }
    }

    public static void smokeFoam(double x, double y, double z, int packetCount) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        int count = Math.max(1, packetCount);
        for (int i = 0; i < count; i++) {
            FoamParticle foam = new FoamParticle(level, x, y, z);
            foam.setLifetime(50);
            foam.setParticleSpeed(0, 0, 0);
            mc.particleEngine.add(foam);
        }
    }

    public static void smokeShock(double x, double y, double z, int packetCount, double strength) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        int count = Math.max(1, packetCount);
        ClientEffects.spawnSmokeShock(level, x, y, z, count, strength);
    }

    public static void smokeBurst(double x, double y, double z, int packetCount, double strength) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource random = level.getRandom();
        int count = Math.max(1, packetCount);
        float angle = random.nextInt(360) * Mth.DEG_TO_RAD;
        float step = Mth.TWO_PI / count;
        for (int i = 0; i < count; i++) {
            ClientEffects.spawnGasFlame(
                    level,
                    x,
                    y,
                    z,
                    Math.cos(angle) * strength,
                    0D,
                    Math.sin(angle) * strength,
                    6.5F);
            angle += step;
        }
    }

    public static void mukeFull(double x, double y, double z, boolean balefire) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        Player player = mc.player;
        if (player == null) return;
        mc.particleEngine.add(new MukeWaveParticle(level, x, y, z));
        mc.particleEngine.add(new MukeFlashParticle(level, x, y, z, balefire));
        player.animateHurt(0F);
        player.hurtTime = 15;
        player.hurtDuration = 15;
    }

    public static void mukeTiny(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        Player player = mc.player;
        if (player == null) return;
        mc.particleEngine.add(new MukeWaveParticle(level, x, y, z));
        MushroomCloudEmitter.TINY.emit(
                mc.particleEngine, level, level.getRandom(), x, y, z, MukeCloudParticle::new);
        player.animateHurt(0F);
        player.hurtTime = 15;
        player.hurtDuration = 15;
    }

    private static boolean showsDroneNetwork(Minecraft mc) {
        if (mc.player == null) return false;
        ItemStack held = mc.player.getMainHandItem();
        return ModItems.DRONE.typeOf(held) != null
                || held.is(ModItems.DRONE_LINKER.get())
                || held.is(ModBlocks.DRONE_CRATE_PROVIDER.get().asItem())
                || held.is(ModBlocks.DRONE_CRATE_REQUESTER.get().asItem())
                || held.is(ModBlocks.DRONE_DOCK.get().asItem())
                || held.is(ModBlocks.DRONE_WAYPOINT.get().asItem())
                || held.is(ModBlocks.DRONE_WAYPOINT_REQUEST.get().asItem());
    }
}
