// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.ai;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

public final class EntityAIFireGun extends Goal {

    private final Mob host;

    public double attackMoveSpeed = 1.0D;
    public double maxRange = 20D;
    public int burstTime = 10;
    public int minWait = 10;
    public int maxWait = 40;
    public float inaccuracy = 30F;
    public boolean randomBurst = true;

    private int attackTimer;
    private FireState state = FireState.IDLE;
    private int stateTimer;

    public EntityAIFireGun(Mob host) {
        this.host = host;
    }

    @Override
    public boolean canUse() {
        return host.getTarget() != null && getYerGun() != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = host.getTarget();
        ItemStack stack = host.getMainHandItem();
        ItemGunBaseNT gun = getYerGun();
        if (target == null || gun == null || !(host.level() instanceof ServerLevel level)) return;

        gun.inventoryTick(stack, level, host, EquipmentSlot.MAINHAND);

        double distanceToTargetSquared =
                host.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSeeTarget = host.getSensing().hasLineOfSight(target);
        if (canSeeTarget) attackTimer++;
        else attackTimer = 0;

        if (distanceToTargetSquared < maxRange * maxRange && attackTimer > 20) {
            host.getNavigation().stop();
        } else {
            host.getNavigation().moveTo(target, attackMoveSpeed);
        }
        host.getLookControl().setLookAt(target, 30F, 30F);

        stateTimer--;
        if (stateTimer < 0) {
            stateTimer = 0;
            if (state == FireState.WAIT) {
                updateState(FireState.IDLE, 0, gun, stack);
            } else if (state != FireState.IDLE) {
                updateState(
                        FireState.WAIT,
                        host.getRandom().nextInt(maxWait - minWait) + minWait,
                        gun,
                        stack);
            }
        } else if (state == FireState.FIRING) {
            updateKeybind(gun, stack, EnumKeybind.GUN_PRIMARY);
        }

        if (canSeeTarget
                && distanceToTargetSquared < maxRange * maxRange
                && state == FireState.IDLE) {
            GunConfig config = gun.getConfig(stack, 0);
            Receiver receiver = config.getReceivers(stack)[0];
            if (receiver.getMagazine(stack).getAmount(stack, null) <= 0) {
                updateState(FireState.RELOADING, 20, gun, stack);
            } else if (ItemGunBaseNT.getState(stack, 0) == GunState.IDLE) {
                int time = randomBurst ? host.getRandom().nextInt(burstTime) : burstTime;
                updateState(FireState.FIRING, time, gun, stack);
            }
        }
    }

    private void updateState(FireState toState, int time, ItemGunBaseNT gun, ItemStack stack) {
        state = toState;
        stateTimer = time;

        switch (state) {
            case FIRING:
                updateKeybind(gun, stack, EnumKeybind.GUN_PRIMARY);
            case RELOADING:
                updateKeybind(gun, stack, EnumKeybind.RELOAD);
            default:
                clearKeybinds(gun, stack);
        }
    }

    private void clearKeybinds(ItemGunBaseNT gun, ItemStack stack) {
        updateKeybind(gun, stack, null);
    }

    private void updateKeybind(ItemGunBaseNT gun, ItemStack stack, EnumKeybind bind) {
        if (bind != null && bind != EnumKeybind.RELOAD) {
            float spread =
                    inaccuracy
                            * getYerGun()
                                    .getConfig(stack, 0)
                                    .getReceivers(stack)[0]
                                    .getHipfireSpread(stack)
                            * 20F;
            host.yHeadRot += (host.getRandom().nextFloat() - 0.5F) * spread;

            host.xRot += (host.getRandom().nextFloat() - 0.5F) * spread;

            host.yRot = host.yHeadRot;
            host.yBodyRot = host.yRot;
        }

        gun.handleKeybind(
                host, null, stack, EnumKeybind.GUN_PRIMARY, bind == EnumKeybind.GUN_PRIMARY);
        gun.handleKeybind(
                host, null, stack, EnumKeybind.GUN_SECONDARY, bind == EnumKeybind.GUN_SECONDARY);
        gun.handleKeybind(
                host, null, stack, EnumKeybind.GUN_TERTIARY, bind == EnumKeybind.GUN_TERTIARY);
        gun.handleKeybind(host, null, stack, EnumKeybind.RELOAD, bind == EnumKeybind.RELOAD);
    }

    public ItemGunBaseNT getYerGun() {
        ItemStack stack = host.getMainHandItem();
        return stack.getItem() instanceof ItemGunBaseNT gun ? gun : null;
    }

    private enum FireState {
        IDLE,
        WAIT,
        FIRING,
        RELOADING
    }
}
