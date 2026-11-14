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

import com.hbm.blockentity.machine.thermal.LeviathanTurbineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.NTMFluids;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class ChungusVisual
        extends AbstractBoundedBlockEntityVisual<LeviathanTurbineBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final int LEVER = ResourceManager.chungus.partId("Lever");
    private static final int BLADES = ResourceManager.chungus.partId("Blades");
    private static final HFRWavefrontObject MODEL = ResourceManager.chungus;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.chungus_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[LEVER], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[BLADES], MODEL.smoothing(), MATERIAL)
    };
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(0F, 0F, 4.5F)
                .rotateX(0, Mth.DEG_TO_RAD)
                .translate(0F, 0F, -4.5F)
                .end(),
        new ChainInstance.Chain()
                .translate(0F, 2.5F, 0F)
                .rotateZ(1, -Mth.DEG_TO_RAD)
                .translate(0F, -2.5F, 0F)
                .end()
    };
    private final AABB bodyBounds;
    private final ChainParts parts;

    public ChungusVisual(
            VisualizationContext context,
            LeviathanTurbineBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 180)
                                        * Mth.DEG_TO_RAD)
                        .translate(0F, 0F, -3F);
        bodyBounds = LightBounds.of(MODEL, "Body", basePose, pos);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        basePose,
                        new Model[] {PARTS[0].model(), PARTS[1].model()},
                        CHAINS,
                        2);
        updateLever();
        updateRotor();
    }

    public static void initModels() {}

    private static int steamTier(Fluid fluid) {
        if (fluid == NTMFluids.HOTSTEAM) return 1;
        if (fluid == NTMFluids.SUPERHOTSTEAM) return 2;
        if (fluid == NTMFluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateLever();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateRotor();
    }

    private void updateLever() {
        float lever = 15F - steamTier(blockEntity.tanks[0].getTankType()) * 10F;
        parts.set(0, lever, lever);
        parts.write();
    }

    private void updateRotor() {
        parts.set(1, blockEntity.lastRotor, blockEntity.rotor);
        parts.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 14,
                                pos.getY() - 2,
                                pos.getZ() - 14,
                                pos.getX() + 15,
                                pos.getY() + 8,
                                pos.getZ() + 15)
                        .inflate(1));
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
