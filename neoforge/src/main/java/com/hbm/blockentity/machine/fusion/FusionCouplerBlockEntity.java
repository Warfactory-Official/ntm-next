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

package com.hbm.blockentity.machine.fusion;

import com.hbm.blockentity.GraphResident;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.platform.BlockLookupCache;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class FusionCouplerBlockEntity extends BlockEntity
        implements GraphResident, PlasmaReceiver {
    private @Nullable Direction cachedFacing;
    private @Nullable BlockLookupCache<BlockEntity> ignitionPeer;

    public FusionCouplerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COUPLER.get(), pos, state);
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        var right = facing.getOpposite().getClockWise();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.KLYSTRON, core.relative(right).above(2), right),
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA,
                        core.relative(right.getOpposite()).above(2),
                        right.getOpposite()));
    }

    @Override
    public boolean isPlasmaConsumer() {
        return true;
    }

    @Override
    public void receivePlasma(long power, double neutronPower, float red, float green, float blue) {
        var facing = MultiblockCoreBlock.coreFacing(getBlockState());
        if (facing != cachedFacing) {
            ignitionPeer =
                    FusionPorts.cache((ServerLevel) level, links(worldPosition, facing).getFirst());
            cachedFacing = facing;
        }
        FusionKlystronBlockEntity.provideKyU(ignitionPeer, power);
    }
}
