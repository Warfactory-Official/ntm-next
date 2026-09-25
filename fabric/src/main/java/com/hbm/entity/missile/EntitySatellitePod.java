// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.interfaces.StoredItems;
import com.hbm.packet.toclient.GasFlamePayload;
import com.hbm.tileentity.machine.BlockEntityMachineSatDock;
import com.hbm.util.InventoryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class EntitySatellitePod extends EntityThrowableInterp implements StoredItems {

    private static final EntityDataAccessor<Integer> LEGS =
            SynchedEntityData.defineId(EntitySatellitePod.class, EntityDataSerializers.INT);
    private static final float LEG_SPEED = 1F / 20F;

    public ItemStack[] slots = new ItemStack[0];
    public int timer;
    public int callerYPos;
    public double speed = 0.75D;
    public float legs;
    public float prevLegs;

    public EntitySatellitePod(EntityType<? extends EntitySatellitePod> type, Level level) {
        super(type, level);
    }

    public EntitySatellitePod setup(int callerY, List<ItemStack> cargo) {
        callerYPos = callerY;
        slots = cargo.stream().map(ItemStack::copy).toArray(ItemStack[]::new);
        return this;
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = slots[i];
            if (visitor.visit(stack) && stack.isEmpty()) slots[i] = ItemStack.EMPTY;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEGS, 0);
    }

    public boolean doesDeployLegs() {
        return entityData.get(LEGS) == 1;
    }

    public void setDeployLegs(boolean deploy) {
        entityData.set(LEGS, deploy ? 1 : 0);
    }

    public boolean isLanding() {
        return slots.length > 0;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (isLanding()) {
                if (timer > 0) {
                    timer++;
                    setPos(getX(), Math.ceil(getY()), getZ());
                    if (timer >= 100) unloadItems();
                } else if (onGround()) {
                    speed = 0D;
                    timer = 1;
                    setDeployLegs(true);
                } else {
                    if (getY() < callerYPos + 17 && !doesDeployLegs()) setDeployLegs(true);
                    if (getY() < callerYPos + 25) speed -= 0.01D;
                    speed = Mth.clamp(speed, 0.025D, 0.75D);
                }
                setDeltaMovement(0D, -speed, 0D);
            } else {
                speed += 0.01D;
                if (speed >= 0.2D) setDeployLegs(false);
                speed = Mth.clamp(speed, 0D, 2D);
                setDeltaMovement(0D, speed, 0D);
                if (getY() > 300) discard();
            }
        } else {
            prevLegs = legs;
            legs = Mth.clamp(legs + (doesDeployLegs() ? LEG_SPEED : -LEG_SPEED), 0F, 1F);
            if (legs > 0 && getDeltaMovement().y < 0 || getDeltaMovement().y > 0) {
                ClientEffects.spawnGasFlame(
                        level(),
                        getX(),
                        getY() + 0.5D,
                        getZ(),
                        0D,
                        speed - 1D,
                        0D,
                        GasFlamePayload.DEFAULT_SCALE);
            }
        }
    }

    public void unloadItems() {
        BlockPos below = BlockPos.containing(getX(), getY() - 0.5D, getZ());
        if (level().getBlockState(below).is(ModBlocks.SAT_DOCK.get())
                && level().getBlockEntity(below) instanceof BlockEntityMachineSatDock dock) {
            for (int i = 0; i < slots.length; i++) {
                ItemStack stack = slots[i];
                if (stack.isEmpty()) continue;
                slots[i] =
                        InventoryUtil.tryAddItemToInventory(
                                dock, 0, BlockEntityMachineSatDock.CARGO_SLOTS - 1, stack);
            }
        }
        if (level() instanceof ServerLevel server) {
            for (ItemStack stack : slots)
                if (!stack.isEmpty()) spawnAtLocation(server, stack, 0.25F);
        }
        slots = new ItemStack[0];
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setDeployLegs(input.getIntOr("state", 0) == 1);
        timer = input.getIntOr("timer", 0);
        speed = input.getDoubleOr("speed", 0.75D);
        callerYPos = input.getIntOr("callerYPos", 0);
        slots =
                input.read("slots", ItemStack.OPTIONAL_CODEC.listOf())
                        .orElse(List.of())
                        .toArray(ItemStack[]::new);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("state", doesDeployLegs() ? 1 : 0);
        output.putInt("timer", timer);
        output.putDouble("speed", speed);
        output.putInt("callerYPos", callerYPos);
        List<ItemStack> stored = new ArrayList<>(slots.length);
        for (ItemStack stack : slots) stored.add(stack);
        output.store("slots", ItemStack.OPTIONAL_CODEC.listOf(), stored);
    }

    @Override
    protected void onImpact(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK) {
            Vec3 point = result.getLocation();
            setPos(point.x, point.y, point.z);
            setOnGround(true);
        }
    }

    @Override
    public boolean doesImpactEntities() {
        return false;
    }

    @Override
    public float getAirDrag() {
        return 1F;
    }

    @Override
    public float getWaterDrag() {
        return 1F;
    }

    @Override
    public double getGravityVelocity() {
        return 0D;
    }
}
