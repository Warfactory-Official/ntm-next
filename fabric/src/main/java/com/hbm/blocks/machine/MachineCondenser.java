// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.BlockEntityCondenser;
import net.minecraft.core.BlockPos;
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
                prefix
                        + NTMFluidProperties.clientName(tank.getFluid())
                        + ": "
                        + tank.getFill()
                        + "/"
                        + tank.getMaxFill()
                        + "mB",
                color);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCondenser(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCondenser be)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        line(info, "-> ", be.input, 0x00FF00);
        line(info, "<- ", be.output, 0xFF0000);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CONDENSER).fluidIn().fluidOut();
    }
}
