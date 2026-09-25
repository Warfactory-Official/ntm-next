// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.main.Polaroid;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public class ExplosionNukeSmall {

    public static void explode(
            Level world, double posX, double posY, double posZ, MukeParams params) {

        if (!(world instanceof ServerLevel level)) return;

        if (params.particle) {
            boolean balefire = Polaroid.isBalefireDay() || level.getRandom().nextInt(100) == 0;
            Services.NETWORK.sendToAllAround(
                    new MukePayload(posX, posY + 0.5D, posZ, params.tinytot, balefire),
                    new TargetPoint(level, posX, posY, posZ, 250));
        }

        level.playSound(
                null,
                posX,
                posY,
                posZ,
                ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                SoundSource.BLOCKS,
                15.0F,
                1.0F);

        if (params.shrapnelCount > 0)
            ExplosionLarge.spawnShrapnels(level, posX, posY, posZ, params.shrapnelCount);

        if (params.miniNuke && !params.safe) {
            ExplosionVNT vnt = new ExplosionVNT(level, posX, posY, posZ, params.blastRadius);
            vnt.setBlockAllocator(new BlockAllocatorStandard(params.resolution));
            vnt.setBlockProcessor(
                    new BlockProcessorStandard()
                            .withBlockEffect(new BlockMutatorFire())
                            .setNoDrop());
            vnt.explode();
        }

        if (params.killRadius > 0)
            ExplosionNukeGeneric.dealDamage(level, posX, posY, posZ, params.killRadius);

        if (!params.miniNuke) {
            level.addFreshEntity(
                    EntityNukeExplosionMK5.statFac(
                            level, (int) params.blastRadius, posX, posY, posZ));
        }

        if (params.miniNuke) {
            float radMod = params.radiationLevel / 3F;

            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    if (Math.abs(i) + Math.abs(j) >= 4) continue;

                    BlockPos pos =
                            new BlockPos(
                                    (int) Math.floor(posX + i * 16),
                                    (int) Math.floor(posY),
                                    (int) Math.floor(posZ + j * 16));
                    RadiationSystemNT.incrementRad(
                            level, pos, 50D / (Math.abs(i) + Math.abs(j) + 1) * radMod);
                }
            }
        }
    }

    public static final MukeParams PARAMS_SAFE =
            new MukeParams() {
                {
                    safe = true;
                    killRadius = 45F;
                    radiationLevel = 2F;
                }
            };
    public static final MukeParams PARAMS_TOTS =
            new MukeParams() {
                {
                    blastRadius = 10F;
                    killRadius = 30F;
                    tinytot = true;
                    shrapnelCount = 0;
                    resolution = 32;
                    radiationLevel = 1F;
                }
            };
    public static final MukeParams PARAMS_LOW =
            new MukeParams() {
                {
                    blastRadius = 15F;
                    killRadius = 45F;
                    radiationLevel = 2F;
                }
            };
    public static final MukeParams PARAMS_MEDIUM =
            new MukeParams() {
                {
                    blastRadius = 20F;
                    killRadius = 55F;
                    radiationLevel = 3F;
                }
            };
    public static final MukeParams PARAMS_HIGH =
            new MukeParams() {
                {
                    miniNuke = false;
                    blastRadius = ExplosionData.FATMAN_RADIUS.get();
                    shrapnelCount = 0;
                }
            };

    public static class MukeParams {
        public boolean miniNuke = true;
        public boolean safe = false;
        public float blastRadius;
        public float killRadius;
        public float radiationLevel = 1F;

        public boolean particle = true;
        public boolean tinytot = false;
        public int shrapnelCount = 25;
        public int resolution = 64;
    }
}
