// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.BlockEntityTowerSmall;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineTowerSmall extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {18, 0, 2, 2, 2, 2};

    private static final int PORT_REACH = 2;

    public MachineTowerSmall(Properties props) {
        super(props);
    }

    static void towerOverlay(
            ILookOverlay.LookInfo info, Block block, FluidTankNTM in, FluidTankNTM out) {
        info.title(block.getName().getString(), 0xFFFF00, 0x404000);
        info.line(
                ChatFormatting.GREEN
                        + "-> "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(in.getFluid())
                        + ": "
                        + in.getFill()
                        + "/"
                        + in.getMaxFill()
                        + "mB");
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(out.getFluid())
                        + ": "
                        + out.getFill()
                        + "/"
                        + out.getMaxFill()
                        + "mB");
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return PORT_REACH;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.offset(PORT_REACH, 0, 0), MASK_EAST, ROLE_FLUID);
        visitor.cell(core.offset(-PORT_REACH, 0, 0), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.offset(0, 0, PORT_REACH), MASK_SOUTH, ROLE_FLUID);
        visitor.cell(core.offset(0, 0, -PORT_REACH), MASK_NORTH, ROLE_FLUID);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(PORT_REACH, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-PORT_REACH, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, PORT_REACH), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, -PORT_REACH), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityTowerSmall(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityTowerSmall be)) return;
        towerOverlay(info, this, be.input, be.output);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TOWER_SMALL).fluidIn().fluidOut();
    }
}
