// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.container.MenuReactorControl;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityReactorControl extends BlockEntityMachineBase
        implements MenuProvider, IControlReceiver, SyncUnitSchema {

    public enum RodFunction {
        LINEAR,
        QUAD,
        LOG
    }

    public static final int SLOT_SENSOR = 0;
    private static final int[] NO_SLOTS = {};

    @SyncField(units = 1L << 0)
    public int heat;

    @SyncField(units = 1L << 1)
    public double rodLevel;

    @SyncField(units = 1L << 2)
    public int flux;

    @SyncField(units = 1L << 3)
    public boolean isLinked;

    @SyncField(units = 1L << 4)
    public double levelLower;

    @SyncField(units = 1L << 5)
    public double levelUpper;

    @SyncField(units = 1L << 6)
    public double heatLower;

    @SyncField(units = 1L << 7)
    public double heatUpper;

    @SyncField(units = 1L << 8)
    public RodFunction function = RodFunction.LINEAR;

    public BlockEntityReactorControl(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CONTROL.get(), pos, state, 1);
    }

    @Override
    public void tickServer() {
        BlockEntityReactorResearch reactor = establishLink();
        isLinked = reactor != null;

        if (reactor != null) {
            double lowerBound = Math.min(heatLower, heatUpper);
            double upperBound = Math.max(heatLower, heatUpper);
            double fauxLevel;

            if (heat < lowerBound) fauxLevel = levelLower;
            else if (heat > upperBound) fauxLevel = levelUpper;
            else fauxLevel = getTargetLevel(function, heat);

            double level = Mth.clamp(fauxLevel * 0.01D, 0D, 1D);
            if (level != rodLevel) reactor.targetLevel = level;
        }

        networkPackNT(150);
    }

    private @Nullable BlockEntityReactorResearch establishLink() {
        ItemStack sensor = getItem(SLOT_SENSOR);
        Long packed =
                sensor.is(ModItems.REACTOR_SENSOR.get())
                        ? sensor.get(ModDataComponents.REACTOR_POS.get())
                        : null;
        if (packed == null) return null;

        BlockPos pos = BlockPos.of(packed);
        if (!level.isLoaded(pos)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos, level.getBlockState(pos));
        if (core == null
                || !(level.getBlockEntity(core) instanceof BlockEntityReactorResearch reactor))
            return null;

        flux = reactor.totalFlux;
        rodLevel = reactor.controlLevel;
        heat = reactor.heat;
        return reactor;
    }

    public double getTargetLevel(RodFunction function, int heat) {
        return switch (function) {
            case LINEAR ->
                    (heat - heatLower) * ((levelUpper - levelLower) / (heatUpper - heatLower))
                            + levelLower;
            case LOG ->
                    Math.pow((heat - heatUpper) / (heatLower - heatUpper), 2)
                                    * (levelLower - levelUpper)
                            + levelUpper;
            case QUAD ->
                    Math.pow((heat - heatLower) / (heatUpper - heatLower), 2)
                                    * (levelUpper - levelLower)
                            + levelLower;
        };
    }

    public int[] getDisplayData() {
        if (!isLinked) return new int[] {0, 0, 0};
        return new int[] {
            (int) (rodLevel * 100), flux, (int) Math.round(heat * 0.00002 * 980 + 20)
        };
    }

    @Override
    public int getComparatorPower() {
        return (int) Math.ceil(heat * 15D / 50000D);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("function")) {
            int ordinal = data.getIntOr("function", 0);
            if (ordinal < 0 || ordinal >= RodFunction.values().length) return;
            function = RodFunction.values()[ordinal];
        } else {
            levelLower = data.getDoubleOr("levelLower", 0D);
            levelUpper = data.getDoubleOr("levelUpper", 0D);
            heatLower = data.getDoubleOr("heatLower", 0D);
            heatUpper = data.getDoubleOr("heatUpper", 0D);
        }
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.position()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 20D * 20D;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModItems.REACTOR_SENSOR.get());
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.reactorControl");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuReactorControl(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isLinked = input.getBooleanOr("isLinked", false);
        levelLower = input.getDoubleOr("levelLower", 0D);
        levelUpper = input.getDoubleOr("levelUpper", 0D);
        heatLower = input.getDoubleOr("heatLower", 0D);
        heatUpper = input.getDoubleOr("heatUpper", 0D);
        function =
                RodFunction.values()[
                        Mth.clamp(
                                input.getIntOr("function", 0), 0, RodFunction.values().length - 1)];
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isLinked", isLinked);
        output.putDouble("levelLower", levelLower);
        output.putDouble("levelUpper", levelUpper);
        output.putDouble("heatLower", heatLower);
        output.putDouble("heatUpper", heatUpper);
        output.putInt("function", function.ordinal());
    }

    @Override
    public long syncUnitMask() {
        return 0x1ffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(heat);
            case 1 -> output.writeDouble(rodLevel);
            case 2 -> output.writeInt(flux);
            case 3 -> output.writeBoolean(isLinked);
            case 4 -> output.writeDouble(levelLower);
            case 5 -> output.writeDouble(levelUpper);
            case 6 -> output.writeDouble(heatLower);
            case 7 -> output.writeDouble(heatUpper);
            case 8 -> output.writeByte(function.ordinal());
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> heat = input.readInt();
            case 1 -> rodLevel = input.readDouble();
            case 2 -> flux = input.readInt();
            case 3 -> isLinked = input.readBoolean();
            case 4 -> levelLower = input.readDouble();
            case 5 -> levelUpper = input.readDouble();
            case 6 -> heatLower = input.readDouble();
            case 7 -> heatUpper = input.readDouble();
            case 8 -> function = RodFunction.values()[input.readByte()];
            default -> throw new IllegalArgumentException();
        }
    }
}
