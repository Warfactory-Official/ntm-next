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

package com.hbm.blocks.machine;

import com.hbm.blockentity.processing.ChemicalFactoryBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.util.NumberFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ChemicalFactoryBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public ChemicalFactoryBlock(Properties props) {
        super(props);
    }

    private static int gradient(double frac) {
        frac = Math.clamp(frac, 0D, 1D);
        return ((int) (0xFF - 0xFF * frac) << 16) | ((int) (0xFF * frac) << 8);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemicalFactoryBlockEntity(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof ChemicalFactoryBlockEntity be)) return;
        info.title(getName());
        double frac = be.getMaxPower() <= 0 ? 0 : (double) be.getPower() / be.getMaxPower();
        info.row(
                Component.translatable(
                        "overlay.hbm.charge_spaced",
                        NumberFormatter.formatCompact(be.getPower()),
                        NumberFormatter.formatCompact(be.getMaxPower())),
                gradient(frac));

        for (int i = 0; i < ChemicalFactoryBlockEntity.MODULES; i++) {
            if (be.module[i].getRecipeName().isEmpty()) continue;
            GenericRecipe rec = be.module[i].getRecipe();
            String name =
                    rec != null ? rec.getLocalizedName().getString() : be.module[i].getRecipeName();
            info.row(
                    Component.translatable(
                            "overlay.hbm.module",
                            (i + 1),
                            name,
                            (int) Math.round(be.module[i].progress * 100)),
                    0xFFFF00);
        }

        info.row(
                Component.translatable(
                        "overlay.hbm.water", be.water().getFill(), be.water().getMaxFill()),
                0x5555FF);
        info.row(
                Component.translatable(
                        "overlay.hbm.spent_steam", be.lps().getFill(), be.lps().getMaxFill()),
                0xAAAAAA);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().fluidOut().itemsAtCells().fe();
    }
}
