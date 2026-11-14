/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPoolIds;
import com.hbm.items.ModItems;
import com.hbm.registration.OpenRegistry;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
                    "SatelliteMapper",
                    MapperSatellite::new,
                    MapperSatellite.CODEC,
                    null);
    public static final SatelliteType SCANNER =
            ntm(
                    "scanner",
                    "satellite_scanner",
                    "DEPTH_SCANNER",
                    "SatelliteScanner",
                    SatelliteScanner::new,
                    SatelliteScanner.CODEC,
                    null);
    public static final SatelliteType RADAR =
            ntm(
                    "radar",
                    "satellite_radar",
                    "LEO_RADAR",
                    "SatelliteRadar",
                    RadarSatellite::new,
                    RadarSatellite.CODEC,
                    null);
    public static final SatelliteType DEATH_RAY =
            ntm(
                    "death_ray",
                    "satellite_death_ray",
                    "ORBITAL_FUN_PLATFORM_:)",
                    "SatelliteDeathRay",
                    DeathRaySatellite::new,
                    DeathRaySatellite.CODEC,
                    null);
    public static final SatelliteType RESONATOR =
            ntm(
                    "resonator",
                    "satellite_xenium_resonator",
                    "XEN_RELAY",
                    "SatelliteResonator",
                    SatelliteResonator::new,
                    SatelliteResonator.CODEC,
                    null);
    public static final SatelliteType RELAY =
            ntm(
                    "relay",
                    "satellite_relay",
                    "DIMENSIONAL_RELAY",
                    "SatelliteRelay",
                    SatelliteRelay::new,
                    SatelliteRelay.CODEC,
                    null);
    public static final SatelliteType MINER =
            ntm(
                    "miner",
                    "satellite_miner_astro",
                    "ASTEROID_MINER",
                    "SatelliteMiner",
                    MinerSatellite::new,
                    MinerSatellite.CODEC,
                    ItemPoolIds.POOL_SAT_MINER);
    public static final SatelliteType LUNAR_MINER =
            ntm(
                    "lunar_miner",
                    "satellite_miner_lunar",
                    "LUNAR_MINER",
                    "SatelliteLunarMiner",
                    SatelliteLunarMiner::new,
                    SatelliteLunarMiner.CODEC,
                    ItemPoolIds.POOL_SAT_LUNAR);
    public static final SatelliteType HORIZONS =
            ntm(
                    "horizons",
                    "sat_gerald",
                    "PAYLOAD_UNKNOWN",
                    "SatelliteHorizons",
                    HorizonsSatellite::new,
                    HorizonsSatellite.CODEC,
                    null);
    public static final SatelliteType PRECISION_LASER =
            ntm(
                    "precision_laser",
                    "satellite_precision_laser",
                    "ORBITAL_TATOO_REMOVER",
                    "SatellitePrecisionLaser",
                    PrecisionLaserSatellite::new,
                    PrecisionLaserSatellite.CODEC,
                    null);
    public static final SatelliteType DETECTOR =
            ntm(
                    "detector",
                    "satellite_detector",
                    "UWB_EMISSION_DETECTOR",
                    "SatelliteDetector",
                    DetectorSatellite::new,
                    DetectorSatellite.CODEC,
                    null);
    public static final SatelliteType RAY_SCAN =
            ntm(
                    "ray_scan",
                    "satellite_ray_scan",
                    "NB_RAY_SCANNER",
                    "SatelliteRayScan",
                    RayScanSatellite::new,
                    RayScanSatellite.CODEC,
                    null);
    public static final SatelliteType SCIENCE =
            ntm(
                    "science",
                    "satellite_science",
                    "SCIENCE_PROBE",
                    "SatelliteScience",
                    ScienceSatellite::new,
                    ScienceSatellite.CODEC,
                    null);

    public static final Codec<SatelliteType> CODEC = REGISTRY.codec();
    public static final StreamCodec<ByteBuf, SatelliteType> STREAM_CODEC = REGISTRY.streamCodec();
    public static final Codec<Satellite> DISPATCH_CODEC =
            CODEC.dispatch("type", Satellite::kind, SatelliteType::codec);
    public static final Codec<Satellite> PERSISTED_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            DISPATCH_CODEC.fieldOf("sat").forGetter(sat -> sat),
                                            Satellite.STATE_CODEC
                                                    .fieldOf("state")
                                                    .forGetter(Satellite::state))
                                    .apply(i, Satellite::restored));

    private final String name;
    private final Identifier itemId;
    private final String orbitName;
    private final String label;
    private final Supplier<? extends Satellite> factory;
    private final MapCodec<? extends Satellite> codec;
    private final @Nullable String cargo;
    private final Component stationName;
    private final List<Component> stationInfo;
    private @Nullable Item resolved;

    private SatelliteType(
            String name,
            Identifier itemId,
            String orbitName,
            String label,
            Supplier<? extends Satellite> factory,
            MapCodec<? extends Satellite> codec,
            @Nullable String cargo) {
        this.name = name;
        this.itemId = itemId;
        this.orbitName = orbitName;
        this.label = label;
        this.factory = factory;
        this.codec = codec;
        this.cargo = cargo;
        this.stationName = Component.translatable(Util.makeDescriptionId("item", itemId));
        this.stationInfo = List.of(stationName);
    }

    private static SatelliteType ntm(
            String name,
            String itemPath,
            String orbitName,
            String label,
            Supplier<? extends Satellite> factory,
            MapCodec<? extends Satellite> codec,
            @Nullable String cargo) {
        return register(name, RegistryUtil.id(itemPath), orbitName, label, factory, codec, cargo);
    }

    public static SatelliteType register(
            String name,
            Identifier item,
            String orbitName,
            String label,
            Supplier<? extends Satellite> factory,
            MapCodec<? extends Satellite> codec,
            @Nullable String cargo) {
        return REGISTRY.add(new SatelliteType(name, item, orbitName, label, factory, codec, cargo));
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

    public static @Nullable String cargoFor(Item item) {
        var kind = fromItem(item);
        return kind == null ? null : kind.cargo;
    }

    public Satellite create() {
        return factory.get();
    }

    public String orbitName() {
        return orbitName;
    }

    public String label() {
        return label;
    }

    public Component stationName() {
        return stationName;
    }

    public List<Component> stationInfo() {
        return stationInfo;
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
