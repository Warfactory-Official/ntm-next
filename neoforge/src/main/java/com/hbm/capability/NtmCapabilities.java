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

package com.hbm.capability;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidHandler;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.blocks.machine.CMPortBlock;
import com.hbm.blocks.machine.CustomMachinePorts;
import com.hbm.blocks.machine.PWRBlock;
import com.hbm.blocks.multiblock.MultiblockCellBlock;
import com.hbm.blocks.multiblock.MultiblockCellCaps;
import com.hbm.blocks.multiblock.MultiblockCellShapes;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockLayouts;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.PortDomain;
import com.hbm.capability.port.PortView;
import com.hbm.compat.computercraft.ComputerCraft;
import com.hbm.platform.Caps;
import com.hbm.platform.FacedContext;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

public final class NtmCapabilities {
    public static final int FE_INSERT_PLANE = MultiblockCoreBlock.PASSIVE_NONE;
    public static final int FE_EXTRACT_PLANE = MultiblockCoreBlock.PASSIVE_NONE;
    public static final int ITEM_PLANE = MultiblockCoreBlock.PASSIVE_ITEMS;

    private NtmCapabilities() {}

    private static final Set<Block> FLUID_EXPOSED =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public static void exposeFluid(RegistryHandle<? extends Block> handle) {
        FLUID_EXPOSED.add(handle.get());
        Caps.fluidExposures.add((Supplier<? extends Block>) handle);
    }

    public static boolean ownsFluidExposure(BlockState state) {
        return FLUID_EXPOSED.contains(state.getBlock());
    }

    private static final Set<Block> ENERGY_EXPOSED =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public static void exposeEnergyAtBlock(RegistryHandle<? extends Block> handle) {
        ENERGY_EXPOSED.add(handle.get());
        Caps.energyBlockExposures.add((Supplier<? extends Block>) handle);
    }

    public static boolean ownsEnergyExposure(BlockState state) {
        return ENERGY_EXPOSED.contains(state.getBlock());
    }

    public static void registerTokens() {
        EnergyCaps.register();
        FluidCaps.register();
        FusionPorts.register();
        NtmContracts.register();
    }

    public static void declareAll(boolean bakedLayouts) {
        Set<String> beKeyed = new HashSet<>();
        FLUID_EXPOSED.clear();
        MultiblockCellShapes.reset();
        MultiblockCellBlock.resetNeighborForwarding();
        List<RegistryHandle<? extends Block>> roster = Registrar.registeredBlocks;
        MultiblockCellCaps.declare(roster);
        PWRBlock.declareProxies();
        NtmContracts.registerAll(roster);
        ComputerCraft.declare(roster);
        for (RegistryHandle<? extends Block> handle : roster) {
            MachineCaps caps = MachineCaps.declared(handle.get());
            if (handle.get() instanceof MultiblockCoreBlock core) {
                if (bakedLayouts)
                    core.installBakedLayout(
                            caps != null ? caps.declaredBits() : MachineCaps.NONE.declaredBits());
            }
            if (caps == null) continue;
            declare(handle, (CapabilityBlock) handle.get(), caps, beKeyed);
        }
        MultiblockCellShapes.publish();
        if (bakedLayouts) MultiblockLayouts.release();
    }

    public enum CapRole {
        POWER_IN(MachineCaps.POWER_IN, MultiblockCoreBlock.PASSIVE_POWER_IN),
        POWER_OUT(MachineCaps.POWER_OUT, MultiblockCoreBlock.PASSIVE_NONE),
        FLUID_IN(MachineCaps.FLUID_IN, MultiblockCoreBlock.PASSIVE_FLUID_IN),
        FLUID_OUT(MachineCaps.FLUID_OUT, MultiblockCoreBlock.PASSIVE_NONE);

        public static final CapRole[] VALUES = values();
        public static final int COUNT = VALUES.length;

        public final int bit;
        public final int passivePlane;
        public final boolean fluid;
        public final int planeIndex;

        CapRole(int bit, int passivePlane) {
            this.bit = bit;
            this.passivePlane = passivePlane;
            this.fluid = bit == MachineCaps.FLUID_IN || bit == MachineCaps.FLUID_OUT;
            this.planeIndex = ordinal();
        }
    }

    private static boolean once(Set<String> seen, MachineCaps caps, String what) {
        return seen.add(what + "@" + caps.beType().id());
    }

    private static void declare(
            RegistryHandle<? extends Block> handle,
            CapabilityBlock declaring,
            MachineCaps caps,
            Set<String> beKeyed) {
        assertOwnsItsBeType(handle, caps);
        boolean multiblock = handle.get() instanceof MultiblockCoreBlock;
        boolean own = !caps.selfProvided;
        if (caps.powerIn && own)
            provider(
                    handle,
                    multiblock,
                    caps,
                    beKeyed,
                    EnergyCaps.RECEIVER,
                    EnergyHandler.class,
                    CapRole.POWER_IN);
        if (caps.powerOut && own)
            provider(
                    handle,
                    multiblock,
                    caps,
                    beKeyed,
                    EnergyCaps.PROVIDER,
                    EnergyHandler.class,
                    CapRole.POWER_OUT);
        if (caps.fluidIn && own)
            provider(
                    handle,
                    multiblock,
                    caps,
                    beKeyed,
                    FluidCaps.RECEIVER,
                    FluidHandler.class,
                    CapRole.FLUID_IN);
        if (caps.fluidOut && own)
            provider(
                    handle,
                    multiblock,
                    caps,
                    beKeyed,
                    FluidCaps.PROVIDER,
                    FluidHandler.class,
                    CapRole.FLUID_OUT);

        if (caps.fluidIn || caps.fluidOut) exposeFluid(handle);

        if (caps.items && own && once(beKeyed, caps, "item"))
            Caps.itemExposures.add((Supplier<? extends BlockEntityType<?>>) caps.beType());
        if (caps.itemsAtCells) Caps.itemBlockExposures.add((Supplier<? extends Block>) handle);
        if (caps.fe) {
            if (multiblock) {
                exposeEnergyAtBlock(handle);
            } else {
                ENERGY_EXPOSED.add(handle.get());
                if (once(beKeyed, caps, "fe"))
                    Caps.energyExposures.add(
                            (Supplier<? extends BlockEntityType<?>>) caps.beType());
            }
        }
        declaring.declareExtraCaps(handle);
    }

    public static @Nullable BlockEntity itemOwnerAt(
            Level level, BlockPos pos, BlockState state, Direction side) {
        if (state.getBlock() instanceof CMPortBlock)
            return CustomMachinePorts.ownerAt(level, pos, state);
        if (state.getBlock() instanceof PWRBlock) return PWRBlock.ioController(level, pos, state);
        if (!MultiblockSurface.isSurface(state)) return null;
        BlockEntity owner = MultiblockSurface.resolveOwnerIfOpenForItems(level, pos, state, side);
        return owner != null && !MultiblockSurface.ownerDeclares(owner, MachineCaps.ITEMS_AT_CELLS)
                ? null
                : owner;
    }

    private static void assertOwnsItsBeType(
            RegistryHandle<? extends Block> handle, MachineCaps caps) {
        RegistryHandle<? extends BlockEntityType<?>> beType = caps.beTypeOrNull();
        if (beType == null) return;
        Block block = handle.get();
        if (!beType.get().isValid(block.defaultBlockState())) {
            throw new IllegalStateException(
                    handle.id()
                            + " declares capabilities on block entity type "
                            + beType.id()
                            + ", which is not registered for that block");
        }
    }

    private static <T, C> void provider(
            RegistryHandle<? extends Block> handle,
            boolean multiblock,
            MachineCaps caps,
            Set<String> beKeyed,
            BlockCapability<T, C> token,
            Class<T> type,
            CapRole role) {
        if (multiblock) {
            Caps.pendingBlocks.add(
                    new Caps.BlockRegistration<>(
                            token,
                            (Supplier<? extends Block>) handle,
                            (level, pos, state, be, context) ->
                                    surfaceCap(level, pos, state, context, type, role)));
        } else if (once(beKeyed, caps, role.name())) {
            Caps.pending.add(
                    new Caps.Registration<>(
                            token,
                            (Supplier<? extends BlockEntityType<? extends BlockEntity>>)
                                    caps.beType(),
                            (be, context) ->
                                    MachineCaps.accepts(be, context, role) ? type.cast(be) : null));
        }
    }

    public static <T> @Nullable T surfaceCap(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Object context,
            Class<T> type,
            CapRole role) {
        return gated(
                resolveCap(
                        level,
                        pos,
                        state,
                        FacedContext.sideOf(context),
                        type,
                        role,
                        role.passivePlane),
                context,
                role);
    }

    private static <T> @Nullable T gated(
            @Nullable T found, @Nullable Object context, CapRole role) {
        return found instanceof BlockEntity owner && !MachineCaps.accepts(owner, context, role)
                ? null
                : found;
    }

    public static <T> @Nullable T feCap(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction side,
            Class<T> type,
            CapRole role) {
        int plane = role == CapRole.POWER_IN ? FE_INSERT_PLANE : FE_EXTRACT_PLANE;
        BlockEntity core = MultiblockSurface.resolveOwner(level, pos, state, side, plane, role);
        if (core == null || core.isRemoved() || !type.isInstance(core)) return null;
        if ((MachineCaps.declaredDomains(core) & role.bit) == 0) return null;
        if (!MultiblockSurface.ownerDeclares(core, MachineCaps.FE)) return null;
        if (!MachineCaps.accepts(core, side, role)) return null;
        PortDomain<? extends PortView> domain = PortDomain.forCapability(type);
        if (domain != null && core instanceof CapabilityPortProvider host) {
            PortView port = host.portAccess(domain, pos, side);
            if (port != null && port.as(type, role) == null) return null;
        }
        return type.cast(core);
    }

    public static <T> @Nullable T coreCap(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Object context,
            Class<T> type,
            CapRole role) {
        return gated(
                resolveCap(
                        level,
                        pos,
                        state,
                        FacedContext.sideOf(context),
                        type,
                        role,
                        MultiblockCoreBlock.PASSIVE_NONE),
                context,
                role);
    }

    private static <T> @Nullable T resolveCap(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Direction side,
            Class<T> type,
            CapRole role,
            int passivePlane) {
        ServerLevel server = level instanceof ServerLevel s ? s : null;
        if (server != null && side != null) {
            T hit = ResolvedCapCache.of(server).serve(pos, state, side, type, role, passivePlane);
            if (hit != null) return hit;
        }

        BlockEntity core =
                MultiblockSurface.resolveOwner(level, pos, state, side, passivePlane, role);
        int bit = role.bit;
        if (core != null && bit != 0 && !MultiblockSurface.ownerDeclares(core, bit)) return null;
        if (core == null) return null;

        PortDomain<? extends PortView> domain = PortDomain.forCapability(type);
        if (side != null && domain != null && core instanceof CapabilityPortProvider host) {
            PortView port = host.portAccess(domain, pos, side);
            if (port != null) {
                T claimed = port.as(type, role);
                if (claimed != null && server != null) {
                    ResolvedCapCache.of(server)
                            .arm(server, pos, state, side, type, role, passivePlane, claimed, core);
                }
                return claimed;
            }
        }
        T plain = type.isInstance(core) ? type.cast(core) : null;
        if (plain != null && server != null && side != null) {
            ResolvedCapCache.of(server)
                    .arm(server, pos, state, side, type, role, passivePlane, plain, core);
        }
        return plain;
    }
}
