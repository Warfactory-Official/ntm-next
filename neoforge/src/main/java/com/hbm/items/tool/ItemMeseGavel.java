// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.WeaponAbility;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemMeseGavel extends ItemSwordAbility {

    public ItemMeseGavel(Properties properties) {
        super(
                properties,
                AvailableAbilities.builder()
                        .add(WeaponAbility.PHOSPHORUS, 0)
                        .add(WeaponAbility.RADIATION, 2)
                        .add(WeaponAbility.STUN, 3)
                        .add(WeaponAbility.VAMPIRE, 4)
                        .add(WeaponAbility.BEHEADER, 0)
                        .build());
    }

    public static Item.Properties properties() {
        return swordProperties(
                        ToolTier.MESE_GAVEL, 250.0F, 1.5D, ItemToolAbility.repairTag("plate_paa"))
                .stacksTo(1);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (victim.level() instanceof ServerLevel level) {

            level.playSound(
                    null,
                    victim.blockPosition(),
                    ModSounds.GAVEL.get(),
                    SoundSource.HOSTILE,
                    3.0F,
                    1.0F);
        }
        super.hurtEnemy(stack, victim, attacker);
    }
}
