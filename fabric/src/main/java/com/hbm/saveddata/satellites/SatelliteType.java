// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPools;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public enum SatelliteType implements StringRepresentable {
    MAPPER("mapper", "satellite_spy", SatelliteMapper::new, SatelliteMapper.CODEC, null),
    SCANNER("scanner", "satellite_scanner", SatelliteScanner::new, SatelliteScanner.CODEC, null),
    RADAR("radar", "satellite_radar", SatelliteRadar::new, SatelliteRadar.CODEC, null),
    DEATH_RAY(
            "death_ray",
            "satellite_death_ray",
            SatelliteDeathRay::new,
            SatelliteDeathRay.CODEC,
            null),
    RESONATOR(
            "resonator",
            "satellite_xenium_resonator",
            SatelliteResonator::new,
            SatelliteResonator.CODEC,
            null),
    RELAY("relay", "satellite_relay", SatelliteRelay::new, SatelliteRelay.CODEC, null),
    MINER(
            "miner",
            "satellite_miner_astro",
            SatelliteMiner::new,
            SatelliteMiner.CODEC,
            ItemPools.POOL_SAT_MINER),
    LUNAR_MINER(
            "lunar_miner",
            "satellite_miner_lunar",
            SatelliteLunarMiner::new,
            SatelliteLunarMiner.CODEC,
            ItemPools.POOL_SAT_LUNAR),
    HORIZONS("horizons", "sat_gerald", SatelliteHorizons::new, SatelliteHorizons.CODEC, null),
    PRECISION_LASER(
            "precision_laser",
            "satellite_precision_laser",
            SatellitePrecisionLaser::new,
            SatellitePrecisionLaser.CODEC,
            null),
    DETECTOR(
            "detector",
            "satellite_detector",
            SatelliteDetector::new,
            SatelliteDetector.CODEC,
            null),
    RAY_SCAN("ray_scan", "satellite_ray_scan", SatelliteRayScan::new, SatelliteRayScan.CODEC, null),
    SCIENCE("science", "satellite_science", SatelliteScience::new, SatelliteScience.CODEC, null);

    public static final Codec<SatelliteType> CODEC =
            StringRepresentable.fromEnum(SatelliteType::values);

    public static final Codec<Satellite> DISPATCH_CODEC =
            CODEC.dispatch("type", Satellite::type, SatelliteType::codec);
    public static final Codec<Satellite> PERSISTED_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            DISPATCH_CODEC.fieldOf("sat").forGetter(sat -> sat),
                                            Satellite.State.CODEC
                                                    .fieldOf("state")
                                                    .forGetter(Satellite::state))
                                    .apply(i, Satellite::restore));

    private final String name;
    private final String itemPath;
    private final Supplier<? extends Satellite> factory;
    private final MapCodec<? extends Satellite> codec;
    private final @Nullable String cargo;
    private @Nullable Item resolved;

    SatelliteType(
            String name,
            String itemPath,
            Supplier<? extends Satellite> factory,
            MapCodec<? extends Satellite> codec,
            @Nullable String cargo) {
        this.name = name;
        this.itemPath = itemPath;
        this.factory = factory;
        this.codec = codec;
        this.cargo = cargo;
    }

    public static @Nullable SatelliteType fromItem(Item item) {
        if (item == Items.AIR) return null;
        if (item == ModItems.SAT_SCANNER.get()) return SCANNER;
        if (item == ModItems.SAT_LASER.get()) return DEATH_RAY;
        if (item == ModItems.SAT_MAPPER.get()) return MAPPER;
        if (item == ModItems.SAT_RADAR.get()) return RADAR;
        if (item == ModItems.SAT_RESONATOR.get()) return RESONATOR;
        if (item == ModItems.SAT_MINER.get()) return MINER;
        if (item == ModItems.SAT_LUNAR_MINER.get()) return LUNAR_MINER;
        for (SatelliteType type : values()) {
            if (type.item() == item) return type;
        }
        return null;
    }

    public static @Nullable SatelliteType fromStack(ItemStack stack) {
        return stack.isEmpty() ? null : fromItem(stack.getItem());
    }

    public Satellite create() {
        return factory.get();
    }

    public String orbitName() {
        return switch (this) {
            case MAPPER -> "NOT_A_SPY_SATELLITE_:)";
            case SCANNER -> "DEPTH_SCANNER";
            case RADAR -> "LEO_RADAR";
            case DEATH_RAY -> "ORBITAL_FUN_PLATFORM_:)";
            case RESONATOR -> "XEN_RELAY";
            case RELAY -> "DIMENSIONAL_RELAY";
            case MINER -> "ASTEROID_MINER";
            case LUNAR_MINER -> "LUNAR_MINER";
            case HORIZONS -> "PAYLOAD_UNKNOWN";
            case PRECISION_LASER -> "ORBITAL_TATOO_REMOVER";
            case DETECTOR -> "UWB_EMISSION_DETECTOR";
            case RAY_SCAN -> "NB_RAY_SCANNER";
            case SCIENCE -> "SCIENCE_PROBE";
        };
    }

    public String stationNameKey() {
        return switch (this) {
            case MAPPER -> "item.hbm.satellite_spy";
            case SCANNER -> "item.hbm.satellite_scanner";
            case RADAR -> "item.hbm.satellite_radar";
            case DEATH_RAY -> "item.hbm.satellite_death_ray";
            case RESONATOR -> "item.hbm.satellite_xenium_resonator";
            case RELAY -> "item.hbm.satellite_relay";
            case MINER -> "item.hbm.satellite_miner_astro";
            case LUNAR_MINER -> "item.hbm.satellite_miner_lunar";
            case HORIZONS -> "item.hbm.sat_gerald";
            case PRECISION_LASER -> "item.hbm.satellite_precision_laser";
            case DETECTOR -> "item.hbm.satellite_detector";
            case RAY_SCAN -> "item.hbm.satellite_ray_scan";
            case SCIENCE -> "item.hbm.satellite_science";
        };
    }

    public MapCodec<? extends Satellite> codec() {
        return codec;
    }

    public @Nullable String cargo() {
        return cargo;
    }

    public Item item() {
        Item cached = resolved;
        if (cached == null) {
            cached = BuiltInRegistries.ITEM.getValue(Library.id(itemPath));
            if (cached == Items.AIR)
                throw new IllegalStateException(itemPath + " is not registered");
            resolved = cached;
        }
        return cached;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
