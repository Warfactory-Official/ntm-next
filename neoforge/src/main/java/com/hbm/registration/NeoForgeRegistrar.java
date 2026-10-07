// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import com.hbm.NuclearTech;
import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.blocks.fluid.InertFluidType;
import com.hbm.blocks.fluid.NeoForgeClassicFluid;
import com.hbm.inventory.BlockMenuContext;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.fluid.NeoForgeTokenFluid;
import com.hbm.inventory.fluid.NtmFluidType;
import com.hbm.lib.Library;
import com.hbm.platform.NeoForgeAttachments;
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

public final class NeoForgeRegistrar {
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

    private static final Map<String, Registers> REGISTERS = new ConcurrentHashMap<>();
    public static final Map<Identifier, DeferredHolder<FluidType, FluidType>> types =
            new HashMap<>();
    public static final List<LivingAttributes<?>> livingAttributes = new ArrayList<>();
    public static final List<SpawnPlacement<?>> spawnPlacements = new ArrayList<>();
    public static final List<DataPackRegistry<?>> dataPackRegistries = new ArrayList<>();
    public static final List<RegistryHandle<? extends Block>> registeredBlocks = new ArrayList<>();
    public static final List<Map.Entry<ResourceKey<CreativeModeTab>, Supplier<ItemStack>>>
            vanillaTabEntries = new ArrayList<>();
    public static final List<Map.Entry<ItemLike, Consumer<DataComponentMap.Builder>>>
            componentPatches = new ArrayList<>();
    public static boolean frozen;

    public static void subscribe(IEventBus modBus) {
        subscribe(NuclearTech.MOD_ID, modBus);
        modBus.addListener(EventPriority.HIGHEST, (RegisterEvent event) -> frozen = true);
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
        NeoForgeAttachments.subscribe(namespace, modBus);
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
        Identifier id = Library.resolve(name);
        assertOpen("registerBlock");
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
        Identifier id = Library.resolve(name);
        assertOpen("registerItem");
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
        Identifier id = Library.resolve(name);
        assertOpen("registerItem");
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
        Identifier id = Library.resolve(name);
        assertOpen("registerBlockEntityType");
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> holder =
                registers(id).blockEntityTypes.register(id.getPath(), typeFactory);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenu(
            String name, MenuType.MenuSupplier<T> factory) {
        Identifier id = Library.resolve(name);
        assertOpen("registerMenu");
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
            RegistryHandle<MenuType<T>> registerBlockEntityMenu(
                    String name, Class<BE> blockEntityClass) {
        Identifier id = Library.resolve(name);
        assertOpen("registerBlockEntityMenu");
        DeferredHolder<MenuType<?>, MenuType<T>> holder =
                registers(id)
                        .menuTypes
                        .register(
                                id.getPath(),
                                () ->
                                        IMenuTypeExtension.create(
                                                (containerId, inventory, buf) -> {
                                                    var context =
                                                            new BlockMenuContext(
                                                                    buf.readBlockPos(),
                                                                    buf.readVarInt());
                                                    BlockEntity blockEntity =
                                                            inventory
                                                                    .player
                                                                    .level()
                                                                    .getBlockEntity(context.pos());
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
                                                    AbstractContainerMenu menu =
                                                            IGUIProvider.createDispatchedMenu(
                                                                    blockEntityClass.cast(
                                                                            blockEntity),
                                                                    containerId,
                                                                    inventory,
                                                                    inventory.player,
                                                                    context.dispatchId());
                                                    if (menu == null)
                                                        throw new IllegalStateException(
                                                                "GUI provider returned no menu for "
                                                                        + id);
                                                    return (T) menu;
                                                }));
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<CreativeModeTab> registerCreativeModeTab(
            String name, Consumer<CreativeModeTab.Builder> config) {
        Identifier id = Library.resolve(name);
        assertOpen("registerCreativeModeTab");
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

    public static Consumer<CreativeModeTab.Builder> searchBar() {
        return CreativeModeTab.Builder::withSearchBar;
    }

    public static void modifyDefaultComponents(
            ItemLike item, Consumer<DataComponentMap.Builder> patch) {
        assertOpen("modifyDefaultComponents");
        componentPatches.add(Map.entry(item, patch));
    }

    public static <T extends Recipe<?>> RegistryHandle<RecipeType<T>> registerRecipeType(
            String name) {
        Identifier id = Library.resolve(name);
        return registerRecipeType(name, new NamedRecipeType<>(id));
    }

    public static <T extends Recipe<?>> RegistryHandle<RecipeType<T>> registerRecipeType(
            String name, RecipeType<T> instance) {
        assertOpen("registerRecipeType");
        Identifier id = Library.resolve(name);
        DeferredHolder<RecipeType<?>, RecipeType<T>> holder =
                registers(id).recipeTypes.register(id.getPath(), () -> instance);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends Recipe<?>>
            RegistryHandle<RecipeSerializer<T>> registerRecipeSerializer(
                    String name, RecipeSerializer<T> serializer) {
        assertOpen("registerRecipeSerializer");
        Identifier id = Library.resolve(name);
        DeferredHolder<RecipeSerializer<?>, RecipeSerializer<T>> holder =
                registers(id).recipeSerializers.register(id.getPath(), () -> serializer);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends CriterionTrigger<?>> RegistryHandle<T> registerCriterionTrigger(
            String name, T trigger) {
        Identifier id = Library.resolve(name);
        assertOpen("registerCriterionTrigger");
        DeferredHolder<CriterionTrigger<?>, T> holder =
                registers(id).criterionTriggers.register(id.getPath(), () -> trigger);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends DataComponentPredicate>
            RegistryHandle<DataComponentPredicate.Type<T>> registerDataComponentPredicate(
                    String name, Codec<T> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerDataComponentPredicate");
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
        Identifier id = Library.resolve(name);
        DeferredHolder<DataComponentType<?>, DataComponentType<T>> holder =
                registers(id).dataComponents.register(id.getPath(), () -> type);
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<Fluid> registerFluid(String name) {
        Identifier id = Library.resolve(name);
        assertOpen("registerFluid");
        DeferredHolder<Fluid, NeoForgeTokenFluid> fluidHolder =
                registers(id)
                        .fluids
                        .register(id.getPath(), () -> new NeoForgeTokenFluid(fluidTypeOf(id)));
        DeferredHolder<FluidType, FluidType> typeHolder =
                registers(id)
                        .fluidTypes
                        .register(id.getPath(), () -> new NtmFluidType(fluidHolder::get, id));
        types.put(id, typeHolder);
        return RegistryHandle.<Fluid>of(id, fluidHolder::get);
    }

    public static ClassicFluid.Pair registerClassicFluid(String name, ClassicFluid.Spec spec) {
        Identifier id = Library.resolve(name);
        Identifier flowingId = id.withPrefix("flowing_");
        assertOpen("registerClassicFluid");
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

    public static Supplier<? extends FluidType> fluidTypeOf(Identifier id) {
        return () -> types.get(id).get();
    }

    public static RegistryHandle<SoundEvent> registerSound(String name) {
        assertOpen("registerSound");
        Identifier id = Library.resolve(name);
        DeferredHolder<SoundEvent, SoundEvent> holder =
                registers(id)
                        .soundEvents
                        .register(id.getPath(), () -> SoundEvent.createVariableRangeEvent(id));
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<Identifier> registerCustomStat(String name) {
        assertOpen("registerCustomStat");
        Identifier id = Library.resolve(name);
        DeferredHolder<Identifier, Identifier> holder =
                registers(id).customStats.register(id.getPath(), () -> id);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends MobEffect> RegistryHandle<T> registerMobEffect(
            String name, Supplier<T> factory) {
        Identifier id = Library.resolve(name);
        assertOpen("registerMobEffect");
        DeferredHolder<MobEffect, T> holder =
                registers(id).mobEffects.register(id.getPath(), factory);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends Entity> RegistryHandle<EntityType<T>> registerEntityType(
            String name, Supplier<EntityType<T>> typeFactory) {
        Identifier id = Library.resolve(name);
        assertOpen("registerEntityType");
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

    public static <T extends ParticleType<?>> RegistryHandle<T> registerParticleType(
            String name, Supplier<T> typeFactory) {
        Identifier id = Library.resolve(name);
        assertOpen("registerParticleType");
        DeferredHolder<ParticleType<?>, T> holder =
                registers(id).particleTypes.register(id.getPath(), typeFactory);
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<TicketType> registerTicketType(
            String name, Supplier<TicketType> typeFactory) {
        Identifier id = Library.resolve(name);
        assertOpen("registerTicketType");
        DeferredHolder<TicketType, TicketType> holder =
                registers(id).ticketTypes.register(id.getPath(), typeFactory);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends Structure> RegistryHandle<StructureType<T>> registerStructureType(
            String name, MapCodec<T> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerStructureType");
        DeferredHolder<StructureType<?>, StructureType<T>> holder =
                registers(id).structureTypes.register(id.getPath(), () -> () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static RegistryHandle<StructurePieceType> registerStructurePieceType(
            String name, StructurePieceType type) {
        Identifier id = Library.resolve(name);
        assertOpen("registerStructurePieceType");
        DeferredHolder<StructurePieceType, StructurePieceType> holder =
                registers(id).structurePieceTypes.register(id.getPath(), () -> type);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends StructurePoolElement>
            RegistryHandle<StructurePoolElementType<T>> registerStructurePoolElementType(
                    String name, MapCodec<T> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerStructurePoolElementType");
        DeferredHolder<StructurePoolElementType<?>, StructurePoolElementType<T>> holder =
                registers(id).structurePoolElementTypes.register(id.getPath(), () -> () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <P extends PlacementModifier>
            RegistryHandle<PlacementModifierType<P>> registerPlacementModifierType(
                    String name, MapCodec<P> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerPlacementModifierType");
        DeferredHolder<PlacementModifierType<?>, PlacementModifierType<P>> holder =
                registers(id).placementModifiers.register(id.getPath(), () -> () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <P extends StructurePlacement>
            RegistryHandle<StructurePlacementType<P>> registerStructurePlacementType(
                    String name, MapCodec<P> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerStructurePlacementType");
        DeferredHolder<StructurePlacementType<?>, StructurePlacementType<P>> holder =
                registers(id).structurePlacements.register(id.getPath(), () -> () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <FC extends FeatureConfiguration, F extends Feature<FC>>
            RegistryHandle<F> registerFeature(String name, F feature) {
        Identifier id = Library.resolve(name);
        assertOpen("registerFeature");
        DeferredHolder<Feature<?>, F> holder =
                registers(id).features.register(id.getPath(), () -> feature);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends StructureProcessor>
            RegistryHandle<MapCodec<T>> registerStructureProcessorType(
                    String name, MapCodec<T> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerStructureProcessorType");
        DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<T>> holder =
                registers(id).structureProcessors.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends NumberProvider>
            RegistryHandle<MapCodec<T>> registerLootNumberProviderType(
                    String name, MapCodec<T> codec) {
        Identifier id = Library.resolve(name);
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
        Identifier id = Library.resolve(name);
        assertOpen("registerBiomeSource");
        DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<T>> holder =
                registers(id).biomeSources.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <C extends CarverConfiguration, W extends WorldCarver<C>>
            RegistryHandle<W> registerCarver(String name, W carver) {
        Identifier id = Library.resolve(name);
        assertOpen("registerCarver");
        DeferredHolder<WorldCarver<?>, W> holder =
                registers(id).carvers.register(id.getPath(), () -> carver);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends DensityFunction>
            RegistryHandle<MapCodec<T>> registerDensityFunctionType(
                    String name, MapCodec<T> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerDensityFunctionType");
        DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<T>> holder =
                registers(id).densityFunctionTypes.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends SurfaceRules.RuleSource>
            RegistryHandle<MapCodec<T>> registerMaterialRule(String name, MapCodec<T> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerMaterialRule");
        DeferredHolder<MapCodec<? extends SurfaceRules.RuleSource>, MapCodec<T>> holder =
                registers(id).materialRules.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static <T extends SurfaceRules.ConditionSource>
            RegistryHandle<MapCodec<T>> registerMaterialCondition(String name, MapCodec<T> codec) {
        Identifier id = Library.resolve(name);
        assertOpen("registerMaterialCondition");
        DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<T>> holder =
                registers(id).materialConditions.register(id.getPath(), () -> codec);
        return RegistryHandle.of(id, holder);
    }

    public static void assertOpen(String what) {
        if (frozen) {
            throw new IllegalStateException(
                    "registration is closed: "
                            + what
                            + " was called after the first RegisterEvent - a content holder was class-loaded too late"
                            + " to register");
        }
    }

    public record LivingAttributes<T extends LivingEntity>(
            RegistryHandle<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {}

    public record SpawnPlacement<T extends Mob>(
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

    public record DataPackRegistry<T>(
            ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        public void declare(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(key, codec, networkCodec);
        }
    }

    public static final class NamedRecipeType<T extends Recipe<?>> implements RecipeType<T> {
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
