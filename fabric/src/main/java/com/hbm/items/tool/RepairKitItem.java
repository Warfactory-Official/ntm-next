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

package com.hbm.items.tool;

import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.sound.EntitySounds;
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

public final class RepairKitItem extends Item {

    private final Supplier<SoundEvent> sound;

    public RepairKitItem(Properties properties, Supplier<SoundEvent> sound) {
        super(properties);
        this.sound = sound;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Inventory inventory = player.getInventory();
        boolean mended = false;
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++)
            mended |= mend(inventory.getItem(slot));
        if (mended) {
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    sound.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            player.getItemInHand(hand).hurtAndBreak(1, player, hand.asEquipmentSlot());
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean mend(ItemStack stack) {
        if (!(stack.getItem() instanceof GunBaseNTItem gun)) return false;
        boolean mended = false;
        for (int config = 0; config < gun.getConfigCount(); config++) {
            float durability = gun.getConfig(stack, config).getDurability(stack);
            float wear = GunBaseNTItem.getWear(stack, config);
            if (!(wear > 0 && durability > 0)) continue;
            GunBaseNTItem.setWear(stack, config, Math.max(0F, wear - durability / 4));
            mended = true;
        }
        return mended;
    }
}
