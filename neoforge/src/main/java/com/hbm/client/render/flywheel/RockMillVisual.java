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

import com.hbm.blockentity.machine.RockMillBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RockMillVisual extends AbstractBoundedBlockEntityVisual<RockMillBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final int FRAME = ResourceManager.rock_mill.partId("Frame");
    private static final int WHEEL = ResourceManager.rock_mill.partId("Wheel");
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.rock_mill_tex);
    private static final MeshPart FRAME_PART =
            MeshPart.obj(
                    ResourceManager.rock_mill.groups[FRAME],
                    ResourceManager.rock_mill.smoothing(),
                    MATERIAL);
    private static final MeshPart WHEEL_PART =
            MeshPart.obj(
                    ResourceManager.rock_mill.groups[WHEEL],
                    ResourceManager.rock_mill.smoothing(),
                    MATERIAL);
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().rotateY(0, -Mth.DEG_TO_RAD)
    };

    private final TransformedInstance frame;
    private final ChainParts wheel;
    private final AABB bounds;
    private boolean lastFrame;

    public RockMillVisual(
            VisualizationContext context, RockMillBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        float yaw =
                90F + Facing.yaw(MultiblockCoreBlock.coreFacing(blockEntity.getBlockState()), 0);
        Matrix4f basePose = new Matrix4f().translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        bounds =
                new AABB(
                        pos.getX() - 2,
                        pos.getY(),
                        pos.getZ() - 2,
                        pos.getX() + 3,
                        pos.getY() + 3,
                        pos.getZ() + 3);
        frame =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FRAME_PART.model())
                        .createInstance();
        frame.setTransform(
                        new Matrix4f()
                                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(basePose))
                .light(0)
                .setChanged();
        lastFrame = blockEntity.frame;
        frame.setVisible(lastFrame);
        wheel =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {WHEEL_PART.model()}, CHAINS, 1);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        if (blockEntity.frame != lastFrame) {
            lastFrame = blockEntity.frame;
            frame.setVisible(lastFrame);
        }
        wheel.set(0, blockEntity.prevRotation, blockEntity.rotation);
        wheel.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(frame);
        wheel.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        frame.delete();
        wheel.delete();
    }
}
