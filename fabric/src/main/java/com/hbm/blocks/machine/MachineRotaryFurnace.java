// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineRotaryFurnace;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class MachineRotaryFurnace extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {
    public MachineRotaryFurnace(Properties props) {
        super(props);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockPos hit = info.getHitPos();
        if (hit == null) return;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineRotaryFurnace furnace)) return;

        Direction dir = furnace.coreFacing();
        Direction rot = dir.getCounterClockWise();
        BlockPos front = pos.relative(dir);
        BlockPos back = pos.relative(dir.getOpposite());

        if (hit.equals(back.relative(rot, -1)) || hit.equals(back.relative(rot, -2))) {
            info.line(
                    ChatFormatting.GREEN
                            + "-> "
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(furnace.tanks[1].getTankType()));
            info.line(
                    ChatFormatting.RED
                            + "<- "
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(furnace.tanks[2].getTankType()));
        }

        if (hit.equals(front.relative(rot, 2)) || hit.equals(back.relative(rot, 2))) {
            Fluid recipe = furnace.tanks[0].getTankType();
            info.line(
                    ChatFormatting.GREEN
                            + "-> "
                            + ChatFormatting.RESET
                            + (recipe == null ? "None" : NTMFluidProperties.clientName(recipe)));
        }

        if (hit.equals(front.relative(rot))) {
            info.line(ChatFormatting.YELLOW + "-> " + ChatFormatting.RESET + "Fuel");
        }

        if (!info.isEmpty()) {
            info.title(
                    Component.empty().append(Component.translatable(getDescriptionId())),
                    0xffff00,
                    0x404000);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineRotaryFurnace(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ROTARY_FURNACE)
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells();
    }
}
