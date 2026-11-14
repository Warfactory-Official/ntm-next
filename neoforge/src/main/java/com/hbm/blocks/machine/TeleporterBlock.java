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

import com.hbm.blockentity.travel.TeleporterBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TeleporterBlock extends Block
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {

    public TeleporterBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeleporterBlockEntity(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof TeleporterBlockEntity tele)) return;

        info.title(getName());
        GlobalPos target = tele.target;
        if (target == null) {
            info.row(
                    Component.translatable("overlay.hbm.no_destination")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        info.row(
                Component.literal(
                                String.format(Locale.ROOT, "%,d", tele.power)
                                        + " / "
                                        + String.format(
                                                Locale.ROOT,
                                                "%,d",
                                                TeleporterBlockEntity.MAX_POWER))
                        .withStyle(
                                tele.power >= TeleporterBlockEntity.CONSUMPTION
                                        ? ChatFormatting.GREEN
                                        : ChatFormatting.RED));
        info.row(
                Component.translatable(
                        "overlay.hbm.destination",
                        target.pos().getX(),
                        target.pos().getY(),
                        target.pos().getZ(),
                        target.dimension().identifier().toString()));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TELEBLOCK).powerIn().fe();
    }
}
