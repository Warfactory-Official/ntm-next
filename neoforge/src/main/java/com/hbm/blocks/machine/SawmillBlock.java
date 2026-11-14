/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.thermal.TimberSawBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.util.Blink;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SawmillBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, LookOverlayProvider {

    private static final Component[] PROGRESS_BARS = progressBars();

    private static Component[] progressBars() {
        var bars = new Component[26];
        for (int filled = 0; filled <= 25; filled++) {
            var bar = Component.literal("[ ").withStyle(ChatFormatting.GREEN);
            if (filled > 0) {
                bar.append(Component.literal("▏".repeat(filled)).withStyle(ChatFormatting.GREEN));
            }
            if (filled < 25) {
                bar.append(
                        Component.literal("▏".repeat(25 - filled)).withStyle(ChatFormatting.WHITE));
            }
            bar.append(Component.literal(" ]").withStyle(ChatFormatting.GREEN));
            bars[filled] = bar;
        }
        return bars;
    }

    private static Component progressBar(int limiter) {
        if (limiter >= 0 && limiter < 25) {
            return PROGRESS_BARS[limiter];
        }
        return PROGRESS_BARS[25];
    }

    public SawmillBlock(Properties properties) {
        super(properties);
    }

    @Override
    public TimberSawBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimberSawBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return interact(level, core, player, ItemStack.EMPTY)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        return interact(level, core, player, held)
                ? InteractionResult.SUCCESS
                : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private boolean interact(Level level, BlockPos core, Player player, ItemStack held) {
        if (level.isClientSide()) return true;
        var saw = (TimberSawBlockEntity) level.getBlockEntity(core);

        if (!saw.hasBlade && held.is(ModItems.SAWBLADE.get())) {
            held.shrink(1);
            saw.hbm$syncSet$TimberSawBlockEntity$hasBlade(true);
            saw.setChanged();
            level.playSound(
                    null, core, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.5F, 0.75F);
            return true;
        }

        if (!saw.getItem(1).isEmpty() || !saw.getItem(2).isEmpty()) {
            for (int slot = 1; slot <= 2; slot++) {
                var output = saw.removeItemNoUpdate(slot);
                if (!output.isEmpty())
                    player.getInventory().placeItemBackInInventory(output, Prediction.SERVER_ONLY);
            }
            saw.setChanged();
            return true;
        }

        if (saw.getItem(0).isEmpty() && !held.isEmpty() && saw.getOutput(held) != null) {
            saw.setItem(0, held.copyWithCount(1));
            held.shrink(1);
            return true;
        }
        return false;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        var saw = (TimberSawBlockEntity) level.getBlockEntity(pos);
        int limiter = saw.progress * 26 / TimberSawBlockEntity.PROCESSING_TIME;
        double percent = (saw.heat * 1000 / 300) / 10D;

        info.title(getName());
        info.row(Component.translatable("unit.heat_per_tick", saw.heat));
        double heatFraction = saw.heat / 300D;
        int color =
                heatFraction > 1D
                        ? 0xFF0000
                        : ((int) (0xFF - 0xFF * heatFraction) << 16)
                                | ((int) (0xFF * heatFraction) << 8);
        info.row(Component.translatable("unit.percent", percent), color);
        info.row(progressBar(limiter));

        for (int slot = 0; slot < 3; slot++) {
            var stack = saw.getItem(slot);
            if (stack.isEmpty()) continue;

            Component label = stack.getHoverName();
            if (stack.getCount() > 1) {
                label = Component.translatable("overlay.hbm.item_count", label, stack.getCount());
            }
            if (slot == 0) {
                info.row(LookOverlay.inflow(label));
            } else {
                info.row(LookOverlay.outflow(label));
            }
        }

        if (saw.heat > 300) {
            int blinkColor = Blink.lit() ? 0xFF0000 : 0xFFFF00;
            info.row(Component.translatable("overlay.hbm.overspeed"), blinkColor);
        }
        if (!saw.hasBlade) {
            info.row(
                    Component.translatable("overlay.hbm.blade_missing")
                            .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.SAWMILL).items().itemsAtCells();
    }

    @java.lang.Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @java.lang.Override
    public void cellNeighborChanged(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos core,
            net.minecraft.core.BlockPos cell) {
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(core);
        if (hbm$be instanceof TimberSawBlockEntity)
            ((TimberSawBlockEntity) hbm$be).refreshHeatBelow();
    }
}
