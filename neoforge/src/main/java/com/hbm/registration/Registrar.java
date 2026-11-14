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
import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.blocks.fluid.InertFluidType;
import com.hbm.blocks.fluid.NeoForgeClassicFluid;
import com.hbm.inventory.BlockDataMenuType;
import com.hbm.inventory.BlockMenuContext;
import com.hbm.inventory.BlockMenuType;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.DataMenuFactory;
import com.hbm.inventory.fluid.NeoForgeTokenFluid;
import com.hbm.inventory.fluid.NtmFluidType;
import com.hbm.platform.EntityData;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.*;
import org.jspecify.annotations.Nullable;

public class Registrar {
    public static final List<RegistryHandle<? extends Block>> registeredBlocks = new ArrayList<>();
    public static boolean frozen;
    private static final Map<String, Registers> REGISTERS = new ConcurrentHashMap<>();
    public static final Map<Identifier, DeferredHolder<FluidType, FluidType>> types =
            new HashMap<>();
    private static final List<LivingAttributes<?>> livingAttributes = new ArrayList<>();
    private static final List<SpawnPlacement<?>> spawnPlacements = new ArrayList<>();
    private static final List<DataPackRegistry<?>> dataPackRegistries = new ArrayList<>();
    public static final List<Map.Entry<ResourceKey<CreativeModeTab>, Supplier<ItemStack>>>
            vanillaTabEntries = new ArrayList<>();
    private static final List<Map.Entry<ItemLike, Consumer<DataComponentMap.Builder>>>
            componentPatches = new ArrayList<>();

    public static void subscribe(IEventBus modBus) {
        subscribe(NuclearTech.MOD_ID, modBus);
        modBus.addListener(
                EventPriority.HIGHEST,
                (RegisterEvent event) -> {
                    if (!frozen) Shared.settle();
                    frozen = true;
                });
        modBus.addListener(
                (BuildCreativeModeTabContentsEvent event) -> {
                    for (Map.Entry<ResourceKey<CreativeModeTab>, Supplier<ItemStack>> entry :
                            vanillaTabEntries) {
                        if (event.getTabKey() == entry.getKey())
                            event.accept(entry.getValue().get());
                    }
                });
        modBus.addListener(
                (EntityAttributeCreationEvent event) -> {
                    for (LivingAttributes<?> entry : livingAttributes)
                        event.put(entry.type().get(), entry.attributes().get().build());
                });
        modBus.addListener(
                (RegisterSpawnPlacementsEvent event) -> {
                    for (SpawnPlacement<?> entry : spawnPlacements) entry.register(event);
                });
        modBus.addListener(
                (DataPackRegistryEvent.NewRegistry event) -> {
                    for (DataPackRegistry<?> entry : dataPackRegistries) entry.declare(event);
                });
        modBus.addListener(
                (ModifyDefaultComponentsEvent event) -> {
                    for (Map.Entry<ItemLike, Consumer<DataComponentMap.Builder>> entry :
                            componentPatches) {
                        event.modify(
                                entry.getKey(),
                                (components, context, item) -> entry.getValue().accept(components));
                    }
                });
    }

    public static void subscribe(String namespace, IEventBus modBus) {
        Registers registers = new Registers(namespace);
        if (REGISTERS.putIfAbsent(namespace, registers) != null) {
            throw new IllegalStateException("namespace " + namespace + " is already subscribed");
        }
        registers.subscribe(modBus);
        EntityData.subscribe(namespace, modBus);
    }

    public static void addon(String namespace, IEventBus modBus, Addon addon) {
        subscribe(namespace, modBus);
        modBus.addListener((FMLConstructModEvent event) -> event.enqueueWork(addon::register));
    }

    private static Registers registers(Identifier id) {
        Registers registers = REGISTERS.get(id.getNamespace());
        if (registers == null) {
            throw new IllegalStateException(
                    id + ": no mod subscribed namespace " + id.getNamespace());
        }
        return registers;
    }

    public static <B extends Block> RegistryHandle<B> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props) {
        assertOpen("registerBlock");
        Identifier id = RegistryUtil.resolve(name);
        DeferredBlock<B> holder =
                registers(id)
                        .blocks
                        .register(
                                id.getPath(),
                                key ->
                                        factory.apply(
                                                props.get()
                                                        .setId(
                                                                ResourceKey.create(
                                                                        Registries.BLOCK, key))));
        RegistryHandle<B> handle = RegistryHandle.of(id, holder);
        registeredBlocks.add(handle);
        return handle;
    }

    public static <I extends Item> RegistryHandle<I> registerItem(
            String name,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        assertOpen("registerItem");
        Identifier id = RegistryUtil.resolve(name);
        DeferredItem<I> holder =
                registers(id)
                        .items
                        .register(
                                id.getPath(),
                                key ->
                                        factory.apply(
                                                props.get()
                                                        .setId(
                                                                ResourceKey.create(
                                                                        Registries.ITEM, key))));
        return RegistryHandle.of(id, holder);
    }

    public static <I extends Item> RegistryHandle<I> registerItem(
            String name,
            Identifier model,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        assertOpen("registerItem");
        Identifier id = RegistryUtil.resolve(name);
        Reg.recordItemModel(id, model);
        DeferredItem<I> holder =
                registers(id)
                        .items
                        .register(
                                id.getPath(),
                                key -> {
                                    ResourceKey<Item> itemKey =
                                            ResourceKey.create(Registries.ITEM, key);
                                    I item = factory.apply(props.get().setId(itemKey));
                                    BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.add(
                                            itemKey,
                                            (components, context, resourceKey) ->
                                                    components.set(
                                                            DataComponents.ITEM_MODEL, model));
                                    return item;
                                });
        return RegistryHandle.of(id, holder);
    }

    public static <T extends BlockEntity>
            RegistryHandle<BlockEntityType<T>> registerBlockEntityType(
                    String name, Supplier<BlockEntityType<T>> typeFactory) {
        assertOpen("registerBlockEntityType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> holder =
                registers(id).blockEntityTypes.register(id.getPath(), typeFactory);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenu(
            String name, MenuType.MenuSupplier<T> factory) {
        assertOpen("registerMenu");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MenuType<?>, MenuType<T>> holder =
                registers(id)
                        .menuTypes
                        .register(
                                id.getPath(),
                                () -> new MenuType<>(factory, FeatureFlags.VANILLA_SET));
        return RegistryHandle.of(id, holder);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractContainerMenu, BE extends BlockEntity & MenuProvider>
            BlockMenuType<BE, T> registerBlockEntityMenu(String name, Class<BE> blockEntityClass) {
        BlockMenus.declareOwnMenu(blockEntityClass);
        return new BlockMenuType<>(
                blockEntityMenu(
                        name,
                        blockEntityClass,
                        BlockMenuContext.UNIT,
                        (containerId, inventory, blockEntity, unit) ->
                                (T)
                                        blockEntity.createMenu(
                                                containerId, inventory, inventory.player)));
    }

    public static <T extends AbstractContainerMenu, BE extends BlockEntity & MenuProvider>
            BlockMenuType<BE, T> registerBlockEntityMenu(
                    String name, Class<BE> blockEntityClass, BlockMenuType.Factory<BE, T> factory) {
        return new BlockMenuType<>(
                blockEntityMenu(
                        name,
                        blockEntityClass,
                        BlockMenuContext.UNIT,
                        (containerId, inventory, blockEntity, unit) ->
                                factory.create(containerId, inventory, blockEntity)));
    }

    public static <T extends AbstractContainerMenu, BE extends BlockEntity & MenuProvider, D>
            BlockDataMenuType<BE, T, D> registerBlockEntityMenu(
                    String name,
                    Class<BE> blockEntityClass,
                    StreamCodec<? super RegistryFriendlyByteBuf, D> data,
                    BlockDataMenuType.Factory<BE, T, D> factory) {
        return new BlockDataMenuType<>(
                blockEntityMenu(name, blockEntityClass, BlockMenuContext.codec(data), factory));
    }

    private static <T extends AbstractContainerMenu, BE extends BlockEntity & MenuProvider, D>
            BlockDataMenuType.Binding<BE, T, D> blockEntityMenu(
                    String name,
                    Class<BE> blockEntityClass,
                    StreamCodec<RegistryFriendlyByteBuf, BlockMenuContext<D>> codec,
                    BlockDataMenuType.Factory<BE, T, D> factory) {
        assertOpen("registerBlockEntityMenu");
        Identifier id = RegistryUtil.resolve(name);
        RegistryHandle<MenuType<T>> type =
                registerDataMenu(
                        name,
                        (containerId, inventory, context) -> {
                            BlockEntity blockEntity =
                                    inventory.player.level().getBlockEntity(context.pos());
                            if (!blockEntityClass.isInstance(blockEntity)) {
                                throw new IllegalStateException(
                                        "Expected "
                                                + blockEntityClass.getName()
                                                + " at "
                                                + context.pos()
                                                + " while opening "
                                                + id
                                                + ", found "
                                                + blockEntity);
                            }
                            T menu =
                                    factory.create(
                                            containerId,
                                            inventory,
                                            blockEntityClass.cast(blockEntity),
                                            context.data());
                            if (menu == null)
                                throw new IllegalStateException(
                                        "Block entity returned no menu for " + id);
                            return menu;
                        },
                        codec);
        return new BlockDataMenuType.Binding<>(type, codec, factory);
    }

    public static <T extends AbstractContainerMenu, D> RegistryHandle<MenuType<T>> registerDataMenu(
            String name,
            DataMenuFactory<T, D> factory,
            StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        assertOpen("registerDataMenu");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MenuType<?>, MenuType<T>> holder =
                registers(id)
                        .menuTypes
                        .register(
                                id.getPath(),
                                () ->
                                        IMenuTypeExtension.create(
                                                (containerId, inventory, buf) ->
                                                        factory.create(
                                                                containerId,
                                                                inventory,
                                                                codec.decode(buf))));
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<CreativeModeTab> registerCreativeModeTab(
            String name, Consumer<CreativeModeTab.Builder> config) {
        assertOpen("registerCreativeModeTab");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<CreativeModeTab, CreativeModeTab> holder =
                registers(id)
                        .creativeTabs
                        .register(
                                id.getPath(),
                                () -> {
                                    CreativeModeTab.Builder builder = CreativeModeTab.builder();
                                    config.accept(builder);
                                    return builder.build();
                                });
        return RegistryHandle.of(id, holder);
    }

    public static <T extends Recipe<?>> RegistryHandle<RecipeType<T>> registerRecipeType(
            String name) {
        Identifier id = RegistryUtil.resolve(name);
        return registerRecipeType(name, new NamedRecipeType<>(id));
    }

    public static <T extends Recipe<?>> RegistryHandle<RecipeType<T>> registerRecipeType(
            String name, RecipeType<T> instance) {
        assertOpen("registerRecipeType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<RecipeType<?>, RecipeType<T>> holder =
                registers(id).recipeTypes.register(id.getPath(), () -> instance);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends Recipe<?>>
            RegistryHandle<RecipeSerializer<T>> registerRecipeSerializer(
                    String name, RecipeSerializer<T> serializer) {
        assertOpen("registerRecipeSerializer");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<RecipeSerializer<?>, RecipeSerializer<T>> holder =
                registers(id).recipeSerializers.register(id.getPath(), () -> serializer);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends Recipe<?>>
            RegistryHandle<RecipeSerializer<T>> registerListedRecipeSerializer(
                    String name, RecipeSerializer<T> serializer) {
        RegistryHandle<RecipeSerializer<T>> handle = registerRecipeSerializer(name, serializer);
        return handle;
    }

    public static <T extends CriterionTrigger<?>> RegistryHandle<T> registerCriterionTrigger(
            String name, T trigger) {
        assertOpen("registerCriterionTrigger");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<CriterionTrigger<?>, T> holder =
                registers(id).criterionTriggers.register(id.getPath(), () -> trigger);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends DataComponentPredicate>
            RegistryHandle<DataComponentPredicate.Type<T>> registerDataComponentPredicate(
                    String name, Codec<T> codec) {
        assertOpen("registerDataComponentPredicate");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<T>> holder =
                registers(id)
                        .componentPredicates
                        .register(
                                id.getPath(),
                                () -> new DataComponentPredicate.ConcreteType<>(codec));
        return RegistryHandle.of(id, holder);
    }

    public static <T> RegistryHandle<DataComponentType<T>> registerDataComponent(
            String name, DataComponentType<T> type) {
        assertOpen("registerDataComponent");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<DataComponentType<?>, DataComponentType<T>> holder =
                registers(id).dataComponents.register(id.getPath(), () -> type);
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<Fluid> registerFluid(String name) {
        assertOpen("registerFluid");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<Fluid, NeoForgeTokenFluid> fluidHolder =
                registers(id)
                        .fluids
                        .register(
                                id.getPath(),
                                () ->
                                        new NeoForgeTokenFluid(
                                                ((Supplier<? extends FluidType>)
                                                        (() -> types.get(id).get()))));
        DeferredHolder<FluidType, FluidType> typeHolder =
                registers(id)
                        .fluidTypes
                        .register(id.getPath(), () -> new NtmFluidType(fluidHolder::get, id));
        types.put(id, typeHolder);
        return RegistryHandle.<Fluid>of(id, fluidHolder::get);
    }

    public static ClassicFluid.Pair registerClassicFluid(String name, ClassicFluid.Spec spec) {
        assertOpen("registerClassicFluid");
        Identifier id = RegistryUtil.resolve(name);
        Identifier flowingId = id.withPrefix("flowing_");
        Registers registers = registers(id);
        DeferredHolder<Fluid, Fluid> source = DeferredHolder.create(Registries.FLUID, id);
        DeferredHolder<Fluid, Fluid> flowing = DeferredHolder.create(Registries.FLUID, flowingId);
        Supplier<? extends FluidType> type =
                switch (spec.physics()) {
                    case WATER -> NeoForgeMod.WATER_TYPE::value;
                    case LAVA -> NeoForgeMod.LAVA_TYPE::value;
                    case NONE ->
                            registers.fluidTypes.register(
                                    id.getPath(), () -> new InertFluidType(spec));
                };
        DeferredHolder<Fluid, NeoForgeClassicFluid.Source> still =
                registers.fluids.register(
                        id.getPath(),
                        () -> new NeoForgeClassicFluid.Source(spec, source, flowing, type));
        DeferredHolder<Fluid, NeoForgeClassicFluid.Flowing> flow =
                registers.fluids.register(
                        flowingId.getPath(),
                        () -> new NeoForgeClassicFluid.Flowing(spec, source, flowing, type));
        return new ClassicFluid.Pair(
                RegistryHandle.of(id, still), RegistryHandle.of(flowingId, flow));
    }

    public static RegistryHandle<SoundEvent> registerSound(String name) {
        assertOpen("registerSound");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<SoundEvent, SoundEvent> holder =
                registers(id)
                        .soundEvents
                        .register(id.getPath(), () -> SoundEvent.createVariableRangeEvent(id));
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<Identifier> registerCustomStat(String name) {
        assertOpen("registerCustomStat");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<Identifier, Identifier> holder =
                registers(id).customStats.register(id.getPath(), () -> id);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends MobEffect> RegistryHandle<T> registerMobEffect(
            String name, Supplier<T> factory) {
        assertOpen("registerMobEffect");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MobEffect, T> holder =
                registers(id).mobEffects.register(id.getPath(), factory);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends ParticleType<?>> RegistryHandle<T> registerParticleType(
            String name, Supplier<T> typeFactory) {
        assertOpen("registerParticleType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<ParticleType<?>, T> holder =
                registers(id).particleTypes.register(id.getPath(), typeFactory);
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<TicketType> registerTicketType(
            String name, Supplier<TicketType> typeFactory) {
        assertOpen("registerTicketType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<TicketType, TicketType> holder =
                registers(id).ticketTypes.register(id.getPath(), typeFactory);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends Entity> RegistryHandle<EntityType<T>> registerEntityType(
            String name, Supplier<EntityType<T>> typeFactory) {
        assertOpen("registerEntityType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<EntityType<?>, EntityType<T>> holder =
                registers(id).entityTypes.register(id.getPath(), typeFactory);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends LivingEntity> void registerLivingAttributes(
            RegistryHandle<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {
        assertOpen("registerLivingAttributes");
        livingAttributes.add(new LivingAttributes<>(type, attributes));
    }

    public static <T extends Mob> void registerSpawnPlacement(
            RegistryHandle<EntityType<T>> type,
            SpawnPlacementType placement,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate) {
        assertOpen("registerSpawnPlacement");
        spawnPlacements.add(new SpawnPlacement<>(type, placement, heightmap, predicate));
    }

    public static <T extends Structure> RegistryHandle<StructureType<T>> registerStructureType(
            String name, MapCodec<T> codec) {
        assertOpen("registerStructureType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<StructureType<?>, StructureType<T>> holder =
                registers(id).structureTypes.register(id.getPath(), () -> () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<StructurePieceType> registerStructurePieceType(
            String name, StructurePieceType type) {
        assertOpen("registerStructurePieceType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<StructurePieceType, StructurePieceType> holder =
                registers(id).structurePieceTypes.register(id.getPath(), () -> type);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends StructurePoolElement>
            RegistryHandle<StructurePoolElementType<T>> registerStructurePoolElementType(
                    String name, MapCodec<T> codec) {
        assertOpen("registerStructurePoolElementType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<StructurePoolElementType<?>, StructurePoolElementType<T>> holder =
                registers(id).structurePoolElementTypes.register(id.getPath(), () -> () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <P extends PlacementModifier>
            RegistryHandle<PlacementModifierType<P>> registerPlacementModifierType(
                    String name, MapCodec<P> codec) {
        assertOpen("registerPlacementModifierType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<PlacementModifierType<?>, PlacementModifierType<P>> holder =
                registers(id).placementModifiers.register(id.getPath(), () -> () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <P extends StructurePlacement>
            RegistryHandle<StructurePlacementType<P>> registerStructurePlacementType(
                    String name, MapCodec<P> codec) {
        assertOpen("registerStructurePlacementType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<StructurePlacementType<?>, StructurePlacementType<P>> holder =
                registers(id).structurePlacements.register(id.getPath(), () -> () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <FC extends FeatureConfiguration, F extends Feature<FC>>
            RegistryHandle<F> registerFeature(String name, F feature) {
        assertOpen("registerFeature");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<Feature<?>, F> holder =
                registers(id).features.register(id.getPath(), () -> feature);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends StructureProcessor>
            RegistryHandle<MapCodec<T>> registerStructureProcessorType(
                    String name, MapCodec<T> codec) {
        assertOpen("registerStructureProcessorType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<T>> holder =
                registers(id).structureProcessors.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends NumberProvider>
            RegistryHandle<MapCodec<T>> registerLootNumberProviderType(
                    String name, MapCodec<T> codec) {
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MapCodec<? extends NumberProvider>, MapCodec<T>> holder =
                registers(id).lootNumberProviders.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <T> void registerDataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        assertOpen("registerDataPackRegistry");
        dataPackRegistries.add(new DataPackRegistry<>(key, codec, null));
    }

    public static <T> void registerSyncedDataPackRegistry(
            ResourceKey<Registry<T>> key, Codec<T> codec) {
        assertOpen("registerSyncedDataPackRegistry");
        dataPackRegistries.add(new DataPackRegistry<>(key, codec, codec));
    }

    public static <T extends BiomeSource> RegistryHandle<MapCodec<T>> registerBiomeSource(
            String name, MapCodec<T> codec) {
        assertOpen("registerBiomeSource");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<T>> holder =
                registers(id).biomeSources.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <C extends CarverConfiguration, W extends WorldCarver<C>>
            RegistryHandle<W> registerCarver(String name, W carver) {
        assertOpen("registerCarver");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<WorldCarver<?>, W> holder =
                registers(id).carvers.register(id.getPath(), () -> carver);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends DensityFunction>
            RegistryHandle<MapCodec<T>> registerDensityFunctionType(
                    String name, MapCodec<T> codec) {
        assertOpen("registerDensityFunctionType");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<T>> holder =
                registers(id).densityFunctionTypes.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends SurfaceRules.RuleSource>
            RegistryHandle<MapCodec<T>> registerMaterialRule(String name, MapCodec<T> codec) {
        assertOpen("registerMaterialRule");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MapCodec<? extends SurfaceRules.RuleSource>, MapCodec<T>> holder =
                registers(id).materialRules.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends SurfaceRules.ConditionSource>
            RegistryHandle<MapCodec<T>> registerMaterialCondition(String name, MapCodec<T> codec) {
        assertOpen("registerMaterialCondition");
        Identifier id = RegistryUtil.resolve(name);
        DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<T>> holder =
                registers(id).materialConditions.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static void assertOpen(String what) {
        if (frozen) {
            throw new IllegalStateException(
                    "registration is closed: "
                            + what
                            + " was called after the first RegisterEvent"
                            + " - a content holder was class-loaded too late to register");
        }
    }

    public static void modifyDefaultComponents(
            ItemLike item, Consumer<DataComponentMap.Builder> patch) {
        assertOpen("modifyDefaultComponents");
        componentPatches.add(Map.entry(item, patch));
    }

    private record LivingAttributes<T extends LivingEntity>(
            RegistryHandle<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {}

    private record SpawnPlacement<T extends Mob>(
            RegistryHandle<EntityType<T>> type,
            SpawnPlacementType placement,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate) {
        public void register(RegisterSpawnPlacementsEvent event) {
            event.register(
                    type.get(),
                    placement,
                    heightmap,
                    predicate,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private record DataPackRegistry<T>(
            ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        public void declare(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(key, codec, networkCodec);
        }
    }

    private static final class Registers {
        final DeferredRegister.Blocks blocks;
        final DeferredRegister.Items items;
        final DeferredRegister<RecipeType<?>> recipeTypes;
        final DeferredRegister<RecipeSerializer<?>> recipeSerializers;
        final DeferredRegister<CriterionTrigger<?>> criterionTriggers;
        final DeferredRegister<DataComponentPredicate.Type<?>> componentPredicates;
        final DeferredRegister<DataComponentType<?>> dataComponents;
        final DeferredRegister<BlockEntityType<?>> blockEntityTypes;
        final DeferredRegister<MenuType<?>> menuTypes;
        final DeferredRegister<CreativeModeTab> creativeTabs;
        final DeferredRegister<Fluid> fluids;
        final DeferredRegister<SoundEvent> soundEvents;
        final DeferredRegister<Identifier> customStats;
        final DeferredRegister<MobEffect> mobEffects;
        final DeferredRegister<EntityType<?>> entityTypes;
        final DeferredRegister<ParticleType<?>> particleTypes;
        final DeferredRegister<TicketType> ticketTypes;
        final DeferredRegister<StructureType<?>> structureTypes;
        final DeferredRegister<StructurePieceType> structurePieceTypes;
        final DeferredRegister<StructurePoolElementType<?>> structurePoolElementTypes;
        final DeferredRegister<PlacementModifierType<?>> placementModifiers;
        final DeferredRegister<StructurePlacementType<?>> structurePlacements;
        final DeferredRegister<Feature<?>> features;
        final DeferredRegister<MapCodec<? extends StructureProcessor>> structureProcessors;
        final DeferredRegister<MapCodec<? extends NumberProvider>> lootNumberProviders;
        final DeferredRegister<FluidType> fluidTypes;
        final DeferredRegister<MapCodec<? extends BiomeSource>> biomeSources;
        final DeferredRegister<WorldCarver<?>> carvers;
        final DeferredRegister<MapCodec<? extends DensityFunction>> densityFunctionTypes;
        final DeferredRegister<MapCodec<? extends SurfaceRules.RuleSource>> materialRules;
        final DeferredRegister<MapCodec<? extends SurfaceRules.ConditionSource>> materialConditions;

        Registers(String namespace) {
            blocks = DeferredRegister.createBlocks(namespace);
            items = DeferredRegister.createItems(namespace);
            recipeTypes = DeferredRegister.create(Registries.RECIPE_TYPE, namespace);
            recipeSerializers = DeferredRegister.create(Registries.RECIPE_SERIALIZER, namespace);
            criterionTriggers = DeferredRegister.create(Registries.TRIGGER_TYPE, namespace);
            componentPredicates =
                    DeferredRegister.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, namespace);
            dataComponents = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, namespace);
            blockEntityTypes = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, namespace);
            menuTypes = DeferredRegister.create(Registries.MENU, namespace);
            creativeTabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, namespace);
            fluids = DeferredRegister.create(Registries.FLUID, namespace);
            soundEvents = DeferredRegister.create(Registries.SOUND_EVENT, namespace);
            customStats = DeferredRegister.create(Registries.CUSTOM_STAT, namespace);
            mobEffects = DeferredRegister.create(Registries.MOB_EFFECT, namespace);
            entityTypes = DeferredRegister.create(Registries.ENTITY_TYPE, namespace);
            particleTypes = DeferredRegister.create(Registries.PARTICLE_TYPE, namespace);
            ticketTypes = DeferredRegister.create(Registries.TICKET_TYPE, namespace);
            structureTypes = DeferredRegister.create(Registries.STRUCTURE_TYPE, namespace);
            structurePieceTypes = DeferredRegister.create(Registries.STRUCTURE_PIECE, namespace);
            structurePoolElementTypes =
                    DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT, namespace);
            placementModifiers =
                    DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, namespace);
            structurePlacements =
                    DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, namespace);
            features = DeferredRegister.create(Registries.FEATURE, namespace);
            structureProcessors =
                    DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, namespace);
            lootNumberProviders =
                    DeferredRegister.create(Registries.LOOT_NUMBER_PROVIDER_TYPE, namespace);
            fluidTypes = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, namespace);
            biomeSources = DeferredRegister.create(Registries.BIOME_SOURCE, namespace);
            carvers = DeferredRegister.create(Registries.CARVER, namespace);
            densityFunctionTypes =
                    DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, namespace);
            materialRules = DeferredRegister.create(Registries.MATERIAL_RULE, namespace);
            materialConditions = DeferredRegister.create(Registries.MATERIAL_CONDITION, namespace);
        }

        void subscribe(IEventBus modBus) {
            blocks.register(modBus);
            items.register(modBus);
            recipeTypes.register(modBus);
            recipeSerializers.register(modBus);
            criterionTriggers.register(modBus);
            componentPredicates.register(modBus);
            dataComponents.register(modBus);
            blockEntityTypes.register(modBus);
            menuTypes.register(modBus);
            creativeTabs.register(modBus);
            fluids.register(modBus);
            soundEvents.register(modBus);
            customStats.register(modBus);
            mobEffects.register(modBus);
            entityTypes.register(modBus);
            particleTypes.register(modBus);
            ticketTypes.register(modBus);
            structureTypes.register(modBus);
            structurePieceTypes.register(modBus);
            structurePoolElementTypes.register(modBus);
            placementModifiers.register(modBus);
            structurePlacements.register(modBus);
            features.register(modBus);
            structureProcessors.register(modBus);
            lootNumberProviders.register(modBus);
            fluidTypes.register(modBus);
            biomeSources.register(modBus);
            carvers.register(modBus);
            densityFunctionTypes.register(modBus);
            materialRules.register(modBus);
            materialConditions.register(modBus);
        }
    }

    private static final class NamedRecipeType<T extends Recipe<?>> implements RecipeType<T> {
        private final Identifier id;

        public NamedRecipeType(Identifier id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id.toString();
        }
    }
}
