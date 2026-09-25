// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.lib.Library;
import mov.movblock.tenon.strip.api.DropSafe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class OreDictManager {

    public static final TagKey<Item> KEY_ANYGLASS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "glass_blocks"));

    public static final TagKey<Item> KEY_CLEARGLASS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath("c", "glass_blocks/colorless"));

    public static final TagKey<Item> KEY_ANYPANE =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "glass_panes"));

    public static final TagKey<Item> KEY_CONCRETE =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "concretes"));

    public static final TagKey<Item> KEY_SAND = ItemTags.SAND;

    public static final TagKey<Item> KEY_COBBLESTONE = common("cobblestones");

    public static final TagKey<Item> KEY_DYE = common("dyes");

    public static final TagKey<Item> KEY_STICK = common("rods/wooden");
    public static final TagKey<Item> KEY_TOOL_SCREWDRIVER =
            TagKey.create(Registries.ITEM, Library.id("tools/screwdriver"));
    public static final TagKey<Item> KEY_TOOL_HANDDRILL =
            TagKey.create(Registries.ITEM, Library.id("tools/hand_drill"));
    public static final TagKey<Item> KEY_TOOL_CHEMISTRYSET =
            TagKey.create(Registries.ITEM, Library.id("tools/chemistry_set"));
    public static final TagKey<Item> KEY_TOOL_TORCH =
            TagKey.create(Registries.ITEM, Library.id("tools/torch"));
    public static final DictFrame WOOD = new DictFrame("wood");
    public static final DictFrame BONE = new DictFrame("bone");
    public static final DictFrame COAL = new DictFrame("coal");
    public static final DictFrame IRON = new DictFrame("iron");
    public static final DictFrame GOLD = new DictFrame("gold");
    public static final DictFrame LAPIS = new DictFrame("lapis");
    public static final DictFrame REDSTONE = new DictFrame("redstone");
    public static final DictFrame NETHERQUARTZ = new DictFrame("nether_quartz");
    public static final DictFrame QUARTZ = new DictFrame("quartz");
    public static final DictFrame DIAMOND = new DictFrame("diamond");
    public static final DictFrame EMERALD = new DictFrame("emerald");
    public static final DictFrame U = new DictFrame("uranium");
    public static final DictFrame U233 = new DictFrame("uranium_233");
    public static final DictFrame U235 = new DictFrame("uranium_235");
    public static final DictFrame U238 = new DictFrame("uranium_238");
    public static final DictFrame TH232 = new DictFrame("thorium");
    public static final DictFrame PU = new DictFrame("plutonium");
    public static final DictFrame PURG = new DictFrame("plutonium_rg");
    public static final DictFrame PU238 = new DictFrame("plutonium_238");
    public static final DictFrame PU239 = new DictFrame("plutonium_239");
    public static final DictFrame PU240 = new DictFrame("plutonium_240");
    public static final DictFrame PU241 = new DictFrame("plutonium_241");
    public static final DictFrame AM241 = new DictFrame("americium_241");
    public static final DictFrame AM242 = new DictFrame("americium_242");
    public static final DictFrame AMRG = new DictFrame("americium_rg");
    public static final DictFrame NP237 = new DictFrame("neptunium_237", "neptunium");
    public static final DictFrame PO210 = new DictFrame("polonium_210", "polonium");
    public static final DictFrame TC99 = new DictFrame("technetium_99");
    public static final DictFrame RA226 = new DictFrame("radium_226");
    public static final DictFrame AC227 = new DictFrame("actinium_227");
    public static final DictFrame CO60 = new DictFrame("cobalt_60");
    public static final DictFrame AU198 = new DictFrame("gold_198");
    public static final DictFrame PB209 = new DictFrame("lead_209");
    public static final DictFrame SA326 = new DictFrame("schrabidium");
    public static final DictFrame SA327 = new DictFrame("solinium");
    public static final DictFrame SBD = new DictFrame("schrabidate");
    public static final DictFrame SRN = new DictFrame("schraranium");
    public static final DictFrame GH336 = new DictFrame("ghiorsium_336");
    public static final DictFrame MUD = new DictFrame("watz_mud");
    public static final DictFrame TI = new DictFrame("titanium");
    public static final DictFrame CU = new DictFrame("copper");
    public static final DictFrame MINGRADE = new DictFrame("mingrade");
    public static final DictFrame W = new DictFrame("tungsten");
    public static final DictFrame WC = new DictFrame("tungsten_carbide");
    public static final DictFrame AL = new DictFrame("aluminum");
    public static final DictFrame STEEL = new DictFrame("steel");
    public static final DictFrame TCALLOY = new DictFrame("tc_alloy");
    public static final DictFrame CDALLOY = new DictFrame("cd_alloy");
    public static final DictFrame BBRONZE = new DictFrame("bismuth_bronze");
    public static final DictFrame ABRONZE = new DictFrame("arsenic_bronze");
    public static final DictFrame BSCCO = new DictFrame("bscco");
    public static final DictFrame PB = new DictFrame("lead");
    public static final DictFrame BI = new DictFrame("bismuth");
    public static final DictFrame AS = new DictFrame("arsenic");
    public static final DictFrame CA = new DictFrame("calcium");
    public static final DictFrame CD = new DictFrame("cadmium");
    public static final DictFrame TA = new DictFrame("tantalum");
    public static final DictFrame COLTAN = new DictFrame("coltan");
    public static final DictFrame NB = new DictFrame("niobium");
    public static final DictFrame BE = new DictFrame("beryllium");
    public static final DictFrame CO = new DictFrame("cobalt");
    public static final DictFrame B = new DictFrame("boron");
    public static final DictFrame SI = new DictFrame("silicon");
    public static final DictFrame GRAPHITE = new DictFrame("graphite");
    public static final DictFrame CARBON = new DictFrame("carbon");
    public static final DictFrame DURA = new DictFrame("dura_steel");
    public static final DictFrame POLYMER = new DictFrame("polymer");
    public static final DictFrame BAKELITE = new DictFrame("bakelite");
    public static final DictFrame PET = new DictFrame("pet");
    public static final DictFrame PC = new DictFrame("polycarbonate");
    public static final DictFrame PVC = new DictFrame("pvc");
    public static final DictFrame LATEX = new DictFrame("latex");
    public static final DictFrame RUBBER = new DictFrame("rubber");
    public static final DictFrame MAGTUNG = new DictFrame("magnetized_tungsten");
    public static final DictFrame CMB = new DictFrame("cmbsteel");
    public static final DictFrame DESH = new DictFrame("workers_alloy");
    public static final DictFrame STAR = new DictFrame("starmetal");
    public static final DictFrame GUNMETAL = new DictFrame("gun_metal");
    public static final DictFrame WEAPONSTEEL = new DictFrame("weapon_steel");
    public static final DictFrame BIGMT = new DictFrame("saturnite");
    public static final DictFrame FERRO = new DictFrame("ferrouranium");
    public static final DictFrame EUPH = new DictFrame("euphemium");
    public static final DictFrame DNT = new DictFrame("dineutronium");
    public static final DictFrame FIBER = new DictFrame("fiberglass");
    public static final DictFrame ASBESTOS = new DictFrame("asbestos");
    public static final DictFrame OSMIRIDIUM = new DictFrame("osmiridium");
    public static final DictFrame S = new DictFrame("sulfur");
    public static final DictFrame KNO = new DictFrame("saltpeter");
    public static final DictFrame F = new DictFrame("fluorite");
    public static final DictFrame LIGNITE = new DictFrame("lignite");
    public static final DictFrame COALCOKE = new DictFrame("coke");
    public static final DictFrame PETCOKE = new DictFrame("pet_coke");
    public static final DictFrame LIGCOKE = new DictFrame("lignite_coke");
    public static final DictFrame CINNABAR = new DictFrame("cinnabar");
    public static final DictFrame BORAX = new DictFrame("borax");
    public static final DictFrame CHLOROCALCITE = new DictFrame("chlorocalcite");
    public static final DictFrame MOLYSITE = new DictFrame("molysite");
    public static final DictFrame SODALITE = new DictFrame("sodalite");
    public static final DictFrame VOLCANIC = new DictFrame("volcanic");
    public static final DictFrame HEMATITE = new DictFrame("hematite");
    public static final DictFrame MALACHITE = new DictFrame("malachite");
    public static final DictFrame LIMESTONE = new DictFrame("limestone");
    public static final DictFrame SLAG = new DictFrame("slag");
    public static final DictFrame BAUXITE = new DictFrame("bauxite");
    public static final DictFrame CRYOLITE = new DictFrame("cryolite");
    public static final DictFrame LI = new DictFrame("lithium");
    public static final DictFrame NA = new DictFrame("sodium");
    public static final DictFrame P_WHITE = new DictFrame("white_phosphorus");
    public static final DictFrame P_RED = new DictFrame("red_phosphorus");
    public static final DictFrame AUSTRALIUM = new DictFrame("australium");
    public static final DictFrame RAREEARTH = new DictFrame("rare_earth");
    public static final DictFrame LA = new DictFrame("lanthanum");
    public static final DictFrame ZR = new DictFrame("zirconium");
    public static final DictFrame ND = new DictFrame("neodymium");
    public static final DictFrame CE = new DictFrame("cerium");
    public static final DictFrame I = new DictFrame("iodine");
    public static final DictFrame AT = new DictFrame("astatine");
    public static final DictFrame CS = new DictFrame("caesium");
    public static final DictFrame ST = new DictFrame("strontium");
    public static final DictFrame BR = new DictFrame("bromine");
    public static final DictFrame TS = new DictFrame("tennessine");
    public static final DictFrame SR = new DictFrame("strontium");
    public static final DictFrame SR90 = new DictFrame("strontium_90");
    public static final DictFrame I131 = new DictFrame("iodine_131");
    public static final DictFrame XE135 = new DictFrame("xenon_135");
    public static final DictFrame CS137 = new DictFrame("caesium_137");
    public static final DictFrame AT209 = new DictFrame("astatine_209");
    public static final DictFrame ANY_GUNPOWDER = DictFrame.privateFrame("any_propellant");
    public static final DictFrame ANY_SMOKELESS = DictFrame.privateFrame("any_smokeless");
    public static final DictFrame ANY_PLASTICEXPLOSIVE =
            DictFrame.privateFrame("any_plasticexplosive");
    public static final DictFrame ANY_HIGHEXPLOSIVE = DictFrame.privateFrame("any_highexplosive");
    public static final DictFrame ANY_COKE = DictFrame.privateFrame("any_coke");
    public static final DictFrame ANY_ASH = DictFrame.privateFrame("ash");
    public static final DictGroup ANY_RUBBER = new DictGroup("any_rubber", LATEX, RUBBER);
    public static final DictGroup ANY_PLASTIC = new DictGroup("any_plastic", POLYMER, BAKELITE);
    public static final DictGroup ANY_HARDPLASTIC = new DictGroup("any_hard_plastic", PC, PVC);
    public static final DictGroup ANY_RESISTANTALLOY =
            new DictGroup("any_resistant_alloy", TCALLOY, CDALLOY);
    public static final DictGroup ANY_BISMOIDBRONZE =
            new DictGroup("any_bismoid_bronze", BBRONZE, ABRONZE);
    public static final DictGroup ANY_BISMOID = new DictGroup("any_bismoid", BI, AS);

    public static final TagKey<Item> KEY_ANY_TAR =
            TagKey.create(Registries.ITEM, Library.id("any_tar"));

    private OreDictManager() {}

    @DropSafe
    public static TagKey<Item> dye(DyeColor color) {
        return common("dyes/" + color.getSerializedName());
    }

    @DropSafe
    public static TagKey<Item> dyed(DyeColor color) {
        return common("dyed/" + color.getSerializedName());
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    public record DictFrame(String mat, @Nullable String alias, boolean privateKey) {

        public DictFrame(String mat) {
            this(mat, null, false);
        }

        public DictFrame(String mat, String alias) {
            this(mat, alias, false);
        }

        public static DictFrame privateFrame(String mat) {
            return new DictFrame(mat, null, true);
        }

        private TagKey<Item> key(MaterialShapes shape) {
            return privateKey ? shape.tagForPrivate(mat) : shape.tagFor(mat);
        }

        @DropSafe
        public TagKey<Item> any() {
            return TagKey.create(Registries.ITEM, Library.id("any_" + mat));
        }

        @DropSafe
        public TagKey<Item> nugget() {
            return key(MaterialShapes.NUGGET);
        }

        @DropSafe
        public TagKey<Item> dustTiny() {
            return key(MaterialShapes.DUSTTINY);
        }

        public TagKey<Item> fragment() {
            return key(MaterialShapes.FRAGMENT);
        }

        public TagKey<Item> wireFine() {
            return key(MaterialShapes.WIRE);
        }

        public TagKey<Item> bolt() {
            return key(MaterialShapes.BOLT);
        }

        @DropSafe
        public TagKey<Item> billet() {
            return key(MaterialShapes.BILLET);
        }

        @DropSafe
        public TagKey<Item> ingot() {
            return key(MaterialShapes.INGOT);
        }

        @DropSafe
        public TagKey<Item> gem() {
            return key(MaterialShapes.GEM);
        }

        @DropSafe
        public TagKey<Item> crystal() {
            return key(MaterialShapes.CRYSTAL);
        }

        @DropSafe
        public TagKey<Item> dust() {
            return key(MaterialShapes.DUST);
        }

        public TagKey<Item> wireDense() {
            return key(MaterialShapes.DENSEWIRE);
        }

        public TagKey<Item> plate() {
            return key(MaterialShapes.PLATE);
        }

        public TagKey<Item> plateCast() {
            return key(MaterialShapes.CASTPLATE);
        }

        public TagKey<Item> plateWelded() {
            return key(MaterialShapes.WELDEDPLATE);
        }

        public TagKey<Item> shell() {
            return key(MaterialShapes.SHELL);
        }

        public TagKey<Item> pipe() {
            return key(MaterialShapes.PIPE);
        }

        @DropSafe
        public TagKey<Item> block() {
            return key(MaterialShapes.BLOCK);
        }

        @DropSafe
        public TagKey<Item> ore() {
            return key(MaterialShapes.ORE);
        }

        public TagKey<Item> barrelLight() {
            return key(MaterialShapes.LIGHTBARREL);
        }

        public TagKey<Item> barrelHeavy() {
            return key(MaterialShapes.HEAVYBARREL);
        }

        public TagKey<Item> receiverLight() {
            return key(MaterialShapes.LIGHTRECEIVER);
        }

        public TagKey<Item> receiverHeavy() {
            return key(MaterialShapes.HEAVYRECEIVER);
        }

        public TagKey<Item> mechanism() {
            return key(MaterialShapes.MECHANISM);
        }

        public TagKey<Item> stock() {
            return key(MaterialShapes.STOCK);
        }

        public TagKey<Item> grip() {
            return key(MaterialShapes.GRIP);
        }

        public TagKey<Item> tag(MaterialShapes shape) {
            return key(shape);
        }
    }

    public record DictGroup(String groupName, DictFrame... members) {

        @DropSafe
        public TagKey<Item> nugget() {
            return MaterialShapes.NUGGET.tagForPrivate(groupName);
        }

        @DropSafe
        public TagKey<Item> ingot() {
            return MaterialShapes.INGOT.tagForPrivate(groupName);
        }

        @DropSafe
        public TagKey<Item> billet() {
            return MaterialShapes.BILLET.tagForPrivate(groupName);
        }

        @DropSafe
        public TagKey<Item> dust() {
            return MaterialShapes.DUST.tagForPrivate(groupName);
        }

        public TagKey<Item> plate() {
            return MaterialShapes.PLATE.tagForPrivate(groupName);
        }

        public TagKey<Item> plateCast() {
            return MaterialShapes.CASTPLATE.tagForPrivate(groupName);
        }

        public TagKey<Item> plateWelded() {
            return MaterialShapes.WELDEDPLATE.tagForPrivate(groupName);
        }

        public TagKey<Item> barrelLight() {
            return MaterialShapes.LIGHTBARREL.tagForPrivate(groupName);
        }

        public TagKey<Item> barrelHeavy() {
            return MaterialShapes.HEAVYBARREL.tagForPrivate(groupName);
        }

        public TagKey<Item> receiverLight() {
            return MaterialShapes.LIGHTRECEIVER.tagForPrivate(groupName);
        }

        public TagKey<Item> receiverHeavy() {
            return MaterialShapes.HEAVYRECEIVER.tagForPrivate(groupName);
        }

        public TagKey<Item> mechanism() {
            return MaterialShapes.MECHANISM.tagForPrivate(groupName);
        }

        public TagKey<Item> stock() {
            return MaterialShapes.STOCK.tagForPrivate(groupName);
        }

        public TagKey<Item> grip() {
            return MaterialShapes.GRIP.tagForPrivate(groupName);
        }

        @DropSafe
        public TagKey<Item> block() {
            return MaterialShapes.BLOCK.tagForPrivate(groupName);
        }

        @DropSafe
        public TagKey<Item> ore() {
            return MaterialShapes.ORE.tagForPrivate(groupName);
        }

        public TagKey<Item> tag(MaterialShapes shape) {
            return shape.tagForPrivate(groupName);
        }
    }
}
