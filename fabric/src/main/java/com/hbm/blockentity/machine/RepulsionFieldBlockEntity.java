/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.ForceFieldMenu;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RepulsionFieldBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_RADIUS = 1;
    public static final int SLOT_HEALTH = 2;
    public static final int SLOT_COUNT = 3;
    public static final int BASE_HEALTH = 100;
    public static final int COLOR_HIT = 0xFF0000;
    private static final int[] BATTERY_SLOT = {SLOT_BATTERY};
    private static final Predicate<Entity> AFFECTED =
            entity -> !(entity instanceof Player) && !entity.isSpectator();

    public float fieldRadius;

    {
        hbm$syncSet$RepulsionFieldBlockEntity$fieldRadius(
                MachineData.FORCE_FIELD_BASE_RADIUS.get());
    }

    public int shieldHealth;

    {
        hbm$syncSet$RepulsionFieldBlockEntity$shieldHealth(BASE_HEALTH);
    }

    public int shieldCapacity;

    {
        hbm$syncSet$RepulsionFieldBlockEntity$shieldCapacity(BASE_HEALTH);
    }

    public long storedEnergy;
    public boolean requestedOn;
    public int fieldColor;

    {
        hbm$syncSet$RepulsionFieldBlockEntity$fieldColor(0x0000FF);
    }

    public int recoveryTicks;
    public int energyPerTick;

    {
        hbm$syncSet$RepulsionFieldBlockEntity$energyPerTick(
                MachineData.FORCE_FIELD_BASE_CONSUMPTION.get());
    }

    public int hitFlashTicks;

    private final Vec3 fieldCenter;
    private IntOpenHashSet previousInside = new IntOpenHashSet();
    private IntOpenHashSet previousOutside = new IntOpenHashSet();
    private IntOpenHashSet currentInside = new IntOpenHashSet();
    private IntOpenHashSet currentOutside = new IntOpenHashSet();

    public RepulsionFieldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORCEFIELD.get(), pos, state, SLOT_COUNT);
        fieldCenter = Vec3.atCenterOf(pos);
    }

    @Override
    public void tickServer() {
        long oldEnergy = storedEnergy;
        int oldHealth = shieldHealth;
        int oldRecovery = recoveryTicks;
        int oldFlash = hitFlashTicks;
        updateUpgrades();
        hbm$syncSet$RepulsionFieldBlockEntity$storedEnergy(
                (long)
                        (storedEnergy
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - storedEnergy, false))));
        if (hitFlashTicks > 0) {
            hitFlashTicks--;
            hbm$syncSet$RepulsionFieldBlockEntity$fieldColor(COLOR_HIT);
        } else {
            hbm$syncSet$RepulsionFieldBlockEntity$fieldColor(0x00FF00);
        }
        fieldPhase(true);
        if (storedEnergy < energyPerTick) hbm$syncSet$RepulsionFieldBlockEntity$storedEnergy(0L);
        if (oldEnergy != storedEnergy
                || oldHealth != shieldHealth
                || oldRecovery != recoveryTicks
                || oldFlash != hitFlashTicks) markChanged();
        networkPackNT(500);
    }

    @Override
    public void tickClient() {
        fieldPhase(false);
    }

    private void updateUpgrades() {
        int radius = MachineData.FORCE_FIELD_BASE_RADIUS.get();
        hbm$syncSet$RepulsionFieldBlockEntity$shieldCapacity(BASE_HEALTH);
        hbm$syncSet$RepulsionFieldBlockEntity$energyPerTick(
                MachineData.FORCE_FIELD_BASE_CONSUMPTION.get());
        var radiusStack = inventory.get(SLOT_RADIUS);
        if (radiusStack.is(ModItems.UPGRADE_RADIUS.get())) {
            int count = radiusStack.getCount();
            radius += count * MachineData.FORCE_FIELD_RADIUS_UPGRADE.get();
            hbm$syncSet$RepulsionFieldBlockEntity$energyPerTick(
                    (int)
                            (energyPerTick
                                    + (count * MachineData.FORCE_FIELD_RADIUS_CONSUMPTION.get())));
        }
        hbm$syncSet$RepulsionFieldBlockEntity$fieldRadius(radius);
        var healthStack = inventory.get(SLOT_HEALTH);
        if (healthStack.is(ModItems.UPGRADE_HEALTH.get())) {
            int count = healthStack.getCount();
            hbm$syncSet$RepulsionFieldBlockEntity$shieldCapacity(
                    (int)
                            (shieldCapacity
                                    + (count * MachineData.FORCE_FIELD_SHIELD_UPGRADE.get())));
            hbm$syncSet$RepulsionFieldBlockEntity$energyPerTick(
                    (int)
                            (energyPerTick
                                    + (count * MachineData.FORCE_FIELD_SHIELD_CONSUMPTION.get())));
        }
    }

    private void fieldPhase(boolean server) {
        if (recoveryTicks > 0) {
            hbm$syncSet$RepulsionFieldBlockEntity$recoveryTicks((int) (recoveryTicks - 1));
        } else {
            if (shieldHealth < shieldCapacity) {
                hbm$syncSet$RepulsionFieldBlockEntity$shieldHealth(
                        (int)
                                (shieldHealth
                                        + (shieldCapacity / 100)
                                                * MachineData.FORCE_FIELD_HEALTH_REGEN_MODIFIER
                                                        .get()));
            }
            if (shieldHealth > shieldCapacity)
                hbm$syncSet$RepulsionFieldBlockEntity$shieldHealth(shieldCapacity);
        }
        if (!isOperational()) {
            previousInside.clear();
            previousOutside.clear();
            currentInside.clear();
            currentOutside.clear();
            return;
        }

        currentInside.clear();
        currentOutside.clear();
        var candidates =
                level.getEntities(
                        (Entity) null,
                        new AABB(fieldCenter, fieldCenter).inflate(fieldRadius + 25),
                        AFFECTED);
        for (int index = 0; index < candidates.size(); index++) {
            var entity = candidates.get(index);
            int id = entity.getId();
            boolean outside = entity.position().distanceTo(fieldCenter) > fieldRadius;
            boolean crossing = outside ? previousInside.contains(id) : previousOutside.contains(id);
            if (crossing) {
                redirect(entity, !outside, server);
                outside = !outside;
            }
            (outside ? currentOutside : currentInside).add(id);
        }
        var spare = previousInside;
        previousInside = currentInside;
        currentInside = spare;
        spare = previousOutside;
        previousOutside = currentOutside;
        currentOutside = spare;
        if (server)
            hbm$syncSet$RepulsionFieldBlockEntity$storedEnergy(
                    (long) (storedEnergy - (energyPerTick)));
    }

    private void redirect(Entity entity, boolean entering, boolean server) {
        var radial = entity.position().subtract(fieldCenter).normalize();
        double magnitude = entity.getDeltaMovement().length();
        double placement = fieldRadius + (entering ? 1 : -1);
        entity.snapTo(
                fieldCenter.x + radial.x * placement,
                fieldCenter.y + radial.y * placement,
                fieldCenter.z + radial.z * placement,
                0F,
                0F);
        var velocity = radial.scale(entering ? magnitude : -magnitude);
        entity.setDeltaMovement(velocity);
        entity.setPos(
                entity.getX() - velocity.x, entity.getY() - velocity.y, entity.getZ() - velocity.z);
        if (!entering || !isMuffled()) {
            EntitySounds.play(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.GUN_SPARK_SHOOT.get(),
                    SoundSource.BLOCKS,
                    2.5F,
                    1F);
        }
        if (server) applyImpact(entity);
    }

    private void applyImpact(Entity entity) {
        double speed = entity.getDeltaMovement().length();
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        double moved = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (speed == 0D) speed = moved;
        else if (moved != 0D) speed = Math.min(speed, moved);
        float width = entity.getBbWidth();
        int payment = (int) (entity.getBbHeight() * width * width * speed * 50);
        hbm$syncSet$RepulsionFieldBlockEntity$shieldHealth((int) (shieldHealth - (payment)));
        if (payment >= shieldCapacity / 250) hitFlashTicks = 5;
        if (shieldHealth <= 0) {
            hbm$syncSet$RepulsionFieldBlockEntity$shieldHealth(0);
            hbm$syncSet$RepulsionFieldBlockEntity$recoveryTicks(
                    (int)
                            (100
                                    + fieldRadius
                                            * MachineData.FORCE_FIELD_COOLDOWN_MODIFIER
                                                    .get()
                                                    .floatValue()));
        }
    }

    public boolean isOperational() {
        return requestedOn
                && recoveryTicks == 0
                && shieldHealth > 0
                && storedEnergy >= energyPerTick;
    }

    public void setRequestedOn(boolean on) {
        if (requestedOn == on) return;
        hbm$syncSet$RepulsionFieldBlockEntity$requestedOn(on);
        markChanged();
    }

    public void toggleFromControl() {
        setRequestedOn(!requestedOn);
    }

    public int healthBarPixels(int height) {
        return shieldHealth * height / shieldCapacity;
    }

    @Override
    public long getPower() {
        return storedEnergy;
    }

    @Override
    public long getMaxPower() {
        return MachineData.FORCE_FIELD_MAX_POWER.get();
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$RepulsionFieldBlockEntity$storedEnergy(Math.clamp(stored, 0L, getMaxPower()));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return BATTERY_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY ? EnergyItem.matches(stack) : slot == SLOT_RADIUS;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.forceField");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ForceFieldMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RepulsionFieldBlockEntity$storedEnergy(input.getLongOr("power", 0L));
        hbm$syncSet$RepulsionFieldBlockEntity$shieldHealth(input.getIntOr("health", BASE_HEALTH));
        hbm$syncSet$RepulsionFieldBlockEntity$recoveryTicks(input.getIntOr("cooldown", 0));
        hitFlashTicks = input.getIntOr("blink", 0);
        hbm$syncSet$RepulsionFieldBlockEntity$requestedOn(input.getBooleanOr("isOn", false));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", storedEnergy);
        output.putInt("health", shieldHealth);
        output.putInt("cooldown", recoveryTicks);
        output.putInt("blink", hitFlashTicks);
        output.putBoolean("isOn", requestedOn);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xFFL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeFloat(fieldRadius);
            case 1 -> output.writeInt(shieldHealth);
            case 2 -> output.writeInt(shieldCapacity);
            case 3 -> output.writeLong(storedEnergy);
            case 4 -> output.writeBoolean(requestedOn);
            case 5 -> output.writeInt(fieldColor);
            case 6 -> output.writeInt(recoveryTicks);
            case 7 -> output.writeInt(energyPerTick);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$RepulsionFieldBlockEntity$fieldRadius(input.readFloat());
            case 1 -> hbm$syncSet$RepulsionFieldBlockEntity$shieldHealth(input.readInt());
            case 2 -> hbm$syncSet$RepulsionFieldBlockEntity$shieldCapacity(input.readInt());
            case 3 -> hbm$syncSet$RepulsionFieldBlockEntity$storedEnergy(input.readLong());
            case 4 -> hbm$syncSet$RepulsionFieldBlockEntity$requestedOn(input.readBoolean());
            case 5 -> hbm$syncSet$RepulsionFieldBlockEntity$fieldColor(input.readInt());
            case 6 -> hbm$syncSet$RepulsionFieldBlockEntity$recoveryTicks(input.readInt());
            case 7 -> hbm$syncSet$RepulsionFieldBlockEntity$energyPerTick(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$RepulsionFieldBlockEntity$fieldRadius(float next) {
        if (Float.floatToRawIntBits(this.fieldRadius) != Float.floatToRawIntBits(next)) {
            this.fieldRadius = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RepulsionFieldBlockEntity$shieldHealth(int next) {
        if (this.shieldHealth != next) {
            this.shieldHealth = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$RepulsionFieldBlockEntity$shieldCapacity(int next) {
        if (this.shieldCapacity != next) {
            this.shieldCapacity = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$RepulsionFieldBlockEntity$storedEnergy(long next) {
        if (this.storedEnergy != next) {
            this.storedEnergy = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$RepulsionFieldBlockEntity$requestedOn(boolean next) {
        if (this.requestedOn != next) {
            this.requestedOn = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$RepulsionFieldBlockEntity$fieldColor(int next) {
        if (this.fieldColor != next) {
            this.fieldColor = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$RepulsionFieldBlockEntity$recoveryTicks(int next) {
        if (this.recoveryTicks != next) {
            this.recoveryTicks = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$RepulsionFieldBlockEntity$energyPerTick(int next) {
        if (this.energyPerTick != next) {
            this.energyPerTick = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    static {
        com.hbm.inventory.container.SyncedData.fields(
                RepulsionFieldBlockEntity.class, "storedEnergy");
    }
}
