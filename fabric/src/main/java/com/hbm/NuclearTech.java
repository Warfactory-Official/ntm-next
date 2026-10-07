// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm;

import com.hbm.advancement.HbmCriteria;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blockentity.BlockEntityPedestal;
import com.hbm.blockentity.PendingCoreInvalidation;
import com.hbm.blockentity.machine.BlockEntityMachineRadar;
import com.hbm.blockentity.network.RTTYSystem;
import com.hbm.blockentity.network.RequestNetwork;
import com.hbm.blocks.FlammableBlocks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.blocks.network.ChunkDuctNodes;
import com.hbm.blocks.network.GeneratedConductors;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.compat.computercraft.ComputerCraft;
import com.hbm.config.*;
import com.hbm.data.*;
import com.hbm.entity.LoadedEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.extprop.ModEntityData;
import com.hbm.handler.*;
import com.hbm.handler.neutron.NeutronHandler;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.radiation.*;
import com.hbm.hazard.HazardAssignment;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.TravelersEquipmentHazards;
import com.hbm.hazard.TrinketsHazards;
import com.hbm.inventory.TrinketsAccessories;
import com.hbm.inventory.WornAccessories;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.machine.CustomMachineDefinitions;
import com.hbm.inventory.recipes.ModRecipes;
import com.hbm.inventory.recipes.ingredient.FluidContentPredicate;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModCreativeTabs;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.food.ConsumableTreatment;
import com.hbm.items.food.ConsumableTreatments;
import com.hbm.items.special.Autogen;
import com.hbm.items.special.ItemBetaFeatures;
import com.hbm.items.tool.ItemRTTYPager;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.Polaroid;
import com.hbm.packet.BeSyncTable;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncWire;
import com.hbm.particle.HbmParticles;
import com.hbm.platform.FabricServerEvents;
import com.hbm.potion.HbmPotion;
import com.hbm.registration.FabricRegistrar;
import com.hbm.registration.Reg;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.TomSaveData;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.ModSounds;
import com.hbm.stats.ModStats;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.util.ChunkUtil;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.world.HbmFeatures;
import com.hbm.world.HbmWorldgen;
import com.hbm.world.gen.nbt.HbmStructureProcessors;
import com.hbm.world.loot.ConfigScaledNumberProvider;
import com.hbm.world.loot.EntityEnchantmentLevel;
import com.hbm.world.ore.OreGeneration;
import com.hbm.world.structure.HbmStructureTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NuclearTech {
    public static final String MOD_ID = "hbm";
    public static final Logger LOGGER = LoggerFactory.getLogger("NTM");
    private static volatile boolean clientFacades;

    private NuclearTech() {}

    public static void init() {
        LOGGER.info("NTM common init on {}", "Fabric");
        Polaroid.roll();
        com.hbm.handler.JoinGreeting.register();

        ModDataComponents.registerBlockItemStates();
        load(ModBlocks.class);
        ModDataComponents.register();
        ModStats.register();
        HbmCriteria.register();
        FluidContentPredicate.register();
        ModEntityData.register();
        GeneratedConductors.init();
        load(ModItems.class);
        Autogen.register();
        load(ModBlockEntities.class);
        ModEntities.register();
        ModMenus.register();
        ModCreativeTabs.register();
        ModRecipes.register();
        ModSounds.register();
        HbmParticles.register();
        ChunkUtil.register();
        HbmPotion.register();
        HbmStructureTypes.register();
        HbmFeatures.register();
        HbmStructureProcessors.register();
        FabricRegistrar.registerLootNumberProviderType(
                "config_scaled", ConfigScaledNumberProvider.MAP_CODEC);
        FabricRegistrar.registerLootNumberProviderType(
                "entity_enchantment_level", EntityEnchantmentLevel.MAP_CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(
                HazardAssignment.REGISTRY, HazardAssignment.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(
                HazmatProtection.REGISTRY, HazmatProtection.CODEC);
        FabricRegistrar.registerDataPackRegistry(BiomeRadiation.REGISTRY, BiomeRadiation.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(
                EquipmentProtection.REGISTRY, EquipmentProtection.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(
                SuitProtection.REGISTRY, SuitProtection.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(
                ConsumableTreatment.REGISTRY, ConsumableTreatment.CODEC);
        FabricRegistrar.registerDataPackRegistry(
                IrradiationTransformation.REGISTRY, IrradiationTransformation.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(
                CustomMachineDefinition.REGISTRY, CustomMachineDefinition.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(MachineData.REGISTRY, MachineConfig.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(ExplosionData.REGISTRY, DataGroup.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(MobData.REGISTRY, DataGroup.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(WorldData.REGISTRY, DataGroup.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(ItemData.REGISTRY, DataGroup.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(EnergyData.REGISTRY, DataGroup.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(RadiationData.REGISTRY, DataGroup.CODEC);
        FabricRegistrar.registerDataPackRegistry(
                RadiationSettings.REGISTRY, RadiationSettings.CODEC);
        FabricRegistrar.registerDataPackRegistry(
                RadiationDiffusivity.REGISTRY, RadiationDiffusivity.CODEC);
        FabricRegistrar.registerDataPackRegistry(RadSourceRule.REGISTRY, RadSourceRule.CODEC);
        FabricRegistrar.registerSyncedDataPackRegistry(
                RadResistanceRule.REGISTRY, RadResistanceRule.CODEC);
        FabricRegistrar.registerDataPackRegistry(
                FalloutConfigJSON.REGISTRY, FalloutConfigJSON.CODEC);
        NtmCapabilities.registerTokens();
        NTMFluids.init();
        PowerNetwork.init();
        FluidNetwork.init();
        PneumaticNetwork.init();
        HazardSystem.init();
        RadiationSystemNT.init();
        RadVisServer.init();
        PollutionHandler.init();
        UnstableFuses.init();
        LoadedEntities.init();
        DamageResistanceHandler.init();
        BossSpawnHandler.init();
        ImpactWorldHandler.init();
        BlockEntityPedestal.init();
        ArmorSuitEffects.init();
        ChunkUtil.init();
        PendingCoreInvalidation.init();
        AssembledMembers.init();
        ResolvedCapCache.init();
        EndpointRegistry.init();
        ChunkDuctNodes.init();
        ComputerCraft.init();
        HbmWorldgen.register();

        FabricServerEvents.onServerTickPost(
                server -> {
                    if (server.getTickCount() % 20 == 0 && FabricHbmConfig.pollForExternalEdit()) {
                        deriveLiveConfigFacades();
                        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
                            deriveClientConfigFacades();
                    }
                });

        FabricServerEvents.onServerTickPost(NeutronHandler::onServerTick);
        FabricServerEvents.onServerStopping(server -> NeutronNodeWorld.removeAllWorlds());

        FabricServerEvents.onServerTickPost(
                server -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        AkimboGhost.tick(player);
                    }
                });

        FabricServerEvents.onLevelLoad(TomSaveData::publish);
        FabricServerEvents.onServerStopping(server -> TomSaveData.onServerStopping());
        FabricServerEvents.onServerTickPre(ItemBetaFeatures::tick);
        FabricServerEvents.onServerTickPre(RTTYSystem::updateBroadcastQueue);
        FabricServerEvents.onServerTickPre(RequestNetwork::updateEntries);
        BlockEntityMachineRadar.registerScanSystem();
        FabricServerEvents.onServerStopping(server -> RTTYSystem.onServerStopping());
        FabricServerEvents.onServerTickPre(SatelliteDetector::tick);
        FabricServerEvents.onServerTickPre(
                server -> {
                    for (ServerLevel level : server.getAllLevels()) {
                        SatelliteSavedData data = SatelliteSavedData.get(level);
                        for (var satellite : data.sats.values()) satellite.onUpdateTick(level);
                    }
                });
        FabricServerEvents.onServerTickPre(SatelliteRayEvents::tick);
        FabricServerEvents.onServerStopping(server -> SatelliteRayEvents.clear());
        FabricServerEvents.onServerStopping(server -> SatelliteDetector.clear());
        FabricServerEvents.onServerStopping(server -> RequestNetwork.onServerStopping());
        FabricServerEvents.onServerTickPost(ItemRTTYPager::tickInventory);
        FabricServerEvents.onServerTickPost(server -> BeSyncTable.pump());
        FabricServerEvents.onServerTickPre(server -> SyncWire.beforeTick());
        FabricServerEvents.onServerTickPost(EntityRailCarBase::updateTrains);
        FabricServerEvents.onPlayerDisconnect(
                player -> {
                    ChunkTrackerIndex.forget(player, player.level());
                    BeSyncTable.forget(player.getUUID());
                });
        FabricServerEvents.onPlayerRespawn(ChunkTrackerIndex::respawn);
        FabricServerEvents.onPlayerChangeLevel(
                (player, origin, destination) -> {
                    ChunkTrackerIndex.forget(player, origin);
                    BeSyncTable.forgetIn(origin, player.getUUID());
                });
        FabricServerEvents.onServerTickPost(server -> PacketWire.flushPending());
    }

    private static void load(Class<?>... holders) {
        for (Class<?> holder : holders) {
            try {
                Class.forName(holder.getName(), true, holder.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "content holder is missing: " + holder.getName(), e);
            }
        }
    }

    public static void deriveConfigFacades() {
        BalanceConfig.loadFrom(FabricHbmConfig.content().store());
        deriveLiveConfigFacades();
        if (clientFacades) deriveClientConfigFacades();
    }

    public static void deriveLiveConfigFacades() {
        ConfigStore runtime = FabricHbmConfig.runtime().store();
        RadiationConfig.loadFrom(runtime);
        BombConfig.loadFrom(runtime);
    }

    public static void deriveClientConfigFacades() {
        ConfigStore client = FabricHbmConfig.STORE;
        RenderConfig.loadFrom(client);
        HudConfig.loadFrom(client);
        GunVisualConfig.loadFrom(client);
        InteractionConfig.loadFrom(client);
        ConfigSchema.loadAddonClientFacades(client);
        clientFacades = true;
    }

    public static void commonSetup() {
        deriveConfigFacades();
        RadiationSystemNT.onLoadComplete();
        Reg.runAfterRegistration();
        NTMFluids.bake();
        com.hbm.inventory.ItemContents.bootstrap();
        XWeaponModManager.init();
        HazardSystem.registerContent();
        if (FabricLoader.getInstance().isModLoaded("travelersbackpack")) {
            HazardSystem.addExternalEquipmentApplicator(TravelersEquipmentHazards::apply);
        }
        if (FabricLoader.getInstance().isModLoaded("trinkets_updated")) {
            HazardSystem.addExternalEquipmentApplicator(TrinketsHazards::apply);
            WornAccessories.register(TrinketsAccessories::collect);
        }
        ModItems.initHazards();
        DispenserBehaviorHandler.init();
    }

    public static void registerFlammability() {
        FlammableBlocks.register();
    }

    public static void serverAboutToStart(MinecraftServer server) {
        assert server.levelKeys().isEmpty();
        DataGroups.applyAll(server.registryAccess());
        PacketWire.onServerStarting(server);
        ModRecipes.bootstrap();
        ItemPool.load(server.registryAccess());
        HazardSystem.applyDataPack(server.registryAccess());
        HazmatRegistry.applyDataPack(server.registryAccess());
        BiomeRadiation.applyDataPack(server.registryAccess());
        EquipmentProtections.applyDataPack(server.registryAccess());
        SuitProtections.applyDataPack(server.registryAccess());
        ConsumableTreatments.applyDataPack(server.registryAccess());
        IrradiationTransformations.applyDataPack(server.registryAccess());
        CustomMachineDefinitions.applyDataPack(server.registryAccess());
        RadiationSystemNT.registerConstantSources(server.registryAccess());
        RadiationSystemNT.registerResistantStates(server.registryAccess());
    }

    public static void serverStopped() {
        HazmatRegistry.clear();
        BiomeRadiation.clear();
        EquipmentProtections.clear();
        SuitProtections.clear();
        ConsumableTreatments.clear();
        IrradiationTransformations.clear();
        OreGeneration.serverStopped();
        PacketWire.onServerStopped();
        BobmazonOffers.invalidate();
    }
}
