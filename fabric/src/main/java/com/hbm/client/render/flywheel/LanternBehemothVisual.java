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

import com.hbm.blockentity.prop.BehemothLanternBlockEntity;
import com.hbm.blocks.generic.LanternBehemothBlock;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class LanternBehemothVisual
        extends AbstractBoundedBlockEntityVisual<BehemothLanternBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final Material BODY_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.lantern_rusty_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final Material LIGHT_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .useLight(false)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .build();
    private static final MeshPart LANTERN_PART =
            MeshPart.obj(
                    ResourceManager.lantern.groups[ResourceManager.lantern.partId("Lantern")],
                    false,
                    BODY_MATERIAL);
    private static final MeshPart LIGHT_PART =
            MeshPart.obj(
                    ResourceManager.lantern.groups[ResourceManager.lantern.partId("Light")],
                    ResourceManager.lantern.smoothing(),
                    LIGHT_MATERIAL);

    private final TransformedInstance lantern;
    private final TransformedInstance light;
    private final AABB bodyBounds;
    private final boolean broken;
    private int lastLightColor;
    private boolean initialized;

    public LanternBehemothVisual(
            VisualizationContext context,
            BehemothLanternBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        lantern =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LANTERN_PART.model())
                        .createInstance();
        light =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LIGHT_PART.model())
                        .createInstance();
        broken = blockState.getValue(LanternBehemothBlock.BROKEN);
        Matrix4f bodyPose = new Matrix4f().translate(.5F, 0F, .5F);
        if (broken)
            bodyPose.rotateX((float) Math.toRadians(5F)).rotateZ((float) Math.toRadians(10F));
        bodyBounds = LightBounds.lightBounds(LANTERN_PART.model(), bodyPose, pos);
        Matrix4f instancePose =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(bodyPose);
        lantern.setVisible(broken);
        lantern.setTransform(instancePose).light(0).setChanged();
        light.setTransform(instancePose).light(LightCoordsUtil.FULL_BRIGHT);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        long millis = blockEntity.getLevel().getGameTime() * 50L + (long) (partialTick * 50F);
        float wave = (float) (Math.sin(millis / 200D) * 0.5D + 0.5D);
        int color =
                broken
                        ? ARGB.colorFromFloat(1F, wave, 0F, 0F)
                        : ARGB.colorFromFloat(1F, 0F, wave * 0.5F + 0.5F, 0F);
        if (initialized && color == lastLightColor) return;
        light.colorArgb(color);
        light.setChanged();
        lastLightColor = color;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 6,
                                pos.getZ() + 1)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(lantern);
    }

    @Override
    protected void _delete() {
        lantern.delete();
        light.delete();
    }
}
