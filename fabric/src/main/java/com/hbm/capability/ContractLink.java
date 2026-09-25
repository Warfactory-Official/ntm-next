// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.NtmContracts.Contract;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ContractLink<T> {

    private final Contract<T> contract;
    private @Nullable T resolved;
    private @Nullable BlockEntity residency;
    private @Nullable LevelChunk resolvedChunk;
    private boolean known;
    private boolean connected;

    public ContractLink(Contract<T> contract) {
        this.contract = contract;
    }

    public void refresh(Level level, BlockPos pos) {

        if (!level.isLoaded(pos)) {
            resolved = null;
            residency = null;
            resolvedChunk = null;
            known = false;
            return;
        }
        T found = contract.at(level, pos);
        residency = found instanceof BlockEntity be ? be : null;
        resolvedChunk = null;
        if (found != null && level instanceof ServerLevel server) {
            BlockPos at = found instanceof BlockEntity be ? be.getBlockPos() : pos;
            LevelChunk chunk = BlockMultiblockCore.readableChunk(server, at.getX(), at.getZ());
            if (chunk == null) {
                found = null;
                residency = null;
            } else {
                resolvedChunk = chunk;
            }
        }
        resolved = found;
        connected = found != null;

        known = !level.isClientSide() || found != null;
    }

    public @Nullable T get(Level level, BlockPos pos) {
        if (known && resident()) return resolved;

        if (!known && !connected && !level.isClientSide()) return null;
        refresh(level, pos);
        return resolved;
    }

    private boolean resident() {
        LevelChunk chunk = resolvedChunk;
        if (chunk != null && !chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) return false;
        BlockEntity be = residency;
        return be == null || !be.isRemoved();
    }

    public void load(ValueInput input, String key) {
        connected = input.getBooleanOr(key, false);
    }

    public void save(ValueOutput output, String key) {
        output.putBoolean(key, connected);
    }
}
