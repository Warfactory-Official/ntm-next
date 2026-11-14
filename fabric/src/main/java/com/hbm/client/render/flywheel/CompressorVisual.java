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

import com.hbm.blockentity.machine.PneumaticCompressorBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class CompressorVisual
        extends AbstractBoundedBlockEntityVisual<PneumaticCompressorBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.compressor_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final HFRWavefrontObject MODEL = ResourceManager.compressor;
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[MODEL.partId("Pump")], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[MODEL.partId("Fan")], MODEL.smoothing(), MATERIAL)
    };
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().translate(0F, -3F, 0F).slideY(0, 3F),
        new ChainInstance.Chain()
                .translate(0F, 1.5F, 0F)
                .rotateX(1, Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F)
                .end()
    };
    private static final float[] PISTON_LOW = {0F, 0F};
    private static final float[] PISTON_HIGH = {1F, 0F};
    private final ChainParts parts;
    private final AABB lightBounds;

    public CompressorVisual(
            VisualizationContext context,
            PneumaticCompressorBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90)
                                        * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(MODEL, "Compressor", basePose, pos);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        basePose,
                        new Model[] {PARTS[0].model(), PARTS[1].model()},
                        CHAINS,
                        2);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                bodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 2,
                                        pos.getY(),
                                        pos.getZ() - 2,
                                        pos.getX() + 3,
                                        pos.getY() + 9,
                                        pos.getZ() + 3)
                                .inflate(1)));
        parts.includeLightBounds(bounds, pos, PISTON_LOW, PISTON_HIGH);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        parts.set(0, blockEntity.prevPiston, blockEntity.piston);
        parts.set(1, blockEntity.prevFanSpin, blockEntity.fanSpin);
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
