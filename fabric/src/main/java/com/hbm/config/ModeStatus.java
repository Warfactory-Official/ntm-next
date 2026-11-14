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

package com.hbm.config;

import com.hbm.data.MobData;
import com.hbm.data.WorldData;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.items.machine.RtgFuelHandler;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ModeSettingsLayout;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.world.HbmPlacedFeatures;
import com.mojang.serialization.JsonOps;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModeStatus {
    public static final int MODE_528 = 1;
    public static final int EXPENSIVE = 2;
    public static final int RAMPANT = 4;
    public static final int MODE_328 = 8;

    public static volatile int clientBadges;
    public static volatile boolean clientRtgDecay = true;
    public static volatile boolean clientRtgScalePower;

    private ModeStatus() {}

    public static void sendTo(ServerPlayer player) {
        PacketWire.sendTo(encoded(player.level().getServer()), player);
    }

    public static void broadcast(MinecraftServer server) {
        PacketWire.sendToAll(encoded(server));
    }

    private static EncodedPayload encoded(MinecraftServer server) {
        boolean full528 =
                BalanceConfig.enable528
                        && BalanceConfig.enable528ReasimBoilers
                        && !WorldData.COLTAN_SPAWN.get()
                        && BalanceConfig.enable528BosniaSimulator
                        && WorldData.NETHER_BURN.get()
                        && BalanceConfig.enable528PressurizedRecipes
                        && WorldData.EXPLOSIVE_ENERGISTICS.get()
                        && BalanceConfig.enable528MachineGravity
                        && coltanRateAtMostTwo(
                                server.registryAccess()
                                        .lookupOrThrow(Registries.PLACED_FEATURE)
                                        .getValueOrThrow(HbmPlacedFeatures.ORE_COLTAN));
        boolean expensive =
                BalanceConfig.enableExpensiveMode
                        && !precisionAssemblerModified(
                                server.getResourceManager(),
                                server.getRecipeManager().getRecipes());
        boolean rampant =
                MobData.RAMPANT_MODE.get()
                        && MobData.rampantNaturalScoutSpawn()
                        && MobData.scoutThreshold() <= 0.1
                        && MobData.rampantExtendedTargetting()
                        && MobData.rampantDig()
                        && MobData.rampantGlyphidGuidance();
        var rtg = RtgFuelHandler.current();
        return ModeSettingsLayout.encode(
                badgeMask(full528, expensive, rampant) & ~MODE_328,
                rtg.decays(),
                rtg.scalesPower());
    }

    public static int badgeMask(boolean mode528, boolean expensive, boolean rampant) {
        return (mode528 ? MODE_528 : 0)
                | (expensive ? EXPENSIVE : 0)
                | (rampant ? RAMPANT : 0)
                | (mode528 && expensive && rampant ? MODE_328 : 0);
    }

    public static boolean coltanRateAtMostTwo(PlacedFeature feature) {
        long count = 1;
        for (var placement : feature.placement()) {
            if (placement instanceof CountPlacement repeat) {
                var encoded =
                        CountPlacement.CODEC
                                .codec()
                                .encodeStart(JsonOps.INSTANCE, repeat)
                                .getOrThrow();
                int maximum =
                        IntProviders.codec(0, 4096)
                                .parse(JsonOps.INSTANCE, encoded.getAsJsonObject().get("count"))
                                .getOrThrow()
                                .maxInclusive();
                count = Math.min(3, count * maximum);
            }
        }
        return count <= 2;
    }

    public static boolean precisionAssemblerModified(
            ResourceManager resources, Collection<RecipeHolder<?>> recipes) {
        Set<Identifier> original = new HashSet<>();
        resources
                .listPacks()
                .filter(ModeStatus::ownPack)
                .forEach(
                        pack ->
                                pack.listResources(
                                        PackType.SERVER_DATA,
                                        "hbm",
                                        "recipe/precision_assembler",
                                        (id, stream) -> {
                                            if (id.getPath().endsWith(".json")) original.add(id);
                                        }));
        for (Identifier id : original) {
            if (resources.getResource(id).filter(resource -> ownPack(resource.source())).isEmpty())
                return true;
        }
        for (var resource :
                resources
                        .listResources(
                                "recipe/precision_assembler", id -> id.getPath().endsWith(".json"))
                        .values()) {
            if (!ownPack(resource.source())) return true;
        }
        for (var recipe : recipes) {
            if (recipe.value().getType() != PrecAssRecipes.INSTANCE) continue;
            Identifier id = recipe.id().identifier().withPath(path -> "recipe/" + path + ".json");
            if (resources.getResource(id).filter(resource -> ownPack(resource.source())).isEmpty())
                return true;
        }
        return false;
    }

    private static boolean ownPack(PackResources pack) {
        return pack.packId().equals("hbm") || pack.packId().equals("mod/hbm");
    }

    public static void accept(int badges, boolean rtgDecay, boolean rtgScalePower) {
        clientBadges = badges | (badges == (MODE_528 | EXPENSIVE | RAMPANT) ? MODE_328 : 0);
        clientRtgDecay = rtgDecay;
        clientRtgScalePower = rtgScalePower;
    }

    public static void disconnected() {
        clientBadges = 0;
        clientRtgDecay = true;
        clientRtgScalePower = false;
    }
}
