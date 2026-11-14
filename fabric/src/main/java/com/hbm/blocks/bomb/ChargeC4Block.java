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

package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.SinglePointExplosionEntityProcessor;
import com.hbm.interfaces.TriggerResult;
import com.hbm.particle.helper.ExplosionCreator;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;

public class ChargeC4Block extends BaseChargeBlock {

    public ChargeC4Block(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return TriggerResult.NONE;
        if (!TntRule.explodes(level)) return TriggerResult.TNT_DISABLED;

        removeSafely(level, pos);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        ComposableExplosion explosion = new ComposableExplosion(level, x, y, z, 15F);
        explosion.blockSelector(new AttenuatedRayBlockSelector(32));
        explosion.blockProcessor(CellDestruction.noDrops());
        explosion.entityProcessor(new SinglePointExplosionEntityProcessor());
        explosion.explode();
        ExplosionCreator.composeEffectSmall(level, x, pos.getY() + 1.0D, z);
        return TriggerResult.DETONATED;
    }

    @Override
    public void addChargeTooltip(Consumer<Component> tooltip) {
        super.addChargeTooltip(tooltip);
        tooltip.accept(
                Component.translatable("desc.block.chargeC4.doesNotDropBlocks")
                        .withStyle(ChatFormatting.BLUE));
    }
}
