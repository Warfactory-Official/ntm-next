// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.StoredItems;
import com.hbm.tileentity.machine.storage.BlockEntityCrateSupply;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityParachuteCrate extends Entity implements StoredItems {

    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    public List<ItemStack> items = new ArrayList<>();

    @Override
    public void visitStoredItems(Visitor visitor) {
        boolean changed = false;
        for (ItemStack stack : items) changed |= visitor.visit(stack);
        if (changed) items.removeIf(ItemStack::isEmpty);
    }

    public EntityParachuteCrate(EntityType<? extends EntityParachuteCrate> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) return;

        var motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (motion.y > -0.2) setDeltaMovement(motion.x, motion.y - 0.02, motion.z);
        if (getY() > 600) setPos(getX(), 600, getZ());

        BlockPos pos = blockPosition();
        if (!level().getBlockState(pos).isAir()) {
            discard();

            BlockPos above = pos.above();
            level().setBlockAndUpdate(above, ModBlocks.CRATE_SUPPLY.get().defaultBlockState());
            if (level().getBlockEntity(above) instanceof BlockEntityCrateSupply crate) {
                int slot = 0;
                for (ItemStack stack : this.items) {

                    if (stack.isEmpty() || slot >= crate.inventory.size()) continue;
                    crate.inventory.set(slot++, stack);
                }
                crate.setChanged();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.items =
                new ArrayList<>(
                        input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(this.items));
    }
}
