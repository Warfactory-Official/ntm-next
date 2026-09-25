// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.impl;

import com.hbm.config.GunVisualConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ItemGunStinger extends ItemGunBaseNT {

    public static final String KEY_LOCKINGON = "lockingon";
    public static final String KEY_LOCKONPROGRESS = "lockonprogress";

    public static float prevLockon;
    public static float lockon;

    public ItemGunStinger(WeaponQuality quality, Item.Properties properties, GunConfig... cfg) {
        super(quality, properties, cfg);
    }

    public static void clientLockonTick(ItemStack stack) {
        prevLockon = lockon;
        if (getLockonProgress(stack) > 1) {
            lockon += (1F / 60F);
        } else {
            lockon = 0;
        }
    }

    public static int getLockonTarget(Player player, double distance, double angleThreshold) {

        if (player == null) return -1;

        double x = player.getX();
        double y = player.getY() + player.getEyeHeight();
        double z = player.getZ();

        Vec3 delta = player.getViewVector(1F).scale(distance);
        Vec3 look = delta.add(x, y, z);

        Vec3 left = delta.yRot((float) Math.toRadians(-angleThreshold)).add(x, y, z).add(0, 10, 0);
        Vec3 right = delta.yRot((float) Math.toRadians(angleThreshold)).add(x, y, z).add(0, -10, 0);
        Vec3 pos = new Vec3(x, y, z);

        AABB aabb =
                new AABB(
                        Math.min(Math.min(look.x, left.x), Math.min(right.x, pos.x)),
                        Math.min(Math.min(look.y, left.y), Math.min(right.y, pos.y)),
                        Math.min(Math.min(look.z, left.z), Math.min(right.z, pos.z)),
                        Math.max(Math.max(look.x, left.x), Math.max(right.x, pos.x)),
                        Math.max(Math.max(look.y, left.y), Math.max(right.y, pos.y)),
                        Math.max(Math.max(look.z, left.z), Math.max(right.z, pos.z)));
        List<Entity> entities = player.level().getEntities(player, aabb);
        Entity closestEntity = null;
        double closestAngle = 360D;

        for (Entity entity : entities) {
            if (entity.getBbHeight() < 0.5F || !entity.isPickable()) continue;
            Vec3 toEntity =
                    new Vec3(
                            entity.getX() - x,
                            entity.getY() + entity.getBbHeight() / 2D - y,
                            entity.getZ() - z);

            double vecProd = toEntity.x * delta.x + toEntity.y * delta.y + toEntity.z * delta.z;
            double bot = toEntity.length() * delta.length();
            double angle = Math.abs(Math.acos(vecProd / bot) * 180 / Math.PI);

            if (angle < closestAngle && angle < angleThreshold) {
                closestAngle = angle;
                closestEntity = entity;
            }
        }

        return closestEntity == null ? -1 : closestEntity.getId();
    }

    public static boolean getIsLockingOn(ItemStack stack) {
        return getValueBool(stack, KEY_LOCKINGON);
    }

    public static void setIsLockingOn(ItemStack stack, boolean value) {
        setValueBool(stack, KEY_LOCKINGON, value);
    }

    public static int getLockonProgress(ItemStack stack) {
        return getValueInt(stack, KEY_LOCKONPROGRESS);
    }

    public static void setLockonProgress(ItemStack stack, int value) {
        setValueInt(stack, KEY_LOCKONPROGRESS, value);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (entity instanceof Player player) {
            boolean isHeld = slot == EquipmentSlot.MAINHAND;
            if (!isHeld && getIsLockingOn(stack)) {
                setIsLockingOn(stack, false);
            }

            int prevTarget = getLockonTarget(stack);
            if (isHeld
                    && getIsLockingOn(stack)
                    && getIsAiming(stack)
                    && this.getConfig(stack, 0)
                                    .getReceivers(stack)[0]
                                    .getMagazine(stack)
                                    .getAmount(stack, player.getInventory())
                            > 0) {
                int newLockonTarget = getLockonTarget(player, 150D, 10D);

                if (newLockonTarget == -1) {
                    if (!getIsLockedOn(stack)) resetLockon(stack);
                } else {
                    if (!getIsLockedOn(stack) && newLockonTarget != prevTarget) {
                        resetLockon(stack);
                        setLockonTarget(stack, newLockonTarget);
                    }
                    setLockonProgress(stack, getLockonProgress(stack) + 1);

                    if (getLockonProgress(stack) >= 60 && !getIsLockedOn(stack)) {
                        level.playSound(
                                null,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                ModSounds.TECH_BLEEP.get(),
                                SoundSource.PLAYERS,
                                1F,
                                1F);
                        setIsLockedOn(stack, true);
                    }
                }
            } else {
                resetLockon(stack);
            }
        }
    }

    public void resetLockon(ItemStack stack) {
        setLockonProgress(stack, 0);
        setIsLockedOn(stack, false);
    }
}
