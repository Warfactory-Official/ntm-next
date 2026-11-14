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

import com.hbm.blockentity.turret.TurretRichardBlockEntity;
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

public final class TurretRichardVisual
        extends AbstractBoundedBlockEntityVisual<TurretRichardBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final int MISSILES = 17;
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Carriage")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_carriage_tex));
    private static final MeshPart LAUNCHER_PART =
            MeshPart.obj(
                    ResourceManager.turret_richard
                            .groups[ResourceManager.turret_richard.partId("Launcher")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_richard_tex));
    private static final MeshPart MISSILE_PART =
            MeshPart.obj(
                    ResourceManager.turret_richard
                            .groups[ResourceManager.turret_richard.partId("MissileLoaded")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_richard_tex));
    private static final ChainInstance.Chain[] CHAINS = chains();

    private final AABB lightBounds;
    private final TurretConnectors connectors;
    private final ChainParts parts;
    private int lastLoaded = Integer.MIN_VALUE;

    public TurretRichardVisual(
            VisualizationContext context, TurretRichardBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        Matrix4f placement = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        AABB baseBounds = LightBounds.of(ResourceManager.turret_chekhov, "Base", placement, pos);
        connectors = new TurretConnectors(context, visualPos, placement, CONNECTOR_PART.model());
        Model[] models = new Model[2 + MISSILES];
        models[0] = CARRIAGE_PART.model();
        models[1] = LAUNCHER_PART.model();
        for (int i = 0; i < MISSILES; i++) models[2 + i] = MISSILE_PART.model();
        parts = new ChainParts(context, visualPos, placement, models, CHAINS, 2);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, baseBounds.minmax(new AABB(pos).inflate(8)));
        parts.includeLightBounds(bounds, pos);
        connectors.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateState();
        updateAim();
    }

    public static void initModels() {}

    private static ChainInstance.Chain[] chains() {
        var chains = new ChainInstance.Chain[2 + MISSILES];
        chains[0] = TurretChains.carriage();
        chains[1] = TurretChains.pitched(1.5F, false, 1F).end();
        float y = .375F, z = .1875F;
        for (int i = 0; i < MISSILES; i++) {
            if (i > 0) {
                int last = i - 1;
                if (last == 2 || last == 6 || last == 9 || last == 13) {
                    y += -.1875F;
                    z += .1875F * 2F + .09375F;
                } else {
                    z += -.1875F;
                }
            }
            chains[2 + i] = TurretChains.pitched(1.5F, false, 1F).translate(0F, y, z).end();
        }
        return chains;
    }

    private void updateState() {
        connectors.update(blockEntity.connectorMask);
        int loaded = Mth.clamp(blockEntity.magazine(), 0, MISSILES);
        if (loaded == lastLoaded) return;
        lastLoaded = loaded;
        for (int i = 0; i < MISSILES; i++) parts.instance(2 + i).setVisible(i < loaded);
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateState();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateAim();
    }

    private void updateAim() {
        TurretChains.aim(parts, blockEntity, Mth.HALF_PI);
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
