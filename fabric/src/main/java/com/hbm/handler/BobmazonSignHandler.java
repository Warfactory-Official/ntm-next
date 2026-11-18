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

package com.hbm.handler;

import com.hbm.items.ModItems;
import com.hbm.util.ShadyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

public final class BobmazonSignHandler {

    private BobmazonSignHandler() {}

    public static boolean onSignUsed(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return false;
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return false;

        var messages = sign.getText(SignTextSlot.FRONT).getMessages(false);
        String result =
                ShadyUtil.smoosh(
                        messages.get(0).getString(),
                        messages.get(1).getString(),
                        messages.get(2).getString(),
                        messages.get(3).getString());

        if (!ShadyUtil.HASHES.contains(result)) return false;

        level.destroyBlock(pos, false);

        ItemEntity drop =
                new ItemEntity(
                        level,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        new ItemStack(ModItems.BOBMAZON_HIDDEN));
        drop.setPickUpDelay(10);
        level.addFreshEntity(drop);

        return true;
    }
}
