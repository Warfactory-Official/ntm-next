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

package com.hbm.blockentity.machine.storage;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.network.REDDBatteryBlock;
import com.hbm.inventory.container.BatteryREDDMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.uninos.graph.LevelNodeGraph;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ReddBlockEntity extends BatteryBlockEntity implements SoundEmitter, AudioLoop {
    public static final long SPEED = Long.MAX_VALUE / 100L;
    private static final long VIEW = SPEED / 2L;
    private static final int[] NO_SLOTS = {};
    private static final String[] NO_ROR = {};
    private static final long UNIT_EXACT_STOCK = 1L << 2;
    private static final long UNIT_EXACT_DELTA = 1L << 3;
    private static final String[] PERSISTENT_KEYS = {"bigPower", "muffled"};
    private static final double TWO_64 = 0x1p64;

    public static @Nullable Consumer<ReddBlockEntity> CLIENT_SOUND;

    public float rotation;
    public float prevRotation;
    private long stockHi;
    private long stockLo;
    private long deltaHi;
    private long deltaLo;
    private @Nullable BigInteger stockWide;
    private @Nullable BigInteger deltaWide;
    private final long[] meterHi = new long[METER_WINDOW];
    private final long[] meterLo = new long[METER_WINDOW];
    private final BigInteger[] meterWide = new BigInteger[METER_WINDOW];
    private int meterSlot;
    private @Nullable BigInteger stockView;
    private @Nullable BigInteger deltaView;

    public ReddBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_REDD.get(), pos, state, SPEED);
    }

    @Override
    public void tickServer() {
        long beganHi = stockHi;
        long beganLo = stockLo;
        BigInteger beganWide = stockWide;
        long charged = ItemEnergyTransfer.insert(this, SLOT_OUT, getPower(), false);
        if (charged > 0L) usePower(charged);
        long discharged = ItemEnergyTransfer.extract(this, SLOT_IN, SPEED, false);
        if (discharged > 0L) absorb(discharged);
        exactMeter(beganHi, beganLo, beganWide);
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        rotation += getSpeed();
        if (rotation >= 360F) {
            rotation -= 360F;
            prevRotation -= 360F;
        }
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    public float getSpeed() {
        return (float) Math.min(Math.pow(Math.log(0.05D * stockAsDouble() + 1D) * 0.05F, 5D), 15D);
    }

    public float audioPitch() {
        return 0.5F + 1.5F * getSpeed() / 15F;
    }

    @Override
    public boolean tickSound() {
        return getSpeed() > 0F;
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.FENSU_HUM_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 5.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(1.5F),
                25F,
                audioPitch());
    }

    private void exactMeter(long beganHi, long beganLo, @Nullable BigInteger beganWide) {
        if (stockWide != null || beganWide != null || meterWide[meterSlot] != null) {
            BigInteger beginning = beganWide == null ? toBig(beganHi, beganLo) : beganWide;
            BigInteger average = getExactStock().add(beginning).shiftRight(1);
            BigInteger previous = meterWide[meterSlot];
            if (previous == null) previous = toBig(meterHi[meterSlot], meterLo[meterSlot]);
            setExactDelta(average.subtract(previous));
            if (average.bitLength() > 127) {
                meterWide[meterSlot] = average;
            } else {
                meterWide[meterSlot] = null;
                meterHi[meterSlot] = average.shiftRight(64).longValue();
                meterLo[meterSlot] = average.longValue();
            }
            meterSlot = (meterSlot + 1) % METER_WINDOW;
            return;
        }
        long halfHi = (stockHi >> 1) + (beganHi >> 1);
        long sumLo = ((stockLo >>> 1) | (stockHi << 63)) + ((beganLo >>> 1) | (beganHi << 63));
        long lowCarry =
                Long.compareUnsigned(sumLo, (stockLo >>> 1) | (stockHi << 63)) < 0 ? 1L : 0L;
        long meanHi = halfHi + lowCarry;
        long meanLo = sumLo + (stockLo & beganLo & 1L);
        if (Long.compareUnsigned(meanLo, sumLo) < 0) meanHi++;
        long oldHi = meterHi[meterSlot];
        long oldLo = meterLo[meterSlot];
        long nextLo = meanLo - oldLo;
        long differenceHi = meanHi - oldHi;
        long borrow = Long.compareUnsigned(meanLo, oldLo) < 0 ? 1L : 0L;
        long nextHi = differenceHi - borrow;
        boolean wideDifference =
                ((meanHi ^ oldHi) & (meanHi ^ differenceHi)) < 0L
                        || borrow != 0L && differenceHi == Long.MIN_VALUE;
        meterHi[meterSlot] = meanHi;
        meterLo[meterSlot] = meanLo;
        meterSlot = (meterSlot + 1) % METER_WINDOW;
        if (wideDifference) {
            setExactDelta(toBig(meanHi, meanLo).subtract(toBig(oldHi, oldLo)));
            return;
        }
        if (deltaWide == null && nextHi == deltaHi && nextLo == deltaLo) return;
        hbm$syncSet$ReddBlockEntity$deltaHi(nextHi);
        hbm$syncSet$ReddBlockEntity$deltaLo(nextLo);
        hbm$syncSet$ReddBlockEntity$deltaWide(null);
        deltaView = null;
    }

    private void setExactDelta(BigInteger value) {
        if (value.bitLength() > 127) {
            hbm$syncSet$ReddBlockEntity$deltaWide(value);
            hbm$syncSet$ReddBlockEntity$deltaHi(0L);
            hbm$syncSet$ReddBlockEntity$deltaLo(0L);
        } else {
            hbm$syncSet$ReddBlockEntity$deltaWide(null);
            hbm$syncSet$ReddBlockEntity$deltaHi(value.shiftRight(64).longValue());
            hbm$syncSet$ReddBlockEntity$deltaLo(value.longValue());
        }
        deltaView = value;
    }

    private void addExact(long amount) {
        if (stockWide != null) {
            setExactStock(stockWide.add(BigInteger.valueOf(amount)));
            return;
        }
        long sum = stockLo + amount;
        long carry = Long.compareUnsigned(sum, stockLo) < 0 ? 1L : 0L;
        long step = (amount >> 63) + carry;
        long hi = stockHi + step;
        if (((stockHi ^ hi) & (step ^ hi)) < 0L) {
            setExactStock(toBig(stockHi, stockLo).add(BigInteger.valueOf(amount)));
            return;
        }
        hbm$syncSet$ReddBlockEntity$stockHi(hi);
        hbm$syncSet$ReddBlockEntity$stockLo(sum);
        stockChanged();
    }

    private void stockChanged() {
        stockView = null;
        onEnergyChanged();
    }

    private double stockAsDouble() {
        if (stockWide != null) return stockWide.doubleValue();
        return stockHi == stockLo >> 63
                ? (double) stockLo
                : stockHi * TWO_64 + (stockLo >>> 1) * 2D + (stockLo & 1L);
    }

    public BigInteger getExactStock() {
        if (stockWide != null) return stockWide;
        BigInteger view = stockView;
        if (view == null) stockView = view = toBig(stockHi, stockLo);
        return view;
    }

    public BigInteger getExactDelta() {
        if (deltaWide != null) return deltaWide;
        BigInteger view = deltaView;
        if (view == null) deltaView = view = toBig(deltaHi, deltaLo);
        return view;
    }

    public void setExactStock(BigInteger stock) {
        if (stock.bitLength() > 127) {
            hbm$syncSet$ReddBlockEntity$stockWide(stock);
            hbm$syncSet$ReddBlockEntity$stockHi(0L);
            hbm$syncSet$ReddBlockEntity$stockLo(0L);
        } else {
            hbm$syncSet$ReddBlockEntity$stockWide(null);
            hbm$syncSet$ReddBlockEntity$stockHi(stock.shiftRight(64).longValue());
            hbm$syncSet$ReddBlockEntity$stockLo(stock.longValue());
        }
        stockChanged();
    }

    private static BigInteger toBig(long hi, long lo) {
        if (hi == lo >> 63) return BigInteger.valueOf(lo);
        return BigInteger.valueOf(hi).shiftLeft(64).or(BigInteger.valueOf(lo).and(LOW_64));
    }

    private static final BigInteger LOW_64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    @Override
    public long getPower() {
        if (stockWide != null) return stockWide.signum() > 0 ? VIEW : 0L;
        if (stockHi != 0L) return stockHi > 0L ? VIEW : 0L;
        return stockLo < 0L || stockLo > VIEW ? VIEW : stockLo;
    }

    @Override
    public void setPower(long stored) {}

    @Override
    public void usePower(long amount) {
        if (amount == 0L) return;
        addExact(Math.negateExact(amount));
    }

    @Override
    public long transferPower(long amount) {
        if (amount != 0L) addExact(amount);
        return 0L;
    }

    @Override
    protected void absorb(long amount) {
        addExact(amount);
    }

    @Override
    public long getSendRate() {
        return getActiveMode().sends() ? SPEED : 0L;
    }

    @Override
    public long getReceiveRate() {
        return getActiveMode().receives() ? SPEED : 0L;
    }

    @Override
    public boolean canReceiveDirectly() {
        return false;
    }

    @Override
    protected boolean pushes(TransferMode mode) {
        return false;
    }

    @Override
    protected boolean conducts(TransferMode mode) {
        return false;
    }

    @Override
    protected void updateComparator() {}

    @Override
    public int getComparatorPower() {
        return 0;
    }

    @Override
    protected boolean signalled(ServerLevel level) {
        for (BlockPos port : REDDBatteryBlock.ports(worldPosition, facing())) {
            if (level.hasNeighborSignal(port)) return true;
        }
        return false;
    }

    private Direction facing() {
        return getBlockState().getValue(MultiblockCoreBlock.FACING);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshMode();
        LevelNodeGraph<CableData> graph = PowerGraph.get(server);
        if (!graph.containsCell(worldPosition.asLong()))
            REDDBatteryBlock.mintRing(graph, worldPosition, facing());
        graph.setSelfEndpoint(worldPosition.asLong(), true);
    }

    @Override
    public void onGraphDetach(ServerLevel server) {
        REDDBatteryBlock.removeRing(PowerGraph.get(server), worldPosition, facing());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public String[] getFunctionInfo() {
        return NO_ROR;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return null;
    }

    @Override
    public void runRadioFunction(String key, String[] params) {}

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.batteryREDD");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BatteryREDDMenu(containerId, inventory, this);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(ModDataComponents.REDD_CHARGE.get(), getExactStock());
        if (isMuffled()) components.set(ModDataComponents.MUFFLED.get(), true);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        BigInteger charge = components.get(ModDataComponents.REDD_CHARGE.get());
        if (charge != null) setExactStock(charge);
        if (Boolean.TRUE.equals(components.get(ModDataComponents.MUFFLED.get()))) setMuffled();
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        setExactStock(new BigInteger(input.getStringOr("bigPower", "0")));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("bigPower", getExactStock().toString());
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | UNIT_EXACT_STOCK | UNIT_EXACT_DELTA;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 2 -> writeExact(output, stockHi, stockLo, stockWide);
            case 3 -> writeExact(output, deltaHi, deltaLo, deltaWide);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 2 -> {
                readExact(input);
                hbm$syncSet$ReddBlockEntity$stockHi(readHi);
                hbm$syncSet$ReddBlockEntity$stockLo(readLo);
                hbm$syncSet$ReddBlockEntity$stockWide(readWide);
                stockView = null;
            }
            case 3 -> {
                readExact(input);
                hbm$syncSet$ReddBlockEntity$deltaHi(readHi);
                hbm$syncSet$ReddBlockEntity$deltaLo(readLo);
                hbm$syncSet$ReddBlockEntity$deltaWide(readWide);
                deltaView = null;
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    private static void writeExact(ByteBuf output, long hi, long lo, @Nullable BigInteger wide) {
        if (wide != null) {
            byte[] bytes = wide.toByteArray();
            output.writeInt(bytes.length);
            output.writeBytes(bytes);
            return;
        }
        int sign = hi < 0L ? 0xFF : 0;
        int length = 16;
        while (length > 1
                && byteAt(hi, lo, length - 1) == sign
                && (byteAt(hi, lo, length - 2) & 0x80) == (sign & 0x80)) length--;
        output.writeInt(length);
        for (int i = length - 1; i >= 0; i--) output.writeByte(byteAt(hi, lo, i));
    }

    private static int byteAt(long hi, long lo, int index) {
        return (int) ((index < 8 ? lo >>> (8 * index) : hi >>> (8 * (index - 8))) & 0xFFL);
    }

    private long readHi;
    private long readLo;
    private @Nullable BigInteger readWide;

    private void readExact(ByteBuf input) {
        ByteBuf bytes = input.readSlice(input.readInt());
        int length = bytes.readableBytes();
        if (length == 0) throw new NumberFormatException("Zero length BigInteger");
        if (length > 16) {
            byte[] value = new byte[length];
            bytes.readBytes(value);
            readWide = new BigInteger(value);
            readHi = 0L;
            readLo = 0L;
            return;
        }
        readWide = null;
        long hi = bytes.getByte(0) < 0 ? -1L : 0L;
        long lo = hi;
        for (int i = 0; i < length; i++) {
            hi = (hi << 8) | (lo >>> 56);
            lo = (lo << 8) | (bytes.readByte() & 0xFFL);
        }
        readHi = hi;
        readLo = lo;
    }

    public final void hbm$syncSet$ReddBlockEntity$stockHi(long next) {
        if (this.stockHi != next) {
            this.stockHi = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$ReddBlockEntity$stockLo(long next) {
        if (this.stockLo != next) {
            this.stockLo = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$ReddBlockEntity$deltaHi(long next) {
        if (this.deltaHi != next) {
            this.deltaHi = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$ReddBlockEntity$deltaLo(long next) {
        if (this.deltaLo != next) {
            this.deltaLo = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$ReddBlockEntity$stockWide(BigInteger next) {
        if (this.stockWide != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.stockWide);
            this.stockWide = next;
            if (bound) bindSyncValue(next, 3, 1L << 2);
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$ReddBlockEntity$deltaWide(BigInteger next) {
        if (this.deltaWide != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.deltaWide);
            this.deltaWide = next;
            if (bound) bindSyncValue(next, 3, 1L << 3);
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ReddBlockEntity.class, "stockWide", 3, 1L << 2);
        com.hbm.packet.SyncBindings.field(ReddBlockEntity.class, "deltaWide", 3, 1L << 3);
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }
}
