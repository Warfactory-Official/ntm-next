// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.inventory.recipes.SpaceAssemblerRecipe;
import com.hbm.inventory.recipes.SpaceAssemblerRecipes;
import com.hbm.items.ModItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.util.BobMathUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;

public final class SatelliteScience extends Satellite {

    public static final int COOLDOWN = 15 * 60 * 20;
    public static final int SENSOR_DURATION = 100 * 60 * 60 * 20;
    public static final MapCodec<SatelliteScience> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .optionalFieldOf("lastScience", 0L)
                                                    .forGetter(s -> s.lastScience),
                                            Codec.INT
                                                    .optionalFieldOf("sensorProgress", 0)
                                                    .forGetter(s -> s.sensorProgress),
                                            Codec.INT
                                                    .optionalFieldOf("sensorCount", 0)
                                                    .forGetter(s -> s.sensorCount),
                                            Codec.INT
                                                    .optionalFieldOf("assemblerCount", 0)
                                                    .forGetter(s -> s.assemblerCount),
                                            Codec.DOUBLE
                                                    .optionalFieldOf("assemblerProgress", 0D)
                                                    .forGetter(s -> s.assemblerProgress),
                                            Codec.STRING
                                                    .listOf()
                                                    .optionalFieldOf("assemblerTasks", List.of())
                                                    .forGetter(s -> s.assemblerTasks))
                                    .apply(i, SatelliteScience::new));

    public long lastScience;
    public int sensorProgress;
    public int sensorCount;
    public int assemblerCount;
    public double assemblerProgress;
    public List<String> assemblerTasks = new ArrayList<>();

    public SatelliteScience() {}

    private SatelliteScience(
            long lastScience,
            int sensorProgress,
            int sensorCount,
            int assemblerCount,
            double assemblerProgress,
            List<String> assemblerTasks) {
        this.lastScience = lastScience;
        this.sensorProgress = sensorProgress;
        this.sensorCount = sensorCount;
        this.assemblerCount = assemblerCount;
        this.assemblerProgress = assemblerProgress;
        this.assemblerTasks = new ArrayList<>(assemblerTasks);
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.SCIENCE;
    }

    @Override
    public boolean hasData(ServerLevel level) {
        if (super.hasData(level)) return true;
        if (level.getGameTime() > lastScience + COOLDOWN) {
            produceData(DriveType.DISK_EMPTY, DriveType.DISK_FLIGHTDATA);
            lastScience = level.getGameTime();
            SatelliteSavedData.get(level).setDirty();
        }
        return super.hasData(level);
    }

    @Override
    public void onPartDelivered(ServerLevel level, ItemStack part) {
        if (part.is(ModItems.SATELLITE_SCIENCE_SENSOR.get())) {
            sensorCount++;
        } else if (part.is(ModItems.SATELLITE_SCIENCE_ASSEMBLER.get())) {
            assemblerCount++;
        } else {
            SpaceAssemblerRecipe recipe = SpaceAssemblerRecipes.INSTANCE.getRecipeFor(part);
            if (recipe == null) return;
            assemblerTasks.add(recipe.getInternalName());
        }
        SatelliteSavedData.get(level).setDirty();
    }

    @Override
    public void onUpdateTick(ServerLevel level) {
        if (sensorProgress < SENSOR_DURATION) {
            if (sensorCount > 0) {
                sensorProgress += sensorCount;
                SatelliteSavedData.get(level).setDirty();
            }
        } else {
            sensorProgress = 0;
            produceData(DriveType.DISK_EMPTY, DriveType.DISK_ORBITDATA);
            SatelliteSavedData.get(level).setDirty();
        }

        if (assemblerCount > 0 && requestableSlots.isEmpty() && !assemblerTasks.isEmpty()) {
            SpaceAssemblerRecipe recipe =
                    SpaceAssemblerRecipes.INSTANCE.getRecipe(assemblerTasks.getFirst());
            if (recipe == null)
                throw new IllegalStateException(
                        "Unknown orbital assembly recipe: " + assemblerTasks.getFirst());
            assemblerProgress += (double) assemblerCount / recipe.duration;
            SatelliteSavedData.get(level).setDirty();
            if (assemblerProgress >= 1D) {
                List<ItemStack> output = new ArrayList<>();
                for (WeightedList<ItemStack> slot : recipe.outputItems()) {
                    output.add(slot.getRandom(level.getRandom()).orElse(ItemStack.EMPTY).copy());
                }
                requestableSlots = List.copyOf(output);
                assemblerTasks.removeFirst();
                assemblerProgress = 0D;
            }
        }
    }

    @Override
    public List<Component> getInfo(ServerLevel level) {
        long cooldown = lastScience + COOLDOWN - level.getGameTime();
        long seconds = cooldown / 20;
        List<Component> info = new ArrayList<>();
        info.add(Component.translatable(type().stationNameKey()));
        info.add(
                cooldown <= 0
                        ? Component.translatable("satellite.ready")
                        : Component.translatable(
                                "satellite.cooldown",
                                Component.translatable(
                                        "unit.minutes_seconds", seconds / 60, seconds % 60)));
        if (sensorCount > 0) {
            info.add(Component.translatable("satellite.sensors", sensorCount));
            info.add(
                    Component.translatable(
                            "satellite.pending",
                            BobMathUtil.format(SENSOR_DURATION - sensorProgress)));
        }
        if (driveOutput.orElse(null) == DriveType.DISK_ORBITDATA) {
            info.add(Component.translatable("satellite.data"));
        }
        if (assemblerCount > 0) {
            info.add(Component.translatable("satellite.assemblers", assemblerCount));
            info.add(
                    Component.translatable(
                            "satellite.progress",
                            Component.translatable(
                                    "unit.percent", (int) Math.round(assemblerProgress * 100D))));
            info.add(Component.translatable("satellite.queue", assemblerTasks.size()));
        }
        return info;
    }
}
