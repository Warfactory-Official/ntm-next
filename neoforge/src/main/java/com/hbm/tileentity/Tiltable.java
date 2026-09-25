// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public interface Tiltable {

    BooleanProperty TILTED = BooleanProperty.create("tilted");

    enum TiltType {
        UNAVOIDABLE,
        CONFIG
    }

    int getFloorCount();

    @Nullable BlockPos getFloorPosFromIndex(int index);

    boolean isTilted();

    void checkTilt(TiltType cfg, boolean extraHeavy);

    default BlockPos standardFloor3x3(int index) {
        return ((BlockEntity) this)
                .getBlockPos()
                .offset(-1 + (index / 2) * 2, -1, -1 + (index % 2) * 2);
    }

    default BlockPos standardFloor5x5(int index) {
        return ((BlockEntity) this)
                .getBlockPos()
                .offset(-2 + (index / 3) * 2, -1, -2 + (index % 3) * 2);
    }

    default BlockPos standardFloor7x7(int index) {
        return ((BlockEntity) this)
                .getBlockPos()
                .offset(-3 + (index / 4) * 2, -1, -3 + (index % 4) * 2);
    }
}
