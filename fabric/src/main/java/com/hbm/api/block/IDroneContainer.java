// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.block;

import net.minecraft.world.Container;

public interface IDroneContainer {

    interface Provider extends IDroneContainer {
        Container droneOfferInventory();
    }

    interface Requester extends IDroneContainer {
        Container droneDeliveryInventory();
    }

    interface Dock extends IDroneContainer {
        Container droneDockInventory();
    }
}
