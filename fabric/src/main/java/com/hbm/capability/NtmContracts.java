// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.IHeatSource;
import com.hbm.interfaces.*;
import com.hbm.lib.Library;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class NtmContracts {

    @FunctionalInterface
    public interface OwnerLookup {
        @Nullable BlockPos ownerOf(Level level, BlockPos pos, BlockState state);
    }

    private static final List<Contract<?>> ALL = new ArrayList<>();

    private static final Set<String> PROXIED = new HashSet<>();

    public static final Contract<ICrucibleAcceptor> CRUCIBLE_ACCEPTOR =
            contract("crucible_acceptor", ICrucibleAcceptor.class);

    public static final Contract<IHeatSource> HEAT_SOURCE =
            contract("heat_source", IHeatSource.class);

    public static final Contract<IToolable> TOOLABLE = contract("toolable", IToolable.class);

    public static final Contract<ILookOverlay> LOOK_OVERLAY =
            contract("look_overlay", ILookOverlay.class);

    public static final Contract<IBlowable> BLOWABLE = contract("blowable", IBlowable.class);

    public static final Contract<IInsertable> INSERTABLE =
            contract("insertable", IInsertable.class);

    public static final Contract<IBomb> BOMB = contract("bomb", IBomb.class);

    public static final Contract<IConveyorBelt> CONVEYOR_BELT =
            contract("conveyor_belt", IConveyorBelt.class);

    public static final Contract<IEnterableBlock> ENTERABLE =
            contract("enterable", IEnterableBlock.class);

    public static final Contract<IRORValueProvider> ROR_VALUE_PROVIDER =
            contract("ror_value_provider", IRORValueProvider.class);

    public static final Contract<IRORInteractive> ROR_INTERACTIVE =
            contract("ror_interactive", IRORInteractive.class);

    public static final Contract<Container> INVENTORY = contract("inventory", Container.class);

    public static final class Contract<T> {

        private final String path;
        private final Class<T> type;
        private @Nullable BlockApiLookup<T, Direction> token;

        private Contract(String path, Class<T> type) {
            this.path = path;
            this.type = type;
        }

        public Class<T> type() {
            return type;
        }

        public @Nullable T at(Level level, BlockPos pos) {
            return at(level, pos, level.getBlockState(pos), null);
        }

        public @Nullable T at(Level level, BlockPos pos, BlockState state) {
            return at(level, pos, state, null);
        }

        public @Nullable T at(
                Level level, BlockPos pos, BlockState state, @Nullable Direction side) {

            Block block = state.getBlock();
            if (type.isInstance(block)) return type.cast(block);
            return Services.CAPS.find(token(), level, pos, side);
        }

        public BlockLookupCache<T> cacheAt(ServerLevel level, BlockPos pos) {
            return Services.CAPS.createCache(token(), level, pos, null);
        }

        private BlockApiLookup<T, Direction> token() {
            var t = token;
            if (t == null)
                throw new IllegalStateException(path + " queried before NtmContracts.register()");
            return t;
        }

        private @Nullable T carriedBy(
                Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            Block block = state.getBlock();
            if (type.isInstance(block)) return type.cast(block);
            if (be == null && state.hasBlockEntity()) be = level.getBlockEntity(pos);
            return type.isInstance(be) ? type.cast(be) : null;
        }
    }

    private NtmContracts() {}

    private static <T> Contract<T> contract(String path, Class<T> type) {
        Contract<T> c = new Contract<>(path, type);
        ALL.add(c);
        return c;
    }

    public static void register() {
        for (Contract<?> c : ALL) mint(c);
    }

    private static <T> void mint(Contract<T> c) {
        c.token = Services.CAPS.createToken(Library.id(c.path), c.type, Direction.class);
    }

    public static void registerAll(List<RegistryHandle<? extends Block>> blocks) {
        for (Contract<?> c : ALL) registerOne(c, blocks);
    }

    private static <T> void registerOne(
            Contract<T> c, List<RegistryHandle<? extends Block>> blocks) {
        for (RegistryHandle<? extends Block> handle : blocks) {
            Block block = handle.get();
            if (!c.type.isInstance(block) && !(block instanceof EntityBlock)) continue;
            if (PROXIED.contains(claim(c, handle))) continue;
            Services.CAPS.registerBlockProvider(
                    c.token(),
                    handle,
                    (level, pos, state, be, side) -> c.carriedBy(level, pos, state, be));
        }
    }

    public static <T> void proxy(
            Contract<T> contract, RegistryHandle<? extends Block> cell, OwnerLookup owner) {
        if (contract.type.isInstance(cell.get())) {
            throw new IllegalStateException(
                    cell.id()
                            + " implements "
                            + contract.type.getSimpleName()
                            + " and also proxies it; two providers for one token on one block make which answers a "
                            + "question of registration order");
        }
        PROXIED.add(claim(contract, cell));
        Services.CAPS.registerBlockProvider(
                contract.token(),
                cell,
                (level, pos, state, be, side) -> {
                    BlockPos at = owner.ownerOf(level, pos, state);

                    return at == null
                            ? contract.carriedBy(level, pos, state, be)
                            : contract.at(level, at, level.getBlockState(at), side);
                });
    }

    private static String claim(Contract<?> contract, RegistryHandle<? extends Block> block) {
        return contract.path + "@" + block.id();
    }
}
