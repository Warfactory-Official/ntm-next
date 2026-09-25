// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.NuclearTech;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BlockEntityLockableBase extends BlockEntityMachineBase
        implements SyncUnitSchema {

    @SyncField(units = 1L << 3)
    public boolean cheesable = true;

    @SyncField(units = 1L)
    protected int lock;

    @SyncField(units = 1L << 2)
    protected double lockMod = 0.1D;

    @SyncField(units = 1L << 1)
    private boolean locked;

    protected BlockEntityLockableBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    private static boolean hasScrewdriver(Player player) {
        return player.getInventory().contains(BlockEntityLockableBase::isScrewdriver);
    }

    private static boolean isScrewdriver(ItemStack stack) {
        return stack.is(ModItems.SCREWDRIVER.get()) || stack.is(ModItems.SCREWDRIVER_DESH.get());
    }

    private static boolean consumePin(Player player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(ModItems.PIN.get())) {
                stack.shrink(1);
                player.inventoryMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    private static boolean isJacket(ItemStack stack) {
        return stack.is(ModItems.JACKT.get()) || stack.is(ModItems.JACKT2.get());
    }

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        if (lock == 0) {
            NuclearTech.LOGGER.error(
                    "A block has been set to locked state before setting pins: {}", this);
        }
        locked = true;
        setChanged();
    }

    public void unlock() {
        locked = false;
        setChanged();
    }

    public int getPins() {
        return lock;
    }

    public void setPins(int pins) {
        lock = pins;
        setChanged();
    }

    public double getMod() {
        return lockMod;
    }

    public void setMod(double mod) {
        lockMod = mod;
        setChanged();
    }

    public LockStateData getLockState() {
        return new LockStateData(lock, locked, lockMod, cheesable);
    }

    public void applyLockState(LockStateData state) {
        lock = state.lock();
        locked = state.isLocked();
        lockMod = state.lockMod();
        cheesable = state.cheesable();
    }

    public boolean canAccess(Player player) {
        if (!locked) return true;
        if (player == null) return false;

        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof ItemKey && ItemKeyPin.getPins(stack) == lock) {
            playLockOpen(player);
            return true;
        }
        if (stack.is(ModItems.KEY_RED.get())) {
            playLockOpen(player);
            return true;
        }
        return tryPick(player);
    }

    private void playLockOpen(Player player) {
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.LOCK_OPEN.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
    }

    private boolean tryPick(Player player) {
        ItemStack held = player.getMainHandItem();
        boolean canPick = false;
        double chanceOfSuccess = lockMod * 100;

        if (held.is(ModItems.PIN.get()) && hasScrewdriver(player)) {
            held.shrink(1);
            canPick = true;
        }
        if (isScrewdriver(held) && consumePin(player)) {
            canPick = true;
        }

        if (!canPick) return false;

        if (isJacket(player.getItemBySlot(EquipmentSlot.CHEST))) chanceOfSuccess *= 100D;
        if (level.getRandom().nextDouble() * 100 < chanceOfSuccess) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.PIN_UNLOCK.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
            return true;
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.PIN_BREAK.get(),
                SoundSource.BLOCKS,
                1.0F,
                0.8F + level.getRandom().nextFloat() * 0.2F);
        return false;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0b1111;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(lock);
            case 1 -> output.writeBoolean(locked);
            case 2 -> output.writeDouble(lockMod);
            case 3 -> output.writeBoolean(cheesable);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> lock = input.readInt();
            case 1 -> locked = input.readBoolean();
            case 2 -> lockMod = input.readDouble();
            case 3 -> cheesable = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lock = input.getIntOr("lock", 0);
        cheesable = input.getBooleanOr("cheesable", false);
        locked = input.getBooleanOr("isLocked", false);
        lockMod = input.getDoubleOr("lockMod", 0D);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("lock", lock);
        output.putBoolean("cheesable", cheesable);
        output.putBoolean("isLocked", locked);
        output.putDouble("lockMod", lockMod);
    }
}
