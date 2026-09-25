// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineAnnihilator;
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

public class MachineAnnihilator extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        2, 0, 4, 4, 1, 1, 0, 0, 0,
        8, -2, 1, 1, 1, 1, -3, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {2, 0, 4, 4, 1, 1};

    private static final int[] FLUE = {8, -2, 1, 1, 1, 1};

    public MachineAnnihilator(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 4;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * (o - 3), 0, dir.getStepZ() * (o - 3));
        return MultiblockHandlerXR.checkSpace(level, origin, FLUE, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);
        MultiblockHandlerXR.visitBox(core.relative(facing, -3), FLUE, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing, 3).relative(rot), MASK_WEST);
        visitor.cell(core.relative(facing, 3).relative(rot, -1), MASK_EAST);
        visitor.cell(core.relative(facing, 4), MASK_SOUTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        visitor.passiveCell(
                core.relative(facing, 3).relative(rot), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.relative(facing, 3).relative(rot, -1),
                MASK_ALL,
                PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(core.relative(facing, 4), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
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
        return new BlockEntityMachineAnnihilator(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().itemsAtCells();
    }
}
