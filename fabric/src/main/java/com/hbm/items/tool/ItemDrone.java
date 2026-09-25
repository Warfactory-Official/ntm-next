// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.entity.item.EntityDeliveryDrone;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemDrone extends Item {

    public final EnumDroneType type;

    public ItemDrone(Properties props, EnumDroneType type) {
        super(props);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (type != EnumDroneType.REQUEST) {
            EntityDeliveryDrone drone = new EntityDeliveryDrone(level);
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
        context.getItemInHand().shrink(1);

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
