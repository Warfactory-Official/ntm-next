// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class ArmorMods {
    public static final ArmorMods EMPTY =
            new ArmorMods(new ItemStackTemplate[ArmorModHandler.MOD_SLOTS]);
    public static final Codec<ArmorMods> CODEC =
            Socket.CODEC
                    .sizeLimitedListOf(ArmorModHandler.MOD_SLOTS)
                    .xmap(ArmorMods::fromSockets, ArmorMods::sockets);
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorMods> STREAM_CODEC =
            Socket.STREAM_CODEC
                    .apply(ByteBufCodecs.list(ArmorModHandler.MOD_SLOTS))
                    .map(ArmorMods::fromSockets, ArmorMods::sockets);

    private final ItemStackTemplate[] slots;
    private final int occupied;
    private final int hash;

    private ArmorMods(ItemStackTemplate[] slots) {
        this.slots = slots;
        int occupied = 0;
        for (int i = 0; i < slots.length; i++) if (slots[i] != null) occupied |= 1 << i;
        this.occupied = occupied;
        this.hash = Arrays.hashCode(slots);
    }

    private static ArmorMods fromSockets(List<Socket> sockets) {
        ItemStackTemplate[] slots = new ItemStackTemplate[ArmorModHandler.MOD_SLOTS];
        for (Socket socket : sockets) {
            if (socket.index < 0
                    || socket.index >= slots.length
                    || slots[socket.index] != null
                    || socket.item.count() != 1)
                throw new IllegalArgumentException("Invalid armor socket " + socket.index);
            validated(socket.item);
            slots[socket.index] = socket.item;
        }
        return sockets.isEmpty() ? EMPTY : new ArmorMods(slots);
    }

    private List<Socket> sockets() {
        List<Socket> sockets = new ArrayList<>(Integer.bitCount(occupied));
        for (int i = 0; i < slots.length; i++)
            if (slots[i] != null) sockets.add(new Socket(i, slots[i]));
        return sockets;
    }

    public int occupied() {
        return occupied;
    }

    public @Nullable ItemStackTemplate template(int slot) {
        return slots[slot];
    }

    public Item item(int slot) {
        return slots[slot] == null ? Items.AIR : slots[slot].item().value();
    }

    private static ItemStack validated(ItemStackTemplate template) {
        return ItemStack.validateStrict(
                        new ItemStack(template.item(), template.count(), template.components()))
                .getOrThrow();
    }

    public ItemStack copy(int slot) {
        return slots[slot] == null ? ItemStack.EMPTY : validated(slots[slot]);
    }

    public ArmorMods with(int slot, ItemStack stack) {
        ItemStackTemplate next =
                stack.isEmpty() ? null : ItemStackTemplate.fromNonEmptyStack(stack).withCount(1);
        if (java.util.Objects.equals(slots[slot], next)) return this;
        if (next != null) validated(next);
        ItemStackTemplate[] changed = slots.clone();
        changed[slot] = next;
        ArmorMods result = new ArmorMods(changed);
        return result.occupied == 0 ? EMPTY : result;
    }

    public boolean sameAttributes(ArmorMods other) {
        for (int i = 0; i < slots.length; i++) {
            Item before = other.item(i), after = item(i);
            if (before != after
                    && (before instanceof ItemArmorMod beforeMod && beforeMod.hasAttributes()
                            || after instanceof ItemArmorMod afterMod && afterMod.hasAttributes()))
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof ArmorMods mods
                        && hash == mods.hash
                        && Arrays.equals(slots, mods.slots);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    private record Socket(int index, ItemStackTemplate item) {
        private static final Codec<Socket> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.intRange(0, ArmorModHandler.MOD_SLOTS - 1)
                                                        .fieldOf("slot")
                                                        .forGetter(Socket::index),
                                                ItemStackTemplate.CODEC
                                                        .fieldOf("item")
                                                        .forGetter(Socket::item))
                                        .apply(i, Socket::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, Socket> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        Socket::index,
                        ItemStackTemplate.STREAM_CODEC,
                        Socket::item,
                        Socket::new);
    }
}
