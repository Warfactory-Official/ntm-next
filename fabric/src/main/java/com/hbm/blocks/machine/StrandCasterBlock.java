/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.blockentity.machine.ContinuousCaster;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.MaterialTooltip;
import com.hbm.items.machine.MoldItem;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class StrandCasterBlock extends MultiblockCoreBlock
        implements TickingEntityBlock,
                CapabilityBlock,
                LookOverlayProvider,
                ToolInteractionHandler {
    public StrandCasterBlock(Properties props) {
        super(props);
    }

    private static @Nullable ContinuousCaster caster(Level level, BlockPos core) {
        return level.getBlockEntity(core) instanceof ContinuousCaster be ? be : null;
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
        if (player.isSpectator()) return InteractionResult.PASS;
        ContinuousCaster caster = caster(level, core);
        if (caster == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (held.getItem() instanceof MoldItem
                && caster.inventory.get(ContinuousCaster.SLOT_MOLD).isEmpty()) {
            if (!level.isClientSide()) {
                caster.inventory.set(ContinuousCaster.SLOT_MOLD, held.copyWithCount(1));
                held.consume(1, player);
                level.playSound(
                        null, core, ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                caster.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide() && caster.amount > 0 && caster.type != null) {
                ItemStack scrap = ScrapsItem.create(new MaterialStack(caster.type, caster.amount));
                caster.hbm$syncSet$FoundryBaseBlockEntity$amount(0);
                caster.hbm$syncSet$FoundryBaseBlockEntity$type(null);
                caster.setChanged();
                player.getInventory().placeItemBackInInventory(scrap);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (player.isSpectator()) return false;
        if (tool != ToolType.SCREWDRIVER) return false;

        ContinuousCaster caster = caster(level, pos);
        if (caster == null) return false;

        ItemStack mold = caster.inventory.get(ContinuousCaster.SLOT_MOLD);
        if (mold.isEmpty()) return false;

        if (!level.isClientSide()) {
            caster.inventory.set(ContinuousCaster.SLOT_MOLD, ItemStack.EMPTY);
            caster.setChanged();
            player.getInventory().placeItemBackInInventory(mold);
        }
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        ContinuousCaster caster = caster(level, pos);
        if (caster == null) return;

        info.title(
                Component.empty().append(Component.translatable(getDescriptionId())),
                0xFF4000,
                0x401000);

        if (caster.inventory.get(ContinuousCaster.SLOT_MOLD).getItem() instanceof MoldItem mold) {
            info.row(mold.mold.getTitle(), 0x5555FF);
        } else {
            info.row(Component.empty().append(Component.translatable("foundry.noCast")), 0xFF5555);
        }

        if (caster.type != null && caster.amount > 0) {
            info.row(
                    caster.type
                            .displayName()
                            .copy()
                            .append(": ")
                            .append(MaterialTooltip.of(caster.amount, false)),
                    0xFFFF55);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ContinuousCaster(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.STRAND_CASTER)
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells();
    }
}
