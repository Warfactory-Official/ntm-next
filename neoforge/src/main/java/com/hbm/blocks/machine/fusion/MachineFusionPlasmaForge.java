// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionPlasmaForge;
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

public class MachineFusionPlasmaForge extends BlockFusionMachine implements ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        2, 0, 2, 2, 5, 5, 0, 0, 0,
        4, -3, 0, 0, 4, 4, 0, 0, 0,
        2, 0, 3, -2, 4, 4, 0, 0, 0,
        2, 0, -2, 3, 4, 4, 0, 0, 0,
        2, 0, 4, -3, 3, 3, 0, 0, 0,
        2, 0, -3, 4, 3, 3, 0, 0, 0,
        2, 0, 5, -4, 2, 2, 0, 0, 0,
        2, 0, -4, 5, 2, 2, 0, 0, 0,
        3, -2, 1, 1, 5, 5, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final double[] PLACEMENT_EXTRAS = {
        1.5, 3.5, 1, -1, 5.5, 5.5,
        1.5, 3.5, 1, -1, -5.5, -5.5
    };

    @Override
    protected double[] placementExtraBoxes() {
        return PLACEMENT_EXTRAS;
    }

    private static final int[] DIMENSIONS = {2, 0, 2, 2, 5, 5};

    private static final int[][] EXTRA_BOXES = {
        {2, 0, 3, -2, 4, 4},
        {2, 0, -2, 3, 4, 4},
        {2, 0, 4, -3, 3, 3},
        {2, 0, -3, 4, 3, 3},
        {2, 0, 5, -4, 2, 2},
        {2, 0, -4, 5, 2, 2},
        {3, -2, 1, 1, 5, 5},
        {4, -3, 0, 0, 4, 4}
    };

    public MachineFusionPlasmaForge(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 5;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        for (int[] box : EXTRA_BOXES) {
            if (!MultiblockHandlerXR.checkSpace(level, origin, box, placed, dir)) return false;
        }
        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] box : EXTRA_BOXES) MultiblockHandlerXR.visitBox(core, box, facing, visitor);

        Direction rot = facing.getClockWise();
        for (int i = -2; i <= 2; i++) {
            visitor.cell(
                    core.offset(
                            facing.getStepX() * 5 + rot.getStepX() * i,
                            0,
                            facing.getStepZ() * 5 + rot.getStepZ() * i),
                    MASK_SOUTH);
            visitor.cell(
                    core.offset(
                            -facing.getStepX() * 5 + rot.getStepX() * i,
                            0,
                            -facing.getStepZ() * 5 + rot.getStepZ() * i),
                    MASK_NORTH);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int i = -2; i <= 2; i++) {
            visitor.passiveCell(
                    core.offset(
                            facing.getStepX() * 5 + rot.getStepX() * i,
                            0,
                            facing.getStepZ() * 5 + rot.getStepZ() * i),
                    MASK_ALL,
                    domains);
            visitor.passiveCell(
                    core.offset(
                            -facing.getStepX() * 5 + rot.getStepX() * i,
                            0,
                            -facing.getStepZ() * 5 + rot.getStepZ() * i),
                    MASK_ALL,
                    domains);
        }
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
        return new BlockEntityFusionPlasmaForge(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().itemsAtCells().fe();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionPlasmaForge.links(core, facing);
    }
}
