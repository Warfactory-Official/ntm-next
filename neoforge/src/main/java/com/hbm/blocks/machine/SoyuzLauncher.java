// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.tileentity.machine.BlockEntitySoyuzLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

@Deprecated
public class SoyuzLauncher extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        0, 1, 6, 6, 6, 6, 0, 0, 0,
        -2, 4, -3, 6, -3, 6, 0, 0, 0,
        -2, 4, 6, -3, -3, 6, 0, 0, 0,
        -2, 4, 6, -3, 6, -3, 0, 0, 0,
        -2, 4, -3, 6, 6, -3, 0, 0, 0,
        0, 4, 1, 1, -6, 8, 0, 0, 0,
        0, 4, 2, 2, 9, -5, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    public static final int HEIGHT = 4;

    public static final Direction FACING = Direction.EAST;

    private static final int RADIUS = 6;

    private static final int[] PAD = {0, 1, RADIUS, RADIUS, RADIUS, RADIUS};

    private static final int[][] LEGS = {
        {-2, 4, -3, 6, -3, 6}, {-2, 4, 6, -3, -3, 6}, {-2, 4, 6, -3, 6, -3}, {-2, 4, -3, 6, 6, -3}
    };

    private static final int[] SUPPORT = {0, 4, 1, 1, -6, 8};

    private static final int[] TOWER = {0, 4, 2, 2, 9, -5};

    public SoyuzLauncher(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return PAD;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return HEIGHT;
    }

    @Override
    public Direction getDirModified(Direction dir) {
        return FACING;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);

        for (int[] leg : LEGS) {
            if (!MultiblockHandlerXR.checkSpace(level, origin, leg, placed, dir)) return false;
        }
        return MultiblockHandlerXR.checkSpace(level, origin, SUPPORT, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, TOWER, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {

        super.visitCells(core, facing, visitor);
        for (int[] leg : LEGS) MultiblockHandlerXR.visitBox(core, leg, facing, visitor);
        MultiblockHandlerXR.visitBox(core, SUPPORT, facing, visitor);
        MultiblockHandlerXR.visitBox(core, TOWER, facing, visitor);

        for (int ly = -1; ly <= 0; ly++) {
            for (int i = -RADIUS; i <= RADIUS; i++) {
                int corner = Math.abs(i) == RADIUS ? (i > 0 ? MASK_EAST : MASK_WEST) : MASK_NONE;
                visitor.cell(core.offset(i, ly, RADIUS), MASK_SOUTH | corner);
                visitor.cell(core.offset(i, ly, -RADIUS), MASK_NORTH | corner);
                if (corner != MASK_NONE) continue;
                visitor.cell(core.offset(RADIUS, ly, i), MASK_EAST);
                visitor.cell(core.offset(-RADIUS, ly, i), MASK_WEST);
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int supplied = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        for (int i = -RADIUS; i <= RADIUS; i++) {
            visitor.passiveCell(core.offset(i, 0, RADIUS), MASK_ALL, supplied);
            visitor.passiveCell(core.offset(i, 0, -RADIUS), MASK_ALL, supplied);
            visitor.passiveCell(core.offset(RADIUS, 0, i), MASK_ALL, supplied);
            visitor.passiveCell(core.offset(-RADIUS, 0, i), MASK_ALL, supplied);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) BossSpawnHandler.markFBI(player);
        return super.useAtCore(coreState, level, core, player, hit);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySoyuzLauncher(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.SOYUZ_LAUNCHER)
                .powerIn()
                .fluidIn()
                .fe()
                .fluidFaces(
                        BlockEntitySoyuzLauncher.class,
                        (be, face) -> face.side().getAxis().isHorizontal());
    }
}
