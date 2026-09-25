// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.particle.ParticleResources;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;

public final class FlywheelResources {
    private static final ArrayList<Runnable> INITIALIZERS = new ArrayList<>();
    private static boolean initialized;

    private FlywheelResources() {}

    public static void onInitialize(Runnable initializer) {
        assert !initialized;
        INITIALIZERS.add(initializer);
    }

    public static void reload() {
        assert Minecraft.getInstance().isSameThread();
        VisualTextures.reload();
        FluidTankVisual.reloadTextures();
        WorldItem.clear();
        WorldSprite.clear();
        if (!initialized) {
            initialize();
            for (Runnable initializer : INITIALIZERS) initializer.run();
            INITIALIZERS.clear();
            initialized = true;
        } else {
            DoorVisual.reloadTextures();
        }
    }

    private static void initialize() {
        ParticleResources.initialize();
        AmmoPressVisual.initModels();
        AnnihilatorVisual.initModels();
        ArcFurnaceVisual.initModels();
        AshpitVisual.initModels();
        AssemblyFactoryVisual.initModels();
        AssemblyMachineVisual.initModels();
        AutosawVisual.initModels();
        BarrelVisual.initModels();
        BatteryREDDVisual.initModels();
        BatterySocketVisual.initModels();
        BeamBombVisual.initModels();
        BigAssTankVisual.initModels();
        BlackHoleVisual.initModels();
        BlastDoorVisual.initModels();
        BobbleVisual.initModels();
        BombletZetaVisual.initModels();
        BreederVisual.initModels();
        CargoElevatorVisual.initModels();
        ChargerVisual.initModels();
        ChemicalFactoryVisual.initModels();
        ChemicalPlantVisual.initModels();
        ChemicalVisual.initModels();
        ChopperMineVisual.initModels();
        ChungusVisual.initModels();
        CloudTomVisual.initModels();
        CombustionEngineVisual.initModels();
        CompactLauncherVisual.initModels();
        CompressorCompactVisual.initModels();
        CompressorVisual.initModels();
        CondenserPoweredVisual.initModels();
        ConveyorPressVisual.initModels();
        CoreComponentVisual.initModels();
        CoreVisual.initModels();
        CrashedBombVisual.initModels();
        CrucibleVisual.initModels();
        CrystallizerVisual.initModels();
        CyberCrabVisual.initModels();
        CyclotronVisual.initModels();
        DemonLampVisual.initModels();
        DieselVisual.initModels();
        DoorVisual.initModels();
        ElectricPressVisual.initModels();
        ElectrolyserVisual.initModels();
        EmitterVisual.initModels();
        EmpBlastVisual.initModels();
        ExcavatorVisual.initModels();
        ExposureChamberVisual.initModels();
        FanVisual.initModels();
        FELVisual.initModels();
        FENSUVisual.initModels();
        FileCabinetVisual.initModels();
        FireboxVisual.initModels();
        FloodlightVisual.initModels();
        FluidTankVisual.initModels();
        FOEQVisual.initModels();
        ForceFieldVisual.initModels();
        FoundryChannelVisual.initModels();
        FoundryOutletVisual.initModels();
        FoundryTankVisual.initModels();
        FoundryVisual.initModels();
        FurnaceCombinationVisual.initModels();
        FurnaceIronVisual.initModels();
        FurnaceSteelVisual.initModels();
        FusionKlystronCreativeVisual.initModels();
        FusionKlystronVisual.initModels();
        FusionMHDTVisual.initModels();
        FusionPlasmaForgeVisual.initModels();
        FusionTorusVisual.initModels();
        GenericGrenadeVisual.initModels();
        GlyphidNuclearVisual.initModels();
        GlyphidVisual.initModels();
        HazardDiamondVisual.initModels();
        HeatBoilerVisual.initModels();
        HeaterOvenVisual.initModels();
        HephaestusVisual.initModels();
        IndustrialTurbineVisual.initModels();
        IntakeVisual.initModels();
        LanternBehemothVisual.initModels();
        LanternVisual.initModels();
        LargeTurbineVisual.initModels();
        LaserColumnVisual.initModels();
        LaserMinerVisual.initModels();
        LaunchPadLargeVisual.initModels();
        LaunchPadRustedVisual.initModels();
        LaunchPadVisual.initModels();
        LaunchTableVisual.initModels();
        LegacyBulletVisual.initModels();
        LiquefactorVisual.initModels();
        LootVisual.initModels();
        LPW2Visual.initModels();
        MassStorageVisual.initModels();
        MeteorVisual.initModels();
        MicrowaveVisual.initModels();
        MissileAssemblyVisual.initModels();
        MixerVisual.initModels();
        MultiCloudVisual.initModels();
        NukeCloudVisual.initModels();
        NukeFstbmbVisual.initModels();
        OrbusVisual.initModels();
        OrdnanceVisuals.initModels();
        OreSlopperVisual.initModels();
        PABeamlineVisual.initModels();
        PigeonVisual.initModels();
        PileVentVisual.initModels();
        PipeAnchorVisual.initModels();
        PistonInserterVisual.initModels();
        PlushieVisual.initModels();
        PourVisual.initModels();
        PrecAssVisual.initModels();
        PressVisual.initModels();
        PreviewCube.initModels();
        PumpjackVisual.initModels();
        PumpVisual.initModels();
        PUREXVisual.initModels();
        PylonWiresVisual.initModels();
        PyroOvenVisual.initModels();
        RadarLargeVisual.initModels();
        RadarScreenVisual.initModels();
        RadarVisual.initModels();
        RadGenVisual.initModels();
        RailVisual.initModels();
        RBMKAutoloaderVisual.initModels();
        RBMKConsoleVisual.initModels();
        RBMKControlRodVisual.initModels();
        RBMKCraneConsoleVisual.initModels();
        RBMKDisplayVisual.initModels();
        RBMKFuelRodVisual.initModels();
        RBMKGaugeVisual.initModels();
        RBMKGraphVisual.initModels();
        RBMKIndicatorVisual.initModels();
        RBMKKeyPadVisual.initModels();
        RBMKLeverVisual.initModels();
        RBMKNumitronVisual.initModels();
        RBMKPanelModels.initModels();
        RefuelerVisual.initModels();
        RotaryFurnaceVisual.initModels();
        RubberBoatVisual.initModels();
        SawmillVisual.initModels();
        ShrapnelVisual.initModels();
        SlagVisual.initModels();
        SmallReactorVisual.initModels();
        SnowglobeVisual.initModels();
        LaunchpadSoyuzVisual.initModels();
        SolarBoilerVisual.initModels();
        SolarMirrorVisual.initModels();
        SolidifierVisual.initModels();
        SoyuzLauncherVisual.initModels();
        SpearVisual.initModels();
        SteamEngineVisual.initModels();
        StirlingVisual.initModels();
        StrandCasterVisual.initModels();
        TeslaVisual.initModels();
        ThresherVisual.initModels();
        TorexVisual.initModels();
        TurbofanVisual.initModels();
        TurretArtyVisual.initModels();
        TurretChekhovVisual.initModels();
        TurretFriendlyVisual.initModels();
        TurretFritzVisual.initModels();
        TurretHIMARSVisual.initModels();
        TurretHowardDamagedVisual.initModels();
        TurretHowardVisual.initModels();
        TurretJeremyVisual.initModels();
        TurretMaxwellVisual.initModels();
        TurretRichardVisual.initModels();
        TurretSentryVisual.initModels();
        TurretTauonVisual.initModels();
        VehicleVisuals.initModels();
    }
}
