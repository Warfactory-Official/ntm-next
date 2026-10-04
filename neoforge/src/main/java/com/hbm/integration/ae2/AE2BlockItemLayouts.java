// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.hbm.inventory.BlockItemContents;
import com.hbm.inventory.StorageLayouts.Field;
import com.hbm.inventory.StorageLayouts.Format;
import com.hbm.inventory.StorageLayouts.Packed;
import com.hbm.inventory.StorageLayouts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class AE2BlockItemLayouts {
    private record Path(String field, Format format, @Nullable String itemKey, int ordinal) {}

    private AE2BlockItemLayouts() {}

    public static void register() {
        for (var entry : StorageLayouts.BLOCK_TYPES.entrySet()) {
            Identifier type = Identifier.parse(entry.getKey());
            if (type.getNamespace().equals("ae2"))
                BlockItemContents.register(type, new NativeLayout(type, entry.getValue().packed()));
        }
    }

    private record NativeLayout(Identifier type, Packed definition)
            implements BlockItemContents.Layout {
        @Override
        public CustomData capture(CompoundTag nativeTag) {
            return AE2BlockItemLayouts.capture(type, nativeTag);
        }

        @Override
        public List<BlockItemContents.Entry> index(CustomData physical, RegistryOps<Tag> ops) {
            return AE2BlockItemLayouts.index(type, physical, ops);
        }

        @Override
        public void patch(
                CompoundTag physical,
                Object path,
                ItemStack replacement,
                long amount,
                RegistryOps<Tag> ops) {
            AE2BlockItemLayouts.patch(physical, (Path) path, replacement, amount, ops);
        }

        @Override
        public void strip(ValueOutput nativeTag) {
            for (Field field : definition.fields()) nativeTag.discard(field.name());
        }

        @Override
        public void merge(CompoundTag nativeTag, CustomData physical) {
            CompoundTag payload = physical.copyTag();
            for (Field field : definition.fields()) {
                Tag value = payload.get(field.name());
                if (value != null) nativeTag.put(field.name(), value.copy());
            }
        }
    }

    public static @Nullable Packed layout(Identifier type) {
        if (!type.getNamespace().equals("ae2")) return null;
        var declaration = StorageLayouts.BLOCK_TYPES.get(type.toString());
        return declaration == null ? null : declaration.packed();
    }

    public static CustomData capture(Identifier type, CompoundTag nativeTag) {
        Packed layout = layout(type);
        if (layout == null || layout.coreOnly() && !nativeTag.getBooleanOr("core", false))
            return CustomData.EMPTY;
        CompoundTag payload = new CompoundTag();
        for (Field field : layout.fields()) {
            Tag data = nativeTag.get(field.name());
            if (data != null) payload.put(field.name(), data.copy());
        }
        if (payload.isEmpty()) return CustomData.EMPTY;
        if (layout.coreOnly()) payload.putBoolean("core", true);
        return CustomData.of(payload);
    }

    public static void strip(Identifier type, CompoundTag nativeTag) {
        Packed layout = layout(type);
        if (layout == null || layout.coreOnly() && !nativeTag.getBooleanOr("core", false)) return;
        for (Field field : layout.fields()) nativeTag.remove(field.name());
    }

    public static List<BlockItemContents.Entry> index(
            Identifier type, CustomData payload, RegistryOps<Tag> ops) {
        Packed layout = layout(type);
        if (layout == null)
            throw new IllegalArgumentException("Unsupported AE block-item type: " + type);
        CompoundTag data = payload.copyTag();
        if (layout.coreOnly() && !data.getBooleanOr("core", false)) return List.of();
        ArrayList<BlockItemContents.Entry> result = new ArrayList<>();
        for (Field field : layout.fields()) {
            Tag contents = data.get(field.name());
            if (contents == null) continue;
            switch (field.format()) {
                case INV_MAP -> indexInventory(field, (CompoundTag) contents, ops, result);
                case ITEM_LIST -> indexItems(field, (ListTag) contents, ops, result);
                case KEY_LIST, KEY_MAP -> indexKeys(field, (ListTag) contents, ops, result);
            }
        }
        return List.copyOf(result);
    }

    private static void indexInventory(
            Field field,
            CompoundTag inventory,
            RegistryOps<Tag> ops,
            List<BlockItemContents.Entry> result) {
        for (int slot = field.firstSlot(); slot < field.capacity(); slot++) {
            String key = "item" + slot;
            CompoundTag item = inventory.getCompoundOrEmpty(key);
            if (!item.isEmpty()) indexItem(field, key, -1, item, ops, result);
        }
    }

    private static void indexItems(
            Field field,
            ListTag inventory,
            RegistryOps<Tag> ops,
            List<BlockItemContents.Entry> result) {
        int[] last = new int[field.capacity()];
        Arrays.fill(last, -1);
        for (int ordinal = 0; ordinal < inventory.size(); ordinal++) {
            if (!(inventory.get(ordinal) instanceof CompoundTag item)) continue;
            int slot = item.getIntOr("Slot", 0);
            if (slot >= field.firstSlot() && slot < last.length) last[slot] = ordinal;
        }
        for (int ordinal = 0; ordinal < inventory.size(); ordinal++) {
            if (!(inventory.get(ordinal) instanceof CompoundTag item)) continue;
            int slot = item.getIntOr("Slot", 0);
            if (slot >= field.firstSlot() && slot < last.length && last[slot] == ordinal)
                indexItem(field, null, ordinal, item, ops, result);
        }
    }

    private static void indexItem(
            Field field,
            @Nullable String key,
            int ordinal,
            CompoundTag item,
            RegistryOps<Tag> ops,
            List<BlockItemContents.Entry> result) {
        if (item.isEmpty()) return;
        ItemStackTemplate template =
                ItemStackTemplate.MAP_CODEC.codec().parse(ops, item).getOrThrow();
        if (template.count() > 0)
            result.add(
                    new BlockItemContents.Entry(
                            template.withCount(1),
                            template.count(),
                            new Path(field.name(), field.format(), key, ordinal)));
    }

    private static void indexKeys(
            Field field,
            ListTag inventory,
            RegistryOps<Tag> ops,
            List<BlockItemContents.Entry> result) {
        int count = Math.min(inventory.size(), field.capacity());
        for (int ordinal = field.firstSlot(); ordinal < count; ordinal++) {
            if (!(inventory.get(ordinal) instanceof CompoundTag item)
                    || !item.contains(AEKey.TYPE_FIELD)) continue;
            GenericStack generic = GenericStack.CODEC.parse(ops, item).getOrThrow();
            if (generic.amount() > 0 && generic.what() instanceof AEItemKey key)
                result.add(
                        new BlockItemContents.Entry(
                                ItemStackTemplate.fromNonEmptyStack(key.toStack()),
                                generic.amount(),
                                new Path(field.name(), field.format(), null, ordinal)));
        }
    }

    private static void patch(
            CompoundTag payload,
            Path entry,
            ItemStack replacement,
            long amount,
            RegistryOps<Tag> ops) {
        assert replacement.isEmpty() || replacement.getCount() == 1;
        CompoundTag changed;
        if (replacement.isEmpty()) changed = new CompoundTag();
        else if (entry.format() == Format.KEY_LIST || entry.format() == Format.KEY_MAP) {
            changed =
                    (CompoundTag)
                            AEKey.CODEC.encodeStart(ops, AEItemKey.of(replacement)).getOrThrow();
            changed.putLong(
                    GenericStack.AMOUNT_FIELD, Math.multiplyExact(amount, replacement.getCount()));
        } else {
            ItemStack full =
                    replacement.copyWithCount(
                            Math.toIntExact(Math.multiplyExact(amount, replacement.getCount())));
            changed = (CompoundTag) ItemStack.MAP_CODEC.codec().encodeStart(ops, full).getOrThrow();
        }
        if (entry.format() == Format.INV_MAP) {
            CompoundTag inventory = payload.getCompound(entry.field()).orElseThrow();
            if (replacement.isEmpty()) inventory.remove(entry.itemKey());
            else inventory.put(entry.itemKey(), changed);
        } else {
            ListTag inventory = payload.getList(entry.field()).orElseThrow();
            if (replacement.isEmpty()
                    && (entry.format() == Format.ITEM_LIST || entry.format() == Format.KEY_MAP)) {
                inventory.remove(entry.ordinal());
                return;
            }
            if (entry.format() == Format.ITEM_LIST) {
                CompoundTag previous = (CompoundTag) inventory.get(entry.ordinal());
                changed.putInt("Slot", previous.getIntOr("Slot", 0));
            }
            inventory.set(entry.ordinal(), changed);
        }
    }
}
