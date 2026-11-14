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

import com.hbm.blockentity.bomb.CustomNukeBlockEntity;
import com.hbm.entity.projectile.FallingNukeEntity;
import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.explosion.assembly.ChargeYield;
import com.hbm.interfaces.TriggerResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class NukeCustomBlock extends BombRotatableBlock {

    public NukeCustomBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomNukeBlockEntity(pos, state);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!(level instanceof ServerLevel server)) return TriggerResult.NONE;
        if (!(server.getBlockEntity(pos) instanceof CustomNukeBlockEntity bomb)) {
            return TriggerResult.NONE;
        }

        if (!TntRule.explodes(server)) return TriggerResult.TNT_DISABLED;

        ChargeYield t = bomb.snapshot();
        if (bomb.hasFallingAccessory()) {
            FallingNukeEntity falling =
                    new FallingNukeEntity(
                            server,
                            t.conventional(),
                            t.nuclear(),
                            t.thermonuclear(),
                            t.antimatter(),
                            t.fallout(),
                            t.schrabidium(),
                            t.antiMass());
            falling.setNukeMeta(server.getBlockState(pos).getValue(FACING));
            falling.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0F, 0F);
            bomb.clearContent();
            server.removeBlock(pos, false);
            server.addFreshEntity(falling);
            return TriggerResult.ACTUATED;
        }

        bomb.clearContent();
        server.destroyBlock(pos, false);
        ExplosionNukeCustom.explodeCustom(
                server,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                t.conventional(),
                t.nuclear(),
                t.thermonuclear(),
                t.antimatter(),
                t.fallout(),
                t.schrabidium(),
                t.antiMass());
        return TriggerResult.DETONATED;
    }
}
