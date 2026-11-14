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

package com.hbm.inventory.container;

import com.hbm.packet.ControlAuthority;
import com.hbm.registration.RegistryHandle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class HeldItemMenu extends AbstractContainerMenu {

    public final InteractionHand hand;
    private final ItemStack stack;

    public HeldItemMenu(
            MenuType<?> type, int containerId, Inventory inventory, InteractionHand hand) {
        super(type, containerId);
        this.hand = hand;
        this.stack = inventory.player.getItemInHand(hand);
    }

    public static void open(
            Player player,
            InteractionHand hand,
            RegistryHandle<MenuType<HeldItemMenu>> type,
            Component title) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        final MenuProvider provider =
                new SimpleMenuProvider(
                        (containerId, inventory, opener) ->
                                new HeldItemMenu(type.get(), containerId, inventory, hand),
                        title);
        final StreamCodec<? super RegistryFriendlyByteBuf, InteractionHand> codec =
                InteractionHand.STREAM_CODEC;
        serverPlayer.openMenu(provider, buf -> codec.encode(buf, hand));
    }

    public static @Nullable HeldItemMenu current(
            ServerPlayer player, int containerId, RegistryHandle<MenuType<HeldItemMenu>> type) {
        return ControlAuthority.openMenu(player, containerId) instanceof HeldItemMenu menu
                        && menu.getType() == type.get()
                ? menu
                : null;
    }

    public ItemStack stack() {
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isSpectator() && player.getItemInHand(hand) == stack;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
