// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumBatteryPack;
import com.hbm.items.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockLootCrate extends FallingBlock {

    private static final long CONTENTS = WorldgenHash.identifier(Library.id("crate"));

    public static final MapCodec<BlockLootCrate> CODEC = simpleCodec(BlockLootCrate::new);

    private static List<ItemStack> crateListCache;
    private static List<ItemStack> redListCache;
    private static List<ItemStack> weaponListCache;
    private static List<ItemStack> leadListCache;
    private static List<ItemStack> metalListCache;

    public BlockLootCrate(Properties props) {
        super(props);
    }

    private static List<ItemStack> crateList() {
        if (crateListCache == null) {
            List<ItemStack> list = new ArrayList<>();
            addWeighted(list, ModItems.SYRINGE_METAL_STIMPAK.get(), 10);
            addWeighted(list, ModItems.SYRINGE_ANTIDOTE.get(), 5);
            addWeighted(
                    list,
                    ItemGrenadeUniversal.make(
                            EnumGrenadeShell.FRAG,
                            EnumGrenadeFilling.HE,
                            EnumGrenadeFuze.S3,
                            EnumGrenadeExtra.FRAG_SLEEVE),
                    8);
            addWeighted(
                    list,
                    ItemGrenadeUniversal.make(
                            EnumGrenadeShell.STICK, EnumGrenadeFilling.HE, EnumGrenadeFuze.IMPACT),
                    6);
            addWeighted(
                    list,
                    ItemGrenadeUniversal.make(
                            EnumGrenadeShell.FRAG, EnumGrenadeFilling.INC, EnumGrenadeFuze.S7),
                    4);
            addWeighted(list, ModItems.AMMO_CONTAINER.get(), 2);
            crateListCache = list;
        }
        return crateListCache;
    }

    private static List<ItemStack> redList() {
        if (redListCache == null) {
            List<ItemStack> list = new ArrayList<>();
            list.add(new ItemStack(ModItems.MYSTERY_SHOVEL));
            list.add(new ItemStack(ModItems.GUN_HEAVY_REVOLVER_LILMAC));
            list.add(new ItemStack(ModItems.GUN_AUTOSHOTGUN_SEXY));
            list.add(new ItemStack(ModItems.GUN_MARESLEG_BROKEN));
            list.add(ModItems.AMMO_SECRET.stack(EnumAmmoSecret.M44_EQUESTRIAN));
            list.add(ModItems.AMMO_SECRET.stack(EnumAmmoSecret.G12_EQUESTRIAN));
            list.add(ModItems.AMMO_SECRET.stack(EnumAmmoSecret.BMG50_EQUESTRIAN));
            list.add(new ItemStack(ModItems.BATTERY_SPARK));
            list.add(new ItemStack(ModItems.BOTTLE_SPARKLE));
            list.add(new ItemStack(ModItems.BOTTLE_RAD));
            list.add(new ItemStack(ModItems.RING_STARMETAL));
            list.add(new ItemStack(ModItems.FLAME_PONY));
            list.add(new ItemStack(Item.byBlock(ModBlocks.NTM_DIRT.get())));
            list.add(new ItemStack(ModBlocks.BROADCASTER_PC));
            redListCache = list;
        }
        return redListCache;
    }

    private static List<ItemStack> weaponList() {
        if (weaponListCache == null) {
            List<ItemStack> list = new ArrayList<>();
            addWeighted(list, ModItems.GUN_LIGHT_REVOLVER.get(), 10);
            addWeighted(list, ModItems.GUN_MARESLEG.get(), 7);
            addWeighted(list, ModItems.GUN_HEAVY_REVOLVER.get(), 5);
            addWeighted(list, ModItems.GUN_GREASEGUN.get(), 5);
            addWeighted(list, ModItems.GUN_LIBERATOR.get(), 2);
            addWeighted(list, ModItems.GUN_FLAREGUN.get(), 8);
            addWeighted(list, ModItems.GUN_PANZERSCHRECK.get(), 1);
            weaponListCache = list;
        }
        return weaponListCache;
    }

    private static List<ItemStack> leadList() {
        if (leadListCache == null) {
            List<ItemStack> list = new ArrayList<>();
            addWeighted(list, ModItems.ingot(Mats.MAT_URANIUM), 10);
            addWeighted(list, ModItems.ingot(Mats.MAT_U238), 8);
            addWeighted(list, ModItems.ingot(Mats.MAT_PLUTONIUM), 7);
            addWeighted(list, ModItems.ingot(Mats.MAT_PU240), 6);
            addWeighted(list, ModItems.ingot(Mats.MAT_NEPTUNIUM), 7);
            addWeighted(list, ModItems.INGOT_URANIUM_FUEL.get(), 8);
            addWeighted(list, ModItems.INGOT_PLUTONIUM_FUEL.get(), 7);
            addWeighted(list, ModItems.INGOT_MOX_FUEL.get(), 6);
            addWeighted(list, ModItems.nugget(Mats.MAT_URANIUM), 10);
            addWeighted(list, ModItems.nugget(Mats.MAT_U238), 8);
            addWeighted(list, ModItems.nugget(Mats.MAT_PLUTONIUM), 7);
            addWeighted(list, ModItems.nugget(Mats.MAT_PU240), 6);
            addWeighted(list, ModItems.nugget(Mats.MAT_NEPTUNIUM), 7);
            addWeighted(list, ModItems.NUGGET_URANIUM_FUEL.get(), 8);
            addWeighted(list, ModItems.NUGGET_PLUTONIUM_FUEL.get(), 7);
            addWeighted(list, ModItems.NUGGET_MOX_FUEL.get(), 6);
            addWeighted(list, ModItems.CELL_DEUTERIUM.get(), 8);
            addWeighted(list, ModItems.CELL_TRITIUM.get(), 8);
            addWeighted(list, ModItems.CELL_UF6.get(), 8);
            addWeighted(list, ModItems.CELL_PUF6.get(), 8);
            addWeighted(list, ModItems.PELLET_RTG.get(), 6);
            addWeighted(list, ModItems.PELLET_RTG_WEAK.get(), 7);
            addWeighted(list, ModItems.POWDER_YELLOWCAKE.get(), 10);
            leadListCache = list;
        }
        return leadListCache;
    }

    private static List<ItemStack> metalList() {
        if (metalListCache == null) {
            List<ItemStack> list = new ArrayList<>();
            addWeighted(list, Item.byBlock(ModBlocks.MACHINE_PRESS.get()), 10);
            addWeighted(list, Item.byBlock(ModBlocks.MACHINE_REACTOR_BREEDING.get()), 6);
            addWeighted(list, Item.byBlock(ModBlocks.MACHINE_WOOD_BURNER.get()), 10);
            addWeighted(list, Item.byBlock(ModBlocks.MACHINE_DIESEL.get()), 8);
            addWeighted(list, Item.byBlock(ModBlocks.MACHINE_RTG.get()), 4);
            addWeighted(list, Item.byBlock(ModBlocks.RED_PYLON.get()), 9);
            addWeighted(list, ModItems.batteryPack(EnumBatteryPack.BATTERY_LEAD), 10);
            addWeighted(list, Item.byBlock(ModBlocks.MACHINE_ELECTRIC_FURNACE.get()), 8);
            addWeighted(list, Item.byBlock(ModBlocks.MACHINE_ASSEMBLY_MACHINE.get()), 10);
            addWeighted(list, Item.byBlock(ModBlocks.MACHINE_FLUID_TANK.get()), 7);
            addWeighted(list, ModItems.CENTRIFUGE_ELEMENT.get(), 6);
            addWeighted(list, ModItems.MOTOR.get(), 8);
            addWeighted(list, ModItems.COIL_TUNGSTEN.get(), 7);
            addWeighted(list, ModItems.PHOTO_PANEL.get(), 3);
            addWeighted(list, ModItems.COIL_COPPER.get(), 10);
            addWeighted(list, ModItems.BLADE_TITANIUM.get(), 3);
            addWeighted(list, ModItems.PISTON_SELENIUM.get(), 6);
            metalListCache = list;
        }
        return metalListCache;
    }

    private static void addWeighted(List<ItemStack> list, Item item, int weight) {
        for (int i = 0; i < weight; i++) list.add(new ItemStack(item));
    }

    private static void addWeighted(List<ItemStack> list, ItemStack stack, int weight) {
        for (int i = 0; i < weight; i++) list.add(stack);
    }

    @Override
    protected MapCodec<BlockLootCrate> codec() {
        return CODEC;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getMapColor(level, pos).col;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!held.is(ModItems.CROWBAR.get())) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            dropItems(server, pos);

            level.removeBlock(pos, false);
            level.playSound(null, pos, ModSounds.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    public static List<ItemStack> poolOf(Block crate) {
        if (crate == ModBlocks.CRATE_RED.get()) return redList();
        if (crate == ModBlocks.CRATE_WEAPON.get()) return weaponList();
        if (crate == ModBlocks.CRATE_LEAD.get()) return leadList();
        if (crate == ModBlocks.CRATE_METAL.get()) return metalList();
        return crateList();
    }

    private void dropItems(ServerLevel level, BlockPos pos) {
        RandomSource rand = NtmWorldgenFields.get(level).random(CONTENTS, pos);
        List<ItemStack> chosen = new ArrayList<>();

        if (this == ModBlocks.CRATE_RED.get()) {
            chosen.addAll(redList());
        } else {
            List<ItemStack> pool = poolOf(this);
            int count;
            if (this == ModBlocks.CRATE_WEAPON.get()) {

                count = rand.nextInt(2) + 1;
                if (rand.nextInt(100) == 34) count = 25;
            } else {
                count = rand.nextInt(3) + 3;
            }
            if (pool.isEmpty()) return;
            for (int i = 0; i < count; i++) chosen.add(pool.get(rand.nextInt(pool.size())));
        }

        for (ItemStack proto : chosen) {
            float fx = rand.nextFloat() * 0.8F + 0.1F;
            float fy = rand.nextFloat() * 0.8F + 0.1F;
            float fz = rand.nextFloat() * 0.8F + 0.1F;
            ItemEntity entity =
                    new ItemEntity(
                            level, pos.getX() + fx, pos.getY() + fy, pos.getZ() + fz, proto.copy());
            entity.setDeltaMovement(
                    rand.nextGaussian() * 0.05,
                    rand.nextGaussian() * 0.05 + 0.2,
                    rand.nextGaussian() * 0.05);
            level.addFreshEntity(entity);
        }
    }
}
