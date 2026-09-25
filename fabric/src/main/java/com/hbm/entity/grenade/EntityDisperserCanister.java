// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.grenade;

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemDisperser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityDisperserCanister extends EntityGrenadeBase {

    private static final EntityDataAccessor<Integer> FLUID =
            SynchedEntityData.defineId(EntityDisperserCanister.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> TYPE =
            SynchedEntityData.defineId(
                    EntityDisperserCanister.class, EntityDataSerializers.ITEM_STACK);

    public EntityDisperserCanister(
            EntityType<? extends EntityDisperserCanister> type, Level level) {
        super(type, level);
    }

    public EntityDisperserCanister(Level level) {
        this(ModEntities.DISPERSER_CANISTER.get(), level);
    }

    public EntityDisperserCanister(Level level, LivingEntity thrower) {
        this(level);
        initThrower(thrower);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLUID, 0);
        builder.define(TYPE, ItemStack.EMPTY);
    }

    public Fluid getFluid() {
        Fluid fluid = BuiltInRegistries.FLUID.byId(entityData.get(FLUID));
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    public EntityDisperserCanister setFluid(Fluid fluid) {
        entityData.set(FLUID, BuiltInRegistries.FLUID.getId(fluid));
        return this;
    }

    public ItemDisperser getDisperserItem() {
        return entityData.get(TYPE).getItem() instanceof ItemDisperser disperser
                ? disperser
                : ModItems.DISPERSER_CANISTER.get();
    }

    public EntityDisperserCanister setDisperserItem(ItemDisperser item) {
        entityData.set(TYPE, new ItemStack(item));
        return this;
    }

    @Override
    public void explode() {
        if (!level().isClientSide()) {
            EntityMist mist = new EntityMist(level());
            mist.setType(getFluid());
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(10, 5);
            mist.setDuration(80);
            level().addFreshEntity(mist);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("fluid", entityData.get(FLUID));
        output.store("item", ItemStack.OPTIONAL_CODEC, entityData.get(TYPE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(FLUID, input.getIntOr("fluid", 0));
        input.read("item", ItemStack.OPTIONAL_CODEC)
                .ifPresent(stack -> entityData.set(TYPE, stack));
    }
}
