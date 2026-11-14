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

import com.hbm.blockentity.machine.AutosawBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
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

public final class AutosawVisual extends AbstractBoundedBlockEntityVisual<AutosawBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final int MAIN = ResourceManager.autosaw.partId("Main");
    private static final int ENGINE = ResourceManager.autosaw.partId("Engine");
    private static final int ARM_UPPER = ResourceManager.autosaw.partId("ArmUpper");
    private static final int ARM_LOWER = ResourceManager.autosaw.partId("ArmLower");
    private static final int ARM_TIP = ResourceManager.autosaw.partId("ArmTip");
    private static final int SAWBLADE = ResourceManager.autosaw.partId("Sawblade");
    private static final HFRWavefrontObject MODEL = ResourceManager.autosaw;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.autosaw_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[MAIN], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[ENGINE], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[ARM_UPPER], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[ARM_LOWER], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[ARM_TIP], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[SAWBLADE], MODEL.smoothing(), MATERIAL)
    };
    private static final int TURN = 0, ANGLE = 1, SPIN = 2, SHAKE = 3;
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().rotateY(TURN, -Mth.DEG_TO_RAD),
        new ChainInstance.Chain().sine(SHAKE).rotateY(TURN, -Mth.DEG_TO_RAD).slideY(SHAKE, .01F),
        arm(1),
        arm(2),
        arm(3),
        arm(4)
    };
    private final AABB lightBounds;
    private final ChainParts parts;
    private boolean lastOn;
    private boolean initialized;

    public AutosawVisual(
            VisualizationContext context, AutosawBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose = new Matrix4f().translation(.5F, 0F, .5F);
        AABB bodyBounds = LightBounds.of(MODEL, "Base", basePose, pos);
        Model[] models = new Model[PARTS.length];
        for (int i = 0; i < PARTS.length; i++) models[i] = PARTS[i].model();
        parts = new ChainParts(context, visualPos, basePose, models, CHAINS, 4);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                bodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 12,
                                        pos.getY(),
                                        pos.getZ() - 12,
                                        pos.getX() + 13,
                                        pos.getY() + 10,
                                        pos.getZ() + 13)
                                .inflate(1)));
        parts.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateOn();
        updateTick();
    }

    private static ChainInstance.Chain arm(int joints) {
        var chain =
                new ChainInstance.Chain()
                        .rotateY(TURN, -Mth.DEG_TO_RAD)
                        .translate(0F, 1.75F, 0F)
                        .rotateX(ANGLE, Mth.DEG_TO_RAD)
                        .translate(0F, -1.75F, 0F);
        if (joints > 1) {
            chain.translate(0F, 1.75F, -4F)
                    .rotateX(ANGLE, -2F * Mth.DEG_TO_RAD)
                    .translate(0F, -1.75F, 4F)
                    .translate(-.01F, 0F, 0F);
        }
        if (joints > 2) {
            chain.translate(.01F, 0F, 0F)
                    .translate(0F, 1.75F, -8F)
                    .rotateX(ANGLE, Mth.DEG_TO_RAD)
                    .translate(0F, -1.75F, 8F);
        }
        if (joints > 3)
            chain.translate(0F, 1.75F, -10F)
                    .rotateY(SPIN, -Mth.DEG_TO_RAD)
                    .translate(0F, -1.75F, 10F);
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
        shake();
        parts.write();
    }

    private void updateTick() {
        parts.set(TURN, blockEntity.prevRotationYaw, blockEntity.rotationYaw);
        parts.set(ANGLE, 80F - blockEntity.prevRotationPitch, 80F - blockEntity.rotationPitch);
        parts.set(SPIN, blockEntity.lastSpin, blockEntity.spin);
        shake();
        parts.write();
    }

    private void shake() {
        if (lastOn) {
            float from = (float) (level.getGameTime() * 2 % (2 * Math.PI));
            parts.set(SHAKE, from, from + 1F);
        } else {
            parts.set(SHAKE, 0F, 0F);
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
