// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.network.CableConductorBlockBase;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.storage.BlockEntityBatterySocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityBatterySocket.class, calling = "refreshMode")
public class MachineBatterySocket extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 1, 0, 1, 0};

    private static final int NODE_MASK =
            (1 << Direction.NORTH.ordinal())
                    | (1 << Direction.SOUTH.ordinal())
                    | (1 << Direction.WEST.ordinal())
                    | (1 << Direction.EAST.ordinal());

    public MachineBatterySocket(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityBatterySocket(pos, state);
    }

    public static BlockPos[] ports(BlockPos core, Direction facing) {
        Direction rot = facing.getClockWise();
        return new BlockPos[] {
            core,
            core.relative(facing.getOpposite()),
            core.relative(rot),
            core.relative(facing.getOpposite()).relative(rot)
        };
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        visitor.cell(
                core.offset(-facing.getStepX(), 0, -facing.getStepZ()), MASK_NORTH | MASK_EAST);
        visitor.cell(core.offset(rot.getStepX(), 0, rot.getStepZ()), MASK_WEST | MASK_SOUTH);
        visitor.cell(
                core.offset(
                        -facing.getStepX() + rot.getStepX(),
                        0,
                        -facing.getStepZ() + rot.getStepZ()),
                MASK_NORTH | MASK_WEST);
    }

    @Override
    public int coreMask() {
        return MASK_SOUTH | MASK_EAST;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(back), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(back).relative(rot), MASK_ALL, domains);
    }

    public static void mintNodes(ServerLevel level, BlockPos core, BlockState state) {
        for (BlockPos port : ports(core, coreFacing(state))) {
            CableConductorBlockBase.mintNode(level, port, state, NODE_MASK);
        }
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        for (BlockPos port : ports(pos, coreFacing(state)))
            CableConductorBlockBase.dropNode(level, port);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected boolean cellsHaveAnalogOutput() {
        return true;
    }

    @Override
    protected boolean comparatorCellAt(int lx, int ly, int lz) {
        return ly == 0;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.BATTERY_SOCKET)
                .powerIn()
                .powerOut()
                .items()
                .itemsAtCells()
                .fe();
    }
}
