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

package com.hbm.inventory;

import com.hbm.registration.RegistryHandle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public sealed class BlockDataMenuType<
                BE extends BlockEntity & MenuProvider, M extends AbstractContainerMenu, D>
        implements RegistryHandle<MenuType<M>> permits BlockMenuType {

    @FunctionalInterface
    public interface Factory<BE, M, D> {
        @Nullable M create(int containerId, Inventory inventory, BE blockEntity, D data);
    }

    public record Binding<BE, M extends AbstractContainerMenu, D>(
            RegistryHandle<MenuType<M>> type,
            StreamCodec<RegistryFriendlyByteBuf, BlockMenuContext<D>> codec,
            Factory<BE, M, D> factory) {}

    private final Binding<BE, M, D> binding;

    public BlockDataMenuType(Binding<BE, M, D> binding) {
        this.binding = binding;
    }

    @Override
    public Identifier id() {
        return binding.type().id();
    }

    @Override
    public MenuType<M> get() {
        return binding.type().get();
    }

    public void open(Player player, BE blockEntity, D data) {
        open(player, blockEntity, data, blockEntity.getDisplayName());
    }

    public void open(Player player, BE blockEntity, D data, Component title) {
        if (!(player instanceof ServerPlayer server) || !BlockMenus.unpackLoot(server, blockEntity))
            return;
        final MenuProvider provider =
                new SimpleMenuProvider(
                        (containerId, inventory, opener) ->
                                binding.factory().create(containerId, inventory, blockEntity, data),
                        title);
        final BlockMenuContext<D> data2 = new BlockMenuContext<>(blockEntity.getBlockPos(), data);
        final StreamCodec<? super RegistryFriendlyByteBuf, BlockMenuContext<D>> codec =
                binding.codec();
        server.openMenu(provider, buf -> codec.encode(buf, data2));
    }
}
