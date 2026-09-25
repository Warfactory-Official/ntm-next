// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityICF;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineICF extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        5, 0, 1, 1, 8, 8, 0, 0, 0,
        1, 1, -1, 2, 8, 8, 0, 3, 0,
        1, 1, 2, -1, 8, 8, 0, 3, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {5, 0, 1, 1, 8, 8};

    private static final int[] WING_A = {1, 1, -1, 2, 8, 8};
    private static final int[] WING_B = {1, 1, 2, -1, 8, 8};

    public MachineICF(Properties props) {
        super(props);
    }

    private static BlockPos chimney(BlockPos core) {
        return core.above(5);
    }

    private static BlockPos corner(
            BlockPos core, Direction dir, Direction rot, int alongSign, int sideSign) {
        return core.offset(
                dir.getStepX() * 2 * alongSign + rot.getStepX() * 6 * sideSign,
                3,
                dir.getStepZ() * 2 * alongSign + rot.getStepZ() * 6 * sideSign);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        BlockPos wingOrigin = core.above(3);
        MultiblockHandlerXR.visitBox(wingOrigin, WING_A, facing, visitor);
        MultiblockHandlerXR.visitBox(wingOrigin, WING_B, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(chimney(core), MASK_UP);
        for (int alongSign : new int[] {1, -1}) {

            int mask = alongSign > 0 ? MASK_SOUTH : MASK_NORTH;
            for (int sideSign : new int[] {1, -1}) {
                visitor.cell(corner(core, facing, rot, alongSign, sideSign), mask);
            }
        }
    }

    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        Direction rot = facing.getClockWise();
        visitor.passiveCell(chimney(core), MASK_ALL, domains);
        for (int alongSign : new int[] {1, -1}) {
            for (int sideSign : new int[] {1, -1}) {
                visitor.passiveCell(
                        corner(core, facing, rot, alongSign, sideSign), MASK_ALL, domains);
            }
        }
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos wingOrigin = placed.offset(dir.getStepX() * o, 3, dir.getStepZ() * o);
        if (!MultiblockHandlerXR.checkSpace(level, wingOrigin, WING_A, placed, dir)) return false;
        return MultiblockHandlerXR.checkSpace(level, wingOrigin, WING_B, placed, dir);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu)
            openCoreMenu(player, core, menu);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityICF(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut().itemsAtCells();
    }
}
