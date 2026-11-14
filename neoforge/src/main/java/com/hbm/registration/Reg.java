/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.registration;

import com.hbm.NuclearTech;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.client.model.BlockModel;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.items.MaterialShapeRoster;
import com.hbm.items.PersistentInfoBlockItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

public final class Reg {

    private static final List<Runnable> AFTER_REGISTRATION = new ArrayList<>();

    private static final Map<Identifier, Identifier> ITEM_MODELS = new LinkedHashMap<>();

    private static final Map<Identifier, Supplier<BlockModel<?>>> BAKED_BY = new LinkedHashMap<>();

    private static final List<BeHandle<?>> TICKING = new ArrayList<>();

    private static final Map<Handle<?>, ItemSubtype> ITEM_SUBTYPES = new LinkedHashMap<>();

    private static volatile @Nullable TickerTables tickerTables;

    private static volatile @Nullable Map<Item, ItemSubtype> subtypesByItem;

    private Reg() {}

    private record TickerTables(
            Map<BlockEntityType<?>, BlockEntityTicker<?>> client,
            Map<BlockEntityType<?>, BlockEntityTicker<?>> server) {

        static TickerTables resolve() {
            Map<BlockEntityType<?>, BlockEntityTicker<?>> client = new IdentityHashMap<>();
            Map<BlockEntityType<?>, BlockEntityTicker<?>> server = new IdentityHashMap<>();
            for (BeHandle<?> handle : TICKING) {
                BlockEntityType<?> type = handle.get();
                if (handle.client != null) client.put(type, handle.client);
                if (handle.server != null) server.put(type, handle.server);
            }
            return new TickerTables(client, server);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> @Nullable BlockEntityTicker<T> ticker(
            BlockEntityType<T> type, boolean client) {
        TickerTables tables = tickerTables;
        if (tables == null) tickerTables = tables = TickerTables.resolve();
        return (BlockEntityTicker<T>) (client ? tables.client() : tables.server()).get(type);
    }

    public static void recordItemModel(Identifier item, Identifier model) {
        ITEM_MODELS.put(item, model);
    }

    public static Identifier itemModel(Identifier item) {
        return ITEM_MODELS.get(item);
    }

    public static Supplier<BlockModel<?>> declaredBakedBy(Identifier block) {
        return BAKED_BY.get(block);
    }

    public static Supplier<BlockModel<?>> bakedBy(Identifier block) {
        return BAKED_BY.get(block);
    }

    public static Map<Identifier, Supplier<BlockModel<?>>> bakedBy() {
        return Collections.unmodifiableMap(BAKED_BY);
    }

    public static void runAfterRegistration() {
        for (Runnable action : AFTER_REGISTRATION) {
            action.run();
        }
        AFTER_REGISTRATION.clear();
        MAP_COLORS.values().forEach(MapColors::finishRegistration);
    }

    public static <T> void afterRegistration(RegistryHandle<T> handle, Consumer<? super T> action) {
        Registrar.assertOpen("Reg.afterRegistration(" + handle.id() + ")");
        AFTER_REGISTRATION.add(() -> action.accept(handle.get()));
    }

    private static final Map<String, MapColors> MAP_COLORS = new LinkedHashMap<>();
    private static final Namespace HBM = new Namespace(NuclearTech.MOD_ID, NuclearTech.class);

    private static volatile Set<String> namespaces = Set.of(NuclearTech.MOD_ID);

    public static synchronized Namespace in(String namespace) {
        return in(namespace, null);
    }

    public static synchronized Namespace in(String namespace, @Nullable Class<?> resourceAnchor) {
        if (!namespaces.contains(namespace)) {
            Set<String> next = new HashSet<>(namespaces);
            next.add(namespace);
            namespaces = Set.copyOf(next);
        }
        return new Namespace(namespace, resourceAnchor);
    }

    public static @Nullable MapColors mapColors(String namespace) {
        return MAP_COLORS.get(namespace);
    }

    public static boolean isNtmNamespace(String namespace) {
        return namespaces.contains(namespace);
    }

    public static Set<String> addonNamespaces() {
        Set<String> addons = new HashSet<>(namespaces);
        addons.remove(NuclearTech.MOD_ID);
        return Set.copyOf(addons);
    }

    public static <I extends Item> Handle<I> item(
            String name,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        return HBM.item(name, factory, props);
    }

    public static <I extends Item> Handle<I> item(
            String name,
            Identifier model,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        return HBM.item(name, model, factory, props);
    }

    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
            String base,
            Class<E> type,
            BiFunction<Item.Properties, E, ? extends I> factory,
            Supplier<Item.Properties> props) {
        return HBM.family(base, type, factory, props);
    }

    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
            Class<E> type,
            Function<E, String> id,
            BiFunction<Item.Properties, E, ? extends I> factory,
            Supplier<Item.Properties> props) {
        return HBM.family(type, id, factory, props);
    }

    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
            E[] constants,
            Function<E, String> id,
            BiFunction<Item.Properties, E, ? extends I> factory,
            Supplier<Item.Properties> props) {
        return HBM.family(constants, id, factory, props);
    }

    static String memberId(String raw) {
        String id = raw.replaceAll("_{2,}", "_");
        int from = 0;
        int to = id.length();
        while (from < to && id.charAt(from) == '_') from++;
        while (to > from && id.charAt(to - 1) == '_') to--;
        return id.substring(from, to);
    }

    public static void visitItemSubtypes(BiConsumer<Item, ItemSubtype> visitor) {
        ITEM_SUBTYPES.forEach((handle, kind) -> visitor.accept(handle.asItem(), kind));
    }

    public static @Nullable ItemSubtype itemSubtype(Item item) {
        Map<Item, ItemSubtype> table = subtypesByItem;
        if (table == null) {
            Map<Item, ItemSubtype> built = new IdentityHashMap<>();
            visitItemSubtypes(built::put);
            subtypesByItem = table = built;
        }
        return table.get(item);
    }

    public static <B extends Block> Handle<B> block(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props) {
        return HBM.block(name, factory, props);
    }

    public static <B extends Block> Handle<B> blockItem(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props) {
        return HBM.blockItem(name, factory, props);
    }

    public static <B extends Block> Handle<B> blockItem(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props,
            BiFunction<? super B, Item.Properties, ? extends Item> itemFactory) {
        return HBM.blockItem(name, factory, props, itemFactory);
    }

    public static ClassicFluid.Pair classicFluid(String name, ClassicFluid.Spec spec) {
        return HBM.classicFluid(name, spec);
    }

    @SafeVarargs
    public static <T extends BlockEntity> BeHandle<T> be(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            RegistryHandle<? extends Block>... blocks) {
        return HBM.be(name, factory, blocks);
    }

    public static <T extends BlockEntity> BeHandle<T> be(
            String name, Supplier<BlockEntityType<T>> typeFactory) {
        return HBM.be(name, typeFactory);
    }

    public static <T extends Entity> EntityHandle<T> entity(
            String name, Supplier<EntityType<T>> typeFactory) {
        return HBM.entity(name, typeFactory);
    }

    private static Item defaultBlockItem(Block block, Item.Properties props) {
        return block instanceof PersistentItemTooltipProvider
                ? new PersistentInfoBlockItem(block, props)
                : new BlockItem(block, props);
    }

    public static final class Namespace {
        private final String namespace;
        private final @Nullable MapColors colors;

        private Namespace(String namespace, @Nullable Class<?> resourceAnchor) {
            this.namespace = namespace;
            colors = resourceAnchor == null ? null : new MapColors(resourceAnchor, namespace);
            if (colors != null) MAP_COLORS.put(namespace, colors);
        }

        private Supplier<BlockBehaviour.Properties> properties(
                String name, Supplier<BlockBehaviour.Properties> source) {
            return colors == null ? source : () -> colors.apply(name, source.get());
        }

        public MapColor constantMapColor(String owner, MapColor sourceMaterial) {
            return colors.constant(owner, sourceMaterial);
        }

        private String qualify(String name) {
            return namespace + ":" + name;
        }

        public <I extends Item> Handle<I> item(
                String name,
                Function<Item.Properties, ? extends I> factory,
                Supplier<Item.Properties> props) {
            return new Handle<>(Registrar.registerItem(qualify(name), factory, props));
        }

        public <I extends Item> Handle<I> item(
                String name,
                Identifier model,
                Function<Item.Properties, ? extends I> factory,
                Supplier<Item.Properties> props) {
            return new Handle<>(Registrar.registerItem(qualify(name), model, factory, props));
        }

        public <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
                String base,
                Class<E> type,
                BiFunction<Item.Properties, E, ? extends I> factory,
                Supplier<Item.Properties> props) {
            return family(
                    type,
                    constant -> base + "_" + constant.name().toLowerCase(Locale.ROOT),
                    factory,
                    props);
        }

        public <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
                Class<E> type,
                Function<E, String> id,
                BiFunction<Item.Properties, E, ? extends I> factory,
                Supplier<Item.Properties> props) {
            return family(type.getEnumConstants(), id, factory, props);
        }

        public <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
                E[] constants,
                Function<E, String> id,
                BiFunction<Item.Properties, E, ? extends I> factory,
                Supplier<Item.Properties> props) {
            List<Handle<I>> members = new ArrayList<>(constants.length);
            for (E constant : constants) {
                members.add(
                        item(
                                memberId(id.apply(constant)),
                                properties -> factory.apply(properties, constant),
                                props));
            }
            return new ItemFamily<>(constants, members);
        }

        private <B extends Block> Handle<B> registeredBlock(
                String name,
                Function<BlockBehaviour.Properties, ? extends B> factory,
                Supplier<BlockBehaviour.Properties> props) {
            return new Handle<>(
                    Registrar.registerBlock(
                            qualify(name),
                            properties -> {
                                var block = factory.apply(properties);
                                block.hbm$bulkManaged(true);
                                return block;
                            },
                            properties(name, props)));
        }

        public <B extends Block> Handle<B> block(
                String name,
                Function<BlockBehaviour.Properties, ? extends B> factory,
                Supplier<BlockBehaviour.Properties> props) {
            return registeredBlock(name, factory, props);
        }

        public <B extends Block> Handle<B> sharedBlock(
                String name,
                int priority,
                Function<BlockBehaviour.Properties, ? extends B> factory,
                Supplier<BlockBehaviour.Properties> props) {
            Shared.Entry<B> entry =
                    Shared.request(
                            Registries.BLOCK,
                            name,
                            namespace,
                            priority,
                            owner -> {
                                Identifier assets = Identifier.fromNamespaceAndPath(owner, name);
                                return Registrar.registerBlock(
                                        NuclearTech.MOD_ID + ":" + name,
                                        properties -> {
                                            var block = factory.apply(properties);
                                            block.hbm$bulkManaged(true);
                                            return block;
                                        },
                                        () ->
                                                properties(name, props)
                                                        .get()
                                                        .overrideDescription(
                                                                Util.makeDescriptionId(
                                                                        "block", assets))
                                                        .overrideLootTable(
                                                                Optional.of(
                                                                        ResourceKey.create(
                                                                                Registries
                                                                                        .LOOT_TABLE,
                                                                                assets.withPrefix(
                                                                                        "blocks/")))));
                            });
            return new Handle<>(entry, Identifier.fromNamespaceAndPath(namespace, name));
        }

        public <B extends Block> Handle<B> blockItem(
                String name,
                Function<BlockBehaviour.Properties, ? extends B> factory,
                Supplier<BlockBehaviour.Properties> props) {
            return blockItem(name, factory, props, Reg::defaultBlockItem);
        }

        public <B extends Block> Handle<B> blockItem(
                String name,
                Function<BlockBehaviour.Properties, ? extends B> factory,
                Supplier<BlockBehaviour.Properties> props,
                BiFunction<? super B, Item.Properties, ? extends Item> itemFactory) {
            Handle<B> block = registeredBlock(name, factory, props);
            Registrar.registerItem(
                    qualify(name),
                    itemProps -> itemFactory.apply(block.get(), itemProps),
                    () -> new Item.Properties().useBlockDescriptionPrefix());
            return block;
        }

        public ClassicFluid.Pair classicFluid(String name, ClassicFluid.Spec spec) {
            return Registrar.registerClassicFluid(qualify(name), spec);
        }

        @SafeVarargs
        public final <T extends BlockEntity> BeHandle<T> be(
                String name,
                BlockEntityType.BlockEntitySupplier<T> factory,
                RegistryHandle<? extends Block>... blocks) {
            return be(
                    name,
                    () ->
                            new BlockEntityType<>(
                                    factory,
                                    Arrays.stream(blocks)
                                            .map(RegistryHandle::get)
                                            .collect(Collectors.toUnmodifiableSet())));
        }

        public <T extends BlockEntity> BeHandle<T> be(
                String name, Supplier<BlockEntityType<T>> typeFactory) {
            return new BeHandle<>(Registrar.registerBlockEntityType(qualify(name), typeFactory));
        }

        public <T extends Entity> EntityHandle<T> entity(
                String name, Supplier<EntityType<T>> typeFactory) {
            return new EntityHandle<>(Registrar.registerEntityType(qualify(name), typeFactory));
        }
    }

    public static final class BeHandle<E extends BlockEntity>
            implements RegistryHandle<BlockEntityType<E>> {

        private final RegistryHandle<BlockEntityType<E>> delegate;

        private @Nullable BlockEntityTicker<?> client;
        private @Nullable BlockEntityTicker<?> server;
        private boolean tracked;

        private BeHandle(RegistryHandle<BlockEntityType<E>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Identifier id() {
            return delegate.id();
        }

        @Override
        public BlockEntityType<E> get() {
            return delegate.get();
        }

        public BeHandle<E> andThen(Consumer<? super BeHandle<E>> action) {
            action.accept(this);
            return this;
        }

        public BeHandle<E> ticksAt(BlockEntityTicker<? super E> both) {
            return ticksClientAt(both).ticksServerAt(both);
        }

        public BeHandle<E> ticksClientAt(BlockEntityTicker<? super E> ticker) {
            if (client != null)
                throw new IllegalStateException(id() + " states a client ticker twice");
            client = ticker;
            track();
            return this;
        }

        public BeHandle<E> ticksServerAt(BlockEntityTicker<? super E> ticker) {
            if (server != null)
                throw new IllegalStateException(id() + " states a server ticker twice");
            server = ticker;
            track();
            return this;
        }

        private void track() {
            if (tracked) return;
            tracked = true;
            TICKING.add(this);
        }
    }

    public static final class EntityHandle<T extends Entity>
            implements RegistryHandle<EntityType<T>> {

        private final RegistryHandle<EntityType<T>> delegate;

        private EntityHandle(RegistryHandle<EntityType<T>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Identifier id() {
            return delegate.id();
        }

        @Override
        public EntityType<T> get() {
            return delegate.get();
        }
    }

    public static final class Handle<T> implements RegistryHandle<T> {

        private final RegistryHandle<T> delegate;
        private final Identifier assets;

        private Handle(RegistryHandle<T> delegate) {
            this(delegate, delegate.id());
        }

        private Handle(RegistryHandle<T> delegate, Identifier assets) {
            this.delegate = delegate;
            this.assets = assets;
        }

        @Override
        public Identifier id() {
            return delegate.id();
        }

        @Override
        public T get() {
            return delegate.get();
        }

        @SafeVarargs
        public final Handle<T> state(Supplier<? extends DataComponentType<?>>... types) {
            ItemStates.declare(this, List.of(types));
            return subtypes(ItemSubtype.STATE);
        }

        public Handle<T> subtypes(ItemSubtype kind) {
            assert kind != null;
            ITEM_SUBTYPES.put(this, kind);
            return this;
        }

        public Handle<T> andThen(Consumer<? super Handle<T>> action) {
            action.accept(this);
            return this;
        }

        public Handle<T> afterRegistration(Consumer<? super T> action) {
            AFTER_REGISTRATION.add(() -> action.accept(get()));
            return this;
        }

        public Handle<T> addTo(List<? super Handle<T>> roster) {
            roster.add(this);
            return this;
        }

        public Handle<T> materialShape(Material material, MaterialShape shape) {

            MaterialShapeRoster.claim(material, shape);
            return this;
        }

        public Handle<T> bakedBy(Supplier<BlockModel<?>> model) {
            Supplier<BlockModel<?>> clash = BAKED_BY.put(id(), model);
            if (clash != null) {
                throw new IllegalStateException(
                        id()
                                + " states bakedBy twice, so only the later one decides "
                                + "and the other reads as live");
            }
            return this;
        }
    }
}
