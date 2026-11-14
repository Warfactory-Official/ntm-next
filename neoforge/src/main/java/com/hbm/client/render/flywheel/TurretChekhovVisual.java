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

import com.hbm.blockentity.turret.TurretChekhovBlockEntity;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
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

public final class TurretChekhovVisual
        extends AbstractBoundedBlockEntityVisual<TurretChekhovBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Carriage")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_carriage_tex));
    private static final MeshPart GUN_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Body")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_chekhov_tex));
    private static final MeshPart BARRELS_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Barrels")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_chekhov_barrels_tex));
    private static final int SPIN = 2;
    private static final ChainInstance.Chain[] CHAINS = {
        TurretChains.carriage(),
        TurretChains.pitched(1.5F, false, 1F).end(),
        TurretChains.pitched(1.5F, false, 1F)
                .translate(0F, 1.5F, 0F)
                .rotateX(SPIN, -Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F)
                .end()
    };

    private final AABB lightBounds;
    private final TurretConnectors connectors;
    private final ChainParts parts;

    public TurretChekhovVisual(
            VisualizationContext context, TurretChekhovBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        Matrix4f bodyLocal = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        AABB bodyBounds = LightBounds.of(ResourceManager.turret_chekhov, "Base", bodyLocal, pos);
        connectors = new TurretConnectors(context, visualPos, bodyLocal, CONNECTOR_PART.model());
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        bodyLocal,
                        new Model[] {CARRIAGE_PART.model(), GUN_PART.model(), BARRELS_PART.model()},
                        CHAINS,
                        3);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, bodyBounds.minmax(new AABB(pos).inflate(8)));
        parts.includeLightBounds(bounds, pos);
        connectors.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        connectors.update(blockEntity.connectorMask);
        updateAim();
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        connectors.update(blockEntity.connectorMask);
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

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
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
    }
}
