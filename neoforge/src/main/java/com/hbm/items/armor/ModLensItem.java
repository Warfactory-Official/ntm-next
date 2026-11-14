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

package com.hbm.items.armor;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MarkerLayout;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteScanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ModLensItem extends ArmorModItem implements SatelliteFrequencyProvider {

    private static final int RANGE = 3;
    private static final int MAX_HITS = 100;
    private static final int EXPIRES_MILLIS = 15_000;
    private static final double MAX_DIST = 300D;

    private static final List<Map.Entry<Supplier<? extends Block>, Target>> ADDED =
            new ArrayList<>();
    private static final List<Map.Entry<Supplier<? extends Block>, Supplier<? extends Block>>>
            ALIASES = new ArrayList<>();

    public ModLensItem(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, false, false, false);
    }

    public static void addTarget(
            Supplier<? extends Block> block, int chance, Component label, int color) {
        ADDED.add(Map.entry(block, new Target(chance, label, color)));
    }

    public static void alias(Supplier<? extends Block> block, Supplier<? extends Block> like) {
        ALIASES.add(Map.entry(block, like));
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("satchip.frequency")
                        .append(": " + SatelliteFrequencyProvider.frequency(stack))
                        .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ")
                        .append(stack.getHoverName())
                        .append(" (")
                        .append(
                                Component.translatable(
                                        "item.hbm.neutrino_lens.description.frequency"))
                        .append(": " + SatelliteFrequencyProvider.frequency(stack) + ")")
                        .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean ticksServer() {
        return true;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity instanceof ServerPlayer player)) return;
        ServerLevel level = player.level();
        var lens = ArmorModHandler.mods(armor).template(ArmorModHandler.EXTRA);
        if (!(SatelliteSavedData.get(level)
                        .getSatFromFreq(SatelliteFrequencyProvider.frequency(lens))
                instanceof SatelliteScanner)) return;

        int cX = SectionPos.blockToSectionCoord(Mth.floor(player.getX()));
        int cZ = SectionPos.blockToSectionCoord(Mth.floor(player.getZ()));
        int bottom = level.getMinY();
        int top = Math.min(Math.max(Mth.floor(player.getY()) + 10, 64), level.getMaxY());
        int seg = bottom + (int) (level.getGameTime() % (top - bottom));

        Map<Block, Target> targets = Targets.BY_BLOCK;
        Map<Integer, Map<BlockPos, Component>> byColor = new LinkedHashMap<>();
        int hits = 0;
        scan:
        for (int chunkX = cX - RANGE; chunkX <= cX + RANGE; chunkX++) {
            for (int chunkZ = cZ - RANGE; chunkZ <= cZ + RANGE; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(seg));
                if (!section.maybeHas(state -> targets.containsKey(state.getBlock()))) continue;
                int sectionY = SectionPos.sectionRelative(seg);

                for (int ix = 0; ix < 16; ix++) {
                    for (int iz = 0; iz < 16; iz++) {
                        Target target =
                                targets.get(section.getBlockState(ix, sectionY, iz).getBlock());
                        if (target == null || player.getRandom().nextInt(target.chance) != 0)
                            continue;
                        byColor.computeIfAbsent(target.color, color -> new LinkedHashMap<>())
                                .put(
                                        new BlockPos((chunkX << 4) + ix, seg, (chunkZ << 4) + iz),
                                        target.label);
                        if (++hits > MAX_HITS) break scan;
                    }
                }
            }
        }
        byColor.forEach(
                (color, markers) ->
                        PacketWire.sendTo(
                                MarkerLayout.encode(color, EXPIRES_MILLIS, MAX_DIST, markers),
                                player));
    }

    private record Target(int chance, Component label, int color) {}

    private static final class Targets {

        static final Map<Block, Target> BY_BLOCK =
                new HashMap<>(
                        Map.ofEntries(
                                Map.entry(
                                        ModBlocks.ORE_ALEXANDRITE.get(),
                                        labelled(1, "alexandrite", 0x00ffff)),
                                Map.entry(ModBlocks.ORE_OIL.get(), labelled(300, "oil", 0xa0a0a0)),
                                Map.entry(
                                        ModBlocks.ORE_BEDROCK_OIL.get(),
                                        labelled(300, "bedrock_oil", 0xa0a0a0)),
                                Map.entry(
                                        ModBlocks.ORE_COLTAN.get(),
                                        labelled(5, "coltan", 0xa0a000)),
                                Map.entry(
                                        ModBlocks.STONE_GNEISS.get(),
                                        labelled(5000, "schist", 0x8080ff)),
                                Map.entry(
                                        ModBlocks.ORE_AUSTRALIUM.get(),
                                        labelled(1000, "australium", 0xffff00)),
                                Map.entry(
                                        Blocks.END_PORTAL_FRAME,
                                        labelled(1, "end_portal", 0x40b080)),
                                Map.entry(
                                        ModBlocks.VOLCANO_CORE.get(),
                                        labelled(1, "volcano_core", 0xff4000)),
                                Map.entry(
                                        ModBlocks.PINK_LOG.get(),
                                        labelled(1, "pink_log", 0xff00ff)),
                                Map.entry(
                                        ModBlocks.BOBBLEHEAD.get(),
                                        labelled(1, "treasure", 0xff0000)),
                                Map.entry(
                                        ModBlocks.LOOT.get(),
                                        new Target(1, Component.empty(), 0x800000)),
                                Map.entry(
                                        ModBlocks.CRATE_AMMO.get(),
                                        new Target(1, Component.empty(), 0x800000)),
                                Map.entry(
                                        ModBlocks.CRATE_CAN.get(),
                                        new Target(1, Component.empty(), 0x800000)),
                                Map.entry(
                                        ModBlocks.ORE_BEDROCK.get(),
                                        labelled(1, "bedrock_ore", 0xff0000))));

        static {
            for (Map.Entry<Supplier<? extends Block>, Target> added : ADDED) {
                Target clash = BY_BLOCK.putIfAbsent(added.getKey().get(), added.getValue());
                assert clash == null : added.getKey().get();
            }
            for (Map.Entry<Supplier<? extends Block>, Supplier<? extends Block>> alias : ALIASES) {
                Target like = BY_BLOCK.get(alias.getValue().get());
                if (like == null)
                    throw new IllegalStateException(alias.getValue().get() + " marks nothing");
                Target clash = BY_BLOCK.putIfAbsent(alias.getKey().get(), like);
                assert clash == null : alias.getKey().get();
            }
        }

        private static Target labelled(int chance, String label, int color) {
            return new Target(
                    chance, Component.translatable("marker.hbm.neutrino_lens." + label), color);
        }
    }
}
