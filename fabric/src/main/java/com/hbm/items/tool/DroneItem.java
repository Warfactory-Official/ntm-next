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

package com.hbm.items.tool;

import com.hbm.entity.item.DeliveryDroneEntity;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DroneItem extends Item {

    public final EnumDroneType type;

    public DroneItem(Properties props, EnumDroneType type) {
        super(props);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player != null && player.isSpectator()) return InteractionResult.PASS;
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (type != EnumDroneType.REQUEST) {
            DeliveryDroneEntity drone = new DeliveryDroneEntity(level);
            drone.setExpress(
                    type == EnumDroneType.PATROL_EXPRESS
                            || type == EnumDroneType.PATROL_EXPRESS_CHUNKLOADING);
            drone.setChunkLoading(
                    type == EnumDroneType.PATROL_CHUNKLOADING
                            || type == EnumDroneType.PATROL_EXPRESS_CHUNKLOADING);
            BlockPos pos = context.getClickedPos();
            drone.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            level.addFreshEntity(drone);
        }
        context.getItemInHand().consume(1, player);

        return InteractionResult.SUCCESS;
    }

    public enum EnumDroneType {
        PATROL,
        PATROL_CHUNKLOADING,
        PATROL_EXPRESS,
        PATROL_EXPRESS_CHUNKLOADING,
        REQUEST;

        public static final EnumDroneType[] VALUES = values();
        public final String id = name().toLowerCase(Locale.ROOT);
    }
}
