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

import com.hbm.blockentity.machine.pile.PileControlBlockEntity;
import com.hbm.blocks.machine.pile.PileDeviceBlock;
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

public final class PileControlVisual
        extends AbstractBoundedBlockEntityVisual<PileControlBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final int ROD = ResourceManager.pile_control.partId("Rod");
    private static final MeshPart ROD_PART =
            MeshPart.obj(
                    ResourceManager.pile_control.groups[ROD],
                    ResourceManager.pile_control.smoothing(),
                    MeshPart.litCutout(ResourceManager.pile_control_tex));
    private static final ChainInstance.Chain[] CHAINS = {new ChainInstance.Chain().slideY(0, .75F)};

    private final ChainParts rod;

    public PileControlVisual(
            VisualizationContext context, PileControlBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(blockState.getValue(PileDeviceBlock.FACING), 90)
                                        * Mth.DEG_TO_RAD);
        rod =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {ROD_PART.model()}, CHAINS, 1);
        update();
    }

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        rod.set(0, (float) blockEntity.previousTravel, (float) blockEntity.displayedTravel);
        rod.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(2);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        rod.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        rod.delete();
    }
}
