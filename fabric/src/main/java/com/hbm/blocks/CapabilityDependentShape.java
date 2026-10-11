// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;

public interface CapabilityDependentShape {
    static BlockBehaviour.Properties deferShapeToFullChunk(BlockBehaviour.Properties props) {
        return props.postProcess((state, level, pos) -> pos);
    }

    interface Endpoint extends CapabilityDependentShape {}
}
