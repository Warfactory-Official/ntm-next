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

import com.hbm.blockentity.turret.TurretTauonBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractDynamicExtentBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TurretTauonVisual
        extends AbstractDynamicExtentBlockEntityVisual<TurretTauonBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int CANNON = ResourceManager.turret_tauon.partId("Cannon");
    private static final int ROTOR = ResourceManager.turret_tauon.partId("Rotor");
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov.groups[CARRIAGE],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_carriage_tex));
    private static final MeshPart CANNON_PART =
            MeshPart.obj(
                    ResourceManager.turret_tauon.groups[CANNON],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_tauon_tex));
    private static final MeshPart ROTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_tauon.groups[ROTOR],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_tauon_tex));
    private static final int SPIN = 2;
    private static final ChainInstance.Chain[] CHAINS = {
        TurretChains.carriage(),
        TurretChains.pitched(1.5F, false, 1F).end(),
        TurretChains.pitched(1.5F, false, 1F)
                .translate(0F, 1.375F, 0F)
                .rotateX(SPIN, -Mth.DEG_TO_RAD)
                .translate(0F, -1.375F, 0F)
                .end()
    };
    private final TurretConnectors connectors;
    private final ChainParts parts;
    private final BeamVisual beam;
    private final Matrix4f placement = new Matrix4f();
    private final Matrix4f beamPose = new Matrix4f();
    private Vec3 beamVector = Vec3.ZERO;
    private final AABB lightBounds;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private long lastBeamTick = Long.MIN_VALUE;
    private double lastBeamDistance = Double.NaN;
    private boolean lastBeam;
    private boolean initialized;
    private boolean extentBeam;
    private double extentDistance = Double.NaN;

    public TurretTauonVisual(
            VisualizationContext context, TurretTauonBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var rawOffset = blockEntity.getHorizontalOffset();
        placement.translation((float) rawOffset.x, 0F, (float) rawOffset.z);
        AABB rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.turret_chekhov, "Base", placement, pos));
        connectors = new TurretConnectors(context, visualPos, placement, CONNECTOR_PART.model());
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        placement,
                        new Model[] {
                            CARRIAGE_PART.model(), CANNON_PART.model(), ROTOR_PART.model()
                        },
                        CHAINS,
                        3);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, rawBodyBounds.minmax(new AABB(pos).inflate(8)));
        parts.includeLightBounds(bounds, pos);
        connectors.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        beam = new BeamVisual(context, level, pos, true, 1, 1F);
        beforeFrame();
        updateAim();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void beforeFrame() {
        boolean active = blockEntity.beam > 0;
        double distance = blockEntity.lastDist;
        if (active == extentBeam && (!active || distance == extentDistance)) return;
        extentBeam = active;
        extentDistance = distance;
        refreshVisibleBounds();
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateMovingParts(context.partialTick());
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateAim();
    }

    private void updateAim() {
        TurretChains.aim(parts, blockEntity, Mth.HALF_PI);
        parts.set(SPIN, blockEntity.lastSpin, blockEntity.spin);
        parts.write();
    }

    public void updateMovingParts(float partialTick) {
        connectors.update(blockEntity.connectorMask);
        boolean beamActive = blockEntity.beam > 0;
        float yaw =
                beamActive
                        ? (float)
                                -Math.toDegrees(
                                        Mth.lerp(
                                                partialTick,
                                                blockEntity.lastRotationYaw,
                                                blockEntity.rotationYaw))
                        : lastYaw;
        float pitch =
                beamActive
                        ? (float)
                                Math.toDegrees(
                                        Mth.lerp(
                                                partialTick,
                                                blockEntity.lastRotationPitch,
                                                blockEntity.rotationPitch))
                        : lastPitch;
        long beamTick = level.getGameTime() / 5L;
        double beamDistance = blockEntity.lastDist;
        double previousBeamDistance = lastBeamDistance;
        boolean aimChanged =
                Float.compare(yaw, lastYaw) != 0 || Float.compare(pitch, lastPitch) != 0;
        boolean beamChanged =
                !initialized
                        || beamActive != lastBeam
                        || beamActive
                                && (aimChanged
                                        || beamTick != lastBeamTick
                                        || Double.compare(beamDistance, lastBeamDistance) != 0);
        lastYaw = yaw;
        lastPitch = pitch;
        lastBeam = beamActive;
        lastBeamTick = beamTick;
        lastBeamDistance = beamDistance;
        initialized = true;
        if (!beamChanged) return;
        if (beamActive) {
            beamPose.set(placement)
                    .rotateY((yaw - 90F) * Mth.DEG_TO_RAD)
                    .translate(0F, 1.5F, 0F)
                    .rotateZ((pitch) * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F)
                    .translate(0F, 1.5F, 0F);
            if (Double.compare(beamDistance, previousBeamDistance) != 0 || beamVector == Vec3.ZERO)
                beamVector = new Vec3(beamDistance, 0D, 0D);
            beam.update(
                    beamPose,
                    beamVector,
                    EnumWaveType.RANDOM,
                    (int) (beamTick % 360L),
                    (int) beamDistance + 1,
                    .1F,
                    0F,
                    0xFFA200,
                    0xFFD000);
        } else beam.hide();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    protected AABB visibleBounds() {
        AABB lit = getRenderBoundingBox();
        return extentBeam ? lit.minmax(new AABB(pos).inflate(extentDistance + 3D)) : lit;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        connectors.collectCrumblingInstances(consumer);
        parts.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        connectors.delete();
        parts.delete();
        beam.delete();
    }
}
