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

package com.hbm.blockentity.prop;

import com.google.common.base.Suppliers;
import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.LanternBehemothBlock;
import com.hbm.entity.missile.BobmazonEntity;
import com.hbm.extprop.PlayerState;
import com.hbm.interfaces.Extinguishable;
import com.hbm.interfaces.RepairHandler;
import com.hbm.inventory.material.MaterialTagRegistry;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModItems;
import com.hbm.items.special.KitCustomItem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class BehemothLanternBlockEntity extends BlockEntity
        implements BlockEntityTicks, RepairHandler, Extinguishable {
    private static final Supplier<List<CountIngredient>> MATERIALS =
            Suppliers.memoize(
                    () ->
                            List.of(
                                    CountIngredient.of(MaterialTagRegistry.STEEL.plate(), 2),
                                    CountIngredient.of(ModItems.CIRCUIT_BASIC.get(), 1)));

    public int countdown = -1;
    public int rewardReputation;

    public BehemothLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANTERN_BEHEMOTH.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (countdown < 0) return;
        switch (countdown) {
            case 360 ->
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition,
                            ModSounds.HORN_NEAR_SINGLE.get(),
                            SoundSource.BLOCKS,
                            10F,
                            1F);
            case 280 ->
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition,
                            ModSounds.HORN_FAR_SINGLE.get(),
                            SoundSource.BLOCKS,
                            10000F,
                            1F);
            case 220 ->
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition,
                            ModSounds.HORN_NEAR_DUAL.get(),
                            SoundSource.BLOCKS,
                            10F,
                            1F);
            case 100 ->
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition,
                            ModSounds.HORN_FAR_DUAL.get(),
                            SoundSource.BLOCKS,
                            10000F,
                            1F);
            case 0 -> sendSupplies();
        }
        countdown--;
        setChanged();
    }

    private void sendSupplies() {
        var random = level.getRandom();
        double x = worldPosition.getX() + 0.5 + random.nextGaussian() * 10;
        double z = worldPosition.getZ() + 0.5 + random.nextGaussian() * 10;
        var basic = new ItemStack(ModItems.CIRCUIT_BASIC.get(), 4 + random.nextInt(4));
        var advanced = new ItemStack(ModItems.CIRCUIT_ADVANCED.get(), 4 + random.nextInt(2));
        var coins = new ItemStack(ModItems.COIN_TOKEN.get(), Math.max(1, rewardReputation));
        var gem =
                rewardReputation >= 10
                        ? new ItemStack(ModItems.GEM_ALEXANDRITE.get())
                        : new ItemStack(Items.DIAMOND, 6 + random.nextInt(6));
        var delivery = new BobmazonEntity(level);
        delivery.setPos(x, 300, z);
        delivery.payload =
                KitCustomItem.create(
                        "Supplies",
                        null,
                        0xffffff,
                        0x008000,
                        basic,
                        advanced,
                        coins,
                        gem,
                        new ItemStack(Items.POPPY));
        level.addFreshEntity(delivery);
    }

    @Override
    public boolean isDamaged() {
        return getBlockState().getValue(LanternBehemothBlock.BROKEN);
    }

    @Override
    public List<CountIngredient> getRepairMaterials() {
        return MATERIALS.get();
    }

    @Override
    public void repair(Player player) {
        level.setBlock(
                worldPosition,
                getBlockState().setValue(LanternBehemothBlock.BROKEN, false),
                Block.UPDATE_ALL);
        countdown = 400;
        rewardReputation = PlayerState.getOrCreate(player).reputation;
        setChanged();
    }

    @Override
    public void tryExtinguish(Level level, BlockPos pos, Agent agent) {}

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        for (var player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(50))) {
            var reputation = PlayerState.getOrCreate(player);
            if (reputation.reputation > -25) reputation.reputation--;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("comTimer", countdown);
        output.putInt("reputation", rewardReputation);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        countdown = input.getIntOr("comTimer", -1);
        rewardReputation = input.getIntOr("reputation", 0);
    }
}
