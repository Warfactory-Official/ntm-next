// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.blockentity.machine.storage.BlockEntityMassStorage;
import com.hbm.blocks.machine.storage.BlockMassStorage;
import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ItemUnstable;
import com.hbm.items.special.UnstableItemData;
import com.hbm.platform.FabricInventoryPlatform;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

public final class ItemContents {
    @FunctionalInterface
    public interface Reader {
        void accept(ItemInstance item, long quantity);
    }

    public interface Editor {
        boolean visit(ItemStack item);

        default boolean selects(ItemInstance item) {
            return true;
        }
    }

    public abstract static class Field {
        public final boolean radiation;

        protected Field(boolean radiation) {
            this.radiation = radiation;
        }

        public abstract boolean present(ItemInstance parent);

        public abstract void visit(ItemInstance parent, @Nullable ServerLevel level, Reader reader);

        public abstract boolean rewrite(ItemStack parent, ServerLevel level, Editor editor);

        protected void summarize(ItemInstance parent, Summary summary) {
            visit(parent, null, summary);
        }
    }

    public static class Adapter {
        public final Field[] fields;
        public final boolean copiedCarrier;

        public Adapter(boolean copiedCarrier, Field... fields) {
            this.copiedCarrier = copiedCarrier;
            this.fields = fields.clone();
        }

        public boolean radiationShielded(ItemInstance parent) {
            return false;
        }
    }

    private static final Adapter EMPTY = new Adapter(false);
    private static final Map<Class<?>, Adapter> EXACT = new HashMap<>();
    private static final Map<Class<?>, Adapter> FAMILIES = new HashMap<>();
    private static final ClassValue<Adapter> ADAPTERS =
            new ClassValue<>() {
                @Override
                protected Adapter computeValue(Class<?> type) {
                    Adapter exact = EXACT.get(type);
                    if (exact != null) return exact;
                    for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
                        Adapter family = FAMILIES.get(parent);
                        if (family != null) return family;
                    }
                    return EMPTY;
                }
            };
    private static DataComponentType<?>[] independent = new DataComponentType<?>[0];
    private static Field[] common = new Field[0];
    public static Field CONTAINER;
    private static final ThreadLocal<Walk> WALKS = ThreadLocal.withInitial(Walk::new);

    private ItemContents() {}

    public static void bootstrap() {
        CONTAINER =
                new ContentFields.Slots(DataComponents.CONTAINER, true) {
                    @Override
                    protected long quantity(
                            ItemInstance parent, int slot, ItemStackTemplate child) {
                        return massStorage(parent) && slot == BlockEntityMassStorage.SLOT_TYPE
                                ? parent.getOrDefault(ModDataComponents.MASS_STOCKPILE.get(), 0)
                                : child.count();
                    }

                    @Override
                    protected void removed(ItemStack parent, int slot) {
                        if (massStorage(parent) && slot == BlockEntityMassStorage.SLOT_TYPE)
                            parent.remove(ModDataComponents.MASS_STOCKPILE.get());
                    }
                };
        common =
                new Field[] {
                    CONTAINER,
                    ContentFields.bundle(),
                    ContentFields.projectiles(),
                    new ContentFields.Scalar(ModDataComponents.ARTY_CARGO.get(), false),
                    new ContentFields.Sequence(ModDataComponents.KIT_CONTENTS.get(), false),
                    ContentFields.packed(),
                    ContentFields.sealed()
                };
        VanillaBlockItemLayouts.register();
        FabricInventoryPlatform.registerContents();
        if (FabricLoader.getInstance().isModLoaded("travelersbackpack")) Traveler.register();
    }

    public static void register(Class<?> type, Adapter adapter) {
        if (EXACT.putIfAbsent(type, adapter) != null)
            throw new IllegalStateException("Duplicate item contents: " + type.getName());
    }

    public static void registerFamily(Class<?> type, Adapter adapter) {
        if (FAMILIES.putIfAbsent(type, adapter) != null)
            throw new IllegalStateException("Duplicate item contents family: " + type.getName());
    }

    public static void independentComponent(DataComponentType<?> type) {
        independent = Arrays.copyOf(independent, independent.length + 1);
        independent[independent.length - 1] = type;
    }

    public static boolean affectsMetadata(DataComponentType<?> type) {
        for (DataComponentType<?> independent : independent) if (independent == type) return false;
        return true;
    }

    public static boolean copiedCarrier(ItemInstance parent) {
        return adapter(parent).copiedCarrier;
    }

    public static boolean radiationShielded(ItemInstance parent) {
        return adapter(parent).radiationShielded(parent);
    }

    private static Adapter adapter(ItemInstance parent) {
        return ADAPTERS.get(parent.typeHolder().value().getClass());
    }

    private static boolean massStorage(ItemInstance item) {
        return item.typeHolder().value() instanceof BlockItem block
                && block.getBlock() instanceof BlockMassStorage;
    }

    public static boolean hasContents(ItemInstance parent, boolean radiation) {
        return hasContents(parent, radiation, true);
    }

    public static boolean hasContents(ItemInstance parent, boolean radiation, boolean container) {
        for (Field field : common)
            if ((!radiation || field.radiation)
                    && (container || field != CONTAINER)
                    && field.present(parent)) return true;
        for (Field field : adapter(parent).fields)
            if ((!radiation || field.radiation) && field.present(parent)) return true;
        return false;
    }

    public static void visit(
            ItemInstance parent,
            @Nullable ServerLevel level,
            Reader reader,
            boolean radiation,
            boolean container) {
        for (Field field : common) {
            if ((!radiation || field.radiation)
                    && (container || field != CONTAINER)
                    && field.present(parent)) field.visit(parent, level, reader);
        }
        for (Field field : adapter(parent).fields)
            if ((!radiation || field.radiation) && field.present(parent))
                field.visit(parent, level, reader);
    }

    public static boolean rewrite(ItemStack parent, ServerLevel level, Editor editor) {
        boolean changed = false;
        for (Field field : common)
            if (field.present(parent)) changed |= field.rewrite(parent, level, editor);
        for (Field field : adapter(parent).fields)
            if (field.present(parent)) changed |= field.rewrite(parent, level, editor);
        return changed;
    }

    private static long cachedDeadline(ItemInstance item) {
        return item instanceof ItemStack stack
                ? UnstableItemData.cached(stack.components)
                : item instanceof ItemStackTemplate template
                        ? UnstableItemData.cached(template)
                        : UnstableItemData.UNRESOLVED;
    }

    private static void cacheDeadline(ItemInstance item, long deadline) {
        if (item instanceof ItemStack stack) UnstableItemData.cache(stack.components, deadline);
        else if (item instanceof ItemStackTemplate template)
            UnstableItemData.cache(template, deadline);
    }

    public static long minimumDeadline(ItemInstance item) {
        if (item.count() == 0) return Long.MAX_VALUE;
        long cached = cachedDeadline(item);
        return cached != UnstableItemData.UNRESOLVED ? cached : summarize(item).minimum;
    }

    public static Object[] references(ItemInstance item) {
        if (item.count() == 0) return ItemInteriors.EMPTY;
        Object[] cached = ItemInteriorData.cached(item);
        return cached != null ? cached : summarize(item).references;
    }

    public static boolean opaqueReferences(ItemInstance item) {
        if (item.count() == 0) return false;
        if (ItemInteriorData.cached(item) == null) summarize(item);
        return ItemInteriorData.opaque(item);
    }

    public static long minimumDeadline(ItemContainerContents contents) {
        long minimum = Long.MAX_VALUE;
        for (ItemStackTemplate item : contents.nonEmptyItems())
            minimum = Math.min(minimum, minimumDeadline(item));
        return minimum;
    }

    public static boolean hasReferences(ItemContainerContents contents) {
        for (ItemStackTemplate item : contents.nonEmptyItems())
            if (references(item).length != 0 || opaqueReferences(item)) return true;
        return false;
    }

    private static Summary summarize(ItemInstance item) {
        Walk walk = WALKS.get();
        Summary summary = walk.push();
        try {
            Long own =
                    item.typeHolder().value() instanceof ItemUnstable
                            ? item.get(ModDataComponents.UNSTABLE_AT.get())
                            : null;
            summary.minimum = own == null ? Long.MAX_VALUE : own;
            summary.references = ItemInteriors.directReferences(item);
            summary.opaque = false;
            for (Field field : common) if (field.present(item)) field.summarize(item, summary);
            for (Field field : adapter(item).fields)
                if (field.present(item)) field.summarize(item, summary);
            cacheDeadline(item, summary.minimum);
            ItemInteriorData.opaque(item, summary.opaque);
            ItemInteriorData.cache(item, summary.references);
            return summary;
        } finally {
            walk.depth--;
        }
    }

    public static final class Summary implements Reader {
        long minimum;
        Object[] references;
        boolean opaque;

        @Override
        public void accept(ItemInstance item, long quantity) {
            if (quantity <= 0) return;
            minimum = Math.min(minimum, minimumDeadline(item));
            references = ItemInteriors.merge(references, ItemContents.references(item));
            opaque |= opaqueReferences(item);
        }

        public void merge(long minimum, Object[] references, boolean opaque) {
            this.minimum = Math.min(this.minimum, minimum);
            this.references = ItemInteriors.merge(this.references, references);
            this.opaque |= opaque;
        }
    }

    private static final class Walk {
        private Summary[] frames = new Summary[8];
        private int depth;

        Summary push() {
            if (depth == frames.length) frames = Arrays.copyOf(frames, depth * 2);
            Summary frame = frames[depth];
            if (frame == null) frames[depth] = frame = new Summary();
            depth++;
            return frame;
        }
    }

    private static final class Traveler {
        static void register() {
            registerFamily(
                    com.tiviacz.travelersbackpack.item.TravelersBackpackItem.class,
                    new Adapter(
                            false,
                            new ContentFields.Slots(
                                    Identifier.parse("travelersbackpack:backpack_container"), true),
                            new ContentFields.Slots(
                                    Identifier.parse("travelersbackpack:tools_container"), true),
                            new ContentFields.Slots(
                                    Identifier.parse("travelersbackpack:upgrades"), true)));
        }
    }
}
