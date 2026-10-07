// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.api.environment.Environment;
import com.hbm.blockentity.machine.BlockEntityCondenser;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BobMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineCondenser extends Block
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public MachineCondenser(Properties props) {
        super(props);
    }

    private static void line(
            ILookOverlay.LookInfo info, String prefix, FluidTankNTM tank, int color) {
        info.line(
                Component.literal(prefix)
                        .append(NTMFluidProperties.getDisplayName(tank.getFluid()))
                        .append(": ")
                        .append(
                                Component.translatable(
                                        "unit.millibuckets",
                                        tank.getFill() + "/" + tank.getMaxFill())),
                color);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCondenser(pos, state);
    }

    static void vacuumWarning(Level level, ILookOverlay.LookInfo info) {
        if (!Environment.vacuum(level)) return;
        info.line(
                Component.literal("! ! ! ")
                        .append(Component.translatable("atmosphere.noVacuum"))
                        .append(" ! ! !"),
                BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCondenser be)) return;
        info.title(getName(), 0xFFFF00, 0x404000);
        vacuumWarning(level, info);
        line(info, "-> ", be.input, 0x00FF00);
        line(info, "<- ", be.output, 0xFF0000);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CONDENSER).fluidIn().fluidOut();
    }
}
