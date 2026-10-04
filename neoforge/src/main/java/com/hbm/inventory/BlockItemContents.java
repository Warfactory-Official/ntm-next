// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.handler.UnstableFuses;
import com.hbm.items.ModDataComponents;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class BlockItemContents {
    public record Entry(ItemStackTemplate item, long amount, Object path) {}

    public interface Layout {
        CustomData capture(CompoundTag nativeTag);

        List<Entry> index(CustomData physical, RegistryOps<Tag> ops);

        void patch(
                CompoundTag physical,
                Object path,
                ItemStack replacement,
                long amount,
                RegistryOps<Tag> ops);

        void strip(ValueOutput nativeTag);

        void merge(CompoundTag nativeTag, CustomData physical);
    }

    private record Payload(Identifier type, CustomData contents) {}

    private static final Codec<Payload> PAYLOAD_CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Identifier.CODEC
                                                    .fieldOf("type")
                                                    .forGetter(Payload::type),
                                            CustomData.CODEC
                                                    .fieldOf("contents")
                                                    .forGetter(Payload::contents))
                                    .apply(instance, Payload::new));
    private static final Map<Identifier, Layout> LAYOUTS = new HashMap<>();

    public static final Codec<BlockItemContents> CODEC =
            new Codec<>() {
                @Override
                public <T> DataResult<Pair<BlockItemContents, T>> decode(
                        DynamicOps<T> ops, T input) {
                    if (!(ops instanceof RegistryOps<?> registries))
                        return DataResult.error(
                                () -> "Block inventory requires registry-aware ops");
                    return PAYLOAD_CODEC
                            .decode(ops, input)
                            .flatMap(
                                    decoded -> {
                                        Payload payload = decoded.getFirst();
                                        if (!LAYOUTS.containsKey(payload.type()))
                                            return DataResult.error(
                                                    () ->
                                                            "Unregistered block inventory layout: "
                                                                    + payload.type());
                                        return DataResult.success(
                                                Pair.of(
                                                        new BlockItemContents(
                                                                payload.type(),
                                                                payload.contents(),
                                                                registries.withParent(
                                                                        NbtOps.INSTANCE)),
                                                        decoded.getSecond()));
                                    });
                }

                @Override
                public <T> DataResult<T> encode(
                        BlockItemContents input, DynamicOps<T> ops, T prefix) {
                    return PAYLOAD_CODEC.encode(
                            new Payload(input.type, input.contents), ops, prefix);
                }
            };
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockItemContents> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private final Identifier type;
    private final CustomData contents;
    private final List<Entry> entries;
    private final long minimum;
    private final Object[] references;
    private final boolean opaqueReferences;

    private BlockItemContents(Identifier type, CustomData contents, RegistryOps<Tag> ops) {
        this.type = type;
        this.contents = contents;
        entries = List.copyOf(LAYOUTS.get(type).index(contents, ops));
        long minimum = Long.MAX_VALUE;
        Object[] references = ItemInteriors.EMPTY;
        boolean opaqueReferences = false;
        for (Entry entry : entries) {
            assert entry.item().count() == 1 && entry.amount() > 0;
            minimum = Math.min(minimum, UnstableFuses.deadline(entry.item()));
            references = ItemInteriors.merge(references, ItemInteriors.references(entry.item()));
            opaqueReferences |= ItemContents.opaqueReferences(entry.item());
        }
        this.minimum = minimum;
        this.references = references;
        this.opaqueReferences = opaqueReferences;
    }

    public static void register(Identifier type, Layout layout) {
        if (LAYOUTS.putIfAbsent(type, layout) != null)
            throw new IllegalStateException("Duplicate block inventory layout: " + type);
    }

    public List<Entry> entries() {
        return entries;
    }

    public long minimumDeadline() {
        return minimum;
    }

    public Object[] references() {
        return references;
    }

    public boolean opaqueReferences() {
        return opaqueReferences;
    }

    public static @Nullable BlockItemContents capture(BlockEntity owner) {
        Identifier type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(owner.getType());
        Layout layout = LAYOUTS.get(type);
        if (layout == null) return null;
        RegistryAccess registries = owner.getLevel().registryAccess();
        CustomData physical = layout.capture(owner.saveCustomOnly(registries));
        return physical.isEmpty()
                ? null
                : new BlockItemContents(
                        type, physical, RegistryOps.create(NbtOps.INSTANCE, registries));
    }

    public static void strip(BlockEntity owner, ValueOutput output) {
        Layout layout = LAYOUTS.get(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(owner.getType()));
        if (layout != null) layout.strip(output);
    }

    public static void apply(BlockEntity owner, DataComponentGetter components) {
        BlockItemContents packed = components.get(ModDataComponents.BLOCK_INVENTORY.get());
        if (packed == null) return;
        Identifier type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(owner.getType());
        if (!packed.type.equals(type))
            throw new IllegalStateException(
                    "Block inventory type mismatch: " + packed.type + " / " + type);
        RegistryAccess registries = owner.getLevel().registryAccess();
        CompoundTag nativeTag = owner.saveCustomOnly(registries);
        LAYOUTS.get(type).merge(nativeTag, packed.contents);
        owner.loadCustomOnly(
                TagValueInput.create(ProblemReporter.DISCARDING, registries, nativeTag));
    }

    public static boolean rewrite(
            ItemStack carrier, ItemContents.Editor visitor, RegistryAccess registries) {
        BlockItemContents packed = carrier.get(ModDataComponents.BLOCK_INVENTORY.get());
        if (packed == null) return false;
        CompoundTag replacement = null;
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        for (int index = packed.entries.size() - 1; index >= 0; index--) {
            Entry entry = packed.entries.get(index);
            if (!visitor.selects(entry.item())) continue;
            ItemStack item = entry.item().create();
            if (!visitor.visit(item)) continue;
            if (replacement == null) replacement = packed.contents.copyTag();
            LAYOUTS.get(packed.type).patch(replacement, entry.path(), item, entry.amount(), ops);
        }
        if (replacement == null) return false;
        carrier.set(
                ModDataComponents.BLOCK_INVENTORY.get(),
                new BlockItemContents(packed.type, CustomData.of(replacement), ops));
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BlockItemContents packed
                && type.equals(packed.type)
                && contents.equals(packed.contents);
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode() + contents.hashCode();
    }
}
