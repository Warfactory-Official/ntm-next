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

import com.hbm.blockentity.processing.ChemicalPlantBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class ChemPlantSound {

    private ChemPlantSound() {}

    public static void tick(ChemicalPlantBlockEntity be) {
        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                be.isProgressing
                        && me != null
                        && me.getEyePosition().distanceToSqr(pos.getX(), pos.getY(), pos.getZ())
                                < 30 * 30,
                1F);
    }
}
