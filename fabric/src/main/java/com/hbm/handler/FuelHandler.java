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

package com.hbm.handler;

import com.hbm.NuclearTech;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.machine.AshType;
import com.hbm.items.variant.BriquetteType;
import com.hbm.registration.Registrar;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

public final class FuelHandler {

    private static final int SINGLE = 200;
    private static final List<Consumer<ObjIntConsumer<ItemLike>>> ADDONS = new ArrayList<>();

    private FuelHandler() {}

    public static int getBurnTime(BlockEntity entity, ItemStack fuel) {
        if (!fuel.has(DataComponents.COOKING_FUEL)) return 0;
        LootParams.Builder params =
                new LootParams.Builder((ServerLevel) entity.getLevel())
                        .withParameter(LootContextParams.BLOCK_STATE, entity.getBlockState())
                        .withParameter(LootContextParams.BLOCK_ENTITY, entity)
                        .withParameter(
                                LootContextParams.ORIGIN, Vec3.atCenterOf(entity.getBlockPos()))
                        .withParameter(LootContextParams.CONTAINER, (Container) entity);
        LootContext context =
                new LootContext.Builder(params.create(LootContextParamSets.CONTAINER_PROCESS))
                        .create(Optional.empty());
        return ResolvableInt.getFromItem(
                fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
    }

    public static boolean isFuel(BlockEntity entity, ItemStack stack) {
        return entity.getLevel().isClientSide()
                ? stack.has(DataComponents.COOKING_FUEL)
                : getBurnTime(entity, stack) > 0;
    }

    public static ResourceKey<ContextIntProvider> burnTimeKey(ItemLike item) {
        var id = BuiltInRegistries.ITEM.getKey(item.asItem());
        String path =
                id.getNamespace().equals(NuclearTech.MOD_ID)
                        ? id.getPath()
                        : id.getNamespace() + "/" + id.getPath();
        return ResourceKey.create(
                Registries.CONTEXT_INT_PROVIDER, RegistryUtil.id("cooking/fuel/" + path));
    }

    public static CookingFuel cookingFuel(ItemLike item) {
        return new CookingFuel(
                ResolvableInt.fromKey(burnTimeKey(item)),
                ResolvableFloat.fromKey(ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER));
    }

    public static void addon(Consumer<ObjIntConsumer<ItemLike>> fuels) {
        Registrar.assertOpen("FuelHandler.addon");
        ADDONS.add(fuels);
    }

    public static void forEachAddonFuel(ObjIntConsumer<ItemLike> out) {
        for (Consumer<ObjIntConsumer<ItemLike>> fuels : ADDONS) fuels.accept(out);
    }

    public static void forEachUniformFuel(ObjIntConsumer<ItemLike> out) {
        forEachNtmFuel(out);
        forEachAddonFuel(out);
    }

    public static void forEachNtmFuel(ObjIntConsumer<ItemLike> out) {
        out.accept(ModItems.SOLID_FUEL.get(), SINGLE * 16);
        out.accept(ModItems.SOLID_FUEL_PRESTO.get(), SINGLE * 40);
        out.accept(ModItems.SOLID_FUEL_PRESTO_TRIPLET.get(), SINGLE * 200);
        out.accept(ModItems.SOLID_FUEL_BF.get(), SINGLE * 160);
        out.accept(ModItems.SOLID_FUEL_PRESTO_BF.get(), SINGLE * 400);
        out.accept(ModItems.SOLID_FUEL_PRESTO_TRIPLET_BF.get(), SINGLE * 2000);
        out.accept(ModItems.ROCKET_FUEL.get(), SINGLE * 32);

        out.accept(ModItems.BIOMASS.get(), SINGLE * 2);
        out.accept(ModItems.BIOMASS_COMPRESSED.get(), SINGLE * 4);
        out.accept(ModItems.powder(Materials.COAL), SINGLE * 8);
        out.accept(ModItems.SCRAP.get(), SINGLE / 4);
        out.accept(ModItems.DUST.get(), SINGLE / 8);
        out.accept(ModBlocks.BLOCK_SCRAP.get(), SINGLE * 2);
        out.accept(ModItems.POWDER_FIRE.get(), 6400);
        out.accept(ModItems.LIGNITE.get(), 1200);
        out.accept(ModItems.powder(Materials.LIGNITE), 1200);
        for (var coke : ModItems.COKE) out.accept(coke.get(), SINGLE * 16);
        out.accept(ModBlocks.BLOCK_COKE_COAL.get(), SINGLE * 160);
        out.accept(ModBlocks.BLOCK_COKE_LIGNITE.get(), SINGLE * 160);
        out.accept(ModBlocks.BLOCK_COKE_PETROLEUM.get(), SINGLE * 160);
        for (var book : ModItems.GUIDE_BOOK) out.accept(book.get(), SINGLE);
        out.accept(ModItems.COAL_INFERNAL.get(), 4800);
        out.accept(ModItems.COAL_ETERNAL.get(), SINGLE * 16);
        out.accept(ModItems.CRYSTAL_COAL.get(), 6400);
        out.accept(ModItems.POWDER_SAWDUST.get(), SINGLE / 2);
        out.accept(ModItems.BRIQUETTE.get(BriquetteType.COAL), SINGLE * 10);
        out.accept(ModItems.BRIQUETTE.get(BriquetteType.LIGNITE), SINGLE * 8);
        out.accept(ModItems.BRIQUETTE.get(BriquetteType.WOOD), SINGLE * 2);
        out.accept(ModItems.POWDER_ASH.get(AshType.WOOD), SINGLE / 2);
        out.accept(ModItems.POWDER_ASH.get(AshType.COAL), SINGLE);
        out.accept(ModItems.POWDER_ASH.get(AshType.MISC), SINGLE / 2);
        out.accept(ModItems.POWDER_ASH.get(AshType.FLY), SINGLE);
        out.accept(ModItems.POWDER_ASH.get(AshType.SOOT), SINGLE / 2);

        out.accept(ModBlocks.WOOD_BARRIER.get(), 300);
        out.accept(ModBlocks.WOOD_STRUCTURE_ROOF.get(), 300);
        out.accept(ModBlocks.WOOD_STRUCTURE_SCAFFOLD.get(), 300);
        out.accept(ModBlocks.WOOD_STRUCTURE_CEILING.get(), 300);
        out.accept(ModBlocks.WASTE_LOG.get(), 300);
        out.accept(ModBlocks.WASTE_PLANKS.get(), 300);
        out.accept(ModBlocks.FROZEN_LOG.get(), 300);
        out.accept(ModBlocks.FROZEN_PLANKS.get(), 300);
        out.accept(ModBlocks.RED_PYLON_MEDIUM_WOOD.get(), 300);
        out.accept(ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get(), 300);
        out.accept(ModBlocks.RADIO_TELEX.get(), 300);
        out.accept(ModBlocks.CRATE_SUPPLY.get(), 300);
        out.accept(ModBlocks.CRATE.get(), 300);
        out.accept(ModBlocks.CRATE_WEAPON.get(), 300);
        out.accept(ModBlocks.CRATE_CAN.get(), 300);
        out.accept(ModItems.WOOD_GAVEL.get(), 200);
        out.accept(ModBlocks.PINK_LOG.get(), 300);
        out.accept(
                BuiltInRegistries.ITEM
                        .getOrThrow(
                                ResourceKey.create(Registries.ITEM, RegistryUtil.id("pink_planks")))
                        .value(),
                300);
        out.accept(
                BuiltInRegistries.ITEM
                        .getOrThrow(
                                ResourceKey.create(Registries.ITEM, RegistryUtil.id("pink_slab")))
                        .value(),
                150);
        out.accept(
                BuiltInRegistries.ITEM
                        .getOrThrow(
                                ResourceKey.create(Registries.ITEM, RegistryUtil.id("pink_stairs")))
                        .value(),
                300);
    }
}
