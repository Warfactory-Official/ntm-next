// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.BlockEntityCondenserPowered;
import com.hbm.util.BobMathUtil;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineCondenserPowered extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 1, 1, 3, 3};

    public MachineCondenserPowered(Properties props) {
        super(props);
    }

    private static void line(
            ILookOverlay.LookInfo info, String prefix, FluidTankNTM tank, int color) {
        info.line(
                prefix
                        + NTMFluidProperties.clientName(tank.getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", tank.getFill())
                        + " / "
                        + String.format(Locale.US, "%,d", tank.getMaxFill())
                        + " mB",
                color);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(rx * 3, 1, rz * 3), MASK_WEST);
        visitor.cell(core.offset(-rx * 3, 1, -rz * 3), MASK_EAST);
        visitor.cell(core.offset(dx + rx, 1, dz + rz), MASK_SOUTH);
        visitor.cell(core.offset(dx - rx, 1, dz - rz), MASK_SOUTH);
        visitor.cell(core.offset(-dx + rx, 1, -dz + rz), MASK_NORTH);
        visitor.cell(core.offset(-dx - rx, 1, -dz - rz), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        visitor.passiveCell(core.offset(rx * 3, 1, rz * 3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rx * 3, 1, -rz * 3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx + rx, 1, dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx - rx, 1, dz - rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dx + rx, 1, -dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dx - rx, 1, -dz - rz), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCondenserPowered(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCondenserPowered be)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(
                BobMathUtil.getShortNumber(be.power)
                        + "HE / "
                        + BobMathUtil.getShortNumber(be.getMaxPower())
                        + "HE",
                0xFFFF00);
        line(info, "-> ", be.input, 0x00FF00);
        line(info, "<- ", be.output, 0xFF0000);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().fluidOut().fe();
    }
}
