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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.machine.FloodlightBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FloodlightBeamBlockEntity extends BlockEntity implements GraphResident {

    public static final int RECHECK_TICKS = 100;

    private int sourceX;
    private int sourceY;
    private int sourceZ;
    private int index;
    private boolean hasSource;

    public FloodlightBeamBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOODLIGHT_BEAM.get(), pos, state);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        if (ChunkPublication.publishing(server, worldPosition)) {
            check(server);
            return;
        }
        MinecraftServer host = server.getServer();
        host.schedule(host.wrapRunnable(() -> check(server)));
    }

    public void check(ServerLevel level) {
        if (isRemoved()) return;
        if (hasSource) {
            BlockPos source = new BlockPos(sourceX, sourceY, sourceZ);
            LevelChunk chunk = level.getChunkSource().getChunkNow(sourceX >> 4, sourceZ >> 4);
            if (chunk == null) {
                level.scheduleTick(worldPosition, getBlockState().getBlock(), RECHECK_TICKS);
                return;
            }
            if (chunk.getBlockEntity(source) instanceof FloodlightBlockEntity floodlight
                    && floodlight.isOn
                    && floodlight.ownsBeam(worldPosition, index)) {
                return;
            }
        }
        level.removeBlock(worldPosition, false);
    }

    public void setSource(FloodlightBlockEntity floodlight, int index) {
        BlockPos source = floodlight.getBlockPos();
        sourceX = source.getX();
        sourceY = source.getY();
        sourceZ = source.getZ();
        this.index = index;
        hasSource = true;
        setChanged();
    }

    public boolean isFrom(BlockPos floodlight) {
        return hasSource
                && sourceX == floodlight.getX()
                && sourceY == floodlight.getY()
                && sourceZ == floodlight.getZ();
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        sourceX = in.getIntOr("sourceX", 0);
        sourceY = in.getIntOr("sourceY", 0);
        sourceZ = in.getIntOr("sourceZ", 0);
        index = in.getIntOr("index", 0);
        hasSource = in.getBooleanOr("hasSource", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("sourceX", sourceX);
        out.putInt("sourceY", sourceY);
        out.putInt("sourceZ", sourceZ);
        out.putInt("index", index);
        out.putBoolean("hasSource", hasSource);
    }
}
