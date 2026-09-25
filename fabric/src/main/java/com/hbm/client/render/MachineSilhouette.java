// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.IBlockHighlight;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class MachineSilhouette {
    private MachineSilhouette() {}

    public static @Nullable State extract(
            Level level, BlockPos hit, BlockState hitState, Vec3 camera) {
        if (!MultiblockSurface.isSurface(hitState)) return extractResolved(hit, hitState, camera);
        BlockPos core = MultiblockSurface.coreOfAny(level, hit, hitState);
        if (core == null) return null;
        BlockState coreState = core.equals(hit) ? hitState : level.getBlockState(core);
        return extractResolved(core, coreState, camera);
    }

    static @Nullable State extractResolved(BlockPos core, BlockState coreState, Vec3 camera) {
        VoxelShape shape =
                shapeForState(
                        coreState,
                        camera.x - core.getX(),
                        camera.y - core.getY(),
                        camera.z - core.getZ());
        if (shape == null) return null;
        return new State(core.immutable(), shape);
    }

    public static @Nullable VoxelShape shapeForState(
            BlockState state, double cameraX, double cameraY, double cameraZ) {
        if (!(state.getBlock() instanceof IBlockHighlight block)) return null;
        return block.visualOutline(state).at(cameraX, cameraY, cameraZ);
    }

    public static void submit(
            State state,
            BlockOutlineRenderState outline,
            SubmitNodeCollector collector,
            PoseStack poseStack,
            LevelRenderState levelState,
            float lineWidth) {
        Vec3 camera = levelState.cameraRenderState.pos;
        BlockPos core = state.core();
        poseStack.pushPose();
        poseStack.translate(core.getX() - camera.x, core.getY() - camera.y, core.getZ() - camera.z);
        if (outline.highContrast()) {
            collector.submitShapeOutline(
                    poseStack,
                    state.shape(),
                    RenderTypes.secondaryBlockOutline(),
                    -16777216,
                    7F,
                    outline.isTranslucent());
        }
        int color = outline.highContrast() ? -11010079 : ARGB.black(102);
        collector.submitShapeOutline(
                poseStack,
                state.shape(),
                RenderTypes.lines(),
                color,
                lineWidth,
                outline.isTranslucent());
        poseStack.popPose();
    }

    public record State(BlockPos core, VoxelShape shape) {}
}
