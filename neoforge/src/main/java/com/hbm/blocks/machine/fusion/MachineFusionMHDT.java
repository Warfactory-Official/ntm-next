// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.data.MachineData;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionMHDT;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineFusionMHDT extends BlockFusionMachine
        implements ILookOverlay, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        2, 0, 6, 7, 2, 2, 0, 0, 0,
        3, -2, 6, 2, 1, 1, 0, 0, 0,
        3, -2, -6, 7, 1, 1, 0, 0, 0,
        3, -2, -3, 5, 2, 2, 0, 0, 0,
        4, -3, -3, 5, 1, 1, 0, 0, 0,
        1, 0, 0, 1, 3, 3, 3, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final double[] PLACEMENT_EXTRAS = {1.5, 3.5, -6.5, -6.5, 1, -1};

    @Override
    protected double[] placementExtraBoxes() {
        return PLACEMENT_EXTRAS;
    }

    private static final int[] DIMENSIONS = {2, 0, 6, 7, 2, 2};

    private static final int[][] EXTRA_BOXES = {
        {3, -2, 6, 2, 1, 1},
        {3, -2, -6, 7, 1, 1},
        {3, -2, -3, 5, 2, 2},
        {4, -3, -3, 5, 1, 1}
    };
    private static final int[] NOZZLE_BOX = {1, 0, 0, 1, 3, 3};

    public MachineFusionMHDT(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 7;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        for (int[] box : EXTRA_BOXES) {
            if (!MultiblockHandlerXR.checkSpace(level, origin, box, placed, dir)) return false;
        }
        BlockPos nozzle = placed.offset(dir.getStepX() * (o + 3), 0, dir.getStepZ() * (o + 3));
        return MultiblockHandlerXR.checkSpace(level, nozzle, NOZZLE_BOX, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] box : EXTRA_BOXES) MultiblockHandlerXR.visitBox(core, box, facing, visitor);
        MultiblockHandlerXR.visitBox(
                core.offset(facing.getStepX() * 3, 0, facing.getStepZ() * 3),
                NOZZLE_BOX,
                facing,
                visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(
                core.offset(
                        facing.getStepX() * 4 + rot.getStepX() * 3,
                        0,
                        facing.getStepZ() * 4 + rot.getStepZ() * 3),
                MASK_WEST);
        visitor.cell(
                core.offset(
                        facing.getStepX() * 4 - rot.getStepX() * 3,
                        0,
                        facing.getStepZ() * 4 - rot.getStepZ() * 3),
                MASK_EAST);
        visitor.cell(core.offset(facing.getStepX() * 7, 1, facing.getStepZ() * 7), MASK_SOUTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(
                core.offset(
                        facing.getStepX() * 4 + rot.getStepX() * 3,
                        0,
                        facing.getStepZ() * 4 + rot.getStepZ() * 3),
                MASK_ALL,
                domains);
        visitor.passiveCell(
                core.offset(
                        facing.getStepX() * 4 - rot.getStepX() * 3,
                        0,
                        facing.getStepZ() * 4 - rot.getStepZ() * 3),
                MASK_ALL,
                domains);
        visitor.passiveCell(
                core.offset(facing.getStepX() * 7, 1, facing.getStepZ() * 7), MASK_ALL, domains);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityFusionMHDT turbine)) return;

        boolean hasPlasma = turbine.hasMinimumPlasma();
        boolean isCool = turbine.isCool();
        long power =
                (long) Math.floor(turbine.plasmaEnergy * BlockEntityFusionMHDT.PLASMA_EFFICIENCY);
        if (!hasPlasma) power /= 2;

        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(
                ChatFormatting.GREEN
                        + "-> "
                        + (hasPlasma ? ChatFormatting.RESET : ChatFormatting.GOLD)
                        + BobMathUtil.getShortNumber(turbine.plasmaEnergy)
                        + "TU/t / "
                        + BobMathUtil.getShortNumber(MachineData.MHD_TURBINE_MINIMUM_PLASMA.get())
                        + "TU/t");
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + BobMathUtil.getShortNumber(isCool ? power : 0)
                        + "HE/t");

        for (int i = 0; i < turbine.tanks.length; i++) {
            FluidTankNTM tank = turbine.tanks[i];
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

        if (turbine.plasmaEnergy > 0 && !hasPlasma) {
            info.line("! LOW POWER !", BobMathUtil.getBlink() ? 0xFF8000 : 0xFFFF00);
        }
        if (!isCool)
            info.line(
                    "! ! ! INSUFFICIENT COOLING ! ! !",
                    BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFusionMHDT(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerOut().fluidIn().fluidOut().fe();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionMHDT.links(core, facing);
    }
}
