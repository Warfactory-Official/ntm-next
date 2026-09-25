// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public final class DebrisChunk implements BlockAndTintGetter {

    public final int sizeX, sizeY, sizeZ;
    private final BlockState[] blocks;
    private final BlockState air = Blocks.AIR.defaultBlockState();
    private final int blockLight;
    private final int skyLight;
    private final CardinalLighting cardinalLighting;
    private final int tint;

    public DebrisChunk(int sizeX, int sizeY, int sizeZ, ClientLevel level, BlockPos origin) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = new BlockState[Math.max(0, sizeX * sizeY * sizeZ)];
        this.blockLight = level.getBrightness(LightLayer.BLOCK, origin);
        this.skyLight = level.getBrightness(LightLayer.SKY, origin);
        this.cardinalLighting = level.cardinalLighting();
        this.tint = -1;
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    public BlockState get(int x, int y, int z) {
        if (!inBounds(x, y, z)) return air;
        BlockState state = blocks[(y * sizeZ + z) * sizeX + x];
        return state == null ? air : state;
    }

    public void set(int x, int y, int z, BlockState state) {
        if (!inBounds(x, y, z)) return;
        blocks[(y * sizeZ + z) * sizeX + x] = state;
    }

    public boolean isEmpty() {
        for (BlockState state : blocks) {
            if (state != null && !state.isAir()) return false;
        }
        return true;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return get(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return LevelLightEngine.EMPTY;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return layer == LightLayer.SKY ? skyLight : blockLight;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return Math.max(blockLight, skyLight - darkening);
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return cardinalLighting;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver color) {
        return tint;
    }

    @Override
    public int getHeight() {
        return sizeY;
    }

    @Override
    public int getMinY() {
        return 0;
    }
}
