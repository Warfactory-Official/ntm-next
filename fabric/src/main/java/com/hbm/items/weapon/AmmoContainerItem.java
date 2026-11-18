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

package com.hbm.items.weapon;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.TooltipStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class AmmoContainerItem extends Item {

    public final boolean constrained;

    public AmmoContainerItem(Properties properties, boolean constrained) {
        super(properties.stacksTo(1));
        this.constrained = constrained;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack container = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        List<GunBaseNTItem> guns = new ArrayList<>();
        for (ItemStack carried : player.getInventory().getNonEquipmentItems()) {
            if (!(carried.getItem() instanceof GunBaseNTItem gun)
                    || gun.defaultAmmoType == null
                    || constrained && gun.isDefaultExpensive) continue;
            guns.add(gun);
        }
        if (guns.isEmpty()) return InteractionResult.SUCCESS;

        Collections.shuffle(guns);
        for (int index = 0; index < Math.min(3, guns.size()); index++) {
            GunBaseNTItem gun = guns.get(index);
            ItemStack ammo =
                    ModItems.AMMO_STANDARD.stack(
                            (GunFactory.EnumAmmo) gun.defaultAmmoType, gun.defaultAmmoAmount);
            if (constrained) ammo.setCount((int) Math.ceil(ammo.getCount() / 2.0D));
            player.getInventory().placeItemBackInInventory(ammo, Prediction.SERVER_ONLY);
        }

        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.ITEM_UNPACK.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        container.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (Component line :
                TooltipStyle.lines(
                        Component.translatableWithFallback(
                                "item.hbm.ammo_container" + (constrained ? ".1" : "") + ".desc",
                                ""))) {
            adder.accept(line.copy().withStyle(ChatFormatting.YELLOW));
        }
    }
}
