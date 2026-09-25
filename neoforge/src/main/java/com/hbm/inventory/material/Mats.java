// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.material;

import com.hbm.hazard.HazardRegistry;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.items.machine.ItemScraps;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.util.I18nUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.inventory.material.MaterialShapes.*;

public class Mats {

    public static final List<NTMMaterial> orderedList = new ArrayList<>();
    public static final Map<Integer, NTMMaterial> matById = new HashMap<>();
    public static final Map<String, NTMMaterial> matByName = new HashMap<>();

    public static final Map<TagKey<Item>, MaterialStack> tagToMaterial = new HashMap<>();

    public static final int _VS = 0;
    public static final int _AS = 30;
    public static final int _ES = 20_000;

    public static final NTMMaterial MAT_WOOD =
            makeNonSmeltable(_VS + 3, WOOD, 0x896727, 0x281E0B, 0x896727)
                    .setAutogen(STOCK, GRIP)
                    .n();
    public static final NTMMaterial MAT_IVORY =
            makeNonSmeltable(_VS + 4, BONE, 0xFFFEEE, 0x797870, 0xEDEBCA).setAutogen(GRIP).n();
    public static final NTMMaterial MAT_STONE =
            makeSmeltable(_VS, df("stone"), 0x7F7F7F, 0x353535, 0x4D2F23).n();
    public static final NTMMaterial MAT_CARBON =
            makeAdditive(699, CARBON, 0x363636, 0x030303, 0x404040)
                    .setAutogen(WIRE, BLOCK)
                    .setTagOnly(INGOT)
                    .n();
    public static final NTMMaterial MAT_COAL =
            makeNonSmeltable(600, COAL, 0x363636, 0x030303, 0x404040)
                    .setConversion(MAT_CARBON, 2, 1)
                    .setAutogen(FRAGMENT, DUSTTINY, DUST)
                    .setTagOnly(GEM)
                    .coal(1D)
                    .legacy(DUST, "powder_coal")
                    .n();
    public static final NTMMaterial MAT_LIGNITE =
            makeNonSmeltable(601, LIGNITE, 0x542D0F, 0x261508, 0x472913)
                    .setConversion(MAT_CARBON, 3, 1)
                    .setAutogen(FRAGMENT)
                    .setTagOnly(GEM, DUST, ORE)
                    .coal(1D)
                    .legacy(DUST, "powder_lignite")
                    .n();
    public static final NTMMaterial MAT_COALCOKE =
            make(610, COALCOKE).setConversion(MAT_CARBON, 4, 3).setTagOnly(GEM, BLOCK).n();
    public static final NTMMaterial MAT_PETCOKE =
            make(611, PETCOKE).setConversion(MAT_CARBON, 4, 3).setTagOnly(GEM, BLOCK).n();
    public static final NTMMaterial MAT_LIGCOKE =
            make(612, LIGCOKE).setConversion(MAT_CARBON, 4, 3).setTagOnly(GEM, BLOCK).n();
    public static final NTMMaterial MAT_GRAPHITE =
            make(620, GRAPHITE)
                    .setConversion(MAT_CARBON, 1, 1)
                    .setTagOnly(INGOT, BLOCK)
                    .legacy(INGOT, "ingot_graphite")
                    .n();
    public static final NTMMaterial MAT_DIAMOND =
            makeNonSmeltable(1430, DIAMOND, 0xFFFFFF, 0x1B7B6B, 0x8CF4E2)
                    .setConversion(MAT_CARBON, 1, 1)
                    .setAutogen(FRAGMENT)
                    .setTagOnly(DUST, ORE)
                    .legacy(DUST, "powder_diamond")
                    .n();
    public static final NTMMaterial MAT_IRON =
            makeSmeltable(2600, IRON, 0xFFFFFF, 0x353535, 0xFFA259)
                    .setAutogen(FRAGMENT, DUST, PIPE, CASTPLATE, WELDEDPLATE, BLOCK)
                    .setTagOnly(INGOT, NUGGET, PLATE, ORE)
                    .legacy(DUST, "powder_iron")
                    .legacy(PLATE, "plate_iron")
                    .m();
    public static final NTMMaterial MAT_GOLD =
            makeSmeltable(7900, GOLD, 0xFFFF8B, 0xC26E00, 0xE8D754)
                    .setAutogen(FRAGMENT, WIRE, NUGGET, DUST, DENSEWIRE, CASTPLATE, BLOCK)
                    .setTagOnly(INGOT, PLATE, ORE)
                    .legacy(DUST, "powder_gold")
                    .legacy(PLATE, "plate_gold")
                    .m();
    public static final NTMMaterial MAT_REDSTONE =
            makeSmeltable(_VS + 1, REDSTONE, 0xE3260C, 0x700E06, 0xFF1000)
                    .setAutogen(FRAGMENT, INGOT)
                    .n();
    public static final NTMMaterial MAT_OBSIDIAN =
            makeSmeltable(_VS + 2, df("obsidian"), 0x3D234D, 0x3D234D, 0x3D234D).n();
    public static final NTMMaterial MAT_HEMATITE =
            makeAdditive(2601, HEMATITE, 0xDFB7AE, 0x5F372E, 0x6E463D).setTagOnly(ORE).m();
    public static final NTMMaterial MAT_WROUGHTIRON =
            makeSmeltable(2602, df("wrought_iron"), 0xFAAB89, 0xFAAB89, 0xFAAB89).m();
    public static final NTMMaterial MAT_PIGIRON =
            makeSmeltable(2603, df("pig_iron"), 0xFF8B59, 0xFF8B59, 0xFF8B59).m();
    public static final NTMMaterial MAT_METEORICIRON =
            makeSmeltable(2604, df("meteoric_iron"), 0x715347, 0x715347, 0x715347).m();
    public static final NTMMaterial MAT_MALACHITE =
            makeAdditive(2901, MALACHITE, 0xA2F0C8, 0x227048, 0x61AF87).setTagOnly(INGOT, ORE).m();
    public static final NTMMaterial MAT_BAUXITE =
            makeNonSmeltable(2902, BAUXITE, 0xF4BA30, 0xAA320A, 0xE2560F)
                    .setAutogen(FRAGMENT)
                    .setTagOnly(ORE)
                    .n();
    public static final NTMMaterial MAT_CRYOLITE =
            makeNonSmeltable(2903, CRYOLITE, 0xCBC2A4, 0x8B711F, 0x8B701A)
                    .setAutogen(FRAGMENT)
                    .setTagOnly(CRYSTAL)
                    .n();

    public static final NTMMaterial MAT_URANIUM =
            makeSmeltable(9200, U, 0xC1C7BD, 0x2B3227, 0x9AA196)
                    .setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .rad(HazardRegistry.u)
                    .legacy(INGOT, "ingot_uranium")
                    .legacy(DUST, "powder_uranium")
                    .legacy(NUGGET, "nugget_uranium")
                    .legacy(BILLET, "billet_uranium")
                    .m();
    public static final NTMMaterial MAT_U233 =
            makeSmeltable(9233, U233, 0xC1C7BD, 0x2B3227, 0x9AA196)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.u233)
                    .legacy(INGOT, "ingot_u233")
                    .legacy(NUGGET, "nugget_u233")
                    .legacy(BILLET, "billet_u233")
                    .m();
    public static final NTMMaterial MAT_U235 =
            makeSmeltable(9235, U235, 0xC1C7BD, 0x2B3227, 0x9AA196)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.u235)
                    .legacy(INGOT, "ingot_u235")
                    .legacy(NUGGET, "nugget_u235")
                    .legacy(BILLET, "billet_u235")
                    .m();
    public static final NTMMaterial MAT_U238 =
            makeSmeltable(9238, U238, 0xC1C7BD, 0x2B3227, 0x9AA196)
                    .setAutogen(FRAGMENT, NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.u238)
                    .legacy(INGOT, "ingot_u238")
                    .legacy(NUGGET, "nugget_u238")
                    .legacy(BILLET, "billet_u238")
                    .m();
    public static final NTMMaterial MAT_THORIUM =
            makeSmeltable(9032, TH232, 0xBF825F, 0x1C0000, 0xBF825F)
                    .setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .rad(HazardRegistry.th232)
                    .legacy(INGOT, "ingot_th232")
                    .legacy(DUST, "powder_thorium")
                    .legacy(NUGGET, "nugget_th232")
                    .legacy(BILLET, "billet_th232")
                    .m();
    public static final NTMMaterial MAT_PLUTONIUM =
            makeSmeltable(9400, PU, 0x9AA3A0, 0x111A17, 0x78817E)
                    .setAutogen(NUGGET, BILLET, DUST, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .rad(HazardRegistry.pu)
                    .legacy(INGOT, "ingot_plutonium")
                    .legacy(DUST, "powder_plutonium")
                    .legacy(NUGGET, "nugget_plutonium")
                    .legacy(BILLET, "billet_plutonium")
                    .m();
    public static final NTMMaterial MAT_RGP =
            makeSmeltable(9401, PURG, 0x9AA3A0, 0x111A17, 0x78817E)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.purg)
                    .legacy(INGOT, "ingot_pu_mix")
                    .legacy(NUGGET, "nugget_pu_mix")
                    .legacy(BILLET, "billet_pu_mix")
                    .m();
    public static final NTMMaterial MAT_PU238 =
            makeSmeltable(9438, PU238, 0xFFBC59, 0xFF8E2B, 0x78817E)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.pu238)
                    .hot(3D)
                    .legacy(INGOT, "ingot_pu238")
                    .legacy(NUGGET, "nugget_pu238")
                    .legacy(BILLET, "billet_pu238")
                    .m();
    public static final NTMMaterial MAT_PU239 =
            makeSmeltable(9439, PU239, 0x9AA3A0, 0x111A17, 0x78817E)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.pu239)
                    .legacy(INGOT, "ingot_pu239")
                    .legacy(NUGGET, "nugget_pu239")
                    .legacy(BILLET, "billet_pu239")
                    .m();
    public static final NTMMaterial MAT_PU240 =
            makeSmeltable(9440, PU240, 0x9AA3A0, 0x111A17, 0x78817E)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.pu240)
                    .legacy(INGOT, "ingot_pu240")
                    .legacy(NUGGET, "nugget_pu240")
                    .legacy(BILLET, "billet_pu240")
                    .m();
    public static final NTMMaterial MAT_PU241 =
            makeSmeltable(9441, PU241, 0x9AA3A0, 0x111A17, 0x78817E)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.pu241)
                    .legacy(INGOT, "ingot_pu241")
                    .legacy(NUGGET, "nugget_pu241")
                    .legacy(BILLET, "billet_pu241")
                    .m();
    public static final NTMMaterial MAT_RGA =
            makeSmeltable(9501, AMRG, 0xCEB3B9, 0x3A1C21, 0x93767B)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.amrg)
                    .legacy(INGOT, "ingot_am_mix")
                    .legacy(NUGGET, "nugget_am_mix")
                    .legacy(BILLET, "billet_am_mix")
                    .m();
    public static final NTMMaterial MAT_AM241 =
            makeSmeltable(9541, AM241, 0xCEB3B9, 0x3A1C21, 0x93767B)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.am241)
                    .legacy(INGOT, "ingot_am241")
                    .legacy(NUGGET, "nugget_am241")
                    .legacy(BILLET, "billet_am241")
                    .m();
    public static final NTMMaterial MAT_AM242 =
            makeSmeltable(9542, AM242, 0xCEB3B9, 0x3A1C21, 0x93767B)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.am242)
                    .legacy(INGOT, "ingot_am242")
                    .legacy(NUGGET, "nugget_am242")
                    .legacy(BILLET, "billet_am242")
                    .m();
    public static final NTMMaterial MAT_NEPTUNIUM =
            makeSmeltable(9337, NP237, 0xA6B2A6, 0x030F03, 0x647064)
                    .setAutogen(NUGGET, BILLET, DUST, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.np237)
                    .legacy(INGOT, "ingot_neptunium")
                    .legacy(DUST, "powder_neptunium")
                    .legacy(NUGGET, "nugget_neptunium")
                    .legacy(BILLET, "billet_neptunium")
                    .m();
    public static final NTMMaterial MAT_POLONIUM =
            makeSmeltable(8410, PO210, 0x968779, 0x3D1509, 0x715E4A)
                    .setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.po210)
                    .hot(3D)
                    .legacy(INGOT, "ingot_polonium")
                    .legacy(DUST, "powder_polonium")
                    .legacy(NUGGET, "nugget_polonium")
                    .legacy(BILLET, "billet_polonium")
                    .m();
    public static final NTMMaterial MAT_TECHNETIUM =
            makeSmeltable(4399, TC99, 0xFAFFFF, 0x576C6C, 0xCADFDF)
                    .setAutogen(FRAGMENT, NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.tc99)
                    .legacy(INGOT, "ingot_technetium")
                    .legacy(NUGGET, "nugget_technetium")
                    .legacy(BILLET, "billet_technetium")
                    .m();
    public static final NTMMaterial MAT_RADIUM =
            makeSmeltable(8826, RA226, 0xFCFCFC, 0xADBFBA, 0xE9FAF6)
                    .setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.ra226)
                    .legacy(INGOT, "ingot_ra226")
                    .legacy(DUST, "powder_ra226")
                    .legacy(NUGGET, "nugget_ra226")
                    .legacy(BILLET, "billet_ra226")
                    .m();
    public static final NTMMaterial MAT_ACTINIUM =
            makeSmeltable(8927, AC227, 0xECE0E0, 0x221616, 0x958989)
                    .setAutogen(NUGGET, BILLET)
                    .setTagOnly(INGOT, DUST, BLOCK, DUSTTINY)
                    .rad(HazardRegistry.ac227)
                    .legacy(INGOT, "ingot_actinium")
                    .legacy(DUST, "powder_actinium")
                    .legacy(NUGGET, "nugget_actinium")
                    .legacy(BILLET, "billet_actinium")
                    .m();
    public static final NTMMaterial MAT_CO60 =
            makeSmeltable(2760, CO60, 0xC2D1EE, 0x353554, 0x8F72AE)
                    .setAutogen(NUGGET, BILLET, DUST)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.co60)
                    .hot(1D)
                    .legacy(INGOT, "ingot_co60")
                    .legacy(DUST, "powder_co60")
                    .legacy(NUGGET, "nugget_co60")
                    .legacy(BILLET, "billet_co60")
                    .m();
    public static final NTMMaterial MAT_AU198 =
            makeSmeltable(7998, AU198, 0xFFFF8B, 0xC26E00, 0xE8D754)
                    .setAutogen(NUGGET, BILLET, DUST)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.au198)
                    .hot(5D)
                    .legacy(INGOT, "ingot_au198")
                    .legacy(DUST, "powder_au198")
                    .legacy(NUGGET, "nugget_au198")
                    .legacy(BILLET, "billet_au198")
                    .m();
    public static final NTMMaterial MAT_PB209 =
            makeSmeltable(8209, PB209, 0xB38A94, 0x12020E, 0x7B535D)
                    .setAutogen(NUGGET, BILLET)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.pb209)
                    .blinding(50D)
                    .hot(7D)
                    .legacy(INGOT, "ingot_pb209")
                    .legacy(NUGGET, "nugget_pb209")
                    .legacy(BILLET, "billet_pb209")
                    .m();
    public static final NTMMaterial MAT_SCHRABIDIUM =
            makeSmeltable(12626, SA326, 0x32FFFF, 0x005C5C, 0x32FFFF)
                    .setAutogen(NUGGET, WIRE, BILLET, DUST, DENSEWIRE, PLATE, CASTPLATE, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .rad(HazardRegistry.sa326)
                    .blinding(50D)
                    .legacy(INGOT, "ingot_schrabidium")
                    .legacy(DUST, "powder_schrabidium")
                    .legacy(NUGGET, "nugget_schrabidium")
                    .legacy(BILLET, "billet_schrabidium")
                    .legacy(PLATE, "plate_schrabidium")
                    .m();
    public static final NTMMaterial MAT_SOLINIUM =
            makeSmeltable(12627, SA327, 0xA2E6E0, 0x00433D, 0x72B6B0)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.sa327)
                    .blinding(50D)
                    .legacy(INGOT, "ingot_solinium")
                    .legacy(NUGGET, "nugget_solinium")
                    .legacy(BILLET, "billet_solinium")
                    .m();
    public static final NTMMaterial MAT_SCHRABIDATE =
            makeSmeltable(12600, SBD, 0x77C0D7, 0x39005E, 0x6589B4)
                    .setAutogen(DUST, DENSEWIRE, CASTPLATE, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.sb)
                    .blinding(50D)
                    .legacy(INGOT, "ingot_schrabidate")
                    .legacy(DUST, "powder_schrabidate")
                    .m();
    public static final NTMMaterial MAT_SCHRARANIUM =
            makeSmeltable(12601, SRN, 0x2B3227, 0x2B3227, 0x24AFAC)
                    .setAutogen(BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.sr)
                    .blinding(50D)
                    .legacy(INGOT, "ingot_schraranium")
                    .m();
    public static final NTMMaterial MAT_GHIORSIUM =
            makeSmeltable(12836, GH336, 0xF4EFE1, 0x2A3306, 0xC6C6A1)
                    .setAutogen(NUGGET, BILLET, BLOCK)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.gh336)
                    .legacy(INGOT, "ingot_gh336")
                    .legacy(NUGGET, "nugget_gh336")
                    .legacy(BILLET, "billet_gh336")
                    .m();

    public static final NTMMaterial MAT_TITANIUM =
            makeSmeltable(2200, TI, 0xF7F3F2, 0x4F4C4B, 0xA99E79)
                    .setAutogen(
                            FRAGMENT, DUST, PLATE, DENSEWIRE, CASTPLATE, WELDEDPLATE, SHELL, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .legacy(INGOT, "ingot_titanium")
                    .legacy(DUST, "powder_titanium")
                    .legacy(PLATE, "plate_titanium")
                    .m();
    public static final NTMMaterial MAT_COPPER =
            makeSmeltable(2900, CU, 0xFDCA88, 0x601E0D, 0xC18336)
                    .setAutogen(
                            FRAGMENT,
                            WIRE,
                            DUST,
                            PLATE,
                            DENSEWIRE,
                            CASTPLATE,
                            WELDEDPLATE,
                            SHELL,
                            PIPE,
                            BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .legacy(DUST, "powder_copper")
                    .legacy(PLATE, "plate_copper")
                    .m();
    public static final NTMMaterial MAT_TUNGSTEN =
            makeSmeltable(7400, W, 0x868686, 0x000000, 0x977474)
                    .setAutogen(
                            FRAGMENT, WIRE, BOLT, DUST, DENSEWIRE, CASTPLATE, WELDEDPLATE, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .legacy(INGOT, "ingot_tungsten")
                    .legacy(DUST, "powder_tungsten")
                    .m();
    public static final NTMMaterial MAT_ALUMINIUM =
            makeSmeltable(1300, AL, 0xFFFFFF, 0x344550, 0xD0B8EB)
                    .setAutogen(
                            FRAGMENT, WIRE, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .legacy(INGOT, "ingot_aluminium")
                    .legacy(DUST, "powder_aluminium")
                    .legacy(PLATE, "plate_aluminium")
                    .m();
    public static final NTMMaterial MAT_LEAD =
            makeSmeltable(8200, PB, 0xA6A6B2, 0x03030F, 0x646470)
                    .setAutogen(FRAGMENT, NUGGET, WIRE, BOLT, DUST, PLATE, CASTPLATE, PIPE, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .legacy(INGOT, "ingot_lead")
                    .legacy(DUST, "powder_lead")
                    .legacy(NUGGET, "nugget_lead")
                    .legacy(PLATE, "plate_lead")
                    .m();
    public static final NTMMaterial MAT_BISMUTH =
            makeSmeltable(8300, BI, 0xB200FF, 0xB200FF, 0xB200FF)
                    .setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_bismuth")
                    .legacy(DUST, "powder_bismuth")
                    .legacy(NUGGET, "nugget_bismuth")
                    .legacy(BILLET, "billet_bismuth")
                    .m();
    public static final NTMMaterial MAT_ARSENIC =
            makeSmeltable(3300, AS, 0x6CBABA, 0x242525, 0x558080)
                    .setAutogen(NUGGET)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_arsenic")
                    .legacy(NUGGET, "nugget_arsenic")
                    .m();
    public static final NTMMaterial MAT_TANTALIUM =
            makeSmeltable(7300, TA, 0xFFFFFF, 0x1D1D36, 0xA89B74)
                    .setAutogen(FRAGMENT, NUGGET, DUST, BLOCK)
                    .setTagOnly(GEM, INGOT)
                    .legacy(INGOT, "ingot_tantalium")
                    .legacy(DUST, "powder_tantalium")
                    .legacy(NUGGET, "nugget_tantalium")
                    .m();
    public static final NTMMaterial MAT_NEODYMIUM =
            makeSmeltable(6000, ND, 0xE6E6B6, 0x1C1C00, 0x8F8F5F)
                    .setAutogen(FRAGMENT, NUGGET, DUSTTINY, INGOT, DUST, DENSEWIRE, BLOCK)
                    .setTagOnly(ORE)
                    .legacy(DUST, "powder_neodymium")
                    .m();
    public static final NTMMaterial MAT_NIOBIUM =
            makeSmeltable(4100, NB, 0xB76EC9, 0x2F2D42, 0xD576B1)
                    .setAutogen(FRAGMENT, NUGGET, DUSTTINY, DUST, DENSEWIRE, BLOCK)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_niobium")
                    .legacy(DUST, "powder_niobium")
                    .legacy(NUGGET, "nugget_niobium")
                    .m();
    public static final NTMMaterial MAT_BERYLLIUM =
            makeSmeltable(400, BE, 0xB2B2A6, 0x0F0F03, 0xAE9572)
                    .setAutogen(FRAGMENT, NUGGET, DUST, BLOCK)
                    .setTagOnly(BILLET, INGOT, ORE)
                    .legacy(INGOT, "ingot_beryllium")
                    .legacy(DUST, "powder_beryllium")
                    .legacy(NUGGET, "nugget_beryllium")
                    .legacy(BILLET, "billet_beryllium")
                    .m();
    public static final NTMMaterial MAT_EMERALD =
            makeNonSmeltable(401, EMERALD, 0xBAFFD4, 0x003900, 0x17DD62)
                    .setConversion(MAT_BERYLLIUM, 4, 3)
                    .setAutogen(FRAGMENT, DUST, GEM, BLOCK)
                    .setTagOnly(ORE)
                    .legacy(DUST, "powder_emerald")
                    .n();
    public static final NTMMaterial MAT_COBALT =
            makeSmeltable(2700, CO, 0xC2D1EE, 0x353554, 0x8F72AE)
                    .setAutogen(FRAGMENT, NUGGET, DUSTTINY, BILLET, DUST, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .legacy(INGOT, "ingot_cobalt")
                    .legacy(DUST, "powder_cobalt")
                    .legacy(NUGGET, "nugget_cobalt")
                    .legacy(BILLET, "billet_cobalt")
                    .m();
    public static final NTMMaterial MAT_BORON =
            makeSmeltable(500, B, 0xBDC8D2, 0x29343E, 0xAD72AE)
                    .setAutogen(FRAGMENT, DUSTTINY, DUST, BLOCK)
                    .setTagOnly(NUGGET, INGOT)
                    .legacy(INGOT, "ingot_boron")
                    .legacy(DUST, "powder_boron")
                    .m();
    public static final NTMMaterial MAT_BORAX =
            makeSmeltable(501, BORAX, 0xFFFFFF, 0x946E23, 0xFFECC6)
                    .setAutogen(FRAGMENT, INGOT, DUST)
                    .setTagOnly(ORE)
                    .legacy(DUST, "powder_borax")
                    .n();
    public static final NTMMaterial MAT_LANTHANIUM =
            makeSmeltable(5700, LA, 0xC8E0E0, 0x3B5353, 0xA1B9B9)
                    .setAutogen(FRAGMENT, BLOCK)
                    .setTagOnly(NUGGET, INGOT, DUSTTINY, DUST)
                    .legacy(INGOT, "ingot_lanthanium")
                    .legacy(DUST, "powder_lanthanium")
                    .m();
    public static final NTMMaterial MAT_ZIRCONIUM =
            makeSmeltable(4000, ZR, 0xE3DCBE, 0x3E3719, 0xADA688)
                    .setAutogen(FRAGMENT, NUGGET, WIRE, BILLET, DUST, CASTPLATE, WELDEDPLATE, BLOCK)
                    .setTagOnly(INGOT, ORE)
                    .legacy(INGOT, "ingot_zirconium")
                    .legacy(DUST, "powder_zirconium")
                    .legacy(NUGGET, "nugget_zirconium")
                    .legacy(BILLET, "billet_zirconium")
                    .m();
    public static final NTMMaterial MAT_SODIUM =
            makeSmeltable(1100, NA, 0xD3BF9E, 0x3A5A6B, 0x7E9493)
                    .setAutogen(FRAGMENT, INGOT, DUST)
                    .legacy(DUST, "powder_sodium")
                    .m();
    public static final NTMMaterial MAT_SODALITE =
            makeNonSmeltable(1101, SODALITE, 0xDCE5F6, 0x4927B4, 0x96A7E6)
                    .setAutogen(FRAGMENT, GEM)
                    .n();
    public static final NTMMaterial MAT_STRONTIUM =
            makeSmeltable(3800, SR, 0xF1E8BA, 0x271E00, 0xCAC193)
                    .setAutogen(FRAGMENT, INGOT, DUST)
                    .legacy(DUST, "powder_strontium")
                    .m();
    public static final NTMMaterial MAT_CALCIUM =
            makeSmeltable(2000, CA, 0xCFCFA6, 0x747F6E, 0xB7B784)
                    .setAutogen(DUST)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_calcium")
                    .legacy(DUST, "powder_calcium")
                    .m();
    public static final NTMMaterial MAT_LITHIUM =
            makeSmeltable(300, LI, 0xFFFFFF, 0x818181, 0xD6D6D6)
                    .setAutogen(FRAGMENT, DUST, BLOCK)
                    .setTagOnly(INGOT, DUSTTINY, ORE)
                    .legacy(DUST, "powder_lithium")
                    .m();
    public static final NTMMaterial MAT_SULFUR =
            makeNonSmeltable(1600, S, 0xFCEE80, 0xBDA022, 0xF1DF68)
                    .setAutogen(FRAGMENT, DUST, BLOCK)
                    .setTagOnly(ORE)
                    .n();
    public static final NTMMaterial MAT_KNO =
            makeNonSmeltable(700, KNO, 0xD4D4D4, 0x969696, 0xC9C9C9)
                    .setAutogen(FRAGMENT, DUST, BLOCK)
                    .setTagOnly(ORE)
                    .n();
    public static final NTMMaterial MAT_FLUORITE =
            makeNonSmeltable(900, F, 0xFFFFFF, 0xB0A192, 0xE1DBD4)
                    .setAutogen(FRAGMENT, DUST, BLOCK)
                    .setTagOnly(ORE)
                    .n();
    public static final NTMMaterial MAT_PHOSPHORUS =
            makeNonSmeltable(1500, P_RED, 0xCB0213, 0x600006, 0xBA0615)
                    .setAutogen(FRAGMENT, DUST, BLOCK)
                    .legacy(DUST, "powder_fire", "powder_fire")
                    .n();

    public static final NTMMaterial MAT_IODINE =
            makeNonSmeltable(5300, I, 0x7A8796, 0x2B1C43, 0x50556E)
                    .setAutogen(DUST)
                    .legacy(DUST, "powder_iodine")
                    .n();
    public static final NTMMaterial MAT_CHLOROCALCITE =
            makeNonSmeltable(1701, CHLOROCALCITE, 0xF7E761, 0x475B46, 0xB8B963)
                    .setAutogen(FRAGMENT, DUST)
                    .legacy(DUST, "powder_chlorocalcite")
                    .n();
    public static final NTMMaterial MAT_MOLYSITE =
            makeNonSmeltable(1702, MOLYSITE, 0xF9E97B, 0x216E00, 0xD0D264)
                    .setAutogen(FRAGMENT, DUST)
                    .setTagOnly(ORE)
                    .legacy(DUST, "powder_molysite")
                    .n();
    public static final NTMMaterial MAT_CINNABAR =
            makeNonSmeltable(8001, CINNABAR, 0xD87070, 0x993030, 0xBF4E4E)
                    .setAutogen(FRAGMENT, GEM)
                    .setTagOnly(CRYSTAL, ORE)
                    .n();

    public static final NTMMaterial MAT_CADMIUM =
            makeSmeltable(4800, CD, 0xFFFADE, 0x350000, 0xA85600)
                    .setAutogen(DUST)
                    .setTagOnly(INGOT, BLOCK)
                    .legacy(INGOT, "ingot_cadmium")
                    .legacy(DUST, "powder_cadmium")
                    .m();
    public static final NTMMaterial MAT_SILICON =
            makeSmeltable(1400, SI, 0xD1D7DF, 0x1A1A3D, 0x878B9E)
                    .setAutogen(FRAGMENT, NUGGET, BILLET)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_silicon")
                    .legacy(NUGGET, "nugget_silicon")
                    .legacy(BILLET, "billet_silicon")
                    .m();
    public static final NTMMaterial MAT_ASBESTOS =
            makeSmeltable(1401, ASBESTOS, 0xD8D9CF, 0x616258, 0xB0B3A8)
                    .setAutogen(FRAGMENT, BLOCK)
                    .setTagOnly(INGOT, DUST, ORE)
                    .asbestos(1D)
                    .legacy(INGOT, "ingot_asbestos")
                    .legacy(DUST, "powder_asbestos")
                    .n();
    public static final NTMMaterial MAT_OSMIRIDIUM =
            makeSmeltable(7699, OSMIRIDIUM, 0xDBE3EF, 0x7891BE, 0xACBDD9)
                    .setAutogen(NUGGET, CASTPLATE, WELDEDPLATE)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_osmiridium")
                    .legacy(NUGGET, "nugget_osmiridium")
                    .m();

    public static final NTMMaterial MAT_STEEL =
            makeSmeltable(_AS, STEEL, 0xAFAFAF, 0x0F0F0F, 0x4A4A4A)
                    .setAutogen(
                            DUSTTINY,
                            BOLT,
                            WIRE,
                            DUST,
                            PLATE,
                            CASTPLATE,
                            WELDEDPLATE,
                            SHELL,
                            PIPE,
                            BLOCK,
                            LIGHTBARREL,
                            HEAVYBARREL,
                            LIGHTRECEIVER,
                            GRIP)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_steel")
                    .legacy(DUST, "powder_steel")
                    .legacy(PLATE, "plate_steel")
                    .m();
    public static final NTMMaterial MAT_MINGRADE =
            makeSmeltable(_AS + 1, MINGRADE, 0xFFBA7D, 0xAF1700, 0xE44C0F)
                    .setAutogen(WIRE, DUST, DENSEWIRE, BLOCK)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_red_copper")
                    .legacy(DUST, "powder_red_copper")
                    .m();
    public static final NTMMaterial MAT_DURA =
            makeSmeltable(_AS + 3, DURA, 0x82A59C, 0x06281E, 0x42665C)
                    .setAutogen(
                            BOLT,
                            DUST,
                            PLATE,
                            CASTPLATE,
                            PIPE,
                            BLOCK,
                            LIGHTBARREL,
                            HEAVYBARREL,
                            LIGHTRECEIVER,
                            HEAVYRECEIVER,
                            GRIP)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_dura_steel")
                    .legacy(DUST, "powder_dura_steel")
                    .legacy(PLATE, "plate_dura_steel")
                    .m();
    public static final NTMMaterial MAT_DESH =
            makeSmeltable(_AS + 12, DESH, 0xFF6D6D, 0x720000, 0xF22929)
                    .setAutogen(
                            DUST,
                            CASTPLATE,
                            BLOCK,
                            LIGHTBARREL,
                            HEAVYBARREL,
                            LIGHTRECEIVER,
                            STOCK,
                            GRIP)
                    .setTagOnly(NUGGET, INGOT)
                    .legacy(INGOT, "ingot_desh")
                    .legacy(DUST, "powder_desh")
                    .legacy(NUGGET, "nugget_desh")
                    .m();
    public static final NTMMaterial MAT_STAR =
            makeSmeltable(_AS + 5, STAR, 0xCCCCEA, 0x11111A, 0xA5A5D3)
                    .setAutogen(DUST, DENSEWIRE, CASTPLATE, BLOCK)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_starmetal")
                    .m();
    public static final NTMMaterial MAT_FERRO =
            makeSmeltable(_AS + 7, FERRO, 0xB7B7C9, 0x101022, 0x6B6B8B)
                    .setAutogen(CASTPLATE, HEAVYBARREL, HEAVYRECEIVER)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_ferrouranium")
                    .m();
    public static final NTMMaterial MAT_TCALLOY =
            makeSmeltable(_AS + 6, TCALLOY, 0xD4D6D6, 0x323D3D, 0x9CA6A6)
                    .setAutogen(
                            DUST,
                            CASTPLATE,
                            WELDEDPLATE,
                            LIGHTBARREL,
                            HEAVYBARREL,
                            LIGHTRECEIVER,
                            HEAVYRECEIVER)
                    .setTagOnly(INGOT, BLOCK)
                    .legacy(INGOT, "ingot_tcalloy")
                    .legacy(DUST, "powder_tcalloy")
                    .m();
    public static final NTMMaterial MAT_CDALLOY =
            makeSmeltable(_AS + 13, CDALLOY, 0xF7DF8F, 0x604308, 0xFBD368)
                    .setAutogen(
                            CASTPLATE,
                            WELDEDPLATE,
                            LIGHTBARREL,
                            HEAVYBARREL,
                            LIGHTRECEIVER,
                            HEAVYRECEIVER)
                    .setTagOnly(INGOT, BLOCK)
                    .legacy(INGOT, "ingot_cdalloy")
                    .m();
    public static final NTMMaterial MAT_BBRONZE =
            makeSmeltable(_AS + 16, BBRONZE, 0xE19A69, 0x485353, 0x987D65)
                    .setAutogen(CASTPLATE, LIGHTBARREL, LIGHTRECEIVER, HEAVYRECEIVER)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_bismuth_bronze")
                    .m();
    public static final NTMMaterial MAT_ABRONZE =
            makeSmeltable(_AS + 17, ABRONZE, 0xDB9462, 0x203331, 0x77644D)
                    .setAutogen(CASTPLATE, LIGHTBARREL, LIGHTRECEIVER, HEAVYRECEIVER)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_arsenic_bronze")
                    .m();
    public static final NTMMaterial MAT_BSCCO =
            makeSmeltable(_AS + 18, BSCCO, 0x767BF1, 0x000000, 0x5E62C0)
                    .setAutogen(DENSEWIRE)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_bscco")
                    .m();
    public static final NTMMaterial MAT_MAGTUNG =
            makeSmeltable(_AS + 8, MAGTUNG, 0x22A2A2, 0x0F0F0F, 0x22A2A2)
                    .setAutogen(WIRE, DUST, DENSEWIRE, BLOCK)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_magnetized_tungsten")
                    .legacy(DUST, "powder_magnetized_tungsten")
                    .m();
    public static final NTMMaterial MAT_CMB =
            makeSmeltable(_AS + 9, CMB, 0x6F6FB4, 0x000011, 0x6F6FB4)
                    .setAutogen(DUST, PLATE, CASTPLATE, WELDEDPLATE, BLOCK)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_combine_steel")
                    .legacy(DUST, "powder_combine_steel")
                    .legacy(PLATE, "plate_combine_steel")
                    .m();
    public static final NTMMaterial MAT_DNT =
            makeSmeltable(_AS + 15, DNT, 0x7582B9, 0x16000E, 0x455289)
                    .setAutogen(DUST, DENSEWIRE, BLOCK)
                    .setTagOnly(NUGGET, INGOT)
                    .legacy(INGOT, "ingot_dineutronium")
                    .legacy(DUST, "powder_dineutronium")
                    .legacy(NUGGET, "nugget_dineutronium")
                    .m();
    public static final NTMMaterial MAT_FLUX =
            makeAdditive(_AS + 10, df("flux"), 0xF1E0BB, 0x6F6256, 0xDECCAD).setAutogen(DUST).n();
    public static final NTMMaterial MAT_SLAG =
            makeSmeltable(_AS + 11, SLAG, 0x554940, 0x34281F, 0x6C6562)
                    .setAutogen(INGOT, BLOCK)
                    .n();
    public static final NTMMaterial MAT_MUD =
            makeSmeltable(_AS + 14, MUD, 0xBCB5A9, 0x481213, 0x96783B)
                    .setTagOnly(INGOT)
                    .rad(HazardRegistry.mud)
                    .legacy(INGOT, "ingot_mud")
                    .n();
    public static final NTMMaterial MAT_GUNMETAL =
            makeSmeltable(_AS + 19, GUNMETAL, 0xFFEF3F, 0xAD3600, 0xF9C62C)
                    .setAutogen(
                            LIGHTBARREL,
                            HEAVYBARREL,
                            LIGHTRECEIVER,
                            HEAVYRECEIVER,
                            MECHANISM,
                            STOCK,
                            GRIP)
                    .setTagOnly(INGOT, PLATE)
                    .legacy(INGOT, "ingot_gunmetal")
                    .legacy(PLATE, "plate_gunmetal")
                    .n();
    public static final NTMMaterial MAT_WEAPONSTEEL =
            makeSmeltable(_AS + 20, WEAPONSTEEL, 0xA0A0A0, 0x000000, 0x808080)
                    .setAutogen(
                            CASTPLATE,
                            SHELL,
                            LIGHTBARREL,
                            HEAVYBARREL,
                            LIGHTRECEIVER,
                            HEAVYRECEIVER,
                            MECHANISM,
                            STOCK,
                            GRIP)
                    .setTagOnly(INGOT, PLATE)
                    .legacy(INGOT, "ingot_weaponsteel", "ingot_gunsteel")
                    .legacy(PLATE, "plate_weaponsteel", "plate_gunsteel")
                    .n();
    public static final NTMMaterial MAT_SATURN =
            makeSmeltable(_AS + 4, BIGMT, 0x3AC4DA, 0x09282C, 0x30A4B7)
                    .setAutogen(
                            PLATE,
                            CASTPLATE,
                            SHELL,
                            BLOCK,
                            LIGHTBARREL,
                            HEAVYBARREL,
                            LIGHTRECEIVER,
                            HEAVYRECEIVER,
                            MECHANISM,
                            STOCK,
                            GRIP)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_saturnite")
                    .legacy(PLATE, "plate_saturnite")
                    .m();

    public static final NTMMaterial MAT_RAREEARTH =
            makeNonSmeltable(_ES, RAREEARTH, 0xC1BDBD, 0x384646, 0x7B7F7F)
                    .setAutogen(FRAGMENT)
                    .setTagOnly(INGOT, ORE)
                    .n();
    public static final NTMMaterial MAT_POLYMER =
            makeNonSmeltable(_ES + 1, POLYMER, 0x363636, 0x040404, 0x272727)
                    .setAutogen(STOCK, GRIP)
                    .setTagOnly(INGOT, DUST, BLOCK)
                    .legacy(INGOT, "ingot_polymer")
                    .legacy(DUST, "powder_polymer")
                    .n();
    public static final NTMMaterial MAT_BAKELITE =
            makeNonSmeltable(_ES + 2, BAKELITE, 0xF28086, 0x2B0608, 0xC93940)
                    .setAutogen(STOCK, GRIP)
                    .setTagOnly(INGOT, DUST, BLOCK)
                    .legacy(INGOT, "ingot_bakelite")
                    .legacy(DUST, "powder_bakelite")
                    .n();
    public static final NTMMaterial MAT_RUBBER =
            makeNonSmeltable(_ES + 3, RUBBER, 0x817F75, 0x0F0D03, 0x4B4A3F)
                    .setAutogen(PIPE, GRIP)
                    .setTagOnly(INGOT, BLOCK)
                    .legacy(INGOT, "ingot_rubber")
                    .n();
    public static final NTMMaterial MAT_HARDPLASTIC =
            makeNonSmeltable(_ES + 4, PC, 0xEDE7C4, 0x908A67, 0xE1DBB8)
                    .setAutogen(STOCK, GRIP)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_pc")
                    .n();
    public static final NTMMaterial MAT_PVC =
            makeNonSmeltable(_ES + 5, PVC, 0xFCFCFC, 0x9F9F9F, 0xF0F0F0)
                    .setAutogen(STOCK, GRIP)
                    .setTagOnly(INGOT)
                    .legacy(INGOT, "ingot_pvc")
                    .n();

    static {
        for (NTMMaterial mat : orderedList) {
            for (MaterialShapes shape : MaterialShapes.allShapes) {
                if (shape.tagPathPlural == null || shape.q(1) == 0) continue;
                tagToMaterial.put(shape.tagFor(mat.tagPath), new MaterialStack(mat, shape.q(1)));
            }
        }
    }

    public static NTMMaterial make(int id, DictFrame dict) {
        return new NTMMaterial(id, dict);
    }

    public static NTMMaterial makeSmeltable(
            int id, DictFrame dict, int solidColorLight, int solidColorDark, int moltenColor) {
        return new NTMMaterial(id, dict)
                .smeltable(NTMMaterial.SmeltingBehavior.SMELTABLE)
                .setSolidColor(solidColorLight, solidColorDark)
                .setMoltenColor(moltenColor);
    }

    public static NTMMaterial makeAdditive(
            int id, DictFrame dict, int solidColorLight, int solidColorDark, int moltenColor) {
        return new NTMMaterial(id, dict)
                .smeltable(NTMMaterial.SmeltingBehavior.ADDITIVE)
                .setSolidColor(solidColorLight, solidColorDark)
                .setMoltenColor(moltenColor);
    }

    public static NTMMaterial makeNonSmeltable(
            int id, DictFrame dict, int solidColorLight, int solidColorDark, int moltenColor) {
        return new NTMMaterial(id, dict)
                .smeltable(NTMMaterial.SmeltingBehavior.NOT_SMELTABLE)
                .setSolidColor(solidColorLight, solidColorDark)
                .setMoltenColor(moltenColor);
    }

    public static DictFrame df(String segment) {
        return new DictFrame(segment);
    }

    public static List<MaterialStack> getMaterialsFromItem(ItemStack stack) {
        List<MaterialStack> list = new ArrayList<>();
        if (stack.isEmpty()) return list;

        MatDistribution.Distribution melt = MatDistribution.live();

        var holder = stack.getItem().builtInRegistryHolder();

        var tags = holder.tags().iterator();
        while (tags.hasNext()) {
            List<MaterialStack> oreEntries = melt.byOreTag().get(tags.next());
            if (oreEntries == null) continue;
            for (MaterialStack entry : oreEntries) list.add(entry.copy());
            break;
        }

        if (list.isEmpty()) {
            tags = holder.tags().iterator();
            while (tags.hasNext()) {
                MaterialStack hit = tagToMaterial.get(tags.next());
                if (hit == null) continue;

                SmeltingBehavior into = hit.material.smeltsInto.smeltable;
                if (into != SmeltingBehavior.SMELTABLE && into != SmeltingBehavior.ADDITIVE)
                    continue;
                list.add(hit.copy());
                break;
            }
        }

        List<MaterialStack> entries = melt.byItem().get(stack.getItem());
        if (entries != null)
            for (MaterialStack entry : entries) if (entry != null) list.add(entry.copy());

        MaterialStack scrap = ItemScraps.getMats(stack);
        if (scrap != null) list.add(scrap);

        return list;
    }

    public static Map<Ingredient, List<MaterialStack>> smeltingCatalog() {
        Map<Ingredient, List<MaterialStack>> out = new LinkedHashMap<>();
        MatDistribution.Distribution melt = MatDistribution.live();

        for (Map.Entry<TagKey<Item>, MaterialStack> entry : tagToMaterial.entrySet()) {
            MaterialStack shape = entry.getValue();
            NTMMaterial into = shape.material.smeltsInto;
            if (into.smeltable != SmeltingBehavior.SMELTABLE
                    && into.smeltable != SmeltingBehavior.ADDITIVE) {
                continue;
            }
            var members = BuiltInRegistries.ITEM.get(entry.getKey());
            if (members.isEmpty()) continue;
            int amount =
                    (int) ((long) shape.amount * shape.material.convOut / shape.material.convIn);
            out.put(Ingredient.of(members.get()), List.of(new MaterialStack(into, amount)));
        }

        for (Map.Entry<TagKey<Item>, List<MaterialStack>> entry : melt.byOreTag().entrySet()) {
            BuiltInRegistries.ITEM
                    .get(entry.getKey())
                    .ifPresent(
                            members -> out.put(Ingredient.of(members), copies(entry.getValue())));
        }

        for (Map.Entry<Item, List<MaterialStack>> entry : melt.byItem().entrySet()) {
            out.put(Ingredient.of(entry.getKey()), copies(entry.getValue()));
        }

        return out;
    }

    private static List<MaterialStack> copies(List<MaterialStack> materials) {
        List<MaterialStack> out = new ArrayList<>(materials.size());
        for (MaterialStack material : materials) out.add(material.copy());
        return out;
    }

    public static List<MaterialStack> getSmeltingMaterialsFromItem(ItemStack stack) {
        List<MaterialStack> base = getMaterialsFromItem(stack);
        List<MaterialStack> out = new ArrayList<>(base.size());
        for (MaterialStack m : base) {
            int amount = (int) ((long) m.amount * m.material.convOut / m.material.convIn);
            out.add(new MaterialStack(m.material.smeltsInto, amount));
        }
        return out;
    }

    public static String formatAmount(int amount, boolean showInMb) {
        if (showInMb) return (amount * 2) + "mB";

        StringBuilder out = new StringBuilder();

        int blocks = amount / BLOCK.q(1);
        amount -= BLOCK.q(blocks);
        int ingots = amount / INGOT.q(1);
        amount -= INGOT.q(ingots);
        int nuggets = amount / NUGGET.q(1);
        amount -= NUGGET.q(nuggets);
        int quanta = amount;

        if (blocks > 0)
            out.append(
                            I18nUtil.resolveKey(
                                    blocks == 1 ? "matshape.block" : "matshape.blocks", blocks))
                    .append(' ');
        if (ingots > 0)
            out.append(
                            I18nUtil.resolveKey(
                                    ingots == 1 ? "matshape.ingot" : "matshape.ingots", ingots))
                    .append(' ');
        if (nuggets > 0)
            out.append(
                            I18nUtil.resolveKey(
                                    nuggets == 1 ? "matshape.nugget" : "matshape.nuggets", nuggets))
                    .append(' ');
        if (quanta > 0)
            out.append(
                    I18nUtil.resolveKey(
                            quanta == 1 ? "matshape.quantum" : "matshape.quanta", quanta));

        return out.toString().trim();
    }

    public static class MaterialStack implements SyncSource {

        public static final Codec<MaterialStack> CODEC =
                RecordCodecBuilder.create(
                        inst ->
                                inst.group(
                                                NTMMaterial.CODEC
                                                        .fieldOf("material")
                                                        .forGetter(
                                                                (MaterialStack stack) ->
                                                                        stack.material),
                                                Codec.INT
                                                        .fieldOf("amount")
                                                        .forGetter(
                                                                (MaterialStack stack) ->
                                                                        stack.amount))
                                        .apply(inst, MaterialStack::new));

        public static final StreamCodec<ByteBuf, MaterialStack> STREAM_CODEC =
                StreamCodec.composite(
                        NTMMaterial.STREAM_CODEC,
                        (MaterialStack stack) -> stack.material,
                        ByteBufCodecs.VAR_INT,
                        (MaterialStack stack) -> stack.amount,
                        MaterialStack::new);

        @SyncField public final NTMMaterial material;
        @SyncField public int amount;

        public MaterialStack(NTMMaterial material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public MaterialStack copy() {
            return new MaterialStack(material, amount);
        }
    }
}
