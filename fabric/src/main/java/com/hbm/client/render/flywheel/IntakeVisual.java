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

import com.hbm.blockentity.machine.IntakeBlockEntity;
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

public final class IntakeVisual extends AbstractBoundedBlockEntityVisual<IntakeBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.intake;
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.intake_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final MeshPart FAN_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Fan")], MODEL.smoothing(), MATERIAL);
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().translate(-.5F, 0F, .5F).rotateY(0, -Mth.DEG_TO_RAD)
    };
    private final ChainParts fan;
    private final AABB lightBounds;

    public IntakeVisual(
            VisualizationContext context, IntakeBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90)
                                        * Mth.DEG_TO_RAD);
        AABB rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Base", basePose, pos));
        fan =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {FAN_PART.model()}, CHAINS, 1);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                rawBodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 1,
                                        pos.getY(),
                                        pos.getZ() - 1,
                                        pos.getX() + 2,
                                        pos.getY() + 1,
                                        pos.getZ() + 2)
                                .inflate(1)));
        fan.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        fan.set(0, blockEntity.lastRotor, blockEntity.rotor);
        fan.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
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
