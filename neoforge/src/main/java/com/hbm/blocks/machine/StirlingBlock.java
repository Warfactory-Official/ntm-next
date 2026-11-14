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

import com.hbm.blockentity.machine.thermal.StirlingGeneratorBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.util.Blink;
import com.hbm.util.NumberFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class StirlingBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, LookOverlayProvider {

    private final Tier tier;

    public StirlingBlock(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerOut().fe();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingGeneratorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
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
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(core) instanceof StirlingGeneratorBlockEntity machine)) {
            return InteractionResult.PASS;
        }

        var gear = ModItems.GEAR_LARGE.typeOf(held);
        if (machine.hasCog || gear == null || gear.ordinal() != machine.gearVariant())
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        machine.hbm$syncSet$StirlingGeneratorBlockEntity$hasCog(true);
        machine.setChanged();
        held.consume(1, player);
        level.playSound(null, core, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.5F, 0.75F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay card) {
        if (!(level.getBlockEntity(pos) instanceof StirlingGeneratorBlockEntity machine)) return;

        long power = 0;
        if (machine.hasCog) power = machine.powerBuffer;
        card.title(getName());
        card.row(
                Component.translatable(
                        "unit.heat_per_tick", NumberFormatter.formatGrouped(machine.heat)));
        card.row(
                Component.translatable(
                        "unit.energy_per_tick", NumberFormatter.formatCompact(power)));
        if (tier.creative) return;

        int maxHeat = tier.maxHeat;
        double fraction = (double) machine.heat / maxHeat;
        int heatColor = 0xFF0000;
        if (fraction <= 1) {
            int red = (int) (0xFF - 0xFF * fraction);
            int green = (int) (0xFF * fraction);
            heatColor = red << 16 | green << 8;
        }
        double percent = machine.heat * 1000 / maxHeat / 10D;
        card.row(Component.translatable("unit.percent", percent), heatColor);

        if (machine.heat > maxHeat) {
            var warningColor = ChatFormatting.YELLOW;
            if (Blink.lit()) warningColor = ChatFormatting.RED;
            card.row(Component.translatable("overlay.hbm.overspeed").withStyle(warningColor));
        }
        if (!machine.hasCog) {
            card.row(
                    Component.translatable("overlay.hbm.gear_missing")
                            .withStyle(ChatFormatting.RED));
        }
    }

    public enum Tier {
        NORMAL(0, 300, false),
        STEEL(1, 1500, false),
        CREATIVE(2, 1500, true);

        public final int gear;
        public final int maxHeat;
        public final boolean creative;

        Tier(int gear, int maxHeat, boolean creative) {
            this.gear = gear;
            this.maxHeat = maxHeat;
            this.creative = creative;
        }
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
        if (hbm$be instanceof StirlingGeneratorBlockEntity)
            ((StirlingGeneratorBlockEntity) hbm$be).refreshHeatBelow();
    }
}
