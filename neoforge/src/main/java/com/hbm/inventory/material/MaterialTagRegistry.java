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

package com.hbm.inventory.material;

import com.hbm.util.RegistryUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

public final class MaterialTagRegistry {

    public static final MaterialTags WOOD = MaterialTags.of("wood");
    public static final MaterialTags BONE = MaterialTags.of("bone");
    public static final MaterialTags COAL = MaterialTags.of("coal");
    public static final MaterialTags IRON = MaterialTags.of("iron");
    public static final MaterialTags GOLD = MaterialTags.of("gold");
    public static final MaterialTags LAPIS = MaterialTags.of("lapis");
    public static final MaterialTags REDSTONE = MaterialTags.of("redstone");
    public static final MaterialTags NETHERQUARTZ = MaterialTags.of("nether_quartz");
    public static final MaterialTags QUARTZ = MaterialTags.of("quartz");
    public static final MaterialTags DIAMOND = MaterialTags.of("diamond");
    public static final MaterialTags EMERALD = MaterialTags.of("emerald");
    public static final MaterialTags U = MaterialTags.of("uranium");
    public static final MaterialTags U233 = MaterialTags.of("uranium_233");
    public static final MaterialTags U235 = MaterialTags.of("uranium_235");
    public static final MaterialTags U238 = MaterialTags.of("uranium_238");
    public static final MaterialTags TH232 = MaterialTags.of("thorium");
    public static final MaterialTags PU = MaterialTags.of("plutonium");
    public static final MaterialTags PURG = MaterialTags.of("plutonium_rg");
    public static final MaterialTags PU238 = MaterialTags.of("plutonium_238");
    public static final MaterialTags PU239 = MaterialTags.of("plutonium_239");
    public static final MaterialTags PU240 = MaterialTags.of("plutonium_240");
    public static final MaterialTags PU241 = MaterialTags.of("plutonium_241");
    public static final MaterialTags AM241 = MaterialTags.of("americium_241");
    public static final MaterialTags AM242 = MaterialTags.of("americium_242");
    public static final MaterialTags AMRG = MaterialTags.of("americium_rg");
    public static final MaterialTags NP237 = MaterialTags.aliased("neptunium_237", "neptunium");
    public static final MaterialTags PO210 = MaterialTags.aliased("polonium_210", "polonium");
    public static final MaterialTags TC99 = MaterialTags.of("technetium_99");
    public static final MaterialTags RA226 = MaterialTags.of("radium_226");
    public static final MaterialTags AC227 = MaterialTags.of("actinium_227");
    public static final MaterialTags CO60 = MaterialTags.of("cobalt_60");
    public static final MaterialTags AU198 = MaterialTags.of("gold_198");
    public static final MaterialTags PB209 = MaterialTags.of("lead_209");
    public static final MaterialTags SA326 = MaterialTags.of("schrabidium");
    public static final MaterialTags SA327 = MaterialTags.of("solinium");
    public static final MaterialTags SBD = MaterialTags.of("schrabidate");
    public static final MaterialTags SRN = MaterialTags.of("schraranium");
    public static final MaterialTags GH336 = MaterialTags.of("ghiorsium_336");
    public static final MaterialTags MUD = MaterialTags.of("watz_mud");
    public static final MaterialTags TI = MaterialTags.of("titanium");
    public static final MaterialTags CU = MaterialTags.of("copper");
    public static final MaterialTags MINGRADE = MaterialTags.of("mingrade");
    public static final MaterialTags W = MaterialTags.of("tungsten");
    public static final MaterialTags WC = MaterialTags.of("tungsten_carbide");
    public static final MaterialTags AL = MaterialTags.of("aluminum");
    public static final MaterialTags STEEL = MaterialTags.of("steel");
    public static final MaterialTags TCALLOY = MaterialTags.of("tc_alloy");
    public static final MaterialTags CDALLOY = MaterialTags.of("cd_alloy");
    public static final MaterialTags BBRONZE = MaterialTags.of("bismuth_bronze");
    public static final MaterialTags ABRONZE = MaterialTags.of("arsenic_bronze");
    public static final MaterialTags BSCCO = MaterialTags.of("bscco");
    public static final MaterialTags PB = MaterialTags.of("lead");
    public static final MaterialTags BI = MaterialTags.of("bismuth");
    public static final MaterialTags AS = MaterialTags.of("arsenic");
    public static final MaterialTags CA = MaterialTags.of("calcium");
    public static final MaterialTags CD = MaterialTags.of("cadmium");
    public static final MaterialTags TA = MaterialTags.of("tantalum");
    public static final MaterialTags COLTAN = MaterialTags.of("coltan");
    public static final MaterialTags NB = MaterialTags.of("niobium");
    public static final MaterialTags BE = MaterialTags.of("beryllium");
    public static final MaterialTags CO = MaterialTags.of("cobalt");
    public static final MaterialTags B = MaterialTags.of("boron");
    public static final MaterialTags SI = MaterialTags.of("silicon");
    public static final MaterialTags GRAPHITE = MaterialTags.of("graphite");
    public static final MaterialTags CARBON = MaterialTags.of("carbon");
    public static final MaterialTags DURA = MaterialTags.of("dura_steel");
    public static final MaterialTags POLYMER = MaterialTags.of("polymer");
    public static final MaterialTags BAKELITE = MaterialTags.of("bakelite");
    public static final MaterialTags PET = MaterialTags.of("pet");
    public static final MaterialTags PC = MaterialTags.of("polycarbonate");
    public static final MaterialTags PVC = MaterialTags.of("pvc");
    public static final MaterialTags LATEX = MaterialTags.of("latex");
    public static final MaterialTags RUBBER = MaterialTags.of("rubber");
    public static final MaterialTags MAGTUNG = MaterialTags.of("magnetized_tungsten");
    public static final MaterialTags CMB = MaterialTags.of("cmbsteel");
    public static final MaterialTags DESH = MaterialTags.of("workers_alloy");
    public static final MaterialTags STAR = MaterialTags.of("starmetal");
    public static final MaterialTags GUNMETAL = MaterialTags.of("gun_metal");
    public static final MaterialTags WEAPONSTEEL = MaterialTags.of("weapon_steel");
    public static final MaterialTags BIGMT = MaterialTags.of("saturnite");
    public static final MaterialTags FERRO = MaterialTags.of("ferrouranium");
    public static final MaterialTags EUPH = MaterialTags.of("euphemium");
    public static final MaterialTags DNT = MaterialTags.of("dineutronium");
    public static final MaterialTags FIBER = MaterialTags.of("fiberglass");
    public static final MaterialTags ASBESTOS = MaterialTags.of("asbestos");
    public static final MaterialTags OSMIRIDIUM = MaterialTags.of("osmiridium");
    public static final MaterialTags S = MaterialTags.of("sulfur");
    public static final MaterialTags KNO = MaterialTags.of("saltpeter");
    public static final MaterialTags F = MaterialTags.of("fluorite");
    public static final MaterialTags LIGNITE = MaterialTags.of("lignite");
    public static final MaterialTags COALCOKE = MaterialTags.of("coke");
    public static final MaterialTags PETCOKE = MaterialTags.of("pet_coke");
    public static final MaterialTags LIGCOKE = MaterialTags.of("lignite_coke");
    public static final MaterialTags CINNABAR = MaterialTags.of("cinnabar");
    public static final MaterialTags BORAX = MaterialTags.of("borax");
    public static final MaterialTags CHLOROCALCITE = MaterialTags.of("chlorocalcite");
    public static final MaterialTags MOLYSITE = MaterialTags.of("molysite");
    public static final MaterialTags SODALITE = MaterialTags.of("sodalite");
    public static final MaterialTags VOLCANIC = MaterialTags.of("volcanic");
    public static final MaterialTags HEMATITE = MaterialTags.of("hematite");
    public static final MaterialTags MALACHITE = MaterialTags.of("malachite");
    public static final MaterialTags LIMESTONE = MaterialTags.of("limestone");
    public static final MaterialTags SLAG = MaterialTags.of("slag");
    public static final MaterialTags BAUXITE = MaterialTags.of("bauxite");
    public static final MaterialTags CRYOLITE = MaterialTags.of("cryolite");
    public static final MaterialTags LI = MaterialTags.of("lithium");
    public static final MaterialTags NA = MaterialTags.of("sodium");
    public static final MaterialTags P_WHITE = MaterialTags.of("white_phosphorus");
    public static final MaterialTags P_RED = MaterialTags.of("red_phosphorus");
    public static final MaterialTags AUSTRALIUM = MaterialTags.of("australium");
    public static final MaterialTags RAREEARTH = MaterialTags.of("rare_earth");
    public static final MaterialTags LA = MaterialTags.of("lanthanum");
    public static final MaterialTags ZR = MaterialTags.of("zirconium");
    public static final MaterialTags ND = MaterialTags.of("neodymium");
    public static final MaterialTags CE = MaterialTags.of("cerium");
    public static final MaterialTags I = MaterialTags.of("iodine");
    public static final MaterialTags AT = MaterialTags.of("astatine");
    public static final MaterialTags CS = MaterialTags.of("caesium");
    public static final MaterialTags ST = MaterialTags.of("strontium");
    public static final MaterialTags BR = MaterialTags.of("bromine");
    public static final MaterialTags TS = MaterialTags.of("tennessine");
    public static final MaterialTags SR = MaterialTags.of("strontium");
    public static final MaterialTags SR90 = MaterialTags.of("strontium_90");
    public static final MaterialTags I131 = MaterialTags.of("iodine_131");
    public static final MaterialTags XE135 = MaterialTags.of("xenon_135");
    public static final MaterialTags CS137 = MaterialTags.of("caesium_137");
    public static final MaterialTags AT209 = MaterialTags.of("astatine_209");
    public static final MaterialTags ANY_GUNPOWDER = MaterialTags.internal("any_propellant");
    public static final MaterialTags ANY_SMOKELESS = MaterialTags.internal("any_smokeless");
    public static final MaterialTags ANY_PLASTICEXPLOSIVE =
            MaterialTags.internal("any_plasticexplosive");
    public static final MaterialTags ANY_HIGHEXPLOSIVE = MaterialTags.internal("any_highexplosive");
    public static final MaterialTags ANY_COKE = MaterialTags.internal("any_coke");
    public static final MaterialTags ANY_ASH = MaterialTags.internal("ash");

    public static final MaterialTags ANY_RUBBER = MaterialTags.anyOf("any_rubber", LATEX, RUBBER);
    public static final MaterialTags ANY_PLASTIC =
            MaterialTags.anyOf("any_plastic", POLYMER, BAKELITE);
    public static final MaterialTags ANY_HARDPLASTIC =
            MaterialTags.anyOf("any_hard_plastic", PC, PVC);
    public static final MaterialTags ANY_RESISTANTALLOY =
            MaterialTags.anyOf("any_resistant_alloy", TCALLOY, CDALLOY);
    public static final MaterialTags ANY_BISMOIDBRONZE =
            MaterialTags.anyOf("any_bismoid_bronze", BBRONZE, ABRONZE);
    public static final MaterialTags ANY_BISMOID = MaterialTags.anyOf("any_bismoid", BI, AS);

    public static final TagKey<Item> KEY_ANYGLASS = convention("glass_blocks");
    public static final TagKey<Item> KEY_CLEARGLASS = convention("glass_blocks/colorless");
    public static final TagKey<Item> KEY_ANYPANE = convention("glass_panes");
    public static final TagKey<Item> KEY_CONCRETE = convention("concretes");
    public static final TagKey<Item> KEY_SAND = ItemTags.SAND;
    public static final TagKey<Item> KEY_COBBLESTONE = convention("cobblestones");
    public static final TagKey<Item> KEY_COBBLESTONE_DEEPSLATE =
            convention("cobblestones/deepslate");
    public static final TagKey<Item> KEY_DYE = convention("dyes");
    public static final TagKey<Item> KEY_STICK = convention("rods/wooden");
    public static final TagKey<Item> KEY_TOOL_SCREWDRIVER = own("tools/screwdriver");
    public static final TagKey<Item> KEY_TOOL_HANDDRILL = own("tools/hand_drill");
    public static final TagKey<Item> KEY_TOOL_CHEMISTRYSET = own("tools/chemistry_set");
    public static final TagKey<Item> KEY_TOOL_TORCH = own("tools/torch");
    public static final TagKey<Item> KEY_ANY_TAR = own("any_tar");

    private static final TagKey<Item>[] DYES = byColor("dyes/");
    private static final TagKey<Item>[] DYED = byColor("dyed/");

    private MaterialTagRegistry() {}

    private static TagKey<Item> convention(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Item> own(String path) {
        return TagKey.create(Registries.ITEM, RegistryUtil.id(path));
    }

    @SuppressWarnings("unchecked")
    private static TagKey<Item>[] byColor(String family) {
        DyeColor[] colors = DyeColor.values();
        TagKey<Item>[] tags = new TagKey[colors.length];
        for (DyeColor color : colors)
            tags[color.ordinal()] = convention(family + color.getSerializedName());
        return tags;
    }

    public static TagKey<Item> dye(DyeColor color) {
        return DYES[color.ordinal()];
    }

    public static TagKey<Item> dyed(DyeColor color) {
        return DYED[color.ordinal()];
    }
}
