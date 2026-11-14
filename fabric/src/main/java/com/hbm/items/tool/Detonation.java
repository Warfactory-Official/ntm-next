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

package com.hbm.items.tool;

import com.hbm.NuclearTech;
import com.hbm.capability.NtmContracts;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

final class Detonation {

    private Detonation() {}

    static NtmContracts.@Nullable Target<TriggerHandler> find(Level level, BlockPos pos) {
        return NtmContracts.BOMB.target(level, pos);
    }

    static TriggerResult fire(
            Level level,
            NtmContracts.Target<TriggerHandler> target,
            @Nullable Entity cause,
            String by) {
        TriggerResult code = target.handler().trigger(level, target.pos(), cause);
        if (((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging()) {
            BlockPos pos = target.pos();
            NuclearTech.LOGGER.info(
                    "[DET] Tried to detonate block at {} / {} / {} by {}!",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    by);
        }
        return code;
    }

    static Component report(TriggerResult code) {
        return code.message()
                .withStyle(code.success() ? ChatFormatting.YELLOW : ChatFormatting.RED);
    }
}
