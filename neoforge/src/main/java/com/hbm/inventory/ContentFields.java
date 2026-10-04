// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.blockentity.machine.storage.LockedContents;
import com.hbm.items.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

public final class ContentFields {
    private ContentFields() {}

    public static class Slots extends ItemContents.Field {
        private volatile DataComponentType<ItemContainerContents> type;
        private final Identifier deferred;

        public Slots(DataComponentType<ItemContainerContents> type, boolean radiation) {
            super(radiation);
            this.type = type;
            deferred = null;
        }

        public Slots(Identifier deferred, boolean radiation) {
            super(radiation);
            this.deferred = deferred;
        }

        @SuppressWarnings("unchecked")
        private DataComponentType<ItemContainerContents> type() {
            if (type == null) {
                if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(deferred))
                    throw new IllegalStateException("Missing contents component: " + deferred);
                type =
                        (DataComponentType<ItemContainerContents>)
                                (DataComponentType<?>)
                                        BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(deferred);
            }
            return type;
        }

        @Override
        public boolean present(ItemInstance parent) {
            var contents = parent.get(type());
            return contents != null && !contents.items.isEmpty();
        }

        protected long quantity(ItemInstance parent, int slot, ItemStackTemplate child) {
            return child.count();
        }

        protected void removed(ItemStack parent, int slot) {}

        @Override
        public void visit(
                ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
            var contents = parent.get(type());
            for (int slot = 0; slot < contents.items.size(); slot++) {
                var child = contents.items.get(slot);
                if (child.isPresent()) {
                    long quantity = quantity(parent, slot, child.get());
                    if (quantity > 0) reader.accept(child.get(), quantity);
                }
            }
        }

        @Override
        public boolean rewrite(ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
            var contents = parent.get(type());
            List<Optional<ItemStackTemplate>> replacement = null;
            for (int slot = 0; slot < contents.items.size(); slot++) {
                var entry = contents.items.get(slot);
                if (entry.isEmpty()
                        || quantity(parent, slot, entry.get()) <= 0
                        || !editor.selects(entry.get())) continue;
                ItemStack child = entry.get().create();
                if (!editor.visit(child)) continue;
                if (replacement == null) replacement = new ArrayList<>(contents.items);
                replacement.set(
                        slot,
                        child.isEmpty()
                                ? Optional.empty()
                                : Optional.of(ItemStackTemplate.fromNonEmptyStack(child)));
                if (child.isEmpty()) removed(parent, slot);
            }
            if (replacement == null) return false;
            parent.set(type(), slots(replacement));
            return true;
        }
    }

    private static ItemContainerContents slots(List<Optional<ItemStackTemplate>> items) {
        int end = items.size();
        while (end > 0 && items.get(end - 1).isEmpty()) end--;
        return end == 0
                ? ItemContainerContents.EMPTY
                : new ItemContainerContents(List.copyOf(items.subList(0, end)));
    }

    public static final class Scalar extends ItemContents.Field {
        private final DataComponentType<ItemStackTemplate> type;

        public Scalar(DataComponentType<ItemStackTemplate> type, boolean radiation) {
            super(radiation);
            this.type = type;
        }

        @Override
        public boolean present(ItemInstance parent) {
            return parent.get(type) != null;
        }

        @Override
        public void visit(
                ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
            ItemStackTemplate child = parent.get(type);
            reader.accept(child, child.count());
        }

        @Override
        public boolean rewrite(ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
            ItemStackTemplate original = parent.get(type);
            if (!editor.selects(original)) return false;
            ItemStack child = original.create();
            if (!editor.visit(child)) return false;
            if (child.isEmpty()) parent.remove(type);
            else parent.set(type, ItemStackTemplate.fromNonEmptyStack(child));
            return true;
        }
    }

    public static class Sequence extends ItemContents.Field {
        private final DataComponentType<List<ItemStackTemplate>> type;

        public Sequence(DataComponentType<List<ItemStackTemplate>> type, boolean radiation) {
            super(radiation);
            this.type = type;
        }

        @Override
        public boolean present(ItemInstance parent) {
            var contents = parent.get(type);
            return contents != null && !contents.isEmpty();
        }

        @Override
        public void visit(
                ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
            for (ItemStackTemplate child : parent.get(type)) reader.accept(child, child.count());
        }

        @Override
        public boolean rewrite(ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
            var changed = sequence(parent.get(type), -1, editor);
            if (changed == null) return false;
            parent.set(type, List.copyOf(changed.items));
            return true;
        }
    }

    private record Changes(List<ItemStackTemplate> items, int selected) {}

    private static @Nullable Changes sequence(
            List<ItemStackTemplate> items, int selected, ItemContents.Editor editor) {
        List<ItemStackTemplate> replacement = null;
        int nextSelection = selected;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStackTemplate original = items.get(slot);
            if (!editor.selects(original)) continue;
            ItemStack child = original.create();
            if (!editor.visit(child)) continue;
            if (replacement == null) replacement = new ArrayList<>(items);
            replacement.set(
                    slot, child.isEmpty() ? null : ItemStackTemplate.fromNonEmptyStack(child));
            if (child.isEmpty()) {
                if (slot < selected) nextSelection--;
                else if (slot == selected) nextSelection = -1;
            }
        }
        if (replacement == null) return null;
        replacement.removeIf(item -> item == null);
        return new Changes(replacement, nextSelection);
    }

    public static ItemContents.Field bundle() {
        return new ItemContents.Field(true) {
            @Override
            public boolean present(ItemInstance parent) {
                var contents = parent.get(DataComponents.BUNDLE_CONTENTS);
                return contents != null && !contents.items().isEmpty();
            }

            @Override
            public void visit(
                    ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
                for (var child : parent.get(DataComponents.BUNDLE_CONTENTS).items())
                    reader.accept(child, child.count());
            }

            @Override
            public boolean rewrite(
                    ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
                BundleContents contents = parent.get(DataComponents.BUNDLE_CONTENTS);
                var changed = sequence(contents.items(), contents.getSelectedItemIndex(), editor);
                if (changed == null) return false;
                parent.set(
                        DataComponents.BUNDLE_CONTENTS,
                        new BundleContents(changed.items, changed.selected));
                return true;
            }
        };
    }

    public static ItemContents.Field projectiles() {
        return new ItemContents.Field(true) {
            @Override
            public boolean present(ItemInstance parent) {
                var contents = parent.get(DataComponents.CHARGED_PROJECTILES);
                return contents != null && !contents.isEmpty();
            }

            @Override
            public void visit(
                    ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
                for (var child : parent.get(DataComponents.CHARGED_PROJECTILES).items())
                    reader.accept(child, child.count());
            }

            @Override
            public boolean rewrite(
                    ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
                var changed =
                        sequence(
                                parent.get(DataComponents.CHARGED_PROJECTILES).items(), -1, editor);
                if (changed == null) return false;
                parent.set(
                        DataComponents.CHARGED_PROJECTILES, new ChargedProjectiles(changed.items));
                return true;
            }
        };
    }

    public static ItemContents.Field packed() {
        return new ItemContents.Field(true) {
            @Override
            public boolean present(ItemInstance parent) {
                return parent.get(ModDataComponents.BLOCK_INVENTORY.get()) != null;
            }

            @Override
            public void visit(
                    ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
                for (var entry : parent.get(ModDataComponents.BLOCK_INVENTORY.get()).entries())
                    reader.accept(entry.item(), entry.amount());
            }

            @Override
            protected void summarize(ItemInstance parent, ItemContents.Summary summary) {
                var contents = parent.get(ModDataComponents.BLOCK_INVENTORY.get());
                summary.merge(
                        contents.minimumDeadline(),
                        contents.references(),
                        contents.opaqueReferences());
            }

            @Override
            public boolean rewrite(
                    ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
                return BlockItemContents.rewrite(parent, editor, level.registryAccess());
            }
        };
    }

    public static ItemContents.Field sealed() {
        return new ItemContents.Field(false) {
            @Override
            public boolean present(ItemInstance parent) {
                return parent.get(ModDataComponents.LOCKED_CONTENTS.get()) != null;
            }

            @Override
            protected void summarize(ItemInstance parent, ItemContents.Summary summary) {
                LockedContents contents = parent.get(ModDataComponents.LOCKED_CONTENTS.get());
                summary.merge(
                        contents.minimumDeadline(), ItemInteriors.EMPTY, contents.hasReferences());
            }

            @Override
            public void visit(
                    ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
                var contents = parent.get(ModDataComponents.LOCKED_CONTENTS.get()).open(level);
                if (contents != null)
                    for (var child : contents.nonEmptyItems()) reader.accept(child, child.count());
            }

            @Override
            public boolean rewrite(
                    ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
                var original = parent.get(ModDataComponents.LOCKED_CONTENTS.get());
                var contents = original.open(level);
                if (contents == null) return false;
                List<Optional<ItemStackTemplate>> replacement = null;
                for (int slot = 0; slot < contents.items.size(); slot++) {
                    var entry = contents.items.get(slot);
                    if (entry.isEmpty() || !editor.selects(entry.get())) continue;
                    ItemStack child = entry.get().create();
                    if (!editor.visit(child)) continue;
                    if (replacement == null) replacement = new ArrayList<>(contents.items);
                    replacement.set(
                            slot,
                            child.isEmpty()
                                    ? Optional.empty()
                                    : Optional.of(ItemStackTemplate.fromNonEmptyStack(child)));
                }
                if (replacement == null) return false;
                ItemContainerContents changed = slots(replacement);
                if (changed == ItemContainerContents.EMPTY)
                    parent.remove(ModDataComponents.LOCKED_CONTENTS.get());
                else
                    parent.set(
                            ModDataComponents.LOCKED_CONTENTS.get(),
                            LockedContents.seal(level, changed));
                return true;
            }
        };
    }
}
