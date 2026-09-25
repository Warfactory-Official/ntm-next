// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;
import com.hbm.items.food.ItemConserve.EnumFoodType;
import com.hbm.items.machine.ItemBlueprintFolder;
import com.hbm.items.special.ItemKitCustom;
import com.hbm.items.special.ItemKitNBT;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.tileentity.BlockEntityPlushie;
import com.hbm.tileentity.BlockEntitySnowglobe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import static com.hbm.handler.BobmazonRequirement.ASSEMBLY;
import static com.hbm.handler.BobmazonRequirement.CHEMICS;
import static com.hbm.handler.BobmazonRequirement.HIDDEN;
import static com.hbm.handler.BobmazonRequirement.NUCLEAR;
import static com.hbm.handler.BobmazonRequirement.OIL;
import static com.hbm.handler.BobmazonRequirement.STEEL;

public final class BobmazonOffers {

    private static @Nullable List<BobmazonOffer> standard;
    private static @Nullable List<BobmazonOffer> special;

    private BobmazonOffers() {}

    public static synchronized List<BobmazonOffer> standard() {
        if (standard == null) standard = buildStandard();
        return standard;
    }

    public static synchronized List<BobmazonOffer> special() {
        if (special == null) special = buildSpecial();
        return special;
    }

    public static synchronized void invalidate() {
        standard = null;
        special = null;
    }

    public static @Nullable List<BobmazonOffer> forStack(ItemStack stack) {
        if (stack.is(ModItems.BOBMAZON.get())) return standard();
        if (stack.is(ModItems.BOBMAZON_HIDDEN.get())) return special();
        return null;
    }

    private static ItemStack ingot(NTMMaterial material, int count) {
        return new ItemStack(ModItems.ingot(material), count);
    }

    private static ItemStack barrel(Fluid fluid, int count) {
        ItemStack stack = new ItemStack(ModItems.FLUID_BARREL, count);
        ModItems.FLUID_BARREL.get().setContent(stack, new FluidStackNTM(fluid, 16000));
        return stack;
    }

    private static List<BobmazonOffer> buildStandard() {
        List<BobmazonOffer> out = new ArrayList<>();

        out.add(new BobmazonOffer(new ItemStack(Items.TORCH, 64), BobmazonRequirement.NONE, 2));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModItems.DEFINITELY_FOOD, 16), BobmazonRequirement.NONE, 4));
        out.add(new BobmazonOffer(new ItemStack(ModItems.NITRA, 4), CHEMICS, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.GUN_KIT_1), ASSEMBLY, 16));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModItems.GEIGER_COUNTER), BobmazonRequirement.NONE, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.MATCHSTICK, 16), STEEL, 2));

        out.add(
                new BobmazonOffer(
                        ModItems.BLUEPRINT_FOLDER.stack(ItemBlueprintFolder.Kind.ALT),
                        ASSEMBLY,
                        64));
        out.add(
                new BobmazonOffer(
                        ModItems.BLUEPRINT_FOLDER.stack(ItemBlueprintFolder.Kind.DISCOVER),
                        OIL,
                        256));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.VENDING_MACHINE), CHEMICS, 64));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.VENDING_MACHINE_SNACKS), CHEMICS, 64));

        out.add(new BobmazonOffer(new ItemStack(Items.JUNGLE_SAPLING), STEEL, 12, 9));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.PLANT_FLOWER_FOXGLOVE), STEEL, 16, 5));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.PLANT_FLOWER_TOBACCO), STEEL, 16, 9));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.PLANT_FLOWER_NIGHTSHADE), STEEL, 16, 3));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.PLANT_FLOWER_WEED), STEEL, 4, 10));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.PLANT_FLOWER_CD0), NUCLEAR, 64, 8));

        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CONCRETE_EXT_MACHINE, 16), CHEMICS, 4));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_MACHINE_STRIPE, 16), CHEMICS, 4));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CONCRETE_EXT_INDIGO, 16), CHEMICS, 4));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CONCRETE_EXT_PURPLE, 16), CHEMICS, 4));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CONCRETE_EXT_PINK, 16), CHEMICS, 4));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CONCRETE_EXT_HAZARD, 16), CHEMICS, 4));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CONCRETE_EXT_SAND, 16), CHEMICS, 4));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CONCRETE_EXT_BRONZE, 16), CHEMICS, 4));

        for (SnowglobeType globe : SnowglobeType.values()) {
            out.add(new BobmazonOffer(BlockEntitySnowglobe.stackOf(globe), CHEMICS, 128));
        }

        for (int ordinal = 1; ordinal < PlushieType.count(); ordinal++) {
            out.add(
                    new BobmazonOffer(
                            BlockEntityPlushie.stackOf(PlushieType.byOrdinal(ordinal)),
                            OIL,
                            16,
                            ordinal < 3 ? 10 : 0));
        }

        return out;
    }

    private static List<BobmazonOffer> buildSpecial() {
        List<BobmazonOffer> out = new ArrayList<>();

        out.add(new BobmazonOffer(new ItemStack(Items.IRON_INGOT, 64), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_STEEL, 64), STEEL, 1));

        out.add(new BobmazonOffer(new ItemStack(Items.COPPER_INGOT, 64), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_MINGRADE, 64), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_TITANIUM, 64), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_TUNGSTEN, 64), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_COBALT, 64), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_DESH, 64), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_TANTALIUM, 64), STEEL, 5));
        out.add(new BobmazonOffer(ingot(Mats.MAT_BISMUTH, 16), STEEL, 5));
        out.add(new BobmazonOffer(ingot(Mats.MAT_SCHRABIDIUM, 16), STEEL, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.INGOT_EUPHEMIUM, 8), STEEL, 16));
        out.add(new BobmazonOffer(ingot(Mats.MAT_DNT, 1), STEEL, 16));
        out.add(new BobmazonOffer(ingot(Mats.MAT_STAR, 16), STEEL, 8));
        out.add(new BobmazonOffer(new ItemStack(ModItems.INGOT_SEMTEX, 16), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_U235, 16), STEEL, 1));
        out.add(new BobmazonOffer(ingot(Mats.MAT_PU239, 16), STEEL, 1));
        out.add(new BobmazonOffer(new ItemStack(ModItems.AMMO_CONTAINER, 16), STEEL, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.NUKE_STARTER_KIT), STEEL, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.NUKE_ADVANCED_KIT), STEEL, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.NUKE_COMMERCIALLY_KIT), STEEL, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.BOY_KIT), STEEL, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.PROTOTYPE_KIT), STEEL, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.MISSILE_KIT), STEEL, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.JETPACK_VECTOR), STEEL, 2));
        out.add(new BobmazonOffer(new ItemStack(ModItems.JETPACK_TANK), STEEL, 2));
        out.add(new BobmazonOffer(new ItemStack(ModItems.GUN_KIT_1), STEEL, 1));
        out.add(new BobmazonOffer(new ItemStack(ModItems.GUN_KIT_2), STEEL, 3));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE), STEEL, 3));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE), STEEL, 3));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.STRUCT_LAUNCHER, 40), STEEL, 7));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.STRUCT_SCAFFOLD, 11), STEEL, 7));
        out.add(new BobmazonOffer(new ItemStack(ModItems.LOOT_10), STEEL, 2));
        out.add(new BobmazonOffer(new ItemStack(ModItems.LOOT_15), STEEL, 2));
        out.add(new BobmazonOffer(new ItemStack(ModItems.LOOT_MISC), STEEL, 2));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CRATE_CAN), STEEL, 1));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CRATE_AMMO), STEEL, 2));
        out.add(new BobmazonOffer(ModItems.CRUCIBLE.get().depleted(), STEEL, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SPAWN_CHOPPER), STEEL, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SPAWN_WORM), STEEL, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SPAWN_UFO), STEEL, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SAT_LASER), HIDDEN, 8));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SAT_GERALD), HIDDEN, 32));
        out.add(new BobmazonOffer(new ItemStack(ModItems.BILLET_YHARONITE.get(), 4), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.INGOT_CHAINSTEEL), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.INGOT_ELECTRONIUM), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.BOOK_OF), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.MESE_PICKAXE), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.MYSTERY_SHOVEL), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.NTM_DIRT), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.EUPHEMIUM_KIT), HIDDEN, 64));

        out.add(
                new BobmazonOffer(
                        ItemKitCustom.create(
                                "Fusion Man",
                                "For the nuclear physicist on the go",
                                0xff00ff,
                                0x800080,
                                new ItemStack(ModBlocks.FUSION_KLYSTRON),
                                new ItemStack(ModBlocks.FUSION_TORUS),
                                new ItemStack(ModBlocks.FUSION_MHDT),
                                new ItemStack(ModBlocks.MACHINE_INTAKE, 3),
                                new ItemStack(ModItems.BATTERY_SPARK),
                                new ItemStack(ModBlocks.MACHINE_CHEMICAL_FACTORY, 4),
                                new ItemStack(ModBlocks.MACHINE_FLUID_TANK, 8),
                                new ItemStack(ModBlocks.RED_WIRE_COATED, 64),
                                new ItemStack(ModBlocks.CABLE, 64),
                                barrel(NTMFluids.DEUTERIUM, 64),
                                barrel(NTMFluids.TRITIUM, 64),
                                barrel(NTMFluids.PERFLUOROMETHYL, 64),
                                new ItemStack(ModBlocks.RED_PYLON_LARGE, 8),
                                new ItemStack(ModBlocks.SUBSTATION, 4),
                                new ItemStack(ModBlocks.RED_CONNECTOR, 64),
                                new ItemStack(ModItems.WIRING_RED_COPPER),
                                new ItemStack(ModBlocks.MACHINE_CHUNGUS, 3),
                                new ItemStack(ModItems.TEMPLATE_FOLDER),
                                new ItemStack(Items.PAPER, 64),
                                new ItemStack(Items.INK_SAC, 64)),
                        HIDDEN,
                        64));

        out.add(
                new BobmazonOffer(
                        ItemKitCustom.create(
                                "Maid's Cleaning Utensils",
                                "For the hard to reach spots",
                                0x00ff00,
                                0x008000,
                                new ItemStack(ModItems.GUN_M2),
                                ammo(EnumAmmo.BMG50_DU),
                                ammo(EnumAmmo.BMG50_DU),
                                ammo(EnumAmmo.BMG50_DU),
                                ammo(EnumAmmo.BMG50_DU),
                                ammo(EnumAmmo.BMG50_DU),
                                new ItemStack(ModItems.GUN_AUTOSHOTGUN),
                                ammo(EnumAmmo.G12_MAGNUM),
                                ammo(EnumAmmo.G12_MAGNUM),
                                ammo(EnumAmmo.G12_MAGNUM),
                                ammo(EnumAmmo.G12_EXPLOSIVE),
                                ammo(EnumAmmo.G12_EXPLOSIVE)),
                        HIDDEN,
                        64));

        out.add(
                new BobmazonOffer(
                        named(
                                ItemKitNBT.create(
                                        named(new ItemStack(ModItems.ROD_OF_DISCORD), "Cock Joke"),
                                        named(
                                                ModItems.CANNED_CONSERVE.stack(
                                                        EnumFoodType.SLIME, 64),
                                                "Class A Horse Semen"),
                                        named(
                                                new ItemStack(ModItems.WEAPON_PIPE_LEAD),
                                                "Get Nutted, Dumbass"),
                                        new ItemStack(ModItems.GEM_ALEXANDRITE)),
                                "The Nut Bucket"),
                        HIDDEN,
                        64));

        out.add(
                new BobmazonOffer(
                        named(
                                ItemKitNBT.create(
                                        new ItemStack(ModItems.RPA_HELMET),
                                        new ItemStack(ModItems.RPA_PLATE),
                                        new ItemStack(ModItems.RPA_LEGS),
                                        new ItemStack(ModItems.RPA_BOOTS),
                                        new ItemStack(ModItems.GUN_MINIGUN_LACUNAE),
                                        ammo(EnumAmmo.CAPACITOR_OVERCHARGE),
                                        ammo(EnumAmmo.CAPACITOR_OVERCHARGE),
                                        ammo(EnumAmmo.CAPACITOR_OVERCHARGE)),
                                "Frenchie's Reward"),
                        HIDDEN,
                        32));

        return out;
    }

    private static ItemStack ammo(EnumAmmo type) {
        return ModItems.AMMO_STANDARD.stack(type, 64);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
}
