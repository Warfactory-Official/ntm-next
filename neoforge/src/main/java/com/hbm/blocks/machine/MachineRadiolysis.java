// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.tileentity.machine.BlockEntityMachineRadiolysis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineRadiolysis extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 1, 1, 1, 1};

    public MachineRadiolysis(Properties props) {
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
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, domains);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) BossSpawnHandler.markFBI(player);
        return super.useAtCore(coreState, level, core, player, hit);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineRadiolysis(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RADIOLYSIS)
                .powerOut()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe()
                .fluidFaces(
                        BlockEntityMachineRadiolysis.class,
                        (be, face) -> face.side() != Direction.DOWN);
    }
}
