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

import com.hbm.blockentity.SnowglobeBlockEntity;
import com.hbm.blocks.generic.SnowglobeBlock.SnowglobeType;
import com.hbm.blocks.generic.SnowglobeBlock;
import com.hbm.client.render.SnowglobeRenderer;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class SnowglobeVisual extends AbstractBoundedBlockEntityVisual<SnowglobeBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final SnowglobeType[] FEATURE_TYPES = {
        SnowglobeType.RIVETCITY,
        SnowglobeType.TENPENNYTOWER,
        SnowglobeType.LUCKY38,
        SnowglobeType.SIERRAMADRE,
        SnowglobeType.PRYDWEN
    };
    static final MeshPart SOCKET_PART =
            MeshPart.obj(
                    ResourceManager.snowglobe.groups[SnowglobeRenderer.SOCKET_PART],
                    ResourceManager.snowglobe.smoothing(),
                    body(ResourceManager.snowglobe_tex));
    static final MeshPart GLASS_PART =
            MeshPart.obj(
                    ResourceManager.snowglobe.groups[SnowglobeRenderer.GLASS_PART],
                    ResourceManager.snowglobe.smoothing(),
                    body(ResourceManager.snowglobe_glass_tex));
    static final MeshPart[] FEATURE_PARTS = buildFeatureParts();
    private final TransformedInstance socket;
    private final TransformedInstance glass;
    private final TransformedInstance[] features = new TransformedInstance[FEATURE_TYPES.length];
    private final Matrix4f instancePose = new Matrix4f();
    private final WorldText label =
            new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
    private final WorldText.Posing labelPosing;
    private SnowglobeType lastType;
    private boolean initialized;

    public SnowglobeVisual(
            VisualizationContext context, SnowglobeBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        int rotation = blockState.getValue(SnowglobeBlock.ROTATION);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .translate(.5F, 0F, .5F)
                .rotateY(-(22.5F * rotation + 90F) * Mth.DEG_TO_RAD)
                .scale(SnowglobeRenderer.SCALE);
        socket =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, SOCKET_PART.model())
                        .createInstance();
        glass =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, GLASS_PART.model())
                        .createInstance();
        socket.setTransform(instancePose).light(0).setChanged();
        glass.setTransform(instancePose).light(0).setChanged();
        for (int i = 0; i < FEATURE_TYPES.length; i++) {
            MeshPart part = FEATURE_PARTS[FEATURE_TYPES[i].ordinal()];
            features[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, part.model())
                            .createInstance();
        }
        Matrix4f labelBase = SnowglobeRenderer.labelBase(new Matrix4f(instancePose));
        int lineHeight = Minecraft.getInstance().font.lineHeight;
        labelPosing =
                (width, out) -> SnowglobeRenderer.labelWidth(out.set(labelBase), width, lineHeight);
        updateMovingParts(partialTick);
    }

    private static MeshPart[] buildFeatureParts() {
        var parts = new MeshPart[SnowglobeType.values().length];
        for (SnowglobeType type : FEATURE_TYPES) {
            parts[type.ordinal()] =
                    MeshPart.obj(
                            ResourceManager.snowglobe.groups[SnowglobeRenderer.featurePart(type)],
                            ResourceManager.snowglobe.smoothing(),
                            body(ResourceManager.snowglobe_features_tex));
        }
        return parts;
    }

    public static void initModels() {}

    private static Material body(Identifier texture) {
        return SimpleMaterial.builderOf(MeshPart.litCutout(texture)).backfaceCulling(false).build();
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        SnowglobeType type = blockEntity.type;
        if (!initialized || type != lastType) {
            lastType = type;
            initialized = true;
            for (int i = 0; i < features.length; i++) {
                boolean shown = FEATURE_TYPES[i] == type;
                features[i].setVisible(shown);
                if (shown) features[i].setTransform(instancePose).light(0).setChanged();
            }
            label.set(Component.literal(type.label), 0F, 0F, SnowglobeRenderer.LABEL_COLOR);
        }
        label.write(labelPosing);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(socket);
        consumer.accept(glass);
        for (var feature : features) consumer.accept(feature);
    }

    @Override
    protected void _delete() {
        socket.delete();
        glass.delete();
        for (var feature : features) feature.delete();
        label.delete();
    }
}
