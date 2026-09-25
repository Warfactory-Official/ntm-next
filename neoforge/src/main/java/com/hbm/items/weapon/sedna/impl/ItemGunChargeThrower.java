// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.impl;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.config.GunVisualConfig;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ItemGunChargeThrower extends ItemGunBaseNT {

    public static final String KEY_LASTHOOK = "lasthook";

    public ItemGunChargeThrower(WeaponQuality quality, Properties properties, GunConfig... cfg) {
        super(quality, properties, cfg);
    }

    public static void clientGrappleTick(ItemStack stack) {
        Player player = ClientPlayerAccess.player();
        if (player == null) return;

        Entity e = player.level().getEntity(getLastHook(stack));
        if (e == null
                || !e.isAlive()
                || !(e instanceof EntityBulletBaseMK4 hook)
                || hook.config != XFactoryTool.ct_hook
                || hook.velocity >= 0.01) return;

        Vec3 eye = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vec3 vec = e.position().subtract(eye);
        double line = vec.length();
        HbmPlayerProps props = HbmPlayerProps.getData(player);

        if (props.getKeyPressed(EnumKeybind.GUN_PRIMARY)) {

            Vec3 pull = vec.normalize().scale(0.1);
            player.setDeltaMovement(player.getDeltaMovement().add(pull.x, pull.y + 0.04, pull.z));
        } else if (!props.getKeyPressed(EnumKeybind.GUN_SECONDARY)) {

            Vec3 motion = player.getDeltaMovement();
            Vec3 nextPos =
                    new Vec3(
                            player.getX() + motion.x,
                            player.getY() + player.getEyeHeight() + motion.y,
                            player.getZ() + motion.z);
            Vec3 delta = e.position().subtract(nextPos);
            if (delta.length() > line) {
                delta = delta.normalize().scale(line);
                Vec3 newNext = e.position().subtract(delta);
                Vec3 vel = newNext.subtract(eye);
                if (vel.length() < 3) {
                    player.setDeltaMovement(vel);
                }
            }
        } else {

            player.setDeltaMovement(player.getDeltaMovement().scale(0.5));
        }

        if (player.getDeltaMovement().y > -0.1) player.fallDistance = 0;
    }

    public static boolean anchored(Player player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof ItemGunChargeThrower
                && anchoredHook(player.level(), stack) != null;
    }

    private static @Nullable Entity anchoredHook(Level level, ItemStack stack) {
        Entity e = level.getEntity(getLastHook(stack));
        return e != null
                        && e.isAlive()
                        && e instanceof EntityBulletBaseMK4 hook
                        && hook.config == XFactoryTool.ct_hook
                        && hook.velocity < 0.01
                ? e
                : null;
    }

    public static int getLastHook(ItemStack stack) {
        return getValueInt(stack, KEY_LASTHOOK);
    }

    public static void setLastHook(ItemStack stack, int value) {
        setValueInt(stack, KEY_LASTHOOK, value);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (getState(stack, 0) == GunState.RELOADING) {
            if (getLastHook(stack) != -1) setLastHook(stack, -1);
        }

        if (slot == EquipmentSlot.MAINHAND && entity instanceof Player player) {
            Entity e = anchoredHook(level, stack);
            if (e != null) {

                double line =
                        e.position()
                                .subtract(
                                        player.getX(),
                                        player.getY() + player.getEyeHeight(),
                                        player.getZ())
                                .length();
                if (HbmPlayerProps.getData(player).getKeyPressed(EnumKeybind.GUN_PRIMARY)
                        && line < 2) e.discard();
            }
        } else {
            if (getLastHook(stack) != -1) setLastHook(stack, -1);
        }
    }
}
