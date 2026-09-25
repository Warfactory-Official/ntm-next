// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuHeaterOven;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.tileentity.NeighborDerived;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityHeaterOven extends BlockEntityFireboxBase implements MenuProvider {

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

    @NeighborDerived(at = "below")
    private final ContractLink<IHeatSource> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);

    public BlockEntityHeaterOven(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATING_OVEN.get(), pos, state);
    }

    @Override
    public void tickServer() {

        tryPullHeat();
        super.tickServer();
    }

    private void tryPullHeat() {
        BlockPos heatPos = worldPosition.below();
        IHeatSource source = heatBelow.get(level, heatPos);
        if (source == null) return;

        int toPull =
                Math.max(
                        Math.min(
                                source.getHeatStored(level, heatPos),
                                MachineData.HEATER_OVEN_MAX_HEAT_ENERGY.get() - this.heatEnergy),
                        0);
        this.heatEnergy += (int) (toPull * MachineData.HEATER_OVEN_HEAT_EFF.get());
        source.useUpHeat(level, heatPos, toPull);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterOven");
    }

    @Override
    public ModuleBurnTime getModule() {
        return BURN_MODULE;
    }

    @Override
    public int getBaseHeat() {
        return MachineData.HEATER_OVEN_BASE_HEAT.get();
    }

    @Override
    public double getTimeMult() {
        return MachineData.HEATER_OVEN_TIME_MULT.get();
    }

    @Override
    public int getMaxHeat() {
        return MachineData.HEATER_OVEN_MAX_HEAT_ENERGY.get();
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
        return new MenuHeaterOven(containerId, playerInventory, this);
    }
}
