// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.machine.BlockEntityMachineIntake;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineIntake extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {0, 0, 1, 0, 1, 0};

    public MachineIntake(Properties props) {
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

        Direction rot = facing.getClockWise();
        BlockPos back = core.relative(facing.getOpposite());
        visitor.cell(back, MASK_NORTH | MASK_EAST);
        visitor.cell(core.relative(rot), MASK_SOUTH | MASK_WEST);
        visitor.cell(back.relative(rot), MASK_NORTH | MASK_WEST);
    }

    @Override
    public int coreMask() {
        return MASK_EAST | MASK_SOUTH;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineIntake intake)) return;

        info.title(getName().getString(), 0xFFFF00, 0x404000);
        long tickCost = BlockEntityMachineIntake.MAX_POWER / SharedConstants.TICKS_PER_SECOND;
        info.line(
                "Power: " + BobMathUtil.getShortNumber(intake.power) + "HE",
                intake.power < tickCost ? 0xFF5555 : 0x55FF55);
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(intake.compair.getFluid())
                        + ": "
                        + intake.compair.getFill()
                        + "/"
                        + intake.compair.getMaxFill()
                        + "mB");
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineIntake(pos, state);
    }

    @Override
    public MachineCaps caps() {

        return MachineCaps.blockKeyed()
                .powerIn()
                .fluidOut()
                .fe()
                .faces(
                        BlockEntityMachineIntake.class,
                        (be, side) -> side == null || side.getAxis() != Direction.Axis.Y)
                .fluidFaces(
                        BlockEntityMachineIntake.class,
                        (be, face) ->
                                (face.fluid() == null || face.fluid() == NTMFluids.AIR)
                                        && (face.side() == null
                                                || face.side().getAxis() != Direction.Axis.Y));
    }
}
