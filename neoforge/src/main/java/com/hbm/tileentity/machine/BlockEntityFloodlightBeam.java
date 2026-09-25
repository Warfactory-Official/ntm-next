// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.tileentity.GraphResident;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityFloodlightBeam extends BlockEntity implements GraphResident {

    public static final int RECHECK_TICKS = 100;

    private int sourceX;
    private int sourceY;
    private int sourceZ;
    private int index;
    private boolean hasSource;

    public BlockEntityFloodlightBeam(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOODLIGHT_BEAM.get(), pos, state);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
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
            if (chunk.getBlockEntity(source) instanceof BlockEntityFloodlight floodlight
                    && floodlight.isOn
                    && floodlight.ownsBeam(worldPosition, index)) {
                return;
            }
        }
        level.removeBlock(worldPosition, false);
    }

    public void setSource(BlockEntityFloodlight floodlight, int index) {
        BlockPos source = floodlight.getBlockPos();
        sourceX = source.getX();
        sourceY = source.getY();
        sourceZ = source.getZ();
        this.index = index;
        hasSource = true;
        setChanged();
    }

    boolean isFrom(BlockPos floodlight) {
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
