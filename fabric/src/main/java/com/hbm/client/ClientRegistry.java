// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.gas.BlockGasBase;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockSnowglobe;
import com.hbm.blocks.generic.GuideBlock;
import com.hbm.blocks.machine.RadioRec;
import com.hbm.blocks.machine.rbmk.RBMKGauge;
import com.hbm.blocks.machine.rbmk.RBMKGraph;
import com.hbm.blocks.machine.rbmk.RBMKIndicator;
import com.hbm.blocks.machine.rbmk.RBMKKeyPad;
import com.hbm.blocks.machine.rbmk.RBMKLever;
import com.hbm.blocks.machine.rbmk.RBMKNumitron;
import com.hbm.blocks.machine.rbmk.RBMKTerminal;
import com.hbm.blocks.network.CableDiodeBlock;
import com.hbm.blocks.network.CraneRouter;
import com.hbm.blocks.network.FluidPumpBlock;
import com.hbm.blocks.network.RadioAUTOCAL;
import com.hbm.blocks.network.RadioTelex;
import com.hbm.blocks.network.RadioTorchBlock;
import com.hbm.client.gui.*;
import com.hbm.client.gui.ScreenBookLore;
import com.hbm.client.model.PaddedSpriteSource;
import com.hbm.client.particle.*;
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
import com.hbm.entity.ModEntities;
import com.hbm.entity.particle.EntityChlorineFX;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.BobmazonOffer;
import com.hbm.handler.BobmazonOffers;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.handler.radiation.RadVisOverlay;
import com.hbm.inventory.container.ModMenus;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.special.ItemBookLore;
import com.hbm.items.special.ItemClayTablet;
import com.hbm.items.special.ItemHolotapeImage;
import com.hbm.items.tool.ItemBoltgun;
import com.hbm.items.tool.ItemCatalog;
import com.hbm.items.tool.ItemDesignatorManual;
import com.hbm.items.tool.ItemGuideBook;
import com.hbm.items.tool.ItemRTTYPager;
import com.hbm.items.tool.ItemSatelliteInterface;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.GunFactoryClient;
import com.hbm.items.weapon.sedna.impl.ItemGunStinger;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.particle.AshRevealParticleOptions;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.particle.FlameParticleOptions;
import com.hbm.particle.ParticleAshReveal;
import com.hbm.particle.SplashParticleOptions;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.util.RenderScreenOverlay;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.*;
import com.hbm.tileentity.machine.fusion.*;
import com.hbm.tileentity.machine.oil.BlockEntityMachinePyroOven;
import com.hbm.tileentity.machine.oil.BlockEntityMachineRefinery;
import com.hbm.tileentity.machine.oil.BlockEntityMachineVacuumDistill;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import com.hbm.tileentity.machine.storage.BlockEntityBatteryREDD;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.network.BlockEntityRadioTorch;
import com.hbm.tileentity.network.BlockEntityRadioTorchCounter;
import com.hbm.wiaj.cannery.Jars;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
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
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.LoggerFactory;

public final class ClientRegistry {
    public static final Identifier LOOK_OVERLAY_ID = Library.id("look_overlay");
    public static final Identifier RADIATION_HUD_ID = Library.id("radiation_hud");
    public static final Identifier RADVIS_HUD_ID = Library.id("radvis_hud");
    public static final Identifier ARMOR_OVERLAY_ID = Library.id("armor_overlay");
    public static final Identifier INFO_HUD_ID = Library.id("info_hud");
    public static final Identifier GUN_HUD_ID = Library.id("gun_hud");
    public static final Identifier GUN_SCOPE_ID = Library.id("gun_scope");
    public static final Identifier GUN_CLIENT_SETUP_ID = Library.id("gun_client_setup");
    public static final Identifier HEV_HUD_ID = Library.id("hev_hud");
    public static final Identifier DASH_BAR_ID = Library.id("dash_bar");
    public static final Identifier SHIELD_BAR_ID = Library.id("shield_bar");
    public static final Identifier TOOL_ABILITY_HUD_ID = Library.id("tool_ability_hud");
    public static final Identifier NUKE_FLASH_ID = Library.id("nuke_flash");

    public static final Identifier VISUAL_RESOURCES_ID =
            Identifier.fromNamespaceAndPath("hbm", "visual_resources");

    private ClientRegistry() {}

    public static void bindClientHooks() {

        BlockGasBase.REVEALED_TO_VIEWER =
                () -> {
                    LocalPlayer viewer = Minecraft.getInstance().player;
                    return viewer != null
                            && viewer.getItemBySlot(EquipmentSlot.HEAD)
                                    .is(ModItems.ASHGLASSES.get());
                };
        ClientGameTime.install();
        EntityEffectHandler.CLIENT_RADIATION_FX = ClientEffects::radiationAura;
        EntityEffectHandler.CLIENT_CRATER_AURA = ClientEffects::craterAura;
        AudioSystem.installClientFactory(ClientAudioFactory.INSTANCE);
        BlockEntityRefueler.CLIENT_PARTICLE = RenderRefueler::spawnFluidFill;
        BlockEntityMachineCentrifuge.CLIENT_SOUND = CentrifugeSound::tick;
        BlockEntitySoyuzLauncher.CLIENT_SOUND = SoyuzLauncherSound::tick;
        BlockEntityMachineGasCent.CLIENT_SOUND = GasCentSound::tick;
        BlockEntityMachineArcFurnace.CLIENT_SOUND = ArcFurnaceSound::tick;
        BlockEntityMachineAssemblyMachine.CLIENT_SOUND = AssemblerSound::tick;
        AssemblerSound.installOneShots();
        BlockEntityMachinePrecAss.CLIENT_SOUND = PrecAssSound::tick;
        PrecAssSound.installOneShots();
        BlockEntityMachineAssemblyFactory.CLIENT_SOUND = AssemblyFactorySound::tick;
        AssemblyFactorySound.installOneShots();
        BlockEntityMachineChemicalPlant.CLIENT_SOUND = ChemPlantSound::tick;
        BlockEntityMachineRockMill.CLIENT_DUST = RockMillEffects::tick;
        BlockEntityMachinePUREX.CLIENT_SOUND = PurexSound::tick;
        BlockEntityMachineChemicalFactory.CLIENT_SOUND = ChemFactorySound::tick;
        BlockEntityMachineRefinery.CLIENT_SOUND = RefinerySound::tick;
        BlockEntityMachinePWRController.CLIENT_SOUND = PWRControllerSound::tick;
        BlockEntityHeaterElectric.CLIENT_SOUND = ElectricHeaterSound::tick;
        BlockEntityMachineDiesel.CLIENT_SOUND = DieselSound::tick;
        BlockEntityMachineLargeTurbine.CLIENT_SOUND = LargeTurbineSound::tick;
        BlockEntityMachineIndustrialTurbine.CLIENT_SOUND = IndustrialTurbineSound::tick;
        BlockEntityChungus.CLIENT_SOUND = ChungusSound::tick;
        BlockEntityBatteryREDD.CLIENT_SOUND = FensuSound::tick;
        BlockEntityHeatBoilerBase.CLIENT_SOUND = BoilerSound::tick;
        BlockEntityMachineCombustionEngine.CLIENT_SOUND = CombustionEngineSound::tick;
        BlockEntityMachineTurbineGas.CLIENT_SOUND = TurbineGasSound::tick;
        BlockEntityMachineTurbofan.CLIENT_SOUND = TurbofanSound::tick;

        BlockEntityMachinePumpBase.CLIENT_SOUND = PumpSound::tick;
        BlockEntityMachineBlastFurnace.CLIENT_TOWER = BlastFurnaceTower::tick;
        BlockEntityChimneyBase.CLIENT_TOWER = ChimneyTower::tick;
        BlockEntityRBMKAutoloader.CLIENT_SOUND = AutoloaderSound::tick;
        BlockEntityRBMKAutoloader.CLIENT_TOWER = AutoloaderSound::plume;
        BlockEntityMachineCrystallizer.CLIENT_SOUND = CrystallizerSound::tick;
        BlockEntityMachinePyroOven.CLIENT_SOUND = PyroOvenSound::tick;
        BlockEntityMachineVacuumDistill.CLIENT_SOUND = VacuumDistillSound::tick;
        BlockEntityThresher.CLIENT_SOUND = ThresherSound::tick;
        BlockEntityMachineAutosaw.CLIENT_SOUND = AutosawSound::tick;
        BlockEntityFusionTorus.CLIENT_SOUND = FusionSound::tick;
        BlockEntityFusionKlystron.CLIENT_SOUND = FusionSound::tick;
        BlockEntityFusionKlystronCreative.CLIENT_SOUND = FusionSound::tick;
        BlockEntityFusionMHDT.CLIENT_SOUND = FusionSound::tick;
        BlockEntityFusionPlasmaForge.PLAY_STRIKER =
                be -> {
                    if (be.isMuffled()) return;

                    BlockPos pos = be.getBlockPos();
                    Minecraft.getInstance()
                            .getSoundManager()
                            .play(
                                    new SimpleSoundInstance(
                                            ModSounds.BOLTGUN.get(),
                                            SoundSource.BLOCKS,
                                            be.getVolume(0.25F),
                                            1.25F,
                                            SoundInstance.createUnseededRandom(),
                                            pos.getX(),
                                            pos.getY(),
                                            pos.getZ()));
                };

        FluidIdentifierItem.OPEN_SELECTOR =
                player -> Minecraft.getInstance().gui.setScreen(new ScreenFluidIdentifier(player));
        ItemCrucible.PLAY_SWING =
                pitch ->
                        Minecraft.getInstance()
                                .getSoundManager()
                                .play(
                                        SimpleSoundInstance.forUI(
                                                ModSounds.CRUCIBLE_SWING.get(), pitch));
        ItemClayTablet.OPEN_SCREEN =
                player -> Minecraft.getInstance().gui.setScreen(new ScreenClayTablet(player));
        ItemGuideBook.OPEN_SCREEN =
                type -> Minecraft.getInstance().gui.setScreen(new ScreenGuide(type));
        GuideBlock.OPEN_WIKI =
                () -> Util.getPlatform().openUri("https://nucleartech.wiki/wiki/Main_Page");
        ItemBookLore.OPEN_SCREEN =
                player -> Minecraft.getInstance().gui.setScreen(new ScreenBookLore(player));
        ItemSatelliteInterface.OPEN_SCREEN =
                player -> Minecraft.getInstance().gui.setScreen(new ScreenSatCoord(player));
        ItemCatalog.OPEN_SCREEN =
                player -> {
                    List<BobmazonOffer> offers = BobmazonOffers.forStack(player.getMainHandItem());
                    if (offers != null)
                        Minecraft.getInstance().gui.setScreen(new GUIScreenBobmazon(offers));
                };
        ItemToolAbility.OPEN_SCREEN =
                stack -> {
                    if (stack.getItem() instanceof ItemToolAbility tool) {
                        Minecraft.getInstance().gui.setScreen(new ScreenToolAbility(stack, tool));
                    }
                };
        ItemDesignatorManual.OPEN_SCREEN =
                hand -> Minecraft.getInstance().gui.setScreen(new ScreenManualDesignator(hand));
        ItemRTTYPager.OPEN_SCREEN =
                player ->
                        Minecraft.getInstance()
                                .gui
                                .setScreen(new ScreenPager(player.getMainHandItem()));
        ItemHolotapeImage.OPEN_SCREEN =
                hand -> Minecraft.getInstance().gui.setScreen(new ScreenHolotape(hand));
        FluidPumpBlock.OPEN_GUI =
                pump -> Minecraft.getInstance().gui.setScreen(new ScreenFluidPump(pump));
        CableDiodeBlock.OPEN_SCREEN =
                diode -> Minecraft.getInstance().gui.setScreen(new ScreenCableDiode(diode));
        RadioRec.OPEN_GUI =
                radio -> Minecraft.getInstance().gui.setScreen(new ScreenRadioRec(radio));
        RadioTorchBlock.OPEN_SCREEN =
                radio -> {
                    if (radio instanceof BlockEntityRadioTorch rtty
                            && !(rtty instanceof BlockEntityRadioTorchCounter)) {
                        Minecraft.getInstance().gui.setScreen(new ScreenRadioTorch(rtty));
                    }
                };
        RadioTelex.OPEN_GUI =
                telex -> Minecraft.getInstance().gui.setScreen(new ScreenRadioTelex(telex));
        RadioAUTOCAL.OPEN_GUI =
                autocal -> Minecraft.getInstance().gui.setScreen(new ScreenRadioAUTOCAL(autocal));
        BlockBobble.OPEN_GUI =
                bobble -> Minecraft.getInstance().gui.setScreen(new ScreenBobble(bobble));
        BlockSnowglobe.OPEN_GUI =
                globe -> Minecraft.getInstance().gui.setScreen(new ScreenSnowglobe(globe));
        RBMKGauge.OPEN_SCREEN =
                gauge -> Minecraft.getInstance().gui.setScreen(new ScreenRBMKGauge(gauge));
        RBMKNumitron.OPEN_SCREEN =
                numitron -> Minecraft.getInstance().gui.setScreen(new ScreenRBMKNumitron(numitron));
        RBMKIndicator.OPEN_SCREEN =
                indicator ->
                        Minecraft.getInstance().gui.setScreen(new ScreenRBMKIndicator(indicator));
        RBMKGraph.OPEN_SCREEN =
                graph -> Minecraft.getInstance().gui.setScreen(new ScreenRBMKGraph(graph));
        RBMKLever.OPEN_SCREEN =
                lever -> Minecraft.getInstance().gui.setScreen(new ScreenRBMKLever(lever));
        RBMKKeyPad.OPEN_SCREEN =
                keypad -> Minecraft.getInstance().gui.setScreen(new ScreenRBMKKeyPad(keypad));
        RBMKTerminal.OPEN_SCREEN =
                terminal -> Minecraft.getInstance().gui.setScreen(new ScreenRBMKTerminal(terminal));
    }

    public static void clientTick() {
        ClientSyncRecovery.tick();
        SettingsToolInfo.clientTick();
        InfoSystem.clientTick();
        FluidPipeTintData.flushRequests();
        CraneKeybindHandler.poll();
        ToolKeybindHandler.poll();
        GunKeybindHandler.poll();
        PlayerKeybindHandler.poll();
        CalculatorKeybindHandler.poll();
        AbilityKeybindHandler.poll();
        ArmorMovementHandler.clientTick();
        if (Minecraft.getInstance().player != null)
            ArmorModHandler.clientTick(Minecraft.getInstance().player);
        No9Flashlight.clientTick();
        NTMSkybox.clientTick();
        SootFog.clientTick();
        WiringReadout.clientTick();
        RebarPlacerPreview.clientTick();
        DroneLinkerReadout.clientTick();
        HbmAnimations.expireFinished();
        ItemRenderWeaponBase.expireFlashes();
        ItemGunBaseNT.clientHeldTick();
        ItemGrenadeUniversal.clientHeldTick();
        ItemBoltgun.clientHeldTick();
        ItemGunBaseNT.clientRecoilTick();
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
        return new ParticleRadiationFog.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> rbmkMushProvider(SpriteSet sprites) {
        return new ParticleRBMKMush.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> digammaSmokeProvider(SpriteSet sprites) {
        return new ParticleDigammaSmoke.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> launchSmokeProvider(SpriteSet sprites) {
        return new ParticleLaunchSmoke.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> volcanoSmokeProvider(SpriteSet sprites) {
        return new ParticleVolcanoSmoke.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> schrabFogProvider(SpriteSet sprites) {
        return new ParticleSchrabFog.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> gasFlameProvider(SpriteSet sprites) {
        return new ParticleGasFlame.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> hadronProvider(SpriteSet sprites) {
        return new ParticleHadron.Provider(sprites);
    }

    public static ParticleProvider<SimpleParticleType> blackPowderSparkProvider(SpriteSet sprites) {
        return new ParticleBlackPowderSpark.Provider(sprites);
    }

    public static ParticleProvider<CoolingTowerParticleOptions> mistTowerProvider(
            SpriteSet sprites) {
        return new ParticleCoolingTower.Provider(sprites);
    }

    public static ParticleProvider<FlameParticleOptions> flameProvider(SpriteSet sprites) {
        return new ParticleFlamethrower.Provider(sprites);
    }

    public static ParticleProvider<AshRevealParticleOptions> ashRevealProvider(SpriteSet sprites) {
        return new ParticleAshReveal.Provider(sprites);
    }

    public static ParticleProvider<SplashParticleOptions> splashProvider(SpriteSet sprites) {
        return new ParticleSplash.Provider(sprites);
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
                Arrays.stream(CraneRouter.FACE_COLOURS)
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
        reg.accept(ModEntities.FALLOUT_RAIN.get(), RenderFallout::new);
        reg.accept(ModEntities.FALLING_BLOCK_NT.get(), RenderFallingBlockNT::new);
        reg.accept(ModEntities.FALLING_MULTIBLOCK.get(), RenderFallingMultiblock::new);
        reg.accept(ModEntities.NUKE_TOREX.get(), RenderTorex::new);
        reg.accept(
                ModEntities.VORTEX.get(),
                ctx -> new RenderBlackHole<>(ctx, RenderBlackHole.Kind.VORTEX));
        reg.accept(
                ModEntities.RAGING_VORTEX.get(),
                ctx -> new RenderBlackHole<>(ctx, RenderBlackHole.Kind.RAGING));
        reg.accept(
                ModEntities.BLACK_HOLE.get(),
                ctx -> new RenderBlackHole<>(ctx, RenderBlackHole.Kind.HOLE));
        reg.accept(
                ModEntities.DIGAMMA_QUASAR.get(),
                ctx -> new RenderBlackHole<>(ctx, RenderBlackHole.Kind.QUASAR));
        reg.accept(ModEntities.UNDEAD_SOLDIER.get(), RenderUndeadSoldier::new);
        reg.accept(ModEntities.GLYPHID.get(), ctx -> new RenderGlyphid<>(ctx));
        reg.accept(ModEntities.GLYPHID_BRAWLER.get(), ctx -> new RenderGlyphid<>(ctx));
        reg.accept(ModEntities.GLYPHID_BEHEMOTH.get(), ctx -> new RenderGlyphid<>(ctx));
        reg.accept(ModEntities.GLYPHID_BRENDA.get(), ctx -> new RenderGlyphid<>(ctx));
        reg.accept(ModEntities.GLYPHID_BOMBARDIER.get(), ctx -> new RenderGlyphid<>(ctx));
        reg.accept(ModEntities.GLYPHID_BLASTER.get(), ctx -> new RenderGlyphid<>(ctx));
        reg.accept(ModEntities.GLYPHID_SCOUT.get(), ctx -> new RenderGlyphid<>(ctx));
        reg.accept(ModEntities.GLYPHID_DIGGER.get(), ctx -> new RenderGlyphid<>(ctx));
        reg.accept(ModEntities.GLYPHID_NUCLEAR.get(), RenderGlyphidNuclear::new);
        reg.accept(ModEntities.GLYPHID_WAYPOINT.get(), NoopRenderer::new);
        reg.accept(ModEntities.ACID_BOMB.get(), RenderAcidBomb::new);
        reg.accept(ModEntities.PARASITE_MAGGOT.get(), RenderParasiteMaggot::new);
        reg.accept(ModEntities.C130.get(), RenderC130::new);
        reg.accept(ModEntities.BOMBER.get(), RenderBomber::new);
        reg.accept(ModEntities.GRENADE_UNIVERSAL.get(), RenderGrenadeUniversal::new);

        reg.accept(
                ModEntities.GRENADE_BOUNCY_GENERIC.get(), ctx -> new RenderGenericGrenade<>(ctx));
        reg.accept(ModEntities.DISPERSER_CANISTER.get(), ctx -> new RenderGenericGrenade<>(ctx));
        reg.accept(ModEntities.WASTE_PEARL.get(), NoopRenderer::new);
        reg.accept(ModEntities.ITEM_BUOYANT.get(), ItemEntityRenderer::new);
        reg.accept(ModEntities.MOVING_ITEM.get(), RenderMovingItem::new);
        reg.accept(ModEntities.MOVING_PACKAGE.get(), RenderMovingPackage::new);

        reg.accept(ModEntities.TNT_NTM.get(), TntRenderer::new);

        reg.accept(ModEntities.CLOUD_FLEIJA.get(), RenderCloudFleija::new);
        reg.accept(ModEntities.CLOUD_SOLINIUM.get(), RenderCloudSolinium::new);
        reg.accept(ModEntities.CLOUD_RAINBOW.get(), RenderCloudRainbow::new);
        reg.accept(ModEntities.MOONSTONE_BLAST.get(), RenderCloudTom::new);
        reg.accept(
                ModEntities.AGENT_ORANGE.get(),
                ctx ->
                        new RenderMultiCloud<EntityOrangeFX>(
                                ctx,
                                "orange",
                                EntityOrangeFX::particleAge,
                                EntityOrangeFX::maxAge));
        reg.accept(
                ModEntities.CHLORINE_FX.get(),
                ctx ->
                        new RenderMultiCloud<EntityChlorineFX>(
                                ctx,
                                "chlorine",
                                EntityChlorineFX::particleAge,
                                EntityChlorineFX::maxAge));
        reg.accept(
                ModEntities.CLOUD_FX.get(),
                ctx ->
                        new RenderMultiCloud<EntityCloudFX>(
                                ctx, "cloud", EntityCloudFX::particleAge, EntityCloudFX::maxAge));
        reg.accept(
                ModEntities.PINK_CLOUD_FX.get(),
                ctx ->
                        new RenderMultiCloud<EntityPinkCloudFX>(
                                ctx,
                                "pc",
                                EntityPinkCloudFX::particleAge,
                                EntityPinkCloudFX::maxAge));
        reg.accept(ModEntities.METEOR.get(), RenderMeteor::new);
        reg.accept(ModEntities.BOMBLET_ZETA.get(), RenderBombletZeta::new);
        reg.accept(ModEntities.PARACHUTE_CRATE.get(), RenderParachuteCrate::new);
        reg.accept(ModEntities.CHEMICAL.get(), RenderChemical::new);
        reg.accept(
                ModEntities.BOXCAR.get(), ctx -> new RenderBoxcar<>(ctx, RenderBoxcar.Kind.BOXCAR));
        reg.accept(
                ModEntities.TORPEDO.get(),
                ctx -> new RenderBoxcar<>(ctx, RenderBoxcar.Kind.TORPEDO));
        reg.accept(
                ModEntities.DUCHESS_GAMBIT.get(),
                ctx -> new RenderBoxcar<>(ctx, RenderBoxcar.Kind.DUCHESS_GAMBIT));
        reg.accept(
                ModEntities.FALLING_BUILDING.get(),
                ctx -> new RenderBoxcar<>(ctx, RenderBoxcar.Kind.BUILDING));

        reg.accept(
                ModEntities.MISSILE_GENERIC.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_HE_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_INCENDIARY.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_IN_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_DECOY.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_decoy_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_BUSTER.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_BU_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_STRONG.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_HE_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_INCENDIARY_STRONG.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_IN_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_BUSTER_STRONG.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_BU_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_BURST.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileHuge,
                                ResourceManager.missileHuge_HE_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_INFERNO.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileHuge,
                                ResourceManager.missileHuge_IN_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_NUCLEAR.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileNuclear_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_NUCLEAR_CLUSTER.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileMIRV_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_STEALTH.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileStealth,
                                ResourceManager.missileStealth_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_DOOMSDAY.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileDoomsday_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_DOOMSDAY_RUSTED.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileDoomsdayRusted_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_MICRO.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicro_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_TAINT.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroTaint_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_BHOLE.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroBHole_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_SCHRABIDIUM.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroSchrab_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_EMP.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroEMP_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_TEST.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileMicro,
                                ResourceManager.missileMicroTest_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_CLUSTER.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileV2,
                                ResourceManager.missileV2_CL_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_CLUSTER_STRONG.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_CL_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_EMP_STRONG.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileStrong,
                                ResourceManager.missileStrong_EMP_tex,
                                1.5F));
        reg.accept(
                ModEntities.MISSILE_RAIN.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileHuge,
                                ResourceManager.missileHuge_CL_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_DRILL.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileHuge,
                                ResourceManager.missileHuge_BU_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_VOLCANO.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileNuclear,
                                ResourceManager.missileVolcano_tex,
                                1F));
        reg.accept(
                ModEntities.MISSILE_SHUTTLE.get(),
                ctx ->
                        new RenderMissile(
                                ctx,
                                ResourceManager.missileShuttle,
                                ResourceManager.missileShuttle_tex,
                                1F));
        reg.accept(ModEntities.MISSILE_CUSTOM.get(), RenderMissileCustom::new);
        reg.accept(ModEntities.MISSILE_ANTI.get(), RenderMissileAntiBallistic::new);

        reg.accept(ModEntities.RUBBLE.get(), RenderRubble::new);
        reg.accept(ModEntities.RBMK_DEBRIS.get(), RenderRBMKDebris::new);
        reg.accept(ModEntities.ZIRNOX_DEBRIS.get(), RenderZirnoxDebris::new);
        reg.accept(ModEntities.SPEAR.get(), RenderSpear::new);
        reg.accept(ModEntities.MIST.get(), NoopRenderer::new);
        reg.accept(ModEntities.WASTE_ITEM.get(), ItemEntityRenderer::new);
        reg.accept(ModEntities.SHRAPNEL.get(), ctx -> new RenderShrapnel<>(ctx));
        reg.accept(ModEntities.BULLET_MK4.get(), RenderBulletMK4::new);
        reg.accept(ModEntities.BULLET_BEAM.get(), RenderBeam::new);
        reg.accept(ModEntities.BULLET.get(), RenderBullet::new);
        reg.accept(ModEntities.COIN.get(), RenderCoin::new);
        reg.accept(ModEntities.FIRE_LINGERING.get(), NoopRenderer::new);

        reg.accept(ModEntities.ARTILLERY_SHELL.get(), RenderArtilleryShell::new);
        reg.accept(ModEntities.HIMARS.get(), RenderArtilleryRocket::new);
        reg.accept(ModEntities.COG.get(), RenderCog::cog);
        reg.accept(ModEntities.STRAY_SAW.get(), RenderCog::sawblade);
        reg.accept(ModEntities.CHOPPER_MINE.get(), RenderChopperMine::new);
        reg.accept(ModEntities.FIREWORK_BALL.get(), ctx -> new RenderShrapnel<>(ctx));
        reg.accept(ModEntities.FALLING_BOMB.get(), RenderFallingNuke::new);
        reg.accept(ModEntities.BURNING_FOEQ.get(), RenderFOEQ::new);
        reg.accept(ModEntities.BOBMAZON_DELIVERY.get(), RenderMinerRocket::bobmazon);
        reg.accept(ModEntities.SOYUZ.get(), RenderSoyuz::new);
        reg.accept(ModEntities.SOYUZ_CAPSULE.get(), RenderSoyuzCapsule::new);
        reg.accept(ModEntities.SATELLITE_POD.get(), RenderDropship::new);
        reg.accept(ModEntities.TOM_THE_MOONSTONE.get(), RenderTom::new);
        reg.accept(ModEntities.EMP_BLAST.get(), RenderEMPBlast::new);
        reg.accept(ModEntities.LASER_BLAST.get(), RenderDeathBlast::new);
        reg.accept(ModEntities.ORBITAL_LASER.get(), RenderOrbitalLaser::new);
        reg.accept(ModEntities.BEAM_BOMB.get(), RenderBeamBomb::new);
        reg.accept(ModEntities.SIEGE_LASER.get(), RenderSiegeLaser::new);

        reg.accept(ModEntities.DELIVERY_DRONE.get(), ctx -> new RenderDeliveryDrone<>(ctx));
        reg.accept(ModEntities.REQUEST_DRONE.get(), ctx -> new RenderDeliveryDrone<>(ctx));

        for (var cart :
                List.of(
                        ModEntities.CART_ORE,
                        ModEntities.CART_POWDER,
                        ModEntities.CART_SEMTEX,
                        ModEntities.CART_CRATE,
                        ModEntities.CART_DESTROYER)) {
            reg.accept(cart.get(), ctx -> new MinecartRenderer(ctx, ModelLayers.MINECART));
        }

        reg.accept(ModEntities.CART_TEST.get(), RenderMinecartTest::new);
        reg.accept(ModEntities.BOAT_RUBBER.get(), RenderBoatRubber::new);

        reg.accept(ModEntities.MASK_MAN.get(), RenderMaskMan::new);
        reg.accept(ModEntities.UFO.get(), RenderUFO::new);
        reg.accept(ModEntities.BALLS_O_TRON.get(), RenderBOTPrime::head);
        reg.accept(ModEntities.BALLS_O_TRON_SEG.get(), RenderBOTPrime::body);

        reg.accept(ModEntities.CARGO_TRAM.get(), ctx -> new RenderTrainCargoTram<>(ctx));
        reg.accept(ModEntities.CARGO_TRAM_TRAILER.get(), RenderTrainCargoTramTrailer::new);

        reg.accept(ModEntities.BOUNDING_DUMMY.get(), NoopRenderer::new);
        reg.accept(ModEntities.SEAT_DUMMY.get(), NoopRenderer::new);

        reg.accept(
                ModEntities.CREEPER_NUCLEAR.get(),
                ctx -> new RenderCreeperUniversal(ctx, ResourceManager.creeper_nuclear_tex, 5F));
        reg.accept(
                ModEntities.CREEPER_TAINTED.get(),
                ctx -> new RenderCreeperUniversal(ctx, ResourceManager.creeper_tainted_tex, 1F));
        reg.accept(
                ModEntities.CREEPER_PHOSGENE.get(),
                ctx -> new RenderCreeperUniversal(ctx, ResourceManager.creeper_phosgene_tex, 1F));
        reg.accept(
                ModEntities.CREEPER_VOLATILE.get(),
                ctx -> new RenderCreeperUniversal(ctx, ResourceManager.creeper_volatile_tex, 1F));
        reg.accept(
                ModEntities.CREEPER_GOLD.get(),
                ctx -> new RenderCreeperUniversal(ctx, ResourceManager.creeper_gold_tex, 1F));

        reg.accept(ModEntities.CYBER_CRAB.get(), RenderCyberCrab::new);
        reg.accept(ModEntities.TESLA_CRAB.get(), RenderTeslaCrab::tesla);
        reg.accept(ModEntities.TAINT_CRAB.get(), RenderTeslaCrab::taint);
        reg.accept(ModEntities.BLOCK_SPIDER.get(), RenderBlockSpider::new);
        reg.accept(ModEntities.DUCK.get(), RenderDuck::duck);
        reg.accept(ModEntities.QUACKOS.get(), RenderDuck::quackos);
        reg.accept(ModEntities.PIGEON.get(), RenderPigeon::new);
        reg.accept(ModEntities.PLASTIC_BAG.get(), RenderPlasticBag::new);
        reg.accept(ModEntities.GHOST.get(), RenderHumanoidMob::ghost);
        reg.accept(ModEntities.TEST_DUMMY.get(), RenderHumanoidMob::dummy);
        reg.accept(ModEntities.FBI.get(), RenderHumanoidMob::fbi);
        reg.accept(ModEntities.FBI_DRONE.get(), RenderFBIDrone::new);
        reg.accept(ModEntities.RAD_BEAST.get(), RenderRADBeast::new);
        reg.accept(ModEntities.HUNTER_CHOPPER.get(), RenderHunterChopper::new);

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
        reg.accept(ModMenus.MACHINE_CENTRIFUGE.get(), ScreenMachineCentrifuge::new);
        reg.accept(ModMenus.MACHINE_GASCENT.get(), ScreenMachineGasCent::new);
        reg.accept(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), ScreenMachineElectricFurnace::new);
        reg.accept(ModMenus.MACHINE_PRESS.get(), ScreenMachinePress::new);
        reg.accept(ModMenus.MACHINE_AUTOCRAFTER.get(), ScreenMachineAutocrafter::new);
        reg.accept(ModMenus.PNEUMATIC_TUBE.get(), ScreenPneumoTube::new);
        reg.accept(ModMenus.CRANE_INSERTER.get(), ScreenCraneInserter::new);
        reg.accept(ModMenus.CRANE_EXTRACTOR.get(), ScreenCraneExtractor::new);
        reg.accept(ModMenus.CRANE_GRABBER.get(), ScreenCraneGrabber::new);
        reg.accept(ModMenus.CRANE_BOXER.get(), ScreenCraneBoxer::new);
        reg.accept(ModMenus.CRANE_UNBOXER.get(), ScreenCraneUnboxer::new);
        reg.accept(ModMenus.CRANE_ROUTER.get(), ScreenCraneRouter::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_ACCESS.get(), ScreenPneumoStorageAccess::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_CLUTTER.get(), ScreenPneumoStorageClutter::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_MONO.get(), ScreenPneumoStorageMono::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_IMPORTER.get(), ScreenPneumoStorageImporter::new);
        reg.accept(ModMenus.PNEUMATIC_STORAGE_EXPORTER.get(), ScreenPneumoStorageExporter::new);
        reg.accept(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), ScreenMachineAssemblyMachine::new);
        reg.accept(ModMenus.MACHINE_ASSEMBLY_FACTORY.get(), ScreenMachineAssemblyFactory::new);
        reg.accept(ModMenus.MACHINE_CHEMICAL_PLANT.get(), ScreenMachineChemicalPlant::new);
        reg.accept(ModMenus.MACHINE_ROCK_MILL.get(), ScreenMachineRockMill::new);
        reg.accept(ModMenus.MACHINE_CUSTOM.get(), ScreenMachineCustom::new);
        reg.accept(ModMenus.MACHINE_RADIOLYSIS.get(), ScreenMachineRadiolysis::new);
        reg.accept(ModMenus.FUSION_BREEDER.get(), ScreenFusionBreeder::new);
        reg.accept(ModMenus.CORE_CORE.get(), ScreenCore::new);
        reg.accept(ModMenus.CORE_EMITTER.get(), ScreenCoreEmitter::new);
        reg.accept(ModMenus.CORE_RECEIVER.get(), ScreenCoreReceiver::new);
        reg.accept(ModMenus.CORE_INJECTOR.get(), ScreenCoreInjector::new);
        reg.accept(ModMenus.CORE_STABILIZER.get(), ScreenCoreStabilizer::new);
        reg.accept(ModMenus.FUSION_KLYSTRON.get(), ScreenFusionKlystron::new);
        reg.accept(ModMenus.FUSION_TORUS.get(), ScreenFusionTorus::new);
        reg.accept(ModMenus.MACHINE_PLASMA_FORGE.get(), ScreenMachinePlasmaForge::new);
        reg.accept(ModMenus.MACHINE_CHEMICAL_FACTORY.get(), ScreenMachineChemicalFactory::new);
        reg.accept(ModMenus.MACHINE_ELECTROLYSER_FLUID.get(), ScreenMachineElectrolyserFluid::new);
        reg.accept(ModMenus.MACHINE_ELECTROLYSER_METAL.get(), ScreenMachineElectrolyserMetal::new);
        reg.accept(ModMenus.MACHINE_REFINERY.get(), ScreenMachineRefinery::new);
        reg.accept(ModMenus.MACHINE_CATALYTIC_REFORMER.get(), ScreenMachineCatalyticReformer::new);
        reg.accept(ModMenus.MACHINE_HYDROTREATER.get(), ScreenMachineHydrotreater::new);
        reg.accept(ModMenus.MACHINE_PUREX.get(), ScreenMachinePUREX::new);
        reg.accept(ModMenus.MACHINE_MIXER.get(), ScreenMachineMixer::new);
        reg.accept(ModMenus.MACHINE_SILEX.get(), ScreenMachineSILEX::new);
        reg.accept(ModMenus.MACHINE_ICF.get(), ScreenICF::new);
        reg.accept(ModMenus.MACHINE_ICF_PRESS.get(), ScreenICFPress::new);
        reg.accept(ModMenus.MACHINE_ORE_SLOPPER.get(), ScreenMachineOreSlopper::new);
        reg.accept(ModMenus.MACHINE_ANNIHILATOR.get(), ScreenMachineAnnihilator::new);
        reg.accept(ModMenus.MACHINE_BLAST_FURNACE.get(), ScreenMachineBlastFurnace::new);
        reg.accept(ModMenus.MACHINE_CRUCIBLE.get(), ScreenCrucible::new);
        reg.accept(ModMenus.MACHINE_STRAND_CASTER.get(), ScreenMachineStrandCaster::new);
        reg.accept(ModMenus.MACHINE_PRECASS.get(), ScreenMachinePrecAss::new);
        reg.accept(ModMenus.MACHINE_MINING_LASER.get(), ScreenMachineMiningLaser::new);
        reg.accept(ModMenus.FORCEFIELD.get(), ScreenForceField::new);
        reg.accept(ModMenus.MACHINE_EXCAVATOR.get(), ScreenMachineExcavator::new);
        reg.accept(ModMenus.MACHINE_CYCLOTRON.get(), ScreenMachineCyclotron::new);
        reg.accept(ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), ScreenMachineExposureChamber::new);
        reg.accept(ModMenus.MACHINE_RADGEN.get(), ScreenMachineRadGen::new);
        reg.accept(ModMenus.MACHINE_RADAR.get(), ScreenMachineRadar::new);
        reg.accept(ModMenus.MACHINE_RADAR_SLOTS.get(), ScreenMachineRadarSlots::new);
        reg.accept(ModMenus.MACHINE_REACTOR_BREEDING.get(), ScreenMachineReactorBreeding::new);
        reg.accept(ModMenus.REACTOR_RESEARCH.get(), ScreenReactorResearch::new);
        reg.accept(ModMenus.REACTOR_CONTROL.get(), ScreenReactorControl::new);
        reg.accept(ModMenus.MACHINE_ROTARY_FURNACE.get(), ScreenMachineRotaryFurnace::new);
        reg.accept(ModMenus.MACHINE_ARC_FURNACE.get(), ScreenMachineArcFurnace::new);
        reg.accept(ModMenus.HEATER_FIREBOX.get(), ScreenFirebox::new);
        reg.accept(ModMenus.MACHINE_WELL.get(), ScreenMachineOilWell::new);
        reg.accept(ModMenus.MACHINE_COMPRESSOR.get(), ScreenMachineCompressor::new);
        reg.accept(ModMenus.MACHINE_GAS_FLARE.get(), ScreenMachineGasFlare::new);
        reg.accept(ModMenus.REACTOR_ZIRNOX.get(), ScreenReactorZirnox::new);
        reg.accept(ModMenus.WATZ.get(), ScreenWatz::new);
        reg.accept(ModMenus.MACHINE_FEL.get(), ScreenFEL::new);
        reg.accept(ModMenus.PA_SOURCE.get(), ScreenPASource::new);
        reg.accept(ModMenus.PA_RFC.get(), ScreenPARFC::new);
        reg.accept(ModMenus.PA_QUADRUPOLE.get(), ScreenPAQuadrupole::new);
        reg.accept(ModMenus.PA_DIPOLE.get(), ScreenPADipole::new);
        reg.accept(ModMenus.PA_DETECTOR.get(), ScreenPADetector::new);
        reg.accept(ModMenus.MACHINE_SOLDERING_STATION.get(), ScreenMachineSolderingStation::new);
        reg.accept(ModMenus.MACHINE_ARC_WELDER.get(), ScreenMachineArcWelder::new);
        reg.accept(ModMenus.MACHINE_EPRESS.get(), ScreenMachineEPress::new);
        reg.accept(ModMenus.MACHINE_SHREDDER.get(), ScreenMachineShredder::new);
        reg.accept(ModMenus.TURRET_BASE.get(), ScreenTurretBase::new);
        reg.accept(ModMenus.MACHINE_FLUID_TANK.get(), ScreenMachineFluidTank::new);
        reg.accept(ModMenus.BARREL.get(), ScreenBarrel::new);
        reg.accept(ModMenus.MASS_STORAGE.get(), ScreenMassStorage::new);
        reg.accept(ModMenus.FILE_CABINET.get(), ScreenFileCabinet::new);
        reg.accept(ModMenus.MACHINE_WOOD_BURNER.get(), ScreenMachineWoodBurner::new);
        reg.accept(ModMenus.MACHINE_DIESEL.get(), ScreenMachineDiesel::new);
        reg.accept(ModMenus.MACHINE_RTG.get(), ScreenMachineRTG::new);
        reg.accept(ModMenus.MACHINE_TURBINE.get(), ScreenMachineTurbine::new);
        reg.accept(ModMenus.MACHINE_LARGE_TURBINE.get(), ScreenMachineLargeTurbine::new);
        reg.accept(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), ScreenMachineCombustionEngine::new);
        reg.accept(ModMenus.MACHINE_TURBINEGAS.get(), ScreenMachineTurbineGas::new);
        reg.accept(ModMenus.MACHINE_TURBOFAN.get(), ScreenMachineTurbofan::new);
        reg.accept(ModMenus.WASTE_DRUM.get(), ScreenWasteDrum::new);
        reg.accept(ModMenus.STORAGE_DRUM.get(), ScreenStorageDrum::new);
        reg.accept(ModMenus.BATTERY_SOCKET.get(), ScreenBatterySocket::new);
        reg.accept(ModMenus.MACHINE_BATTERY.get(), ScreenMachineBattery::new);
        reg.accept(ModMenus.BATTERY_REDD.get(), ScreenBatteryREDD::new);
        reg.accept(ModMenus.TRAIN_CARGO_TRAM.get(), ScreenTrainCargoTram::new);
        reg.accept(ModMenus.TRAIN_CARGO_TRAM_TRAILER.get(), ScreenTrainCargoTramTrailer::new);
        reg.accept(ModMenus.CART_DESTROYER.get(), ScreenCartDestroyer::new);
        reg.accept(ModMenus.NUKE_MAN.get(), ScreenNukeMan::new);
        reg.accept(ModMenus.NUKE_GADGET.get(), ScreenNukeGadget::new);
        reg.accept(ModMenus.NUKE_BOY.get(), ScreenNukeBoy::new);
        reg.accept(ModMenus.NUKE_MIKE.get(), ScreenNukeMike::new);
        reg.accept(ModMenus.NUKE_TSAR.get(), ScreenNukeTsar::new);
        reg.accept(ModMenus.NUKE_FLEIJA.get(), ScreenNukeFleija::new);
        reg.accept(ModMenus.NUKE_PROTOTYPE.get(), ScreenNukePrototype::new);
        reg.accept(ModMenus.NUKE_SOLINIUM.get(), ScreenNukeSolinium::new);
        reg.accept(ModMenus.NUKE_N2.get(), ScreenNukeN2::new);
        reg.accept(ModMenus.MACHINE_MISSILE_ASSEMBLY.get(), ScreenMachineMissileAssembly::new);
        reg.accept(ModMenus.LAUNCH_PAD.get(), ScreenLaunchPad::new);

        reg.accept(ModMenus.LAUNCH_PAD_LARGE.get(), ScreenLaunchPad::new);
        reg.accept(ModMenus.LAUNCH_PAD_RUSTED.get(), ScreenLaunchPadRusted::new);
        reg.accept(ModMenus.LAUNCH_TABLE.get(), ScreenLaunchTable::new);
        reg.accept(ModMenus.COMPACT_LAUNCHER.get(), ScreenCompactLauncher::new);
        reg.accept(ModMenus.NUKE_CUSTOM.get(), ScreenNukeCustom::new);
        reg.accept(ModMenus.NUKE_FSTBMB.get(), ScreenNukeFstbmb::new);
        reg.accept(ModMenus.BOMB_MULTI.get(), ScreenBombMulti::new);
        reg.accept(ModMenus.SOYUZ_CAPSULE.get(), ScreenSoyuzCapsule::new);
        reg.accept(ModMenus.SOYUZ_LAUNCHER.get(), ScreenSoyuzLauncher::new);
        reg.accept(ModMenus.LAUNCHPAD_SOYUZ.get(), ScreenLaunchpadSoyuz::new);
        reg.accept(ModMenus.SAT_DOCK.get(), ScreenSatDock::new);
        reg.accept(ModMenus.RADIO_TORCH_COUNTER.get(), ScreenRadioTorchCounter::new);
        reg.accept(ModMenus.DRONE_CRATE.get(), ScreenDroneCrate::new);
        reg.accept(ModMenus.DRONE_DOCK.get(), ScreenDroneDock::new);
        reg.accept(ModMenus.DRONE_PROVIDER.get(), ScreenDroneProvider::new);
        reg.accept(ModMenus.DRONE_REQUESTER.get(), ScreenDroneRequester::new);
        reg.accept(ModMenus.WEAPON_TABLE.get(), ScreenWeaponTable::new);
        reg.accept(ModMenus.BOOK_OF.get(), ScreenBook::new);
        reg.accept(ModMenus.LEMEGETON.get(), ScreenLemegeton::new);
        reg.accept(ModMenus.ARMOR_TABLE.get(), ScreenArmorTable::new);
        reg.accept(ModMenus.RBMK_ROD.get(), ScreenRBMKRod::new);
        reg.accept(ModMenus.RBMK_CONTROL.get(), ScreenRBMKControl::new);
        reg.accept(ModMenus.RBMK_CONTROL_AUTO.get(), ScreenRBMKControlAuto::new);
        reg.accept(ModMenus.RBMK_BOILER.get(), ScreenRBMKBoiler::new);
        reg.accept(ModMenus.RBMK_STORAGE.get(), ScreenRBMKStorage::new);
        reg.accept(ModMenus.RBMK_HEATER.get(), ScreenRBMKHeater::new);
        reg.accept(ModMenus.RBMK_OUTGASSER.get(), ScreenRBMKOutgasser::new);
        reg.accept(ModMenus.RBMK_CONSOLE.get(), ScreenRBMKConsole::new);
        reg.accept(ModMenus.RBMK_AUTOLOADER.get(), ScreenRBMKAutoloader::new);
        reg.accept(ModMenus.PWR_CONTROLLER.get(), ScreenPWR::new);
        reg.accept(ModMenus.FURNACE_IRON.get(), ScreenFurnaceIron::new);
        reg.accept(ModMenus.FURNACE_STEEL.get(), ScreenFurnaceSteel::new);
        reg.accept(ModMenus.FURNACE_COMBINATION.get(), ScreenFurnaceCombination::new);
        reg.accept(ModMenus.FURNACE_BRICK.get(), ScreenFurnaceBrick::new);
        reg.accept(ModMenus.HEATER_OVEN.get(), ScreenHeaterOven::new);
        reg.accept(ModMenus.MACHINE_ASHPIT.get(), ScreenMachineAshpit::new);
        reg.accept(ModMenus.MACHINE_SIREN.get(), ScreenMachineSiren::new);
        reg.accept(ModMenus.HEATER_OILBURNER.get(), ScreenMachineOilburner::new);
        reg.accept(ModMenus.HEATER_HEATEX.get(), ScreenMachineHeatex::new);
        reg.accept(ModMenus.MACHINE_CRYSTALLIZER.get(), ScreenMachineCrystallizer::new);
        reg.accept(ModMenus.MACHINE_COKER.get(), ScreenMachineCoker::new);
        reg.accept(ModMenus.MACHINE_LIQUEFACTOR.get(), ScreenMachineLiquefactor::new);
        reg.accept(ModMenus.MACHINE_SOLIDIFIER.get(), ScreenMachineSolidifier::new);
        reg.accept(ModMenus.MACHINE_PYROOVEN.get(), ScreenMachinePyroOven::new);
        reg.accept(ModMenus.MACHINE_VACUUM_DISTILL.get(), ScreenMachineVacuumDistill::new);
        reg.accept(ModMenus.MACHINE_FUNNEL.get(), ScreenMachineFunnel::new);
        reg.accept(ModMenus.MACHINE_MICROWAVE.get(), ScreenMachineMicrowave::new);
        reg.accept(ModMenus.MACHINE_SATLINKER.get(), ScreenMachineSatLinker::new);
        reg.accept(ModMenus.MACHINE_SUPERCOMPUTER.get(), ScreenMachineSuperComputer::new);
        reg.accept(ModMenus.MACHINE_TAPE_DRIVE.get(), ScreenTapeDrive::new);
        reg.accept(ModMenus.MACHINE_KEYFORGE.get(), ScreenMachineKeyForge::new);
        reg.accept(ModMenus.MACHINE_AMMO_PRESS.get(), ScreenMachineAmmoPress::new);
        for (int tier : ModBlocks.ANVIL_TIERS)
            reg.accept(ModMenus.anvilMenuType(tier).get(), ScreenAnvil::new);
        for (CrateType type : CrateType.values())
            reg.accept(ModMenus.crateMenuType(type).get(), ScreenCrate::new);
        reg.accept(ModMenus.CONTAINMENT_BOX.get(), ScreenItemBox::new);
        reg.accept(ModMenus.TOOLBOX.get(), ScreenItemBox::new);
        reg.accept(ModMenus.AMMO_BAG.get(), ScreenItemBox::new);
        reg.accept(ModMenus.CASING_BAG.get(), ScreenItemBox::new);
        reg.accept(ModMenus.PLASTIC_BAG.get(), ScreenItemBox::new);
        reg.accept(ModMenus.REBAR_PLACER.get(), ScreenRebarPlacer::new);
    }

    public static void registerBlockEntityRenderers(BlockEntityRendererRegistrar reg) {
        reg.accept(ModBlockEntities.FEL.get(), context -> new RenderFEL(context));
        reg.accept(ModBlockEntities.RAIL.get(), context -> new RenderRail());

        reg.accept(ModBlockEntities.PA_BEAMLINE.get(), context -> new RenderPABeamline(context));
        reg.accept(ModBlockEntities.TESLA_COIL.get(), context -> new RenderTesla());
        reg.accept(ModBlockEntities.BATTERY_SOCKET.get(), RenderBatterySocket::new);
        reg.accept(ModBlockEntities.NTM_CHARGER.get(), context -> new RenderCharger());
        reg.accept(ModBlockEntities.REFUELER.get(), RenderRefueler::new);
        reg.accept(
                ModBlockEntities.ASSEMBLYMACHINE.get(),
                context -> new RenderAssemblyMachine(context));
        reg.accept(
                ModBlockEntities.ASSEMBLYFACTORY.get(),
                context -> new RenderAssemblyFactory(context));
        reg.accept(ModBlockEntities.CHEMICALPLANT.get(), context -> new RenderChemicalPlant());
        reg.accept(ModBlockEntities.ROCK_MILL.get(), context -> new RenderRockMill());
        reg.accept(ModBlockEntities.FUSION_TORUS.get(), context -> new RenderFusionTorus());
        reg.accept(ModBlockEntities.CORE_CORE.get(), context -> new RenderCore());
        reg.accept(ModBlockEntities.CORE_EMITTER.get(), context -> new RenderCoreComponent());
        reg.accept(ModBlockEntities.CORE_INJECTOR.get(), context -> new RenderCoreComponent());
        reg.accept(ModBlockEntities.CORE_STABILIZER.get(), context -> new RenderCoreComponent());
        reg.accept(ModBlockEntities.FUSION_KLYSTRON.get(), context -> new RenderFusionKlystron());
        reg.accept(
                ModBlockEntities.FUSION_KLYSTRON_CREATIVE.get(),
                context -> new RenderFusionKlystronCreative());
        reg.accept(ModBlockEntities.FUSION_MHDT.get(), context -> new RenderFusionMHDT());
        reg.accept(ModBlockEntities.FUSION_PLASMA_FORGE.get(), RenderFusionPlasmaForge::new);
        reg.accept(ModBlockEntities.CHEMICALFACTORY.get(), context -> new RenderChemicalFactory());
        reg.accept(ModBlockEntities.ELECTROLYSER.get(), context -> new RenderElectrolyser());
        reg.accept(ModBlockEntities.PUREX.get(), context -> new RenderPUREX());
        reg.accept(ModBlockEntities.MIXER.get(), context -> new RenderMixer());
        reg.accept(ModBlockEntities.ORE_SLOPPER.get(), context -> new RenderOreSlopper(context));
        reg.accept(ModBlockEntities.ANNIHILATOR.get(), context -> new RenderAnnihilator(context));
        reg.accept(ModBlockEntities.SUPERCOMPUTER.get(), context -> new RenderSuperComputer());
        reg.accept(ModBlockEntities.TAPE_DRIVE.get(), context -> new RenderTapeDrive());
        reg.accept(ModBlockEntities.HEPHAESTUS.get(), context -> new RenderHephaestus());
        reg.accept(ModBlockEntities.CRUCIBLE.get(), context -> new RenderCrucible());
        reg.accept(ModBlockEntities.STRAND_CASTER.get(), context -> new RenderStrandCaster());
        reg.accept(ModBlockEntities.PRECASS.get(), context -> new RenderPrecAss(context));
        reg.accept(ModBlockEntities.ROTARY_FURNACE.get(), context -> new RenderRotaryFurnace());
        reg.accept(ModBlockEntities.ARC_FURNACE_LARGE.get(), context -> new RenderArcFurnace());
        reg.accept(ModBlockEntities.FIREBOX.get(), context -> new RenderFirebox());
        reg.accept(ModBlockEntities.FOUNDRY_MOLD.get(), context -> new RenderFoundry(context));
        reg.accept(ModBlockEntities.FOUNDRY_BASIN.get(), context -> new RenderFoundry(context));
        reg.accept(ModBlockEntities.FOUNDRY_CHANNEL.get(), context -> new RenderFoundryChannel());
        reg.accept(ModBlockEntities.FOUNDRY_OUTLET.get(), context -> new RenderFoundryOutlet());
        reg.accept(ModBlockEntities.FOUNDRY_SLAGTAP.get(), context -> new RenderFoundryOutlet());
        reg.accept(ModBlockEntities.FOUNDRY_TANK.get(), context -> new RenderFoundryTank());
        reg.accept(ModBlockEntities.FOUNDRY_SLAG.get(), context -> new RenderSlag());
        reg.accept(ModBlockEntities.NTM_LOOT.get(), context -> new RenderLoot(context));
        reg.accept(ModBlockEntities.EXPLOSIVE_CHARGE.get(), RenderExplosiveCharge::new);
        reg.accept(ModBlockEntities.MACHINE_PUMPJACK.get(), context -> new RenderPumpjack());
        reg.accept(ModBlockEntities.MINING_LASER.get(), context -> new RenderLaserMiner());
        reg.accept(ModBlockEntities.FORCEFIELD.get(), context -> new RenderMachineForceField());
        reg.accept(ModBlockEntities.EXCAVATOR.get(), context -> new RenderExcavator());
        reg.accept(ModBlockEntities.CYCLOTRON.get(), RenderCyclotron::new);
        reg.accept(ModBlockEntities.EXPOSURE_CHAMBER.get(), context -> new RenderExposureChamber());
        reg.accept(ModBlockEntities.RADGEN.get(), context -> new RenderRadGen());
        reg.accept(ModBlockEntities.LANTERN_BEHEMOTH.get(), context -> new RenderLanternBehemoth());
        reg.accept(ModBlockEntities.LANTERN.get(), context -> new RenderLantern());
        reg.accept(ModBlockEntities.RADAR.get(), context -> new RenderRadar());
        reg.accept(ModBlockEntities.RADAR_LARGE.get(), context -> new RenderRadarLarge());
        reg.accept(ModBlockEntities.SATLINK.get(), context -> new RenderSatLink());
        reg.accept(ModBlockEntities.RADAR_SCREEN.get(), context -> new RenderRadarScreen());
        reg.accept(ModBlockEntities.MACHINE_REACTOR_BREEDING.get(), context -> new RenderBreeder());
        reg.accept(ModBlockEntities.REACTOR_RESEARCH.get(), context -> new RenderSmallReactor());
        reg.accept(ModBlockEntities.COMPRESSOR.get(), context -> new RenderCompressor());
        reg.accept(ModBlockEntities.DIESEL_GENERATOR.get(), context -> new RenderDiesel());
        reg.accept(ModBlockEntities.INDUSTRIAL_TURBINE.get(), context -> new RenderLargeTurbine());
        reg.accept(ModBlockEntities.MACHINE_TURBOFAN.get(), context -> new RenderTurbofan());
        reg.accept(ModBlockEntities.LPW2.get(), context -> new RenderLPW2());
        reg.accept(ModBlockEntities.CARGO_ELEVATOR.get(), context -> new RenderCargoElevator());
        reg.accept(ModBlockEntities.IND_TURBINE.get(), context -> new RenderIndustrialTurbine());
        reg.accept(ModBlockEntities.CHUNGUS.get(), context -> new RenderChungus());
        reg.accept(ModBlockEntities.HEAT_BOILER.get(), context -> new RenderHeatBoiler());
        reg.accept(ModBlockEntities.STEAM_ENGINE.get(), context -> new RenderSteamEngine());
        reg.accept(ModBlockEntities.FENSU.get(), context -> new RenderFENSU());
        reg.accept(ModBlockEntities.BATTERY_REDD.get(), context -> new RenderBatteryREDD());
        reg.accept(
                ModBlockEntities.COMBUSTION_ENGINE.get(), context -> new RenderCombustionEngine());
        reg.accept(ModBlockEntities.STEAM_PUMP.get(), context -> new RenderPump());
        reg.accept(ModBlockEntities.SOLARMIRROR.get(), context -> new RenderSolarMirror());
        reg.accept(ModBlockEntities.SOLARBOILER.get(), context -> new RenderSolarBoiler(context));
        reg.accept(
                ModBlockEntities.COMPRESSOR_COMPACT.get(),
                context -> new RenderCompressorCompact());
        reg.accept(ModBlockEntities.FLUID_TANK.get(), context -> new RenderFluidTank());
        reg.accept(ModBlockEntities.FLUID_BARREL.get(), context -> new RenderBarrel());
        reg.accept(ModBlockEntities.MASS_STORAGE.get(), RenderMassStorage::new);
        reg.accept(ModBlockEntities.FILE_CABINET.get(), context -> new RenderFileCabinet());
        reg.accept(ModBlockEntities.PIOE_ANCHOR.get(), context -> new RenderPipeAnchor());
        reg.accept(ModBlockEntities.PYLON_REDWIRE.get(), context -> new RenderPylonWires());
        reg.accept(ModBlockEntities.PYLON_MEDIUM.get(), context -> new RenderPylonWires());
        reg.accept(ModBlockEntities.CONNECTOR_REDWIRE.get(), context -> new RenderPylonWires());
        reg.accept(
                ModBlockEntities.CONNECTOR_REDWIRE_SUPER.get(), context -> new RenderPylonWires());
        reg.accept(ModBlockEntities.PYLON_LARGE.get(), context -> new RenderPylonWires());
        reg.accept(ModBlockEntities.SUBSTATION.get(), context -> new RenderPylonWires());
        reg.accept(
                ModBlockEntities.SOLDERING_STATION.get(),
                context -> new RenderSolderingStation(context));
        reg.accept(ModBlockEntities.ARC_WELDER.get(), context -> new RenderArcWelder(context));
        reg.accept(
                ModBlockEntities.TURRET_HOWARD_DAMAGED.get(),
                context -> new RenderTurretHowardDamaged());
        reg.accept(
                ModBlockEntities.TURRET_CHEKHOV.get(),
                context -> new RenderTurretChekhov<>(ResourceManager.turret_carriage_tex));
        reg.accept(
                ModBlockEntities.TURRET_FRIENDLY.get(),
                context -> new RenderTurretChekhov<>(ResourceManager.turret_carriage_friendly_tex));
        reg.accept(ModBlockEntities.TURRET_JEREMY.get(), context -> new RenderTurretJeremy());
        reg.accept(ModBlockEntities.TURRET_TAUON.get(), context -> new RenderTurretTauon());
        reg.accept(ModBlockEntities.TURRET_RICHARD.get(), context -> new RenderTurretRichard());
        reg.accept(ModBlockEntities.TURRET_HOWARD.get(), context -> new RenderTurretHoward());
        reg.accept(ModBlockEntities.TURRET_MAXWELL.get(), context -> new RenderTurretMaxwell());
        reg.accept(ModBlockEntities.TURRET_FRITZ.get(), context -> new RenderTurretFritz());
        reg.accept(ModBlockEntities.TURRET_ARTY.get(), context -> new RenderTurretArty());
        reg.accept(ModBlockEntities.TURRET_HIMARS.get(), context -> new RenderTurretHIMARS());
        reg.accept(ModBlockEntities.TURRET_SENTRY.get(), context -> new RenderTurretSentry());
        reg.accept(
                ModBlockEntities.TURRET_SENTRY_DAMAGED.get(), context -> new RenderTurretSentry());
        reg.accept(ModBlockEntities.NTM_DOOR.get(), context -> new RenderDoorGeneric());
        reg.accept(ModBlockEntities.BLAST_DOOR.get(), context -> new RenderBlastDoor());
        reg.accept(ModBlockEntities.EMITTER.get(), context -> new RenderEmitter());
        reg.accept(ModBlockEntities.LAUNCH1.get(), context -> new RenderLaunchPad());
        reg.accept(ModBlockEntities.LAUNCHPAD_RUSTED.get(), context -> new RenderLaunchPadRusted());
        reg.accept(ModBlockEntities.LAUNCHPAD_LARGE.get(), context -> new RenderLaunchPadLarge());
        reg.accept(ModBlockEntities.LARGE_LAUNCH_TABLE.get(), context -> new RenderLaunchTable());
        reg.accept(ModBlockEntities.SMALL_LAUNCHER.get(), context -> new RenderCompactLauncher());
        reg.accept(
                ModBlockEntities.MULTI_CORE.get(), context -> new RenderMultiblockStructPreview());
        reg.accept(ModBlockEntities.NUKE_FSTBMB.get(), context -> new RenderNukeFstbmb());
        reg.accept(ModBlockEntities.SOYUZ_STRUCT.get(), context -> new RenderSoyuzStructPreview());
        reg.accept(ModBlockEntities.SOYUZ_LAUNCHER.get(), context -> new RenderSoyuzLauncher());
        reg.accept(ModBlockEntities.LAUNCHPAD_SOYUZ.get(), context -> new RenderLaunchpadSoyuz());
        reg.accept(ModBlockEntities.MISSILE_ASSEMBLY.get(), context -> new RenderMissileAssembly());
        reg.accept(ModBlockEntities.RBMK_ROD.get(), context -> new RenderRBMKFuelRod());
        reg.accept(ModBlockEntities.RBMK_ROD_REASIM.get(), context -> new RenderRBMKFuelRod());
        reg.accept(ModBlockEntities.RBMK_CONTROL.get(), context -> new RenderRBMKControlRod());
        reg.accept(ModBlockEntities.RBMK_CONTROL_AUTO.get(), context -> new RenderRBMKControlRod());
        reg.accept(ModBlockEntities.RBMK_CONSOLE.get(), context -> new RenderRBMKConsole(context));
        reg.accept(ModBlockEntities.RBMK_GAUGE.get(), context -> new RenderRBMKGauge(context));
        reg.accept(ModBlockEntities.RBMK_DISPLAY.get(), context -> new RenderRBMKDisplay());
        reg.accept(
                ModBlockEntities.RBMK_NUMITRON.get(), context -> new RenderRBMKNumitron(context));
        reg.accept(
                ModBlockEntities.RBMK_INDICATOR.get(), context -> new RenderRBMKIndicator(context));
        reg.accept(ModBlockEntities.RBMK_GRAPH.get(), context -> new RenderRBMKGraph(context));
        reg.accept(ModBlockEntities.RBMK_LEVER.get(), context -> new RenderRBMKLever(context));
        reg.accept(ModBlockEntities.RBMK_KEYPAD.get(), context -> new RenderRBMKKeyPad(context));
        reg.accept(
                ModBlockEntities.RBMK_TERMINAL.get(), context -> new RenderRBMKTerminal(context));
        reg.accept(ModBlockEntities.RBMK_AUTOLOADER.get(), context -> new RenderRBMKAutoloader());
        reg.accept(ModBlockEntities.ICF_STRUCT.get(), context -> new RenderICFStructPreview());
        reg.accept(ModBlockEntities.WATZ_STRUCT.get(), context -> new RenderWatzStructPreview());
        reg.accept(
                ModBlockEntities.FUSION_TORUS_STRUCT.get(),
                context -> new RenderFusionTorusStructPreview());
        reg.accept(ModBlockEntities.PILE_LOADER.get(), context -> new RenderPileLoader());
        reg.accept(ModBlockEntities.PILE_VENT.get(), context -> new RenderPileVent());
        reg.accept(ModBlockEntities.PILE_CONTROL.get(), context -> new RenderPileControl());
        reg.accept(ModBlockEntities.FAN.get(), context -> new RenderFan());
        reg.accept(
                ModBlockEntities.PISTON_INSERTER.get(),
                context -> new RenderPistonInserter(context));
        reg.accept(
                ModBlockEntities.RBMK_CRANE_CONSOLE.get(), context -> new RenderRBMKCraneConsole());
        reg.accept(ModBlockEntities.CONVEYOR_PRESS.get(), RenderConveyorPress::new);
        reg.accept(ModBlockEntities.CRASHED_BALEFIRE.get(), context -> new RenderCrashedBomb());
        reg.accept(
                ModBlockEntities.PRESS.get(),
                context ->
                        new RenderPress<BlockEntityMachinePress>(
                                context,
                                ResourceManager.press_head,
                                ResourceManager.press_head_tex,
                                0F,
                                false,
                                0F,
                                0.99F,
                                BlockEntityMachinePress.MAX_PROGRESS,
                                (be, f) -> be.lastPress + (be.renderPress - be.lastPress) * f,
                                be -> be.getItem(BlockEntityMachinePress.SLOT_INPUT),
                                RenderPress.PRESS_ITEM));
        reg.accept(
                ModBlockEntities.ELECTRIC_PRESS.get(),
                context ->
                        new RenderPress<BlockEntityMachineEPress>(
                                context,
                                ResourceManager.epress_head,
                                ResourceManager.epress_head_tex,
                                90F,
                                true,
                                1F,
                                1.0F,
                                BlockEntityMachineEPress.MAX_PROGRESS,
                                (be, f) -> be.lastPress + (be.renderPress - be.lastPress) * f,
                                be -> be.getItem(BlockEntityMachineEPress.SLOT_INPUT),
                                RenderPress.EPRESS_ITEM));
        reg.accept(ModBlockEntities.FURNACE_IRON.get(), context -> new RenderFurnaceIron());
        reg.accept(ModBlockEntities.FURNACE_STEEL.get(), context -> new RenderFurnaceSteel());
        reg.accept(
                ModBlockEntities.COMBINATION_OVEN.get(), context -> new RenderFurnaceCombination());
        reg.accept(ModBlockEntities.HEATING_OVEN.get(), context -> new RenderHeaterOven());
        reg.accept(ModBlockEntities.ASHPIT.get(), context -> new RenderAshpit());
        reg.accept(ModBlockEntities.ACIDOMATIC.get(), context -> new RenderCrystallizer());
        reg.accept(ModBlockEntities.LIQUEFACTOR.get(), context -> new RenderLiquefactor());
        reg.accept(ModBlockEntities.SOLIDIFIER.get(), context -> new RenderSolidifier());
        reg.accept(ModBlockEntities.PYROOVEN.get(), context -> new RenderPyroOven());
        reg.accept(ModBlockEntities.ELECTRIC_PUMP.get(), context -> new RenderPump());
        reg.accept(ModBlockEntities.INTAKE.get(), context -> new RenderIntake());
        reg.accept(
                ModBlockEntities.CONDENSER_POWERED.get(), context -> new RenderCondenserPowered());
        reg.accept(ModBlockEntities.BIGASSTANK.get(), context -> new RenderBigAssTank());
        reg.accept(ModBlockEntities.ORBUS.get(), context -> new RenderOrbus());
        reg.accept(ModBlockEntities.MICROWAVE.get(), context -> new RenderMicrowave());
        reg.accept(ModBlockEntities.THRESHER.get(), context -> new RenderThresher());
        reg.accept(ModBlockEntities.AUTOSAW.get(), context -> new RenderAutosaw());
        reg.accept(ModBlockEntities.SAWMILL.get(), context -> new RenderSawmill());
        reg.accept(ModBlockEntities.STIRLING.get(), context -> new RenderStirling());
        reg.accept(ModBlockEntities.AMMO_PRESS.get(), context -> new RenderAmmoPress());
        reg.accept(
                ModBlockEntities.NTM_SKELETON.get(), context -> new RenderSkeletonHolder(context));
        reg.accept(ModBlockEntities.NTM_PEDESTAL.get(), RenderPedestal::new);
        reg.accept(ModBlockEntities.FLOODLIGHT.get(), context -> new RenderFloodlight());
        reg.accept(ModBlockEntities.DEMON_LAMP.get(), context -> new RenderDemonLamp());
        reg.accept(ModBlockEntities.NTM_BOBBLEHEAD.get(), context -> new RenderBobble(context));
        reg.accept(ModBlockEntities.NTM_SNOWGLOBE.get(), context -> new RenderSnowglobe(context));
        reg.accept(ModBlockEntities.NTM_PLUSHIE.get(), context -> new RenderPlushie(context));
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
        if (!hudVisible()
                || ItemGunBaseNT.aimingProgress != 1F
                || ItemGunBaseNT.prevAimingProgress != 1F) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof ItemGunBaseNT gun))
            return;
        Identifier scope =
                gun.getConfig(player.getMainHandItem(), 0)
                        .getScopeTexture(player.getMainHandItem());
        if (scope != null)
            NukeHud.shaken(graphics, () -> RenderScreenOverlay.renderScope(graphics, scope));
    }

    private static void renderGunHudComponents(GuiGraphicsExtractor graphics) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return;

        for (int i = 0; i < gun.getConfigCount(); i++) {
            var components = gun.getConfig(stack, i).getHUDComponents(stack);
            int bottomOffset = 0;
            if (components != null)
                for (var component : components) {
                    component.renderHUDComponent(graphics, player, stack, bottomOffset, i);
                    bottomOffset += component.getComponentHeight(player, stack);
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

    public static boolean renderShieldBar(GuiGraphicsExtractor graphics, int top) {
        return hudVisible() && ShieldHudRenderer.render(graphics, top);
    }

    public static boolean replacesCrosshair() {

        if (Minecraft.getInstance().gui.hud.isHidden()) return false;
        var player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem() instanceof ItemGunBaseNT;
    }

    public static void renderGunCrosshair(GuiGraphicsExtractor graphics) {
        var player = Minecraft.getInstance().player;
        var stack = player.getMainHandItem();
        var gun = (ItemGunBaseNT) stack.getItem();
        var config = gun.getConfig(stack, 0);
        if (gun instanceof ItemGunStinger) {
            if (ItemGunBaseNT.aimingProgress < 1F) return;
            RenderScreenOverlay.renderCustomCrosshairs(graphics, config.getCrosshair(stack));
            RenderScreenOverlay.renderStingerLockon(graphics);
            return;
        }
        if (config.getHideCrosshair(stack) && ItemGunBaseNT.aimingProgress >= 1F) return;
        RenderScreenOverlay.renderCustomCrosshairs(graphics, config.getCrosshair(stack));
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
            register(Library.id(name), value);
        }
    }
}
