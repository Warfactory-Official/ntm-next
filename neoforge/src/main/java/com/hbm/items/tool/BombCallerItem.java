/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.items.tool;

import com.hbm.entity.logic.BomberEntity;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BombCallerItem extends Item {

    public final EnumCallerType type;

    public BombCallerItem(Properties properties, EnumCallerType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(500D, 1F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            double x = blockHit.getBlockPos().getX();
            double y = blockHit.getBlockPos().getY();
            double z = blockHit.getBlockPos().getZ();
            BomberEntity bomber =
                    switch (type) {
                        case CARPET_BOMBING -> BomberEntity.statFacCarpet(level, x, y, z);
                        case NAPALM -> BomberEntity.statFacNapalm(level, x, y, z);
                        case POISON_GAS -> BomberEntity.statFacChlorine(level, x, y, z);
                        case AGENT_ORANGE -> BomberEntity.statFacOrange(level, x, y, z);
                        case ATOMIC_BOMB -> BomberEntity.statFacABomb(level, x, y, z);
                        case VT_STINGER_ROCKETS -> BomberEntity.statFacStinger(level, x, y, z);
                        case PIP_OH_GOD -> BomberEntity.statFacBoxcar(level, x, y, z);
                        case CLOUD_THE_CLOUD -> BomberEntity.statFacPC(level, x, y, z);
                    };
            level.addFreshEntity(bomber);
            player.sendSystemMessage(
                    Component.translatable("desc.item.bombCaller.calledInAirstrike"));
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1F,
                    1F);
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return type.ordinal() >= EnumCallerType.ATOMIC_BOMB.ordinal();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.bombCaller.aimClickToCall"));
        adder.accept(
                Component.translatable("desc.shared.type", Component.translatable(type.key())));
    }

    public enum EnumCallerType {
        CARPET_BOMBING,
        NAPALM,
        POISON_GAS,
        AGENT_ORANGE,
        ATOMIC_BOMB,
        VT_STINGER_ROCKETS,
        PIP_OH_GOD,
        CLOUD_THE_CLOUD;

        public String key() {
            String[] words = name().toLowerCase(Locale.ROOT).split("_");
            StringBuilder key = new StringBuilder("desc.item.bombCaller.").append(words[0]);
            for (int i = 1; i < words.length; i++) {
                key.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
            }
            return key.toString();
        }
    }
}
