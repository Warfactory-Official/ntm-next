// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPools;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.registration.OpenRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class SatelliteType {

    private static final OpenRegistry<SatelliteType> REGISTRY =
            new OpenRegistry<>("satellite", SatelliteType::getSerializedName);

    public static final SatelliteType MAPPER =
            ntm(
                    "mapper",
                    "satellite_spy",
                    "NOT_A_SPY_SATELLITE_:)",
                    SatelliteMapper::new,
                    SatelliteMapper.CODEC,
                    null);
    public static final SatelliteType SCANNER =
            ntm(
                    "scanner",
                    "satellite_scanner",
                    "DEPTH_SCANNER",
                    SatelliteScanner::new,
                    SatelliteScanner.CODEC,
                    null);
    public static final SatelliteType RADAR =
            ntm(
                    "radar",
                    "satellite_radar",
                    "LEO_RADAR",
                    SatelliteRadar::new,
                    SatelliteRadar.CODEC,
                    null);
    public static final SatelliteType DEATH_RAY =
            ntm(
                    "death_ray",
                    "satellite_death_ray",
                    "ORBITAL_FUN_PLATFORM_:)",
                    SatelliteDeathRay::new,
                    SatelliteDeathRay.CODEC,
                    null);
    public static final SatelliteType RESONATOR =
            ntm(
                    "resonator",
                    "satellite_xenium_resonator",
                    "XEN_RELAY",
                    SatelliteResonator::new,
                    SatelliteResonator.CODEC,
                    null);
    public static final SatelliteType RELAY =
            ntm(
                    "relay",
                    "satellite_relay",
                    "DIMENSIONAL_RELAY",
                    SatelliteRelay::new,
                    SatelliteRelay.CODEC,
                    null);
    public static final SatelliteType MINER =
            ntm(
                    "miner",
                    "satellite_miner_astro",
                    "ASTEROID_MINER",
                    SatelliteMiner::new,
                    SatelliteMiner.CODEC,
                    ItemPools.POOL_SAT_MINER);
    public static final SatelliteType LUNAR_MINER =
            ntm(
                    "lunar_miner",
                    "satellite_miner_lunar",
                    "LUNAR_MINER",
                    SatelliteLunarMiner::new,
                    SatelliteLunarMiner.CODEC,
                    ItemPools.POOL_SAT_LUNAR);
    public static final SatelliteType HORIZONS =
            ntm(
                    "horizons",
                    "sat_gerald",
                    "PAYLOAD_UNKNOWN",
                    SatelliteHorizons::new,
                    SatelliteHorizons.CODEC,
                    null);
    public static final SatelliteType PRECISION_LASER =
            ntm(
                    "precision_laser",
                    "satellite_precision_laser",
                    "ORBITAL_TATOO_REMOVER",
                    SatellitePrecisionLaser::new,
                    SatellitePrecisionLaser.CODEC,
                    null);
    public static final SatelliteType DETECTOR =
            ntm(
                    "detector",
                    "satellite_detector",
                    "UWB_EMISSION_DETECTOR",
                    SatelliteDetector::new,
                    SatelliteDetector.CODEC,
                    null);
    public static final SatelliteType RAY_SCAN =
            ntm(
                    "ray_scan",
                    "satellite_ray_scan",
                    "NB_RAY_SCANNER",
                    SatelliteRayScan::new,
                    SatelliteRayScan.CODEC,
                    null);
    public static final SatelliteType SCIENCE =
            ntm(
                    "science",
                    "satellite_science",
                    "SCIENCE_PROBE",
                    SatelliteScience::new,
                    SatelliteScience.CODEC,
                    null);

    public static final Codec<SatelliteType> CODEC = REGISTRY.codec();
    public static final StreamCodec<ByteBuf, SatelliteType> STREAM_CODEC = REGISTRY.streamCodec();
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
    private final Identifier itemId;
    private final String orbitName;
    private final Supplier<? extends Satellite> factory;
    private final MapCodec<? extends Satellite> codec;
    private final @Nullable String cargo;
    private @Nullable Item resolved;

    private SatelliteType(
            String name,
            Identifier itemId,
            String orbitName,
            Supplier<? extends Satellite> factory,
            MapCodec<? extends Satellite> codec,
            @Nullable String cargo) {
        this.name = name;
        this.itemId = itemId;
        this.orbitName = orbitName;
        this.factory = factory;
        this.codec = codec;
        this.cargo = cargo;
    }

    private static SatelliteType ntm(
            String name,
            String itemPath,
            String orbitName,
            Supplier<? extends Satellite> factory,
            MapCodec<? extends Satellite> codec,
            @Nullable String cargo) {
        return register(name, Library.id(itemPath), orbitName, factory, codec, cargo);
    }

    public static SatelliteType register(
            String name,
            Identifier item,
            String orbitName,
            Supplier<? extends Satellite> factory,
            MapCodec<? extends Satellite> codec,
            @Nullable String cargo) {
        return REGISTRY.add(new SatelliteType(name, item, orbitName, factory, codec, cargo));
    }

    public static List<SatelliteType> values() {
        return REGISTRY.values();
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
        return orbitName;
    }

    public String stationNameKey() {
        return Util.makeDescriptionId("item", itemId);
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
            cached = BuiltInRegistries.ITEM.getValue(itemId);
            if (cached == Items.AIR) throw new IllegalStateException(itemId + " is not registered");
            resolved = cached;
        }
        return cached;
    }

    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
