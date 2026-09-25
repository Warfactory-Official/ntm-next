// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

public interface PaintableCamoBlock {

    BooleanProperty paintedProperty();

    BooleanProperty overlayProperty();
}
