// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.Library;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
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

    public static void register(IRegistrar r) {

        PARTS =
                register(
                        r,
                        "parts",
                        CreativeContents.Tab.PARTS,
                        () -> new ItemStack(ModItems.ingot(Mats.MAT_URANIUM)));
        CONTROL =
                register(
                        r,
                        "control",
                        CreativeContents.Tab.CONTROL,
                        () -> new ItemStack(ModItems.PELLET_RTG));

        TEMPLATES =
                register(
                        r,
                        "templates",
                        CreativeContents.Tab.TEMPLATES,
                        () -> new ItemStack(ModItems.BLUEPRINTS),
                        r.searchBar());
        BLOCKS =
                register(
                        r,
                        "blocks",
                        CreativeContents.Tab.BLOCKS,
                        () -> new ItemStack(ModBlocks.ORE_URANIUM));
        MACHINES =
                register(
                        r,
                        "machines",
                        CreativeContents.Tab.MACHINES,
                        () -> new ItemStack(ModBlocks.PWR_CONTROLLER));

        NUKES =
                register(
                        r,
                        "nukes",
                        CreativeContents.Tab.NUKES,
                        () -> new ItemStack(ModBlocks.NUKE_MAN),
                        builder ->
                                builder.backgroundTexture(
                                        Library.id(
                                                "textures/gui/container/creative_inventory/tab_nuke.png")));
        MISSILES =
                register(
                        r,
                        "missiles",
                        CreativeContents.Tab.MISSILES,
                        () -> new ItemStack(ModItems.MISSILE_NUCLEAR));
        WEAPONS =
                register(
                        r,
                        "weapons",
                        CreativeContents.Tab.WEAPONS,
                        () -> new ItemStack(ModItems.GUN_GREASEGUN));
        CONSUMABLES =
                register(
                        r,
                        "consumables",
                        CreativeContents.Tab.CONSUMABLES,
                        () -> new ItemStack(ModItems.BOTTLE_NUKA));

        CreativeContents.acceptLegacyVanillaTabs(r::addToVanillaTab);
    }

    private static RegistryHandle<CreativeModeTab> register(
            IRegistrar r, String name, CreativeContents.Tab tab, Supplier<ItemStack> icon) {
        return register(r, name, tab, icon, builder -> {});
    }

    private static RegistryHandle<CreativeModeTab> register(
            IRegistrar r,
            String name,
            CreativeContents.Tab tab,
            Supplier<ItemStack> icon,
            Consumer<CreativeModeTab.Builder> extra) {
        return r.registerCreativeModeTab(
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
