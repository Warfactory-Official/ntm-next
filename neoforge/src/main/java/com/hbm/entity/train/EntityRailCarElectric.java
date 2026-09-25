// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.train;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class EntityRailCarElectric extends EntityRailCarRidable {

    private static final EntityDataAccessor<Integer> POWER =
            SynchedEntityData.defineId(EntityRailCarElectric.class, EntityDataSerializers.INT);

    public EntityRailCarElectric(EntityType<? extends EntityRailCarElectric> type, Level level) {
        super(type, level);
    }

    public abstract int getMaxPower();

    public abstract int getPowerConsumption();

    public boolean hasChargeSlot() {
        return false;
    }

    public int getChargeSlot() {
        return 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POWER, 0);
    }

    @Override
    public boolean canAccelerate() {
        return true;
    }

    @Override
    public void consumeFuel() {}

    public void setPower(int power) {
        this.entityData.set(POWER, power);
    }

    public int getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide() && this.hasChargeSlot()) {

            ItemStack stack = this.getItem(this.getChargeSlot());

            if (stack.getItem() instanceof IBatteryItem battery) {
                int powerNeeded = this.getMaxPower() - this.getPower();
                long powerProvided =
                        Math.min(battery.getDischargeRate(stack), battery.getCharge(stack));
                int powerTransfered = (int) Math.min(powerNeeded, powerProvided);

                if (powerTransfered > 0) {
                    battery.dischargeBattery(stack, powerTransfered);
                    this.setPower(this.getPower() + powerTransfered);
                }
            } else if (stack.is(ModItems.BATTERY_CREATIVE.get())) {
                this.setPower(this.getMaxPower());
            }
        }
    }
}
