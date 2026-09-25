// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityRagingVortex;
import com.hbm.entity.effect.EntityVortex;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.ToolAreaAbility;
import com.hbm.handler.ability.ToolHarvestAbility;
import com.hbm.handler.ability.WeaponAbility;
import com.hbm.hazard.HazardClass;
import com.hbm.interfaces.HalfLifeType;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.armor.*;
import com.hbm.items.armor.ModArmorItem.Suit;
import com.hbm.items.bomb.ItemBombPart;
import com.hbm.items.food.*;
import com.hbm.items.machine.*;
import com.hbm.items.machine.ItemMold.MoldMulti;
import com.hbm.items.machine.ItemMold.MoldShape;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc;
import com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc;
import com.hbm.items.machine.ItemStamp.StampType;
import com.hbm.items.machine.ItemZirnoxRod.EnumZirnoxType;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.special.*;
import com.hbm.items.special.CircuitComponentType;
import com.hbm.items.special.ItemAMSCore;
import com.hbm.items.special.ItemAntiSchrabidiumCell;
import com.hbm.items.special.ItemBookLore;
import com.hbm.items.special.ItemSingularity;
import com.hbm.items.special.ItemTrain;
import com.hbm.items.special.MaterialDyeItem;
import com.hbm.items.special.ScrapType;
import com.hbm.items.tool.*;
import com.hbm.items.tool.ItemBoatRubber;
import com.hbm.items.tool.ItemCouplingTool;
import com.hbm.items.tool.ItemDrone;
import com.hbm.items.tool.ItemFusionCore;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.tool.ItemPlasticBag;
import com.hbm.items.tool.ItemRebarPlacer;
import com.hbm.items.tool.ItemToolAbility.ToolType;
import com.hbm.items.tool.ToolTier;
import com.hbm.items.weapon.GunB92;
import com.hbm.items.weapon.GunB92Cell;
import com.hbm.items.weapon.ItemAmmoArty;
import com.hbm.items.weapon.ItemAmmoContainer;
import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart.WarheadType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemDisperser;
import com.hbm.items.weapon.ItemGrenadeDynamite;
import com.hbm.items.weapon.ItemGrenadeFishing;
import com.hbm.items.weapon.ItemMissile.MissileFormFactor;
import com.hbm.items.weapon.ItemMissile.MissileTier;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.items.weapon.WeaponizedCell;
import com.hbm.items.weapon.grenade.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.factory.XFactoryPA;
import com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.hbm.items.weapon.sedna.impl.ItemGunChemthrower;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.impl.ItemGunNI4NI;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.ItemSubtype;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.rbmk.IRBMKFluxReceiver.NType;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.RTGUtil;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import static com.hbm.inventory.OreDictManager.*;

public final class ModItems {

    public static final List<RegistryHandle<? extends ItemBattery>> BATTERIES = new ArrayList<>();

    public static RegistryHandle<ItemBatteryPack> BATTERY_PACK_QUANTUM;

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<ItemBatteryPack>[] BATTERY_PACKS =
            new RegistryHandle[EnumBatteryPack.VALUES.length];

    public static final List<RegistryHandle<ItemMachineUpgrade>> UPGRADES = new ArrayList<>();
    public static final List<RegistryHandle<ItemStamp>> STAMPS = new ArrayList<>();

    public static final NTMMaterial[] PLATE_MATERIALS = {
        Mats.MAT_IRON, Mats.MAT_GOLD, Mats.MAT_TITANIUM, Mats.MAT_ALUMINIUM, Mats.MAT_STEEL,
        Mats.MAT_LEAD, Mats.MAT_COPPER, Mats.MAT_GUNMETAL, Mats.MAT_WEAPONSTEEL, Mats.MAT_CMB,
        Mats.MAT_DURA, Mats.MAT_SATURN, Mats.MAT_SCHRABIDIUM
    };
    public static final List<RegistryHandle<MaterialShapeItem>> PLATES = plateRoster();
    public static final List<RegistryHandle<ItemMold>> MOLDS = new ArrayList<>();
    public static final List<RegistryHandle<MaterialShapeItem>> INGOTS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> RAW_MATERIALS = new ArrayList<>();
    public static final RegistryHandle<MaterialShapeItem> INGOT_SCHRARANIUM =
            MaterialShapeRoster.bespoke(
                    MaterialShapes.INGOT, Mats.MAT_SCHRARANIUM, ItemSchraranium::new);
    public static final RegistryHandle<ItemBattery> BATTERY_POTATO =
            battery("battery_potato", 1000L, 0L, 100L);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_SPEED_1 =
            upgrade("upgrade_speed_1", UpgradeType.SPEED, 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_SPEED_2 =
            upgrade("upgrade_speed_2", UpgradeType.SPEED, 2);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_SPEED_3 =
            upgrade("upgrade_speed_3", UpgradeType.SPEED, 3);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_EFFECT_1 =
            upgrade("upgrade_effect_1", UpgradeType.EFFECT, 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_EFFECT_2 =
            upgrade("upgrade_effect_2", UpgradeType.EFFECT, 2);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_EFFECT_3 =
            upgrade("upgrade_effect_3", UpgradeType.EFFECT, 3);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_POWER_1 =
            upgrade("upgrade_power_1", UpgradeType.POWER, 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_POWER_2 =
            upgrade("upgrade_power_2", UpgradeType.POWER, 2);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_POWER_3 =
            upgrade("upgrade_power_3", UpgradeType.POWER, 3);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_FORTUNE_1 =
            upgrade("upgrade_fortune_1", UpgradeType.FORTUNE, 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_FORTUNE_2 =
            upgrade("upgrade_fortune_2", UpgradeType.FORTUNE, 2);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_FORTUNE_3 =
            upgrade("upgrade_fortune_3", UpgradeType.FORTUNE, 3);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_AFTERBURN_1 =
            upgrade("upgrade_afterburn_1", UpgradeType.AFTERBURN, 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_AFTERBURN_2 =
            upgrade("upgrade_afterburn_2", UpgradeType.AFTERBURN, 2);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_AFTERBURN_3 =
            upgrade("upgrade_afterburn_3", UpgradeType.AFTERBURN, 3);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_OVERDRIVE_1 =
            upgrade("upgrade_overdrive_1", UpgradeType.OVERDRIVE, 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_OVERDRIVE_2 =
            upgrade("upgrade_overdrive_2", UpgradeType.OVERDRIVE, 2);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_OVERDRIVE_3 =
            upgrade("upgrade_overdrive_3", UpgradeType.OVERDRIVE, 3);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_RADIUS =
            special(
                    "upgrade_radius",
                    16,
                    Component.translatable("desc.upgrade7").withStyle(ChatFormatting.GOLD),
                    Component.literal(" ").append(Component.translatable("desc.upgraderd")),
                    Component.empty(),
                    Component.literal(" ").append(Component.translatable("desc.upgradestack")));
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_HEALTH =
            special(
                    "upgrade_health",
                    16,
                    Component.translatable("desc.upgrade8").withStyle(ChatFormatting.GOLD),
                    Component.literal(" ").append(Component.translatable("desc.upgradeht")),
                    Component.empty(),
                    Component.literal(" ").append(Component.translatable("desc.upgradestack")));
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_GC_SPEED =
            special(
                    "upgrade_gc_speed",
                    1,
                    Component.translatable("desc.item.gasCentrifugeUpgrade")
                            .withStyle(ChatFormatting.RED),
                    Component.translatable("desc.item.allowsForTotalIsotopic"),
                    Component.translatable("desc.item.alsoYourCentrifugeGoes")
                            .withStyle(ChatFormatting.YELLOW));

    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_SMELTER =
            special(
                    "upgrade_smelter",
                    1,
                    Component.translatable("desc.item.miningLaserUpgrade")
                            .withStyle(ChatFormatting.RED),
                    Component.translatable("desc.item.smeltsBlocksEasyEnough"));
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_SHREDDER =
            special(
                    "upgrade_shredder",
                    1,
                    Component.translatable("desc.item.miningLaserUpgrade")
                            .withStyle(ChatFormatting.RED),
                    Component.translatable("desc.item.crunchesOres"));
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_CENTRIFUGE =
            special(
                    "upgrade_centrifuge",
                    1,
                    Component.translatable("desc.item.miningLaserUpgrade")
                            .withStyle(ChatFormatting.RED),
                    Component.translatable("desc.item.hopefullySelfExplanatory"));
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_CRYSTALLIZER =
            special(
                    "upgrade_crystallizer",
                    1,
                    Component.translatable("desc.item.miningLaserUpgrade")
                            .withStyle(ChatFormatting.RED),
                    Component.translatable("desc.item.yourNewBestFriend"));
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_NULLIFIER =
            special(
                    "upgrade_nullifier",
                    1,
                    Component.translatable("desc.item.miningLaserUpgrade")
                            .withStyle(ChatFormatting.RED),
                    Component.translatable("desc.item.50ChanceToOverride"),
                    Component.translatable("desc.item.50ChanceToMove"));
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_SCREM =
            special(
                    "upgrade_screm",
                    1,
                    Component.translatable("desc.item.miningLaserUpgrade")
                            .withStyle(ChatFormatting.RED),
                    Component.translatable("desc.item.itSLikeIn"),
                    Component.translatable("desc.item.actuallyToadsButHere"),
                    Component.translatable("desc.item.andTheyScreamA"));

    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_5G = special("upgrade_5g", 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_STACK_1 =
            upgrade("upgrade_stack_1", UpgradeType.STACK, 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_STACK_2 =
            upgrade("upgrade_stack_2", UpgradeType.STACK, 2);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_STACK_3 =
            upgrade("upgrade_stack_3", UpgradeType.STACK, 3);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_EJECTOR_1 =
            upgrade("upgrade_ejector_1", UpgradeType.EJECTOR, 1);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_EJECTOR_2 =
            upgrade("upgrade_ejector_2", UpgradeType.EJECTOR, 2);
    public static final RegistryHandle<ItemMachineUpgrade> UPGRADE_EJECTOR_3 =
            upgrade("upgrade_ejector_3", UpgradeType.EJECTOR, 3);
    public static final RegistryHandle<ItemMuffler> UPGRADE_MUFFLER =
            Reg.item("upgrade_muffler", ItemMuffler::new, Item.Properties::new);
    public static final RegistryHandle<Item> UPGRADE_TEMPLATE =
            Reg.item("upgrade_template", Item::new, () -> new Item.Properties().stacksTo(16));
    public static final RegistryHandle<ItemStamp> STAMP_STONE_FLAT =
            stampDamageable("stamp_stone_flat", 32, StampType.FLAT);
    public static final RegistryHandle<ItemStamp> STAMP_STONE_PLATE =
            stampDamageable("stamp_stone_plate", 32, StampType.PLATE);
    public static final RegistryHandle<ItemStamp> STAMP_STONE_WIRE =
            stampDamageable("stamp_stone_wire", 32, StampType.WIRE);
    public static final RegistryHandle<ItemStamp> STAMP_STONE_CIRCUIT =
            stampDamageable("stamp_stone_circuit", 32, StampType.CIRCUIT);
    public static final RegistryHandle<ItemStamp> STAMP_IRON_FLAT =
            stampDamageable("stamp_iron_flat", 64, StampType.FLAT);
    public static final RegistryHandle<ItemStamp> STAMP_IRON_PLATE =
            stampDamageable("stamp_iron_plate", 64, StampType.PLATE);
    public static final RegistryHandle<ItemStamp> STAMP_IRON_WIRE =
            stampDamageable("stamp_iron_wire", 64, StampType.WIRE);
    public static final RegistryHandle<ItemStamp> STAMP_IRON_CIRCUIT =
            stampDamageable("stamp_iron_circuit", 64, StampType.CIRCUIT);
    public static final RegistryHandle<ItemStamp> STAMP_STEEL_FLAT =
            stampDamageable("stamp_steel_flat", 192, StampType.FLAT);
    public static final RegistryHandle<ItemStamp> STAMP_STEEL_PLATE =
            stampDamageable("stamp_steel_plate", 192, StampType.PLATE);
    public static final RegistryHandle<ItemStamp> STAMP_STEEL_WIRE =
            stampDamageable("stamp_steel_wire", 192, StampType.WIRE);
    public static final RegistryHandle<ItemStamp> STAMP_STEEL_CIRCUIT =
            stampDamageable("stamp_steel_circuit", 192, StampType.CIRCUIT);
    public static final RegistryHandle<ItemStamp> STAMP_TITANIUM_FLAT =
            stampDamageable("stamp_titanium_flat", 256, StampType.FLAT);
    public static final RegistryHandle<ItemStamp> STAMP_TITANIUM_PLATE =
            stampDamageable("stamp_titanium_plate", 256, StampType.PLATE);
    public static final RegistryHandle<ItemStamp> STAMP_TITANIUM_WIRE =
            stampDamageable("stamp_titanium_wire", 256, StampType.WIRE);
    public static final RegistryHandle<ItemStamp> STAMP_TITANIUM_CIRCUIT =
            stampDamageable("stamp_titanium_circuit", 256, StampType.CIRCUIT);
    public static final RegistryHandle<ItemStamp> STAMP_OBSIDIAN_FLAT =
            stampDamageable("stamp_obsidian_flat", 512, StampType.FLAT);
    public static final RegistryHandle<ItemStamp> STAMP_OBSIDIAN_PLATE =
            stampDamageable("stamp_obsidian_plate", 512, StampType.PLATE);
    public static final RegistryHandle<ItemStamp> STAMP_OBSIDIAN_WIRE =
            stampDamageable("stamp_obsidian_wire", 512, StampType.WIRE);
    public static final RegistryHandle<ItemStamp> STAMP_OBSIDIAN_CIRCUIT =
            stampDamageable("stamp_obsidian_circuit", 512, StampType.CIRCUIT);

    public static final RegistryHandle<ItemStamp> STAMP_DESH_FLAT =
            stampIndestructible("stamp_desh_flat", StampType.FLAT);
    public static final RegistryHandle<ItemStamp> STAMP_DESH_PLATE =
            stampIndestructible("stamp_desh_plate", StampType.PLATE);
    public static final RegistryHandle<ItemStamp> STAMP_DESH_WIRE =
            stampIndestructible("stamp_desh_wire", StampType.WIRE);
    public static final RegistryHandle<ItemStamp> STAMP_DESH_CIRCUIT =
            stampIndestructible("stamp_desh_circuit", StampType.CIRCUIT);
    public static final RegistryHandle<ItemStamp> STAMP_357 =
            stampDamageable("stamp_357", 1000, StampType.C357);
    public static final RegistryHandle<ItemStamp> STAMP_44 =
            stampDamageable("stamp_44", 1000, StampType.C44);
    public static final RegistryHandle<ItemStamp> STAMP_9 =
            stampDamageable("stamp_9", 1000, StampType.C9);
    public static final RegistryHandle<ItemStamp> STAMP_50 =
            stampDamageable("stamp_50", 1000, StampType.C50);
    public static final RegistryHandle<ItemStamp> STAMP_DESH_357 =
            stampIndestructible("stamp_desh_357", StampType.C357);
    public static final RegistryHandle<ItemStamp> STAMP_DESH_44 =
            stampIndestructible("stamp_desh_44", StampType.C44);
    public static final RegistryHandle<ItemStamp> STAMP_DESH_9 =
            stampIndestructible("stamp_desh_9", StampType.C9);
    public static final RegistryHandle<ItemStamp> STAMP_DESH_50 =
            stampIndestructible("stamp_desh_50", StampType.C50);
    public static final RegistryHandle<ItemFluidBucket> FLUID_BUCKET =
            Reg.item(
                            "fluid_bucket",
                            ItemFluidBucket::new,
                            () -> new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))
                    .subtypes(ItemSubtype.FLUID_CONTENT);

    public static final RegistryHandle<BucketItem> BUCKET_ACID =
            Reg.item(
                    "bucket_acid",
                    props -> new BucketItem(ModBlocks.ACID_FLUID.source().get(), props),
                    () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final RegistryHandle<BucketItem> BUCKET_TOXIC =
            Reg.item(
                    "bucket_toxic",
                    props -> new BucketItem(ModBlocks.TOXIC_FLUID.source().get(), props),
                    () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
    public static final RegistryHandle<ItemFluidTank> FLUID_TANK =
            fluidTank("fluid_tank", 1000, ItemFluidTank.Family.TANK);
    public static final RegistryHandle<ItemFluidTank> FLUID_BARREL =
            fluidTank("fluid_barrel", 16000, ItemFluidTank.Family.BARREL);

    public static final RegistryHandle<ItemFluidTank> FLUID_TANK_LEAD =
            fluidTank("fluid_tank_lead", 1000, ItemFluidTank.Family.LEAD);
    public static final RegistryHandle<ItemFluidTank> CANISTER =
            fluidTank("canister", 1000, ItemFluidTank.Family.CANISTER);
    public static final RegistryHandle<ItemFluidTank> GAS_TANK =
            fluidTank("gas_tank", 1000, ItemFluidTank.Family.GAS);

    public static final RegistryHandle<ItemFluidTank> FLUID_PACK =
            fluidTank("fluid_pack", 32_000, ItemFluidTank.Family.PACK);

    public static final RegistryHandle<ItemFluidContainerInfinite> FLUID_BARREL_INFINITE =
            Reg.item(
                    "fluid_barrel_infinite",
                    p -> new ItemFluidContainerInfinite(p, null, 1_000_000_000),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemFluidContainerInfinite> INF_WATER =
            Reg.item(
                    "inf_water",
                    p -> new ItemFluidContainerInfinite(p, () -> NTMFluids.WATER, 50),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemFluidContainerInfinite> INF_WATER_MK2 =
            Reg.item(
                    "inf_water_mk2",
                    p -> new ItemFluidContainerInfinite(p, () -> NTMFluids.WATER, 500),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemFluidContainerInfinite> CHLORINE_PINWHEEL =
            Reg.item(
                    "chlorine_pinwheel",
                    p -> new ItemFluidContainerInfinite(p, () -> NTMFluids.CHLORINE, 1, 2),
                    Item.Properties::new);

    public static final RegistryHandle<Item> FRAGMENT_NEODYMIUM =
            oreFragment("fragment_neodymium")
                    .materialShape(Mats.MAT_NEODYMIUM, MaterialShapes.NUGGET);
    public static final RegistryHandle<Item> FRAGMENT_COBALT = oreFragment("fragment_cobalt");
    public static final RegistryHandle<Item> FRAGMENT_NIOBIUM = oreFragment("fragment_niobium");
    public static final RegistryHandle<Item> FRAGMENT_CERIUM = oreFragment("fragment_cerium");
    public static final RegistryHandle<Item> FRAGMENT_LANTHANIUM =
            oreFragment("fragment_lanthanium")
                    .materialShape(Mats.MAT_LANTHANIUM, MaterialShapes.NUGGET);
    public static final RegistryHandle<Item> FRAGMENT_ACTINIUM = oreFragment("fragment_actinium");
    public static final RegistryHandle<Item> FRAGMENT_BORON =
            oreFragment("fragment_boron").materialShape(Mats.MAT_BORON, MaterialShapes.NUGGET);
    public static final RegistryHandle<Item> FRAGMENT_METEORITE =
            Reg.item("fragment_meteorite", Item::new, () -> new Item.Properties());
    public static final RegistryHandle<Item> FRAGMENT_COLTAN =
            Reg.item("fragment_coltan", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> RAW_ALUMINIUM =
            rawMaterial("aluminium", "aluminum", "Aluminium");
    public static final RegistryHandle<Item> RAW_BERYLLIUM =
            rawMaterial("beryllium", "beryllium", "Beryllium");
    public static final RegistryHandle<Item> RAW_LEAD = rawMaterial("lead", "lead", "Lead");
    public static final RegistryHandle<Item> RAW_THORIUM =
            rawMaterial("thorium", "thorium", "Thorium");
    public static final RegistryHandle<Item> RAW_TITANIUM =
            rawMaterial("titanium", "titanium", "Titanium");
    public static final RegistryHandle<Item> RAW_TUNGSTEN =
            rawMaterial("tungsten", "tungsten", "Tungsten");
    public static final RegistryHandle<Item> RAW_URANIUM =
            rawMaterial("uranium", "uranium", "Uranium");
    public static final RegistryHandle<Item> RAW_PLUTONIUM =
            rawMaterial("plutonium", "plutonium", "Plutonium");
    public static final RegistryHandle<Item> RAW_COBALT = rawMaterial("cobalt", "cobalt", "Cobalt");
    public static final RegistryHandle<Item> RAW_SCHRABIDIUM =
            rawMaterial("schrabidium", "schrabidium", "Schrabidium");
    public static final RegistryHandle<Item> RAW_LITHIUM =
            rawMaterial("lithium", "lithium", "Lithium");

    public static final RegistryHandle<ItemHotDusted> INGOT_STEEL_DUSTED =
            Reg.item(
                            "ingot_steel_dusted",
                            properties -> new ItemHotDusted(properties, 200),
                            () ->
                                    new Item.Properties()
                                            .component(ModDataComponents.FORGE_COUNT.get(), 0))
                    .state(ModDataComponents.FORGE_COUNT);
    public static final RegistryHandle<ItemHot> INGOT_CHAINSTEEL =
            Reg.item(
                    "ingot_chainsteel",
                    properties -> new ItemHot(properties, 100),
                    Item.Properties::new);
    public static final RegistryHandle<ItemHot> INGOT_METEORITE =
            Reg.item(
                    "ingot_meteorite",
                    properties -> new ItemHot(properties, 200),
                    Item.Properties::new);
    public static final RegistryHandle<ItemHot> INGOT_METEORITE_FORGED =
            Reg.item(
                    "ingot_meteorite_forged",
                    properties -> new ItemHot(properties, 200),
                    Item.Properties::new);
    public static final RegistryHandle<ItemHot> BLADE_METEORITE =
            Reg.item(
                    "blade_meteorite",
                    properties -> new ItemHot(properties, 200),
                    Item.Properties::new);
    public static final List<RegistryHandle<MaterialShapeItem>> POWDERS = new ArrayList<>();
    public static final RegistryHandle<MaterialShapeItem> POWDER_COAL =
            MaterialShapeRoster.bespoke(
                    MaterialShapes.DUST,
                    Mats.MAT_COAL,
                    (props, material, shape) ->
                            new MaterialDyeItem(props, material, shape, DyeColor.BLACK));
    public static final RegistryHandle<MaterialShapeItem> POWDER_LIGNITE =
            MaterialShapeRoster.bespoke(
                    MaterialShapes.DUST,
                    Mats.MAT_LIGNITE,
                    (props, material, shape) ->
                            new MaterialDyeItem(props, material, shape, DyeColor.BROWN));
    public static final RegistryHandle<MaterialShapeItem> POWDER_TITANIUM =
            MaterialShapeRoster.bespoke(
                    MaterialShapes.DUST,
                    Mats.MAT_TITANIUM,
                    (props, material, shape) ->
                            new MaterialDyeItem(props, material, shape, DyeColor.LIGHT_GRAY));
    public static final RegistryHandle<MaterialShapeItem> POWDER_CADMIUM =
            MaterialShapeRoster.bespoke(
                    MaterialShapes.DUST,
                    Mats.MAT_CADMIUM,
                    (props, material, shape) ->
                            new MaterialDyeItem(props, material, shape, DyeColor.ORANGE));
    public static final List<RegistryHandle<MaterialShapeItem>> NUGGETS = new ArrayList<>();
    public static final List<RegistryHandle<MaterialShapeItem>> BILLETS = new ArrayList<>();
    public static final NTMMaterial[] BILLET_MATERIALS =
            MaterialShapeRoster.roster(MaterialShapes.BILLET, Set.of()).toArray(new NTMMaterial[0]);
    public static final List<RegistryHandle<? extends Item>> MISC_ALL = new ArrayList<>();
    public static final List<RegistryHandle<? extends Item>> ARMOR_MOD_ALL = new ArrayList<>();
    public static final List<RegistryHandle<? extends Item>> MISSILE_ALL = new ArrayList<>();
    public static final List<RegistryHandle<? extends Item>> WEAPON_ALL = new ArrayList<>();
    public static final List<RegistryHandle<? extends Item>> COMPONENT_ALL = new ArrayList<>();
    public static final long T51_MAX_POWER = 1_000_000L;
    public static final long T51_CHARGE_RATE = 10_000L;
    public static final long T51_CONSUMPTION = 1_000L;
    public static final long T51_DRAIN = 5L;
    public static final List<RegistryHandle<? extends Item>> ARMOR_ALL = new ArrayList<>();
    public static final List<RegistryHandle<? extends Item>> RESIDUAL_ALL = new ArrayList<>();
    public static final List<RegistryHandle<? extends Item>> COLLECTIBLE_ALL = new ArrayList<>();
    public static final List<RegistryHandle<ItemRBMKRod>> RBMK_FUELS = new ArrayList<>();

    public static final List<RegistryHandle<ItemRBMKRod>> CRAFTABLE_RODS = new ArrayList<>();
    public static final List<RegistryHandle<ItemRBMKPellet>> PELLETS = new ArrayList<>();
    public static final List<RegistryHandle<ItemPWRFuel>> PWR_FUELS = new ArrayList<>();
    public static final List<RegistryHandle<ItemDepletedFuel>> DEPLETED_ALL = new ArrayList<>();
    public static final List<RegistryHandle<? extends Item>> RTG_ALL = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<ItemZirnoxRod>[] FUEL =
            new RegistryHandle[EnumZirnoxType.VALUES.length];

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<ItemZirnoxRodDepleted>[] DEPLETED =
            new RegistryHandle[EnumZirnoxType.VALUES.length];

    public static final List<RegistryHandle<? extends Item>> ZIRNOX_ALL = new ArrayList<>();
    public static final List<RegistryHandle<ItemPileRod>> PILE_ALL = new ArrayList<>();
    public static final List<RegistryHandle<ItemPlateFuel>> PLATE_FUELS = new ArrayList<>();
    public static final RegistryHandle<Item> TEST_ITEM =
            Reg.item("test_item", Item::new, Item.Properties::new);

    public static final RegistryHandle<ItemUnstable> INGOT_U238M2 =
            Reg.item("ingot_u238m2", ItemUnstable::new, Item.Properties::new);

    public static final RegistryHandle<Item> HS_ELEMENTS = hs("hs_elements", "item/hs-elements", 1);
    public static final RegistryHandle<Item> HS_ARSENIC = hs("hs_arsenic", "item/hs-arsenic", 2);
    public static final RegistryHandle<Item> HS_VAULT = hs("hs_vault", "item/hs-vault", 3);
    public static final RegistryHandle<Item> INGOT_SR90 =
            residualItem("ingot_sr90").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> INGOT_TUNGSTEN_CARBIDE =
            residualItem("ingot_tungsten_carbide").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> INGOT_FIREBRICK =
            residualItem("ingot_firebrick").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_BIORUBBER =
            residualLore("ingot_biorubber").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_URANIUM_FUEL =
            residualLore("ingot_uranium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_THORIUM_FUEL =
            residualLore("ingot_thorium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_PLUTONIUM_FUEL =
            residualLore("ingot_plutonium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_NEPTUNIUM_FUEL =
            residualLore("ingot_neptunium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_MOX_FUEL =
            residualLore("ingot_mox_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_AMERICIUM_FUEL =
            residualLore("ingot_americium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_SCHRABIDIUM_FUEL =
            residualLore("ingot_schrabidium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_HES =
            residualLore("ingot_hes").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_LES =
            residualLore("ingot_les").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_AUSTRALIUM =
            residualLore("ingot_australium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_EUPHEMIUM =
            residualLore("ingot_euphemium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_ELECTRONIUM =
            Reg.item("ingot_electronium", ItemCustomLore::new, Item.Properties::new);
    public static final RegistryHandle<Item> INGOT_SMORE =
            Reg.item("ingot_smore", Item::new, () -> food(10, 20F)).addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> INGOT_PHOSPHORUS =
            residualLore("ingot_phosphorus").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> LITHIUM =
            residualLore("lithium").addTo(RESIDUAL_ALL);

    public static final RegistryHandle<ItemCustomLore> INGOT_SEMTEX =
            Reg.item(
                            "ingot_semtex",
                            ItemCustomLore::new,
                            () ->
                                    new Item.Properties()
                                            .food(
                                                    new FoodProperties.Builder()
                                                            .nutrition(4)
                                                            .saturationModifier(5F)
                                                            .build()))
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> INGOT_C4 =
            Reg.item(
                            "ingot_c4",
                            Item::new,
                            () ->
                                    new Item.Properties()
                                            .food(
                                                    new FoodProperties.Builder()
                                                            .nutrition(4)
                                                            .saturationModifier(5F)
                                                            .build()))
                    .addTo(RESIDUAL_ALL);
    public static final ItemFamily<EnumTarType, Item> OIL_TAR =
            Reg.<EnumTarType, Item>family(
                    "oil_tar",
                    EnumTarType.class,
                    (props, type) -> new DyeItem(props.component(DataComponents.DYE, type.dye)),
                    Item.Properties::new);

    public static final RegistryHandle<Item> LIGNITE =
            Reg.item("lignite", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> COAL_INFERNAL =
            Reg.item("coal_infernal", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemEternalCoal> COAL_ETERNAL =
            Reg.item("coal_eternal", ItemEternalCoal::new, () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<Item> SOLID_FUEL =
            Reg.item("solid_fuel", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> SOLID_FUEL_PRESTO =
            Reg.item("solid_fuel_presto", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> SOLID_FUEL_PRESTO_TRIPLET =
            Reg.item("solid_fuel_presto_triplet", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> SOLID_FUEL_BF =
            Reg.item("solid_fuel_bf", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> SOLID_FUEL_PRESTO_BF =
            Reg.item("solid_fuel_presto_bf", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> SOLID_FUEL_PRESTO_TRIPLET_BF =
            Reg.item("solid_fuel_presto_triplet_bf", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> ROCKET_FUEL =
            Reg.item("rocket_fuel", Item::new, Item.Properties::new);
    public static final RegistryHandle<ItemCustomLore> INGOT_FIBERGLASS =
            residualLore("ingot_fiberglass").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_UZH =
            residualLore("billet_uzh").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_SR90 =
            residualLore("billet_sr90").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_AUSTRALIUM =
            residualLore("billet_australium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_AUSTRALIUM_LESSER =
            residualLore("billet_australium_lesser").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_AUSTRALIUM_GREATER =
            residualLore("billet_australium_greater").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_URANIUM_FUEL =
            residualLore("billet_uranium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_THORIUM_FUEL =
            residualLore("billet_thorium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_PLUTONIUM_FUEL =
            residualLore("billet_plutonium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_NEPTUNIUM_FUEL =
            residualLore("billet_neptunium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_MOX_FUEL =
            residualLore("billet_mox_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_AMERICIUM_FUEL =
            residualLore("billet_americium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_LES =
            residualLore("billet_les").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_SCHRABIDIUM_FUEL =
            residualLore("billet_schrabidium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_HES =
            residualLore("billet_hes").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_PO210BE =
            residualLore("billet_po210be").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_RA226BE =
            residualLore("billet_ra226be").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_PU238BE =
            residualLore("billet_pu238be").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_ZFB_BISMUTH =
            residualLore("billet_zfb_bismuth").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_ZFB_PU241 =
            residualLore("billet_zfb_pu241").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_ZFB_AM_MIX =
            residualLore("billet_zfb_am_mix").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> BILLET_YHARONITE =
            residualItem("billet_yharonite").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_BALEFIRE_GOLD =
            residualLore("billet_balefire_gold").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_FLASHLEAD =
            residualLore("billet_flashlead").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> BILLET_NUCLEAR_WASTE =
            residualLore("billet_nuclear_waste").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_MERCURY_TINY =
            Reg.item("nugget_mercury_tiny", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_MERCURY =
            Reg.item("nugget_mercury", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> BOTTLE_MERCURY =
            Reg.item(
                            "bottle_mercury",
                            ItemCustomLore::new,
                            () -> new Item.Properties().craftRemainder(Items.GLASS_BOTTLE))
                    .addTo(COLLECTIBLE_ALL);
    public static final ItemFamily<EnumCokeType, Item> COKE =
            Reg.family(
                    "coke",
                    EnumCokeType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final ItemFamily<EnumBriquetteType, Item> BRIQUETTE =
            Reg.family(
                    "briquette",
                    EnumBriquetteType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final RegistryHandle<Item> SULFUR =
            Reg.<Item>item(
                            "sulfur",
                            props ->
                                    new DyeItem(
                                            props.component(DataComponents.DYE, DyeColor.YELLOW)),
                            Item.Properties::new)
                    .materialShape(Mats.MAT_SULFUR, MaterialShapes.DUST)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> NITER =
            Reg.item("niter", Item::new, Item.Properties::new)
                    .materialShape(Mats.MAT_KNO, MaterialShapes.DUST)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> NITRA =
            Reg.item("nitra", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> NITRA_SMALL =
            Reg.item("nitra_small", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> FLUORITE =
            Reg.<Item>item(
                            "fluorite",
                            props ->
                                    new DyeItem(
                                            props.component(DataComponents.DYE, DyeColor.WHITE)),
                            Item.Properties::new)
                    .addTo(COMPONENT_ALL);

    public static final RegistryHandle<MaterialShapeItem> POWDER_COAL_TINY =
            Reg.item(
                            "powder_coal_tiny",
                            Library.id("powder_coal_tiny"),
                            props ->
                                    new MaterialShapeItem(
                                            props, Mats.MAT_COAL, MaterialShapes.DUSTTINY),
                            Item.Properties::new)
                    .materialShape(Mats.MAT_COAL, MaterialShapes.DUSTTINY);
    public static final RegistryHandle<Item> POWDER_LAPIS =
            Reg.<Item>item(
                            "powder_lapis",
                            props ->
                                    new DyeItem(props.component(DataComponents.DYE, DyeColor.BLUE)),
                            Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> POWDER_QUARTZ =
            Reg.item("powder_quartz", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_SR90 =
            residualLore("powder_sr90").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_SR90_TINY =
            residualLore("powder_sr90_tiny").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_I131 =
            residualLore("powder_i131").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_I131_TINY =
            residualLore("powder_i131_tiny").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_XE135 =
            residualLore("powder_xe135").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_XE135_TINY =
            residualLore("powder_xe135_tiny").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_CS137 =
            residualLore("powder_cs137").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_CS137_TINY =
            residualLore("powder_cs137_tiny").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_AT209 =
            residualLore("powder_at209").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> POWDER_RED_COPPER =
            Reg.item("powder_red_copper", Item::new, Item.Properties::new)
                    .materialShape(Mats.MAT_MINGRADE, MaterialShapes.DUST)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> POWDER_STEEL_TINY =
            Reg.item("powder_steel_tiny", Item::new, Item.Properties::new)
                    .materialShape(Mats.MAT_STEEL, MaterialShapes.DUSTTINY)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> POWDER_COLTAN =
            Reg.item("powder_coltan", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_COLTAN_ORE =
            residualLore("powder_coltan_ore").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_TANTALIUM =
            residualLore("powder_tantalium")
                    .materialShape(Mats.MAT_TANTALIUM, MaterialShapes.DUST)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_TEKTITE =
            residualLore("powder_tektite").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_PALEOGENITE =
            residualLore("powder_paleogenite").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_PALEOGENITE_TINY =
            residualLore("powder_paleogenite_tiny").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_IMPURE_OSMIRIDIUM =
            residualLore("powder_impure_osmiridium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_YELLOWCAKE =
            residualLore("powder_yellowcake").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> POWDER_CHLOROPHYTE =
            Reg.item("powder_chlorophyte", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> POWDER_COMBINE_STEEL =
            Reg.item("powder_combine_steel", Item::new, Item.Properties::new)
                    .materialShape(Mats.MAT_CMB, MaterialShapes.DUST)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_LITHIUM_TINY =
            residualLore("powder_lithium_tiny")
                    .materialShape(Mats.MAT_LITHIUM, MaterialShapes.DUSTTINY)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_NEODYMIUM_TINY =
            residualLore("powder_neodymium_tiny")
                    .materialShape(Mats.MAT_NEODYMIUM, MaterialShapes.DUSTTINY)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_ASTATINE =
            residualLore("powder_astatine").addTo(RESIDUAL_ALL);

    public static final RegistryHandle<ItemCustomLore> POWDER_CAESIUM =
            Reg.item(
                            "powder_caesium",
                            ItemCustomLore::new,
                            () -> new Item.Properties().rarity(Rarity.EPIC))
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_AUSTRALIUM =
            residualLore("powder_australium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_COBALT_TINY =
            residualLore("powder_cobalt_tiny")
                    .materialShape(Mats.MAT_COBALT, MaterialShapes.DUSTTINY)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_BROMINE =
            residualLore("powder_bromine").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_NIOBIUM_TINY =
            residualLore("powder_niobium_tiny")
                    .materialShape(Mats.MAT_NIOBIUM, MaterialShapes.DUSTTINY)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_TENNESSINE =
            residualLore("powder_tennessine").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_CERIUM =
            residualLore("powder_cerium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_CERIUM_TINY =
            residualLore("powder_cerium_tiny").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_LANTHANIUM_TINY =
            residualLore("powder_lanthanium_tiny")
                    .materialShape(Mats.MAT_LANTHANIUM, MaterialShapes.DUSTTINY)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_ACTINIUM_TINY =
            residualLore("powder_actinium_tiny")
                    .materialShape(Mats.MAT_ACTINIUM, MaterialShapes.DUSTTINY)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_BORON_TINY =
            residualLore("powder_boron_tiny")
                    .materialShape(Mats.MAT_BORON, MaterialShapes.DUSTTINY)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_ASBESTOS =
            residualLore("powder_asbestos")
                    .materialShape(Mats.MAT_ASBESTOS, MaterialShapes.DUST)
                    .addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_MAGIC =
            residualLore("powder_magic").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> POWDER_FLUX =
            residualItem("powder_flux").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> POWDER_SAWDUST =
            Reg.item("powder_sawdust", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemFertilizer> POWDER_FERTILIZER =
            Reg.item("powder_fertilizer", ItemFertilizer::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_BALEFIRE =
            residualLore("powder_balefire").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> POWDER_SEMTEX_MIX =
            Reg.item("powder_semtex_mix", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> POWDER_DESH_MIX =
            Reg.item("powder_desh_mix", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> POWDER_DESH_READY =
            Reg.item("powder_desh_ready", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> POWDER_DESH =
            Reg.item("powder_desh", Item::new, Item.Properties::new)
                    .materialShape(Mats.MAT_DESH, MaterialShapes.DUST)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_NITAN_MIX =
            residualLore("powder_nitan_mix").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_SPARK_MIX =
            residualLore("powder_spark_mix").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> POWDER_METEORITE =
            Reg.item("powder_meteorite", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> POWDER_METEORITE_TINY =
            Reg.item("powder_meteorite_tiny", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_EUPHEMIUM =
            residualLore("powder_euphemium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> DUST =
            Reg.item("dust", ItemCustomLore::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> DUST_TINY =
            Reg.item("dust_tiny", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> FALLOUTITEM =
            Reg.item("falloutitem", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final ItemFamily<EnumAshType, Item> POWDER_ASH =
            Reg.<EnumAshType, Item>family(
                    "powder_ash",
                    EnumAshType.class,
                    (props, type) -> new DyeItem(props.component(DataComponents.DYE, type.dye)),
                    Item.Properties::new);
    public static final RegistryHandle<Item> POWDER_LIMESTONE =
            Reg.item("powder_limestone", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);

    public static final RegistryHandle<Item> POWDER_CEMENT =
            Reg.item(
                            "powder_cement",
                            Item::new,
                            () ->
                                    new Item.Properties()
                                            .food(
                                                    new FoodProperties.Builder()
                                                            .nutrition(2)
                                                            .saturationModifier(0.5F)
                                                            .build()))
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_FIRE =
            Reg.item("powder_fire", ItemCustomLore::new, Item.Properties::new)
                    .materialShape(Mats.MAT_PHOSPHORUS, MaterialShapes.DUST)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> POWDER_ICE =
            Reg.item("powder_ice", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_POISON =
            Reg.item("powder_poison", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_THERMITE =
            residualLore("powder_thermite").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> POWDER_POWER =
            residualLore("powder_power").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> CORDITE =
            Reg.item("cordite", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> BALLISTITE =
            Reg.item("ballistite", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> BALL_DYNAMITE =
            Reg.item("ball_dynamite", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> BALL_TNT =
            Reg.item("ball_tnt", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> BALL_TATB =
            Reg.item("ball_tatb", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> BALL_RESIN =
            Reg.item("ball_resin", ItemCustomLore::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> BALL_FIRECLAY =
            Reg.item("ball_fireclay", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemBedrockOreBase> BEDROCK_ORE_BASE =
            Reg.item("bedrock_ore_base", ItemBedrockOreBase::new, Item.Properties::new);
    public static final RegistryHandle<ItemBedrockOreNew> BEDROCK_ORE =
            Reg.item(
                            "bedrock_ore",
                            ItemBedrockOreNew::new,
                            () ->
                                    new Item.Properties()
                                            .component(
                                                    ModDataComponents.BEDROCK_ORE.get(),
                                                    ItemBedrockOreNew.Ore.BASE))
                    .state(ModDataComponents.BEDROCK_ORE);

    public static final RegistryHandle<Item> CRYSTAL_COAL =
            Reg.item("crystal_coal", Item::new, Item.Properties::new);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_IRON =
            Reg.item("crystal_iron", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_GOLD =
            Reg.item("crystal_gold", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_REDSTONE =
            Reg.item("crystal_redstone", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_LAPIS =
            Reg.item("crystal_lapis", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_DIAMOND =
            Reg.item("crystal_diamond", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_URANIUM =
            Reg.item("crystal_uranium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_THORIUM =
            Reg.item("crystal_thorium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_PLUTONIUM =
            Reg.item("crystal_plutonium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_TITANIUM =
            Reg.item("crystal_titanium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_SULFUR =
            Reg.item("crystal_sulfur", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_NITER =
            Reg.item("crystal_niter", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_COPPER =
            Reg.item("crystal_copper", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_TUNGSTEN =
            Reg.item("crystal_tungsten", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_ALUMINIUM =
            Reg.item("crystal_aluminium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_FLUORITE =
            Reg.item("crystal_fluorite", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_BERYLLIUM =
            Reg.item("crystal_beryllium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_LEAD =
            Reg.item("crystal_lead", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_SCHRARANIUM =
            Reg.item("crystal_schraranium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_SCHRABIDIUM =
            Reg.item("crystal_schrabidium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_RARE =
            Reg.item("crystal_rare", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_CINNABAR =
            Reg.item("crystal_cinnabar", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_PHOSPHORUS =
            Reg.item("crystal_phosphorus", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_LITHIUM =
            Reg.item("crystal_lithium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_COBALT =
            Reg.item("crystal_cobalt", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_STARMETAL =
            Reg.item("crystal_starmetal", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_TRIXITE =
            Reg.item("crystal_trixite", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_OSMIRIDIUM =
            Reg.item("crystal_osmiridium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> GEM_SODALITE =
            Reg.item("gem_sodalite", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> GEM_TANTALIUM =
            Reg.item("gem_tantalium", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> GEM_VOLCANIC =
            Reg.item(
                            "gem_volcanic",
                            ItemCustomLore::new,
                            () -> new Item.Properties().rarity(Rarity.UNCOMMON))
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> GEM_RAD =
            Reg.item("gem_rad", ItemCustomLore::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> GEM_ALEXANDRITE =
            Reg.item("gem_alexandrite", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final ItemFamily<EnumChunkType, Item> CHUNK_ORE =
            Reg.family(
                    "chunk_ore",
                    EnumChunkType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final RegistryHandle<Item> BIOMASS =
            Reg.item("biomass", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> BIOMASS_COMPRESSED =
            Reg.item("biomass_compressed", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> BIO_WAFER =
            Reg.item(
                    "bio_wafer",
                    Item::new,
                    () ->
                            new Item.Properties()
                                    .food(
                                            new FoodProperties.Builder()
                                                    .nutrition(4)
                                                    .saturationModifier(2F)
                                                    .build()));
    public static final RegistryHandle<ItemCustomLore> NUGGET_SR90 =
            residualLore("nugget_sr90").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_URANIUM_FUEL =
            residualLore("nugget_uranium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_THORIUM_FUEL =
            residualLore("nugget_thorium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_PLUTONIUM_FUEL =
            residualLore("nugget_plutonium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_NEPTUNIUM_FUEL =
            residualLore("nugget_neptunium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_MOX_FUEL =
            residualLore("nugget_mox_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_AMERICIUM_FUEL =
            residualLore("nugget_americium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_SCHRABIDIUM_FUEL =
            residualLore("nugget_schrabidium_fuel").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_HES =
            residualLore("nugget_hes").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_LES =
            residualLore("nugget_les").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_AUSTRALIUM =
            residualLore("nugget_australium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_AUSTRALIUM_LESSER =
            residualLore("nugget_australium_lesser").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemCustomLore> NUGGET_AUSTRALIUM_GREATER =
            residualLore("nugget_australium_greater").addTo(RESIDUAL_ALL);

    public static final RegistryHandle<ItemCustomLore> NUGGET_EUPHEMIUM =
            residualLore("nugget_euphemium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> NEUTRON_REFLECTOR =
            Reg.item("neutron_reflector", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_MIXED =
            Reg.item("plate_mixed", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> PLATE_PAA =
            Reg.item("plate_paa", ItemCustomLore::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_POLYMER =
            Reg.item("plate_polymer", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_KEVLAR =
            Reg.item("plate_kevlar", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_DALEKANIUM =
            Reg.item("plate_dalekanium", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_DESH =
            Reg.item("plate_desh", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> PLATE_BISMUTH =
            Reg.item("plate_bismuth", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> PLATE_EUPHEMIUM =
            residualLore("plate_euphemium").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<Item> PLATE_DINEUTRONIUM =
            Reg.item("plate_dineutronium", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_ARMOR_TITANIUM =
            Reg.item("plate_armor_titanium", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_ARMOR_AJR =
            Reg.item("plate_armor_ajr", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_ARMOR_HEV =
            Reg.item("plate_armor_hev", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_ARMOR_LUNAR =
            Reg.item("plate_armor_lunar", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_ARMOR_FAU =
            Reg.item("plate_armor_fau", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PLATE_ARMOR_DNT =
            Reg.item("plate_armor_dnt", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> BOLT_SPIKE =
            Reg.item("bolt_spike", ItemCustomLore::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> HAZMAT_CLOTH =
            Reg.item("hazmat_cloth", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> HAZMAT_CLOTH_RED =
            Reg.item("hazmat_cloth_red", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> HAZMAT_CLOTH_GREY =
            Reg.item("hazmat_cloth_grey", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> ASBESTOS_CLOTH =
            Reg.item("asbestos_cloth", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemRag> RAG =
            Reg.item("rag", ItemRag::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> RAG_DAMP =
            Reg.item("rag_damp", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> RAG_PISS =
            Reg.item("rag_piss", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> FILTER_COAL =
            Reg.item("filter_coal", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CINNABAR =
            Reg.<Item>item(
                            "cinnabar",
                            props -> new DyeItem(props.component(DataComponents.DYE, DyeColor.RED)),
                            Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> COIL_COPPER =
            Reg.item("coil_copper", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> COIL_COPPER_TORUS =
            Reg.item("coil_copper_torus", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> COIL_GOLD =
            Reg.item("coil_gold", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> COIL_GOLD_TORUS =
            Reg.item("coil_gold_torus", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> COIL_TUNGSTEN =
            Reg.item("coil_tungsten", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> COIL_MAGNETIZED_TUNGSTEN =
            Reg.item("coil_magnetized_tungsten", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> SAFETY_FUSE =
            Reg.item("safety_fuse", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> TANK_STEEL = missileItem("tank_steel");

    public static final RegistryHandle<ItemCustomLore> EARLY_EXPLOSIVE_LENSES =
            Reg.item("early_explosive_lenses", ItemCustomLore::new, Item.Properties::new);
    public static final RegistryHandle<ItemCustomLore> EXPLOSIVE_LENSES =
            Reg.item("explosive_lenses", ItemCustomLore::new, Item.Properties::new);
    public static final RegistryHandle<Item> GADGET_WIREING =
            Reg.item("gadget_wireing", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemCustomLore> GADGET_CORE =
            Reg.item(
                    "gadget_core",
                    ItemCustomLore::new,
                    () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryHandle<Item> BOY_IGNITER =
            Reg.item("boy_igniter", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> BOY_PROPELLANT =
            Reg.item("boy_propellant", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemCustomLore> BOY_BULLET =
            Reg.item(
                    "boy_bullet",
                    ItemCustomLore::new,
                    () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryHandle<ItemCustomLore> BOY_TARGET =
            Reg.item(
                    "boy_target",
                    ItemCustomLore::new,
                    () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryHandle<Item> BOY_SHIELDING =
            Reg.item("boy_shielding", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> MAN_IGNITER =
            Reg.item("man_igniter", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemCustomLore> MAN_CORE =
            Reg.item(
                    "man_core",
                    ItemCustomLore::new,
                    () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryHandle<Item> MIKE_CORE =
            Reg.item("mike_core", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> MIKE_DEUT =
            Reg.item(
                    "mike_deut",
                    Item::new,
                    () -> new Item.Properties().stacksTo(1).craftRemainder(TANK_STEEL.get()));
    public static final RegistryHandle<Item> MIKE_COOLING_UNIT =
            Reg.item("mike_cooling_unit", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> TSAR_CORE =
            Reg.item("tsar_core", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemBombPart> FLEIJA_IGNITER =
            Reg.item(
                    "fleija_igniter",
                    p -> new ItemBombPart(p, () -> ModBlocks.NUKE_FLEIJA.get()),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemBombPart> FLEIJA_PROPELLANT =
            Reg.item(
                    "fleija_propellant",
                    p -> new ItemBombPart(p, () -> ModBlocks.NUKE_FLEIJA.get()),
                    () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final RegistryHandle<ItemBombPart> FLEIJA_CORE =
            Reg.item(
                    "fleija_core",
                    p -> new ItemBombPart(p, () -> ModBlocks.NUKE_FLEIJA.get()),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemBombPart> SOLINIUM_IGNITER =
            Reg.item(
                    "solinium_igniter",
                    p -> new ItemBombPart(p, () -> ModBlocks.NUKE_SOLINIUM.get()),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemBombPart> SOLINIUM_PROPELLANT =
            Reg.item(
                    "solinium_propellant",
                    p -> new ItemBombPart(p, () -> ModBlocks.NUKE_SOLINIUM.get()),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemBombPart> SOLINIUM_CORE =
            Reg.item(
                    "solinium_core",
                    p -> new ItemBombPart(p, () -> ModBlocks.NUKE_SOLINIUM.get()),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemBombPart> N2_CHARGE =
            Reg.item(
                    "n2_charge",
                    p -> new ItemBombPart(p, () -> ModBlocks.NUKE_N2.get()),
                    () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<Item> MOTOR =
            Reg.item("motor", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> MOTOR_DESH =
            Reg.item("motor_desh", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> MOTOR_BISMUTH =
            Reg.item("motor_bismuth", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CENTRIFUGE_ELEMENT =
            Reg.item("centrifuge_element", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> REACTOR_CORE =
            Reg.item("reactor_core", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> RTG_UNIT =
            Reg.item("rtg_unit", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PIPES_STEEL =
            Reg.item("pipes_steel", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);

    public static final RegistryHandle<Item> CIRCUIT_SILICON =
            Reg.item("circuit_silicon", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CAPACITOR =
            Reg.item("circuit_capacitor", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CAPACITOR_TANTALIUM =
            Reg.item("circuit_capacitor_tantalium", Item::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_PCB =
            Reg.item("circuit_pcb", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CHIP =
            Reg.item("circuit_chip", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_ADVANCED =
            Reg.item("circuit_advanced", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_BASIC =
            Reg.item("circuit_basic", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_VACUUM_TUBE =
            Reg.item("circuit_vacuum_tube", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_ANALOG =
            Reg.item("circuit_analog", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CHIP_BISMOID =
            Reg.item("circuit_chip_bismoid", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);

    public static final RegistryHandle<Item> CIRCUIT_CHIP_QUANTUM =
            Reg.item("circuit_chip_quantum", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_QUANTUM =
            Reg.item("circuit_quantum", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CRYSTAL =
            Reg.item("circuit_crystal", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_BISMOID =
            Reg.item("circuit_bismoid", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);

    public static final RegistryHandle<Item> CIRCUIT_CAPACITOR_BOARD =
            Reg.item("circuit_capacitor_board", Item::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_ATOMIC_CLOCK =
            Reg.item("circuit_atomic_clock", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CONTROLLER =
            Reg.item("circuit_controller", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CONTROLLER_CHASSIS =
            Reg.item("circuit_controller_chassis", Item::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CONTROLLER_ADVANCED =
            Reg.item("circuit_controller_advanced", Item::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_CONTROLLER_QUANTUM =
            Reg.item("circuit_controller_quantum", Item::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CIRCUIT_NUMITRON =
            Reg.item("circuit_numitron", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> DRILL_TITANIUM =
            Reg.item("drill_titanium", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PHOTO_PANEL =
            Reg.item("photo_panel", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> RING_STARMETAL =
            Reg.item("ring_starmetal", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> DEUTERIUM_FILTER =
            Reg.item("deuterium_filter", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final ItemFamily<EnumIngotMetal, Item> INGOT_METAL =
            Reg.family(
                    "ingot_metal",
                    EnumIngotMetal.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final ItemFamily<EnumLegendaryType, Item> PARTS_LEGENDARY =
            Reg.family(
                    "parts_legendary",
                    EnumLegendaryType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final ItemFamily<EnumChemDye, Item> CRAYON =
            Reg.<EnumChemDye, Item>family(
                    "crayon",
                    EnumChemDye.class,
                    (props, type) -> new DyeItem(props.component(DataComponents.DYE, type.dye)),
                    () -> alwaysEdible(3, 0.6F));
    public static final ItemFamily<EnumChemDye, Item> CHEMICAL_DYE =
            Reg.<EnumChemDye, Item>family(
                    "chemical_dye",
                    EnumChemDye.class,
                    (props, type) -> new DyeItem(props.component(DataComponents.DYE, type.dye)),
                    Item.Properties::new);
    public static final ItemFamily<EnumPartType, Item> PART_GENERIC =
            Reg.family(
                    "part_generic",
                    EnumPartType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final ItemFamily<EnumPlantType, Item> PLANT_ITEM =
            Reg.family(
                    "plant_item",
                    EnumPlantType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);

    public static final RegistryHandle<Item> INGOT_CFT =
            Reg.item("ingot_cft", Item::new, Item.Properties::new);
    public static final ItemFamily<EnumExpensiveType, ItemExpensive> ITEM_EXPENSIVE =
            Reg.family(
                    "item_expensive",
                    EnumExpensiveType.class,
                    (props, type) -> new ItemExpensive(props),
                    Item.Properties::new);
    public static final ItemFamily<EnumSecretType, Item> ITEM_SECRET =
            Reg.family(
                    "item_secret",
                    EnumSecretType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final ItemFamily<EnumGearType, Item> GEAR_LARGE =
            Reg.family(
                    EnumGearType.class,
                    type -> type == EnumGearType.LARGE ? "gear_large" : "gear_large_steel",
                    (props, type) -> new Item(props),
                    Item.Properties::new);

    public static final RegistryHandle<Item> SAWBLADE =
            Reg.item("sawblade", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> ENTANGLEMENT_KIT =
            Reg.item("entanglement_kit", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> FINS_FLAT = missileItem("fins_flat");
    public static final RegistryHandle<Item> FINS_SMALL_STEEL = missileItem("fins_small_steel");
    public static final RegistryHandle<Item> FINS_BIG_STEEL = missileItem("fins_big_steel");
    public static final RegistryHandle<Item> FINS_TRI_STEEL = missileItem("fins_tri_steel");
    public static final RegistryHandle<Item> FINS_QUAD_TITANIUM = missileItem("fins_quad_titanium");
    public static final RegistryHandle<Item> SPHERE_STEEL = missileItem("sphere_steel");
    public static final RegistryHandle<Item> PEDESTAL_STEEL = missileItem("pedestal_steel");
    public static final RegistryHandle<Item> DYSFUNCTIONAL_REACTOR =
            missileItem("dysfunctional_reactor");
    public static final ItemFamily<ItemAmmoArty.ArtilleryShellType, ItemAmmoArty> AMMO_ARTY =
            Reg.family(
                            ItemAmmoArty.ArtilleryShellType.class,
                            type -> type.name,
                            ItemAmmoArty::new,
                            Item.Properties::new)
                    .creative(
                            ItemAmmoArty.ArtilleryShellType.NORMAL,
                            ItemAmmoArty.ArtilleryShellType.CLASSIC,
                            ItemAmmoArty.ArtilleryShellType.EXPLOSIVE,
                            ItemAmmoArty.ArtilleryShellType.PHOSPHORUS,
                            ItemAmmoArty.ArtilleryShellType.PHOSPHORUS_MULTI,
                            ItemAmmoArty.ArtilleryShellType.MINI_NUKE,
                            ItemAmmoArty.ArtilleryShellType.MINI_NUKE_MULTI,
                            ItemAmmoArty.ArtilleryShellType.NUKE,
                            ItemAmmoArty.ArtilleryShellType.CARGO,
                            ItemAmmoArty.ArtilleryShellType.CHLORINE,
                            ItemAmmoArty.ArtilleryShellType.PHOSGENE,
                            ItemAmmoArty.ArtilleryShellType.MUSTARD);
    public static final ItemFamily<ItemAmmoHIMARS.HIMARSRocketType, ItemAmmoHIMARS> AMMO_HIMARS =
            Reg.family(
                            ItemAmmoHIMARS.HIMARSRocketType.class,
                            type -> "ammo_himars_" + type.name,
                            ItemAmmoHIMARS::new,
                            () -> new Item.Properties().stacksTo(1))
                    .creative(
                            ItemAmmoHIMARS.HIMARSRocketType.SMALL,
                            ItemAmmoHIMARS.HIMARSRocketType.SMALL_HE,
                            ItemAmmoHIMARS.HIMARSRocketType.SMALL_WP,
                            ItemAmmoHIMARS.HIMARSRocketType.SMALL_TB,
                            ItemAmmoHIMARS.HIMARSRocketType.SMALL_LAVA,
                            ItemAmmoHIMARS.HIMARSRocketType.SMALL_MINI_NUKE,
                            ItemAmmoHIMARS.HIMARSRocketType.LARGE,
                            ItemAmmoHIMARS.HIMARSRocketType.LARGE_TB);
    public static final RegistryHandle<Item> BLADE_TITANIUM =
            Reg.item("blade_titanium", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> BLADE_TUNGSTEN =
            Reg.item("blade_tungsten", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> TURBINE_TITANIUM =
            Reg.item("turbine_titanium", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> TURBINE_TUNGSTEN =
            Reg.item("turbine_tungsten", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> FLYWHEEL_BERYLLIUM =
            Reg.item("flywheel_beryllium", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> DUCTTAPE =
            Reg.item("ducttape", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CATALYST_CLAY =
            Reg.item("catalyst_clay", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> MISSILE_ASSEMBLY = missileItem("missile_assembly");
    public static final RegistryHandle<Item> WARHEAD_GENERIC_SMALL =
            missileItem("warhead_generic_small");
    public static final RegistryHandle<Item> WARHEAD_GENERIC_MEDIUM =
            missileItem("warhead_generic_medium");
    public static final RegistryHandle<Item> WARHEAD_GENERIC_LARGE =
            missileItem("warhead_generic_large");
    public static final RegistryHandle<Item> WARHEAD_INCENDIARY_SMALL =
            missileItem("warhead_incendiary_small");
    public static final RegistryHandle<Item> WARHEAD_INCENDIARY_MEDIUM =
            missileItem("warhead_incendiary_medium");
    public static final RegistryHandle<Item> WARHEAD_INCENDIARY_LARGE =
            missileItem("warhead_incendiary_large");
    public static final RegistryHandle<Item> WARHEAD_CLUSTER_SMALL =
            missileItem("warhead_cluster_small");
    public static final RegistryHandle<Item> WARHEAD_CLUSTER_MEDIUM =
            missileItem("warhead_cluster_medium");
    public static final RegistryHandle<Item> WARHEAD_CLUSTER_LARGE =
            missileItem("warhead_cluster_large");
    public static final RegistryHandle<Item> WARHEAD_BUSTER_SMALL =
            missileItem("warhead_buster_small");
    public static final RegistryHandle<Item> WARHEAD_BUSTER_MEDIUM =
            missileItem("warhead_buster_medium");
    public static final RegistryHandle<Item> WARHEAD_BUSTER_LARGE =
            missileItem("warhead_buster_large");
    public static final RegistryHandle<Item> WARHEAD_NUCLEAR = missileItem("warhead_nuclear");
    public static final RegistryHandle<Item> WARHEAD_MIRV = missileItem("warhead_mirv");
    public static final RegistryHandle<Item> WARHEAD_VOLCANO = missileItem("warhead_volcano");
    public static final RegistryHandle<Item> FUEL_TANK_SMALL = missileItem("fuel_tank_small");
    public static final RegistryHandle<Item> FUEL_TANK_MEDIUM = missileItem("fuel_tank_medium");
    public static final RegistryHandle<Item> FUEL_TANK_LARGE = missileItem("fuel_tank_large");
    public static final RegistryHandle<Item> THRUSTER_SMALL = missileItem("thruster_small");
    public static final RegistryHandle<Item> THRUSTER_MEDIUM = missileItem("thruster_medium");
    public static final RegistryHandle<Item> THRUSTER_LARGE = missileItem("thruster_large");
    public static final RegistryHandle<Item> THRUSTER_NUCLEAR = missileItem("thruster_nuclear");
    public static final RegistryHandle<Item> SEG_10 = missileItem("seg_10");
    public static final RegistryHandle<Item> SEG_15 = missileItem("seg_15");
    public static final RegistryHandle<Item> SEG_20 = missileItem("seg_20");
    public static final RegistryHandle<Item> COMBINE_SCRAP =
            Reg.item("combine_scrap", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> SHIMMER_HEAD = missileItem("shimmer_head");
    public static final RegistryHandle<Item> SHIMMER_AXE_HEAD = missileItem("shimmer_axe_head");
    public static final RegistryHandle<Item> SHIMMER_HANDLE = missileItem("shimmer_handle");
    public static final ItemFamily<Satellite.DriveType, ItemDrive> DRIVE =
            Reg.family("drive", Satellite.DriveType.class, ItemDrive::new, Item.Properties::new)
                    .addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> CRT_DISPLAY =
            Reg.item("crt_display", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final ItemFamily<ScrapType, Item> CIRCUIT_STAR_PIECE =
            Reg.family(
                    "circuit_star_piece",
                    ScrapType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final ItemFamily<CircuitComponentType, Item> CIRCUIT_STAR_COMPONENT =
            Reg.family(
                    "circuit_star_component",
                    CircuitComponentType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final RegistryHandle<ItemCustomLore> CIRCUIT_STAR =
            Reg.item(
                            "circuit_star",
                            ItemCustomLore::new,
                            () -> new Item.Properties().rarity(Rarity.UNCOMMON))
                    .addTo(MISC_ALL);
    public static final ItemFamily<EnumCasingType, Item> CASING =
            Reg.family(
                    "casing",
                    EnumCasingType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final RegistryHandle<Item> ASSEMBLY_NUKE =
            Reg.item("assembly_nuke", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> FLAME_PONY =
            Reg.item("flame_pony", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> FLAME_CONSPIRACY =
            Reg.item("flame_conspiracy", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> FLAME_POLITICS =
            Reg.item("flame_politics", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> FLAME_OPINION =
            Reg.item("flame_opinion", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_RADIUM =
            rtgPellet("pellet_rtg_radium", 3, DepletedRTGMaterial.LEAD, 16.0F, HalfLifeType.LONG)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_WEAK =
            rtgPellet("pellet_rtg_weak", 5, DepletedRTGMaterial.LEAD, 1.0F, HalfLifeType.LONG)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG =
            rtgPellet("pellet_rtg", 10, DepletedRTGMaterial.LEAD, 87.7F, HalfLifeType.MEDIUM)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_STRONTIUM =
            rtgPellet(
                            "pellet_rtg_strontium",
                            15,
                            DepletedRTGMaterial.ZIRCONIUM,
                            29.0F,
                            HalfLifeType.MEDIUM)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_COBALT =
            rtgPellet(
                            "pellet_rtg_cobalt",
                            15,
                            DepletedRTGMaterial.NICKEL,
                            5.3F,
                            HalfLifeType.MEDIUM)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_ACTINIUM =
            rtgPellet(
                            "pellet_rtg_actinium",
                            20,
                            DepletedRTGMaterial.LEAD,
                            21.8F,
                            HalfLifeType.MEDIUM)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_POLONIUM =
            rtgPellet(
                            "pellet_rtg_polonium",
                            50,
                            DepletedRTGMaterial.LEAD,
                            138.0F,
                            HalfLifeType.SHORT)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_AMERICIUM =
            rtgPellet(
                            "pellet_rtg_americium",
                            20,
                            DepletedRTGMaterial.NEPTUNIUM,
                            4.7F,
                            HalfLifeType.LONG)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_GOLD =
            rtgPellet(
                            "pellet_rtg_gold",
                            RTGUtil.RTG_DECAY ? 200 : 100,
                            DepletedRTGMaterial.MERCURY,
                            2.7F,
                            HalfLifeType.SHORT)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemRTGPellet> PELLET_RTG_LEAD =
            rtgPellet(
                            "pellet_rtg_lead",
                            RTGUtil.RTG_DECAY ? 600 : 200,
                            DepletedRTGMaterial.BISMUTH,
                            0.3F,
                            HalfLifeType.SHORT)
                    .addTo(RTG_ALL);
    public static final RegistryHandle<ItemCustomLore> PELLET_CLUSTER =
            Reg.item("pellet_cluster", ItemCustomLore::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> PELLET_BUCKSHOT =
            Reg.item("pellet_buckshot", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> PELLET_CHARGED =
            Reg.item("pellet_charged", ItemCustomLore::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> PELLET_GAS =
            Reg.item("pellet_gas", ItemCustomLore::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> MAGNETRON =
            Reg.item("magnetron", ItemCustomLore::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> PISTON_SELENIUM =
            Reg.item("piston_selenium", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final ItemFamily<ItemPistons.EnumPistonType, ItemPistons> PISTON_SET =
            Reg.family(
                    "piston_set",
                    ItemPistons.EnumPistonType.class,
                    ItemPistons::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final ItemFamily<ItemDrillbit.EnumDrillType, ItemDrillbit> DRILLBIT =
            Reg.family(
                    "drillbit",
                    ItemDrillbit.EnumDrillType.class,
                    ItemDrillbit::new,
                    () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemFluidCell> CELL_EMPTY =
            Reg.item("cell_empty", ItemFluidCell::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemFluidCell> CELL_UF6 =
            Reg.item(
                            "cell_uf6",
                            props -> new ItemFluidCell(props, () -> NTMFluids.UF6, CELL_EMPTY),
                            () -> new Item.Properties().craftRemainder(CELL_EMPTY.get()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemFluidCell> CELL_PUF6 =
            Reg.item(
                            "cell_puf6",
                            props -> new ItemFluidCell(props, () -> NTMFluids.PUF6, CELL_EMPTY),
                            () -> new Item.Properties().craftRemainder(CELL_EMPTY.get()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemFluidCell> CELL_DEUTERIUM =
            Reg.item(
                            "cell_deuterium",
                            props ->
                                    new ItemFluidCell(props, () -> NTMFluids.DEUTERIUM, CELL_EMPTY),
                            () -> new Item.Properties().craftRemainder(CELL_EMPTY.get()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemFluidCell> CELL_TRITIUM =
            Reg.item(
                            "cell_tritium",
                            props -> new ItemFluidCell(props, () -> NTMFluids.TRITIUM, CELL_EMPTY),
                            () -> new Item.Properties().craftRemainder(CELL_EMPTY.get()))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<ItemFluidCell> CELL_SAS3 =
            Reg.item(
                            "cell_sas3",
                            props -> new ItemFluidCell(props, () -> NTMFluids.SAS3, CELL_EMPTY),
                            () ->
                                    new Item.Properties()
                                            .rarity(Rarity.RARE)
                                            .craftRemainder(CELL_EMPTY.get()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemAntimatterCell> CELL_ANTIMATTER =
            Reg.item(
                            "cell_antimatter",
                            ItemAntimatterCell::new,
                            () -> new Item.Properties().craftRemainder(CELL_EMPTY.get()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemAntiSchrabidiumCell> CELL_ANTI_SCHRABIDIUM =
            Reg.item(
                            "cell_anti_schrabidium",
                            ItemAntiSchrabidiumCell::new,
                            () -> new Item.Properties().craftRemainder(CELL_EMPTY.get()))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<Item> CELL_BALEFIRE =
            Reg.item(
                            "cell_balefire",
                            Item::new,
                            () -> new Item.Properties().craftRemainder(CELL_EMPTY.get()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemDemonCore> DEMON_CORE_OPEN =
            Reg.item("demon_core_open", ItemDemonCore::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> DEMON_CORE_CLOSED =
            Reg.item("demon_core_closed", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final ItemFamily<ItemPACoil.EnumCoilType, ItemPACoil> PA_COIL =
            Reg.family(
                    "pa_coil",
                    ItemPACoil.EnumCoilType.class,
                    ItemPACoil::new,
                    () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemFluidCell> PARTICLE_EMPTY =
            Reg.item("particle_empty", ItemFluidCell::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemFluidCell> PARTICLE_HYDROGEN =
            Reg.item(
                            "particle_hydrogen",
                            props ->
                                    new ItemFluidCell(
                                            props, () -> NTMFluids.HYDROGEN, PARTICLE_EMPTY),
                            ModItems::capsuleProperties)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_COPPER =
            Reg.item("particle_copper", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_LEAD =
            Reg.item("particle_lead", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final RegistryHandle<ItemFluidCell> PARTICLE_AMAT =
            Reg.item(
                            "particle_amat",
                            props -> new ItemFluidCell(props, () -> NTMFluids.AMAT, PARTICLE_EMPTY),
                            ModItems::capsuleProperties)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemFluidCell> PARTICLE_ASCHRAB =
            Reg.item(
                            "particle_aschrab",
                            props ->
                                    new ItemFluidCell(
                                            props, () -> NTMFluids.ASCHRAB, PARTICLE_EMPTY),
                            ModItems::capsuleProperties)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_HIGGS =
            Reg.item("particle_higgs", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_MUON =
            Reg.item("particle_muon", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_TACHYON =
            Reg.item("particle_tachyon", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_STRANGE =
            Reg.item("particle_strange", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_DARK =
            Reg.item("particle_dark", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_SPARKTICLE =
            Reg.item("particle_sparkticle", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final RegistryHandle<ItemDigamma> PARTICLE_DIGAMMA =
            Reg.item(
                            "particle_digamma",
                            props -> new ItemDigamma(props, 60),
                            ModItems::capsuleProperties)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> PARTICLE_LUTECE =
            Reg.item("particle_lutece", Item::new, ModItems::capsuleProperties).addTo(MISC_ALL);
    public static final ItemFamily<EnumFuelAdditive, Item> FUEL_ADDITIVE =
            Reg.family(
                            "fuel_additive",
                            EnumFuelAdditive.class,
                            (props, type) -> new Item(props),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> CANISTER_NAPALM =
            Reg.item(
                            "canister_napalm",
                            ItemCustomLore::new,
                            () -> new Item.Properties().craftRemainder(CANISTER.get()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> BATTERY_SPARK =
            Reg.item("battery_spark", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> BATTERY_TRIXITE =
            Reg.item("battery_trixite", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);

    public static final RegistryHandle<ItemFusionCore> SUIT_BATTERY =
            Reg.item(
                            "suit_battery",
                            props -> new ItemFusionCore(props, 150_000L),
                            () -> new Item.Properties().stacksTo(4))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<ItemFusionCore> FUSION_CORE =
            Reg.item(
                            "fusion_core",
                            props -> new ItemFusionCore(props, 2_500_000L),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemBlueprints> BLUEPRINTS =
            Reg.item("blueprints", ItemBlueprints::new, Item.Properties::new)
                    .subtypes(ItemSubtype.BLUEPRINT_POOL)
                    .addTo(MISC_ALL);
    public static final ItemFamily<ItemBlueprintFolder.Kind, ItemBlueprintFolder> BLUEPRINT_FOLDER =
            Reg.family(
                            ItemBlueprintFolder.Kind.class,
                            kind ->
                                    kind == ItemBlueprintFolder.Kind.ALT
                                            ? "blueprint_folder"
                                            : "blueprint_folder_"
                                                    + kind.name().toLowerCase(Locale.ROOT),
                            ItemBlueprintFolder::new,
                            () -> new Item.Properties().stacksTo(1))
                    .creative(ItemBlueprintFolder.Kind.ALT, ItemBlueprintFolder.Kind.DISCOVER);
    public static final RegistryHandle<ItemCatalog> BOBMAZON =
            Reg.item("bobmazon", ItemCatalog::new, () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemCatalog> BOBMAZON_HIDDEN =
            Reg.item("bobmazon_hidden", ItemCatalog::new, () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemKitCustom> KIT_CUSTOM =
            Reg.item("kit_custom", ItemKitCustom::new, () -> new Item.Properties().stacksTo(1));

    public static final ItemFamily<ItemStamp.StampType, ItemStamp> STAMP_BOOK =
            Reg.family(
                    new ItemStamp.StampType[] {
                        ItemStamp.StampType.PRINTING1,
                        ItemStamp.StampType.PRINTING2,
                        ItemStamp.StampType.PRINTING3,
                        ItemStamp.StampType.PRINTING4,
                        ItemStamp.StampType.PRINTING5,
                        ItemStamp.StampType.PRINTING6,
                        ItemStamp.StampType.PRINTING7,
                        ItemStamp.StampType.PRINTING8
                    },
                    type -> "stamp_book_" + type.name().toLowerCase(Locale.ROOT),
                    ItemStamp::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> MOLD_BASE =
            Reg.item("mold_base", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemScraps> SCRAPS =
            Reg.item("scraps", ItemScraps::new, Item.Properties::new)
                    .subtypes(ItemSubtype.SCRAP_MATERIAL);
    public static final RegistryHandle<ItemFluidIcon> FLUID_ICON =
            Reg.item("fluid_icon", ItemFluidIcon::new, () -> new Item.Properties().stacksTo(1))
                    .subtypes(ItemSubtype.FLUID_CONTENT)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> FUSE =
            Reg.item("fuse", ItemCustomLore::new, () -> new Item.Properties().stacksTo(16))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemTooling> SCREWDRIVER =
            Reg.item(
                    "screwdriver",
                    props -> new ItemTooling(props, IToolable.ToolType.SCREWDRIVER),
                    () -> new Item.Properties().durability(100));
    public static final RegistryHandle<ItemTooling> SCREWDRIVER_DESH =
            Reg.item(
                    "screwdriver_desh",
                    props -> new ItemTooling(props, IToolable.ToolType.SCREWDRIVER),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemTooling> HAND_DRILL_DESH =
            Reg.item(
                    "hand_drill_desh",
                    props -> new ItemTooling(props, IToolable.ToolType.HAND_DRILL),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemTooling> HAND_DRILL =
            Reg.item(
                    "hand_drill",
                    props -> new ItemTooling(props, IToolable.ToolType.HAND_DRILL),
                    () -> new Item.Properties().durability(100));

    public static final RegistryHandle<ItemCraftingDegradation> CHEMISTRY_SET =
            Reg.item(
                    "chemistry_set",
                    ItemCraftingDegradation::new,
                    () -> new Item.Properties().durability(100));
    public static final RegistryHandle<ItemCraftingDegradation> CHEMISTRY_SET_BORON =
            Reg.item(
                    "chemistry_set_boron",
                    ItemCraftingDegradation::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final ItemFamily<ItemConveyorWand.ConveyorType, ItemConveyorWand> CONVEYOR_WAND =
            Reg.family(
                    ItemConveyorWand.ConveyorType.class,
                    type -> "conveyor_wand_" + type.id,
                    ItemConveyorWand::new,
                    Item.Properties::new);
    public static final RegistryHandle<ItemBlowtorch> BLOWTORCH =
            Reg.item("blowtorch", ItemBlowtorch::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemBlowtorch> ACETYLENE_TORCH =
            Reg.item(
                            "acetylene_torch",
                            props -> new ItemBlowtorch(props, ItemBlowtorch.Variant.ACETYLENE),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemPWRPrinter> PWR_PRINTER =
            Reg.item("pwr_printer", ItemPWRPrinter::new, Item.Properties::new);
    public static final ItemFamily<ItemGuideBook.BookType, ItemGuideBook> GUIDE_BOOK =
            Reg.family(
                            "book_guide",
                            ItemGuideBook.BookType.class,
                            ItemGuideBook::new,
                            () -> new Item.Properties().stacksTo(1))
                    .creative(ItemGuideBook.BookType.RBMK, ItemGuideBook.BookType.STARTER);
    public static final RegistryHandle<ItemBookLemegeton> BOOK_LEMEGETON =
            Reg.item(
                    "book_lemegeton",
                    ItemBookLemegeton::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemCrucible> CRUCIBLE =
            Reg.item(
                    "crucible",
                    ItemCrucible::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                            new ToolTier(
                                                    net.minecraft.tags.BlockTags
                                                            .INCORRECT_FOR_NETHERITE_TOOL,
                                                    3,
                                                    50,
                                                    0),
                                            5000,
                                            1.0D,
                                            null)
                                    .component(
                                            DataComponents.WEAPON,
                                            new net.minecraft.world.item.component.Weapon(0)));

    public static final RegistryHandle<ItemBoltgun> BOLTGUN =
            Reg.item("boltgun", ItemBoltgun::new, () -> new Item.Properties().stacksTo(1));

    public static final ItemFamily<ItemArcElectrode.EnumElectrodeType, ItemArcElectrode>
            ARC_ELECTRODE =
                    Reg.family(
                                    "arc_electrode",
                                    ItemArcElectrode.EnumElectrodeType.class,
                                    ItemArcElectrode::new,
                                    () -> new Item.Properties().stacksTo(1))
                            .addTo(MISC_ALL);
    public static final ItemFamily<ItemArcElectrode.EnumElectrodeType, ItemArcElectrodeBurnt>
            ARC_ELECTRODE_BURNT =
                    Reg.family(
                                    "arc_electrode_burnt",
                                    ItemArcElectrode.EnumElectrodeType.class,
                                    ItemArcElectrodeBurnt::new,
                                    () -> new Item.Properties().stacksTo(1))
                            .addTo(MISC_ALL);
    public static final RegistryHandle<Item> PART_LITHIUM =
            Reg.item("part_lithium", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PART_BERYLLIUM =
            Reg.item("part_beryllium", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PART_CARBON =
            Reg.item("part_carbon", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PART_COPPER =
            Reg.item("part_copper", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> PART_PLUTONIUM =
            Reg.item("part_plutonium", Item::new, Item.Properties::new).addTo(MISC_ALL);

    public static final RegistryHandle<ItemFELCrystal> LASER_CRYSTAL_CO2 =
            Reg.item(
                            "laser_crystal_co2",
                            props -> new ItemFELCrystal(props, EnumWavelengths.IR),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemFELCrystal> LASER_CRYSTAL_BISMUTH =
            Reg.item(
                            "laser_crystal_bismuth",
                            props -> new ItemFELCrystal(props, EnumWavelengths.VISIBLE),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemFELCrystal> LASER_CRYSTAL_CMB =
            Reg.item(
                            "laser_crystal_cmb",
                            props -> new ItemFELCrystal(props, EnumWavelengths.UV),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemFELCrystal> LASER_CRYSTAL_DNT =
            Reg.item(
                            "laser_crystal_dnt",
                            props -> new ItemFELCrystal(props, EnumWavelengths.GAMMA),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemFELCrystal> LASER_CRYSTAL_DIGAMMA =
            Reg.item(
                            "laser_crystal_digamma",
                            props -> new ItemFELCrystal(props, EnumWavelengths.DRX),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> RUNE_BLANK =
            Reg.item("rune_blank", ItemCustomLore::new, ModItems::runeProperties)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> RUNE_ISA =
            Reg.item("rune_isa", ItemCustomLore::new, ModItems::runeProperties)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> RUNE_DAGAZ =
            Reg.item("rune_dagaz", ItemCustomLore::new, ModItems::runeProperties)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> RUNE_HAGALAZ =
            Reg.item("rune_hagalaz", ItemCustomLore::new, ModItems::runeProperties)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> RUNE_JERA =
            Reg.item("rune_jera", ItemCustomLore::new, ModItems::runeProperties)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> RUNE_THURISAZ =
            Reg.item("rune_thurisaz", ItemCustomLore::new, ModItems::runeProperties)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> AMS_CATALYST_BLANK =
            Reg.item("ams_catalyst_blank", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_ALUMINIUM =
            catalyst("aluminium", 0xCCCCCC);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_BERYLLIUM =
            catalyst("beryllium", 0x97978B);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_CAESIUM =
            catalyst("caesium", 0x6400FF);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_CERIUM =
            catalyst("cerium", 0x1D3FFF);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_COBALT =
            catalyst("cobalt", 0x789BBE);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_COPPER =
            catalyst("copper", 0xAADE29);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_EUPHEMIUM =
            catalyst("euphemium", 0xFF9CD2);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_DINEUTRONIUM =
            catalyst("dineutronium", 0x334077);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_IRON = catalyst("iron", 0xFF7E22);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_LITHIUM =
            catalyst("lithium", 0xFF2727);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_NIOBIUM =
            catalyst("niobium", 0x3BF1B6);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_SCHRABIDIUM =
            catalyst("schrabidium", 0x32FFFF);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_STRONTIUM =
            catalyst("strontium", 0xDD0D35);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_THORIUM =
            catalyst("thorium", 0x653B22);
    public static final RegistryHandle<ItemCatalyst> AMS_CATALYST_TUNGSTEN =
            catalyst("tungsten", 0xF5FF48);
    public static final RegistryHandle<ItemBlades> BLADES_STEEL =
            Reg.item("blades_steel", ItemBlades::new, () -> new Item.Properties().durability(400));
    public static final RegistryHandle<ItemBlades> BLADES_TITANIUM =
            Reg.item(
                    "blades_titanium",
                    ItemBlades::new,
                    () -> new Item.Properties().durability(500));

    public static final RegistryHandle<ItemBlades> BLADES_DESH =
            Reg.item("blades_desh", ItemBlades::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<FluidIdentifierItem> FLUID_IDENTIFIER =
            Reg.item(
                            "fluid_identifier_multi",
                            FluidIdentifierItem::new,
                            () -> new Item.Properties().stacksTo(1))
                    .subtypes(ItemSubtype.FLUID_IDENTIFIER);

    public static final ItemFamily<ItemCassette.TrackType, ItemCassette> SIREN_TRACK =
            Reg.family(
                    Arrays.copyOfRange(
                            ItemCassette.TrackType.VALUES, 1, ItemCassette.TrackType.VALUES.length),
                    type -> "siren_track_" + type.name().toLowerCase(Locale.ROOT),
                    (props, type) -> new ItemCassette(props),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> THERMO_ELEMENT =
            Reg.item("thermo_element", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> CATALYTIC_CONVERTER =
            Reg.item("catalytic_converter", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<ItemLens> AMS_LENS =
            Reg.item("ams_lens", ItemLens::new, () -> new Item.Properties().durability(432_000_000))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemAMSCore> AMS_CORE_SING =
            amsCore("sing", 500, Rarity.UNCOMMON, false);
    public static final RegistryHandle<ItemAMSCore> AMS_CORE_WORMHOLE =
            amsCore("wormhole", 650, Rarity.UNCOMMON, false);
    public static final RegistryHandle<ItemAMSCore> AMS_CORE_EYEOFHARMONY =
            amsCore("eyeofharmony", 800, Rarity.UNCOMMON, false);

    public static final RegistryHandle<ItemAMSCore> AMS_CORE_THINGY =
            amsCore("thingy", 2500, Rarity.EPIC, true);
    public static final RegistryHandle<Item> ROD_EMPTY =
            Reg.item("rod_empty", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final ItemFamily<ItemBreedingRod.BreedingRodType, ItemBreedingRod> ROD =
            breedingRod("rod", ItemBreedingRod.Family.SINGLE, ROD_EMPTY);
    public static final RegistryHandle<Item> ROD_DUAL_EMPTY =
            Reg.item("rod_dual_empty", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final ItemFamily<ItemBreedingRod.BreedingRodType, ItemBreedingRod> ROD_DUAL =
            breedingRod("rod_dual", ItemBreedingRod.Family.DUAL, ROD_DUAL_EMPTY);
    public static final RegistryHandle<Item> ROD_QUAD_EMPTY =
            Reg.item("rod_quad_empty", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final ItemFamily<ItemBreedingRod.BreedingRodType, ItemBreedingRod> ROD_QUAD =
            breedingRod("rod_quad", ItemBreedingRod.Family.QUAD, ROD_QUAD_EMPTY);
    public static final RegistryHandle<Item> ROD_ZIRNOX_EMPTY =
            Reg.item("rod_zirnox_empty", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> ROD_ZIRNOX_TRITIUM =
            Reg.item(
                            "rod_zirnox_tritium",
                            Item::new,
                            () ->
                                    new Item.Properties()
                                            .stacksTo(1)
                                            .craftRemainder(ROD_ZIRNOX_EMPTY.get()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemDepletedFuel> WASTE_NATURAL_URANIUM =
            depletedFuel("waste_natural_uranium");
    public static final RegistryHandle<ItemDepletedFuel> WASTE_URANIUM =
            depletedFuel("waste_uranium");
    public static final RegistryHandle<ItemDepletedFuel> WASTE_THORIUM =
            depletedFuel("waste_thorium");
    public static final RegistryHandle<ItemDepletedFuel> WASTE_MOX = depletedFuel("waste_mox");
    public static final RegistryHandle<ItemDepletedFuel> WASTE_PLUTONIUM =
            depletedFuel("waste_plutonium");
    public static final RegistryHandle<ItemDepletedFuel> WASTE_U233 = depletedFuel("waste_u233");
    public static final RegistryHandle<ItemDepletedFuel> WASTE_U235 = depletedFuel("waste_u235");
    public static final RegistryHandle<ItemDepletedFuel> WASTE_SCHRABIDIUM =
            depletedFuel("waste_schrabidium");
    public static final RegistryHandle<ItemDepletedFuel> WASTE_ZFB_MOX =
            depletedFuel("waste_zfb_mox");

    @Deprecated
    public static final RegistryHandle<ItemDepletedFuel> WASTE_PLATE_U233 =
            depletedFuel("waste_plate_u233");

    @Deprecated
    public static final RegistryHandle<ItemDepletedFuel> WASTE_PLATE_U235 =
            depletedFuel("waste_plate_u235");

    @Deprecated
    public static final RegistryHandle<ItemDepletedFuel> WASTE_PLATE_MOX =
            depletedFuel("waste_plate_mox");

    @Deprecated
    public static final RegistryHandle<ItemDepletedFuel> WASTE_PLATE_PU239 =
            depletedFuel("waste_plate_pu239");

    @Deprecated
    public static final RegistryHandle<ItemDepletedFuel> WASTE_PLATE_RA226BE =
            depletedFuel("waste_plate_ra226be");

    @Deprecated
    public static final RegistryHandle<ItemDepletedFuel> WASTE_PLATE_SA326 =
            depletedFuel("waste_plate_sa326");

    @Deprecated
    public static final RegistryHandle<ItemDepletedFuel> WASTE_PLATE_PU238BE =
            depletedFuel("waste_plate_pu238be");

    @Deprecated
    public static final RegistryHandle<ItemPileRod> PILE_ROD_URANIUM = rod("pile_rod_uranium");

    @Deprecated
    public static final RegistryHandle<ItemPileRod> PILE_ROD_PU239 = rod("pile_rod_pu239");

    @Deprecated
    public static final RegistryHandle<ItemPileRod> PILE_ROD_PLUTONIUM = rod("pile_rod_plutonium");

    @Deprecated
    public static final RegistryHandle<ItemPileRod> PILE_ROD_SOURCE = rod("pile_rod_source");

    @Deprecated
    public static final RegistryHandle<ItemPileRod> PILE_ROD_BORON = rod("pile_rod_boron");

    @Deprecated
    public static final RegistryHandle<ItemPileRod> PILE_ROD_LITHIUM = rod("pile_rod_lithium");

    @Deprecated
    public static final RegistryHandle<ItemPileRod> PILE_ROD_DETECTOR = rod("pile_rod_detector");

    public static final ItemFamily<ItemPileRodMK2.RodType, ItemPileRodMK2> PILE_ROD =
            Reg.family(
                    "pile_rod_mk2",
                    ItemPileRodMK2.RodType.class,
                    ItemPileRodMK2::new,
                    Item.Properties::new);

    @Deprecated
    public static final RegistryHandle<ItemPlateFuel> PLATE_FUEL_U233 =
            plateFuel(
                    "plate_fuel_u233",
                    2_200_000,
                    ItemPlateFuel.FunctionEnum.SQUARE_ROOT,
                    50,
                    WASTE_PLATE_U233);

    @Deprecated
    public static final RegistryHandle<ItemPlateFuel> PLATE_FUEL_U235 =
            plateFuel(
                    "plate_fuel_u235",
                    2_200_000,
                    ItemPlateFuel.FunctionEnum.SQUARE_ROOT,
                    40,
                    WASTE_PLATE_U235);

    @Deprecated
    public static final RegistryHandle<ItemPlateFuel> PLATE_FUEL_MOX =
            plateFuel(
                    "plate_fuel_mox",
                    2_400_000,
                    ItemPlateFuel.FunctionEnum.LOGARITHM,
                    50,
                    WASTE_PLATE_MOX);

    @Deprecated
    public static final RegistryHandle<ItemPlateFuel> PLATE_FUEL_PU239 =
            plateFuel(
                    "plate_fuel_pu239",
                    2_000_000,
                    ItemPlateFuel.FunctionEnum.NEGATIVE_QUADRATIC,
                    50,
                    WASTE_PLATE_PU239);

    @Deprecated
    public static final RegistryHandle<ItemPlateFuel> PLATE_FUEL_SA326 =
            plateFuel(
                    "plate_fuel_sa326",
                    2_000_000,
                    ItemPlateFuel.FunctionEnum.LINEAR,
                    80,
                    WASTE_PLATE_SA326);

    @Deprecated
    public static final RegistryHandle<ItemPlateFuel> PLATE_FUEL_RA226BE =
            plateFuel(
                    "plate_fuel_ra226be",
                    1_300_000,
                    ItemPlateFuel.FunctionEnum.PASSIVE,
                    30,
                    WASTE_PLATE_RA226BE);

    @Deprecated
    public static final RegistryHandle<ItemPlateFuel> PLATE_FUEL_PU238BE =
            plateFuel(
                    "plate_fuel_pu238be",
                    1_000_000,
                    ItemPlateFuel.FunctionEnum.PASSIVE,
                    50,
                    WASTE_PLATE_PU238BE);

    public static final ItemFamily<ItemPWRFuel.EnumPWRFuel, ItemPWRFuelStage> PWR_FUEL_HOT =
            pwrFuelStage("pwr_fuel_hot", true);
    public static final ItemFamily<ItemPWRFuel.EnumPWRFuel, ItemPWRFuelStage> PWR_FUEL_DEPLETED =
            pwrFuelStage("pwr_fuel_depleted", false);
    public static final RegistryHandle<ItemRBMKLid> RBMK_LID =
            Reg.item(
                    "rbmk_lid",
                    props -> new ItemRBMKLid(props, RBMKBase.Lid.CONCRETE),
                    Item.Properties::new);
    public static final RegistryHandle<ItemRBMKLid> RBMK_LID_GLASS =
            Reg.item(
                    "rbmk_lid_glass",
                    props -> new ItemRBMKLid(props, RBMKBase.Lid.GLASS),
                    Item.Properties::new);
    public static final RegistryHandle<Item> RBMK_FUEL_EMPTY =
            Reg.item("rbmk_fuel_empty", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_UEU =
            rbmkFuel(
                    "rbmk_fuel_ueu",
                    "Un-Enriched Uranium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(15)
                                    .setFunction(EnumBurnFunc.LOG_TEN)
                                    .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                                    .setHeat(0.65)
                                    .setMeltingPoint(2865)
                                    .setTint(0x868D82));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_MEU =
            rbmkFuel(
                    "rbmk_fuel_meu",
                    "Medium-Enriched Uranium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(20)
                                    .setFunction(EnumBurnFunc.LOG_TEN)
                                    .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                                    .setHeat(0.65)
                                    .setMeltingPoint(2865)
                                    .setTint(0x868D82));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HEU233 =
            rbmkFuel(
                    "rbmk_fuel_heu233",
                    "High-Enriched Uranium-233",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(27.5D)
                                    .setFunction(EnumBurnFunc.LINEAR)
                                    .setHeat(1.25D)
                                    .setMeltingPoint(2865)
                                    .setTint(0x868D82));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HEU235 =
            rbmkFuel(
                    "rbmk_fuel_heu235",
                    "High-Enriched Uranium-235",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(50)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setMeltingPoint(2865)
                                    .setTint(0x868D82));

    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_UZH =
            rbmkFuel(
                    "rbmk_fuel_uzh",
                    "Uranium-Zirconium Hydride",
                    rod ->
                            rod.setYield(50_000_000D)
                                    .setStats(30)
                                    .setFunction(EnumBurnFunc.LOG_TEN)
                                    .setDepletionFunction(EnumDepleteFunc.GENTLE_SLOPE)
                                    .setHeat(0.75)
                                    .setHeatCoeff(1_000D, 500D)
                                    .setDiffusion(0.1D)
                                    .setMeltingPoint(1845)
                                    .setTint(0x7077AF));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_THMEU =
            rbmkFuel(
                    "rbmk_fuel_thmeu",
                    "Medium-Enriched Thorium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(20)
                                    .setFunction(EnumBurnFunc.PLATEU)
                                    .setDepletionFunction(EnumDepleteFunc.BOOSTED_SLOPE)
                                    .setHeat(0.65D)
                                    .setMeltingPoint(3350)
                                    .setTint(0x665448));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HEP =
            rbmkFuel(
                    "rbmk_fuel_hep",
                    "High-Enriched Plutonium-239",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(30)
                                    .setFunction(EnumBurnFunc.LINEAR)
                                    .setHeat(1.25D)
                                    .setMeltingPoint(2744)
                                    .setTint(0x656E6B));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_LEP =
            rbmkFuel(
                    "rbmk_fuel_lep",
                    "Low-Enriched Plutonium-239",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(35)
                                    .setFunction(EnumBurnFunc.LOG_TEN)
                                    .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                                    .setHeat(0.75D)
                                    .setMeltingPoint(2744)
                                    .setTint(0x656E6B));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_MEP =
            rbmkFuel(
                    "rbmk_fuel_mep",
                    "Medium-Enriched Plutonium-239",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(35)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setMeltingPoint(2744)
                                    .setTint(0x656E6B));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HEP241 =
            rbmkFuel(
                    "rbmk_fuel_hep241",
                    "High-Enriched Plutonium-241",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(40)
                                    .setFunction(EnumBurnFunc.LINEAR)
                                    .setHeat(1.75D)
                                    .setMeltingPoint(2744)
                                    .setTint(0x656E6B));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_LEA =
            rbmkFuel(
                    "rbmk_fuel_lea",
                    "Low-Enriched Americium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(60, 10)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                                    .setHeat(1.5D)
                                    .setMeltingPoint(2386)
                                    .setTint(0xA88A8F));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_MEA =
            rbmkFuel(
                    "rbmk_fuel_mea",
                    "Medium-Enriched Americium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(35D, 20)
                                    .setFunction(EnumBurnFunc.ARCH)
                                    .setHeat(1.75D)
                                    .setMeltingPoint(2386)
                                    .setTint(0xA88A8F));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HEA241 =
            rbmkFuel(
                    "rbmk_fuel_hea241",
                    "High-Enriched Americium-241",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(65, 15)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setHeat(1.85D)
                                    .setMeltingPoint(2386)
                                    .setNeutronTypes(NType.FAST, NType.FAST)
                                    .setTint(0xA88A8F));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HEA242 =
            rbmkFuel(
                    "rbmk_fuel_hea242",
                    "High-Enriched Americium-242",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(45)
                                    .setFunction(EnumBurnFunc.LINEAR)
                                    .setHeat(2D)
                                    .setMeltingPoint(2386)
                                    .setTint(0xA88A8F));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_MEN =
            rbmkFuel(
                    "rbmk_fuel_men",
                    "Medium-Enriched Neptunium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(30)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                                    .setHeat(0.75)
                                    .setMeltingPoint(2800)
                                    .setNeutronTypes(NType.ANY, NType.FAST)
                                    .setTint(0x757E73));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HEN =
            rbmkFuel(
                    "rbmk_fuel_hen",
                    "High-Enriched Neptunium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(40)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setMeltingPoint(2800)
                                    .setNeutronTypes(NType.FAST, NType.FAST)
                                    .setTint(0x757E73));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_MOX =
            rbmkFuel(
                    "rbmk_fuel_mox",
                    "Mixed Oxide Fuel",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(40)
                                    .setFunction(EnumBurnFunc.LOG_TEN)
                                    .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                                    .setMeltingPoint(2815)
                                    .setTint(0x868D82));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_LES =
            rbmkFuel(
                    "rbmk_fuel_les",
                    "Low-Enriched Schrabidium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(50)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setHeat(1.25D)
                                    .setMeltingPoint(2500)
                                    .setNeutronTypes(NType.SLOW, NType.SLOW)
                                    .setTint(0x2D9A94));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_MES =
            rbmkFuel(
                    "rbmk_fuel_mes",
                    "Medium-Enriched Schrabidium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(75D)
                                    .setFunction(EnumBurnFunc.ARCH)
                                    .setHeat(1.5D)
                                    .setMeltingPoint(2750)
                                    .setTint(0x2D9A94));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HES =
            rbmkFuel(
                    "rbmk_fuel_hes",
                    "High-Enriched Schrabidium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(90)
                                    .setFunction(EnumBurnFunc.LINEAR)
                                    .setDepletionFunction(EnumDepleteFunc.LINEAR)
                                    .setHeat(1.75D)
                                    .setMeltingPoint(3000)
                                    .setTint(0x2D9A94));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_LEAUS =
            rbmkFuel(
                    "rbmk_fuel_leaus",
                    "Low-Enriched Australium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(30)
                                    .setFunction(EnumBurnFunc.SIGMOID)
                                    .setDepletionFunction(EnumDepleteFunc.LINEAR)
                                    .setXenon(0.05D, 50D)
                                    .setHeat(1.5D)
                                    .setMeltingPoint(7029)
                                    .setTint(0xFFEE00));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_HEAUS =
            rbmkFuel(
                    "rbmk_fuel_heaus",
                    "High-Enriched Australium",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(35)
                                    .setFunction(EnumBurnFunc.LINEAR)
                                    .setXenon(0.05D, 50D)
                                    .setHeat(1.5D)
                                    .setMeltingPoint(5211)
                                    .setTint(0xFFEE00));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_PO210BE =
            rbmkFuel(
                    "rbmk_fuel_po210be",
                    "Polonium-210 / Beryllium Source",
                    rod ->
                            rod.setYield(25_000_000D)
                                    .setStats(0D, 50)
                                    .setFunction(EnumBurnFunc.PASSIVE)
                                    .setDepletionFunction(EnumDepleteFunc.LINEAR)
                                    .setXenon(0.0D, 50D)
                                    .setHeat(0.1D)
                                    .setDiffusion(0.05D)
                                    .setMeltingPoint(1287)
                                    .setNeutronTypes(NType.SLOW, NType.SLOW)
                                    .setTint(0x563A26));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_RA226BE =
            rbmkFuel(
                    "rbmk_fuel_ra226be",
                    "Radium-226 / Beryllium Source",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(0D, 20)
                                    .setFunction(EnumBurnFunc.PASSIVE)
                                    .setDepletionFunction(EnumDepleteFunc.LINEAR)
                                    .setXenon(0.0D, 50D)
                                    .setHeat(0.035D)
                                    .setDiffusion(0.5D)
                                    .setMeltingPoint(700)
                                    .setNeutronTypes(NType.SLOW, NType.SLOW)
                                    .setTint(0xB3B6AD));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_PU238BE =
            rbmkFuel(
                    "rbmk_fuel_pu238be",
                    "Plutonium-238 / Beryllium Source",
                    rod ->
                            rod.setYield(50_000_000D)
                                    .setStats(40, 40)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setHeat(0.1D)
                                    .setDiffusion(0.05D)
                                    .setMeltingPoint(1287)
                                    .setNeutronTypes(NType.SLOW, NType.SLOW)
                                    .setTint(0x656E6B));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_BALEFIRE_GOLD =
            rbmkFuel(
                    "rbmk_fuel_balefire_gold",
                    "Flashgold Balefire",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(50, 10)
                                    .setFunction(EnumBurnFunc.ARCH)
                                    .setDepletionFunction(EnumDepleteFunc.LINEAR)
                                    .setXenon(0.0D, 50D)
                                    .setMeltingPoint(2000)
                                    .setTint(0xDC9613));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_FLASHLEAD =
            rbmkFuel(
                    "rbmk_fuel_flashlead",
                    "Flashlead",
                    rod ->
                            rod.setYield(250_000_000D)
                                    .setStats(40, 50)
                                    .setFunction(EnumBurnFunc.ARCH)
                                    .setDepletionFunction(EnumDepleteFunc.LINEAR)
                                    .setXenon(0.0D, 50D)
                                    .setMeltingPoint(2050)
                                    .setTint(0x7B7B87));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_BALEFIRE =
            rbmkFuel(
                    "rbmk_fuel_balefire",
                    "Balefire",
                    rod ->
                            rod.setYield(100_000_000D)
                                    .setStats(100, 35)
                                    .setFunction(EnumBurnFunc.LINEAR)
                                    .setXenon(0.0D, 50D)
                                    .setHeat(3D)
                                    .setMeltingPoint(3652)
                                    .setTint(0xB2FF1B));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_ZFB_BISMUTH =
            rbmkFuel(
                    "rbmk_fuel_zfb_bismuth",
                    "Zirconium Fast Breeder (Bismuth)",
                    rod ->
                            rod.setYield(50_000_000D)
                                    .setStats(20)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setHeat(1.75D)
                                    .setMeltingPoint(2744)
                                    .setTint(0xAAA36A));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_ZFB_PU241 =
            rbmkFuel(
                    "rbmk_fuel_zfb_pu241",
                    "Zirconium Fast Breeder (Pu-241)",
                    rod ->
                            rod.setYield(50_000_000D)
                                    .setStats(20)
                                    .setFunction(EnumBurnFunc.SQUARE_ROOT)
                                    .setMeltingPoint(2865)
                                    .setTint(0xAAA36A));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_ZFB_AM_MIX =
            rbmkFuel(
                    "rbmk_fuel_zfb_am_mix",
                    "Zirconium Fast Breeder (Fuel-Grade Americium)",
                    rod ->
                            rod.setYield(50_000_000D)
                                    .setStats(20)
                                    .setFunction(EnumBurnFunc.LINEAR)
                                    .setHeat(1.75D)
                                    .setMeltingPoint(2744)
                                    .setTint(0xAAA36A));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_DRX =
            rbmkFuel(
                    "rbmk_fuel_drx",
                    "Digamma Radiation Experiment",
                    rod ->
                            rod.setYield(10_000_000D)
                                    .setStats(1000, 10)
                                    .setFunction(EnumBurnFunc.QUADRATIC)
                                    .setHeat(0.1D)
                                    .setMeltingPoint(100000)
                                    .setTint(0xD77276));
    public static final RegistryHandle<ItemRBMKRod> RBMK_FUEL_TEST =
            rbmkFuel(
                    "rbmk_fuel_test",
                    "THE VOICES",
                    rod ->
                            rod.setYield(1_000_000D)
                                    .setStats(100)
                                    .setFunction(EnumBurnFunc.EXPERIMENTAL)
                                    .setHeat(1.0D)
                                    .setMeltingPoint(100000));
    public static final ItemFamily<ItemWatzPellet.EnumWatzType, ItemWatzPellet> WATZ_PELLET =
            watzPellet("watz_pellet", false);
    public static final ItemFamily<ItemWatzPellet.EnumWatzType, ItemWatzPellet>
            WATZ_PELLET_DEPLETED = watzPellet("watz_pellet_depleted", true);
    public static final RegistryHandle<Item> ICF_PELLET_EMPTY =
            Reg.item("icf_pellet_empty", Item::new, Item.Properties::new).addTo(MISC_ALL);

    public static final RegistryHandle<ItemICFPellet> ICF_PELLET =
            Reg.item("icf_pellet", ItemICFPellet::new, () -> new Item.Properties().stacksTo(1))
                    .subtypes(ItemSubtype.ICF_FUEL);
    public static final RegistryHandle<Item> ICF_PELLET_DEPLETED =
            Reg.item("icf_pellet_depleted", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> DEBRIS_GRAPHITE =
            Reg.item("debris_graphite", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> DEBRIS_METAL =
            Reg.item("debris_metal", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> DEBRIS_FUEL =
            Reg.item("debris_fuel", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> DEBRIS_CONCRETE =
            Reg.item("debris_concrete", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> DEBRIS_EXCHANGER =
            Reg.item("debris_exchanger", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> DEBRIS_SHRAPNEL =
            Reg.item("debris_shrapnel", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<Item> DEBRIS_ELEMENT =
            Reg.item("debris_element", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> UNDEFINED =
            Reg.item("undefined", ItemCustomLore::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final ItemFamily<ScrapType, Item> SCRAP_PLASTIC =
            Reg.family(
                    "scrap_plastic",
                    ScrapType.class,
                    (props, type) -> new Item(props),
                    Item.Properties::new);
    public static final RegistryHandle<Item> SCRAP =
            Reg.item("scrap", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> SCRAP_OIL =
            Reg.item("scrap_oil", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<Item> SCRAP_NUCLEAR =
            Reg.item("scrap_nuclear", Item::new, Item.Properties::new).addTo(COMPONENT_ALL);
    public static final RegistryHandle<ItemCustomLore> TRINITITE =
            residualLore("trinitite").addTo(RESIDUAL_ALL);
    public static final ItemFamily<ItemWasteLong.WasteClass, ItemWasteLong> NUCLEAR_WASTE_LONG =
            wasteLong("nuclear_waste_long");
    public static final ItemFamily<ItemWasteLong.WasteClass, ItemWasteLong>
            NUCLEAR_WASTE_LONG_TINY = wasteLong("nuclear_waste_long_tiny");
    public static final ItemFamily<ItemWasteShort.WasteClass, ItemWasteShort> NUCLEAR_WASTE_SHORT =
            wasteShort("nuclear_waste_short");
    public static final ItemFamily<ItemWasteShort.WasteClass, ItemWasteShort>
            NUCLEAR_WASTE_SHORT_TINY = wasteShort("nuclear_waste_short_tiny");
    public static final ItemFamily<ItemWasteLong.WasteClass, ItemWasteLong>
            NUCLEAR_WASTE_LONG_DEPLETED = wasteLong("nuclear_waste_long_depleted");
    public static final ItemFamily<ItemWasteLong.WasteClass, ItemWasteLong>
            NUCLEAR_WASTE_LONG_DEPLETED_TINY = wasteLong("nuclear_waste_long_depleted_tiny");
    public static final ItemFamily<ItemWasteShort.WasteClass, ItemWasteShort>
            NUCLEAR_WASTE_SHORT_DEPLETED = wasteShort("nuclear_waste_short_depleted");
    public static final ItemFamily<ItemWasteShort.WasteClass, ItemWasteShort>
            NUCLEAR_WASTE_SHORT_DEPLETED_TINY = wasteShort("nuclear_waste_short_depleted_tiny");
    public static final List<ItemFamily<ItemWasteLong.WasteClass, ItemWasteLong>> WASTE_LONG =
            List.of(
                    NUCLEAR_WASTE_LONG,
                    NUCLEAR_WASTE_LONG_TINY,
                    NUCLEAR_WASTE_LONG_DEPLETED,
                    NUCLEAR_WASTE_LONG_DEPLETED_TINY);
    public static final List<ItemFamily<ItemWasteShort.WasteClass, ItemWasteShort>> WASTE_SHORT =
            List.of(
                    NUCLEAR_WASTE_SHORT,
                    NUCLEAR_WASTE_SHORT_TINY,
                    NUCLEAR_WASTE_SHORT_DEPLETED,
                    NUCLEAR_WASTE_SHORT_DEPLETED_TINY);

    public static final RegistryHandle<ItemNuclearWaste> NUCLEAR_WASTE =
            waste("nuclear_waste").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemNuclearWaste> NUCLEAR_WASTE_TINY =
            waste("nuclear_waste_tiny").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemNuclearWaste> NUCLEAR_WASTE_VITRIFIED =
            waste("nuclear_waste_vitrified").addTo(RESIDUAL_ALL);
    public static final RegistryHandle<ItemNuclearWaste> NUCLEAR_WASTE_VITRIFIED_TINY =
            waste("nuclear_waste_vitrified_tiny").addTo(RESIDUAL_ALL);

    public static final List<RegistryHandle<ItemSpawnEgg>> SPAWN_EGGS =
            List.of(
                    spawnEgg(
                            "entity_mob_nuclear_creeper",
                            () -> ModEntities.CREEPER_NUCLEAR,
                            0x204131,
                            0x75CE00),
                    spawnEgg(
                            "entity_mob_tainted_creeper",
                            () -> ModEntities.CREEPER_TAINTED,
                            0x813b9b,
                            0xd71fdd),
                    spawnEgg(
                            "entity_mob_phosgene_creeper",
                            () -> ModEntities.CREEPER_PHOSGENE,
                            0xE3D398,
                            0xB8A06B),
                    spawnEgg(
                            "entity_mob_volatile_creeper",
                            () -> ModEntities.CREEPER_VOLATILE,
                            0xC28153,
                            0x4D382C),
                    spawnEgg(
                            "entity_mob_gold_creeper",
                            () -> ModEntities.CREEPER_GOLD,
                            0xECC136,
                            0x9E8B3E),
                    spawnEgg(
                            "entity_mob_hunter_chopper",
                            () -> ModEntities.HUNTER_CHOPPER,
                            0x000020,
                            0x2D2D72),
                    spawnEgg("entity_cyber_crab", () -> ModEntities.CYBER_CRAB, 0xAAAAAA, 0x444444),
                    spawnEgg("entity_tesla_crab", () -> ModEntities.TESLA_CRAB, 0xAAAAAA, 0x440000),
                    spawnEgg("entity_taint_crab", () -> ModEntities.TAINT_CRAB, 0xAAAAAA, 0xFF00FF),
                    spawnEgg("entity_mob_mask_man", () -> ModEntities.MASK_MAN, 0x818572, 0xC7C1B7),
                    spawnEgg("entity_fucc_a_ducc", () -> ModEntities.DUCK, 0xd0d0d0, 0xFFBF00),
                    spawnEgg("entity_elder_one", () -> ModEntities.QUACKOS, 0xd0d0d0, 0xFFBF00),
                    spawnEgg("entity_pigeon", () -> ModEntities.PIGEON, 0xC8C9CD, 0x858894),
                    spawnEgg("entity_fbi", () -> ModEntities.FBI, 0x008000, 0x404040),
                    spawnEgg("entity_fbi_drone", () -> ModEntities.FBI_DRONE, 0x008000, 0x404040),
                    spawnEgg(
                            "entity_radiation_blaze",
                            () -> ModEntities.RAD_BEAST,
                            0x303030,
                            0x008000),
                    spawnEgg("entity_glyphid", () -> ModEntities.GLYPHID, 0x724A21, 0xD2BB72),
                    spawnEgg(
                            "entity_glyphid_brawler",
                            () -> ModEntities.GLYPHID_BRAWLER,
                            0x273038,
                            0xD2BB72),
                    spawnEgg(
                            "entity_glyphid_behemoth",
                            () -> ModEntities.GLYPHID_BEHEMOTH,
                            0x267F00,
                            0xD2BB72),
                    spawnEgg(
                            "entity_glyphid_brenda",
                            () -> ModEntities.GLYPHID_BRENDA,
                            0x4FC0C0,
                            0xA0A0A0),
                    spawnEgg(
                            "entity_glyphid_bombardier",
                            () -> ModEntities.GLYPHID_BOMBARDIER,
                            0xDDD919,
                            0xDBB79D),
                    spawnEgg(
                            "entity_glyphid_blaster",
                            () -> ModEntities.GLYPHID_BLASTER,
                            0xD83737,
                            0xDBB79D),
                    spawnEgg(
                            "entity_glyphid_scout",
                            () -> ModEntities.GLYPHID_SCOUT,
                            0x273038,
                            0xB9E36B),
                    spawnEgg(
                            "entity_glyphid_nuclear",
                            () -> ModEntities.GLYPHID_NUCLEAR,
                            0x267F00,
                            0xA0A0A0),
                    spawnEgg(
                            "entity_glyphid_digger",
                            () -> ModEntities.GLYPHID_DIGGER,
                            0x273038,
                            0x724A21),
                    spawnEgg(
                            "entity_plastic_bag",
                            () -> ModEntities.PLASTIC_BAG,
                            0xd0d0d0,
                            0x808080),
                    spawnEgg(
                            "entity_parasite_maggot",
                            () -> ModEntities.PARASITE_MAGGOT,
                            0xd0d0d0,
                            0x808080),
                    spawnEgg("entity_test_dummy", () -> ModEntities.TEST_DUMMY, 0xffffff, 0x000000),
                    spawnEgg(
                            "entity_undead_soldier",
                            () -> ModEntities.UNDEAD_SOLDIER,
                            0x749F30,
                            0x6C5B44));

    public static final RegistryHandle<ItemChopper> SPAWN_CHOPPER =
            Reg.item(
                    "chopper",
                    props -> new ItemChopper(props, () -> ModEntities.HUNTER_CHOPPER.get()),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemChopper> SPAWN_WORM =
            Reg.item(
                    "spawn_worm",
                    props ->
                            new ItemChopper(
                                    props,
                                    () -> ModEntities.BALLS_O_TRON.get(),
                                    0,
                                    mob -> {},
                                    "Without a player in survival mode",
                                    "to target, he struggles around a lot.",
                                    "",
                                    "He's doing his best so please show him",
                                    "some consideration."),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemChopper> SPAWN_UFO =
            Reg.item(
                    "spawn_ufo",
                    props ->
                            new ItemChopper(
                                    props,
                                    () -> ModEntities.UFO.get(),
                                    35,
                                    mob -> ((EntityUFO) mob).scanCooldown = 100),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemChopper> SPAWN_DUCK =
            Reg.item(
                    "spawn_duck",
                    props -> new ItemChopper(props, () -> ModEntities.DUCK.get()),
                    () -> new Item.Properties().stacksTo(16));
    public static final RegistryHandle<ItemRangefinder> RANGEFINDER =
            Reg.item("rangefinder", ItemRangefinder::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemDesignator> DESIGNATOR =
            Reg.item("designator", ItemDesignator::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemDesignatorRange> DESIGNATOR_RANGE =
            Reg.item(
                            "designator_range",
                            ItemDesignatorRange::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemDesignatorManual> DESIGNATOR_MANUAL =
            Reg.item(
                            "designator_manual",
                            ItemDesignatorManual::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemTeleLink> LINKER =
            Reg.item("linker", ItemTeleLink::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemDesignatorArtyRange> DESIGNATOR_ARTY_RANGE =
            Reg.item(
                            "designator_arty_range",
                            ItemDesignatorArtyRange::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemOilDetector> OIL_DETECTOR =
            Reg.item("oil_detector", ItemOilDetector::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemSurveyScanner> SURVEY_SCANNER =
            Reg.item(
                    "survey_scanner",
                    ItemSurveyScanner::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemMirrorTool> MIRROR_TOOL =
            Reg.item(
                    "mirror_tool",
                    ItemMirrorTool::new,
                    () -> new Item.Properties().attributes(weaponModifier(2.0D)));
    public static final RegistryHandle<ItemRBMKTool> RBMK_TOOL =
            Reg.item(
                    "rbmk_tool",
                    ItemRBMKTool::new,
                    () -> new Item.Properties().attributes(weaponModifier(2.0D)));
    public static final RegistryHandle<ItemRadarLinker> RADAR_LINKER =
            Reg.item("radar_linker", ItemRadarLinker::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemSettingsTool> SETTINGS_TOOL =
            Reg.item("settings_tool", ItemSettingsTool::new, Item.Properties::new);
    public static final RegistryHandle<ItemRTTYPager> RTTY_PAGER =
            Reg.item("rtty_pager", ItemRTTYPager::new, () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemTurretChip> TURRET_CHIP =
            Reg.item("turret_chip", ItemTurretChip::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemGeigerCounter> GEIGER_COUNTER =
            Reg.item(
                    "geiger_counter",
                    ItemGeigerCounter::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemReactorSensor> REACTOR_SENSOR =
            Reg.item(
                    "reactor_sensor",
                    ItemReactorSensor::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemOreDensityScanner> ORE_DENSITY_SCANNER =
            Reg.item(
                    "ore_density_scanner",
                    ItemOreDensityScanner::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemDigammaDiagnostic> DIGAMMA_DIAGNOSTIC =
            Reg.item(
                    "digamma_diagnostic",
                    ItemDigammaDiagnostic::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemDosimeter> DOSIMETER =
            Reg.item("dosimeter", ItemDosimeter::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemPollutionDetector> POLLUTION_DETECTOR =
            Reg.item(
                    "pollution_detector",
                    ItemPollutionDetector::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemPlasticBag> PLASTIC_BAG =
            Reg.item("plastic_bag", ItemPlasticBag::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemKey> KEY =
            Reg.item("key", ItemKey::new, () -> new Item.Properties().stacksTo(1)).addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> KEY_RED =
            Reg.item("key_red", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> KEY_RED_CRACKED =
            Reg.item(
                            "key_red_cracked",
                            ItemCustomLore::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCounterfeitKeys> KEY_KIT =
            Reg.item("key_kit", ItemCounterfeitKeys::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemKey> KEY_FAKE =
            Reg.item("key_fake", ItemKey::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> MECH_KEY =
            Reg.item("mech_key", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> PIN =
            Reg.item("pin", ItemCustomLore::new, () -> new Item.Properties().stacksTo(8))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemLock> PADLOCK_RUSTY =
            Reg.item(
                            "padlock_rusty",
                            props -> new ItemLock(props, 1D),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemLock> PADLOCK =
            Reg.item(
                            "padlock",
                            props -> new ItemLock(props, 0.1D),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemLock> PADLOCK_REINFORCED =
            Reg.item(
                            "padlock_reinforced",
                            props -> new ItemLock(props, 0.02D),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemLock> PADLOCK_UNBREAKABLE =
            Reg.item(
                    "padlock_unbreakable",
                    props -> new ItemLock(props, 0D),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> LAUNCH_CODE_PIECE = missileItem("launch_code_piece");
    public static final RegistryHandle<Item> LAUNCH_CODE =
            Reg.item("launch_code", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<Item> LAUNCH_KEY =
            Reg.item("launch_key", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);

    public static final RegistryHandle<ItemMissile> MISSILE_TEST =
            Reg.item(
                    "missile_test",
                    p -> new ItemMissile(p, MissileFormFactor.MICRO, MissileTier.TIER0),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemMissile> MISSILE_TAINT =
            missile("missile_taint", MissileFormFactor.MICRO, MissileTier.TIER0);
    public static final RegistryHandle<ItemMissile> MISSILE_MICRO =
            missile("missile_micro", MissileFormFactor.MICRO, MissileTier.TIER0);
    public static final RegistryHandle<ItemMissile> MISSILE_BHOLE =
            missile("missile_bhole", MissileFormFactor.MICRO, MissileTier.TIER0);
    public static final RegistryHandle<ItemMissile> MISSILE_SCHRABIDIUM =
            missile("missile_schrabidium", MissileFormFactor.MICRO, MissileTier.TIER0);
    public static final RegistryHandle<ItemMissile> MISSILE_EMP =
            missile("missile_emp", MissileFormFactor.MICRO, MissileTier.TIER0);

    public static final RegistryHandle<ItemMissile> MISSILE_GENERIC =
            missile("missile_generic", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryHandle<ItemMissile> MISSILE_DECOY =
            missile("missile_decoy", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryHandle<ItemMissile> MISSILE_INCENDIARY =
            missile("missile_incendiary", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryHandle<ItemMissile> MISSILE_CLUSTER =
            missile("missile_cluster", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryHandle<ItemMissile> MISSILE_BUSTER =
            missile("missile_buster", MissileFormFactor.V2, MissileTier.TIER1);

    public static final RegistryHandle<ItemMissile> MISSILE_STEALTH =
            missile("missile_stealth", MissileFormFactor.STRONG, MissileTier.TIER1);

    public static final RegistryHandle<ItemMissile> MISSILE_ANTI_BALLISTIC =
            missile("missile_anti_ballistic", MissileFormFactor.ABM, MissileTier.TIER1);
    public static final RegistryHandle<ItemMissile> MISSILE_STRONG =
            missile("missile_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryHandle<ItemMissile> MISSILE_INCENDIARY_STRONG =
            missile("missile_incendiary_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryHandle<ItemMissile> MISSILE_CLUSTER_STRONG =
            missile("missile_cluster_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryHandle<ItemMissile> MISSILE_BUSTER_STRONG =
            missile("missile_buster_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryHandle<ItemMissile> MISSILE_EMP_STRONG =
            missile("missile_emp_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryHandle<ItemMissile> MISSILE_BURST =
            missile("missile_burst", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryHandle<ItemMissile> MISSILE_INFERNO =
            missile("missile_inferno", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryHandle<ItemMissile> MISSILE_RAIN =
            missile("missile_rain", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryHandle<ItemMissile> MISSILE_DRILL =
            missile("missile_drill", MissileFormFactor.HUGE, MissileTier.TIER3);

    public static final RegistryHandle<ItemMissile> MISSILE_SHUTTLE =
            missile("missile_shuttle", MissileFormFactor.OTHER, MissileTier.TIER3);
    public static final RegistryHandle<ItemMissile> MISSILE_NUCLEAR =
            missile("missile_nuclear", MissileFormFactor.ATLAS, MissileTier.TIER4);
    public static final RegistryHandle<ItemMissile> MISSILE_NUCLEAR_CLUSTER =
            missile("missile_nuclear_cluster", MissileFormFactor.ATLAS, MissileTier.TIER4);
    public static final RegistryHandle<ItemMissile> MISSILE_VOLCANO =
            missile("missile_volcano", MissileFormFactor.ATLAS, MissileTier.TIER4);

    public static final RegistryHandle<ItemMissile> MISSILE_DOOMSDAY =
            missile("missile_doomsday", MissileFormFactor.ATLAS, MissileTier.TIER4);
    public static final RegistryHandle<ItemMissile> MISSILE_DOOMSDAY_RUSTED =
            Reg.item(
                            "missile_doomsday_rusted",
                            p ->
                                    new ItemMissile(
                                            p,
                                            MissileFormFactor.ATLAS,
                                            MissileTier.TIER4,
                                            ItemMissile.MissileFuel.JETFUEL_LOXY,
                                            false),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);

    public static final RegistryHandle<ItemSoyuz> MISSILE_SOYUZ_0 =
            soyuz(0, "original", Rarity.UNCOMMON);
    public static final RegistryHandle<ItemSoyuz> MISSILE_SOYUZ_1 =
            soyuz(1, "lunaSpaceCenter", Rarity.RARE);
    public static final RegistryHandle<ItemSoyuz> MISSILE_SOYUZ_2 =
            soyuz(2, "postWar", Rarity.EPIC);
    public static final RegistryHandle<ItemCustomLore> MISSILE_SOYUZ_LANDER =
            missileLore("missile_soyuz_lander");

    public static final RegistryHandle<ItemCustomMissile> MISSILE_CUSTOM =
            Reg.item(
                            "missile_custom",
                            ItemCustomMissile::new,
                            () -> new Item.Properties().stacksTo(1))
                    .subtypes(ItemSubtype.MISSILE);

    public static final RegistryHandle<ItemSingularity> BLACK_HOLE =
            singularity("black_hole", EntityBlackHole::new, 1.5F);
    public static final RegistryHandle<ItemSingularity> SINGULARITY =
            singularity("singularity", EntityVortex::new, 1.5F);

    public static final RegistryHandle<ItemSingularity> SINGULARITY_COUNTER_RESONANT =
            singularity("singularity_counter_resonant", EntityVortex::new, 2.5F);
    public static final RegistryHandle<ItemSingularity> SINGULARITY_SUPER_HEATED =
            singularity("singularity_super_heated", EntityVortex::new, 2.5F);
    public static final RegistryHandle<ItemSingularity> SINGULARITY_SPARK =
            singularity("singularity_spark", EntityRagingVortex::new, 3.5F);

    public static final RegistryHandle<ItemAntimatterPellet> PELLET_ANTIMATTER =
            Reg.item(
                    "pellet_antimatter",
                    ItemAntimatterPellet::new,
                    () -> new Item.Properties().craftRemainder(CELL_EMPTY.get()));
    public static final RegistryHandle<ItemXenCrystal> CRYSTAL_XEN =
            Reg.item("crystal_xen", ItemXenCrystal::new, () -> new Item.Properties().stacksTo(1));

    public static final ItemFamily<ItemModMinecart.EnumMinecart, ItemModMinecart> CART =
            Reg.family(
                    ItemModMinecart.EnumMinecart.class,
                    cart -> "cart_" + cart.id,
                    ItemModMinecart::new,
                    () ->
                            new Item.Properties()
                                    .stacksTo(4)
                                    .component(
                                            ModDataComponents.CART_BASE.get(),
                                            ItemModMinecart.EnumCartBase.VANILLA));

    public static final ItemFamily<ItemTrain.EnumTrainType, ItemTrain> TRAIN =
            ModItems.dotted(
                    "train",
                    ItemTrain.EnumTrainType.class,
                    ItemTrain::new,
                    () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemCouplingTool> COUPLING_TOOL =
            Reg.item(
                            "coupling_tool",
                            ItemCouplingTool::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(COLLECTIBLE_ALL);

    public static final RegistryHandle<ItemBoatRubber> BOAT_RUBBER =
            Reg.item("boat_rubber", ItemBoatRubber::new, () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemDroneLinker> DRONE_LINKER =
            Reg.item("drone_linker", ItemDroneLinker::new, () -> new Item.Properties().stacksTo(1));

    public static final ItemFamily<ItemDrone.EnumDroneType, ItemDrone> DRONE =
            ModItems.dotted(
                    "drone", ItemDrone.EnumDroneType.class, ItemDrone::new, Item.Properties::new);

    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_10_KEROSENE =
            missilePart(
                    "mp_thruster_10_kerosene",
                    "mp_thruster",
                    "mp_t_10_kerosene",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_10_kerosene.png",
                    1F,
                    1F,
                    p ->
                            p.makeThruster(FuelType.KEROSENE, 1F, 1.5F, PartSize.SIZE_10)
                                    .setHealth(10F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_10_SOLID =
            missilePart(
                    "mp_thruster_10_solid",
                    "mp_thruster",
                    "mp_t_10_solid",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_10_solid.png",
                    0.5F,
                    1F,
                    p -> p.makeThruster(FuelType.SOLID, 1F, 1.5F, PartSize.SIZE_10).setHealth(15F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_10_XENON =
            missilePart(
                    "mp_thruster_10_xenon",
                    "mp_thruster",
                    "mp_t_10_xenon",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_10_xenon.png",
                    0.5F,
                    1F,
                    p -> p.makeThruster(FuelType.XENON, 1F, 1.5F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_KEROSENE =
            missilePart(
                    "mp_thruster_15_kerosene",
                    "mp_thruster",
                    "mp_t_15_kerosene",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_kerosene.png",
                    1.5F,
                    1.5F,
                    p ->
                            p.makeThruster(FuelType.KEROSENE, 1F, 7.5F, PartSize.SIZE_15)
                                    .setHealth(15F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_KEROSENE_DUAL =
            missilePart(
                    "mp_thruster_15_kerosene_dual",
                    "mp_thruster",
                    "mp_t_15_kerosene_dual",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_kerosene_dual.png",
                    1F,
                    1.5F,
                    p ->
                            p.makeThruster(FuelType.KEROSENE, 1F, 2.5F, PartSize.SIZE_15)
                                    .setHealth(15F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_KEROSENE_TRIPLE =
            missilePart(
                    "mp_thruster_15_kerosene_triple",
                    "mp_thruster",
                    "mp_t_15_kerosene_triple",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_kerosene_dual.png",
                    1F,
                    1.5F,
                    p ->
                            p.makeThruster(FuelType.KEROSENE, 1F, 5F, PartSize.SIZE_15)
                                    .setHealth(15F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_SOLID =
            missilePart(
                    "mp_thruster_15_solid",
                    "mp_thruster",
                    "mp_t_15_solid",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_solid.png",
                    0.5F,
                    1F,
                    p -> p.makeThruster(FuelType.SOLID, 1F, 5F, PartSize.SIZE_15).setHealth(20F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_SOLID_HEXDECUPLE =
            missilePart(
                    "mp_thruster_15_solid_hexdecuple",
                    "mp_thruster",
                    "mp_t_15_solid_hexdecuple",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_solid_hexdecuple.png",
                    0.5F,
                    1F,
                    p ->
                            p.makeThruster(FuelType.SOLID, 1F, 5F, PartSize.SIZE_15)
                                    .setHealth(25F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_HYDROGEN =
            missilePart(
                    "mp_thruster_15_hydrogen",
                    "mp_thruster",
                    "mp_t_15_kerosene",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_hydrogen.png",
                    1.5F,
                    1.5F,
                    p ->
                            p.makeThruster(FuelType.HYDROGEN, 1F, 7.5F, PartSize.SIZE_15)
                                    .setHealth(20F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_HYDROGEN_DUAL =
            missilePart(
                    "mp_thruster_15_hydrogen_dual",
                    "mp_thruster",
                    "mp_t_15_kerosene_dual",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_hydrogen_dual.png",
                    1F,
                    1.5F,
                    p ->
                            p.makeThruster(FuelType.HYDROGEN, 1F, 2.5F, PartSize.SIZE_15)
                                    .setHealth(15F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_BALEFIRE_SHORT =
            missilePart(
                    "mp_thruster_15_balefire_short",
                    "mp_thruster",
                    "mp_t_15_balefire_short",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_balefire_short.png",
                    2F,
                    2F,
                    p ->
                            p.makeThruster(FuelType.BALEFIRE, 1F, 5F, PartSize.SIZE_15)
                                    .setHealth(25F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_BALEFIRE =
            missilePart(
                    "mp_thruster_15_balefire",
                    "mp_thruster",
                    "mp_t_15_balefire",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_balefire.png",
                    3F,
                    2.5F,
                    p ->
                            p.makeThruster(FuelType.BALEFIRE, 1F, 5F, PartSize.SIZE_15)
                                    .setHealth(25F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_BALEFIRE_LARGE =
            missilePart(
                    "mp_thruster_15_balefire_large",
                    "mp_thruster",
                    "mp_t_15_balefire_large",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_balefire_large.png",
                    3F,
                    2.5F,
                    p ->
                            p.makeThruster(FuelType.BALEFIRE, 1F, 7.5F, PartSize.SIZE_15)
                                    .setHealth(35F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_15_BALEFIRE_LARGE_RAD =
            missilePart(
                    "mp_thruster_15_balefire_large_rad",
                    "mp_thruster",
                    "mp_t_15_balefire_large",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_15_balefire_large_rad.png",
                    3F,
                    2.5F,
                    p ->
                            p.makeThruster(FuelType.BALEFIRE, 1F, 7.5F, PartSize.SIZE_15)
                                    .setAuthor("The Master")
                                    .setHealth(35F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_KEROSENE =
            missilePart(
                    "mp_thruster_20_kerosene",
                    "mp_thruster",
                    "mp_t_20_kerosene",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_20_kerosene.png",
                    3F,
                    2.5F,
                    p ->
                            p.makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20)
                                    .setHealth(30F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_KEROSENE_DUAL =
            missilePart(
                    "mp_thruster_20_kerosene_dual",
                    "mp_thruster",
                    "mp_t_20_kerosene_dual",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_20_kerosene_dual.png",
                    2F,
                    2F,
                    p ->
                            p.makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20)
                                    .setHealth(30F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_KEROSENE_TRIPLE =
            missilePart(
                    "mp_thruster_20_kerosene_triple",
                    "mp_thruster",
                    "mp_t_20_kerosene_triple",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_20_kerosene_dual.png",
                    2F,
                    2F,
                    p ->
                            p.makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20)
                                    .setHealth(30F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_SOLID =
            missilePart(
                    "mp_thruster_20_solid",
                    "mp_thruster",
                    "mp_t_20_solid",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_20_solid.png",
                    1F,
                    1.75F,
                    p ->
                            p.makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20)
                                    .setHealth(35F)
                                    .setWittyText(
                                            "It's basically just a big hole at the end of the fuel tank."));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_SOLID_MULTI =
            missilePart(
                    "mp_thruster_20_solid_multi",
                    "mp_thruster",
                    "mp_t_20_solid_multi",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_20_solid_multi.png",
                    0.5F,
                    1.5F,
                    p -> p.makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20).setHealth(35F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_SOLID_MULTIER =
            missilePart(
                    "mp_thruster_20_solid_multier",
                    "mp_thruster",
                    "mp_t_20_solid_multi",
                    "hbm:textures/models/missile_parts/thrusters/mp_t_20_solid_multier.png",
                    0.5F,
                    1.5F,
                    p ->
                            p.makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20)
                                    .setHealth(35F)
                                    .setWittyText("Did I miscount? Hope not."));

    public static final RegistryHandle<ItemCustomMissilePart> MP_STABILITY_10_FLAT =
            missilePart(
                    "mp_stability_10_flat",
                    "mp_stability",
                    "mp_s_10_flat",
                    "hbm:textures/models/missile_parts/stability/mp_s_10_flat.png",
                    0F,
                    2F,
                    p -> p.makeStability(0.5F, PartSize.SIZE_10).setHealth(10F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_STABILITY_10_CRUISE =
            missilePart(
                    "mp_stability_10_cruise",
                    "mp_stability",
                    "mp_s_10_cruise",
                    "hbm:textures/models/missile_parts/stability/mp_s_10_cruise.png",
                    0F,
                    3F,
                    p -> p.makeStability(0.25F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_STABILITY_10_SPACE =
            missilePart(
                    "mp_stability_10_space",
                    "mp_stability",
                    "mp_s_10_space",
                    "hbm:textures/models/missile_parts/stability/mp_s_10_space.png",
                    0F,
                    2F,
                    p ->
                            p.makeStability(0.35F, PartSize.SIZE_10)
                                    .setHealth(5F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setWittyText(
                                            "Standing there alone, the ship is waiting / All systems are go, are you sure?"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_STABILITY_15_FLAT =
            missilePart(
                    "mp_stability_15_flat",
                    "mp_stability",
                    "mp_s_15_flat",
                    "hbm:textures/models/missile_parts/stability/mp_s_15_flat.png",
                    0F,
                    3F,
                    p -> p.makeStability(0.5F, PartSize.SIZE_15).setHealth(10F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_STABILITY_15_THIN =
            missilePart(
                    "mp_stability_15_thin",
                    "mp_stability",
                    "mp_s_15_thin",
                    "hbm:textures/models/missile_parts/stability/mp_s_15_thin.png",
                    0F,
                    3F,
                    p -> p.makeStability(0.35F, PartSize.SIZE_15).setHealth(5F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_STABILITY_15_SOYUZ =
            missilePart(
                    "mp_stability_15_soyuz",
                    "mp_stability",
                    "mp_s_15_soyuz",
                    "hbm:textures/models/missile_parts/stability/mp_s_15_soyuz.png",
                    0F,
                    3F,
                    p ->
                            p.makeStability(0.25F, PartSize.SIZE_15)
                                    .setHealth(15F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setWittyText("Союз!"));

    public static final RegistryHandle<ItemCustomMissilePart> MP_STABILITY_20_FLAT =
            missilePart(
                    "mp_s_20",
                    "mp_stability",
                    "mp_s_20",
                    "hbm:textures/models/thegadget3_.png",
                    0F,
                    3F,
                    p -> p.makeStability(0.5F, PartSize.SIZE_20));

    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE =
            missilePart(
                    "mp_fuselage_10_kerosene",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setAuthor("Hoboy")
                                    .setHealth(20F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE_CAMO =
            missilePart(
                    "mp_fuselage_10_kerosene_camo",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_camo.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE_DESERT =
            missilePart(
                    "mp_fuselage_10_kerosene_desert",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_desert.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Desert Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE_SKY =
            missilePart(
                    "mp_fuselage_10_kerosene_sky",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_sky.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Sky Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE_FLAMES =
            missilePart(
                    "mp_fuselage_10_kerosene_flames",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_flames.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Sick Flames"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE_INSULATION =
            missilePart(
                    "mp_fuselage_10_kerosene_insulation",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_insulation.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(25F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Orange Insulation"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE_SLEEK =
            missilePart(
                    "mp_fuselage_10_kerosene_sleek",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_sleek.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(35F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("IF-R&D"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE_METAL =
            missilePart(
                    "mp_fuselage_10_kerosene_metal",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_metal.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Bolted Metal")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_KEROSENE_TAINT =
            missilePart(
                    "mp_fuselage_10_kerosene_taint",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_kerosene_taint.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Tainted")
                                    .setAuthor("Sam"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID =
            missilePart(
                    "mp_fuselage_10_solid",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(25F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID_FLAMES =
            missilePart(
                    "mp_fuselage_10_solid_flames",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_flames.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(25F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Sick Flames"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID_INSULATION =
            missilePart(
                    "mp_fuselage_10_solid_insulation",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_insulation.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Orange Insulation"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID_SLEEK =
            missilePart(
                    "mp_fuselage_10_solid_sleek",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_sleek.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(35F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("IF-R&D"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID_SOVIET_GLORY =
            missilePart(
                    "mp_fuselage_10_solid_soviet_glory",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_soviet_glory.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(35F)
                                    .setRarity(ItemCustomMissilePart.Rarity.EPIC)
                                    .setTitle("Soviet Glory")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID_CATHEDRAL =
            missilePart(
                    "mp_fuselage_10_solid_cathedral",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_solid_cathedral.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(25F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Unholy Cathedral")
                                    .setAuthor("Satan")
                                    .setWittyText("Quakeesque!"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID_MOONLIT =
            missilePart(
                    "mp_fuselage_10_solid_moonlit",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_solid_moonlit.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(25F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Moonlit")
                                    .setAuthor("The Master & Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID_BATTERY =
            missilePart(
                    "mp_fuselage_10_solid_battery",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_solid_battery.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Ecstatic")
                                    .setAuthor("wolfmonster222")
                                    .setWittyText("I got caught eating batteries again :("));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_SOLID_DURACELL =
            missilePart(
                    "mp_fuselage_10_solid_duracell",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_duracell.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            2500F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Duracell")
                                    .setAuthor("Hoboy")
                                    .setWittyText("The crunchiest battery on the market!"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_XENON =
            missilePart(
                    "mp_fuselage_10_xenon",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_xenon.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.XENON,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(20F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_XENON_BHOLE =
            missilePart(
                    "mp_fuselage_10_xenon_bhole",
                    "mp_fuselage",
                    "mp_f_10_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_xenon_bhole.png",
                    4F,
                    3F,
                    p ->
                            p.makeFuselage(
                                            FuelType.XENON,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Morceus-1457")
                                    .setAuthor("Sten89"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE =
            missilePart(
                    "mp_fuselage_10_long_kerosene",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setAuthor("Hoboy")
                                    .setHealth(30F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_CAMO =
            missilePart(
                    "mp_fuselage_10_long_kerosene_camo",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_camo.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_DESERT =
            missilePart(
                    "mp_fuselage_10_long_kerosene_desert",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_desert.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Desert Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_SKY =
            missilePart(
                    "mp_fuselage_10_long_kerosene_sky",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_sky.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Sky Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_FLAMES =
            missilePart(
                    "mp_fuselage_10_long_kerosene_flames",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_flames.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Sick Flames"));
    public static final RegistryHandle<ItemCustomMissilePart>
            MP_FUSELAGE_10_LONG_KEROSENE_INSULATION =
                    missilePart(
                            "mp_fuselage_10_long_kerosene_insulation",
                            "mp_fuselage",
                            "mp_f_10_long_kerosene",
                            "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_insulation.png",
                            7F,
                            5F,
                            p ->
                                    p.makeFuselage(
                                                    FuelType.KEROSENE,
                                                    5000F,
                                                    PartSize.SIZE_10,
                                                    PartSize.SIZE_10)
                                            .setHealth(35F)
                                            .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                            .setTitle("Orange Insulation"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_SLEEK =
            missilePart(
                    "mp_fuselage_10_long_kerosene_sleek",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_sleek.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(40F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("IF-R&D"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_METAL =
            missilePart(
                    "mp_fuselage_10_long_kerosene_metal",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_metal.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(35F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_DASH =
            missilePartNoTab(
                    "mp_fuselage_10_long_kerosene_dash",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_dash.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.EPIC)
                                    .setTitle("Dash")
                                    .setAuthor("Sam")
                                    .setWittyText("I wash my hands of it."));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_TAINT =
            missilePart(
                    "mp_fuselage_10_long_kerosene_taint",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_taint.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Tainted")
                                    .setAuthor("Sam"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_KEROSENE_VAP =
            missilePart(
                    "mp_fuselage_10_long_kerosene_vap",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_vap.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(30F)
                                    .setRarity(ItemCustomMissilePart.Rarity.EPIC)
                                    .setTitle("Minty Contrail")
                                    .setAuthor("VT-6/24")
                                    .setWittyText("Upper rivet!"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_SOLID =
            missilePart(
                    "mp_fuselage_10_long_solid",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(35F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_SOLID_FLAMES =
            missilePart(
                    "mp_fuselage_10_long_solid_flames",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid_flames.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(35F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Sick Flames"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_SOLID_INSULATION =
            missilePart(
                    "mp_fuselage_10_long_solid_insulation",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid_insulation.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(40F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Orange Insulation"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_SOLID_SLEEK =
            missilePart(
                    "mp_fuselage_10_long_solid_sleek",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid_sleek.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(45F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("IF-R&D"));
    public static final RegistryHandle<ItemCustomMissilePart>
            MP_FUSELAGE_10_LONG_SOLID_SOVIET_GLORY =
                    missilePart(
                            "mp_fuselage_10_long_solid_soviet_glory",
                            "mp_fuselage",
                            "mp_f_10_long_kerosene",
                            "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid_soviet_glory.png",
                            7F,
                            5F,
                            p ->
                                    p.makeFuselage(
                                                    FuelType.SOLID,
                                                    5000F,
                                                    PartSize.SIZE_10,
                                                    PartSize.SIZE_10)
                                            .setHealth(45F)
                                            .setRarity(ItemCustomMissilePart.Rarity.EPIC)
                                            .setTitle("Soviet Glory")
                                            .setAuthor("Hoboy")
                                            .setWittyText(
                                                    "Fully Automated Luxury Gay Space Communism!"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_LONG_SOLID_BULLET =
            missilePart(
                    "mp_fuselage_10_long_solid_bullet",
                    "mp_fuselage",
                    "mp_f_10_long_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_solid_bullet.png",
                    7F,
                    5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            5000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_10)
                                    .setHealth(35F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Bullet Bill")
                                    .setAuthor("Sam"));
    public static final RegistryHandle<ItemCustomMissilePart>
            MP_FUSELAGE_10_LONG_SOLID_SILVERMOONLIGHT =
                    missilePart(
                            "mp_fuselage_10_long_solid_silvermoonlight",
                            "mp_fuselage",
                            "mp_f_10_long_kerosene",
                            "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_solid_silvermoonlight.png",
                            7F,
                            5F,
                            p ->
                                    p.makeFuselage(
                                                    FuelType.SOLID,
                                                    5000F,
                                                    PartSize.SIZE_10,
                                                    PartSize.SIZE_10)
                                            .setHealth(35F)
                                            .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                            .setTitle("Silver Moonlight")
                                            .setAuthor("The Master"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_15_KEROSENE =
            missilePart(
                    "mp_fuselage_10_15_kerosene",
                    "mp_fuselage",
                    "mp_f_10_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_15_kerosene.png",
                    9F,
                    5.5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            10000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_15)
                                    .setHealth(40F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_15_SOLID =
            missilePart(
                    "mp_fuselage_10_15_solid",
                    "mp_fuselage",
                    "mp_f_10_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_15_solid.png",
                    9F,
                    5.5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            10000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_15)
                                    .setHealth(40F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_15_HYDROGEN =
            missilePart(
                    "mp_fuselage_10_15_hydrogen",
                    "mp_fuselage",
                    "mp_f_10_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_15_hydrogen.png",
                    9F,
                    5.5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.HYDROGEN,
                                            10000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_15)
                                    .setHealth(40F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_15_BALEFIRE =
            missilePart(
                    "mp_fuselage_10_15_balefire",
                    "mp_fuselage",
                    "mp_f_10_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_10_15_balefire.png",
                    9F,
                    5.5F,
                    p ->
                            p.makeFuselage(
                                            FuelType.BALEFIRE,
                                            10000F,
                                            PartSize.SIZE_10,
                                            PartSize.SIZE_15)
                                    .setHealth(40F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE =
            missilePart(
                    "mp_fuselage_15_kerosene",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setAuthor("Hoboy")
                                    .setHealth(50F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_CAMO =
            missilePart(
                    "mp_fuselage_15_kerosene_camo",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_camo.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(50F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_DESERT =
            missilePart(
                    "mp_fuselage_15_kerosene_desert",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_desert.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(50F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Desert Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_SKY =
            missilePart(
                    "mp_fuselage_15_kerosene_sky",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_sky.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(50F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Sky Camo"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_INSULATION =
            missilePart(
                    "mp_fuselage_15_kerosene_insulation",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_insulation.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(55F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Orange Insulation")
                                    .setWittyText("Rest in spaghetti Columbia :("));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_METAL =
            missilePart(
                    "mp_fuselage_15_kerosene_metal",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_metal.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Bolted Metal")
                                    .setAuthor("Hoboy")
                                    .setWittyText(
                                            "Metal frame with metal plating reinforced with bolted metal sheets and metal."));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_DECORATED =
            missilePart(
                    "mp_fuselage_15_kerosene_decorated",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_decorated.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Decorated")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_STEAMPUNK =
            missilePart(
                    "mp_fuselage_15_kerosene_steampunk",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_steampunk.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Steampunk")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_POLITE =
            missilePart(
                    "mp_fuselage_15_kerosene_polite",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_polite.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.LEGENDARY)
                                    .setTitle("Polite")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_BLACKJACK =
            missilePart(
                    "mp_fuselage_15_kerosene_blackjack",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/base/mp_f_15_kerosene_blackjack.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(100F)
                                    .setRarity(ItemCustomMissilePart.Rarity.LEGENDARY)
                                    .setTitle("Queen Whiskey"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_LAMBDA =
            missilePart(
                    "mp_fuselage_15_kerosene_lambda",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_lambda.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(75F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Lambda Complex")
                                    .setAuthor("VT-6/24")
                                    .setWittyText("MAGNIFICENT MICROWAVE CASSEROLE"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_MINUTEMAN =
            missilePart(
                    "mp_fuselage_15_kerosene_minuteman",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_minuteman.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(50F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("MX 1702")
                                    .setAuthor("Spexta"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_PIP =
            missilePartNoTab(
                    "mp_fuselage_15_kerosene_pip",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_pip.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(50F)
                                    .setRarity(ItemCustomMissilePart.Rarity.EPIC)
                                    .setTitle("LittlePip")
                                    .setAuthor("The Doctor")
                                    .setWittyText("31!"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_TAINT =
            missilePart(
                    "mp_fuselage_15_kerosene_taint",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_taint.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(50F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Tainted")
                                    .setAuthor("Sam")
                                    .setWittyText("DUN-DUN!"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_KEROSENE_YUCK =
            missilePart(
                    "mp_fuselage_15_kerosene_yuck",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_yuck.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.EPIC)
                                    .setTitle("Flesh")
                                    .setAuthor("Hoboy")
                                    .setWittyText(
                                            "Note: Never clean DNA vials with your own spit."));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID =
            missilePart(
                    "mp_fuselage_15_solid",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_INSULATION =
            missilePart(
                    "mp_fuselage_15_solid_insulation",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_insulation.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(65F)
                                    .setRarity(ItemCustomMissilePart.Rarity.COMMON)
                                    .setTitle("Orange Insulation"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_DESH =
            missilePart(
                    "mp_fuselage_15_solid_desh",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_desh.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(80F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Desh Plating")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_SOVIET_GLORY =
            missilePart(
                    "mp_fuselage_15_solid_soviet_glory",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_soviet_glory.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(70F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Soviet Glory")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_SOVIET_STANK =
            missilePart(
                    "mp_fuselage_15_solid_soviet_stank",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_soviet_stank.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(15F)
                                    .setRarity(ItemCustomMissilePart.Rarity.EPIC)
                                    .setTitle("Soviet Stank")
                                    .setAuthor("Hoboy")
                                    .setWittyText("Aged like a fine wine! Well, almost."));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_FAUST =
            missilePart(
                    "mp_fuselage_15_solid_faust",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_solid_faust.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(250F)
                                    .setRarity(ItemCustomMissilePart.Rarity.LEGENDARY)
                                    .setTitle("Mighty Lauren")
                                    .setAuthor("Dr.Nostalgia")
                                    .setWittyText("Welcome to Subway, may I take your order?"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_SILVERMOONLIGHT =
            missilePart(
                    "mp_fuselage_15_solid_silvermoonlight",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_solid_silvermoonlight.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Silver Moonlight")
                                    .setAuthor("The Master"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_SNOWY =
            missilePart(
                    "mp_fuselage_15_solid_snowy",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_solid_snowy.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Chilly Day")
                                    .setAuthor("Dr.Nostalgia"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_PANORAMA =
            missilePart(
                    "mp_fuselage_15_solid_panorama",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_panorama.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Panorama")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_ROSES =
            missilePart(
                    "mp_fuselage_15_solid_roses",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_roses.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Bed of roses")
                                    .setAuthor("Hoboy"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_SOLID_MIMI =
            missilePart(
                    "mp_fuselage_15_solid_mimi",
                    "mp_fuselage",
                    "mp_f_15_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_mimi.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(60F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("Mimi-chan"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_HYDROGEN =
            missilePart(
                    "mp_fuselage_15_hydrogen",
                    "mp_fuselage",
                    "mp_f_15_hydrogen",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_hydrogen.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.HYDROGEN,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(50F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_HYDROGEN_CATHEDRAL =
            missilePart(
                    "mp_fuselage_15_hydrogen_cathedral",
                    "mp_fuselage",
                    "mp_f_15_hydrogen",
                    "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_hydrogen_cathedral.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.HYDROGEN,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(50F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Unholy Cathedral")
                                    .setAuthor("Satan"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_BALEFIRE =
            missilePart(
                    "mp_fuselage_15_balefire",
                    "mp_fuselage",
                    "mp_f_15_hydrogen",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_balefire.png",
                    10F,
                    6F,
                    p ->
                            p.makeFuselage(
                                            FuelType.BALEFIRE,
                                            15000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_15)
                                    .setHealth(75F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_20_KEROSENE =
            missilePart(
                    "mp_fuselage_15_20_kerosene",
                    "mp_fuselage",
                    "mp_f_15_20_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_20_kerosene.png",
                    16F,
                    10F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            20000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_20)
                                    .setAuthor("Hoboy")
                                    .setHealth(70F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_20_KEROSENE_MAGNUSSON =
            missilePart(
                    "mp_fuselage_15_20_kerosene_magnusson",
                    "mp_fuselage",
                    "mp_f_15_20_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_20_kerosene_magnusson.png",
                    16F,
                    10F,
                    p ->
                            p.makeFuselage(
                                            FuelType.KEROSENE,
                                            20000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_20)
                                    .setHealth(70F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("White Forest Rocket")
                                    .setAuthor("VT-6/24")
                                    .setWittyText(
                                            "And get your cranio-conjugal parasite away from my nose cone!"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_15_20_SOLID =
            missilePart(
                    "mp_fuselage_15_20_solid",
                    "mp_fuselage",
                    "mp_f_15_20_kerosene",
                    "hbm:textures/models/missile_parts/fuselages/mp_f_15_20_solid.png",
                    16F,
                    10F,
                    p ->
                            p.makeFuselage(
                                            FuelType.SOLID,
                                            20000F,
                                            PartSize.SIZE_15,
                                            PartSize.SIZE_20)
                                    .setHealth(70F));

    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_10_HE =
            missilePart(
                    "mp_warhead_10_he",
                    "mp_warhead",
                    "mp_w_10_he",
                    "hbm:textures/models/missile_parts/warheads/mp_w_10_he.png",
                    2F,
                    1.5F,
                    p -> p.makeWarhead(WarheadType.HE, 15F, 1.5F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_10_INCENDIARY =
            missilePart(
                    "mp_warhead_10_incendiary",
                    "mp_warhead",
                    "mp_w_10_incendiary",
                    "hbm:textures/models/missile_parts/warheads/mp_w_10_incendiary.png",
                    2.5F,
                    2F,
                    p -> p.makeWarhead(WarheadType.INC, 15F, 1.5F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_10_BUSTER =
            missilePart(
                    "mp_warhead_10_buster",
                    "mp_warhead",
                    "mp_w_10_buster",
                    "hbm:textures/models/missile_parts/warheads/mp_w_10_buster.png",
                    0.5F,
                    1F,
                    p ->
                            p.makeWarhead(WarheadType.BUSTER, 5F, 1.5F, PartSize.SIZE_10)
                                    .setHealth(5F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_10_NUCLEAR =
            missilePart(
                    "mp_warhead_10_nuclear",
                    "mp_warhead",
                    "mp_w_10_nuclear",
                    "hbm:textures/models/missile_parts/warheads/mp_w_10_nuclear.png",
                    2F,
                    1.5F,
                    p ->
                            p.makeWarhead(WarheadType.NUCLEAR, 35F, 1.5F, PartSize.SIZE_10)
                                    .setTitle("Tater Tot")
                                    .setHealth(10F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_10_NUCLEAR_LARGE =
            missilePart(
                    "mp_warhead_10_nuclear_large",
                    "mp_warhead",
                    "mp_w_10_nuclear_large",
                    "hbm:textures/models/missile_parts/warheads/mp_w_10_nuclear_large.png",
                    2.5F,
                    1.5F,
                    p ->
                            p.makeWarhead(WarheadType.NUCLEAR, 75F, 2.5F, PartSize.SIZE_10)
                                    .setTitle("Chernobyl Boris")
                                    .setHealth(15F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_10_TAINT =
            missilePart(
                    "mp_warhead_10_taint",
                    "mp_warhead",
                    "mp_w_10_taint",
                    "hbm:textures/models/missile_parts/warheads/mp_w_10_taint.png",
                    2.25F,
                    1.5F,
                    p ->
                            p.makeWarhead(WarheadType.TAINT, 15F, 1.5F, PartSize.SIZE_10)
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setWittyText(
                                            "Eat my taint! Bureaucracy is dead and we killed it!"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_10_CLOUD =
            missilePart(
                    "mp_warhead_10_cloud",
                    "mp_warhead",
                    "mp_w_10_taint",
                    "hbm:textures/models/missile_parts/warheads/mp_w_10_cloud.png",
                    2.25F,
                    1.5F,
                    p ->
                            p.makeWarhead(WarheadType.CLOUD, 15F, 1.5F, PartSize.SIZE_10)
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_HE =
            missilePart(
                    "mp_warhead_15_he",
                    "mp_warhead",
                    "mp_w_15_he",
                    "hbm:textures/models/missile_parts/warheads/mp_w_15_he.png",
                    2F,
                    1.5F,
                    p -> p.makeWarhead(WarheadType.HE, 50F, 2.5F, PartSize.SIZE_15).setHealth(10F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_INCENDIARY =
            missilePart(
                    "mp_warhead_15_incendiary",
                    "mp_warhead",
                    "mp_w_15_incendiary",
                    "hbm:textures/models/missile_parts/warheads/mp_w_15_incendiary.png",
                    2F,
                    1.5F,
                    p ->
                            p.makeWarhead(WarheadType.INC, 35F, 2.5F, PartSize.SIZE_15)
                                    .setHealth(10F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_NUCLEAR =
            missilePart(
                    "mp_warhead_15_nuclear",
                    "mp_warhead",
                    "mp_w_15_nuclear",
                    "hbm:textures/models/missile_parts/warheads/mp_w_15_nuclear.png",
                    3.5F,
                    2F,
                    p ->
                            p.makeWarhead(WarheadType.NUCLEAR, 125F, 5F, PartSize.SIZE_15)
                                    .setTitle("Auntie Bertha")
                                    .setHealth(15F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_NUCLEAR_SHARK =
            missilePart(
                    "mp_warhead_15_nuclear_shark",
                    "mp_warhead",
                    "mp_w_15_nuclear",
                    "hbm:textures/models/missile_parts/warheads/mp_w_15_nuclear_shark.png",
                    3.5F,
                    2F,
                    p ->
                            p.makeWarhead(WarheadType.NUCLEAR, 125F, 5F, PartSize.SIZE_15)
                                    .setHealth(15F)
                                    .setRarity(ItemCustomMissilePart.Rarity.UNCOMMON)
                                    .setTitle("Discount Bullet Bill")
                                    .setWittyText("Nose art on a cannon bullet? Who does that?"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_NUCLEAR_MIMI =
            missilePart(
                    "mp_warhead_15_nuclear_mimi",
                    "mp_warhead",
                    "mp_w_15_nuclear",
                    "hbm:textures/models/missile_parts/warheads/mp_w_15_nuclear_mimi.png",
                    3.5F,
                    2F,
                    p ->
                            p.makeWarhead(WarheadType.NUCLEAR, 125F, 5F, PartSize.SIZE_15)
                                    .setHealth(15F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE)
                                    .setTitle("FASHIONABLE MISSILE"));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_BOXCAR =
            missilePart(
                    "mp_warhead_15_boxcar",
                    "mp_warhead",
                    "mp_w_15_boxcar",
                    "hbm:textures/block/models/boxcar.png",
                    2.25F,
                    7.5F,
                    p ->
                            p.makeWarhead(WarheadType.TX, 250F, 7.5F, PartSize.SIZE_15)
                                    .setWittyText("?!?!")
                                    .setHealth(35F)
                                    .setRarity(ItemCustomMissilePart.Rarity.LEGENDARY));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_N2 =
            missilePart(
                    "mp_warhead_15_n2",
                    "mp_warhead",
                    "mp_w_15_n2",
                    "hbm:textures/models/missile_parts/warheads/mp_w_15_n2.png",
                    3F,
                    2F,
                    p ->
                            p.makeWarhead(WarheadType.N2, 100F, 5F, PartSize.SIZE_15)
                                    .setWittyText("[screams geometrically]")
                                    .setHealth(20F)
                                    .setRarity(ItemCustomMissilePart.Rarity.RARE));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_BALEFIRE =
            missilePart(
                    "mp_warhead_15_balefire",
                    "mp_warhead",
                    "mp_w_15_balefire",
                    "hbm:textures/models/missile_parts/warheads/mp_w_15_balefire.png",
                    2.75F,
                    2F,
                    p ->
                            p.makeWarhead(WarheadType.BALEFIRE, 100F, 7.5F, PartSize.SIZE_15)
                                    .setRarity(ItemCustomMissilePart.Rarity.LEGENDARY)
                                    .setAuthor("VT-6/24")
                                    .setHealth(15F)
                                    .setWittyText("Hightower, never forgetti."));
    public static final RegistryHandle<ItemCustomMissilePart> MP_WARHEAD_15_TURBINE =
            missilePart(
                    "mp_warhead_15_turbine",
                    "mp_warhead",
                    "mp_w_15_turbine",
                    "hbm:textures/models/missile_parts/warheads/mp_w_15_turbine.png",
                    2.25F,
                    2F,
                    p ->
                            p.makeWarhead(WarheadType.TURBINE, 200F, 5F, PartSize.SIZE_15)
                                    .setRarity(ItemCustomMissilePart.Rarity.STRANGE)
                                    .setHealth(250F));

    public static final RegistryHandle<ItemCustomMissilePart> MP_CHIP_1 =
            missilePart("mp_c_1", "mp_c_1", p -> p.makeChip(0.1F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_CHIP_2 =
            missilePart("mp_c_2", "mp_c_2", p -> p.makeChip(0.05F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_CHIP_3 =
            missilePart("mp_c_3", "mp_c_3", p -> p.makeChip(0.01F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_CHIP_4 =
            missilePart("mp_c_4", "mp_c_4", p -> p.makeChip(0.005F));
    public static final RegistryHandle<ItemCustomMissilePart> MP_CHIP_5 =
            missilePart("mp_c_5", "mp_c_5", p -> p.makeChip(0.0F));
    public static final RegistryHandle<ItemSatellite> SATELLITE_SPY =
            Reg.item("satellite_spy", ItemSatellite::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_SCANNER =
            Reg.item(
                            "satellite_scanner",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_RADAR =
            Reg.item("satellite_radar", ItemSatellite::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_MINER_ASTRO =
            Reg.item(
                            "satellite_miner_astro",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_MINER_LUNAR =
            Reg.item(
                            "satellite_miner_lunar",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_PRECISION_LASER =
            Reg.item(
                            "satellite_precision_laser",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_DEATH_RAY =
            Reg.item(
                            "satellite_death_ray",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_XENIUM_RESONATOR =
            Reg.item(
                            "satellite_xenium_resonator",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_RELAY =
            Reg.item("satellite_relay", ItemSatellite::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_DETECTOR =
            Reg.item(
                            "satellite_detector",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_RAY_SCAN =
            Reg.item(
                            "satellite_ray_scan",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_SCIENCE =
            Reg.item(
                            "satellite_science",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_SCIENCE_ASSEMBLER =
            Reg.item(
                            "satellite_science_assembler",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatellite> SATELLITE_SCIENCE_SENSOR =
            Reg.item(
                            "satellite_science_sensor",
                            ItemSatellite::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemOrbitalAssembly> ORBITAL_ASSEMBLY =
            Reg.item(
                    "orbital_assembly",
                    ItemOrbitalAssembly::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemSatelliteChip> SAT_MAPPER =
            satChipLegacy("sat_mapper", "satchip.mapper");
    public static final RegistryHandle<ItemSatelliteChip> SAT_SCANNER =
            satChipLegacy("sat_scanner", "satchip.scanner");
    public static final RegistryHandle<ItemSatelliteChip> SAT_RADAR =
            satChipLegacy("sat_radar", "satchip.radar");
    public static final RegistryHandle<ItemSatelliteChip> SAT_LASER =
            satChipLegacy("sat_laser", "satchip.laser");
    public static final RegistryHandle<ItemSatelliteChip> SAT_FOEQ =
            satChipLegacy("sat_foeq", "satchip.foeq");
    public static final RegistryHandle<ItemSatelliteChip> SAT_RESONATOR =
            satChipLegacy("sat_resonator", "satchip.resonator");
    public static final RegistryHandle<ItemSatelliteChip> SAT_MINER =
            satChipLegacy("sat_miner", "satchip.miner");
    public static final RegistryHandle<ItemSatelliteChip> SAT_LUNAR_MINER =
            satChipLegacy("sat_lunar_miner", "satchip.lunar_miner");
    public static final RegistryHandle<ItemSatelliteChip> SAT_GERALD =
            satChip("sat_gerald", "satchip.gerald.desc");
    public static final RegistryHandle<ItemSatelliteChip> SAT_CHIP = satChip("sat_chip", null);
    public static final RegistryHandle<ItemSatelliteInterface> SAT_COORD =
            Reg.item(
                            "sat_coord",
                            ItemSatelliteInterface::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);

    public static final RegistryHandle<ItemSatelliteDesignator> SAT_DESIGNATOR =
            Reg.item(
                            "sat_designator",
                            ItemSatelliteDesignator::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemSatelliteChip> SAT_RELAY = satChip("sat_relay", null);
    public static final RegistryHandle<ItemCustomLore> AMMO_DGK =
            Reg.item("ammo_dgk", ItemCustomLore::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemAmmoContainer> AMMO_CONTAINER =
            Reg.item(
                    "ammo_container",
                    props -> new ItemAmmoContainer(props, false),
                    Item.Properties::new);

    public static final RegistryHandle<ItemAmmoContainer> AMMO_CONTAINER_ALT =
            Reg.item(
                    "ammo_container_alt",
                    props -> new ItemAmmoContainer(props, true),
                    Item.Properties::new);

    public static final RegistryHandle<ItemGrenadeDynamite> STICK_DYNAMITE =
            Reg.item(
                            "stick_dynamite",
                            p -> new ItemGrenadeDynamite(p, 3),
                            () -> new Item.Properties().stacksTo(16))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemGrenadeFishing> STICK_DYNAMITE_FISHING =
            Reg.item(
                            "stick_dynamite_fishing",
                            p -> new ItemGrenadeFishing(p, 3),
                            () -> new Item.Properties().stacksTo(16))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> STICK_TNT =
            Reg.item("stick_tnt", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> STICK_SEMTEX =
            Reg.item("stick_semtex", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> STICK_C4 =
            Reg.item("stick_c4", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final ItemFamily<ItemGrenadeShell.EnumGrenadeShell, ItemGrenadeShell>
            GRENADE_SHELL =
                    dotted(
                            "grenade_shell",
                            ItemGrenadeShell.EnumGrenadeShell.class,
                            ItemGrenadeShell::new,
                            Item.Properties::new);
    public static final ItemFamily<ItemGrenadeFilling.EnumGrenadeFilling, ItemGrenadeFilling>
            GRENADE_FILLING =
                    dotted(
                            "grenade_filling",
                            ItemGrenadeFilling.EnumGrenadeFilling.class,
                            ItemGrenadeFilling::new,
                            Item.Properties::new);
    public static final ItemFamily<ItemGrenadeFuze.EnumGrenadeFuze, ItemGrenadeFuze> GRENADE_FUZE =
            dotted(
                    "grenade_fuze",
                    ItemGrenadeFuze.EnumGrenadeFuze.class,
                    ItemGrenadeFuze::new,
                    Item.Properties::new);
    public static final ItemFamily<ItemGrenadeExtra.EnumGrenadeExtra, ItemGrenadeExtra>
            GRENADE_EXTRA =
                    dotted(
                            "grenade_extra",
                            ItemGrenadeExtra.EnumGrenadeExtra.class,
                            ItemGrenadeExtra::new,
                            Item.Properties::new);
    public static final RegistryHandle<ItemGrenadeUniversal> GRENADE_UNIVERSAL =
            Reg.item(
                            "grenade_universal",
                            ItemGrenadeUniversal::new,
                            () -> new Item.Properties().stacksTo(4))
                    .subtypes(ItemSubtype.GRENADE)
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> DISPERSER_CANISTER_EMPTY =
            Reg.item("disperser_canister_empty", Item::new, Item.Properties::new).addTo(WEAPON_ALL);

    public static final RegistryHandle<ItemDisperser> DISPERSER_CANISTER =
            Reg.item(
                            "disperser_canister",
                            p ->
                                    new ItemDisperser(
                                            p,
                                            2000,
                                            "disperser_canister",
                                            ItemDisperser.Kind.CANISTER,
                                            Library.id("disperser_canister_empty"),
                                            Library.id("disperser_canister_overlay")),
                            () ->
                                    new Item.Properties()
                                            .craftRemainder(DISPERSER_CANISTER_EMPTY.get()))
                    .subtypes(ItemSubtype.FLUID_CONTENT);
    public static final RegistryHandle<Item> GLYPHID_GLAND_EMPTY =
            Reg.item("glyphid_gland_empty", Item::new, Item.Properties::new).addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemDisperser> GLYPHID_GLAND =
            Reg.item(
                            "glyphid_gland",
                            p ->
                                    new ItemDisperser(
                                            p,
                                            4000,
                                            "glyphid_gland",
                                            ItemDisperser.Kind.GLAND,
                                            Library.id("glyphid_gland_empty"),
                                            Library.id("fluid_identifier_overlay")),
                            () -> new Item.Properties().craftRemainder(GLYPHID_GLAND_EMPTY.get()))
                    .subtypes(ItemSubtype.FLUID_CONTENT);

    public static final RegistryHandle<ItemWeaponSpecial> ULLAPOOL_CABER =
            Reg.item(
                    "ullapool_caber",
                    properties -> new ItemWeaponSpecial(properties, ItemWeaponSpecial.Effect.CABER),
                    () ->
                            ItemSwordAbility.swordProperties(
                                            ToolTier.STEEL,
                                            6.0F,
                                            0,
                                            MaterialShapes.INGOT.tagFor("steel"))
                                    .stacksTo(1)
                                    .rarity(Rarity.UNCOMMON));
    public static final RegistryHandle<GunB92> GUN_B92 =
            Reg.item(
                    "gun_b92",
                    GunB92::new,
                    () ->
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.UNCOMMON)
                                    .attributes(weaponModifier(3.5D)));
    public static final RegistryHandle<GunB92Cell> GUN_B92_AMMO =
            Reg.item("gun_b92_ammo", GunB92Cell::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<WeaponizedCell> WEAPONIZED_STARBLASTER_CELL =
            Reg.item(
                    "weaponized_starblaster_cell",
                    WeaponizedCell::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> GLYPHID_MEAT =
            Reg.item(
                    "glyphid_meat",
                    Item::new,
                    () ->
                            new Item.Properties()
                                    .food(
                                            new FoodProperties.Builder()
                                                    .nutrition(3)
                                                    .saturationModifier(0.5F)
                                                    .build()));

    public static final RegistryHandle<ItemWaffle> BOMB_WAFFLE =
            Reg.item(
                    "bomb_waffle",
                    ItemWaffle::new,
                    () ->
                            new Item.Properties()
                                    .food(
                                            new FoodProperties.Builder()
                                                    .nutrition(20)
                                                    .saturationModifier(0.6F)
                                                    .build()));

    public static final RegistryHandle<Item> GLOWING_STEW = stew("glowing_stew");
    public static final RegistryHandle<Item> BALEFIRE_SCRAMBLED = stew("balefire_scrambled");
    public static final RegistryHandle<Item> BALEFIRE_AND_HAM = stew("balefire_and_ham");
    public static final RegistryHandle<Item> GLYPHID_MEAT_GRILLED =
            Reg.item(
                    "glyphid_meat_grilled",
                    Item::new,
                    () ->
                            new Item.Properties()
                                    .food(
                                            new FoodProperties.Builder()
                                                    .nutrition(8)
                                                    .saturationModifier(0.75F)
                                                    .build(),
                                            Consumables.defaultFood()
                                                    .onConsume(
                                                            new ApplyStatusEffectsConsumeEffect(
                                                                    new MobEffectInstance(
                                                                            MobEffects.STRENGTH,
                                                                            180,
                                                                            1),
                                                                    1.0F))
                                                    .build()));
    public static final RegistryHandle<ItemGavel> WOOD_GAVEL =
            Reg.item(
                    "wood_gavel",
                    props -> new ItemGavel(props, ItemGavel.Type.WOOD),
                    () ->
                            ItemSwordAbility.swordProperties(
                                            ToolTier.VANILLA_WOOD,
                                            4.0F,
                                            0,
                                            ItemTags.WOODEN_TOOL_MATERIALS)
                                    .stacksTo(1));
    public static final RegistryHandle<ItemGavel> LEAD_GAVEL =
            Reg.item(
                    "lead_gavel",
                    props -> new ItemGavel(props, ItemGavel.Type.LEAD),
                    () ->
                            ItemSwordAbility.swordProperties(
                                            ToolTier.STEEL,
                                            6.0F,
                                            0,
                                            MaterialShapes.INGOT.tagFor("steel"))
                                    .stacksTo(1));
    public static final RegistryHandle<ItemGavel> DIAMOND_GAVEL =
            Reg.item(
                    "diamond_gavel",
                    props -> new ItemGavel(props, ItemGavel.Type.DIAMOND),
                    () ->
                            ItemSwordAbility.swordProperties(
                                            ToolTier.VANILLA_DIAMOND,
                                            7.0F,
                                            0,
                                            ItemTags.DIAMOND_TOOL_MATERIALS)
                                    .stacksTo(1));
    public static final RegistryHandle<ItemMeseGavel> MESE_GAVEL =
            Reg.item("mese_gavel", ItemMeseGavel::new, ItemMeseGavel::properties);
    public static final RegistryHandle<ItemSwordAbility> COBALT_SWORD =
            Reg.item(
                    "cobalt_sword",
                    properties -> new ItemSwordAbility(properties, AvailableAbilities.EMPTY),
                    () -> ItemSwordAbility.swordProperties(ToolTier.COBALT, 12.0F, 0, null));
    public static final RegistryHandle<ItemSwordAbility> COBALT_DECORATED_SWORD =
            Reg.item(
                    "cobalt_decorated_sword",
                    properties ->
                            new ItemSwordAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.BOBBLE, 0)
                                            .build()),
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.COBALT_DECORATED,
                                    15.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("cobalt")));
    public static final RegistryHandle<ItemSwordAbility> STARMETAL_SWORD =
            Reg.item(
                    "starmetal_sword",
                    properties ->
                            new ItemSwordAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .add(WeaponAbility.STUN, 1)
                                            .add(WeaponAbility.BOBBLE, 0)
                                            .build()),
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.STARMETAL,
                                    25.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("starmetal")));

    private static final float[] TTP_HIGH = {0.125F, 0.625F, 0};
    private static final float[] TTP_LOW = {0, 0.75F, 0};

    private static final float[] TTP_METEORITE = {0.2F, 0.55F, 0};
    private static final float[] SFP_SWORD = {1.36F, 1.36F, 0.68F};
    private static final float[] SFP_DEFAULT = {1, 1, 1};

    private static final long ELEC_CHARGE = 500_000L;
    private static final long ELEC_CHARGE_RATE = 1_000L;
    private static final long ELEC_CONSUMPTION = 100L;

    private static final float HOE_DAMAGE = 0.0F;

    private static Item.Properties undamageableHoeProperties() {
        return new Item.Properties() {
            @Override
            public Item.Properties durability(int maxDamage) {
                return maxDamage > 0 ? super.durability(maxDamage) : stacksTo(1);
            }
        };
    }

    public static final RegistryHandle<ItemToolAbility> SCHRABIDIUM_PICKAXE =
            Reg.item(
                    "schrabidium_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.RADIATION, 0)
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 6)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 4)
                                            .add(ToolHarvestAbility.SMELTER, 0)
                                            .add(ToolHarvestAbility.SHREDDER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                            ToolTier.SCHRABIDIUM,
                                            ToolType.PICKAXE,
                                            20.0F,
                                            0,
                                            Mats.MAT_SCHRABIDIUM.tag(MaterialShapes.INGOT))
                                    .rarity(Rarity.RARE));
    public static final RegistryHandle<ItemToolAbility> SCHRABIDIUM_AXE =
            Reg.item(
                    "schrabidium_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.RADIATION, 0)
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 6)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 4)
                                            .add(ToolHarvestAbility.SMELTER, 0)
                                            .add(ToolHarvestAbility.SHREDDER, 0)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                            ToolTier.SCHRABIDIUM,
                                            ToolType.AXE,
                                            25.0F,
                                            0,
                                            Mats.MAT_SCHRABIDIUM.tag(MaterialShapes.INGOT))
                                    .rarity(Rarity.RARE));
    public static final RegistryHandle<ItemToolAbility> SCHRABIDIUM_SHOVEL =
            Reg.item(
                    "schrabidium_shovel",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.RADIATION, 0)
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 6)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 4)
                                            .add(ToolHarvestAbility.SMELTER, 0)
                                            .add(ToolHarvestAbility.SHREDDER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                            ToolTier.SCHRABIDIUM,
                                            ToolType.SHOVEL,
                                            15.0F,
                                            0,
                                            Mats.MAT_SCHRABIDIUM.tag(MaterialShapes.INGOT))
                                    .rarity(Rarity.RARE));
    public static final RegistryHandle<ItemToolAbility> TITANIUM_PICKAXE =
            Reg.item(
                    "titanium_pickaxe",
                    properties -> new ItemToolAbility(properties, AvailableAbilities.EMPTY, false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.TITANIUM,
                                    ToolType.PICKAXE,
                                    4.5F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("titanium")));
    public static final RegistryHandle<ItemToolAbility> TITANIUM_AXE =
            Reg.item(
                    "titanium_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.TITANIUM,
                                    ToolType.AXE,
                                    5.5F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("titanium")));
    public static final RegistryHandle<ItemToolAbility> TITANIUM_SHOVEL =
            Reg.item(
                    "titanium_shovel",
                    properties -> new ItemToolAbility(properties, AvailableAbilities.EMPTY, false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.TITANIUM,
                                    ToolType.SHOVEL,
                                    3.5F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("titanium")));
    public static final RegistryHandle<ItemToolAbility> STEEL_PICKAXE =
            Reg.item(
                    "steel_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.STEEL,
                                    ToolType.PICKAXE,
                                    4.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("steel")));
    public static final RegistryHandle<ItemToolAbility> STEEL_AXE =
            Reg.item(
                    "steel_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 0)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.STEEL,
                                    ToolType.AXE,
                                    5.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("steel")));
    public static final RegistryHandle<ItemToolAbility> STEEL_SHOVEL =
            Reg.item(
                    "steel_shovel",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.STEEL,
                                    ToolType.SHOVEL,
                                    3.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("steel")));
    public static final RegistryHandle<ItemToolAbility> CMB_PICKAXE =
            Reg.item(
                    "cmb_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(ToolHarvestAbility.SMELTER, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 2)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.CMB,
                                    ToolType.PICKAXE,
                                    10.0F,
                                    0,
                                    Mats.MAT_CMB.tag(MaterialShapes.INGOT)));
    public static final RegistryHandle<ItemToolAbility> CMB_AXE =
            Reg.item(
                    "cmb_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(ToolHarvestAbility.SMELTER, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 2)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.CMB,
                                    ToolType.AXE,
                                    30.0F,
                                    0,
                                    Mats.MAT_CMB.tag(MaterialShapes.INGOT)));
    public static final RegistryHandle<ItemToolAbility> CMB_SHOVEL =
            Reg.item(
                    "cmb_shovel",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(ToolHarvestAbility.SMELTER, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 2)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.CMB,
                                    ToolType.SHOVEL,
                                    8.0F,
                                    0,
                                    Mats.MAT_CMB.tag(MaterialShapes.INGOT)));
    public static final RegistryHandle<ItemToolAbility> DESH_PICKAXE =
            Reg.item(
                    "desh_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolAreaAbility.RECURSION, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 1)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.DESH,
                                    ToolType.PICKAXE,
                                    5.0F,
                                    -0.05,
                                    Mats.MAT_DESH.tag(MaterialShapes.INGOT)));
    public static final RegistryHandle<ItemToolAbility> DESH_AXE =
            Reg.item(
                    "desh_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolAreaAbility.RECURSION, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 1)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.DESH,
                                    ToolType.AXE,
                                    7.5F,
                                    -0.05,
                                    Mats.MAT_DESH.tag(MaterialShapes.INGOT)));
    public static final RegistryHandle<ItemToolAbility> DESH_SHOVEL =
            Reg.item(
                    "desh_shovel",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolAreaAbility.RECURSION, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 1)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.DESH,
                                    ToolType.SHOVEL,
                                    4.0F,
                                    -0.05,
                                    Mats.MAT_DESH.tag(MaterialShapes.INGOT)));
    public static final RegistryHandle<ItemToolAbility> COBALT_PICKAXE =
            Reg.item(
                    "cobalt_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.COBALT, ToolType.PICKAXE, 4.0F, 0, null));
    public static final RegistryHandle<ItemToolAbility> COBALT_AXE =
            Reg.item(
                    "cobalt_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 0)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.COBALT, ToolType.AXE, 6.0F, 0, null));
    public static final RegistryHandle<ItemToolAbility> COBALT_SHOVEL =
            Reg.item(
                    "cobalt_shovel",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.COBALT, ToolType.SHOVEL, 3.5F, 0, null));
    public static final RegistryHandle<ItemToolAbility> COBALT_DECORATED_PICKAXE =
            Reg.item(
                    "cobalt_decorated_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 2)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.COBALT_DECORATED,
                                    ToolType.PICKAXE,
                                    6.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("cobalt")));
    public static final RegistryHandle<ItemToolAbility> COBALT_DECORATED_AXE =
            Reg.item(
                    "cobalt_decorated_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 2)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.COBALT_DECORATED,
                                    ToolType.AXE,
                                    8.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("cobalt")));
    public static final RegistryHandle<ItemToolAbility> COBALT_DECORATED_SHOVEL =
            Reg.item(
                    "cobalt_decorated_shovel",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 2)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.COBALT_DECORATED,
                                    ToolType.SHOVEL,
                                    5.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("cobalt")));
    public static final RegistryHandle<ItemToolAbility> STARMETAL_PICKAXE =
            Reg.item(
                    "starmetal_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 3)
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 4)
                                            .add(WeaponAbility.STUN, 1)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.STARMETAL,
                                    ToolType.PICKAXE,
                                    8.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("starmetal")));
    public static final RegistryHandle<ItemToolAbility> STARMETAL_AXE =
            Reg.item(
                    "starmetal_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 3)
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 4)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .add(WeaponAbility.STUN, 1)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.STARMETAL,
                                    ToolType.AXE,
                                    12.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("starmetal")));
    public static final RegistryHandle<ItemToolAbility> STARMETAL_SHOVEL =
            Reg.item(
                    "starmetal_shovel",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.RECURSION, 3)
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 4)
                                            .add(WeaponAbility.STUN, 1)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.STARMETAL,
                                    ToolType.SHOVEL,
                                    7.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("starmetal")));
    public static final RegistryHandle<ItemToolAbility> CENTRI_STICK =
            Reg.item(
                    "centri_stick",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolHarvestAbility.CENTRIFUGE, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                            ToolTier.ELEC, ToolType.MINER, 3.0F, 0, null)
                                    .durability(50));
    public static final RegistryHandle<ItemToolAbility> SMASHING_HAMMER =
            Reg.item(
                    "smashing_hammer",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolHarvestAbility.SHREDDER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                            ToolTier.STEEL,
                                            ToolType.MINER,
                                            12.0F,
                                            -0.1,
                                            MaterialShapes.INGOT.tagFor("steel"))
                                    .durability(2500));
    public static final RegistryHandle<ItemToolAbility> BISMUTH_PICKAXE =
            Reg.item(
                    "bismuth_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.SHREDDER, 0)
                                            .add(ToolHarvestAbility.LUCK, 1)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(WeaponAbility.STUN, 2)
                                            .add(WeaponAbility.VAMPIRE, 0)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    true),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.BISMUTH,
                                    ToolType.MINER,
                                    15.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("bismuth")));
    public static final RegistryHandle<ItemToolAbility> BISMUTH_AXE =
            Reg.item(
                    "bismuth_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.SHREDDER, 0)
                                            .add(ToolHarvestAbility.LUCK, 1)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(WeaponAbility.STUN, 3)
                                            .add(WeaponAbility.VAMPIRE, 1)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.BISMUTH,
                                    ToolType.AXE,
                                    25.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("bismuth")));
    public static final RegistryHandle<ItemToolAbility> VOLCANIC_PICKAXE =
            Reg.item(
                    "volcanic_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.SMELTER, 0)
                                            .add(ToolHarvestAbility.LUCK, 2)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(WeaponAbility.FIRE, 0)
                                            .add(WeaponAbility.VAMPIRE, 0)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    true),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.VOLCANIC,
                                    ToolType.MINER,
                                    15.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("bismuth")));
    public static final RegistryHandle<ItemToolAbility> VOLCANIC_AXE =
            Reg.item(
                    "volcanic_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.SMELTER, 0)
                                            .add(ToolHarvestAbility.LUCK, 2)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(WeaponAbility.FIRE, 1)
                                            .add(WeaponAbility.VAMPIRE, 1)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.VOLCANIC,
                                    ToolType.AXE,
                                    25.0F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("bismuth")));
    public static final RegistryHandle<ItemToolAbility> CHLOROPHYTE_PICKAXE =
            Reg.item(
                    "chlorophyte_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.LUCK, 3)
                                            .add(ToolHarvestAbility.CENTRIFUGE, 0)
                                            .add(ToolHarvestAbility.MERCURY, 0)
                                            .add(WeaponAbility.STUN, 3)
                                            .add(WeaponAbility.VAMPIRE, 2)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    true),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.CHLOROPHYTE,
                                    ToolType.MINER,
                                    20.0F,
                                    0,
                                    ItemToolAbility.repairTag("powder_chlorophyte")));
    public static final RegistryHandle<ItemToolAbility> CHLOROPHYTE_AXE =
            Reg.item(
                    "chlorophyte_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 1)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 1)
                                            .add(ToolAreaAbility.RECURSION, 1)
                                            .add(ToolHarvestAbility.LUCK, 3)
                                            .add(WeaponAbility.STUN, 4)
                                            .add(WeaponAbility.VAMPIRE, 3)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.CHLOROPHYTE,
                                    ToolType.AXE,
                                    50.0F,
                                    0,
                                    ItemToolAbility.repairTag("powder_chlorophyte")));
    public static final RegistryHandle<ItemToolAbility> MESE_PICKAXE =
            Reg.item(
                    "mese_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 2)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 2)
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(ToolHarvestAbility.CRYSTALLIZER, 0)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 5)
                                            .add(ToolAreaAbility.EXPLOSION, 3)
                                            .add(WeaponAbility.STUN, 3)
                                            .add(WeaponAbility.PHOSPHORUS, 0)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    true),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.MESE,
                                    ToolType.MINER,
                                    35.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemToolAbility> MESE_AXE =
            Reg.item(
                    "mese_axe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 2)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 2)
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 5)
                                            .add(ToolAreaAbility.EXPLOSION, 3)
                                            .add(WeaponAbility.STUN, 4)
                                            .add(WeaponAbility.PHOSPHORUS, 1)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.MESE,
                                    ToolType.AXE,
                                    75.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemToolAbility> DWARVEN_PICKAXE =
            Reg.item(
                    "dwarven_pickaxe",
                    properties ->
                            new ItemToolAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .build(),
                                    false),
                    () ->
                            ItemToolAbility.toolProperties(
                                            ToolTier.DWARVEN,
                                            ToolType.MINER,
                                            5.0F,
                                            -0.1,
                                            MaterialShapes.INGOT.tagFor("copper"))
                                    .durability(250));

    public static final RegistryHandle<ItemChainsaw> CHAINSAW =
            Reg.item(
                    "chainsaw",
                    properties ->
                            new ItemChainsaw(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(WeaponAbility.CHAINSAW, 1)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    () ->
                                            new Fluid[] {
                                                NTMFluids.DIESEL,
                                                NTMFluids.DIESEL_CRACK,
                                                NTMFluids.KEROSENE,
                                                NTMFluids.BIOFUEL,
                                                NTMFluids.GASOLINE,
                                                NTMFluids.GASOLINE_LEADED,
                                                NTMFluids.PETROIL,
                                                NTMFluids.PETROIL_LEADED,
                                                NTMFluids.COALGAS,
                                                NTMFluids.COALGAS_LEADED
                                            },
                                    5_000,
                                    1,
                                    250),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.CHAINSAW, ToolType.AXE, 25.0F, -0.05, null));

    public static final RegistryHandle<ItemSwordAbilityPower> ELEC_SWORD =
            Reg.item(
                    "elec_sword",
                    properties ->
                            new ItemSwordAbilityPower(
                                    properties,
                                    AvailableAbilities.builder().add(WeaponAbility.STUN, 2).build(),
                                    ELEC_CHARGE,
                                    ELEC_CHARGE_RATE,
                                    ELEC_CONSUMPTION),
                    () -> ItemSwordAbility.swordProperties(ToolTier.ELEC, 12.5F, 0, null));
    public static final RegistryHandle<ItemToolAbilityPower> ELEC_PICKAXE =
            Reg.item(
                    "elec_pickaxe",
                    properties ->
                            new ItemToolAbilityPower(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 1)
                                            .build(),
                                    false,
                                    ELEC_CHARGE,
                                    ELEC_CHARGE_RATE,
                                    ELEC_CONSUMPTION),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.ELEC, ToolType.PICKAXE, 6.0F, 0, null));
    public static final RegistryHandle<ItemToolAbilityPower> ELEC_AXE =
            Reg.item(
                    "elec_axe",
                    properties ->
                            new ItemToolAbilityPower(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 1)
                                            .add(WeaponAbility.CHAINSAW, 0)
                                            .add(WeaponAbility.BEHEADER, 0)
                                            .build(),
                                    false,
                                    true,
                                    ELEC_CHARGE,
                                    ELEC_CHARGE_RATE,
                                    ELEC_CONSUMPTION),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.ELEC, ToolType.AXE, 10.0F, 0, null));
    public static final RegistryHandle<ItemToolAbilityPower> ELEC_SHOVEL =
            Reg.item(
                    "elec_shovel",
                    properties ->
                            new ItemToolAbilityPower(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(ToolAreaAbility.HAMMER, 0)
                                            .add(ToolAreaAbility.HAMMER_FLAT, 0)
                                            .add(ToolAreaAbility.RECURSION, 2)
                                            .add(ToolHarvestAbility.SILK, 0)
                                            .add(ToolHarvestAbility.LUCK, 1)
                                            .build(),
                                    false,
                                    ELEC_CHARGE,
                                    ELEC_CHARGE_RATE,
                                    ELEC_CONSUMPTION),
                    () ->
                            ItemToolAbility.toolProperties(
                                    ToolTier.ELEC, ToolType.SHOVEL, 5.0F, 0, null));
    public static final RegistryHandle<ItemSwordAbility> SCHRABIDIUM_SWORD =
            Reg.item(
                    "schrabidium_sword",
                    properties ->
                            new ItemSwordAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.RADIATION, 1)
                                            .add(WeaponAbility.VAMPIRE, 0)
                                            .build()),
                    () ->
                            ItemSwordAbility.swordProperties(
                                            ToolTier.SCHRABIDIUM,
                                            75.0F,
                                            0,
                                            Mats.MAT_SCHRABIDIUM.tag(MaterialShapes.INGOT))
                                    .rarity(Rarity.RARE));
    public static final RegistryHandle<ItemSwordAbility> TITANIUM_SWORD =
            Reg.item(
                    "titanium_sword",
                    properties -> new ItemSwordAbility(properties, AvailableAbilities.EMPTY),
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.TITANIUM,
                                    6.5F,
                                    0,
                                    MaterialShapes.INGOT.tagFor("titanium")));
    public static final RegistryHandle<ItemSwordAbility> STEEL_SWORD =
            Reg.item(
                    "steel_sword",
                    properties ->
                            new ItemSwordAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.STUN, 0)
                                            .build()),
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.STEEL, 6.0F, 0, MaterialShapes.INGOT.tagFor("steel")));
    public static final RegistryHandle<ItemSwordAbility> CMB_SWORD =
            Reg.item(
                    "cmb_sword",
                    properties ->
                            new ItemSwordAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.STUN, 0)
                                            .add(WeaponAbility.VAMPIRE, 0)
                                            .build()),
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.CMB,
                                    35.0F,
                                    0,
                                    Mats.MAT_CMB.tag(MaterialShapes.INGOT)));
    public static final RegistryHandle<ItemSwordAbility> DESH_SWORD =
            Reg.item(
                    "desh_sword",
                    properties ->
                            new ItemSwordAbility(
                                    properties,
                                    AvailableAbilities.builder()
                                            .add(WeaponAbility.STUN, 0)
                                            .build()),
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.DESH,
                                    12.5F,
                                    0,
                                    Mats.MAT_DESH.tag(MaterialShapes.INGOT)));
    public static final RegistryHandle<ItemSwordAbility> DNT_SWORD =
            Reg.item(
                    "dnt_sword",
                    properties -> new ItemSwordAbility(properties, AvailableAbilities.EMPTY),
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.MESE,
                                    12.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<HoeItem> SCHRABIDIUM_HOE =
            Reg.item(
                    "schrabidium_hoe",
                    properties ->
                            new HoeItem(
                                    ToolTier.SCHRABIDIUM.vanilla(
                                            Mats.MAT_SCHRABIDIUM.tag(MaterialShapes.INGOT)),
                                    HOE_DAMAGE,
                                    ItemToolAbility.hoeSpeed(ToolTier.SCHRABIDIUM),
                                    properties),
                    () -> new Item.Properties().rarity(Rarity.RARE));
    public static final RegistryHandle<HoeItem> TITANIUM_HOE =
            Reg.item(
                    "titanium_hoe",
                    properties ->
                            new HoeItem(
                                    ToolTier.TITANIUM.vanilla(
                                            Mats.MAT_TITANIUM.tag(MaterialShapes.INGOT)),
                                    HOE_DAMAGE,
                                    ItemToolAbility.hoeSpeed(ToolTier.TITANIUM),
                                    properties),
                    () -> new Item.Properties());
    public static final RegistryHandle<HoeItem> STEEL_HOE =
            Reg.item(
                    "steel_hoe",
                    properties ->
                            new HoeItem(
                                    ToolTier.STEEL.vanilla(
                                            Mats.MAT_STEEL.tag(MaterialShapes.INGOT)),
                                    HOE_DAMAGE,
                                    ItemToolAbility.hoeSpeed(ToolTier.STEEL),
                                    properties),
                    () -> new Item.Properties());
    public static final RegistryHandle<HoeItem> CMB_HOE =
            Reg.item(
                    "cmb_hoe",
                    properties ->
                            new HoeItem(
                                    ToolTier.CMB.vanilla(Mats.MAT_CMB.tag(MaterialShapes.INGOT)),
                                    HOE_DAMAGE,
                                    ItemToolAbility.hoeSpeed(ToolTier.CMB),
                                    properties),
                    () -> new Item.Properties());
    public static final RegistryHandle<HoeItem> DESH_HOE =
            Reg.item(
                    "desh_hoe",
                    properties ->
                            new HoeItem(
                                    ToolTier.DESH.vanilla(Mats.MAT_DESH.tag(MaterialShapes.INGOT)),
                                    HOE_DAMAGE,
                                    ItemToolAbility.hoeSpeed(ToolTier.DESH),
                                    properties),
                    ModItems::undamageableHoeProperties);
    public static final RegistryHandle<HoeItem> COBALT_HOE =
            Reg.item(
                    "cobalt_hoe",
                    properties ->
                            new HoeItem(
                                    ToolTier.COBALT.vanilla(
                                            Mats.MAT_COBALT.tag(MaterialShapes.INGOT)),
                                    HOE_DAMAGE,
                                    ItemToolAbility.hoeSpeed(ToolTier.COBALT),
                                    properties),
                    () -> new Item.Properties());
    public static final RegistryHandle<HoeItem> COBALT_DECORATED_HOE =
            Reg.item(
                    "cobalt_decorated_hoe",
                    properties ->
                            new HoeItem(
                                    ToolTier.COBALT_DECORATED.vanilla(
                                            Mats.MAT_COBALT.tag(MaterialShapes.INGOT)),
                                    HOE_DAMAGE,
                                    ItemToolAbility.hoeSpeed(ToolTier.COBALT_DECORATED),
                                    properties),
                    () -> new Item.Properties());
    public static final RegistryHandle<HoeItem> STARMETAL_HOE =
            Reg.item(
                    "starmetal_hoe",
                    properties ->
                            new HoeItem(
                                    ToolTier.STARMETAL.vanilla(
                                            Mats.MAT_STAR.tag(MaterialShapes.INGOT)),
                                    HOE_DAMAGE,
                                    ItemToolAbility.hoeSpeed(ToolTier.STARMETAL),
                                    properties),
                    () -> new Item.Properties());

    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD =
            Reg.item(
                    "meteorite_sword",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    9.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_SEARED =
            Reg.item(
                    "meteorite_sword_seared",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    10.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_REFORGED =
            Reg.item(
                    "meteorite_sword_reforged",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    12.5F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_HARDENED =
            Reg.item(
                    "meteorite_sword_hardened",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    15.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_ALLOYED =
            Reg.item(
                    "meteorite_sword_alloyed",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    17.5F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_MACHINED =
            Reg.item(
                    "meteorite_sword_machined",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    20.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_TREATED =
            Reg.item(
                    "meteorite_sword_treated",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    22.5F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_ETCHED =
            Reg.item(
                    "meteorite_sword_etched",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    25.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_BRED =
            Reg.item(
                    "meteorite_sword_bred",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    30.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_IRRADIATED =
            Reg.item(
                    "meteorite_sword_irradiated",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    35.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_FUSED =
            Reg.item(
                    "meteorite_sword_fused",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    50.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));
    public static final RegistryHandle<ItemSwordMeteorite> METEORITE_SWORD_BALEFUL =
            Reg.item(
                    "meteorite_sword_baleful",
                    ItemSwordMeteorite::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.METEORITE,
                                    75.0F,
                                    0,
                                    ItemToolAbility.repairTag("plate_paa")));

    public static final List<RegistryHandle<ItemSwordMeteorite>> METEORITE_SWORDS =
            List.of(
                    METEORITE_SWORD,
                    METEORITE_SWORD_SEARED,
                    METEORITE_SWORD_REFORGED,
                    METEORITE_SWORD_HARDENED,
                    METEORITE_SWORD_ALLOYED,
                    METEORITE_SWORD_MACHINED,
                    METEORITE_SWORD_TREATED,
                    METEORITE_SWORD_ETCHED,
                    METEORITE_SWORD_BRED,
                    METEORITE_SWORD_IRRADIATED,
                    METEORITE_SWORD_FUSED,
                    METEORITE_SWORD_BALEFUL);

    public static final RegistryHandle<ItemRedstoneSword> REDSTONE_SWORD =
            Reg.item(
                    "redstone_sword",
                    ItemRedstoneSword::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.VANILLA_STONE,
                                    5.0F,
                                    0,
                                    ItemTags.STONE_TOOL_MATERIALS));
    public static final RegistryHandle<Item> BIG_SWORD =
            Reg.item(
                    "big_sword",
                    Item::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.VANILLA_DIAMOND,
                                    7.0F,
                                    0,
                                    ItemTags.DIAMOND_TOOL_MATERIALS));

    public static final RegistryHandle<Item> BISMUTH_TOOL =
            Reg.item("bismuth_tool", Item::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemWeaponSpecial> SCHRABIDIUM_HAMMER =
            Reg.item(
                    "schrabidium_hammer",
                    properties ->
                            new ItemWeaponSpecial(
                                    properties, ItemWeaponSpecial.Effect.INSTANT_KILL),
                    () ->
                            ItemSwordAbility.swordProperties(
                                            ToolTier.SCHRABIDIUM_HAMMER,
                                            1000000000.0F,
                                            -0.5D,
                                            Mats.MAT_SCHRABIDIUM.tag(MaterialShapes.BLOCK))
                                    .stacksTo(1)
                                    .rarity(Rarity.RARE));
    public static final RegistryHandle<ItemWeaponSpecial> SHIMMER_SLEDGE =
            Reg.item(
                    "shimmer_sledge",
                    properties ->
                            new ItemWeaponSpecial(properties, ItemWeaponSpecial.Effect.LAUNCH),
                    () ->
                            ItemSwordAbility.swordProperties(ToolTier.SHIMMER, 30.0F, -0.2D, null)
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC));
    public static final RegistryHandle<ItemWeaponSpecial> SHIMMER_AXE =
            Reg.item(
                    "shimmer_axe",
                    properties -> new ItemWeaponSpecial(properties, ItemWeaponSpecial.Effect.HALVE),
                    () ->
                            ItemSwordAbility.swordProperties(ToolTier.SHIMMER, 30.0F, -0.2D, null)
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC));
    public static final RegistryHandle<ItemMatch> MATCHSTICK =
            Reg.item("matchstick", ItemMatch::new, Item.Properties::new);
    public static final RegistryHandle<ItemBalefireMatch> BALEFIRE_AND_STEEL =
            Reg.item(
                    "balefire_and_steel",
                    ItemBalefireMatch::new,
                    () -> new Item.Properties().stacksTo(1).durability(256));
    public static final RegistryHandle<ModSword> CROWBAR =
            Reg.item(
                    "crowbar",
                    props -> new ModSword(props),
                    () ->
                            ItemSwordAbility.swordProperties(
                                    ToolTier.STEEL, 6.0F, 0, MaterialShapes.INGOT.tagFor("steel")));

    public static final RegistryHandle<ModSword> WEAPON_PIPE_LEAD =
            Reg.item(
                    "weapon_pipe_lead",
                    props -> new ModSword(props, "desc.item.modSword.iMGoingTo"),
                    () ->
                            ItemSwordAbility.swordProperties(ToolTier.PIPE_LEAD, 7.0F, 0, null)
                                    .stacksTo(1));
    public static final RegistryHandle<ItemWiring> WIRING_RED_COPPER =
            Reg.item("wiring_red_copper", ItemWiring::new, Item.Properties::new);
    public static final RegistryHandle<ItemWrench> WRENCH =
            Reg.item("wrench", ItemWrench::new, ItemWrench::properties);

    public static final RegistryHandle<ItemTooling> WRENCH_ARCHINEER =
            Reg.item(
                    "wrench_archineer",
                    props -> new ItemTooling(props, IToolable.ToolType.WRENCH),
                    () -> ItemTooling.weaponProperties(1000, 12.0F));
    public static final RegistryHandle<ItemWeaponSpecial> WRENCH_FLIPPED =
            Reg.item(
                    "wrench_flipped",
                    properties -> new ItemWeaponSpecial(properties, ItemWeaponSpecial.Effect.NONE),
                    () ->
                            ItemSwordAbility.swordProperties(ToolTier.ELEC, 16.0F, -0.1D, null)
                                    .stacksTo(1));
    public static final RegistryHandle<ItemMemespoon> MEMESPOON =
            Reg.item(
                    "memespoon",
                    ItemMemespoon::new,
                    () ->
                            ItemSwordAbility.swordProperties(
                                            ToolTier.STEEL,
                                            6.0F,
                                            0,
                                            MaterialShapes.INGOT.tagFor("steel"))
                                    .stacksTo(1));
    public static final RegistryHandle<ModSword> REER_GRAAR =
            Reg.item(
                    "reer_graar",
                    props ->
                            new ModSword(
                                    props,
                                    "desc.item.modSword.reerGraar.0",
                                    "desc.item.modSword.reerGraar.1"),
                    () ->
                            ItemSwordAbility.swordProperties(
                                            ToolTier.TITANIUM,
                                            6.5F,
                                            0,
                                            MaterialShapes.INGOT.tagFor("titanium"))
                                    .stacksTo(1));
    public static final RegistryHandle<ItemWeaponSpecial> STOPSIGN =
            Reg.item(
                    "stopsign",
                    properties ->
                            new ItemWeaponSpecial(properties, ItemWeaponSpecial.Effect.SOUND_ONLY),
                    ModItems::signProperties);
    public static final RegistryHandle<ItemWeaponSpecial> SOPSIGN =
            Reg.item(
                    "sopsign",
                    properties ->
                            new ItemWeaponSpecial(properties, ItemWeaponSpecial.Effect.SOUND_ONLY),
                    ModItems::signProperties);
    public static final RegistryHandle<ItemWeaponSpecial> CHERNOBYLSIGN =
            Reg.item(
                    "chernobylsign",
                    properties -> new ItemWeaponSpecial(properties, ItemWeaponSpecial.Effect.NONE),
                    ModItems::signProperties);
    public static final RegistryHandle<Item> SYRINGE_EMPTY =
            Reg.item("syringe_empty", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<MedicalSyringeItem> SYRINGE_ANTIDOTE =
            Reg.item(
                            "syringe_antidote",
                            props ->
                                    new MedicalSyringeItem(
                                            props, MedicalSyringeItem.Type.ANTIDOTE, SYRINGE_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalSyringeItem> SYRINGE_POISON =
            Reg.item(
                            "syringe_poison",
                            props ->
                                    new MedicalSyringeItem(
                                            props, MedicalSyringeItem.Type.POISON, SYRINGE_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalSyringeItem> SYRINGE_AWESOME =
            Reg.item(
                            "syringe_awesome",
                            props ->
                                    new MedicalSyringeItem(
                                            props, MedicalSyringeItem.Type.AWESOME, SYRINGE_EMPTY),
                            () ->
                                    new Item.Properties()
                                            .rarity(Rarity.UNCOMMON)
                                            .component(
                                                    DataComponents.ENCHANTMENT_GLINT_OVERRIDE,
                                                    true))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> SYRINGE_METAL_EMPTY =
            Reg.item("syringe_metal_empty", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<MedicalSyringeItem> SYRINGE_METAL_STIMPAK =
            Reg.item(
                            "syringe_metal_stimpak",
                            props ->
                                    new MedicalSyringeItem(
                                            props,
                                            MedicalSyringeItem.Type.STIMPAK,
                                            SYRINGE_METAL_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalSyringeItem> SYRINGE_METAL_MEDX =
            Reg.item(
                            "syringe_metal_medx",
                            props ->
                                    new MedicalSyringeItem(
                                            props,
                                            MedicalSyringeItem.Type.MEDX,
                                            SYRINGE_METAL_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalSyringeItem> SYRINGE_METAL_PSYCHO =
            Reg.item(
                            "syringe_metal_psycho",
                            props ->
                                    new MedicalSyringeItem(
                                            props,
                                            MedicalSyringeItem.Type.PSYCHO,
                                            SYRINGE_METAL_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalSyringeItem> SYRINGE_METAL_SUPER =
            Reg.item(
                            "syringe_metal_super",
                            props ->
                                    new MedicalSyringeItem(
                                            props,
                                            MedicalSyringeItem.Type.SUPER_STIMPAK,
                                            SYRINGE_METAL_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalSyringeItem> SYRINGE_TAINT =
            Reg.item(
                            "syringe_taint",
                            props ->
                                    new MedicalSyringeItem(
                                            props,
                                            MedicalSyringeItem.Type.TAINT,
                                            SYRINGE_METAL_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemSyringeMku> SYRINGE_MKUNICORN =
            Reg.item("syringe_mkunicorn", ItemSyringeMku::new, Item.Properties::new);
    public static final RegistryHandle<MedicalSyringeItem> MED_BAG =
            Reg.item(
                            "med_bag",
                            props ->
                                    new MedicalSyringeItem(
                                            props,
                                            MedicalSyringeItem.Type.MED_BAG,
                                            SYRINGE_METAL_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);

    public static final RegistryHandle<MedicalIVItem> IV_EMPTY =
            Reg.item(
                            "iv_empty",
                            props ->
                                    new MedicalIVItem(
                                            props,
                                            MedicalIVItem.Type.EMPTY,
                                            () -> ModItems.IV_BLOOD.get()),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalIVItem> IV_BLOOD =
            Reg.item(
                            "iv_blood",
                            props -> new MedicalIVItem(props, MedicalIVItem.Type.BLOOD, IV_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalIVItem> IV_XP_EMPTY =
            Reg.item(
                            "iv_xp_empty",
                            props ->
                                    new MedicalIVItem(
                                            props,
                                            MedicalIVItem.Type.XP_EMPTY,
                                            () -> ModItems.IV_XP.get()),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalIVItem> IV_XP =
            Reg.item(
                            "iv_xp",
                            props -> new MedicalIVItem(props, MedicalIVItem.Type.XP, IV_XP_EMPTY),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalFluidItem> RADAWAY =
            Reg.item(
                            "radaway",
                            props -> new MedicalFluidItem(props, IV_EMPTY, 140),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalFluidItem> RADAWAY_STRONG =
            Reg.item(
                            "radaway_strong",
                            props -> new MedicalFluidItem(props, IV_EMPTY, 350),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<MedicalFluidItem> RADAWAY_FLUSH =
            Reg.item(
                            "radaway_flush",
                            props -> new MedicalFluidItem(props, IV_EMPTY, 500),
                            Item.Properties::new)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemPill> RADX = pill("radx", ItemPill.Type.RADX, "radx");
    public static final RegistryHandle<ItemPill> SIOX = pill("siox", ItemPill.Type.SIOX, "siox");
    public static final RegistryHandle<ItemPill> PILL_HERBAL =
            pill("pill_herbal", ItemPill.Type.HERBAL, "pill_herbal");
    public static final RegistryHandle<ItemPill> PILL_IODINE =
            pill("pill_iodine", ItemPill.Type.IODINE, "pill_iodine");
    public static final RegistryHandle<ItemPill> XANAX =
            pill("xanax", ItemPill.Type.XANAX, "xanax");
    public static final RegistryHandle<ItemPill> FMN = pill("fmn", ItemPill.Type.FMN, "tablet");
    public static final RegistryHandle<ItemPill> FIVE_HTP =
            pill("five_htp", ItemPill.Type.FIVE_HTP, "5htp");
    public static final RegistryHandle<ItemPill> PLAN_C =
            pill("plan_c", ItemPill.Type.PLAN_C, "plan_c");
    public static final RegistryHandle<ItemPill> PILL_RED =
            pill("pill_red", ItemPill.Type.RED, "pill_red");
    public static final RegistryHandle<ItemStealthBoy> STEALTH_BOY =
            Reg.item("stealth_boy", ItemStealthBoy::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemGasFilter> GAS_MASK_FILTER = filter("gas_mask_filter");
    public static final RegistryHandle<ItemGasFilter> GAS_MASK_FILTER_MONO =
            filter("gas_mask_filter_mono");
    public static final RegistryHandle<ItemGasFilter> GAS_MASK_FILTER_COMBO =
            filter("gas_mask_filter_combo");
    public static final RegistryHandle<ItemGasFilter> GAS_MASK_FILTER_RAG =
            filter("gas_mask_filter_rag");
    public static final RegistryHandle<ItemGasFilter> GAS_MASK_FILTER_PISS =
            filter("gas_mask_filter_piss");
    private static final ArmorMaterial STEEL_MAT =
            material("steel", 30, 3, 8, 6, 3, 5, Mats.MAT_STEEL.dict.ingot());
    public static final RegistryHandle<ItemPipette> PIPETTE =
            Reg.item(
                    "pipette",
                    props -> new ItemPipette(props, ItemPipette.Variant.PLAIN),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemPipette> PIPETTE_BORON =
            Reg.item(
                    "pipette_boron",
                    props -> new ItemPipette(props, ItemPipette.Variant.BORON),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemPipette> PIPETTE_LABORATORY =
            Reg.item(
                    "pipette_laboratory",
                    props -> new ItemPipette(props, ItemPipette.Variant.LABORATORY),
                    () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemPolaroid> POLAROID =
            Reg.item("polaroid", ItemPolaroid::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemGlitch> GLITCH =
            Reg.item("glitch", ItemGlitch::new, () -> new Item.Properties().durability(1));
    public static final RegistryHandle<ItemMeteorRemote> METEOR_REMOTE =
            Reg.item(
                    "meteor_remote",
                    ItemMeteorRemote::new,
                    () -> new Item.Properties().durability(2));
    public static final RegistryHandle<ItemAnchorRemote> ANCHOR_REMOTE =
            Reg.item(
                    "anchor_remote",
                    ItemAnchorRemote::new,
                    () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemColtanCompass> COLTAN_TOOL =
            Reg.item(
                    "coltan_tool", ItemColtanCompass::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemPowerNetTool> POWER_NET_TOOL =
            Reg.item(
                    "power_net_tool",
                    ItemPowerNetTool::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemAnalysisTool> ANALYSIS_TOOL =
            Reg.item(
                    "analysis_tool",
                    ItemAnalysisTool::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemFluidSiphon> SIPHON =
            Reg.item("siphon", ItemFluidSiphon::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemJetpackTank> JETPACK_TANK =
            Reg.item("jetpack_tank", ItemJetpackTank::new, () -> new Item.Properties().stacksTo(16))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<MedicalSyringeItem> CBT_DEVICE =
            Reg.item(
                    "cbt_device",
                    props ->
                            new MedicalSyringeItem(
                                    props, MedicalSyringeItem.Type.CBT, SYRINGE_EMPTY),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemRepairKit> GUN_KIT_1 =
            Reg.item(
                            "gun_kit_1",
                            props -> new ItemRepairKit(props, () -> ModSounds.ITEM_SPRAY.get()),
                            () -> new Item.Properties().stacksTo(1).durability(9))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemRepairKit> GUN_KIT_2 =
            Reg.item(
                            "gun_kit_2",
                            props -> new ItemRepairKit(props, () -> ModSounds.ITEM_REPAIR.get()),
                            () -> new Item.Properties().stacksTo(1).durability(99))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<ItemSchnitzelVegan> SCHNITZEL_VEGAN =
            Reg.item("schnitzel_vegan", ItemSchnitzelVegan::new, () -> food(0, 0F)).addTo(MISC_ALL);
    public static final RegistryHandle<ItemCottonCandy> COTTON_CANDY =
            Reg.item("cotton_candy", ItemCottonCandy::new, () -> alwaysEdible(5, 0.6F))
                    .addTo(MISC_ALL);

    public static final ItemFamily<ItemAppleBase.Tier, ItemAppleLead> APPLE_LEAD =
            Reg.family(
                    "apple_lead",
                    ItemAppleBase.Tier.class,
                    ItemAppleLead::new,
                    () -> alwaysEdible(5, 0F));
    public static final ItemFamily<ItemAppleBase.Tier, ItemAppleSchrabidium> APPLE_SCHRABIDIUM =
            Reg.family(
                    "apple_schrabidium",
                    ItemAppleBase.Tier.class,
                    ItemAppleSchrabidium::new,
                    () -> alwaysEdible(20, 100F));

    public static final RegistryHandle<ItemAppleEuphemium> APPLE_EUPHEMIUM =
            Reg.item(
                    "apple_euphemium",
                    ItemAppleEuphemium::new,
                    () ->
                            alwaysEdible(20, 100F)
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final ItemFamily<ItemTemFlakes.Tier, ItemTemFlakes> TEM_FLAKES =
            Reg.family(
                    "tem_flakes",
                    ItemTemFlakes.Tier.class,
                    ItemTemFlakes::new,
                    () -> alwaysEdible(0, 0F));

    public static final RegistryHandle<ItemCustomLore> LEMON =
            Reg.item("lemon", ItemCustomLore::new, () -> food(3, 0.5F)).addTo(MISC_ALL);
    public static final RegistryHandle<Item> DEFINITELY_FOOD =
            Reg.item(
                            "definitelyfood",
                            Item::new,
                            () ->
                                    new Item.Properties()
                                            .food(
                                                    new FoodProperties.Builder()
                                                            .nutrition(3)
                                                            .saturationModifier(0.5F)
                                                            .build()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> LOOPS =
            Reg.item("loops", ItemCustomLore::new, () -> food(4, 0.25F)).addTo(MISC_ALL);
    public static final RegistryHandle<ItemLoopStew> LOOP_STEW =
            Reg.item(
                            "loop_stew",
                            ItemLoopStew::new,
                            () -> food(10, 0.5F).stacksTo(1).usingConvertsTo(Items.BOWL))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> SPONGEBOB_MACARONI =
            Reg.item("spongebob_macaroni", Item::new, () -> food(5, 1F)).addTo(MISC_ALL);
    public static final RegistryHandle<Item> FOODITEM =
            Reg.item("fooditem", Item::new, () -> food(2, 5F)).addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> TWINKIE =
            Reg.item(
                            "twinkie",
                            ItemCustomLore::new,
                            () ->
                                    new Item.Properties()
                                            .food(
                                                    new FoodProperties.Builder()
                                                            .nutrition(3)
                                                            .saturationModifier(0.25F)
                                                            .build()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> STATIC_SANDWICH =
            Reg.item("static_sandwich", Item::new, () -> food(6, 1F)).addTo(MISC_ALL);
    public static final RegistryHandle<ItemCustomLore> PUDDING =
            Reg.item(
                            "pudding",
                            ItemCustomLore::new,
                            () ->
                                    new Item.Properties()
                                            .food(
                                                    new FoodProperties.Builder()
                                                            .nutrition(6)
                                                            .saturationModifier(1F)
                                                            .build()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemPancake> PANCAKE =
            Reg.item(
                            "pancake",
                            ItemPancake::new,
                            () -> new Item.Properties().food(ItemPancake.FOOD))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> NUGGET =
            Reg.item("nugget", Item::new, () -> food(200, 1F)).addTo(MISC_ALL);
    public static final RegistryHandle<ItemPeas> PEAS =
            Reg.item("peas", ItemPeas::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemMarshmallow> MARSHMALLOW =
            Reg.item(
                            "marshmallow",
                            ItemMarshmallow::new,
                            () ->
                                    new Item.Properties()
                                            .stacksTo(1)
                                            .component(ModDataComponents.ROASTED.get(), false))
                    .state(ModDataComponents.ROASTED)
                    .addTo(MISC_ALL);
    public static final RegistryHandle<Item> CHEESE =
            Reg.item("cheese", Item::new, () -> food(5, 0.75F)).addTo(MISC_ALL);

    public static final RegistryHandle<ItemCustomLore> CHEESE_QUESADILLA =
            Reg.item("cheese_quesadilla", ItemCustomLore::new, () -> food(8, 1F)).addTo(MISC_ALL);

    public static final RegistryHandle<ItemEmetic> MED_IPECAC =
            Reg.item("med_ipecac", ItemEmetic::new, () -> alwaysEdible(0, 0F)).addTo(MISC_ALL);
    public static final RegistryHandle<ItemEmetic> MED_PTSD =
            Reg.item("med_ptsd", ItemEmetic::new, () -> alwaysEdible(0, 0F)).addTo(MISC_ALL);
    public static final RegistryHandle<Item> EGG_GLYPHID =
            Reg.item("egg_glyphid", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCanteen> CANTEEN_VODKA =
            Reg.item(
                            "canteen_vodka",
                            ItemCanteen::new,
                            () ->
                                    new Item.Properties()
                                            .component(ModDataComponents.CANTEEN_COOLDOWN.get(), 0)
                                            .component(
                                                    DataComponents.CONSUMABLE,
                                                    ItemCanteen.CONSUMABLE))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<ItemMuchoMango> MUCHO_MANGO =
            Reg.item(
                            "mucho_mango",
                            ItemMuchoMango::new,
                            () ->
                                    alwaysEdible(10, 0.6F)
                                            .component(
                                                    DataComponents.CONSUMABLE,
                                                    Consumables.defaultDrink()
                                                            .consumeSeconds(
                                                                    ItemMuchoMango.DRINK_TICKS
                                                                            / (float)
                                                                                    SharedConstants
                                                                                            .TICKS_PER_SECOND)
                                                            .build()))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemPill> CHOCOLATE =
            pill("chocolate", ItemPill.Type.CHOCOLATE, "chocolate");
    public static final RegistryHandle<Item> CAN_EMPTY =
            Reg.item("can_empty", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);

    public static RegistryHandle<ItemDrinkEnergy> CAN_CREATURE;
    public static RegistryHandle<ItemDrinkEnergy> CAN_SMART;
    public static RegistryHandle<ItemDrinkEnergy> CAN_REDBOMB;
    public static RegistryHandle<ItemDrinkEnergy> CAN_MRSUGAR;
    public static RegistryHandle<ItemDrinkEnergy> CAN_OVERCHARGE;
    public static RegistryHandle<ItemDrinkEnergy> CAN_LUNA;
    public static RegistryHandle<ItemDrinkEnergy> CAN_BEPIS;
    public static RegistryHandle<ItemDrinkEnergy> CAN_BREEN;
    public static RegistryHandle<ItemDrinkEnergy> CAN_MUG;
    public static RegistryHandle<ItemDrinkEnergy> BOTTLE_SPARKLE;
    public static RegistryHandle<ItemDrinkEnergy> BOTTLE_RAD;
    public static RegistryHandle<ItemDrinkEnergy> BOTTLE2_KORL;
    public static RegistryHandle<ItemDrinkEnergy> BOTTLE2_FRITZ;
    public static RegistryHandle<ItemDrinkEnergy> BOTTLE_NUKA;
    public static RegistryHandle<ItemDrinkEnergy> BOTTLE_CHERRY;
    public static RegistryHandle<ItemDrinkEnergy> BOTTLE_QUANTUM;

    static {
        Identifier ringPull = Library.id("ring_pull");

        MISC_ALL.add(
                CAN_SMART =
                        Reg.item(
                                "can_smart",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player -> {
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.STRENGTH,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    0));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));
        MISC_ALL.add(
                CAN_CREATURE =
                        Reg.item(
                                "can_creature",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player -> {
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    0));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.REGENERATION,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));

        MISC_ALL.add(
                CAN_REDBOMB =
                        Reg.item(
                                "can_redbomb",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player -> {
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    0));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.ABSORPTION,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.JUMP_BOOST,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));
        MISC_ALL.add(
                CAN_MRSUGAR =
                        Reg.item(
                                "can_mrsugar",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player -> {
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    0));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.HASTE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.JUMP_BOOST,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));
        MISC_ALL.add(
                CAN_OVERCHARGE =
                        Reg.item(
                                "can_overcharge",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player -> {
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.STRENGTH,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    0));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));
        MISC_ALL.add(
                CAN_LUNA =
                        Reg.item(
                                "can_luna",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player -> {
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.STRENGTH,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.REGENERATION,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));
        MISC_ALL.add(
                CAN_BEPIS =
                        Reg.item(
                                "can_bepis",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player -> {
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    3));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    3));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));

        MISC_ALL.add(
                CAN_BREEN =
                        Reg.item(
                                "can_breen",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player ->
                                                        player.addEffect(
                                                                new MobEffectInstance(
                                                                        MobEffects.NAUSEA,
                                                                        30
                                                                                * SharedConstants
                                                                                        .TICKS_PER_SECOND,
                                                                        0))),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));
        MISC_ALL.add(
                CAN_MUG =
                        Reg.item(
                                "can_mug",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                ringPull,
                                                false,
                                                player -> {
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    3
                                                                            * SharedConstants
                                                                                    .TICKS_PER_MINUTE,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.REGENERATION,
                                                                    SharedConstants
                                                                            .TICKS_PER_MINUTE,
                                                                    2));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(CAN_EMPTY.get())));
    }

    public static final RegistryHandle<ItemDrinkEnergy> COFFEE =
            Reg.item(
                            "coffee",
                            props ->
                                    new ItemDrinkEnergy(
                                            props,
                                            null,
                                            false,
                                            player -> {
                                                player.heal(10F);
                                                player.addEffect(
                                                        new MobEffectInstance(
                                                                MobEffects.SPEED,
                                                                60
                                                                        * SharedConstants
                                                                                .TICKS_PER_SECOND,
                                                                2));
                                            }),
                            () ->
                                    new Item.Properties()
                                            .component(
                                                    DataComponents.CONSUMABLE,
                                                    Consumables.DEFAULT_DRINK))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemDrinkEnergy> COFFEE_RADIUM =
            Reg.item(
                            "coffee_radium",
                            props ->
                                    new ItemDrinkEnergy(
                                            props,
                                            null,
                                            false,
                                            player -> {
                                                player.heal(10F);
                                                player.addEffect(
                                                        new MobEffectInstance(
                                                                MobEffects.SPEED,
                                                                60
                                                                        * SharedConstants
                                                                                .TICKS_PER_SECOND,
                                                                2));
                                                HbmLivingProps.incrementRadiation(player, 500D);
                                            }),
                            () ->
                                    new Item.Properties()
                                            .component(
                                                    DataComponents.CONSUMABLE,
                                                    Consumables.DEFAULT_DRINK))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<Item> BOTTLE_EMPTY =
            Reg.item("bottle_empty", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> BOTTLE2_EMPTY =
            Reg.item("bottle2_empty", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);

    static {
        Identifier capNuka = Library.id("cap_nuka");
        Identifier capQuantum = Library.id("cap_quantum");
        MISC_ALL.add(
                BOTTLE_NUKA =
                        Reg.item(
                                "bottle_nuka",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                capNuka,
                                                true,
                                                player -> {
                                                    player.heal(4F);
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.HASTE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    ContaminationUtil.contaminate(
                                                            player,
                                                            HazardType.RADIATION,
                                                            ContaminationType.RAD_BYPASS,
                                                            5.0D);
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(BOTTLE_EMPTY.get())));
        MISC_ALL.add(
                BOTTLE_CHERRY =
                        Reg.item(
                                "bottle_cherry",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                capNuka,
                                                true,
                                                player -> {
                                                    player.heal(6F);
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    0));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.JUMP_BOOST,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    ContaminationUtil.contaminate(
                                                            player,
                                                            HazardType.RADIATION,
                                                            ContaminationType.RAD_BYPASS,
                                                            5.0D);
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(BOTTLE_EMPTY.get())));
        MISC_ALL.add(
                BOTTLE_QUANTUM =
                        Reg.item(
                                "bottle_quantum",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                capQuantum,
                                                true,
                                                player -> {
                                                    player.heal(10F);
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.STRENGTH,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    ContaminationUtil.contaminate(
                                                            player,
                                                            HazardType.RADIATION,
                                                            ContaminationType.RAD_BYPASS,
                                                            15.0D);
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(BOTTLE_EMPTY.get())));
        MISC_ALL.add(
                BOTTLE_SPARKLE =
                        Reg.item(
                                "bottle_sparkle",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                Library.id("cap_sparkle"),
                                                true,
                                                player -> {
                                                    player.heal(10F);
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.STRENGTH,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.HASTE,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    ContaminationUtil.contaminate(
                                                            player,
                                                            HazardType.RADIATION,
                                                            ContaminationType.RAD_BYPASS,
                                                            5.0D);
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(BOTTLE_EMPTY.get())));
        MISC_ALL.add(
                BOTTLE_RAD =
                        Reg.item(
                                "bottle_rad",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                Library.id("cap_rad"),
                                                true,
                                                player -> {
                                                    player.heal(10F);
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.FIRE_RESISTANCE,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    0));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.STRENGTH,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    4));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.HASTE,
                                                                    120
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    ContaminationUtil.contaminate(
                                                            player,
                                                            HazardType.RADIATION,
                                                            ContaminationType.RAD_BYPASS,
                                                            15.0D);
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(BOTTLE_EMPTY.get())));
        MISC_ALL.add(
                BOTTLE2_KORL =
                        Reg.item(
                                "bottle2_korl",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                Library.id("cap_korl"),
                                                true,
                                                player -> {
                                                    player.heal(6F);
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.HASTE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.STRENGTH,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(BOTTLE2_EMPTY.get())));
        MISC_ALL.add(
                BOTTLE2_FRITZ =
                        Reg.item(
                                "bottle2_fritz",
                                props ->
                                        new ItemDrinkEnergy(
                                                props,
                                                Library.id("cap_fritz"),
                                                true,
                                                player -> {
                                                    player.heal(6F);
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.SPEED,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    1));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.RESISTANCE,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                    player.addEffect(
                                                            new MobEffectInstance(
                                                                    MobEffects.JUMP_BOOST,
                                                                    30
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND,
                                                                    2));
                                                }),
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        DataComponents.CONSUMABLE,
                                                        Consumables.DEFAULT_DRINK)
                                                .craftRemainder(BOTTLE2_EMPTY.get())));
    }

    public static final RegistryHandle<ItemBottleOpener> BOTTLE_OPENER =
            Reg.item(
                            "bottle_opener",
                            ItemBottleOpener::new,
                            () ->
                                    ItemSwordAbility.swordProperties(
                                                    ToolTier.BOTTLE_OPENER,
                                                    4.5F,
                                                    0,
                                                    MaterialShapes.PLATE.tagFor("steel"))
                                            .stacksTo(1))
                    .addTo(MISC_ALL);
    public static final ItemFamily<ItemConserve.EnumFoodType, ItemConserve> CANNED_CONSERVE =
            Reg.family(
                    ItemConserve.EnumFoodType.class,
                    type -> "canned_" + type.name().toLowerCase(Locale.ROOT),
                    ItemConserve::new,
                    () ->
                            new Item.Properties()
                                    .component(
                                            DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD));

    public static final RegistryHandle<ItemDrinkEnergy> CHOCOLATE_MILK =
            Reg.item(
                            "chocolate_milk",
                            props ->
                                    new ItemDrinkEnergy(
                                            props,
                                            null,
                                            false,
                                            player ->
                                                    ExplosionLarge.explode(
                                                            player.level(),
                                                            player.getX(),
                                                            player.getY(),
                                                            player.getZ(),
                                                            50F,
                                                            true,
                                                            false,
                                                            false)),
                            () ->
                                    new Item.Properties()
                                            .component(
                                                    DataComponents.CONSUMABLE,
                                                    Consumables.DEFAULT_DRINK))
                    .addTo(MISC_ALL);

    public static final RegistryHandle<Item> CAP_NUKA =
            Reg.item("cap_nuka", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> CAP_QUANTUM =
            Reg.item("cap_quantum", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> CAP_SPARKLE =
            Reg.item("cap_sparkle", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> CAP_RAD =
            Reg.item("cap_rad", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> CAP_KORL =
            Reg.item("cap_korl", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> CAP_FRITZ =
            Reg.item("cap_fritz", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> RING_PULL =
            Reg.item("ring_pull", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> CAN_KEY =
            Reg.item("can_key", Item::new, Item.Properties::new).addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> COIN_CREEPER =
            Reg.item(
                            "coin_creeper",
                            ItemCustomLore::new,
                            () -> new Item.Properties().rarity(Rarity.UNCOMMON))
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> COIN_RADIATION =
            Reg.item(
                            "coin_radiation",
                            ItemCustomLore::new,
                            () -> new Item.Properties().rarity(Rarity.UNCOMMON))
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> COIN_MASKMAN =
            Reg.item(
                            "coin_maskman",
                            ItemCustomLore::new,
                            () -> new Item.Properties().rarity(Rarity.UNCOMMON))
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> COIN_WORM =
            Reg.item(
                            "coin_worm",
                            ItemCustomLore::new,
                            () -> new Item.Properties().rarity(Rarity.UNCOMMON))
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> COIN_UFO =
            Reg.item(
                            "coin_ufo",
                            ItemCustomLore::new,
                            () -> new Item.Properties().rarity(Rarity.UNCOMMON))
                    .addTo(COLLECTIBLE_ALL);

    public static final RegistryHandle<ItemCustomLore> COIN_TOKEN =
            Reg.item("coin_token", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemModMedal> MEDAL_LIQUIDATOR =
            Reg.item("medal_liquidator", ItemModMedal::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);

    public static final RegistryHandle<ItemModCloud> BOTTLED_CLOUD =
            Reg.item("bottled_cloud", ItemModCloud::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModCharm> PROTECTION_CHARM =
            Reg.item("protection_charm", ItemModCharm::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModCharm> METEOR_CHARM =
            Reg.item("meteor_charm", ItemModCharm::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModLens> NEUTRINO_LENS =
            Reg.item("neutrino_lens", ItemModLens::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);

    public static final RegistryHandle<ItemToolBox> TOOLBOX =
            Reg.item("toolbox", ItemToolBox::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemAmmoBag> AMMO_BAG =
            Reg.item(
                    "ammo_bag",
                    props -> new ItemAmmoBag(props, false),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemAmmoBag> AMMO_BAG_INFINITE =
            Reg.item(
                    "ammo_bag_infinite",
                    props -> new ItemAmmoBag(props, true),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemCasingBag> CASING_BAG =
            Reg.item("casing_bag", ItemCasingBag::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemLeadBox> CONTAINMENT_BOX =
            Reg.item("containment_box", ItemLeadBox::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemModSensor> GAS_TESTER =
            Reg.item("gas_tester", ItemModSensor::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModDefuser> DEFUSER_GOLD =
            Reg.item("defuser_gold", ItemModDefuser::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModTwoKick> BALLISTIC_GAUNTLET =
            Reg.item("ballistic_gauntlet", ItemModTwoKick::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModNightVision> NIGHT_VISION =
            Reg.item("night_vision", ItemModNightVision::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModCard> CARD_AOS =
            Reg.item("card_aos", props -> new ItemModCard(props, false), Item.Properties::new);
    public static final RegistryHandle<ItemModCard> CARD_QOS =
            Reg.item("card_qos", props -> new ItemModCard(props, true), Item.Properties::new);
    public static final RegistryHandle<ItemModShield> AUSTRALIUM_III =
            Reg.item("australium_iii", props -> new ItemModShield(props, 25F), Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModBattery> ARMOR_BATTERY =
            armorBattery("armor_battery", 1.25D);
    public static final RegistryHandle<ItemModBattery> ARMOR_BATTERY_MK2 =
            armorBattery("armor_battery_mk2", 1.5D);
    public static final RegistryHandle<ItemModBattery> ARMOR_BATTERY_MK3 =
            armorBattery("armor_battery_mk3", 2D);
    public static final RegistryHandle<ItemModGasmask> ATTACHMENT_MASK =
            Reg.item(
                            "attachment_mask",
                            p -> new ItemModGasmask(p, ItemModGasmask.Variant.STANDARD),
                            Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModGasmask> ATTACHMENT_MASK_MONO =
            Reg.item(
                            "attachment_mask_mono",
                            p -> new ItemModGasmask(p, ItemModGasmask.Variant.MONO),
                            Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModTesla> BACK_TESLA =
            Reg.item("back_tesla", ItemModTesla::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModServos> SERVO_SET =
            Reg.item(
                            "servo_set",
                            props -> new ItemModServos(props, ItemModServos.Tier.STANDARD),
                            Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModServos> SERVO_SET_DESH =
            Reg.item(
                            "servo_set_desh",
                            props -> new ItemModServos(props, ItemModServos.Tier.DESH),
                            Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModPads> PADS_RUBBER = pads("pads_rubber", 0.5F);
    public static final RegistryHandle<ItemModPads> PADS_SLIME = pads("pads_slime", 0.25F);
    public static final RegistryHandle<ItemModPads> PADS_STATIC = pads("pads_static", 0.75F);
    public static final RegistryHandle<ItemModCladding> CLADDING_PAINT =
            cladding("cladding_paint", 0.025D);
    public static final RegistryHandle<ItemModCladding> CLADDING_RUBBER =
            cladding("cladding_rubber", 0.005D);
    public static final RegistryHandle<ItemModCladding> CLADDING_LEAD =
            cladding("cladding_lead", 0.1D);
    public static final RegistryHandle<ItemModCladding> CLADDING_DESH =
            cladding("cladding_desh", 0.2D);
    public static final RegistryHandle<ItemModCladding> CLADDING_GHIORSIUM =
            cladding("cladding_ghiorsium", 0.5D);
    public static final RegistryHandle<ItemModIron> CLADDING_IRON =
            Reg.item("cladding_iron", ItemModIron::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModObsidian> CLADDING_OBSIDIAN =
            Reg.item("cladding_obsidian", ItemModObsidian::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModInsert> INSERT_KEVLAR =
            insert("insert_kevlar", 1500, 1F, 0.9F, 1F, 1F);
    public static final RegistryHandle<ItemModInsert> INSERT_SAPI =
            insert("insert_sapi", 1750, 1F, 0.85F, 1F, 1F);
    public static final RegistryHandle<ItemModInsert> INSERT_ESAPI =
            insert("insert_esapi", 2000, 0.95F, 0.8F, 1F, 1F);
    public static final RegistryHandle<ItemModInsert> INSERT_XSAPI =
            insert("insert_xsapi", 2500, 0.9F, 0.75F, 1F, 1F);
    public static final RegistryHandle<ItemModInsert> INSERT_STEEL =
            insert("insert_steel", 1000, 1F, 0.95F, 0.75F, 0.95F);
    public static final RegistryHandle<ItemModInsert> INSERT_DU =
            insert("insert_du", 1500, 0.9F, 0.85F, 0.5F, 0.9F);
    public static final RegistryHandle<ItemModInsert> INSERT_POLONIUM =
            insert("insert_polonium", 500, 0.9F, 1F, 0.95F, 0.9F);
    public static final RegistryHandle<ItemModInsert> INSERT_GHIORSIUM =
            insert("insert_ghiorsium", 2000, 0.8F, 0.75F, 0.35F, 0.9F);
    public static final RegistryHandle<ItemModInsert> INSERT_ERA =
            insert("insert_era", 25, 0.5F, 1F, 0.25F, 1F);
    public static final RegistryHandle<ItemModInsert> INSERT_YHARONITE =
            insert("insert_yharonite", 9999, 0.01F, 1F, 1F, 1F);
    public static final RegistryHandle<ItemModInsert> INSERT_DOXIUM =
            insert("insert_doxium", 9999, 5.0F, 1F, 1F, 1F);
    public static final RegistryHandle<ItemCigarette> CIGARETTE =
            Reg.item(
                            "cigarette",
                            props ->
                                    new ItemCigarette(
                                            props,
                                            player -> {
                                                HbmLivingProps.incrementBlackLung(player, 2000);
                                                HbmLivingProps.incrementAsbestos(player, 2000);
                                                HbmLivingProps.incrementRadiation(player, 100D);
                                            }),
                            () -> new Item.Properties().stacksTo(16))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemCigarette> CRACKPIPE =
            Reg.item(
                            "crackpipe",
                            props ->
                                    new ItemCigarette(
                                            props,
                                            player -> {
                                                HbmLivingProps.incrementBlackLung(player, 500);
                                                player.addEffect(
                                                        new MobEffectInstance(
                                                                MobEffects.NAUSEA,
                                                                10
                                                                        * SharedConstants
                                                                                .TICKS_PER_SECOND,
                                                                0));
                                                player.heal(10F);
                                            }),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemBDCL> BDCL =
            Reg.item(
                            "bdcl",
                            ItemBDCL::new,
                            () ->
                                    new Item.Properties()
                                            .component(
                                                    DataComponents.CONSUMABLE, ItemBDCL.CONSUMABLE))
                    .addTo(MISC_ALL);

    private static Item.Properties jetpackProps() {
        return new Item.Properties()
                .stacksTo(1)
                .component(
                        DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).build());
    }

    public static final RegistryHandle<ItemJetpack> JETPACK_FLY =
            Reg.item(
                    "jetpack_fly",
                    props ->
                            new ItemJetpack(
                                    props,
                                    () -> NTMFluids.KEROSENE,
                                    12_000,
                                    ArmorSuitEffects.Jetpack.FLY),
                    ModItems::jetpackProps);
    public static final RegistryHandle<ItemJetpack> JETPACK_BREAK =
            Reg.item(
                    "jetpack_break",
                    props ->
                            new ItemJetpack(
                                    props,
                                    () -> NTMFluids.KEROSENE,
                                    12_000,
                                    ArmorSuitEffects.Jetpack.BRAKE),
                    ModItems::jetpackProps);
    public static final RegistryHandle<ItemJetpack> JETPACK_VECTOR =
            Reg.item(
                    "jetpack_vector",
                    props ->
                            new ItemJetpack(
                                    props,
                                    () -> NTMFluids.KEROSENE,
                                    16_000,
                                    ArmorSuitEffects.Jetpack.VECTOR),
                    ModItems::jetpackProps);
    public static final RegistryHandle<ItemJetpack> JETPACK_BOOST =
            Reg.item(
                    "jetpack_boost",
                    props ->
                            new ItemJetpack(
                                    props,
                                    () -> NTMFluids.BALEFIRE_FUEL,
                                    32_000,
                                    ArmorSuitEffects.Jetpack.BOOST),
                    ModItems::jetpackProps);
    public static final RegistryHandle<ItemWings> WINGS_LIMP =
            Reg.item(
                    "wings_limp",
                    props -> new ItemWings(props, ItemWings.Kind.LIMP),
                    ModItems::jetpackProps);
    public static final RegistryHandle<ItemWings> WINGS_MURK =
            Reg.item(
                    "wings_murk",
                    props -> new ItemWings(props, ItemWings.Kind.MURK),
                    ModItems::jetpackProps);
    public static final RegistryHandle<ItemModPolish> ARMOR_POLISH =
            Reg.item("armor_polish", ItemModPolish::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModBandaid> BANDAID =
            Reg.item("bandaid", ItemModBandaid::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModSerum> SERUM =
            Reg.item("serum", ItemModSerum::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModQuartz> QUARTZ_PLUTONIUM =
            Reg.item("quartz_plutonium", ItemModQuartz::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModMorningGlory> MORNING_GLORY =
            Reg.item("morning_glory", ItemModMorningGlory::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModLodestone> LODESTONE = magnet("lodestone", 5);
    public static final RegistryHandle<ItemModLodestone> HORSESHOE_MAGNET =
            magnet("horseshoe_magnet", 8);
    public static final RegistryHandle<ItemModLodestone> INDUSTRIAL_MAGNET =
            magnet("industrial_magnet", 12);
    public static final RegistryHandle<ItemModBathwater> BATHWATER =
            Reg.item(
                            "bathwater",
                            p -> new ItemModBathwater(p, ItemModBathwater.Grade.STANDARD),
                            Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModBathwater> BATHWATER_MK2 =
            Reg.item(
                            "bathwater_mk2",
                            p -> new ItemModBathwater(p, ItemModBathwater.Grade.MK2),
                            Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModMilk> SPIDER_MILK =
            Reg.item("spider_milk", ItemModMilk::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModInk> INK =
            Reg.item("ink", ItemModInk::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModHealth> HEART_PIECE =
            Reg.item("heart_piece", props -> new ItemModHealth(props, 5F), Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModHealth> HEART_CONTAINER =
            Reg.item(
                            "heart_container",
                            props -> new ItemModHealth(props, 20F),
                            Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModHealth> HEART_BOOSTER =
            Reg.item("heart_booster", props -> new ItemModHealth(props, 40F), Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModHealth> HEART_FAB =
            Reg.item("heart_fab", props -> new ItemModHealth(props, 60F), Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModHealth> BLACK_DIAMOND =
            Reg.item("black_diamond", props -> new ItemModHealth(props, 40F), Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final ItemFamily<ItemFlask.EnumInfusion, ItemFlask> FLASK_INFUSION =
            Reg.family(
                    "flask_infusion",
                    ItemFlask.EnumInfusion.class,
                    ItemFlask::new,
                    () ->
                            new Item.Properties()
                                    .component(DataComponents.CONSUMABLE, ItemFlask.CONSUMABLE));

    public static final RegistryHandle<ItemModWD40> WD40 =
            Reg.item("wd40", ItemModWD40::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModRevive> SCRUMPY =
            Reg.item("scrumpy", props -> new ItemModRevive(props, 1), Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModRevive> WILD_P =
            Reg.item("wild_p", props -> new ItemModRevive(props, 3), Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModShackles> SHACKLES =
            Reg.item("shackles", ItemModShackles::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModAuto> INJECTOR_5HTP =
            Reg.item("injector_5htp", ItemModAuto::new, Item.Properties::new).addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<ItemModKnife> INJECTOR_KNIFE =
            Reg.item("injector_knife", ItemModKnife::new, Item.Properties::new)
                    .addTo(ARMOR_MOD_ALL);
    public static final RegistryHandle<Item> EGG_BALEFIRE_SHARD =
            Reg.item("egg_balefire_shard", Item::new, () -> new Item.Properties().stacksTo(16))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<Item> EGG_BALEFIRE =
            Reg.item("egg_balefire", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    private static final ArmorMaterial IRON_MAT = vanilla(ArmorMaterials.IRON, "mask");
    public static final RegistryHandle<ModArmorItem> GOGGLES =
            Reg.item("goggles", p -> new ModArmorItem(p, Suit.NONE), ModItems::bespokeHeadArmor)
                    .addTo(ARMOR_ALL);
    public static final RegistryHandle<ModArmorItem> ASHGLASSES =
            Reg.item("ashglasses", p -> new ModArmorItem(p, Suit.NONE), ModItems::bespokeHeadArmor)
                    .addTo(ARMOR_ALL);
    public static final RegistryHandle<ItemGasMask> GAS_MASK =
            Reg.item(
                            "gas_mask",
                            p -> new ItemGasMask(p, ItemGasMask.Type.GAS_MASK),
                            ModItems::bespokeHeadArmor)
                    .addTo(ARMOR_ALL);

    private static final ArmorMaterial RAG_DAMP_MAT =
            material("rag_damp", 150, 1, 1, 1, 1, 0, armorRepair("rags"));
    private static final ArmorMaterial RAG_PISS_MAT =
            material("rag_piss", 150, 1, 1, 1, 1, 0, armorRepair("rags"));
    public static final RegistryHandle<ItemGasMask> GAS_MASK_M65 =
            Reg.item(
                            "gas_mask_m65",
                            p -> new ItemGasMask(p, ItemGasMask.Type.M65),
                            ModItems::bespokeHeadArmor)
                    .addTo(ARMOR_ALL);
    public static final RegistryHandle<ItemGasMask> GAS_MASK_MONO =
            Reg.item(
                            "gas_mask_mono",
                            p -> new ItemGasMask(p, ItemGasMask.Type.MONO),
                            ModItems::bespokeHeadArmor)
                    .addTo(ARMOR_ALL);
    public static final RegistryHandle<ItemGasMask> GAS_MASK_OLDE =
            Reg.item(
                            "gas_mask_olde",
                            p -> new ItemGasMask(p, ItemGasMask.Type.OLDE),
                            ModItems::bespokeHeadArmor)
                    .addTo(ARMOR_ALL);
    public static final RegistryHandle<ModArmorItem> MASK_RAG =
            Reg.item(
                            "mask_rag",
                            p -> new ModArmorItem(p, Suit.NONE),
                            () -> humanoidArmorProps(RAG_DAMP_MAT, ArmorType.HELMET, Suit.NONE))
                    .addTo(ARMOR_ALL);
    public static final RegistryHandle<ModArmorItem> MASK_PISS =
            Reg.item(
                            "mask_piss",
                            p -> new ModArmorItem(p, Suit.NONE),
                            () -> humanoidArmorProps(RAG_PISS_MAT, ArmorType.HELMET, Suit.NONE))
                    .addTo(ARMOR_ALL);
    private static final ArmorMaterial ALLOY_MAT =
            material("alloy", 40, 3, 8, 6, 3, 12, armorRepair("alloy"));
    public static final RegistryHandle<ItemHat> NOSSY_HAT =
            Reg.item("nossy_hat", ItemHat::new, () -> bespokeHeadArmor(ALLOY_MAT)).addTo(ARMOR_ALL);
    public static final RegistryHandle<ModArmorItem> NO9 =
            Reg.item(
                            "no9",
                            p -> new ModArmorItem(p, Suit.NONE),
                            () -> undamageableArmor(STEEL_MAT, ArmorType.HELMET, Suit.NONE))
                    .addTo(ARMOR_ALL);

    public static final RegistryHandle<ItemBetaFeatures> BETA =
            Reg.item("beta", ItemBetaFeatures::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemCustomLore> CUSTOM_TNT =
            Reg.item("custom_tnt", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> CUSTOM_NUKE =
            Reg.item("custom_nuke", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> CUSTOM_HYDRO =
            Reg.item("custom_hydro", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> CUSTOM_AMAT =
            Reg.item("custom_amat", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> CUSTOM_DIRTY =
            Reg.item("custom_dirty", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> CUSTOM_SCHRAB =
            Reg.item("custom_schrab", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> CUSTOM_FALL =
            Reg.item("custom_fall", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemCustomLore> IGNITER =
            Reg.item("igniter", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemDetonator> DETONATOR =
            Reg.item("detonator", ItemDetonator::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemMultiDetonator> DETONATOR_MULTI =
            Reg.item(
                            "detonator_multi",
                            ItemMultiDetonator::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemLaserDetonator> LASER_DETONATOR =
            Reg.item(
                            "detonator_laser",
                            ItemLaserDetonator::new,
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemDeadManDetonator> DETONATOR_DEADMAN =
            Reg.item(
                            "detonator_deadman",
                            props -> new ItemDeadManDetonator(props, true),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemDeadManDetonator> DETONATOR_DE =
            Reg.item(
                            "detonator_de",
                            props -> new ItemDeadManDetonator(props, false),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemStarterKit> MISSILE_KIT =
            Reg.item(
                            "missile_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.MISSILE),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISSILE_ALL);
    public static final RegistryHandle<ItemLootCrate> LOOT_10 =
            lootCrate("loot_10", ItemLootCrate.LIST_10);
    public static final RegistryHandle<ItemLootCrate> LOOT_15 =
            lootCrate("loot_15", ItemLootCrate.LIST_15);
    public static final RegistryHandle<ItemLootCrate> LOOT_MISC =
            lootCrate("loot_misc", ItemLootCrate.LIST_MISC);
    public static final ItemFamily<ItemBombCaller.EnumCallerType, ItemBombCaller> BOMB_CALLER =
            Reg.family(
                            "bomb_caller",
                            ItemBombCaller.EnumCallerType.class,
                            ItemBombCaller::new,
                            () -> new Item.Properties().stacksTo(1))
                    .creative(
                            ItemBombCaller.EnumCallerType.CARPET_BOMBING,
                            ItemBombCaller.EnumCallerType.NAPALM,
                            ItemBombCaller.EnumCallerType.POISON_GAS,
                            ItemBombCaller.EnumCallerType.AGENT_ORANGE,
                            ItemBombCaller.EnumCallerType.ATOMIC_BOMB);

    public static final RegistryHandle<ItemDefuser> DEFUSER =
            Reg.item(
                    "defuser",
                    props -> new ItemDefuser(props, IToolable.ToolType.DEFUSER),
                    () -> new Item.Properties().durability(100).stacksTo(1));
    public static final RegistryHandle<ItemCustomLore> REACHER =
            Reg.item("reacher", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemDyatlov> MELTDOWN_TOOL =
            Reg.item("meltdown_tool", ItemDyatlov::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    private static final ArmorMaterial HAZMAT_MAT =
            material("hazmat", 60, 2, 5, 4, 1, 5, armorRepair("hazmat"));

    private static final ArmorMaterial HAZMAT_RED_MAT =
            material("hazmat_red", 60, 2, 5, 4, 1, 5, armorRepair("hazmat_red"));
    private static final ArmorMaterial PAA_MAT =
            material("paa", 75, 3, 8, 6, 3, 25, armorRepair("paa"));

    private static final ArmorMaterial HAZMAT_PAA_MAT =
            material("hazmat_paa", 75, 3, 8, 6, 3, 25, armorRepair("paa"));
    private static final ArmorMaterial HAZMAT_GREY_MAT =
            material("hazmat_grey", 60, 2, 5, 4, 1, 5, armorRepair("hazmat_grey"));
    private static final ArmorMaterial LIQUIDATOR_MAT =
            material("liquidator", 750, 3, 8, 6, 3, 10, Mats.MAT_LEAD.dict.plate());
    public static final RegistryHandle<ItemCustomLore> WATCH =
            Reg.item("watch", ItemCustomLore::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_HORN =
            Reg.item("crystal_horn", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemCustomLore> CRYSTAL_CHARRED =
            Reg.item("crystal_charred", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<ItemRebarPlacer> REBAR_PLACER =
            Reg.item("rebar_placer", ItemRebarPlacer::new, () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemCMStructure> STRUCTURE_CUSTOMMACHINE =
            Reg.item(
                    "structure_custommachine",
                    ItemCMStructure::new,
                    () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemRodOfDiscord> ROD_OF_DISCORD =
            Reg.item(
                    "rod_of_discord",
                    ItemRodOfDiscord::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemCustomLore> BOOK_SECRET =
            Reg.item("book_secret", ItemCustomLore::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemStarterKit> NUKE_STARTER_KIT =
            Reg.item(
                            "nuke_starter_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.NUKE_STARTER),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemStarterKit> NUKE_ADVANCED_KIT =
            Reg.item(
                            "nuke_advanced_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.NUKE_ADVANCED),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemStarterKit> NUKE_COMMERCIALLY_KIT =
            Reg.item(
                            "nuke_commercially_kit",
                            props ->
                                    new ItemStarterKit(
                                            props, ItemStarterKit.Kind.NUKE_COMMERCIALLY),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemStarterKit> NUKE_ELECTRIC_KIT =
            Reg.item(
                            "nuke_electric_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.NUKE_ELECTRIC),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemStarterKit> GADGET_KIT =
            Reg.item(
                            "gadget_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.GADGET),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> BOY_KIT =
            Reg.item(
                            "boy_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.BOY),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> MAN_KIT =
            Reg.item(
                            "man_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.MAN),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> MIKE_KIT =
            Reg.item(
                            "mike_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.MIKE),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> TSAR_KIT =
            Reg.item(
                            "tsar_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.TSAR),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> PROTOTYPE_KIT =
            Reg.item(
                            "prototype_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.PROTOTYPE),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> FLEIJA_KIT =
            Reg.item(
                            "fleija_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.FLEIJA),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> SOLINIUM_KIT =
            Reg.item(
                            "solinium_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.SOLINIUM),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> MULTI_KIT =
            Reg.item(
                            "multi_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.MULTI),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> CUSTOM_KIT =
            Reg.item(
                            "custom_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.CUSTOM),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(WEAPON_ALL);
    public static final RegistryHandle<ItemStarterKit> HAZMAT_KIT =
            Reg.item(
                            "hazmat_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.HAZMAT),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemStarterKit> HAZMAT_RED_KIT =
            Reg.item(
                            "hazmat_red_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.HAZMAT_RED),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemStarterKit> HAZMAT_GREY_KIT =
            Reg.item(
                            "hazmat_grey_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.HAZMAT_GREY),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemStarterKit> EUPHEMIUM_KIT =
            Reg.item(
                            "euphemium_kit",
                            props -> new ItemStarterKit(props, ItemStarterKit.Kind.EUPHEMIUM),
                            () -> new Item.Properties().stacksTo(1))
                    .addTo(MISC_ALL);
    public static final RegistryHandle<ItemBookOfBoxcars> BOOK_OF =
            Reg.item("book_of_", ItemBookOfBoxcars::new, () -> new Item.Properties().stacksTo(1));
    public static final ItemFamily<EnumPages, Item> PAGE_OF =
            Reg.family(
                    "page_of_",
                    EnumPages.class,
                    (props, type) -> new Item(props),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemCustomLore> BURNT_BARK =
            Reg.item("burnt_bark", ItemCustomLore::new, Item.Properties::new)
                    .addTo(COLLECTIBLE_ALL);
    public static final RegistryHandle<Item> RECORD_LC = record(ModJukeboxSongs.Song.LC);
    public static final RegistryHandle<Item> RECORD_SS = record(ModJukeboxSongs.Song.SS);
    public static final RegistryHandle<Item> RECORD_VC = record(ModJukeboxSongs.Song.VC);

    public static final RegistryHandle<Item> RECORD_GLASS = record(ModJukeboxSongs.Song.GLASS);

    public static final RegistryHandle<ItemBookLore> BOOK_LORE =
            Reg.item("book_lore", ItemBookLore::new, () -> new Item.Properties().stacksTo(1));

    public static final ItemFamily<ItemHolotapeImage.EnumHoloImage, ItemHolotapeImage>
            HOLOTAPE_IMAGE =
                    Reg.family(
                            "holotape_image",
                            ItemHolotapeImage.EnumHoloImage.class,
                            ItemHolotapeImage::new,
                            () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<Item> HOLOTAPE_DAMAGED =
            Reg.item("holotape_damaged", Item::new, Item.Properties::new).addTo(MISC_ALL);
    public static final RegistryHandle<ItemClayTablet> CLAY_TABLET =
            Reg.item(
                    "clay_tablet",
                    props -> new ItemClayTablet(props, 0),
                    () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<ItemClayTablet> CLAY_TABLET_1 =
            Reg.item(
                    "clay_tablet_1",
                    props -> new ItemClayTablet(props, 1),
                    () -> new Item.Properties().stacksTo(1));
    public static final ItemFamily<EnumAchievementType, Item> ACHIEVEMENT_ICON =
            Reg.family(
                    "achievement_icon",
                    EnumAchievementType.class,
                    (props, type) -> new Item(props),
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<ItemMysteryShovel> MYSTERY_SHOVEL =
            Reg.item(
                    "mysteryshovel",
                    ItemMysteryShovel::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> TEMPLATE_FOLDER =
            Reg.item("template_folder", Item::new, () -> new Item.Properties().stacksTo(1))
                    .addTo(COLLECTIBLE_ALL);

    public static final RegistryHandle<BrokenItem> BROKEN_ITEM =
            Reg.item("broken_item", BrokenItem::new, Item.Properties::new);
    private static final Set<NTMMaterial> AUTOGEN_RAW =
            Set.of(
                    Mats.MAT_REDSTONE,
                    Mats.MAT_NEODYMIUM,
                    Mats.MAT_BORAX,
                    Mats.MAT_SODIUM,
                    Mats.MAT_STRONTIUM,
                    Mats.MAT_SLAG);
    private static final Set<NTMMaterial> VANILLA =
            Set.of(Mats.MAT_IRON, Mats.MAT_GOLD, Mats.MAT_COPPER);

    private static final Set<NTMMaterial> DUPLICATE_INGOT =
            Set.of(Mats.MAT_CARBON, Mats.MAT_LITHIUM, Mats.MAT_MALACHITE, Mats.MAT_RAREEARTH);
    private static final Set<NTMMaterial> EXCLUDED =
            union(union(AUTOGEN_RAW, VANILLA), DUPLICATE_INGOT);
    public static final NTMMaterial[] INGOT_MATERIALS =
            MaterialShapeRoster.roster(MaterialShapes.INGOT, EXCLUDED).toArray(new NTMMaterial[0]);

    private static final Set<NTMMaterial> MP_EXCLUDED =
            Set.of(Mats.MAT_FLUORITE, Mats.MAT_STAR, Mats.MAT_FLUX);
    public static final NTMMaterial[] DUST_MATERIALS =
            MaterialShapeRoster.roster(MaterialShapes.DUST, MP_EXCLUDED)
                    .toArray(new NTMMaterial[0]);
    private static final Set<NTMMaterial> MN_EXCLUDED = Set.of(Mats.MAT_IRON, Mats.MAT_GOLD);
    public static final NTMMaterial[] NUGGET_MATERIALS =
            MaterialShapeRoster.roster(MaterialShapes.NUGGET, MN_EXCLUDED)
                    .toArray(new NTMMaterial[0]);
    public static final RegistryHandle<ModArmorItem> STEEL_HELMET =
            trimmableArmor("steel_helmet", STEEL_MAT, ArmorType.HELMET, Suit.STEEL);
    public static final RegistryHandle<ModArmorItem> STEEL_PLATE =
            trimmableArmor("steel_plate", STEEL_MAT, ArmorType.CHESTPLATE, Suit.STEEL);
    public static final RegistryHandle<ModArmorItem> STEEL_LEGS =
            trimmableArmor("steel_legs", STEEL_MAT, ArmorType.LEGGINGS, Suit.STEEL);
    public static final RegistryHandle<ModArmorItem> STEEL_BOOTS =
            trimmableArmor("steel_boots", STEEL_MAT, ArmorType.BOOTS, Suit.STEEL);
    private static final ArmorMaterial TITANIUM_MAT =
            material("titanium", 25, 3, 8, 6, 3, 9, Mats.MAT_TITANIUM.dict.ingot());
    public static final RegistryHandle<ModArmorItem> TITANIUM_HELMET =
            trimmableArmor("titanium_helmet", TITANIUM_MAT, ArmorType.HELMET, Suit.TITANIUM);
    public static final RegistryHandle<ModArmorItem> TITANIUM_PLATE =
            trimmableArmor("titanium_plate", TITANIUM_MAT, ArmorType.CHESTPLATE, Suit.TITANIUM);
    public static final RegistryHandle<ModArmorItem> TITANIUM_LEGS =
            trimmableArmor("titanium_legs", TITANIUM_MAT, ArmorType.LEGGINGS, Suit.TITANIUM);
    public static final RegistryHandle<ModArmorItem> TITANIUM_BOOTS =
            trimmableArmor("titanium_boots", TITANIUM_MAT, ArmorType.BOOTS, Suit.TITANIUM);
    public static final RegistryHandle<ModArmorItem> ALLOY_HELMET =
            trimmableArmor("alloy_helmet", ALLOY_MAT, ArmorType.HELMET, Suit.ALLOY);
    public static final RegistryHandle<ModArmorItem> ALLOY_PLATE =
            trimmableArmor("alloy_plate", ALLOY_MAT, ArmorType.CHESTPLATE, Suit.ALLOY);
    public static final RegistryHandle<ModArmorItem> ALLOY_LEGS =
            trimmableArmor("alloy_legs", ALLOY_MAT, ArmorType.LEGGINGS, Suit.ALLOY);
    public static final RegistryHandle<ModArmorItem> ALLOY_BOOTS =
            trimmableArmor("alloy_boots", ALLOY_MAT, ArmorType.BOOTS, Suit.ALLOY);

    private static final ArmorMaterial DESH_MAT =
            material("steamsuit", 150, 3, 8, 6, 3, 0, Mats.MAT_DESH.dict.ingot());
    public static final int STEAMSUIT_MAX_FUEL = 64_000;
    public static final int STEAMSUIT_FILL_RATE = 500;
    public static final int STEAMSUIT_CONSUMPTION = 50;
    public static final RegistryHandle<ModArmorItemFueled> STEAMSUIT_HELMET =
            armorFueled(
                    "steamsuit_helmet",
                    DESH_MAT,
                    ArmorType.HELMET,
                    Suit.DESH,
                    () -> NTMFluids.STEAM,
                    STEAMSUIT_MAX_FUEL,
                    STEAMSUIT_FILL_RATE,
                    STEAMSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemFueled> STEAMSUIT_PLATE =
            armorFueled(
                    "steamsuit_plate",
                    DESH_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.DESH,
                    () -> NTMFluids.STEAM,
                    STEAMSUIT_MAX_FUEL,
                    STEAMSUIT_FILL_RATE,
                    STEAMSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemFueled> STEAMSUIT_LEGS =
            armorFueled(
                    "steamsuit_legs",
                    DESH_MAT,
                    ArmorType.LEGGINGS,
                    Suit.DESH,
                    () -> NTMFluids.STEAM,
                    STEAMSUIT_MAX_FUEL,
                    STEAMSUIT_FILL_RATE,
                    STEAMSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemFueled> STEAMSUIT_BOOTS =
            armorFueled(
                    "steamsuit_boots",
                    DESH_MAT,
                    ArmorType.BOOTS,
                    Suit.DESH,
                    () -> NTMFluids.STEAM,
                    STEAMSUIT_MAX_FUEL,
                    STEAMSUIT_FILL_RATE,
                    STEAMSUIT_CONSUMPTION);

    private static final ArmorMaterial DIESEL_MAT =
            material("bnuuy", 150, 3, 8, 6, 3, 0, Mats.MAT_COPPER.dict.plate());
    public static final int DIESELSUIT_MAX_FUEL = 64_000;
    public static final int DIESELSUIT_FILL_RATE = 500;
    public static final int DIESELSUIT_CONSUMPTION = 50;
    public static final RegistryHandle<ModArmorItemFueled> DIESELSUIT_HELMET =
            armorFueled(
                    "dieselsuit_helmet",
                    DIESEL_MAT,
                    ArmorType.HELMET,
                    Suit.DIESEL,
                    () -> NTMFluids.DIESEL,
                    DIESELSUIT_MAX_FUEL,
                    DIESELSUIT_FILL_RATE,
                    DIESELSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemFueled> DIESELSUIT_PLATE =
            armorFueled(
                    "dieselsuit_plate",
                    DIESEL_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.DIESEL,
                    () -> NTMFluids.DIESEL,
                    DIESELSUIT_MAX_FUEL,
                    DIESELSUIT_FILL_RATE,
                    DIESELSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemFueled> DIESELSUIT_LEGS =
            armorFueled(
                    "dieselsuit_legs",
                    DIESEL_MAT,
                    ArmorType.LEGGINGS,
                    Suit.DIESEL,
                    () -> NTMFluids.DIESEL,
                    DIESELSUIT_MAX_FUEL,
                    DIESELSUIT_FILL_RATE,
                    DIESELSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemFueled> DIESELSUIT_BOOTS =
            armorFueled(
                    "dieselsuit_boots",
                    DIESEL_MAT,
                    ArmorType.BOOTS,
                    Suit.DIESEL,
                    () -> NTMFluids.DIESEL,
                    DIESELSUIT_MAX_FUEL,
                    DIESELSUIT_FILL_RATE,
                    DIESELSUIT_CONSUMPTION);
    private static final ArmorMaterial T51_MAT =
            material("t51", 150, 3, 8, 6, 3, 0, armorRepair("t51"));

    public static final RegistryHandle<ModArmorItemPowered> T51_HELMET =
            poweredArmor(
                    "t51_helmet",
                    T51_MAT,
                    ArmorType.HELMET,
                    Suit.T51,
                    T51_MAX_POWER,
                    T51_CHARGE_RATE,
                    T51_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> T51_PLATE =
            poweredArmor(
                    "t51_plate",
                    T51_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.T51,
                    T51_MAX_POWER,
                    T51_CHARGE_RATE,
                    T51_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> T51_LEGS =
            poweredArmor(
                    "t51_legs",
                    T51_MAT,
                    ArmorType.LEGGINGS,
                    Suit.T51,
                    T51_MAX_POWER,
                    T51_CHARGE_RATE,
                    T51_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> T51_BOOTS =
            poweredArmor(
                    "t51_boots",
                    T51_MAT,
                    ArmorType.BOOTS,
                    Suit.T51,
                    T51_MAX_POWER,
                    T51_CHARGE_RATE,
                    T51_CONSUMPTION);
    private static final ArmorMaterial AJR_MAT =
            material("ajr", 150, 3, 8, 6, 3, 0, armorRepair("ajr"));
    public static final long AJR_MAX_POWER = 2_500_000L;
    public static final long AJR_CHARGE_RATE = 10_000L;
    public static final long AJR_CONSUMPTION = 2_000L;
    public static final RegistryHandle<ModArmorItemPowered> AJR_HELMET =
            poweredArmor(
                    "ajr_helmet",
                    AJR_MAT,
                    ArmorType.HELMET,
                    Suit.AJR,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> AJR_PLATE =
            poweredArmor(
                    "ajr_plate",
                    AJR_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.AJR,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> AJR_LEGS =
            poweredArmor(
                    "ajr_legs",
                    AJR_MAT,
                    ArmorType.LEGGINGS,
                    Suit.AJR,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> AJR_BOOTS =
            poweredArmor(
                    "ajr_boots",
                    AJR_MAT,
                    ArmorType.BOOTS,
                    Suit.AJR,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> AJRO_HELMET =
            poweredArmor(
                    "ajro_helmet",
                    AJR_MAT,
                    ArmorType.HELMET,
                    Suit.AJRO,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> AJRO_PLATE =
            poweredArmor(
                    "ajro_plate",
                    AJR_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.AJRO,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> AJRO_LEGS =
            poweredArmor(
                    "ajro_legs",
                    AJR_MAT,
                    ArmorType.LEGGINGS,
                    Suit.AJRO,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> AJRO_BOOTS =
            poweredArmor(
                    "ajro_boots",
                    AJR_MAT,
                    ArmorType.BOOTS,
                    Suit.AJRO,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> RPA_HELMET =
            poweredArmor(
                    "rpa_helmet",
                    AJR_MAT,
                    ArmorType.HELMET,
                    Suit.RPA,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> RPA_PLATE =
            poweredArmor(
                    "rpa_plate",
                    AJR_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.RPA,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> RPA_LEGS =
            poweredArmor(
                    "rpa_legs",
                    AJR_MAT,
                    ArmorType.LEGGINGS,
                    Suit.RPA,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> RPA_BOOTS =
            poweredArmor(
                    "rpa_boots",
                    AJR_MAT,
                    ArmorType.BOOTS,
                    Suit.RPA,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> NCRPA_HELMET =
            poweredArmor(
                    "ncrpa_helmet",
                    AJR_MAT,
                    ArmorType.HELMET,
                    Suit.NCRPA,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> NCRPA_PLATE =
            poweredArmor(
                    "ncrpa_plate",
                    AJR_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.NCRPA,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> NCRPA_LEGS =
            poweredArmor(
                    "ncrpa_legs",
                    AJR_MAT,
                    ArmorType.LEGGINGS,
                    Suit.NCRPA,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> NCRPA_BOOTS =
            poweredArmor(
                    "ncrpa_boots",
                    AJR_MAT,
                    ArmorType.BOOTS,
                    Suit.NCRPA,
                    AJR_MAX_POWER,
                    AJR_CHARGE_RATE,
                    AJR_CONSUMPTION);
    private static final ArmorMaterial BJ_MAT =
            material("bj", 150, 3, 8, 6, 3, 0, armorRepair("bj"));
    public static final long BJ_MAX_POWER = 10_000_000L;
    public static final long BJ_CHARGE_RATE = 10_000L;
    public static final long BJ_CONSUMPTION = 1_000L;
    public static final RegistryHandle<ModArmorItemPowered> BJ_HELMET =
            poweredArmor(
                    "bj_helmet",
                    BJ_MAT,
                    ArmorType.HELMET,
                    Suit.BJ,
                    BJ_MAX_POWER,
                    BJ_CHARGE_RATE,
                    BJ_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> BJ_PLATE =
            poweredArmor(
                    "bj_plate",
                    BJ_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.BJ,
                    BJ_MAX_POWER,
                    BJ_CHARGE_RATE,
                    BJ_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> BJ_PLATE_JETPACK =
            poweredArmor(
                    "bj_plate_jetpack",
                    BJ_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.BJ,
                    BJ_MAX_POWER,
                    BJ_CHARGE_RATE,
                    BJ_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> BJ_LEGS =
            poweredArmor(
                    "bj_legs",
                    BJ_MAT,
                    ArmorType.LEGGINGS,
                    Suit.BJ,
                    BJ_MAX_POWER,
                    BJ_CHARGE_RATE,
                    BJ_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> BJ_BOOTS =
            poweredArmor(
                    "bj_boots",
                    BJ_MAT,
                    ArmorType.BOOTS,
                    Suit.BJ,
                    BJ_MAX_POWER,
                    BJ_CHARGE_RATE,
                    BJ_CONSUMPTION);
    private static final ArmorMaterial ENV_MAT =
            material("envsuit", 150, 3, 8, 6, 3, 10, armorRepair("env"));
    public static final long ENVSUIT_MAX_POWER = 100_000L;
    public static final long ENVSUIT_CHARGE_RATE = 1_000L;
    public static final long ENVSUIT_CONSUMPTION = 250L;
    public static final RegistryHandle<ModArmorItemPowered> ENVSUIT_HELMET =
            poweredArmor(
                    "envsuit_helmet",
                    ENV_MAT,
                    ArmorType.HELMET,
                    Suit.ENVSUIT,
                    ENVSUIT_MAX_POWER,
                    ENVSUIT_CHARGE_RATE,
                    ENVSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> ENVSUIT_PLATE =
            poweredArmor(
                    "envsuit_plate",
                    ENV_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.ENVSUIT,
                    ENVSUIT_MAX_POWER,
                    ENVSUIT_CHARGE_RATE,
                    ENVSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> ENVSUIT_LEGS =
            poweredArmor(
                    "envsuit_legs",
                    ENV_MAT,
                    ArmorType.LEGGINGS,
                    Suit.ENVSUIT,
                    ENVSUIT_MAX_POWER,
                    ENVSUIT_CHARGE_RATE,
                    ENVSUIT_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> ENVSUIT_BOOTS =
            poweredArmor(
                    "envsuit_boots",
                    ENV_MAT,
                    ArmorType.BOOTS,
                    Suit.ENVSUIT,
                    ENVSUIT_MAX_POWER,
                    ENVSUIT_CHARGE_RATE,
                    ENVSUIT_CONSUMPTION);
    private static final ArmorMaterial HEV_MAT =
            material("hev", 150, 3, 8, 6, 3, 0, armorRepair("hev"));
    public static final long HEV_MAX_POWER = 1_000_000L;
    public static final long HEV_CHARGE_RATE = 10_000L;
    public static final long HEV_CONSUMPTION = 2_500L;
    public static final RegistryHandle<ModArmorItemPowered> HEV_HELMET =
            poweredArmor(
                    "hev_helmet",
                    HEV_MAT,
                    ArmorType.HELMET,
                    Suit.HEV,
                    HEV_MAX_POWER,
                    HEV_CHARGE_RATE,
                    HEV_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> HEV_PLATE =
            poweredArmor(
                    "hev_plate",
                    HEV_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.HEV,
                    HEV_MAX_POWER,
                    HEV_CHARGE_RATE,
                    HEV_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> HEV_LEGS =
            poweredArmor(
                    "hev_legs",
                    HEV_MAT,
                    ArmorType.LEGGINGS,
                    Suit.HEV,
                    HEV_MAX_POWER,
                    HEV_CHARGE_RATE,
                    HEV_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> HEV_BOOTS =
            poweredArmor(
                    "hev_boots",
                    HEV_MAT,
                    ArmorType.BOOTS,
                    Suit.HEV,
                    HEV_MAX_POWER,
                    HEV_CHARGE_RATE,
                    HEV_CONSUMPTION);
    private static final ArmorMaterial FAU_MAT =
            material("fau", 150, 3, 8, 6, 3, 0, armorRepair("fau"));
    public static final long FAU_MAX_POWER = 10_000_000L;
    public static final long FAU_CHARGE_RATE = 10_000L;
    public static final long FAU_CONSUMPTION = 2_500L;
    public static final RegistryHandle<ModArmorItemPowered> FAU_HELMET =
            poweredArmor(
                    "fau_helmet",
                    FAU_MAT,
                    ArmorType.HELMET,
                    Suit.DIGAMMA,
                    FAU_MAX_POWER,
                    FAU_CHARGE_RATE,
                    FAU_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> FAU_PLATE =
            poweredArmor(
                    "fau_plate",
                    FAU_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.DIGAMMA,
                    FAU_MAX_POWER,
                    FAU_CHARGE_RATE,
                    FAU_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> FAU_LEGS =
            poweredArmor(
                    "fau_legs",
                    FAU_MAT,
                    ArmorType.LEGGINGS,
                    Suit.DIGAMMA,
                    FAU_MAX_POWER,
                    FAU_CHARGE_RATE,
                    FAU_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> FAU_BOOTS =
            poweredArmor(
                    "fau_boots",
                    FAU_MAT,
                    ArmorType.BOOTS,
                    Suit.DIGAMMA,
                    FAU_MAX_POWER,
                    FAU_CHARGE_RATE,
                    FAU_CONSUMPTION);
    private static final ArmorMaterial DNS_MAT =
            material("dns", 150, 3, 8, 6, 3, 0, armorRepair("dns"));
    public static final long DNS_MAX_POWER = 1_000_000_000L;
    public static final long DNS_CHARGE_RATE = 1_000_000L;
    public static final long DNS_CONSUMPTION = 100_000L;
    public static final RegistryHandle<ModArmorItemPowered> DNS_HELMET =
            poweredArmor(
                    "dns_helmet",
                    DNS_MAT,
                    ArmorType.HELMET,
                    Suit.DNS,
                    DNS_MAX_POWER,
                    DNS_CHARGE_RATE,
                    DNS_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> DNS_PLATE =
            poweredArmor(
                    "dns_plate",
                    DNS_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.DNS,
                    DNS_MAX_POWER,
                    DNS_CHARGE_RATE,
                    DNS_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> DNS_LEGS =
            poweredArmor(
                    "dns_legs",
                    DNS_MAT,
                    ArmorType.LEGGINGS,
                    Suit.DNS,
                    DNS_MAX_POWER,
                    DNS_CHARGE_RATE,
                    DNS_CONSUMPTION);
    public static final RegistryHandle<ModArmorItemPowered> DNS_BOOTS =
            poweredArmor(
                    "dns_boots",
                    DNS_MAT,
                    ArmorType.BOOTS,
                    Suit.DNS,
                    DNS_MAX_POWER,
                    DNS_CHARGE_RATE,
                    DNS_CONSUMPTION);
    private static final ArmorMaterial TAURUN_MAT =
            material("taurun", 150, 3, 8, 6, 3, 10, Mats.MAT_IRON.dict.plate());
    public static final RegistryHandle<ModArmorItem> TAURUN_HELMET =
            undamageableArmor("taurun_helmet", TAURUN_MAT, ArmorType.HELMET, Suit.TAURUN);
    public static final RegistryHandle<ModArmorItem> TAURUN_PLATE =
            undamageableArmor("taurun_plate", TAURUN_MAT, ArmorType.CHESTPLATE, Suit.TAURUN);
    public static final RegistryHandle<ModArmorItem> TAURUN_LEGS =
            undamageableArmor("taurun_legs", TAURUN_MAT, ArmorType.LEGGINGS, Suit.TAURUN);
    public static final RegistryHandle<ModArmorItem> TAURUN_BOOTS =
            undamageableArmor("taurun_boots", TAURUN_MAT, ArmorType.BOOTS, Suit.TAURUN);
    private static final ArmorMaterial TRENCHMASTER_MAT =
            material("trenchmaster", 150, 3, 8, 6, 3, 0, Mats.MAT_IRON.dict.plate());
    public static final RegistryHandle<ModArmorItem> TRENCHMASTER_HELMET =
            undamageableArmor(
                    "trenchmaster_helmet", TRENCHMASTER_MAT, ArmorType.HELMET, Suit.TRENCHMASTER);
    public static final RegistryHandle<ModArmorItem> TRENCHMASTER_PLATE =
            undamageableArmor(
                    "trenchmaster_plate",
                    TRENCHMASTER_MAT,
                    ArmorType.CHESTPLATE,
                    Suit.TRENCHMASTER);
    public static final RegistryHandle<ModArmorItem> TRENCHMASTER_LEGS =
            undamageableArmor(
                    "trenchmaster_legs", TRENCHMASTER_MAT, ArmorType.LEGGINGS, Suit.TRENCHMASTER);
    public static final RegistryHandle<ModArmorItem> TRENCHMASTER_BOOTS =
            undamageableArmor(
                    "trenchmaster_boots", TRENCHMASTER_MAT, ArmorType.BOOTS, Suit.TRENCHMASTER);

    public static final RegistryHandle<ItemHazmatMask> HAZMAT_HELMET =
            hazmatMask("hazmat_helmet", HAZMAT_MAT, Suit.HAZMAT, ItemHazmatMask.Variant.STANDARD);
    public static final RegistryHandle<ModArmorItem> HAZMAT_PLATE =
            armor("hazmat_plate", HAZMAT_MAT, ArmorType.CHESTPLATE, Suit.HAZMAT);
    public static final RegistryHandle<ModArmorItem> HAZMAT_LEGS =
            armor("hazmat_legs", HAZMAT_MAT, ArmorType.LEGGINGS, Suit.HAZMAT);
    public static final RegistryHandle<ModArmorItem> HAZMAT_BOOTS =
            armor("hazmat_boots", HAZMAT_MAT, ArmorType.BOOTS, Suit.HAZMAT);
    public static final RegistryHandle<ItemHazmatMask> HAZMAT_HELMET_RED =
            hazmatMask(
                    "hazmat_helmet_red",
                    HAZMAT_RED_MAT,
                    Suit.HAZMAT_RED,
                    ItemHazmatMask.Variant.RED);
    public static final RegistryHandle<ModArmorItem> HAZMAT_PLATE_RED =
            armor("hazmat_plate_red", HAZMAT_RED_MAT, ArmorType.CHESTPLATE, Suit.HAZMAT_RED);
    public static final RegistryHandle<ModArmorItem> HAZMAT_LEGS_RED =
            armor("hazmat_legs_red", HAZMAT_RED_MAT, ArmorType.LEGGINGS, Suit.HAZMAT_RED);
    public static final RegistryHandle<ModArmorItem> HAZMAT_BOOTS_RED =
            armor("hazmat_boots_red", HAZMAT_RED_MAT, ArmorType.BOOTS, Suit.HAZMAT_RED);
    public static final RegistryHandle<ItemHazmatMask> HAZMAT_HELMET_GREY =
            hazmatMask(
                    "hazmat_helmet_grey",
                    HAZMAT_GREY_MAT,
                    Suit.HAZMAT_GREY,
                    ItemHazmatMask.Variant.GREY);
    public static final RegistryHandle<ModArmorItem> HAZMAT_PLATE_GREY =
            armor("hazmat_plate_grey", HAZMAT_GREY_MAT, ArmorType.CHESTPLATE, Suit.HAZMAT_GREY);
    public static final RegistryHandle<ModArmorItem> HAZMAT_LEGS_GREY =
            armor("hazmat_legs_grey", HAZMAT_GREY_MAT, ArmorType.LEGGINGS, Suit.HAZMAT_GREY);
    public static final RegistryHandle<ModArmorItem> HAZMAT_BOOTS_GREY =
            armor("hazmat_boots_grey", HAZMAT_GREY_MAT, ArmorType.BOOTS, Suit.HAZMAT_GREY);

    public static final RegistryHandle<ItemHazmatMask> HAZMAT_PAA_HELMET =
            hazmatMask(
                    "hazmat_paa_helmet",
                    HAZMAT_PAA_MAT,
                    Suit.HAZMAT_PAA,
                    ItemHazmatMask.Variant.STANDARD);
    public static final RegistryHandle<ModArmorItem> HAZMAT_PAA_PLATE =
            armor("hazmat_paa_plate", HAZMAT_PAA_MAT, ArmorType.CHESTPLATE, Suit.HAZMAT_PAA);
    public static final RegistryHandle<ModArmorItem> HAZMAT_PAA_LEGS =
            armor("hazmat_paa_legs", HAZMAT_PAA_MAT, ArmorType.LEGGINGS, Suit.HAZMAT_PAA);
    public static final RegistryHandle<ModArmorItem> HAZMAT_PAA_BOOTS =
            armor("hazmat_paa_boots", HAZMAT_PAA_MAT, ArmorType.BOOTS, Suit.HAZMAT_PAA);
    public static final RegistryHandle<ModArmorItem> LIQUIDATOR_HELMET =
            liquidator("liquidator_helmet", ArmorType.HELMET);
    public static final RegistryHandle<ModArmorItem> LIQUIDATOR_PLATE =
            liquidator("liquidator_plate", ArmorType.CHESTPLATE);
    public static final RegistryHandle<ModArmorItem> LIQUIDATOR_LEGS =
            liquidator("liquidator_legs", ArmorType.LEGGINGS);
    public static final RegistryHandle<ModArmorItem> LIQUIDATOR_BOOTS =
            liquidator("liquidator_boots", ArmorType.BOOTS);
    private static final ArmorMaterial CMB_MAT =
            material("cmb", 60, 3, 8, 6, 3, 50, Mats.MAT_CMB.dict.ingot());
    public static final RegistryHandle<ModArmorItem> CMB_HELMET =
            trimmableArmor("cmb_helmet", CMB_MAT, ArmorType.HELMET, Suit.CMB);
    public static final RegistryHandle<ModArmorItem> CMB_PLATE =
            trimmableArmor("cmb_plate", CMB_MAT, ArmorType.CHESTPLATE, Suit.CMB);
    public static final RegistryHandle<ModArmorItem> CMB_LEGS =
            trimmableArmor("cmb_legs", CMB_MAT, ArmorType.LEGGINGS, Suit.CMB);
    public static final RegistryHandle<ModArmorItem> CMB_BOOTS =
            trimmableArmor("cmb_boots", CMB_MAT, ArmorType.BOOTS, Suit.CMB);
    public static final RegistryHandle<ModArmorItem> PAA_PLATE =
            armor("paa_plate", PAA_MAT, ArmorType.CHESTPLATE, Suit.PAA);
    public static final RegistryHandle<ModArmorItem> PAA_LEGS =
            armor("paa_legs", PAA_MAT, ArmorType.LEGGINGS, Suit.PAA);
    public static final RegistryHandle<ModArmorItem> PAA_BOOTS =
            armor("paa_boots", PAA_MAT, ArmorType.BOOTS, Suit.PAA);
    private static final ArmorMaterial ASBESTOS_MAT =
            material("asbestos", 20, 1, 4, 3, 1, 5, armorRepair("asbestos"));
    public static final RegistryHandle<ModArmorItem> ASBESTOS_HELMET =
            trimmableArmor("asbestos_helmet", ASBESTOS_MAT, ArmorType.HELMET, Suit.ASBESTOS);
    public static final RegistryHandle<ModArmorItem> ASBESTOS_PLATE =
            trimmableArmor("asbestos_plate", ASBESTOS_MAT, ArmorType.CHESTPLATE, Suit.ASBESTOS);
    public static final RegistryHandle<ModArmorItem> ASBESTOS_LEGS =
            trimmableArmor("asbestos_legs", ASBESTOS_MAT, ArmorType.LEGGINGS, Suit.ASBESTOS);
    public static final RegistryHandle<ModArmorItem> ASBESTOS_BOOTS =
            trimmableArmor("asbestos_boots", ASBESTOS_MAT, ArmorType.BOOTS, Suit.ASBESTOS);
    private static final ArmorMaterial SECURITY_MAT =
            material("security", 100, 3, 8, 6, 3, 15, armorRepair("security"));
    public static final RegistryHandle<ModArmorItem> SECURITY_HELMET =
            armor("security_helmet", SECURITY_MAT, ArmorType.HELMET, Suit.SECURITY);
    public static final RegistryHandle<ModArmorItem> SECURITY_PLATE =
            armor("security_plate", SECURITY_MAT, ArmorType.CHESTPLATE, Suit.SECURITY);
    public static final RegistryHandle<ModArmorItem> SECURITY_LEGS =
            armor("security_legs", SECURITY_MAT, ArmorType.LEGGINGS, Suit.SECURITY);
    public static final RegistryHandle<ModArmorItem> SECURITY_BOOTS =
            armor("security_boots", SECURITY_MAT, ArmorType.BOOTS, Suit.SECURITY);
    private static final ArmorMaterial COBALT_MAT =
            material("cobalt", 70, 3, 8, 6, 3, 60, Mats.MAT_COBALT.dict.ingot());
    public static final RegistryHandle<ModArmorItem> COBALT_HELMET =
            trimmableArmor("cobalt_helmet", COBALT_MAT, ArmorType.HELMET, Suit.COBALT);
    public static final RegistryHandle<ModArmorItem> COBALT_PLATE =
            trimmableArmor("cobalt_plate", COBALT_MAT, ArmorType.CHESTPLATE, Suit.COBALT);
    public static final RegistryHandle<ModArmorItem> COBALT_LEGS =
            trimmableArmor("cobalt_legs", COBALT_MAT, ArmorType.LEGGINGS, Suit.COBALT);
    public static final RegistryHandle<ModArmorItem> COBALT_BOOTS =
            trimmableArmor("cobalt_boots", COBALT_MAT, ArmorType.BOOTS, Suit.COBALT);
    private static final ArmorMaterial STARMETAL_MAT =
            material("starmetal", 150, 3, 8, 6, 3, 100, Mats.MAT_STAR.dict.ingot());
    public static final RegistryHandle<ModArmorItem> STARMETAL_HELMET =
            trimmableArmor("starmetal_helmet", STARMETAL_MAT, ArmorType.HELMET, Suit.STARMETAL);
    public static final RegistryHandle<ModArmorItem> STARMETAL_PLATE =
            trimmableArmor("starmetal_plate", STARMETAL_MAT, ArmorType.CHESTPLATE, Suit.STARMETAL);
    public static final RegistryHandle<ModArmorItem> STARMETAL_LEGS =
            trimmableArmor("starmetal_legs", STARMETAL_MAT, ArmorType.LEGGINGS, Suit.STARMETAL);
    public static final RegistryHandle<ModArmorItem> STARMETAL_BOOTS =
            trimmableArmor("starmetal_boots", STARMETAL_MAT, ArmorType.BOOTS, Suit.STARMETAL);
    private static final ArmorMaterial ZIRCONIUM_MAT =
            material("zirconium", 1_000, 2, 5, 3, 1, 1_000, Mats.MAT_ZIRCONIUM.dict.ingot());
    public static final RegistryHandle<ModArmorItem> ZIRCONIUM_LEGS =
            trimmableArmor("zirconium_legs", ZIRCONIUM_MAT, ArmorType.LEGGINGS, Suit.NONE);
    private static final ArmorMaterial DNT_MAT =
            material("dnt", 3, 1, 1, 1, 1, 0, Mats.MAT_DNT.dict.ingot());
    public static final RegistryHandle<ModArmorItem> DNT_HELMET =
            armor("dnt_helmet", DNT_MAT, ArmorType.HELMET, Suit.DNT);
    public static final RegistryHandle<ModArmorItem> DNT_PLATE =
            armor("dnt_plate", DNT_MAT, ArmorType.CHESTPLATE, Suit.DNT);
    public static final RegistryHandle<ModArmorItem> DNT_LEGS =
            armor("dnt_legs", DNT_MAT, ArmorType.LEGGINGS, Suit.DNT);
    public static final RegistryHandle<ModArmorItem> DNT_BOOTS =
            armor("dnt_boots", DNT_MAT, ArmorType.BOOTS, Suit.DNT);

    private static final ArmorMaterial SCHRABIDIUM_MAT =
            material("schrabidium", 100, 3, 8, 6, 3, 50, Mats.MAT_SCHRABIDIUM.dict.ingot());
    public static final RegistryHandle<ModArmorItem> SCHRABIDIUM_HELMET =
            trimmableArmor(
                    "schrabidium_helmet", SCHRABIDIUM_MAT, ArmorType.HELMET, Suit.SCHRABIDIUM);
    public static final RegistryHandle<ModArmorItem> SCHRABIDIUM_PLATE =
            trimmableArmor(
                    "schrabidium_plate", SCHRABIDIUM_MAT, ArmorType.CHESTPLATE, Suit.SCHRABIDIUM);
    public static final RegistryHandle<ModArmorItem> SCHRABIDIUM_LEGS =
            trimmableArmor(
                    "schrabidium_legs", SCHRABIDIUM_MAT, ArmorType.LEGGINGS, Suit.SCHRABIDIUM);
    public static final RegistryHandle<ModArmorItem> SCHRABIDIUM_BOOTS =
            trimmableArmor("schrabidium_boots", SCHRABIDIUM_MAT, ArmorType.BOOTS, Suit.SCHRABIDIUM);
    private static final ArmorMaterial BISMUTH_MAT =
            material("bismuth", 100, 3, 8, 6, 3, 100, armorRepair("bismuth"));

    public static final RegistryHandle<ModArmorItem> BISMUTH_HELMET =
            armor("bismuth_helmet", BISMUTH_MAT, ArmorType.HELMET, Suit.BISMUTH);
    public static final RegistryHandle<ModArmorItem> BISMUTH_PLATE =
            armor("bismuth_plate", BISMUTH_MAT, ArmorType.CHESTPLATE, Suit.BISMUTH);
    public static final RegistryHandle<ModArmorItem> BISMUTH_LEGS =
            armor("bismuth_legs", BISMUTH_MAT, ArmorType.LEGGINGS, Suit.BISMUTH);
    public static final RegistryHandle<ModArmorItem> BISMUTH_BOOTS =
            armor("bismuth_boots", BISMUTH_MAT, ArmorType.BOOTS, Suit.BISMUTH);
    private static final ArmorMaterial EUPHEMIUM_MAT =
            material("euphemium", 15_000_000, 3, 8, 6, 3, 100, armorRepair("euphemium"));
    public static final RegistryHandle<ModArmorItem> EUPHEMIUM_HELMET =
            trimmableArmor(
                    "euphemium_helmet", EUPHEMIUM_MAT, ArmorType.HELMET, Suit.EUPHEMIUM, true);
    public static final RegistryHandle<ModArmorItem> EUPHEMIUM_PLATE =
            trimmableArmor(
                    "euphemium_plate", EUPHEMIUM_MAT, ArmorType.CHESTPLATE, Suit.EUPHEMIUM, true);
    public static final RegistryHandle<ModArmorItem> EUPHEMIUM_LEGS =
            trimmableArmor(
                    "euphemium_legs", EUPHEMIUM_MAT, ArmorType.LEGGINGS, Suit.EUPHEMIUM, true);
    public static final RegistryHandle<ModArmorItem> EUPHEMIUM_BOOTS =
            trimmableArmor("euphemium_boots", EUPHEMIUM_MAT, ArmorType.BOOTS, Suit.EUPHEMIUM, true);
    private static final ArmorMaterial ROBES_MAT = vanilla(ArmorMaterials.CHAINMAIL, "robes");
    public static final RegistryHandle<ModArmorItem> ROBES_HELMET =
            armor("robes_helmet", ROBES_MAT, ArmorType.HELMET, Suit.ROBES);
    public static final RegistryHandle<ModArmorItem> ROBES_PLATE =
            armor("robes_plate", ROBES_MAT, ArmorType.CHESTPLATE, Suit.ROBES);
    public static final RegistryHandle<ModArmorItem> ROBES_LEGS =
            armor("robes_legs", ROBES_MAT, ArmorType.LEGGINGS, Suit.ROBES);
    public static final RegistryHandle<ModArmorItem> ROBES_BOOTS =
            armor("robes_boots", ROBES_MAT, ArmorType.BOOTS, Suit.ROBES);
    private static final ArmorMaterial INFAMY_MAT = vanilla(ArmorMaterials.IRON, "mask_of_infamy");
    public static final RegistryHandle<ModArmorItem> MASK_OF_INFAMY =
            armor("mask_of_infamy", INFAMY_MAT, ArmorType.HELMET, Suit.NONE);

    private static final ArmorMaterial JACKT_MAT =
            material("jackt", 30, 3, 8, 6, 3, 5, Mats.MAT_STEEL.dict.ingot());
    private static final ArmorMaterial JACKT2_MAT =
            material("jackt2", 30, 3, 8, 6, 3, 5, Mats.MAT_STEEL.dict.ingot());
    public static final RegistryHandle<ModArmorItem> JACKT =
            armor("jackt", JACKT_MAT, ArmorType.CHESTPLATE, Suit.NONE);
    public static final RegistryHandle<ModArmorItem> JACKT2 =
            armor("jackt2", JACKT2_MAT, ArmorType.CHESTPLATE, Suit.NONE);

    @SuppressWarnings("unchecked")
    private static final RegistryHandle<Item>[] RP_DEPLETED =
            new RegistryHandle[DepletedRTGMaterial.VALUES.length];

    public static RegistryHandle<ItemBatteryCreative> BATTERY_CREATIVE;
    public static RegistryHandle<ItemBattery> CUBE_POWER;
    public static RegistryHandle<ItemPotatos> BATTERY_POTATOS;

    public static RegistryHandle<ItemBattery> MEMORY;
    public static RegistryHandle<Item> AMMO_DEBUG;
    public static ItemFamily<GunFactory.EnumAmmo, Item> AMMO_STANDARD;
    public static ItemFamily<ItemAmmoEnums.Ammo240Shell, Item> AMMO_SHELL;
    public static ItemFamily<GunFactory.EnumAmmoSecret, Item> AMMO_SECRET;
    public static ItemFamily<ItemAmmoEnums.AmmoFireExt, Item> AMMO_FIREEXT;
    public static ItemFamily<GunFactory.EnumModTest, Item> WEAPON_MOD_TEST;
    public static ItemFamily<GunFactory.EnumModGeneric, Item> WEAPON_MOD_GENERIC;
    public static ItemFamily<GunFactory.EnumModSpecial, Item> WEAPON_MOD_SPECIAL;
    public static ItemFamily<GunFactory.EnumModCaliber, Item> WEAPON_MOD_CALIBER;
    public static RegistryHandle<ItemGunBaseNT> GUN_DEBUG;
    public static RegistryHandle<ItemGunBaseNT> GUN_CARBINE;
    public static RegistryHandle<ItemGunBaseNT> GUN_LIGHT_REVOLVER;
    public static RegistryHandle<ItemGunBaseNT> GUN_LIGHT_REVOLVER_ATLAS;
    public static RegistryHandle<ItemGunBaseNT> GUN_LIGHT_REVOLVER_DANI;
    public static RegistryHandle<ItemGunBaseNT> GUN_HENRY;
    public static RegistryHandle<ItemGunBaseNT> GUN_HENRY_LINCOLN;
    public static RegistryHandle<ItemGunBaseNT> GUN_HEAVY_REVOLVER;
    public static RegistryHandle<ItemGunBaseNT> GUN_HEAVY_REVOLVER_LILMAC;
    public static RegistryHandle<ItemGunBaseNT> GUN_HEAVY_REVOLVER_PROTEGE;
    public static RegistryHandle<ItemGunBaseNT> GUN_HANGMAN;
    public static RegistryHandle<ItemGunBaseNT> GUN_GREASEGUN;
    public static RegistryHandle<ItemGunBaseNT> GUN_UZI;
    public static RegistryHandle<ItemGunBaseNT> GUN_UZI_AKIMBO;

    public static RegistryHandle<ItemGunBaseNT> GUN_MARESLEG;
    public static RegistryHandle<ItemGunBaseNT> GUN_MARESLEG_AKIMBO;
    public static RegistryHandle<ItemGunBaseNT> GUN_MARESLEG_BROKEN;
    public static RegistryHandle<ItemGunBaseNT> GUN_LIBERATOR;
    public static RegistryHandle<ItemGunBaseNT> GUN_SPAS12;
    public static RegistryHandle<ItemGunBaseNT> GUN_AUTOSHOTGUN;
    public static RegistryHandle<ItemGunBaseNT> GUN_AUTOSHOTGUN_SHREDDER;
    public static RegistryHandle<ItemGunBaseNT> GUN_AUTOSHOTGUN_SEXY;
    public static RegistryHandle<ItemGunBaseNT> GUN_AUTOSHOTGUN_HERETIC;
    public static RegistryHandle<ItemGunBaseNT> GUN_PANZERSCHRECK;
    public static RegistryHandle<ItemGunBaseNT> GUN_LAG;
    public static RegistryHandle<ItemGunBaseNT> GUN_G3;
    public static RegistryHandle<ItemGunBaseNT> GUN_G3_ZEBRA;
    public static RegistryHandle<ItemGunBaseNT> GUN_STG77;
    public static RegistryHandle<ItemGunBaseNT> GUN_AMAT;
    public static RegistryHandle<ItemGunBaseNT> GUN_AMAT_SUBTLETY;
    public static RegistryHandle<ItemGunBaseNT> GUN_AMAT_PENANCE;
    public static RegistryHandle<ItemGunBaseNT> GUN_M2;
    public static RegistryHandle<ItemGunBaseNT> GUN_FLAREGUN;
    public static RegistryHandle<ItemGunBaseNT> GUN_CONGOLAKE;
    public static RegistryHandle<ItemGunBaseNT> GUN_MK108;
    public static RegistryHandle<ItemGunBaseNT> GUN_FATMAN;
    public static RegistryHandle<ItemGunBaseNT> GUN_FLAMER;
    public static RegistryHandle<ItemGunBaseNT> GUN_FLAMER_TOPAZ;
    public static RegistryHandle<ItemGunBaseNT> GUN_FLAMER_DAYBREAKER;
    public static RegistryHandle<ItemGunChemthrower> GUN_CHEMTHROWER;
    public static RegistryHandle<ItemGunBaseNT> GUN_TESLA_CANNON;
    public static RegistryHandle<ItemGunBaseNT> GUN_LASER_PISTOL;
    public static RegistryHandle<ItemGunBaseNT> GUN_LASER_PISTOL_PEW_PEW;
    public static RegistryHandle<ItemGunBaseNT> GUN_LASER_PISTOL_MORNING_GLORY;
    public static RegistryHandle<ItemGunBaseNT> GUN_LASRIFLE;
    public static RegistryHandle<ItemGunBaseNT> GUN_FIREEXT;
    public static RegistryHandle<ItemGunChargeThrower> GUN_CHARGE_THROWER;
    public static RegistryHandle<ItemGunDrill> GUN_DRILL;
    public static RegistryHandle<XFactoryPA.ItemGunPA> GUN_PA_MELEE;
    public static RegistryHandle<XFactoryPA.ItemGunPA> GUN_PA_RANGED;
    public static RegistryHandle<ItemGunBaseNT> GUN_FOLLY;
    public static RegistryHandle<ItemGunBaseNT> GUN_ABERRATOR;
    public static RegistryHandle<ItemGunBaseNT> GUN_ABERRATOR_EOTT;
    public static RegistryHandle<ItemGunBaseNT> GUN_PEPPERBOX;
    public static RegistryHandle<ItemGunBaseNT> GUN_AM180;
    public static RegistryHandle<ItemGunBaseNT> GUN_STAR_F;
    public static RegistryHandle<ItemGunBaseNT> GUN_STAR_F_AKIMBO;
    public static RegistryHandle<ItemGunBaseNT> GUN_BOLTER;
    public static RegistryHandle<ItemGunBaseNT> GUN_TAU;
    public static RegistryHandle<ItemGunBaseNT> GUN_COILGUN;
    public static RegistryHandle<ItemGunBaseNT> GUN_STINGER;
    public static RegistryHandle<ItemGunBaseNT> GUN_QUADRO;
    public static RegistryHandle<ItemGunBaseNT> GUN_MISSILE_LAUNCHER;
    public static RegistryHandle<ItemGunNI4NI> GUN_N_I_4_N_I;
    public static RegistryHandle<ItemGunBaseNT> GUN_DOUBLE_BARREL;
    public static RegistryHandle<ItemGunBaseNT> GUN_DOUBLE_BARREL_SACRED_DRAGON;
    public static RegistryHandle<ItemGunBaseNT> GUN_MINIGUN;
    public static RegistryHandle<ItemGunBaseNT> GUN_MINIGUN_LACUNAE;
    public static RegistryHandle<ItemGunBaseNT> GUN_MINIGUN_DUAL;
    public static RegistryHandle<ItemGunBaseNT> GUN_MAS36;
    private static @Nullable Map<Item, ItemLike> fuelMap;

    static {
        for (EnumBatterySC tier : EnumBatterySC.VALUES) {
            BATTERIES.add(
                    Reg.<ItemBattery>item(
                            "battery_sc_" + tier.id,
                            props -> new ItemBatterySC(props, tier),
                            () -> new Item.Properties().stacksTo(1)));
        }
        for (EnumBatteryPack tier : EnumBatteryPack.VALUES) {
            String drawn = tier.isCapacitor() ? "Capacitor" : "Battery";
            String hidden = tier.isCapacitor() ? "Battery" : "Capacitor";
            String sheet = "block/models/machines/" + tier.tex;
            RegistryHandle<ItemBatteryPack> pack =
                    Reg.<ItemBatteryPack>item(
                                    "battery_pack_" + tier.id,
                                    props -> new ItemBatteryPack(props, tier),
                                    () -> new Item.Properties().stacksTo(1))
                            .subtypes(ItemSubtype.BATTERY_CHARGE);
            BATTERIES.add(pack);
            BATTERY_PACKS[tier.ordinal()] = pack;
        }

        BATTERY_PACK_QUANTUM = BATTERY_PACKS[EnumBatteryPack.BATTERY_QUANTUM.ordinal()];

        BATTERY_CREATIVE =
                Reg.item(
                        "battery_creative",
                        ItemBatteryCreative::new,
                        () -> new Item.Properties().stacksTo(1));
        CUBE_POWER =
                battery(
                        "cube_power",
                        1_000_000_000_000_000_000L,
                        1_000_000_000_000_000L,
                        1_000_000_000_000_000L);

        BATTERY_POTATOS =
                Reg.<ItemPotatos>item(
                                "battery_potatos",
                                props -> new ItemPotatos(props, 500_000L, 0L, 100L),
                                () -> new Item.Properties().stacksTo(1))
                        .subtypes(ItemSubtype.BATTERY_CHARGE)
                        .addTo(BATTERIES);
        MEMORY =
                Reg.item(
                                "memory",
                                props ->
                                        new ItemBattery(
                                                props,
                                                Long.MAX_VALUE / 100L,
                                                100_000_000_000_000L,
                                                100_000_000_000_000L),
                                () -> new Item.Properties().stacksTo(1))
                        .subtypes(ItemSubtype.BATTERY_CHARGE);
    }

    static {
        MaterialShapeRoster.registerNamed(MaterialShapes.INGOT, EXCLUDED, INGOTS);
    }

    static {
        MaterialShapeRoster.registerNamed(
                MaterialShapes.DUST,
                MaterialShapeRoster.roster(MaterialShapes.DUST, MP_EXCLUDED),
                ModItems::properties,
                POWDERS);
    }

    static {
        MaterialShapeRoster.registerNamed(MaterialShapes.NUGGET, MN_EXCLUDED, NUGGETS);
    }

    static {
        MaterialShapeRoster.registerNamed(MaterialShapes.BILLET, Set.of(), BILLETS);
    }

    static {
        GunFactory.init(Services.REGISTRAR);
    }

    static {
        int s = ItemMold.SIZE_SMALL;
        int l = ItemMold.SIZE_LARGE;
        mold(new MoldShape(s, "nugget", MaterialShapes.NUGGET));
        mold(new MoldShape(s, "billet", MaterialShapes.BILLET));
        mold(new MoldShape(s, "ingot", MaterialShapes.INGOT));
        mold(new MoldShape(s, "plate", MaterialShapes.PLATE));
        mold(new MoldShape(s, "wire", MaterialShapes.WIRE, 8));
        mold(new MoldShape(s, "plate_cast", MaterialShapes.CASTPLATE));
        mold(new MoldShape(s, "wire_dense", MaterialShapes.DENSEWIRE));
        mold(
                new MoldMulti(
                        s,
                        "blade",
                        MaterialShapes.INGOT.q(3),
                        Mats.MAT_TITANIUM,
                        "blade_titanium",
                        Mats.MAT_TUNGSTEN,
                        "blade_tungsten"));
        mold(
                new MoldMulti(
                        s,
                        "blades",
                        MaterialShapes.INGOT.q(4),
                        Mats.MAT_STEEL,
                        "blades_steel",
                        Mats.MAT_TITANIUM,
                        "blades_titanium"));
        mold(
                new MoldMulti(
                        s,
                        "stamp",
                        MaterialShapes.INGOT.q(4),
                        Mats.MAT_STONE,
                        "stamp_stone_flat",
                        Mats.MAT_IRON,
                        "stamp_iron_flat",
                        Mats.MAT_STEEL,
                        "stamp_steel_flat",
                        Mats.MAT_TITANIUM,
                        "stamp_titanium_flat",
                        Mats.MAT_OBSIDIAN,
                        "stamp_obsidian_flat"));
        mold(new MoldShape(s, "shell", MaterialShapes.SHELL));
        mold(new MoldShape(s, "pipe", MaterialShapes.PIPE));
        mold(new MoldShape(l, "ingots", MaterialShapes.INGOT, 9));
        mold(new MoldShape(l, "plates", MaterialShapes.PLATE, 9));
        mold(new MoldShape(l, "plates_cast", MaterialShapes.CASTPLATE, 3));
        mold(new MoldShape(l, "wires_dense", MaterialShapes.DENSEWIRE, 9));
        mold(
                new ItemMold.MoldBlock(l, "block", MaterialShapes.BLOCK)
                        .override(Mats.MAT_STONE, "minecraft:stone")
                        .override(Mats.MAT_OBSIDIAN, "minecraft:obsidian"));
        mold(
                new MoldMulti(
                        s,
                        "c9",
                        MaterialShapes.PLATE.q(1, 4),
                        Mats.MAT_GUNMETAL,
                        "casing_small",
                        Mats.MAT_WEAPONSTEEL,
                        "casing_small_steel"));
        mold(
                new MoldMulti(
                        s,
                        "c50",
                        MaterialShapes.PLATE.q(1, 2),
                        Mats.MAT_GUNMETAL,
                        "casing_large",
                        Mats.MAT_WEAPONSTEEL,
                        "casing_large_steel"));
        mold(new MoldShape(s, "barrel_light", MaterialShapes.LIGHTBARREL));
        mold(new MoldShape(s, "barrel_heavy", MaterialShapes.HEAVYBARREL));
        mold(new MoldShape(s, "receiver_light", MaterialShapes.LIGHTRECEIVER));
        mold(new MoldShape(s, "receiver_heavy", MaterialShapes.HEAVYRECEIVER));
        mold(new MoldShape(s, "mechanism", MaterialShapes.MECHANISM));
        mold(new MoldShape(s, "stock", MaterialShapes.STOCK));
        mold(new MoldShape(s, "grip", MaterialShapes.GRIP));
    }

    static {
        for (EnumZirnoxType type : EnumZirnoxType.VALUES) {
            RegistryHandle<ItemZirnoxRod> handle =
                    Reg.item(
                            "rod_zirnox_" + type.id,
                            props -> new ItemZirnoxRod(props, type),
                            () -> new Item.Properties().stacksTo(1));
            FUEL[type.ordinal()] = handle;
            ZIRNOX_ALL.add(handle);
        }
    }

    static {
        dep(EnumZirnoxType.NATURAL_URANIUM_FUEL, 172.5F, 0F);

        dep(EnumZirnoxType.URANIUM_FUEL, 150F, 0F);
        dep(EnumZirnoxType.THORIUM_FUEL, 112.5F, 0F);
        dep(EnumZirnoxType.MOX_FUEL, 150F, 0F);
        dep(EnumZirnoxType.PLUTONIUM_FUEL, 187.5F, 0F);
        dep(EnumZirnoxType.U233_FUEL, 150F, 0F);
        dep(EnumZirnoxType.U235_FUEL, 165F, 0F);
        dep(EnumZirnoxType.LES_FUEL, 225F, 20F);
        dep(EnumZirnoxType.ZFB_MOX_FUEL, 75F, 0F);
    }

    static {
        for (DepletedRTGMaterial mat : DepletedRTGMaterial.values()) {
            RegistryHandle<Item> h =
                    Reg.item(
                            "pellet_rtg_depleted_" + mat.name().toLowerCase(Locale.ROOT),
                            props -> new ItemRTGPelletDepleted(props, mat),
                            () -> new Item.Properties().craftRemainder(plate(Mats.MAT_IRON)));
            RP_DEPLETED[mat.ordinal()] = h;
            RTG_ALL.add(h);
        }
    }

    static {
        pellet(RBMK_FUEL_UEU, "rbmk_pellet_ueu", "Unenriched Uranium");
        pellet(RBMK_FUEL_MEU, "rbmk_pellet_meu", "Medium Enriched Uranium-235");
        pellet(RBMK_FUEL_HEU233, "rbmk_pellet_heu233", "Highly Enriched Uranium-233");
        pellet(RBMK_FUEL_HEU235, "rbmk_pellet_heu235", "Highly Enriched Uranium-235");
        pellet(RBMK_FUEL_UZH, "rbmk_pellet_uzh", "Uranium Zirconium Hydride");
        pellet(RBMK_FUEL_THMEU, "rbmk_pellet_thmeu", "Thorium with MEU Driver Fuel");
        pellet(RBMK_FUEL_LEP, "rbmk_pellet_lep", "Low Enriched Plutonium-239");
        pellet(RBMK_FUEL_MEP, "rbmk_pellet_mep", "Medium Enriched Plutonium-239");
        pellet(RBMK_FUEL_HEP, "rbmk_pellet_hep239", "Highly Enriched Plutonium-239");
        pellet(RBMK_FUEL_HEP241, "rbmk_pellet_hep241", "Highly Enriched Plutonium-241");
        pellet(RBMK_FUEL_LEA, "rbmk_pellet_lea", "Low Enriched Americium-242");
        pellet(RBMK_FUEL_MEA, "rbmk_pellet_mea", "Medium Enriched Americium-242");
        pellet(RBMK_FUEL_HEA241, "rbmk_pellet_hea241", "Highly Enriched Americium-241");
        pellet(RBMK_FUEL_HEA242, "rbmk_pellet_hea242", "Highly Enriched Americium-242");
        pellet(RBMK_FUEL_MEN, "rbmk_pellet_men", "Medium Enriched Neptunium-237");
        pellet(RBMK_FUEL_HEN, "rbmk_pellet_hen", "Highly Enriched Neptunium-237");
        pellet(RBMK_FUEL_MOX, "rbmk_pellet_mox", "Mixed MEU & LEP Oxide");
        pellet(RBMK_FUEL_LES, "rbmk_pellet_les", "Low Enriched Schrabidium-326");
        pellet(RBMK_FUEL_MES, "rbmk_pellet_mes", "Medium Enriched Schrabidium-326");
        pellet(RBMK_FUEL_HES, "rbmk_pellet_hes", "Highly Enriched Schrabidium-326");
        pellet(RBMK_FUEL_LEAUS, "rbmk_pellet_leaus", "Low Enriched Australium (Tasmanite)");
        pellet(RBMK_FUEL_HEAUS, "rbmk_pellet_heaus", "Highly Enriched Australium (Ayerite)");
        pellet(
                RBMK_FUEL_PO210BE,
                "rbmk_pellet_po210be",
                "Polonium-210 & Beryllium Neutron Source",
                true);
        pellet(
                RBMK_FUEL_RA226BE,
                "rbmk_pellet_ra226be",
                "Radium-226 & Beryllium Neutron Source",
                true);
        pellet(
                RBMK_FUEL_PU238BE,
                "rbmk_pellet_pu238be",
                "Plutonium-238 & Beryllium Neutron Source");
        pellet(
                RBMK_FUEL_BALEFIRE_GOLD,
                "rbmk_pellet_balefire_gold",
                "Antihydrogen in a Magnetized Gold-198 Lattice",
                true);
        pellet(
                RBMK_FUEL_FLASHLEAD,
                "rbmk_pellet_flashlead",
                "Antihydrogen confined by a Magnetized Gold-198 and Lead-209 Lattice",
                true);
        pellet(RBMK_FUEL_BALEFIRE, "rbmk_pellet_balefire", "Draconic Flames", true);
        pellet(
                RBMK_FUEL_ZFB_BISMUTH,
                "rbmk_pellet_zfb_bismuth",
                "Zirconium Fast Breeder - LEU/HEP-241#Bi");
        pellet(
                RBMK_FUEL_ZFB_PU241,
                "rbmk_pellet_zfb_pu241",
                "Zirconium Fast Breeder - HEU-235/HEP-240#Pu-241");
        pellet(
                RBMK_FUEL_ZFB_AM_MIX,
                "rbmk_pellet_zfb_am_mix",
                "Zirconium Fast Breeder - HEP-241#MEA");
        pellet(
                RBMK_FUEL_DRX,
                "rbmk_pellet_drx",
                ChatFormatting.OBFUSCATED + "can't you hear, can't you hear the thunder?");
    }

    static {
        for (EnumPWRFuel fuel : EnumPWRFuel.VALUES) {
            PWR_FUELS.add(
                    Reg.item(
                            "pwr_fuel_" + fuel.id,
                            props -> new ItemPWRFuel(props, fuel),
                            Item.Properties::new));
        }
    }

    static {
    }

    private ModItems() {}

    public static @Nullable ItemLike spentZirnoxFuel(Item rod) {
        if (fuelMap == null) {
            Map<Item, ItemLike> resolved = new HashMap<>();
            resolved.put(
                    zirnoxFuel(EnumZirnoxType.TH232_FUEL), zirnoxFuel(EnumZirnoxType.THORIUM_FUEL));
            resolved.put(zirnoxFuel(EnumZirnoxType.LITHIUM_FUEL), ROD_ZIRNOX_TRITIUM);
            for (EnumZirnoxType type : EnumZirnoxType.VALUES) {
                RegistryHandle<ItemZirnoxRodDepleted> handle = DEPLETED[type.ordinal()];
                if (handle != null) resolved.put(zirnoxFuel(type), handle);
            }
            fuelMap = resolved;
        }
        return fuelMap.get(rod);
    }

    private static Reg.Handle<Item> oreFragment(String name) {
        return Reg.item(name, Item::new, Item.Properties::new);
    }

    private static Reg.Handle<ItemFluidTank> fluidTank(
            String name, int capacity, ItemFluidTank.Family family) {
        return Reg.item(
                        name,
                        props -> new ItemFluidTank(props, capacity, name, family),
                        Item.Properties::new)
                .subtypes(ItemSubtype.FLUID_CONTENT);
    }

    private static ItemFamily<ItemPWRFuel.EnumPWRFuel, ItemPWRFuelStage> pwrFuelStage(
            String name, boolean hot) {
        return Reg.family(
                ItemPWRFuel.EnumPWRFuel.class,
                fuel -> name + "_" + fuel.id,
                (props, fuel) -> new ItemPWRFuelStage(props, hot, fuel),
                Item.Properties::new);
    }

    private static Reg.Handle<Item> hs(String name, String sprite, int meta) {
        return Reg.item(name, Item::new, Item.Properties::new);
    }

    private static ItemFamily<ItemWatzPellet.EnumWatzType, ItemWatzPellet> watzPellet(
            String name, boolean depleted) {
        return Reg.family(
                name,
                ItemWatzPellet.EnumWatzType.class,
                (props, type) -> new ItemWatzPellet(props, depleted, type),
                () -> new Item.Properties().stacksTo(16));
    }

    private static ItemFamily<ItemBreedingRod.BreedingRodType, ItemBreedingRod> breedingRod(
            String name, ItemBreedingRod.Family family, RegistryHandle<? extends Item> remainder) {
        return Reg.family(
                name,
                ItemBreedingRod.BreedingRodType.class,
                (props, type) -> new ItemBreedingRod(props, family, type),
                () -> new Item.Properties().craftRemainder(remainder.get()));
    }

    private static Reg.Handle<ItemBattery> battery(
            String name, long maxCharge, long chargeRate, long dischargeRate) {
        return Reg.item(
                        name,
                        props -> new ItemBattery(props, maxCharge, chargeRate, dischargeRate),
                        () -> new Item.Properties().stacksTo(1))
                .subtypes(ItemSubtype.BATTERY_CHARGE)
                .addTo(BATTERIES);
    }

    private static Reg.Handle<ItemMachineUpgrade> upgrade(String name, UpgradeType type, int tier) {
        return Reg.item(
                        name,
                        props -> new ItemMachineUpgrade(props, type, tier),
                        () -> new Item.Properties().stacksTo(1))
                .addTo(UPGRADES);
    }

    private static ItemAttributeModifiers weaponModifier(double damage) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_ID,
                                damage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static Reg.Handle<ItemMachineUpgrade> special(
            String name, int stack, Component... desc) {
        return Reg.item(
                        name,
                        props -> new ItemMachineUpgrade(props, desc),
                        () -> new Item.Properties().stacksTo(stack))
                .addTo(UPGRADES);
    }

    private static Reg.Handle<ItemStamp> stampDamageable(
            String name, int durability, StampType type) {
        return Reg.item(
                        name,
                        props -> new ItemStamp(props, type),
                        () -> new Item.Properties().durability(durability))
                .addTo(STAMPS);
    }

    private static Reg.Handle<ItemStamp> stampIndestructible(String name, StampType type) {
        return Reg.item(
                        name,
                        props -> new ItemStamp(props, type),
                        () -> new Item.Properties().stacksTo(1))
                .addTo(STAMPS);
    }

    private static List<RegistryHandle<MaterialShapeItem>> plateRoster() {
        List<RegistryHandle<MaterialShapeItem>> plates = new ArrayList<>();
        MaterialShapeRoster.registerNamed(
                MaterialShapes.PLATE,
                List.of(PLATE_MATERIALS),
                ignored -> new Item.Properties(),
                plates);
        return plates;
    }

    public static MaterialShapeItem ingot(NTMMaterial material) {
        return MaterialShapeRoster.require(MaterialShapes.INGOT, material);
    }

    public static MaterialShapeItem plate(NTMMaterial material) {
        return MaterialShapeRoster.require(MaterialShapes.PLATE, material);
    }

    public static MaterialShapeItem powder(NTMMaterial material) {
        return MaterialShapeRoster.require(MaterialShapes.DUST, material);
    }

    public static MaterialShapeItem nugget(NTMMaterial material) {
        return MaterialShapeRoster.require(MaterialShapes.NUGGET, material);
    }

    public static MaterialShapeItem billet(NTMMaterial material) {
        return MaterialShapeRoster.require(MaterialShapes.BILLET, material);
    }

    public static MaterialShapeItem plateOf(NTMMaterial material) {
        return MaterialShapeRoster.find(MaterialShapes.PLATE, material);
    }

    private static void mold(ItemMold.Mold mold) {
        MOLDS.add(
                Reg.item(
                        "mold_" + mold.name,
                        props -> new ItemMold(props, mold),
                        () -> new Item.Properties().stacksTo(1)));
    }

    public static ItemMold byName(String name) {
        for (RegistryHandle<ItemMold> handle : MOLDS) {
            ItemMold item = handle.get();
            if (item.mold.name.equals(name)) return item;
        }
        throw new IllegalArgumentException("No such mold: " + name);
    }

    public static MaterialShapeItem ingotOf(NTMMaterial material) {
        return MaterialShapeRoster.find(MaterialShapes.INGOT, material);
    }

    private static Set<NTMMaterial> union(Set<NTMMaterial> a, Set<NTMMaterial> b) {
        Set<NTMMaterial> u = new HashSet<>(a);
        u.addAll(b);
        return u;
    }

    private static Item.Properties properties(NTMMaterial material) {
        if (material == Mats.MAT_THORIUM) return new Item.Properties().rarity(Rarity.UNCOMMON);
        if (material == Mats.MAT_BORON
                || material == Mats.MAT_COBALT
                || material == Mats.MAT_IODINE
                || material == Mats.MAT_NEODYMIUM
                || material == Mats.MAT_STRONTIUM
                || material == Mats.MAT_NIOBIUM) {
            return new Item.Properties().rarity(Rarity.EPIC);
        }
        return new Item.Properties();
    }

    public static MaterialShapeItem powderOf(NTMMaterial material) {
        return MaterialShapeRoster.find(MaterialShapes.DUST, material);
    }

    public static MaterialShapeItem nuggetOf(NTMMaterial material) {
        return MaterialShapeRoster.find(MaterialShapes.NUGGET, material);
    }

    public static MaterialShapeItem billetOf(NTMMaterial material) {
        return MaterialShapeRoster.find(MaterialShapes.BILLET, material);
    }

    private static Item.Properties capsuleProperties() {
        return new Item.Properties().craftRemainder(PARTICLE_EMPTY.get());
    }

    private static Item.Properties food(int nutrition, float saturation) {
        return new Item.Properties()
                .food(
                        new FoodProperties.Builder()
                                .nutrition(nutrition)
                                .saturationModifier(saturation)
                                .build());
    }

    private static Item.Properties alwaysEdible(int nutrition, float saturation) {
        return new Item.Properties()
                .food(
                        new FoodProperties.Builder()
                                .nutrition(nutrition)
                                .saturationModifier(saturation)
                                .alwaysEdible()
                                .build());
    }

    private static Reg.Handle<Item> stew(String name) {
        return Reg.item(
                name,
                Item::new,
                () ->
                        new Item.Properties()
                                .stacksTo(1)
                                .food(
                                        new FoodProperties.Builder()
                                                .nutrition(6)
                                                .saturationModifier(0.6F)
                                                .build())
                                .usingConvertsTo(Items.BOWL));
    }

    private static Reg.Handle<ItemPill> pill(String name, ItemPill.Type type, String texture) {
        return Reg.item(
                        name,
                        props -> new ItemPill(props, type),
                        () -> new Item.Properties().food(ItemPill.FOOD, ItemPill.CONSUMABLE))
                .addTo(MISC_ALL);
    }

    private static Reg.Handle<ItemMissile> missile(
            String name, MissileFormFactor form, MissileTier tier) {
        Reg.Handle<ItemMissile> h =
                Reg.item(
                        name,
                        p -> new ItemMissile(p, form, tier),
                        () -> new Item.Properties().stacksTo(1));
        MISSILE_ALL.add(h);
        return h;
    }

    private static Item.Properties signProperties() {
        return ItemSwordAbility.swordProperties(ToolTier.ALLOY, 9.0F, 0, null).stacksTo(1);
    }

    private static Reg.Handle<Item> missileItem(String name) {
        Reg.Handle<Item> h = Reg.item(name, Item::new, Item.Properties::new);
        MISSILE_ALL.add(h);
        return h;
    }

    private static Reg.Handle<ItemSatelliteChip> satChip(
            String name, @Nullable String description) {
        return Reg.<ItemSatelliteChip>item(
                        name,
                        props -> new ItemSatelliteChip(props, description),
                        () -> new Item.Properties().stacksTo(1))
                .addTo(MISSILE_ALL);
    }

    private static Reg.Handle<ItemSatelliteChip> satChipLegacy(
            String name, @Nullable String description) {
        return Reg.<ItemSatelliteChip>item(
                name,
                props -> new ItemSatelliteChip(props, description),
                () -> new Item.Properties().stacksTo(1));
    }

    private static Reg.Handle<ItemCustomMissilePart> missilePart(
            String name, String icon, UnaryOperator<ItemCustomMissilePart> build) {
        Reg.Handle<ItemCustomMissilePart> h = registerPart(name, build);

        return h;
    }

    private static Reg.Handle<ItemCustomMissilePart> missilePart(
            String name,
            String icon,
            String mesh,
            String skin,
            float height,
            float guiHeight,
            UnaryOperator<ItemCustomMissilePart> build) {
        Reg.Handle<ItemCustomMissilePart> h = registerPart(name, mesh, skin, height, build);

        return h;
    }

    private static Reg.Handle<ItemCustomMissilePart> missilePartNoTab(
            String name,
            String icon,
            String mesh,
            String skin,
            float height,
            float guiHeight,
            UnaryOperator<ItemCustomMissilePart> build) {
        Reg.Handle<ItemCustomMissilePart> h = registerPart(name, mesh, skin, height, build);

        return h;
    }

    private static Reg.Handle<ItemLootCrate> lootCrate(
            String name, List<ItemCustomMissilePart> pool) {
        return Reg.item(
                        name,
                        props -> new ItemLootCrate(props, pool),
                        () -> new Item.Properties().stacksTo(1))
                .addTo(MISSILE_ALL);
    }

    private static Reg.Handle<ItemCustomMissilePart> registerPart(
            String name, UnaryOperator<ItemCustomMissilePart> build) {
        Reg.Handle<ItemCustomMissilePart> h =
                Reg.item(
                        name,
                        props -> build.apply(new ItemCustomMissilePart(props)),
                        () -> new Item.Properties().stacksTo(1));
        MISSILE_ALL.add(h);
        return h;
    }

    private static Reg.Handle<ItemCustomMissilePart> registerPart(
            String name,
            String mesh,
            String skin,
            float height,
            UnaryOperator<ItemCustomMissilePart> build) {
        return registerPart(
                name,
                p ->
                        build.apply(p)
                                .setAssembly(
                                        height,
                                        Library.id("models/missile_parts/" + mesh + ".obj"),
                                        Identifier.parse(skin)));
    }

    public static RegistryHandle<ItemSoyuz> soyuzOfSkin(int skin) {
        return switch (skin) {
            case 1 -> MISSILE_SOYUZ_1;
            case 2 -> MISSILE_SOYUZ_2;
            default -> MISSILE_SOYUZ_0;
        };
    }

    private static Reg.Handle<Item> record(ModJukeboxSongs.Song song) {
        return Reg.item(
                song.itemName(),
                Item::new,
                () ->
                        new Item.Properties()
                                .stacksTo(1)
                                .rarity(Rarity.RARE)
                                .jukeboxPlayable(song.key()));
    }

    private static Reg.Handle<ItemSoyuz> soyuz(int skin, String skinKey, Rarity rarity) {
        Reg.Handle<ItemSoyuz> h =
                Reg.item(
                        "missile_soyuz_" + skin,
                        p -> new ItemSoyuz(p, skin, "desc.item.soyuz." + skinKey),
                        () -> new Item.Properties().stacksTo(1).rarity(rarity));
        MISSILE_ALL.add(h);
        return h;
    }

    private static Reg.Handle<ItemCustomLore> missileLore(String name) {
        Reg.Handle<ItemCustomLore> h =
                Reg.item(name, ItemCustomLore::new, () -> new Item.Properties().stacksTo(1));
        MISSILE_ALL.add(h);
        return h;
    }

    public static void initHazards() {
        hazmat(HAZMAT_HELMET, 0.12);
        hazmat(HAZMAT_PLATE, 0.24);
        hazmat(HAZMAT_LEGS, 0.18);
        hazmat(HAZMAT_BOOTS, 0.06);
        hazmat(HAZMAT_HELMET_RED, 0.2);
        hazmat(HAZMAT_PLATE_RED, 0.4);
        hazmat(HAZMAT_LEGS_RED, 0.3);
        hazmat(HAZMAT_BOOTS_RED, 0.1);
        hazmat(HAZMAT_HELMET_GREY, 0.4);
        hazmat(HAZMAT_PLATE_GREY, 0.8);
        hazmat(HAZMAT_LEGS_GREY, 0.6);
        hazmat(HAZMAT_BOOTS_GREY, 0.2);
        hazmat(LIQUIDATOR_HELMET, 0.48);
        hazmat(LIQUIDATOR_PLATE, 0.96);
        hazmat(LIQUIDATOR_LEGS, 0.72);
        hazmat(LIQUIDATOR_BOOTS, 0.24);
        hazmat(SECURITY_HELMET, .165);
        hazmat(SECURITY_PLATE, .33);
        hazmat(SECURITY_LEGS, .2475);
        hazmat(SECURITY_BOOTS, .0825);
        hazmat(STARMETAL_HELMET, .2);
        hazmat(STARMETAL_PLATE, .4);
        hazmat(STARMETAL_LEGS, .3);
        hazmat(STARMETAL_BOOTS, .1);
        for (RegistryHandle<ModArmorItem> h : List.of(STEEL_HELMET, TITANIUM_HELMET))
            hazmat(h, .009);
        for (RegistryHandle<ModArmorItem> h : List.of(STEEL_PLATE, TITANIUM_PLATE)) hazmat(h, .018);
        for (RegistryHandle<ModArmorItem> h : List.of(STEEL_LEGS, TITANIUM_LEGS)) hazmat(h, .0135);
        for (RegistryHandle<ModArmorItem> h : List.of(STEEL_BOOTS, TITANIUM_BOOTS))
            hazmat(h, .0045);
        hazmat(COBALT_HELMET, .025);
        hazmat(COBALT_PLATE, .05);
        hazmat(COBALT_LEGS, .0375);
        hazmat(COBALT_BOOTS, .0125);
        hazmat(T51_HELMET, .2);
        hazmat(T51_PLATE, .4);
        hazmat(T51_LEGS, .3);
        hazmat(T51_BOOTS, .1);

        hazmat(CMB_HELMET, .26);
        hazmat(CMB_PLATE, .52);
        hazmat(CMB_LEGS, .39);
        hazmat(CMB_BOOTS, .13);

        ArmorRegistry.registerHazard(SCHRABIDIUM_HELMET, fullPackage());
        ArmorRegistry.registerHazard(EUPHEMIUM_HELMET, fullPackage());
        hazmat(SCHRABIDIUM_HELMET, .6);
        hazmat(SCHRABIDIUM_PLATE, 1.2);
        hazmat(SCHRABIDIUM_LEGS, .9);
        hazmat(SCHRABIDIUM_BOOTS, .3);
        hazmat(EUPHEMIUM_HELMET, 2);
        hazmat(EUPHEMIUM_PLATE, 4);
        hazmat(EUPHEMIUM_LEGS, 3);
        hazmat(EUPHEMIUM_BOOTS, 1);

        hazmat(PAA_PLATE, .68);
        hazmat(HAZMAT_PAA_HELMET, .34);
        hazmat(HAZMAT_PAA_PLATE, .68);
        hazmat(HAZMAT_PAA_LEGS, .51);
        hazmat(HAZMAT_PAA_BOOTS, .17);
        hazmat(PAA_LEGS, .51);
        hazmat(PAA_BOOTS, .17);
        hazmat(Items.IRON_HELMET, .0045);
        hazmat(Items.IRON_CHESTPLATE, .009);
        hazmat(Items.IRON_LEGGINGS, .00675);
        hazmat(Items.IRON_BOOTS, .00225);
        hazmat(Items.GOLDEN_HELMET, .0045);
        hazmat(Items.GOLDEN_CHESTPLATE, .009);
        hazmat(Items.GOLDEN_LEGGINGS, .00675);
        hazmat(Items.GOLDEN_BOOTS, .00225);

        ArmorFullSetBonus.register(
                Suit.T51,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.STRENGTH, 0)
                        .vats()
                        .geigerSound()
                        .hardLanding()
                        .steps(
                                ModSounds.STEP_METAL,
                                ModSounds.STEP_IRON_JUMP,
                                ModSounds.STEP_IRON_LAND)
                        .drain(ModItems.T51_DRAIN)
                        .build());
        DamageResistanceHandler.registerSet(
                T51_HELMET.get(),
                T51_PLATE.get(),
                T51_LEGS.get(),
                T51_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 0.5F, 0.35F)
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2.0F, 0.15F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 5.0F, 0.25F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 0.0F, 1.0F)
                        .setOther(0.0F, 0.10F));
        hazmat(TAURUN_HELMET, .025);
        hazmat(TAURUN_PLATE, .05);
        hazmat(TAURUN_LEGS, .0375);
        hazmat(TAURUN_BOOTS, .0125);
        ArmorFullSetBonus.register(
                Suit.TAURUN,
                ArmorFullSetBonus.builder().effect(MobEffects.STRENGTH, 0).stepSize(1).build());
        DamageResistanceHandler.registerSet(
                TAURUN_HELMET.get(),
                TAURUN_PLATE.get(),
                TAURUN_LEGS.get(),
                TAURUN_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.15F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 0F, 0.25F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 0F, 0.25F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 4F, 0.5F)
                        .setOther(2F, 0.1F));
        hazmat(ALLOY_HELMET, .014);
        hazmat(ALLOY_PLATE, .028);
        hazmat(ALLOY_LEGS, .021);
        hazmat(ALLOY_BOOTS, .007);
        hazmat(JACKT, .1);
        hazmat(JACKT2, .1);
        hazmat(GAS_MASK, 0.07);
        hazmat(GAS_MASK_M65, .095);
        ArmorRegistry.registerHazard(
                GAS_MASK_FILTER,
                HazardClass.PARTICLE_COARSE,
                HazardClass.PARTICLE_FINE,
                HazardClass.GAS_LUNG,
                HazardClass.GAS_BLISTERING,
                HazardClass.BACTERIA);
        ArmorRegistry.registerHazard(
                GAS_MASK_FILTER_MONO, HazardClass.PARTICLE_COARSE, HazardClass.GAS_MONOXIDE);
        ArmorRegistry.registerHazard(
                GAS_MASK_FILTER_COMBO,
                HazardClass.PARTICLE_COARSE,
                HazardClass.PARTICLE_FINE,
                HazardClass.GAS_LUNG,
                HazardClass.GAS_BLISTERING,
                HazardClass.BACTERIA,
                HazardClass.GAS_MONOXIDE);
        ArmorRegistry.registerHazard(GAS_MASK_FILTER_RAG, HazardClass.PARTICLE_COARSE);
        ArmorRegistry.registerHazard(
                GAS_MASK_FILTER_PISS, HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);
        ArmorRegistry.registerHazard(GAS_MASK, HazardClass.SAND, HazardClass.LIGHT);
        ArmorRegistry.registerHazard(GAS_MASK_M65, HazardClass.SAND);
        ArmorRegistry.registerHazard(MASK_RAG, HazardClass.PARTICLE_COARSE);
        ArmorRegistry.registerHazard(MASK_PISS, HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);
        ArmorRegistry.registerHazard(ATTACHMENT_MASK, HazardClass.SAND);
        ArmorRegistry.registerHazard(GOGGLES, HazardClass.LIGHT, HazardClass.SAND);
        ArmorRegistry.registerHazard(ASHGLASSES, HazardClass.LIGHT, HazardClass.SAND);
        ArmorRegistry.registerHazard(ASBESTOS_HELMET, HazardClass.LIGHT, HazardClass.SAND);
        ArmorRegistry.registerHazard(HAZMAT_HELMET, HazardClass.SAND);
        ArmorRegistry.registerHazard(HAZMAT_HELMET_RED, HazardClass.SAND);
        ArmorRegistry.registerHazard(HAZMAT_HELMET_GREY, HazardClass.SAND);
        ArmorRegistry.registerHazard(HAZMAT_PAA_HELMET, HazardClass.LIGHT, HazardClass.SAND);
        ArmorRegistry.registerHazard(LIQUIDATOR_HELMET, HazardClass.LIGHT, HazardClass.SAND);

        ArmorRegistry.registerHazard(T51_HELMET, fullNoLight());
        ArmorRegistry.registerHazard(TAURUN_HELMET, fullPackage());

        DamageResistanceHandler.registerItem(
                JACKT.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 1F, 0.20F));
        DamageResistanceHandler.registerItem(
                JACKT2.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.25F));
        DamageResistanceHandler.registerSet(
                STEEL_HELMET.get(),
                STEEL_PLATE.get(),
                STEEL_LEGS.get(),
                STEEL_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.1F));
        DamageResistanceHandler.registerSet(
                TITANIUM_HELMET.get(),
                TITANIUM_PLATE.get(),
                TITANIUM_LEGS.get(),
                TITANIUM_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 3F, 0.1F));
        DamageResistanceHandler.registerSet(
                ALLOY_HELMET.get(),
                ALLOY_PLATE.get(),
                ALLOY_LEGS.get(),
                ALLOY_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.1F));
        DamageResistanceHandler.registerSet(
                COBALT_HELMET.get(),
                COBALT_PLATE.get(),
                COBALT_LEGS.get(),
                COBALT_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.1F));
        DamageResistanceHandler.registerSet(
                STARMETAL_HELMET.get(),
                STARMETAL_PLATE.get(),
                STARMETAL_LEGS.get(),
                STARMETAL_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 3F, 0.25F)
                        .setOther(1F, 0.1F));
        DamageResistanceHandler.registerSet(
                SECURITY_HELMET.get(),
                SECURITY_PLATE.get(),
                SECURITY_LEGS.get(),
                SECURITY_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 5F, 0.5F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 2F, 0.25F));
        DamageResistanceHandler.registerSet(
                ASBESTOS_HELMET.get(),
                ASBESTOS_PLATE.get(),
                ASBESTOS_LEGS.get(),
                ASBESTOS_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 10F, 0.9F));
        DamageResistanceHandler.registerSet(
                ZIRCONIUM_LEGS.get(),
                ZIRCONIUM_LEGS.get(),
                ZIRCONIUM_LEGS.get(),
                ZIRCONIUM_LEGS.get(),
                new DamageResistanceHandler.ResistanceStats().setOther(0F, 1F));

        ArmorFullSetBonus.register(
                Suit.SCHRABIDIUM,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.HASTE, 2)
                        .effect(MobEffects.STRENGTH, 2)
                        .effect(MobEffects.JUMP_BOOST, 1)
                        .effect(MobEffects.SPEED, 2)
                        .build());
        DamageResistanceHandler.registerSet(
                SCHRABIDIUM_HELMET.get(),
                SCHRABIDIUM_PLATE.get(),
                SCHRABIDIUM_LEGS.get(),
                SCHRABIDIUM_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 10F, 0.65F)
                        .setOther(5F, 0.5F));
        ArmorFullSetBonus.register(
                Suit.CMB,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.SPEED, 2)
                        .effect(MobEffects.HASTE, 2)
                        .effect(MobEffects.STRENGTH, 4)
                        .build());
        DamageResistanceHandler.registerSet(
                CMB_HELMET.get(),
                CMB_PLATE.get(),
                CMB_LEGS.get(),
                CMB_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 5F, 0.5F)
                        .setOther(5F, 0.25F));
        ArmorFullSetBonus.register(
                Suit.PAA,
                ArmorFullSetBonus.builder().effect(MobEffects.HASTE, 0).noHelmet().build());
        DamageResistanceHandler.registerSet(
                null,
                PAA_PLATE.get(),
                PAA_LEGS.get(),
                PAA_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats());
        DamageResistanceHandler.registerSet(
                ROBES_HELMET.get(),
                ROBES_PLATE.get(),
                ROBES_LEGS.get(),
                ROBES_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats());
        DamageResistanceHandler.registerSet(
                DNT_HELMET.get(),
                DNT_PLATE.get(),
                DNT_LEGS.get(),
                DNT_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats());
        ArmorFullSetBonus.register(
                Suit.EUPHEMIUM,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.REGENERATION, 127)
                        .effect(MobEffects.RESISTANCE, 127)
                        .effect(MobEffects.FIRE_RESISTANCE, 127)
                        .effect(MobEffects.HASTE, 127)
                        .build());
        DamageResistanceHandler.registerSet(
                EUPHEMIUM_HELMET.get(),
                EUPHEMIUM_PLATE.get(),
                EUPHEMIUM_LEGS.get(),
                EUPHEMIUM_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats().setOther(1_000_000F, 1F));

        DamageResistanceHandler.registerSet(
                BISMUTH_HELMET.get(),
                BISMUTH_PLATE.get(),
                BISMUTH_LEGS.get(),
                BISMUTH_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.15F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 5F, 0.5F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 5F, 0.25F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 0F, 1F)
                        .setOther(2F, 0.25F));
        ArmorFullSetBonus.register(
                Suit.BISMUTH,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.JUMP_BOOST, 6)
                        .effect(MobEffects.SPEED, 6)
                        .effect(MobEffects.REGENERATION, 1)
                        .effect(MobEffects.NIGHT_VISION, 0)
                        .dashCount(3)
                        .build());

        ArmorRegistry.registerHazard(AJR_HELMET, fullPackage());
        ArmorRegistry.registerHazard(AJRO_HELMET, fullPackage());
        hazmat(AJR_HELMET, .26);
        hazmat(AJR_PLATE, .52);
        hazmat(AJR_LEGS, .39);
        hazmat(AJR_BOOTS, .13);
        hazmat(AJRO_HELMET, .26);
        hazmat(AJRO_PLATE, .52);
        hazmat(AJRO_LEGS, .39);
        hazmat(AJRO_BOOTS, .13);
        ArmorFullSetBonus ajrBonus =
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.JUMP_BOOST, 0)
                        .effect(MobEffects.STRENGTH, 0)
                        .vats()
                        .geigerSound()
                        .hardLanding()
                        .steps(
                                ModSounds.STEP_METAL,
                                ModSounds.STEP_IRON_JUMP,
                                ModSounds.STEP_IRON_LAND)
                        .drain(25L)
                        .build();
        ArmorFullSetBonus.register(Suit.AJR, ajrBonus);
        ArmorFullSetBonus.register(Suit.AJRO, ajrBonus);
        DamageResistanceHandler.ResistanceStats ajrResist =
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 4F, 0.15F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 0.5F, 0.35F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 7.5F, 0.25F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 0F, 1F)
                        .setOther(0F, 0.15F);
        DamageResistanceHandler.registerSet(
                AJR_HELMET.get(), AJR_PLATE.get(), AJR_LEGS.get(), AJR_BOOTS.get(), ajrResist);
        DamageResistanceHandler.registerSet(
                AJRO_HELMET.get(), AJRO_PLATE.get(), AJRO_LEGS.get(), AJRO_BOOTS.get(), ajrResist);

        ArmorRegistry.registerHazard(RPA_HELMET, fullPackage());
        ArmorRegistry.registerHazard(NCRPA_HELMET, fullPackage());
        hazmat(RPA_HELMET, .4);
        hazmat(RPA_PLATE, .8);
        hazmat(RPA_LEGS, .6);
        hazmat(RPA_BOOTS, .2);
        hazmat(NCRPA_HELMET, .34);
        hazmat(NCRPA_PLATE, .68);
        hazmat(NCRPA_LEGS, .51);
        hazmat(NCRPA_BOOTS, .17);
        ArmorFullSetBonus.register(
                Suit.RPA,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.STRENGTH, 3)
                        .vats()
                        .geigerSound()
                        .hardLanding()
                        .steps(
                                ModSounds.STEP_POWERED,
                                ModSounds.STEP_POWERED,
                                ModSounds.STEP_POWERED)
                        .drain(25L)
                        .build());
        ArmorFullSetBonus.register(
                Suit.NCRPA,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.STRENGTH, 3)
                        .vats()
                        .geigerSound()
                        .hardLanding()
                        .steps(
                                ModSounds.STEP_POWERED,
                                ModSounds.STEP_POWERED,
                                ModSounds.STEP_POWERED)
                        .drain(25L)
                        .build());
        ArmorPAWeapons.registerMelee(Suit.RPA, new ArmorRPAMelee());
        ArmorPAWeapons.registerMelee(Suit.NCRPA, new ArmorNCRPAMelee());
        ArmorPAWeapons.registerRanged(Suit.NCRPA, new ArmorNCRPARanged());
        DamageResistanceHandler.registerSet(
                RPA_HELMET.get(),
                RPA_PLATE.get(),
                RPA_LEGS.get(),
                RPA_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 50F, 0.75F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 25F, 0.9F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 15F, 0.25F)
                        .addCategory(DamageResistanceHandler.CATEGORY_ENERGY, 30F, 0.8F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 0F, 1F)
                        .setOther(15F, 0.45F));
        DamageResistanceHandler.registerSet(
                NCRPA_HELMET.get(),
                NCRPA_PLATE.get(),
                NCRPA_LEGS.get(),
                NCRPA_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 25F, 0.65F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 10F, 0.9F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 15F, 0.25F)
                        .addCategory(DamageResistanceHandler.CATEGORY_ENERGY, 10F, 0.5F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 0F, 1F)
                        .setOther(15F, 0.25F));

        hazmat(BJ_HELMET, .2);
        hazmat(BJ_PLATE, .4);
        hazmat(BJ_PLATE_JETPACK, .4);
        hazmat(BJ_LEGS, .3);
        hazmat(BJ_BOOTS, .1);
        ArmorFullSetBonus.register(
                Suit.BJ,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.SPEED, 1)
                        .effect(MobEffects.JUMP_BOOST, 0)
                        .vats()
                        .thermal()
                        .geigerSound()
                        .hardLanding()
                        .steps(
                                ModSounds.STEP_METAL,
                                ModSounds.STEP_IRON_JUMP,
                                ModSounds.STEP_IRON_LAND)
                        .drain(100L)
                        .build());
        DamageResistanceHandler.ResistanceStats bjResist =
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 5F, 0.5F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 2.5F, 0.5F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 10F, 0.25F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 0F, 1F)
                        .setOther(2F, 0.15F);
        DamageResistanceHandler.registerSet(
                BJ_HELMET.get(), BJ_PLATE.get(), BJ_LEGS.get(), BJ_BOOTS.get(), bjResist);
        DamageResistanceHandler.registerSet(
                BJ_HELMET.get(), BJ_PLATE_JETPACK.get(), BJ_LEGS.get(), BJ_BOOTS.get(), bjResist);

        ArmorRegistry.registerHazard(ENVSUIT_HELMET, fullPackage());
        hazmat(ENVSUIT_HELMET, .2);
        hazmat(ENVSUIT_PLATE, .4);
        hazmat(ENVSUIT_LEGS, .3);
        hazmat(ENVSUIT_BOOTS, .1);
        ArmorFullSetBonus.register(
                Suit.ENVSUIT,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.SPEED, 1)
                        .effect(MobEffects.JUMP_BOOST, 0)
                        .build());
        DamageResistanceHandler.registerSet(
                ENVSUIT_HELMET.get(),
                ENVSUIT_PLATE.get(),
                ENVSUIT_LEGS.get(),
                ENVSUIT_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 2F, 0.75F)
                        .addExact("drown", 0F, 1F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 5F, 0.75F)
                        .setOther(0F, 0.1F));

        ArmorRegistry.registerHazard(HEV_HELMET, fullPackage());
        hazmat(HEV_HELMET, .46);
        hazmat(HEV_PLATE, .92);
        hazmat(HEV_LEGS, .69);
        hazmat(HEV_BOOTS, .23);
        ArmorFullSetBonus.register(
                Suit.HEV,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.SPEED, 1)
                        .effect(MobEffects.JUMP_BOOST, 0)
                        .geigerSound()
                        .geigerHUD()
                        .build());
        DamageResistanceHandler.registerSet(
                HEV_HELMET.get(),
                HEV_PLATE.get(),
                HEV_LEGS.get(),
                HEV_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.25F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 0.5F, 0.5F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 5F, 0.25F)
                        .addExact("onFire", 0F, 1F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 10F, 0F)
                        .setOther(2F, 0.25F));

        ArmorRegistry.registerHazard(FAU_HELMET, fullPackage());
        hazmat(FAU_HELMET, .8);
        hazmat(FAU_PLATE, 1.6);
        hazmat(FAU_LEGS, 1.2);
        hazmat(FAU_BOOTS, .4);
        ArmorFullSetBonus.register(
                Suit.DIGAMMA,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.JUMP_BOOST, 1)
                        .geigerSound()
                        .thermal()
                        .hardLanding()
                        .steps(
                                ModSounds.STEP_METAL,
                                ModSounds.STEP_IRON_JUMP,
                                ModSounds.STEP_IRON_LAND)
                        .build());
        DamageResistanceHandler.registerSet(
                FAU_HELMET.get(),
                FAU_PLATE.get(),
                FAU_LEGS.get(),
                FAU_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 100F, 0.99F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 50F, 0.95F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 100F, 1F)
                        .addExact("laser", 25F, 0.95F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 0F, 1F)
                        .setOther(100F, 0.99F));

        ArmorRegistry.registerHazard(DNS_HELMET, fullPackage());
        hazmat(DNS_HELMET, 1.0);
        hazmat(DNS_PLATE, 2.0);
        hazmat(DNS_LEGS, 1.5);
        hazmat(DNS_BOOTS, .5);
        ArmorFullSetBonus.register(
                Suit.DNS,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.STRENGTH, 9)
                        .effect(MobEffects.HASTE, 7)
                        .effect(MobEffects.JUMP_BOOST, 2)
                        .geigerSound()
                        .vats()
                        .thermal()
                        .hardLanding()
                        .rocketBoots()
                        .fastFall()
                        .sprintBoost()
                        .steps(
                                ModSounds.STEP_METAL,
                                ModSounds.STEP_IRON_JUMP,
                                ModSounds.STEP_IRON_LAND)
                        .drain(115L)
                        .build());
        DamageResistanceHandler.registerSet(
                DNS_HELMET.get(),
                DNS_PLATE.get(),
                DNS_LEGS.get(),
                DNS_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 1000F, 1F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 100F, 0.99F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 0F, 1F)
                        .setOther(1000F, 1F));

        ArmorRegistry.registerHazard(TRENCHMASTER_HELMET, fullPackage());
        hazmat(TRENCHMASTER_HELMET, .2);
        hazmat(TRENCHMASTER_PLATE, .4);
        hazmat(TRENCHMASTER_LEGS, .3);
        hazmat(TRENCHMASTER_BOOTS, .1);
        ArmorFullSetBonus.register(
                Suit.TRENCHMASTER,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.STRENGTH, 2)
                        .effect(MobEffects.HASTE, 1)
                        .effect(MobEffects.JUMP_BOOST, 1)
                        .effect(MobEffects.SPEED, 0)
                        .vats()
                        .moreAmmo()
                        .stepSize(1)
                        .build());
        DamageResistanceHandler.registerSet(
                TRENCHMASTER_HELMET.get(),
                TRENCHMASTER_PLATE.get(),
                TRENCHMASTER_LEGS.get(),
                TRENCHMASTER_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 5F, 0.5F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 5F, 0.5F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 5F, 0.25F)
                        .addExact("laser", 15F, 0.9F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 10F, 0.5F)
                        .setOther(5F, 0.25F));

        ArmorRegistry.registerHazard(STEAMSUIT_HELMET, fullPackage());
        hazmat(STEAMSUIT_HELMET, .26);
        hazmat(STEAMSUIT_PLATE, .52);
        hazmat(STEAMSUIT_LEGS, .39);
        hazmat(STEAMSUIT_BOOTS, .13);
        ArmorFullSetBonus.register(
                Suit.DESH,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.HASTE, 4)
                        .hardLanding()
                        .drain(1L)
                        .build());
        DamageResistanceHandler.registerSet(
                STEAMSUIT_HELMET.get(),
                STEAMSUIT_PLATE.get(),
                STEAMSUIT_LEGS.get(),
                STEAMSUIT_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.15F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 0.5F, 0.25F)
                        .addExact(DamageResistanceHandler.EXACT_FALL, 5F, 0.25F)
                        .setOther(0F, 0.1F));

        ArmorFullSetBonus.register(
                Suit.DIESEL,
                ArmorFullSetBonus.builder()
                        .effect(MobEffects.SPEED, 2)
                        .effect(MobEffects.JUMP_BOOST, 2)
                        .thermal()
                        .vats()
                        .drain(1L)
                        .build());
        DamageResistanceHandler.registerSet(
                DIESELSUIT_HELMET.get(),
                DIESELSUIT_PLATE.get(),
                DIESELSUIT_LEGS.get(),
                DIESELSUIT_BOOTS.get(),
                new DamageResistanceHandler.ResistanceStats()
                        .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 1F, 0.15F)
                        .addCategory(DamageResistanceHandler.CATEGORY_FIRE, 0.5F, 0.5F)
                        .addCategory(DamageResistanceHandler.CATEGORY_EXPLOSION, 2F, 0.15F)
                        .setOther(0F, 0.1F));
    }

    private static HazardClass[] fullNoLight() {
        return new HazardClass[] {
            HazardClass.PARTICLE_COARSE,
            HazardClass.PARTICLE_FINE,
            HazardClass.GAS_LUNG,
            HazardClass.BACTERIA,
            HazardClass.GAS_BLISTERING,
            HazardClass.GAS_MONOXIDE,
            HazardClass.SAND
        };
    }

    private static HazardClass[] fullPackage() {
        return new HazardClass[] {
            HazardClass.PARTICLE_COARSE,
            HazardClass.PARTICLE_FINE,
            HazardClass.GAS_LUNG,
            HazardClass.BACTERIA,
            HazardClass.GAS_BLISTERING,
            HazardClass.GAS_MONOXIDE,
            HazardClass.LIGHT,
            HazardClass.SAND
        };
    }

    private static void hazmat(ItemLike item, double value) {
        HazmatRegistry.register(item, value);
    }

    private static RegistryHandle<ItemHazmatMask> hazmatMask(
            String name, ArmorMaterial material, Suit suit, ItemHazmatMask.Variant variant) {
        RegistryHandle<ItemHazmatMask> result =
                Reg.item(
                        name,
                        p -> new ItemHazmatMask(p, suit, variant),
                        () -> new Item.Properties().humanoidArmor(material, ArmorType.HELMET));
        ARMOR_ALL.add(result);
        return result;
    }

    private static Reg.Handle<ModArmorItem> armor(
            String name, ArmorMaterial material, ArmorType type, Suit suit) {
        return armor(name, material, type, suit, false);
    }

    private static Reg.Handle<ModArmorItem> trimmableArmor(
            String name, ArmorMaterial material, ArmorType type, Suit suit) {
        return armor(name, material, type, suit);
    }

    private static Reg.Handle<ModArmorItem> trimmableArmor(
            String name, ArmorMaterial material, ArmorType type, Suit suit, boolean wearless) {
        return armor(name, material, type, suit, wearless);
    }

    private static Reg.Handle<ModArmorItem> armor(
            String name, ArmorMaterial material, ArmorType type, Suit suit, boolean wearless) {
        Reg.Handle<ModArmorItem> result =
                Reg.item(
                        name,
                        p -> new ModArmorItem(p, suit),
                        () ->
                                wearless
                                        ? wearlessArmor(material, type, suit)
                                        : humanoidArmorProps(material, type, suit));
        ARMOR_ALL.add(result);
        return result;
    }

    private static Reg.Handle<ModArmorItemFueled> armorFueled(
            String name,
            ArmorMaterial material,
            ArmorType type,
            Suit suit,
            Supplier<Fluid> fuel,
            int maxFuel,
            int fillRate,
            int consumption) {
        Reg.Handle<ModArmorItemFueled> result =
                Reg.item(
                        name,
                        p -> new ModArmorItemFueled(p, suit, fuel, maxFuel, fillRate, consumption),
                        () -> suppliedArmor(material, type, suit));
        ARMOR_ALL.add(result);
        return result;
    }

    private static Reg.Handle<ModArmorItemPowered> poweredArmor(
            String name,
            ArmorMaterial material,
            ArmorType type,
            Suit suit,
            long maxPower,
            long chargeRate,
            long consumption) {
        Reg.Handle<ModArmorItemPowered> result =
                Reg.item(
                        name,
                        p -> new ModArmorItemPowered(p, suit, maxPower, chargeRate, consumption),
                        () -> suppliedArmor(material, type, suit));
        ARMOR_ALL.add(result);
        return result;
    }

    private static RegistryHandle<ModArmorItem> liquidator(String name, ArmorType type) {
        RegistryHandle<ModArmorItem> result =
                Reg.item(
                        name,
                        p ->
                                type == ArmorType.HELMET
                                        ? new ItemLiquidatorMask(p)
                                        : new ModArmorItem(p, Suit.LIQUIDATOR),
                        () -> {
                            ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
                            for (ItemAttributeModifiers.Entry e :
                                    LIQUIDATOR_MAT.createAttributes(type).modifiers())
                                b.add(e.attribute(), e.modifier(), e.slot(), e.display());
                            EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
                            b.add(
                                    Attributes.KNOCKBACK_RESISTANCE,
                                    new AttributeModifier(
                                            Library.id("liquidator_knockback_" + type.getName()),
                                            100D,
                                            AttributeModifier.Operation.ADD_VALUE),
                                    group);
                            b.add(
                                    Attributes.MOVEMENT_SPEED,
                                    new AttributeModifier(
                                            Library.id("liquidator_speed_" + type.getName()),
                                            -0.1D,
                                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                                    group);
                            return new Item.Properties()
                                    .humanoidArmor(LIQUIDATOR_MAT, type)
                                    .attributes(b.build());
                        });
        ARMOR_ALL.add(result);
        return result;
    }

    private static Item.Properties runeProperties() {
        return new Item.Properties()
                .stacksTo(1)
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    private static RegistryHandle<ItemSingularity> singularity(
            String name, BiFunction<Level, Float, Entity> factory, float size) {
        return Reg.item(
                name,
                props -> new ItemSingularity(factory, size, props),
                () -> new Item.Properties().stacksTo(1).craftRemainder(NUCLEAR_WASTE.get()));
    }

    private static RegistryHandle<ItemCatalyst> catalyst(String material, int color) {
        return Reg.item(
                        "ams_catalyst_" + material,
                        props -> new ItemCatalyst(color, props),
                        () -> new Item.Properties().stacksTo(1))
                .addTo(MISC_ALL);
    }

    private static RegistryHandle<ItemAMSCore> amsCore(
            String kind, int multiplier, Rarity rarity, boolean foil) {
        return Reg.item(
                        "ams_core_" + kind,
                        props -> new ItemAMSCore(multiplier, foil, props),
                        () -> new Item.Properties().stacksTo(1).rarity(rarity))
                .addTo(MISC_ALL);
    }

    private static RegistryHandle<ItemGasFilter> filter(String name) {
        return Reg.item(name, ItemGasFilter::new, () -> new Item.Properties().durability(20_000))
                .addTo(ARMOR_ALL);
    }

    private static RegistryHandle<ItemModInsert> insert(
            String name,
            int durability,
            float damage,
            float projectile,
            float explosion,
            float speed) {
        return Reg.item(
                        name,
                        props ->
                                new ItemModInsert(
                                        props, durability, damage, projectile, explosion, speed),
                        Item.Properties::new)
                .addTo(ARMOR_MOD_ALL);
    }

    private static RegistryHandle<ItemModCladding> cladding(String name, double rad) {
        return Reg.item(name, props -> new ItemModCladding(props, rad), Item.Properties::new)
                .addTo(ARMOR_MOD_ALL);
    }

    private static RegistryHandle<ItemModPads> pads(String name, float damageMod) {
        return Reg.item(name, props -> new ItemModPads(props, damageMod), Item.Properties::new)
                .addTo(ARMOR_MOD_ALL);
    }

    private static RegistryHandle<ItemModBattery> armorBattery(String name, double mod) {
        return Reg.item(name, props -> new ItemModBattery(props, mod), Item.Properties::new)
                .addTo(ARMOR_MOD_ALL);
    }

    private static RegistryHandle<ItemModLodestone> magnet(String name, int range) {
        return Reg.item(name, props -> new ItemModLodestone(props, range), Item.Properties::new)
                .addTo(ARMOR_MOD_ALL);
    }

    private static Item.Properties bespokeHeadArmor() {
        return bespokeHeadArmor(IRON_MAT);
    }

    private static Item.Properties bespokeHeadArmor(ArmorMaterial material) {
        return new Item.Properties()
                .humanoidArmor(material, ArmorType.HELMET)
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(ArmorType.HELMET.getSlot())
                                .setEquipSound(material.equipSound())
                                .build());
    }

    private static Reg.Handle<ModArmorItem> undamageableArmor(
            String name, ArmorMaterial material, ArmorType type, Suit suit) {
        Reg.Handle<ModArmorItem> result =
                Reg.item(
                        name,
                        p -> new ModArmorItem(p, suit),
                        () -> undamageableArmor(material, type, suit));
        ARMOR_ALL.add(result);
        return result;
    }

    private static Item.Properties undamageableArmor(
            ArmorMaterial material, ArmorType type, Suit suit) {
        Item.Properties p =
                new Item.Properties()
                        .stacksTo(1)
                        .attributes(material.createAttributes(type))
                        .component(
                                DataComponents.EQUIPPABLE,
                                equippable(material, type, suit).build());
        if (material.enchantmentValue() > 0) p.enchantable(material.enchantmentValue());
        return p;
    }

    private static Item.Properties wearlessArmor(
            ArmorMaterial material, ArmorType type, Suit suit) {
        return humanoidArmorProps(material, type, suit)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(
                        DataComponents.TOOLTIP_DISPLAY,
                        TooltipDisplay.DEFAULT.withHidden(DataComponents.UNBREAKABLE, true));
    }

    private static Item.Properties suppliedArmor(
            ArmorMaterial material, ArmorType type, Suit suit) {
        return humanoidArmorProps(material, type, suit)
                .component(
                        DataComponents.EQUIPPABLE,
                        equippable(material, type, suit).setDamageOnHurt(false).build());
    }

    private static Item.Properties humanoidArmorProps(
            ArmorMaterial material, ArmorType type, Suit suit) {
        Item.Properties p =
                new Item.Properties()
                        .durability(type.getDurability(material.durability()))
                        .attributes(material.createAttributes(type))
                        .component(
                                DataComponents.EQUIPPABLE, equippable(material, type, suit).build())
                        .repairable(material.repairIngredient());
        if (material.enchantmentValue() > 0) p.enchantable(material.enchantmentValue());
        return p;
    }

    private static Equippable.Builder equippable(
            ArmorMaterial material, ArmorType type, Suit suit) {
        Equippable.Builder builder =
                Equippable.builder(type.getSlot()).setEquipSound(material.equipSound());
        return suit.objMesh() ? builder : builder.setAsset(material.assetId());
    }

    public static TagKey<Item> armorRepair(String material) {
        return TagKey.create(Registries.ITEM, Library.id("armor_repair/" + material));
    }

    private static ArmorMaterial material(
            String name,
            int durability,
            int helmet,
            int chest,
            int legs,
            int boots,
            int enchant,
            TagKey<Item> repair) {
        return new ArmorMaterial(
                durability,
                Map.of(
                        ArmorType.HELMET,
                        helmet,
                        ArmorType.CHESTPLATE,
                        chest,
                        ArmorType.LEGGINGS,
                        legs,
                        ArmorType.BOOTS,
                        boots),
                enchant,
                SoundEvents.ARMOR_EQUIP_GENERIC,
                0F,
                0F,
                repair,
                ResourceKey.create(EquipmentAssets.ROOT_ID, Library.id(name)));
    }

    private static ArmorMaterial vanilla(ArmorMaterial base, String name) {
        return new ArmorMaterial(
                base.durability(),
                base.defense(),
                base.enchantmentValue(),
                base.equipSound(),
                base.toughness(),
                base.knockbackResistance(),
                base.repairIngredient(),
                ResourceKey.create(EquipmentAssets.ROOT_ID, Library.id(name)));
    }

    private static Reg.Handle<Item> residualItem(String name) {
        return Reg.item(name, Item::new, Item.Properties::new);
    }

    private static Reg.Handle<Item> rawMaterial(String id, String material, String name) {
        Reg.Handle<Item> handle = Reg.item("raw_" + id, Item::new, Item.Properties::new);
        RAW_MATERIALS.add(handle);
        return handle;
    }

    private static Reg.Handle<ItemCustomLore> residualLore(String name) {
        return Reg.item(name, ItemCustomLore::new, Item.Properties::new);
    }

    private static RegistryHandle<ItemSpawnEgg> spawnEgg(
            String entity,
            Supplier<? extends Supplier<? extends EntityType<?>>> type,
            int primary,
            int secondary) {
        return Reg.item(
                entity + "_spawn_egg",
                ItemSpawnEgg::new,
                () ->
                        new Item.Properties()
                                .delayedComponent(
                                        DataComponents.ENTITY_DATA,
                                        context ->
                                                TypedEntityData.of(
                                                        type.get().get(), new CompoundTag())));
    }

    private static Reg.Handle<ItemNuclearWaste> waste(String name) {
        return Reg.item(name, ItemNuclearWaste::new, Item.Properties::new);
    }

    private static ItemFamily<ItemWasteLong.WasteClass, ItemWasteLong> wasteLong(String name) {
        return Reg.family(
                name, ItemWasteLong.WasteClass.class, ItemWasteLong::new, Item.Properties::new);
    }

    private static ItemFamily<ItemWasteShort.WasteClass, ItemWasteShort> wasteShort(String name) {
        return Reg.family(
                name, ItemWasteShort.WasteClass.class, ItemWasteShort::new, Item.Properties::new);
    }

    private static Reg.Handle<ItemRBMKRod> rbmkFuel(
            String name, String fullName, Configurator cfg) {
        Reg.Handle<ItemRBMKRod> handle =
                Reg.item(
                        name,
                        props -> cfg.apply(new ItemRBMKRod(props, fullName)),
                        () ->
                                new Item.Properties()
                                        .stacksTo(1)
                                        .craftRemainder(RBMK_FUEL_EMPTY.get()));
        RBMK_FUELS.add(handle);
        return handle;
    }

    private static RegistryHandle<ItemRBMKPellet> pellet(
            RegistryHandle<ItemRBMKRod> rod, String name, String fullName) {
        return pellet(rod, name, fullName, false);
    }

    private static RegistryHandle<ItemRBMKPellet> pellet(
            RegistryHandle<ItemRBMKRod> rod, String name, String fullName, boolean disableXenon) {
        int metas = disableXenon ? 5 : 10;
        RegistryHandle<ItemRBMKPellet> handle =
                Reg.item(
                                name,
                                props -> {
                                    ItemRBMKPellet p = new ItemRBMKPellet(props, fullName);
                                    if (disableXenon) p.disableXenon();
                                    return p;
                                },
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        ModDataComponents.RBMK_PELLET.get(),
                                                        ItemRBMKPellet.Stage.FRESH))
                        .state(ModDataComponents.RBMK_PELLET)
                        .afterRegistration(pellet -> rod.get().pellet = pellet);
        PELLETS.add(handle);
        CRAFTABLE_RODS.add(rod);
        return handle;
    }

    public static ItemPWRFuel cold(EnumPWRFuel fuel) {
        return PWR_FUELS.get(fuel.ordinal()).get();
    }

    private static RegistryHandle<ItemDepletedFuel> depletedFuel(String name) {
        RegistryHandle<ItemDepletedFuel> h =
                Reg.item(
                                name,
                                ItemDepletedFuel::new,
                                () ->
                                        new Item.Properties()
                                                .component(
                                                        ModDataComponents.DECAY_HEAT.get(), false))
                        .state(ModDataComponents.DECAY_HEAT);
        DEPLETED_ALL.add(h);
        return h;
    }

    public static Item getDepleted(DepletedRTGMaterial mat) {
        return RP_DEPLETED[mat.ordinal()].get();
    }

    private static Reg.Handle<ItemRTGPellet> rtgPellet(
            String name, int heat, DepletedRTGMaterial mat, float halfLife, HalfLifeType type) {
        long halflife = RTGUtil.getLifespan(halfLife, type, false);
        return Reg.item(
                name,
                props -> new ItemRTGPellet(props, heat).setDecays(mat, halflife, 1.5D),
                () -> new Item.Properties().stacksTo(1));
    }

    private static Reg.Handle<ItemZirnoxRodDepleted> dep(
            EnumZirnoxType type, float wasteRad, float blinding) {
        Reg.Handle<ItemZirnoxRodDepleted> handle =
                Reg.item(
                        "rod_zirnox_depleted_" + type.id,
                        props -> new ItemZirnoxRodDepleted(props, wasteRad, blinding),
                        () -> new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get()));
        DEPLETED[type.ordinal()] = handle;
        ZIRNOX_ALL.add(handle);
        return handle;
    }

    public static ItemZirnoxRodDepleted zirnoxDepleted(EnumZirnoxType type) {
        RegistryHandle<ItemZirnoxRodDepleted> handle = DEPLETED[type.ordinal()];
        if (handle == null)
            throw new IllegalStateException(type + " has no depleted rod of its own");
        return handle.get();
    }

    public static ItemBatteryPack batteryPack(EnumBatteryPack tier) {
        return BATTERY_PACKS[tier.ordinal()].get();
    }

    public static ItemZirnoxRod zirnoxFuel(EnumZirnoxType type) {
        return FUEL[type.ordinal()].get();
    }

    private static Reg.Handle<ItemPlateFuel> plateFuel(
            String name,
            int life,
            ItemPlateFuel.FunctionEnum function,
            int reactivity,
            RegistryHandle<ItemDepletedFuel> spent) {
        Reg.Handle<ItemPlateFuel> handle =
                Reg.item(
                        name,
                        props -> new ItemPlateFuel(props, life, function, reactivity, spent::get),
                        () -> new Item.Properties().stacksTo(1));
        PLATE_FUELS.add(handle);
        return handle;
    }

    private static Reg.Handle<ItemPileRod> rod(String name) {
        Reg.Handle<ItemPileRod> handle = Reg.item(name, ItemPileRod::new, Item.Properties::new);
        PILE_ALL.add(handle);
        return handle;
    }

    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> dotted(
            String base,
            Class<E> type,
            BiFunction<Item.Properties, E, I> factory,
            Supplier<Item.Properties> props) {
        return Reg.family(base, type, factory, props);
    }

    private interface Configurator {
        ItemRBMKRod apply(ItemRBMKRod rod);
    }
}
