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

import com.hbm.blockentity.machine.thermal.SteelFurnaceBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class FurnaceSteelVisual
        extends AbstractBoundedBlockEntityVisual<SteelFurnaceBlockEntity>
        implements CulledDynamicVisual {
    private static final Material SHIMMER_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .light(LightShaders.NONE)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .build();
    private static final Model SHIMMER_MODEL = new SingleMeshModel(shimmerMesh(), SHIMMER_MATERIAL);
    private final TransformedInstance shimmer;
    private final Matrix4f instancePose = new Matrix4f();
    private float lastSine = Float.NaN;
    private boolean lastOn;
    private boolean initialized;

    public FurnaceSteelVisual(
            VisualizationContext context, SteelFurnaceBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var localPose =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 180) - 90F)
                                        * Mth.DEG_TO_RAD);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        shimmer =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, SHIMMER_MODEL)
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static PackedQuadMesh shimmerMesh() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(4).normal(1F, 0F, 0F);
        for (int i = 0; i < 4; i++) {
            double x = 1D + i * .0625D;
            mesh.vertex(x, 1F, -1F, 0, 0, -1)
                    .vertex(x, 1F, 1F, 0, 0, -1)
                    .vertex(x, .5F, 1F, 0, 0, -1)
                    .vertex(x, .5F, -1F, 0, 0, -1);
        }
        return mesh.build();
    }

    private static int color(float a, float r, float g, float b) {
        return ARGB.colorFromFloat(
                Mth.clamp(a, 0F, 1F),
                Mth.clamp(r, 0F, 1F),
                Mth.clamp(g, 0F, 1F),
                Mth.clamp(b, 0F, 1F));
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        boolean on = blockEntity.wasOn;
        float sine = on ? (float) Math.sin(GameTime.now() * .001D) : 0F;
        if (initialized && on == lastOn && (!on || sine == lastSine)) return;
        lastOn = on;
        lastSine = sine;
        initialized = true;
        shimmer.setVisible(on);
        if (!on) return;
        shimmer.setTransform(instancePose)
                .light(LightCoordsUtil.pack(15, 0))
                .colorArgb(color(.5F, .875F + sine * .125F, .625F + sine * .375F, 0F))
                .setChanged();
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(pos).expandTowards(1, 3, 1).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        shimmer.delete();
    }
}
