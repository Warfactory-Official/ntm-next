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
import com.hbm.tileentity.machine.BlockEntityDeuteriumExtractor;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineDeuteriumExtractor extends Block
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public MachineDeuteriumExtractor(Properties props) {
        super(props);
    }

    static void buildOverlay(BlockEntityDeuteriumExtractor be, Block block, LookInfo info) {

        info.title(
                Component.translatable(block.getDescriptionId()).getString(), 0xFFFF00, 0x404000);
        boolean starved = be.power < be.getMaxPower() / 20;
        info.line(
                "Power: " + BobMathUtil.getShortNumber(be.power) + "HE",
                starved ? 0xFF5555 : 0x55FF55);
        for (int i = 0; i < be.tanks.length; i++) {
            FluidTankNTM tank = be.tanks[i];

            String arrow = i < 1 ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ";
            info.line(
                    arrow
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
        return new BlockEntityDeuteriumExtractor(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (level.getBlockEntity(pos) instanceof BlockEntityDeuteriumExtractor be) {
            buildOverlay(be, this, info);
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.DEUTERIUM_EXTRACTOR)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .fe();
    }
}
