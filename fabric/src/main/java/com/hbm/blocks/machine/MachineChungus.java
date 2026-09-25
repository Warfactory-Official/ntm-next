// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.tileentity.machine.BlockEntityChungus;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineChungus extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        3, 0, 0, 3, 2, 2, 0, 0, 0,
        4, -4, 0, 3, 1, 1, 0, 0, 0,
        3, 0, 6, -1, 1, 1, 0, 0, 0,
        2, 0, 10, -7, 1, 1, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {3, 0, 0, 3, 2, 2};

    private static final int[] ARM_A = {4, -4, 0, 3, 1, 1};
    private static final int[] ARM_B = {3, 0, 6, -1, 1, 1};
    private static final int[] ARM_C = {2, 0, 10, -7, 1, 1};

    public MachineChungus(Properties props) {
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
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        if (!MultiblockHandlerXR.checkSpace(level, origin, ARM_B, placed, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, origin, ARM_C, placed, dir)) return false;
        BlockPos top = placed.offset(dir.getStepX(), 2, dir.getStepZ());
        return level.getBlockState(top).canBeReplaced();
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, ARM_A, facing, visitor);
        MultiblockHandlerXR.visitBox(core, ARM_B, facing, visitor);
        MultiblockHandlerXR.visitBox(core, ARM_C, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(rot, 2), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.relative(rot, -2), MASK_EAST, ROLE_FLUID);
        visitor.cell(core.relative(facing, -10), MASK_NORTH, ROLE_POWER);

        visitor.cell(core.relative(facing, 4).above(2), MASK_SOUTH, ROLE_FLUID);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        visitor.passiveCell(core.relative(facing, 4).above(2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -10), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -2), MASK_ALL, domains);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(core) instanceof BlockEntityChungus turbine))
            return InteractionResult.PASS;

        Direction dir = coreState.getValue(FACING);
        Direction turn = dir.getCounterClockWise();
        int iX = core.getX() + dir.getStepX() + turn.getStepX() * 2;
        int iX2 = core.getX() + dir.getStepX() * 2 + turn.getStepX() * 2;
        int iZ = core.getZ() + dir.getStepZ() + turn.getStepZ() * 2;
        int iZ2 = core.getZ() + dir.getStepZ() * 2 + turn.getStepZ() * 2;

        if ((hit.getBlockPos().getX() == iX || hit.getBlockPos().getX() == iX2)
                && (hit.getBlockPos().getZ() == iZ || hit.getBlockPos().getZ() == iZ2)
                && hit.getBlockPos().getY() < core.getY() + 2) {
            return MachineIndustrialTurbine.pullLever(level, hit.getBlockPos(), player, turbine);
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityChungus(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        var owner = BlockMultiblockCore.ownerOf(level, pos);
        BlockPos core = owner != null ? owner.pos() : null;
        if (core == null) return;
        if (!(level.getBlockEntity(core) instanceof BlockEntityChungus chungus)) return;

        FluidTankNTM in = chungus.tanks[0];
        FluidTankNTM out = chungus.tanks[1];
        Fluid inType = in.getTankType();
        Fluid outType = NTMFluids.NONE;
        if (inType != null && NTMFluidProperties.hasTrait(inType, FT_Coolable.class)) {
            outType = NTMFluidProperties.getTrait(inType, FT_Coolable.class).coolsTo();
        }

        info.title(I18nUtil.resolveKey(getDescriptionId()), 0xFFFF00, 0x404000);
        info.line(
                ChatFormatting.GREEN
                        + "-> "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(in.getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", in.getFill())
                        + "/"
                        + String.format(Locale.US, "%,d", in.getMaxFill())
                        + "mB");
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(outType)
                        + ": "
                        + String.format(Locale.US, "%,d", out.getFill())
                        + "/"
                        + String.format(Locale.US, "%,d", out.getMaxFill())
                        + "mB");
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + BobMathUtil.getShortNumber(chungus.powerBuffer)
                        + "HE");
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .powerOut()
                .fluidIn()
                .fluidOut()
                .fe()
                .faces(BlockEntityChungus.class, (be, side) -> side.getAxis().isHorizontal());
    }
}
