// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuFirebox;
import com.hbm.modules.ModuleBurnTime;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityHeaterFirebox extends BlockEntityFireboxBase implements MenuProvider {

    private static final ModuleBurnTime BURN_MODULE =
            new ModuleBurnTime()
                    .setLigniteTimeMod(1.25)
                    .setCoalTimeMod(1.25)
                    .setCokeTimeMod(1.25)
                    .setSolidTimeMod(1.5)
                    .setRocketTimeMod(1.5)
                    .setBalefireTimeMod(0.5)
                    .setLigniteHeatMod(2)
                    .setCoalHeatMod(2)
                    .setCokeHeatMod(2)
                    .setSolidHeatMod(3)
                    .setRocketHeatMod(5)
                    .setBalefireHeatMod(15);

    public BlockEntityHeaterFirebox(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREBOX.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterFirebox");
    }

    @Override
    public ModuleBurnTime getModule() {
        return BURN_MODULE;
    }

    @Override
    public int getBaseHeat() {
        return MachineData.FIREBOX_BASE_HEAT.get();
    }

    @Override
    public double getTimeMult() {
        return MachineData.FIREBOX_TIME_MULT.get();
    }

    @Override
    public int getMaxHeat() {
        return MachineData.FIREBOX_MAX_HEAT_ENERGY.get();
    }

    public List<String> getFuelBonuses() {
        return BURN_MODULE.getDesc();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFirebox(containerId, playerInventory, this);
    }
}
