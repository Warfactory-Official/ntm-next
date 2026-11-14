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

package com.hbm.blockentity.bomb;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.bomb.CrashedBombBlock.EnumDudType;
import com.hbm.blocks.bomb.CrashedBombBlock;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CrashedBombBlockEntity extends BlockEntity implements BlockEntityTicks {

    public CrashedBombBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRASHED_BALEFIRE.get(), pos, state);
    }

    @Override
    public void tickServer() {
        var level = (ServerLevel) this.level;
        var pos = worldPosition;
        var state = getBlockState();
        if (!TickPhase.every(this, 2)) return;
        if (!(state.getBlock() instanceof CrashedBombBlock bomb)) return;
        EnumDudType type = bomb.type;

        if (type == EnumDudType.BALEFIRE) affectEntities(level, pos, 15D, 1F);
        if (type == EnumDudType.NUKE) affectEntities(level, pos, 10D, 0.25F);
        if (type == EnumDudType.SALTED) affectEntities(level, pos, 10D, 0.5F);
    }

    private void affectEntities(ServerLevel level, BlockPos pos, double range, float maxIntensity) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        AABB box = new AABB(cx, cy, cz, cx, cy, cz).inflate(range);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : entities) {
            double dx = cx - entity.getX();
            double dy = cy - (entity.getY() + entity.getBbHeight() / 2);
            double dz = cz - entity.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > range) continue;
            float intensity = (float) (maxIntensity * (1D - dist / range));
            ContaminationUtil.contaminate(
                    entity, HazardType.RADIATION, ContaminationType.CREATIVE, intensity);
        }
    }
}
