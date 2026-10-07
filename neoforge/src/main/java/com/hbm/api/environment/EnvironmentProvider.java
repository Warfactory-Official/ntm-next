// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.environment;

import com.hbm.inventory.fluid.NTMFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public interface EnvironmentProvider {
    float STANDARD_GRAVITY = 1.6F;

    default boolean breatheAir(ServerLevel level, BlockPos pos, int amount) {
        return true;
    }

    default TriState canBreathe(LivingEntity entity) {
        return TriState.DEFAULT;
    }

    default void tickLiving(LivingEntity entity) {}

    default void blockChanged(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {}

    default boolean vacuum(Level level) {
        return false;
    }

    default boolean evaporates(ServerLevel level, BlockPos pos, boolean vacuumProof) {
        return false;
    }

    default boolean hasAir(ServerLevel level, BlockPos pos) {
        return true;
    }

    default boolean compressAir(ServerLevel level, BlockPos pos, int deficit) {
        return true;
    }

    default void releaseGas(ServerLevel level, Fluid fluid, double mB) {}

    default @Nullable Fluid groundwater(ServerLevel level) {
        return NTMFluids.WATER;
    }

    default int sunHeat(ServerLevel level, BlockPos pos, int heat) {
        return heat;
    }

    default double skyRadiation(ServerLevel level, BlockPos pos, double rad) {
        return rad;
    }

    default float gravity(LivingEntity entity) {
        return STANDARD_GRAVITY;
    }

    default boolean orbital(Level level) {
        return false;
    }

    default boolean fertilize(ServerLevel level, BlockPos pos) {
        return true;
    }

    default void chemicalImpact(ServerLevel level, BlockPos pos, Fluid fluid) {}

    default TriState eclipse(Level level) {
        return TriState.DEFAULT;
    }

    default double particlePressure(Level level) {
        return 1.0D;
    }

    default boolean meteorStrikes(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD;
    }

    default @Nullable Component deliveryRefusal(ServerLevel level) {
        return null;
    }
}
