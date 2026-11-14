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

import com.hbm.client.gui.WeaponTableScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class ClientPlayerAccess {

    private ClientPlayerAccess() {}

    public static Component keyLabel(String name) {
        return KeyMapping.get(name).getTranslatedKeyMessage();
    }

    public static @Nullable Player player() {
        return Minecraft.getInstance().player;
    }

    public static boolean firstPersonCamera() {
        return Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    public static boolean weaponTableOpen() {
        return Minecraft.getInstance().gui.screen() instanceof WeaponTableScreen;
    }
}
