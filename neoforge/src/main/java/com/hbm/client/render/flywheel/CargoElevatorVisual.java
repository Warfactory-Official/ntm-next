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

import com.hbm.blockentity.machine.CargoElevatorBlockEntity;
import com.hbm.client.model.Meshes;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractDynamicExtentBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class CargoElevatorVisual
        extends AbstractDynamicExtentBlockEntityVisual<CargoElevatorBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final HFRWavefrontObject MODEL =
            Meshes.load(RegistryUtil.id("models/machines/elevator.obj"));
    private static final Material MATERIAL =
            MeshPart.litCutout(RegistryUtil.id("textures/block/models/machines/elevator.png"));
    private static final MeshPart PLATFORM_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Platform")], MODEL.smoothing(), MATERIAL);
    private static final MeshPart PISTON_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Piston")], MODEL.smoothing(), MATERIAL);

    private final AABB bodyBounds;
    private final TransformedInstance platform;
    private TransformedInstance[] pistons = new TransformedInstance[0];
    private final Matrix4f platformPose = new Matrix4f();
    private final Matrix4f pistonPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private @Nullable AABB lastLightBounds;
    private double lastExtension = Double.NaN;
    private int lastHeight;
    private boolean lastRenderPlatform;
    private boolean initialized;

    public CargoElevatorVisual(
            VisualizationContext context, CargoElevatorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal = new Matrix4f().translation(.5F, 0F, .5F);
        bodyBounds =
                LightBounds.of(MODEL, "Base", bodyLocal, pos)
                        .minmax(LightBounds.of(MODEL, "Guides", bodyLocal, pos));
        platform =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PLATFORM_PART.model())
                        .createInstance();
        lastHeight = blockEntity.height();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void beforeFrame() {
        if (blockEntity.height() == lastHeight) return;
        lastHeight = blockEntity.height();
        lastLightBounds =
                LightBounds.sections(lightSections, getRenderBoundingBox(), lastLightBounds);
        refreshVisibleBounds();
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double extension = blockEntity.renderExtension(partialTick);
        boolean renderPlatform = blockEntity.platformVisible();
        if (initialized && extension == lastExtension && renderPlatform == lastRenderPlatform)
            return;
        lastExtension = extension;
        lastRenderPlatform = renderPlatform;
        int count = (int) Math.ceil(extension + 1D);
        if (count > pistons.length) {
            int from = pistons.length;
            pistons = Arrays.copyOf(pistons, count);
            for (int i = from; i < count; i++)
                pistons[i] =
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, PISTON_PART.model())
                                .createInstance();
        }
        platform.setVisible(renderPlatform);
        if (renderPlatform) {
            platformPose.identity().translate(.5F, (float) extension, .5F);
            world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(platformPose);
            platform.setTransform(world).light(0).setChanged();
        }
        for (int i = 0; i < pistons.length; i++) {
            TransformedInstance piston = pistons[i];
            boolean visible = renderPlatform && i < count;
            piston.setVisible(visible);
            if (!visible) continue;
            pistonPose.identity().translate(.5F, (float) extension - i, .5F);
            world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pistonPose);
            piston.setTransform(world).light(0).setChanged();
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + blockEntity.height() + 1,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(platform);
        for (TransformedInstance piston : pistons) consumer.accept(piston);
    }

    @Override
    protected void _delete() {
        platform.delete();
        for (TransformedInstance piston : pistons) piston.delete();
    }
}
