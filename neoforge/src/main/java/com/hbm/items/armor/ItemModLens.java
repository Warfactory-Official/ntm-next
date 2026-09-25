// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ISatChip;
import com.hbm.packet.toclient.MarkerPayload;
import com.hbm.platform.Services;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteScanner;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

public class ItemModLens extends ItemArmorMod implements ISatChip {

    private static final int RANGE = 3;
    private static final int MAX_HITS = 100;
    private static final int EXPIRES_MILLIS = 15_000;
    private static final double MAX_DIST = 300D;

    public ItemModLens(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, false, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("satchip.frequency")
                        .append(": " + getFreq(stack))
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
                        .append(": " + getFreq(stack) + ")")
                        .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity instanceof ServerPlayer player)) return;
        ServerLevel level = player.level();
        ItemStack lens = ArmorModHandler.pryMod(armor, ArmorModHandler.EXTRA);
        if (!(SatelliteSavedData.get(level).getSatFromFreq(getFreq(lens))
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
                        Services.NETWORK.sendTo(
                                new MarkerPayload(color, EXPIRES_MILLIS, MAX_DIST, markers),
                                player));
    }

    private record Target(int chance, Component label, int color) {}

    private static final class Targets {

        static final Map<Block, Target> BY_BLOCK =
                Map.ofEntries(
                        Map.entry(
                                ModBlocks.ORE_ALEXANDRITE.get(),
                                labelled(1, "alexandrite", 0x00ffff)),
                        Map.entry(ModBlocks.ORE_OIL.get(), labelled(300, "oil", 0xa0a0a0)),
                        Map.entry(
                                ModBlocks.ORE_BEDROCK_OIL.get(),
                                labelled(300, "bedrock_oil", 0xa0a0a0)),
                        Map.entry(ModBlocks.ORE_COLTAN.get(), labelled(5, "coltan", 0xa0a000)),
                        Map.entry(ModBlocks.STONE_GNEISS.get(), labelled(5000, "schist", 0x8080ff)),
                        Map.entry(
                                ModBlocks.ORE_AUSTRALIUM.get(),
                                labelled(1000, "australium", 0xffff00)),
                        Map.entry(Blocks.END_PORTAL_FRAME, labelled(1, "end_portal", 0x40b080)),
                        Map.entry(
                                ModBlocks.VOLCANO_CORE.get(),
                                labelled(1, "volcano_core", 0xff4000)),
                        Map.entry(ModBlocks.PINK_LOG.get(), labelled(1, "pink_log", 0xff00ff)),
                        Map.entry(ModBlocks.BOBBLEHEAD.get(), labelled(1, "treasure", 0xff0000)),
                        Map.entry(ModBlocks.LOOT.get(), new Target(1, Component.empty(), 0x800000)),
                        Map.entry(
                                ModBlocks.CRATE_AMMO.get(),
                                new Target(1, Component.empty(), 0x800000)),
                        Map.entry(
                                ModBlocks.CRATE_CAN.get(),
                                new Target(1, Component.empty(), 0x800000)),
                        Map.entry(
                                ModBlocks.ORE_BEDROCK.get(), labelled(1, "bedrock_ore", 0xff0000)));

        private static Target labelled(int chance, String label, int color) {
            return new Target(
                    chance, Component.translatable("marker.hbm.neutrino_lens." + label), color);
        }
    }
}
