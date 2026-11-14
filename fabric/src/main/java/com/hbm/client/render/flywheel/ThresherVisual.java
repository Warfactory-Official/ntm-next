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

import com.hbm.blockentity.machine.ThresherBlockEntity;
import com.hbm.blocks.machine.ThresherBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
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

public final class ThresherVisual extends AbstractBoundedBlockEntityVisual<ThresherBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final String[] NAMES = {"Engine", "ArmUpper", "ArmLower", "Front", "Wheel"};
    private static final HFRWavefrontObject MODEL = ResourceManager.thresher;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.thresher_tex);
    private static final MeshPart[] MOVING_PARTS = buildMovingParts();
    private static final int ARM = 0, SPIN = 1, ENGINE = 2;
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().sine(ENGINE).slideY(ENGINE, .01F), arm(1), arm(2), arm(3), arm(4)
    };
    private final ChainParts parts;
    private final AABB lightBounds;
    private boolean lastOn;
    private boolean initialized;

    public ThresherVisual(
            VisualizationContext context, ThresherBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyPose =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(blockState.getValue(ThresherBlock.FACING), 0))
                                        * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(MODEL, "Base", bodyPose, pos);
        Model[] models = new Model[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) models[i] = MOVING_PARTS[i].model();
        parts = new ChainParts(context, visualPos, bodyPose, models, CHAINS, 3);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                bodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 10,
                                        pos.getY(),
                                        pos.getZ() - 10,
                                        pos.getX() + 11,
                                        pos.getY() + 7,
                                        pos.getZ() + 11)
                                .inflate(1)));
        parts.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateOn();
        updateTick();
    }

    private static MeshPart[] buildMovingParts() {
        var parts = new MeshPart[NAMES.length];
        for (int i = 0; i < parts.length; i++)
            parts[i] =
                    MeshPart.obj(MODEL.groups[MODEL.partId(NAMES[i])], MODEL.smoothing(), MATERIAL);
        return parts;
    }

    private static ChainInstance.Chain arm(int joints) {
        var chain =
                new ChainInstance.Chain()
                        .translate(0F, .5F, -1F)
                        .rotateX(ARM, Mth.DEG_TO_RAD)
                        .translate(0F, -.5F, 1F);
        if (joints > 1) {
            chain.translate(0F, .5F, -5F)
                    .rotateX(ARM, -2F * Mth.DEG_TO_RAD)
                    .translate(0F, -.5F, 5F)
                    .translate(-.01F, 0F, 0F);
        }
        if (joints > 2) {
            chain.translate(.01F, 0F, 0F)
                    .translate(0F, .5F, -9F)
                    .rotateX(ARM, Mth.DEG_TO_RAD)
                    .translate(0F, -.5F, 9F)
                    .translate(.01F, 0F, 0F);
        }
        if (joints > 3)
            chain.translate(0F, .5F, -11F).rotateX(SPIN, -Mth.DEG_TO_RAD).translate(0F, -.5F, 11F);
        return chain.end();
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        updateOn();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateTick();
    }

    private void updateOn() {
        boolean on = blockEntity.isOn;
        if (initialized && on == lastOn) return;
        lastOn = on;
        initialized = true;
        engine();
        parts.write();
    }

    private void updateTick() {
        parts.set(ARM, 82.5F - blockEntity.prevAngle, 82.5F - blockEntity.angle);
        parts.set(SPIN, blockEntity.lastSpin, blockEntity.spin);
        engine();
        parts.write();
    }

    private void engine() {
        if (lastOn) {
            float from = (float) (level.getGameTime() * 2 % (2 * Math.PI));
            parts.set(ENGINE, from, from + 1F);
        } else {
            parts.set(ENGINE, 0F, 0F);
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        parts.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        parts.delete();
    }
}
