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

import com.hbm.blockentity.machine.DemonLampBlockEntity;
import com.hbm.blocks.machine.DemonLampBlock;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class DemonLampVisual extends AbstractBoundedBlockEntityVisual<DemonLampBlockEntity>
        implements ShaderLightVisual {
    private static final int RAYS = 16;
    private static final double NEAR = .375D;
    private static final double FAR = 15D;
    private static final int INNER = 0x3F00BFFF;
    private static final int OUTER = 0x0000BFFF;
    private static final Material RAY_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.TINY)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(false)
                    .build();
    private static final Model RAY_MODEL = new SingleMeshModel(rays(), RAY_MATERIAL);
    private final TransformedInstance rays;

    public DemonLampVisual(
            VisualizationContext context, DemonLampBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var localPose = new Matrix4f().translate(.5F, .5F, .5F);
        orient(localPose, blockState.getValue(DemonLampBlock.FACING));
        localPose.translate(0F, -.5F, 0F);
        var instancePose =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(localPose);
        rays = instancerProvider().instancer(InstanceTypes.TRANSFORMED, RAY_MODEL).createInstance();
        rays.setTransform(instancePose).setChanged();
    }

    public static void initModels() {}

    private static PackedQuadMesh rays() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(RAYS * 2);
        float turn = (float) (Math.PI * 2D / RAYS);
        float cos = Mth.cos(turn), sin = Mth.sin(turn);
        double x = 1D, z = 0D;
        for (int j = 0; j < 2; j++) {
            float y0 = .5F + j * .125F;
            float y1 = .5F + j * .125F + (j == 0 ? -.5F : .5F);
            for (int i = 0; i < RAYS; i++) {
                mesh.vertex(x * NEAR, y0, z * NEAR, 0, 0, INNER);
                mesh.vertex(x * FAR, y1, z * FAR, 0, 0, OUTER);
                double nx = x * cos + z * sin;
                z = z * cos - x * sin;
                x = nx;
                mesh.vertex(x * FAR, y1, z * FAR, 0, 0, OUTER);
                mesh.vertex(x * NEAR, y0, z * NEAR, 0, 0, INNER);
            }
        }
        return mesh.build();
    }

    private static void orient(Matrix4f pose, Direction facing) {
        switch (facing) {
            case DOWN -> pose.rotateX((180F) * Mth.DEG_TO_RAD);
            case UP -> {}
            case NORTH -> pose.rotateX((90F) * Mth.DEG_TO_RAD).rotateZ((180F) * Mth.DEG_TO_RAD);
            case SOUTH -> pose.rotateX((90F) * Mth.DEG_TO_RAD);
            case WEST -> pose.rotateX((90F) * Mth.DEG_TO_RAD).rotateZ((90F) * Mth.DEG_TO_RAD);
            case EAST -> pose.rotateX((90F) * Mth.DEG_TO_RAD).rotateZ((270F) * Mth.DEG_TO_RAD);
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(FAR + 1);
    }

    @Override
    protected void _delete() {
        rays.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}
}
