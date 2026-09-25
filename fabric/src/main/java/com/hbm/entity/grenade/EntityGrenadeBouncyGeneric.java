// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.grenade;

import com.hbm.entity.ModEntities;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemGenericGrenade;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityGrenadeBouncyGeneric extends EntityGrenadeBouncyBase implements IGenericGrenade {

    private static final EntityDataAccessor<ItemStack> GRENADE =
            SynchedEntityData.defineId(
                    EntityGrenadeBouncyGeneric.class, EntityDataSerializers.ITEM_STACK);

    public EntityGrenadeBouncyGeneric(
            EntityType<? extends EntityGrenadeBouncyGeneric> type, Level level) {
        super(type, level);
    }

    public EntityGrenadeBouncyGeneric(Level level) {
        this(ModEntities.GRENADE_BOUNCY_GENERIC.get(), level);
    }

    public EntityGrenadeBouncyGeneric(Level level, LivingEntity thrower) {
        this(level);
        initThrower(thrower);
    }

    public EntityGrenadeBouncyGeneric(Level level, double x, double y, double z) {
        this(level);
        setPos(x, y, z);
    }

    public EntityGrenadeBouncyGeneric setType(ItemGenericGrenade grenade) {
        entityData.set(GRENADE, new ItemStack(grenade));
        return this;
    }

    @Override
    public ItemGenericGrenade getGrenade() {
        return entityData.get(GRENADE).getItem() instanceof ItemGenericGrenade gren
                ? gren
                : ModItems.STICK_DYNAMITE.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GRENADE, ItemStack.EMPTY);
    }

    @Override
    public void explode() {
        getGrenade().explode(this, getThrower(), level(), getX(), getY(), getZ());
        discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("grenade", ItemStack.OPTIONAL_CODEC, entityData.get(GRENADE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("grenade", ItemStack.OPTIONAL_CODEC)
                .ifPresent(stack -> entityData.set(GRENADE, stack));
    }

    @Override
    protected int getMaxTimer() {
        return getGrenade().getMaxTimer();
    }

    @Override
    protected double getBounceMod() {
        return getGrenade().getBounceMod();
    }
}
