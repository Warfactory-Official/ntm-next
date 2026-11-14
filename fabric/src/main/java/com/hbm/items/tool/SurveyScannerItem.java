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

package com.hbm.items.tool;

import com.hbm.blockentity.BedrockOreBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalBlock;

public final class SurveyScannerItem extends Item {

    public enum Deposit {
        OIL("foundOil", ChatFormatting.BLACK),
        BEDROCK_OIL("foundBedrockOil", ChatFormatting.BLACK),
        COLTAN("foundColtan", ChatFormatting.GOLD),
        DEPTH("foundDepthRock", ChatFormatting.GRAY),
        SCHIST("foundSchist", ChatFormatting.DARK_AQUA),
        AUSTRALIUM("foundAustralium", ChatFormatting.YELLOW);

        private final Component message;

        Deposit(String key, ChatFormatting color) {
            message = Component.translatable("chat.surveyScanner." + key).withStyle(color);
        }

        public Component message() {
            return message;
        }
    }

    public record Survey(Set<Deposit> deposits, ItemStack bedrockOre, List<Component> lines) {}

    private record Alias(Supplier<? extends Block> block, Deposit deposit) {}

    private record ExtraLine(Component message, List<Supplier<? extends Block>> blocks) {}

    private record Classification(
            Map<Block, Deposit> deposits, Map<Block, Integer> lines, List<Component> messages) {}

    private static final Deposit[] ORDER = Deposit.values();
    private static final List<Alias> ALIASES = new ArrayList<>();
    private static final List<ExtraLine> EXTRA_LINES = new ArrayList<>();
    private static Classification classification;

    public SurveyScannerItem(Properties properties) {
        super(properties);
    }

    public static void registerAlias(Supplier<? extends Block> block, Deposit deposit) {
        requireRegistration();
        ALIASES.add(new Alias(block, deposit));
    }

    public static void registerLine(
            String key, ChatFormatting color, List<Supplier<? extends Block>> blocks) {
        requireRegistration();
        EXTRA_LINES.add(
                new ExtraLine(Component.translatable(key).withStyle(color), List.copyOf(blocks)));
    }

    private static void requireRegistration() {
        if (classification != null)
            throw new IllegalStateException("Survey classifications are already in use");
    }

    private static Classification classification() {
        if (classification != null) return classification;
        var deposits = new IdentityHashMap<Block, Deposit>();
        deposits.put(ModBlocks.ORE_OIL.get(), Deposit.OIL);
        deposits.put(ModBlocks.ORE_BEDROCK_OIL.get(), Deposit.BEDROCK_OIL);
        deposits.put(ModBlocks.ORE_COLTAN.get(), Deposit.COLTAN);
        deposits.put(ModBlocks.STONE_DEPTH.get(), Deposit.DEPTH);
        deposits.put(ModBlocks.STONE_DEPTH_NETHER.get(), Deposit.DEPTH);
        deposits.put(ModBlocks.STONE_GNEISS.get(), Deposit.SCHIST);
        deposits.put(ModBlocks.ORE_AUSTRALIUM.get(), Deposit.AUSTRALIUM);
        for (var alias : ALIASES) deposits.put(alias.block().get(), alias.deposit());

        var lines = new IdentityHashMap<Block, Integer>();
        var messages = new ArrayList<Component>(EXTRA_LINES.size());
        for (int index = 0; index < EXTRA_LINES.size(); index++) {
            var line = EXTRA_LINES.get(index);
            messages.add(line.message());
            for (var block : line.blocks()) lines.put(block.get(), index);
        }
        classification =
                new Classification(
                        Collections.unmodifiableMap(deposits),
                        Collections.unmodifiableMap(lines),
                        List.copyOf(messages));
        return classification;
    }

    public static Survey scan(ServerLevel level, BlockPos origin) {
        var lookup = classification();
        var deposits = EnumSet.noneOf(Deposit.class);
        boolean[] foundLines = new boolean[lookup.messages().size()];
        var cursor = new BlockPos.MutableBlockPos();
        int lowerBound = level.getMinY() + 1;
        int firstY = origin.getY() + 15;
        if (firstY > level.getMaxY()) {
            int skippedPairs = (firstY - level.getMaxY() + 1) / 2;
            firstY -= skippedPairs * 2;
        }
        int sampleParity = firstY & 1;

        if (firstY > lowerBound) {
            for (int xIndex = -5; xIndex <= 5; xIndex++) {
                int x = origin.getX() + xIndex * 5;
                for (int zIndex = -5; zIndex <= 5; zIndex++) {
                    int z = origin.getZ() + zIndex * 5;
                    cursor.set(x, firstY, z);
                    if (!level.isInValidBounds(cursor)) continue;
                    var chunk = level.getChunk(x >> 4, z >> 4);
                    for (int y = firstY; y > lowerBound; ) {
                        var section = chunk.getSection(chunk.getSectionIndex(y));
                        if (section.hasOnlyAir()) {
                            int sectionFloor = y & ~15;
                            y = sectionFloor - 2 + sampleParity;
                            continue;
                        }
                        var block = section.getBlockState(x & 15, y & 15, z & 15).getBlock();
                        var deposit = lookup.deposits().get(block);
                        if (deposit != null) deposits.add(deposit);
                        var line = lookup.lines().get(block);
                        if (line != null) foundLines[line] = true;
                        y -= 2;
                    }
                }
            }
        }

        ItemStack bedrockOre = ItemStack.EMPTY;
        for (int xIndex = -5; xIndex <= 5; xIndex++) {
            for (int zIndex = -5; zIndex <= 5; zIndex++) {
                cursor.set(origin.getX() + xIndex * 2, level.getMinY(), origin.getZ() + zIndex * 2);
                if (level.getBlockState(cursor).is(ModBlocks.ORE_BEDROCK.get())) {
                    bedrockOre = ((BedrockOreBlockEntity) level.getBlockEntity(cursor)).resource;
                }
            }
        }
        var messages = new ArrayList<Component>();
        for (int index = 0; index < foundLines.length; index++) {
            if (foundLines[index]) messages.add(lookup.messages().get(index));
        }
        return new Survey(
                Collections.unmodifiableSet(deposits), bedrockOre.copy(), List.copyOf(messages));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator()) return InteractionResult.PASS;
        if (level instanceof ServerLevel serverLevel) {
            var result = scan(serverLevel, player.blockPosition());
            if (result.deposits().contains(Deposit.OIL))
                player.sendSystemMessage(Deposit.OIL.message());
            for (var line : result.lines()) player.sendSystemMessage(line);
            for (int index = 1; index < ORDER.length; index++) {
                var deposit = ORDER[index];
                if (result.deposits().contains(deposit))
                    player.sendSystemMessage(deposit.message());
            }
            if (!result.bedrockOre().isEmpty()) {
                player.sendSystemMessage(
                        Component.translatable(
                                        "chat.surveyScanner.foundBedrockOreFor",
                                        result.bedrockOre().getHoverName())
                                .withStyle(ChatFormatting.RED));
            }
        }
        return accepted(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null
                || player.isSpectator()
                || !context.getLevel()
                        .getBlockState(context.getClickedPos())
                        .is(ModBlocks.BLOCK_BERYLLIUM.get())) {
            return InteractionResult.PASS;
        }
        boolean hasKit = false;
        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.ENTANGLEMENT_KIT.get())) {
                hasKit = true;
                break;
            }
        }
        if (!hasKit) return InteractionResult.PASS;

        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.level().dimension() == Level.END && !serverPlayer.seenCredits) {
                serverPlayer.showEndCredits();
            } else {
                var portal = (EndPortalBlock) Blocks.END_PORTAL;
                var transition =
                        portal.getPortalDestination(
                                serverPlayer.level(), serverPlayer, context.getClickedPos());
                if (transition != null) serverPlayer.teleport(transition);
            }
        }
        return accepted(context.getItemInHand());
    }

    private static InteractionResult accepted(ItemStack stack) {
        return InteractionResult.SUCCESS;
    }
}
