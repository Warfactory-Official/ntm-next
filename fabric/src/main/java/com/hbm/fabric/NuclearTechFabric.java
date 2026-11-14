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

package com.hbm.fabric;

import com.hbm.NuclearTech;
import com.hbm.capability.NtmCapabilities;
import com.hbm.client.UpdateChecker;
import com.hbm.command.ModCommands;
import com.hbm.config.ForeignModChange;
import com.hbm.config.HbmConfig;
import com.hbm.config.ModeStatus;
import com.hbm.explosion.BlockConversionTable;
import com.hbm.fabric.compat.CreateGoggles;
import com.hbm.fabric.compat.CreateMountedCrates;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.BobmazonSignHandler;
import com.hbm.handler.CoalGasHandler;
import com.hbm.handler.ExplosiveEnergisticsHandler;
import com.hbm.handler.FuelHandler;
import com.hbm.handler.pollution.GlyphidSpawning;
import com.hbm.handler.pollution.PollutionEffects;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.FluidPropertyReloadListener;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.recipes.ingredient.*;
import com.hbm.items.armor.ModTwoKickItem;
import com.hbm.items.tool.BoltgunItem;
import com.hbm.items.tool.SettingsToolItem;
import com.hbm.packet.PacketDispatcher;
import com.hbm.platform.Caps;
import com.hbm.platform.Network;
import com.hbm.platform.ServerEvents;
import com.hbm.qmaw.QMAWLoader;
import com.hbm.registration.Addon;
import com.hbm.registration.Registrar;
import com.hbm.registration.Shared;
import com.hbm.util.ContagionUtil;
import com.hbm.util.RegistryUtil;
import com.hbm.world.ore.OreGeneration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class NuclearTechFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerEvents.install();
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (server, resources, success) -> {
                    if (success) ModeStatus.broadcast(server);
                });
        NuclearTech.init();
        FabricLoader.getInstance().invokeEntrypoints("hbm:addon", Addon.class, Addon::register);
        Shared.settle();
        Registrar.frozen = true;
        HbmConfig.load();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
            UpdateChecker.start();
        NtmCapabilities.declareAll(System.getProperty("fabric-api.datagen") == null);
        Caps.flush();
        NuclearTech.commonSetup();
        NuclearTech.registerFlammability();
        ArmorModChestLootFabric.init();

        PacketDispatcher.register();
        Network.flush();

        AttackEntityCallback.EVENT.register(
                (player, level, hand, entity, hit) -> {
                    ModTwoKickItem.onPunch(player);
                    return BoltgunItem.handleAttack(player, entity)
                            ? InteractionResult.FAIL
                            : InteractionResult.PASS;
                });

        UseBlockCallback.EVENT.register(
                (player, level, hand, hit) -> {
                    if (player.isSpectator()) return InteractionResult.PASS;
                    if (ExplosiveEnergisticsHandler.detonate(level, hit.getBlockPos()))
                        return InteractionResult.CONSUME;
                    var stack = player.getItemInHand(hand);
                    if (stack.getItem() instanceof SettingsToolItem) {
                        InteractionResult result =
                                SettingsToolItem.useFirst(
                                        stack, new UseOnContext(player, hand, hit));
                        if (result != InteractionResult.PASS) return result;
                    }
                    return BobmazonSignHandler.onSignUsed(level, hit.getBlockPos(), player)
                            ? InteractionResult.SUCCESS
                            : InteractionResult.PASS;
                });

        PlayerBlockBreakEvents.AFTER.register(
                (level, player, pos, state, blockEntity) -> {
                    CoalGasHandler.onBlockBroken(level, pos, state);
                    PollutionEffects.onBlockBroken(player, pos);
                });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> amount > 0F);
        ResourceLoader.get(PackType.SERVER_DATA)
                .registerReloadListener(
                        FluidPropertyReloadListener.ID, new FluidPropertyReloadListener());
        DataResourceLoader.get().registerReloadListener(QMAWLoader.ID, QMAWLoader::new);
        for (ForeignModChange change : ForeignModChange.values()) {
            if (change.neoForge
                    || !FabricLoader.getInstance().isModLoaded(change.modId)
                    || !change.enabled()) continue;
            if (!ResourceLoader.registerBuiltinPack(
                    RegistryUtil.id(change.pack),
                    FabricLoader.getInstance().getModContainer(NuclearTech.MOD_ID).orElseThrow(),
                    Component.translatable(change.titleKey()),
                    PackActivationType.ALWAYS_ENABLED)) {
                throw new IllegalStateException("Missing builtin pack: " + change.pack);
            }
        }
        if (FabricLoader.getInstance().isModLoaded("create")) {
            CreateMountedCrates.register();
            CreateGoggles.register();
        }
        CommonLifecycleEvents.TAGS_LOADED.register(
                (registries, client) -> {
                    HazardSystem.clearCaches();
                    BlockConversionTable.forgetTags();
                    NTMFluidProperties.bindEquivalents();
                });

        ServerLivingEntityEvents.ALLOW_DEATH.register(
                (entity, source, amount) -> !ArmorModHandler.tryRevive(entity));
        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, source) -> {
                    if (source.getEntity() instanceof ServerPlayer killer
                            && !(killer instanceof FakePlayer)) {
                        ArmorModHandler.onKilledByPlayer(entity);
                    }
                    ContagionUtil.onDeath(entity);
                });
        LootTableEvents.MODIFY_DROPS.register(
                (holder, context, drops) -> {
                    if (!ContagionUtil.isContagious(
                            context.getOptional(LootContextParams.THIS_ENTITY))) return;
                    for (ItemStack drop : drops) ContagionUtil.taint(drop);
                });

        DefaultItemComponentEvents.MODIFY.register(
                context ->
                        FuelHandler.forEachUniformFuel(
                                (item, ticks) ->
                                        context.modify(
                                                item.asItem(),
                                                builder ->
                                                        builder.set(
                                                                DataComponents.COOKING_FUEL,
                                                                FuelHandler.cookingFuel(item)))));

        ResourceConditions.register(FlagResourceCondition.TYPE);

        CustomIngredientSerializer.register(HbmMatchersIngredient.SERIALIZER);
        CustomIngredientSerializer.register(HbmFluidContentIngredient.SERIALIZER);
        CustomIngredientSerializer.register(HbmNarrowedIngredient.SERIALIZER);
        CustomIngredientSerializer.register(HbmRecipeIngredient.SERIALIZER);
        CustomIngredientSerializer.register(HbmAnyIngredient.SERIALIZER);

        ServerLifecycleEvents.SERVER_STARTING.register(NuclearTech::serverAboutToStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> NuclearTech.serverStopped());
        ServerLifecycleEvents.SERVER_STARTED.register(OreGeneration::serverStarted);

        EntitySleepEvents.ALLOW_SLEEPING.register(
                (player, pos) -> {
                    GlyphidSpawning.onSleepAttempt(pos);
                    return null;
                });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, access, env) -> ModCommands.register(dispatcher));
    }
}
