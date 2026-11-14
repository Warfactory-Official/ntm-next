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

package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKDisplayBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RBMKDisplayBlock extends RBMKMiniPanelBlock
        implements TickingEntityBlock, ToolInteractionHandler {

    public RBMKDisplayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKDisplayBlockEntity(pos, state);
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) {
            return false;
        }
        if (!level.isClientSide()) {
            ((RBMKDisplayBlockEntity) level.getBlockEntity(pos)).rotate();
        }
        return true;
    }
}
