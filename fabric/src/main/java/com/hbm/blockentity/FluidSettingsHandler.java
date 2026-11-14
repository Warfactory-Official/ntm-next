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

package com.hbm.blockentity;

import com.hbm.interfaces.SettingsHandler;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public interface FluidSettingsHandler extends SettingsHandler {
    default FluidTankNTM[] settingsTanks() {
        return FluidTankAccess.snapshot(this);
    }

    default String[] settingsFluidIds() {
        FluidTankNTM[] tanks = settingsTanks();
        String[] ids = new String[tanks.length];
        int count = 0;
        for (FluidTankNTM tank : tanks) {
            Fluid fluid = tank.getDeclaredFluid();
            if (fluid == null) fluid = Fluids.EMPTY;
            if (NTMFluidProperties.hasTrait(fluid, FluidTraitSimple.FT_NoID.class)) continue;
            ids[count++] = BuiltInRegistries.FLUID.getKey(fluid).toString();
        }
        return count == ids.length ? ids : Arrays.copyOf(ids, count);
    }

    default @Nullable FluidTankNTM pasteTank() {
        return null;
    }

    @Override
    default CompoundTag copySettings(Level level, BlockPos pos) {
        return writeFluids(new CompoundTag(), settingsFluidIds());
    }

    @Override
    default void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        FluidTankNTM tank = pasteTank();
        if (tank == null) return;
        Fluid fluid = readFluid(settings, option);
        if (fluid == null) return;
        tank.setTankTypeByIdentifier(fluid);
        if (this instanceof BlockEntity be) level.blockEntityChanged(be.getBlockPos());
    }

    @Override
    default String[] getSettingsOptions(Level level, BlockPos pos) {
        return fluidKeys(settingsFluidIds());
    }

    static CompoundTag writeFluids(CompoundTag settings, String[] ids) {
        if (ids.length == 0) return settings;
        settings.putInt("fluidCount", ids.length);
        for (int i = 0; i < ids.length; i++) settings.putString("fluidID" + i, ids[i]);
        return settings;
    }

    static @Nullable Fluid readFluid(CompoundTag settings, int option) {
        int count = settings.getIntOr("fluidCount", 0);
        if (option < 0 || option >= count) return null;
        Identifier id = Identifier.tryParse(settings.getStringOr("fluidID" + option, ""));
        return id == null ? null : BuiltInRegistries.FLUID.getOptional(id).orElse(null);
    }

    static String[] fluidKeys(String[] ids) {
        String[] keys = new String[ids.length];
        for (int i = 0; i < ids.length; i++)
            keys[i] = NTMFluidProperties.nameKey(Identifier.parse(ids[i]));
        return keys;
    }
}
