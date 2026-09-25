// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineRockMill;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.modules.machine.ModuleMachineRockMill;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineRockMill extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                IControlReceiver,
                MenuProvider,
                SyncUnitSchema,
                Audible {
    public static Consumer<BlockEntityMachineRockMill> CLIENT_DUST = be -> {};
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_SCHEMATIC = 1;
    public static final int SLOT_INPUT_START = 2;
    public static final int SLOT_OUTPUT_START = 5;
    public static final int SLOT_COUNT = 8;
    public static final long MIN_POWER = 2_500L;
    private static final int[] INPUT_SLOTS = {2, 3, 4};
    private static final int[] OUTPUT_SLOTS = {5, 6, 7};
    private static final int[] ACCESSIBLE_SLOTS = {2, 3, 4, 5, 6, 7};

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    public long maxPower = MIN_POWER;

    @SyncField(units = 1L << 2)
    public boolean didProcess;

    @SyncField(units = 1L << 3)
    public final ModuleMachineRockMill module;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM[] inputTanks = {new FluidTankNTM(4_000)};

    @SyncField(units = 1L << 5)
    public final FluidTankNTM[] outputTanks = {new FluidTankNTM(4_000)};

    public float rotation;
    public float prevRotation;
    public float rotationSpeed;
    public boolean frame;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineRockMill(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROCK_MILL.get(), pos, state, SLOT_COUNT);
        module =
                new ModuleMachineRockMill(
                        this, this, INPUT_SLOTS, OUTPUT_SLOTS, inputTanks, outputTanks);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRockMill");
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long value) {
        power = Math.max(0L, Math.min(value, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputTanks;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputTanks;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
        if (slot == SLOT_SCHEMATIC) return stack.getItem() instanceof ItemBlueprints;
        return module.isItemValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START || module.isSlotClogged(slot);
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        GenericRecipe selected = module.getRecipe();
        if (selected != null) maxPower = selected.power * 100L;
        maxPower = Math.max(Math.max(power, maxPower), MIN_POWER);
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);

        module.update(1D, 1D, true, inventory.get(SLOT_SCHEMATIC));
        didProcess = module.didProcess;
        if (module.markDirty || power != previousPower) setChanged();
        flush.provide((ServerLevel) level, this);
        if (didProcess && TickPhase.every(this, 3)) {
            SoundType type = SoundType.STONE;
            if (selected != null && selected.getIcon().getItem() instanceof BlockItem block) {
                type = block.getBlock().defaultBlockState().getSoundType();
            }
            level.playSound(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5,
                    type.getStepSound(),
                    SoundSource.BLOCKS,
                    getVolume(1F),
                    0.75F);
        }
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        rotationSpeed = Math.clamp(rotationSpeed + (didProcess ? 0.1F : -0.1F), 0F, 15F);
        rotation += rotationSpeed;
        if (rotation >= 360F) {
            prevRotation -= 360F;
            rotation -= 360F;
        }
        if (TickPhase.every(this, 20)) frame = !level.getBlockState(worldPosition.above(3)).isAir();
        if (didProcess) CLIENT_DUST.accept(this);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (!data.contains("index") || !data.contains("selection")) return;
        if (data.getIntOr("index", 0) != 0) return;
        module.setRecipe(data.getStringOr("selection", ""));
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineRockMill(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(value -> power = value);
        input.getLong("maxPower").ifPresent(value -> maxPower = value);
        input.child("i0").ifPresent(inputTanks[0]::deserialize);
        input.child("o0").ifPresent(outputTanks[0]::deserialize);
        module.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        inputTanks[0].serialize(output.child("i0"));
        outputTanks[0].serialize(output.child("o0"));
        module.save(output);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeLong(maxPower);
            case 2 -> output.writeBoolean(didProcess);
            case 3 -> module.serialize(output);
            case 4 -> inputTanks[0].packetSerialize(output);
            case 5 -> outputTanks[0].packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> power = input.readLong();
            case 1 -> maxPower = input.readLong();
            case 2 -> didProcess = input.readBoolean();
            case 3 -> module.deserialize(input);
            case 4 -> inputTanks[0].packetDeserialize(input);
            case 5 -> outputTanks[0].packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
