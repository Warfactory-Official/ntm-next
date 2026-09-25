// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumCraneKey;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {0},
        units = 1L << 6,
        components = false)
public class BlockEntityCraneConsole extends BlockEntityMachineBase implements SyncUnitSchema {
    private static final double SPEED = 0.05D;

    @SyncField(units = 1L)
    public int centerX;

    @SyncField(units = 1L)
    public int centerY;

    @SyncField(units = 1L)
    public int centerZ;

    @SyncField(units = 1L)
    public int spanF;

    @SyncField(units = 1L)
    public int spanB;

    @SyncField(units = 1L)
    public int spanL;

    @SyncField(units = 1L)
    public int spanR;

    @SyncField(units = 1L)
    public int height;

    @SyncField(units = 0x7fL)
    public boolean setUpCrane = false;

    @SyncField(units = 1L)
    public int craneRotationOffset = 0;

    public double lastTiltFront, lastTiltLeft, tiltFront, tiltLeft;
    public double lastPosFront, lastPosLeft, syncFront, syncLeft;

    @SyncField(units = 1L << 1)
    public double posFront;

    @SyncField(units = 1L << 2)
    public double posLeft;

    public double lastProgress = 1D, syncProgress = 1D;

    @SyncField(units = 1L << 3)
    public double progress = 1D;

    @SyncField(units = 1L << 4)
    public double loadedHeat;

    @SyncField(units = 1L << 5)
    public double loadedEnrichment;

    private boolean goesDown = false;
    private int turnProgress;
    private boolean hasLoaded = false;

    public BlockEntityCraneConsole(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CRANE_CONSOLE.get(), pos, state, 1);
    }

    @Override
    public void tickClient() {
        lastTiltFront = tiltFront;
        lastTiltLeft = tiltLeft;
        lastPosFront = posFront;
        lastPosLeft = posLeft;
        lastProgress = progress;

        if (turnProgress > 0) {
            posFront += (syncFront - posFront) / turnProgress;
            posLeft += (syncLeft - posLeft) / turnProgress;
            progress += (syncProgress - progress) / turnProgress;
            turnProgress--;
        } else {
            posFront = syncFront;
            posLeft = syncLeft;
            progress = syncProgress;
        }

        readControls(true);
    }

    @Override
    public void tickServer() {
        BlockEntityRBMKBase aboveColumn = getColumnAtPos();
        if (aboveColumn != null) aboveColumn.craneIndicator = 10;

        if (goesDown) {
            if (progress > 0) {
                progress -= 0.04D;
            } else {
                progress = 0;
                goesDown = false;
                if (aboveColumn instanceof IRBMKLoadable column && canTargetInteract(column)) {
                    ItemStack carried = inventory.get(0);
                    if (!carried.isEmpty()) {
                        column.load(carried);
                        inventory.set(0, ItemStack.EMPTY);
                    } else {
                        inventory.set(0, column.provideNext().copy());
                        column.unload();
                    }
                    setChanged();
                }
            }
        } else if (progress != 1) {
            progress += 0.04D;
            if (progress > 1D) progress = 1D;
        }

        readControls(false);

        ItemStack carried = inventory.get(0);
        if (carried.getItem() instanceof ItemRBMKRod) {
            this.loadedHeat = ItemRBMKRod.getHullHeat(carried);
            this.loadedEnrichment = ItemRBMKRod.getEnrichment(carried);
        } else {
            this.loadedHeat = 0;
            this.loadedEnrichment = 0;
        }

        networkPackNT(250);
    }

    private void readControls(boolean client) {
        Direction dir = coreDir();
        Direction side = dir.getClockWise();
        double minX = worldPosition.getX() + 0.5 - side.getStepX() * 1.5;
        double maxX = worldPosition.getX() + 0.5 + side.getStepX() * 1.5 + dir.getStepX() * 2;
        double minZ = worldPosition.getZ() + 0.5 - side.getStepZ() * 1.5;
        double maxZ = worldPosition.getZ() + 0.5 + side.getStepZ() * 1.5 + dir.getStepZ() * 2;

        List<Player> players =
                level.getEntitiesOfClass(
                        Player.class,
                        new AABB(
                                Math.min(minX, maxX),
                                worldPosition.getY(),
                                Math.min(minZ, maxZ),
                                Math.max(minX, maxX),
                                worldPosition.getY() + 2,
                                Math.max(minZ, maxZ)));

        tiltFront = 0;
        tiltLeft = 0;

        if (!players.isEmpty() && !isCraneLoading()) {
            HbmPlayerProps props = HbmPlayerProps.getData(players.get(0));
            boolean up = props.isCraneKeyPressed(EnumCraneKey.UP);
            boolean down = props.isCraneKeyPressed(EnumCraneKey.DOWN);
            boolean left = props.isCraneKeyPressed(EnumCraneKey.LEFT);
            boolean right = props.isCraneKeyPressed(EnumCraneKey.RIGHT);

            if (up && !down) {
                tiltFront = 30;
                if (!client) posFront += SPEED;
            }
            if (!up && down) {
                tiltFront = -30;
                if (!client) posFront -= SPEED;
            }
            if (left && !right) {
                tiltLeft = 30;
                if (!client) posLeft += SPEED;
            }
            if (!left && right) {
                tiltLeft = -30;
                if (!client) posLeft -= SPEED;
            }

            if (props.isCraneKeyPressed(EnumCraneKey.LOAD)) goesDown = true;
        }

        posFront = Math.clamp(posFront, -spanB, spanF);
        posLeft = Math.clamp(posLeft, -spanR, spanL);
    }

    public void requestLoad() {
        goesDown = true;
    }

    public Direction coreDir() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    public boolean hasItemLoaded() {
        return level.isClientSide() ? hasLoaded : !inventory.get(0).isEmpty();
    }

    public boolean isCraneLoading() {
        return progress != 1D;
    }

    public boolean isAboveValidTarget() {
        return getLoadableAtPos() != null;
    }

    public boolean canTargetInteract(IRBMKLoadable column) {
        if (column == null) return false;
        return hasItemLoaded() ? column.canLoad(inventory.get(0)) : column.canUnload();
    }

    public @Nullable BlockEntityRBMKBase getColumnAtPos() {
        Direction dir = coreDir();
        Direction left = dir.getCounterClockWise();
        int x =
                (int)
                        Math.floor(
                                centerX
                                        - dir.getStepX() * posFront
                                        - left.getStepX() * posLeft
                                        + 0.5D);
        int y = centerY - 1;
        int z =
                (int)
                        Math.floor(
                                centerZ
                                        - dir.getStepZ() * posFront
                                        - left.getStepZ() * posLeft
                                        + 0.5D);

        BlockPos p = new BlockPos(x, y, z);
        Block b = level.getBlockState(p).getBlock();
        if (b instanceof RBMKBase) {
            BlockPos core = MultiblockSurface.coreOfAny(level, p);
            if (core != null && level.getBlockEntity(core) instanceof BlockEntityRBMKBase column)
                return column;
        }
        return null;
    }

    public @Nullable IRBMKLoadable getLoadableAtPos() {
        return getColumnAtPos() instanceof IRBMKLoadable loadable ? loadable : null;
    }

    public void setTarget(int x, int y, int z) {
        this.centerX = x;
        this.centerY = y + RBMKConfig.getColumnHeight(level) + 1;
        this.centerZ = z;

        int girderY = centerY + 6;
        Direction dir = coreDir().getOpposite();
        this.spanF = findRoomExtent(x, girderY, z, dir, 16);
        dir = dir.getClockWise();
        this.spanR = findRoomExtent(x, girderY, z, dir, 16);
        dir = dir.getClockWise();
        this.spanB = findRoomExtent(x, girderY, z, dir, 16);
        dir = dir.getClockWise();
        this.spanL = findRoomExtent(x, girderY, z, dir, 16);

        this.height = 7;
        this.setUpCrane = true;
        setChanged();
    }

    private int findRoomExtent(int x, int y, int z, Direction dir, int max) {
        for (int i = 1; i < max; i++) {
            if (!level.getBlockState(
                            new BlockPos(x + dir.getStepX() * i, y, z + dir.getStepZ() * i))
                    .isAir()) {
                return i - 1;
            }
        }
        return max;
    }

    public void cycleCraneRotation() {
        this.craneRotationOffset = (this.craneRotationOffset + 90) % 360;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        setUpCrane = input.getBooleanOr("crane", false);
        craneRotationOffset = input.getIntOr("craneRotationOffset", 0);
        centerX = input.getIntOr("centerX", 0);
        centerY = input.getIntOr("centerY", 0);
        centerZ = input.getIntOr("centerZ", 0);
        spanF = input.getIntOr("spanF", 0);
        spanB = input.getIntOr("spanB", 0);
        spanL = input.getIntOr("spanL", 0);
        spanR = input.getIntOr("spanR", 0);
        height = input.getIntOr("height", 0);
        posFront = input.getDoubleOr("posFront", 0);
        posLeft = input.getDoubleOr("posLeft", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("crane", setUpCrane);
        output.putInt("craneRotationOffset", craneRotationOffset);
        output.putInt("centerX", centerX);
        output.putInt("centerY", centerY);
        output.putInt("centerZ", centerZ);
        output.putInt("spanF", spanF);
        output.putInt("spanB", spanB);
        output.putInt("spanL", spanL);
        output.putInt("spanR", spanR);
        output.putInt("height", height);
        output.putDouble("posFront", posFront);
        output.putDouble("posLeft", posLeft);
    }

    private void writeGeometry(ByteBuf output) {
        output.writeBoolean(setUpCrane);
        if (!setUpCrane) return;
        output.writeInt(craneRotationOffset);
        output.writeInt(centerX);
        output.writeInt(centerY);
        output.writeInt(centerZ);
        output.writeInt(spanF);
        output.writeInt(spanB);
        output.writeInt(spanL);
        output.writeInt(spanR);
        output.writeInt(height);
    }

    private void readGeometry(ByteBuf input) {
        setUpCrane = input.readBoolean();
        if (!setUpCrane) return;
        craneRotationOffset = input.readInt();
        centerX = input.readInt();
        centerY = input.readInt();
        centerZ = input.readInt();
        spanF = input.readInt();
        spanB = input.readInt();
        spanL = input.readInt();
        spanR = input.readInt();
        height = input.readInt();
    }

    private void readFront(ByteBuf input) {
        if (input.readBoolean()) syncFront = input.readDouble();
    }

    private void readLeft(ByteBuf input) {
        if (input.readBoolean()) syncLeft = input.readDouble();
    }

    private void readProgress(ByteBuf input) {
        if (input.readBoolean()) syncProgress = input.readDouble();
    }

    private void writeCarriedRod(ByteBuf output) {
        output.writeBoolean(setUpCrane);
        if (setUpCrane) output.writeBoolean(!getItem(0).isEmpty());
    }

    private void readCarriedRod(ByteBuf input) {
        if (input.readBoolean()) hasLoaded = input.readBoolean();
    }

    private void writeFront(ByteBuf output) {
        output.writeBoolean(setUpCrane);
        if (setUpCrane) output.writeDouble(posFront);
    }

    private void writeLeft(ByteBuf output) {
        output.writeBoolean(setUpCrane);
        if (setUpCrane) output.writeDouble(posLeft);
    }

    private void writeProgress(ByteBuf output) {
        output.writeBoolean(setUpCrane);
        if (setUpCrane) output.writeDouble(progress);
    }

    private void writeHeat(ByteBuf output) {
        output.writeBoolean(setUpCrane);
        if (setUpCrane) output.writeDouble(loadedHeat);
    }

    private void readHeat(ByteBuf input) {
        if (input.readBoolean()) loadedHeat = input.readDouble();
    }

    private void writeEnrichment(ByteBuf output) {
        output.writeBoolean(setUpCrane);
        if (setUpCrane) output.writeDouble(loadedEnrichment);
    }

    private void readEnrichment(ByteBuf input) {
        if (input.readBoolean()) loadedEnrichment = input.readDouble();
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeGeometry(output);
            case 1 -> writeFront(output);
            case 2 -> writeLeft(output);
            case 3 -> writeProgress(output);
            case 4 -> writeHeat(output);
            case 5 -> writeEnrichment(output);
            case 6 -> writeCarriedRod(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readGeometry(input);
            case 1 -> readFront(input);
            case 2 -> readLeft(input);
            case 3 -> readProgress(input);
            case 4 -> readHeat(input);
            case 5 -> readEnrichment(input);
            case 6 -> readCarriedRod(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
