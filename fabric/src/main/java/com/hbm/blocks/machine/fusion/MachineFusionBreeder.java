// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionBreeder;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import java.util.List;
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

public class MachineFusionBreeder extends BlockFusionMachine implements ICapabilityBlock {
    private static final double[] PLACEMENT_EXTRAS = {1.5, 3.5, -2.5, -2.5, 1, -1};

    @Override
    protected double[] placementExtraBoxes() {
        return PLACEMENT_EXTRAS;
    }

    private static final int[] DIMENSIONS = {3, 0, 2, 2, 1, 1};

    public MachineFusionBreeder(Properties props) {
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
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();

        visitor.cell(core.offset(rot.getStepX(), 0, rot.getStepZ()), MASK_WEST);
        visitor.cell(core.offset(-rot.getStepX(), 0, -rot.getStepZ()), MASK_EAST);
        visitor.cell(
                core.offset(
                        facing.getStepX() + rot.getStepX(), 0, facing.getStepZ() + rot.getStepZ()),
                MASK_WEST);
        visitor.cell(
                core.offset(
                        facing.getStepX() - rot.getStepX(), 0, facing.getStepZ() - rot.getStepZ()),
                MASK_EAST);
        visitor.cell(core.offset(facing.getStepX() * 2, 2, facing.getStepZ() * 2), MASK_SOUTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        visitor.passiveCell(
                core.offset(rot.getStepX(), 0, rot.getStepZ()),
                MASK_ALL,
                PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.offset(-rot.getStepX(), 0, -rot.getStepZ()),
                MASK_ALL,
                PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.offset(
                        facing.getStepX() + rot.getStepX(), 0, facing.getStepZ() + rot.getStepZ()),
                MASK_ALL,
                PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.offset(
                        facing.getStepX() - rot.getStepX(), 0, facing.getStepZ() - rot.getStepZ()),
                MASK_ALL,
                PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.offset(facing.getStepX() * 2, 2, facing.getStepZ() * 2),
                MASK_ALL,
                PASSIVE_FLUID_IN | PASSIVE_ITEMS);
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
        return new BlockEntityFusionBreeder(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut().itemsAtCells();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionBreeder.links(core, facing);
    }
}
