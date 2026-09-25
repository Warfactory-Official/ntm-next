// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachinePUREX;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.machine.ModuleMachineBase;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachinePUREX extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                IRORValueProvider,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "progress", PREFIX_VALUE + "recipe", PREFIX_VALUE + "active",
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_ITEM_IN_START = 4;
    public static final int SLOT_ITEM_OUT_START = 7;
    public static final int SLOT_COUNT = 13;

    public static final long MAX_POWER = 1_000_000L;
    public static final int INPUT_TANK_COUNT = 3;
    public static final int TANK_CAPACITY = 24_000;

    private static final int[] ITEM_IN_SLOTS = {
        SLOT_ITEM_IN_START, SLOT_ITEM_IN_START + 1, SLOT_ITEM_IN_START + 2
    };
    private static final int[] ITEM_OUT_SLOTS = {
        SLOT_ITEM_OUT_START,
        SLOT_ITEM_OUT_START + 1,
        SLOT_ITEM_OUT_START + 2,
        SLOT_ITEM_OUT_START + 3,
        SLOT_ITEM_OUT_START + 4,
        SLOT_ITEM_OUT_START + 5
    };
    private static final int[] ACCESSIBLE_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);
    public static Consumer<BlockEntityMachinePUREX> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 4)
    public final FluidTankNTM[] inputTanks = new FluidTankNTM[INPUT_TANK_COUNT];

    @SyncField(units = 1L << 5)
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[1];

    @SyncField(units = 1L << 3)
    public final ModuleMachineBase module;

    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    public long maxPower = MAX_POWER;

    @SyncField(units = 1L << 2)
    public boolean isProgressing;

    public boolean frame;
    public int anim;
    public int prevAnim;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachinePUREX(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUREX.get(), pos, state, SLOT_COUNT);
        for (int i = 0; i < INPUT_TANK_COUNT; i++) inputTanks[i] = new FluidTankNTM(TANK_CAPACITY);
        outputTanks[0] = new FluidTankNTM(TANK_CAPACITY);
        this.module =
                new ModuleMachineBase(
                        0,
                        this,
                        PUREXRecipes.INSTANCE,
                        this,
                        ITEM_IN_SLOTS,
                        ITEM_OUT_SLOTS,
                        inputTanks,
                        outputTanks);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machinePUREX");
    }

    @Override
    public void tickClient() {
        prevAnim = anim;
        if (isProgressing) anim++;
        CLIENT_SOUND.accept(this);

        if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            BlockPos upFive = getBlockPos().above(5);
            frame = !level.getBlockState(upFive).isAir();
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CHEMICAL_PLANT_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1F,
                15F,
                0.75F,
                15);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof ItemBlueprints;
        if (slot >= SLOT_ITEM_IN_START && slot < SLOT_ITEM_OUT_START) {
            return module.isItemValid(slot, stack);
        }
        return slot < SLOT_ITEM_OUT_START;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_ITEM_OUT_START || module.isSlotClogged(slot);
    }

    @Override
    public void tickServer() {
        long prevPower = power;

        GenericRecipe selected = module.getRecipe();
        if (selected != null) maxPower = selected.power * 100L;
        maxPower = Math.max(Math.max(power, maxPower), MAX_POWER);

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
        int overdriveLevel = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        double speed = 1D + speedLevel / 3D + overdriveLevel;
        double pow = 1D - powerLevel * 0.25D + speedLevel + overdriveLevel * 10D / 3D;

        module.update(speed, pow, true, getItem(SLOT_BLUEPRINT));
        isProgressing = module.didProcess;

        if (module.markDirty || power != prevPower) setChanged();
        flush.provide((ServerLevel) level, this);
        networkPackNT(100);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            if (data.getIntOr("index", 0) != 0) return;

            module.setRecipe(data.getStringOr("selection", ""));
            setChanged();
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "progress").equals(name))
            return "" + (int) Math.round(module.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return module.legacyRecipeName();
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (isProgressing ? 1 : 0);
        return null;
    }

    private void writeInputTanks(ByteBuf output) {
        for (FluidTankNTM tank : inputTanks) tank.packetSerialize(output);
    }

    private void readInputTanks(ByteBuf input) {
        for (FluidTankNTM tank : inputTanks) tank.packetDeserialize(input);
    }

    private void writeOutputTanks(ByteBuf output) {
        outputTanks[0].packetSerialize(output);
    }

    private void readOutputTanks(ByteBuf input) {
        outputTanks[0].packetDeserialize(input);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachinePUREX(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        module.load(input);
        for (int i = 0; i < INPUT_TANK_COUNT; i++) {
            int idx = i;
            input.child("i" + i).ifPresent(t -> inputTanks[idx].deserialize(t));
        }
        input.child("o0").ifPresent(outputTanks[0]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        module.save(output);
        for (int i = 0; i < INPUT_TANK_COUNT; i++) inputTanks[i].serialize(output.child("i" + i));
        outputTanks[0].serialize(output.child("o0"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeLong(this.maxPower);
            case 2 -> output.writeBoolean(this.isProgressing);
            case 3 -> this.module.serialize(output);
            case 4 -> writeInputTanks(output);
            case 5 -> writeOutputTanks(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.maxPower = input.readLong();
            case 2 -> this.isProgressing = input.readBoolean();
            case 3 -> this.module.deserialize(input);
            case 4 -> readInputTanks(input);
            case 5 -> readOutputTanks(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
