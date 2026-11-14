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

import com.hbm.blockentity.turret.TurretMaxwellBlockEntity;
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

public final class TurretMaxwellVisual
        extends AbstractDynamicExtentBlockEntityVisual<TurretMaxwellBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final int CARRIAGE = ResourceManager.turret_howard.partId("Carriage");
    private static final int MICROWAVE = ResourceManager.turret_maxwell.partId("Microwave");
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_howard.groups[CARRIAGE],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_carriage_ciws_tex));
    private static final MeshPart MICROWAVE_PART =
            MeshPart.obj(
                    ResourceManager.turret_maxwell.groups[MICROWAVE],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_maxwell_tex));
    private static final ChainInstance.Chain[] CHAINS = {
        TurretChains.carriage(), TurretChains.pitched(1.5F, false, 1F).end()
    };
    private final TurretConnectors connectors;
    private final ChainParts parts;
    private final BeamVisual[] beams = new BeamVisual[8];
    private final Matrix4f placement = new Matrix4f();
    private final Matrix4f beamBasePose = new Matrix4f();
    private final AABB lightBounds;
    private Vec3 beamDirection = new Vec3(0D, 0D, 0D);
    private double lastBeamLength = Double.NaN;
    private int lastBeamSegments;
    private boolean lastBeamActive;
    private boolean initialized;
    private boolean extentBeam;
    private double extentDistance = Double.NaN;

    public TurretMaxwellVisual(
            VisualizationContext context, TurretMaxwellBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var offset = blockEntity.getHorizontalOffset();
        placement.translation((float) offset.x, 0F, (float) offset.z);
        AABB bodyBounds = LightBounds.of(ResourceManager.turret_chekhov, "Base", placement, pos);
        connectors = new TurretConnectors(context, visualPos, placement, CONNECTOR_PART.model());
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        placement,
                        new Model[] {CARRIAGE_PART.model(), MICROWAVE_PART.model()},
                        CHAINS,
                        2);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, bodyBounds.minmax(new AABB(pos).inflate(8)));
        parts.includeLightBounds(bounds, pos);
        connectors.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        for (int i = 0; i < beams.length; i++)
            beams[i] = new BeamVisual(context, level, pos, false, 2, .1F);
        beforeFrame();
        updateAim();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void beforeFrame() {
        boolean beam = blockEntity.beam > 0;
        double distance = blockEntity.lastDist;
        if (beam == extentBeam && (!beam || distance == extentDistance)) return;
        extentBeam = beam;
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
        parts.write();
    }

    public void updateMovingParts(float partialTick) {
        connectors.update(blockEntity.connectorMask);
        boolean beamActive = blockEntity.beam > 0;
        if (beamActive) {
            float yaw =
                    (float)
                            -Math.toDegrees(
                                    Mth.lerp(
                                            partialTick,
                                            blockEntity.lastRotationYaw,
                                            blockEntity.rotationYaw));
            float pitch =
                    (float)
                            Math.toDegrees(
                                    Mth.lerp(
                                            partialTick,
                                            blockEntity.lastRotationPitch,
                                            blockEntity.rotationPitch));
            double barrelLength = blockEntity.getBarrelLength();
            double length = blockEntity.lastDist - barrelLength;
            int segments = (int) (blockEntity.lastDist + 1D);
            if (!initialized
                    || !lastBeamActive
                    || Double.doubleToLongBits(length) != Double.doubleToLongBits(lastBeamLength)
                    || segments != lastBeamSegments) {
                beamDirection = new Vec3(length, 0D, 0D);
                lastBeamLength = length;
                lastBeamSegments = segments;
            }
            beamBasePose
                    .set(placement)
                    .rotateY((yaw - 90F) * Mth.DEG_TO_RAD)
                    .translate(0F, 1.5F, 0F)
                    .rotateZ((pitch) * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F)
                    .translate((float) barrelLength, 2F, 0F);
            for (int i = 0; i < beams.length; i++) {
                int start = (int) ((level.getGameTime() + partialTick) * -50F + i * 45F) % 360;
                beams[i].update(
                        beamBasePose,
                        beamDirection,
                        EnumWaveType.SPIRAL,
                        start,
                        segments,
                        .375F,
                        .05F,
                        0x2020FF,
                        0x2020FF);
            }
        } else if (!initialized || lastBeamActive) {
            for (BeamVisual beam : beams) beam.hide();
        }
        lastBeamActive = beamActive;
        initialized = true;
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
        for (BeamVisual beam : beams) beam.delete();
    }
}
