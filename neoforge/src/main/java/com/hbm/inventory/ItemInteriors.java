// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.Arrays;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ItemInteriors {
    public static final Object[] EMPTY = new Object[0];
    private static ItemInteriorAdapter[] adapters = new ItemInteriorAdapter[0];
    private static final ItemInteriorAdapter NONE =
            new ItemInteriorAdapter() {
                @Override
                public boolean handles(Class<?> type) {
                    return false;
                }

                @Override
                public UUID reference(ItemInstance stack) {
                    return null;
                }

                @Override
                public ItemInteriorBackend backend(
                        MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                    throw new UnsupportedOperationException();
                }
            };
    private static final ClassValue<ItemInteriorAdapter> ADAPTERS =
            new ClassValue<>() {
                @Override
                protected ItemInteriorAdapter computeValue(Class<?> type) {
                    for (ItemInteriorAdapter adapter : adapters)
                        if (adapter.handles(type)) return adapter;
                    return NONE;
                }
            };

    private ItemInteriors() {}

    public static void register(ItemInteriorAdapter adapter) {
        adapters = Arrays.copyOf(adapters, adapters.length + 1);
        adapters[adapters.length - 1] = adapter;
    }

    public static void backendChanged(
            MinecraftServer server, ItemInteriorAdapter adapter, UUID id) {
        var state = server.hbm$unstableFuses().interiorSubscriptions();
        if (state != null) state.changed(adapter, id);
    }

    public static void publish(Object owner, Object source, int slot, ItemStack item) {
        var access = InventoryOwnerAccess.of(owner);
        var level = access.level(owner);
        if (level == null) return;
        var server = level.getServer();
        var state = server.hbm$unstableFuses().interiorSubscriptions();
        if (state == null && !hasReferences(item)) return;
        server.hbm$unstableFuses().interiorSubscriptions(server).publish(owner, source, slot, item);
    }

    public static SlotSource canonicalSource(MinecraftServer server, Object source) {
        var state = server.hbm$unstableFuses().interiorSubscriptions();
        return state == null ? null : state.canonicalSource(source);
    }

    public static void suppressSource(MinecraftServer server, Object source) {
        var state = server.hbm$unstableFuses().interiorSubscriptions();
        if (state != null) state.suppressSource(source);
    }

    public static void flush(MinecraftServer server) {
        var state = server.hbm$unstableFuses().interiorSubscriptions();
        if (state != null) state.flush();
    }

    public static void retire(MinecraftServer server, Object owner) {
        var state = server.hbm$unstableFuses().interiorSubscriptions();
        if (state != null) state.retire(owner);
    }

    public static void replaceSources(Object owner, Object sources) {
        var level = InventoryOwnerAccess.of(owner).level(owner);
        if (level == null) return;
        var server = level.getServer();
        var state = server.hbm$unstableFuses().interiorSubscriptions();
        if (state == null) {
            boolean aliases = sources instanceof InventoryAlias;
            if (sources instanceof Object[] multiple)
                for (Object source : multiple) aliases |= source instanceof InventoryAlias;
            if (!aliases) return;
            state = server.hbm$unstableFuses().interiorSubscriptions(server);
        }
        state.replaceSources(owner, sources);
    }

    public static boolean hasReferences(ItemInstance stack) {
        return references(stack).length != 0 || ItemContents.opaqueReferences(stack);
    }

    public static Object[] references(ItemInstance stack) {
        return ItemContents.references(stack);
    }

    static Object[] directReferences(ItemInstance stack) {
        ItemInteriorAdapter adapter = ADAPTERS.get(stack.typeHolder().value().getClass());
        UUID id = adapter.reference(stack);
        if (id == null) return EMPTY;
        ItemStackTemplate prototype =
                stack instanceof ItemStackTemplate template
                        ? template
                        : ItemStackTemplate.fromNonEmptyStack((ItemStack) stack);
        return new Object[] {adapter, id, prototype};
    }

    public static Object[] references(ItemInstance stack, MinecraftServer server) {
        Object[] pure = references(stack);
        if (!ItemContents.opaqueReferences(stack)) return pure;
        assert server.isSameThread();
        ReferenceWalk walk = REFERENCE_WALKS.get();
        ReferenceFrame frame = walk.push();
        try {
            frame.server = server;
            frame.references = pure;
            ItemContents.visit(stack, server.overworld(), frame, false, true);
            return frame.references;
        } finally {
            frame.server = null;
            frame.references = EMPTY;
            walk.depth--;
        }
    }

    private static final ThreadLocal<ReferenceWalk> REFERENCE_WALKS =
            ThreadLocal.withInitial(ReferenceWalk::new);

    private static final class ReferenceFrame implements ItemContents.Reader {
        MinecraftServer server;
        Object[] references;

        @Override
        public void accept(ItemInstance item, long quantity) {
            if (quantity > 0)
                references = merge(references, ItemInteriors.references(item, server));
        }
    }

    private static final class ReferenceWalk {
        ReferenceFrame[] frames = new ReferenceFrame[8];
        int depth;

        ReferenceFrame push() {
            if (depth == frames.length) frames = Arrays.copyOf(frames, depth * 2);
            ReferenceFrame frame = frames[depth];
            if (frame == null) frames[depth] = frame = new ReferenceFrame();
            depth++;
            return frame;
        }
    }

    public static Object[] merge(Object[] first, Object[] second) {
        if (second.length == 0) return first;
        if (first.length == 0) return second;
        Object[] merged = Arrays.copyOf(first, first.length + second.length);
        int size = first.length;
        outer:
        for (int i = 0; i < second.length; i += 3) {
            for (int j = 0; j < size; j += 3)
                if (merged[j] == second[i] && merged[j + 1].equals(second[i + 1])) continue outer;
            System.arraycopy(second, i, merged, size, 3);
            size += 3;
        }
        return size == first.length
                ? first
                : size == merged.length ? merged : Arrays.copyOf(merged, size);
    }

    public static int compareCarriers(Object first, Object second) {
        var a = InventoryOwnerAccess.of(first);
        var b = InventoryOwnerAccess.of(second);
        int compared =
                a.level(first)
                        .dimension()
                        .identifier()
                        .compareTo(b.level(second).dimension().identifier());
        if (compared != 0) return compared;
        boolean firstBlock = first instanceof BlockEntity;
        boolean secondBlock = second instanceof BlockEntity;
        if (firstBlock != secondBlock) return firstBlock ? -1 : 1;
        if (firstBlock) {
            var x = ((BlockEntity) first).getBlockPos();
            var y = ((BlockEntity) second).getBlockPos();
            compared = Integer.compare(x.getX(), y.getX());
            if (compared == 0) compared = Integer.compare(x.getY(), y.getY());
            if (compared == 0) compared = Integer.compare(x.getZ(), y.getZ());
            return compared;
        }
        return ((Entity) first).getUUID().compareTo(((Entity) second).getUUID());
    }
}
