// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.items.storage.BasicStorageCell;
import appeng.items.storage.ViewCellItem;
import appeng.items.tools.NetworkToolItem;
import appeng.items.tools.powered.ColorApplicatorItem;
import appeng.items.tools.powered.MatterCannonItem;
import appeng.items.tools.powered.PortableCellItem;
import appeng.items.tools.powered.WirelessCraftingTerminalItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.cells.BasicCellInventory;
import com.hbm.config.BalanceConfig;
import com.hbm.inventory.ContentFields;
import com.hbm.inventory.InventorySlotBinding;
import com.hbm.inventory.ItemContents;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class AE2ItemContents {
    private static final ItemContents.Field STORED = new StorageField();
    private static final ItemContents.Field UPGRADES =
            new ContentFields.Slots(AEComponents.UPGRADES, true);
    private static final ItemContents.Field CRAFTING =
            new ContentFields.Slots(AEComponents.CRAFTING_INV, true);
    private static final ItemContents.Adapter CELL =
            new ItemContents.Adapter(true, UPGRADES, STORED) {
                @Override
                public boolean radiationShielded(ItemInstance parent) {
                    return BalanceConfig.enableLBSM && BalanceConfig.enableLBSMSafeMEDrives;
                }
            };
    private static final ItemContents.Adapter TOOL =
            new ItemContents.Adapter(true, UPGRADES, STORED);
    private static final ItemContents.Adapter CARD = new ItemContents.Adapter(true, UPGRADES);
    private static final Map<Class<?>, ItemContents.Adapter> SHAPES =
            Map.of(
                    BasicStorageCell.class, CELL,
                    PortableCellItem.class, CELL,
                    MatterCannonItem.class, TOOL,
                    ColorApplicatorItem.class, TOOL,
                    ViewCellItem.class, CARD,
                    WirelessTerminalItem.class, CARD,
                    WirelessCraftingTerminalItem.class,
                            new ItemContents.Adapter(true, UPGRADES, CRAFTING),
                    NetworkToolItem.class, new ItemContents.Adapter(true));

    private AE2ItemContents() {}

    public static void register() {
        SHAPES.forEach(ItemContents::register);
    }

    private static final class StorageField extends ItemContents.Field {
        private StorageField() {
            super(true);
        }

        @Override
        public boolean present(ItemInstance parent) {
            var contents = parent.get(AEComponents.STORAGE_CELL_INV);
            return contents != null && !contents.isEmpty();
        }

        @Override
        public void visit(
                ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
            for (GenericStack held : parent.get(AEComponents.STORAGE_CELL_INV))
                if (held.amount() > 0 && held.what() instanceof AEItemKey item)
                    reader.accept(item.getReadOnlyStack(), held.amount());
        }

        @Override
        public boolean rewrite(ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
            List<GenericStack> contents = parent.get(AEComponents.STORAGE_CELL_INV);
            List<GenericStack> replacement = null;
            for (int slot = 0; slot < contents.size(); slot++) {
                GenericStack held = contents.get(slot);
                if (held.amount() <= 0
                        || !(held.what() instanceof AEItemKey item)
                        || !editor.selects(item.getReadOnlyStack())) continue;
                ItemStack child = item.toStack();
                if (!editor.visit(child)) continue;
                if (replacement == null) replacement = new ArrayList<>(contents);
                replacement.set(
                        slot,
                        child.isEmpty()
                                ? null
                                : new GenericStack(AEItemKey.of(child), held.amount()));
            }
            if (replacement == null) return false;

            var amounts = new Object2LongOpenHashMap<AEKey>(replacement.size());
            for (GenericStack held : replacement)
                if (held != null) amounts.mergeLong(held.what(), held.amount(), Math::addExact);
            var compacted = new ArrayList<GenericStack>(amounts.size());
            for (var iterator = amounts.object2LongEntrySet().fastIterator();
                    iterator.hasNext(); ) {
                var held = iterator.next();
                compacted.add(new GenericStack(held.getKey(), held.getLongValue()));
            }
            if (compacted.isEmpty()) parent.remove(AEComponents.STORAGE_CELL_INV);
            else parent.set(AEComponents.STORAGE_CELL_INV, List.copyOf(compacted));
            return true;
        }
    }

    public static void persistArmedChange(
            BasicCellInventory inventory, ItemStack cell, AEKey key, long moved, Actionable mode) {
        if (mode != Actionable.MODULATE || moved == 0 || !InventorySlotBinding.isBound(cell))
            return;
        if (key instanceof AEItemKey item
                        && (ItemContents.minimumDeadline(item.getReadOnlyStack()) != Long.MAX_VALUE
                                || ItemContents.references(item.getReadOnlyStack()).length != 0
                                || ItemContents.opaqueReferences(item.getReadOnlyStack()))
                || ItemContents.minimumDeadline(cell) != Long.MAX_VALUE) inventory.persist();
    }
}
