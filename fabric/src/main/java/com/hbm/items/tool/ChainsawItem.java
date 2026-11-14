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

import com.hbm.client.HeldItemAnimations;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.items.AnimatedItem;
import com.hbm.items.ItemSwingListener;
import com.hbm.items.tool.ToolAbilityFueledItem;
import com.hbm.render.anim.AnimationEnums.ToolAnimation;
import com.hbm.render.anim.AnimationPlayback;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.util.GameTime;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ChainsawItem extends ToolAbilityFueledItem implements ItemSwingListener, AnimatedItem {

    public ChainsawItem(
            Properties properties,
            AvailableAbilities abilities,
            Supplier<Fluid[]> acceptedFuels,
            int maxFuel,
            int consumption,
            int fillRate) {
        super(properties, abilities, true, acceptedFuels, maxFuel, consumption, fillRate);
    }

    @Override
    public void onEntitySwing(ServerPlayer player, ItemStack stack) {
        AnimatedItem.cue(player, ToolAnimation.SWING);
    }

    @Override
    public @Nullable BusAnimation animation(ToolAnimation type, ItemStack stack) {
        return getAnimation();
    }

    public static @Nullable BusAnimation getAnimation() {
        int forward = 150;
        int sideways = 100;
        int retire = 200;

        AnimationPlayback current = HeldItemAnimations.getActiveAnimation();
        if (current == null) {
            return new BusAnimation()
                    .addBus(
                            "SWING_ROT",
                            new AnimationSequence()
                                    .to(0, 0, 90, forward)
                                    .to(45, 0, 90, sideways)
                                    .to(0, 0, 0, retire))
                    .addBus(
                            "SWING_TRANS",
                            new AnimationSequence()
                                    .to(0, 0, 3, forward)
                                    .to(2, 0, 2, sideways)
                                    .to(0, 0, 0, retire));
        }

        double[] rot = HeldItemAnimations.sampleChannels("SWING_ROT");
        double[] trans = HeldItemAnimations.sampleChannels("SWING_TRANS");

        if (GameTime.millis() - current.start() < 50) return null;

        return new BusAnimation()
                .addBus(
                        "SWING_ROT",
                        new AnimationSequence()
                                .to(rot[0], rot[1], rot[2], 0)
                                .to(0, 0, 90, forward)
                                .to(45, 0, 90, sideways)
                                .to(0, 0, 0, retire))
                .addBus(
                        "SWING_TRANS",
                        new AnimationSequence()
                                .to(trans[0], trans[1], trans[2], 0)
                                .to(0, 0, 3, forward)
                                .to(2, 0, 2, sideways)
                                .to(0, 0, 0, retire));
    }
}
