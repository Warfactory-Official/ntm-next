// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;

public class ItemModBattery extends ItemArmorMod {

    public final double mod;

    public ItemModBattery(Properties properties, double mod) {
        super(properties, ArmorModHandler.BATTERY, true, true, true, true);
        this.mod = mod;
    }
}
