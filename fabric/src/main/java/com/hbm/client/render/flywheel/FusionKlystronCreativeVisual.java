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

import com.hbm.blockentity.machine.fusion.CreativeFusionKlystronBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
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

public final class FusionKlystronCreativeVisual
        extends AbstractBoundedBlockEntityVisual<CreativeFusionKlystronBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final MeshPart ROTOR_PART =
            MeshPart.obj(
                    FusionKlystronVisual.MODEL.groups[FusionKlystronVisual.MODEL.partId("Rotor")],
                    FusionKlystronVisual.MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.fusion_klystron_creative_tex));

    private final ChainParts rotor;
    private final AABB lightBounds;

    public FusionKlystronCreativeVisual(
            VisualizationContext context,
            CreativeFusionKlystronBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90)
                                        * Mth.DEG_TO_RAD);
        AABB bodyBounds =
                LightBounds.of(
                        FusionKlystronVisual.MODEL,
                        "Klystron",
                        new Matrix4f(basePose).translate(-1F, 0F, 0F),
                        pos);
        rotor =
                new ChainParts(
                        context,
                        visualPos,
                        basePose,
                        new Model[] {ROTOR_PART.model()},
                        FusionKlystronVisual.ROTOR_CHAINS,
                        1);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                bodyBounds.minmax(new AABB(pos).inflate(5).expandTowards(0, 5, 0).inflate(1)));
        rotor.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        rotor.set(0, blockEntity.prevFan, blockEntity.fan);
        rotor.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        rotor.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        rotor.delete();
    }
}
