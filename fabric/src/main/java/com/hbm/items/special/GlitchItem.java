/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.items.special;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.VortexEntity;
import com.hbm.entity.projectile.BoxcarEntity;
import com.hbm.entity.projectile.MeteorEntity;
import com.hbm.explosion.BlastSmoke;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.lib.ModDamageTypes;
import com.hbm.main.Polaroid;
import com.hbm.util.TooltipStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class GlitchItem extends Item implements EnergyItem {
    private static final int USE_COST = 5;
    private static final int OUTCOMES = 31;
    private static final int BUFF_TICKS = 60 * 20;
    private static final int BUFF_AMPLIFIER = 9;
    private static final long CHARGE = 200;

    public GlitchItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int outcome = server.getRandom().nextInt(OUTCOMES);
        for (ItemStack gift : gifts(outcome))
            player.getInventory().placeItemBackInInventory(gift, Prediction.SERVER_ONLY);
        player.getItemInHand(hand).hurtAndBreak(USE_COST, player, hand);
        roll(server, player, outcome);
        return InteractionResult.SUCCESS;
    }

    private static List<ItemStack> gifts(int outcome) {
        return switch (outcome) {
            case 5 -> List.of(treasure(1));
            case 6 -> List.of(treasure(3));
            case 7 -> List.of(treasure(10));
            case 8 -> List.of(new ItemStack(ModItems.AMMO_CONTAINER.get(), 10));
            case 9 -> List.of(new ItemStack(ModItems.NUKE_ADVANCED_KIT.get()));
            case 10 -> List.of(new ItemStack(ModItems.NUKE_STARTER_KIT.get()));
            case 13 ->
                    List.of(
                            new ItemStack(ModItems.GUN_HEAVY_REVOLVER_LILMAC.get()),
                            new ItemStack(ModItems.BOTTLE_SPARKLE.get()),
                            new ItemStack(ModItems.GEIGER_COUNTER.get()));
            case 15 -> {
                List<ItemStack> dirt = new ArrayList<>();
                for (int i = 0; i < 36; i++) dirt.add(new ItemStack(Items.DIRT, 64));
                yield dirt;
            }
            case 18 ->
                    List.of(
                            new ItemStack(ModItems.GUN_MARESLEG.get()),
                            ModItems.AMMO_STANDARD.stack(EnumAmmo.G12, 12));
            case 21 -> List.of(new ItemStack(ModItems.MISSILE_NUCLEAR.get()));
            case 30 -> List.of(new ItemStack(ModItems.plate(Materials.SATURNITE)));
            default -> List.of();
        };
    }

    private void roll(ServerLevel level, Player player, int outcome) {
        switch (outcome) {
            case 2 ->
                    player.hurtServer(
                            level, level.damageSources().source(ModDamageTypes.RADIATION), 1000);
            case 3 ->
                    player.hurtServer(
                            level, level.damageSources().source(ModDamageTypes.BOXCAR), 1000);
            case 4 ->
                    player.hurtServer(
                            level, level.damageSources().source(ModDamageTypes.BLACKHOLE), 1000);
            case 11 -> boxcar(level, player, 0);
            case 12 -> {
                for (int i = 0; i < 10; i++) boxcar(level, player, 25);
            }
            case 14 -> {
                player.getInventory().dropAll();
                ExplosionChaos.igniteAllBlocks(
                        level, player.getBlockX(), player.getBlockY(), player.getBlockZ(), 5);
            }
            case 24 ->
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.RESISTANCE, BUFF_TICKS, BUFF_AMPLIFIER));
            case 25 ->
                    player.addEffect(
                            new MobEffectInstance(MobEffects.STRENGTH, BUFF_TICKS, BUFF_AMPLIFIER));
            case 26 ->
                    player.addEffect(
                            new MobEffectInstance(MobEffects.SLOWNESS, BUFF_TICKS, BUFF_AMPLIFIER));
            case 27 -> {
                VortexEntity vortex = new VortexEntity(level, 2.5F);
                vortex.setPos(player.getX(), player.getY() - 15, player.getZ());
                level.addFreshEntity(vortex);
            }
            case 28 -> {
                MeteorEntity meteor = new MeteorEntity(level);
                meteor.setPos(player.getX(), player.getY() + 100, player.getZ());
                level.addFreshEntity(meteor);
            }
            case 29 ->
                    BlastSmoke.flameRing(level, player.getX(), player.getY(), player.getZ(), 27, 3);
            default -> {}
        }
        say(player, outcome);
    }

    private static void say(Player player, int outcome) {
        String key = "chat.glitch." + outcome;
        Component line = Component.translatable(key);
        if (line.getString().equals(key)) return;
        player.sendSystemMessage(line);
    }

    private static ItemStack treasure(int count) {
        return new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get(), count);
    }

    private static void boxcar(ServerLevel level, Player player, double spread) {
        BoxcarEntity boxcar = new BoxcarEntity(ModEntities.BOXCAR.get(), level);
        RandomSource rand = level.getRandom();
        boxcar.setPos(
                player.getX() + (spread == 0 ? 0 : rand.nextGaussian() * spread),
                player.getY() + 50,
                player.getZ() + (spread == 0 ? 0 : rand.nextGaussian() * spread));
        level.addFreshEntity(boxcar);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.glitch"));
        adder.accept(Component.empty());
        for (Component line : TooltipStyle.lines(Polaroid.lore("desc.item.glitch.")))
            adder.accept(line);
    }

    @Override
    public void addCharge(ItemStack stack, long amount) {}

    @Override
    public void setCharge(ItemStack stack, long charge) {}

    @Override
    public void removeCharge(ItemStack stack, long amount) {}

    @Override
    public long getCharge(ItemStack stack) {
        return CHARGE;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return CHARGE;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return 0;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return CHARGE;
    }
}
