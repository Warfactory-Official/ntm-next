// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityChungus;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.util.BobMathUtil;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
    public MachineChungus(Properties props) {
        super(props);
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

        info.title(
                Component.empty().append(Component.translatable(getDescriptionId())),
                0xFFFF00,
                0x404000);
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
