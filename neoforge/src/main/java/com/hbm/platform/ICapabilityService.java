// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

public interface ICapabilityService {

    <T, C> BlockCapability<T, C> createToken(Identifier id, Class<T> type, Class<C> context);

    <T, C> @Nullable T find(
            BlockCapability<T, C> token, Level level, BlockPos pos, @Nullable C context);

    <T, C> BlockLookupCache<T> createCache(
            BlockCapability<T, C> token, ServerLevel level, BlockPos pos, @Nullable C context);

    BlockLookupCache<IFluidHandlerView> createFluidHandlerCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side);

    BlockLookupCache<IEnergyHandlerView> createEnergyHandlerCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side);

    void invalidateCaps(Level level, BlockPos pos);

    void watchForeignCaps(ServerLevel level, BlockPos pos);

    void unwatchForeignCaps(ServerLevel level, BlockPos pos);

    @Nullable Object listenForCapChanges(
            ServerLevel level, Iterable<BlockPos> positions, BooleanSupplier onInvalidate);

    default void clearForeignCapWatches() {}

    <T, C, BE extends BlockEntity> void registerProvider(
            BlockCapability<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> mapper);

    <T, C> void registerBlockProvider(
            BlockCapability<T, C> token,
            Supplier<? extends Block> block,
            BlockCapMapper<T, C> mapper);

    CapabilityProviderLedger providerLedger();

    void exposeItem(Supplier<? extends BlockEntityType<?>> beType);

    void exposeItemAtBlock(Supplier<? extends Block> block);

    void exposeEnergy(Supplier<? extends BlockEntityType<?>> beType);

    void exposeEnergyAtBlock(Supplier<? extends Block> block);

    void exposeFluid(Supplier<? extends Block> block);

    @Nullable IFluidHandlerView findFluidHandler(
            Level level, BlockPos pos, @Nullable Direction side);

    @Nullable IFluidHandlerView findFluidHandler(ItemStack stack);

    @Nullable IFluidHandlerView findFluidHandler(Container container, int slot);

    @Nullable IEnergyHandlerView findEnergyHandler(
            Level level, BlockPos pos, @Nullable Direction side);

    @Nullable IEnergyHandlerView findEnergyHandler(Container container, int slot);

    @FunctionalInterface
    interface FacedContext {
        @Nullable Direction side();
    }

    static @Nullable Direction sideOf(@Nullable Object context) {
        if (context instanceof Direction side) return side;
        return context instanceof FacedContext faced ? faced.side() : null;
    }

    interface BlockCapMapper<T, C> {
        @Nullable T map(
                Level level,
                BlockPos pos,
                BlockState state,
                @Nullable BlockEntity be,
                @Nullable C context);
    }
}
