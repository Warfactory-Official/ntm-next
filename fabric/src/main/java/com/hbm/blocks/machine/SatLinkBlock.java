/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.SatelliteLinkerBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class SatLinkBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider {
    public SatLinkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isSpectator()
                || player.isSecondaryUseActive()
                || !(held.getItem() instanceof SatelliteFrequencyProvider))
            return InteractionResult.PASS;
        if (!level.isClientSide()) {
            var link = (SatelliteLinkerBlockEntity) level.getBlockEntity(core);
            link.setFrequency(SatelliteFrequencyProvider.frequency(held));
            player.sendSystemMessage(
                    Component.translatable("chat.machineSatLink.frequencySet", link.frequency())
                            .withStyle(ChatFormatting.YELLOW));
            EntitySounds.play(
                    player,
                    player.blockPosition(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SatelliteLinkerBlockEntity(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        var link = (SatelliteLinkerBlockEntity) level.getBlockEntity(pos);
        info.title(getName());
        info.row(
                Component.empty()
                        .append(
                                Component.translatable(
                                        "desc.block.machineSatLink.frequency", link.frequency())));
        String answer =
                (link.connected() ? ChatFormatting.GREEN : ChatFormatting.RED)
                        + Component.translatable(
                                        link.connected()
                                                ? "tile.machine_satlink.yes"
                                                : "tile.machine_satlink.no")
                                .getString();
        info.row(
                Component.empty()
                        .append(
                                Component.translatable(
                                        "desc.block.machineSatLink.connected", answer)));
        for (var line : link.readout()) info.row(line);
    }
}
