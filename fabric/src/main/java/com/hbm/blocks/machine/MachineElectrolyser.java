// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser;
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

public class MachineElectrolyser extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        0, 0, 5, 5, 1, 3, 0, 0, 0,
        2, -1, 5, 5, 1, 1, 0, 0, 0,
        3, -3, 5, 5, 0, 0, 0, 0, 0,
        3, -1, 4, -4, -3, 3, 0, 0, 0,
        3, -1, 2, -2, -3, 3, 0, 0, 0,
        3, -1, 0, 0, -3, 3, 0, 0, 0,
        3, -1, -2, 2, -3, 3, 0, 0, 0,
        3, -1, -4, 4, -3, 3, 0, 0, 0,
        0, 0, 0, 0, -1, 2, 4, 3, 0,
        0, 0, 0, 0, -1, 2, 2, 3, 0,
        0, 0, 0, 0, -1, 2, 0, 3, 0,
        0, 0, 0, 0, -1, 2, -2, 3, 0,
        0, 0, 0, 0, -1, 2, -4, 3, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {0, 0, 5, 5, 1, 3};
    private static final int[] TOWER = {0, 0, 0, 0, -1, 2};

    private static final int[][] SLABS = {
        {2, -1, 5, 5, 1, 1},
        {3, -3, 5, 5, 0, 0},
        {3, -1, 4, -4, -3, 3},
        {3, -1, 2, -2, -3, 3},
        {3, -1, 0, 0, -3, 3},
        {3, -1, -2, 2, -3, 3},
        {3, -1, -4, 4, -3, 3},
    };

    private static final int[] TOWER_STEPS = {4, 2, 0, -2, -4};

    public MachineElectrolyser(Properties props) {
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
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        for (int[] slab : SLABS) {
            MultiblockHandlerXR.visitBox(core, slab, facing, visitor);
        }
        for (int step : TOWER_STEPS) {
            MultiblockHandlerXR.visitBox(
                    core.offset(facing.getStepX() * step, 3, facing.getStepZ() * step),
                    TOWER,
                    facing,
                    visitor);
        }

        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(-dx * 5, 0, -dz * 5), MASK_NORTH);
        visitor.cell(core.offset(-dx * 5 + rx, 0, -dz * 5 + rz), MASK_NORTH);
        visitor.cell(core.offset(-dx * 5 - rx, 0, -dz * 5 - rz), MASK_NORTH);
        visitor.cell(core.offset(dx * 5, 0, dz * 5), MASK_SOUTH);
        visitor.cell(core.offset(dx * 5 + rx, 0, dz * 5 + rz), MASK_SOUTH);
        visitor.cell(core.offset(dx * 5 - rx, 0, dz * 5 - rz), MASK_SOUTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;

        visitor.passiveCell(core.offset(-dx * 5, 0, -dz * 5), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dx * 5 + rx, 0, -dz * 5 + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dx * 5 - rx, 0, -dz * 5 - rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx * 5, 0, dz * 5), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx * 5 + rx, 0, dz * 5 + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx * 5 - rx, 0, dz * 5 - rz), MASK_ALL, domains);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);

        for (int[] slab : SLABS) {
            if (!MultiblockHandlerXR.checkSpace(level, origin, slab, placed, dir)) return false;
        }
        for (int step : TOWER_STEPS) {
            BlockPos at = origin.offset(dir.getStepX() * step, 3, dir.getStepZ() * step);
            if (!MultiblockHandlerXR.checkSpace(level, at, TOWER, placed, dir)) return false;
        }
        return true;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof BlockEntityMachineElectrolyser be) {

            IGUIProvider.openBlockMenu(player, be, core, be.getSelectedGUI());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineElectrolyser(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ELECTROLYSER)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe();
    }
}
