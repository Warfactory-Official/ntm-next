// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineExcavator;
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

public class MachineExcavator extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        3, 0, 3, 3, 3, 3, 0, 0, 0,
        -1, 3, 3, -2, 3, -2, 0, 0, 0,
        -1, 3, 3, -2, -2, 3, 0, 0, 0,
        -1, 3, -2, 3, 3, 3, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {3, 0, 3, 3, 3, 3};

    private static final int[] LEG_SPACE_A = {-1, 3, 3, -2, 3, -2};
    private static final int[] LEG_SPACE_B = {-1, 3, 3, -2, -2, 3};
    private static final int[] LEG_SPACE_C = {-1, 3, -2, 3, 3, 3};

    public MachineExcavator(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    public int getHeightOffset() {
        return 3;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos core = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return MultiblockHandlerXR.checkSpace(level, core, LEG_SPACE_A, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, core, LEG_SPACE_B, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, core, LEG_SPACE_C, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        MultiblockHandlerXR.visitBox(core, LEG_SPACE_A, facing, visitor);
        MultiblockHandlerXR.visitBox(core, LEG_SPACE_B, facing, visitor);
        MultiblockHandlerXR.visitBox(core, LEG_SPACE_C, facing, visitor);

        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(dx * 3 + rx, 1, dz * 3 + rz), MASK_SOUTH, ROLE_ALL);
        visitor.cell(core.offset(dx * 3 - rx, 1, dz * 3 - rz), MASK_SOUTH, ROLE_ALL);
        visitor.cell(core.offset(rx * 3, 1, rz * 3), MASK_WEST, ROLE_ALL);
        visitor.cell(core.offset(-rx * 3, 1, -rz * 3), MASK_EAST, ROLE_ALL);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(dx * 3 + rx, 1, dz * 3 + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx * 3 - rx, 1, dz * 3 - rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rx * 3, 1, rz * 3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rx * 3, 1, -rz * 3), MASK_ALL, domains);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineExcavator(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.EXCAVATOR).powerIn().fluidIn().fe();
    }
}
