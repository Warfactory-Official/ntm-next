// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionBoiler;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineFusionBoiler extends BlockFusionMachine
        implements ILookOverlay, ICapabilityBlock {
    private static final double[] PLACEMENT_EXTRAS = {1.5, 3.5, -4.5, -4.5, 1, -1};

    @Override
    protected double[] placementExtraBoxes() {
        return PLACEMENT_EXTRAS;
    }

    private static final int[] DIMENSIONS = {3, 0, 4, 4, 1, 1};

    public MachineFusionBoiler(Properties props) {
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
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();

        visitor.cell(
                core.offset(
                        -facing.getStepX() + rot.getStepX(),
                        0,
                        -facing.getStepZ() + rot.getStepZ()),
                MASK_WEST);
        visitor.cell(
                core.offset(
                        -facing.getStepX() - rot.getStepX(),
                        0,
                        -facing.getStepZ() - rot.getStepZ()),
                MASK_EAST);
        visitor.cell(
                core.offset(
                        facing.getStepX() * 2 + rot.getStepX(),
                        0,
                        facing.getStepZ() * 2 + rot.getStepZ()),
                MASK_WEST);
        visitor.cell(
                core.offset(
                        facing.getStepX() * 2 - rot.getStepX(),
                        0,
                        facing.getStepZ() * 2 - rot.getStepZ()),
                MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        visitor.passiveCell(
                core.offset(
                        -facing.getStepX() + rot.getStepX(),
                        0,
                        -facing.getStepZ() + rot.getStepZ()),
                MASK_ALL,
                PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.offset(
                        -facing.getStepX() - rot.getStepX(),
                        0,
                        -facing.getStepZ() - rot.getStepZ()),
                MASK_ALL,
                PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.offset(
                        facing.getStepX() * 2 + rot.getStepX(),
                        0,
                        facing.getStepZ() * 2 + rot.getStepZ()),
                MASK_ALL,
                PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.offset(
                        facing.getStepX() * 2 - rot.getStepX(),
                        0,
                        facing.getStepZ() * 2 - rot.getStepZ()),
                MASK_ALL,
                PASSIVE_FLUID_IN);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityFusionBoiler boiler)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(
                ChatFormatting.GREEN
                        + "-> "
                        + ChatFormatting.RESET
                        + String.format(Locale.US, "%,d", boiler.plasmaEnergy)
                        + " TU");
        for (int i = 0; i < boiler.tanks.length; i++) {
            FluidTankNTM tank = boiler.tanks[i];
            info.line(
                    (i == 0 ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ")
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(tank.getFluid())
                            + ": "
                            + tank.getFill()
                            + "/"
                            + tank.getMaxFill()
                            + "mB");
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFusionBoiler(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionBoiler.links(core, facing);
    }
}
