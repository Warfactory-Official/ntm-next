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

package com.hbm.blocks.machine.storage;

import com.hbm.blockentity.machine.storage.BatteryBlockEntity;
import com.hbm.blockentity.machine.storage.BatteryCharge;
import com.hbm.blockentity.machine.storage.FensuBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.items.ModDataComponents;
import com.hbm.util.NumberFormatter;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class FENSUBlock extends MultiblockCoreBlock
        implements TickingEntityBlock,
                CapabilityBlock,
                LookOverlayProvider,
                PersistentItemTooltipProvider {
    public static final MapCodec<FENSUBlock> CODEC = simpleCodec(FENSUBlock::new);

    public FENSUBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FENSU).powerIn().powerOut().items().fe();
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (level.getBlockEntity(core) instanceof BatteryBlockEntity battery) battery.refreshMode();
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        info.title(getName());
        if (level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            info.charge(battery.getPower(), battery.getMaxPower());
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FensuBlockEntity(pos, state);
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        BatteryCharge state = stack.get(ModDataComponents.MACHINE_BATTERY_STATE.get());
        if (state == null) return;
        adder.accept(
                Component.literal(
                                NumberFormatter.formatCompact(state.power())
                                        + "/"
                                        + NumberFormatter.formatCompact(Long.MAX_VALUE)
                                        + "HE")
                        .withStyle(ChatFormatting.YELLOW));
    }
}
