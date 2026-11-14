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
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record MissileVariant(
        Supplier<? extends EntityType<? extends StandardMissile>> type,
        String sourceName,
        Supplier<? extends Item> item,
        Airframe frame,
        Supplier<ItemStack> rareDebris,
        MissilePayload payload,
        @Nullable String getRadarName,
        @Nullable RadarTargetType blip,
        boolean stealth,
        boolean burstsInAir) {

    public MissileVariant(
            Supplier<? extends EntityType<? extends StandardMissile>> type,
            String sourceName,
            Supplier<? extends Item> item,
            Airframe frame,
            Supplier<ItemStack> rareDebris,
            MissilePayload payload) {
        this(type, sourceName, item, frame, rareDebris, payload, null, null, false, false);
    }

    public StandardMissile create(Level level) {
        return new StandardMissile(type.get(), level, this);
    }

    public MissileVariant radar(String name, @Nullable RadarTargetType blip) {
        return new MissileVariant(
                type,
                sourceName,
                item,
                frame,
                rareDebris,
                payload,
                name,
                blip,
                stealth,
                burstsInAir);
    }

    public MissileVariant stealthy() {
        return new MissileVariant(
                type,
                sourceName,
                item,
                frame,
                rareDebris,
                payload,
                getRadarName,
                blip,
                true,
                burstsInAir);
    }

    public MissileVariant airburst() {
        return new MissileVariant(
                type,
                sourceName,
                item,
                frame,
                rareDebris,
                payload,
                getRadarName,
                blip,
                stealth,
                true);
    }

    public record Airframe(
            float contrailScale, PlumeLayout plumes, Supplier<List<ItemStack>> debris) {}
}
