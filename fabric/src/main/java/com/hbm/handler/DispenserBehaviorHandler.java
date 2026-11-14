/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.handler;

import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.items.ModItems;
import com.hbm.items.tool.FertilizerItem;
import com.hbm.items.weapon.grenade.GrenadeShellItem.EnumGrenadeShell;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LevelEvent;

public final class DispenserBehaviorHandler {

    private DispenserBehaviorHandler() {}

    public static void init() {

        DispenserBlock.registerBehavior(
                ModItems.GRENADE_UNIVERSAL.get(),
                new DefaultDispenseItemBehavior() {
                    @Override
                    protected ItemStack execute(BlockSource source, ItemStack stack) {

                        Direction facing = source.state().getValue(DispenserBlock.FACING);

                        GrenadeUniversalEntity grenade =
                                new GrenadeUniversalEntity(source.level(), stack);
                        EnumGrenadeShell shell = grenade.getShell();

                        grenade.setPos(
                                source.center().x + facing.getStepX() * 0.75,
                                source.center().y + facing.getStepY() * 0.75,
                                source.center().z + facing.getStepZ() * 0.75);
                        grenade.setDeltaMovement(
                                facing.getStepX() * shell.getYeetForce(),
                                facing.getStepY() * shell.getYeetForce(),
                                facing.getStepZ() * shell.getYeetForce());
                        source.level().addFreshEntity(grenade);

                        stack.shrink(1);
                        return stack;
                    }
                });

        DispenserBlock.registerBehavior(
                ModItems.POWDER_FERTILIZER.get(),
                new DefaultDispenseItemBehavior() {
                    private boolean grew = true;

                    @Override
                    protected ItemStack execute(BlockSource source, ItemStack stack) {
                        Direction facing = source.state().getValue(DispenserBlock.FACING);
                        grew =
                                FertilizerItem.spread(
                                        stack, source.level(), source.pos().relative(facing));
                        return stack;
                    }

                    @Override
                    protected void playSound(BlockSource source) {
                        source.level()
                                .levelEvent(
                                        grew
                                                ? LevelEvent.SOUND_DISPENSER_DISPENSE
                                                : LevelEvent.SOUND_DISPENSER_FAIL,
                                        source.pos(),
                                        0);
                    }
                });

        DispenserBlock.registerProjectileBehavior(ModItems.STICK_DYNAMITE.get());
        DispenserBlock.registerProjectileBehavior(ModItems.STICK_DYNAMITE_FISHING.get());
    }
}
