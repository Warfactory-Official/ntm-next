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

import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKManualControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RbmkParams;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RBMKControlRodVisual
        extends AbstractBoundedBlockEntityVisual<RBMKControlBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final int LID = ResourceManager.rbmk_rods.partId("Lid");
    private static final int SKINS = 2 + 5;
    private static final MeshPart[] LIDS = buildLids();
    private final AABB bodyBounds;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private final float columnHeight;
    private @Nullable TransformedInstance lid;
    private @Nullable AABB lastLightBounds;
    private double lastLevel = Double.NaN;
    private int lastSelected = -1;

    public RBMKControlRodVisual(
            VisualizationContext context, RBMKControlBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        int segments = RbmkParams.columnHeight();
        columnHeight = RbmkParams.heightAboveCore();
        bodyBounds = new AABB(pos).expandTowards(0, segments + .125D, 0).inflate(1);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static MeshPart[] buildLids() {
        var model = ResourceManager.rbmk_rods;
        var mesh = PackedQuadMesh.of(model.groups[LID], model.smoothing());
        var skins = new MeshPart[SKINS];
        skins[0] = MeshPart.create(mesh, MeshPart.litCutout(ResourceManager.rbmk_control_tex));
        skins[1] = MeshPart.create(mesh, MeshPart.litCutout(ResourceManager.rbmk_control_auto_tex));
        for (int i = 0; i < 5; i++)
            skins[2 + i] =
                    MeshPart.create(
                            mesh, MeshPart.litCutout(ResourceManager.rbmk_control_color_tex[i]));
        return skins;
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double level = Mth.lerp(partialTick, blockEntity.lastExtraction, blockEntity.extraction);
        int selected =
                blockEntity instanceof RBMKControlAutoBlockEntity
                        ? 1
                        : blockEntity instanceof RBMKManualControlBlockEntity manual
                                        && manual.group != null
                                ? 2 + manual.group.ordinal()
                                : 0;
        boolean levelChanged = Double.doubleToLongBits(level) != Double.doubleToLongBits(lastLevel);
        boolean selectedChanged = selected != lastSelected;
        if (!levelChanged && !selectedChanged) return;
        if (selectedChanged) {
            if (lid != null) lid.delete();
            lid =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LIDS[selected].model())
                            .createInstance();
        }
        pose.identity().translate(.5F, columnHeight + (float) level, .5F);
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        lid.setTransform(world).light(0).setChanged();
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, LIDS[selected].model(), pose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastLevel = level;
        lastSelected = selected;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1D,
                                pos.getY() + 17D,
                                pos.getZ() + 1D)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (lid != null) consumer.accept(lid);
    }

    @Override
    protected void _delete() {
        if (lid != null) lid.delete();
    }
}
