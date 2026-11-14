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

package com.hbm.items.block;

import com.hbm.blockentity.machine.LockStateData;
import com.hbm.blockentity.machine.storage.CrateBlockEntity;
import com.hbm.blockentity.machine.storage.CrateType;
import com.hbm.blockentity.machine.storage.LockedContents;
import com.hbm.blocks.machine.storage.CrateBlock;
import com.hbm.config.InteractionConfig;
import com.hbm.extprop.PlayerState;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.CrateMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.KeyItem;
import com.hbm.items.tool.KeyPinItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class StorageCrateBlockItem extends BlockItem {

    private final CrateType crateType;

    public StorageCrateBlockItem(CrateBlock block, Properties properties) {
        super(block, properties);
        this.crateType = block.crateType();
    }

    private static boolean opensHeld(Player player) {
        return player.level().isClientSide()
                ? InteractionConfig.crateOpensHeld
                : PlayerState.getOrCreate(player).crateOpenHeld;
    }

    private static boolean isLocked(ItemStack stack) {
        LockStateData lock = stack.get(ModDataComponents.LOCK_STATE.get());
        return lock != null && lock.isLocked();
    }

    private static boolean sealed(ItemStack stack) {
        return isLocked(stack) || stack.has(ModDataComponents.SPIDERS.get());
    }

    public static boolean canOpen(Player player, ItemStack stack) {
        if (stack.getCount() != 1) return false;
        if (!isLocked(stack)) return true;
        int pins = stack.get(ModDataComponents.LOCK_STATE.get()).lock();
        for (ItemStack key : player.getInventory().getNonEquipmentItems()) {
            if (key.getItem() instanceof KeyItem && KeyPinItem.getPins(key) == pins) return true;
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && opensHeld(player) && !player.isSecondaryUseActive())
            return InteractionResult.PASS;
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!opensHeld(player) || !canOpen(player, stack)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            ItemStackContainer.Store store = storage(server, stack);
            if (store == null) return InteractionResult.FAIL;
            if (stack.has(ModDataComponents.SPIDERS.get())) {
                CrateBlockEntity.spawnSpiders(
                        server, player.getX(), player.getY(), player.getZ(), player);
                stack.remove(ModDataComponents.SPIDERS.get());
            }
            stamp(stack, server);
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new CrateMenu(
                                            id,
                                            inv,
                                            crateType,
                                            new ItemStackContainer(
                                                    opener, stack, crateType.slots, store) {
                                                @Override
                                                public void stopOpen(ContainerUser user) {
                                                    super.stopOpen(user);
                                                    stack.remove(ModDataComponents.STACKLOCK.get());
                                                    if (!stack.getComponentsPatch().isEmpty())
                                                        stamp(stack, server);
                                                }
                                            }),
                            name != null ? name : Component.translatable(crateType.containerKey)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getLevel() instanceof ServerLevel server
                && storage(server, context.getItemInHand()) == null) {
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    public static ItemStackContainer.@Nullable Store storage(ServerLevel level, ItemStack stack) {
        LockedContents locked = stack.get(ModDataComponents.LOCKED_CONTENTS.get());
        if (locked != null && locked.open(level) == null) return null;
        return new ItemStackContainer.Store() {
            @Override
            public ItemContainerContents read(ItemStack target) {
                LockedContents current = target.get(ModDataComponents.LOCKED_CONTENTS.get());
                return current == null
                        ? target.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        : current.open(level);
            }

            @Override
            public void write(ItemStack target, ItemContainerContents contents) {
                target.remove(ModDataComponents.LOCKED_CONTENTS.get());
                target.remove(DataComponents.CONTAINER);
                stamp(target, level);
                if (contents == ItemContainerContents.EMPTY) return;
                if (sealed(target)) {
                    target.set(
                            ModDataComponents.LOCKED_CONTENTS.get(),
                            LockedContents.seal(level, contents));
                } else {
                    target.set(DataComponents.CONTAINER, contents);
                }
            }
        };
    }

    private static void stamp(ItemStack stack, ServerLevel level) {
        stack.set(ModDataComponents.STACKLOCK.get(), level.getRandom().nextLong());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        if (stack.has(ModDataComponents.SPIDERS.get())) {
            if (isLocked(stack)) {
                adder.accept(
                        Component.translatable("desc.item.storageCrate.thisContainerIsLocked")
                                .withStyle(ChatFormatting.RED));
            }
            adder.accept(
                    Component.translatable("desc.item.storageCrate.skitteringEmanatesFrom")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        if (!isLocked(stack)) return;
        LockedContents locked = stack.get(ModDataComponents.LOCKED_CONTENTS.get());
        adder.accept(
                Component.translatable("desc.item.storageCrate.thisContainerIsLocked")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable(
                                locked != null && locked.heavy()
                                        ? "desc.item.storageCrate.itFeelsHeavy"
                                        : "desc.item.storageCrate.itFeelsEmpty")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
