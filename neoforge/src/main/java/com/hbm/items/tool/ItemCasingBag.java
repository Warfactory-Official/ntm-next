// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuItemBox;
import com.hbm.inventory.container.ModMenus;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemCasingBag extends Item {

    public static final MenuItemBox.Layout LAYOUT =
            new MenuItemBox.Layout(
                    3,
                    5,
                    44,
                    18,
                    8,
                    100,
                    158,
                    176,
                    186,
                    6,
                    0xFFFFFF,
                    88,
                    Library.id("textures/gui/gui_casing_bag.png"));

    public ItemCasingBag(Properties properties) {
        super(properties);
    }

    public static boolean pushCasing(ItemStack bag, ItemStack casing, float amount) {
        if (casing.isEmpty()) return false;
        Map<String, Float> progress =
                new HashMap<>(bag.getOrDefault(ModDataComponents.CASING_PROGRESS.get(), Map.of()));
        String key = key(casing);
        float carried = progress.getOrDefault(key, 0F);
        boolean took = false;

        if (carried < 1F) {
            carried += amount;
            took = true;
        }

        if (carried >= 1F) {
            ItemStackContainer contents = new ItemStackContainer(null, bag, LAYOUT.slots());
            ItemStack toAdd = casing.copy();
            while (carried >= 1F) {
                if (!place(contents, toAdd)) break;
                carried -= 1F;
                took = true;
            }
            contents.setChanged();
        }

        progress.put(key, carried);
        bag.set(ModDataComponents.CASING_PROGRESS.get(), Map.copyOf(progress));
        return took;
    }

    private static boolean place(ItemStackContainer contents, ItemStack toAdd) {
        boolean any = false;
        for (int slot = 0; slot < contents.getContainerSize() && toAdd.getCount() > 0; slot++) {
            ItemStack held = contents.getItem(slot);
            if (held.isEmpty() || !ItemStack.isSameItemSameComponents(held, toAdd)) continue;
            int room = Math.min(toAdd.getCount(), held.getMaxStackSize() - held.getCount());
            if (room <= 0) continue;
            toAdd.shrink(room);
            held.grow(room);
            any = true;
        }
        for (int slot = 0; slot < contents.getContainerSize() && toAdd.getCount() > 0; slot++) {
            if (!contents.getItem(slot).isEmpty()) continue;
            contents.setItem(slot, toAdd.copy());
            toAdd.setCount(0);
            return true;
        }
        return any;
    }

    private static String key(ItemStack casing) {
        return BuiltInRegistries.ITEM.getKey(casing.getItem()).toString();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new MenuItemBox(
                                            ModMenus.CASING_BAG.get(),
                                            id,
                                            inv,
                                            LAYOUT,
                                            new ItemStackContainer(opener, stack, LAYOUT.slots())),
                            stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
