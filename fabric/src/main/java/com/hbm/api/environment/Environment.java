// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.environment;

import com.hbm.lib.Library;
import com.hbm.registration.OverridePoint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class Environment {

    public static final OverridePoint<EnvironmentProvider> PROVIDER =
            OverridePoint.of(Library.id("environment"), new EnvironmentProvider() {});

    private Environment() {}

    public static boolean installed() {
        return PROVIDER.owner() != null;
    }

    public static boolean breatheAir(ServerLevel level, BlockPos pos, int amount) {
        return PROVIDER.get().breatheAir(level, pos, amount);
    }

    public static TriState canBreathe(LivingEntity entity) {
        return PROVIDER.get().canBreathe(entity);
    }

    public static void tickLiving(LivingEntity entity) {
        PROVIDER.get().tickLiving(entity);
    }

    public static void blockChanged(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        PROVIDER.get().blockChanged(level, pos, oldState, newState);
    }

    public static boolean vacuum(Level level) {
        return PROVIDER.get().vacuum(level);
    }

    public static boolean evaporates(ServerLevel level, BlockPos pos, boolean vacuumProof) {
        return PROVIDER.get().evaporates(level, pos, vacuumProof);
    }

    public static boolean hasAir(ServerLevel level, BlockPos pos) {
        return PROVIDER.get().hasAir(level, pos);
    }

    public static boolean compressAir(ServerLevel level, BlockPos pos, int deficit) {
        return PROVIDER.get().compressAir(level, pos, deficit);
    }

    public static void releaseGas(ServerLevel level, Fluid fluid, double mB) {
        PROVIDER.get().releaseGas(level, fluid, mB);
    }

    public static @Nullable Fluid groundwater(ServerLevel level) {
        return PROVIDER.get().groundwater(level);
    }

    public static int sunHeat(ServerLevel level, BlockPos pos, int heat) {
        return PROVIDER.get().sunHeat(level, pos, heat);
    }

    public static double skyRadiation(ServerLevel level, BlockPos pos, double rad) {
        return PROVIDER.get().skyRadiation(level, pos, rad);
    }

    public static float gravity(LivingEntity entity) {
        return PROVIDER.get().gravity(entity);
    }

    public static boolean orbital(Level level) {
        return PROVIDER.get().orbital(level);
    }

    public static boolean fertilize(ServerLevel level, BlockPos pos) {
        return PROVIDER.get().fertilize(level, pos);
    }

    public static void chemicalImpact(ServerLevel level, BlockPos pos, Fluid fluid) {
        PROVIDER.get().chemicalImpact(level, pos, fluid);
    }

    public static TriState eclipse(Level level) {
        return PROVIDER.get().eclipse(level);
    }

    public static double particlePressure(Level level) {
        return PROVIDER.get().particlePressure(level);
    }

    public static boolean meteorStrikes(ServerLevel level) {
        return PROVIDER.get().meteorStrikes(level);
    }

    public static @Nullable Component deliveryRefusal(ServerLevel level) {
        return PROVIDER.get().deliveryRefusal(level);
    }
}
