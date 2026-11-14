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

import com.hbm.blockentity.machine.storage.FensuBlockEntity;
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
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class FENSUVisual extends AbstractBoundedBlockEntityVisual<FensuBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.fensu;
    private static final MeshPart DISC_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Disc")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.fensu_tex));
    private static final Material LIGHT_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.fensu_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart LIGHT_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Lights")], MODEL.smoothing(), LIGHT_MATERIAL);
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(0F, 2.5F, 0F)
                .rotateX(0, Mth.DEG_TO_RAD)
                .translate(0F, -2.5F, 0F)
                .end()
    };
    private final AABB lightBounds;
    private final ChainParts disc;
    private final ChainParts lights;

    public FENSUVisual(
            VisualizationContext context, FensuBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90)
                                        * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(MODEL, "Base", basePose, pos);
        disc =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {DISC_PART.model()}, CHAINS, 1);
        lights =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {LIGHT_PART.model()}, CHAINS, 1);
        lights.instance(0).overlay(OverlayTexture.NO_OVERLAY).light(LightCoordsUtil.FULL_BRIGHT);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds, bodyBounds.minmax(new AABB(pos).expandTowards(0, 3, 0).inflate(1)));
        disc.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        disc.set(0, blockEntity.prevRotation, blockEntity.rotation);
        lights.set(0, blockEntity.prevRotation, blockEntity.rotation);
        disc.write();
        lights.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        disc.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        disc.delete();
        lights.delete();
    }
}
