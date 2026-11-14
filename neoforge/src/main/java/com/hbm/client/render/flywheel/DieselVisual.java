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

import com.hbm.blockentity.machine.DieselGeneratorBlockEntity;
import com.hbm.blocks.machine.DieselBlock;
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

public final class DieselVisual extends AbstractBoundedBlockEntityVisual<DieselGeneratorBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.dieselgen;
    private static final MeshPart ENGINE_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Engine")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.dieselgen_tex));
    private static final double SHAKE_X = 25D;
    private static final double SHAKE_Z = 50D;
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .clock(0)
                .sine(0)
                .clock(1)
                .sine(1)
                .slideX(0, .005F)
                .slideZ(1, .005F)
    };
    private final ChainParts engine;
    private final AABB lightBounds;
    private long base;
    private boolean lastShaking;
    private boolean initialized;

    public DieselVisual(
            VisualizationContext context,
            DieselGeneratorBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(blockState.getValue(DieselBlock.FACING), 90)
                                        * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(MODEL, "Generator", basePose, pos);
        engine =
                new ChainParts(
                        context, visualPos, basePose, new Model[] {ENGINE_PART.model()}, CHAINS, 2);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, bodyBounds.minmax(new AABB(pos).inflate(1)));
        engine.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        updateMovingParts(context.partialTick());
    }

    @Override
    public void tick(TickableVisual.Context context) {
        if (lastShaking && GameTime.now() - base >= ChainInstance.REBASE_MILLIS) shake();
    }

    public void updateMovingParts(float partialTick) {
        boolean shaking =
                blockEntity.isOn
                        && blockEntity.hasAcceptableFuel()
                        && blockEntity.tank.getFill() > 0;
        if (initialized && shaking == lastShaking) return;
        lastShaking = shaking;
        initialized = true;
        if (shaking) {
            shake();
        } else {
            engine.set(0, 0F, 0F);
            engine.set(1, 0F, 0F);
            engine.write();
        }
    }

    private void shake() {
        base = ChainInstance.clockBase(GameTime.now(), 0);
        engine.clock(base, 0);
        engine.set(0, (float) (base / SHAKE_X % (Math.PI * 2D)), (float) (1D / SHAKE_X));
        engine.set(1, (float) (base / SHAKE_Z % (Math.PI * 2D)), (float) (1D / SHAKE_Z));
        engine.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        engine.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        engine.delete();
    }
}
