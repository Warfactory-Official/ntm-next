// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineRadarLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineRadarLarge extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {4, 0, 1, 1, 1, 1};

    public MachineRadarLarge(Properties props) {
        super(props);
    }

    private static boolean isConnector(int lx, int ly, int lz) {
        return ly == 0 && Math.abs(lx) + Math.abs(lz) == 1;
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
        visitor.cell(core.east(), MASK_EAST, ROLE_POWER_IN);
        visitor.cell(core.west(), MASK_WEST, ROLE_POWER_IN);
        visitor.cell(core.south(), MASK_SOUTH, ROLE_POWER_IN);
        visitor.cell(core.north(), MASK_NORTH, ROLE_POWER_IN);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.east(), MASK_ALL, PASSIVE_POWER_IN);
        visitor.passiveCell(core.west(), MASK_ALL, PASSIVE_POWER_IN);
        visitor.passiveCell(core.south(), MASK_ALL, PASSIVE_POWER_IN);
        visitor.passiveCell(core.north(), MASK_ALL, PASSIVE_POWER_IN);
    }

    @Override
    protected BlockState cellStateFor(int lx, int ly, int lz, Direction facing, int shapeId) {
        if (!isConnector(lx, ly, lz)) return super.cellStateFor(lx, ly, lz, facing, shapeId);
        return ModBlocks.RADAR_MAST_CELL
                .get()
                .defaultBlockState()
                .setValue(BlockMultiblockCell.SOUND, cellSound())
                .setValue(BlockMultiblockCell.SEALED, cellsSealRadiation());
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return MachineRadar.openAbove(level, core, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineRadarLarge(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RADAR_LARGE).powerIn().fe();
    }
}
