// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityBoatRubber;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemBoatRubber extends Item {

    public ItemBoatRubber(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

        if (hit.getType() == HitResult.Type.MISS) return InteractionResult.PASS;

        Vec3 look = player.getViewVector(1F);
        List<Entity> blocking =
                level.getEntities(
                        player,
                        player.getBoundingBox().expandTowards(look.scale(5D)).inflate(1D),
                        EntitySelector.CAN_BE_PICKED);

        if (!blocking.isEmpty()) {
            Vec3 eye = player.getEyePosition();

            for (Entity entity : blocking) {
                AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (box.contains(eye)) return InteractionResult.PASS;
            }
        }

        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;

        EntityBoatRubber boat =
                ModEntities.BOAT_RUBBER.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (boat == null) return InteractionResult.FAIL;

        Vec3 at = hit.getLocation();
        boat.setInitialPos(at.x, at.y, at.z);
        boat.setYRot(player.getYRot());
        if (level instanceof ServerLevel server) {
            EntityType.<EntityBoatRubber>createDefaultStackConfig(server, stack, player)
                    .apply(boat);
        }

        if (!level.noCollision(boat, boat.getBoundingBox())) return InteractionResult.FAIL;

        if (!level.isClientSide()) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
