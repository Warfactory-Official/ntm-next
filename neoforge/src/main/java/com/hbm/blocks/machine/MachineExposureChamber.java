// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineExposureChamber;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineExposureChamber extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        4, 0, 2, 2, 2, 2, 0, 0, 0,
        3, 0, 0, 0, -3, 8, 0, 0, 0,
        0, 0, 1, -1, -3, 6, 0, 2, 0,
        0, 0, -1, 1, -3, 6, 0, 2, 0,
        3, 0, 1, -1, 0, 1, 0, 0, -7,
        3, 0, -1, 1, 0, 1, 0, 0, -7,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {4, 0, 2, 2, 2, 2};

    private static final int[] LINE = {3, 0, 0, 0, -3, 8};
    private static final int[] RAIL_NORTH = {0, 0, 1, -1, -3, 6};
    private static final int[] RAIL_SOUTH = {0, 0, -1, 1, -3, 6};
    private static final int[] TOWER_NORTH = {3, 0, 1, -1, 0, 1};
    private static final int[] TOWER_SOUTH = {3, 0, -1, 1, 0, 1};
    private static final int RAIL_LIFT = 2;
    private static final int TOWER_OFFSET = 7;

    public MachineExposureChamber(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos core = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        BlockPos rails = core.above(RAIL_LIFT);
        BlockPos towers = local(core, TOWER_OFFSET, 0, 0, dir);

        return MultiblockHandlerXR.checkSpace(level, core, LINE, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, rails, RAIL_NORTH, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, rails, RAIL_SOUTH, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, towers, TOWER_NORTH, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, towers, TOWER_SOUTH, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        MultiblockHandlerXR.visitBox(core, LINE, facing, visitor);
        BlockPos rails = core.above(RAIL_LIFT);
        MultiblockHandlerXR.visitBox(rails, RAIL_NORTH, facing, visitor);
        MultiblockHandlerXR.visitBox(rails, RAIL_SOUTH, facing, visitor);
        BlockPos towers = local(core, TOWER_OFFSET, 0, 0, facing);
        MultiblockHandlerXR.visitBox(towers, TOWER_NORTH, facing, visitor);
        MultiblockHandlerXR.visitBox(towers, TOWER_SOUTH, facing, visitor);

        visitor.cell(local(core, 7, 0, 1, facing), MASK_SOUTH);
        visitor.cell(local(core, 7, 0, -1, facing), MASK_NORTH);
        visitor.cell(local(core, 8, 0, 1, facing), MASK_SOUTH);
        visitor.cell(local(core, 8, 0, -1, facing), MASK_NORTH);
        visitor.cell(local(core, 8, 0, 0, facing), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_ITEMS;

        visitor.passiveCell(local(core, 7, 0, 1, facing), MASK_ALL, domains);
        visitor.passiveCell(local(core, 7, 0, -1, facing), MASK_ALL, domains);
        visitor.passiveCell(local(core, 8, 0, 1, facing), MASK_ALL, domains);
        visitor.passiveCell(local(core, 8, 0, -1, facing), MASK_ALL, domains);
        visitor.passiveCell(local(core, 8, 0, 0, facing), MASK_ALL, domains);
    }

    private static BlockPos local(BlockPos core, int lx, int ly, int lz, Direction facing) {
        return core.offset(
                MultiblockMaskTable.worldX(lx, ly, lz, facing),
                ly,
                MultiblockMaskTable.worldZ(lx, ly, lz, facing));
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineExposureChamber(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.EXPOSURE_CHAMBER)
                .powerIn()
                .items()
                .itemsAtCells()
                .fe();
    }
}
