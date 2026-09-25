// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import com.hbm.platform.Services;
import net.minecraft.world.level.block.Block;

public final class FlammableBlocks {

    private FlammableBlocks() {}

    public static void register() {

        set(ModBlocks.BLOCK_GRAPHITE.get(), 30, 5);

        set(ModBlocks.BLOCK_GRAPHITE_DRILLED.get(), 30, 5);
        set(ModBlocks.BLOCK_GRAPHITE_FUEL.get(), 30, 5);
        set(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get(), 30, 5);
        set(ModBlocks.BLOCK_GRAPHITE_ROD.get(), 30, 5);
        set(ModBlocks.BLOCK_GRAPHITE_SOURCE.get(), 30, 5);
        set(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get(), 30, 5);
        set(ModBlocks.BLOCK_GRAPHITE_TRITIUM.get(), 30, 5);
        set(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get(), 30, 5);

        set(ModBlocks.PILE_BRICK.get(), 30, 5);

        set(ModBlocks.BLOCK_COKE_COAL.get(), 10, 5);
        set(ModBlocks.BLOCK_COKE_LIGNITE.get(), 10, 5);
        set(ModBlocks.BLOCK_COKE_PETROLEUM.get(), 10, 5);

        set(ModBlocks.RED_BARREL.get(), 2, 15);
        set(ModBlocks.PINK_BARREL.get(), 2, 15);
        set(ModBlocks.LOX_BARREL.get(), 0, 0);
        set(ModBlocks.TAINT_BARREL.get(), 0, 0);

        set(ModBlocks.YELLOW_BARREL.get(), 0, 0);
        set(ModBlocks.VITRIFIED_BARREL.get(), 0, 0);

        set(ModBlocks.DET_CHARGE.get(), 0, 0);
        set(ModBlocks.DET_NUKE.get(), 0, 0);

        set(ModBlocks.DET_CORD.get(), 0, 0);

        set(ModBlocks.BLOCK_SEMTEX.get(), 0, 0);
        set(ModBlocks.BLOCK_C4.get(), 0, 0);

        set(ModBlocks.DYNAMITE.get(), 15, 100);
        set(ModBlocks.TNT_NTM.get(), 15, 100);
        set(ModBlocks.SEMTEX.get(), 15, 100);
        set(ModBlocks.C4.get(), 15, 100);
        set(ModBlocks.FISSURE_BOMB.get(), 15, 100);
    }

    private static void set(Block block, int encouragement, int flammability) {
        Services.PLATFORM.setFlammable(block, encouragement, flammability);
    }
}
