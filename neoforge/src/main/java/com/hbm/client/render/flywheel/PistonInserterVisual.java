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

import com.hbm.blockentity.machine.PistonInserterBlockEntity;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.PistonInserterRenderer;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class PistonInserterVisual
        extends AbstractBoundedBlockEntityVisual<PistonInserterBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final double STROKE = .9375D;
    private static final MeshPart PISTON_PART =
            MeshPart.obj(
                    ResourceManager.piston_inserter
                            .groups[ResourceManager.piston_inserter.partId("Piston")],
                    ResourceManager.piston_inserter.smoothing(),
                    MeshPart.litCutout(ResourceManager.piston_inserter_tex));
    private final AABB frameBounds;
    private final Direction facing;
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().slideY(0, (float) STROKE)
    };
    private final ChainParts piston;
    private final WorldItem item = new WorldItem(visualizationContext, level, pos);
    private final PoseStack itemPoses = new PoseStack();

    public PistonInserterVisual(
            VisualizationContext context,
            PistonInserterBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        facing = blockEntity.facing();
        Matrix4f framePose = new Matrix4f().translation(.5F, .5F, .5F);
        rotate(framePose, facing);
        framePose.translate(0F, -.5F, 0F);
        frameBounds = LightBounds.of(ResourceManager.piston_inserter, "Frame", framePose, pos);
        piston =
                new ChainParts(
                        context,
                        visualPos,
                        framePose,
                        new Model[] {PISTON_PART.model()},
                        CHAINS,
                        1);
        updatePiston();
        updateItem(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(PistonInserterBlockEntity be) {
        return !WorldItem.drawsFramed(be.getItem(0));
    }

    private static void rotate(Matrix4f pose, Direction facing) {
        switch (facing) {
            case DOWN -> pose.rotateX(180F * Mth.DEG_TO_RAD);
            case UP -> {}
            case NORTH -> pose.rotateX(-90F * Mth.DEG_TO_RAD).rotateY(180F * Mth.DEG_TO_RAD);
            case SOUTH -> pose.rotateX(90F * Mth.DEG_TO_RAD);
            case WEST -> pose.rotateZ(90F * Mth.DEG_TO_RAD).rotateY(-90F * Mth.DEG_TO_RAD);
            case EAST -> pose.rotateZ(-90F * Mth.DEG_TO_RAD).rotateY(90F * Mth.DEG_TO_RAD);
        }
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateItem(context.partialTick());
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updatePiston();
    }

    private void updatePiston() {
        piston.set(
                0,
                (float) (blockEntity.previousExtension / PistonInserterBlockEntity.TRAVEL_TICKS),
                (float) (blockEntity.displayedExtension / PistonInserterBlockEntity.TRAVEL_TICKS));
        piston.write();
    }

    private void updateItem(float partialTick) {
        ItemStack stack = blockEntity.getItem(0);
        FramedItem.Arm arm = item.setFramed(stack, level);
        if (arm == null) return;
        itemPoses.setIdentity();
        itemPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        float extend =
                (float)
                        ((blockEntity.previousExtension
                                        + (blockEntity.displayedExtension
                                                        - blockEntity.previousExtension)
                                                * partialTick)
                                / PistonInserterBlockEntity.TRAVEL_TICKS);
        PistonInserterRenderer.pistonPose(itemPoses, facing, extend);
        PistonInserterRenderer.itemPose(itemPoses, stack.getItem() instanceof BlockItem, arm);
        item.write(itemPoses.last().pose(), partialTick);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return frameBounds.minmax(
                new AABB(pos)
                        .expandTowards(facing.getStepX(), facing.getStepY(), facing.getStepZ())
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        piston.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        piston.delete();
        item.delete();
    }
}
