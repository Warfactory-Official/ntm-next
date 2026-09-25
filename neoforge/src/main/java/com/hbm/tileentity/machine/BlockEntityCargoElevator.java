// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BlockCargoElevator.Part;
import com.hbm.blocks.machine.BlockCargoElevator;
import com.hbm.blocks.machine.CargoElevatorShape;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockEntityCargoElevator extends BlockEntity
        implements Synced, FoldedCoreResident, SyncUnitSchema, IRORInteractive {

    public static final double SPEED = 2D / 20D;

    @SyncField(units = 1L << 1)
    public int height;

    @SyncField(units = 1L << 2)
    public double extension;

    public double prevExtension;
    public double syncExtension;
    public int targetExtension;

    @SyncField(units = 1L << 0)
    public boolean renderPlatform;

    public boolean checkLower = true;
    private int sync;
    private int shapeHeight = -1;
    private double shapeExtension;
    private VoxelShape shape = Shapes.empty();
    private AABB[] boxes = new AABB[0];

    public BlockEntityCargoElevator(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARGO_ELEVATOR.get(), pos, state);
    }

    public void tickServer() {
        prevExtension = extension;
        if (checkLower) {
            checkLower = false;
            if (mergeIntoLower()) return;
        }
        if (extension < targetExtension)
            extension = Mth.clamp(extension + SPEED, 0, targetExtension);
        else if (extension > targetExtension)
            extension = Mth.clamp(extension - SPEED, targetExtension, height);
        extension = Mth.clamp(extension, 0, height);
        if (!renderPlatform) {
            renderPlatform = true;
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(BlockCargoElevator.PART, Part.CORE),
                    Block.UPDATE_CLIENTS);
        }
        if (prevExtension != extension) setChanged();
        networkPackNT(300);
        moveRiders();
    }

    public void tickClient() {
        prevExtension = extension;
        if (sync > 0) {
            extension += (syncExtension - extension) / sync;
            sync--;
        } else {
            extension = syncExtension;
        }
        moveRiders();
    }

    private void moveRiders() {
        if (extension == prevExtension) return;
        double lower = worldPosition.getY() + 1 + Math.min(extension, prevExtension);
        double upper = worldPosition.getY() + 1 + Math.max(extension, prevExtension);
        AABB swept =
                new AABB(
                        worldPosition.getX() - 0.99,
                        lower,
                        worldPosition.getZ() - 0.99,
                        worldPosition.getX() + 1.99,
                        upper,
                        worldPosition.getZ() + 1.99);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, swept)) {

            if (entity instanceof Player && !level.isClientSide()) continue;
            double feet = entity.getBoundingBox().minY;
            if (feet >= lower && feet <= upper) {
                entity.move(
                        MoverType.SELF,
                        new Vec3(0, worldPosition.getY() + 1 + extension - feet, 0));
                entity.setOnGround(true);
                entity.move(MoverType.SELF, new Vec3(0, -0.125, 0));
            }
        }
    }

    public boolean addSection() {
        int top = worldPosition.getY() + height + 1;
        if (level.isOutsideBuildHeight(top)) return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                pos.set(worldPosition.getX() + x, top, worldPosition.getZ() + z);
                if (!BlockMultiblockCore.canReadWithoutLoading(level, pos)
                        || !level.getBlockState(pos).canBeReplaced()) return false;
            }
        }
        height++;
        ((BlockCargoElevator) getBlockState().getBlock())
                .fillSpace(
                        level, worldPosition, getBlockState().getValue(BlockMultiblockCore.FACING));
        setChanged();
        return true;
    }

    private boolean mergeIntoLower() {
        BlockPos below = worldPosition.below();
        if (level.isOutsideBuildHeight(below)) return false;
        BlockCargoElevator block = (BlockCargoElevator) getBlockState().getBlock();
        BlockEntityCargoElevator lower = block.elevator(level, below);
        if (lower == null
                || lower == this
                || lower.getBlockPos().getX() != worldPosition.getX()
                || lower.getBlockPos().getZ() != worldPosition.getZ()) return false;
        lower.height += height + 1;
        Direction facing = lower.getBlockState().getValue(BlockMultiblockCore.FACING);
        BlockMultiblockCore.withoutTeardown(
                () -> {
                    block.fillSpace(level, lower.getBlockPos(), facing);

                    level.removeBlockEntity(worldPosition);
                });
        lower.setChanged();
        return true;
    }

    public void toggleElevator() {
        targetExtension = targetExtension == 0 ? height : 0;
        setChanged();
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setextension").equals(name) && params.length > 0) {
            targetExtension = IRORInteractive.parseInt(params[0], 0, height);
            setChanged();
        }
        return null;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {PREFIX_VALUE + "extension", PREFIX_FUNCTION + "setextension"};
    }

    public VoxelShape shape() {
        if (shapeHeight == height && shapeExtension == extension) return shape;
        shapeHeight = height;
        shapeExtension = extension;
        boxes =
                new AABB[] {
                    new AABB(-1, 0, -1, -0.75, height + 1, -0.75),
                    new AABB(-1, 0, 1.75, -0.75, height + 1, 2),
                    new AABB(1.75, 0, -1, 2, height + 1, -0.75),
                    new AABB(1.75, 0, 1.75, 2, height + 1, 2),
                    new AABB(-1, 0.75 + extension, -1, 2, 1 + extension, 2)
                };
        shape = Shapes.empty();
        for (AABB box : boxes) shape = Shapes.or(shape, Shapes.create(box));
        shape = new CargoElevatorShape(shape, boxes, height);
        return shape;
    }

    public AABB[] boxes() {
        shape();
        return boxes;
    }

    private void writeHeight(ByteBuf output) {
        output.writeShort(height);
    }

    private void readHeight(ByteBuf input) {
        height = input.readShort();
    }

    private void readExtension(ByteBuf input) {
        syncExtension = input.readDouble();
        if (syncExtension > 0 && syncExtension < height) sync = 3;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        extension = input.getDoubleOr("extension", 0);
        targetExtension = input.getIntOr("targetExtension", 0);
        height = input.getIntOr("height", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("extension", extension);
        output.putInt("targetExtension", targetExtension);
        output.putInt("height", height);
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.renderPlatform);
            case 1 -> writeHeight(output);
            case 2 -> output.writeDouble(this.extension);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.renderPlatform = input.readBoolean();
            case 1 -> readHeight(input);
            case 2 -> readExtension(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
