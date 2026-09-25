// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class WorldInAJar implements BlockAndTintGetter {
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;
    public int lightlevel = 15;

    private BlockState[] blocks;
    private BlockEntity[] tiles;
    private Fluid[] pipeFluids;
    private int revision;

    public WorldInAJar(int x, int y, int z) {
        sizeX = x;
        sizeY = y;
        sizeZ = z;
        blocks = new BlockState[x * y * z];
        tiles = new BlockEntity[blocks.length];
        pipeFluids = new Fluid[blocks.length];
    }

    public void nuke() {
        blocks = new BlockState[blocks.length];
        tiles = new BlockEntity[tiles.length];
        pipeFluids = new Fluid[pipeFluids.length];
        revision++;
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ;
    }

    private int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    public BlockState getBlock(int x, int y, int z) {
        if (!inBounds(x, y, z)) return Blocks.AIR.defaultBlockState();
        BlockState state = blocks[index(x, y, z)];
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        if (inBounds(x, y, z)) {
            blocks[index(x, y, z)] = state;
            revision++;
        }
    }

    public @Nullable BlockEntity getTileEntity(int x, int y, int z) {
        return inBounds(x, y, z) ? tiles[index(x, y, z)] : null;
    }

    public void setTileEntity(int x, int y, int z, BlockEntity tile) {
        if (inBounds(x, y, z)) {
            tiles[index(x, y, z)] = tile;
            revision++;
        }
    }

    public void setPipeFluid(int x, int y, int z, Fluid fluid) {
        if (inBounds(x, y, z)) {
            pipeFluids[index(x, y, z)] = fluid;
            revision++;
        }
    }

    public int revision() {
        return revision;
    }

    public Fluid pipeFluid(BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (!inBounds(x, y, z)) return Fluids.EMPTY;
        Fluid fluid = pipeFluids[index(x, y, z)];
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return getBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return getTileEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return LevelLightEngine.EMPTY;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return lightlevel;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return lightlevel;
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return CardinalLighting.DEFAULT;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        var plains =
                Minecraft.getInstance()
                        .level
                        .registryAccess()
                        .lookupOrThrow(Registries.BIOME)
                        .getOrThrow(Biomes.PLAINS);
        return resolver.getColor(plains.value(), pos.getX(), pos.getZ());
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
