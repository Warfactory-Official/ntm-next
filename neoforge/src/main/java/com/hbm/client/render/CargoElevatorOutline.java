// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.BlockCargoElevator;
import com.hbm.tileentity.machine.BlockEntityCargoElevator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class CargoElevatorOutline {

    public static final RenderPipeline PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/cargo_elevator_outline")
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withDepthStencilState(
                                    new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)));
    private static final RenderType TYPE =
            RenderType.create(
                    "cargo_elevator_outline", RenderSetup.builder(PIPELINE).createRenderSetup());

    private CargoElevatorOutline() {}

    public static boolean submit(
            PoseStack pose, SubmitNodeCollector collector, LevelRenderState state) {
        var level = Minecraft.getInstance().level;
        if (level == null || state.blockOutlineRenderState == null) return false;
        BlockPos hit = state.blockOutlineRenderState.pos();
        if (!(level.getBlockState(hit).getBlock() instanceof BlockCargoElevator block))
            return false;
        BlockEntityCargoElevator elevator = block.elevator(level, hit);
        if (elevator == null) return true;
        BlockPos core = elevator.getBlockPos();
        Vec3 camera = state.cameraRenderState.pos;
        pose.pushPose();
        pose.translate(core.getX() - camera.x, core.getY() - camera.y, core.getZ() - camera.z);
        for (AABB box : elevator.boxes()) {
            collector.submitShapeOutline(
                    pose, Shapes.create(box.inflate(0.002F)), TYPE, 0x66000000, 2F, false);
        }
        pose.popPose();
        return true;
    }
}
