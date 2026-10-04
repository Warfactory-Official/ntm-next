// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.NuclearTech;
import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.client.ClientRegistry;
import com.hbm.client.ItemTooltipEvents;
import com.hbm.client.Keybinds;
import com.hbm.client.NukeHud;
import com.hbm.client.RadiationResistanceTooltip;
import com.hbm.client.WikiRenderCommand;
import com.hbm.client.ctm.CtmEngine;
import com.hbm.client.ctm.HbmCtmLoader;
import com.hbm.client.gui.FluidTooltipFrame;
import com.hbm.client.gui.PwrSliceRenderState;
import com.hbm.client.gui.PwrSliceRenderer;
import com.hbm.client.model.HbmModelJson;
import com.hbm.client.model.SectionGeometry;
import com.hbm.client.qmaw.QMAWClient;
import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.command.ConfigCommand;
import com.hbm.data.DataGroups;
import com.hbm.handler.BobmazonOffers;
import com.hbm.handler.EquipmentProtections;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.SuitProtections;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.NTMTokenFluidBase;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import com.hbm.inventory.machine.CustomMachineDefinitions;
import com.hbm.inventory.recipes.ModRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.items.ModItems;
import com.hbm.items.food.ConsumableTreatments;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.Library;
import com.hbm.neoforge.client.ctm.CtmLibModelDefinition;
import com.hbm.packet.toserver.CrateOpenHeldPayload;
import com.hbm.particle.HbmParticleGroups;
import com.hbm.particle.HbmParticles;
import com.hbm.render.loader.ObjMeshCache;
import com.hbm.wiaj.CanneryClient;
import com.hbm.wiaj.JarRenderState;
import com.hbm.wiaj.JarSceneRenderer;
import com.hbm.world.ore.OreGeneration;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.FluidTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

@Mod(value = NuclearTech.MOD_ID, dist = Dist.CLIENT)
public final class NuclearTechNeoForgeClient {
    private static final Set<Identifier> NUKE_SHAKEN_LAYERS =
            Set.of(
                    VanillaGuiLayers.HOTBAR,
                    VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND,
                    VanillaGuiLayers.EXPERIENCE_LEVEL,
                    VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
                    VanillaGuiLayers.SELECTED_ITEM_NAME,
                    VanillaGuiLayers.SLEEP_OVERLAY,
                    VanillaGuiLayers.SCOREBOARD_SIDEBAR,
                    VanillaGuiLayers.OVERLAY_MESSAGE,
                    VanillaGuiLayers.CHAT,
                    VanillaGuiLayers.TAB_LIST);
    private static boolean nukeShakePushed;

    public NuclearTechNeoForgeClient(IEventBus modBus) {
        ClientRegistry.bindClientHooks();
        modBus.addListener(
                (RegisterPictureInPictureRenderersEvent event) ->
                        event.register(PwrSliceRenderState.class, PwrSliceRenderer::new));
        modBus.addListener(
                (RegisterPictureInPictureRenderersEvent event) ->
                        event.register(JarRenderState.class, JarSceneRenderer::new));
        CtmEngine ctm = CtmEngine.selected();
        if (ctm != CtmEngine.NONE) {
            modBus.addListener(
                    (AddPackFindersEvent event) ->
                            event.addPackFinders(
                                    Library.id(ctm.directory()),
                                    PackType.CLIENT_RESOURCES,
                                    ctm.title(),
                                    PackSource.BUILT_IN,
                                    true,
                                    Pack.Position.TOP));
        }
        if (ctm == CtmEngine.CONTINUITY) {
            HbmCtmLoader.register();
        }
        modBus.addListener(
                (RegisterSpriteSourcesEvent event) ->
                        ClientRegistry.registerSpriteSources(event::register));
        NeoForge.EVENT_BUS.addListener(
                (RegisterClientCommandsEvent event) ->
                        ConfigCommand.registerClient(
                                event.getDispatcher(), CommandSourceStack::sendSuccess));
        NeoForge.EVENT_BUS.addListener(
                (RegisterClientCommandsEvent event) ->
                        event.getDispatcher().register(WikiRenderCommand.build()));
        NeoForge.EVENT_BUS.addListener(
                TagsUpdatedEvent.ClientPacketReceived.class,
                event -> {
                    if (event.shouldUpdateStaticData()) ModRecipes.bootstrapClientRecipes();
                    GenericRecipes.invalidateIndexes();
                    HazardSystem.clearCaches();
                    NTMFluidProperties.bindEquivalents();
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingIn event) -> {
                    RadiationResistanceTooltip.joined(event.getPlayer().registryAccess());
                    if (!Minecraft.getInstance().hasSingleplayerServer()) {
                        HazmatRegistry.applyDataPack(event.getPlayer().registryAccess());
                        EquipmentProtections.applyDataPack(event.getPlayer().registryAccess());
                        SuitProtections.applyDataPack(event.getPlayer().registryAccess());
                        ConsumableTreatments.applyDataPack(event.getPlayer().registryAccess());
                        HazardSystem.applyDataPack(event.getPlayer().registryAccess());
                        CustomMachineDefinitions.applyDataPack(event.getPlayer().registryAccess());
                        DataGroups.applyAll(event.getPlayer().registryAccess());
                    }
                    CrateOpenHeldPayload.report();
                });
        modBus.addListener((FMLClientSetupEvent event) -> NuclearTech.deriveClientConfigFacades());
        modBus.addListener(
                (ModConfigEvent.Reloading event) -> {
                    if (event.getConfig().getType() != ModConfig.Type.CLIENT) return;
                    NuclearTech.deriveClientConfigFacades();
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.execute(
                            () -> {
                                if (minecraft.getConnection() != null)
                                    CrateOpenHeldPayload.report();
                            });
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) ->
                        RadiationResistanceTooltip.disconnected());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> {
                    if (!Minecraft.getInstance().hasSingleplayerServer()) {
                        HazmatRegistry.clear();
                        EquipmentProtections.clear();
                        SuitProtections.clear();
                        ConsumableTreatments.clear();
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> RecipeSource.clearClient());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> QMAWClient.disconnected());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> CanneryClient.disconnected());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) ->
                        NTMFluidProperties.onClientDisconnect());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> OreGeneration.disconnect());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> BobmazonOffers.invalidate());
        NeoForge.EVENT_BUS.addListener(
                (FluidTooltipEvent event) -> {
                    FluidTraitTooltip.addInfo(
                            event.getFluidStack().getFluid(), event.getToolTip()::add);
                    QMAWClient.fluidTooltip(
                            event.getFluidStack().getFluid(), event.getToolTip()::add);
                });
        NeoForgeAssemblyMarkers.register();
        NeoForgeRadVis.register();
        NeoForgeMachineSilhouette.register();
        NeoForgeRebarPlacerPreview.register();
        NeoForge.EVENT_BUS.addListener(
                (AddSectionGeometryEvent event) -> {
                    var geometry =
                            SectionGeometry.capture(event.getLevel(), event.getSectionOrigin());
                    if (geometry != null)
                        event.addRenderer(
                                context ->
                                        geometry.emit(
                                                context.getRegion(),
                                                context::getOrCreateChunkBuffer));
                });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientRegistry.clientTick());
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Unload event) -> {
                    if (event.getLevel().isClientSide()) {
                        FluidPipeTintData.evictChunk(
                                (Level) event.getLevel(), event.getChunk().getPos().pack());
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ItemTooltipEvent event) ->
                        ItemTooltipEvents.append(
                                event.getItemStack(),
                                event.getEntity(),
                                event.getFlags(),
                                event.getToolTip()));
        NeoForge.EVENT_BUS.addListener(
                (ItemTooltipEvent event) ->
                        ContainedHazardTooltipSync.request(event.getItemStack()));
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> ContainedHazardTooltipSync.reset());
        modBus.addListener(
                (RegisterKeyMappingsEvent event) -> {
                    for (KeyMapping key : Keybinds.ALL) event.register(key);
                });
        modBus.addListener(
                (RegisterMenuScreensEvent event) ->
                        ClientRegistry.registerMenuScreens(event::register));
        modBus.addListener(
                (AddClientReloadListenersEvent event) ->
                        event.addListener(ObjMeshCache.ID, ObjMeshCache.INSTANCE));
        modBus.addListener(
                (AddClientReloadListenersEvent event) ->
                        event.addListener(
                                ClientRegistry.VISUAL_RESOURCES_ID,
                                (ResourceManagerReloadListener)
                                        ClientRegistry::reloadVisualResources));
        modBus.addListener(
                (AddClientReloadListenersEvent event) ->
                        event.addListener(
                                ClientRegistry.GUN_CLIENT_SETUP_ID,
                                (ResourceManagerReloadListener)
                                        resourceManager -> ClientRegistry.setupGunClient()));
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) -> {
                    ClientRegistry.registerBlockEntityRenderers(event::registerBlockEntityRenderer);
                    ClientRegistry.registerEntityRenderers(event::registerEntityRenderer);
                });
        modBus.addListener(
                (RegisterColorHandlersEvent.ItemTintSources event) ->
                        ClientRegistry.registerItemTintSources(event::register));
        ClientRegistry.registerSelectItemModelProperties(SelectItemModelProperties.ID_MAPPER::put);
        modBus.addListener(
                (RegisterColorHandlersEvent.BlockTintSources event) ->
                        ClientRegistry.registerBlockTintSources(event::register));
        modBus.addListener(
                (RegisterSpecialModelRendererEvent event) ->
                        ClientRegistry.registerSpecialModelRenderers(event::register));
        modBus.addListener(
                (RegisterItemModelsEvent event) ->
                        ClientRegistry.registerItemModels(event::register));
        modBus.addListener(
                (RegisterClientExtensionsEvent event) -> {
                    event.registerItem(
                            GunClientExtensions.INSTANCE,
                            BuiltInRegistries.ITEM.stream()
                                    .filter(ItemGunBaseNT.class::isInstance)
                                    .toArray(Item[]::new));
                    event.registerItem(
                            GunClientExtensions.INSTANCE, ModItems.LASER_DETONATOR.get());
                });
        modBus.addListener(
                (RegisterFluidModelsEvent event) ->
                        BuiltInRegistries.FLUID.stream()
                                .filter(NTMTokenFluidBase.class::isInstance)
                                .forEach(
                                        fluid -> {
                                            String path = NTMFluids.spritePath(fluid);
                                            Material material =
                                                    new Material(Library.id("block/fluid/" + path));
                                            event.register(
                                                    new FluidModel.Unbaked(
                                                            material, material, null, null, null),
                                                    fluid);
                                        }));
        modBus.addListener(
                (RegisterFluidModelsEvent event) ->
                        BuiltInRegistries.FLUID.stream()
                                .filter(ClassicFluid.Source.class::isInstance)
                                .map(ClassicFluid.class::cast)
                                .forEach(
                                        fluid ->
                                                event.register(
                                                        new FluidModel.Unbaked(
                                                                new Material(
                                                                        fluid.spec()
                                                                                .sprite()
                                                                                .withSuffix(
                                                                                        "_still")),
                                                                new Material(
                                                                        fluid.spec()
                                                                                .sprite()
                                                                                .withSuffix(
                                                                                        "_flowing")),
                                                                null,
                                                                null,
                                                                null),
                                                        fluid,
                                                        fluid.getFlowing())));
        modBus.addListener(
                (RegisterClientTooltipComponentFactoriesEvent event) ->
                        event.register(FluidTooltipFrame.class, FluidTooltipFrame.Renderer::new));
        modBus.addListener(
                (RegisterParticleProvidersEvent event) -> {
                    event.registerSpriteSet(
                            HbmParticles.RAD_FOG.get(), ClientRegistry::radFogProvider);
                    event.registerSpriteSet(
                            HbmParticles.RBMK_MUSH.get(), ClientRegistry::rbmkMushProvider);
                    event.registerSpriteSet(
                            HbmParticles.DIGAMMA_SMOKE.get(), ClientRegistry::digammaSmokeProvider);
                    event.registerSpriteSet(
                            HbmParticles.LAUNCH_SMOKE.get(), ClientRegistry::launchSmokeProvider);
                    event.registerSpriteSet(
                            HbmParticles.VOLCANO_SMOKE.get(), ClientRegistry::volcanoSmokeProvider);
                    event.registerSpriteSet(
                            HbmParticles.SCHRAB_FOG.get(), ClientRegistry::schrabFogProvider);
                    event.registerSpriteSet(
                            HbmParticles.GAS_FLAME.get(), ClientRegistry::gasFlameProvider);
                    event.registerSpriteSet(
                            HbmParticles.HADRON.get(), ClientRegistry::hadronProvider);
                    event.registerSpriteSet(
                            HbmParticles.BLACK_POWDER_SPARK.get(),
                            ClientRegistry::blackPowderSparkProvider);
                    event.registerSpriteSet(
                            HbmParticles.MIST_TOWER.get(), ClientRegistry::mistTowerProvider);
                    event.registerSpriteSet(
                            HbmParticles.SPLASH.get(), ClientRegistry::splashProvider);
                    event.registerSpriteSet(
                            HbmParticles.FLAME.get(), ClientRegistry::flameProvider);
                    event.registerSpriteSet(
                            HbmParticles.ASH_REVEAL.get(), ClientRegistry::ashRevealProvider);
                });
        modBus.addListener(
                (RegisterParticleGroupsEvent event) -> HbmParticleGroups.forEach(event::register));
        modBus.addListener(
                (RegisterRenderPipelinesEvent event) ->
                        HbmRenderPipelines.forEach(event::registerPipeline));
        modBus.addListener(
                (FMLClientSetupEvent event) ->
                        event.enqueueWork(ClientRegistry::registerFlywheelVisualizers));
        modBus.addListener(
                (RegisterGuiLayersEvent event) -> {
                    event.registerBelow(
                            VanillaGuiLayers.ARMOR_LEVEL,
                            ClientRegistry.SHIELD_BAR_ID,
                            (graphics, deltaTracker) -> {
                                var hud = Minecraft.getInstance().gui.hud;
                                if (ClientRegistry.renderShieldBar(
                                        graphics, graphics.guiHeight() - hud.leftHeight))
                                    hud.leftHeight += 10;
                            });
                    event.registerAboveAll(
                            ClientRegistry.LOOK_OVERLAY_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderLookOverlay(graphics));
                    event.registerAboveAll(
                            ClientRegistry.RADIATION_HUD_ID,
                            (graphics, deltaTracker) ->
                                    ClientRegistry.renderRadiationHud(graphics));
                    event.registerAboveAll(
                            ClientRegistry.RADVIS_HUD_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderRadVisHud(graphics));
                    event.registerBelow(
                            VanillaGuiLayers.CROSSHAIR,
                            ClientRegistry.GUN_SCOPE_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderGunScope(graphics));
                    event.registerBelow(
                            VanillaGuiLayers.CROSSHAIR,
                            ClientRegistry.NUKE_FLASH_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderNukeFlash(graphics));
                    event.registerBelow(
                            VanillaGuiLayers.CROSSHAIR,
                            ClientRegistry.INFO_HUD_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderInfoHud(graphics));
                    event.registerBelowAll(
                            ClientRegistry.ARMOR_OVERLAY_ID,
                            (graphics, deltaTracker) ->
                                    ClientRegistry.renderArmorOverlay(graphics));
                    event.registerBelow(
                            VanillaGuiLayers.CROSSHAIR,
                            ClientRegistry.TOOL_ABILITY_HUD_ID,
                            (graphics, deltaTracker) ->
                                    ClientRegistry.renderToolAbilityHud(graphics));
                    event.registerBelow(
                            VanillaGuiLayers.HOTBAR,
                            ClientRegistry.GUN_HUD_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderGunHud(graphics));
                    event.registerAboveAll(
                            ClientRegistry.HEV_HUD_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderHevHud(graphics));
                    event.registerAboveAll(
                            ClientRegistry.DASH_BAR_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderDashBar(graphics));
                });
        NeoForge.EVENT_BUS.addListener(
                (RenderGuiLayerEvent.Pre event) -> {
                    if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)
                            && ClientRegistry.replacesCrosshair()) {
                        event.setCanceled(true);
                        ClientRegistry.renderGunCrosshair(event.getGuiGraphics());
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                (RenderGuiLayerEvent.Pre event) -> {
                    if (NUKE_SHAKEN_LAYERS.contains(event.getName())) {
                        nukeShakePushed = NukeHud.pushShake(event.getGuiGraphics());
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                (RenderGuiLayerEvent.Post event) -> {
                    if (nukeShakePushed && NUKE_SHAKEN_LAYERS.contains(event.getName())) {
                        NukeHud.popShake(event.getGuiGraphics());
                        nukeShakePushed = false;
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (RenderGuiLayerEvent.Pre event) -> {
                    if ((event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                                    || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL))
                            && ClientRegistry.replacesVanillaVitals()) {
                        event.setCanceled(true);
                    }
                });
        modBus.addListener(
                (ModelEvent.RegisterLoaders event) -> {
                    event.register(Library.id("obj"), new HbmModelLoader(HbmModelJson::obj));
                });
        modBus.addListener(
                (RegisterBlockStateModels event) -> {
                    event.registerDefinition(
                            Library.id("baked"), new BakedBlockModelDefinition().codec());
                    event.registerDefinition(Library.id("ctm"), CtmLibModelDefinition.CODEC);
                });
    }
}
