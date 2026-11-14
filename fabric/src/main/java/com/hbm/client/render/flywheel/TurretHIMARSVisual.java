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

package com.hbm.client.render.flywheel;

import com.hbm.blockentity.turret.TurretHIMARSBlockEntity;
import com.hbm.items.weapon.AmmoHIMARSItem.HIMARSRocketType;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class TurretHIMARSVisual
        extends AbstractBoundedBlockEntityVisual<TurretHIMARSBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final HFRWavefrontObject HIMARS = ResourceManager.turret_himars;
    private static final Material HIMARS_MATERIAL =
            MeshPart.litCutout(ResourceManager.turret_himars_tex);
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    HIMARS.groups[HIMARS.partId("Carriage")], HIMARS.smoothing(), HIMARS_MATERIAL);
    private static final MeshPart LAUNCHER_PART =
            MeshPart.obj(
                    HIMARS.groups[HIMARS.partId("Launcher")], HIMARS.smoothing(), HIMARS_MATERIAL);
    private static final MeshPart CRANE_PART =
            MeshPart.obj(
                    HIMARS.groups[HIMARS.partId("Crane")], HIMARS.smoothing(), HIMARS_MATERIAL);
    private static final RocketAssets[] ROCKETS = buildRockets();
    private static final int CRANE = 2;
    private static final ChainInstance.Chain CRANE_CHAIN =
            TurretChains.carriage()
                    .translate(0F, 2.25F, 2F)
                    .rotateX(TurretChains.PITCH, 1F)
                    .translate(0F, -2.25F, -2F)
                    .slideZ(CRANE, -5F);
    private static final ChainInstance.Chain[] CHAINS = {
        TurretChains.carriage(),
        TurretChains.carriage()
                .translate(0F, 2.25F, 2F)
                .rotateX(TurretChains.PITCH, 1F)
                .translate(0F, -2.25F, -2F)
                .end(),
        CRANE_CHAIN
    };
    private static final float[] CRANE_LOW = new float[3];
    private static final float[] CRANE_HIGH = {0F, 0F, 1F};
    private final AABB lightBounds;
    private final Matrix4f bodyLocal;
    private final ChainParts parts;
    private @Nullable RocketVariant rocketVariant;
    private int rocketType = -1;
    private int lastAmmo = Integer.MIN_VALUE;
    private boolean lastHasAmmo;

    public TurretHIMARSVisual(
            VisualizationContext context, TurretHIMARSBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        bodyLocal = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        AABB bodyBounds = LightBounds.of(ResourceManager.turret_arty, "Base", bodyLocal, pos);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        bodyLocal,
                        new Model[] {
                            CARRIAGE_PART.model(), LAUNCHER_PART.model(), CRANE_PART.model()
                        },
                        CHAINS,
                        3);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, bodyBounds.minmax(new AABB(pos).inflate(16)));
        parts.includeLightBounds(bounds, pos, CRANE_LOW, CRANE_HIGH);
        for (RocketAssets rocket : ROCKETS) {
            LightBounds.includeLightBounds(
                    bounds,
                    CRANE_CHAIN.sweep(
                            rocket.tube().model().boundingSphere(), CRANE_LOW, CRANE_HIGH),
                    bodyLocal,
                    pos);
            for (MeshPart cap : rocket.caps()) {
                LightBounds.includeLightBounds(
                        bounds,
                        CRANE_CHAIN.sweep(cap.model().boundingSphere(), CRANE_LOW, CRANE_HIGH),
                        bodyLocal,
                        pos);
            }
        }
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateRockets();
        updateAim();
    }

    public static void initModels() {}

    private static RocketAssets[] buildRockets() {
        var types = HIMARSRocketType.values();
        var result = new RocketAssets[types.length];
        for (var type : types) result[type.ordinal()] = buildRocket(type);
        return result;
    }

    private static RocketAssets buildRocket(HIMARSRocketType type) {
        var material = MeshPart.litCutout(type.texture);
        if (type.modelType != 0) {
            return new RocketAssets(
                    MeshPart.obj(
                            HIMARS.groups[HIMARS.partId("TubeSingle")],
                            HIMARS.smoothing(),
                            material),
                    new MeshPart[] {
                        MeshPart.obj(
                                HIMARS.groups[HIMARS.partId("CapSingle")],
                                HIMARS.smoothing(),
                                material)
                    });
        }
        var caps = new MeshPart[6];
        for (int i = 0; i < caps.length; i++)
            caps[i] =
                    MeshPart.obj(
                            HIMARS.groups[HIMARS.partId("CapStandard" + (6 - i))],
                            HIMARS.smoothing(),
                            material);
        return new RocketAssets(
                MeshPart.obj(
                        HIMARS.groups[HIMARS.partId("TubeStandard")], HIMARS.smoothing(), material),
                caps);
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateRockets();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateAim();
    }

    private void updateAim() {
        TurretChains.aim(parts, blockEntity, Mth.PI);
        writeCrane();
    }

    private void writeCrane() {
        parts.set(CRANE, blockEntity.lastCrane, blockEntity.crane);
        parts.write();
        if (rocketVariant == null) return;
        TurretChains.aim(rocketVariant.parts, blockEntity, Mth.PI);
        rocketVariant.parts.set(CRANE, blockEntity.lastCrane, blockEntity.crane);
        rocketVariant.parts.write();
    }

    private void updateRockets() {
        HIMARSRocketType type = blockEntity.loadedType();
        int typeIndex = type == null ? -1 : type.ordinal();
        int rockets = blockEntity.rocketsLeft();
        boolean hasAmmo = blockEntity.isLoaded();
        boolean typeChanged = typeIndex != rocketType;
        if (typeChanged) {
            if (rocketVariant != null) rocketVariant.delete();
            rocketVariant = type == null ? null : new RocketVariant(type);
            rocketType = typeIndex;
        }
        if (rocketVariant != null
                && (typeChanged || rockets != lastAmmo || hasAmmo != lastHasAmmo)) {
            rocketVariant.show(rockets, hasAmmo);
        }
        lastAmmo = rockets;
        lastHasAmmo = hasAmmo;
        writeCrane();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        parts.collectCrumblingInstances(consumer);
        if (rocketVariant != null) rocketVariant.parts.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        parts.delete();
        if (rocketVariant != null) rocketVariant.delete();
    }

    private record RocketAssets(MeshPart tube, MeshPart[] caps) {}

    private final class RocketVariant {
        private final boolean standard;
        private final int caps;
        private final ChainParts parts;

        private RocketVariant(HIMARSRocketType type) {
            standard = type.modelType == 0;
            RocketAssets assets = ROCKETS[type.ordinal()];
            caps = assets.caps().length;
            Model[] models = new Model[1 + assets.caps().length];
            ChainInstance.Chain[] chains = new ChainInstance.Chain[models.length];
            models[0] = assets.tube().model();
            for (int i = 0; i < assets.caps().length; i++) models[1 + i] = assets.caps()[i].model();
            for (int i = 0; i < chains.length; i++) chains[i] = CRANE_CHAIN;
            parts = new ChainParts(visualizationContext, visualPos, bodyLocal, models, chains, 3);
        }

        private void show(int ammo, boolean hasAmmo) {
            for (int i = 0; i < caps; i++)
                parts.instance(1 + i).setVisible(standard ? i < ammo : hasAmmo);
        }

        private void delete() {
            parts.delete();
        }
    }
}
