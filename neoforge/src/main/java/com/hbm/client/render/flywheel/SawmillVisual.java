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

import com.hbm.blockentity.machine.thermal.TimberSawBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
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

public final class SawmillVisual extends AbstractBoundedBlockEntityVisual<TimberSawBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final String[] NAMES = {"Blade", "GearLeft", "GearRight"};
    private static final HFRWavefrontObject MODEL = ResourceManager.sawmill;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.sawmill_tex);
    private static final MeshPart[] MOVING_PARTS = buildMovingParts();
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(0F, 1.375F, 0F)
                .rotateZ(0, -2F * Mth.DEG_TO_RAD)
                .translate(0F, -1.375F, 0F)
                .end(),
        new ChainInstance.Chain()
                .translate(.5625F, 1.375F, 0F)
                .rotateZ(0, Mth.DEG_TO_RAD)
                .translate(-.5625F, -1.375F, 0F)
                .end(),
        new ChainInstance.Chain()
                .translate(-.5625F, 1.375F, 0F)
                .rotateZ(0, -Mth.DEG_TO_RAD)
                .translate(.5625F, -1.375F, 0F)
                .end()
    };
    private final AABB lightBounds;
    private final ChainParts parts;
    private boolean lastHasBlade;
    private boolean initialized;

    public SawmillVisual(
            VisualizationContext context, TimberSawBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(
                                                MultiblockCoreBlock.coreFacing(
                                                        blockEntity.getBlockState()),
                                                180))
                                        * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(MODEL, "Main", bodyLocal, pos);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        bodyLocal,
                        new Model[] {
                            MOVING_PARTS[0].model(),
                            MOVING_PARTS[1].model(),
                            MOVING_PARTS[2].model()
                        },
                        CHAINS,
                        1);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds, bodyBounds.minmax(new AABB(pos).expandTowards(1, 2, 1).inflate(1)));
        parts.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateBlade();
        updateSpin();
    }

    private static MeshPart[] buildMovingParts() {
        var parts = new MeshPart[NAMES.length];
        for (int i = 0; i < parts.length; i++)
            parts[i] =
                    MeshPart.obj(MODEL.groups[MODEL.partId(NAMES[i])], MODEL.smoothing(), MATERIAL);
        return parts;
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        updateBlade();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateSpin();
    }

    private void updateBlade() {
        boolean hasBlade = blockEntity.hasBlade;
        if (initialized && hasBlade == lastHasBlade) return;
        lastHasBlade = hasBlade;
        initialized = true;
        parts.instance(0).setVisible(hasBlade);
    }

    private void updateSpin() {
        parts.set(0, blockEntity.lastSpin, blockEntity.spin);
        parts.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        parts.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        parts.delete();
    }
}
