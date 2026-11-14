/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.security;

import com.hbm.NuclearTech;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.machine.LockStateData;
import com.hbm.items.ModItems;
import com.hbm.items.tool.KeyItem;
import com.hbm.items.tool.KeyPinItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class LockableBlockEntity extends MachineBlockEntity implements SyncUnitSchema {
    private int configuredPins;
    private boolean secureState;
    private double pickThreshold;

    {
        hbm$syncSet$LockableBlockEntity$pickThreshold(0.1D);
    }

    private boolean allowCounterfeit;

    {
        hbm$syncSet$LockableBlockEntity$allowCounterfeit(true);
    }

    protected LockableBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public int getPinCode() {
        return configuredPins;
    }

    public void setPinCode(int pins) {
        if (configuredPins == pins) return;
        hbm$syncSet$LockableBlockEntity$configuredPins(pins);
        setChanged();
    }

    public double getPickModifier() {
        return pickThreshold;
    }

    public void setPickModifier(double modifier) {
        if (Double.compare(pickThreshold, modifier) == 0) return;
        hbm$syncSet$LockableBlockEntity$pickThreshold(modifier);
        setChanged();
    }

    public boolean isLocked() {
        return secureState;
    }

    public boolean allowsCounterfeitKeys() {
        return allowCounterfeit;
    }

    public void setCounterfeitKeysAllowed(boolean allowed) {
        if (allowCounterfeit == allowed) return;
        hbm$syncSet$LockableBlockEntity$allowCounterfeit(allowed);
        setChanged();
    }

    public void lock() {
        if (configuredPins == 0) {
            NuclearTech.LOGGER.error(
                    "A block has been set to locked state before setting pins, this should not happen and may cause errors! "
                            + this);
        }
        if (secureState) return;
        hbm$syncSet$LockableBlockEntity$secureState(true);
        setChanged();
        onLockChanged();
    }

    public void unlock() {
        if (!secureState) return;
        hbm$syncSet$LockableBlockEntity$secureState(false);
        setChanged();
        onLockChanged();
    }

    protected void onLockChanged() {}

    public LockStateData getLockState() {
        return new LockStateData(configuredPins, secureState, pickThreshold, allowCounterfeit);
    }

    public void applyLockState(LockStateData snapshot) {
        var changed =
                configuredPins != snapshot.lock()
                        || secureState != snapshot.isLocked()
                        || Double.compare(pickThreshold, snapshot.lockMod()) != 0
                        || allowCounterfeit != snapshot.cheesable();
        var eligibilityChanged = secureState != snapshot.isLocked();
        hbm$syncSet$LockableBlockEntity$configuredPins(snapshot.lock());
        hbm$syncSet$LockableBlockEntity$secureState(snapshot.isLocked());
        hbm$syncSet$LockableBlockEntity$pickThreshold(snapshot.lockMod());
        hbm$syncSet$LockableBlockEntity$allowCounterfeit(snapshot.cheesable());
        if (changed) setChanged();
        if (eligibilityChanged) onLockChanged();
    }

    public boolean canAccess(@Nullable Player player) {
        if (player != null && player.isSpectator()) return false;
        if (!secureState) return true;
        if (player == null) return false;
        var held = player.getMainHandItem();
        if (held.getItem() instanceof KeyItem && KeyPinItem.getPins(held) == configuredPins
                || held.is(ModItems.KEY_RED.get())) {
            accessCue(player, ModSounds.LOCK_OPEN.get(), 1.0F);
            return true;
        }
        if (!spendPick(player, held)) return false;
        var jacket = player.getItemBySlot(EquipmentSlot.CHEST);
        var threshold = pickThreshold;
        if (jacket.is(ModItems.JACKT.get()) || jacket.is(ModItems.JACKT2.get()))
            threshold *= 100.0D;
        if (level.getRandom().nextDouble() < threshold) {
            accessCue(player, ModSounds.PIN_UNLOCK.get(), 1.0F);
            return true;
        }
        accessCue(player, ModSounds.PIN_BREAK.get(), 0.8F + level.getRandom().nextFloat() * 0.2F);
        return false;
    }

    private static boolean screwdriver(ItemStack stack) {
        return stack.is(ModItems.SCREWDRIVER.get()) || stack.is(ModItems.SCREWDRIVER_DESH.get());
    }

    private static boolean spendPick(Player player, ItemStack held) {
        var heldPin = held.is(ModItems.PIN.get());
        if (!heldPin && !screwdriver(held)) return false;
        var items = player.getInventory().getNonEquipmentItems();
        for (var slot = 0; slot < items.size(); slot++) {
            var ingredient = items.get(slot);
            if (heldPin ? screwdriver(ingredient) : ingredient.is(ModItems.PIN.get())) {
                if (heldPin) {
                    held.consume(1, player);
                } else {
                    ingredient.consume(1, player);
                    player.inventoryMenu.broadcastChanges();
                }
                return true;
            }
        }
        return false;
    }

    private static void accessCue(Player player, SoundEvent sound, float pitch) {
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.BLOCKS,
                1.0F,
                pitch);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 15L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(configuredPins);
            case 1 -> output.writeBoolean(secureState);
            case 2 -> output.writeDouble(pickThreshold);
            case 3 -> output.writeBoolean(allowCounterfeit);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$LockableBlockEntity$configuredPins(input.readInt());
            case 1 -> {
                var previous = secureState;
                hbm$syncSet$LockableBlockEntity$secureState(input.readBoolean());
                if (previous != secureState) onLockChanged();
            }
            case 2 -> hbm$syncSet$LockableBlockEntity$pickThreshold(input.readDouble());
            case 3 -> hbm$syncSet$LockableBlockEntity$allowCounterfeit(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var previous = secureState;
        hbm$syncSet$LockableBlockEntity$configuredPins(input.getIntOr("lock", 0));
        hbm$syncSet$LockableBlockEntity$secureState(input.getBooleanOr("isLocked", false));
        hbm$syncSet$LockableBlockEntity$pickThreshold(input.getDoubleOr("lockMod", 0.0D));
        hbm$syncSet$LockableBlockEntity$allowCounterfeit(input.getBooleanOr("cheesable", false));
        if (previous != secureState) onLockChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("lock", configuredPins);
        output.putBoolean("isLocked", secureState);
        output.putDouble("lockMod", pickThreshold);
        output.putBoolean("cheesable", allowCounterfeit);
    }

    public final void hbm$syncSet$LockableBlockEntity$configuredPins(int next) {
        if (this.configuredPins != next) {
            this.configuredPins = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$LockableBlockEntity$secureState(boolean next) {
        if (this.secureState != next) {
            this.secureState = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$LockableBlockEntity$pickThreshold(double next) {
        if (Double.doubleToRawLongBits(this.pickThreshold) != Double.doubleToRawLongBits(next)) {
            this.pickThreshold = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$LockableBlockEntity$allowCounterfeit(boolean next) {
        if (this.allowCounterfeit != next) {
            this.allowCounterfeit = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }
}
