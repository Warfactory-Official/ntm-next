/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.inventory.material;

import com.hbm.hazard.HazardRegistry;

import static com.hbm.inventory.material.MaterialShape.*;
import static com.hbm.inventory.material.SmeltingBehavior.*;

public final class Materials {
    public static final int BASE_VANILLA = 0;
    public static final int BASE_ALLOY = 30;
    public static final int BASE_EXTENSION = 20_000;

    public static final Material WOOD =
            Material.define(
                            3,
                            MaterialTagRegistry.WOOD,
                            NOT_SMELTABLE,
                            0x896727,
                            0x281E0B,
                            0x896727)
                    .autoShapes(STOCK, GRIP)
                    .register();
    public static final Material BONE =
            Material.define(
                            4,
                            MaterialTagRegistry.BONE,
                            NOT_SMELTABLE,
                            0xFFFEEE,
                            0x797870,
                            0xEDEBCA)
                    .autoShapes(GRIP)
                    .register();
    public static final Material STONE =
            Material.define(0, MaterialTags.of("stone"), SMELTABLE, 0x7F7F7F, 0x353535, 0x4D2F23)
                    .register();
    public static final Material CARBON =
            Material.define(699, MaterialTagRegistry.CARBON, ADDITIVE, 0x363636, 0x030303, 0x404040)
                    .autoShapes(FINE_WIRE, BLOCK)
                    .memberShapes(INGOT)
                    .register();
    public static final Material COAL =
            Material.define(
                            600,
                            MaterialTagRegistry.COAL,
                            NOT_SMELTABLE,
                            0x363636,
                            0x030303,
                            0x404040)
                    .convertsInto(CARBON, 2, 1)
                    .coal(1.0)
                    .autoShapes(FRAGMENT, TINY_DUST, DUST)
                    .memberShapes(GEM)
                    .namedId(DUST, "powder_coal")
                    .register();
    public static final Material LIGNITE =
            Material.define(
                            601,
                            MaterialTagRegistry.LIGNITE,
                            NOT_SMELTABLE,
                            0x542D0F,
                            0x261508,
                            0x472913)
                    .convertsInto(CARBON, 3, 1)
                    .coal(1.0)
                    .autoShapes(FRAGMENT)
                    .memberShapes(GEM, DUST, ORE)
                    .namedId(DUST, "powder_lignite")
                    .register();
    public static final Material COKE =
            Material.define(610, MaterialTagRegistry.COALCOKE)
                    .convertsInto(CARBON, 4, 3)
                    .memberShapes(GEM, BLOCK)
                    .register();
    public static final Material PET_COKE =
            Material.define(611, MaterialTagRegistry.PETCOKE)
                    .convertsInto(CARBON, 4, 3)
                    .memberShapes(GEM, BLOCK)
                    .register();
    public static final Material LIGNITE_COKE =
            Material.define(612, MaterialTagRegistry.LIGCOKE)
                    .convertsInto(CARBON, 4, 3)
                    .memberShapes(GEM, BLOCK)
                    .register();
    public static final Material GRAPHITE =
            Material.define(620, MaterialTagRegistry.GRAPHITE)
                    .convertsInto(CARBON, 1, 1)
                    .memberShapes(INGOT, BLOCK)
                    .namedId(INGOT, "ingot_graphite")
                    .register();
    public static final Material DIAMOND =
            Material.define(
                            1430,
                            MaterialTagRegistry.DIAMOND,
                            NOT_SMELTABLE,
                            0xFFFFFF,
                            0x1B7B6B,
                            0x8CF4E2)
                    .convertsInto(CARBON, 1, 1)
                    .autoShapes(FRAGMENT)
                    .memberShapes(DUST, ORE)
                    .namedId(DUST, "powder_diamond")
                    .register();
    public static final Material IRON =
            Material.define(2600, MaterialTagRegistry.IRON, SMELTABLE, 0xFFFFFF, 0x353535, 0xFFA259)
                    .autoShapes(FRAGMENT, DUST, PIPE, CAST_PLATE, WELDED_PLATE, BLOCK)
                    .memberShapes(INGOT, NUGGET, PLATE, ORE)
                    .namedId(DUST, "powder_iron")
                    .namedId(PLATE, "plate_iron")
                    .register();
    public static final Material GOLD =
            Material.define(7900, MaterialTagRegistry.GOLD, SMELTABLE, 0xFFFF8B, 0xC26E00, 0xE8D754)
                    .autoShapes(FRAGMENT, FINE_WIRE, NUGGET, DUST, DENSE_WIRE, CAST_PLATE, BLOCK)
                    .memberShapes(INGOT, PLATE, ORE)
                    .namedId(DUST, "powder_gold")
                    .namedId(PLATE, "plate_gold")
                    .register();
    public static final Material REDSTONE =
            Material.define(
                            1,
                            MaterialTagRegistry.REDSTONE,
                            SMELTABLE,
                            0xE3260C,
                            0x700E06,
                            0xFF1000)
                    .autoShapes(FRAGMENT, INGOT)
                    .register();
    public static final Material OBSIDIAN =
            Material.define(2, MaterialTags.of("obsidian"), SMELTABLE, 0x3D234D, 0x3D234D, 0x3D234D)
                    .register();
    public static final Material HEMATITE =
            Material.define(
                            2601,
                            MaterialTagRegistry.HEMATITE,
                            ADDITIVE,
                            0xDFB7AE,
                            0x5F372E,
                            0x6E463D)
                    .memberShapes(ORE)
                    .register();
    public static final Material WROUGHT_IRON =
            Material.define(
                            2602,
                            MaterialTags.of("wrought_iron"),
                            SMELTABLE,
                            0xFAAB89,
                            0xFAAB89,
                            0xFAAB89)
                    .register();
    public static final Material PIG_IRON =
            Material.define(
                            2603,
                            MaterialTags.of("pig_iron"),
                            SMELTABLE,
                            0xFF8B59,
                            0xFF8B59,
                            0xFF8B59)
                    .register();
    public static final Material METEORIC_IRON =
            Material.define(
                            2604,
                            MaterialTags.of("meteoric_iron"),
                            SMELTABLE,
                            0x715347,
                            0x715347,
                            0x715347)
                    .register();
    public static final Material MALACHITE =
            Material.define(
                            2901,
                            MaterialTagRegistry.MALACHITE,
                            ADDITIVE,
                            0xA2F0C8,
                            0x227048,
                            0x61AF87)
                    .memberShapes(INGOT, ORE)
                    .register();
    public static final Material BAUXITE =
            Material.define(
                            2902,
                            MaterialTagRegistry.BAUXITE,
                            NOT_SMELTABLE,
                            0xF4BA30,
                            0xAA320A,
                            0xE2560F)
                    .autoShapes(FRAGMENT)
                    .memberShapes(ORE)
                    .register();
    public static final Material CRYOLITE =
            Material.define(
                            2903,
                            MaterialTagRegistry.CRYOLITE,
                            NOT_SMELTABLE,
                            0xCBC2A4,
                            0x8B711F,
                            0x8B701A)
                    .autoShapes(FRAGMENT)
                    .memberShapes(CRYSTAL)
                    .register();
    public static final Material URANIUM =
            Material.define(9200, MaterialTagRegistry.U, SMELTABLE, 0xC1C7BD, 0x2B3227, 0x9AA196)
                    .radiation(HazardRegistry.u)
                    .autoShapes(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_uranium")
                    .namedId(DUST, "powder_uranium")
                    .namedId(NUGGET, "nugget_uranium")
                    .namedId(BILLET, "billet_uranium")
                    .register();
    public static final Material URANIUM_233 =
            Material.define(9233, MaterialTagRegistry.U233, SMELTABLE, 0xC1C7BD, 0x2B3227, 0x9AA196)
                    .radiation(HazardRegistry.u233)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_u233")
                    .namedId(NUGGET, "nugget_u233")
                    .namedId(BILLET, "billet_u233")
                    .register();
    public static final Material URANIUM_235 =
            Material.define(9235, MaterialTagRegistry.U235, SMELTABLE, 0xC1C7BD, 0x2B3227, 0x9AA196)
                    .radiation(HazardRegistry.u235)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_u235")
                    .namedId(NUGGET, "nugget_u235")
                    .namedId(BILLET, "billet_u235")
                    .register();
    public static final Material URANIUM_238 =
            Material.define(9238, MaterialTagRegistry.U238, SMELTABLE, 0xC1C7BD, 0x2B3227, 0x9AA196)
                    .radiation(HazardRegistry.u238)
                    .autoShapes(FRAGMENT, NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_u238")
                    .namedId(NUGGET, "nugget_u238")
                    .namedId(BILLET, "billet_u238")
                    .register();
    public static final Material THORIUM =
            Material.define(
                            9032,
                            MaterialTagRegistry.TH232,
                            SMELTABLE,
                            0xBF825F,
                            0x1C0000,
                            0xBF825F)
                    .radiation(HazardRegistry.th232)
                    .autoShapes(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_th232")
                    .namedId(DUST, "powder_thorium")
                    .namedId(NUGGET, "nugget_th232")
                    .namedId(BILLET, "billet_th232")
                    .register();
    public static final Material PLUTONIUM =
            Material.define(9400, MaterialTagRegistry.PU, SMELTABLE, 0x9AA3A0, 0x111A17, 0x78817E)
                    .radiation(HazardRegistry.pu)
                    .autoShapes(NUGGET, BILLET, DUST, BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_plutonium")
                    .namedId(DUST, "powder_plutonium")
                    .namedId(NUGGET, "nugget_plutonium")
                    .namedId(BILLET, "billet_plutonium")
                    .register();
    public static final Material PLUTONIUM_RG =
            Material.define(9401, MaterialTagRegistry.PURG, SMELTABLE, 0x9AA3A0, 0x111A17, 0x78817E)
                    .radiation(HazardRegistry.purg)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_pu_mix")
                    .namedId(NUGGET, "nugget_pu_mix")
                    .namedId(BILLET, "billet_pu_mix")
                    .register();
    public static final Material PLUTONIUM_238 =
            Material.define(
                            9438,
                            MaterialTagRegistry.PU238,
                            SMELTABLE,
                            0xFFBC59,
                            0xFF8E2B,
                            0x78817E)
                    .radiation(HazardRegistry.pu238)
                    .hot(3.0)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_pu238")
                    .namedId(NUGGET, "nugget_pu238")
                    .namedId(BILLET, "billet_pu238")
                    .register();
    public static final Material PLUTONIUM_239 =
            Material.define(
                            9439,
                            MaterialTagRegistry.PU239,
                            SMELTABLE,
                            0x9AA3A0,
                            0x111A17,
                            0x78817E)
                    .radiation(HazardRegistry.pu239)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_pu239")
                    .namedId(NUGGET, "nugget_pu239")
                    .namedId(BILLET, "billet_pu239")
                    .register();
    public static final Material PLUTONIUM_240 =
            Material.define(
                            9440,
                            MaterialTagRegistry.PU240,
                            SMELTABLE,
                            0x9AA3A0,
                            0x111A17,
                            0x78817E)
                    .radiation(HazardRegistry.pu240)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_pu240")
                    .namedId(NUGGET, "nugget_pu240")
                    .namedId(BILLET, "billet_pu240")
                    .register();
    public static final Material PLUTONIUM_241 =
            Material.define(
                            9441,
                            MaterialTagRegistry.PU241,
                            SMELTABLE,
                            0x9AA3A0,
                            0x111A17,
                            0x78817E)
                    .radiation(HazardRegistry.pu241)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_pu241")
                    .namedId(NUGGET, "nugget_pu241")
                    .namedId(BILLET, "billet_pu241")
                    .register();
    public static final Material AMERICIUM_RG =
            Material.define(9501, MaterialTagRegistry.AMRG, SMELTABLE, 0xCEB3B9, 0x3A1C21, 0x93767B)
                    .radiation(HazardRegistry.amrg)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_am_mix")
                    .namedId(NUGGET, "nugget_am_mix")
                    .namedId(BILLET, "billet_am_mix")
                    .register();
    public static final Material AMERICIUM_241 =
            Material.define(
                            9541,
                            MaterialTagRegistry.AM241,
                            SMELTABLE,
                            0xCEB3B9,
                            0x3A1C21,
                            0x93767B)
                    .radiation(HazardRegistry.am241)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_am241")
                    .namedId(NUGGET, "nugget_am241")
                    .namedId(BILLET, "billet_am241")
                    .register();
    public static final Material AMERICIUM_242 =
            Material.define(
                            9542,
                            MaterialTagRegistry.AM242,
                            SMELTABLE,
                            0xCEB3B9,
                            0x3A1C21,
                            0x93767B)
                    .radiation(HazardRegistry.am242)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_am242")
                    .namedId(NUGGET, "nugget_am242")
                    .namedId(BILLET, "billet_am242")
                    .register();
    public static final Material NEPTUNIUM_237 =
            Material.define(
                            9337,
                            MaterialTagRegistry.NP237,
                            SMELTABLE,
                            0xA6B2A6,
                            0x030F03,
                            0x647064)
                    .radiation(HazardRegistry.np237)
                    .autoShapes(NUGGET, BILLET, DUST, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_neptunium")
                    .namedId(DUST, "powder_neptunium")
                    .namedId(NUGGET, "nugget_neptunium")
                    .namedId(BILLET, "billet_neptunium")
                    .register();
    public static final Material POLONIUM_210 =
            Material.define(
                            8410,
                            MaterialTagRegistry.PO210,
                            SMELTABLE,
                            0x968779,
                            0x3D1509,
                            0x715E4A)
                    .radiation(HazardRegistry.po210)
                    .hot(3.0)
                    .autoShapes(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_polonium")
                    .namedId(DUST, "powder_polonium")
                    .namedId(NUGGET, "nugget_polonium")
                    .namedId(BILLET, "billet_polonium")
                    .register();
    public static final Material TECHNETIUM_99 =
            Material.define(4399, MaterialTagRegistry.TC99, SMELTABLE, 0xFAFFFF, 0x576C6C, 0xCADFDF)
                    .radiation(HazardRegistry.tc99)
                    .autoShapes(FRAGMENT, NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_technetium")
                    .namedId(NUGGET, "nugget_technetium")
                    .namedId(BILLET, "billet_technetium")
                    .register();
    public static final Material RADIUM_226 =
            Material.define(
                            8826,
                            MaterialTagRegistry.RA226,
                            SMELTABLE,
                            0xFCFCFC,
                            0xADBFBA,
                            0xE9FAF6)
                    .radiation(HazardRegistry.ra226)
                    .autoShapes(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_ra226")
                    .namedId(DUST, "powder_ra226")
                    .namedId(NUGGET, "nugget_ra226")
                    .namedId(BILLET, "billet_ra226")
                    .register();
    public static final Material ACTINIUM_227 =
            Material.define(
                            8927,
                            MaterialTagRegistry.AC227,
                            SMELTABLE,
                            0xECE0E0,
                            0x221616,
                            0x958989)
                    .radiation(HazardRegistry.ac227)
                    .autoShapes(NUGGET, BILLET)
                    .memberShapes(INGOT, DUST, BLOCK, TINY_DUST)
                    .namedId(INGOT, "ingot_actinium")
                    .namedId(DUST, "powder_actinium")
                    .namedId(NUGGET, "nugget_actinium")
                    .namedId(BILLET, "billet_actinium")
                    .register();
    public static final Material COBALT_60 =
            Material.define(2760, MaterialTagRegistry.CO60, SMELTABLE, 0xC2D1EE, 0x353554, 0x8F72AE)
                    .radiation(HazardRegistry.co60)
                    .hot(1.0)
                    .autoShapes(NUGGET, BILLET, DUST)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_co60")
                    .namedId(DUST, "powder_co60")
                    .namedId(NUGGET, "nugget_co60")
                    .namedId(BILLET, "billet_co60")
                    .register();
    public static final Material GOLD_198 =
            Material.define(
                            7998,
                            MaterialTagRegistry.AU198,
                            SMELTABLE,
                            0xFFFF8B,
                            0xC26E00,
                            0xE8D754)
                    .radiation(HazardRegistry.au198)
                    .hot(5.0)
                    .autoShapes(NUGGET, BILLET, DUST)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_au198")
                    .namedId(DUST, "powder_au198")
                    .namedId(NUGGET, "nugget_au198")
                    .namedId(BILLET, "billet_au198")
                    .register();
    public static final Material LEAD_209 =
            Material.define(
                            8209,
                            MaterialTagRegistry.PB209,
                            SMELTABLE,
                            0xB38A94,
                            0x12020E,
                            0x7B535D)
                    .radiation(HazardRegistry.pb209)
                    .hot(7.0)
                    .blinding(50.0)
                    .autoShapes(NUGGET, BILLET)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_pb209")
                    .namedId(NUGGET, "nugget_pb209")
                    .namedId(BILLET, "billet_pb209")
                    .register();
    public static final Material SCHRABIDIUM =
            Material.define(
                            12626,
                            MaterialTagRegistry.SA326,
                            SMELTABLE,
                            0x32FFFF,
                            0x005C5C,
                            0x32FFFF)
                    .radiation(HazardRegistry.sa326)
                    .blinding(50.0)
                    .autoShapes(
                            NUGGET, FINE_WIRE, BILLET, DUST, DENSE_WIRE, PLATE, CAST_PLATE, BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_schrabidium")
                    .namedId(DUST, "powder_schrabidium")
                    .namedId(NUGGET, "nugget_schrabidium")
                    .namedId(BILLET, "billet_schrabidium")
                    .namedId(PLATE, "plate_schrabidium")
                    .register();
    public static final Material SOLINIUM =
            Material.define(
                            12627,
                            MaterialTagRegistry.SA327,
                            SMELTABLE,
                            0xA2E6E0,
                            0x00433D,
                            0x72B6B0)
                    .radiation(HazardRegistry.sa327)
                    .blinding(50.0)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_solinium")
                    .namedId(NUGGET, "nugget_solinium")
                    .namedId(BILLET, "billet_solinium")
                    .register();
    public static final Material SCHRABIDATE =
            Material.define(12600, MaterialTagRegistry.SBD, SMELTABLE, 0x77C0D7, 0x39005E, 0x6589B4)
                    .radiation(HazardRegistry.sb)
                    .blinding(50.0)
                    .autoShapes(DUST, DENSE_WIRE, CAST_PLATE, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_schrabidate")
                    .namedId(DUST, "powder_schrabidate")
                    .register();
    public static final Material SCHRARANIUM =
            Material.define(12601, MaterialTagRegistry.SRN, SMELTABLE, 0x2B3227, 0x2B3227, 0x24AFAC)
                    .radiation(HazardRegistry.sr)
                    .blinding(50.0)
                    .autoShapes(BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_schraranium")
                    .register();
    public static final Material GHIORSIUM_336 =
            Material.define(
                            12836,
                            MaterialTagRegistry.GH336,
                            SMELTABLE,
                            0xF4EFE1,
                            0x2A3306,
                            0xC6C6A1)
                    .radiation(HazardRegistry.gh336)
                    .autoShapes(NUGGET, BILLET, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_gh336")
                    .namedId(NUGGET, "nugget_gh336")
                    .namedId(BILLET, "billet_gh336")
                    .register();
    public static final Material TITANIUM =
            Material.define(2200, MaterialTagRegistry.TI, SMELTABLE, 0xF7F3F2, 0x4F4C4B, 0xA99E79)
                    .autoShapes(
                            FRAGMENT,
                            DUST,
                            PLATE,
                            DENSE_WIRE,
                            CAST_PLATE,
                            WELDED_PLATE,
                            SHELL,
                            BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_titanium")
                    .namedId(DUST, "powder_titanium")
                    .namedId(PLATE, "plate_titanium")
                    .register();
    public static final Material COPPER =
            Material.define(2900, MaterialTagRegistry.CU, SMELTABLE, 0xFDCA88, 0x601E0D, 0xC18336)
                    .autoShapes(
                            FRAGMENT,
                            FINE_WIRE,
                            DUST,
                            PLATE,
                            DENSE_WIRE,
                            CAST_PLATE,
                            WELDED_PLATE,
                            SHELL,
                            PIPE,
                            BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(DUST, "powder_copper")
                    .namedId(PLATE, "plate_copper")
                    .register();
    public static final Material TUNGSTEN =
            Material.define(7400, MaterialTagRegistry.W, SMELTABLE, 0x868686, 0x000000, 0x977474)
                    .autoShapes(
                            FRAGMENT,
                            FINE_WIRE,
                            BOLT,
                            DUST,
                            DENSE_WIRE,
                            CAST_PLATE,
                            WELDED_PLATE,
                            BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_tungsten")
                    .namedId(DUST, "powder_tungsten")
                    .register();
    public static final Material ALUMINUM =
            Material.define(1300, MaterialTagRegistry.AL, SMELTABLE, 0xFFFFFF, 0x344550, 0xD0B8EB)
                    .autoShapes(
                            FRAGMENT,
                            FINE_WIRE,
                            DUST,
                            PLATE,
                            CAST_PLATE,
                            WELDED_PLATE,
                            SHELL,
                            PIPE,
                            BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_aluminium")
                    .namedId(DUST, "powder_aluminium")
                    .namedId(PLATE, "plate_aluminium")
                    .register();
    public static final Material LEAD =
            Material.define(8200, MaterialTagRegistry.PB, SMELTABLE, 0xA6A6B2, 0x03030F, 0x646470)
                    .autoShapes(
                            FRAGMENT, NUGGET, FINE_WIRE, BOLT, DUST, PLATE, CAST_PLATE, PIPE, BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_lead")
                    .namedId(DUST, "powder_lead")
                    .namedId(NUGGET, "nugget_lead")
                    .namedId(PLATE, "plate_lead")
                    .register();
    public static final Material BISMUTH =
            Material.define(8300, MaterialTagRegistry.BI, SMELTABLE, 0xB200FF, 0xB200FF, 0xB200FF)
                    .autoShapes(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_bismuth")
                    .namedId(DUST, "powder_bismuth")
                    .namedId(NUGGET, "nugget_bismuth")
                    .namedId(BILLET, "billet_bismuth")
                    .register();
    public static final Material ARSENIC =
            Material.define(3300, MaterialTagRegistry.AS, SMELTABLE, 0x6CBABA, 0x242525, 0x558080)
                    .autoShapes(NUGGET)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_arsenic")
                    .namedId(NUGGET, "nugget_arsenic")
                    .register();
    public static final Material TANTALUM =
            Material.define(7300, MaterialTagRegistry.TA, SMELTABLE, 0xFFFFFF, 0x1D1D36, 0xA89B74)
                    .autoShapes(FRAGMENT, NUGGET, DUST, BLOCK)
                    .memberShapes(GEM, INGOT)
                    .namedId(INGOT, "ingot_tantalium")
                    .namedId(DUST, "powder_tantalium")
                    .namedId(NUGGET, "nugget_tantalium")
                    .register();
    public static final Material NEODYMIUM =
            Material.define(6000, MaterialTagRegistry.ND, SMELTABLE, 0xE6E6B6, 0x1C1C00, 0x8F8F5F)
                    .autoShapes(FRAGMENT, NUGGET, TINY_DUST, INGOT, DUST, DENSE_WIRE, BLOCK)
                    .memberShapes(ORE)
                    .namedId(DUST, "powder_neodymium")
                    .register();
    public static final Material NIOBIUM =
            Material.define(4100, MaterialTagRegistry.NB, SMELTABLE, 0xB76EC9, 0x2F2D42, 0xD576B1)
                    .autoShapes(FRAGMENT, NUGGET, TINY_DUST, DUST, DENSE_WIRE, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_niobium")
                    .namedId(DUST, "powder_niobium")
                    .namedId(NUGGET, "nugget_niobium")
                    .register();
    public static final Material BERYLLIUM =
            Material.define(400, MaterialTagRegistry.BE, SMELTABLE, 0xB2B2A6, 0x0F0F03, 0xAE9572)
                    .autoShapes(FRAGMENT, NUGGET, DUST, BLOCK)
                    .memberShapes(BILLET, INGOT, ORE)
                    .namedId(INGOT, "ingot_beryllium")
                    .namedId(DUST, "powder_beryllium")
                    .namedId(NUGGET, "nugget_beryllium")
                    .namedId(BILLET, "billet_beryllium")
                    .register();
    public static final Material EMERALD =
            Material.define(
                            401,
                            MaterialTagRegistry.EMERALD,
                            NOT_SMELTABLE,
                            0xBAFFD4,
                            0x003900,
                            0x17DD62)
                    .convertsInto(BERYLLIUM, 4, 3)
                    .autoShapes(FRAGMENT, DUST, GEM, BLOCK)
                    .memberShapes(ORE)
                    .namedId(DUST, "powder_emerald")
                    .register();
    public static final Material COBALT =
            Material.define(2700, MaterialTagRegistry.CO, SMELTABLE, 0xC2D1EE, 0x353554, 0x8F72AE)
                    .autoShapes(FRAGMENT, NUGGET, TINY_DUST, BILLET, DUST, BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_cobalt")
                    .namedId(DUST, "powder_cobalt")
                    .namedId(NUGGET, "nugget_cobalt")
                    .namedId(BILLET, "billet_cobalt")
                    .register();
    public static final Material BORON =
            Material.define(500, MaterialTagRegistry.B, SMELTABLE, 0xBDC8D2, 0x29343E, 0xAD72AE)
                    .autoShapes(FRAGMENT, TINY_DUST, DUST, BLOCK)
                    .memberShapes(NUGGET, INGOT)
                    .namedId(INGOT, "ingot_boron")
                    .namedId(DUST, "powder_boron")
                    .register();
    public static final Material BORAX =
            Material.define(501, MaterialTagRegistry.BORAX, SMELTABLE, 0xFFFFFF, 0x946E23, 0xFFECC6)
                    .autoShapes(FRAGMENT, INGOT, DUST)
                    .memberShapes(ORE)
                    .namedId(DUST, "powder_borax")
                    .register();
    public static final Material LANTHANUM =
            Material.define(5700, MaterialTagRegistry.LA, SMELTABLE, 0xC8E0E0, 0x3B5353, 0xA1B9B9)
                    .autoShapes(FRAGMENT, BLOCK)
                    .memberShapes(NUGGET, INGOT, TINY_DUST, DUST)
                    .namedId(INGOT, "ingot_lanthanium")
                    .namedId(DUST, "powder_lanthanium")
                    .register();
    public static final Material ZIRCONIUM =
            Material.define(4000, MaterialTagRegistry.ZR, SMELTABLE, 0xE3DCBE, 0x3E3719, 0xADA688)
                    .autoShapes(
                            FRAGMENT,
                            NUGGET,
                            FINE_WIRE,
                            BILLET,
                            DUST,
                            CAST_PLATE,
                            WELDED_PLATE,
                            BLOCK)
                    .memberShapes(INGOT, ORE)
                    .namedId(INGOT, "ingot_zirconium")
                    .namedId(DUST, "powder_zirconium")
                    .namedId(NUGGET, "nugget_zirconium")
                    .namedId(BILLET, "billet_zirconium")
                    .register();
    public static final Material SODIUM =
            Material.define(1100, MaterialTagRegistry.NA, SMELTABLE, 0xD3BF9E, 0x3A5A6B, 0x7E9493)
                    .autoShapes(FRAGMENT, INGOT, DUST)
                    .namedId(DUST, "powder_sodium")
                    .register();
    public static final Material SODALITE =
            Material.define(
                            1101,
                            MaterialTagRegistry.SODALITE,
                            NOT_SMELTABLE,
                            0xDCE5F6,
                            0x4927B4,
                            0x96A7E6)
                    .autoShapes(FRAGMENT, GEM)
                    .register();
    public static final Material STRONTIUM =
            Material.define(3800, MaterialTagRegistry.ST, SMELTABLE, 0xF1E8BA, 0x271E00, 0xCAC193)
                    .autoShapes(FRAGMENT, INGOT, DUST)
                    .namedId(DUST, "powder_strontium")
                    .register();
    public static final Material CALCIUM =
            Material.define(2000, MaterialTagRegistry.CA, SMELTABLE, 0xCFCFA6, 0x747F6E, 0xB7B784)
                    .autoShapes(DUST)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_calcium")
                    .namedId(DUST, "powder_calcium")
                    .register();
    public static final Material LITHIUM =
            Material.define(300, MaterialTagRegistry.LI, SMELTABLE, 0xFFFFFF, 0x818181, 0xD6D6D6)
                    .autoShapes(FRAGMENT, DUST, BLOCK)
                    .memberShapes(INGOT, TINY_DUST, ORE)
                    .namedId(DUST, "powder_lithium")
                    .register();
    public static final Material SULFUR =
            Material.define(
                            1600,
                            MaterialTagRegistry.S,
                            NOT_SMELTABLE,
                            0xFCEE80,
                            0xBDA022,
                            0xF1DF68)
                    .autoShapes(FRAGMENT, DUST, BLOCK)
                    .memberShapes(ORE)
                    .register();
    public static final Material SALTPETER =
            Material.define(
                            700,
                            MaterialTagRegistry.KNO,
                            NOT_SMELTABLE,
                            0xD4D4D4,
                            0x969696,
                            0xC9C9C9)
                    .autoShapes(FRAGMENT, DUST, BLOCK)
                    .memberShapes(ORE)
                    .register();
    public static final Material FLUORITE =
            Material.define(900, MaterialTagRegistry.F, NOT_SMELTABLE, 0xFFFFFF, 0xB0A192, 0xE1DBD4)
                    .autoShapes(FRAGMENT, DUST, BLOCK)
                    .memberShapes(ORE)
                    .register();
    public static final Material RED_PHOSPHORUS =
            Material.define(
                            1500,
                            MaterialTagRegistry.P_RED,
                            NOT_SMELTABLE,
                            0xCB0213,
                            0x600006,
                            0xBA0615)
                    .autoShapes(FRAGMENT, DUST, BLOCK)
                    .namedId(DUST, "powder_fire", "powder_fire")
                    .register();
    public static final Material CHLOROCALCITE =
            Material.define(
                            1701,
                            MaterialTagRegistry.CHLOROCALCITE,
                            NOT_SMELTABLE,
                            0xF7E761,
                            0x475B46,
                            0xB8B963)
                    .autoShapes(FRAGMENT, DUST)
                    .namedId(DUST, "powder_chlorocalcite")
                    .register();
    public static final Material MOLYSITE =
            Material.define(
                            1702,
                            MaterialTagRegistry.MOLYSITE,
                            NOT_SMELTABLE,
                            0xF9E97B,
                            0x216E00,
                            0xD0D264)
                    .autoShapes(FRAGMENT, DUST)
                    .memberShapes(ORE)
                    .namedId(DUST, "powder_molysite")
                    .register();
    public static final Material CINNABAR =
            Material.define(
                            8001,
                            MaterialTagRegistry.CINNABAR,
                            NOT_SMELTABLE,
                            0xD87070,
                            0x993030,
                            0xBF4E4E)
                    .autoShapes(FRAGMENT, GEM)
                    .memberShapes(CRYSTAL, ORE)
                    .register();
    public static final Material CADMIUM =
            Material.define(4800, MaterialTagRegistry.CD, SMELTABLE, 0xFFFADE, 0x350000, 0xA85600)
                    .autoShapes(DUST)
                    .memberShapes(INGOT, BLOCK)
                    .namedId(INGOT, "ingot_cadmium")
                    .namedId(DUST, "powder_cadmium")
                    .register();
    public static final Material SILICON =
            Material.define(1400, MaterialTagRegistry.SI, SMELTABLE, 0xD1D7DF, 0x1A1A3D, 0x878B9E)
                    .autoShapes(FRAGMENT, NUGGET, BILLET)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_silicon")
                    .namedId(NUGGET, "nugget_silicon")
                    .namedId(BILLET, "billet_silicon")
                    .register();
    public static final Material ASBESTOS =
            Material.define(
                            1401,
                            MaterialTagRegistry.ASBESTOS,
                            SMELTABLE,
                            0xD8D9CF,
                            0x616258,
                            0xB0B3A8)
                    .asbestos(1.0)
                    .autoShapes(FRAGMENT, BLOCK)
                    .memberShapes(INGOT, DUST, ORE)
                    .namedId(INGOT, "ingot_asbestos")
                    .namedId(DUST, "powder_asbestos")
                    .register();
    public static final Material OSMIRIDIUM =
            Material.define(
                            7699,
                            MaterialTagRegistry.OSMIRIDIUM,
                            SMELTABLE,
                            0xDBE3EF,
                            0x7891BE,
                            0xACBDD9)
                    .autoShapes(NUGGET, CAST_PLATE, WELDED_PLATE)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_osmiridium")
                    .namedId(NUGGET, "nugget_osmiridium")
                    .register();
    public static final Material STEEL =
            Material.define(30, MaterialTagRegistry.STEEL, SMELTABLE, 0xAFAFAF, 0x0F0F0F, 0x4A4A4A)
                    .autoShapes(
                            TINY_DUST,
                            BOLT,
                            FINE_WIRE,
                            DUST,
                            PLATE,
                            CAST_PLATE,
                            WELDED_PLATE,
                            SHELL,
                            PIPE,
                            BLOCK,
                            LIGHT_BARREL,
                            HEAVY_BARREL,
                            LIGHT_RECEIVER,
                            GRIP)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_steel")
                    .namedId(DUST, "powder_steel")
                    .namedId(PLATE, "plate_steel")
                    .register();
    public static final Material MINGRADE =
            Material.define(
                            31,
                            MaterialTagRegistry.MINGRADE,
                            SMELTABLE,
                            0xFFBA7D,
                            0xAF1700,
                            0xE44C0F)
                    .autoShapes(FINE_WIRE, DUST, DENSE_WIRE, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_red_copper")
                    .namedId(DUST, "powder_red_copper")
                    .register();
    public static final Material DURA_STEEL =
            Material.define(33, MaterialTagRegistry.DURA, SMELTABLE, 0x82A59C, 0x06281E, 0x42665C)
                    .autoShapes(
                            BOLT,
                            DUST,
                            PLATE,
                            CAST_PLATE,
                            PIPE,
                            BLOCK,
                            LIGHT_BARREL,
                            HEAVY_BARREL,
                            LIGHT_RECEIVER,
                            HEAVY_RECEIVER,
                            GRIP)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_dura_steel")
                    .namedId(DUST, "powder_dura_steel")
                    .namedId(PLATE, "plate_dura_steel")
                    .register();
    public static final Material WORKERS_ALLOY =
            Material.define(42, MaterialTagRegistry.DESH, SMELTABLE, 0xFF6D6D, 0x720000, 0xF22929)
                    .autoShapes(
                            DUST,
                            CAST_PLATE,
                            BLOCK,
                            LIGHT_BARREL,
                            HEAVY_BARREL,
                            LIGHT_RECEIVER,
                            STOCK,
                            GRIP)
                    .memberShapes(NUGGET, INGOT)
                    .namedId(INGOT, "ingot_desh")
                    .namedId(DUST, "powder_desh")
                    .namedId(NUGGET, "nugget_desh")
                    .register();
    public static final Material STARMETAL =
            Material.define(35, MaterialTagRegistry.STAR, SMELTABLE, 0xCCCCEA, 0x11111A, 0xA5A5D3)
                    .autoShapes(DUST, DENSE_WIRE, CAST_PLATE, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_starmetal")
                    .register();
    public static final Material FERROURANIUM =
            Material.define(37, MaterialTagRegistry.FERRO, SMELTABLE, 0xB7B7C9, 0x101022, 0x6B6B8B)
                    .autoShapes(CAST_PLATE, HEAVY_BARREL, HEAVY_RECEIVER)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_ferrouranium")
                    .register();
    public static final Material TC_ALLOY =
            Material.define(
                            36,
                            MaterialTagRegistry.TCALLOY,
                            SMELTABLE,
                            0xD4D6D6,
                            0x323D3D,
                            0x9CA6A6)
                    .autoShapes(
                            DUST,
                            CAST_PLATE,
                            WELDED_PLATE,
                            LIGHT_BARREL,
                            HEAVY_BARREL,
                            LIGHT_RECEIVER,
                            HEAVY_RECEIVER)
                    .memberShapes(INGOT, BLOCK)
                    .namedId(INGOT, "ingot_tcalloy")
                    .namedId(DUST, "powder_tcalloy")
                    .register();
    public static final Material CD_ALLOY =
            Material.define(
                            43,
                            MaterialTagRegistry.CDALLOY,
                            SMELTABLE,
                            0xF7DF8F,
                            0x604308,
                            0xFBD368)
                    .autoShapes(
                            CAST_PLATE,
                            WELDED_PLATE,
                            LIGHT_BARREL,
                            HEAVY_BARREL,
                            LIGHT_RECEIVER,
                            HEAVY_RECEIVER)
                    .memberShapes(INGOT, BLOCK)
                    .namedId(INGOT, "ingot_cdalloy")
                    .register();
    public static final Material BISMUTH_BRONZE =
            Material.define(
                            46,
                            MaterialTagRegistry.BBRONZE,
                            SMELTABLE,
                            0xE19A69,
                            0x485353,
                            0x987D65)
                    .autoShapes(CAST_PLATE, LIGHT_BARREL, LIGHT_RECEIVER, HEAVY_RECEIVER)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_bismuth_bronze")
                    .register();
    public static final Material ARSENIC_BRONZE =
            Material.define(
                            47,
                            MaterialTagRegistry.ABRONZE,
                            SMELTABLE,
                            0xDB9462,
                            0x203331,
                            0x77644D)
                    .autoShapes(CAST_PLATE, LIGHT_BARREL, LIGHT_RECEIVER, HEAVY_RECEIVER)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_arsenic_bronze")
                    .register();
    public static final Material BSCCO =
            Material.define(48, MaterialTagRegistry.BSCCO, SMELTABLE, 0x767BF1, 0x000000, 0x5E62C0)
                    .autoShapes(DENSE_WIRE)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_bscco")
                    .register();
    public static final Material MAGNETIZED_TUNGSTEN =
            Material.define(
                            38,
                            MaterialTagRegistry.MAGTUNG,
                            SMELTABLE,
                            0x22A2A2,
                            0x0F0F0F,
                            0x22A2A2)
                    .autoShapes(FINE_WIRE, DUST, DENSE_WIRE, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_magnetized_tungsten")
                    .namedId(DUST, "powder_magnetized_tungsten")
                    .register();
    public static final Material CMBSTEEL =
            Material.define(39, MaterialTagRegistry.CMB, SMELTABLE, 0x6F6FB4, 0x000011, 0x6F6FB4)
                    .autoShapes(DUST, PLATE, CAST_PLATE, WELDED_PLATE, BLOCK)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_combine_steel")
                    .namedId(DUST, "powder_combine_steel")
                    .namedId(PLATE, "plate_combine_steel")
                    .register();
    public static final Material DINEUTRONIUM =
            Material.define(45, MaterialTagRegistry.DNT, SMELTABLE, 0x7582B9, 0x16000E, 0x455289)
                    .autoShapes(DUST, DENSE_WIRE, BLOCK)
                    .memberShapes(NUGGET, INGOT)
                    .namedId(INGOT, "ingot_dineutronium")
                    .namedId(DUST, "powder_dineutronium")
                    .namedId(NUGGET, "nugget_dineutronium")
                    .register();
    public static final Material FLUX =
            Material.define(40, MaterialTags.of("flux"), ADDITIVE, 0xF1E0BB, 0x6F6256, 0xDECCAD)
                    .autoShapes(DUST)
                    .register();
    public static final Material SLAG =
            Material.define(41, MaterialTagRegistry.SLAG, SMELTABLE, 0x554940, 0x34281F, 0x6C6562)
                    .autoShapes(INGOT, BLOCK)
                    .register();
    public static final Material WATZ_MUD =
            Material.define(44, MaterialTagRegistry.MUD, SMELTABLE, 0xBCB5A9, 0x481213, 0x96783B)
                    .radiation(HazardRegistry.mud)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_mud")
                    .register();
    public static final Material GUN_METAL =
            Material.define(
                            49,
                            MaterialTagRegistry.GUNMETAL,
                            SMELTABLE,
                            0xFFEF3F,
                            0xAD3600,
                            0xF9C62C)
                    .autoShapes(
                            LIGHT_BARREL,
                            HEAVY_BARREL,
                            LIGHT_RECEIVER,
                            HEAVY_RECEIVER,
                            MECHANISM,
                            STOCK,
                            GRIP)
                    .memberShapes(INGOT, PLATE)
                    .namedId(INGOT, "ingot_gunmetal")
                    .namedId(PLATE, "plate_gunmetal")
                    .register();
    public static final Material WEAPON_STEEL =
            Material.define(
                            50,
                            MaterialTagRegistry.WEAPONSTEEL,
                            SMELTABLE,
                            0xA0A0A0,
                            0x000000,
                            0x808080)
                    .autoShapes(
                            CAST_PLATE,
                            SHELL,
                            LIGHT_BARREL,
                            HEAVY_BARREL,
                            LIGHT_RECEIVER,
                            HEAVY_RECEIVER,
                            MECHANISM,
                            STOCK,
                            GRIP)
                    .memberShapes(INGOT, PLATE)
                    .namedId(INGOT, "ingot_weaponsteel", "ingot_gunsteel")
                    .namedId(PLATE, "plate_weaponsteel", "plate_gunsteel")
                    .register();
    public static final Material SATURNITE =
            Material.define(34, MaterialTagRegistry.BIGMT, SMELTABLE, 0x3AC4DA, 0x09282C, 0x30A4B7)
                    .autoShapes(
                            PLATE,
                            CAST_PLATE,
                            SHELL,
                            BLOCK,
                            LIGHT_BARREL,
                            HEAVY_BARREL,
                            LIGHT_RECEIVER,
                            HEAVY_RECEIVER,
                            MECHANISM,
                            STOCK,
                            GRIP)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_saturnite")
                    .namedId(PLATE, "plate_saturnite")
                    .register();
    public static final Material NETHERITE =
            Material.define(
                            51,
                            MaterialTags.of("netherite"),
                            SMELTABLE,
                            0x737173,
                            0x111111,
                            0x625859)
                    .memberShapes(INGOT, BLOCK)
                    .register();
    public static final Material NETHERITE_SCRAP =
            Material.define(
                            6,
                            MaterialTags.of("netherite_scrap"),
                            ADDITIVE,
                            0x95867E,
                            0x24110B,
                            0x654740)
                    .register();
    public static final Material RARE_EARTH =
            Material.define(
                            20000,
                            MaterialTagRegistry.RAREEARTH,
                            NOT_SMELTABLE,
                            0xC1BDBD,
                            0x384646,
                            0x7B7F7F)
                    .autoShapes(FRAGMENT)
                    .memberShapes(INGOT, ORE)
                    .register();
    public static final Material POLYMER =
            Material.define(
                            20001,
                            MaterialTagRegistry.POLYMER,
                            NOT_SMELTABLE,
                            0x363636,
                            0x040404,
                            0x272727)
                    .autoShapes(STOCK, GRIP)
                    .memberShapes(INGOT, DUST, BLOCK)
                    .namedId(INGOT, "ingot_polymer")
                    .namedId(DUST, "powder_polymer")
                    .register();
    public static final Material BAKELITE =
            Material.define(
                            20002,
                            MaterialTagRegistry.BAKELITE,
                            NOT_SMELTABLE,
                            0xF28086,
                            0x2B0608,
                            0xC93940)
                    .autoShapes(STOCK, GRIP)
                    .memberShapes(INGOT, DUST, BLOCK)
                    .namedId(INGOT, "ingot_bakelite")
                    .namedId(DUST, "powder_bakelite")
                    .register();
    public static final Material RUBBER =
            Material.define(
                            20003,
                            MaterialTagRegistry.RUBBER,
                            NOT_SMELTABLE,
                            0x817F75,
                            0x0F0D03,
                            0x4B4A3F)
                    .autoShapes(PIPE, GRIP)
                    .memberShapes(INGOT, BLOCK)
                    .namedId(INGOT, "ingot_rubber")
                    .register();
    public static final Material POLYCARBONATE =
            Material.define(
                            20004,
                            MaterialTagRegistry.PC,
                            NOT_SMELTABLE,
                            0xEDE7C4,
                            0x908A67,
                            0xE1DBB8)
                    .autoShapes(STOCK, GRIP)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_pc")
                    .register();
    public static final Material PVC =
            Material.define(
                            20005,
                            MaterialTagRegistry.PVC,
                            NOT_SMELTABLE,
                            0xFCFCFC,
                            0x9F9F9F,
                            0xF0F0F0)
                    .autoShapes(STOCK, GRIP)
                    .memberShapes(INGOT)
                    .namedId(INGOT, "ingot_pvc")
                    .register();

    private Materials() {}

    static void load() {}
}
