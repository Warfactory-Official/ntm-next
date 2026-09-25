// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import net.minecraft.world.level.block.state.BlockState;

public interface ISectionGeometryVertexConsumer {
    void hbm$beginSectionGeometry(BlockState state, int x, int y, int z);

    void hbm$endSectionGeometry();
}
