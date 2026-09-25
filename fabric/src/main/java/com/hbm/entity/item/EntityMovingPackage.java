// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.entity.ModEntities;
import com.hbm.interfaces.StoredItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityMovingPackage extends EntityMovingConveyorObject
        implements IConveyorPackage, StoredItems {

    protected List<ItemStack> contents = new ArrayList<>();

    @Override
    public void visitStoredItems(Visitor visitor) {
        boolean changed = false;
        for (ItemStack stack : contents) changed |= visitor.visit(stack);
        if (changed) contents.removeIf(ItemStack::isEmpty);
    }

    public EntityMovingPackage(EntityType<? extends EntityMovingPackage> type, Level level) {
        super(type, level);
    }

    public EntityMovingPackage(Level level) {
        super(ModEntities.MOVING_PACKAGE.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    public void setItemStacks(List<ItemStack> stacks) {
        this.contents = new ArrayList<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) this.contents.add(stack.copy());
    }

    @Override
    public List<ItemStack> getItemStacks() {
        return this.contents;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (level().isClientSide() || isRemoved()) return InteractionResult.PASS;

        for (ItemStack stack : this.contents) {
            player.getInventory().placeItemBackInInventory(stack);
        }

        discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            discard();
            for (ItemStack stack : this.contents) {
                server.addFreshEntity(
                        new ItemEntity(server, getX(), getY() + 0.125, getZ(), stack));
            }
        }
        return false;
    }

    @Override
    public void enterBlock(IEnterableBlock enterable, BlockPos pos, @Nullable Direction dir) {
        if (isRemoved()) return;

        if (enterable.canPackageEnter(level(), pos, dir, this)) {
            enterable.onPackageEnter(level(), pos, dir, this);
            discard();
        }
    }

    @Override
    public boolean onLeaveConveyor() {
        discard();

        Vec3 motion = getDeltaMovement();
        for (ItemStack stack : this.contents) {
            ItemEntity item =
                    new ItemEntity(
                            level(),
                            getX() + motion.x * 2,
                            getY() + motion.y * 2,
                            getZ() + motion.z * 2,
                            stack);
            item.setDeltaMovement(motion.x * 2, 0.1, motion.z * 2);
            level().addFreshEntity(item);
        }

        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

        this.contents =
                new ArrayList<>(input.read("contents", ItemStack.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("contents", ItemStack.CODEC.listOf(), List.copyOf(this.contents));
    }
}
