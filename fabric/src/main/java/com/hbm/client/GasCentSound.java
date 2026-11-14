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

import com.hbm.blockentity.machine.GasCentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class GasCentSound {

    private GasCentSound() {}

    public static void tick(GasCentBlockEntity be) {
        be.tickSound();

        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                be.audioDuration > 10
                        && me != null
                        && me.getEyePosition()
                                        .distanceToSqr(
                                                pos.getX() + 0.5,
                                                pos.getY() + 0.5,
                                                pos.getZ() + 0.5)
                                < 25 * 25,
                1F,
                (be.audioDuration - 10) / 100F + 0.5F);
    }
}
