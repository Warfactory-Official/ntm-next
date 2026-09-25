// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.ai;

import com.hbm.entity.mob.EntityPigeon;
import com.hbm.entity.mob.IFlyingCreature;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public class EntityAIEatBread extends Goal {

    private final EntityPigeon pigeon;
    private final double speed;
    private @Nullable ItemEntity item;

    public EntityAIEatBread(EntityPigeon pigeon, double speed) {
        this.pigeon = pigeon;
        this.speed = speed;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (pigeon.isFat() || pigeon.getFlyingState() != IFlyingCreature.STATE_WALKING)
            return false;

        List<ItemEntity> items =
                pigeon.level()
                        .getEntitiesOfClass(
                                ItemEntity.class, pigeon.getBoundingBox().inflate(10, 10, 10));

        for (ItemEntity candidate : items) {
            if (candidate.getItem().is(Items.BREAD)) {
                item = candidate;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return item != null && !item.isRemoved() && canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (item == null) return;

        pigeon.getLookControl().setLookAt(item, 30.0F, pigeon.getMaxHeadXRot());

        if (pigeon.distanceTo(item) > 1) {
            pigeon.getNavigation().moveTo(item, speed);
            return;
        }

        if (pigeon.getRandom().nextInt(3) == 0) {
            ItemStack stack = item.getItem();

            if (stack.getCount() > 1) {
                stack.shrink(1);
                ItemEntity remainder =
                        new ItemEntity(
                                pigeon.level(), item.getX(), item.getY(), item.getZ(), stack);
                pigeon.level().addFreshEntity(remainder);
            }

            item.discard();
        }

        pigeon.setFat(true);
        pigeon.playSound(
                SoundEvents.GENERIC_EAT.value(),
                0.5F + 0.5F * pigeon.getRandom().nextInt(2),
                (pigeon.getRandom().nextFloat() - pigeon.getRandom().nextFloat()) * 0.2F + 1.0F);
    }
}
