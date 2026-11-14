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

package com.hbm.blocks.network;

import com.hbm.blockentity.network.DroneDockBlockEntity;
import com.hbm.blockentity.network.DroneProviderBlockEntity;
import com.hbm.blockentity.network.DroneRequesterBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.BlockMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class DroneDockBlock extends Block implements TickingEntityBlock, CapabilityBlock {
    private final Kind kind;

    public DroneDockBlock(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case DOCK -> new DroneDockBlockEntity(pos, state);
            case PROVIDER -> new DroneProviderBlockEntity(pos, state);
            case REQUESTER -> new DroneRequesterBlockEntity(pos, state);
        };
    }

    @Override
    public MachineCaps caps() {
        return switch (kind) {
            case DOCK -> MachineCaps.of(ModBlockEntities.DRONE_DOCK);
            case PROVIDER -> MachineCaps.of(ModBlockEntities.DRONE_PROVIDER).items();
            case REQUESTER -> MachineCaps.of(ModBlockEntities.DRONE_REQUESTER).items();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider))
            return InteractionResult.PASS;
        if (!level.isClientSide()) BlockMenus.open(player, provider);
        return InteractionResult.SUCCESS;
    }

    public enum Kind {
        DOCK,
        PROVIDER,
        REQUESTER
    }
}
