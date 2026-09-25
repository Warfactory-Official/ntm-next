// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.particles.ParticleType;
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

public interface IRegistrar {

    default Identifier idOf(String path) {
        return Library.id(path);
    }

    void freeze();

    List<RegistryHandle<? extends Block>> blocks();

    <B extends Block> RegistryHandle<B> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> props);

    <I extends Item> RegistryHandle<I> registerItem(
            String name,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props);

    <I extends Item> RegistryHandle<I> registerItem(
            String name,
            Identifier model,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> props);

    <T extends BlockEntity> RegistryHandle<BlockEntityType<T>> registerBlockEntityType(
            String name, Supplier<BlockEntityType<T>> typeFactory);

    <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenu(
            String name, MenuType.MenuSupplier<T> factory);

    <T extends AbstractContainerMenu, BE extends BlockEntity & MenuProvider>
            RegistryHandle<MenuType<T>> registerBlockEntityMenu(
                    String name, Class<BE> blockEntityClass);

    RegistryHandle<CreativeModeTab> registerCreativeModeTab(
            String name, Consumer<CreativeModeTab.Builder> config);

    Consumer<CreativeModeTab.Builder> searchBar();

    void addToVanillaTab(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> entry);

    <T extends Recipe<?>> RegistryHandle<RecipeType<T>> registerRecipeType(String name);

    <T extends Recipe<?>> RegistryHandle<RecipeType<T>> registerRecipeType(
            String name, RecipeType<T> instance);

    <T extends Recipe<?>> RegistryHandle<RecipeSerializer<T>> registerRecipeSerializer(
            String name, RecipeSerializer<T> serializer);

    default <T extends Recipe<?>>
            RegistryHandle<RecipeSerializer<T>> registerListedRecipeSerializer(
                    String name, RecipeSerializer<T> serializer) {
        return registerRecipeSerializer(name, serializer);
    }

    <T extends CriterionTrigger<?>> RegistryHandle<T> registerCriterionTrigger(
            String name, T trigger);

    <T extends DataComponentPredicate>
            RegistryHandle<DataComponentPredicate.Type<T>> registerDataComponentPredicate(
                    String name, Codec<T> codec);

    <T> RegistryHandle<DataComponentType<T>> registerDataComponent(
            String name, DataComponentType<T> type);

    RegistryHandle<Fluid> registerFluid(String name);

    ClassicFluid.Pair registerClassicFluid(String name, ClassicFluid.Spec spec);

    RegistryHandle<SoundEvent> registerSound(String name);

    RegistryHandle<Identifier> registerCustomStat(String name);

    <T extends MobEffect> RegistryHandle<T> registerMobEffect(String name, Supplier<T> factory);

    <T extends ParticleType<?>> RegistryHandle<T> registerParticleType(
            String name, Supplier<T> typeFactory);

    RegistryHandle<TicketType> registerTicketType(String name, Supplier<TicketType> typeFactory);

    <T extends Entity> RegistryHandle<EntityType<T>> registerEntityType(
            String name, Supplier<EntityType<T>> typeFactory);

    <T extends LivingEntity> void registerLivingAttributes(
            RegistryHandle<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes);

    <T extends Mob> void registerSpawnPlacement(
            RegistryHandle<EntityType<T>> type,
            SpawnPlacementType placement,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate);

    <T extends Structure> RegistryHandle<StructureType<T>> registerStructureType(
            String name, MapCodec<T> codec);

    RegistryHandle<StructurePieceType> registerStructurePieceType(
            String name, StructurePieceType type);

    <T extends StructurePoolElement>
            RegistryHandle<StructurePoolElementType<T>> registerStructurePoolElementType(
                    String name, MapCodec<T> codec);

    <P extends PlacementModifier>
            RegistryHandle<PlacementModifierType<P>> registerPlacementModifierType(
                    String name, MapCodec<P> codec);

    <FC extends FeatureConfiguration, F extends Feature<FC>> RegistryHandle<F> registerFeature(
            String name, F feature);

    <T extends StructureProcessor> RegistryHandle<MapCodec<T>> registerStructureProcessorType(
            String name, MapCodec<T> codec);

    <T extends NumberProvider> RegistryHandle<MapCodec<T>> registerLootNumberProviderType(
            String name, MapCodec<T> codec);

    <T> void registerDataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec);

    <T> void registerSyncedDataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec);
}
