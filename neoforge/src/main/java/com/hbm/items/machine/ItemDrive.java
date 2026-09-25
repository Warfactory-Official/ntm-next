// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.saveddata.satellites.Satellite.DriveType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ItemDrive extends Item {
    public final DriveType type;

    public ItemDrive(Properties properties, DriveType type) {
        super(properties);
        this.type = type;
    }

    public static @Nullable DriveType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemDrive drive ? drive.type : null;
    }
}
