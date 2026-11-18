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

package com.hbm.blocks.generic;

import com.hbm.NuclearTech;
import com.hbm.config.BlockSelector;
import com.hbm.config.ConfigEntry;
import com.hbm.config.ConfigSchema;
import com.hbm.config.ConfigStore;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public final class DoorControls {
    static final byte PULSE = 1;
    static final byte TOOLABLE = 1 << 1;
    static final byte REDSTONE_ONLY = 1 << 2;

    private static volatile List<BlockSelector> pulse = List.of();
    private static volatile List<BlockSelector> toolable = List.of();
    private static volatile List<BlockSelector> redstoneOnly = List.of();
    private static volatile boolean tagsBound;

    private DoorControls() {}

    public static void reload(ConfigStore runtime) {
        pulse = parse(runtime, ConfigSchema.DOOR_PULSE);
        toolable = parse(runtime, ConfigSchema.DOOR_TOOLABLE);
        redstoneOnly = parse(runtime, ConfigSchema.DOOR_REDSTONE_ONLY);
        if (tagsBound) resolve();
    }

    public static void tagsBound() {
        tagsBound = true;
        resolve();
    }

    private static List<BlockSelector> parse(ConfigStore runtime, ConfigEntry<List<String>> entry) {
        return runtime.get(entry).stream().map(BlockSelector::parse).toList();
    }

    private static void resolve() {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof DoorBaseBlock door)) continue;
            var holder = block.builtInRegistryHolder();
            var toolableBy = first(toolable, holder);
            var redstoneOnlyBy = first(redstoneOnly, holder);
            if (toolableBy != null && redstoneOnlyBy != null) {
                NuclearTech.LOGGER.error(
                        "Door {} matches toolable {} and redstone_only {}; neither applies",
                        holder.key().identifier(),
                        toolableBy,
                        redstoneOnlyBy);
                toolableBy = redstoneOnlyBy = null;
            }
            door.controls =
                    (byte)
                            ((first(pulse, holder) != null ? PULSE : 0)
                                    | (toolableBy != null ? TOOLABLE : 0)
                                    | (redstoneOnlyBy != null ? REDSTONE_ONLY : 0));
        }
    }

    private static @Nullable BlockSelector first(
            List<BlockSelector> selectors, Holder.Reference<Block> block) {
        for (var selector : selectors) {
            if (selector.matches(block)) return selector;
        }
        return null;
    }
}
