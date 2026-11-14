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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.ZirnoxDestroyedBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RbmkFlameLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class DestroyedZirnoxBlock extends MultiblockCoreBlock implements TickingEntityBlock {
    private static final int FLAME_CHANCE = 4;
    private static final int FLAME_MAX_AGE = 90;
    private static final int FLAME_RANGE = 75;

    public DestroyedZirnoxBlock(Properties props) {
        super(props);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel server) || state.is(oldState.getBlock())) return;

        RandomSource rand = server.getRandom();
        if (rand.nextInt(FLAME_CHANCE) != 0) return;

        flame(server, pos);
        server.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS,
                1.0F + rand.nextFloat(),
                rand.nextFloat() * 0.7F + 0.3F);
    }

    public static void flame(ServerLevel level, BlockPos pos) {
        RandomSource rand = level.getRandom();
        double px = pos.getX() + 0.25D + rand.nextDouble() * 0.5D;
        double py = pos.getY() + 1.75D;
        double pz = pos.getZ() + 0.25D + rand.nextDouble() * 0.5D;
        PacketWire.sendToAllAround(
                RbmkFlameLayout.encode(px, py, pz, FLAME_MAX_AGE),
                new TargetPoint(level, pos.getX() + 0.5D, py, pos.getZ() + 0.5D, FLAME_RANGE));
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZirnoxDestroyedBlockEntity(pos, state);
    }
}
