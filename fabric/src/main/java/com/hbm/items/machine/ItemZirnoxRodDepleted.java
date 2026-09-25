// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import net.minecraft.world.item.Item;

public class ItemZirnoxRodDepleted extends Item {

    public final float wasteRad;
    public final float blinding;

    public ItemZirnoxRodDepleted(Properties props, float wasteRad, float blinding) {
        super(props);
        this.wasteRad = wasteRad;
        this.blinding = blinding;
    }
}
