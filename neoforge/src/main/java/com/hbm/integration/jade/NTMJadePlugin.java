// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jade;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

@WailaPlugin
public final class NTMJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEnergyStorage(Energy.INSTANCE, BlockEntity.class);
        registration.registerFluidStorage(Fluids.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(Energy.INSTANCE);
        registration.registerFluidStorageClient(Fluids.INSTANCE);
        registration.addRayTraceCallback(
                (hit, accessor, original) -> toCore(registration, accessor));
    }

    private static Accessor<?> toCore(IWailaClientRegistration registration, Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor cell)
                || !MultiblockSurface.isFoldedCell(cell.getBlockState())) {
            return accessor;
        }
        Level level = cell.getLevel();
        BlockPos core = MultiblockSurface.clientCoreOf(level, cell.getPosition());
        if (core == null) return registration.emptyAccessor().hit(cell.getHitResult()).build();
        return registration
                .blockAccessor()
                .from(cell)
                .blockState(level.getBlockState(core))
                .blockEntity(() -> level.getBlockEntity(core))
                .hit(cell.getHitResult().withPosition(core))
                .build();
    }

    private static final class Energy
            implements IServerExtensionProvider<EnergyView.Data>,
                    IClientExtensionProvider<EnergyView.Data, EnergyView> {
        static final Energy INSTANCE = new Energy();

        @Override
        public Identifier getUid() {
            return Library.id("energy_storage");
        }

        @Override
        public @Nullable List<ViewGroup<EnergyView.Data>> getGroups(Accessor<?> accessor) {
            if (!(accessor.getTarget() instanceof IEnergyHandlerMK2 machine)
                    || machine.getMaxPower() <= 0) return null;
            return List.of(
                    new ViewGroup<>(
                            List.of(
                                    new EnergyView.Data(
                                            machine.getPower(), machine.getMaxPower()))));
        }

        @Override
        public boolean shouldRequestData(Accessor<?> accessor) {
            return accessor.getTarget() instanceof IEnergyHandlerMK2;
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(
                Accessor<?> accessor, List<ViewGroup<EnergyView.Data>> groups) {
            return ClientViewGroup.map(groups, data -> EnergyView.read(data, "HE"), null);
        }
    }

    private static final class Fluids
            implements IServerExtensionProvider<FluidView.Data>,
                    IClientExtensionProvider<FluidView.Data, FluidView> {
        static final Fluids INSTANCE = new Fluids();

        @Override
        public Identifier getUid() {
            return Library.id("fluid_storage");
        }

        @Override
        public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
            if (!(accessor.getTarget() instanceof IFluidHandlerMK2 machine)) return null;
            FluidTankNTM[] tanks = machine.getAllTanks();
            if (tanks.length == 0) return null;
            List<FluidView.Data> views = new ArrayList<>(tanks.length);
            for (FluidTankNTM tank : tanks) {
                Fluid fluid = tank.getFluid();
                JadeFluidObject content =
                        fluid == null
                                ? JadeFluidObject.empty()
                                : JadeFluidObject.of(fluid, toPlatform(tank.getFill()));
                views.add(new FluidView.Data(content, toPlatform(tank.getMaxFill())));
            }
            return List.of(new ViewGroup<>(views));
        }

        @Override
        public boolean shouldRequestData(Accessor<?> accessor) {
            return accessor.getTarget() instanceof IFluidHandlerMK2;
        }

        @Override
        public List<ClientViewGroup<FluidView>> getClientGroups(
                Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
            return ClientViewGroup.map(groups, FluidView::readDefault, null);
        }

        private static long toPlatform(int millibuckets) {
            return millibuckets * JadeFluidObject.bucketVolume() / 1000L;
        }
    }
}
