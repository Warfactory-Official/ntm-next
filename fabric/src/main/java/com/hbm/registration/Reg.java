// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.client.model.BlockModel;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.MaterialShapeRoster;
import com.hbm.items.PersistentInfoBlockItem;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.*;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    static void recordItemModel(Identifier item, Identifier model) {
        ITEM_MODELS.put(item, model);
    }

    public static Identifier itemModel(Identifier item) {
        return ITEM_MODELS.get(item);
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
    }

    public static <I extends Item> Handle<I> item(
            String name,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        return new Handle<>(Services.REGISTRAR.registerItem(name, factory, props));
    }

    public static <I extends Item> Handle<I> item(
            String name,
            Identifier model,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        return new Handle<>(Services.REGISTRAR.registerItem(name, model, factory, props));
    }

    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
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

    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
            Class<E> type,
            Function<E, String> id,
            BiFunction<Item.Properties, E, ? extends I> factory,
            Supplier<Item.Properties> props) {
        return family(type.getEnumConstants(), id, factory, props);
    }

    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> family(
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
        return new Handle<>(Services.REGISTRAR.registerBlock(name, factory, props));
    }

    public static <B extends Block> Handle<B> blockItem(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props) {
        return blockItem(name, factory, props, Reg::defaultBlockItem);
    }

    private static Item defaultBlockItem(Block block, Item.Properties props) {
        return block instanceof IPersistentInfoProvider
                ? new PersistentInfoBlockItem(block, props)
                : new BlockItem(block, props);
    }

    public static <B extends Block> Handle<B> blockItem(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props,
            BiFunction<? super B, Item.Properties, ? extends Item> itemFactory) {
        Handle<B> block = new Handle<>(Services.REGISTRAR.registerBlock(name, factory, props));
        Services.REGISTRAR.registerItem(
                name,
                itemProps -> itemFactory.apply(block.get(), itemProps),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    public static ClassicFluid.Pair classicFluid(String name, ClassicFluid.Spec spec) {
        return Services.REGISTRAR.registerClassicFluid(name, spec);
    }

    public static <T extends BlockEntity> BeHandle<T> be(
            String name, Supplier<BlockEntityType<T>> typeFactory) {
        return new BeHandle<>(Services.REGISTRAR.registerBlockEntityType(name, typeFactory));
    }

    public static <T extends Entity> EntityHandle<T> entity(
            String name, Supplier<EntityType<T>> typeFactory) {
        return new EntityHandle<>(Services.REGISTRAR.registerEntityType(name, typeFactory));
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

        public BeHandle<E> ticks(Consumer<? super E> both) {
            return ticks(both, both);
        }

        public BeHandle<E> ticks(Consumer<? super E> client, Consumer<? super E> server) {
            return ticksClient(client).ticksServer(server);
        }

        public BeHandle<E> ticksClient(Consumer<? super E> tick) {
            return ticksClientAt((level, pos, state, be) -> tick.accept(be));
        }

        public BeHandle<E> ticksServer(Consumer<? super E> tick) {
            return ticksServerAt((level, pos, state, be) -> tick.accept(be));
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

        private Handle(RegistryHandle<T> delegate) {
            this.delegate = delegate;
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

        public Handle<T> materialShape(NTMMaterial material, MaterialShapes shape) {
            id();
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
