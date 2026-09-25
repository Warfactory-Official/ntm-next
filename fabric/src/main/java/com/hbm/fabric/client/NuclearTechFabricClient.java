// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.hbm.NuclearTech;
import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.client.AbilityKeybindHandler;
import com.hbm.client.CalculatorKeybindHandler;
import com.hbm.client.ClientRegistry;
import com.hbm.client.CraneKeybindHandler;
import com.hbm.client.GunKeybindHandler;
import com.hbm.client.ItemTooltipEvents;
import com.hbm.client.NukeHud;
import com.hbm.client.PlayerKeybindHandler;
import com.hbm.client.ToolKeybindHandler;
import com.hbm.client.WikiRenderCommand;
import com.hbm.client.ctm.CtmEngine;
import com.hbm.client.ctm.HbmCtmLoader;
import com.hbm.client.gui.FluidTooltipFrame;
import com.hbm.client.qmaw.QMAWClient;
import com.hbm.command.ConfigCommand;
import com.hbm.data.DataGroups;
import com.hbm.handler.BobmazonOffers;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.NTMTokenFluidBase;
import com.hbm.inventory.machine.CustomMachineDefinitions;
import com.hbm.inventory.recipes.ModRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.CrateOpenHeldPayload;
import com.hbm.particle.HbmParticleGroups;
import com.hbm.particle.HbmParticles;
import com.hbm.platform.FabricNetworkService;
import com.hbm.platform.Services;
import com.hbm.render.loader.ObjMeshCache;
import com.hbm.wiaj.CanneryClient;
import com.hbm.world.ore.OreGeneration;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.SpriteSourceRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class NuclearTechFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        NuclearTech.deriveClientConfigFacades();

        FabricClientNetworkBinder.bind((FabricNetworkService) Services.NETWORK);

        ClientRegistry.registerSpriteSources(SpriteSourceRegistry::register);
        ClientRegistry.bindClientHooks();
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, context) ->
                        ConfigCommand.registerClient(
                                dispatcher,
                                (source, message, broadcast) ->
                                        source.sendFeedback(message.get())));
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, context) -> dispatcher.register(WikiRenderCommand.build()));
        ClientTooltipComponentCallback.EVENT.register(
                data ->
                        data instanceof FluidTooltipFrame frame
                                ? new FluidTooltipFrame.Renderer(frame)
                                : null);
        FabricAssemblyMarkers.register();
        FabricRadVis.register();
        FabricMachineSilhouette.register();
        FabricRebarPlacerPreview.register();
        CtmEngine ctm = CtmEngine.selected();

        if (System.getProperty("fabric-api.datagen") == null
                && ctm != CtmEngine.NONE
                && !ResourceLoader.registerBuiltinPack(
                        ctm.packId(),
                        FabricLoader.getInstance().getModContainer("hbm").orElseThrow(),
                        ctm.title(),
                        PackActivationType.ALWAYS_ENABLED)) {
            throw new IllegalStateException("Missing connected-texture pack: " + ctm.directory());
        }
        if (ctm == CtmEngine.CONTINUITY) {
            HbmCtmLoader.register();
        }
        FluidTraitRenderHandler.register();
        BuiltInRegistries.FLUID.stream()
                .filter(NTMTokenFluidBase.class::isInstance)
                .forEach(
                        fluid -> {
                            String path = NTMFluids.spritePath(fluid);
                            Material material = new Material(Library.id("block/fluid/" + path));
                            FluidRenderingRegistry.register(
                                    fluid, new FluidModel.Unbaked(material, material, null, null));
                        });
        BuiltInRegistries.FLUID.stream()
                .filter(ClassicFluid.Source.class::isInstance)
                .map(ClassicFluid.class::cast)
                .forEach(
                        fluid ->
                                FluidRenderingRegistry.register(
                                        fluid,
                                        fluid.getFlowing(),
                                        new FluidModel.Unbaked(
                                                new Material(
                                                        fluid.spec().sprite().withSuffix("_still")),
                                                new Material(
                                                        fluid.spec()
                                                                .sprite()
                                                                .withSuffix("_flowing")),
                                                null,
                                                null)));

        ClientPlayConnectionEvents.JOIN.register(
                (listener, sender, client) -> {
                    if (!client.hasSingleplayerServer()) {
                        ModRecipes.bootstrapClientRecipes();

                        HazardSystem.applyDataPack(listener.registryAccess());
                        CustomMachineDefinitions.applyDataPack(listener.registryAccess());
                        DataGroups.applyAll(listener.registryAccess());
                    }
                    GenericRecipes.invalidateIndexes();
                    CrateOpenHeldPayload.report();
                });

        ClientPlayConnectionEvents.DISCONNECT.register(
                (listener, client) -> {
                    RecipeSource.clearClient();
                    QMAWClient.disconnected();
                    CanneryClient.disconnected();
                    BobmazonOffers.invalidate();
                    NTMFluidProperties.onClientDisconnect();
                    OreGeneration.disconnect();
                });
        ClientConfigurationConnectionEvents.INIT.register(
                (listener, client) -> OreGeneration.disconnect());
        ClientConfigurationConnectionEvents.DISCONNECT.register(
                (listener, client) -> OreGeneration.disconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientRegistry.clientTick());
        ClientChunkEvents.CHUNK_UNLOAD.register(
                (level, chunk) -> FluidPipeTintData.evictChunk(chunk.getPos().pack()));
        ItemTooltipCallback.EVENT.register(
                (stack, tooltipContext, tooltipType, lines) ->
                        ItemTooltipEvents.append(
                                stack, Minecraft.getInstance().player, tooltipType, lines));
        for (KeyMapping key : CraneKeybindHandler.KEYS) {
            KeyMappingHelper.registerKeyMapping(key);
        }
        for (KeyMapping key : ToolKeybindHandler.KEYS) {
            KeyMappingHelper.registerKeyMapping(key);
        }
        for (KeyMapping key : GunKeybindHandler.KEYS) {
            KeyMappingHelper.registerKeyMapping(key);
        }
        for (KeyMapping key : PlayerKeybindHandler.KEYS) {
            KeyMappingHelper.registerKeyMapping(key);
        }
        for (KeyMapping key : AbilityKeybindHandler.KEYS) {
            KeyMappingHelper.registerKeyMapping(key);
        }
        KeyMappingHelper.registerKeyMapping(CalculatorKeybindHandler.OPEN);
        KeyMappingHelper.registerKeyMapping(QMAWClient.OPEN_MANUAL);

        ClientRegistry.registerMenuScreens(MenuScreens::register);
        ClientRegistry.registerBlockEntityRenderers(BlockEntityRendererRegistry::register);
        ClientRegistry.registerFlywheelVisualizers();
        ClientRegistry.registerEntityRenderers(EntityRendererRegistry::register);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.RAD_FOG.get(), ClientRegistry::radFogProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.RBMK_MUSH.get(), ClientRegistry::rbmkMushProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.DIGAMMA_SMOKE.get(), ClientRegistry::digammaSmokeProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.LAUNCH_SMOKE.get(), ClientRegistry::launchSmokeProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.VOLCANO_SMOKE.get(), ClientRegistry::volcanoSmokeProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.SCHRAB_FOG.get(), ClientRegistry::schrabFogProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.GAS_FLAME.get(), ClientRegistry::gasFlameProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.HADRON.get(), ClientRegistry::hadronProvider);
        ParticleProviderRegistry.getInstance()
                .register(
                        HbmParticles.BLACK_POWDER_SPARK.get(),
                        ClientRegistry::blackPowderSparkProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.MIST_TOWER.get(), ClientRegistry::mistTowerProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.SPLASH.get(), ClientRegistry::splashProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.FLAME.get(), ClientRegistry::flameProvider);
        ParticleProviderRegistry.getInstance()
                .register(HbmParticles.ASH_REVEAL.get(), ClientRegistry::ashRevealProvider);
        HbmParticleGroups.forEach(ParticleGroupRegistry::register);

        ClientRegistry.registerItemTintSources(ItemTintSources.ID_MAPPER::put);
        ClientRegistry.registerSelectItemModelProperties(SelectItemModelProperties.ID_MAPPER::put);
        ClientRegistry.registerBlockTintSources(BlockColorRegistry::register);

        ClientRegistry.registerSpecialModelRenderers(SpecialModelRenderers.ID_MAPPER::put);
        ClientRegistry.registerItemModels(ItemModels.ID_MAPPER::put);
        HbmModelDeserializers.register();
        ModelLoadingPlugin.register(new NtmFabricModelPlugin());
        HudElementRegistry.addLast(
                ClientRegistry.LOOK_OVERLAY_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderLookOverlay(graphics));
        HudElementRegistry.addLast(
                ClientRegistry.RADIATION_HUD_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderRadiationHud(graphics));

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                ClientRegistry.GUN_SCOPE_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderGunScope(graphics));
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                ClientRegistry.NUKE_FLASH_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderNukeFlash(graphics));
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                ClientRegistry.INFO_HUD_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderInfoHud(graphics));

        for (Identifier shaken :
                List.of(
                        VanillaHudElements.HOTBAR,
                        VanillaHudElements.INFO_BAR,
                        VanillaHudElements.EXPERIENCE_LEVEL,
                        VanillaHudElements.HELD_ITEM_TOOLTIP,
                        VanillaHudElements.SLEEP,
                        VanillaHudElements.SCOREBOARD,
                        VanillaHudElements.OVERLAY_MESSAGE,
                        VanillaHudElements.CHAT,
                        VanillaHudElements.PLAYER_LIST)) {
            HudElementRegistry.replaceElement(
                    shaken,
                    original ->
                            (graphics, deltaTracker) ->
                                    NukeHud.shaken(
                                            graphics,
                                            () ->
                                                    original.extractRenderState(
                                                            graphics, deltaTracker)));
        }
        HudElementRegistry.addFirst(
                ClientRegistry.ARMOR_OVERLAY_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderArmorOverlay(graphics));
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                ClientRegistry.TOOL_ABILITY_HUD_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderToolAbilityHud(graphics));
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                ClientRegistry.GUN_HUD_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderGunHud(graphics));
        HudElementRegistry.addLast(
                ClientRegistry.HEV_HUD_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderHevHud(graphics));
        HudElementRegistry.addLast(
                ClientRegistry.DASH_BAR_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderDashBar(graphics));
        HudElementRegistry.replaceElement(
                VanillaHudElements.CROSSHAIR,
                original ->
                        (graphics, deltaTracker) -> {
                            if (ClientRegistry.replacesCrosshair()) {
                                ClientRegistry.renderGunCrosshair(graphics);
                            } else {
                                original.extractRenderState(graphics, deltaTracker);
                            }
                        });
        HudElementRegistry.replaceElement(
                VanillaHudElements.HEALTH_BAR,
                original ->
                        (graphics, deltaTracker) -> {
                            if (!ClientRegistry.replacesVanillaVitals())
                                original.extractRenderState(graphics, deltaTracker);
                        });
        HudElementRegistry.replaceElement(
                VanillaHudElements.ARMOR_BAR,
                original ->
                        (graphics, deltaTracker) -> {
                            if (!ClientRegistry.replacesVanillaVitals())
                                original.extractRenderState(graphics, deltaTracker);
                            else
                                ClientRegistry.renderShieldBar(graphics, graphics.guiHeight() - 39);
                        });
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(ObjMeshCache.ID, ObjMeshCache.INSTANCE);
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(
                        ClientRegistry.VISUAL_RESOURCES_ID,
                        (ResourceManagerReloadListener) ClientRegistry::reloadVisualResources);
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(
                        ClientRegistry.GUN_CLIENT_SETUP_ID,
                        (ResourceManagerReloadListener)
                                resourceManager -> ClientRegistry.setupGunClient());
    }
}
