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

import com.hbm.blockentity.machine.MicrowaveBlockEntity;
import com.hbm.blocks.machine.MicrowaveBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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
import org.joml.Matrix4f;

public final class MicrowaveVisual extends AbstractBoundedBlockEntityVisual<MicrowaveBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final float OFF_X = -0.5F, OFF_Y = -0.785F, OFF_Z = 0.65F;
    private static final MeshPart PLATE_PART =
            MeshPart.obj(
                    ResourceManager.microwave
                            .groups[ResourceManager.microwave.partId("plate_Cylinder")],
                    ResourceManager.microwave.smoothing(),
                    MeshPart.litCutout(ResourceManager.microwave_tex));
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(.575F, 0F, -.45F)
                .rotateY(0, Mth.DEG_TO_RAD)
                .translate(-.575F, 0F, .45F)
                .end()
    };
    private final AABB bodyBounds;
    private final ChainParts plate;

    public MicrowaveVisual(
            VisualizationContext context, MicrowaveBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f plateBasePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(blockState.getValue(MicrowaveBlock.FACING), 0)
                                        * Mth.DEG_TO_RAD)
                        .translate(OFF_X, OFF_Y, OFF_Z);
        var mesh = ResourceManager.microwave;
        bodyBounds =
                LightBounds.of(mesh, "mainbody_Cube.001", plateBasePose, pos)
                        .minmax(LightBounds.of(mesh, "window_Cube.002", plateBasePose, pos));
        plate =
                new ChainParts(
                        context,
                        visualPos,
                        plateBasePose,
                        new Model[] {PLATE_PART.model()},
                        CHAINS,
                        1);
        update();
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        update();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        update();
    }

    private void update() {
        if (blockEntity.progress > 0) {
            int speed = blockEntity.speed;
            float from =
                    (float) ((double) GameTime.millis(blockEntity.getLevel()) * speed / 10D % 360D);
            plate.set(0, from, from + speed * 5F);
        } else {
            plate.set(0, 0F, 0F);
        }
        plate.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        plate.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        plate.delete();
    }
}
