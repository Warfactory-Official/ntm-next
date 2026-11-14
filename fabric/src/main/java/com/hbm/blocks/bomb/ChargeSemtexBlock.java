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

package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.interfaces.TriggerResult;
import com.hbm.particle.helper.ExplosionCreator;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ChargeSemtexBlock extends BaseChargeBlock {

    public ChargeSemtexBlock(Properties properties) {
        super(properties);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity initiator) {
        if (level.isClientSide()) {
            return TriggerResult.NONE;
        }
        if (!TntRule.explodes(level)) {
            return TriggerResult.TNT_DISABLED;
        }

        removeSafely(level, pos);
        var center = Vec3.atCenterOf(pos);
        new ComposableExplosion(level, center.x, center.y, center.z, 10F)
                .blockSelector(new AttenuatedRayBlockSelector(32))
                .blockProcessor(CellDestruction.fortune(3))
                .explode();
        ExplosionCreator.composeEffectSmall(level, center.x, pos.getY() + 1D, center.z);
        return TriggerResult.DETONATED;
    }

    @Override
    public void addChargeTooltip(Consumer<Component> tooltip) {
        super.addChargeTooltip(tooltip);
        tooltip.accept(
                Component.translatable("desc.shared.willDropAllBlocks")
                        .withStyle(ChatFormatting.BLUE));
        tooltip.accept(
                Component.translatable("desc.shared.doesNotDoDamage")
                        .withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.literal("").withStyle(ChatFormatting.BLUE));
        tooltip.accept(
                Component.translatable("desc.block.chargeSemtex.fortuneIii")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
