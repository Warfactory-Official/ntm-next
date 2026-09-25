// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib;

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
                        SoundEvent.createVariableRangeEvent(Library.id("door_open")),
                        SoundEvent.createVariableRangeEvent(Library.id("door_open")),
                        SoundEvent.createVariableRangeEvent(Library.id("door_open")),
                        SoundEvent.createVariableRangeEvent(Library.id("door_open")),
                        SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
                        SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
                        SoundEvents.STONE_BUTTON_CLICK_OFF,
                        SoundEvents.STONE_BUTTON_CLICK_ON));
    }
}
