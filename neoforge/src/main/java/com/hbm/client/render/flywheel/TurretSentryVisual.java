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

import com.hbm.blockentity.turret.TurretSentryBlockEntity;
import com.hbm.blockentity.turret.TurretSentryDamagedBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TurretSentryVisual
        extends AbstractBoundedBlockEntityVisual<TurretSentryBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.turret_sentry;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.turret_sentry_tex);
    private static final Material DAMAGED_MATERIAL =
            MeshPart.litCutout(ResourceManager.turret_sentry_damaged_tex);
    private static final int GROUP_1 = MODEL.partId("Pivot");
    private static final int GROUP_2 = MODEL.partId("Body");
    private static final int GROUP_3 = MODEL.partId("Drum");
    private static final int GROUP_4 = MODEL.partId("BarrelL");
    private static final int GROUP_5 = MODEL.partId("BarrelR");
    private static final MeshPart PART_1 = MeshPart.obj(MODEL.groups[GROUP_1], true, MATERIAL);
    private static final MeshPart PART_2 = MeshPart.obj(MODEL.groups[GROUP_2], true, MATERIAL);
    private static final MeshPart PART_3 = MeshPart.obj(MODEL.groups[GROUP_3], true, MATERIAL);
    private static final MeshPart PART_4 = MeshPart.obj(MODEL.groups[GROUP_4], true, MATERIAL);
    private static final MeshPart PART_5 = MeshPart.obj(MODEL.groups[GROUP_5], true, MATERIAL);
    private static final MeshPart DAMAGED_1 =
            MeshPart.obj(MODEL.groups[GROUP_1], true, DAMAGED_MATERIAL);
    private static final MeshPart DAMAGED_2 =
            MeshPart.obj(MODEL.groups[GROUP_2], true, DAMAGED_MATERIAL);
    private static final MeshPart DAMAGED_3 =
            MeshPart.obj(MODEL.groups[GROUP_3], true, DAMAGED_MATERIAL);
    private static final MeshPart DAMAGED_4 =
            MeshPart.obj(MODEL.groups[GROUP_4], true, DAMAGED_MATERIAL);
    private static final MeshPart DAMAGED_5 =
            MeshPart.obj(MODEL.groups[GROUP_5], true, DAMAGED_MATERIAL);
    private static final int LEFT = 2, RIGHT = 3, JAMMED = 4;
    private static final ChainInstance.Chain BODY = TurretChains.pitched(1.25F, true, -1F).end();
    private static final ChainInstance.Chain[] CHAINS = {
        TurretChains.carriage(),
        BODY,
        BODY,
        TurretChains.pitched(1.25F, true, -1F).slideZ(LEFT, -.5F),
        TurretChains.pitched(1.25F, true, -1F).slideZ(RIGHT, -.5F)
    };
    private static final ChainInstance.Chain[] DAMAGED_CHAINS = {
        TurretChains.carriage(),
        BODY,
        BODY,
        TurretChains.pitched(1.25F, true, -1F).slideZ(LEFT, -.5F),
        TurretChains.pitched(1.25F, true, -1F)
                .translate(0F, 1.5F, .5F)
                .rotateX(JAMMED, Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, -.5F)
                .end()
    };
    private static final float[] RECOIL_LOW = new float[5];
    private static final float[] RECOIL_HIGH = {0F, 0F, 1.5F, 1.5F, 0F};
    private final AABB lightBounds;
    private final ChainParts parts;

    public TurretSentryVisual(
            VisualizationContext context, TurretSentryBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        Matrix4f placement = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        boolean damaged = blockEntity instanceof TurretSentryDamagedBlockEntity;
        Model[] models =
                damaged
                        ? new Model[] {
                            DAMAGED_1.model(),
                            DAMAGED_2.model(),
                            DAMAGED_3.model(),
                            DAMAGED_4.model(),
                            DAMAGED_5.model()
                        }
                        : new Model[] {
                            PART_1.model(),
                            PART_2.model(),
                            PART_3.model(),
                            PART_4.model(),
                            PART_5.model()
                        };
        AABB baseBounds = LightBounds.of(MODEL, "Base", placement, pos);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        placement,
                        models,
                        damaged ? DAMAGED_CHAINS : CHAINS,
                        5);
        parts.set(JAMMED, 25F, 25F);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, baseBounds.minmax(new AABB(pos).inflate(8)));
        parts.includeLightBounds(bounds, pos, RECOIL_LOW, RECOIL_HIGH);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        update();
    }

    public static void initModels() {}

    @Override
    public void tick(Context context) {
        update();
    }

    private void update() {
        TurretChains.aim(parts, blockEntity, 0D);
        parts.set(LEFT, (float) blockEntity.lastBarrelLeftPos, (float) blockEntity.barrelLeftPos);
        parts.set(
                RIGHT, (float) blockEntity.lastBarrelRightPos, (float) blockEntity.barrelRightPos);
        parts.write();
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
