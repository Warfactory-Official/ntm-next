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

import com.hbm.blockentity.machine.CompactPneumaticCompressor;
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

public final class CompressorCompactVisual
        extends AbstractBoundedBlockEntityVisual<CompactPneumaticCompressor>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.compressor_compact_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final HFRWavefrontObject MODEL = ResourceManager.condenser;
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[MODEL.partId("Fan1")], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[MODEL.partId("Fan2")], MODEL.smoothing(), MATERIAL)
    };
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(0F, 1.5F, 0F)
                .rotateX(0, Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F)
                .end(),
        new ChainInstance.Chain()
                .translate(0F, 1.5F, 0F)
                .rotateX(0, -Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F)
                .end()
    };
    private final AABB bodyBounds;
    private final ChainParts fans;

    public CompressorCompactVisual(
            VisualizationContext context,
            CompactPneumaticCompressor blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90)
                                        * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Condenser", basePose, pos);
        fans =
                new ChainParts(
                        context,
                        visualPos,
                        basePose,
                        new Model[] {PARTS[0].model(), PARTS[1].model()},
                        CHAINS,
                        1);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        fans.set(0, blockEntity.prevFanSpin, blockEntity.fanSpin);
        fans.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                pos.getY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 3,
                                pos.getZ() + 4)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        fans.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        fans.delete();
    }
}
