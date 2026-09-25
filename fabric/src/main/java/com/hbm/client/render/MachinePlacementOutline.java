// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class MachinePlacementOutline {
    private MachinePlacementOutline() {}

    public static @Nullable State extract(
            Level level, BlockHitResult hit, @Nullable Player player) {

        if (player == null
                || hit.getType() != HitResult.Type.BLOCK
                || !(player.getMainHandItem().getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof BlockMultiblockCore block)) return null;

        int quadrant = Mth.floor(player.getYRot() * 4F / block.placementYawSpan() + 0.5D) & 3;
        Direction direction =
                block.getDirModified(
                        switch (quadrant) {
                            case 1 -> Direction.EAST;
                            case 2 -> Direction.SOUTH;
                            case 3 -> Direction.WEST;
                            default -> Direction.NORTH;
                        });
        int offset = -block.getOffset();
        BlockPos placed =
                hit.getBlockPos().relative(hit.getDirection()).above(block.getHeightOffset());
        BlockPos core =
                placed.offset(
                        direction.getStepX() * offset,
                        direction.getStepY() * offset,
                        direction.getStepZ() * offset);
        return new State(
                core,
                block.placementOutline(direction),
                block.placementExtraOutlines(direction),
                block.checkRequirement(level, placed, direction, offset));
    }

    public static void submit(
            State state,
            BlockOutlineRenderState outline,
            SubmitNodeCollector collector,
            PoseStack poses,
            LevelRenderState levelState) {
        int pulse =
                (int)
                        (255
                                * (Math.sin((GameTime.now() % (1000D * Math.PI)) / 250D) * 0.25D
                                        + 0.75D));
        int color = state.placeable ? 0xFF000000 | pulse << 8 : 0xFF000000 | pulse << 16;
        Vec3 camera = levelState.cameraRenderState.pos;
        poses.pushPose();
        poses.translate(
                state.core.getX() - camera.x,
                state.core.getY() - camera.y,
                state.core.getZ() - camera.z);

        collector.submitShapeOutline(
                poses,
                state.shape,
                RenderTypes.linesTranslucent(),
                color,
                2F,
                outline.isTranslucent());
        for (VoxelShape extra : state.extras) {
            collector.submitShapeOutline(
                    poses,
                    extra,
                    RenderTypes.linesTranslucent(),
                    0xFF000000 | pulse,
                    2F,
                    outline.isTranslucent());
        }
        poses.popPose();
    }

    public record State(BlockPos core, VoxelShape shape, VoxelShape[] extras, boolean placeable) {}
}
