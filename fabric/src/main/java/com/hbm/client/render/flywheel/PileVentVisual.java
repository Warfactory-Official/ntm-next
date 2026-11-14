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

import com.hbm.blockentity.machine.pile.PileVentBlockEntity;
import com.hbm.blocks.machine.pile.PileDeviceBlock;
import com.hbm.client.model.PileDeviceModel;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class PileVentVisual extends AbstractBoundedBlockEntityVisual<PileVentBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final int FAN = ResourceManager.pile_vent.partId(PileDeviceModel.VENT_FAN);
    private static final MeshPart FAN_PART =
            MeshPart.obj(
                    ResourceManager.pile_vent.groups[FAN],
                    ResourceManager.pile_vent.smoothing(),
                    MeshPart.litCutout(ResourceManager.pile_vent_tex));
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().rotateY(0, Mth.DEG_TO_RAD)
    };
    private final ChainParts fan;
    private final AABB rawBodyBounds;

    public PileVentVisual(
            VisualizationContext context, PileVentBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(blockState.getValue(PileDeviceBlock.FACING), 90)
                                        * Mth.DEG_TO_RAD);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(LightBounds.of(ResourceManager.pile_vent, "Pipe", basePose, pos));
        fan =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {FAN_PART.model()}, CHAINS, 1);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        fan.set(0, blockEntity.lastFan, blockEntity.fan);
        fan.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        fan.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        fan.delete();
    }
}
