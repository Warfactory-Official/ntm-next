// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.machine.fusion.BlockEntityFusionMHDT;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.data.MachineData;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
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
    public MachineFusionMHDT(Properties props) {
        super(props);
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
