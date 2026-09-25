// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.config.BalanceConfig;
import com.hbm.hazard.modifier.HazardModifierDecayHeat;
import com.hbm.hazard.modifier.HazardModifierFuelRadiation;
import com.hbm.hazard.modifier.HazardModifierLevel;
import com.hbm.hazard.modifier.HazardModifierRBMKHot;
import com.hbm.hazard.modifier.HazardModifierRBMKRadiation;
import com.hbm.hazard.modifier.HazardModifierRTGRadiation;
import com.hbm.hazard.transformer.*;
import com.hbm.hazard.type.*;
import com.hbm.inventory.OreDictManager;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemZirnoxRodDepleted;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class HazardRegistry {

    public static final IHazardType RADIATION = new HazardTypeRadiation();
    public static final IHazardType CONTAMINATING = new HazardTypeContaminating();
    public static final IHazardType DIGAMMA = new HazardTypeDigamma();
    public static final IHazardType HOT = new HazardTypeHot();
    public static final IHazardType BLINDING = new HazardTypeBlinding();
    public static final IHazardType ASBESTOS = new HazardTypeAsbestos();
    public static final IHazardType COAL = new HazardTypeCoal();
    public static final IHazardType HYDROACTIVE = new HazardTypeHydroactive();
    public static final IHazardType EXPLOSIVE = new HazardTypeExplosive();
    public static final IHazardType TOXIC = new HazardTypeToxic();
    public static final IHazardType COLD = new HazardTypeCold();

    private static final Map<Identifier, IHazardType> TYPES_BY_ID = new LinkedHashMap<>();
    private static final Map<IHazardType, Identifier> IDS_BY_TYPE = new IdentityHashMap<>();

    static {
        nameType("radiation", RADIATION);
        nameType("contaminating", CONTAMINATING);
        nameType("digamma", DIGAMMA);
        nameType("hot", HOT);
        nameType("blinding", BLINDING);
        nameType("asbestos", ASBESTOS);
        nameType("coal", COAL);
        nameType("hydroactive", HYDROACTIVE);
        nameType("explosive", EXPLOSIVE);
        nameType("toxic", TOXIC);
        nameType("cold", COLD);
    }

    public static final Codec<IHazardType> TYPE_CODEC =
            Identifier.CODEC.comapFlatMap(
                    id -> {
                        IHazardType type = TYPES_BY_ID.get(id);
                        return type == null
                                ? DataResult.error(
                                        () ->
                                                "unknown hazard type "
                                                        + id
                                                        + ", known: "
                                                        + TYPES_BY_ID.keySet())
                                : DataResult.success(type);
                    },
                    HazardRegistry::idOf);

    private static void nameType(String name, IHazardType type) {
        Identifier id = Library.id(name);
        TYPES_BY_ID.put(id, type);
        IDS_BY_TYPE.put(type, id);
    }

    public static Identifier idOf(IHazardType type) {
        Identifier id = IDS_BY_TYPE.get(type);
        if (id == null)
            throw new IllegalArgumentException("hazard type carries no datapack name: " + type);
        return id;
    }

    public static final double mult_nugget = 0.1D;
    public static final double mult_ingot = 1.0D;
    public static final double mult_gem = 1.0D;
    public static final double mult_plate = mult_ingot;
    public static final double mult_plateCast = mult_plate * 3D;
    public static final double mult_powder = mult_ingot * 3D;
    public static final double mult_powderTiny = mult_nugget * 3D;
    public static final double mult_ore = mult_ingot;
    public static final double mult_billet = 0.5D;
    public static final double mult_block = 10.0D;

    public static final double mult_rod = 0.5D;
    public static final double mult_rod_rbmk = mult_rod * 8D;
    public static final double mult_rtg = mult_billet * 3D;
    public static final double mult_crystal = mult_gem;

    public static final double co60 = 30.0D;
    public static final double tc99 = 2.75D;
    public static final double au198 = 500.0D;
    public static final double pb209 = 10000.0D;
    public static final double po210 = 75.0D;
    public static final double ra226 = 7.5D;
    public static final double ac227 = 30.0D;
    public static final double th232 = 0.1D;
    public static final double u = 0.35D;
    public static final double u233 = 5.0D;
    public static final double u235 = 1.0D;
    public static final double u238 = 0.25D;
    public static final double np237 = 2.5D;
    public static final double pu = 7.5D;
    public static final double purg = 6.25D;
    public static final double pu238 = 10.0D;
    public static final double pu239 = 5.0D;
    public static final double pu240 = 7.5D;
    public static final double pu241 = 25.0D;
    public static final double am241 = 8.5D;
    public static final double am242 = 9.5D;
    public static final double amrg = 9.0D;

    public static final double uf = 0.5D;
    public static final double saf = 5.85D;

    public static final double thf = 1.75D;
    public static final double uzh = 0.125D;
    public static final double npf = 1.5D;
    public static final double puf = 4.25D;
    public static final double amf = 4.75D;
    public static final double mox = 2.5D;
    public static final double trn = 0.1D;
    public static final double trx = 25.0D;
    public static final double sa326 = 15.0D;
    public static final double sa327 = 17.5D;
    public static final double gh336 = 5.0D;
    public static final double mud = 1.0D;
    public static final double sr = sa326 * 0.1D;
    public static final double sb = sa326 * 0.1D;
    public static final double sas3 = 5.0D;

    public static final double sr90 = 15.0D;
    public static final double i131 = 150.0D;
    public static final double xe135 = 1250.0D;
    public static final double cs137 = 20.0D;
    public static final double at209 = 7500.0D;
    public static final double bf = 300_000.0D;
    public static final double radsource_mult = 3.0D;
    public static final double rabe = ra226 * radsource_mult;
    public static final double pobe = po210 * radsource_mult;
    public static final double pube = pu238 * radsource_mult;

    public static final double wst = 15.0D;
    public static final double wstv = 7.5D;
    public static final double fo = 10.0D;

    private HazardRegistry() {}

    private static final double ZIRNOX_TRITIUM_RAD = 0.001D;

    private static final ToDoubleFunction<ItemStack> ZIRNOX_DEPLETION =
            stack ->
                    stack.getItem() instanceof ItemZirnoxRod rod
                            ? (double) ItemZirnoxRod.getLifeTime(stack) / (double) rod.type.maxLife
                            : 0D;

    public static HazardData makeData(IHazardType type, double level) {
        return new HazardData().addEntry(type, level);
    }

    public static TagKey<Item> hazardTag(String name) {
        return TagKey.create(Registries.ITEM, Library.id("hazard/" + name));
    }

    public static void registerItems() {

        HazardSystem.blacklist(OreDictManager.TH232.ore());
        HazardSystem.blacklist(OreDictManager.U.ore());
        registerZirnox();
        registerBreedingRods();
        registerWatzPellets();
        registerRBMKFuel();
        registerRTGPellets();
        registerPlateFuel();
        registerDepletedFuel();
        registerPWRFuelStages();

        HazardSystem.register(
                ModBlocks.SELLAFIELD.get(),
                new HazardData()
                        .addEntry(
                                new HazardEntry(RADIATION, 0D)
                                        .addMod(
                                                new HazardModifierLevel(
                                                        BlockSellafield.LEVEL,
                                                        0.5D,
                                                        1D,
                                                        2.5D,
                                                        4D,
                                                        5D,
                                                        10D))));
    }

    public static double pwrFuelRad(EnumPWRFuel fuel) {
        return switch (fuel) {
            case MEU -> uf * mult_billet * 2D;
            case HEU233 -> u233 * mult_billet * 2D;
            case HEU235 -> u235 * mult_billet * 2D;
            case MEN -> npf * mult_billet * 2D;
            case HEN237 -> np237 * mult_billet * 2D;
            case MOX -> mox * mult_billet * 2D;
            case MEP -> purg * mult_billet * 2D;
            case HEP239 -> pu239 * mult_billet * 2D;
            case HEP241 -> pu241 * mult_billet * 2D;
            case MEA -> amrg * mult_billet * 2D;
            case HEA242 -> am242 * mult_billet * 2D;
            case HES326 -> sa326 * mult_billet * 2D;
            case HES327 -> sa327 * mult_billet * 2D;
            case BFB_AM_MIX -> amrg * mult_billet;
            case BFB_PU241 -> pu241 * mult_billet;
        };
    }

    private static void registerPWRFuelStages() {
        for (EnumPWRFuel fuel : EnumPWRFuel.VALUES) {
            double spent = pwrFuelRad(fuel) * 10D;
            HazardSystem.register(
                    ModItems.PWR_FUEL_HOT.get(fuel), makeData(RADIATION, spent).addEntry(HOT, 5D));
            HazardSystem.register(ModItems.PWR_FUEL_DEPLETED.get(fuel), makeData(RADIATION, spent));
        }
    }

    private static final ToDoubleFunction<ItemStack> PLATE_DEPLETION =
            stack ->
                    stack.getItem() instanceof ItemPlateFuel plate
                            ? (double) ItemPlateFuel.getLifeTime(stack) / (double) plate.lifeTime
                            : 0D;

    private static void registerPlateFuel() {
        plateFuel(ModItems.PLATE_FUEL_U233, u233 * mult_ingot, wst * mult_ingot * 13D, false);
        plateFuel(ModItems.PLATE_FUEL_U235, u235 * mult_ingot, wst * mult_ingot * 10D, false);
        plateFuel(ModItems.PLATE_FUEL_MOX, mox * mult_ingot, wst * mult_ingot * 16D, false);
        plateFuel(ModItems.PLATE_FUEL_PU239, pu239 * mult_ingot, wst * mult_ingot * 13.5D, false);
        plateFuel(ModItems.PLATE_FUEL_SA326, sa326 * mult_ingot, wst * mult_ingot * 10D, true);
        plateFuel(ModItems.PLATE_FUEL_RA226BE, rabe * mult_billet, pobe * mult_nugget * 3D, false);
        plateFuel(ModItems.PLATE_FUEL_PU238BE, pube * mult_billet, pube * mult_nugget, false);
    }

    private static void plateFuel(
            RegistryHandle<ItemPlateFuel> fuel, double base, double target, boolean blinding) {
        HazardData data =
                new HazardData()
                        .addEntry(
                                new HazardEntry(RADIATION, base)
                                        .addMod(
                                                new HazardModifierFuelRadiation(
                                                        target, PLATE_DEPLETION)));
        if (blinding) data.addEntry(BLINDING, 20D);
        HazardSystem.register(fuel.get(), data);
    }

    private static void registerDepletedFuel() {
        otherWaste(ModItems.WASTE_NATURAL_URANIUM, wst * mult_billet * 11.5D);
        otherWaste(ModItems.WASTE_URANIUM, wst * mult_billet * 10D);
        otherWaste(ModItems.WASTE_THORIUM, wst * mult_billet * 7.5D);
        otherWaste(ModItems.WASTE_MOX, wst * mult_billet * 10D);
        otherWaste(ModItems.WASTE_PLUTONIUM, wst * mult_billet * 12.5D);
        otherWaste(ModItems.WASTE_U233, wst * mult_billet * 10D);
        otherWaste(ModItems.WASTE_U235, wst * mult_billet * 11D);
        otherWaste(ModItems.WASTE_SCHRABIDIUM, wst * mult_billet * 15D);
        otherWaste(ModItems.WASTE_ZFB_MOX, wst * mult_billet * 5D);
        otherWaste(ModItems.WASTE_PLATE_U233, wst * mult_ingot * 13D);
        otherWaste(ModItems.WASTE_PLATE_U235, wst * mult_ingot * 10D);
        otherWaste(ModItems.WASTE_PLATE_MOX, wst * mult_ingot * 16D);
        otherWaste(ModItems.WASTE_PLATE_PU239, wst * mult_ingot * 13.5D);
        otherWaste(ModItems.WASTE_PLATE_SA326, wst * mult_ingot * 10D);
        wasteStages(ModItems.WASTE_PLATE_RA226BE, pobe * mult_nugget * 3D, pobe * mult_nugget * 3D);
        wasteStages(ModItems.WASTE_PLATE_PU238BE, pube * mult_nugget, pube * mult_nugget);
    }

    private static void otherWaste(RegistryHandle<ItemDepletedFuel> waste, double base) {
        wasteStages(waste, base * 0.075D, base);
    }

    private static void wasteStages(
            RegistryHandle<ItemDepletedFuel> waste, double cooled, double hot) {
        HazardSystem.register(
                waste.get(),
                new HazardData()
                        .addEntry(
                                new HazardEntry(RADIATION, 0D)
                                        .addMod(new HazardModifierDecayHeat(cooled, hot)))
                        .addEntry(
                                new HazardEntry(HOT, 0D)
                                        .addMod(new HazardModifierDecayHeat(0D, 5D))));
    }

    private static void registerZirnox() {
        for (RegistryHandle<ItemZirnoxRod> handle : ModItems.FUEL) {
            if (handle == null) continue;
            ItemZirnoxRod rod = handle.get();
            HazardSystem.register(
                    rod,
                    new HazardData()
                            .addEntry(
                                    new HazardEntry(RADIATION, rod.type.freshRad)
                                            .addMod(
                                                    new HazardModifierFuelRadiation(
                                                            zirnoxWasteRad(rod),
                                                            ZIRNOX_DEPLETION))));
        }
        for (RegistryHandle<ItemZirnoxRodDepleted> handle : ModItems.DEPLETED) {

            if (handle == null) continue;
            ItemZirnoxRodDepleted dep = handle.get();
            HazardData data = new HazardData();
            if (dep.wasteRad > 0F) data.addEntry(RADIATION, dep.wasteRad);
            if (dep.blinding > 0F) data.addEntry(BLINDING, dep.blinding);
            HazardSystem.register(dep, data);
        }
        HazardSystem.register(
                ModItems.ROD_ZIRNOX_TRITIUM.get(), makeData(RADIATION, ZIRNOX_TRITIUM_RAD));

        HazardSystem.register(ModItems.INSERT_POLONIUM.get(), makeData(RADIATION, 100D));

        HazardSystem.register(
                ModItems.POWDER_CAESIUM.get(),
                new HazardData().addEntry(HYDROACTIVE, 1D).addEntry(HOT, 3D));

        registerEach(ModItems.NUCLEAR_WASTE_LONG, () -> makeData(RADIATION, 5D));
        registerEach(ModItems.NUCLEAR_WASTE_LONG_TINY, () -> makeData(RADIATION, 0.5D));
        registerEach(
                ModItems.NUCLEAR_WASTE_SHORT,
                () -> new HazardData().addEntry(RADIATION, 30D).addEntry(HOT, 5D));
        registerEach(
                ModItems.NUCLEAR_WASTE_SHORT_TINY,
                () -> new HazardData().addEntry(RADIATION, 3D).addEntry(HOT, 5D));
        registerEach(ModItems.NUCLEAR_WASTE_LONG_DEPLETED, () -> makeData(RADIATION, 0.5D));
        registerEach(ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY, () -> makeData(RADIATION, 0.05D));
        registerEach(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED, () -> makeData(RADIATION, 3D));
        registerEach(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY, () -> makeData(RADIATION, 0.3D));

        HazardSystem.register(ModItems.BOY_PROPELLANT.get(), makeData(EXPLOSIVE, 2D));
        HazardSystem.register(
                ModItems.GADGET_CORE.get(), makeData(RADIATION, pu239 * mult_nugget * 10D));
        HazardSystem.register(
                ModItems.BOY_TARGET.get(), makeData(RADIATION, u235 * mult_ingot * 2D));
        HazardSystem.register(ModItems.BOY_BULLET.get(), makeData(RADIATION, u235 * mult_ingot));
        HazardSystem.register(
                ModItems.MAN_CORE.get(), makeData(RADIATION, pu239 * mult_nugget * 10D));
        HazardSystem.register(
                ModItems.MIKE_CORE.get(), makeData(RADIATION, u238 * mult_nugget * 10D));
        HazardSystem.register(
                ModItems.TSAR_CORE.get(), makeData(RADIATION, pu239 * mult_nugget * 15D));
        HazardSystem.register(
                ModItems.FLEIJA_PROPELLANT.get(),
                new HazardData()
                        .addEntry(RADIATION, 15D)
                        .addEntry(EXPLOSIVE, 8D)
                        .addEntry(BLINDING, 50D));
        HazardSystem.register(ModItems.FLEIJA_CORE.get(), makeData(RADIATION, 10D));
        HazardSystem.register(ModItems.SOLINIUM_PROPELLANT.get(), makeData(EXPLOSIVE, 10D));
        HazardSystem.register(
                ModItems.SOLINIUM_CORE.get(),
                new HazardData()
                        .addEntry(RADIATION, sa327 * mult_nugget * 8D)
                        .addEntry(BLINDING, 45D));
    }

    private static double zirnoxWasteRad(ItemZirnoxRod rod) {
        ItemLike spent = ModItems.spentZirnoxFuel(rod);
        if (spent == null) return 0D;
        Item item = spent.asItem();
        if (item instanceof ItemZirnoxRodDepleted dep) return dep.wasteRad;
        if (item instanceof ItemZirnoxRod bred) return bred.type.freshRad;
        return ZIRNOX_TRITIUM_RAD;
    }

    private static void registerEach(ItemFamily<?, ?> family, Supplier<HazardData> data) {
        for (var member : family) HazardSystem.register(member.get(), data.get());
    }

    private static void registerBreedingRods() {
        for (var family : List.of(ModItems.ROD, ModItems.ROD_DUAL, ModItems.ROD_QUAD)) {
            for (var member : family) {
                ItemBreedingRod rod = member.get();

                if (rod.type.radiation == 0D) continue;
                HazardSystem.register(
                        rod,
                        new HazardData()
                                .addEntry(
                                        new HazardEntry(
                                                RADIATION,
                                                rod.type.radiation
                                                        * rod.family.radiationMultiplier)));
            }
        }
    }

    private static void registerWatzPellets() {
        for (var member : ModItems.WATZ_PELLET) {
            double rad =
                    switch (member.get().type) {
                        case SCHRABIDIUM -> sa326 * mult_ingot * 4D;
                        case HES, MES, LES -> saf * mult_ingot * 4D;
                        case HEN -> np237 * mult_ingot * 4D;
                        case MEU -> uf * mult_ingot * 4D;
                        case MEP -> purg * mult_ingot * 4D;
                        case DU -> u238 * mult_ingot * 4D;
                        case NQD -> u235 * mult_ingot * 4D;
                        case NQR -> pu239 * mult_ingot * 4D;
                        case LEAD, BORON -> 0D;
                    };
            if (rad > 0D) HazardSystem.register(member.get(), makeData(RADIATION, rad));
        }
    }

    private static void registerRBMKFuel() {
        Map<String, RegistryHandle<ItemRBMKPellet>> pellets = new HashMap<>();
        for (RegistryHandle<ItemRBMKPellet> handle : ModItems.PELLETS)
            pellets.put(handle.id().getPath(), handle);
        RBMKBuilder b = new RBMKBuilder(pellets);

        b.rod(ModItems.RBMK_FUEL_UEU, u * mult_rod_rbmk, wst * mult_rod_rbmk * 20D);
        b.rod(ModItems.RBMK_FUEL_MEU, uf * mult_rod_rbmk, wst * mult_rod_rbmk * 21.5D);
        b.rod(ModItems.RBMK_FUEL_HEU233, u233 * mult_rod_rbmk, wst * mult_rod_rbmk * 31D);
        b.rod(ModItems.RBMK_FUEL_HEU235, u235 * mult_rod_rbmk, wst * mult_rod_rbmk * 30D);
        b.rod(ModItems.RBMK_FUEL_UZH, uzh * mult_rod_rbmk, wst * mult_rod_rbmk * 20D);
        b.rod(ModItems.RBMK_FUEL_THMEU, thf * mult_rod_rbmk, wst * mult_rod_rbmk * 17.5D);
        b.rod(ModItems.RBMK_FUEL_LEP, puf * mult_rod_rbmk, wst * mult_rod_rbmk * 25D);
        b.rod(ModItems.RBMK_FUEL_MEP, purg * mult_rod_rbmk, wst * mult_rod_rbmk * 30D);
        b.rod(ModItems.RBMK_FUEL_HEP, pu239 * mult_rod_rbmk, wst * mult_rod_rbmk * 32.5D);
        b.rod(ModItems.RBMK_FUEL_HEP241, pu241 * mult_rod_rbmk, wst * mult_rod_rbmk * 35D);
        b.rod(ModItems.RBMK_FUEL_LEA, amf * mult_rod_rbmk, wst * mult_rod_rbmk * 26D);
        b.rod(ModItems.RBMK_FUEL_MEA, amrg * mult_rod_rbmk, wst * mult_rod_rbmk * 30.5D);
        b.rod(ModItems.RBMK_FUEL_HEA241, am241 * mult_rod_rbmk, wst * mult_rod_rbmk * 33.5D);
        b.rod(ModItems.RBMK_FUEL_HEA242, am242 * mult_rod_rbmk, wst * mult_rod_rbmk * 34D);
        b.rod(ModItems.RBMK_FUEL_MEN, npf * mult_rod_rbmk, wst * mult_rod_rbmk * 22.5D);
        b.rod(ModItems.RBMK_FUEL_HEN, np237 * mult_rod_rbmk, wst * mult_rod_rbmk * 30D);
        b.rod(ModItems.RBMK_FUEL_MOX, mox * mult_rod_rbmk, wst * mult_rod_rbmk * 25.5D);
        b.rod(ModItems.RBMK_FUEL_LES, saf * mult_rod_rbmk, wst * mult_rod_rbmk * 24.5D);
        b.rod(ModItems.RBMK_FUEL_MES, saf * mult_rod_rbmk, wst * mult_rod_rbmk * 30D);
        b.rod(ModItems.RBMK_FUEL_HES, saf * mult_rod_rbmk, wst * mult_rod_rbmk * 50D);
        b.rod(ModItems.RBMK_FUEL_LEAUS, 0D, wst * mult_rod_rbmk * 37.5D);
        b.rod(ModItems.RBMK_FUEL_HEAUS, 0D, wst * mult_rod_rbmk * 32.5D);
        b.rod(ModItems.RBMK_FUEL_PO210BE, pobe * mult_rod_rbmk, pobe * mult_rod_rbmk * 0.1D, true);
        b.rod(ModItems.RBMK_FUEL_RA226BE, rabe * mult_rod_rbmk, rabe * mult_rod_rbmk * 0.4D, true);
        b.rod(ModItems.RBMK_FUEL_PU238BE, pube * mult_rod_rbmk, wst * mult_rod_rbmk * 2.5D);
        b.rod(
                ModItems.RBMK_FUEL_BALEFIRE_GOLD,
                au198 * mult_rod_rbmk,
                bf * mult_rod_rbmk * 0.5D,
                true);
        b.rod(
                ModItems.RBMK_FUEL_FLASHLEAD,
                pb209 * 1.25D * mult_rod_rbmk,
                pb209 * mult_nugget * 0.05D * mult_rod_rbmk,
                true);
        b.rod(ModItems.RBMK_FUEL_BALEFIRE, bf * mult_rod_rbmk, bf * mult_rod_rbmk * 100D, true);
        b.rod(
                ModItems.RBMK_FUEL_ZFB_BISMUTH,
                pu241 * mult_rod_rbmk * 0.1D,
                wst * mult_rod_rbmk * 5D);
        b.rod(
                ModItems.RBMK_FUEL_ZFB_PU241,
                pu239 * mult_rod_rbmk * 0.1D,
                wst * mult_rod_rbmk * 7.5D);
        b.rod(
                ModItems.RBMK_FUEL_ZFB_AM_MIX,
                pu241 * mult_rod_rbmk * 0.1D,
                wst * mult_rod_rbmk * 10D);
        b.rbmk(
                ModItems.RBMK_FUEL_DRX,
                bf * mult_rod_rbmk,
                bf * mult_rod_rbmk * 100D,
                true,
                true,
                0D,
                1D / 3D);

        b.pellet("rbmk_pellet_ueu", u * mult_billet, wst * mult_billet * 20D);
        b.pellet("rbmk_pellet_meu", uf * mult_billet, wst * mult_billet * 21.5D);
        b.pellet("rbmk_pellet_heu233", u233 * mult_billet, wst * mult_billet * 31D);
        b.pellet("rbmk_pellet_heu235", u235 * mult_billet, wst * mult_billet * 30D);
        b.pellet("rbmk_pellet_uzh", uzh * mult_billet, wst * mult_billet * 20D);
        b.pellet("rbmk_pellet_thmeu", thf * mult_billet, wst * mult_billet * 17.5D);
        b.pellet("rbmk_pellet_lep", puf * mult_billet, wst * mult_billet * 25D);
        b.pellet("rbmk_pellet_mep", purg * mult_billet, wst * mult_billet * 30D);
        b.pellet("rbmk_pellet_hep239", pu239 * mult_billet, wst * mult_billet * 32.5D);
        b.pellet("rbmk_pellet_hep241", pu241 * mult_billet, wst * mult_billet * 35D);
        b.pellet("rbmk_pellet_lea", amf * mult_billet, wst * mult_billet * 26D);
        b.pellet("rbmk_pellet_mea", amrg * mult_billet, wst * mult_billet * 30.5D);
        b.pellet("rbmk_pellet_hea241", am241 * mult_billet, wst * mult_billet * 33.5D);
        b.pellet("rbmk_pellet_hea242", am242 * mult_billet, wst * mult_billet * 34D);
        b.pellet("rbmk_pellet_men", npf * mult_billet, wst * mult_billet * 22.5D);
        b.pellet("rbmk_pellet_hen", np237 * mult_billet, wst * mult_billet * 30D);
        b.pellet("rbmk_pellet_mox", mox * mult_billet, wst * mult_billet * 25.5D);
        b.pellet("rbmk_pellet_les", saf * mult_billet, wst * mult_billet * 24.5D);
        b.pellet("rbmk_pellet_mes", saf * mult_billet, wst * mult_billet * 30D);
        b.pellet("rbmk_pellet_hes", saf * mult_billet, wst * mult_billet * 50D);
        b.pellet("rbmk_pellet_leaus", 0D, wst * mult_billet * 37.5D);
        b.pellet("rbmk_pellet_heaus", 0D, wst * mult_billet * 32.5D);
        b.pellet("rbmk_pellet_po210be", pobe * mult_billet, pobe * mult_billet * 0.1D, true);
        b.pellet("rbmk_pellet_ra226be", rabe * mult_billet, rabe * mult_billet * 0.4D, true);

        b.pellet("rbmk_pellet_pu238be", pube * mult_billet, wst * 1.5D);
        b.pellet("rbmk_pellet_balefire_gold", au198 * mult_billet, bf * mult_billet * 0.5D, true);
        b.pellet(
                "rbmk_pellet_flashlead",
                pb209 * 1.25D * mult_billet,
                pb209 * mult_nugget * 0.05D,
                true);
        b.pellet("rbmk_pellet_balefire", bf * mult_billet, bf * mult_billet * 100D, true);
        b.pellet("rbmk_pellet_zfb_bismuth", pu241 * mult_billet * 0.1D, wst * mult_billet * 5D);
        b.pellet("rbmk_pellet_zfb_pu241", pu239 * mult_billet * 0.1D, wst * mult_billet * 7.5D);
        b.pellet("rbmk_pellet_zfb_am_mix", pu241 * mult_billet * 0.1D, wst * mult_billet * 10D);
        b.pellet("rbmk_pellet_drx", bf * mult_billet, bf * mult_billet * 100D, true, 0D, 1D / 24D);
    }

    private static void registerRTGPellets() {
        put(ModItems.PELLET_RTG, pu238 * mult_rtg, 0D, 3D, 0D);
        put(ModItems.PELLET_RTG_RADIUM, ra226 * mult_rtg, 0D, 0D, 0D);

        put(ModItems.PELLET_RTG_WEAK, (pu238 + (u238 * 2D)) * mult_billet, 0D, 0D, 0D);
        put(ModItems.PELLET_RTG_STRONTIUM, sr90 * mult_rtg, 0D, 0D, 0D);
        put(ModItems.PELLET_RTG_COBALT, co60 * mult_rtg, 0D, 0D, 0D);
        put(ModItems.PELLET_RTG_ACTINIUM, ac227 * mult_rtg, 0D, 0D, 0D);
        put(ModItems.PELLET_RTG_POLONIUM, po210 * mult_rtg, 0D, 3D, 0D);
        put(ModItems.PELLET_RTG_LEAD, pb209 * mult_rtg, 0D, 7D, 50D);
        put(ModItems.PELLET_RTG_GOLD, au198 * mult_rtg, 0D, 5D, 0D);
        put(ModItems.PELLET_RTG_AMERICIUM, am241 * mult_rtg, 0D, 0D, 0D);
    }

    private static void put(
            RegistryHandle<ItemRTGPellet> pellet,
            double base,
            double target,
            double hot,
            double blinding) {
        HazardData data = new HazardData();
        data.addEntry(
                new HazardEntry(RADIATION, base).addMod(new HazardModifierRTGRadiation(target)));
        if (hot > 0D) data.addEntry(HOT, hot);
        if (blinding > 0D) data.addEntry(BLINDING, blinding);
        HazardSystem.register(pellet.get(), data);
    }

    private static final class RBMKBuilder {

        private final Map<String, RegistryHandle<ItemRBMKPellet>> pellets;

        RBMKBuilder(Map<String, RegistryHandle<ItemRBMKPellet>> pellets) {
            this.pellets = pellets;
        }

        void rod(RegistryHandle<ItemRBMKRod> rod, double base, double dep) {
            rbmk(rod, base, dep, true, false, 0D, 0D);
        }

        void rod(RegistryHandle<ItemRBMKRod> rod, double base, double dep, boolean linear) {
            rbmk(rod, base, dep, true, linear, 0D, 0D);
        }

        void rbmk(
                RegistryHandle<ItemRBMKRod> rod,
                double base,
                double dep,
                boolean hot,
                boolean linear,
                double blinding,
                double digamma) {
            HazardSystem.register(rod.get(), data(base, dep, hot, linear, blinding, digamma));
        }

        void pellet(String path, double base, double dep) {
            pellet(path, base, dep, false, 0D, 0D);
        }

        void pellet(String path, double base, double dep, boolean linear) {
            pellet(path, base, dep, linear, 0D, 0D);
        }

        void pellet(
                String path,
                double base,
                double dep,
                boolean linear,
                double blinding,
                double digamma) {
            RegistryHandle<ItemRBMKPellet> handle = pellets.get(path);
            if (handle == null)
                throw new IllegalStateException("no RBMK pellet registered as " + path);
            HazardSystem.register(handle.get(), data(base, dep, false, linear, blinding, digamma));
        }

        private HazardData data(
                double base,
                double dep,
                boolean hot,
                boolean linear,
                double blinding,
                double digamma) {
            HazardData data = new HazardData();
            data.addEntry(
                    new HazardEntry(RADIATION, base)
                            .addMod(new HazardModifierRBMKRadiation(dep, linear)));
            if (hot) data.addEntry(new HazardEntry(HOT, 0D).addMod(new HazardModifierRBMKHot()));
            if (blinding > 0D) data.addEntry(BLINDING, blinding);
            if (digamma > 0D) data.addEntry(DIGAMMA, digamma);
            return data;
        }
    }

    public static void registerTrafos() {
        for (IHazardTransformer transformer : transformers()) {
            HazardSystem.addTransformer(transformer);
        }
    }

    public static List<IHazardTransformer> transformers() {
        List<IHazardTransformer> out = new ArrayList<>();
        out.add(new HazardTransformerRadiationNBT());
        if (!(BalanceConfig.enableLBSM && BalanceConfig.enableLBSMSafeCrates))
            out.add(new HazardTransformerRadiationContainer());

        out.add(new HazardTransformerPostCustom());
        return out;
    }
}
