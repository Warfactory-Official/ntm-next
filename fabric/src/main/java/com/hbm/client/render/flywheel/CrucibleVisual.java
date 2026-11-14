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

import com.hbm.blockentity.machine.CrucibleBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.data.MachineData;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class CrucibleVisual extends AbstractBoundedBlockEntityVisual<CrucibleBlockEntity>
        implements CulledDynamicVisual {
    private static final int POUR_REACH = 6;
    private static final MeshPart MELT_MODEL =
            FoundryTankVisual.molten(
                    MeshPart.face(
                            Direction.UP,
                            SimpleMaterial.builderOf(FoundryTankVisual.MOLTEN)
                                    .texture(ResourceManager.foundry_lava_tex)
                                    .build()));

    private final TransformedInstance melt;
    private final ArrayList<PourVisual> pours = new ArrayList<>();
    private final Matrix4f local = new Matrix4f(), pose = new Matrix4f();
    private final Direction facing;
    private final Matrix4f basePose = new Matrix4f();
    private int lastMass = Integer.MIN_VALUE;

    public CrucibleVisual(
            VisualizationContext context, CrucibleBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        facing = MultiblockCoreBlock.coreFacing(blockState);
        basePose.translation(.5F, 0F, .5F).rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD);
        melt =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MELT_MODEL.model())
                        .createInstance();
        melt.setVisible(false);
    }

    public static void initModels() {}

    @Override
    public boolean doDistanceLimitThisFrame(Context context) {
        return lastMass != Integer.MIN_VALUE && super.doDistanceLimitThisFrame(context);
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int
                capacity =
                        MachineData.CRUCIBLE_RECIPE_CAPACITY.get()
                                + MachineData.CRUCIBLE_WASTE_CAPACITY.get(),
                mass = 0;
        for (var stack : blockEntity.recipeStack) mass += stack.amount;
        for (var stack : blockEntity.wasteStack) mass += stack.amount;
        if (mass != lastMass) {
            melt.setVisible(mass > 0);
            if (mass > 0) {
                float height = (float) (.5D + (double) mass / capacity * .875D);
                local.set(basePose)
                        .translate(0F, height, 0F)
                        .translate(-1F, -1F, -1F)
                        .scale(2F, 1F, 2F);
                pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
                melt.setTransform(pose).light(LightCoordsUtil.FULL_BRIGHT).setChanged();
            }
            lastMass = mass;
        }
        long now = level.getGameTime();
        int used = 0;
        for (var stream : blockEntity.streams) {
            float age = now - stream.birth() + partialTick;
            if (age >= 20F) continue;
            if (used == pours.size()) pours.add(new PourVisual(visualizationContext, level, pos));
            Direction direction = stream.waste() ? facing.getOpposite() : facing;
            pours.get(used++)
                    .update(
                            stream.color(),
                            direction,
                            stream.len(),
                            age,
                            .625F,
                            .625F,
                            (float) (.5D + direction.getStepX() * 1.875D),
                            0,
                            (float) (.5D + direction.getStepZ() * 1.875D));
        }
        while (pours.size() > used) pours.removeLast().delete();
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(pos).inflate(2).expandTowards(0, -POUR_REACH, 0);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        melt.delete();
        for (var pour : pours) pour.delete();
        pours.clear();
    }
}
