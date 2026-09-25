// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.wthit;

import com.hbm.NuclearTech;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITargetRedirector;
import mcp.mobius.waila.api.IWailaClientPlugin;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import mcp.mobius.waila.api.data.EnergyData;
import mcp.mobius.waila.api.data.FluidData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class NTMWthitPlugin implements IWailaCommonPlugin, IWailaClientPlugin {

    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(Storage.INSTANCE, BlockEntity.class);
    }

    @Override
    public void register(IClientRegistrar registrar) {
        registrar.redirect(ToCore.INSTANCE, BlockMultiblockCell.class);
        registrar.redirect(ToCore.INSTANCE, BlockMultiblockCore.class);
        EnergyData.describe(NuclearTech.MOD_ID).unit("HE");
    }

    private enum ToCore implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public ITargetRedirector.@Nullable Result redirect(
                ITargetRedirector redirect, IBlockAccessor accessor, IPluginConfig config) {
            if (!MultiblockSurface.isFoldedCell(accessor.getBlockState())) return null;
            BlockPos core =
                    MultiblockSurface.clientCoreOf(accessor.getLevel(), accessor.getPosition());
            return core == null
                    ? redirect.toNowhere()
                    : redirect.to(accessor.getBlockHitResult().withPosition(core));
        }
    }

    private enum Storage implements IDataProvider<BlockEntity> {
        INSTANCE;

        @Override
        public void appendData(
                IDataWriter data, IServerAccessor<BlockEntity> accessor, IPluginConfig config) {
            BlockEntity target = accessor.getTarget();
            if (target instanceof IEnergyHandlerMK2 machine && machine.getMaxPower() > 0) {
                data.add(
                        EnergyData.TYPE,
                        result ->
                                result.add(
                                        EnergyData.of(machine.getPower(), machine.getMaxPower())));
            }
            if (target instanceof IFluidHandlerMK2 machine && machine.getAllTanks().length > 0) {
                data.add(
                        FluidData.TYPE,
                        result -> {
                            FluidTankNTM[] tanks = machine.getAllTanks();
                            FluidData fluids =
                                    FluidData.of(FluidData.Unit.MILLIBUCKETS, tanks.length);
                            for (FluidTankNTM tank : tanks) {
                                Fluid fluid = tank.getFluid();
                                if (fluid != null)
                                    fluids.add(
                                            fluid,
                                            DataComponentPatch.EMPTY,
                                            tank.getFill(),
                                            tank.getMaxFill());
                            }
                            result.add(fluids);
                        });
            }
        }
    }
}
