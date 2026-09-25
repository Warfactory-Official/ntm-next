// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        out.accept(ParticleSpentCasing.CASING_GROUP, engine -> new CasingParticleGroup(engine));
        out.accept(
                ParticlePlasmaBlast.PLASMA_BLAST_GROUP,
                engine -> new PlasmaBlastParticleGroup(engine));
        out.accept(
                ParticleExplosionSmall.EXPLOSION_SMALL_GROUP,
                engine -> new ExplosionSmallParticleGroup(engine));
        out.accept(ParticleMukeWave.MUKE_WAVE_GROUP, engine -> new MukeWaveParticleGroup(engine));
        out.accept(
                ParticleMukeFlash.MUKE_FLASH_GROUP, engine -> new MukeFlashParticleGroup(engine));
        out.accept(
                ParticleMukeCloud.MUKE_CLOUD_GROUP, engine -> new MukeCloudParticleGroup(engine));
        out.accept(ParticleExSmoke.EX_SMOKE_GROUP, engine -> new ExSmokeParticleGroup(engine));
        out.accept(ParticleHaze.HAZE_GROUP, engine -> new HazeParticleGroup(engine));
        out.accept(
                ParticleRocketFlame.ROCKET_FLAME_GROUP,
                engine -> new RocketFlameParticleGroup(engine));
        out.accept(ParticleContrail.CONTRAIL_GROUP, engine -> new ContrailParticleGroup(engine));
        out.accept(ParticleDebris.DEBRIS_GROUP, engine -> new DebrisParticleGroup(engine));
        out.accept(
                ParticleAmatFlash.AMAT_FLASH_GROUP, engine -> new AmatFlashParticleGroup(engine));
        out.accept(ParticleRift.RIFT_GROUP, engine -> new RiftParticleGroup(engine));
        out.accept(ParticleAshes.ASHES_GROUP, engine -> new AshesParticleGroup(engine));
        out.accept(ParticleFoam.FOAM_GROUP, engine -> new FoamParticleGroup(engine));
        out.accept(
                ParticleRBMKFlame.RBMK_FLAME_GROUP,
                engine -> new RbmkJetParticleGroup(engine, ResourceManager.rbmk_fire_tex));
        out.accept(
                ParticleRBMKSteam.RBMK_STEAM_GROUP,
                engine -> new RbmkJetParticleGroup(engine, ResourceManager.rbmk_jet_steam_tex));
        out.accept(
                ParticleSkeleton.SKELETON_GROUP,
                engine -> new SkeletonParticleGroup(engine, ResourceManager.skeleton_tex));
        out.accept(
                ParticleSkeleton.SKELETON_EXT_GROUP,
                engine -> new SkeletonParticleGroup(engine, ResourceManager.skoilet_tex));
        out.accept(
                ParticleSkeleton.GIB_GROUP,
                engine -> new SkeletonParticleGroup(engine, ResourceManager.skeleton_blood_tex));
        out.accept(
                ParticleSkeleton.GIB_EXT_GROUP,
                engine -> new SkeletonParticleGroup(engine, ResourceManager.skoilet_blood_tex));

        out.accept(
                ParticleGiblet.MEAT_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine, ParticleRenderTypes.opaque(ResourceManager.meat_tex)));
        out.accept(
                ParticleGiblet.SLIME_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine, ParticleRenderTypes.opaque(ResourceManager.slime_tex)));
        out.accept(
                ParticleGiblet.METAL_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine, ParticleRenderTypes.opaque(ResourceManager.metal_tex)));
        out.accept(
                ParticleDeadLeaf.DEAD_LEAF_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine,
                                ParticleRenderTypes.litTranslucent(ResourceManager.dead_leaf_tex)));

        out.accept(
                ParticleBlackPowderSmoke.BLACK_POWDER_SMOKE_GROUP,
                engine ->
                        new QuadParticleGroup(
                                engine,
                                ParticleRenderTypes.litTranslucent(
                                        ResourceManager.particle_base_tex)));
        out.accept(ParticleSpark.SPARK_GROUP, engine -> new LineParticleGroup(engine));
        out.accept(ParticleDebugLine.DEBUG_LINE_GROUP, engine -> new LineParticleGroup(engine));
        out.accept(ParticleText.TEXT_GROUP, engine -> new TextParticleGroup(engine));
        out.accept(ParticleLetter.LETTER_GROUP, engine -> new TextParticleGroup(engine));
    }
}
