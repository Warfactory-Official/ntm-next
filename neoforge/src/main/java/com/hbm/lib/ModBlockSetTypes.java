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

package com.hbm.lib;

import com.hbm.util.RegistryUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public final class ModBlockSetTypes {
    public static final BlockSetType HBM_METAL = register("hbm_metal", SoundType.METAL);
    public static final BlockSetType HBM_DOOR = register("hbm_door", SoundType.STONE);

    private ModBlockSetTypes() {}

    private static BlockSetType register(String name, SoundType step) {
        return BlockSetType.register(
                new BlockSetType(
                        name,
                        true,
                        true,
                        false,
                        BlockSetType.PressurePlateSensitivity.EVERYTHING,
                        step,
                        SoundEvent.createVariableRangeEvent(RegistryUtil.id("door_open")),
                        SoundEvent.createVariableRangeEvent(RegistryUtil.id("door_open")),
                        SoundEvent.createVariableRangeEvent(RegistryUtil.id("door_open")),
                        SoundEvent.createVariableRangeEvent(RegistryUtil.id("door_open")),
                        SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
                        SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
                        SoundEvents.STONE_BUTTON_CLICK_OFF,
                        SoundEvents.STONE_BUTTON_CLICK_ON));
    }
}
