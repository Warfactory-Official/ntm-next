// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.cart;

import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class EntityMinecartContainerBase extends AbstractMinecartContainer
        implements ICartChassis {

    private static final EntityDataAccessor<Integer> BASE =
            SynchedEntityData.defineId(
                    EntityMinecartContainerBase.class, EntityDataSerializers.INT);

    protected EntityMinecartContainerBase(
            EntityType<? extends EntityMinecartContainerBase> type, Level level) {
        super(type, level);

        clearItemStacks();
    }

    protected EntityMinecartContainerBase(
            EntityType<? extends EntityMinecartContainerBase> type,
            Level level,
            double x,
            double y,
            double z,
            EnumCartBase base) {
        this(type, level);
        setInitialPos(x, y, z);
        setBase(base);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BASE, 0);
    }

    public void setBase(EnumCartBase type) {
        entityData.set(BASE, type.ordinal());
    }

    @Override
    public EnumCartBase getBase() {
        return EnumCartBase.values()[entityData.get(BASE)];
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    protected Item getDropItem() {
        return getCartItem().getItem();
    }

    @Override
    public ItemStack getPickResult() {
        return getCartItem();
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        clearContent();
        kill(level);

        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack stack = getCartItem();
            if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());
            spawnAtLocation(level, stack);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("base", entityData.get(BASE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(BASE, input.getIntOr("base", 0));
    }
}
