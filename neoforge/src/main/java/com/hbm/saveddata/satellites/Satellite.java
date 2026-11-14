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

package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.SatellitePod;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.items.machine.DriveType;
import com.hbm.saveddata.SatelliteSavedData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public abstract class Satellite {
    public static final int SURFACE = Integer.MIN_VALUE;
    public static final String SAT_LINK = "SAT_LINK";

    static final Component READY = Component.translatable("satellite.ready");
    private static final double POD_DROP_Y = 300D;

    static final Codec<State> STATE_CODEC =
            RecordCodecBuilder.<State>create(
                            i ->
                                    i.group(
                                                    Codec.INT
                                                            .optionalFieldOf("targetX", 0)
                                                            .forGetter(State::targetX),
                                                    Codec.INT
                                                            .optionalFieldOf("targetZ", 0)
                                                            .forGetter(State::targetZ),
                                                    Codec.STRING
                                                            .optionalFieldOf("tx", "")
                                                            .forGetter(State::reply),
                                                    DriveType.CODEC
                                                            .optionalFieldOf("driveInput")
                                                            .forGetter(State::driveInput),
                                                    DriveType.CODEC
                                                            .optionalFieldOf("driveOutput")
                                                            .forGetter(State::driveOutput),
                                                    ItemStack.OPTIONAL_CODEC
                                                            .listOf()
                                                            .optionalFieldOf(
                                                                    "requestableSlots", List.of())
                                                            .forGetter(State::cargo),
                                                    DataComponentMap.CODEC
                                                            .optionalFieldOf(
                                                                    "components",
                                                                    DataComponentMap.EMPTY)
                                                            .forGetter(State::carried))
                                            .apply(i, State::new))
                    .validate(
                            state ->
                                    state.driveInput().isPresent()
                                                    == state.driveOutput().isPresent()
                                            ? DataResult.success(state)
                                            : DataResult.error(
                                                    () ->
                                                            "driveInput and driveOutput are saved together"));

    private @Nullable SatelliteSavedData owner;
    private int targetX;
    private int targetZ;
    private String reply = "";
    private @Nullable DriveOffer offer;
    private final List<ItemStack> cargo = new ArrayList<>();
    private DataComponentMap carried = DataComponentMap.EMPTY;

    public abstract SatelliteType kind();

    public final void attach(SatelliteSavedData registry) {
        if (owner == registry) return;
        if (owner != null)
            throw new IllegalStateException(
                    "a " + kind() + " satellite cannot join a second registry");
        owner = registry;
    }

    protected final void changed() {
        if (owner == null)
            throw new IllegalStateException(
                    "a " + kind() + " satellite outside any registry changed");
        owner.setDirty();
    }

    public final int targetX() {
        return targetX;
    }

    public final int targetZ() {
        return targetZ;
    }

    public final void aim(int x, int z) {
        targetX = x;
        targetZ = z;
    }

    public final String reply() {
        return reply;
    }

    public final void reply(String text) {
        reply = text;
    }

    public final DataComponentMap carried() {
        return carried;
    }

    final void carry(DataComponentMap components) {
        carried = components;
    }

    public final List<ItemStack> cargo() {
        return cargo;
    }

    public final boolean requestPod(ServerLevel level, int x, int y, int z) {
        if (cargo.isEmpty()) return false;
        var pod = new SatellitePod(ModEntities.SATELLITE_POD.get(), level).deliver(y, cargo);
        pod.setPos(x + 0.5D, POD_DROP_Y, z + 0.5D);
        level.addFreshEntity(pod);
        cargo.clear();
        changed();
        return true;
    }

    public final void command(ServerLevel level, String... tokens) {
        if (tokens.length == 0) return;
        switch (tokens[0]) {
            case "settarget" -> {
                if (tokens.length == 3 || tokens.length == 4) {
                    targetX = RadioCommandParser.intParam(tokens[1]);
                    targetZ = RadioCommandParser.intParam(tokens[tokens.length - 1]);
                }
            }
            case "gettarget" -> reply = targetX + ";" + targetZ;
            case "gettargetx" -> reply = Integer.toString(targetX);
            case "gettargetz" -> reply = Integer.toString(targetZ);
            default -> {}
        }
        kindCommand(level, tokens);
    }

    protected void kindCommand(ServerLevel level, String[] tokens) {}

    protected static int index(String token, int size) {
        return size == 0 ? -1 : RadioCommandParser.intParam(token, 1, size) - 1;
    }

    public void designate(ServerLevel level, Player player, int x, int y, int z) {}

    public void enterOrbit(ServerLevel level, double x, double y, double z) {
        RadioSystem.send(
                level,
                SAT_LINK,
                "Established connection to "
                        + kind().orbitName()
                        + " at "
                        + targetX
                        + " / "
                        + targetZ);
    }

    public void receivePart(ServerLevel level, ItemStack part) {}

    public void tick(ServerLevel level) {}

    public List<Component> info(ServerLevel level) {
        return kind().stationInfo();
    }

    public boolean dataReady(ServerLevel level) {
        return offer != null;
    }

    public final @Nullable DriveOffer offer() {
        return offer;
    }

    public final @Nullable DriveType dataFor(DriveType input) {
        var pending = offer;
        return pending != null && pending.input() == input ? pending.output() : null;
    }

    public final void offerData(DriveType input, DriveType output) {
        offer = new DriveOffer(input, output);
    }

    public final void takeData() {
        offer = null;
    }

    final State state() {
        var pending = offer;
        var input = pending == null ? Optional.<DriveType>empty() : Optional.of(pending.input());
        var output = pending == null ? Optional.<DriveType>empty() : Optional.of(pending.output());
        return new State(targetX, targetZ, reply, input, output, cargo, carried);
    }

    static Satellite restored(Satellite satellite, State state) {
        satellite.targetX = state.targetX();
        satellite.targetZ = state.targetZ();
        satellite.reply = state.reply();
        satellite.offer =
                state.driveInput().isEmpty()
                        ? null
                        : new DriveOffer(
                                state.driveInput().get(), state.driveOutput().orElseThrow());
        satellite.cargo.addAll(state.cargo());
        satellite.carried = state.carried();
        return satellite;
    }

    public record DriveOffer(DriveType input, DriveType output) {}

    record State(
            int targetX,
            int targetZ,
            String reply,
            Optional<DriveType> driveInput,
            Optional<DriveType> driveOutput,
            List<ItemStack> cargo,
            DataComponentMap carried) {}
}
