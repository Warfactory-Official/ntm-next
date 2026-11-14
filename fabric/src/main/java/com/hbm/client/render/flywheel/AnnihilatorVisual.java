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

import com.hbm.blockentity.machine.AnnihilatorBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class AnnihilatorVisual
        extends AbstractBoundedBlockEntityVisual<AnnihilatorBlockEntity>
        implements ShaderLightVisual {
    private static final int ROLLER = ResourceManager.annihilator.partId("Roller");
    private static final int BELT = ResourceManager.annihilator.partId("Belt");
    private static final HFRWavefrontObject MODEL = ResourceManager.annihilator;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.annihilator_tex);
    private static final MeshPart ROLLER_PART =
            MeshPart.obj(MODEL.groups[ROLLER], MODEL.smoothing(), BODY_MATERIAL);
    private static final MeshPart BELT_PART =
            MeshPart.obj(
                    MODEL.groups[BELT],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.annihilator_belt_tex));
    private static final int ROLLER_PERIOD = 2400;
    private static final int BELT_PERIOD = 3000;
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .clock(0)
                .translate(0F, 1.75F, 0F)
                .rotateZ(0, -Mth.DEG_TO_RAD)
                .translate(0F, -1.75F, 0F)
                .end()
    };
    private final ChainParts roller;
    private final ScrollInstance belt;
    private final AABB rawBodyBounds;

    public AnnihilatorVisual(
            VisualizationContext context, AnnihilatorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f base =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(
                                                MultiblockCoreBlock.coreFacing(
                                                        blockEntity.getBlockState()),
                                                90)
                                        * Mth.DEG_TO_RAD);
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Annihilator", base, pos));
        roller =
                new ChainParts(
                        context, visualPos, base, new Model[] {ROLLER_PART.model()}, CHAINS, 1);
        roller.clock(ChainInstance.clockBase(GameTime.now(), ROLLER_PERIOD), ROLLER_PERIOD);
        roller.set(0, 0F, .15F);
        roller.write();
        belt =
                instancerProvider()
                        .instancer(ScrollInstance.TYPE, BELT_PART.model())
                        .createInstance();
        belt.clock(ChainInstance.clockBase(GameTime.now(), BELT_PERIOD), BELT_PERIOD)
                .scroll(0F, 1F / BELT_PERIOD)
                .direction(-1F, 0F)
                .setTransform(
                        new Matrix4f()
                                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(base))
                .light(0)
                .setChanged();
    }

    public static void initModels() {}

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 5,
                                pos.getY(),
                                pos.getZ() - 5,
                                pos.getX() + 6,
                                pos.getY() + 8,
                                pos.getZ() + 6)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        roller.collectCrumblingInstances(consumer);
        consumer.accept(belt);
    }

    @Override
    protected void _delete() {
        roller.delete();
        belt.delete();
    }
}
