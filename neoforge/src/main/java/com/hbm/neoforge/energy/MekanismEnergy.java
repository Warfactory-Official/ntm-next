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

package com.hbm.neoforge.energy;

import com.hbm.NuclearTech;
import com.hbm.api.energymk2.ForeignEnergyAdapter;
import com.hbm.api.energymk2.ForeignEnergyAdapters;
import com.hbm.api.energymk2.ForeignEnergyNode;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class MekanismEnergy implements ForeignEnergyAdapter {
    private final Class<?> cable;
    private final Class<?> cube;
    private final Class<?> port;
    private final Class<?> entangloporter;
    private final MethodHandle transmitterOf;
    private final MethodHandle networkOf;
    private final MethodHandle buffer;
    private final MethodHandle capacity;
    private final @Nullable MethodHandle emitted;
    private final @Nullable MethodHandle acceptorCache;
    private final @Nullable MethodHandle acceptors;
    private final MethodHandle tier;
    private final MethodHandle multiblock;
    private final MethodHandle formed;
    private final MethodHandle active;
    private final @Nullable MethodHandle matrixContainer;
    private final @Nullable MethodHandle remainingInput;
    private final @Nullable MethodHandle remainingOutput;
    private final MethodHandle frequency;

    private MekanismEnergy() throws ReflectiveOperationException {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        cable = type("mekanism.common.tile.transmitter.TileEntityUniversalCable");
        cube = type("mekanism.common.tile.TileEntityEnergyCube");
        port = type("mekanism.common.tile.multiblock.TileEntityInductionPort");
        entangloporter = type("mekanism.common.tile.TileEntityQuantumEntangloporter");
        Class<?> transmitter = type("mekanism.common.content.network.transmitter.Transmitter");
        Class<?> network = type("mekanism.common.content.network.EnergyNetwork");
        Class<?> dynamic = type("mekanism.common.lib.transmitter.DynamicNetwork");
        Class<?> buffered = type("mekanism.common.lib.transmitter.DynamicBufferedNetwork");
        Class<?> cache = type("mekanism.common.lib.transmitter.acceptor.NetworkAcceptorCache");
        Class<?> multiblockData = type("mekanism.common.lib.multiblock.MultiblockData");
        Class<?> tileMultiblock = type("mekanism.common.tile.prefab.TileEntityMultiblock");
        transmitterOf =
                virtual(
                        lookup,
                        cable,
                        "getTransmitter",
                        type("mekanism.common.content.network.transmitter.UniversalCable"));
        networkOf =
                virtual(
                        lookup,
                        transmitter,
                        "getTransmitterNetwork",
                        type("mekanism.common.lib.transmitter.DynamicNetwork"));
        buffer = virtual(lookup, network, "getBuffer", Long.class);
        capacity = virtual(lookup, buffered, "getCapacity", long.class);
        emitted = optionalGetter(network, "prevTransferAmount", long.class);
        acceptorCache = optionalGetter(dynamic, "acceptorCache", cache);
        acceptors = optionalGetter(cache, "cachedAcceptors", Long2ObjectMap.class);
        tier = virtual(lookup, cube, "getTier", type("mekanism.common.tier.EnergyCubeTier"));
        multiblock = virtual(lookup, tileMultiblock, "getMultiblock", multiblockData);
        formed = virtual(lookup, multiblockData, "isFormed", boolean.class);
        active =
                virtual(
                        lookup,
                        type("mekanism.common.tile.base.TileEntityMekanism"),
                        "getActive",
                        boolean.class);
        frequency =
                virtual(
                        lookup,
                        entangloporter,
                        "getFreq",
                        type("mekanism.common.content.entangloporter.InventoryFrequency"));
        MethodHandle container = null;
        MethodHandle input = null;
        MethodHandle output = null;
        try {
            Class<?> matrix = type("mekanism.common.content.matrix.MatrixMultiblockData");
            Class<?> energy = type("mekanism.common.content.matrix.MatrixEnergyContainer");
            container = virtual(lookup, matrix, "energyContainer", energy);
            MethodHandles.Lookup inside =
                    MethodHandles.privateLookupIn(energy, MethodHandles.lookup());
            input =
                    inside.findVirtual(
                                    energy, "getRemainingInput", MethodType.methodType(long.class))
                            .asType(MethodType.methodType(long.class, Object.class));
            output =
                    inside.findVirtual(
                                    energy, "getRemainingOutput", MethodType.methodType(long.class))
                            .asType(MethodType.methodType(long.class, Object.class));
        } catch (ReflectiveOperationException e) {
            NuclearTech.LOGGER.warn(
                    "Mekanism induction matrix rate limits unavailable; ports share no limit", e);
        }
        matrixContainer = container;
        remainingInput = input;
        remainingOutput = output;
    }

    public static void register() {
        try {
            ForeignEnergyAdapters.register(new MekanismEnergy());
        } catch (ReflectiveOperationException | LinkageError e) {
            NuclearTech.LOGGER.warn(
                    "Mekanism energy members did not resolve; Mekanism stays on the opaque path",
                    e);
        }
    }

    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, false, MekanismEnergy.class.getClassLoader());
    }

    private static MethodHandle virtual(
            MethodHandles.Lookup lookup, Class<?> owner, String name, Class<?> result)
            throws ReflectiveOperationException {
        return lookup.findVirtual(owner, name, MethodType.methodType(result))
                .asType(
                        MethodType.methodType(
                                result.isPrimitive() ? result : Object.class, Object.class));
    }

    private static @Nullable MethodHandle optionalGetter(
            Class<?> owner, String name, Class<?> type) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup())
                    .findGetter(owner, name, type)
                    .asType(
                            MethodType.methodType(
                                    type.isPrimitive() ? type : Object.class, Object.class));
        } catch (ReflectiveOperationException e) {
            NuclearTech.LOGGER.warn(
                    "Mekanism {}.{} unavailable; transit demand falls back to its buffer room",
                    owner.getSimpleName(),
                    name,
                    e);
            return null;
        }
    }

    private static Object object(MethodHandle handle, Object owner) {
        try {
            return (Object) handle.invokeExact(owner);
        } catch (Throwable e) {
            throw new IllegalStateException("Mekanism energy member failed", e);
        }
    }

    private static long number(MethodHandle handle, Object owner) {
        try {
            return (long) handle.invokeExact(owner);
        } catch (Throwable e) {
            throw new IllegalStateException("Mekanism energy member failed", e);
        }
    }

    private static boolean test(MethodHandle handle, Object owner) {
        try {
            return (boolean) handle.invokeExact(owner);
        } catch (Throwable e) {
            throw new IllegalStateException("Mekanism energy member failed", e);
        }
    }

    @Override
    public @Nullable ForeignEnergyNode node(
            ServerLevel level, BlockPos pos, Direction side, BlockEntity be) {
        if (cable.isInstance(be)) {
            Object network = object(networkOf, object(transmitterOf, be));
            return new Transit(network == null ? be : network, network);
        }
        if (cube.isInstance(be)) {
            return "CREATIVE".equals(((Enum<?>) object(tier, be)).name())
                    ? null
                    : new Storage(be, null, false);
        }
        if (port.isInstance(be)) {
            Object matrix = object(multiblock, be);
            return matrix == null || !test(formed, matrix)
                    ? null
                    : new Storage(
                            matrix,
                            matrixContainer == null ? null : object(matrixContainer, matrix),
                            test(active, be));
        }
        if (entangloporter.isInstance(be)) {
            Object frequency = object(this.frequency, be);
            return frequency == null ? null : new Storage(frequency, null, false);
        }
        return null;
    }

    private final class Transit extends ForeignEnergyNode {
        private final Object identity;
        private final @Nullable Object network;

        Transit(Object identity, @Nullable Object network) {
            this.identity = identity;
            this.network = network;
        }

        @Override
        public Kind kind() {
            return Kind.TRANSIT;
        }

        @Override
        public Object identity() {
            return identity;
        }

        @Override
        public long stockFe() {
            return network == null ? 0 : (Long) object(buffer, network);
        }

        @Override
        public long capacityFe() {
            return network == null ? 0 : number(capacity, network);
        }

        @Override
        public long emittedFe() {
            return emitted == null || network == null ? 0 : number(emitted, network);
        }

        @Override
        public boolean otherAcceptors(LongSet ntmPositions) {
            if (network == null) return false;
            if (acceptors == null || acceptorCache == null) return true;
            Long2ObjectMap<?> cached =
                    (Long2ObjectMap<?>) object(acceptors, object(acceptorCache, network));
            int ntm = 0;
            for (LongIterator it = ntmPositions.iterator(); it.hasNext(); )
                if (cached.containsKey(it.nextLong())) ntm++;
            return cached.size() > ntm;
        }

        @Override
        public long fromLimitFe(long probedExtract) {
            return Integer.MAX_VALUE;
        }
    }

    private final class Storage extends ForeignEnergyNode {
        private final Object identity;
        private final @Nullable Object matrix;
        private final boolean ejects;

        Storage(Object identity, @Nullable Object matrix, boolean ejects) {
            this.identity = identity;
            this.matrix = matrix;
            this.ejects = ejects;
        }

        @Override
        public long fromLimitFe(long probedExtract) {
            return ejects ? outputLimitFe() : probedExtract;
        }

        @Override
        public Kind kind() {
            return Kind.STORAGE;
        }

        @Override
        public Object identity() {
            return identity;
        }

        @Override
        public long inputLimitFe() {
            return matrix == null || remainingInput == null
                    ? Long.MAX_VALUE
                    : number(remainingInput, matrix);
        }

        @Override
        public long outputLimitFe() {
            return matrix == null || remainingOutput == null
                    ? Long.MAX_VALUE
                    : number(remainingOutput, matrix);
        }
    }
}
