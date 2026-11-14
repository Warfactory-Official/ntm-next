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

import com.hbm.blockentity.machine.LargeTurbineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
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

public final class LargeTurbineVisual
        extends AbstractBoundedBlockEntityVisual<LargeTurbineBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final Material BLADES_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.turbofan_blades_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final MeshPart BLADES_PART =
            MeshPart.obj(
                    ResourceManager.turbine.groups[ResourceManager.turbine.partId("Blades")],
                    false,
                    BLADES_MATERIAL);
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(0F, 1F, 0F)
                .rotateZ(0, Mth.DEG_TO_RAD)
                .translate(0F, -1F, 0F)
                .end()
    };

    private final ChainParts blades;
    private final AABB bodyBounds;

    public LargeTurbineVisual(
            VisualizationContext context, LargeTurbineBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 180)
                                        * Mth.DEG_TO_RAD)
                        .translate(0F, 0F, -1F);
        blades =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {BLADES_PART.model()}, CHAINS, 1);
        bodyBounds = LightBounds.of(ResourceManager.turbine, "Body", basePose, pos);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        blades.set(0, blockEntity.lastRotor, blockEntity.rotor);
        blades.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 8,
                                pos.getY() - 1,
                                pos.getZ() - 8,
                                pos.getX() + 9,
                                pos.getY() + 6,
                                pos.getZ() + 9)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        blades.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        blades.delete();
    }
}
