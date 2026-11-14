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

import com.hbm.blockentity.processing.ConveyorStampBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class ConveyorPressVisual
        extends AbstractBoundedBlockEntityVisual<ConveyorStampBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.conveyor_press;
    private static final MeshPart PISTON_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Piston")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.conveyor_press_tex));
    private static final MeshPart BELT_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Belt")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.conveyor_press_belt_tex));
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().slideY(0, -.75F)
    };
    private static final float[] PRESS_LOW = {0F};
    private static final float[] PRESS_HIGH = {1F};
    private static final int BELT_PERIOD = 16;
    private final AABB lightBounds;
    private final ChainParts piston;
    private final ScrollInstance belt;
    private boolean lastStamped;
    private boolean initialized;

    public ConveyorPressVisual(
            VisualizationContext context, ConveyorStampBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f base =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90)
                                        * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(MODEL, "Press", base, pos);
        piston =
                new ChainParts(
                        context, visualPos, base, new Model[] {PISTON_PART.model()}, CHAINS, 1);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                bodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 1,
                                        pos.getY(),
                                        pos.getZ() - 1,
                                        pos.getX() + 2,
                                        pos.getY() + 3,
                                        pos.getZ() + 2)
                                .inflate(1)));
        piston.includeLightBounds(bounds, pos, PRESS_LOW, PRESS_HIGH);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        belt =
                instancerProvider()
                        .instancer(ScrollInstance.TYPE, BELT_PART.model())
                        .createInstance();
        belt.clock(ChainInstance.clockBase(GameTime.now(), BELT_PERIOD), BELT_PERIOD)
                .scroll(-2F / BELT_PERIOD, 1F / BELT_PERIOD)
                .direction(0F, 1F)
                .setTransform(
                        new Matrix4f()
                                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(base))
                .light(0)
                .setChanged();
        updateStamp();
        updatePress();
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        updateStamp();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updatePress();
    }

    private void updateStamp() {
        boolean stamped = !blockEntity.syncStack.isEmpty();
        if (initialized && stamped == lastStamped) return;
        lastStamped = stamped;
        initialized = true;
        piston.instance(0).setVisible(stamped);
    }

    private void updatePress() {
        piston.set(0, (float) blockEntity.lastPress, (float) blockEntity.renderPress);
        piston.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        piston.collectCrumblingInstances(consumer);
        consumer.accept(belt);
    }

    @Override
    protected void _delete() {
        piston.delete();
        belt.delete();
    }
}
