// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.MultiblockCrumbling;
import dev.engine_room.flywheel.api.visualization.CrumblingOwners;

public final class MultiblockCrumblingVisuals {

    private MultiblockCrumblingVisuals() {}

    public static void register() {
        CrumblingOwners.register(MultiblockCrumbling::ownerOf);
    }
}
