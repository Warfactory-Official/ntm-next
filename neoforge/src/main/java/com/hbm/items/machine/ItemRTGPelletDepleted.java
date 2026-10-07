// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public class ItemRTGPelletDepleted extends Item {

    private final @Nullable DepletedRTGMaterial material;

    public ItemRTGPelletDepleted(Properties props, DepletedRTGMaterial material) {
        super(props);
        this.material = material;
    }

    public ItemRTGPelletDepleted(Properties props) {
        super(props);
        this.material = null;
    }

    public @Nullable DepletedRTGMaterial getMaterial() {
        return material;
    }
}
