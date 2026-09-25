// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.handler.MissileStruct;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.ICFPelletData;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemScraps;
import com.hbm.items.machine.RBMKFuelData;
import com.hbm.items.special.BedrockOreSample;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.tool.ConveyorRunData;
import com.hbm.items.tool.ItemCMStructure;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.items.weapon.sedna.GunStateData;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.machine.LockStateData;
import com.hbm.tileentity.machine.oil.DrillContents;
import com.hbm.tileentity.machine.oil.RefineryContents;
import com.hbm.tileentity.machine.storage.BatteryCharge;
import com.hbm.tileentity.machine.storage.CapacitorCharge;
import com.hbm.tileentity.machine.storage.FluidTankContents;
import com.hbm.tileentity.machine.storage.LockedContents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

public final class ModDataComponents {

    private static final int MAX_TOOL_PRESETS = 99;

    private static final Codec<BigInteger> BIG_INTEGER =
            Codec.STRING.comapFlatMap(
                    text -> {
                        try {
                            return DataResult.success(new BigInteger(text));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a decimal integer: " + text);
                        }
                    },
                    BigInteger::toString);

    private static final StreamCodec<ByteBuf, BigInteger> BIG_INTEGER_STREAM =
            ByteBufCodecs.STRING_UTF8.map(BigInteger::new, BigInteger::toString);

    public static RegistryHandle<DataComponentType<Integer>> FOOD_ADDITIVES;

    public static RegistryHandle<DataComponentType<Boolean>> DECAY_HEAT;
    public static RegistryHandle<DataComponentType<Boolean>> ROASTED;

    public static RegistryHandle<DataComponentType<Boolean>> POLARIZED;
    public static RegistryHandle<DataComponentType<Integer>> FORGE_COUNT;
    public static RegistryHandle<DataComponentType<ItemRBMKPellet.Stage>> RBMK_PELLET;
    public static RegistryHandle<DataComponentType<ItemBedrockOreNew.Ore>> BEDROCK_ORE;
    public static RegistryHandle<DataComponentType<BedrockOreSample>> BEDROCK_ORE_SAMPLE;

    public static RegistryHandle<DataComponentType<Boolean>> HAS_COG;

    public static RegistryHandle<DataComponentType<Boolean>> HAS_BLADE;
    public static RegistryHandle<DataComponentType<Integer>> CANTEEN_COOLDOWN;
    public static RegistryHandle<DataComponentType<Integer>> PIPETTE_CAPACITY;

    public static RegistryHandle<DataComponentType<ItemModMinecart.EnumCartBase>> CART_BASE;

    public static RegistryHandle<DataComponentType<Integer>> HOT_ITEM_HEAT;

    public static RegistryHandle<DataComponentType<Long>> HOT_UNTIL;

    public static RegistryHandle<DataComponentType<Long>> UNSTABLE_AT;

    public static RegistryHandle<DataComponentType<Long>> BATTERY_CHARGE;

    public static RegistryHandle<DataComponentType<BatteryCharge>> MACHINE_BATTERY_STATE;

    public static RegistryHandle<DataComponentType<CapacitorCharge>> CAPACITOR_STATE;

    public static RegistryHandle<DataComponentType<DrillContents>> DRILL_CONTENTS;

    public static RegistryHandle<DataComponentType<RefineryContents>> REFINERY_CONTENTS;
    public static RegistryHandle<DataComponentType<FluidTankContents>> FLUID_TANK_CONTENTS;

    public static RegistryHandle<DataComponentType<List<FluidStackNTM>>> TANK_CONTENTS;

    public static RegistryHandle<DataComponentType<BigInteger>> REDD_CHARGE;

    public static RegistryHandle<DataComponentType<CustomData>> PERSISTENT_DATA;

    public static RegistryHandle<DataComponentType<LockStateData>> LOCK_STATE;
    public static RegistryHandle<DataComponentType<LockedContents>> LOCKED_CONTENTS;

    public static RegistryHandle<DataComponentType<Unit>> SPIDERS;

    public static RegistryHandle<DataComponentType<Long>> STACKLOCK;

    public static RegistryHandle<DataComponentType<Integer>> MASS_STOCKPILE;

    public static RegistryHandle<DataComponentType<GunStateData>> GUN_STATE;

    public static RegistryHandle<DataComponentType<Boolean>> AKIMBO_GHOST;

    public static RegistryHandle<DataComponentType<Boolean>> NO9_LAMP;
    public static RegistryHandle<DataComponentType<Boolean>> CONTAGION;
    public static RegistryHandle<DataComponentType<Boolean>> TOOLBOX_OPEN;

    public static RegistryHandle<DataComponentType<Boolean>> MUFFLED;

    public static RegistryHandle<DataComponentType<Map<String, Float>>> CASING_PROGRESS;
    public static RegistryHandle<DataComponentType<Integer>> POTATO_TIMER;

    public static RegistryHandle<DataComponentType<Integer>> B92_ENERGY;
    public static RegistryHandle<DataComponentType<Integer>> B92_ANIMATION;

    public static RegistryHandle<DataComponentType<Long>> ANCHOR_POS;

    public static RegistryHandle<DataComponentType<Long>> REACTOR_POS;

    public static RegistryHandle<DataComponentType<GrenadeData>> GRENADE;

    public static RegistryHandle<DataComponentType<CustomData>> SETTINGS_TOOL_DATA;

    public static RegistryHandle<DataComponentType<FluidIdentifierData>> FLUID_IDENTIFIER;

    public static RegistryHandle<DataComponentType<ResourceKey<CustomMachineDefinition>>>
            CUSTOM_MACHINE_TYPE;

    public static RegistryHandle<DataComponentType<ItemCMStructure.Selection>> CM_STRUCTURE;

    public static RegistryHandle<DataComponentType<Integer>> HBM_PRESSURE;

    public static RegistryHandle<DataComponentType<Float>> HAZ_RADIATION;

    public static RegistryHandle<DataComponentType<Float>> NEUTRON_ACTIVATION;

    public static RegistryHandle<DataComponentType<FluidStackNTM>> FLUID_CONTENT;

    public static RegistryHandle<DataComponentType<Long>> TARGET_DESIGNATOR;

    public static RegistryHandle<DataComponentType<Long>> DETONATOR_POS;

    public static RegistryHandle<DataComponentType<List<Long>>> DETONATOR_POS_LIST;

    public static RegistryHandle<DataComponentType<ConveyorRunData>> CONVEYOR_RUN;
    public static RegistryHandle<DataComponentType<List<ToolPreset>>> TOOL_PRESETS;
    public static RegistryHandle<DataComponentType<Integer>> TOOL_PRESET_INDEX;

    public static RegistryHandle<DataComponentType<RBMKFuelData>> RBMK_FUEL;

    public static RegistryHandle<DataComponentType<Long>> RBMK_TOOL_TARGET;

    public static RegistryHandle<DataComponentType<Long>> MIRROR_TOOL_TARGET;

    public static RegistryHandle<DataComponentType<Long>> ARTY_DESIGNATOR_TARGET;

    public static RegistryHandle<DataComponentType<Long>> RADAR_LINKER_TARGET;

    public static RegistryHandle<DataComponentType<ItemStackTemplate>> ARTY_CARGO;

    public static RegistryHandle<DataComponentType<Long>> PELLET_DEPLETION;

    public static RegistryHandle<DataComponentType<ICFPelletData>> ICF_PELLET;

    public static RegistryHandle<DataComponentType<Integer>> ZIRNOX_LIFE;

    public static RegistryHandle<DataComponentType<Integer>> PLATE_FUEL_LIFE;

    public static RegistryHandle<DataComponentType<Double>> WATZ_YIELD;

    public static RegistryHandle<DataComponentType<ItemScraps.ScrapData>> SCRAP;
    public static RegistryHandle<DataComponentType<Identifier>> BROKEN_ITEM_SOURCE;

    public static RegistryHandle<DataComponentType<Long>> WRENCH_TARGET;

    public static RegistryHandle<DataComponentType<Long>> REBAR_CORNER;

    public static RegistryHandle<DataComponentType<ItemContainerContents>> REBAR_CONCRETE;

    public static RegistryHandle<DataComponentType<Long>> WIRE_TARGET;

    public static RegistryHandle<DataComponentType<Long>> DRONE_LINK_TARGET;

    public static RegistryHandle<DataComponentType<GlobalPos>> LINKER_TARGET;

    public static RegistryHandle<DataComponentType<String>> BLUEPRINT_POOL;

    public static RegistryHandle<DataComponentType<List<String>>> TURRET_WHITELIST;

    public static RegistryHandle<DataComponentType<String>> RTTY_PAGER_CHANNEL;

    public static RegistryHandle<DataComponentType<Long>> TABLET_SEED;

    public static RegistryHandle<DataComponentType<String>> LORE_BOOK_KEY;
    public static RegistryHandle<DataComponentType<Integer>> LORE_BOOK_PAGES;
    public static RegistryHandle<DataComponentType<Integer>> LORE_BOOK_COVER;
    public static RegistryHandle<DataComponentType<Integer>> LORE_BOOK_TITLE;

    public static RegistryHandle<DataComponentType<Map<String, List<String>>>> LORE_BOOK_ARGS;

    public static RegistryHandle<DataComponentType<Integer>> ARC_ELECTRODE_DURABILITY;

    public static RegistryHandle<DataComponentType<List<Integer>>> PNEUMATIC_BULK;

    public static RegistryHandle<DataComponentType<MissileStruct>> MISSILE_PARTS;

    public static RegistryHandle<DataComponentType<Item>> MISSILE_CHIP;

    public static RegistryHandle<DataComponentType<List<ItemStackTemplate>>> KIT_CONTENTS;

    public static RegistryHandle<DataComponentType<Integer>> KIT_COLOR_1;
    public static RegistryHandle<DataComponentType<Integer>> KIT_COLOR_2;

    private ModDataComponents() {}

    public static void registerBlockItemStates(IRegistrar r) {
        HAS_COG =
                r.registerDataComponent(
                        "has_cog",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        HAS_BLADE =
                r.registerDataComponent(
                        "has_blade",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
    }

    public static void register(IRegistrar r) {
        DECAY_HEAT =
                r.registerDataComponent(
                        "decay_heat",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        ROASTED =
                r.registerDataComponent(
                        "roasted",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        POLARIZED =
                r.registerDataComponent(
                        "polarized",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        FORGE_COUNT =
                r.registerDataComponent(
                        "forge_count",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.intRange(0, 9))
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        RBMK_PELLET =
                r.registerDataComponent(
                        "rbmk_pellet",
                        DataComponentType.<ItemRBMKPellet.Stage>builder()
                                .persistent(ItemRBMKPellet.Stage.CODEC)
                                .networkSynchronized(ItemRBMKPellet.Stage.STREAM_CODEC)
                                .build());
        BEDROCK_ORE =
                r.registerDataComponent(
                        "bedrock_ore",
                        DataComponentType.<ItemBedrockOreNew.Ore>builder()
                                .persistent(ItemBedrockOreNew.Ore.CODEC)
                                .networkSynchronized(ItemBedrockOreNew.Ore.STREAM_CODEC)
                                .build());
        BEDROCK_ORE_SAMPLE =
                r.registerDataComponent(
                        "bedrock_ore_sample",
                        DataComponentType.<BedrockOreSample>builder()
                                .persistent(BedrockOreSample.CODEC)
                                .networkSynchronized(BedrockOreSample.STREAM_CODEC)
                                .build());
        FOOD_ADDITIVES =
                r.registerDataComponent(
                        "food_additives",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.intRange(0, 3))
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        PIPETTE_CAPACITY =
                r.registerDataComponent(
                        "pipette_capacity",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.intRange(1, 1_000))
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .ignoreSwapAnimation()
                                .build());
        CANTEEN_COOLDOWN =
                r.registerDataComponent(
                        "canteen_cooldown",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.intRange(0, 180))
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .ignoreSwapAnimation()
                                .build());
        CART_BASE =
                r.registerDataComponent(
                        "cart_base",
                        DataComponentType.<ItemModMinecart.EnumCartBase>builder()
                                .persistent(ItemModMinecart.EnumCartBase.CODEC)
                                .networkSynchronized(
                                        ByteBufCodecs.idMapper(
                                                i -> ItemModMinecart.EnumCartBase.VALUES[i],
                                                ItemModMinecart.EnumCartBase::ordinal))
                                .build());
        HOT_ITEM_HEAT =
                r.registerDataComponent(
                        "hot_item_heat",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .ignoreSwapAnimation()
                                .build());
        UNSTABLE_AT =
                r.registerDataComponent(
                        "unstable_at",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .ignoreSwapAnimation()
                                .build());
        HOT_UNTIL =
                r.registerDataComponent(
                        "hot_until",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .ignoreSwapAnimation()
                                .build());
        BATTERY_CHARGE =
                r.registerDataComponent(
                        "battery_charge",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());

        PERSISTENT_DATA =
                r.registerDataComponent(
                        "persistent_data",
                        DataComponentType.<CustomData>builder()
                                .persistent(CustomData.CODEC)
                                .build());
        LOCKED_CONTENTS =
                r.registerDataComponent(
                        "locked_contents",
                        DataComponentType.<LockedContents>builder()
                                .persistent(LockedContents.CODEC)
                                .networkSynchronized(LockedContents.STREAM_CODEC)
                                .build());
        SPIDERS =
                r.registerDataComponent(
                        "spiders",
                        DataComponentType.<Unit>builder()
                                .persistent(Unit.CODEC)
                                .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
                                .build());
        STACKLOCK =
                r.registerDataComponent(
                        "stacklock",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.LONG)
                                .ignoreSwapAnimation()
                                .build());
        LOCK_STATE =
                r.registerDataComponent(
                        "lock_state",
                        DataComponentType.<LockStateData>builder()
                                .persistent(LockStateData.CODEC)
                                .networkSynchronized(LockStateData.STREAM_CODEC)
                                .build());
        MASS_STOCKPILE =
                r.registerDataComponent(
                        "mass_stockpile",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());

        GUN_STATE =
                r.registerDataComponent(
                        "gun_state",
                        DataComponentType.<GunStateData>builder()
                                .persistent(GunStateData.CODEC)
                                .ignoreSwapAnimation()
                                .build());
        AKIMBO_GHOST =
                r.registerDataComponent(
                        "akimbo_ghost",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        NO9_LAMP =
                r.registerDataComponent(
                        "no9_lamp",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        CONTAGION =
                r.registerDataComponent(
                        "contagion",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        TOOLBOX_OPEN =
                r.registerDataComponent(
                        "toolbox_open",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        MUFFLED =
                r.registerDataComponent(
                        "muffled",
                        DataComponentType.<Boolean>builder()
                                .persistent(Codec.BOOL)
                                .networkSynchronized(ByteBufCodecs.BOOL)
                                .build());
        ANCHOR_POS =
                r.registerDataComponent(
                        "anchor_pos",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        REACTOR_POS =
                r.registerDataComponent(
                        "reactor_pos",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        POTATO_TIMER =
                r.registerDataComponent(
                        "potato_timer",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        B92_ENERGY =
                r.registerDataComponent(
                        "b92_energy",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        B92_ANIMATION =
                r.registerDataComponent(
                        "b92_animation",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        CASING_PROGRESS =
                r.registerDataComponent(
                        "casing_progress",
                        DataComponentType.<Map<String, Float>>builder()
                                .persistent(Codec.unboundedMap(Codec.STRING, Codec.FLOAT))
                                .networkSynchronized(
                                        ByteBufCodecs.map(
                                                HashMap::new,
                                                ByteBufCodecs.STRING_UTF8,
                                                ByteBufCodecs.FLOAT))
                                .build());
        GRENADE =
                r.registerDataComponent(
                        "grenade",
                        DataComponentType.<GrenadeData>builder()
                                .persistent(GrenadeData.CODEC)
                                .networkSynchronized(GrenadeData.STREAM_CODEC)
                                .build());
        SETTINGS_TOOL_DATA =
                r.registerDataComponent(
                        "settings_tool_data",
                        DataComponentType.<CustomData>builder()
                                .persistent(CustomData.CODEC)
                                .ignoreSwapAnimation()
                                .build());
        FLUID_IDENTIFIER =
                r.registerDataComponent(
                        "fluid_identifier",
                        DataComponentType.<FluidIdentifierData>builder()
                                .persistent(FluidIdentifierData.CODEC)
                                .networkSynchronized(FluidIdentifierData.STREAM_CODEC)
                                .build());
        CM_STRUCTURE =
                r.registerDataComponent(
                        "cm_structure",
                        DataComponentType.<ItemCMStructure.Selection>builder()
                                .persistent(ItemCMStructure.Selection.CODEC)
                                .networkSynchronized(ItemCMStructure.Selection.STREAM_CODEC)
                                .build());
        CUSTOM_MACHINE_TYPE =
                r.registerDataComponent(
                        "custom_machine_type",
                        DataComponentType.<ResourceKey<CustomMachineDefinition>>builder()
                                .persistent(ResourceKey.codec(CustomMachineDefinition.REGISTRY))
                                .networkSynchronized(
                                        ResourceKey.streamCodec(CustomMachineDefinition.REGISTRY))
                                .build());
        HBM_PRESSURE =
                r.registerDataComponent(
                        "pressure",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        PNEUMATIC_BULK =
                r.registerDataComponent(
                        "pneumatic_bulk",
                        DataComponentType.<List<Integer>>builder()
                                .persistent(Codec.INT.listOf())
                                .networkSynchronized(
                                        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()))
                                .build());
        TURRET_WHITELIST =
                r.registerDataComponent(
                        "turret_whitelist",
                        DataComponentType.<List<String>>builder()
                                .persistent(Codec.STRING.listOf())
                                .networkSynchronized(
                                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                                .build());
        HAZ_RADIATION =
                r.registerDataComponent(
                        "haz_radiation",
                        DataComponentType.<Float>builder()
                                .persistent(Codec.FLOAT)
                                .networkSynchronized(ByteBufCodecs.FLOAT)
                                .build());
        NEUTRON_ACTIVATION =
                r.registerDataComponent(
                        "neutron_activation",
                        DataComponentType.<Float>builder()
                                .persistent(Codec.FLOAT)
                                .networkSynchronized(ByteBufCodecs.FLOAT)
                                .build());
        CAPACITOR_STATE =
                r.registerDataComponent(
                        "capacitor_state",
                        DataComponentType.<CapacitorCharge>builder()
                                .persistent(CapacitorCharge.CODEC)
                                .networkSynchronized(CapacitorCharge.STREAM_CODEC)
                                .build());
        DRILL_CONTENTS =
                r.registerDataComponent(
                        "drill_contents",
                        DataComponentType.<DrillContents>builder()
                                .persistent(DrillContents.CODEC)
                                .networkSynchronized(DrillContents.STREAM_CODEC)
                                .build());
        REFINERY_CONTENTS =
                r.registerDataComponent(
                        "refinery_contents",
                        DataComponentType.<RefineryContents>builder()
                                .persistent(RefineryContents.CODEC)
                                .networkSynchronized(RefineryContents.STREAM_CODEC)
                                .build());
        FLUID_TANK_CONTENTS =
                r.registerDataComponent(
                        "fluid_tank_contents",
                        DataComponentType.<FluidTankContents>builder()
                                .persistent(FluidTankContents.CODEC)
                                .networkSynchronized(FluidTankContents.STREAM_CODEC)
                                .build());
        TANK_CONTENTS =
                r.registerDataComponent(
                        "tank_contents",
                        DataComponentType.<List<FluidStackNTM>>builder()
                                .persistent(FluidStackNTM.CODEC.listOf())
                                .networkSynchronized(
                                        FluidStackNTM.STREAM_CODEC.apply(ByteBufCodecs.list()))
                                .build());
        REDD_CHARGE =
                r.registerDataComponent(
                        "redd_charge",
                        DataComponentType.<BigInteger>builder()
                                .persistent(BIG_INTEGER)
                                .networkSynchronized(BIG_INTEGER_STREAM)
                                .build());
        MACHINE_BATTERY_STATE =
                r.registerDataComponent(
                        "machine_battery_state",
                        DataComponentType.<BatteryCharge>builder()
                                .persistent(BatteryCharge.CODEC)
                                .networkSynchronized(BatteryCharge.STREAM_CODEC)
                                .build());
        FLUID_CONTENT =
                r.registerDataComponent(
                        "fluid_content",
                        DataComponentType.<FluidStackNTM>builder()
                                .persistent(FluidStackNTM.CODEC)
                                .networkSynchronized(FluidStackNTM.STREAM_CODEC)
                                .build());
        TARGET_DESIGNATOR =
                r.registerDataComponent(
                        "target_designator",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        DETONATOR_POS =
                r.registerDataComponent(
                        "detonator_pos",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        DETONATOR_POS_LIST =
                r.registerDataComponent(
                        "detonator_pos_list",
                        DataComponentType.<List<Long>>builder()
                                .persistent(Codec.LONG.listOf())
                                .networkSynchronized(
                                        ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()))
                                .build());
        TOOL_PRESETS =
                r.registerDataComponent(
                        "tool_presets",
                        DataComponentType.<List<ToolPreset>>builder()
                                .persistent(ToolPreset.CODEC.listOf())
                                .networkSynchronized(
                                        ToolPreset.STREAM_CODEC.apply(
                                                ByteBufCodecs.list(MAX_TOOL_PRESETS)))
                                .build());
        TOOL_PRESET_INDEX =
                r.registerDataComponent(
                        "tool_preset_index",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        CONVEYOR_RUN =
                r.registerDataComponent(
                        "conveyor_run",
                        DataComponentType.<ConveyorRunData>builder()
                                .persistent(ConveyorRunData.CODEC)
                                .networkSynchronized(ConveyorRunData.STREAM_CODEC)
                                .build());
        RBMK_FUEL =
                r.registerDataComponent(
                        "rbmk_fuel",
                        DataComponentType.<RBMKFuelData>builder()
                                .persistent(RBMKFuelData.CODEC)
                                .networkSynchronized(RBMKFuelData.STREAM_CODEC)
                                .build());
        RBMK_TOOL_TARGET =
                r.registerDataComponent(
                        "rbmk_tool_target",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        MIRROR_TOOL_TARGET =
                r.registerDataComponent(
                        "mirror_tool_target",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        ARTY_DESIGNATOR_TARGET =
                r.registerDataComponent(
                        "arty_designator_target",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        RADAR_LINKER_TARGET =
                r.registerDataComponent(
                        "radar_linker_target",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        ARTY_CARGO =
                r.registerDataComponent(
                        "arty_cargo",
                        DataComponentType.<ItemStackTemplate>builder()
                                .persistent(ItemStackTemplate.CODEC)
                                .networkSynchronized(ItemStackTemplate.STREAM_CODEC)
                                .build());
        PELLET_DEPLETION =
                r.registerDataComponent(
                        "pellet_depletion",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        ICF_PELLET =
                r.registerDataComponent(
                        "icf_pellet",
                        DataComponentType.<ICFPelletData>builder()
                                .persistent(ICFPelletData.CODEC)
                                .networkSynchronized(ICFPelletData.STREAM_CODEC)
                                .build());
        ZIRNOX_LIFE =
                r.registerDataComponent(
                        "zirnox_life",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        PLATE_FUEL_LIFE =
                r.registerDataComponent(
                        "plate_fuel_life",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        WATZ_YIELD =
                r.registerDataComponent(
                        "watz_yield",
                        DataComponentType.<Double>builder()
                                .persistent(Codec.DOUBLE)
                                .networkSynchronized(ByteBufCodecs.DOUBLE)
                                .build());
        SCRAP =
                r.registerDataComponent(
                        "scrap",
                        DataComponentType.<ItemScraps.ScrapData>builder()
                                .persistent(ItemScraps.ScrapData.CODEC)
                                .networkSynchronized(ItemScraps.ScrapData.STREAM_CODEC)
                                .build());
        BROKEN_ITEM_SOURCE =
                r.registerDataComponent(
                        "broken_item_source",
                        DataComponentType.<Identifier>builder()
                                .persistent(Identifier.CODEC)
                                .networkSynchronized(Identifier.STREAM_CODEC)
                                .build());
        WRENCH_TARGET =
                r.registerDataComponent(
                        "wrench_target",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        REBAR_CORNER =
                r.registerDataComponent(
                        "rebar_corner",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        REBAR_CONCRETE =
                r.registerDataComponent(
                        "rebar_concrete",
                        DataComponentType.<ItemContainerContents>builder()
                                .persistent(ItemContainerContents.CODEC)
                                .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                                .build());
        LINKER_TARGET =
                r.registerDataComponent(
                        "linker_target",
                        DataComponentType.<GlobalPos>builder()
                                .persistent(GlobalPos.CODEC)
                                .networkSynchronized(GlobalPos.STREAM_CODEC)
                                .build());
        DRONE_LINK_TARGET =
                r.registerDataComponent(
                        "drone_link_target",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        WIRE_TARGET =
                r.registerDataComponent(
                        "wire_target",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        BLUEPRINT_POOL =
                r.registerDataComponent(
                        "blueprint_pool",
                        DataComponentType.<String>builder()
                                .persistent(Codec.STRING)
                                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                                .build());
        RTTY_PAGER_CHANNEL =
                r.registerDataComponent(
                        "rtty_pager_channel",
                        DataComponentType.<String>builder()
                                .persistent(Codec.STRING)
                                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                                .build());
        TABLET_SEED =
                r.registerDataComponent(
                        "tablet_seed",
                        DataComponentType.<Long>builder()
                                .persistent(Codec.LONG)
                                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                                .build());
        LORE_BOOK_KEY =
                r.registerDataComponent(
                        "lore_book_key",
                        DataComponentType.<String>builder()
                                .persistent(Codec.STRING)
                                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                                .build());
        LORE_BOOK_PAGES =
                r.registerDataComponent(
                        "lore_book_pages",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        LORE_BOOK_COVER =
                r.registerDataComponent(
                        "lore_book_cover",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        LORE_BOOK_TITLE =
                r.registerDataComponent(
                        "lore_book_title",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        Codec<Map<String, List<String>>> argCodec =
                Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf());
        LORE_BOOK_ARGS =
                r.registerDataComponent(
                        "lore_book_args",
                        DataComponentType.<Map<String, List<String>>>builder()
                                .persistent(argCodec)
                                .networkSynchronized(
                                        ByteBufCodecs.map(
                                                HashMap::new,
                                                ByteBufCodecs.STRING_UTF8,
                                                ByteBufCodecs.STRING_UTF8.apply(
                                                        ByteBufCodecs.list())))
                                .build());
        ARC_ELECTRODE_DURABILITY =
                r.registerDataComponent(
                        "arc_electrode_durability",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.VAR_INT)
                                .build());
        MISSILE_PARTS =
                r.registerDataComponent(
                        "missile_parts",
                        DataComponentType.<MissileStruct>builder()
                                .persistent(MissileStruct.CODEC)
                                .networkSynchronized(MissileStruct.STREAM_CODEC)
                                .build());
        MISSILE_CHIP =
                r.registerDataComponent(
                        "missile_chip",
                        DataComponentType.<Item>builder()
                                .persistent(BuiltInRegistries.ITEM.byNameCodec())
                                .networkSynchronized(ByteBufCodecs.registry(Registries.ITEM))
                                .build());
        KIT_CONTENTS =
                r.registerDataComponent(
                        "kit_contents",
                        DataComponentType.<List<ItemStackTemplate>>builder()
                                .persistent(ItemStackTemplate.CODEC.listOf())
                                .networkSynchronized(
                                        ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()))
                                .build());
        KIT_COLOR_1 =
                r.registerDataComponent(
                        "kit_color_1",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.INT)
                                .build());
        KIT_COLOR_2 =
                r.registerDataComponent(
                        "kit_color_2",
                        DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.INT)
                                .build());
    }
}
