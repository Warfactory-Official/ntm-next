// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge;

import com.hbm.NuclearTech;
import com.hbm.capability.CapabilityInvalidation;
import com.hbm.capability.NtmCapabilities;
import com.hbm.command.ModCommands;
import com.hbm.config.ForeignModChange;
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.BobmazonSignHandler;
import com.hbm.handler.CoalGasHandler;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.hazard.HazardSystem;
import com.hbm.integration.top.NTMTopPlugin;
import com.hbm.inventory.WornAccessories;
import com.hbm.inventory.fluid.FluidPropertyReloadListener;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.recipes.ingredient.*;
import com.hbm.items.armor.ItemModTwoKick;
import com.hbm.items.tool.ItemBoltgun;
import com.hbm.items.tool.ItemDeadManDetonator;
import com.hbm.lib.Library;
import com.hbm.neoforge.hazard.CuriosHazards;
import com.hbm.neoforge.hazard.HazardTransformerSophisticatedBackpacks;
import com.hbm.neoforge.hazard.HazardTransformerSophisticatedStorage;
import com.hbm.neoforge.inventory.CuriosAccessories;
import com.hbm.packet.PacketDispatcher;
import com.hbm.platform.NeoForgeCaps;
import com.hbm.platform.NeoForgeNetwork;
import com.hbm.platform.NeoForgeServerEvents;
import com.hbm.qmaw.QMAWLoader;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.util.ContagionUtil;
import com.hbm.util.EntityDamageUtil;
import com.hbm.world.ore.OreGeneration;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(NuclearTech.MOD_ID)
public final class NuclearTechNeoForge {
    public NuclearTechNeoForge(IEventBus modBus, ModContainer container) {
        NeoForgeServerEvents.install();
        NeoForgeRegistrar.subscribe(modBus);
        NuclearTech.init();
        NeoForge.EVENT_BUS.addListener(CapabilityInvalidation::onLevelTick);
        NeoForge.EVENT_BUS.addListener(ArmorModChestLootNeoForge::onLootTableLoad);
        modBus.addListener(
                (AddPackFindersEvent event) -> {
                    for (ForeignModChange change : ForeignModChange.values()) {
                        if (!change.neoForge
                                || !ModList.get().isLoaded(change.modId)
                                || !change.enabled()) continue;
                        event.addPackFinders(
                                Library.id("resourcepacks/" + change.pack),
                                PackType.SERVER_DATA,
                                Component.translatable(change.titleKey()),
                                PackSource.BUILT_IN,
                                true,
                                Pack.Position.TOP);
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (AddServerReloadListenersEvent event) ->
                        event.addListener(
                                FluidPropertyReloadListener.ID, new FluidPropertyReloadListener()));
        NeoForge.EVENT_BUS.addListener(
                (AddServerReloadListenersEvent event) ->
                        event.addListener(QMAWLoader.ID, new QMAWLoader()));
        NeoForge.EVENT_BUS.addListener(
                (TagsUpdatedEvent.ServerDataLoad event) -> {
                    HazardSystem.clearCaches();
                    NTMFluidProperties.bindEquivalents();
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerInteractEvent.RightClickBlock event) -> {
                    if (BobmazonSignHandler.onSignUsed(
                            event.getLevel(), event.getPos(), event.getEntity()))
                        event.setCanceled(true);
                });
        NeoForge.EVENT_BUS.addListener(
                (BreakBlockEvent event) -> {
                    CoalGasHandler.onBlockBroken(
                            (Level) event.getLevel(), event.getPos(), event.getState());
                    PollutionHandler.onBlockBroken(event.getPlayer(), event.getPos());
                });
        NeoForge.EVENT_BUS.addListener(
                (AttackEntityEvent event) -> {
                    ItemModTwoKick.onPunch(event.getEntity());
                    if (ItemBoltgun.handleAttack(event.getEntity(), event.getTarget()))
                        event.setCanceled(true);
                });
        NeoForge.EVENT_BUS.addListener(
                (LivingIncomingDamageEvent event) -> {
                    if (!EntityDamageUtil.allowsAttack(
                            event.getEntity(), event.getSource(), event.getAmount()))
                        event.setCanceled(true);
                });
        NeoForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                (LivingDeathEvent event) -> {
                    if (ArmorModHandler.tryRevive(event.getEntity())) event.setCanceled(true);
                });
        NeoForge.EVENT_BUS.addListener(
                (LivingDeathEvent event) -> {
                    if (event.getSource().getEntity() instanceof ServerPlayer killer
                            && !(killer instanceof FakePlayer)) {
                        ArmorModHandler.onKilledByPlayer(event.getEntity());
                    }
                    ContagionUtil.onDeath(event.getEntity());
                });
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                (LivingDeathEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        ItemDeadManDetonator.onOwnerDeath(player);
                });
        NeoForge.EVENT_BUS.addListener(
                (LivingDropsEvent event) -> {
                    if (!ContagionUtil.isContagious(event.getEntity())) return;
                    for (ItemEntity drop : event.getDrops()) ContagionUtil.taint(drop.getItem());
                });
        NeoForge.EVENT_BUS.addListener(
                (ItemTossEvent event) -> ArmorModHandler.onItemTossed(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.ItemSmeltedEvent event) ->
                        ArmorModHandler.onItemSmelted(event.getEntity(), event.getSmelting()));
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerChangedDimensionEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        HbmPlayerProps.getData(player).grenadeDeployment = 0;
                });
        NeoForgeHbmConfig.registerSpecs(container);

        DeferredRegister<MapCodec<? extends ICondition>> conditionCodecs =
                DeferredRegister.create(
                        NeoForgeRegistries.Keys.CONDITION_CODECS, NuclearTech.MOD_ID);
        conditionCodecs.register("flag", () -> FlagCondition.CODEC);
        conditionCodecs.register(modBus);

        HbmMatchersIngredient.subscribe(modBus);
        HbmFluidContentIngredient.subscribe(modBus);
        HbmNarrowedIngredient.subscribe(modBus);
        HbmRecipeIngredient.subscribe(modBus);
        HbmAnyIngredient.subscribe(modBus);

        PacketDispatcher.register();
        modBus.addListener((RegisterPayloadHandlersEvent event) -> NeoForgeNetwork.flush(event));
        modBus.addListener(
                (RegisterConfigurationTasksEvent event) ->
                        NeoForgeNetwork.registerConfigurationTasks(event));
        modBus.addListener(
                (RegisterCapabilitiesEvent event) -> {
                    NtmCapabilities.declareAll();
                    NeoForgeCaps.flush(event);
                    if (ModList.get().isLoaded("ae2"))
                        com.hbm.integration.ae2.AE2Capabilities.register(event);
                });
        modBus.addListener(
                (FMLCommonSetupEvent event) -> {
                    NuclearTech.commonSetup();
                    if (ModList.get().isLoaded("sophisticatedstorage")) {
                        HazardSystem.addTransformer(new HazardTransformerSophisticatedStorage());
                        com.hbm.neoforge.inventory.SophisticatedItemInteriors.registerStorage();
                    }
                    if (ModList.get().isLoaded("sophisticatedbackpacks")) {
                        HazardSystem.addTransformer(new HazardTransformerSophisticatedBackpacks());
                        com.hbm.neoforge.inventory.SophisticatedItemInteriors.registerBackpacks();
                    }
                    if (ModList.get().isLoaded("curios")) {
                        HazardSystem.addExternalEquipmentApplicator(CuriosHazards::apply);
                        WornAccessories.register(CuriosAccessories::collect);
                    }
                    event.enqueueWork(NuclearTech::registerFlammability);
                });
        modBus.addListener(
                (ModConfigEvent.Reloading event) -> {
                    if (event.getConfig().getType() == ModConfig.Type.CLIENT) return;
                    NeoForgeHbmConfig.externalEdit = true;
                });
        if (ModList.get().isLoaded("theoneprobe")) {
            modBus.addListener(
                    (InterModEnqueueEvent event) ->
                            InterModComms.sendTo(
                                    "theoneprobe", "getTheOneProbe", NTMTopPlugin::new));
        }

        NeoForge.EVENT_BUS.addListener(
                (ServerAboutToStartEvent event) ->
                        NuclearTech.serverAboutToStart(event.getServer()));
        NeoForge.EVENT_BUS.addListener(
                (OnDatapackSyncEvent event) -> NeoForgeNetwork.onDatapackSync(event));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> NuclearTech.serverStopped());
        NeoForge.EVENT_BUS.addListener(
                (ServerStartedEvent event) -> OreGeneration.serverStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener(
                (LevelEvent.PotentialSpawns event) -> {
                    if (!event.isCanceled()
                            && event.getLevel() instanceof ServerLevel serverLevel) {
                        PollutionHandler.rampantScoutPopulator(serverLevel, event.getPos());
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (MobSpawnEvent.PositionCheck event) -> {
                    EntitySpawnReason reason = event.getSpawnType();
                    if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.SPAWNER)
                            && ImpactWorldHandler.deniesSpawn(
                                    event.getLevel().getLevel(), event.getEntity())) {
                        event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (CanPlayerSleepEvent event) ->
                        PollutionHandler.rampantTargetSetter(event.getPos()));
        NeoForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) -> ModCommands.register(event.getDispatcher()));
    }
}
