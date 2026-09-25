// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ArmorDash {

    public static final int PER_DASH = 30;

    private ArmorDash() {}

    public static void tick(Player player, float forward, float strafe) {
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        int dashes = dashCount(player);
        props.dashCount = dashes;

        if (dashes * PER_DASH < props.stamina) props.stamina = dashes * PER_DASH;
        if (dashes <= 0) return;

        int stamina = props.stamina;

        if (props.dashCooldown > 0) {
            props.dashCooldown--;

            props.setKeyPressed(EnumKeybind.DASH, false);
        } else if (props.getKeyPressed(EnumKeybind.DASH) && stamina >= PER_DASH) {
            dash(player, forward, strafe);
            props.dashCooldown = HbmPlayerProps.dashCooldownLength;
            stamina -= PER_DASH;
        }

        if (stamina < dashes * PER_DASH) {
            stamina++;
            if (stamina % PER_DASH == PER_DASH - 1) {

                if (!player.level().isClientSide()) {
                    player.level()
                            .playSound(
                                    null,
                                    player.getX(),
                                    player.getY(),
                                    player.getZ(),
                                    ModSounds.TECH_BOOP.get(),
                                    SoundSource.PLAYERS,
                                    1.0F,
                                    1.0F + (1F / 12F) * (stamina / PER_DASH));
                }
                stamina++;
            }
        }

        props.stamina = stamina;
    }

    private static void dash(Player player, float forward, float strafe) {

        if (player.level().isClientSide()) {
            int f = (int) Math.signum(forward);
            int s = (int) Math.signum(strafe);

            if (f == 0 && s == 0) f = 1;

            Vec3 look = player.getLookAngle();

            Vec3 right = new Vec3(-look.z, 0D, look.x);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(
                    motion.x + look.x * f + right.x * s, 0D, motion.z + look.z * f + right.z * s);
            player.fallDistance = 0F;
            return;
        }
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.WEAPON_ROCKET_FLAME.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
    }

    private static int dashCount(Player player) {
        int count = 0;
        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate) {
            ArmorFullSetBonus bonus = ArmorFullSetBonus.get(plate.suit());
            if (bonus != null
                    && bonus.dashCount() > 0
                    && ArmorSuitEffects.hasFullSet(player, plate.suit()))
                count += bonus.dashCount();
        }
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!ArmorModHandler.isArmor(armor)) continue;
            for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                if (mod.getItem() instanceof ItemModCloud cloud) count += cloud.dashes();
            }
        }
        return count;
    }
}
