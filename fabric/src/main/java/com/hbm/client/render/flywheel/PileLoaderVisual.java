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

import com.hbm.blockentity.machine.pile.PileLoaderBlockEntity;
import com.hbm.blocks.machine.pile.PileDeviceBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
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

public final class PileLoaderVisual extends AbstractBoundedBlockEntityVisual<PileLoaderBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final int LEVER = ResourceManager.pile_loader.partId("Lever");
    private static final int SLIDER = ResourceManager.pile_loader.partId("Slider");
    private static final int ROD = ResourceManager.pile_loader.partId("Rod");
    private static final MeshPart LEVER_PART = part(LEVER);
    private static final MeshPart SLIDER_PART = part(SLIDER);
    private static final MeshPart ROD_PART = part(ROD);
    private static final ChainInstance.Chain SLIDE = new ChainInstance.Chain().slideX(0, -.5F);
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(-.1875F, .5F, 0F)
                .rotateZ(0, Mth.HALF_PI)
                .translate(.1875F, -.5F, 0F)
                .end(),
        SLIDE,
        SLIDE
    };

    private final ChainParts parts;
    private boolean lastRod;
    private boolean initialized;

    private static MeshPart part(int id) {
        return MeshPart.obj(
                ResourceManager.pile_loader.groups[id],
                ResourceManager.pile_loader.smoothing(),
                MeshPart.litCutout(ResourceManager.pile_loader_tex));
    }

    public PileLoaderVisual(
            VisualizationContext context, PileLoaderBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(blockState.getValue(PileDeviceBlock.FACING), 90)
                                        * Mth.DEG_TO_RAD);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        basePose,
                        new Model[] {LEVER_PART.model(), SLIDER_PART.model(), ROD_PART.model()},
                        CHAINS,
                        1);
        updateRod();
        updateExtension();
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateRod();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateExtension();
    }

    private void updateRod() {
        boolean hasRod = !blockEntity.getItem(0).isEmpty();
        if (initialized && hasRod == lastRod) return;
        lastRod = hasRod;
        initialized = true;
        parts.instance(2).setVisible(hasRod);
    }

    private void updateExtension() {
        parts.set(0, (float) blockEntity.previousTravel, (float) blockEntity.displayedTravel);
        parts.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(2);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(parts.instance(0));
        consumer.accept(parts.instance(1));
        if (lastRod) consumer.accept(parts.instance(2));
    }

    @Override
    protected void _delete() {
        parts.delete();
    }
}
