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

import com.hbm.blockentity.machine.FanBlockEntity;
import com.hbm.blocks.machine.FanBlock;
import com.hbm.client.model.FanModel;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
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

public final class FanVisual extends AbstractBoundedBlockEntityVisual<FanBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.fan;
    private static final MeshPart BLADES_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId(FanModel.BLADES)],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.fan_tex));
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().rotateY(0, -Mth.DEG_TO_RAD)
    };
    private final ChainParts blades;
    private final AABB lightBounds;

    public FanVisual(VisualizationContext context, FanBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose = new Matrix4f().translation(.5F, .5F, .5F);
        switch (blockState.getValue(FanBlock.FACING)) {
            case DOWN -> basePose.rotateX((180F) * Mth.DEG_TO_RAD);
            case UP -> {}
            case NORTH -> basePose.rotateX((-90F) * Mth.DEG_TO_RAD);
            case SOUTH -> basePose.rotateX((90F) * Mth.DEG_TO_RAD);
            case WEST -> basePose.rotateZ((90F) * Mth.DEG_TO_RAD);
            case EAST -> basePose.rotateZ((-90F) * Mth.DEG_TO_RAD);
        }
        basePose.translate(0F, -.5F, 0F);
        AABB rawBodyBounds = LightBounds.of(MODEL, "Frame", basePose, pos);
        blades =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {BLADES_PART.model()}, CHAINS, 1);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, rawBodyBounds.minmax(new AABB(pos).inflate(1)));
        blades.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        blades.set(0, blockEntity.previousSpin, blockEntity.spin);
        blades.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
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
