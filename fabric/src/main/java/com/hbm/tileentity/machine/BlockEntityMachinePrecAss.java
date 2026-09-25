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
import com.hbm.inventory.container.MenuMachinePrecAss;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.machine.ModuleMachineAssembler;
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
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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

public class BlockEntityMachinePrecAss extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_INPUT_START = 4;
    public static final int SLOT_INPUT_END = 12;
    public static final int SLOT_OUTPUT_START = 13;
    public static final int SLOT_OUTPUT_END = 21;
    public static final int SLOT_COUNT = 22;

    public static final long MAX_POWER = 100_000L;
    public static final int FLUID_CAPACITY = 4_000;

    public static final double[] NULL_POSITION = {45, -30, 45};
    public static final double[] WORKING_POSITION = {45, -15, -5};

    private static final int[] INPUT_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final int[] OUTPUT_SLOTS = {13, 14, 15, 16, 17, 18, 19, 20, 21};
    private static final int[] ACCESSIBLE_SLOTS = {
        4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21
    };
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);

    public static Consumer<BlockEntityMachinePrecAss> CLIENT_SOUND = be -> {};

    public static Consumer<BlockEntityMachinePrecAss> CLIENT_SOUND_START = be -> {};

    public static Consumer<BlockEntityMachinePrecAss> CLIENT_SOUND_STRIKE = be -> {};

    public static Consumer<BlockEntityMachinePrecAss> CLIENT_SOUND_STOP = be -> {};

    @SyncField(units = 1L << 2)
    public final FluidTankNTM inputTank = new FluidTankNTM(FLUID_CAPACITY);

    @SyncField(units = 1L << 3)
    public final FluidTankNTM outputTank = new FluidTankNTM(FLUID_CAPACITY);

    @SyncField(units = 1L << 1)
    public final ModuleMachineAssembler recipeModule =
            new ModuleMachineAssembler(
                    0,
                    this,
                    PrecAssRecipes.INSTANCE,
                    this,
                    INPUT_SLOTS,
                    OUTPUT_SLOTS,
                    new FluidTankNTM[] {inputTank},
                    new FluidTankNTM[] {outputTank});

    public final double[] armAngles = {45, -15, -5};
    public final double[] prevArmAngles = {45, -15, -5};
    public final double[] strikers = new double[4];
    public final double[] prevStrikers = new double[4];
    private final boolean[] strikerDir = new boolean[4];
    private final UpgradeManager upgradeManager = new UpgradeManager(this);
    private final Random animRand = new Random();

    @ContainerSync public long power;
    @ContainerSync public long maxPower = MAX_POWER;

    @SyncField(units = 1L << 0)
    public boolean isProgressing;

    public boolean frame;
    public double ring;
    public double prevRing;

    private double ringTarget;
    private double ringSpeed;
    private int ringDelay;
    private int strikerIndex;
    private int strikerDelay;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] receiving = {inputTank};
    private final FluidTankNTM[] sending = {outputTank};

    public BlockEntityMachinePrecAss(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRECASS.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        long prevPower = power;

        GenericRecipe selected = recipeModule.getRecipe();
        if (selected != null) maxPower = selected.power * 100L;
        maxPower = Math.max(Math.max(power, maxPower), MAX_POWER);

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
        int overdriveLevel = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        double speed = 1D + speedLevel / 3D + overdriveLevel;
        double pow = 1D - powerLevel * 0.25D + speedLevel + overdriveLevel * 10D / 3D;

        recipeModule.update(speed, pow, true, inventory.get(SLOT_BLUEPRINT));
        isProgressing = recipeModule.didProcess;

        if (recipeModule.markDirty || power != prevPower) setChanged();
        flush.provide((ServerLevel) level, this);

        networkPackNT(100);
    }

    @Override
    public void tickClient() {

        if (TickPhase.every(this, 20)) frame = !level.getBlockState(worldPosition.above(3)).isAir();

        CLIENT_SOUND.accept(this);
        animateClient();

        if (isInWorkingPosition(prevArmAngles) && !isInWorkingPosition(armAngles))
            CLIENT_SOUND_STOP.accept(this);
    }

    private void animateClient() {
        System.arraycopy(armAngles, 0, prevArmAngles, 0, armAngles.length);
        System.arraycopy(strikers, 0, prevStrikers, 0, strikers.length);
        prevRing = ring;

        for (int i = 0; i < strikers.length; i++) {
            if (strikerDir[i]) {
                strikers[i] = -0.75D;
                strikerDir[i] = false;
                CLIENT_SOUND_STRIKE.accept(this);
            } else {
                strikers[i] = Mth.clamp(strikers[i] + 0.5D, -0.75D, 0D);
            }
        }

        if (ring != ringTarget) {
            double delta = Math.abs(ringTarget - ring);
            if (delta <= ringSpeed) ring = ringTarget;
            if (ringTarget > ring) ring += ringSpeed;
            if (ringTarget < ring) ring -= ringSpeed;
            if (ringTarget == ring) {

                double sub = ringTarget >= 360 ? -360D : 360D;
                ringTarget += sub;
                ring += sub;
                prevRing += sub;
                ringDelay = 100 + animRand.nextInt(21);
            }
        }

        if (isProgressing) {
            if (ring == ringTarget) {
                if (ringDelay > 0) ringDelay--;
                if (ringDelay <= 0) {
                    ringTarget += 45 * (animRand.nextBoolean() ? -1 : 1);
                    ringSpeed = 10D + animRand.nextDouble() * 5D;
                    CLIENT_SOUND_START.accept(this);
                }
            }

            if (!isInWorkingPosition(armAngles) && canArmsMove()) move(WORKING_POSITION);

            if (isInWorkingPosition(armAngles)) {
                strikerDelay--;
                if (strikerDelay <= 0) {
                    strikerDir[strikerIndex] = true;
                    strikerIndex = (strikerIndex + 1) % strikers.length;
                    strikerDelay = strikerIndex == 3 ? (10 + animRand.nextInt(3)) : 2;
                }
            }
        } else {
            for (int i = 0; i < strikerDir.length; i++) strikerDir[i] = false;
            if (canArmsMove()) move(NULL_POSITION);
        }
    }

    private boolean canArmsMove() {
        for (double striker : strikers) if (striker != 0) return false;
        return true;
    }

    private boolean isInWorkingPosition(double[] arms) {
        for (int i = 0; i < WORKING_POSITION.length; i++)
            if (arms[i] != WORKING_POSITION[i]) return false;
        return true;
    }

    private void move(double[] targetAngles) {
        for (int i = 0; i < armAngles.length; i++) {
            if (armAngles[i] == targetAngles[i]) continue;
            double angle = armAngles[i];
            double target = targetAngles[i];
            double turn = 15D;

            if (Math.abs(angle - target) <= turn) {
                armAngles[i] = target;
                continue;
            }
            armAngles[i] += angle < target ? turn : -turn;
        }
    }

    public void armPositions(int striker, float partialTicks, double[] out) {
        out[0] = Mth.lerp(partialTicks, prevArmAngles[0], armAngles[0]);
        out[1] = Mth.lerp(partialTicks, prevArmAngles[1], armAngles[1]);
        out[2] = Mth.lerp(partialTicks, prevArmAngles[2], armAngles[2]);
        out[3] = Mth.lerp(partialTicks, prevStrikers[striker], strikers[striker]);
    }

    @Override
    public AudioWrapper createAudioLoop() {

        return AudioSystem.getLoopedSound(
                ModSounds.ASSEMBLER_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.5F,
                15F,
                0.75F,
                20);
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
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof ItemBlueprints;
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END)
            return ItemMachineUpgrade.isUpgrade(stack);
        return recipeModule.isItemValid(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START || recipeModule.isSlotClogged(slot);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            if (data.getIntOr("index", 0) != 0) return;
            recipeModule.setRecipe(data.getStringOr("selection", ""));
            setChanged();
        }
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
        return new MenuMachinePrecAss(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getLong("power").ifPresent(v -> power = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        recipeModule.load(input);
        input.child("tankIn").ifPresent(inputTank::deserialize);
        input.child("tankOut").ifPresent(outputTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        recipeModule.save(output);
        inputTank.serialize(output.child("tankIn"));
        outputTank.serialize(output.child("tankOut"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machinePrecAss");
    }

    public @Nullable GenericRecipe selectedRecipe() {
        return recipeModule.getRecipe();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isProgressing);
            case 1 -> this.recipeModule.serialize(output);
            case 2 -> this.inputTank.packetSerialize(output);
            case 3 -> this.outputTank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.isProgressing = input.readBoolean();
            case 1 -> this.recipeModule.deserialize(input);
            case 2 -> this.inputTank.packetDeserialize(input);
            case 3 -> this.outputTank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
