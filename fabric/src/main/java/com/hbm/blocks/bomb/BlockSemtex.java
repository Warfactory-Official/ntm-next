// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.entity.item.EntityTntNtm;
import net.minecraft.world.level.Level;

public class BlockSemtex extends BlockTNTBase {

    public BlockSemtex(Properties props) {
        super(props, 20);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, EntityTntNtm entity) {
        vanillaBlast(level, x, y, z, entity, 12F);
    }
}
