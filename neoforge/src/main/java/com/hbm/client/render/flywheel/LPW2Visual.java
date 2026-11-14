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

import com.hbm.blockentity.machine.LPW2BlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.render.LPW2Clock;
import com.hbm.client.render.LPW2Poses;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class LPW2Visual extends AbstractBoundedBlockEntityVisual<LPW2BlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final InstanceType<LPW2Instance> INSTANCE_TYPE = LPW2Instance.TYPE;
    private static Model[] models;

    private final LPW2Instance[] instances = new LPW2Instance[LPW2Poses.GROUPS.length];
    private final LPW2Clock clock = new LPW2Clock();
    private final AABB renderBounds;
    private float lastWorldTime = Float.NaN;
    private double lastMechanicalTime = Double.NaN;

    public LPW2Visual(
            VisualizationContext context, LPW2BlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        float yaw = Facing.yaw(blockState.getValue(MultiblockCoreBlock.FACING), 90);
        var localBody =
                new Matrix4f().translation(0.5f, 0, 0.5f).rotateY((float) Math.toRadians(yaw));
        var frameBounds = LightBounds.of(ResourceManager.lpw2, "Frame", localBody, pos);
        var motionBounds =
                new AABB(
                                pos.getX() - 10,
                                pos.getY(),
                                pos.getZ() - 10,
                                pos.getX() + 11,
                                pos.getY() + 7,
                                pos.getZ() + 11)
                        .inflate(1);
        renderBounds = frameBounds.minmax(motionBounds);

        var visualPosition = getVisualPosition();
        for (int slot = 0; slot < instances.length; slot++) {
            var instance =
                    instancerProvider().instancer(INSTANCE_TYPE, models[slot]).createInstance();
            instance.initialize(
                    slot, visualPosition.getX(), visualPosition.getY(), visualPosition.getZ(), yaw);
            instances[slot] = instance;
        }
        writeFrame(partialTick);
    }

    public static void initModels() {
        var body = MeshPart.litCutout(ResourceManager.lpw2_tex);
        var screen = MeshPart.litCutout(ResourceManager.lpw2_error_tex);
        var parts = MeshPart.objParts(ResourceManager.lpw2, true, body);
        models = new Model[LPW2Poses.GROUPS.length];
        for (int slot = 0; slot < models.length; slot++) {
            var part = parts[ResourceManager.lpw2.partId(LPW2Poses.GROUPS[slot])];
            if (slot == LPW2Poses.SCREEN) {
                part = part.withMaterial(screen);
            }
            models[slot] = part.model();
        }
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    protected double mechanicalTime(float partialTick, double worldTime) {
        return worldTime;
    }

    private void writeFrame(float partialTick) {
        float worldTime = LPW2Clock.getWorldTime(level, partialTick);
        double mechanicalTime = mechanicalTime(partialTick, worldTime);
        boolean mechanicalChanged = mechanicalTime != lastMechanicalTime;
        if (!mechanicalChanged && worldTime == lastWorldTime) {
            return;
        }
        clock.prepare(mechanicalTime, worldTime);
        if (mechanicalChanged) {
            for (var instance : instances) {
                instance.frame(clock);
            }
        } else {
            instances[LPW2Poses.SCREEN].frame(clock);
        }
        lastWorldTime = worldTime;
        lastMechanicalTime = mechanicalTime;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return renderBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var instance : instances) {
            consumer.accept(instance);
        }
    }

    @Override
    protected void _delete() {
        for (var instance : instances) {
            instance.delete();
        }
    }
}
