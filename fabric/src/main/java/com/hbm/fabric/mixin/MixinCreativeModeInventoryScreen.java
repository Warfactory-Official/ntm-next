// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.registration.FabricRegistrar;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreen
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private EditBox searchBox;
    @Shadow private float scrollOffs;

    @Unique private Collection<ItemStack> hbm$indexed;
    @Unique private SearchTree<ItemStack> hbm$names;
    @Unique private SearchTree<ItemStack> hbm$tags;

    private MixinCreativeModeInventoryScreen(
            CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Shadow
    private void updateVisibleTags(String searchTerm) {
        throw new AssertionError();
    }

    @WrapOperation(
            method = {
                "refreshCurrentTabContents",
                "charTyped",
                "preeditUpdated",
                "keyPressed",
                "selectTab"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/CreativeModeTab;getType()Lnet/minecraft/world/item/CreativeModeTab$Type;"))
    private CreativeModeTab.Type hbm$searchTab(
            CreativeModeTab tab, Operation<CreativeModeTab.Type> original) {
        return FabricRegistrar.hasSearchBar(tab) ? CreativeModeTab.Type.SEARCH : original.call(tab);
    }

    @Inject(method = "refreshSearchResults", at = @At("HEAD"), cancellable = true)
    private void hbm$searchOwnItems(CallbackInfo ci) {
        if (!FabricRegistrar.hasSearchBar(selectedTab)) return;
        ci.cancel();
        menu.items.clear();
        String term = searchBox.getValue();
        Collection<ItemStack> items = selectedTab.getDisplayItems();
        if (term.isEmpty()) {
            menu.items.addAll(items);
        } else {
            if (hbm$indexed != items) hbm$index(items);
            SearchTree<ItemStack> tree;
            if (term.startsWith("#")) {
                term = term.substring(1);
                tree = hbm$tags;
                updateVisibleTags(term);
            } else {
                tree = hbm$names;
            }
            menu.items.addAll(tree.search(term.toLowerCase(Locale.ROOT)));
        }
        scrollOffs = 0F;
        menu.scrollTo(0F);
    }

    @Unique
    private void hbm$index(Collection<ItemStack> items) {
        List<ItemStack> list = List.copyOf(items);
        Item.TooltipContext context = Item.TooltipContext.of(minecraft.level.registryAccess());
        TooltipFlag flag = TooltipFlag.Default.NORMAL.asCreative();
        hbm$names =
                new FullTextSearchTree<>(
                        stack ->
                                stack.getTooltipLines(context, null, flag).stream()
                                        .map(
                                                line ->
                                                        ChatFormatting.stripFormatting(
                                                                        line.getString())
                                                                .trim())
                                        .filter(line -> !line.isEmpty()),
                        stack ->
                                stack
                                        .typeHolder()
                                        .unwrapKey()
                                        .map(ResourceKey::identifier)
                                        .stream(),
                        list);
        hbm$tags = new IdSearchTree<>(stack -> stack.tags().map(TagKey::location), list);
        hbm$indexed = items;
    }
}
