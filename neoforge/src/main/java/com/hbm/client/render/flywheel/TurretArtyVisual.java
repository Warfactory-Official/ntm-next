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

import com.hbm.blockentity.turret.TurretArtyBlockEntity;
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

public final class TurretArtyVisual extends AbstractBoundedBlockEntityVisual<TurretArtyBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.turret_arty;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.turret_arty_tex);
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Carriage")], MODEL.smoothing(), MATERIAL);
    private static final MeshPart CANNON_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Cannon")], MODEL.smoothing(), MATERIAL);
    private static final MeshPart BARREL_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Barrel")], MODEL.smoothing(), MATERIAL);
    private static final int RECOIL = 2;
    private static final ChainInstance.Chain[] CHAINS = {
        TurretChains.carriage(),
        TurretChains.pitched(3F, true, 1F).end(),
        TurretChains.pitched(3F, true, 1F).slideZ(RECOIL, 2.5F)
    };
    private static final float[] RECOIL_LOW = {0F, 0F, 0F};
    private static final float[] RECOIL_HIGH = {0F, 0F, 1.5F};
    private final AABB lightBounds;
    private final ChainParts parts;

    public TurretArtyVisual(
            VisualizationContext context, TurretArtyBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        Matrix4f bodyLocal = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        AABB bodyBounds = LightBounds.of(MODEL, "Base", bodyLocal, pos);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        bodyLocal,
                        new Model[] {
                            CARRIAGE_PART.model(), CANNON_PART.model(), BARREL_PART.model()
                        },
                        CHAINS,
                        3);
        double[] bounds = new double[6];
        LightBounds.resetBounds(bounds, bodyBounds.minmax(new AABB(pos).inflate(12)));
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
        TurretChains.aim(parts, blockEntity, Mth.PI);
        parts.set(RECOIL, (float) blockEntity.lastBarrelPos, (float) blockEntity.barrelPos);
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
