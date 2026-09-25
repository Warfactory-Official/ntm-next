// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.NuclearTech;
import com.hbm.client.gui.ScreenWikiRender;
import com.hbm.items.CreativeContents;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemRBMKPellet;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class WikiRenderCommand {

    private static final String NAME = "ntmwikirender";
    private static final String DIRECTORY = "wiki-block-renders-256";

    private WikiRenderCommand() {}

    public static <S> LiteralArgumentBuilder<S> build() {
        return LiteralArgumentBuilder.<S>literal(NAME)
                .executes(context -> usage())
                .then(
                        RequiredArgumentBuilder.<S, String>argument(
                                        "type", StringArgumentType.greedyString())
                                .executes(WikiRenderCommand::execute));
    }

    private static int usage() {
        Minecraft client = Minecraft.getInstance();
        client.player.sendSystemMessage(
                Component.translatable("commands.hbm.wikiRender.usageCall")
                        .withStyle(ChatFormatting.GREEN)
                        .append(" ")
                        .append(
                                Component.translatable("commands.hbm.wikiRender.usageDescription")
                                        .withStyle(ChatFormatting.LIGHT_PURPLE)));
        return 0;
    }

    private static <S> int execute(CommandContext<S> context) {
        String type = StringArgumentType.getString(context, "type").split("\\s+", 2)[0];
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return 0;
        NuclearTech.LOGGER.info("Taking a screenshot of {}", type);
        List<ItemStack> stacks = stacksForType(type);
        client.gui.setScreen(new ScreenWikiRender(stacks, type, DIRECTORY, 16));
        return stacks.size();
    }

    public static List<ItemStack> stacksForType(String type) {
        Map<Item, List<ItemStack>> variants = new IdentityHashMap<>();
        for (CreativeContents.Tab tab : CreativeContents.Tab.values()) {
            CreativeContents.accept(tab, stack -> add(variants, stack));
        }
        CreativeContents.acceptLegacyVanillaTabs((tab, stack) -> add(variants, stack.get()));

        List<ItemStack> result = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!"hbm".equals(id.getNamespace()) || ignored(id) || !matchesType(item, type))
                continue;

            if (item instanceof ItemRBMKPellet || item instanceof ItemDepletedFuel) {
                result.add(new ItemStack(item));
            } else {
                List<ItemStack> shown = variants.get(item);
                if (shown == null || shown.isEmpty()) result.add(new ItemStack(item));
                else shown.forEach(stack -> result.add(stack.copy()));
            }
        }
        return result;
    }

    private static boolean ignored(Identifier id) {
        String path = id.getPath();
        return path.equals("achievement_icon") || path.startsWith("achievement_icon_");
    }

    private static boolean matchesType(Item item, String type) {
        if (item.getClass().getSimpleName().equalsIgnoreCase(type)) return true;
        Block block = Block.byItem(item);
        return block != Blocks.AIR && block.getClass().getSimpleName().equalsIgnoreCase(type);
    }

    private static void add(Map<Item, List<ItemStack>> variants, ItemStack stack) {
        List<ItemStack> listed =
                variants.computeIfAbsent(stack.getItem(), ignored -> new ArrayList<>());
        for (ItemStack existing : listed) {
            if (existing.getCount() == stack.getCount()
                    && ItemStack.isSameItemSameComponents(existing, stack)) return;
        }
        listed.add(stack);
    }
}
