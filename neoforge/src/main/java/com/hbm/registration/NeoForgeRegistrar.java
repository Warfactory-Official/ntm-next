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
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.*;
import org.jspecify.annotations.Nullable;

public final class NeoForgeRegistrar implements IRegistrar {

    private final Map<String, DeferredHolder<FluidType, FluidType>> types = new HashMap<>();

    private final DeferredRegister.Blocks blocks =
            DeferredRegister.createBlocks(NuclearTech.MOD_ID);
    private final DeferredRegister.Items items = DeferredRegister.createItems(NuclearTech.MOD_ID);
    private final DeferredRegister<RecipeType<?>> recipeTypes =
            DeferredRegister.create(Registries.RECIPE_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<RecipeSerializer<?>> recipeSerializers =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, NuclearTech.MOD_ID);
    private final DeferredRegister<CriterionTrigger<?>> criterionTriggers =
            DeferredRegister.create(Registries.TRIGGER_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<DataComponentPredicate.Type<?>> componentPredicates =
            DeferredRegister.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<DataComponentType<?>> dataComponents =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<BlockEntityType<?>> blockEntityTypes =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<MenuType<?>> menuTypes =
            DeferredRegister.create(Registries.MENU, NuclearTech.MOD_ID);
    private final DeferredRegister<CreativeModeTab> creativeTabs =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NuclearTech.MOD_ID);
    private final DeferredRegister<Fluid> fluids =
            DeferredRegister.create(Registries.FLUID, NuclearTech.MOD_ID);
    private final DeferredRegister<SoundEvent> soundEvents =
            DeferredRegister.create(Registries.SOUND_EVENT, NuclearTech.MOD_ID);
    private final DeferredRegister<Identifier> customStats =
            DeferredRegister.create(Registries.CUSTOM_STAT, NuclearTech.MOD_ID);
    private final DeferredRegister<MobEffect> mobEffects =
            DeferredRegister.create(Registries.MOB_EFFECT, NuclearTech.MOD_ID);
    private final DeferredRegister<EntityType<?>> entityTypes =
            DeferredRegister.create(Registries.ENTITY_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<ParticleType<?>> particleTypes =
            DeferredRegister.create(Registries.PARTICLE_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<TicketType> ticketTypes =
            DeferredRegister.create(Registries.TICKET_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<StructureType<?>> structureTypes =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<StructurePieceType> structurePieceTypes =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, NuclearTech.MOD_ID);
    private final DeferredRegister<StructurePoolElementType<?>> structurePoolElementTypes =
            DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT, NuclearTech.MOD_ID);
    private final DeferredRegister<PlacementModifierType<?>> placementModifiers =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<Feature<?>> features =
            DeferredRegister.create(Registries.FEATURE, NuclearTech.MOD_ID);
    private final DeferredRegister<MapCodec<? extends StructureProcessor>> structureProcessors =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, NuclearTech.MOD_ID);
    private final DeferredRegister<MapCodec<? extends NumberProvider>> lootNumberProviders =
            DeferredRegister.create(Registries.LOOT_NUMBER_PROVIDER_TYPE, NuclearTech.MOD_ID);
    private final DeferredRegister<FluidType> fluidTypes =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, NuclearTech.MOD_ID);
    private final List<LivingAttributes<?>> livingAttributes = new ArrayList<>();
    private final List<SpawnPlacement<?>> spawnPlacements = new ArrayList<>();
    private final List<DataPackRegistry<?>> dataPackRegistries = new ArrayList<>();
    private final List<RegistryHandle<? extends Block>> registeredBlocks = new ArrayList<>();
    private final List<Map.Entry<ResourceKey<CreativeModeTab>, Supplier<ItemStack>>>
            vanillaTabEntries = new ArrayList<>();
    private boolean frozen;

    public void subscribe(IEventBus modBus) {
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
        fluidTypes.register(modBus);
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
        features.register(modBus);
        structureProcessors.register(modBus);
        lootNumberProviders.register(modBus);
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
    }

    @Override
    public <B extends Block> RegistryHandle<B> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props) {
        assertOpen("registerBlock");
        DeferredBlock<B> holder =
                blocks.register(
                        name,
                        key ->
                                factory.apply(
                                        props.get()
                                                .setId(ResourceKey.create(Registries.BLOCK, key))));
        RegistryHandle<B> handle = RegistryHandle.of(idOf(name), holder);
        registeredBlocks.add(handle);
        return handle;
    }

    @Override
    public <I extends Item> RegistryHandle<I> registerItem(
            String name,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        assertOpen("registerItem");
        DeferredItem<I> holder =
                items.register(
                        name,
                        key ->
                                factory.apply(
                                        props.get()
                                                .setId(ResourceKey.create(Registries.ITEM, key))));
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <I extends Item> RegistryHandle<I> registerItem(
            String name,
            Identifier model,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        assertOpen("registerItem");
        Reg.recordItemModel(idOf(name), model);
        DeferredItem<I> holder =
                items.register(
                        name,
                        key -> {
                            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, key);
                            I item = factory.apply(props.get().setId(itemKey));
                            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.add(
                                    itemKey,
                                    (components, context, resourceKey) ->
                                            components.set(DataComponents.ITEM_MODEL, model));
                            return item;
                        });
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends BlockEntity> RegistryHandle<BlockEntityType<T>> registerBlockEntityType(
            String name, Supplier<BlockEntityType<T>> typeFactory) {
        assertOpen("registerBlockEntityType");
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> holder =
                blockEntityTypes.register(name, typeFactory);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenu(
            String name, MenuType.MenuSupplier<T> factory) {
        assertOpen("registerMenu");
        DeferredHolder<MenuType<?>, MenuType<T>> holder =
                menuTypes.register(name, () -> new MenuType<>(factory, FeatureFlags.VANILLA_SET));
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractContainerMenu, BE extends BlockEntity & MenuProvider>
            RegistryHandle<MenuType<T>> registerBlockEntityMenu(
                    String name, Class<BE> blockEntityClass) {
        assertOpen("registerBlockEntityMenu");
        DeferredHolder<MenuType<?>, MenuType<T>> holder =
                menuTypes.register(
                        name,
                        () ->
                                IMenuTypeExtension.create(
                                        (containerId, inventory, buf) -> {
                                            var context =
                                                    new BlockMenuContext(
                                                            buf.readBlockPos(), buf.readVarInt());
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
                                                                + idOf(name)
                                                                + ", found "
                                                                + blockEntity);
                                            }
                                            AbstractContainerMenu menu =
                                                    IGUIProvider.createDispatchedMenu(
                                                            blockEntityClass.cast(blockEntity),
                                                            containerId,
                                                            inventory,
                                                            inventory.player,
                                                            context.dispatchId());
                                            if (menu == null)
                                                throw new IllegalStateException(
                                                        "GUI provider returned no menu for "
                                                                + idOf(name));
                                            return (T) menu;
                                        }));
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public RegistryHandle<CreativeModeTab> registerCreativeModeTab(
            String name, Consumer<CreativeModeTab.Builder> config) {
        assertOpen("registerCreativeModeTab");
        DeferredHolder<CreativeModeTab, CreativeModeTab> holder =
                creativeTabs.register(
                        name,
                        () -> {
                            CreativeModeTab.Builder builder = CreativeModeTab.builder();
                            config.accept(builder);
                            return builder.build();
                        });
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public Consumer<CreativeModeTab.Builder> searchBar() {
        return CreativeModeTab.Builder::withSearchBar;
    }

    @Override
    public void addToVanillaTab(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> entry) {
        vanillaTabEntries.add(Map.entry(tab, entry));
    }

    @Override
    public <T extends Recipe<?>> RegistryHandle<RecipeType<T>> registerRecipeType(String name) {
        Identifier id = idOf(name);
        return registerRecipeType(
                name,
                new RecipeType<T>() {
                    @Override
                    public String toString() {
                        return id.toString();
                    }
                });
    }

    @Override
    public <T extends Recipe<?>> RegistryHandle<RecipeType<T>> registerRecipeType(
            String name, RecipeType<T> instance) {
        assertOpen("registerRecipeType");
        Identifier id = idOf(name);
        DeferredHolder<RecipeType<?>, RecipeType<T>> holder =
                recipeTypes.register(name, () -> instance);
        return RegistryHandle.of(id, holder);
    }

    @Override
    public <T extends Recipe<?>> RegistryHandle<RecipeSerializer<T>> registerRecipeSerializer(
            String name, RecipeSerializer<T> serializer) {
        assertOpen("registerRecipeSerializer");
        Identifier id = idOf(name);
        DeferredHolder<RecipeSerializer<?>, RecipeSerializer<T>> holder =
                recipeSerializers.register(name, () -> serializer);
        return RegistryHandle.of(id, holder);
    }

    @Override
    public <T extends CriterionTrigger<?>> RegistryHandle<T> registerCriterionTrigger(
            String name, T trigger) {
        assertOpen("registerCriterionTrigger");
        DeferredHolder<CriterionTrigger<?>, T> holder =
                criterionTriggers.register(name, () -> trigger);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends DataComponentPredicate>
            RegistryHandle<DataComponentPredicate.Type<T>> registerDataComponentPredicate(
                    String name, Codec<T> codec) {
        assertOpen("registerDataComponentPredicate");
        DeferredHolder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<T>> holder =
                componentPredicates.register(
                        name, () -> new DataComponentPredicate.ConcreteType<>(codec));
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T> RegistryHandle<DataComponentType<T>> registerDataComponent(
            String name, DataComponentType<T> type) {
        assertOpen("registerDataComponent");
        Identifier id = idOf(name);
        DeferredHolder<DataComponentType<?>, DataComponentType<T>> holder =
                dataComponents.register(name, () -> type);
        return RegistryHandle.of(id, holder);
    }

    @Override
    public RegistryHandle<Fluid> registerFluid(String name) {
        assertOpen("registerFluid");
        DeferredHolder<Fluid, NeoForgeTokenFluid> fluidHolder =
                fluids.register(name, () -> new NeoForgeTokenFluid(fluidTypeOf(name)));
        DeferredHolder<FluidType, FluidType> typeHolder =
                fluidTypes.register(name, () -> new NtmFluidType(fluidHolder::get, name));
        types.put(name, typeHolder);
        return RegistryHandle.<Fluid>of(idOf(name), fluidHolder::get);
    }

    @Override
    public ClassicFluid.Pair registerClassicFluid(String name, ClassicFluid.Spec spec) {
        assertOpen("registerClassicFluid");
        String flowingName = "flowing_" + name;
        DeferredHolder<Fluid, Fluid> source = DeferredHolder.create(Registries.FLUID, idOf(name));
        DeferredHolder<Fluid, Fluid> flowing =
                DeferredHolder.create(Registries.FLUID, idOf(flowingName));
        Supplier<? extends FluidType> type =
                switch (spec.physics()) {
                    case WATER -> NeoForgeMod.WATER_TYPE::value;
                    case LAVA -> NeoForgeMod.LAVA_TYPE::value;
                    case NONE -> fluidTypes.register(name, () -> new InertFluidType(spec));
                };
        DeferredHolder<Fluid, NeoForgeClassicFluid.Source> still =
                fluids.register(
                        name, () -> new NeoForgeClassicFluid.Source(spec, source, flowing, type));
        DeferredHolder<Fluid, NeoForgeClassicFluid.Flowing> flow =
                fluids.register(
                        flowingName,
                        () -> new NeoForgeClassicFluid.Flowing(spec, source, flowing, type));
        return new ClassicFluid.Pair(
                RegistryHandle.of(idOf(name), still), RegistryHandle.of(idOf(flowingName), flow));
    }

    private Supplier<? extends FluidType> fluidTypeOf(String name) {
        return () -> types.get(name).get();
    }

    @Override
    public RegistryHandle<SoundEvent> registerSound(String name) {
        assertOpen("registerSound");
        Identifier id = idOf(name);
        DeferredHolder<SoundEvent, SoundEvent> holder =
                soundEvents.register(name, () -> SoundEvent.createVariableRangeEvent(id));
        return RegistryHandle.of(id, holder);
    }

    @Override
    public RegistryHandle<Identifier> registerCustomStat(String name) {
        assertOpen("registerCustomStat");
        Identifier id = idOf(name);
        DeferredHolder<Identifier, Identifier> holder = customStats.register(name, () -> id);
        return RegistryHandle.of(id, holder);
    }

    @Override
    public <T extends MobEffect> RegistryHandle<T> registerMobEffect(
            String name, Supplier<T> factory) {
        assertOpen("registerMobEffect");
        DeferredHolder<MobEffect, T> holder = mobEffects.register(name, factory);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends Entity> RegistryHandle<EntityType<T>> registerEntityType(
            String name, Supplier<EntityType<T>> typeFactory) {
        assertOpen("registerEntityType");
        DeferredHolder<EntityType<?>, EntityType<T>> holder =
                entityTypes.register(name, typeFactory);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends LivingEntity> void registerLivingAttributes(
            RegistryHandle<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {
        assertOpen("registerLivingAttributes");
        livingAttributes.add(new LivingAttributes<>(type, attributes));
    }

    @Override
    public <T extends Mob> void registerSpawnPlacement(
            RegistryHandle<EntityType<T>> type,
            SpawnPlacementType placement,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate) {
        assertOpen("registerSpawnPlacement");
        spawnPlacements.add(new SpawnPlacement<>(type, placement, heightmap, predicate));
    }

    @Override
    public <T extends ParticleType<?>> RegistryHandle<T> registerParticleType(
            String name, Supplier<T> typeFactory) {
        assertOpen("registerParticleType");
        DeferredHolder<ParticleType<?>, T> holder = particleTypes.register(name, typeFactory);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public RegistryHandle<TicketType> registerTicketType(
            String name, Supplier<TicketType> typeFactory) {
        assertOpen("registerTicketType");
        DeferredHolder<TicketType, TicketType> holder = ticketTypes.register(name, typeFactory);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends Structure> RegistryHandle<StructureType<T>> registerStructureType(
            String name, MapCodec<T> codec) {
        assertOpen("registerStructureType");
        DeferredHolder<StructureType<?>, StructureType<T>> holder =
                structureTypes.register(name, () -> () -> codec);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public RegistryHandle<StructurePieceType> registerStructurePieceType(
            String name, StructurePieceType type) {
        assertOpen("registerStructurePieceType");
        DeferredHolder<StructurePieceType, StructurePieceType> holder =
                structurePieceTypes.register(name, () -> type);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends StructurePoolElement>
            RegistryHandle<StructurePoolElementType<T>> registerStructurePoolElementType(
                    String name, MapCodec<T> codec) {
        assertOpen("registerStructurePoolElementType");
        DeferredHolder<StructurePoolElementType<?>, StructurePoolElementType<T>> holder =
                structurePoolElementTypes.register(name, () -> () -> codec);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <P extends PlacementModifier>
            RegistryHandle<PlacementModifierType<P>> registerPlacementModifierType(
                    String name, MapCodec<P> codec) {
        assertOpen("registerPlacementModifierType");
        DeferredHolder<PlacementModifierType<?>, PlacementModifierType<P>> holder =
                placementModifiers.register(name, () -> () -> codec);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <FC extends FeatureConfiguration, F extends Feature<FC>>
            RegistryHandle<F> registerFeature(String name, F feature) {
        assertOpen("registerFeature");
        DeferredHolder<Feature<?>, F> holder = features.register(name, () -> feature);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends StructureProcessor>
            RegistryHandle<MapCodec<T>> registerStructureProcessorType(
                    String name, MapCodec<T> codec) {
        assertOpen("registerStructureProcessorType");
        DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<T>> holder =
                structureProcessors.register(name, () -> codec);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T extends NumberProvider> RegistryHandle<MapCodec<T>> registerLootNumberProviderType(
            String name, MapCodec<T> codec) {
        DeferredHolder<MapCodec<? extends NumberProvider>, MapCodec<T>> holder =
                lootNumberProviders.register(name, () -> codec);
        return RegistryHandle.of(idOf(name), holder);
    }

    @Override
    public <T> void registerDataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        assertOpen("registerDataPackRegistry");
        dataPackRegistries.add(new DataPackRegistry<>(key, codec, null));
    }

    @Override
    public <T> void registerSyncedDataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        assertOpen("registerSyncedDataPackRegistry");
        dataPackRegistries.add(new DataPackRegistry<>(key, codec, codec));
    }

    @Override
    public void freeze() {
        frozen = true;
    }

    @Override
    public List<RegistryHandle<? extends Block>> blocks() {
        return registeredBlocks;
    }

    private void assertOpen(String what) {
        if (frozen) {
            throw new IllegalStateException(
                    "registration is closed: "
                            + what
                            + " was called after mod init - a content holder was class-loaded too late to register");
        }
    }

    private record LivingAttributes<T extends LivingEntity>(
            RegistryHandle<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {}

    private record SpawnPlacement<T extends Mob>(
            RegistryHandle<EntityType<T>> type,
            SpawnPlacementType placement,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate) {
        void register(RegisterSpawnPlacementsEvent event) {
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
        void declare(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(key, codec, networkCodec);
        }
    }
}
