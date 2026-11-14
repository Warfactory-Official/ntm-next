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

import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class BlockMenuType<
                BE extends BlockEntity & MenuProvider, M extends AbstractContainerMenu>
        extends BlockDataMenuType<BE, M, Unit> {

    @FunctionalInterface
    public interface Factory<BE, M> {
        @Nullable M create(int containerId, Inventory inventory, BE blockEntity);
    }

    public BlockMenuType(Binding<BE, M, Unit> binding) {
        super(binding);
    }

    public void open(Player player, BE blockEntity) {
        open(player, blockEntity, Unit.INSTANCE);
    }
}
