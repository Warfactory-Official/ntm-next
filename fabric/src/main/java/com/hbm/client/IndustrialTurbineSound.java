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

import com.hbm.blockentity.machine.IndustrialTurbineBlockEntity;
import net.minecraft.client.Minecraft;

public final class IndustrialTurbineSound {

    private IndustrialTurbineSound() {}

    public static void tick(IndustrialTurbineBlockEntity be) {
        Minecraft mc = Minecraft.getInstance();
        double x = be.getBlockPos().getX() + 0.5,
                y = be.getBlockPos().getY() + 0.5,
                z = be.getBlockPos().getZ() + 0.5;
        boolean near =
                mc.player != null && mc.player.getEyePosition().distanceToSqr(x, y, z) <= 35 * 35;

        be.audioLoop(be.spin > 0 && near, be.audioVolume(), be.audioPitch());
    }
}
