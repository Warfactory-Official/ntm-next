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

package com.hbm.blockentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class LanternBlockEntity extends BlockEntity implements BlockEntityTicks {

    public LanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANTERN.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getGameTime() % 20 != 0) return;

        BlockPos pos = getBlockPos();
        AABB aabb =
                new AABB(
                                pos.getX() + 0.5,
                                pos.getY() + 5.5,
                                pos.getZ() + 0.5,
                                pos.getX() + 0.5,
                                pos.getY() + 5.5,
                                pos.getZ() + 0.5)
                        .inflate(7.5);

        List<GlyphidEntity> glyphids = serverLevel.getEntitiesOfClass(GlyphidEntity.class, aabb);
        for (GlyphidEntity glyphid : glyphids) {
            glyphid.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }
    }
}
