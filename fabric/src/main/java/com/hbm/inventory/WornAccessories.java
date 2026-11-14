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

package com.hbm.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public final class WornAccessories {
    private static Source[] sources = new Source[0];

    @FunctionalInterface
    public interface Source {
        void collect(Player player, List<Container> out);
    }

    private WornAccessories() {}

    public static void register(Source source) {
        sources = Arrays.copyOf(sources, sources.length + 1);
        sources[sources.length - 1] = source;
    }

    public static List<Container> of(Player player) {
        if (sources.length == 0) return List.of();
        List<Container> out = new ArrayList<>();
        for (Source source : sources) source.collect(player, out);
        return out;
    }
}
