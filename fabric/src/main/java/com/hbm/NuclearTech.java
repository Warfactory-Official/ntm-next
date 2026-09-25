// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm;

import com.hbm.advancement.HbmCriteria;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blocks.FlammableBlocks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.compat.computercraft.ComputerCraft;
import com.hbm.config.BalanceConfig;
import com.hbm.config.BombConfig;
import com.hbm.config.ConfigStore;
import com.hbm.config.FalloutConfigJSON;
import com.hbm.config.GunVisualConfig;
import com.hbm.config.HudConfig;
import com.hbm.config.InteractionConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.config.RenderConfig;
import com.hbm.data.DataGroup;
import com.hbm.data.DataGroups;
import com.hbm.data.EnergyData;
import com.hbm.data.ExplosionData;
import com.hbm.data.ItemData;
import com.hbm.data.MachineConfig;
import com.hbm.data.MachineData;
import com.hbm.data.MobData;
import com.hbm.data.RadiationData;
import com.hbm.data.WorldData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.extprop.ModEntityData;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.BobmazonOffers;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.handler.DispenserBehaviorHandler;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.handler.UnstableFuses;
import com.hbm.handler.neutron.NeutronHandler;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.radiation.RadSourceRule;
import com.hbm.handler.radiation.RadVisServer;
import com.hbm.handler.radiation.RadiationDiffusivity;
import com.hbm.handler.radiation.RadiationSettings;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.hazard.HazardAssignment;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.TravelersEquipmentHazards;
import com.hbm.hazard.TrinketsHazards;
import com.hbm.hazard.transformer.HazardTransformerCompactStorage;
import com.hbm.hazard.transformer.HazardTransformerTravelersBackpack;
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
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import com.hbm.registration.Reg;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.TomSaveData;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.ModSounds;
import com.hbm.stats.ModStats;
import com.hbm.tileentity.BlockEntityPedestal;
import com.hbm.tileentity.PendingCoreInvalidation;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.RequestNetwork;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.util.ChunkUtil;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.world.HbmFeatures;
import com.hbm.world.HbmWorldgen;
import com.hbm.world.gen.nbt.HbmStructureProcessors;
import com.hbm.world.loot.ConfigScaledNumberProvider;
import com.hbm.world.structure.HbmStructureTypes;
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
        LOGGER.info("NTM common init on {}", Services.PLATFORM.getPlatformName());
        Polaroid.roll();

        ModDataComponents.registerBlockItemStates(Services.REGISTRAR);
        load(ModBlocks.class);
        ModDataComponents.register(Services.REGISTRAR);
        ModStats.register(Services.REGISTRAR);
        HbmCriteria.register(Services.REGISTRAR);
        FluidContentPredicate.register(Services.REGISTRAR);
        ModEntityData.register();
        load(ModItems.class);
        Autogen.register();
        load(ModBlockEntities.class);
        ModEntities.register(Services.REGISTRAR);
        ModMenus.register(Services.REGISTRAR);
        ModCreativeTabs.register(Services.REGISTRAR);
        ModRecipes.register(Services.REGISTRAR);
        ModSounds.register(Services.REGISTRAR);
        HbmParticles.register(Services.REGISTRAR);
        ChunkUtil.register(Services.REGISTRAR);
        HbmPotion.register(Services.REGISTRAR);
        HbmStructureTypes.register(Services.REGISTRAR);
        HbmFeatures.register(Services.REGISTRAR);
        HbmStructureProcessors.register(Services.REGISTRAR);

        Services.REGISTRAR.registerLootNumberProviderType(
                "config_scaled", ConfigScaledNumberProvider.MAP_CODEC);

        Services.REGISTRAR.registerSyncedDataPackRegistry(
                HazardAssignment.REGISTRY, HazardAssignment.CODEC);

        Services.REGISTRAR.registerSyncedDataPackRegistry(
                CustomMachineDefinition.REGISTRY, CustomMachineDefinition.CODEC);
        Services.REGISTRAR.registerSyncedDataPackRegistry(
                MachineData.REGISTRY, MachineConfig.CODEC);
        Services.REGISTRAR.registerSyncedDataPackRegistry(ExplosionData.REGISTRY, DataGroup.CODEC);
        Services.REGISTRAR.registerSyncedDataPackRegistry(MobData.REGISTRY, DataGroup.CODEC);
        Services.REGISTRAR.registerSyncedDataPackRegistry(WorldData.REGISTRY, DataGroup.CODEC);
        Services.REGISTRAR.registerSyncedDataPackRegistry(ItemData.REGISTRY, DataGroup.CODEC);
        Services.REGISTRAR.registerSyncedDataPackRegistry(EnergyData.REGISTRY, DataGroup.CODEC);
        Services.REGISTRAR.registerSyncedDataPackRegistry(RadiationData.REGISTRY, DataGroup.CODEC);
        Services.REGISTRAR.registerDataPackRegistry(
                RadiationSettings.REGISTRY, RadiationSettings.CODEC);
        Services.REGISTRAR.registerDataPackRegistry(
                RadiationDiffusivity.REGISTRY, RadiationDiffusivity.CODEC);
        Services.REGISTRAR.registerDataPackRegistry(RadSourceRule.REGISTRY, RadSourceRule.CODEC);
        Services.REGISTRAR.registerDataPackRegistry(
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
        ArmorModHandler.init();
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
        ComputerCraft.init();
        HbmWorldgen.register();

        Services.SERVER.onServerTickPost(
                server -> {
                    if (server.getTickCount() % 20 == 0 && Services.CONFIG.pollForExternalEdit()) {
                        deriveLiveConfigFacades();
                        if (Services.PLATFORM.isPhysicalClient()) deriveClientConfigFacades();
                    }
                });

        Services.SERVER.onServerTickPost(NeutronHandler::onServerTick);
        Services.SERVER.onServerStopping(server -> NeutronNodeWorld.removeAllWorlds());

        Services.SERVER.onServerTickPost(
                server -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        AkimboGhost.tick(player);
                    }
                });

        Services.SERVER.onLevelLoad(TomSaveData::publish);
        Services.SERVER.onServerStopping(server -> TomSaveData.onServerStopping());

        Services.SERVER.onServerTickPre(ItemBetaFeatures::tick);

        Services.SERVER.onServerTickPre(RTTYSystem::updateBroadcastQueue);

        Services.SERVER.onServerTickPre(RequestNetwork::updateEntries);

        BlockEntityMachineRadar.registerScanSystem();
        Services.SERVER.onServerTickPre(BlockEntityMachineRadar::updateSystem);

        Services.SERVER.onServerStopping(server -> BlockEntityMachineRadar.MATCHING.clear());
        Services.SERVER.onServerStopping(server -> RTTYSystem.onServerStopping());
        Services.SERVER.onServerTickPre(SatelliteDetector::tick);

        Services.SERVER.onServerTickPre(
                server -> {
                    for (ServerLevel level : server.getAllLevels()) {
                        SatelliteSavedData data = SatelliteSavedData.get(level);
                        for (var satellite : data.sats.values()) satellite.onUpdateTick(level);
                    }
                });

        Services.SERVER.onServerTickPre(SatelliteRayEvents::tick);
        Services.SERVER.onServerStopping(server -> SatelliteRayEvents.clear());
        Services.SERVER.onServerStopping(server -> SatelliteDetector.clear());
        Services.SERVER.onServerStopping(server -> RequestNetwork.onServerStopping());
        Services.SERVER.onServerTickPost(ItemRTTYPager::tickInventory);
        Services.SERVER.onServerTickPost(server -> BeSyncTable.pump());
        Services.SERVER.onServerTickPre(server -> SyncWire.beforeTick());

        Services.SERVER.onServerTickPost(EntityRailCarBase::updateTrains);
        Services.SERVER.onPlayerDisconnect(
                player -> {
                    ChunkTrackerIndex.forget(player, player.level());
                    BeSyncTable.forget(player.getUUID());
                });
        Services.SERVER.onPlayerRespawn(ChunkTrackerIndex::respawn);
        Services.SERVER.onPlayerChangeLevel(
                (player, origin, destination) -> {
                    ChunkTrackerIndex.forget(player, origin);
                    BeSyncTable.forgetIn(origin, player.getUUID());
                });

        Services.SERVER.onServerTickPost(server -> PacketWire.flushPending());

        Services.REGISTRAR.freeze();
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
        BalanceConfig.loadFrom(Services.CONFIG.content().store());
        deriveLiveConfigFacades();
        if (clientFacades) deriveClientConfigFacades();
    }

    public static void deriveLiveConfigFacades() {
        ConfigStore runtime = Services.CONFIG.runtime().store();
        RadiationConfig.loadFrom(runtime);
        BombConfig.loadFrom(runtime);
    }

    public static void deriveClientConfigFacades() {
        ConfigStore client = Services.CONFIG.clientStore();
        RenderConfig.loadFrom(client);
        HudConfig.loadFrom(client);
        GunVisualConfig.loadFrom(client);
        InteractionConfig.loadFrom(client);
        clientFacades = true;
    }

    public static void commonSetup() {

        deriveConfigFacades();
        RadiationSystemNT.onLoadComplete();

        Reg.runAfterRegistration();
        NTMFluids.bake();
        XWeaponModManager.init();
        HazardSystem.registerContent();
        if (Services.PLATFORM.isModLoaded("compact_storage")) {
            HazardSystem.addTransformer(new HazardTransformerCompactStorage());
        }
        if (Services.PLATFORM.isModLoaded("travelersbackpack")) {
            HazardSystem.addTransformer(new HazardTransformerTravelersBackpack());
            HazardSystem.addExternalEquipmentApplicator(TravelersEquipmentHazards::apply);
        }
        if (Services.PLATFORM.isModLoaded("trinkets_updated")) {
            HazardSystem.addExternalEquipmentApplicator(TrinketsHazards::apply);
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
        CustomMachineDefinitions.applyDataPack(server.registryAccess());
        RadiationSystemNT.registerConstantSources(server.registryAccess());
    }

    public static void serverStopped() {
        com.hbm.world.ore.OreGeneration.serverStopped();
        PacketWire.onServerStopped();
        Services.CAPS.clearForeignCapWatches();

        BobmazonOffers.invalidate();
    }
}
