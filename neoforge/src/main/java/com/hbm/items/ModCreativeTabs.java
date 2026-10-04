// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.Library;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {

    public static RegistryHandle<CreativeModeTab> PARTS;
    public static RegistryHandle<CreativeModeTab> CONTROL;
    public static RegistryHandle<CreativeModeTab> TEMPLATES;
    public static RegistryHandle<CreativeModeTab> BLOCKS;
    public static RegistryHandle<CreativeModeTab> MACHINES;
    public static RegistryHandle<CreativeModeTab> NUKES;
    public static RegistryHandle<CreativeModeTab> MISSILES;
    public static RegistryHandle<CreativeModeTab> WEAPONS;
    public static RegistryHandle<CreativeModeTab> CONSUMABLES;

    private ModCreativeTabs() {}

    public static void register() {
        PARTS =
                register(
                        "parts",
                        CreativeContents.Tab.PARTS,
                        () -> new ItemStack(ModItems.ingot(Mats.MAT_URANIUM)));
        CONTROL =
                register(
                        "control",
                        CreativeContents.Tab.CONTROL,
                        () -> new ItemStack(ModItems.PELLET_RTG));
        TEMPLATES =
                register(
                        "templates",
                        CreativeContents.Tab.TEMPLATES,
                        () -> new ItemStack(ModItems.BLUEPRINTS),
                        NeoForgeRegistrar.searchBar());
        BLOCKS =
                register(
                        "blocks",
                        CreativeContents.Tab.BLOCKS,
                        () -> new ItemStack(ModBlocks.ORE_URANIUM));
        MACHINES =
                register(
                        "machines",
                        CreativeContents.Tab.MACHINES,
                        () -> new ItemStack(ModBlocks.PWR_CONTROLLER));
        NUKES =
                register(
                        "nukes",
                        CreativeContents.Tab.NUKES,
                        () -> new ItemStack(ModBlocks.NUKE_MAN),
                        builder ->
                                builder.backgroundTexture(
                                        Library.id(
                                                "textures/gui/container/creative_inventory/tab_nuke.png")));
        MISSILES =
                register(
                        "missiles",
                        CreativeContents.Tab.MISSILES,
                        () -> new ItemStack(ModItems.MISSILE_NUCLEAR));
        WEAPONS =
                register(
                        "weapons",
                        CreativeContents.Tab.WEAPONS,
                        () -> new ItemStack(ModItems.GUN_GREASEGUN));
        CONSUMABLES =
                register(
                        "consumables",
                        CreativeContents.Tab.CONSUMABLES,
                        () -> new ItemStack(ModItems.BOTTLE_NUKA));

        CreativeContents.acceptLegacyVanillaTabs(
                (tab, entry) -> NeoForgeRegistrar.vanillaTabEntries.add(Map.entry(tab, entry)));
    }

    private static RegistryHandle<CreativeModeTab> register(
            String name, CreativeContents.Tab tab, Supplier<ItemStack> icon) {
        return register(name, tab, icon, builder -> {});
    }

    private static RegistryHandle<CreativeModeTab> register(
            String name,
            CreativeContents.Tab tab,
            Supplier<ItemStack> icon,
            Consumer<CreativeModeTab.Builder> extra) {
        return NeoForgeRegistrar.registerCreativeModeTab(
                name,
                builder -> {
                    builder.title(Component.translatable("itemGroup.hbm." + name))
                            .icon(icon)
                            .displayItems(
                                    (params, output) ->
                                            CreativeContents.accept(tab, output::accept));
                    extra.accept(builder);
                });
    }
}
