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

package com.hbm.client;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.blockentity.machine.*;
import com.hbm.blockentity.machine.*;
import com.hbm.blockentity.machine.fusion.*;
import com.hbm.blockentity.machine.fusion.*;
import com.hbm.blockentity.machine.oil.PyroOvenBlockEntity;
import com.hbm.blockentity.machine.oil.RefineryBlockEntity;
import com.hbm.blockentity.machine.oil.VacuumRefineryBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm.blockentity.machine.storage.CrateType;
import com.hbm.blockentity.machine.storage.ReddBlockEntity;
import com.hbm.blockentity.machine.thermal.ElectricHeatBlockEntity;
import com.hbm.blockentity.machine.thermal.FluidBoilerBlockEntity;
import com.hbm.blockentity.machine.thermal.LeviathanTurbineBlockEntity;
import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.blockentity.processing.AssemblyMachineBlockEntity;
import com.hbm.blockentity.processing.BurnerPressBlockEntity;
import com.hbm.blockentity.processing.CentrifugeBlockEntity;
import com.hbm.blockentity.processing.ChemicalFactoryBlockEntity;
import com.hbm.blockentity.processing.ChemicalPlantBlockEntity;
import com.hbm.blockentity.processing.CrystalProcessorBlockEntity;
import com.hbm.blockentity.processing.ElectricPressBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.gas.GasBlock;
import com.hbm.blocks.generic.BobbleBlock;
import com.hbm.blocks.generic.GuideBlock;
import com.hbm.blocks.generic.SnowglobeBlock;
import com.hbm.blocks.network.CraneRouterBlock;
import com.hbm.client.gui.*;
import com.hbm.client.gui.*;
import com.hbm.client.gui.BookLoreScreen;
import com.hbm.client.model.PaddedSpriteSource;
import com.hbm.client.particle.*;
import com.hbm.client.render.*;
import com.hbm.client.render.*;
import com.hbm.client.render.CrucibleItemRenderer;
import com.hbm.client.render.flywheel.DoorVisuals;
import com.hbm.client.render.flywheel.EntityVisuals;
import com.hbm.client.render.flywheel.FlywheelResources;
import com.hbm.client.render.flywheel.GlyphidVisuals;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.client.render.flywheel.MachineVisuals;
import com.hbm.client.render.flywheel.MultiblockCrumblingVisuals;
import com.hbm.client.render.flywheel.TorexVisuals;
import com.hbm.client.render.flywheel.TurretVisuals;
import com.hbm.client.sound.ClientAudioFactory;
import com.hbm.client.sound.MachineSoundInstance;
import com.hbm.client.weapon.GunClientProperties;
import com.hbm.client.weapon.GunClientState;
import com.hbm.config.HudConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.particle.ChlorineFXEntity;
import com.hbm.entity.particle.CloudFXEntity;
import com.hbm.entity.particle.OrangeFXEntity;
import com.hbm.entity.particle.PinkCloudFXEntity;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.handler.radiation.RadVisOverlay;
import com.hbm.inventory.container.ModMenus;
import com.hbm.items.ModItems;
import com.hbm.items.special.BookLoreItem;
import com.hbm.items.special.ClayTabletItem;
import com.hbm.items.special.HolotapeImageItem;
import com.hbm.items.tool.BoltgunItem;
import com.hbm.items.tool.GuideBookItem;
import com.hbm.items.tool.ToolAbilityItem;
import com.hbm.items.weapon.CrucibleItem;
import com.hbm.items.weapon.grenade.GrenadeUniversalItem;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.factory.GunFactoryClient;
import com.hbm.items.weapon.sedna.impl.GunStingerItem;
import com.hbm.main.ResourceManager;
import com.hbm.particle.AshRevealParticle;
import com.hbm.particle.AshRevealParticleOptions;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.particle.FlameParticleOptions;
import com.hbm.particle.SplashParticleOptions;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.item.weapon.sedna.WeaponItemRenderer;
import com.hbm.render.util.ScreenOverlayRenderer;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.util.RegistryUtil;
import com.hbm.wiaj.cannery.Jars;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.serialization.MapCodec;
import dev.engine_room.flywheel.impl.compat.EntityFeatureCompat;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.LoggerFactory;

public final class ClientRegistry {
    public static final Identifier LOOK_OVERLAY_ID = RegistryUtil.id("look_overlay");
    public static final Identifier RADIATION_HUD_ID = RegistryUtil.id("radiation_hud");
    public static final Identifier RADVIS_HUD_ID = RegistryUtil.id("radvis_hud");
    public static final Identifier ARMOR_OVERLAY_ID = RegistryUtil.id("armor_overlay");
    public static final Identifier INFO_HUD_ID = RegistryUtil.id("info_hud");
    public static final Identifier GUN_HUD_ID = RegistryUtil.id("gun_hud");
    public static final Identifier GUN_SCOPE_ID = RegistryUtil.id("gun_scope");
    public static final Identifier GUN_CLIENT_SETUP_ID = RegistryUtil.id("gun_client_setup");
    public static final Identifier HEV_HUD_ID = RegistryUtil.id("hev_hud");
    public static final Identifier DASH_BAR_ID = RegistryUtil.id("dash_bar");
    public static final Identifier SHIELD_BAR_ID = RegistryUtil.id("shield_bar");
    public static final Identifier ARMOR_BARS_ID = RegistryUtil.id("armor_bars");
    public static final Identifier TOOL_ABILITY_HUD_ID = RegistryUtil.id("tool_ability_hud");
    public static final Identifier NUKE_FLASH_ID = RegistryUtil.id("nuke_flash");
    public static final Identifier MODE_BADGES_ID = RegistryUtil.id("mode_badges");
    public static final Identifier VISUAL_RESOURCES_ID =
            Identifier.fromNamespaceAndPath("hbm", "visual_resources");

    private ClientRegistry() {}

    public static void bindClientHooks() {
        EntityFeatureCompat.registerEmfHeldUse(
                entity ->
                        entity instanceof Player
                                && entity.getMainHandItem().getItem() instanceof GunBaseNTItem);
        EntityFeatureCompat.registerEmfBowRecognition(
                entity ->
                        entity instanceof Player player
                                && AkimboGhost.isAkimbo(player.getMainHandItem())
                                && AkimboGhost.isGhost(player.getOffhandItem()));
        GasBlock.REVEALED_TO_VIEWER =
                () -> {
                    LocalPlayer viewer = Minecraft.getInstance().player;
                    return viewer != null
                            && viewer.getItemBySlot(EquipmentSlot.HEAD)
                                    .is(ModItems.ASHGLASSES.get());
                };
        ClientGameTime.install();
        FarView.init();
        EntityEffectHandler.CLIENT_RADIATION_FX = ClientEffects::radiationAura;
        EntityEffectHandler.CLIENT_CRATER_AURA = ClientEffects::craterAura;
        AudioSystem.installClientFactory(ClientAudioFactory.INSTANCE);
        RefuelerBlockEntity.CLIENT_PARTICLE = RefuelerRenderer::spawnFluidFill;
        CentrifugeBlockEntity.CLIENT_SOUND = CentrifugeSound::tick;
        SoyuzLauncherBlockEntity.CLIENT_SOUND = SoyuzLauncherSound::tick;
        GasCentBlockEntity.CLIENT_SOUND = GasCentSound::tick;
        ArcFurnaceBlockEntity.CLIENT_SOUND = ArcFurnaceSound::tick;
        AssemblyMachineBlockEntity.CLIENT_SOUND = AssemblerSound::tick;
        AssemblerSound.installOneShots();
        PrecisionAssemblerBlockEntity.CLIENT_SOUND = PrecAssSound::tick;
        PrecAssSound.installOneShots();
        AssemblyFactoryBlockEntity.CLIENT_SOUND = AssemblyFactorySound::tick;
        AssemblyFactorySound.installOneShots();
        ChemicalPlantBlockEntity.CLIENT_SOUND = ChemPlantSound::tick;
        RockMillBlockEntity.CLIENT_DUST = RockMillEffects::tick;
        PurexBlockEntity.CLIENT_SOUND = PurexSound::tick;
        ChemicalFactoryBlockEntity.CLIENT_SOUND = ChemFactorySound::tick;
        RefineryBlockEntity.CLIENT_SOUND = RefinerySound::tick;
        PWRControllerBlockEntity.CLIENT_SOUND = PWRControllerSound::tick;
        ElectricHeatBlockEntity.CLIENT_SOUND = ElectricHeaterSound::tick;
        DieselGeneratorBlockEntity.CLIENT_SOUND = DieselSound::tick;
        LargeTurbineBlockEntity.CLIENT_SOUND = LargeTurbineSound::tick;
        IndustrialTurbineBlockEntity.CLIENT_SOUND = IndustrialTurbineSound::tick;
        LeviathanTurbineBlockEntity.CLIENT_SOUND = ChungusSound::tick;
        ReddBlockEntity.CLIENT_SOUND = FensuSound::tick;
        FluidBoilerBlockEntity.CLIENT_SOUND = BoilerSound::tick;
        CombustionEngineBlockEntity.CLIENT_SOUND = CombustionEngineSound::tick;
        TurbineGasBlockEntity.CLIENT_SOUND = TurbineGasSound::tick;
        TurbofanBlockEntity.CLIENT_SOUND = TurbofanSound::tick;
        PumpBlockEntity.CLIENT_SOUND = PumpSound::tick;
        BlastFurnaceBlockEntity.CLIENT_TOWER = BlastFurnaceTower::tick;
        ChimneyBaseBlockEntity.CLIENT_TOWER = ChimneyTower::tick;
        RBMKAutoloaderBlockEntity.CLIENT_SOUND = AutoloaderSound::tick;
        RBMKAutoloaderBlockEntity.CLIENT_TOWER = AutoloaderSound::plume;
        CrystalProcessorBlockEntity.CLIENT_SOUND = CrystallizerSound::tick;
        PyroOvenBlockEntity.CLIENT_SOUND = PyroOvenSound::tick;
        VacuumRefineryBlockEntity.CLIENT_SOUND = VacuumDistillSound::tick;
        ThresherBlockEntity.CLIENT_SOUND = ThresherSound::tick;
        AutosawBlockEntity.CLIENT_SOUND = AutosawSound::tick;
        FusionTorusBlockEntity.CLIENT_SOUND = FusionSound::tick;
        FusionKlystronBlockEntity.CLIENT_SOUND = FusionSound::tick;
        CreativeFusionKlystronBlockEntity.CLIENT_SOUND = FusionSound::tick;
        FusionMHDTBlockEntity.CLIENT_SOUND = FusionSound::tick;
        FusionPlasmaForgeBlockEntity.PLAY_STRIKER =
                be -> {
                    if (be.isMuffled()) return;
                    BlockPos pos = be.getBlockPos();
                    Minecraft.getInstance()
                            .getSoundManager()
                            .play(
                                    new MachineSoundInstance(
                                            pos,
                                            ModSounds.BOLTGUN.get(),
                                            SoundSource.BLOCKS,
                                            be.getVolume(0.25F),
                                            1.25F,
                                            SoundInstance.createUnseededRandom(),
                                            pos.getX(),
                                            pos.getY(),
                                            pos.getZ()));
                };
        CrucibleItem.PLAY_SWING =
                pitch ->
                        Minecraft.getInstance()
                                .getSoundManager()
                                .play(
                                        SimpleSoundInstance.forUI(
                                                ModSounds.CRUCIBLE_SWING.get(), pitch));
        ClayTabletItem.OPEN_SCREEN =
                player -> Minecraft.getInstance().gui.setScreen(new ClayTabletScreen(player));
        GuideBookItem.OPEN_SCREEN =
                type -> Minecraft.getInstance().gui.setScreen(new GuideScreen(type));
        GuideBlock.OPEN_WIKI =
                () -> Blaze3D.openUri(URI.create("https://nucleartech.wiki/wiki/Main_Page"));
        BookLoreItem.OPEN_SCREEN =
                player -> Minecraft.getInstance().gui.setScreen(new BookLoreScreen(player));
        ToolAbilityItem.CYCLE_FROM_USE = Keybinds::cycleFromUse;
        HolotapeImageItem.OPEN_SCREEN =
                hand -> Minecraft.getInstance().gui.setScreen(new HolotapeScreen(hand));
        BobbleBlock.OPEN_GUI =
                bobble -> Minecraft.getInstance().gui.setScreen(new BobbleScreen(bobble));
        SnowglobeBlock.OPEN_GUI =
                globe -> Minecraft.getInstance().gui.setScreen(new SnowglobeScreen(globe));
    }

    public static void clientTick() {
        UpdateChecker.tick();
        ClientSyncRecovery.tick();
        SettingsToolInfo.clientTick();
        InfoSystem.clientTick();
        FluidPipeTintData.flushRequests(Minecraft.getInstance().level);
        Keybinds.poll();
        ArmorMovementHandler.clientTick();
        if (Minecraft.getInstance().player != null)
            ArmorModHandler.clientTick(Minecraft.getInstance().player);
        No9Flashlight.clientTick();
        NTMSkybox.clientTick();
        SootFog.clientTick();
        WiringReadout.clientTick();
        RebarPlacerPreview.clientTick();
        DroneLinkerReadout.clientTick();
        HeldItemAnimations.removeExpired();
        WeaponItemRenderer.expireFlashes();
        GunBaseNTItem.clientHeldTick();
        GrenadeUniversalItem.clientHeldTick();
        BoltgunItem.clientHeldTick();
        if (!Minecraft.getInstance().isPaused()) GunBaseNTItem.clientRecoilTick();
        RadFogVisuals.tick();
        RocketFlameVisuals.tick();
        ContrailVisuals.tick();
        DebrisVisuals.tick();
        RadVisOverlay.clientTick();
    }

    public static void reloadVisualResources(
            net.minecraft.server.packs.resources.ResourceManager resources) {
        SlagTextures.reload(resources);
        FlywheelResources.reload();
        ItemVisuals.register(resources);
    }

    public static void registerFlywheelVisualizers() {
        MachineVisuals.register();
        DoorVisuals.register();
        TorexVisuals.register();
        GlyphidVisuals.register();
        TurretVisuals.register();
        EntityVisuals.register();
        MultiblockCrumblingVisuals.register();
    }

    public static ParticleProvider<SimpleParticleType> radFogProvider(SpriteSet sprites) {
        return new RadiationFogParticle.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> rbmkMushProvider(SpriteSet sprites) {
        return new RBMKMushParticle.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> digammaSmokeProvider(SpriteSet sprites) {
        return new DigammaSmokeParticle.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> launchSmokeProvider(SpriteSet sprites) {
        return new LaunchSmokeParticle.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> volcanoSmokeProvider(SpriteSet sprites) {
        return new VolcanoSmokeParticle.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> schrabFogProvider(SpriteSet sprites) {
        return new SchrabFogParticles.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> gasFlameProvider(SpriteSet sprites) {
        return new GasFlameParticle.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> hadronProvider(SpriteSet sprites) {
        return new HadronParticle.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> blackPowderSparkProvider(SpriteSet sprites) {
        return new BlackPowderSparkParticle.Provider(sprites);
    }

    public static ParticleProvider<CoolingTowerParticleOptions> mistTowerProvider(
            SpriteSet sprites) {
        return new CoolingTowerParticle.Provider(sprites);
    }

    public static ParticleProvider<FlameParticleOptions> flameProvider(SpriteSet sprites) {
        return new FlamethrowerParticle.Provider(sprites);
    }

    public static ParticleProvider<AshRevealParticleOptions> ashRevealProvider(SpriteSet sprites) {
        return new AshRevealParticle.Provider(sprites);
    }

    public static ParticleProvider<SplashParticleOptions> splashProvider(SpriteSet sprites) {
        return new SplashParticle.Provider(sprites);
    }

    public static void registerSpriteSources(IdRegistrar<MapCodec<? extends SpriteSource>> reg) {
        reg.register("padded", PaddedSpriteSource.MAP_CODEC);
    }

    public static void registerSelectItemModelProperties(
            IdRegistrar<SelectItemModelProperty.Type<?, ?>> reg) {
        reg.register("blueprint_pool", BlueprintPoolProperty.TYPE);
        reg.register("full_schrab", FullSchrabProperty.TYPE);
        reg.register("polaroid", PolaroidProperty.TYPE);
        reg.register("scrap_material", ScrapMaterialProperty.TYPE);
    }

    public static void registerItemTintSources(
            IdRegistrar<MapCodec<? extends ItemTintSource>> reg) {
        reg.register("scraps_color", ScrapsColorTintSource.MAP_CODEC);
        reg.register("bedrock_ore_color", BedrockOreTintSource.MAP_CODEC);
        reg.register("icf_pellet_color", ICFPelletTintSource.MAP_CODEC);
        reg.register("fluid_color", FluidColorTintSource.MAP_CODEC);
        reg.register("fluid_identifier_color", FluidIdentifierTintSource.MAP_CODEC);
        reg.register("pipe_fluid_color", PipeItemTintSource.MAP_CODEC);
        reg.register("canister_color", CanisterTintSource.MAP_CODEC);
        reg.register("gas_bottle_color", GasTankTintSource.BOTTLE_CODEC);
        reg.register("gas_label_color", GasTankTintSource.LABEL_CODEC);
        reg.register("waste_cooling", WasteCoolingTintSource.MAP_CODEC);
        reg.register("cassette_color", CassetteColorTintSource.MAP_CODEC);
        reg.register("chemical_dye_color", ChemicalDyeTintSource.MAP_CODEC);
        reg.register("lore_book_cover", LoreBookTintSource.COVER_CODEC);
        reg.register("lore_book_title", LoreBookTintSource.TITLE_CODEC);
        reg.register("kit_color_1", KitColorTintSource.CODEC_1);
        reg.register("kit_color_2", KitColorTintSource.CODEC_2);
        reg.register("plant_grass", PlantGrassTintSource.MAP_CODEC);
    }

    public static void registerBlockTintSources(BlockTintRegistrar reg) {
        reg.accept(
                List.of(
                        BlockTintSources.constant(CommonColors.WHITE),
                        FluidPipeBlockTintSource.INSTANCE),
                ModBlocks.FLUID_PIPE.get(),
                ModBlocks.FLUID_PIPE_SILVER.get(),
                ModBlocks.FLUID_PIPE_COLORED.get());
        reg.accept(
                List.of(FluidPipeBlockTintSource.LIGHTENED),
                ModBlocks.FLUID_DUCT_BOX[2][0].get(),
                ModBlocks.FLUID_DUCT_BOX[2][1].get(),
                ModBlocks.FLUID_DUCT_BOX[2][2].get(),
                ModBlocks.FLUID_DUCT_BOX[2][3].get(),
                ModBlocks.FLUID_DUCT_BOX[2][4].get());
        reg.accept(
                List.of(FluidPipeBlockTintSource.INSTANCE), ModBlocks.FLUID_DUCT_PAINTABLE.get());
        reg.accept(
                List.of(SellafieldShadeTintSource.INSTANCE),
                ModBlocks.SELLAFIELD_SLAKED.get(),
                ModBlocks.SELLAFIELD_BEDROCK.get(),
                ModBlocks.ORE_SELLAFIELD_DIAMOND.get(),
                ModBlocks.ORE_SELLAFIELD_EMERALD.get(),
                ModBlocks.ORE_SELLAFIELD_RADGEM.get(),
                ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get(),
                ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get());
        reg.accept(List.of(SellafieldLevelTintSource.INSTANCE), ModBlocks.SELLAFIELD.get());
        reg.accept(
                Arrays.stream(CraneRouterBlock.FACE_COLOURS)
                        .mapToObj(BlockTintSources::constant)
                        .toList(),
                ModBlocks.CRANE_ROUTER.get());
        reg.accept(
                List.of(PlantFoliageTintSource.INSTANCE),
                ModBlocks.PLANT_FLOWER_TOBACCO.get(),
                ModBlocks.PLANT_FLOWER_WEED.get(),
                ModBlocks.PLANT_TALL_WEED.get());
        reg.accept(List.of(BedrockOreBlockTintSource.INSTANCE), ModBlocks.ORE_BEDROCK.get());
        reg.accept(List.of(BalefireTintSource.INSTANCE), ModBlocks.BALEFIRE.get());
    }

    public static void registerSpecialModelRenderers(
            IdRegistrar<MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> reg) {}

    public static void registerItemModels(IdRegistrar<MapCodec<? extends ItemModel.Unbaked>> reg) {
        reg.register("bobble", BobbleItemRenderer.Unbaked.MAP_CODEC);
        reg.register("snowglobe", SnowglobeItemRenderer.Unbaked.MAP_CODEC);
        reg.register("plushie", PlushieItemRenderer.Unbaked.MAP_CODEC);
        reg.register("lantern", LanternItemRenderer.Unbaked.MAP_CODEC);
        reg.register("gun", GunItemRenderer.Unbaked.MAP_CODEC);
        reg.register("mesh", MeshItemRenderer.Unbaked.MAP_CODEC);
        reg.register("grenade", GrenadeItemRenderer.Unbaked.MAP_CODEC);
        reg.register("boltgun", BoltgunItemRenderer.Unbaked.MAP_CODEC);
        reg.register("chainsaw", ChainsawItemRenderer.Unbaked.MAP_CODEC);
        reg.register("crucible", CrucibleItemRenderer.Unbaked.MAP_CODEC);
        reg.register("gun_b92", GunB92ItemRenderer.Unbaked.MAP_CODEC);
        reg.register("missile_custom", MissileCustomItemRenderer.Unbaked.MAP_CODEC);
        reg.register("detonator_laser", DetonatorLaserItemRenderer.Unbaked.MAP_CODEC);
        reg.register("radar_large", RadarLargeItemRenderer.Unbaked.MAP_CODEC);
        reg.register("radar", RadarItemRenderer.Unbaked.MAP_CODEC);
        reg.register("compressor", CompressorItemRenderer.Unbaked.MAP_CODEC);
        reg.register("fluid_tank", FluidTankItemRenderer.Unbaked.MAP_CODEC);
        reg.register("hot", HotItemRenderer.Unbaked.MAP_CODEC);
        reg.register("tinted_glint", TintedGlintItemModel.Unbaked.MAP_CODEC);
        reg.register("broken_item_source", BrokenItemSourceModel.Unbaked.MAP_CODEC);
    }

    public static void registerEntityRenderers(EntityRendererRegistrar downstream) {
        Set<EntityType<?>> bound = new HashSet<>();
        EntityRendererRegistrar reg =
                new EntityRendererRegistrar() {
                    @Override
                    public <T extends Entity> void accept(
                            EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                        bound.add(type);
                        downstream.accept(type, provider);
                    }
                };

        reg.accept(ModEntities.BEAM_DISCHARGE.get(), NoopRenderer::new);
        reg.accept(ModEntities.BALEFIRE.get(), NoopRenderer::new);
        reg.accept(ModEntities.TOM_BUST.get(), NoopRenderer::new);
        reg.accept(ModEntities.NUKE_EXPLOSION_MK3.get(), NoopRenderer::new);
        reg.accept(ModEntities.NUKE_EXPLOSION_MK5.get(), NoopRenderer::new);
        reg.accept(ModEntities.EMP_LOGIC.get(), NoopRenderer::new);
        reg.accept(ModEntities.FALLOUT_RAIN.get(), FalloutRenderer::new);
        reg.accept(ModEntities.FALLING_BLOCK_NT.get(), FallingBlockNTRenderer::new);
        reg.accept(ModEntities.FALLING_BLOCK_BATCH.get(), FallingBlockBatchRenderer::new);
        reg.accept(ModEntities.FALLING_MULTIBLOCK.get(), FallingMultiblockRenderer::new);
        reg.accept(ModEntities.NUKE_TOREX.get(), TorexRenderer::new);
        reg.accept(
                ModEntities.VORTEX.get(),
                ctx -> new BlackHoleRenderer<>(ctx, BlackHoleRenderer.Kind.VORTEX));
        reg.accept(
                ModEntities.RAGING_VORTEX.get(),
                ctx -> new BlackHoleRenderer<>(ctx, BlackHoleRenderer.Kind.RAGING));
        reg.accept(
                ModEntities.BLACK_HOLE.get(),
                ctx -> new BlackHoleRenderer<>(ctx, BlackHoleRenderer.Kind.HOLE));
        reg.accept(
                ModEntities.DIGAMMA_QUASAR.get(),
                ctx -> new BlackHoleRenderer<>(ctx, BlackHoleRenderer.Kind.QUASAR));
        reg.accept(ModEntities.UNDEAD_SOLDIER.get(), UndeadSoldierRenderer::new);
        reg.accept(ModEntities.GLYPHID.get(), ctx -> new GlyphidRenderer<>(ctx));
        reg.accept(ModEntities.GLYPHID_BRAWLER.get(), ctx -> new GlyphidRenderer<>(ctx));
        reg.accept(ModEntities.GLYPHID_BEHEMOTH.get(), ctx -> new GlyphidRenderer<>(ctx));
        reg.accept(ModEntities.GLYPHID_BRENDA.get(), ctx -> new GlyphidRenderer<>(ctx));
        reg.accept(ModEntities.GLYPHID_BOMBARDIER.get(), ctx -> new GlyphidRenderer<>(ctx));
        reg.accept(ModEntities.GLYPHID_BLASTER.get(), ctx -> new GlyphidRenderer<>(ctx));
        reg.accept(ModEntities.GLYPHID_SCOUT.get(), ctx -> new GlyphidRenderer<>(ctx));
        reg.accept(ModEntities.GLYPHID_DIGGER.get(), ctx -> new GlyphidRenderer<>(ctx));
        reg.accept(ModEntities.GLYPHID_NUCLEAR.get(), GlyphidNuclearRenderer::new);
        reg.accept(ModEntities.GLYPHID_WAYPOINT.get(), NoopRenderer::new);
        reg.accept(ModEntities.ACID_BOMB.get(), AcidBombRenderer::new);
        reg.accept(ModEntities.PARASITE_MAGGOT.get(), ParasiteMaggotRenderer::new);
        reg.accept(ModEntities.C130.get(), C130Renderer::new);
        reg.accept(ModEntities.BOMBER.get(), BomberRenderer::new);
        reg.accept(ModEntities.GRENADE_UNIVERSAL.get(), GrenadeUniversalRenderer::new);
        reg.accept(
                ModEntities.GRENADE_BOUNCY_GENERIC.get(), ctx -> new GenericGrenadeRenderer<>(ctx));
        reg.accept(ModEntities.DISPERSER_CANISTER.get(), ctx -> new GenericGrenadeRenderer<>(ctx));
        reg.accept(ModEntities.WASTE_PEARL.get(), NoopRenderer::new);
        reg.accept(ModEntities.ITEM_BUOYANT.get(), ItemEntityRenderer::new);
        reg.accept(ModEntities.MOVING_ITEM.get(), MovingItemRenderer::new);
        reg.accept(ModEntities.MOVING_PACKAGE.get(), MovingPackageRenderer::new);
        reg.accept(ModEntities.TNT_NTM.get(), TntRenderer::new);
        reg.accept(ModEntities.CLOUD_FLEIJA.get(), CloudFleijaRenderer::new);
        reg.accept(ModEntities.CLOUD_SOLINIUM.get(), CloudSoliniumRenderer::new);
        reg.accept(ModEntities.CLOUD_RAINBOW.get(), CloudRainbowRenderer::new);
        reg.accept(ModEntities.MOONSTONE_BLAST.get(), CloudTomRenderer::new);
        reg.accept(
                ModEntities.AGENT_ORANGE.get(),
                ctx ->
                        new MultiCloudRenderer<OrangeFXEntity>(
                                ctx,
                                "orange",
                                OrangeFXEntity::particleAge,
                                OrangeFXEntity::maxAge));
        reg.accept(
                ModEntities.CHLORINE_FX.get(),
                ctx ->
                        new MultiCloudRenderer<ChlorineFXEntity>(
                                ctx,
                                "chlorine",
                                ChlorineFXEntity::particleAge,
                                ChlorineFXEntity::maxAge));
        reg.accept(
                ModEntities.CLOUD_FX.get(),
                ctx ->
                        new MultiCloudRenderer<CloudFXEntity>(
                                ctx, "cloud", CloudFXEntity::particleAge, CloudFXEntity::maxAge));
        reg.accept(
                ModEntities.PINK_CLOUD_FX.get(),
                ctx ->
                        new MultiCloudRenderer<PinkCloudFXEntity>(
                                ctx,
                                "pc",
                                PinkCloudFXEntity::particleAge,
                                PinkCloudFXEntity::maxAge));
        reg.accept(ModEntities.METEOR.get(), MeteorRenderer::new);
        reg.accept(ModEntities.BOMBLET_ZETA.get(), BombletZetaRenderer::new);
        reg.accept(ModEntities.PARACHUTE_CRATE.get(), ParachuteCrateRenderer::new);
        reg.accept(ModEntities.CHEMICAL.get(), ChemicalRenderer::new);
        reg.accept(
                ModEntities.BOXCAR.get(),
                ctx -> new BoxcarRenderer<>(ctx, BoxcarRenderer.Kind.BOXCAR));
        reg.accept(
                ModEntities.TORPEDO.get(),
                ctx -> new BoxcarRenderer<>(ctx, BoxcarRenderer.Kind.TORPEDO));
        reg.accept(
                ModEntities.DUCHESS_GAMBIT.get(),
                ctx -> new BoxcarRenderer<>(ctx, BoxcarRenderer.Kind.DUCHESS_GAMBIT));
        reg.accept(
                ModEntities.FALLING_BUILDING.get(),
                ctx -> new BoxcarRenderer<>(ctx, BoxcarRenderer.Kind.BUILDING));

        reg.accept(
                ModEntities.MISSILE_GENERIC.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_HE_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_INCENDIARY.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_IN_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_DECOY.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_decoy_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_BUSTER.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_BU_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_STRONG.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_HE_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_INCENDIARY_STRONG.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_IN_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_BUSTER_STRONG.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_BU_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_BURST.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileHuge,
                                ResourceManager.missileHuge_HE_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_INFERNO.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileHuge,
                                ResourceManager.missileHuge_IN_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_NUCLEAR.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileNuclear_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_NUCLEAR_CLUSTER.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileMIRV_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_STEALTH.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileStealth,
                                ResourceManager.missileStealth_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_DOOMSDAY.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileDoomsday_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_DOOMSDAY_RUSTED.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileDoomsdayRusted_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_MICRO.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicro_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_TAINT.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroTaint_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_BHOLE.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroBHole_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_SCHRABIDIUM.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroSchrab_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_EMP.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroEMP_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_TEST.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroTest_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_CLUSTER.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_CL_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_CLUSTER_STRONG.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_CL_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_EMP_STRONG.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_EMP_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_RAIN.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileHuge,
                                ResourceManager.missileHuge_CL_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_DRILL.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileHuge,
                                ResourceManager.missileHuge_BU_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_VOLCANO.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileVolcano_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_SHUTTLE.get(),
                ctx ->
                        new MissileRenderer(
                                ctx,
                                ResourceManager.missileShuttle,
                                ResourceManager.missileShuttle_tex,
                                1F));
        reg.accept(ModEntities.MISSILE_CUSTOM.get(), MissileCustomRenderer::new);
        reg.accept(ModEntities.MISSILE_ANTI.get(), MissileAntiBallisticRenderer::new);

        reg.accept(ModEntities.RUBBLE.get(), RubbleRenderer::new);
        reg.accept(ModEntities.RBMK_DEBRIS.get(), RBMKDebrisRenderer::new);
        reg.accept(ModEntities.ZIRNOX_DEBRIS.get(), ZirnoxDebrisRenderer::new);
        reg.accept(ModEntities.SPEAR.get(), SpearRenderer::new);
        reg.accept(ModEntities.MIST.get(), NoopRenderer::new);
        reg.accept(ModEntities.WASTE_ITEM.get(), ItemEntityRenderer::new);
        reg.accept(ModEntities.SHRAPNEL.get(), ctx -> new ShrapnelRenderer<>(ctx));
        reg.accept(ModEntities.BULLET_MK4.get(), BulletMK4Renderer::new);
        reg.accept(ModEntities.BULLET_BEAM.get(), BeamRenderer::new);
        reg.accept(ModEntities.BULLET.get(), BulletRenderer::new);
        reg.accept(ModEntities.COIN.get(), CoinRenderer::new);
        reg.accept(ModEntities.FIRE_LINGERING.get(), NoopRenderer::new);

        reg.accept(ModEntities.ARTILLERY_SHELL.get(), ArtilleryShellRenderer::new);
        reg.accept(ModEntities.HIMARS.get(), ArtilleryRocketRenderer::new);
        reg.accept(ModEntities.COG.get(), CogRenderer::cog);
        reg.accept(ModEntities.STRAY_SAW.get(), CogRenderer::sawblade);
        reg.accept(ModEntities.CHOPPER_MINE.get(), ChopperMineRenderer::new);
        reg.accept(ModEntities.FIREWORK_BALL.get(), ctx -> new ShrapnelRenderer<>(ctx));
        reg.accept(ModEntities.FALLING_BOMB.get(), FallingNukeRenderer::new);
        reg.accept(ModEntities.BURNING_FOEQ.get(), FOEQRenderer::new);
        reg.accept(ModEntities.BOBMAZON_DELIVERY.get(), MinerRocketRenderer::bobmazon);
        reg.accept(ModEntities.SOYUZ.get(), SoyuzRenderer::new);
        reg.accept(ModEntities.SOYUZ_CAPSULE.get(), SoyuzCapsuleRenderer::new);
        reg.accept(ModEntities.SATELLITE_POD.get(), DropshipRenderer::new);
        reg.accept(ModEntities.TOM_THE_MOONSTONE.get(), TomRenderer::new);
        reg.accept(ModEntities.EMP_BLAST.get(), EMPBlastRenderer::new);
        reg.accept(ModEntities.LASER_BLAST.get(), DeathBlastRenderer::new);
        reg.accept(ModEntities.ORBITAL_LASER.get(), OrbitalLaserRenderer::new);
        reg.accept(ModEntities.BEAM_BOMB.get(), BeamBombRenderer::new);
        reg.accept(ModEntities.SIEGE_LASER.get(), SiegeLaserRenderer::new);
        reg.accept(ModEntities.DELIVERY_DRONE.get(), ctx -> new DeliveryDroneRenderer<>(ctx));
        reg.accept(ModEntities.REQUEST_DRONE.get(), ctx -> new DeliveryDroneRenderer<>(ctx));

        for (var cart :
                List.of(
                        ModEntities.CART_ORE,
                        ModEntities.CART_POWDER,
                        ModEntities.CART_SEMTEX,
                        ModEntities.CART_CRATE,
                        ModEntities.CART_DESTROYER)) {
            reg.accept(cart.get(), ctx -> new MinecartRenderer(ctx, ModelLayers.MINECART));
        }

        reg.accept(ModEntities.CART_TEST.get(), MinecartTestRenderer::new);
        reg.accept(ModEntities.BOAT_RUBBER.get(), BoatRubberRenderer::new);

        reg.accept(ModEntities.MASK_MAN.get(), MaskManRenderer::new);
        reg.accept(ModEntities.UFO.get(), UFORenderer::new);
        reg.accept(ModEntities.BALLS_O_TRON.get(), BOTPrimeRenderer::head);
        reg.accept(ModEntities.BALLS_O_TRON_SEG.get(), BOTPrimeRenderer::body);

        reg.accept(ModEntities.CARGO_TRAM.get(), ctx -> new TrainCargoTramRenderer<>(ctx));
        reg.accept(ModEntities.CARGO_TRAM_TRAILER.get(), TrainCargoTramTrailerRenderer::new);
        reg.accept(ModEntities.BOUNDING_DUMMY.get(), NoopRenderer::new);
        reg.accept(ModEntities.SEAT_DUMMY.get(), NoopRenderer::new);

        reg.accept(
                ModEntities.CREEPER_NUCLEAR.get(),
                ctx -> new CreeperUniversalRenderer(ctx, ResourceManager.creeper_nuclear_tex, 5F));
        reg.accept(
                ModEntities.CREEPER_TAINTED.get(),
                ctx -> new CreeperUniversalRenderer(ctx, ResourceManager.creeper_tainted_tex, 1F));
        reg.accept(
                ModEntities.CREEPER_PHOSGENE.get(),
                ctx -> new CreeperUniversalRenderer(ctx, ResourceManager.creeper_phosgene_tex, 1F));
        reg.accept(
                ModEntities.CREEPER_VOLATILE.get(),
                ctx -> new CreeperUniversalRenderer(ctx, ResourceManager.creeper_volatile_tex, 1F));
        reg.accept(
                ModEntities.CREEPER_GOLD.get(),
                ctx -> new CreeperUniversalRenderer(ctx, ResourceManager.creeper_gold_tex, 1F));

        reg.accept(ModEntities.CYBER_CRAB.get(), CyberCrabRenderer::new);
        reg.accept(ModEntities.TESLA_CRAB.get(), TeslaCrabRenderer::tesla);
        reg.accept(ModEntities.TAINT_CRAB.get(), TeslaCrabRenderer::taint);
        reg.accept(ModEntities.BLOCK_SPIDER.get(), BlockSpiderRenderer::new);
        reg.accept(ModEntities.DUCK.get(), DuckRenderer::duck);
        reg.accept(ModEntities.QUACKOS.get(), DuckRenderer::quackos);
        reg.accept(ModEntities.PIGEON.get(), PigeonRenderer::new);
        reg.accept(ModEntities.PLASTIC_BAG.get(), PlasticBagRenderer::new);
        reg.accept(ModEntities.GHOST.get(), HumanoidMobRenderer::ghost);
        reg.accept(ModEntities.TEST_DUMMY.get(), HumanoidMobRenderer::dummy);
        reg.accept(ModEntities.FBI.get(), HumanoidMobRenderer::fbi);
        reg.accept(ModEntities.FBI_DRONE.get(), FBIDroneRenderer::new);
        reg.accept(ModEntities.RAD_BEAST.get(), RADBeastRenderer::new);
        reg.accept(ModEntities.HUNTER_CHOPPER.get(), HunterChopperRenderer::new);

        verifyEntityRenderers(downstream, bound::contains);
    }

    private static void verifyEntityRenderers(
            EntityRendererRegistrar reg, Predicate<EntityType<?>> bound) {
        List<String> missing = new ArrayList<>();
        for (Field field : ModEntities.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!RegistryHandle.class.isAssignableFrom(field.getType())) continue;
            if (!(field.getGenericType() instanceof ParameterizedType parameterized)) continue;
            if (!parameterized
                    .getActualTypeArguments()[0]
                    .getTypeName()
                    .startsWith(EntityType.class.getName())) continue;
            try {
                if (!(((RegistryHandle<?>) field.get(null)).get() instanceof EntityType<?> type))
                    continue;
                if (bound.test(type)) continue;
                missing.add(field.getName());
                reg.accept(type, NoopRenderer::new);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "ModEntities." + field.getName() + " is not readable", e);
            }
        }
        if (!missing.isEmpty()) {
            LoggerFactory.getLogger("NTM")
                    .error(
                            "No entity renderer bound for {} - falling back to NoopRenderer so the render thread survives. "
                                    + "Bind them in ClientRegistry#registerEntityRenderers.",
                            String.join(", ", missing));
        }
    }

    public static void registerMenuScreens(MenuScreenRegistrar reg) {
        Jars.initJars();
        reg.accept(ModMenus.MACHINE_CENTRIFUGE.get(), CentrifugeScreen::new);
        reg.accept(ModMenus.MACHINE_GASCENT.get(), GasCentScreen::new);
        reg.accept(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
        reg.accept(ModMenus.MACHINE_PRESS.get(), PressScreen::new);
        reg.accept(ModMenus.MACHINE_AUTOCRAFTER.get(), AutocrafterScreen::new);
        reg.accept(ModMenus.PNEUMATIC_TUBE.get(), PneumoTubeScreen::new);
        reg.accept(ModMenus.CRANE_INSERTER.get(), CraneInserterScreen::new);
        reg.accept(ModMenus.CRANE_EXTRACTOR.get(), CraneExtractorScreen::new);
        reg.accept(ModMenus.CRANE_GRABBER.get(), CraneGrabberScreen::new);
        reg.accept(ModMenus.CRANE_BOXER.get(), CraneBoxerScreen::new);
        reg.accept(ModMenus.CRANE_UNBOXER.get(), CraneUnboxerScreen::new);
        reg.accept(ModMenus.CRANE_ROUTER.get(), CraneRouterScreen::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_ACCESS.get(), PneumoStorageAccessScreen::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_CLUTTER.get(), PneumoStorageClutterScreen::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_MONO.get(), PneumoStorageMonoScreen::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_IMPORTER.get(), PneumoStorageImporterScreen::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_EXPORTER.get(), PneumoStorageExporterScreen::new);
        reg.accept(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), AssemblyMachineScreen::new);
        reg.accept(ModMenus.MACHINE_ASSEMBLY_FACTORY.get(), AssemblyFactoryScreen::new);
        reg.accept(ModMenus.MACHINE_CHEMICAL_PLANT.get(), ChemicalPlantScreen::new);
        reg.accept(ModMenus.MACHINE_ROCK_MILL.get(), RockMillScreen::new);
        reg.accept(ModMenus.MACHINE_CUSTOM.get(), CustomScreen::new);
        reg.accept(ModMenus.MACHINE_RADIOLYSIS.get(), RadiolysisScreen::new);
        reg.accept(ModMenus.FUSION_BREEDER.get(), FusionBreederScreen::new);
        reg.accept(ModMenus.CORE_CORE.get(), CoreScreen::new);
        reg.accept(ModMenus.CORE_EMITTER.get(), CoreEmitterScreen::new);
        reg.accept(ModMenus.CORE_RECEIVER.get(), CoreReceiverScreen::new);
        reg.accept(ModMenus.CORE_INJECTOR.get(), CoreInjectorScreen::new);
        reg.accept(ModMenus.CORE_STABILIZER.get(), CoreStabilizerScreen::new);
        reg.accept(ModMenus.FUSION_KLYSTRON.get(), FusionKlystronScreen::new);
        reg.accept(ModMenus.FUSION_TORUS.get(), FusionTorusScreen::new);
        reg.accept(ModMenus.MACHINE_PLASMA_FORGE.get(), PlasmaForgeScreen::new);
        reg.accept(ModMenus.MACHINE_CHEMICAL_FACTORY.get(), ChemicalFactoryScreen::new);
        reg.accept(ModMenus.MACHINE_ELECTROLYSER_FLUID.get(), ElectrolyserFluidScreen::new);
        reg.accept(ModMenus.MACHINE_ELECTROLYSER_METAL.get(), ElectrolyserMetalScreen::new);
        reg.accept(ModMenus.MACHINE_REFINERY.get(), RefineryScreen::new);
        reg.accept(ModMenus.MACHINE_CATALYTIC_REFORMER.get(), CatalyticReformerScreen::new);
        reg.accept(ModMenus.MACHINE_HYDROTREATER.get(), HydrotreaterScreen::new);
        reg.accept(ModMenus.MACHINE_PUREX.get(), PUREXScreen::new);
        reg.accept(ModMenus.MACHINE_MIXER.get(), MixerScreen::new);
        reg.accept(ModMenus.MACHINE_SILEX.get(), SILEXScreen::new);
        reg.accept(ModMenus.MACHINE_ICF.get(), ICFScreen::new);
        reg.accept(ModMenus.MACHINE_ICF_PRESS.get(), ICFPressScreen::new);
        reg.accept(ModMenus.MACHINE_ORE_SLOPPER.get(), OreSlopperScreen::new);
        reg.accept(ModMenus.MACHINE_ANNIHILATOR.get(), AnnihilatorScreen::new);
        reg.accept(ModMenus.MACHINE_BLAST_FURNACE.get(), BlastFurnaceScreen::new);
        reg.accept(ModMenus.MACHINE_CRUCIBLE.get(), CrucibleScreen::new);
        reg.accept(ModMenus.MACHINE_STRAND_CASTER.get(), StrandCasterScreen::new);
        reg.accept(ModMenus.MACHINE_PRECASS.get(), PrecAssScreen::new);
        reg.accept(ModMenus.MACHINE_MINING_LASER.get(), MiningLaserScreen::new);
        reg.accept(ModMenus.FORCEFIELD.get(), ForceFieldScreen::new);
        reg.accept(ModMenus.MACHINE_EXCAVATOR.get(), ExcavatorScreen::new);
        reg.accept(ModMenus.MACHINE_CYCLOTRON.get(), CyclotronScreen::new);
        reg.accept(ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), ExposureChamberScreen::new);
        reg.accept(ModMenus.MACHINE_RADGEN.get(), RadGenScreen::new);
        reg.accept(ModMenus.MACHINE_RADAR.get(), RadarScreen::new);
        reg.accept(ModMenus.MACHINE_RADAR_SLOTS.get(), RadarSlotsScreen::new);
        reg.accept(ModMenus.MACHINE_REACTOR_BREEDING.get(), ReactorBreedingScreen::new);
        reg.accept(ModMenus.REACTOR_RESEARCH.get(), ReactorResearchScreen::new);
        reg.accept(ModMenus.REACTOR_CONTROL.get(), ReactorControlScreen::new);
        reg.accept(ModMenus.MACHINE_ROTARY_FURNACE.get(), RotaryFurnaceScreen::new);
        reg.accept(ModMenus.MACHINE_ARC_FURNACE.get(), ArcFurnaceScreen::new);
        reg.accept(ModMenus.HEATER_FIREBOX.get(), FireboxScreen::new);
        reg.accept(ModMenus.MACHINE_WELL.get(), OilWellScreen::new);
        reg.accept(ModMenus.MACHINE_COMPRESSOR.get(), CompressorScreen::new);
        reg.accept(ModMenus.MACHINE_GAS_FLARE.get(), GasFlareScreen::new);
        reg.accept(ModMenus.REACTOR_ZIRNOX.get(), ReactorZirnoxScreen::new);
        reg.accept(ModMenus.WATZ.get(), WatzScreen::new);
        reg.accept(ModMenus.MACHINE_FEL.get(), FELScreen::new);
        reg.accept(ModMenus.PA_SOURCE.get(), PASourceScreen::new);
        reg.accept(ModMenus.PA_RFC.get(), PARFCScreen::new);
        reg.accept(ModMenus.PA_QUADRUPOLE.get(), PAQuadrupoleScreen::new);
        reg.accept(ModMenus.PA_DIPOLE.get(), PADipoleScreen::new);
        reg.accept(ModMenus.PA_DETECTOR.get(), PADetectorScreen::new);
        reg.accept(ModMenus.MACHINE_SOLDERING_STATION.get(), SolderingStationScreen::new);
        reg.accept(ModMenus.MACHINE_ARC_WELDER.get(), ArcWelderScreen::new);
        reg.accept(ModMenus.MACHINE_EPRESS.get(), EPressScreen::new);
        reg.accept(ModMenus.MACHINE_SHREDDER.get(), ShredderScreen::new);
        reg.accept(ModMenus.TURRET_BASE.get(), TurretBaseScreen::new);
        reg.accept(ModMenus.MACHINE_FLUID_TANK.get(), FluidTankScreen::new);
        reg.accept(ModMenus.BARREL.get(), BarrelScreen::new);
        reg.accept(ModMenus.MASS_STORAGE.get(), MassStorageScreen::new);
        reg.accept(ModMenus.FILE_CABINET.get(), FileCabinetScreen::new);
        reg.accept(ModMenus.MACHINE_WOOD_BURNER.get(), WoodBurnerScreen::new);
        reg.accept(ModMenus.MACHINE_DIESEL.get(), DieselScreen::new);
        reg.accept(ModMenus.MACHINE_RTG.get(), RTGScreen::new);
        reg.accept(ModMenus.MACHINE_TURBINE.get(), TurbineScreen::new);
        reg.accept(ModMenus.MACHINE_LARGE_TURBINE.get(), LargeTurbineScreen::new);
        reg.accept(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), CombustionEngineScreen::new);
        reg.accept(ModMenus.MACHINE_TURBINEGAS.get(), TurbineGasScreen::new);
        reg.accept(ModMenus.MACHINE_TURBOFAN.get(), TurbofanScreen::new);
        reg.accept(ModMenus.WASTE_DRUM.get(), WasteDrumScreen::new);
        reg.accept(ModMenus.STORAGE_DRUM.get(), StorageDrumScreen::new);
        reg.accept(ModMenus.BATTERY_SOCKET.get(), BatterySocketScreen::new);
        reg.accept(ModMenus.MACHINE_BATTERY.get(), BatteryScreen::new);
        reg.accept(ModMenus.BATTERY_REDD.get(), BatteryREDDScreen::new);
        reg.accept(ModMenus.TRAIN_CARGO_TRAM.get(), TrainCargoTramScreen::new);
        reg.accept(ModMenus.TRAIN_CARGO_TRAM_TRAILER.get(), TrainCargoTramTrailerScreen::new);
        reg.accept(ModMenus.CART_DESTROYER.get(), CartDestroyerScreen::new);
        reg.accept(ModMenus.NUKE_MAN.get(), NukeManScreen::new);
        reg.accept(ModMenus.NUKE_GADGET.get(), NukeGadgetScreen::new);
        reg.accept(ModMenus.NUKE_BOY.get(), NukeBoyScreen::new);
        reg.accept(ModMenus.NUKE_MIKE.get(), NukeMikeScreen::new);
        reg.accept(ModMenus.NUKE_TSAR.get(), NukeTsarScreen::new);
        reg.accept(ModMenus.NUKE_FLEIJA.get(), NukeFleijaScreen::new);
        reg.accept(ModMenus.NUKE_PROTOTYPE.get(), NukePrototypeScreen::new);
        reg.accept(ModMenus.NUKE_SOLINIUM.get(), NukeSoliniumScreen::new);
        reg.accept(ModMenus.NUKE_N2.get(), NukeN2Screen::new);
        reg.accept(ModMenus.MACHINE_MISSILE_ASSEMBLY.get(), MissileAssemblyScreen::new);
        reg.accept(ModMenus.LAUNCH_PAD.get(), LaunchPadScreen::new);
        reg.accept(ModMenus.LAUNCH_PAD_LARGE.get(), LaunchPadScreen::new);
        reg.accept(ModMenus.LAUNCH_PAD_RUSTED.get(), LaunchPadRustedScreen::new);
        reg.accept(ModMenus.LAUNCH_TABLE.get(), LaunchTableScreen::new);
        reg.accept(ModMenus.COMPACT_LAUNCHER.get(), CompactLauncherScreen::new);
        reg.accept(ModMenus.NUKE_CUSTOM.get(), NukeCustomScreen::new);
        reg.accept(ModMenus.NUKE_FSTBMB.get(), NukeFstbmbScreen::new);
        reg.accept(ModMenus.BOMB_MULTI.get(), BombMultiScreen::new);
        reg.accept(ModMenus.SOYUZ_CAPSULE.get(), SoyuzCapsuleScreen::new);
        reg.accept(ModMenus.SOYUZ_LAUNCHER.get(), SoyuzLauncherScreen::new);
        reg.accept(ModMenus.LAUNCHPAD_SOYUZ.get(), LaunchpadSoyuzScreen::new);
        reg.accept(ModMenus.SAT_DOCK.get(), SatDockScreen::new);
        reg.accept(ModMenus.RADIO_TORCH_COUNTER.get(), RadioTorchCounterScreen::new);
        reg.accept(ModMenus.CABLE_DIODE.get(), CableDiodeScreen::new);
        reg.accept(ModMenus.FLUID_PUMP.get(), FluidPumpScreen::new);
        reg.accept(ModMenus.RADIO_REC.get(), RadioRecScreen::new);
        reg.accept(ModMenus.RADIO_TELEX.get(), RadioTelexScreen::new);
        reg.accept(ModMenus.RADIO_AUTOCAL.get(), RadioAUTOCALScreen::new);
        reg.accept(ModMenus.RBMK_GAUGE.get(), RBMKGaugeScreen::new);
        reg.accept(ModMenus.RBMK_GRAPH.get(), RBMKGraphScreen::new);
        reg.accept(ModMenus.RBMK_INDICATOR.get(), RBMKIndicatorScreen::new);
        reg.accept(ModMenus.RBMK_KEYPAD.get(), RBMKKeyPadScreen::new);
        reg.accept(ModMenus.RBMK_LEVER.get(), RBMKLeverScreen::new);
        reg.accept(ModMenus.RBMK_NUMITRON.get(), RBMKNumitronScreen::new);
        reg.accept(ModMenus.RBMK_TERMINAL.get(), RBMKTerminalScreen::new);
        reg.accept(ModMenus.RADIO_TORCH.get(), RadioTorchScreen::new);
        reg.accept(ModMenus.CATALOG.get(), GUIScreenBobmazon::new);
        reg.accept(ModMenus.FLUID_IDENTIFIER.get(), FluidIdentifierScreen::new);
        reg.accept(ModMenus.DESIGNATOR_MANUAL.get(), ManualDesignatorScreen::new);
        reg.accept(ModMenus.RTTY_PAGER.get(), PagerScreen::new);
        reg.accept(ModMenus.SATELLITE_INTERFACE.get(), SatCoordScreen::new);
        reg.accept(ModMenus.TOOL_PRESETS.get(), ToolAbilityScreen::new);
        reg.accept(ModMenus.DRONE_CRATE.get(), DroneCrateScreen::new);
        reg.accept(ModMenus.DRONE_DOCK.get(), DroneDockScreen::new);
        reg.accept(ModMenus.DRONE_PROVIDER.get(), DroneProviderScreen::new);
        reg.accept(ModMenus.DRONE_REQUESTER.get(), DroneRequesterScreen::new);
        reg.accept(ModMenus.WEAPON_TABLE.get(), WeaponTableScreen::new);
        reg.accept(ModMenus.BOOK_OF.get(), BookScreen::new);
        reg.accept(ModMenus.LEMEGETON.get(), LemegetonScreen::new);
        reg.accept(ModMenus.ARMOR_TABLE.get(), ArmorTableScreen::new);
        reg.accept(ModMenus.RBMK_ROD.get(), RBMKRodScreen::new);
        reg.accept(ModMenus.RBMK_CONTROL.get(), RBMKControlScreen::new);
        reg.accept(ModMenus.RBMK_CONTROL_AUTO.get(), RBMKControlAutoScreen::new);
        reg.accept(ModMenus.RBMK_BOILER.get(), RBMKBoilerScreen::new);
        reg.accept(ModMenus.RBMK_STORAGE.get(), RBMKStorageScreen::new);
        reg.accept(ModMenus.RBMK_HEATER.get(), RBMKHeaterScreen::new);
        reg.accept(ModMenus.RBMK_OUTGASSER.get(), RBMKOutgasserScreen::new);
        reg.accept(ModMenus.RBMK_CONSOLE.get(), RBMKConsoleScreen::new);
        reg.accept(ModMenus.RBMK_AUTOLOADER.get(), RBMKAutoloaderScreen::new);
        reg.accept(ModMenus.PWR_CONTROLLER.get(), PWRScreen::new);
        reg.accept(ModMenus.FURNACE_IRON.get(), FurnaceIronScreen::new);
        reg.accept(ModMenus.FURNACE_STEEL.get(), FurnaceSteelScreen::new);
        reg.accept(ModMenus.FURNACE_COMBINATION.get(), FurnaceCombinationScreen::new);
        reg.accept(ModMenus.FURNACE_BRICK.get(), FurnaceBrickScreen::new);
        reg.accept(ModMenus.HEATER_OVEN.get(), HeaterOvenScreen::new);
        reg.accept(ModMenus.MACHINE_ASHPIT.get(), AshpitScreen::new);
        reg.accept(ModMenus.MACHINE_SIREN.get(), SirenScreen::new);
        reg.accept(ModMenus.HEATER_OILBURNER.get(), OilburnerScreen::new);
        reg.accept(ModMenus.HEATER_HEATEX.get(), HeatexScreen::new);
        reg.accept(ModMenus.MACHINE_CRYSTALLIZER.get(), CrystallizerScreen::new);
        reg.accept(ModMenus.MACHINE_COKER.get(), CokerScreen::new);
        reg.accept(ModMenus.MACHINE_LIQUEFACTOR.get(), LiquefactorScreen::new);
        reg.accept(ModMenus.MACHINE_SOLIDIFIER.get(), SolidifierScreen::new);
        reg.accept(ModMenus.MACHINE_PYROOVEN.get(), PyroOvenScreen::new);
        reg.accept(ModMenus.MACHINE_VACUUM_DISTILL.get(), VacuumDistillScreen::new);
        reg.accept(ModMenus.MACHINE_FUNNEL.get(), FunnelScreen::new);
        reg.accept(ModMenus.MACHINE_MICROWAVE.get(), MicrowaveScreen::new);
        reg.accept(ModMenus.MACHINE_SATLINKER.get(), SatLinkerScreen::new);
        reg.accept(ModMenus.MACHINE_SUPERCOMPUTER.get(), SuperComputerScreen::new);
        reg.accept(ModMenus.MACHINE_TAPE_DRIVE.get(), TapeDriveScreen::new);
        reg.accept(ModMenus.MACHINE_KEYFORGE.get(), KeyForgeScreen::new);
        reg.accept(ModMenus.MACHINE_AMMO_PRESS.get(), AmmoPressScreen::new);
        for (int tier : ModBlocks.ANVIL_TIERS)
            reg.accept(ModMenus.anvilMenuType(tier).get(), AnvilScreen::new);
        for (CrateType type : CrateType.values())
            reg.accept(ModMenus.crateMenuType(type).get(), CrateScreen::new);
        reg.accept(ModMenus.CONTAINMENT_BOX.get(), ItemBoxScreen::new);
        reg.accept(ModMenus.TOOLBOX.get(), ItemBoxScreen::new);
        reg.accept(ModMenus.AMMO_BAG.get(), ItemBoxScreen::new);
        reg.accept(ModMenus.CASING_BAG.get(), ItemBoxScreen::new);
        reg.accept(ModMenus.PLASTIC_BAG.get(), ItemBoxScreen::new);
        reg.accept(ModMenus.REBAR_PLACER.get(), RebarPlacerScreen::new);
    }

    public static void registerBlockEntityRenderers(BlockEntityRendererRegistrar reg) {
        reg.accept(ModBlockEntities.FEL.get(), context -> new FELRenderer(context));
        reg.accept(ModBlockEntities.RAIL.get(), context -> new RailRenderer());
        reg.accept(ModBlockEntities.PA_BEAMLINE.get(), context -> new PABeamlineRenderer(context));
        reg.accept(ModBlockEntities.TESLA_COIL.get(), context -> new TeslaRenderer());
        reg.accept(ModBlockEntities.BATTERY_SOCKET.get(), BatterySocketRenderer::new);
        reg.accept(ModBlockEntities.NTM_CHARGER.get(), context -> new ChargerRenderer());
        reg.accept(ModBlockEntities.REFUELER.get(), RefuelerRenderer::new);
        reg.accept(
                ModBlockEntities.ASSEMBLYMACHINE.get(),
                context -> new AssemblyMachineRenderer(context));
        reg.accept(
                ModBlockEntities.ASSEMBLYFACTORY.get(),
                context -> new AssemblyFactoryRenderer(context));
        reg.accept(ModBlockEntities.CHEMICALPLANT.get(), context -> new ChemicalPlantRenderer());
        reg.accept(ModBlockEntities.ROCK_MILL.get(), context -> new RockMillRenderer());
        reg.accept(ModBlockEntities.FUSION_TORUS.get(), context -> new FusionTorusRenderer());
        reg.accept(ModBlockEntities.CORE_CORE.get(), context -> new CoreRenderer());
        reg.accept(ModBlockEntities.CORE_EMITTER.get(), context -> new CoreComponentRenderer());
        reg.accept(ModBlockEntities.CORE_INJECTOR.get(), context -> new CoreComponentRenderer());
        reg.accept(ModBlockEntities.CORE_STABILIZER.get(), context -> new CoreComponentRenderer());
        reg.accept(ModBlockEntities.FUSION_KLYSTRON.get(), context -> new FusionKlystronRenderer());
        reg.accept(
                ModBlockEntities.FUSION_KLYSTRON_CREATIVE.get(),
                context -> new FusionKlystronCreativeRenderer());
        reg.accept(ModBlockEntities.FUSION_MHDT.get(), context -> new FusionMHDTRenderer());
        reg.accept(ModBlockEntities.FUSION_PLASMA_FORGE.get(), FusionPlasmaForgeRenderer::new);
        reg.accept(
                ModBlockEntities.CHEMICALFACTORY.get(), context -> new ChemicalFactoryRenderer());
        reg.accept(ModBlockEntities.ELECTROLYSER.get(), context -> new ElectrolyserRenderer());
        reg.accept(ModBlockEntities.PUREX.get(), context -> new PUREXRenderer());
        reg.accept(ModBlockEntities.MIXER.get(), context -> new MixerRenderer());
        reg.accept(ModBlockEntities.ORE_SLOPPER.get(), context -> new OreSlopperRenderer(context));
        reg.accept(ModBlockEntities.ANNIHILATOR.get(), context -> new AnnihilatorRenderer(context));
        reg.accept(ModBlockEntities.SUPERCOMPUTER.get(), context -> new SuperComputerRenderer());
        reg.accept(ModBlockEntities.TAPE_DRIVE.get(), context -> new TapeDriveRenderer());
        reg.accept(ModBlockEntities.HEPHAESTUS.get(), context -> new HephaestusRenderer());
        reg.accept(ModBlockEntities.CRUCIBLE.get(), context -> new CrucibleRenderer());
        reg.accept(ModBlockEntities.STRAND_CASTER.get(), context -> new StrandCasterRenderer());
        reg.accept(ModBlockEntities.PRECASS.get(), context -> new PrecAssRenderer(context));
        reg.accept(ModBlockEntities.ROTARY_FURNACE.get(), context -> new RotaryFurnaceRenderer());
        reg.accept(ModBlockEntities.ARC_FURNACE_LARGE.get(), context -> new ArcFurnaceRenderer());
        reg.accept(ModBlockEntities.FIREBOX.get(), context -> new FireboxRenderer());
        reg.accept(ModBlockEntities.FOUNDRY_MOLD.get(), context -> new FoundryRenderer(context));
        reg.accept(ModBlockEntities.FOUNDRY_BASIN.get(), context -> new FoundryRenderer(context));
        reg.accept(ModBlockEntities.FOUNDRY_CHANNEL.get(), context -> new FoundryChannelRenderer());
        reg.accept(ModBlockEntities.FOUNDRY_OUTLET.get(), context -> new FoundryOutletRenderer());
        reg.accept(ModBlockEntities.FOUNDRY_SLAGTAP.get(), context -> new FoundryOutletRenderer());
        reg.accept(ModBlockEntities.FOUNDRY_TANK.get(), context -> new FoundryTankRenderer());
        reg.accept(ModBlockEntities.FOUNDRY_SLAG.get(), context -> new SlagRenderer());
        reg.accept(ModBlockEntities.NTM_LOOT.get(), context -> new LootRenderer(context));
        reg.accept(ModBlockEntities.EXPLOSIVE_CHARGE.get(), ExplosiveChargeRenderer::new);
        reg.accept(ModBlockEntities.MACHINE_PUMPJACK.get(), context -> new PumpjackRenderer());
        reg.accept(ModBlockEntities.MINING_LASER.get(), context -> new LaserMinerRenderer());
        reg.accept(ModBlockEntities.FORCEFIELD.get(), context -> new ForceFieldRenderer());
        reg.accept(ModBlockEntities.EXCAVATOR.get(), context -> new ExcavatorRenderer());
        reg.accept(ModBlockEntities.CYCLOTRON.get(), CyclotronRenderer::new);
        reg.accept(
                ModBlockEntities.EXPOSURE_CHAMBER.get(), context -> new ExposureChamberRenderer());
        reg.accept(ModBlockEntities.RADGEN.get(), context -> new RadGenRenderer());
        reg.accept(
                ModBlockEntities.LANTERN_BEHEMOTH.get(), context -> new LanternBehemothRenderer());
        reg.accept(ModBlockEntities.LANTERN.get(), context -> new LanternRenderer());
        reg.accept(ModBlockEntities.RADAR.get(), context -> new RadarRenderer());
        reg.accept(ModBlockEntities.RADAR_LARGE.get(), context -> new RadarLargeRenderer());
        reg.accept(ModBlockEntities.SATLINK.get(), context -> new SatLinkRenderer());
        reg.accept(ModBlockEntities.RADAR_SCREEN.get(), context -> new RadarScreenRenderer());
        reg.accept(
                ModBlockEntities.MACHINE_REACTOR_BREEDING.get(), context -> new BreederRenderer());
        reg.accept(ModBlockEntities.REACTOR_RESEARCH.get(), context -> new SmallReactorRenderer());
        reg.accept(ModBlockEntities.COMPRESSOR.get(), context -> new CompressorRenderer());
        reg.accept(ModBlockEntities.DIESEL_GENERATOR.get(), context -> new DieselRenderer());
        reg.accept(
                ModBlockEntities.INDUSTRIAL_TURBINE.get(), context -> new LargeTurbineRenderer());
        reg.accept(ModBlockEntities.MACHINE_TURBOFAN.get(), context -> new TurbofanRenderer());
        reg.accept(ModBlockEntities.LPW2.get(), context -> new LPW2Renderer());
        reg.accept(ModBlockEntities.CARGO_ELEVATOR.get(), context -> new CargoElevatorRenderer());
        reg.accept(ModBlockEntities.IND_TURBINE.get(), context -> new IndustrialTurbineRenderer());
        reg.accept(ModBlockEntities.CHUNGUS.get(), context -> new ChungusRenderer());
        reg.accept(ModBlockEntities.HEAT_BOILER.get(), context -> new HeatBoilerRenderer());
        reg.accept(ModBlockEntities.STEAM_ENGINE.get(), context -> new SteamEngineRenderer());
        reg.accept(ModBlockEntities.FENSU.get(), context -> new FENSURenderer());
        reg.accept(ModBlockEntities.BATTERY_REDD.get(), context -> new BatteryREDDRenderer());
        reg.accept(
                ModBlockEntities.COMBUSTION_ENGINE.get(),
                context -> new CombustionEngineRenderer());
        reg.accept(ModBlockEntities.STEAM_PUMP.get(), context -> new PumpRenderer());
        reg.accept(ModBlockEntities.SOLARMIRROR.get(), context -> new SolarMirrorRenderer());
        reg.accept(ModBlockEntities.SOLARBOILER.get(), context -> new SolarBoilerRenderer(context));
        reg.accept(
                ModBlockEntities.COMPRESSOR_COMPACT.get(),
                context -> new CompressorCompactRenderer());
        reg.accept(ModBlockEntities.FLUID_TANK.get(), context -> new FluidTankRenderer());
        reg.accept(ModBlockEntities.FLUID_BARREL.get(), context -> new BarrelRenderer());
        reg.accept(ModBlockEntities.MASS_STORAGE.get(), MassStorageRenderer::new);
        reg.accept(ModBlockEntities.FILE_CABINET.get(), context -> new FileCabinetRenderer());
        reg.accept(ModBlockEntities.PIOE_ANCHOR.get(), context -> new PipeAnchorRenderer());
        reg.accept(ModBlockEntities.PYLON_REDWIRE.get(), context -> new PylonWiresRenderer());
        reg.accept(ModBlockEntities.PYLON_MEDIUM.get(), context -> new PylonWiresRenderer());
        reg.accept(ModBlockEntities.CONNECTOR_REDWIRE.get(), context -> new PylonWiresRenderer());
        reg.accept(
                ModBlockEntities.CONNECTOR_REDWIRE_SUPER.get(),
                context -> new PylonWiresRenderer());
        reg.accept(ModBlockEntities.PYLON_LARGE.get(), context -> new PylonWiresRenderer());
        reg.accept(ModBlockEntities.SUBSTATION.get(), context -> new PylonWiresRenderer());
        reg.accept(
                ModBlockEntities.SOLDERING_STATION.get(),
                context -> new SolderingStationRenderer(context));
        reg.accept(ModBlockEntities.ARC_WELDER.get(), context -> new ArcWelderRenderer(context));
        reg.accept(
                ModBlockEntities.TURRET_HOWARD_DAMAGED.get(),
                context -> new TurretHowardDamagedRenderer());
        reg.accept(
                ModBlockEntities.TURRET_CHEKHOV.get(),
                context -> new TurretChekhovRenderer<>(ResourceManager.turret_carriage_tex));
        reg.accept(
                ModBlockEntities.TURRET_FRIENDLY.get(),
                context ->
                        new TurretChekhovRenderer<>(ResourceManager.turret_carriage_friendly_tex));
        reg.accept(ModBlockEntities.TURRET_JEREMY.get(), context -> new TurretJeremyRenderer());
        reg.accept(ModBlockEntities.TURRET_TAUON.get(), context -> new TurretTauonRenderer());
        reg.accept(ModBlockEntities.TURRET_RICHARD.get(), context -> new TurretRichardRenderer());
        reg.accept(ModBlockEntities.TURRET_HOWARD.get(), context -> new TurretHowardRenderer());
        reg.accept(ModBlockEntities.TURRET_MAXWELL.get(), context -> new TurretMaxwellRenderer());
        reg.accept(ModBlockEntities.TURRET_FRITZ.get(), context -> new TurretFritzRenderer());
        reg.accept(ModBlockEntities.TURRET_ARTY.get(), context -> new TurretArtyRenderer());
        reg.accept(ModBlockEntities.TURRET_HIMARS.get(), context -> new TurretHIMARSRenderer());
        reg.accept(ModBlockEntities.TURRET_SENTRY.get(), context -> new TurretSentryRenderer());
        reg.accept(
                ModBlockEntities.TURRET_SENTRY_DAMAGED.get(),
                context -> new TurretSentryRenderer());
        reg.accept(ModBlockEntities.NTM_DOOR.get(), context -> new DoorGenericRenderer());
        reg.accept(ModBlockEntities.BLAST_DOOR.get(), context -> new BlastDoorRenderer());
        reg.accept(ModBlockEntities.EMITTER.get(), context -> new EmitterRenderer());
        reg.accept(ModBlockEntities.LAUNCH1.get(), context -> new LaunchPadRenderer());
        reg.accept(
                ModBlockEntities.LAUNCHPAD_RUSTED.get(), context -> new LaunchPadRustedRenderer());
        reg.accept(ModBlockEntities.LAUNCHPAD_LARGE.get(), context -> new LaunchPadLargeRenderer());
        reg.accept(ModBlockEntities.LARGE_LAUNCH_TABLE.get(), context -> new LaunchTableRenderer());
        reg.accept(ModBlockEntities.SMALL_LAUNCHER.get(), context -> new CompactLauncherRenderer());
        reg.accept(ModBlockEntities.MULTI_CORE.get(), context -> new StructurePreviewRenderer<>());
        reg.accept(ModBlockEntities.NUKE_FSTBMB.get(), context -> new NukeFstbmbRenderer());
        reg.accept(
                ModBlockEntities.SOYUZ_STRUCT.get(), context -> new StructurePreviewRenderer<>());
        reg.accept(ModBlockEntities.SOYUZ_LAUNCHER.get(), context -> new SoyuzLauncherRenderer());
        reg.accept(ModBlockEntities.LAUNCHPAD_SOYUZ.get(), context -> new LaunchpadSoyuzRenderer());
        reg.accept(
                ModBlockEntities.LAUNCHPAD_SOYUZ_ANCHOR.get(),
                context -> new LaunchpadSoyuzRenderer());
        reg.accept(
                ModBlockEntities.MISSILE_ASSEMBLY.get(), context -> new MissileAssemblyRenderer());
        reg.accept(ModBlockEntities.RBMK_ROD.get(), context -> new RBMKFuelRodRenderer());
        reg.accept(ModBlockEntities.RBMK_ROD_REASIM.get(), context -> new RBMKFuelRodRenderer());
        reg.accept(ModBlockEntities.RBMK_CONTROL.get(), context -> new RBMKControlRodRenderer());
        reg.accept(
                ModBlockEntities.RBMK_CONTROL_AUTO.get(), context -> new RBMKControlRodRenderer());
        reg.accept(
                ModBlockEntities.RBMK_CONSOLE.get(), context -> new RBMKConsoleRenderer(context));
        reg.accept(ModBlockEntities.RBMK_GAUGE.get(), context -> new RBMKGaugeRenderer(context));
        reg.accept(ModBlockEntities.RBMK_DISPLAY.get(), context -> new RBMKDisplayRenderer());
        reg.accept(
                ModBlockEntities.RBMK_NUMITRON.get(), context -> new RBMKNumitronRenderer(context));
        reg.accept(
                ModBlockEntities.RBMK_INDICATOR.get(),
                context -> new RBMKIndicatorRenderer(context));
        reg.accept(ModBlockEntities.RBMK_GRAPH.get(), context -> new RBMKGraphRenderer(context));
        reg.accept(ModBlockEntities.RBMK_LEVER.get(), context -> new RBMKLeverRenderer(context));
        reg.accept(ModBlockEntities.RBMK_KEYPAD.get(), context -> new RBMKKeyPadRenderer(context));
        reg.accept(
                ModBlockEntities.RBMK_TERMINAL.get(), context -> new RBMKTerminalRenderer(context));
        reg.accept(ModBlockEntities.RBMK_AUTOLOADER.get(), context -> new RBMKAutoloaderRenderer());
        reg.accept(ModBlockEntities.ICF_STRUCT.get(), context -> new StructurePreviewRenderer<>());
        reg.accept(ModBlockEntities.WATZ_STRUCT.get(), context -> new StructurePreviewRenderer<>());
        reg.accept(
                ModBlockEntities.FUSION_TORUS_STRUCT.get(),
                context -> new StructurePreviewRenderer<>());
        reg.accept(ModBlockEntities.PILE_LOADER.get(), context -> new PileLoaderRenderer());
        reg.accept(ModBlockEntities.PILE_VENT.get(), context -> new PileVentRenderer());
        reg.accept(ModBlockEntities.PILE_CONTROL.get(), context -> new PileControlRenderer());
        reg.accept(ModBlockEntities.FAN.get(), context -> new FanRenderer());
        reg.accept(
                ModBlockEntities.PISTON_INSERTER.get(),
                context -> new PistonInserterRenderer(context));
        reg.accept(
                ModBlockEntities.RBMK_CRANE_CONSOLE.get(),
                context -> new RBMKCraneConsoleRenderer());
        reg.accept(ModBlockEntities.CONVEYOR_PRESS.get(), ConveyorPressRenderer::new);
        reg.accept(ModBlockEntities.CRASHED_BALEFIRE.get(), context -> new CrashedBombRenderer());
        reg.accept(
                ModBlockEntities.PRESS.get(),
                context ->
                        new PressRenderer<BurnerPressBlockEntity>(
                                context,
                                ResourceManager.press_head,
                                ResourceManager.press_head_tex,
                                0F,
                                false,
                                0F,
                                0.99F,
                                BurnerPressBlockEntity.MAX_PROGRESS,
                                (be, f) -> be.lastPress + (be.renderPress - be.lastPress) * f,
                                be -> be.getItem(BurnerPressBlockEntity.SLOT_INPUT),
                                PressRenderer.PRESS_ITEM));
        reg.accept(
                ModBlockEntities.ELECTRIC_PRESS.get(),
                context ->
                        new PressRenderer<ElectricPressBlockEntity>(
                                context,
                                ResourceManager.epress_head,
                                ResourceManager.epress_head_tex,
                                90F,
                                true,
                                1F,
                                1.0F,
                                ElectricPressBlockEntity.MAX_PROGRESS,
                                (be, f) -> be.lastPress + (be.renderPress - be.lastPress) * f,
                                be -> be.getItem(ElectricPressBlockEntity.SLOT_INPUT),
                                PressRenderer.EPRESS_ITEM));
        reg.accept(ModBlockEntities.FURNACE_IRON.get(), context -> new FurnaceIronRenderer());
        reg.accept(ModBlockEntities.FURNACE_STEEL.get(), context -> new FurnaceSteelRenderer());
        reg.accept(
                ModBlockEntities.COMBINATION_OVEN.get(),
                context -> new FurnaceCombinationRenderer());
        reg.accept(ModBlockEntities.HEATING_OVEN.get(), context -> new HeaterOvenRenderer());
        reg.accept(ModBlockEntities.ASHPIT.get(), context -> new AshpitRenderer());
        reg.accept(ModBlockEntities.ACIDOMATIC.get(), context -> new CrystallizerRenderer());
        reg.accept(ModBlockEntities.LIQUEFACTOR.get(), context -> new LiquefactorRenderer());
        reg.accept(ModBlockEntities.SOLIDIFIER.get(), context -> new SolidifierRenderer());
        reg.accept(ModBlockEntities.PYROOVEN.get(), context -> new PyroOvenRenderer());
        reg.accept(ModBlockEntities.ELECTRIC_PUMP.get(), context -> new PumpRenderer());
        reg.accept(ModBlockEntities.INTAKE.get(), context -> new IntakeRenderer());
        reg.accept(
                ModBlockEntities.CONDENSER_POWERED.get(),
                context -> new CondenserPoweredRenderer());
        reg.accept(ModBlockEntities.BIGASSTANK.get(), context -> new BigAssTankRenderer());
        reg.accept(ModBlockEntities.ORBUS.get(), context -> new OrbusRenderer());
        reg.accept(ModBlockEntities.MICROWAVE.get(), context -> new MicrowaveRenderer());
        reg.accept(ModBlockEntities.THRESHER.get(), context -> new ThresherRenderer());
        reg.accept(ModBlockEntities.AUTOSAW.get(), context -> new AutosawRenderer());
        reg.accept(ModBlockEntities.SAWMILL.get(), context -> new SawmillRenderer());
        reg.accept(ModBlockEntities.STIRLING.get(), context -> new StirlingRenderer());
        reg.accept(ModBlockEntities.AMMO_PRESS.get(), context -> new AmmoPressRenderer());
        reg.accept(
                ModBlockEntities.NTM_SKELETON.get(),
                context -> new SkeletonHolderRenderer(context));
        reg.accept(ModBlockEntities.NTM_PEDESTAL.get(), PedestalRenderer::new);
        reg.accept(ModBlockEntities.FLOODLIGHT.get(), context -> new FloodlightRenderer());
        reg.accept(ModBlockEntities.DEMON_LAMP.get(), context -> new DemonLampRenderer());
        reg.accept(ModBlockEntities.NTM_BOBBLEHEAD.get(), context -> new BobbleRenderer(context));
        reg.accept(ModBlockEntities.NTM_SNOWGLOBE.get(), context -> new SnowglobeRenderer(context));
        reg.accept(ModBlockEntities.NTM_PLUSHIE.get(), context -> new PlushieRenderer(context));
    }

    private static boolean hudVisible() {
        Gui gui = Minecraft.getInstance().gui;
        return !gui.hud.isHidden() || gui.screen() != null;
    }

    public static void renderLookOverlay(GuiGraphicsExtractor graphics) {
        if (!hudVisible()) return;
        LookOverlayRenderer.render(graphics);
    }

    public static void renderArmorOverlay(GuiGraphicsExtractor graphics) {
        if (!hudVisible()) return;
        ArmorOverlayRenderer.render(graphics);
    }

    public static void renderRadiationHud(GuiGraphicsExtractor graphics) {
        if (!hudVisible()) return;
        NukeHud.shaken(graphics, () -> RadiationHudRenderer.render(graphics));
    }

    public static void renderModeBadges(GuiGraphicsExtractor graphics) {
        if (!hudVisible() || !HudConfig.modeBadges) return;
        ScreenOverlayRenderer.renderBadges(graphics);
    }

    public static void renderRadVisHud(GuiGraphicsExtractor graphics) {
        if (!hudVisible()) return;
        RadVisOverlay.renderHud(graphics);
    }

    public static void renderNukeFlash(GuiGraphicsExtractor graphics) {
        if (!hudVisible()) return;
        NukeHud.renderFlash(graphics);
    }

    public static void renderInfoHud(GuiGraphicsExtractor graphics) {
        if (!hudVisible()) return;
        InfoSystem.render(graphics);
    }

    public static void renderGunHud(GuiGraphicsExtractor graphics) {
        if (Minecraft.getInstance().gui.hud.isHidden()) return;
        NukeHud.shaken(graphics, () -> renderGunHudComponents(graphics));
    }

    public static void renderGunScope(GuiGraphicsExtractor graphics) {
        if (!hudVisible() || !GunClientState.isSteadyAim()) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof GunBaseNTItem gun))
            return;
        Identifier scope =
                gun.getConfig(player.getMainHandItem(), 0)
                        .getScopeTexture(player.getMainHandItem());
        if (scope != null)
            NukeHud.shaken(graphics, () -> ScreenOverlayRenderer.renderScope(graphics, scope));
    }

    private static void renderGunHudComponents(GuiGraphicsExtractor graphics) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunBaseNTItem gun)) return;

        for (int i = 0; i < gun.getConfigCount(); i++) {
            var components = GunClientProperties.getHudWidgets(gun, i);
            int bottomOffset = 0;
            if (components != null)
                for (var component : components) {
                    component.draw(graphics, player, stack, bottomOffset, i);
                    bottomOffset += component.height();
                }
        }
    }

    public static boolean replacesVanillaVitals() {
        return hudVisible() && ArmorHevHudRenderer.active();
    }

    public static void renderDashBar(GuiGraphicsExtractor graphics) {
        if (!hudVisible()) return;
        NukeHud.shaken(graphics, () -> DashBarRenderer.render(graphics));
    }

    public static void renderHevHud(GuiGraphicsExtractor graphics) {
        if (!replacesVanillaVitals()) return;
        ArmorHevHudRenderer.render(graphics);
    }

    public static boolean shieldShown() {
        return hudVisible() && ShieldHudRenderer.shown();
    }

    public static boolean renderShieldBar(GuiGraphicsExtractor graphics, int top) {
        if (!shieldShown()) return false;
        ShieldHudRenderer.render(graphics, top);
        return true;
    }

    public static boolean armorBarsShown() {
        Minecraft minecraft = Minecraft.getInstance();
        return !minecraft.gui.hud.isHidden()
                && minecraft.gameMode.canHurtPlayer()
                && !replacesVanillaVitals();
    }

    public static boolean replacesCrosshair() {
        if (Minecraft.getInstance().gui.hud.isHidden()) return false;
        var player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem() instanceof GunBaseNTItem;
    }

    public static void renderGunCrosshair(GuiGraphicsExtractor graphics) {
        var player = Minecraft.getInstance().player;
        var stack = player.getMainHandItem();
        var gun = (GunBaseNTItem) stack.getItem();
        var config = gun.getConfig(stack, 0);
        if (gun instanceof GunStingerItem) {
            if (GunClientState.getAim() < 1F) return;
            ScreenOverlayRenderer.renderCustomCrosshairs(graphics, config.getCrosshair(stack));
            ScreenOverlayRenderer.renderStingerLockon(graphics);
            return;
        }
        if (config.getHideCrosshair(stack) && GunClientState.getAim() >= 1F) return;
        ScreenOverlayRenderer.renderCustomCrosshairs(graphics, config.getCrosshair(stack));
    }

    public static void setupGunClient() {
        GunFactoryClient.init();
    }

    public static void renderToolAbilityHud(GuiGraphicsExtractor graphics) {
        if (Minecraft.getInstance().gui.hud.isHidden()) return;
        ToolAbilityHudRenderer.render(graphics);
    }

    @FunctionalInterface
    public interface MenuScreenRegistrar {
        <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void accept(
                MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> constructor);
    }

    @FunctionalInterface
    public interface BlockEntityRendererRegistrar {
        <T extends BlockEntity, S extends BlockEntityRenderState> void accept(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider);
    }

    @FunctionalInterface
    public interface EntityRendererRegistrar {
        <T extends Entity> void accept(
                EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    @FunctionalInterface
    public interface BlockTintRegistrar {
        void accept(List<BlockTintSource> sources, Block... blocks);
    }

    @FunctionalInterface
    public interface IdRegistrar<T> {
        void register(Identifier id, T value);

        default void register(String name, T value) {
            register(RegistryUtil.id(name), value);
        }
    }
}
