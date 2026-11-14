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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.extprop.PlayerState;
import com.hbm.handler.CraneKey;
import com.hbm.items.machine.RBMKRodItem;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class RBMKCraneConsoleBlockEntity extends MachineBlockEntity implements SyncUnitSchema {

    private static final double SPEED = 0.05D;
    private static final double LIFT_STEP = 0.04D;
    private static final int TILT = 30;
    private static final int HIGHLIGHT = 10;
    private static final int GIRDER_RISE = 6;
    private static final int MAX_SPAN = 16;
    private static final int GIRDER_HEIGHT = 7;

    public boolean rigged;
    public int rotation;
    public int centerX;
    public int centerY;
    public int centerZ;
    public int spanFront;
    public int spanBack;
    public int spanLeft;
    public int spanRight;
    public int height;
    public double hookFront;
    public double hookLeft;
    public double lift;

    {
        hbm$syncSet$RBMKCraneConsoleBlockEntity$lift(1D);
    }

    public double carriedHeat;
    public double carriedEnrichment;
    public int tiltFront;
    public int tiltLeft;
    public int lastTiltFront;
    public int lastTiltLeft;
    public double lastHookFront;
    public double lastHookLeft;
    public double lastLift = 1D;
    public double receivedFront;
    public double receivedLeft;
    public double receivedLift = 1D;
    private boolean carrying;
    private boolean liftRequested;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
    private @Nullable RBMKBaseBlockEntity hooked;
    private int hookedX;
    private int hookedY;
    private int hookedZ;

    public RBMKCraneConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CRANE_CONSOLE.get(), pos, state, 1);
    }

    public Direction facing() {
        return MultiblockCoreBlock.coreFacing(getBlockState());
    }

    public void link(int x, int y, int z) {
        hbm$syncSet$RBMKCraneConsoleBlockEntity$centerX(x);
        hbm$syncSet$RBMKCraneConsoleBlockEntity$centerY(y + RbmkParams.heightAboveCore() + 1);
        hbm$syncSet$RBMKCraneConsoleBlockEntity$centerZ(z);
        Direction front = facing().getOpposite();
        hbm$syncSet$RBMKCraneConsoleBlockEntity$spanFront(span(front));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$spanRight(span(front.getClockWise()));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$spanBack(span(front.getOpposite()));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$spanLeft(span(front.getCounterClockWise()));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$height(GIRDER_HEIGHT);
        hbm$syncSet$RBMKCraneConsoleBlockEntity$rigged(true);
        markChanged();
    }

    private int span(Direction side) {
        for (int i = 1; i < MAX_SPAN; i++) {
            cursor.set(
                    centerX + side.getStepX() * i,
                    centerY + GIRDER_RISE,
                    centerZ + side.getStepZ() * i);
            if (!level.getBlockState(cursor).isAir()) return i - 1;
        }
        return MAX_SPAN;
    }

    public void rotateCrane() {
        hbm$syncSet$RBMKCraneConsoleBlockEntity$rotation((rotation + 90) % 360);
        markChanged();
    }

    public void requestLift() {
        liftRequested = true;
    }

    public boolean isLifting() {
        return lift != 1D;
    }

    public boolean isCarrying() {
        return level.isClientSide() ? carrying : !getItem(0).isEmpty();
    }

    public int hookX() {
        Direction facing = facing();
        return (int)
                Math.floor(
                        centerX
                                - facing.getStepX() * hookFront
                                - facing.getCounterClockWise().getStepX() * hookLeft
                                + 0.5D);
    }

    public int hookZ() {
        Direction facing = facing();
        return (int)
                Math.floor(
                        centerZ
                                - facing.getStepZ() * hookFront
                                - facing.getCounterClockWise().getStepZ() * hookLeft
                                + 0.5D);
    }

    public @Nullable RBMKBaseBlockEntity hookColumn() {
        if (!rigged) return null;
        int x = hookX(), y = centerY - 1, z = hookZ();
        if (hooked != null && !hooked.isRemoved() && x == hookedX && y == hookedY && z == hookedZ)
            return hooked;
        hooked = null;
        var column = ColumnLookup.column(level, cursor.set(x, y, z), probe);
        if (column == null) return null;
        hooked = column;
        hookedX = x;
        hookedY = y;
        hookedZ = z;
        return column;
    }

    public boolean isAboveTarget() {
        return hookColumn() instanceof CraneLoadable;
    }

    @Override
    public void tickServer() {
        var column = hookColumn();
        if (column != null) column.hbm$syncSet$RBMKBaseBlockEntity$craneIndicator(HIGHLIGHT);
        if (!liftRequested) {
            hbm$syncSet$RBMKCraneConsoleBlockEntity$lift(Math.min(1D, lift + LIFT_STEP));
        } else if (lift > 0D) {
            hbm$syncSet$RBMKCraneConsoleBlockEntity$lift((double) (lift - (LIFT_STEP)));
        } else {
            hbm$syncSet$RBMKCraneConsoleBlockEntity$lift(0D);
            liftRequested = false;
            if (column instanceof CraneLoadable loadable) exchange(loadable);
            markChanged();
        }
        double front = hookFront, left = hookLeft;
        steer(false);
        if (hookFront != front || hookLeft != left) markChanged();
        ItemStack carried = getItem(0);
        boolean rod = carried.getItem() instanceof RBMKRodItem;
        hbm$syncSet$RBMKCraneConsoleBlockEntity$carriedHeat(
                rod ? RBMKRodItem.getHullHeat(carried) : 0D);
        hbm$syncSet$RBMKCraneConsoleBlockEntity$carriedEnrichment(
                rod ? RBMKRodItem.getEnrichment(carried) : 0D);
        networkPackNT(250);
    }

    @Override
    public void tickClient() {
        lastTiltFront = tiltFront;
        lastTiltLeft = tiltLeft;
        lastHookFront = hookFront;
        lastHookLeft = hookLeft;
        lastLift = lift;
        hbm$syncSet$RBMKCraneConsoleBlockEntity$hookFront(receivedFront);
        hbm$syncSet$RBMKCraneConsoleBlockEntity$hookLeft(receivedLeft);
        hbm$syncSet$RBMKCraneConsoleBlockEntity$lift(receivedLift);
        steer(true);
    }

    private void exchange(CraneLoadable column) {
        ItemStack carried = getItem(0);
        if (!carried.isEmpty()) {
            if (!column.acceptsLoad(carried)) return;
            column.takeLoad(carried);
            setItem(0, ItemStack.EMPTY);
        } else if (column.hasUnload()) {
            setItem(0, column.peekUnload().copy());
            column.takeUnload();
        }
    }

    private void steer(boolean client) {
        tiltFront = 0;
        tiltLeft = 0;
        if (!isLifting()) {
            Player operator = operator();
            if (operator != null) {
                var keys = PlayerState.getOrCreate(operator);
                int front = axis(keys.held(CraneKey.UP), keys.held(CraneKey.DOWN));
                int side = axis(keys.held(CraneKey.LEFT), keys.held(CraneKey.RIGHT));
                tiltFront = TILT * front;
                tiltLeft = TILT * side;
                if (!client) {
                    hbm$syncSet$RBMKCraneConsoleBlockEntity$hookFront(
                            (double) (hookFront + (SPEED * front)));
                    hbm$syncSet$RBMKCraneConsoleBlockEntity$hookLeft(
                            (double) (hookLeft + (SPEED * side)));
                    if (keys.held(CraneKey.LOAD)) liftRequested = true;
                }
            }
        }
        hbm$syncSet$RBMKCraneConsoleBlockEntity$hookFront(
                Math.clamp(hookFront, -spanBack, spanFront));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$hookLeft(
                Math.clamp(hookLeft, -spanRight, spanLeft));
    }

    private static int axis(boolean positive, boolean negative) {
        return positive == negative ? 0 : positive ? 1 : -1;
    }

    private @Nullable Player operator() {
        Direction facing = facing();
        Direction right = facing.getClockWise();
        double x = worldPosition.getX() + 0.5D, z = worldPosition.getZ() + 0.5D;
        double x0 = x - right.getStepX() * 1.5D,
                x1 = x + right.getStepX() * 1.5D + facing.getStepX() * 2;
        double z0 = z - right.getStepZ() * 1.5D,
                z1 = z + right.getStepZ() * 1.5D + facing.getStepZ() * 2;
        double minX = Math.min(x0, x1),
                maxX = Math.max(x0, x1),
                minZ = Math.min(z0, z1),
                maxZ = Math.max(z0, z1);
        double minY = worldPosition.getY(), maxY = minY + 2;
        var players = level.players();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (!player.isSpectator()
                    && player.getBoundingBox().intersects(minX, minY, minZ, maxX, maxY, maxZ)) {
                return player;
            }
        }
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RBMKCraneConsoleBlockEntity$rigged(input.getBooleanOr("crane", false));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$rotation(input.getIntOr("craneRotationOffset", 0));
        if (rotation < 0 || rotation >= 360 || rotation % 90 != 0) {
            throw new IllegalArgumentException("Invalid crane rotation " + rotation);
        }
        hbm$syncSet$RBMKCraneConsoleBlockEntity$centerX(input.getIntOr("centerX", 0));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$centerY(input.getIntOr("centerY", 0));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$centerZ(input.getIntOr("centerZ", 0));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$spanFront(input.getIntOr("spanF", 0));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$spanBack(input.getIntOr("spanB", 0));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$spanLeft(input.getIntOr("spanL", 0));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$spanRight(input.getIntOr("spanR", 0));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$height(input.getIntOr("height", 0));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$hookFront(input.getDoubleOr("posFront", 0D));
        hbm$syncSet$RBMKCraneConsoleBlockEntity$hookLeft(input.getDoubleOr("posLeft", 0D));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("crane", rigged);
        output.putInt("craneRotationOffset", rotation);
        output.putInt("centerX", centerX);
        output.putInt("centerY", centerY);
        output.putInt("centerZ", centerZ);
        output.putInt("spanF", spanFront);
        output.putInt("spanB", spanBack);
        output.putInt("spanL", spanLeft);
        output.putInt("spanR", spanRight);
        output.putInt("height", height);
        output.putDouble("posFront", hookFront);
        output.putDouble("posLeft", hookLeft);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit > 6) {
            super.writeSyncUnit(unit, output);
            return;
        }
        output.writeBoolean(rigged);
        if (!rigged) return;
        switch (unit) {
            case 0 -> {
                output.writeInt(rotation);
                output.writeInt(centerX);
                output.writeInt(centerY);
                output.writeInt(centerZ);
                output.writeInt(spanFront);
                output.writeInt(spanBack);
                output.writeInt(spanLeft);
                output.writeInt(spanRight);
                output.writeInt(height);
            }
            case 1 -> output.writeDouble(hookFront);
            case 2 -> output.writeDouble(hookLeft);
            case 3 -> output.writeDouble(lift);
            case 4 -> output.writeDouble(carriedHeat);
            case 5 -> output.writeDouble(carriedEnrichment);
            case 6 -> output.writeBoolean(!getItem(0).isEmpty());
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit > 6) {
            super.readSyncUnit(unit, input);
            return;
        }
        boolean linked = input.readBoolean();
        if (unit == 0) hbm$syncSet$RBMKCraneConsoleBlockEntity$rigged(linked);
        if (!linked) return;
        switch (unit) {
            case 0 -> {
                hbm$syncSet$RBMKCraneConsoleBlockEntity$rotation(input.readInt());
                hbm$syncSet$RBMKCraneConsoleBlockEntity$centerX(input.readInt());
                hbm$syncSet$RBMKCraneConsoleBlockEntity$centerY(input.readInt());
                hbm$syncSet$RBMKCraneConsoleBlockEntity$centerZ(input.readInt());
                hbm$syncSet$RBMKCraneConsoleBlockEntity$spanFront(input.readInt());
                hbm$syncSet$RBMKCraneConsoleBlockEntity$spanBack(input.readInt());
                hbm$syncSet$RBMKCraneConsoleBlockEntity$spanLeft(input.readInt());
                hbm$syncSet$RBMKCraneConsoleBlockEntity$spanRight(input.readInt());
                hbm$syncSet$RBMKCraneConsoleBlockEntity$height(input.readInt());
            }
            case 1 -> receivedFront = input.readDouble();
            case 2 -> receivedLeft = input.readDouble();
            case 3 -> receivedLift = input.readDouble();
            case 4 -> hbm$syncSet$RBMKCraneConsoleBlockEntity$carriedHeat(input.readDouble());
            case 5 -> hbm$syncSet$RBMKCraneConsoleBlockEntity$carriedEnrichment(input.readDouble());
            case 6 -> carrying = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$rigged(boolean next) {
        if (this.rigged != next) {
            this.rigged = next;
            if ((127L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 127L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$rotation(int next) {
        if (this.rotation != next) {
            this.rotation = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$centerX(int next) {
        if (this.centerX != next) {
            this.centerX = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$centerY(int next) {
        if (this.centerY != next) {
            this.centerY = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$centerZ(int next) {
        if (this.centerZ != next) {
            this.centerZ = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$spanFront(int next) {
        if (this.spanFront != next) {
            this.spanFront = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$spanBack(int next) {
        if (this.spanBack != next) {
            this.spanBack = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$spanLeft(int next) {
        if (this.spanLeft != next) {
            this.spanLeft = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$spanRight(int next) {
        if (this.spanRight != next) {
            this.spanRight = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$height(int next) {
        if (this.height != next) {
            this.height = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$hookFront(double next) {
        if (Double.doubleToRawLongBits(this.hookFront) != Double.doubleToRawLongBits(next)) {
            this.hookFront = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$hookLeft(double next) {
        if (Double.doubleToRawLongBits(this.hookLeft) != Double.doubleToRawLongBits(next)) {
            this.hookLeft = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$lift(double next) {
        if (Double.doubleToRawLongBits(this.lift) != Double.doubleToRawLongBits(next)) {
            this.lift = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$carriedHeat(double next) {
        if (Double.doubleToRawLongBits(this.carriedHeat) != Double.doubleToRawLongBits(next)) {
            this.carriedHeat = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$RBMKCraneConsoleBlockEntity$carriedEnrichment(double next) {
        if (Double.doubleToRawLongBits(this.carriedEnrichment)
                != Double.doubleToRawLongBits(next)) {
            this.carriedEnrichment = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                RBMKCraneConsoleBlockEntity.class, new int[] {0}, 1L << 6, false, false);
    }
}
