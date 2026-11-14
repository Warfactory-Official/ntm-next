/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory;

import com.hbm.NuclearTech;
import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.blockentity.machine.RadarScreenBlockEntity;
import com.hbm.blockentity.processing.ElectrolysisBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SpectatorMenus {

    private SpectatorMenus() {}

    public static boolean open(ServerPlayer player, Level level, BlockPos pos) {
        if (MultiblockSurface.isFoldedCell(level.getBlockState(pos))) {
            MultiblockCoreBlock.FoldedOwner owner = MultiblockCoreBlock.ownerOf(level, pos);
            if (owner == null) return false;
            pos = owner.pos();
        }
        BlockEntity be = ChunkUtil.blockEntityIfLoaded(level, pos);
        if (be instanceof RadarScreenBlockEntity screen) {
            BlockPos radarPos = screen.linkedRadar();
            if (radarPos == null) return false;
            be = ChunkUtil.blockEntityIfLoaded(level, radarPos);
            if (!(be instanceof RadarBlockEntity)) return false;
        }
        if (be instanceof RadarBlockEntity radar) return radar.openMap(player);
        if (!(be instanceof MenuProvider provider)
                || !NuclearTech.MOD_ID.equals(
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getNamespace()))
            return false;
        if (be instanceof RandomizableContainer container && container.getLootTable() != null)
            return true;
        if (be instanceof ElectrolysisBlockEntity electrolyser)
            electrolyser.selectedMenu().open(player, electrolyser);
        else BlockMenus.open(player, provider);
        return true;
    }
}
