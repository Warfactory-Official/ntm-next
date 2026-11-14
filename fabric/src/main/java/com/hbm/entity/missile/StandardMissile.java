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

package com.hbm.entity.missile;

import com.hbm.api.entity.RadarTargetType;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class StandardMissile extends Missile {

    private final MissileVariant variant;
    private double @Nullable [] plumes;

    public StandardMissile(
            EntityType<? extends StandardMissile> type, Level level, MissileVariant variant) {
        super(type, level);
        this.variant = variant;
        burstsInAir = variant.burstsInAir();
    }

    public MissileVariant variant() {
        return variant;
    }

    @Override
    public String sourceName() {
        return variant.sourceName();
    }

    @Override
    public Item missileItem() {
        return variant.item().get();
    }

    @Override
    public void detonate(@Nullable BlockHitResult contact) {
        variant.payload().detonate(this);
    }

    @Override
    public List<ItemStack> debris() {
        return variant.frame().debris().get();
    }

    @Override
    public ItemStack rareDebris() {
        return variant.rareDebris().get();
    }

    @Override
    public String getRadarName() {
        var name = variant.getRadarName();
        return name != null ? name : super.getRadarName();
    }

    @Override
    public RadarTargetType getTargetType() {
        var blip = variant.blip();
        return blip != null ? blip : super.getTargetType();
    }

    @Override
    public boolean canBeSeenBy(@Nullable BlockEntity observer) {
        return !variant.stealth();
    }

    @Override
    protected float contrailScale() {
        return variant.frame().contrailScale();
    }

    @Override
    protected void exhaust(Vec3 step) {
        var offsets = plumes;
        if (offsets == null) plumes = offsets = new double[12];
        var nose = noseAxis();
        for (int i = 0,
                        n =
                                variant.frame()
                                        .plumes()
                                        .offsets(viewYaw, viewPitch, padFacing(), offsets);
                i < n;
                i++) {
            plume(step, nose, offsets[3 * i], offsets[3 * i + 1], offsets[3 * i + 2]);
        }
    }
}
