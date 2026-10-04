// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.ArrayList;
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

public final class VanillaBlockItemLayouts {
    private VanillaBlockItemLayouts() {}

    public static void register() {
        BlockItemContents.register(Identifier.withDefaultNamespace("lectern"), new Scalar("Book"));
        BlockItemContents.register(
                Identifier.withDefaultNamespace("jukebox"), new Scalar("RecordItem"));
        BlockItemContents.register(
                Identifier.withDefaultNamespace("brushable_block"), new Scalar("item"));
        BlockItemContents.register(Identifier.withDefaultNamespace("vault"), new EjectionQueue());
    }

    private record Scalar(String field) implements BlockItemContents.Layout {
        @Override
        public CustomData capture(CompoundTag nativeTag) {
            CompoundTag payload = new CompoundTag();
            Tag item = nativeTag.get(field);
            if (item != null) payload.put(field, item.copy());
            return CustomData.of(payload);
        }

        @Override
        public List<BlockItemContents.Entry> index(CustomData physical, RegistryOps<Tag> ops) {
            CompoundTag item = physical.copyTag().getCompoundOrEmpty(field);
            return item.isEmpty() ? List.of() : List.of(entry(item, field, ops));
        }

        @Override
        public void patch(
                CompoundTag physical,
                Object path,
                ItemStack replacement,
                long amount,
                RegistryOps<Tag> ops) {
            if (replacement.isEmpty()) physical.remove(field);
            else physical.put(field, encoded(replacement, amount, ops));
        }

        @Override
        public void strip(ValueOutput nativeTag) {
            nativeTag.discard(field);
        }

        @Override
        public void merge(CompoundTag nativeTag, CustomData physical) {
            Tag item = physical.copyTag().get(field);
            if (item == null) nativeTag.remove(field);
            else nativeTag.put(field, item.copy());
        }
    }

    private static final class EjectionQueue implements BlockItemContents.Layout {
        @Override
        public CustomData capture(CompoundTag nativeTag) {
            CompoundTag payload = new CompoundTag();
            Tag items = nativeTag.getCompoundOrEmpty("server_data").get("items_to_eject");
            if (items != null) payload.put("items_to_eject", items.copy());
            return CustomData.of(payload);
        }

        @Override
        public List<BlockItemContents.Entry> index(CustomData physical, RegistryOps<Tag> ops) {
            ListTag items = physical.copyTag().getListOrEmpty("items_to_eject");
            var result = new ArrayList<BlockItemContents.Entry>(items.size());
            for (int index = 0; index < items.size(); index++)
                result.add(entry((CompoundTag) items.get(index), index, ops));
            return result;
        }

        @Override
        public void patch(
                CompoundTag physical,
                Object path,
                ItemStack replacement,
                long amount,
                RegistryOps<Tag> ops) {
            ListTag items = physical.getList("items_to_eject").orElseThrow();
            int index = (Integer) path;
            if (replacement.isEmpty()) items.remove(index);
            else items.set(index, encoded(replacement, amount, ops));
        }

        @Override
        public void strip(ValueOutput nativeTag) {
            nativeTag.child("server_data").discard("items_to_eject");
        }

        @Override
        public void merge(CompoundTag nativeTag, CustomData physical) {
            CompoundTag state = nativeTag.getCompoundOrEmpty("server_data");
            state.put("items_to_eject", physical.copyTag().getListOrEmpty("items_to_eject").copy());
            nativeTag.put("server_data", state);
        }
    }

    private static BlockItemContents.Entry entry(
            CompoundTag item, Object path, RegistryOps<Tag> ops) {
        ItemStackTemplate template =
                ItemStackTemplate.MAP_CODEC.codec().parse(ops, item).getOrThrow();
        return new BlockItemContents.Entry(template.withCount(1), template.count(), path);
    }

    private static Tag encoded(ItemStack item, long amount, RegistryOps<Tag> ops) {
        assert item.getCount() == 1;
        ItemStack full = item.copyWithCount(Math.toIntExact(amount));
        return ItemStack.MAP_CODEC.codec().encodeStart(ops, full).getOrThrow();
    }
}
