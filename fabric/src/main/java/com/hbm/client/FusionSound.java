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

package com.hbm.client;

import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.machine.fusion.CreativeFusionKlystronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionKlystronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionMHDTBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FusionSound {

    private FusionSound() {}

    public static void tick(FusionTorusBlockEntity be) {
        loop(be, be.magnetSpeed / 30F, 50F, 2.5D, false);
    }

    public static void tick(FusionKlystronBlockEntity be) {
        loop(be, be.fanSpeed / 5F, 30F, 2.5D, false);
    }

    public static void tick(CreativeFusionKlystronBlockEntity be) {
        loop(be, be.fanSpeed / 5F, 30F, 2.5D, true);
    }

    public static void tick(FusionMHDTBlockEntity be) {
        loop(be, be.rotorSpeed / 15F, 30F, 2.5D, true);
    }

    private static <T extends BlockEntity & AudioLoop> void loop(
            T be, float speed, float range, double centreY, boolean entityPosition) {
        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                speed > 0F
                        && me != null
                        && (entityPosition ? me.position() : me.getEyePosition())
                                        .distanceToSqr(
                                                pos.getX() + 0.5,
                                                pos.getY() + centreY,
                                                pos.getZ() + 0.5)
                                < range * range,
                speed,
                speed);
    }
}
