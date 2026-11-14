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

package com.hbm.entity.missile;

import com.hbm.blocks.ModBlocks;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.NukeTorexEntity;
import com.hbm.entity.logic.EMPEntity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.explosion.BlastDebris;
import com.hbm.explosion.BlastSmoke;
import com.hbm.explosion.CompositeExplosion;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.Erosion;
import com.hbm.explosion.vanillant.standard.MultiPointExplosionEntityProcessor;
import com.hbm.explosion.vanillant.standard.SinglePointExplosionEntityProcessor;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public sealed interface MissilePayload {

    void detonate(Missile missile);

    enum Smoke {
        SMALL,
        STANDARD,
        LARGE;

        void compose(Level level, double x, double y, double z) {
            switch (this) {
                case SMALL -> ExplosionCreator.composeEffectSmall(level, x, y, z);
                case STANDARD -> ExplosionCreator.composeEffectStandard(level, x, y, z);
                case LARGE -> ExplosionCreator.composeEffectLarge(level, x, y, z);
            }
        }
    }

    record HighExplosive(float strength, int resolution, boolean incendiary, Smoke smoke)
            implements MissilePayload {
        @Override
        public void detonate(Missile missile) {
            var level = missile.level();
            double x = missile.getX(), y = missile.getY(), z = missile.getZ();
            var cells = CellDestruction.noDrops();
            if (incendiary) cells.transform(new BlockMutatorFire());
            new ComposableExplosion(level, x, y, z, strength)
                    .blockSelector(new AttenuatedRayBlockSelector(resolution))
                    .blockProcessor(cells)
                    .entityProcessor(new MultiPointExplosionEntityProcessor(7.5D).range(2F))
                    .explode();
            smoke.compose(level, x, y, z);
        }
    }

    record Detonation(float strength, boolean breaksBlocks) implements MissilePayload {
        @Override
        public void detonate(Missile missile) {
            missile.level()
                    .explode(
                            missile,
                            missile.getX(),
                            missile.getY(),
                            missile.getZ(),
                            strength,
                            breaksBlocks
                                    ? Level.ExplosionInteraction.BLOCK
                                    : Level.ExplosionInteraction.NONE);
        }
    }

    record Submunitions(int count) implements MissilePayload {
        private static final float SPREAD = (float) (Math.PI / 4D);

        @Override
        public void detonate(Missile missile) {
            ExplosionChaos.cluster(
                    missile.level(),
                    missile.getX(),
                    missile.getY(),
                    missile.getZ(),
                    count,
                    missile.getYRot(),
                    missile.getXRot(),
                    SPREAD,
                    SPREAD,
                    1F);
        }
    }

    record Ignition(boolean everything, int radius, boolean rounded) implements MissilePayload {
        public static int centre(double coordinate, boolean rounded) {
            return rounded ? (int) ((float) coordinate + 0.5F) : (int) coordinate;
        }

        @Override
        public void detonate(Missile missile) {
            var level = (ServerLevel) missile.level();
            int x = centre(missile.getX(), rounded), y = centre(missile.getY(), rounded);
            int z = centre(missile.getZ(), rounded);
            if (everything) {
                ExplosionChaos.igniteAllBlocks(level, x, y, z, radius);
            } else {
                ExplosionChaos.igniteFlammableBlocks(level, x, y, z, radius);
            }
        }
    }

    record Bunker(int depth, float strength, int debris) implements MissilePayload {
        @Override
        public void detonate(Missile missile) {
            var level = missile.level();
            double x = missile.getX(), y = missile.getY(), z = missile.getZ();
            for (int i = 0; i < depth; i++) {
                level.explode(missile, x, y - i, z, strength, Level.ExplosionInteraction.BLOCK);
            }
            BlastSmoke.cloud(level, x, y, z, debris);
            BlastDebris.shrapnel(level, x, y, z, debris);
            BlastDebris.rubble(level, x, y, z, debris);
        }
    }

    record Pulse() implements MissilePayload {
        @Override
        public void detonate(Missile missile) {
            var level = missile.level();
            var pulse = new EMPEntity(ModEntities.EMP_LOGIC.get(), level);
            pulse.setPos(missile.getX(), missile.getY(), missile.getZ());
            level.addFreshEntity(pulse);
        }
    }

    record Nuclear(int radiusFactor, int fallout) implements MissilePayload {
        @Override
        public void detonate(Missile missile) {
            var level = missile.level();
            double x = missile.getX(), y = missile.getY(), z = missile.getZ();
            int radius = ExplosionData.MISSILE_RADIUS.get() * radiusFactor;
            var blast = NukeExplosionMK5Entity.statFac(level, radius, x, y, z);
            if (fallout > 0) blast.moreFallout(fallout);
            level.addFreshEntity(blast);
            NukeTorexEntity.statFac(level, x, y, z, radius);
        }
    }

    record Volcano() implements MissilePayload {
        @Override
        public void detonate(Missile missile) {
            var level = missile.level();
            CompositeExplosion.detonate(
                    level,
                    missile.getX(),
                    missile.getY(),
                    missile.getZ(),
                    10F,
                    CompositeExplosion.CLOUD
                            | CompositeExplosion.RUBBLE
                            | CompositeExplosion.SHRAPNEL);
            var centre = missile.blockPosition();
            var lava = ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        level.setBlock(centre.offset(dx, dy, dz), lava, Block.UPDATE_ALL);
                    }
                }
            }
            level.setBlock(
                    centre, ModBlocks.VOLCANO_CORE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    record Drill(int depth) implements MissilePayload {
        private static final float STRENGTH = 10F;

        @Override
        public void detonate(Missile missile) {
            var level = missile.level();
            double x = missile.getX(), y = missile.getY(), z = missile.getZ();
            var erosion = Erosion.concrete();
            for (int i = 0; i < depth; i++) {
                new ComposableExplosion(level, x, y - i, z, STRENGTH, missile)
                        .blockSelector(erosion.rays(16))
                        .blockProcessor(erosion.handler(0.6F))
                        .entityProcessor(new SinglePointExplosionEntityProcessor())
                        .effects(
                                (_,
                                        at,
                                        bx,
                                        by,
                                        bz,
                                        _) ->
                                        at.playSound(
                                                null,
                                                bx,
                                                by,
                                                bz,
                                                SoundEvents.GENERIC_EXPLODE,
                                                SoundSource.BLOCKS,
                                                4F,
                                                explosionPitch(at)))
                        .explode();
            }
            BlastSmoke.cloud(level, x, y, z, 25);
            BlastDebris.shrapnel(level, x, y, z, 12);
            BlastDebris.jolt(level, x, y, z, 10D, 50, 1D);
        }
    }

    record Shuttle() implements MissilePayload {
        @Override
        public void detonate(Missile missile) {
            var level = missile.level();
            double x = missile.getX(), y = missile.getY(), z = missile.getZ();
            new ComposableExplosion(level, x + 0.5D, y + 0.5D, z + 0.5D, 20F)
                    .blockSelector(new AttenuatedRayBlockSelector(64))
                    .blockProcessor(CellDestruction.decay())
                    .entityProcessor(new SinglePointExplosionEntityProcessor())
                    .explode();
            ExplosionCreator.composeEffectRBMKMush(level, x + 0.5D, y + 1D, z + 0.5D, 10F);
            WorldSounds.explosion(
                    level,
                    null,
                    x,
                    y,
                    z,
                    ModSounds.ROBIN_EXPLOSION.get(),
                    SoundSource.PLAYERS,
                    4F,
                    explosionPitch(level));
        }
    }

    record Sequence(List<MissilePayload> steps) implements MissilePayload {
        public Sequence(MissilePayload... steps) {
            this(List.of(steps));
        }

        @Override
        public void detonate(Missile missile) {
            for (var step : steps) step.detonate(missile);
        }
    }

    private static float explosionPitch(Level level) {
        var random = level.getRandom();
        return (1F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F;
    }
}
