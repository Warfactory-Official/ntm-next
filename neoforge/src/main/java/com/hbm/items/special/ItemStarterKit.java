// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumBatteryPack;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ItemStarterKit extends Item {

    private enum Haz {
        PLAIN,
        RED,
        GREY
    }

    public enum Kind {
        NUKE_STARTER(ItemStarterKit::nukeStarter, Haz.RED, true, true),
        NUKE_ADVANCED(ItemStarterKit::nukeAdvanced, Haz.GREY, true, true),
        NUKE_COMMERCIALLY(ItemStarterKit::nukeCommercially, Haz.GREY, true, true),
        NUKE_ELECTRIC(ItemStarterKit::nukeElectric, null, true, false),
        GADGET(ItemStarterKit::gadget, Haz.PLAIN, true, true),
        BOY(ItemStarterKit::boy, Haz.PLAIN, true, true),
        MAN(ItemStarterKit::man, Haz.PLAIN, true, true),
        MIKE(ItemStarterKit::mike, Haz.PLAIN, true, true),
        TSAR(ItemStarterKit::tsar, Haz.PLAIN, true, true),
        MULTI(ItemStarterKit::multi, null, true, false),
        CUSTOM(ItemStarterKit::custom, null, false, false),
        FLEIJA(ItemStarterKit::fleija, Haz.GREY, true, true),
        SOLINIUM(ItemStarterKit::solinium, Haz.RED, true, true),
        PROTOTYPE(ItemStarterKit::prototype, Haz.GREY, true, true),
        MISSILE(ItemStarterKit::missile, null, true, false),
        EUPHEMIUM(ItemStarterKit::euphemium, null, false, false),
        HAZMAT(List::of, Haz.PLAIN, false, true),
        HAZMAT_RED(List::of, Haz.RED, false, false),
        HAZMAT_GREY(List::of, Haz.GREY, false, false);

        private final Supplier<List<ItemStack>> contents;
        private final @Nullable Haz haz;
        private final boolean emptyWarning;
        private final boolean armorWarning;

        Kind(
                Supplier<List<ItemStack>> contents,
                @Nullable Haz haz,
                boolean emptyWarning,
                boolean armorWarning) {
            this.contents = contents;
            this.haz = haz;
            this.emptyWarning = emptyWarning;
            this.armorWarning = armorWarning;
        }
    }

    private final Kind kind;

    public ItemStarterKit(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel server) {
            ItemStack stack = player.getItemInHand(hand);

            for (ItemStack content : kind.contents.get()) {
                player.getInventory().placeItemBackInInventory(content);
            }
            if (kind.haz != null) giveHaz(server, player, kind.haz);

            server.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.ITEM_UNPACK.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static void giveHaz(ServerLevel level, Player player, Haz tier) {
        for (EquipmentSlot slot :
                new EquipmentSlot[] {
                    EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
                }) {
            ItemStack worn = player.getItemBySlot(slot);
            if (!worn.isEmpty()) {
                level.addFreshEntity(
                        new ItemEntity(
                                level,
                                player.getX(),
                                player.getEyeY(),
                                player.getZ(),
                                worn.copy()));
            }
        }
        player.setItemSlot(
                EquipmentSlot.HEAD,
                new ItemStack(
                        switch (tier) {
                            case PLAIN -> ModItems.HAZMAT_HELMET;
                            case RED -> ModItems.HAZMAT_HELMET_RED;
                            case GREY -> ModItems.HAZMAT_HELMET_GREY;
                        }));
        player.setItemSlot(
                EquipmentSlot.CHEST,
                new ItemStack(
                        switch (tier) {
                            case PLAIN -> ModItems.HAZMAT_PLATE;
                            case RED -> ModItems.HAZMAT_PLATE_RED;
                            case GREY -> ModItems.HAZMAT_PLATE_GREY;
                        }));
        player.setItemSlot(
                EquipmentSlot.LEGS,
                new ItemStack(
                        switch (tier) {
                            case PLAIN -> ModItems.HAZMAT_LEGS;
                            case RED -> ModItems.HAZMAT_LEGS_RED;
                            case GREY -> ModItems.HAZMAT_LEGS_GREY;
                        }));
        player.setItemSlot(
                EquipmentSlot.FEET,
                new ItemStack(
                        switch (tier) {
                            case PLAIN -> ModItems.HAZMAT_BOOTS;
                            case RED -> ModItems.HAZMAT_BOOTS_RED;
                            case GREY -> ModItems.HAZMAT_BOOTS_GREY;
                        }));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> lines,
            TooltipFlag flag) {
        if (kind.emptyWarning) lines.accept(Component.translatable("desc.item.kitPool"));
        if (kind.armorWarning) lines.accept(Component.translatable("desc.item.kitHaz"));
    }

    private static ItemStack filled(
            RegistryHandle<ItemFluidTank> handle, Fluid fluid, int amount, int count) {
        ItemStack stack = handle.get().make(fluid, amount);
        stack.setCount(count);
        return stack;
    }

    private static List<ItemStack> nukeStarter() {
        return List.of(
                new ItemStack(ModItems.ingot(Mats.MAT_URANIUM), 32),
                new ItemStack(ModItems.POWDER_YELLOWCAKE, 32),
                new ItemStack(ModBlocks.MACHINE_PRESS),
                new ItemStack(ModBlocks.MACHINE_BLAST_FURNACE),
                new ItemStack(ModBlocks.MACHINE_GASCENT),
                new ItemStack(ModBlocks.MACHINE_REACTOR_BREEDING),
                new ItemStack(ModBlocks.MACHINE_ASSEMBLY_MACHINE),
                new ItemStack(ModBlocks.MACHINE_CHEMICAL_PLANT),
                new ItemStack(ModBlocks.REACTOR_RESEARCH),
                new ItemStack(ModBlocks.MACHINE_TURBINE, 2),
                new ItemStack(ModItems.RADAWAY, 8),
                new ItemStack(ModItems.RADX, 2),
                new ItemStack(ModItems.STAMP_TITANIUM_FLAT),
                new ItemStack(ModItems.STAMP_TITANIUM_FLAT),
                new ItemStack(ModItems.STAMP_TITANIUM_FLAT),
                new ItemStack(ModItems.ingot(Mats.MAT_STEEL), 64),
                new ItemStack(ModItems.ingot(Mats.MAT_LEAD), 64),
                new ItemStack(Items.COPPER_INGOT, 64),
                new ItemStack(ModItems.GAS_MASK_M65),
                new ItemStack(ModItems.GEIGER_COUNTER));
    }

    private static List<ItemStack> nukeAdvanced() {
        return List.of(
                new ItemStack(ModItems.POWDER_YELLOWCAKE, 64),
                new ItemStack(ModItems.powder(Mats.MAT_PLUTONIUM), 64),
                new ItemStack(ModItems.ingot(Mats.MAT_STEEL), 64),
                new ItemStack(Items.COPPER_INGOT, 64),
                new ItemStack(ModItems.ingot(Mats.MAT_TUNGSTEN), 64),
                new ItemStack(ModItems.ingot(Mats.MAT_LEAD), 64),
                new ItemStack(ModItems.ingot(Mats.MAT_POLYMER), 64),
                new ItemStack(ModBlocks.MACHINE_BLAST_FURNACE, 3),
                new ItemStack(ModBlocks.MACHINE_GASCENT, 3),
                new ItemStack(ModBlocks.MACHINE_CENTRIFUGE, 2),
                new ItemStack(ModBlocks.MACHINE_UF6_TANK, 2),
                new ItemStack(ModBlocks.MACHINE_PUF6_TANK, 2),
                new ItemStack(ModBlocks.MACHINE_REACTOR_BREEDING, 2),
                new ItemStack(ModBlocks.REACTOR_RESEARCH, 4),
                new ItemStack(ModBlocks.MACHINE_TURBINE, 4),
                new ItemStack(ModBlocks.MACHINE_RADGEN),
                new ItemStack(ModBlocks.MACHINE_RTG),
                new ItemStack(ModBlocks.MACHINE_ASSEMBLY_MACHINE, 3),
                new ItemStack(ModBlocks.MACHINE_CHEMICAL_PLANT, 2),
                new ItemStack(ModBlocks.MACHINE_FLUID_TANK),
                new ItemStack(ModItems.PELLET_RTG),
                new ItemStack(ModItems.PELLET_RTG),
                new ItemStack(ModItems.PELLET_RTG),
                new ItemStack(ModItems.PELLET_RTG_WEAK),
                new ItemStack(ModItems.PELLET_RTG_WEAK),
                new ItemStack(ModItems.PELLET_RTG_WEAK),
                new ItemStack(ModItems.CELL_EMPTY, 32),
                new ItemStack(ModItems.ROD_EMPTY, 32),
                filled(ModItems.FLUID_BARREL, NTMFluids.COOLANT, 16_000, 4),
                new ItemStack(ModItems.RADAWAY_STRONG, 4),
                new ItemStack(ModItems.RADX, 4),
                new ItemStack(ModItems.PILL_IODINE),
                new ItemStack(ModItems.GEIGER_COUNTER),
                new ItemStack(ModItems.SURVEY_SCANNER),
                new ItemStack(ModItems.GAS_MASK_M65));
    }

    private static List<ItemStack> nukeCommercially() {
        return List.of(
                new ItemStack(ModBlocks.REACTOR_RESEARCH, 8),
                new ItemStack(ModBlocks.MACHINE_REACTOR_BREEDING, 8),
                new ItemStack(ModBlocks.MACHINE_FLUID_TANK, 8),
                new ItemStack(ModItems.BILLET_PU238BE, 40),
                new ItemStack(ModItems.ingot(Mats.MAT_U233), 40),
                new ItemStack(ModItems.INGOT_URANIUM_FUEL, 32),
                new ItemStack(ModItems.INGOT_PLUTONIUM_FUEL, 16),
                new ItemStack(ModItems.INGOT_MOX_FUEL, 8),
                new ItemStack(ModItems.INF_WATER_MK2),
                new ItemStack(ModItems.INF_WATER_MK2),
                new ItemStack(ModItems.INF_WATER_MK2),
                new ItemStack(ModItems.ROD_EMPTY, 64),
                new ItemStack(ModItems.ROD_DUAL_EMPTY, 64),
                new ItemStack(ModItems.ROD_QUAD_EMPTY, 64),
                new ItemStack(ModItems.FLUID_TANK_LEAD, 64),
                new ItemStack(ModItems.FLUID_BARREL, 64),
                new ItemStack(ModBlocks.BARREL_STEEL, 16),
                new ItemStack(ModItems.plate(Mats.MAT_IRON), 64),
                new ItemStack(Items.DYE.black(), 64),
                new ItemStack(ModItems.RADAWAY_FLUSH, 8),
                new ItemStack(ModItems.IV_BLOOD, 8),
                new ItemStack(ModItems.PILL_IODINE, 8),
                new ItemStack(ModItems.GAS_MASK_FILTER_COMBO),
                new ItemStack(ModItems.GAS_MASK_FILTER_COMBO),
                new ItemStack(ModItems.GAS_MASK_FILTER_COMBO));
    }

    private static List<ItemStack> nukeElectric() {

        return List.of(
                new ItemStack(ModItems.COIL_COPPER, 16),
                new ItemStack(ModItems.COIL_GOLD, 8),
                new ItemStack(ModItems.COIL_TUNGSTEN, 8),
                new ItemStack(ModItems.MOTOR, 4),
                new ItemStack(ModItems.CIRCUIT_VACUUM_TUBE, 16),
                new ItemStack(ModItems.CIRCUIT_CAPACITOR, 16),
                new ItemStack(ModItems.CIRCUIT_BASIC, 16),
                new ItemStack(ModItems.WIRING_RED_COPPER),
                new ItemStack(ModItems.MAGNETRON, 5),
                new ItemStack(ModItems.PISTON_SELENIUM),
                new ItemStack(ModItems.PISTON_SELENIUM),
                new ItemStack(ModItems.PISTON_SELENIUM),
                filled(ModItems.CANISTER, NTMFluids.DIESEL, 1_000, 16),
                filled(ModItems.CANISTER, NTMFluids.BIOFUEL, 1_000, 16),
                new ItemStack(ModItems.BATTERY_POTATO),
                new ItemStack(ModItems.SCREWDRIVER),
                new ItemStack(ModBlocks.MACHINE_EXCAVATOR),
                new ItemStack(ModBlocks.MACHINE_DIESEL, 2),
                new ItemStack(ModBlocks.CABLE, 64),
                new ItemStack(ModBlocks.RED_WIRE_COATED, 16),
                new ItemStack(ModBlocks.RED_PYLON, 8),
                new ItemStack(ModBlocks.MACHINE_BATTERY_SOCKET, 4),
                new ItemStack(ModItems.batteryPack(EnumBatteryPack.BATTERY_LEAD), 4));
    }

    private static List<ItemStack> gadget() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_GADGET),
                new ItemStack(ModItems.EARLY_EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EARLY_EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EARLY_EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EARLY_EXPLOSIVE_LENSES),
                new ItemStack(ModItems.GADGET_WIREING),
                new ItemStack(ModItems.GADGET_CORE));
    }

    private static List<ItemStack> boy() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_BOY),
                new ItemStack(ModItems.BOY_SHIELDING),
                new ItemStack(ModItems.BOY_TARGET),
                new ItemStack(ModItems.BOY_BULLET),
                new ItemStack(ModItems.BOY_PROPELLANT),
                new ItemStack(ModItems.BOY_IGNITER));
    }

    private static List<ItemStack> man() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_MAN),
                new ItemStack(ModItems.EARLY_EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EARLY_EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EARLY_EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EARLY_EXPLOSIVE_LENSES),
                new ItemStack(ModItems.MAN_IGNITER),
                new ItemStack(ModItems.MAN_CORE));
    }

    private static List<ItemStack> mike() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_MIKE),
                new ItemStack(ModItems.EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EXPLOSIVE_LENSES),
                new ItemStack(ModItems.MAN_CORE),
                new ItemStack(ModItems.MIKE_CORE),
                new ItemStack(ModItems.MIKE_DEUT),
                new ItemStack(ModItems.MIKE_COOLING_UNIT));
    }

    private static List<ItemStack> tsar() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_TSAR),
                new ItemStack(ModItems.EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EXPLOSIVE_LENSES),
                new ItemStack(ModItems.EXPLOSIVE_LENSES),
                new ItemStack(ModItems.MAN_CORE),
                new ItemStack(ModItems.TSAR_CORE));
    }

    private static List<ItemStack> multi() {
        return List.of(
                new ItemStack(ModBlocks.BOMB_MULTI, 6),
                new ItemStack(Items.TNT, 26),
                new ItemStack(Items.GUNPOWDER, 2),
                new ItemStack(ModItems.PELLET_CLUSTER, 2),
                new ItemStack(ModItems.POWDER_FIRE, 2),
                new ItemStack(ModItems.POWDER_POISON, 2),
                new ItemStack(ModItems.PELLET_GAS, 2));
    }

    private static List<ItemStack> custom() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_CUSTOM),
                new ItemStack(ModItems.CUSTOM_TNT),
                new ItemStack(ModItems.CUSTOM_TNT),
                new ItemStack(ModItems.CUSTOM_TNT),
                new ItemStack(ModItems.CUSTOM_TNT),
                new ItemStack(ModItems.CUSTOM_TNT),
                new ItemStack(ModItems.CUSTOM_TNT),
                new ItemStack(ModItems.CUSTOM_NUKE),
                new ItemStack(ModItems.CUSTOM_NUKE),
                new ItemStack(ModItems.CUSTOM_NUKE),
                new ItemStack(ModItems.CUSTOM_NUKE),
                new ItemStack(ModItems.CUSTOM_HYDRO),
                new ItemStack(ModItems.CUSTOM_HYDRO),
                new ItemStack(ModItems.CUSTOM_AMAT),
                new ItemStack(ModItems.CUSTOM_AMAT),
                new ItemStack(ModItems.CUSTOM_DIRTY),
                new ItemStack(ModItems.CUSTOM_DIRTY),
                new ItemStack(ModItems.CUSTOM_DIRTY),
                new ItemStack(ModItems.CUSTOM_SCHRAB),
                new ItemStack(ModItems.CUSTOM_FALL));
    }

    private static List<ItemStack> fleija() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_FLEIJA),
                new ItemStack(ModItems.FLEIJA_IGNITER),
                new ItemStack(ModItems.FLEIJA_IGNITER),
                new ItemStack(ModItems.FLEIJA_PROPELLANT),
                new ItemStack(ModItems.FLEIJA_PROPELLANT),
                new ItemStack(ModItems.FLEIJA_PROPELLANT),
                new ItemStack(ModItems.FLEIJA_CORE),
                new ItemStack(ModItems.FLEIJA_CORE),
                new ItemStack(ModItems.FLEIJA_CORE),
                new ItemStack(ModItems.FLEIJA_CORE),
                new ItemStack(ModItems.FLEIJA_CORE),
                new ItemStack(ModItems.FLEIJA_CORE));
    }

    private static List<ItemStack> solinium() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_SOLINIUM),
                new ItemStack(ModItems.SOLINIUM_IGNITER),
                new ItemStack(ModItems.SOLINIUM_IGNITER),
                new ItemStack(ModItems.SOLINIUM_IGNITER),
                new ItemStack(ModItems.SOLINIUM_IGNITER),
                new ItemStack(ModItems.SOLINIUM_PROPELLANT),
                new ItemStack(ModItems.SOLINIUM_PROPELLANT),
                new ItemStack(ModItems.SOLINIUM_PROPELLANT),
                new ItemStack(ModItems.SOLINIUM_PROPELLANT),
                new ItemStack(ModItems.SOLINIUM_CORE));
    }

    private static List<ItemStack> prototype() {
        return List.of(
                new ItemStack(ModBlocks.NUKE_PROTOTYPE),
                new ItemStack(ModItems.IGNITER),
                new ItemStack(ModItems.CELL_SAS3, 4),
                ModItems.ROD_QUAD.stack(BreedingRodType.URANIUM, 4),
                ModItems.ROD_QUAD.stack(BreedingRodType.LEAD, 4),
                ModItems.ROD_QUAD.stack(BreedingRodType.NP237, 2));
    }

    private static List<ItemStack> missile() {
        return List.of(
                new ItemStack(ModBlocks.LAUNCH_PAD),
                new ItemStack(ModItems.DESIGNATOR),
                new ItemStack(ModItems.DESIGNATOR_RANGE),
                new ItemStack(ModItems.DESIGNATOR_MANUAL),
                new ItemStack(ModItems.MISSILE_GENERIC),
                new ItemStack(ModItems.MISSILE_STRONG),
                new ItemStack(ModItems.MISSILE_BURST),
                new ItemStack(ModItems.MISSILE_INCENDIARY),
                new ItemStack(ModItems.MISSILE_INCENDIARY_STRONG),
                new ItemStack(ModItems.MISSILE_INFERNO),
                new ItemStack(ModItems.MISSILE_CLUSTER),
                new ItemStack(ModItems.MISSILE_CLUSTER_STRONG),
                new ItemStack(ModItems.MISSILE_RAIN),
                new ItemStack(ModItems.MISSILE_BUSTER),
                new ItemStack(ModItems.MISSILE_BUSTER_STRONG),
                new ItemStack(ModItems.MISSILE_DRILL),
                new ItemStack(ModItems.MISSILE_NUCLEAR),
                new ItemStack(ModItems.MISSILE_NUCLEAR_CLUSTER),
                new ItemStack(ModItems.MISSILE_VOLCANO),
                new ItemStack(ModItems.MISSILE_DOOMSDAY),
                new ItemStack(ModItems.MISSILE_TAINT),
                new ItemStack(ModItems.MISSILE_MICRO),
                new ItemStack(ModItems.MISSILE_BHOLE),
                new ItemStack(ModItems.MISSILE_SCHRABIDIUM),
                new ItemStack(ModItems.MISSILE_EMP));
    }

    private static List<ItemStack> euphemium() {
        return List.of(
                new ItemStack(ModItems.EUPHEMIUM_HELMET),
                new ItemStack(ModItems.EUPHEMIUM_PLATE),
                new ItemStack(ModItems.EUPHEMIUM_LEGS),
                new ItemStack(ModItems.EUPHEMIUM_BOOTS),
                new ItemStack(ModBlocks.STATUE_ELB_F));
    }
}
