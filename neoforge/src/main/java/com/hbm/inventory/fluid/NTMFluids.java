// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class NTMFluids {

    public static final Fluid NONE = Fluids.EMPTY;
    private static final Map<String, Supplier<? extends Fluid>> ALIASES =
            Map.of(
                    "WATER", () -> Fluids.WATER,
                    "LAVA", () -> Fluids.LAVA);
    private static final Map<String, RegistryHandle<Fluid>> HANDLES = new LinkedHashMap<>();
    private static final Map<String, Fluid> BY_NAME = new LinkedHashMap<>();
    private static final Map<Fluid, String> NAME_OF = new IdentityHashMap<>();
    private static final Map<Fluid, Integer> LEGACY_ID = new IdentityHashMap<>();

    private static final Map<String, String> LEGACY_NAMES =
            Map.of("BALEFIRE_FUEL", "BALEFIRE", "LIQUID_CONCRETE", "CONCRETE", "WATZ_MUD", "WATZ");

    private static final String[] LEGACY_ID_ORDER = {
        "NONE",
        "WATER",
        "STEAM",
        "HOTSTEAM",
        "SUPERHOTSTEAM",
        "ULTRAHOTSTEAM",
        "COOLANT",
        "LAVA",
        "DEUTERIUM",
        "TRITIUM",
        "OIL",
        "HOTOIL",
        "HEAVYOIL",
        "BITUMEN",
        "SMEAR",
        "HEATINGOIL",
        "RECLAIMED",
        "PETROIL",
        "LUBRICANT",
        "NAPHTHA",
        "DIESEL",
        "LIGHTOIL",
        "KEROSENE",
        "GAS",
        "PETROLEUM",
        "LPG",
        "BIOGAS",
        "BIOFUEL",
        "NITAN",
        "UF6",
        "PUF6",
        "SAS3",
        "SCHRABIDIC",
        "AMAT",
        "ASCHRAB",
        "PEROXIDE",
        "WATZ_MUD",
        "CRYOGEL",
        "HYDROGEN",
        "OXYGEN",
        "XENON",
        "BALEFIRE_FUEL",
        "MERCURY",
        "PAIN",
        "WASTEFLUID",
        "WASTEGAS",
        "GASOLINE",
        "COALGAS",
        "SPENTSTEAM",
        "FRACKSOL",
        "PLASMA_DT",
        "PLASMA_HD",
        "PLASMA_HT",
        "PLASMA_XM",
        "PLASMA_BF",
        "CARBONDIOXIDE",
        "PLASMA_DH3",
        "HELIUM3",
        "DEATH",
        "ETHANOL",
        "HEAVYWATER",
        "CRACKOIL",
        "COALOIL",
        "HOTCRACKOIL",
        "NAPHTHA_CRACK",
        "LIGHTOIL_CRACK",
        "DIESEL_CRACK",
        "AROMATICS",
        "UNSATURATEDS",
        "SALIENT",
        "XPJUICE",
        "ENDERJUICE",
        "PETROIL_LEADED",
        "GASOLINE_LEADED",
        "COALGAS_LEADED",
        "SULFURIC_ACID",
        "COOLANT_HOT",
        "MUG",
        "MUG_HOT",
        "WOODOIL",
        "COALCREOSOTE",
        "SEEDSLURRY",
        "NITRIC_ACID",
        "SOLVENT",
        "BLOOD",
        "BLOOD_HOT",
        "SYNGAS",
        "OXYHYDROGEN",
        "RADIOSOLVENT",
        "CHLORINE",
        "HEAVYOIL_VACUUM",
        "REFORMATE",
        "LIGHTOIL_VACUUM",
        "SOURGAS",
        "XYLENE",
        "HEATINGOIL_VACUUM",
        "DIESEL_REFORM",
        "DIESEL_CRACK_REFORM",
        "KEROSENE_REFORM",
        "REFORMGAS",
        "COLLOID",
        "PHOSGENE",
        "MUSTARDGAS",
        "IONGEL",
        "OIL_COKER",
        "NAPHTHA_COKER",
        "GAS_COKER",
        "EGG",
        "CHOLESTEROL",
        "ESTRADIOL",
        "FISHOIL",
        "SUNFLOWEROIL",
        "NITROGLYCERIN",
        "REDMUD",
        "CHLOROCALCITE_SOLUTION",
        "CHLOROCALCITE_MIX",
        "CHLOROCALCITE_CLEANED",
        "POTASSIUM_CHLORIDE",
        "CALCIUM_CHLORIDE",
        "CALCIUM_SOLUTION",
        "SMOKE",
        "SMOKE_LEADED",
        "SMOKE_POISON",
        "HELIUM4",
        "HEAVYWATER_HOT",
        "SODIUM",
        "SODIUM_HOT",
        "THORIUM_SALT",
        "THORIUM_SALT_HOT",
        "THORIUM_SALT_DEPLETED",
        "FULLERENE",
        "PHEROMONE",
        "PHEROMONE_M",
        "OIL_DS",
        "HOTOIL_DS",
        "CRACKOIL_DS",
        "HOTCRACKOIL_DS",
        "NAPHTHA_DS",
        "LIGHTOIL_DS",
        "STELLAR_FLUX",
        "VITRIOL",
        "SLOP",
        "LEAD",
        "LEAD_HOT",
        "PERFLUOROMETHYL",
        "PERFLUOROMETHYL_COLD",
        "PERFLUOROMETHYL_HOT",
        "LYE",
        "SODIUM_ALUMINATE",
        "BAUXITE_SOLUTION",
        "ALUMINA",
        "AIR",
        "LIQUID_CONCRETE",
        "DHC",
        "AIRBLAST",
        "FLUE"
    };
    private static final String[] DISPLAY_ORDER = {
        "AIR", "AIRBLAST", "WATER", "HEAVYWATER",
        "HEAVYWATER_HOT", "LAVA", "STEAM", "HOTSTEAM",
        "SUPERHOTSTEAM", "ULTRAHOTSTEAM", "SPENTSTEAM", "CARBONDIOXIDE",
        "COOLANT", "COOLANT_HOT", "PERFLUOROMETHYL", "PERFLUOROMETHYL_COLD",
        "PERFLUOROMETHYL_HOT", "CRYOGEL", "MUG", "MUG_HOT",
        "BLOOD", "BLOOD_HOT", "SODIUM", "SODIUM_HOT",
        "LEAD", "LEAD_HOT", "THORIUM_SALT", "THORIUM_SALT_HOT",
        "THORIUM_SALT_DEPLETED", "HYDROGEN", "DEUTERIUM", "TRITIUM",
        "HELIUM3", "HELIUM4", "OXYGEN", "XENON",
        "CHLORINE", "MERCURY", "OIL", "OIL_DS",
        "CRACKOIL", "CRACKOIL_DS", "COALOIL", "OIL_COKER",
        "HOTOIL", "HOTOIL_DS", "HOTCRACKOIL", "HOTCRACKOIL_DS",
        "HEAVYOIL", "HEAVYOIL_VACUUM", "NAPHTHA", "NAPHTHA_DS",
        "NAPHTHA_CRACK", "NAPHTHA_COKER", "REFORMATE", "LIGHTOIL",
        "LIGHTOIL_DS", "LIGHTOIL_CRACK", "LIGHTOIL_VACUUM", "BITUMEN",
        "SMEAR", "HEATINGOIL", "HEATINGOIL_VACUUM", "RECLAIMED",
        "LUBRICANT", "FLUE", "GAS", "GAS_COKER",
        "PETROLEUM", "SOURGAS", "LPG", "SYNGAS",
        "OXYHYDROGEN", "AROMATICS", "UNSATURATEDS", "XYLENE",
        "REFORMGAS", "DIESEL", "DIESEL_REFORM", "DIESEL_CRACK",
        "DIESEL_CRACK_REFORM", "KEROSENE", "KEROSENE_REFORM", "PETROIL",
        "PETROIL_LEADED", "GASOLINE", "GASOLINE_LEADED", "COALGAS",
        "COALGAS_LEADED", "COALCREOSOTE", "WOODOIL", "BIOGAS",
        "BIOFUEL", "ETHANOL", "FISHOIL", "SUNFLOWEROIL",
        "NITAN", "DHC", "BALEFIRE_FUEL", "SALIENT",
        "SEEDSLURRY", "COLLOID", "VITRIOL", "SLOP",
        "IONGEL", "PEROXIDE", "SULFURIC_ACID", "NITRIC_ACID",
        "SOLVENT", "RADIOSOLVENT", "SCHRABIDIC", "UF6",
        "PUF6", "SAS3", "PAIN", "DEATH",
        "WATZ_MUD", "REDMUD", "FULLERENE", "EGG",
        "CHOLESTEROL", "CHLOROCALCITE_SOLUTION", "CHLOROCALCITE_MIX", "CHLOROCALCITE_CLEANED",
        "POTASSIUM_CHLORIDE", "CALCIUM_CHLORIDE", "CALCIUM_SOLUTION", "SODIUM_ALUMINATE",
        "BAUXITE_SOLUTION", "ALUMINA", "LIQUID_CONCRETE", "FRACKSOL",
        "LYE", "PHOSGENE", "MUSTARDGAS", "ESTRADIOL",
        "NITROGLYCERIN", "AMAT", "ASCHRAB", "WASTEFLUID",
        "WASTEGAS", "XPJUICE", "ENDERJUICE", "STELLAR_FLUX",
        "PLASMA_DT", "PLASMA_HD", "PLASMA_HT", "PLASMA_DH3",
        "PLASMA_XM", "PLASMA_BF", "SMOKE", "SMOKE_LEADED",
        "SMOKE_POISON", "PHEROMONE", "PHEROMONE_M"
    };

    public static Fluid WATER;
    public static Fluid LAVA;
    public static Fluid STEAM;
    public static Fluid HOTSTEAM;
    public static Fluid SUPERHOTSTEAM;
    public static Fluid ULTRAHOTSTEAM;
    public static Fluid SPENTSTEAM;
    public static Fluid COOLANT;
    public static Fluid COOLANT_HOT;
    public static Fluid CRYOGEL;
    public static Fluid PERFLUOROMETHYL;
    public static Fluid PERFLUOROMETHYL_COLD;
    public static Fluid PERFLUOROMETHYL_HOT;
    public static Fluid HEAVYWATER;
    public static Fluid HEAVYWATER_HOT;
    public static Fluid MUG;
    public static Fluid MUG_HOT;
    public static Fluid BLOOD;
    public static Fluid BLOOD_HOT;
    public static Fluid SODIUM;
    public static Fluid SODIUM_HOT;
    public static Fluid LEAD;
    public static Fluid LEAD_HOT;
    public static Fluid THORIUM_SALT;
    public static Fluid THORIUM_SALT_HOT;
    public static Fluid THORIUM_SALT_DEPLETED;
    public static Fluid HYDROGEN;
    public static Fluid DEUTERIUM;
    public static Fluid TRITIUM;
    public static Fluid HELIUM3;
    public static Fluid HELIUM4;
    public static Fluid OXYGEN;
    public static Fluid XENON;
    public static Fluid CHLORINE;
    public static Fluid MERCURY;
    public static Fluid CARBONDIOXIDE;
    public static Fluid AIR;
    public static Fluid AIRBLAST;
    public static Fluid DHC;
    public static Fluid OIL;
    public static Fluid OIL_DS;
    public static Fluid CRACKOIL;
    public static Fluid CRACKOIL_DS;
    public static Fluid COALOIL;
    public static Fluid OIL_COKER;
    public static Fluid HOTOIL;
    public static Fluid HOTOIL_DS;
    public static Fluid HOTCRACKOIL;
    public static Fluid HOTCRACKOIL_DS;
    public static Fluid HEAVYOIL;
    public static Fluid HEAVYOIL_VACUUM;
    public static Fluid NAPHTHA;
    public static Fluid NAPHTHA_DS;
    public static Fluid NAPHTHA_CRACK;
    public static Fluid NAPHTHA_COKER;
    public static Fluid REFORMATE;
    public static Fluid LIGHTOIL;
    public static Fluid LIGHTOIL_DS;
    public static Fluid LIGHTOIL_CRACK;
    public static Fluid LIGHTOIL_VACUUM;
    public static Fluid BITUMEN;
    public static Fluid SMEAR;
    public static Fluid HEATINGOIL;
    public static Fluid HEATINGOIL_VACUUM;
    public static Fluid RECLAIMED;
    public static Fluid LUBRICANT;
    public static Fluid FLUE;
    public static Fluid GAS;
    public static Fluid GAS_COKER;
    public static Fluid PETROLEUM;
    public static Fluid SOURGAS;
    public static Fluid LPG;
    public static Fluid SYNGAS;
    public static Fluid OXYHYDROGEN;
    public static Fluid AROMATICS;
    public static Fluid UNSATURATEDS;
    public static Fluid XYLENE;
    public static Fluid REFORMGAS;
    public static Fluid DIESEL;
    public static Fluid DIESEL_REFORM;
    public static Fluid DIESEL_CRACK;
    public static Fluid DIESEL_CRACK_REFORM;
    public static Fluid KEROSENE;
    public static Fluid KEROSENE_REFORM;
    public static Fluid PETROIL;
    public static Fluid PETROIL_LEADED;
    public static Fluid GASOLINE;
    public static Fluid GASOLINE_LEADED;
    public static Fluid COALGAS;
    public static Fluid COALGAS_LEADED;
    public static Fluid COALCREOSOTE;
    public static Fluid WOODOIL;
    public static Fluid BIOGAS;
    public static Fluid BIOFUEL;
    public static Fluid ETHANOL;
    public static Fluid FISHOIL;
    public static Fluid SUNFLOWEROIL;
    public static Fluid NITAN;
    public static Fluid BALEFIRE_FUEL;
    public static Fluid SALIENT;
    public static Fluid SEEDSLURRY;
    public static Fluid COLLOID;
    public static Fluid VITRIOL;
    public static Fluid SLOP;
    public static Fluid IONGEL;
    public static Fluid PEROXIDE;
    public static Fluid SULFURIC_ACID;
    public static Fluid NITRIC_ACID;
    public static Fluid SOLVENT;
    public static Fluid RADIOSOLVENT;
    public static Fluid LYE;
    public static Fluid SODIUM_ALUMINATE;
    public static Fluid BAUXITE_SOLUTION;
    public static Fluid ALUMINA;
    public static Fluid LIQUID_CONCRETE;
    public static Fluid NITROGLYCERIN;
    public static Fluid FULLERENE;
    public static Fluid REDMUD;
    public static Fluid EGG;
    public static Fluid CHOLESTEROL;
    public static Fluid ESTRADIOL;
    public static Fluid CHLOROCALCITE_SOLUTION;
    public static Fluid CHLOROCALCITE_MIX;
    public static Fluid CHLOROCALCITE_CLEANED;
    public static Fluid POTASSIUM_CHLORIDE;
    public static Fluid CALCIUM_CHLORIDE;
    public static Fluid CALCIUM_SOLUTION;
    public static Fluid SCHRABIDIC;
    public static Fluid UF6;
    public static Fluid PUF6;
    public static Fluid SAS3;
    public static Fluid PAIN;
    public static Fluid DEATH;
    public static Fluid WATZ_MUD;
    public static Fluid WASTEFLUID;
    public static Fluid WASTEGAS;
    public static Fluid FRACKSOL;
    public static Fluid AMAT;
    public static Fluid ASCHRAB;
    public static Fluid STELLAR_FLUX;
    public static Fluid PLASMA_DT;
    public static Fluid PLASMA_HD;
    public static Fluid PLASMA_HT;
    public static Fluid PLASMA_DH3;
    public static Fluid PLASMA_XM;
    public static Fluid PLASMA_BF;
    public static Fluid SMOKE;
    public static Fluid SMOKE_LEADED;
    public static Fluid SMOKE_POISON;
    public static Fluid PHEROMONE;
    public static Fluid PHEROMONE_M;
    public static Fluid XPJUICE;
    public static Fluid ENDERJUICE;
    public static Fluid PHOSGENE;
    public static Fluid MUSTARDGAS;
    private static List<Fluid> displayOrder = List.of();

    private NTMFluids() {}

    private static void register(String name) {
        Supplier<? extends Fluid> alias = ALIASES.get(name);
        if (alias != null) {
            BY_NAME.put(name, alias.get());
            return;
        }
        HANDLES.put(name, Services.REGISTRAR.registerFluid(name.toLowerCase(Locale.US)));
    }

    public static Fluid byName(String name) {
        Fluid fluid = BY_NAME.get(name);
        if (fluid == null) throw new IllegalStateException("no fluid is registered under " + name);
        return fluid;
    }

    public static String legacyName(@Nullable Fluid fluid) {
        return fluid == null || fluid == NONE ? "NONE" : NAME_OF.get(fluid);
    }

    public static String spritePath(Fluid fluid) {
        return legacyName(fluid).toLowerCase(Locale.US);
    }

    public static int legacyId(@Nullable Fluid fluid) {
        return fluid == null || fluid == NONE ? 0 : LEGACY_ID.get(fluid);
    }

    public static Fluid byLegacyId(int id) {
        return byName(LEGACY_ID_ORDER[id]);
    }

    public static int legacyIdCount() {
        return LEGACY_ID_ORDER.length;
    }

    public static List<Fluid> displayOrder() {
        return displayOrder;
    }

    public static void init() {
        for (String name : DISPLAY_ORDER) register(name);
    }

    public static void bake() {
        HANDLES.forEach((name, handle) -> BY_NAME.put(name, handle.get()));
        BY_NAME.forEach((name, fluid) -> NAME_OF.put(fluid, LEGACY_NAMES.getOrDefault(name, name)));
        for (int id = 1; id < LEGACY_ID_ORDER.length; id++)
            LEGACY_ID.put(byName(LEGACY_ID_ORDER[id]), id);
        bindFields();
        displayOrder = resolveDisplayOrder();
    }

    private static void bindFields() {
        Set<String> listed = new LinkedHashSet<>(Arrays.asList(DISPLAY_ORDER));
        for (Field field : NTMFluids.class.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (field.getType() != Fluid.class
                    || !Modifier.isStatic(mods)
                    || Modifier.isFinal(mods)) continue;
            if (!listed.remove(field.getName())) {
                throw new IllegalStateException(
                        field.getName()
                                + " is a fluid field the display order never "
                                + "names, so it would stay null and reach no list");
            }
            try {
                field.set(null, byName(field.getName()));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot bind " + field.getName(), e);
            }
        }
        if (!listed.isEmpty()) {
            throw new IllegalStateException(
                    "the display order names fluids no field holds: " + listed);
        }
    }

    private static List<Fluid> resolveDisplayOrder() {
        List<Fluid> out = new ArrayList<>(DISPLAY_ORDER.length);
        Set<Fluid> seen = new HashSet<>();
        for (String name : DISPLAY_ORDER) {
            Fluid fluid = byName(name);
            if (!seen.add(fluid))
                throw new IllegalStateException("the display order reaches " + name + " twice");
            out.add(fluid);
        }
        return List.copyOf(out);
    }
}
