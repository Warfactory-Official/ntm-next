// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.machine.BlockCMPort;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.machine.CustomMachinePorts;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockCellCaps;
import com.hbm.blocks.multiblock.MultiblockCellShapes;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.PortDomain;
import com.hbm.capability.port.PortView;
import com.hbm.compat.computercraft.ComputerCraft;
import com.hbm.platform.ICapabilityService;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class NtmCapabilities {

    public static final int FE_INSERT_PLANE = BlockMultiblockCore.PASSIVE_NONE;
    public static final int FE_EXTRACT_PLANE = BlockMultiblockCore.PASSIVE_NONE;
    public static final int ITEM_PLANE = BlockMultiblockCore.PASSIVE_ITEMS;

    private NtmCapabilities() {}

    private static final Set<Block> FLUID_EXPOSED =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    public static void exposeFluid(RegistryHandle<? extends Block> handle) {
        FLUID_EXPOSED.add(handle.get());
        Services.CAPS.exposeFluid(handle);
    }

    public static boolean ownsFluidExposure(BlockState state) {
        return FLUID_EXPOSED.contains(state.getBlock());
    }

    private static final Set<Block> ENERGY_EXPOSED =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    public static void exposeEnergyAtBlock(RegistryHandle<? extends Block> handle) {
        ENERGY_EXPOSED.add(handle.get());
        Services.CAPS.exposeEnergyAtBlock(handle);
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

    public static void declareAll() {
        Set<String> beKeyed = new HashSet<>();
        FLUID_EXPOSED.clear();
        MultiblockCellShapes.reset();
        List<RegistryHandle<? extends Block>> roster = Services.REGISTRAR.blocks();

        MultiblockCellCaps.declare(roster);
        BlockPWR.declareProxies();
        NtmContracts.registerAll(roster);
        ComputerCraft.declare(roster);
        for (RegistryHandle<? extends Block> handle : roster) {
            if (handle.get() instanceof BlockMultiblockCore core) {
                core.bakeMaskTable(
                        handle.get() instanceof ICapabilityBlock d
                                ? d.caps().declaredBits()
                                : MachineCaps.NONE.declaredBits());
            }
            if (!(handle.get() instanceof ICapabilityBlock declaring)) continue;
            declare(handle, declaring, beKeyed);
        }
    }

    public enum CapRole {
        POWER_IN(MachineCaps.POWER_IN, BlockMultiblockCore.PASSIVE_POWER_IN),
        POWER_OUT(MachineCaps.POWER_OUT, BlockMultiblockCore.PASSIVE_NONE),
        FLUID_IN(MachineCaps.FLUID_IN, BlockMultiblockCore.PASSIVE_FLUID_IN),
        FLUID_OUT(MachineCaps.FLUID_OUT, BlockMultiblockCore.PASSIVE_NONE);

        public static final CapRole[] VALUES = values();
        public static final int COUNT = VALUES.length;

        public final int bit;
        public final int passivePlane;

        public final int planeIndex;

        CapRole(int bit, int passivePlane) {
            this.bit = bit;
            this.passivePlane = passivePlane;
            this.planeIndex = ordinal();
        }
    }

    private static boolean once(Set<String> seen, MachineCaps caps, String what) {
        return seen.add(what + "@" + caps.beType().id());
    }

    private static void declare(
            RegistryHandle<? extends Block> handle,
            ICapabilityBlock declaring,
            Set<String> beKeyed) {
        MachineCaps caps = declaring.caps();
        assertOwnsItsBeType(handle, caps);

        boolean multiblock = handle.get() instanceof BlockMultiblockCore;
        boolean own = !caps.selfProvided;
        if (caps.powerIn && own)
            provider(
                    handle,
                    multiblock,
                    caps,
                    beKeyed,
                    EnergyCaps.RECEIVER,
                    IEnergyHandlerMK2.class,
                    CapRole.POWER_IN,
                    caps.faceGate());
        if (caps.powerOut && own)
            provider(
                    handle,
                    multiblock,
                    caps,
                    beKeyed,
                    EnergyCaps.PROVIDER,
                    IEnergyHandlerMK2.class,
                    CapRole.POWER_OUT,
                    caps.faceGate());
        if (caps.fluidIn && own)
            provider(
                    handle,
                    multiblock,
                    caps,
                    beKeyed,
                    FluidCaps.RECEIVER,
                    IFluidHandlerMK2.class,
                    CapRole.FLUID_IN,
                    caps.fluidGate());
        if (caps.fluidOut && own)
            provider(
                    handle,
                    multiblock,
                    caps,
                    beKeyed,
                    FluidCaps.PROVIDER,
                    IFluidHandlerMK2.class,
                    CapRole.FLUID_OUT,
                    caps.fluidGate());

        if (caps.fluidIn || caps.fluidOut) exposeFluid(handle);

        if (caps.items && once(beKeyed, caps, "item")) Services.CAPS.exposeItem(caps.beType());
        if (caps.itemsAtCells) Services.CAPS.exposeItemAtBlock(handle);
        if (caps.fe) {
            if (multiblock) {
                exposeEnergyAtBlock(handle);
            } else {
                ENERGY_EXPOSED.add(handle.get());
                if (once(beKeyed, caps, "fe")) Services.CAPS.exposeEnergy(caps.beType());
            }
        }
        declaring.declareExtraCaps(handle);
    }

    public static @Nullable BlockEntity itemOwnerAt(
            Level level, BlockPos pos, BlockState state, Direction side) {
        if (state.getBlock() instanceof BlockCMPort)
            return CustomMachinePorts.ownerAt(level, pos, state);
        if (state.getBlock() instanceof BlockPWR) return BlockPWR.ioController(level, pos, state);
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
            BlockApiLookup<T, C> token,
            Class<T> type,
            CapRole role,
            BiPredicate<BlockEntity, ? super C> accepts) {
        if (multiblock) {

            Services.CAPS.registerBlockProvider(
                    token,
                    handle,
                    (level, pos, state, be, context) -> {
                        T owner =
                                fullSurfaceCap(
                                        level,
                                        pos,
                                        state,
                                        ICapabilityService.sideOf(context),
                                        type,
                                        role);
                        if (owner == null) return null;
                        return owner instanceof BlockEntity ownerBe
                                        && !accepts.test(ownerBe, context)
                                ? null
                                : owner;
                    });
        } else if (once(beKeyed, caps, role.name())) {

            Services.CAPS.registerProvider(
                    token,
                    caps.beType(),
                    (be, context) ->
                            context == null || accepts.test(be, context) ? type.cast(be) : null);
        }
    }

    public static <T> @Nullable T feCap(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction side,
            Class<T> type,
            CapRole role) {
        int plane = role == CapRole.POWER_IN ? FE_INSERT_PLANE : FE_EXTRACT_PLANE;
        if (!MultiblockSurface.isOpen(level, pos, state, side, plane, role)) return null;
        BlockEntity core = MultiblockSurface.resolveOwner(level, pos, state, side, plane, role);
        if (core == null || core.isRemoved() || !type.isInstance(core)) return null;

        if ((MachineCaps.declaredDomains(core) & role.bit) == 0) return null;
        if (!MultiblockSurface.ownerDeclares(core, MachineCaps.FE)) return null;
        if (!MachineCaps.acceptsFace(core, side)) return null;
        PortDomain<? extends PortView> domain = PortDomain.forCapability(type);
        if (domain != null && core instanceof IPortHost host) {
            PortView port = host.portAccess(domain, pos, side);

            if (port != null && port.as(type, role) == null) return null;
        }
        return type.cast(core);
    }

    public static <T> @Nullable T coreCap(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Direction side,
            Class<T> type,
            CapRole role) {
        return resolveCap(level, pos, state, side, type, role, BlockMultiblockCore.PASSIVE_NONE);
    }

    public static <T> @Nullable T fullSurfaceCap(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Direction side,
            Class<T> type,
            CapRole role) {
        return resolveCap(level, pos, state, side, type, role, role.passivePlane);
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

        if (side != null && domain != null && core instanceof IPortHost host) {
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
