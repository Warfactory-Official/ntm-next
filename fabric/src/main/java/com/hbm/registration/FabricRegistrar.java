// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.inventory.BlockMenuContext;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.fluid.FabricTokenFluid;
import com.hbm.inventory.fluid.FluidPhysics;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;
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
import org.jspecify.annotations.Nullable;

public final class FabricRegistrar implements IRegistrar {

    private static final Set<CreativeModeTab.Builder> SEARCH_BUILDERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<CreativeModeTab> SEARCH_TABS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private final List<RegistryHandle<? extends Block>> registeredBlocks = new ArrayList<>();
    private boolean frozen;

    @Override
    public <B extends Block> RegistryHandle<B> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props) {
        assertOpen("registerBlock");
        Identifier id = idOf(name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        B block = factory.apply(props.get().setId(key));
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        RegistryHandle<B> handle = RegistryHandle.bound(id, block);
        registeredBlocks.add(handle);
        return handle;
    }

    @Override
    public <I extends Item> RegistryHandle<I> registerItem(
            String name,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        assertOpen("registerItem");
        Identifier id = idOf(name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        I item = factory.apply(props.get().setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return RegistryHandle.bound(id, item);
    }

    @Override
    public <I extends Item> RegistryHandle<I> registerItem(
            String name,
            Identifier model,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props) {
        assertOpen("registerItem");
        Identifier id = idOf(name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        I item = factory.apply(props.get().setId(key));
        Reg.recordItemModel(id, model);
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.add(
                key,
                (components, context, itemKey) -> components.set(DataComponents.ITEM_MODEL, model));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return RegistryHandle.bound(id, item);
    }

    @Override
    public <T extends BlockEntity> RegistryHandle<BlockEntityType<T>> registerBlockEntityType(
            String name, Supplier<BlockEntityType<T>> typeFactory) {
        assertOpen("registerBlockEntityType");
        Identifier id = idOf(name);
        BlockEntityType<T> type = typeFactory.get();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenu(
            String name, MenuType.MenuSupplier<T> factory) {
        assertOpen("registerMenu");
        Identifier id = idOf(name);
        MenuType<T> type = new MenuType<>(factory, FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractContainerMenu, BE extends BlockEntity & MenuProvider>
            RegistryHandle<MenuType<T>> registerBlockEntityMenu(
                    String name, Class<BE> blockEntityClass) {
        assertOpen("registerBlockEntityMenu");
        Identifier id = idOf(name);
        MenuType<T> type =
                new ExtendedMenuType<>(
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
                            AbstractContainerMenu menu =
                                    IGUIProvider.createDispatchedMenu(
                                            blockEntityClass.cast(blockEntity),
                                            containerId,
                                            inventory,
                                            inventory.player,
                                            context.dispatchId());
                            if (menu == null)
                                throw new IllegalStateException(
                                        "GUI provider returned no menu for " + id);
                            return (T) menu;
                        },
                        BlockMenuContext.STREAM_CODEC);
        Registry.register(BuiltInRegistries.MENU, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public RegistryHandle<CreativeModeTab> registerCreativeModeTab(
            String name, Consumer<CreativeModeTab.Builder> config) {
        assertOpen("registerCreativeModeTab");
        Identifier id = idOf(name);
        CreativeModeTab.Builder builder = FabricCreativeModeTab.builder();
        config.accept(builder);
        CreativeModeTab tab = builder.build();
        if (SEARCH_BUILDERS.remove(builder)) SEARCH_TABS.add(tab);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
        return RegistryHandle.bound(id, tab);
    }

    @Override
    public Consumer<CreativeModeTab.Builder> searchBar() {
        return builder -> {
            builder.backgroundTexture(CreativeModeTab.createTextureLocation("item_search"));
            SEARCH_BUILDERS.add(builder);
        };
    }

    public static boolean hasSearchBar(CreativeModeTab tab) {
        return SEARCH_TABS.contains(tab);
    }

    @Override
    public void addToVanillaTab(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> entry) {
        CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> output.accept(entry.get()));
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
        Registry.register(BuiltInRegistries.RECIPE_TYPE, id, instance);
        return RegistryHandle.bound(id, instance);
    }

    @Override
    public <T extends Recipe<?>> RegistryHandle<RecipeSerializer<T>> registerRecipeSerializer(
            String name, RecipeSerializer<T> serializer) {
        assertOpen("registerRecipeSerializer");
        Identifier id = idOf(name);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, serializer);
        return RegistryHandle.bound(id, serializer);
    }

    @Override
    public <T extends Recipe<?>> RegistryHandle<RecipeSerializer<T>> registerListedRecipeSerializer(
            String name, RecipeSerializer<T> serializer) {
        RegistryHandle<RecipeSerializer<T>> handle = registerRecipeSerializer(name, serializer);
        RecipeSynchronization.synchronizeRecipeSerializer(serializer);
        return handle;
    }

    @Override
    public <T extends CriterionTrigger<?>> RegistryHandle<T> registerCriterionTrigger(
            String name, T trigger) {
        assertOpen("registerCriterionTrigger");
        Identifier id = idOf(name);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, id, trigger);
        return RegistryHandle.bound(id, trigger);
    }

    @Override
    public <T extends DataComponentPredicate>
            RegistryHandle<DataComponentPredicate.Type<T>> registerDataComponentPredicate(
                    String name, Codec<T> codec) {
        assertOpen("registerDataComponentPredicate");
        Identifier id = idOf(name);
        DataComponentPredicate.Type<T> type = new DataComponentPredicate.ConcreteType<>(codec);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public <T> RegistryHandle<DataComponentType<T>> registerDataComponent(
            String name, DataComponentType<T> type) {
        assertOpen("registerDataComponent");
        Identifier id = idOf(name);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public RegistryHandle<Fluid> registerFluid(String name) {
        assertOpen("registerFluid");
        Identifier id = idOf(name);
        ResourceKey<Fluid> key = ResourceKey.create(Registries.FLUID, id);
        Fluid fluid = new FabricTokenFluid();
        Registry.register(BuiltInRegistries.FLUID, key, fluid);
        FluidVariantAttributes.register(
                fluid,
                new FluidVariantAttributeHandler() {

                    @Override
                    public Component getName(FluidVariant variant) {
                        return NTMFluidProperties.getDisplayName(variant.getFluid());
                    }

                    @Override
                    public int getTemperature(FluidVariant variant) {
                        return FluidPhysics.of(variant.getFluid()).kelvin();
                    }

                    @Override
                    public int getViscosity(FluidVariant variant, @Nullable Level level) {
                        return FluidPhysics.of(variant.getFluid()).viscosity();
                    }

                    @Override
                    public boolean isLighterThanAir(FluidVariant variant) {
                        return FluidPhysics.of(variant.getFluid()).gaseous();
                    }
                });
        return RegistryHandle.bound(id, fluid);
    }

    @Override
    public ClassicFluid.Pair registerClassicFluid(String name, ClassicFluid.Spec spec) {
        assertOpen("registerClassicFluid");
        Identifier sourceId = idOf(name);
        Identifier flowingId = idOf("flowing_" + name);
        Supplier<Fluid> source = () -> BuiltInRegistries.FLUID.getValue(sourceId);
        Supplier<Fluid> flowing = () -> BuiltInRegistries.FLUID.getValue(flowingId);
        ClassicFluid.Source still =
                Registry.register(
                        BuiltInRegistries.FLUID,
                        sourceId,
                        new ClassicFluid.Source(spec, source, flowing));
        ClassicFluid.Flowing flow =
                Registry.register(
                        BuiltInRegistries.FLUID,
                        flowingId,
                        new ClassicFluid.Flowing(spec, source, flowing));
        return new ClassicFluid.Pair(
                RegistryHandle.bound(sourceId, still), RegistryHandle.bound(flowingId, flow));
    }

    @Override
    public RegistryHandle<SoundEvent> registerSound(String name) {
        assertOpen("registerSound");
        Identifier id = idOf(name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
        return RegistryHandle.bound(id, event);
    }

    @Override
    public RegistryHandle<Identifier> registerCustomStat(String name) {
        assertOpen("registerCustomStat");
        Identifier id = idOf(name);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
        return RegistryHandle.bound(id, id);
    }

    @Override
    public <T extends MobEffect> RegistryHandle<T> registerMobEffect(
            String name, Supplier<T> factory) {
        assertOpen("registerMobEffect");
        Identifier id = idOf(name);
        T effect = factory.get();
        Registry.register(BuiltInRegistries.MOB_EFFECT, id, effect);
        return RegistryHandle.bound(id, effect);
    }

    @Override
    public <T extends Entity> RegistryHandle<EntityType<T>> registerEntityType(
            String name, Supplier<EntityType<T>> typeFactory) {
        assertOpen("registerEntityType");
        Identifier id = idOf(name);
        EntityType<T> type = typeFactory.get();
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public <T extends LivingEntity> void registerLivingAttributes(
            RegistryHandle<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {
        assertOpen("registerLivingAttributes");
        FabricDefaultAttributeRegistry.register(type.get(), attributes.get());
    }

    @Override
    public <T extends Mob> void registerSpawnPlacement(
            RegistryHandle<EntityType<T>> type,
            SpawnPlacementType placement,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate) {
        assertOpen("registerSpawnPlacement");
        SpawnPlacements.register(type.get(), placement, heightmap, predicate);
    }

    @Override
    public <T extends ParticleType<?>> RegistryHandle<T> registerParticleType(
            String name, Supplier<T> typeFactory) {
        assertOpen("registerParticleType");
        Identifier id = idOf(name);
        T type = typeFactory.get();
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public RegistryHandle<TicketType> registerTicketType(
            String name, Supplier<TicketType> typeFactory) {
        assertOpen("registerTicketType");
        Identifier id = idOf(name);
        TicketType type = typeFactory.get();
        Registry.register(BuiltInRegistries.TICKET_TYPE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public <T extends Structure> RegistryHandle<StructureType<T>> registerStructureType(
            String name, MapCodec<T> codec) {
        assertOpen("registerStructureType");
        Identifier id = idOf(name);
        StructureType<T> type = () -> codec;
        Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public RegistryHandle<StructurePieceType> registerStructurePieceType(
            String name, StructurePieceType type) {
        assertOpen("registerStructurePieceType");
        Identifier id = idOf(name);
        Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public <T extends StructurePoolElement>
            RegistryHandle<StructurePoolElementType<T>> registerStructurePoolElementType(
                    String name, MapCodec<T> codec) {
        assertOpen("registerStructurePoolElementType");
        Identifier id = idOf(name);
        StructurePoolElementType<T> type = () -> codec;
        Registry.register(BuiltInRegistries.STRUCTURE_POOL_ELEMENT, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public <P extends PlacementModifier>
            RegistryHandle<PlacementModifierType<P>> registerPlacementModifierType(
                    String name, MapCodec<P> codec) {
        assertOpen("registerPlacementModifierType");
        Identifier id = idOf(name);
        PlacementModifierType<P> type = () -> codec;
        Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, id, type);
        return RegistryHandle.bound(id, type);
    }

    @Override
    public <FC extends FeatureConfiguration, F extends Feature<FC>>
            RegistryHandle<F> registerFeature(String name, F feature) {
        assertOpen("registerFeature");
        Identifier id = idOf(name);
        Registry.register(BuiltInRegistries.FEATURE, id, feature);
        return RegistryHandle.bound(id, feature);
    }

    @Override
    public <T extends StructureProcessor>
            RegistryHandle<MapCodec<T>> registerStructureProcessorType(
                    String name, MapCodec<T> codec) {
        assertOpen("registerStructureProcessorType");
        Identifier id = idOf(name);
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, id, codec);
        return RegistryHandle.bound(id, codec);
    }

    @Override
    public <T extends NumberProvider> RegistryHandle<MapCodec<T>> registerLootNumberProviderType(
            String name, MapCodec<T> codec) {
        Identifier id = idOf(name);
        Registry.register(BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE, id, codec);
        return RegistryHandle.bound(id, codec);
    }

    @Override
    public <T> void registerDataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        assertOpen("registerDataPackRegistry");
        DynamicRegistries.register(key, codec);
    }

    @Override
    public <T> void registerSyncedDataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        assertOpen("registerSyncedDataPackRegistry");
        DynamicRegistries.registerSynced(key, codec);
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
}
