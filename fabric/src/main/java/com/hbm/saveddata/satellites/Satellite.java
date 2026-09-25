// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.EntitySatellitePod;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.tileentity.network.RTTYSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class Satellite {

    public static final String CHAN_SATLINK = "SAT_LINK";

    public int targetX;
    public int targetZ;
    public String tx = "";
    public Optional<DriveType> driveInput = Optional.empty();
    public Optional<DriveType> driveOutput = Optional.empty();
    public List<ItemStack> requestableSlots = List.of();

    public static void orbit(
            ServerLevel level, SatelliteType type, int freq, double x, double y, double z) {
        orbit(level, new ItemStack(type.item()), freq, x, y, z);
    }

    public static boolean orbit(
            ServerLevel level, ItemStack part, int freq, double x, double y, double z) {
        SatelliteSavedData data = SatelliteSavedData.get(level);
        Satellite existing = data.getSatFromFreq(freq);
        if (existing != null) {
            existing.onPartDelivered(level, part);
            data.setDirty();
            return true;
        }
        SatelliteType type = SatelliteType.fromStack(part);
        if (type == null) return false;
        Satellite sat = type.create();
        data.put(freq, sat);
        sat.setTarget((int) Math.floor(x), (int) Math.floor(z));
        sat.onOrbit(level, x, y, z);
        data.setDirty();
        return true;
    }

    public abstract SatelliteType type();

    public void onOrbit(ServerLevel level, double x, double y, double z) {
        RTTYSystem.broadcast(
                level,
                CHAN_SATLINK,
                "Established connection to "
                        + type().orbitName()
                        + " at "
                        + targetX
                        + " / "
                        + targetZ);
    }

    public void onPartDelivered(ServerLevel level, ItemStack part) {}

    public void setTarget(int x, int z) {
        targetX = x;
        targetZ = z;
    }

    public List<Component> getInfo(ServerLevel level) {
        return List.of(Component.translatable(type().stationNameKey()));
    }

    public void onCommand(ServerLevel level, String... command) {
        onCommandTarget(command);
        onCommandImpl(level, command);
    }

    private void onCommandTarget(String... command) {
        if (command.length == 0) return;
        switch (command[0]) {
            case "settarget" -> {
                if (command.length == 3 || command.length == 4) {
                    targetX =
                            IRORInteractive.parseInt(
                                    command[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
                    targetZ =
                            IRORInteractive.parseInt(
                                    command[command.length - 1],
                                    Integer.MIN_VALUE,
                                    Integer.MAX_VALUE);
                }
            }
            case "gettarget" -> tx = targetX + ";" + targetZ;
            case "gettargetx" -> tx = Integer.toString(targetX);
            case "gettargetz" -> tx = Integer.toString(targetZ);
            default -> {}
        }
    }

    protected void onCommandImpl(ServerLevel level, String... command) {}

    public boolean hasData(ServerLevel level) {
        return driveInput.isPresent() && driveOutput.isPresent();
    }

    public void onUpdateTick(ServerLevel level) {}

    public Optional<DriveType> getOutputData(DriveType input) {
        return driveInput.filter(input::equals).flatMap(ignored -> driveOutput);
    }

    public void produceData(DriveType input, DriveType output) {
        driveInput = Optional.of(input);
        driveOutput = Optional.of(output);
    }

    public void consumeData() {
        driveInput = Optional.empty();
        driveOutput = Optional.empty();
    }

    public boolean tryRequestItems(ServerLevel level, int x, int y, int z) {
        if (requestableSlots.isEmpty()) return false;
        EntitySatellitePod pod =
                new EntitySatellitePod(ModEntities.SATELLITE_POD.get(), level)
                        .setup(y, requestableSlots);
        pod.setPos(x + 0.5D, 300D, z + 0.5D);
        level.addFreshEntity(pod);
        requestableSlots = List.of();
        SatelliteSavedData.get(level).setDirty();
        return true;
    }

    public State state() {
        return new State(
                targetX, targetZ, tx, driveInput, driveOutput, List.copyOf(requestableSlots));
    }

    public static Satellite restore(Satellite sat, State state) {
        sat.targetX = state.targetX();
        sat.targetZ = state.targetZ();
        sat.tx = state.tx();
        sat.driveInput = state.driveInput();
        sat.driveOutput = state.driveOutput();
        sat.requestableSlots = state.requestableSlots();
        return sat;
    }

    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {}

    public enum DriveType implements StringRepresentable {
        FLASH_EMPTY,
        DISK_EMPTY,
        FLASH_BROKEN,
        DISK_BROKEN,
        FLASH_FLIGHTSIM,
        FLASH_PARTICLESIM,
        DISK_FLIGHTDATA,
        DISK_FLIGHTDATA_PROCESSED,
        DISK_ORBITDATA,
        DISK_ORBITDATA_PROCESSED,
        KLAUS;

        public static final Codec<DriveType> CODEC =
                StringRepresentable.fromEnum(DriveType::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record State(
            int targetX,
            int targetZ,
            String tx,
            Optional<DriveType> driveInput,
            Optional<DriveType> driveOutput,
            List<ItemStack> requestableSlots) {
        public static final Codec<State> CODEC =
                RecordCodecBuilder.create(
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
                                                        .forGetter(State::tx),
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
                                                        .forGetter(State::requestableSlots))
                                        .apply(i, State::new));
    }
}
