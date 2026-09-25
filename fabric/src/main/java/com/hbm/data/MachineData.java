// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.rbmk.RBMKConfig;
import com.hbm.util.DataCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public final class MachineData {

    public static final ResourceKey<Registry<MachineConfig>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("machine_config"));

    private static final List<DataGroups.Group> GROUPS = new ArrayList<>();

    private static final DataGroups.Group CONDENSER = DataGroups.group(GROUPS, "condenser");
    public static final DataGroups.Param<Integer> CONDENSER_INPUT_TANK_SIZE =
            CONDENSER.integer("input_tank_capacity", 100);
    public static final DataGroups.Param<Integer> CONDENSER_OUTPUT_TANK_SIZE =
            CONDENSER.integer("output_tank_capacity", 100);

    private static final DataGroups.Group CONDENSER_POWERED =
            DataGroups.group(GROUPS, "condenserPowered");
    public static final DataGroups.Param<Long> CONDENSER_POWERED_MAX_POWER =
            CONDENSER_POWERED.longValue("power_capacity", 10_000_000L);
    public static final DataGroups.Param<Integer> CONDENSER_POWERED_INPUT_TANK_SIZE =
            CONDENSER_POWERED.integer("input_tank_capacity", 1_000_000);
    public static final DataGroups.Param<Integer> CONDENSER_POWERED_OUTPUT_TANK_SIZE =
            CONDENSER_POWERED.integer("output_tank_capacity", 1_000_000);
    public static final DataGroups.Param<Integer> CONDENSER_POWERED_POWER_CONSUMPTION =
            CONDENSER_POWERED.integer("power_consumption", 10);

    private static final DataGroups.Group CHUNGUS =
            DataGroups.group(GROUPS, "steamturbineLeviathan");
    public static final DataGroups.Param<Integer> CHUNGUS_INPUT_TANK_SIZE =
            CHUNGUS.integer("input_tank_capacity", 1_000_000_000);
    public static final DataGroups.Param<Integer> CHUNGUS_OUTPUT_TANK_SIZE =
            CHUNGUS.integer("output_tank_capacity", 1_000_000_000);
    public static final DataGroups.Param<Double> CHUNGUS_EFFICIENCY =
            CHUNGUS.real("efficiency", 0.85D);

    private static final DataGroups.Group ASHPIT = DataGroups.group(GROUPS, "ashpit");
    public static final DataGroups.Param<Integer> ASHPIT_THRESHOLD_WOOD =
            ASHPIT.integer("threshold_wood", 2000);
    public static final DataGroups.Param<Integer> ASHPIT_THRESHOLD_COAL =
            ASHPIT.integer("threshold_coal", 2000);
    public static final DataGroups.Param<Integer> ASHPIT_THRESHOLD_MISC =
            ASHPIT.integer("threshold_misc", 2000);
    public static final DataGroups.Param<Integer> ASHPIT_THRESHOLD_FLY =
            ASHPIT.integer("threshold_fly", 2000);
    public static final DataGroups.Param<Integer> ASHPIT_THRESHOLD_SOOT =
            ASHPIT.integer("threshold_soot", 8000);

    private static final DataGroups.Group FORCE_FIELD = DataGroups.group(GROUPS, "forcefield");
    public static final DataGroups.Param<Long> FORCE_FIELD_MAX_POWER =
            FORCE_FIELD.longValue("power_capacity", 1_000_000L);
    public static final DataGroups.Param<Integer> FORCE_FIELD_BASE_CONSUMPTION =
            FORCE_FIELD.integer("base_consumption", 1000);
    public static final DataGroups.Param<Integer> FORCE_FIELD_RADIUS_CONSUMPTION =
            FORCE_FIELD.integer("radius_consumption", 500);
    public static final DataGroups.Param<Integer> FORCE_FIELD_SHIELD_CONSUMPTION =
            FORCE_FIELD.integer("shield_consumption", 250);
    public static final DataGroups.Param<Integer> FORCE_FIELD_BASE_RADIUS =
            FORCE_FIELD.integer("base_radius", 16);
    public static final DataGroups.Param<Integer> FORCE_FIELD_RADIUS_UPGRADE =
            FORCE_FIELD.integer("radius_upgrade", 16);
    public static final DataGroups.Param<Integer> FORCE_FIELD_SHIELD_UPGRADE =
            FORCE_FIELD.integer("shield_upgrade", 50);
    public static final DataGroups.Param<Double> FORCE_FIELD_COOLDOWN_MODIFIER =
            FORCE_FIELD.real("cooldown_modifier", 1D);
    public static final DataGroups.Param<Double> FORCE_FIELD_HEALTH_REGEN_MODIFIER =
            FORCE_FIELD.real("health_regen_modifier", 1D);

    private static final DataGroups.Group CRUCIBLE = DataGroups.group(GROUPS, "crucible");
    public static final DataGroups.Param<Integer> CRUCIBLE_RECIPE_CAPACITY =
            CRUCIBLE.integer("recipe_capacity", MaterialShapes.BLOCK.q(16));
    public static final DataGroups.Param<Integer> CRUCIBLE_WASTE_CAPACITY =
            CRUCIBLE.integer("waste_capacity", MaterialShapes.BLOCK.q(16));
    public static final DataGroups.Param<Integer> CRUCIBLE_PROCESS_TIME =
            CRUCIBLE.integer("processing_heat", 20_000);
    public static final DataGroups.Param<Double> CRUCIBLE_DIFFUSION =
            CRUCIBLE.real("diffusion", 0.25D);
    public static final DataGroups.Param<Integer> CRUCIBLE_MAX_HEAT =
            CRUCIBLE.integer("heat_capacity", 100_000);

    private static final DataGroups.Group CENTRIFUGE = DataGroups.group(GROUPS, "centrifuge");
    public static final DataGroups.Param<Integer> CENTRIFUGE_MAX_POWER =
            CENTRIFUGE.integer("power_capacity", 100_000);
    public static final DataGroups.Param<Integer> CENTRIFUGE_PROCESS_TIME =
            CENTRIFUGE.positive("processing_time", 200);
    public static final DataGroups.Param<Integer> CENTRIFUGE_CONSUMPTION =
            CENTRIFUGE.integer("power_consumption", 200);

    private static final DataGroups.Group DIESEL = DataGroups.group(GROUPS, "dieselgen");
    public static final DataGroups.Param<Long> DIESEL_MAX_POWER =
            DIESEL.longValue("power_capacity", 50_000L);
    public static final DataGroups.Param<Integer> DIESEL_FUEL_CAP =
            DIESEL.integer("fuel_capacity", 16_000);
    public static final DataGroups.Param<List<Double>> DIESEL_EFFICIENCY =
            DIESEL.fuelGrades("efficiency", List.of(0.0D, 0.5D, 0.75D, 0.1D, 0.0D));

    private static final DataGroups.Group RADAR = DataGroups.group(GROUPS, "radar");
    public static final DataGroups.Param<Long> RADAR_MAX_POWER =
            RADAR.longValue("power_capacity", 100_000L);
    public static final DataGroups.Param<Long> RADAR_CONSUMPTION =
            RADAR.longValue("power_consumption", 500L);
    public static final DataGroups.Param<Integer> RADAR_RANGE = RADAR.integer("radar_range", 1_000);
    public static final DataGroups.Param<Integer> RADAR_BUFFER = RADAR.integer("radar_buffer", 30);
    public static final DataGroups.Param<Integer> RADAR_ALTITUDE =
            RADAR.signed("radar_altitude", 55);
    public static final DataGroups.Param<Integer> RADAR_CHUNK_LOAD_CAP =
            RADAR.integer("chunk_load_cap", 10);
    public static final DataGroups.Param<Boolean> RADAR_GENERATE_CHUNKS =
            RADAR.bool("generate_chunks", false);

    private static final DataGroups.Group ICF_CONTROLLER = DataGroups.group(GROUPS, "icfLaser");
    public static final DataGroups.Param<Integer> ICF_CAPACITOR_POWER =
            ICF_CONTROLLER.integer("capacitor_power", 2_500_000);
    public static final DataGroups.Param<Integer> ICF_TURBO_POWER =
            ICF_CONTROLLER.integer("turbo_power", 5_000_000);

    private static final DataGroups.Group RADAR_LARGE = DataGroups.group(GROUPS, "radar_large");
    public static final DataGroups.Param<Integer> RADAR_LARGE_RANGE =
            RADAR_LARGE.integer("radar_large_range", 3_000);

    private static final DataGroups.Group HEATER_OVEN = DataGroups.group(GROUPS, "heatingoven");
    public static final DataGroups.Param<Integer> HEATER_OVEN_BASE_HEAT =
            HEATER_OVEN.integer("base_heat", 500);
    public static final DataGroups.Param<Double> HEATER_OVEN_TIME_MULT =
            HEATER_OVEN.real("burn_time_multiplier", 0.125D);
    public static final DataGroups.Param<Double> HEATER_OVEN_HEAT_EFF =
            HEATER_OVEN.real("heat_pull_efficiency", 0.5D);
    public static final DataGroups.Param<Integer> HEATER_OVEN_MAX_HEAT_ENERGY =
            HEATER_OVEN.integer("heat_capacity", 500_000);

    private static final DataGroups.Group FIREBOX = DataGroups.group(GROUPS, "firebox");
    public static final DataGroups.Param<Integer> FIREBOX_BASE_HEAT =
            FIREBOX.integer("base_heat", 100);
    public static final DataGroups.Param<Double> FIREBOX_TIME_MULT =
            FIREBOX.real("burn_time_multiplier", 1D);
    public static final DataGroups.Param<Integer> FIREBOX_MAX_HEAT_ENERGY =
            FIREBOX.integer("heat_capacity", 100_000);

    private static final DataGroups.Group PUMP = DataGroups.group(GROUPS, "waterpump");
    public static final DataGroups.Param<Integer> PUMP_GROUND_HEIGHT =
            PUMP.signed("ground_height", 70);
    public static final DataGroups.Param<Integer> PUMP_GROUND_DEPTH =
            PUMP.integer("ground_depth", 4);
    public static final DataGroups.Param<Integer> PUMP_STEAM_SPEED =
            PUMP.integer("steam_speed", 1_000);
    public static final DataGroups.Param<Integer> PUMP_ELECTRIC_SPEED =
            PUMP.integer("electric_speed", 10_000);

    private static final DataGroups.Group BOILER = DataGroups.group(GROUPS, "boiler");
    public static final DataGroups.Param<Integer> BOILER_MAX_HEAT =
            BOILER.integer("heat_capacity", 3_200_000);
    public static final DataGroups.Param<Double> BOILER_DIFFUSION = BOILER.real("diffusion", 0.1D);
    public static final DataGroups.Param<Boolean> BOILER_CAN_EXPLODE =
            BOILER.bool("can_explode", true);

    private static final DataGroups.Group BOILER_INDUSTRIAL =
            DataGroups.group(GROUPS, "boilerIndustrial");
    public static final DataGroups.Param<Integer> BOILER_INDUSTRIAL_MAX_HEAT =
            BOILER_INDUSTRIAL.integer("heat_capacity", 12_800_000);
    public static final DataGroups.Param<Double> BOILER_INDUSTRIAL_DIFFUSION =
            BOILER_INDUSTRIAL.real("diffusion", 0.1D);

    private static final DataGroups.Group INDUSTRIAL_TURBINE =
            DataGroups.group(GROUPS, "steamturbineIndustrialMk2");
    public static final DataGroups.Param<Integer> INDUSTRIAL_TURBINE_INPUT_TANK_SIZE =
            INDUSTRIAL_TURBINE.integer("input_tank_capacity", 750_000);
    public static final DataGroups.Param<Integer> INDUSTRIAL_TURBINE_OUTPUT_TANK_SIZE =
            INDUSTRIAL_TURBINE.integer("output_tank_capacity", 3_000_000);
    public static final DataGroups.Param<Double> INDUSTRIAL_TURBINE_EFFICIENCY =
            INDUSTRIAL_TURBINE.real("efficiency", 1D);

    private static final DataGroups.Group STIRLING = DataGroups.group(GROUPS, "stirling");
    public static final DataGroups.Param<Double> STIRLING_DIFFUSION =
            STIRLING.real("diffusion", 0.1D);
    public static final DataGroups.Param<Double> STIRLING_EFFICIENCY =
            STIRLING.real("efficiency", 0.5D);
    public static final DataGroups.Param<Integer> STIRLING_OVERSPEED_LIMIT =
            STIRLING.integer("overspeed_limit", 300);

    private static final DataGroups.Group TOWER_SMALL =
            DataGroups.group(GROUPS, "condenserTowerSmall");
    public static final DataGroups.Param<Integer> TOWER_SMALL_INPUT_TANK_SIZE =
            TOWER_SMALL.integer("input_tank_capacity", 1_000);
    public static final DataGroups.Param<Integer> TOWER_SMALL_OUTPUT_TANK_SIZE =
            TOWER_SMALL.integer("output_tank_capacity", 1_000);

    private static final DataGroups.Group TOWER_LARGE =
            DataGroups.group(GROUPS, "condenserTowerLarge");
    public static final DataGroups.Param<Integer> TOWER_LARGE_INPUT_TANK_SIZE =
            TOWER_LARGE.integer("input_tank_capacity", 10_000);
    public static final DataGroups.Param<Integer> TOWER_LARGE_OUTPUT_TANK_SIZE =
            TOWER_LARGE.integer("output_tank_capacity", 10_000);

    private static final DataGroups.Group STEAM_ENGINE = DataGroups.group(GROUPS, "steamengine");
    public static final DataGroups.Param<Integer> STEAM_ENGINE_STEAM_CAP =
            STEAM_ENGINE.integer("steam_capacity", 2_000);
    public static final DataGroups.Param<Integer> STEAM_ENGINE_LDS_CAP =
            STEAM_ENGINE.integer("low_density_steam_capacity", 20);
    public static final DataGroups.Param<Double> STEAM_ENGINE_EFFICIENCY =
            STEAM_ENGINE.real("efficiency", 0.85D);

    private static final DataGroups.Group TURBINE = DataGroups.group(GROUPS, "steamturbine");
    public static final DataGroups.Param<Long> TURBINE_MAX_POWER =
            TURBINE.longValue("power_capacity", 1_000_000L);
    public static final DataGroups.Param<Integer> TURBINE_INPUT_TANK_SIZE =
            TURBINE.integer("input_tank_capacity", 64_000);
    public static final DataGroups.Param<Integer> TURBINE_OUTPUT_TANK_SIZE =
            TURBINE.integer("output_tank_capacity", 128_000);
    public static final DataGroups.Param<Integer> TURBINE_MAX_STEAM_PER_TICK =
            TURBINE.integer("maximum_steam_per_tick", 6_000);
    public static final DataGroups.Param<Double> TURBINE_EFFICIENCY =
            TURBINE.real("efficiency", 0.85D);

    private static final DataGroups.Group PUMPJACK = DataGroups.group(GROUPS, "pumpjack");
    public static final DataGroups.Param<Integer> PUMPJACK_MAX_POWER =
            PUMPJACK.integer("power_capacity", 250_000);
    public static final DataGroups.Param<Integer> PUMPJACK_CONSUMPTION =
            PUMPJACK.integer("power_consumption", 200);
    public static final DataGroups.Param<Integer> PUMPJACK_DELAY = PUMPJACK.positive("delay", 25);
    public static final DataGroups.Param<Integer> PUMPJACK_OIL_PER_DEPOSIT =
            PUMPJACK.integer("oil_per_deposit", 750);
    public static final DataGroups.Param<Integer> PUMPJACK_GAS_PER_DEPOSIT_MIN =
            PUMPJACK.integer("gas_per_deposit_min", 50);
    public static final DataGroups.Param<Integer> PUMPJACK_GAS_PER_DEPOSIT_MAX =
            PUMPJACK.integer("gas_per_deposit_max", 250);
    public static final DataGroups.Param<Double> PUMPJACK_DRAIN_CHANCE =
            PUMPJACK.chance("drain_chance", 0.025D);

    private static final DataGroups.Group DERRICK = DataGroups.group(GROUPS, "derrick");
    public static final DataGroups.Param<Integer> DERRICK_MAX_POWER =
            DERRICK.integer("power_capacity", 100_000);
    public static final DataGroups.Param<Integer> DERRICK_CONSUMPTION =
            DERRICK.integer("power_consumption", 100);
    public static final DataGroups.Param<Integer> DERRICK_DELAY = DERRICK.positive("delay", 50);
    public static final DataGroups.Param<Integer> DERRICK_OIL_PER_DEPOSIT =
            DERRICK.integer("oil_per_deposit", 500);
    public static final DataGroups.Param<Integer> DERRICK_GAS_PER_DEPOSIT_MIN =
            DERRICK.integer("gas_per_deposit_min", 100);
    public static final DataGroups.Param<Integer> DERRICK_GAS_PER_DEPOSIT_MAX =
            DERRICK.integer("gas_per_deposit_max", 500);
    public static final DataGroups.Param<Double> DERRICK_DRAIN_CHANCE =
            DERRICK.chance("drain_chance", 0.05D);

    private static final DataGroups.Group FRACKING_TOWER =
            DataGroups.group(GROUPS, "frackingtower");
    public static final DataGroups.Param<Integer> FRACKING_MAX_POWER =
            FRACKING_TOWER.integer("power_capacity", 5_000_000);
    public static final DataGroups.Param<Integer> FRACKING_CONSUMPTION =
            FRACKING_TOWER.integer("power_consumption", 5_000);
    public static final DataGroups.Param<Integer> FRACKING_SOLUTION_REQUIRED =
            FRACKING_TOWER.integer("solution_required", 10);
    public static final DataGroups.Param<Integer> FRACKING_DELAY =
            FRACKING_TOWER.positive("delay", 20);
    public static final DataGroups.Param<Integer> FRACKING_OIL_PER_DEPOSIT =
            FRACKING_TOWER.integer("oil_per_deposit", 1_000);
    public static final DataGroups.Param<Integer> FRACKING_GAS_PER_DEPOSIT_MIN =
            FRACKING_TOWER.integer("gas_per_deposit_min", 100);
    public static final DataGroups.Param<Integer> FRACKING_GAS_PER_DEPOSIT_MAX =
            FRACKING_TOWER.integer("gas_per_deposit_max", 500);
    public static final DataGroups.Param<Double> FRACKING_DRAIN_CHANCE =
            FRACKING_TOWER.chance("drain_chance", 0.02D);
    public static final DataGroups.Param<Integer> FRACKING_OIL_PER_BEDROCK_DEPOSIT =
            FRACKING_TOWER.integer("oil_per_bedrock_deposit", 100);
    public static final DataGroups.Param<Integer> FRACKING_GAS_PER_BEDROCK_DEPOSIT_MIN =
            FRACKING_TOWER.integer("gas_per_bedrock_deposit_min", 10);
    public static final DataGroups.Param<Integer> FRACKING_GAS_PER_BEDROCK_DEPOSIT_MAX =
            FRACKING_TOWER.integer("gas_per_bedrock_deposit_max", 50);
    public static final DataGroups.Param<Integer> FRACKING_DESTRUCTION_RANGE =
            FRACKING_TOWER.integer("destruction_range", 75);

    private static final DataGroups.Group MHD_TURBINE = DataGroups.group(GROUPS, "mhd-turbine");
    public static final DataGroups.Param<Long> MHD_TURBINE_MINIMUM_PLASMA =
            MHD_TURBINE.longValue("minimum_plasma", 5_000_000L);

    private static final DataGroups.Group RBMK = DataGroups.group(GROUPS, "rbmk");
    public static final DataGroups.Param<Double> RBMK_PASSIVE_COOLING =
            RBMK.real("passive_cooling", 2.5D);
    public static final DataGroups.Param<Double> RBMK_PASSIVE_COOLING_INNER =
            RBMK.real("passive_cooling_inner", 0.1D);
    public static final DataGroups.Param<Double> RBMK_COLUMN_HEAT_FLOW =
            RBMK.chance("column_heat_flow", 0.2D);
    public static final DataGroups.Param<Double> RBMK_FUEL_DIFFUSION_MOD =
            RBMK.real("diffusion_mod", 1D);
    public static final DataGroups.Param<Double> RBMK_HEAT_PROVISION =
            RBMK.chance("heat_provision", 0.2D);
    public static final DataGroups.Param<Boolean> RBMK_PERMANENT_SCRAP =
            RBMK.bool("permanent_scrap", true);
    public static final DataGroups.Param<Double> RBMK_BOILER_HEAT_CONSUMPTION =
            RBMK.real("boiler_heat_consumption", 0.1D);
    public static final DataGroups.Param<Double> RBMK_CONTROL_SPEED =
            RBMK.real("control_speed", 1D);
    public static final DataGroups.Param<Double> RBMK_REACTIVITY_MOD =
            RBMK.real("reactivity_mod", 1D);
    public static final DataGroups.Param<Double> RBMK_OUTGASSER_MOD =
            RBMK.real("outgasser_speed_mod", 1D);
    public static final DataGroups.Param<Double> RBMK_SURGE_MOD =
            RBMK.real("control_surge_mod", 1D);
    public static final DataGroups.Param<Integer> RBMK_FLUX_RANGE =
            RBMK.add("flux_range", 5, DataCodecs.intRange(1, 100));
    public static final DataGroups.Param<Boolean> RBMK_REASIM_BOILERS =
            RBMK.bool("reasim_boilers", false);
    public static final DataGroups.Param<Double> RBMK_REASIM_BOILER_SPEED =
            RBMK.chance("reasim_boiler_speed", 0.05D);
    public static final DataGroups.Param<Boolean> RBMK_DISABLE_MELTDOWNS =
            RBMK.bool("disable_meltdowns", false);
    public static final DataGroups.Param<Boolean> RBMK_ENABLE_MELTDOWN_OVERPRESSURE =
            RBMK.bool("enable_meltdown_overpressure", false);
    public static final DataGroups.Param<Double> RBMK_MODERATOR_EFFICIENCY =
            RBMK.chance("moderator_efficiency", 1D);
    public static final DataGroups.Param<Double> RBMK_ABSORBER_EFFICIENCY =
            RBMK.chance("absorber_efficiency", 1D);
    public static final DataGroups.Param<Double> RBMK_REFLECTOR_EFFICIENCY =
            RBMK.chance("reflector_efficiency", 1D);
    public static final DataGroups.Param<Boolean> RBMK_DISABLE_DEPLETION =
            RBMK.bool("disable_depletion", false);
    public static final DataGroups.Param<Boolean> RBMK_DISABLE_XENON =
            RBMK.bool("disable_xenon", false);
    public static final DataGroups.Param<Double> RBMK_ABSORBER_HEAT_CONVERSION =
            RBMK.chance("absorber_heat_conversion", 0.05D);
    public static final DataGroups.Param<Integer> RBMK_COLUMN_HEIGHT =
            RBMK.bounded("column_height", 4, 2, 16);

    private static final DataGroups.Group MACHINES = DataGroups.group(GROUPS, "machines");
    public static final DataGroups.Param<Boolean> ENABLE_MACHINE_GRAVITY =
            MACHINES.bool("gravity", false);
    public static final DataGroups.Param<Integer> AUTOCAL_MAX_CLOCK =
            MACHINES.signed("autocal_max_clock", 20);

    static {
        PUMPJACK.randomRange(PUMPJACK_GAS_PER_DEPOSIT_MIN, PUMPJACK_GAS_PER_DEPOSIT_MAX);
        DERRICK.randomRange(DERRICK_GAS_PER_DEPOSIT_MIN, DERRICK_GAS_PER_DEPOSIT_MAX);
        FRACKING_TOWER.randomRange(FRACKING_GAS_PER_DEPOSIT_MIN, FRACKING_GAS_PER_DEPOSIT_MAX);
        FRACKING_TOWER.randomRange(
                FRACKING_GAS_PER_BEDROCK_DEPOSIT_MIN, FRACKING_GAS_PER_BEDROCK_DEPOSIT_MAX);
    }

    private MachineData() {}

    public static List<DataGroups.Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static void applyDataPack(RegistryAccess registries) {
        DataGroups.apply(registries, REGISTRY, GROUPS);
        RBMKConfig.publishColumnHeight();
    }
}
