// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import java.util.List;

import static com.hbm.config.ConfigEntry.*;

public final class ConfigSchema {

    public static final ConfigEntry<Integer> CONVEYOR_CRAM_MAX =
            integer(
                    Domain.RUNTIME,
                    "conveyor",
                    "cram_max",
                    25,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "Nearby conveyor objects required to trigger an anti-cram burst.");
    public static final ConfigEntry<Boolean> CONVEYOR_CRAM_EXPLODE =
            bool(
                    Domain.RUNTIME,
                    "conveyor",
                    "cram_breaks_belt",
                    true,
                    "Whether anti-cram bursts break the conveyor belt under objects older than 400 ticks.");
    public static final ConfigEntry<Boolean> ULTRA_LARP_MODE =
            bool(
                    Domain.RUNTIME,
                    "doors",
                    "ultra_larp_mode",
                    false,
                    "Require redstone or remote control to operate large doors by hand, except the water door.");

    public static final ConfigEntry<Boolean> ENABLE_BOMBER_SHORT_MODE =
            bool(
                    Domain.RUNTIME,
                    "entities",
                    "bomber_short_approach",
                    false,
                    "Has bomber planes spawn closer to their target for smaller render distances.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "enabled",
                    false,
                    "Enables Less Bullshit Mode's alternate recipes.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_FULL_SCHRAB =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "full_schrab",
                    true,
                    "When enabled, this will replace schraranium with full schrabidium ingots in the transmutator's output");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SHORTER_DECAY =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "shorter_decay",
                    true,
                    "When enabled, this will highly accelerate the speed at which nuclear waste disposal drums decay their "
                            + "contents. 60x faster than 528 mode and 5-12x faster than on normal mode.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_ARMOR_RECIPES =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_armor_recipes",
                    true,
                    "When enabled, simplifies the recipe for armor sets like starmetal or schrabidium.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_TOOL_RECIPES =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_tool_recipes",
                    true,
                    "Uses Less Bullshit Mode's simple tool recipe forms.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_CRAFTING =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_crafting",
                    true,
                    "Uses Less Bullshit Mode's simple crafting recipe forms.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_CHEMISTRY =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_chemistry",
                    true,
                    "Uses Less Bullshit Mode's simple chemical plant recipe forms.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_CENTRIFUGE =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_centrifuge",
                    true,
                    "When enabled, enhances centrifuge outputs to make rare materials more common");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_UNLOCK_ANVIL =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "unlock_anvil",
                    true,
                    "When enabled, all anvil recipes are available at tier 1");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_MEDICINE =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_medicine",
                    true,
                    "When enabled, makes some medicine recipes (like ones that require bismuth) much more affordable");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SAFE_CRATES =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "safe_crates",
                    true,
                    "When enabled, prevents crates from becoming radioactive");

    public static final ConfigEntry<Boolean> ENABLE_FOREIGN_MOD_CHANGES =
            bool(
                    Domain.CONTENT,
                    "foreign_mods",
                    "enabled",
                    true,
                    "Master switch for this mod's changes to other mods' recipes; each change also has its own entry. "
                            + "Takes effect on restart.");

    public static final ConfigEntry<Boolean> ENABLE_MEKANISM_CHANGES =
            bool(
                    Domain.CONTENT,
                    "foreign_mods",
                    "mekanism",
                    true,
                    "If enabled, will change some of Mekanism's recipes.");

    public static final ConfigEntry<Boolean> ENABLE_TECHREBORN_CHANGES =
            bool(
                    Domain.CONTENT,
                    "foreign_mods",
                    "techreborn",
                    true,
                    "If enabled, TechReborn's grinder turns a vanilla cinnabar block into one small cinnabar dust, the "
                            + "mercury this mod gets per block.");

    public static final ConfigEntry<Boolean> ENABLE_EXPENSIVE_MODE =
            bool(Domain.CONTENT, "expensive", "enabled", false, "It does what the name implies.");

    public static final ConfigEntry<Boolean> ENABLE_DEBUG_MODE =
            bool(Domain.RUNTIME, "debug", "profiling_logs", false, "Enable debugging mode");
    public static final ConfigEntry<Boolean> ENABLE_EXTENDED_LOGGING =
            bool(
                    Domain.RUNTIME,
                    "debug",
                    "extended_logging",
                    false,
                    "Logs uses of the detonator, nuclear explosions, missile launches, grenades, etc.");
    public static final ConfigEntry<Boolean> ENABLE_SKYBOXES =
            bool(
                    Domain.CLIENT,
                    "render",
                    "skyboxes",
                    true,
                    "If enabled, will try to use NTM's custom skyboxes.");
    public static final ConfigEntry<Boolean> ENABLE_528 =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "enabled",
                    false,
                    "The central toggle for 528 mode, required TRUE for most subsequent toggles to work.");
    public static final ConfigEntry<Boolean> ENABLE_528_PRESSURIZED_RECIPES =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "pressurized_recipes",
                    true,
                    "Sets some recipes to require pressurized input fluid");
    public static final ConfigEntry<Boolean> ENABLE_528_MACHINE_GRAVITY =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "machine_gravity",
                    true,
                    "Requires most large machines to have a proper foundation, or else they tilt and break.");
    public static final ConfigEntry<Boolean> ENABLE_528_FORCE_REASIM_BOILERS =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "force_reasim_boilers",
                    true,
                    "Keeps the RBMK dial for ReaSim boilers on, preventing use of non-ReaSim boiler columns and "
                            + "forcing the use of steam in-/outlets");

    public static final ConfigEntry<Integer> RAD_TICK_RATE =
            integer(
                    Domain.RUNTIME,
                    "radiation",
                    "tick_rate",
                    1,
                    1,
                    Integer.MAX_VALUE,
                    "Ticks between radiation simulation steps. Consumed once at startup.");
    public static final ConfigEntry<Integer> HAZARD_RATE =
            integer(
                    Domain.RUNTIME,
                    "radiation",
                    "hazard_rate",
                    5,
                    1,
                    Integer.MAX_VALUE,
                    "Ticks between item/inventory hazard applications.");

    public static final ConfigEntry<Integer> LIMIT_EXPLOSION_LIFESPAN =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "unloaded_lifespan_seconds",
                    0,
                    0,
                    Integer.MAX_VALUE,
                    "Seconds after which a reloaded MK3 explosion gives up. 0 = no limit");
    public static final ConfigEntry<Integer> BLAST_SPEED =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "mk3_columns_per_tick",
                    1024,
                    1,
                    Integer.MAX_VALUE,
                    "Columns per tick the MK3 spiral steps");

    public static final ConfigEntry<Integer> MK5_BLAST_TIME =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "mk5_millis_per_tick",
                    50,
                    1,
                    Integer.MAX_VALUE,
                    "Minimum amount of milliseconds per tick allocated for mk5 chunk processing");
    public static final ConfigEntry<Integer> FALLOUT_DELAY =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "fallout_delay_ticks",
                    4,
                    0,
                    Integer.MAX_VALUE,
                    "How many ticks to wait for the next fallout chunk computation");

    public static final ConfigEntry<Integer> BLAST_CHUNKS_IN_FLIGHT =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "chunks_in_flight",
                    0,
                    0,
                    65536,
                    "Chunks a blast/fallout may load concurrently. 0 = auto (cpu cores * 8, minimum 64)");
    public static final ConfigEntry<Integer> EXPLOSION_ALGORITHM =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "mk5_algorithm",
                    2,
                    0,
                    2,
                    "Configures the algorithm of mk5 explosion. 0 = Legacy, 1 = Threaded DDA, "
                            + "2 = Threaded DDA with damage accumulation.");

    public static final ConfigEntry<Integer> BOMB_MAX_THREADS =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "max_threads",
                    -1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "Bomb ForkJoinPool workers; <=0 means processors + value (default -1 = processors - 1).");

    public static final ConfigEntry<Double> FOREIGN_EXPORT_RESERVE =
            real(
                    Domain.RUNTIME,
                    "energy",
                    "foreign_export_reserve",
                    0D,
                    0D,
                    1D,
                    "Fraction of a power net's supply that foreign (FE) machines may take at normal priority, ahead of "
                            + "the leftover surplus they receive anyway. 0 keeps NTM machines strictly first.");

    public static final ConfigEntry<Boolean> WAYPOINT_DEBUG =
            bool(
                    Domain.RUNTIME,
                    "debug",
                    "waypoints",
                    false,
                    "Allows glyphid waypoints to be seen, mainly used for debugging, also useful as an aid against them");

    public static final ConfigEntry<Integer> TOOL_RECURSION_DEPTH =
            integer(
                    Domain.RUNTIME,
                    "tools",
                    "veinminer_recursion_depth",
                    1000,
                    1,
                    Integer.MAX_VALUE,
                    "Limits veinminer's recursive function. Usually not an issue, unless you're using bukkit which is especially sensitive for some reason.");

    public static final ConfigEntry<Boolean> DAMAGE_COMPATIBILITY_MODE =
            bool(Domain.RUNTIME, "damage", "compatibility_mode", false, "");
    public static final ConfigEntry<Integer> ITEM_HAZARD_DROP_TICKRATE =
            integer(
                    Domain.RUNTIME,
                    "radiation",
                    "item_hazard_drop_tickrate",
                    2,
                    1,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Boolean> UNSTABLE_RECOVERY_SWEEP =
            bool(
                    Domain.RUNTIME,
                    "radiation",
                    "unstable_recovery_sweep",
                    false,
                    "Audit every loaded container every 20 ticks, so an armed item that left a mod's opaque storage "
                            + "without notifying still detonates. Costs a walk of all loaded chunks and entities.");

    public static final ConfigEntry<Integer> TOOL_HUD_INDICATOR_X =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "tool_indicator_x",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> TOOL_HUD_INDICATOR_Y =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "tool_indicator_y",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");

    public static final ConfigEntry<Boolean> CRATE_OPEN_HELD =
            bool(
                    Domain.CLIENT,
                    "interaction",
                    "crate_opens_held",
                    true,
                    "Right-click with a held crate opens it; sneak to place it.");
    public static final ConfigEntry<Boolean> COOLING_TOWER_PARTICLES =
            bool(Domain.CLIENT, "render", "cooling_tower_particles", true, "");
    public static final ConfigEntry<Boolean> JEI_HIDE_SECRETS =
            bool(
                    Domain.CLIENT,
                    "interaction",
                    "recipe_viewer_hides_secrets",
                    true,
                    "Keep the recipe browser from offering the recipes that are meant to be found rather than "
                            + "looked up. They stay craftable.");
    public static final ConfigEntry<Boolean> GUN_MODEL_FOV =
            bool(
                    Domain.CLIENT,
                    "guns",
                    "model_fov",
                    false,
                    "Render held gun models with the game's FOV setting instead of the fixed gun FOV.");
    public static final ConfigEntry<Boolean> GUN_VISUAL_RECOIL =
            bool(Domain.CLIENT, "guns", "visual_recoil", true, "");
    public static final ConfigEntry<Double> GUN_ANIMATION_SPEED =
            real(Domain.CLIENT, "guns", "animation_speed", 1D, 0.001D, 100D, "");
    public static final ConfigEntry<Boolean> GUN_ANIMS_LEGACY =
            bool(
                    Domain.CLIENT,
                    "guns",
                    "legacy_animations",
                    false,
                    "Use the hand-authored gun animations instead of the Blender-exported sets.");
    public static final ConfigEntry<Boolean> RENDER_CABLE_HANG =
            bool(
                    Domain.CLIENT,
                    "render",
                    "cable_hang",
                    true,
                    "Draw pylon wires with sag instead of straight.");
    public static final ConfigEntry<Boolean> NUKE_HUD_FLASH =
            bool(Domain.CLIENT, "hud", "nuke_flash", true, "");
    public static final ConfigEntry<Boolean> NUKE_HUD_SHAKE =
            bool(Domain.CLIENT, "hud", "nuke_shake", true, "");
    public static final ConfigEntry<Boolean> RENDER_REBAR_SIMPLE =
            bool(
                    Domain.CLIENT,
                    "render",
                    "simple_rebar",
                    false,
                    "Draw rebar as three rods instead of twelve.");
    public static final ConfigEntry<Integer> GEIGER_OFFSET_HORIZONTAL =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "geiger_offset_horizontal",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> GEIGER_OFFSET_VERTICAL =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "geiger_offset_vertical",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");

    public static final ConfigEntry<Integer> INFO_POSITION =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "info_position",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> INFO_OFFSET_HORIZONTAL =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "info_offset_horizontal",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> INFO_OFFSET_VERTICAL =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "info_offset_vertical",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Boolean> TOOLTIP_ORE_DICT =
            bool(
                    Domain.CLIENT,
                    "hud",
                    "tooltip_ore_dict",
                    true,
                    "List an item's tags in advanced tooltips.");
    public static final ConfigEntry<Boolean> TOOLTIP_CUSTOM_NUKE =
            bool(
                    Domain.CLIENT,
                    "hud",
                    "tooltip_custom_nuke",
                    true,
                    "Show what an item adds to a custom nuke.");
    public static final ConfigEntry<Boolean> DODD_RBMK_DIAGNOSTIC =
            bool(
                    Domain.CLIENT,
                    "hud",
                    "dodd_rbmk_diagnostic",
                    true,
                    "List an RBMK column's diagnostic data while looking at it.");

    public static final List<ConfigEntry<?>> ENTRIES =
            List.of(
                    CONVEYOR_CRAM_MAX,
                    CONVEYOR_CRAM_EXPLODE,
                    ULTRA_LARP_MODE,
                    ENABLE_BOMBER_SHORT_MODE,
                    ENABLE_LBSM,
                    ENABLE_LBSM_FULL_SCHRAB,
                    ENABLE_LBSM_SHORTER_DECAY,
                    ENABLE_LBSM_SIMPLE_ARMOR_RECIPES,
                    ENABLE_LBSM_SIMPLE_TOOL_RECIPES,
                    ENABLE_LBSM_SIMPLE_CRAFTING,
                    ENABLE_LBSM_SIMPLE_CHEMISTRY,
                    ENABLE_LBSM_SIMPLE_CENTRIFUGE,
                    ENABLE_LBSM_UNLOCK_ANVIL,
                    ENABLE_LBSM_SIMPLE_MEDICINE,
                    ENABLE_LBSM_SAFE_CRATES,
                    ENABLE_FOREIGN_MOD_CHANGES,
                    ENABLE_MEKANISM_CHANGES,
                    ENABLE_TECHREBORN_CHANGES,
                    ENABLE_EXPENSIVE_MODE,
                    ENABLE_DEBUG_MODE,
                    ENABLE_EXTENDED_LOGGING,
                    ENABLE_SKYBOXES,
                    ENABLE_528,
                    ENABLE_528_PRESSURIZED_RECIPES,
                    ENABLE_528_MACHINE_GRAVITY,
                    ENABLE_528_FORCE_REASIM_BOILERS,
                    RAD_TICK_RATE,
                    HAZARD_RATE,
                    LIMIT_EXPLOSION_LIFESPAN,
                    BLAST_SPEED,
                    MK5_BLAST_TIME,
                    FALLOUT_DELAY,
                    BLAST_CHUNKS_IN_FLIGHT,
                    EXPLOSION_ALGORITHM,
                    BOMB_MAX_THREADS,
                    FOREIGN_EXPORT_RESERVE,
                    WAYPOINT_DEBUG,
                    TOOL_RECURSION_DEPTH,
                    DAMAGE_COMPATIBILITY_MODE,
                    ITEM_HAZARD_DROP_TICKRATE,
                    UNSTABLE_RECOVERY_SWEEP,
                    TOOL_HUD_INDICATOR_X,
                    TOOL_HUD_INDICATOR_Y,
                    CRATE_OPEN_HELD,
                    COOLING_TOWER_PARTICLES,
                    JEI_HIDE_SECRETS,
                    GUN_MODEL_FOV,
                    GUN_VISUAL_RECOIL,
                    GUN_ANIMATION_SPEED,
                    GUN_ANIMS_LEGACY,
                    RENDER_CABLE_HANG,
                    NUKE_HUD_FLASH,
                    NUKE_HUD_SHAKE,
                    RENDER_REBAR_SIMPLE,
                    GEIGER_OFFSET_HORIZONTAL,
                    GEIGER_OFFSET_VERTICAL,
                    INFO_POSITION,
                    INFO_OFFSET_HORIZONTAL,
                    INFO_OFFSET_VERTICAL,
                    TOOLTIP_ORE_DICT,
                    TOOLTIP_CUSTOM_NUKE,
                    DODD_RBMK_DIAGNOSTIC);

    private ConfigSchema() {}

    public static List<ConfigEntry<?>> of(Domain domain) {
        return ENTRIES.stream().filter(e -> e.domain() == domain).toList();
    }
}
