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

import com.hbm.blockentity.machine.AshpitBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class AshpitVisual extends AbstractBoundedBlockEntityVisual<AshpitBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final int DOOR = ResourceManager.heater_oven.partId("Door");
    private static final int INNER_BURNING = ResourceManager.heater_oven.partId("InnerBurning");
    private static final int INNER = ResourceManager.heater_oven.partId("Inner");
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.ashpit_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(
                ResourceManager.heater_oven.groups[DOOR],
                ResourceManager.heater_oven.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.heater_oven.groups[INNER_BURNING],
                ResourceManager.heater_oven.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.heater_oven.groups[INNER],
                ResourceManager.heater_oven.smoothing(),
                MATERIAL)
    };
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().slideZ(0, .75F / 135F)
    };
    private final AABB bodyBounds;
    private final ChainParts door;
    private final TransformedInstance full;
    private final TransformedInstance empty;
    private boolean lastFull;
    private boolean initialized;

    public AshpitVisual(
            VisualizationContext context, AshpitBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        float facing = Facing.yaw(blockState.getValue(MultiblockCoreBlock.FACING), 180);
        Matrix4f bodyPose =
                new Matrix4f().translation(.5F, 0F, .5F).rotateY((facing - 90F) * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.heater_oven, "Main", bodyPose, pos);
        door =
                new ChainParts(
                        context, visualPos, bodyPose, new Model[] {PARTS[0].model()}, CHAINS, 1);
        Matrix4f world =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(bodyPose);
        full =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PARTS[1].model())
                        .createInstance();
        empty =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PARTS[2].model())
                        .createInstance();
        full.setTransform(world).light(0).setChanged();
        empty.setTransform(world).light(0).setChanged();
        updateFill();
        updateDoor();
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        updateFill();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateDoor();
    }

    private void updateFill() {
        boolean isFull = blockEntity.isFull;
        if (initialized && isFull == lastFull) return;
        lastFull = isFull;
        initialized = true;
        full.setVisible(isFull);
        empty.setVisible(!isFull);
    }

    private void updateDoor() {
        door.set(0, blockEntity.prevDoorAngle, blockEntity.doorAngle);
        door.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 1,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        door.collectCrumblingInstances(consumer);
        consumer.accept(full);
        consumer.accept(empty);
    }

    @Override
    protected void _delete() {
        door.delete();
        full.delete();
        empty.delete();
    }
}
