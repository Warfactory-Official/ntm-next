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

package com.hbm.client.weapon;

import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.hud.GunHudWidget;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class GunClientProperties {

    private static final ConcurrentHashMap<Item, GunClientProperties> SPECS =
            new ConcurrentHashMap<>();

    private final List<List<GunHudWidget>> hud;

    private GunClientProperties(List<List<GunHudWidget>> hud) {
        this.hud = hud;
    }

    @SafeVarargs
    public static void register(GunBaseNTItem gun, List<GunHudWidget>... hud) {
        if (hud.length > gun.getConfigCount())
            throw new IllegalArgumentException(
                    hud.length + " HUD lists for " + gun.getConfigCount() + " configs");
        SPECS.put(gun, new GunClientProperties(List.of(hud)));
    }

    public static @Nullable List<GunHudWidget> getHudWidgets(Item gun, int config) {
        var spec = SPECS.get(gun);
        return spec == null || config >= spec.hud.size() ? null : spec.hud.get(config);
    }
}
