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

import com.hbm.blockentity.machine.TurbofanBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.render.TurbofanAnimationData;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class TurbofanVisual extends AbstractBoundedBlockEntityVisual<TurbofanBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final Model[] ROTOR_MODELS = {
        model(TurbofanAnimationData.ROTOR_GROUP, TurbofanAnimationData.MAIN_TEXTURE)
    };
    private static final Model COLD_REAR_MODEL =
            model(TurbofanAnimationData.REAR_GROUP, TurbofanAnimationData.COLD_REAR_TEXTURE);
    private static final Model HOT_REAR_MODEL =
            model(TurbofanAnimationData.REAR_GROUP, TurbofanAnimationData.HOT_REAR_TEXTURE);
    private static final AABB LOCAL_RUNTIME_COVERAGE = new AABB(-3, 0, -3, 4, 3, 4).inflate(1);

    private final ChainParts rotor;
    private final TransformedInstance coldRear;
    private final TransformedInstance hotRear;
    private final Matrix4f fixedPose;
    private final AABB extent;
    private boolean hot;

    private static Model model(String group, Identifier texture) {
        var mesh = TurbofanAnimationData.MESH;
        return MeshPart.obj(
                        mesh.groups[mesh.partId(group)],
                        mesh.smoothing(),
                        MeshPart.litCutout(texture))
                .model();
    }

    public static void initModels() {}

    public TurbofanVisual(
            VisualizationContext context, TurbofanBlockEntity generator, float partialTick) {
        super(context, generator, partialTick);
        int yaw =
                Facing.yaw(
                        MultiblockCoreBlock.coreFacing(blockState),
                        TurbofanAnimationData.NORTH_REFERENCE_YAW);
        var localPose =
                new Matrix4f()
                        .translation(
                                TurbofanAnimationData.CENTER_X,
                                TurbofanAnimationData.CENTER_Y,
                                TurbofanAnimationData.CENTER_Z)
                        .rotateY(yaw * TurbofanAnimationData.DEGREES_TO_RADIANS);
        fixedPose =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(localPose);

        rotor =
                new ChainParts(
                        context,
                        visualPos,
                        localPose,
                        ROTOR_MODELS,
                        TurbofanAnimationData.ROTOR_CHAINS,
                        1);
        coldRear =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, COLD_REAR_MODEL)
                        .createInstance();
        hotRear =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, HOT_REAR_MODEL)
                        .createInstance();
        publishRotor();
        selectRear(generator.afterburner > 0);

        var bounds = new double[6];
        var casing =
                LightBounds.of(
                        TurbofanAnimationData.MESH,
                        TurbofanAnimationData.CASING_GROUP,
                        localPose,
                        pos);
        LightBounds.resetBounds(bounds, casing.minmax(LOCAL_RUNTIME_COVERAGE.move(pos)));
        rotor.includeLightBounds(bounds, pos);
        LightBounds.includeLightBounds(bounds, COLD_REAR_MODEL, localPose, pos);
        LightBounds.includeLightBounds(bounds, HOT_REAR_MODEL, localPose, pos);
        extent = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
    }

    private void publishRotor() {
        rotor.set(0, blockEntity.lastSpin, blockEntity.spin);
        rotor.write();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        publishRotor();
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        boolean nextHot = blockEntity.afterburner > 0;
        if (nextHot != hot) {
            selectRear(nextHot);
        }
    }

    private void selectRear(boolean nextHot) {
        hot = nextHot;
        coldRear.setVisible(!nextHot);
        hotRear.setVisible(nextHot);
        var selected = nextHot ? hotRear : coldRear;
        selected.setTransform(fixedPose);
        selected.light(0);
        selected.setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return extent;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> collector) {
        rotor.collectCrumblingInstances(collector);
        collector.accept(coldRear);
        collector.accept(hotRear);
    }

    @Override
    protected void _delete() {
        rotor.delete();
        coldRear.delete();
        hotRear.delete();
    }
}
