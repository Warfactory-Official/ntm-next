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

package com.hbm.blockentity.machine.pile;

import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.pile.PileBlock;
import com.hbm.items.machine.PileRodMK2Item;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.SyncInterpolation;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class PileLoaderBlockEntity extends PileDeviceBaseBlockEntity
        implements RadioValueProvider, SyncUnitSchema {
    private static final double STEP = 1D / 7D;
    private static final int[] STAGING = {0};
    private static final String[] READOUTS = {
        "VAL:meta", "VAL:depletion", "VAL:deppercent", "VAL:lifetime", "VAL:temp"
    };

    public double travel;
    public ItemStack observedRod;

    {
        hbm$syncSet$PileLoaderBlockEntity$observedRod(ItemStack.EMPTY);
    }

    public double observedDepletionPercent;
    public double observedHeat;
    public double displayedTravel;
    public double previousTravel;
    public boolean extending;
    private int restTicks;
    private boolean previousSignal;
    private final SyncInterpolation easing = new SyncInterpolation();
    private final BlockPos.MutableBlockPos signalContact = new BlockPos.MutableBlockPos();

    public PileLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_LOADER.get(), pos, state, 1);
    }

    public static boolean canLoadRod(ItemStack stack) {
        return stack.getItem() instanceof PileRodMK2Item;
    }

    public void startLoading() {
        if (restTicks <= 0 && travel <= 0D && !extending) {
            extending = true;
            setChanged();
        }
    }

    @Override
    public void tickServer() {
        var channel = attachedChannel(PileBlock.Role.FUEL_IN);
        hbm$syncSet$PileLoaderBlockEntity$observedRod(ItemStack.EMPTY);
        hbm$syncSet$PileLoaderBlockEntity$observedDepletionPercent(0D);
        hbm$syncSet$PileLoaderBlockEntity$observedHeat(0D);
        if (channel != null) {
            hbm$syncSet$PileLoaderBlockEntity$observedRod(channel.rods[channel.rods.length - 1]);
            hbm$syncSet$PileLoaderBlockEntity$observedDepletionPercent(
                    PileRodMK2Item.getDepletionPercent(observedRod));
            hbm$syncSet$PileLoaderBlockEntity$observedHeat(channel.heat);
        }

        var facing = orientation();
        boolean signal =
                level.getSignal(signalContact.set(worldPosition).move(facing), facing.getOpposite())
                        > 0;
        if (signal && !previousSignal) startLoading();
        if (signal != previousSignal) {
            previousSignal = signal;
            setChanged();
        }

        if (restTicks > 0) {
            restTicks--;
            setChanged();
        } else if (extending) {
            if (travel == 0D) endpointCue(1F);
            hbm$syncSet$PileLoaderBlockEntity$travel((double) (travel + (STEP)));
            if (travel >= 1D) {
                hbm$syncSet$PileLoaderBlockEntity$travel(1D);
                extending = false;
                restTicks = 5;
            }
            setChanged();
        } else if (travel > 0D) {
            if (travel == 1D) {
                endpointCue(0.75F);
                if (channel != null) {
                    channel.loadItem(getItem(0));
                    setItem(0, ItemStack.EMPTY);
                }
            }
            hbm$syncSet$PileLoaderBlockEntity$travel(Math.max(0D, travel - STEP));
            setChanged();
        }
        networkPackNT(35);
    }

    private void endpointCue(float pitch) {
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.GUN_BOLT_OPEN.get(),
                SoundSource.BLOCKS,
                1F,
                pitch);
    }

    @Override
    public void tickClient() {
        previousTravel = displayedTravel;
        displayedTravel = easing.step(displayedTravel, travel);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return STAGING;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && canLoadRod(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public String[] getFunctionInfo() {
        return READOUTS;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case "VAL:meta" ->
                    Integer.toString(
                            observedRod.getItem() instanceof PileRodMK2Item rod
                                    ? rod.type.ordinal()
                                    : -1);
            case "VAL:depletion" ->
                    Long.toString(Math.round(PileRodMK2Item.getDepletion(observedRod)));
            case "VAL:deppercent" -> Long.toString(Math.round(observedDepletionPercent));
            case "VAL:lifetime" ->
                    Long.toString(
                            Math.round(
                                    observedRod.getItem() instanceof PileRodMK2Item rod
                                            ? rod.type.life
                                            : 0D));
            case "VAL:temp" -> Long.toString(Math.round(observedHeat));
            default -> null;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        extending = input.getBooleanOr("loading", false);
        hbm$syncSet$PileLoaderBlockEntity$travel(input.getDoubleOr("level", 0D));
        restTicks = input.getIntOr("delay", 0);
        previousSignal = input.getBooleanOr("wasRedstone", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("loading", extending);
        output.putDouble("level", travel);
        output.putInt("delay", restTicks);
        output.putBoolean("wasRedstone", previousSignal);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 15L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(travel);
            case 1 -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(
                        new RegistryFriendlyByteBuf(output, level.registryAccess()), observedRod);
                output.writeDouble(observedDepletionPercent);
                output.writeDouble(observedHeat);
            }
            case 2 -> output.writeInt(channelNumber);
            case 3 ->
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(
                            new RegistryFriendlyByteBuf(output, level.registryAccess()),
                            getItem(0));
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                double sample = input.readDouble();
                if (sample != travel) {
                    hbm$syncSet$PileLoaderBlockEntity$travel(sample);
                    easing.restart(2);
                }
            }
            case 1 -> {
                hbm$syncSet$PileLoaderBlockEntity$observedRod(
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(
                                new RegistryFriendlyByteBuf(input, level.registryAccess())));
                hbm$syncSet$PileLoaderBlockEntity$observedDepletionPercent(input.readDouble());
                hbm$syncSet$PileLoaderBlockEntity$observedHeat(input.readDouble());
            }
            case 2 -> hbm$syncSet$PileDeviceBaseBlockEntity$channelNumber(input.readInt());
            case 3 ->
                    inventory.set(
                            0,
                            ItemStack.OPTIONAL_STREAM_CODEC.decode(
                                    new RegistryFriendlyByteBuf(input, level.registryAccess())));
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PileLoaderBlockEntity$travel(double next) {
        if (Double.doubleToRawLongBits(this.travel) != Double.doubleToRawLongBits(next)) {
            this.travel = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$PileLoaderBlockEntity$observedRod(ItemStack next) {
        if (this.observedRod != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.observedRod);
            this.observedRod = next;
            if (bound) bindSyncValue(next, 3, 1L << 1);
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$PileLoaderBlockEntity$observedDepletionPercent(double next) {
        if (Double.doubleToRawLongBits(this.observedDepletionPercent)
                != Double.doubleToRawLongBits(next)) {
            this.observedDepletionPercent = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$PileLoaderBlockEntity$observedHeat(double next) {
        if (Double.doubleToRawLongBits(this.observedHeat) != Double.doubleToRawLongBits(next)) {
            this.observedHeat = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(PileLoaderBlockEntity.class, "observedRod", 3, 1L << 1);
        com.hbm.packet.SyncInventory.slots(
                PileLoaderBlockEntity.class, new int[] {0}, 1L << 3, false, true);
    }
}
