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

import com.hbm.api.block.MoltenMetalHandler;
import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.api.tile.HeatHandler;
import com.hbm.interfaces.*;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

public final class NtmContracts {
    @FunctionalInterface
    public interface OwnerLookup {
        @Nullable BlockPos ownerOf(Level level, BlockPos pos, BlockState state);
    }

    private static final List<Contract<?>> ALL = new ArrayList<>();
    private static final Set<String> PROXIED = new HashSet<>();

    public static final Contract<MoltenMetalHandler> CRUCIBLE_ACCEPTOR =
            contract("crucible_acceptor", MoltenMetalHandler.class);

    public static final Contract<HeatHandler> HEAT_SOURCE =
            contract("heat_source", HeatHandler.class);

    public static final Contract<ToolInteractionHandler> TOOLABLE =
            contract("toolable", ToolInteractionHandler.class);

    public static final Contract<LookOverlayProvider> LOOK_OVERLAY =
            contract("look_overlay", LookOverlayProvider.class);

    public static final Contract<SettingsHandler> COPIABLE =
            contract("copiable", SettingsHandler.class, false);

    public static final Contract<RepairHandler> REPAIRABLE =
            contract("repairable", RepairHandler.class);

    public static final Contract<Extinguishable> EXTINGUISHABLE =
            contract("extinguishable", Extinguishable.class);

    public static final Contract<FanInteractionHandler> BLOWABLE =
            contract("blowable", FanInteractionHandler.class);

    public static final Contract<ItemInsertionHandler> INSERTABLE =
            contract("insertable", ItemInsertionHandler.class);

    public static final Contract<TriggerHandler> BOMB = contract("bomb", TriggerHandler.class);

    public static final Contract<ConveyorBelt> CONVEYOR_BELT =
            contract("conveyor_belt", ConveyorBelt.class);

    public static final Contract<ConveyorInputHandler> ENTERABLE =
            contract("enterable", ConveyorInputHandler.class);

    public static final Contract<RadioValueProvider> ROR_VALUE_PROVIDER =
            contract("ror_value_provider", RadioValueProvider.class);

    public static final Contract<RadioControlHandler> ROR_INTERACTIVE =
            contract("ror_interactive", RadioControlHandler.class);

    public static final Contract<Container> INVENTORY = contract("inventory", Container.class);

    public record Target<T>(T handler, BlockPos pos) {}

    public static final class Contract<T> implements ContractLink.Resolver<T> {

        private final String path;
        private final Class<T> type;
        private final boolean blockFirst;
        private final Map<Block, OwnerLookup> owners = new IdentityHashMap<>();
        private @Nullable BlockCapability<T, Direction> token;

        private Contract(String path, Class<T> type, boolean blockFirst) {
            this.path = path;
            this.type = type;
            this.blockFirst = blockFirst;
        }

        public Class<T> type() {
            return type;
        }

        @Override
        public @Nullable T at(Level level, BlockPos pos) {
            return at(level, pos, level.getBlockState(pos), null);
        }

        public @Nullable T at(Level level, BlockPos pos, BlockState state) {
            return at(level, pos, state, null);
        }

        public @Nullable T at(
                Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
            Block block = state.getBlock();
            if (blockFirst && type.isInstance(block)) return type.cast(block);
            return token().getCapability(level, pos, null, null, side);
        }

        public @Nullable Target<T> target(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            OwnerLookup lookup = owners.get(state.getBlock());
            BlockPos owner = lookup == null ? null : lookup.ownerOf(level, pos, state);
            T handler = owner == null ? at(level, pos, state, null) : at(level, owner);
            return handler == null ? null : new Target<>(handler, owner == null ? pos : owner);
        }

        public BlockLookupCache<T> cacheAt(ServerLevel level, BlockPos pos) {
            return Caps.createCache(token(), level, pos, null);
        }

        private BlockCapability<T, Direction> token() {
            var t = token;
            if (t == null)
                throw new IllegalStateException(path + " queried before NtmContracts.register()");
            return t;
        }

        private @Nullable T carriedBy(
                Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            Block block = state.getBlock();
            if (blockFirst && type.isInstance(block)) return type.cast(block);
            if (be == null && state.hasBlockEntity()) be = level.getBlockEntity(pos);
            if (type.isInstance(be)) return type.cast(be);
            return type.isInstance(block) ? type.cast(block) : null;
        }
    }

    private NtmContracts() {}

    private static <T> Contract<T> contract(String path, Class<T> type) {
        return contract(path, type, true);
    }

    private static <T> Contract<T> contract(String path, Class<T> type, boolean blockFirst) {
        Contract<T> c = new Contract<>(path, type, blockFirst);
        ALL.add(c);
        return c;
    }

    public static void register() {
        for (Contract<?> c : ALL) mint(c);
    }

    private static <T> void mint(Contract<T> c) {
        c.token = BlockCapability.create(RegistryUtil.id(c.path), c.type, Direction.class);
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
            Caps.pendingBlocks.add(
                    new Caps.BlockRegistration<>(
                            c.token(),
                            (Supplier<? extends Block>) handle,
                            (level, pos, state, be, side) -> c.carriedBy(level, pos, state, be)));
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
        contract.owners.put(cell.get(), owner);
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        contract.token(),
                        (Supplier<? extends Block>) cell,
                        (level, pos, state, be, side) -> {
                            BlockPos at = owner.ownerOf(level, pos, state);
                            return at == null
                                    ? contract.carriedBy(level, pos, state, be)
                                    : contract.at(level, at, level.getBlockState(at), side);
                        }));
    }

    private static String claim(Contract<?> contract, RegistryHandle<? extends Block> block) {
        return contract.path + "@" + block.id();
    }
}
