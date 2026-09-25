// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemFertilizer;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
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

                        EntityGrenadeUniversal grenade =
                                new EntityGrenadeUniversal(source.level(), stack);
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
                                ItemFertilizer.spread(
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
