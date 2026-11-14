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

import com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RBMKAutoloaderVisual
        extends AbstractBoundedBlockEntityVisual<RBMKAutoloaderBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final int PISTON = ResourceManager.rbmk_autoloader.partId("Piston");
    private static final MeshPart PISTON_PART =
            MeshPart.obj(
                    ResourceManager.rbmk_autoloader.groups[PISTON],
                    ResourceManager.rbmk_autoloader.smoothing(),
                    MeshPart.litCutout(ResourceManager.rbmk_autoloader_tex));
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().translate(.5F, 4F, .5F).slideY(0, -4F)
    };
    private static final float[] PISTON_LOW = {0F};
    private static final float[] PISTON_HIGH = {1F};
    private final ChainParts piston;
    private final AABB lightBounds;

    public RBMKAutoloaderVisual(
            VisualizationContext context,
            RBMKAutoloaderBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        AABB rawBodyBounds =
                LightBounds.of(
                        ResourceManager.rbmk_autoloader,
                        "Base",
                        new Matrix4f().translation(.5F, 0F, .5F),
                        pos);
        piston =
                new ChainParts(
                        context,
                        visualPos,
                        new Matrix4f(),
                        new Model[] {PISTON_PART.model()},
                        CHAINS,
                        1);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                rawBodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 1,
                                        pos.getY(),
                                        pos.getZ() - 1,
                                        pos.getX() + 2,
                                        pos.getY() + 9,
                                        pos.getZ() + 2)
                                .inflate(1)));
        piston.includeLightBounds(bounds, pos, PISTON_LOW, PISTON_HIGH);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        piston.set(0, (float) blockEntity.lastPiston, (float) blockEntity.renderPiston);
        piston.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        piston.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        piston.delete();
    }
}
