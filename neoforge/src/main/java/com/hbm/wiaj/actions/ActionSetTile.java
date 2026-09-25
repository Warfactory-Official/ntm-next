// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actions;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;
import net.minecraft.world.level.block.entity.BlockEntity;

public record ActionSetTile(int x, int y, int z, BlockEntity tile) implements IJarAction {
    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public void act(WorldInAJar world, JarScene scene) {
        world.setTileEntity(x, y, z, tile);
    }
}
