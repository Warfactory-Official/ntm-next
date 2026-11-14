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

import com.hbm.blockentity.machine.RadarLargeBlockEntity;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RadarLargeVisual extends AbstractBoundedBlockEntityVisual<RadarLargeBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final MeshPart DISH_PART =
            MeshPart.obj(
                    ResourceManager.radar_large.groups[ResourceManager.radar_large.partId("Dish")],
                    ResourceManager.radar_large.smoothing(),
                    SimpleMaterial.builderOf(MeshPart.litCutout(ResourceManager.radar_large_tex))
                            .backfaceCulling(false)
                            .build());
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().rotateY(0, -Mth.DEG_TO_RAD)
    };
    private final ChainParts dish;
    private final AABB lightBounds;

    public RadarLargeVisual(
            VisualizationContext context, RadarLargeBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal =
                new Matrix4f().translation(.5F, 0F, .5F).rotateY((180F) * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(ResourceManager.radar_large, "Radar", bodyLocal, pos);
        dish =
                new ChainParts(
                        context, visualPos, bodyLocal, new Model[] {DISH_PART.model()}, CHAINS, 1);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                bodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 5,
                                        pos.getY(),
                                        pos.getZ() - 5,
                                        pos.getX() + 6,
                                        pos.getY() + 10,
                                        pos.getZ() + 6)
                                .inflate(1)));
        dish.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        dish.set(0, blockEntity.prevRotation, blockEntity.rotation);
        dish.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        dish.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        dish.delete();
    }
}
