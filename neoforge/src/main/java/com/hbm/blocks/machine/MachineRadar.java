// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.data.MachineData;
import com.hbm.inventory.IGUIProvider;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineRadar extends Block implements ITickingBlock, ICapabilityBlock {

    public MachineRadar(Properties props) {
        super(props);
    }

    static InteractionResult openAbove(Level level, BlockPos core, Player player) {
        if (core.getY() < MachineData.RADAR_ALTITUDE.get()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(
                        Component.translatable("desc.block.radar.errorRadarAltitudeNot")
                                .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(core) instanceof MenuProvider provider) {
            IGUIProvider.openBlockMenu(
                    player, provider, core, BlockEntityMachineRadar.DISPATCH_MAP);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineRadar(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openAbove(level, pos, player);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BlockEntityMachineRadar radar
                ? radar.getRedPower()
                : 0;
    }

    @Override
    protected int getDirectSignal(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RADAR).powerIn().fe();
    }
}
