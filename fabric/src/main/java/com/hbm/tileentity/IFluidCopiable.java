// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public interface IFluidCopiable extends ICopiable {

    default FluidTankNTM[] copiableTanks() {
        return this instanceof IFluidHandlerMK2 tile
                ? tile.getAllTanks()
                : IFluidHandlerMK2.NO_TANKS;
    }

    default String[] getFluidIDToCopy() {
        List<String> types = new ArrayList<>();

        for (FluidTankNTM tank : copiableTanks()) {
            Fluid type = tank.getDeclaredFluid();
            if (type != null && type != Fluids.EMPTY)
                types.add(BuiltInRegistries.FLUID.getKey(type).toString());
        }

        return types.toArray(new String[0]);
    }

    default @Nullable FluidTankNTM getTankToPaste() {
        return null;
    }

    @Override
    default CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        String[] ids = getFluidIDToCopy();
        if (ids.length > 0) tag.putInt("fluidCount", ids.length);
        for (int i = 0; i < ids.length; i++) tag.putString("fluidID" + i, ids[i]);
        return tag;
    }

    @Override
    default void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        FluidTankNTM tank = getTankToPaste();
        if (tank != null) {
            int count = nbt.getIntOr("fluidCount", 0);
            if (count > 0 && index < count) {
                Fluid type =
                        BuiltInRegistries.FLUID.getValue(
                                Identifier.parse(nbt.getStringOr("fluidID" + index, "")));
                tank.setTankTypeByIdentifier(type);
            }
        }
    }

    @Override
    default String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        String[] ids = getFluidIDToCopy();
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++)
            names[i] = NTMFluidProperties.nameKey(Identifier.parse(ids[i]));
        return names;
    }
}
