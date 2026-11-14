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

package com.hbm.blockentity.machine.pile;

import com.hbm.blocks.machine.pile.DrilledGraphiteBlock;
import com.hbm.blocks.machine.pile.GraphiteNeutronDetectorBlock;
import com.hbm.blocks.machine.pile.GraphiteRodBlock;
import com.hbm.handler.neutron.NeutronRay;
import com.hbm.handler.neutron.PileNeutronReceiver;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.RegistryUtil;
import com.hbm.world.gen.LegacyWorldgenRotation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public record PileRay(PileBaseBlockEntity source, double dx, double dy, double dz, int flux)
        implements NeutronRay {

    private static final TagKey<Block> FLUX_ABSORBER =
            TagKey.create(Registries.BLOCK, RegistryUtil.id("pile/flux_absorber"));
    private static final TagKey<Block> FLUX_ATTENUATOR =
            TagKey.create(Registries.BLOCK, RegistryUtil.id("pile/flux_attenuator"));
    private static final float FIRST_SAMPLE = 1F;
    private static final float LAST_SAMPLE = 5F;
    private static final float SAMPLE_STEP = 0.5F;
    private static final EntityTypeTest<Entity, LivingEntity> LIVING =
            EntityTypeTest.forClass(LivingEntity.class);

    private static final BlockPos.MutableBlockPos CELL = new BlockPos.MutableBlockPos();
    private static final List<LivingEntity> NEARBY = new ArrayList<>();

    public static PileRay turned(
            PileBaseBlockEntity source, float roll, float yaw, float pitch, int flux) {
        double x = 1;
        double y = 0;
        double z = 0;
        double cos = LegacyWorldgenRotation.cosine(roll);
        double sin = LegacyWorldgenRotation.sine(roll);
        double turned = x * cos + y * sin;
        y = y * cos - x * sin;
        x = turned;

        cos = LegacyWorldgenRotation.cosine(yaw);
        sin = LegacyWorldgenRotation.sine(yaw);
        turned = x * cos + z * sin;
        z = z * cos - x * sin;
        x = turned;

        cos = LegacyWorldgenRotation.cosine(pitch);
        sin = LegacyWorldgenRotation.sine(pitch);
        turned = y * cos + z * sin;
        z = z * cos - y * sin;
        return new PileRay(source, x, turned, z, flux);
    }

    @Override
    public void travel(ServerLevel level) {
        BlockPos origin = source.getBlockPos();
        double startX = origin.getX() + 0.5D;
        double startY = origin.getY() + 0.5D;
        double startZ = origin.getZ() + 0.5D;
        int lastX = origin.getX();
        int lastY = origin.getY();
        int lastZ = origin.getZ();
        double neutrons = flux;
        boolean queried = false;
        try {
            for (float t = FIRST_SAMPLE; t <= LAST_SAMPLE; t += SAMPLE_STEP) {
                int x = Mth.floor(startX + dx * t);
                int y = Mth.floor(startY + dy * t);
                int z = Mth.floor(startZ + dz * t);
                if (x == lastX && y == lastY && z == lastZ) continue;
                lastX = x;
                lastY = y;
                lastZ = z;
                var state = level.getBlockState(CELL.set(x, y, z));
                if (state.is(FLUX_ABSORBER)) return;
                if (state.is(FLUX_ATTENUATOR)) neutrons *= 0.25D;
                var block = state.getBlock();
                if (block instanceof GraphiteRodBlock
                        && !state.getValue(DrilledGraphiteBlock.WITHDRAWN)) return;
                if (level.getBlockEntity(CELL) instanceof PileNeutronReceiver target) {
                    target.absorbNeutrons((int) neutrons);
                    if (!(block instanceof GraphiteNeutronDetectorBlock
                            && state.getValue(DrilledGraphiteBlock.WITHDRAWN))) return;
                }
                if (!queried) {
                    queried = true;
                    level.getEntities(
                            LIVING,
                            reach(startX, startY, startZ),
                            EntitySelector.NO_SPECTATORS,
                            NEARBY);
                }
                double centreX = x + 0.5D;
                double centreY = y + 0.5D;
                double centreZ = z + 0.5D;
                for (int i = 0; i < NEARBY.size(); i++) {
                    var entity = NEARBY.get(i);
                    if (entity.getBoundingBox()
                            .intersects(centreX, centreY, centreZ, centreX, centreY, centreZ)) {
                        ContaminationUtil.contaminate(
                                entity,
                                HazardType.RADIATION,
                                ContaminationType.CREATIVE,
                                neutrons / 4D);
                    }
                }
            }
        } finally {
            NEARBY.clear();
        }
    }

    private AABB reach(double startX, double startY, double startZ) {
        int nearX = Mth.floor(startX + dx * FIRST_SAMPLE);
        int nearY = Mth.floor(startY + dy * FIRST_SAMPLE);
        int nearZ = Mth.floor(startZ + dz * FIRST_SAMPLE);
        int farX = Mth.floor(startX + dx * LAST_SAMPLE);
        int farY = Mth.floor(startY + dy * LAST_SAMPLE);
        int farZ = Mth.floor(startZ + dz * LAST_SAMPLE);
        return new AABB(
                Math.min(nearX, farX),
                Math.min(nearY, farY),
                Math.min(nearZ, farZ),
                Math.max(nearX, farX) + 1,
                Math.max(nearY, farY) + 1,
                Math.max(nearZ, farZ) + 1);
    }
}
