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

package com.hbm.particle;

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.main.ResourceManager;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;

public final class HbmParticleGroups {

    private HbmParticleGroups() {}

    public static void forEach(
            BiConsumer<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> out) {
        out.accept(CasingParticle.GROUP, engine -> new CasingParticleGroup(engine));
        out.accept(
                PlasmaBlastParticle.PLASMA_BLAST_GROUP,
                engine -> new PlasmaBlastParticleGroup(engine));
        out.accept(
                ExplosionSmallParticle.EXPLOSION_SMALL_GROUP,
                engine -> new ExplosionSmallParticleGroup(engine));
        out.accept(MukeWaveParticle.MUKE_WAVE_GROUP, engine -> new MukeWaveParticleGroup(engine));
        out.accept(
                MukeFlashParticle.MUKE_FLASH_GROUP, engine -> new MukeFlashParticleGroup(engine));
        out.accept(
                MukeCloudParticle.MUKE_CLOUD_GROUP, engine -> new MukeCloudParticleGroup(engine));
        out.accept(ExSmokeParticle.EX_SMOKE_GROUP, engine -> new ExSmokeParticleGroup(engine));
        out.accept(HazeParticle.HAZE_GROUP, engine -> new HazeParticleGroup(engine));
        out.accept(
                RocketFlameParticle.ROCKET_FLAME_GROUP,
                engine -> new RocketFlameParticleGroup(engine));
        out.accept(ContrailParticle.CONTRAIL_GROUP, engine -> new ContrailParticleGroup(engine));
        out.accept(DebrisParticle.DEBRIS_GROUP, engine -> new DebrisParticleGroup(engine));
        out.accept(
                AmatFlashParticle.AMAT_FLASH_GROUP, engine -> new AmatFlashParticleGroup(engine));
        out.accept(RiftParticle.RIFT_GROUP, engine -> new RiftParticleGroup(engine));
        out.accept(AshesParticle.ASHES_GROUP, engine -> new AshesParticleGroup(engine));
        out.accept(FoamParticle.FOAM_GROUP, engine -> new FoamParticleGroup(engine));
        out.accept(
                RBMKFlameParticle.RBMK_FLAME_GROUP,
                engine -> new RbmkJetParticleGroup(engine, ResourceManager.rbmk_fire_tex));
        out.accept(
                RBMKSteamParticle.RBMK_STEAM_GROUP,
                engine -> new RbmkJetParticleGroup(engine, ResourceManager.rbmk_jet_steam_tex));
        out.accept(
                SkeletonParticle.SKELETON_GROUP,
                engine -> new SkeletonParticleGroup(engine, ResourceManager.skeleton_tex));
        out.accept(
                SkeletonParticle.SKELETON_EXT_GROUP,
                engine -> new SkeletonParticleGroup(engine, ResourceManager.skoilet_tex));
        out.accept(
                SkeletonParticle.GIB_GROUP,
                engine -> new SkeletonParticleGroup(engine, ResourceManager.skeleton_blood_tex));
        out.accept(
                SkeletonParticle.GIB_EXT_GROUP,
                engine -> new SkeletonParticleGroup(engine, ResourceManager.skoilet_blood_tex));
        out.accept(
                GibletParticle.MEAT_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine, ParticleRenderTypes.opaque(ResourceManager.meat_tex)));
        out.accept(
                GibletParticle.SLIME_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine, ParticleRenderTypes.opaque(ResourceManager.slime_tex)));
        out.accept(
                GibletParticle.METAL_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine, ParticleRenderTypes.opaque(ResourceManager.metal_tex)));
        out.accept(
                DeadLeafParticle.DEAD_LEAF_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine,
                                ParticleRenderTypes.litTranslucent(ResourceManager.dead_leaf_tex)));
        out.accept(
                BlackPowderSmokeParticle.BLACK_POWDER_SMOKE_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine,
                                ParticleRenderTypes.litTranslucent(
                                        ResourceManager.particle_base_tex)));
        out.accept(SparkParticle.SPARK_GROUP, engine -> new LineParticleGroup(engine));
        out.accept(DebugLineParticle.DEBUG_LINE_GROUP, engine -> new LineParticleGroup(engine));
        out.accept(TextParticle.TEXT_GROUP, engine -> new TextParticleGroup(engine));
        out.accept(LetterParticle.LETTER_GROUP, engine -> new TextParticleGroup(engine));
    }
}
