// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.machine.ItemArcElectrode.EnumElectrodeType;
import net.minecraft.world.item.Item;

public class ItemArcElectrodeBurnt extends Item {

    public final EnumElectrodeType type;

    public ItemArcElectrodeBurnt(Properties props, EnumElectrodeType type) {
        super(props);
        this.type = type;
    }
}
