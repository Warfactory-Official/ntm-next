// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ItemRepairKit extends Item {

    private final Supplier<SoundEvent> sound;

    public ItemRepairKit(Properties properties, Supplier<SoundEvent> sound) {
        super(properties);
        this.sound = sound;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean didSomething = false;
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            ItemStack gunStack = player.getInventory().getItem(slot);
            if (!(gunStack.getItem() instanceof ItemGunBaseNT gun)) continue;

            for (int index = 0; index < gun.getConfigCount(); index++) {
                float maxDurability = gun.getConfig(gunStack, index).getDurability(gunStack);

                if (Math.min(ItemGunBaseNT.getWear(gunStack, index), maxDurability) > 0) {
                    ItemGunBaseNT.setWear(
                            gunStack,
                            index,
                            Math.max(
                                    0F,
                                    ItemGunBaseNT.getWear(gunStack, index)
                                            - maxDurability * 0.25F));
                    didSomething = true;
                }
            }
        }

        ItemStack stack = player.getItemInHand(hand);
        if (didSomething) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    sound.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        }
        return InteractionResult.SUCCESS;
    }
}
