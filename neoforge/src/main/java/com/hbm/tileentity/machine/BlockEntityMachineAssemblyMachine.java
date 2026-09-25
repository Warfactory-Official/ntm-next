// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineAssemblyMachine;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.tool.ItemSwordMeteorite;
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

public class BlockEntityMachineAssemblyMachine extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "progress",
                PREFIX_VALUE + "recipe",
                PREFIX_VALUE + "active",
                PREFIX_FUNCTION + "setrecipe" + NAME_SEPARATOR + "name",
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_INPUT_START = 4;
    public static final int SLOT_INPUT_END = 15;
    public static final int SLOT_OUTPUT = 16;
    public static final int SLOT_COUNT = 17;

    public static final long MAX_POWER = 100_000L;
    public static final int FLUID_CAPACITY = 4_000;

    private static final int[] INPUT_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT};
    private static final int[] ACCESSIBLE_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);

    public static Consumer<BlockEntityMachineAssemblyMachine> CLIENT_SOUND = be -> {};

    public static Consumer<BlockEntityMachineAssemblyMachine> CLIENT_SOUND_START = be -> {};

    public static Consumer<BlockEntityMachineAssemblyMachine> CLIENT_SOUND_STRIKE = be -> {};

    public static Consumer<BlockEntityMachineAssemblyMachine> CLIENT_SOUND_STOP = be -> {};
    public final AssemblerArm[] arms = {new AssemblerArm(), new AssemblerArm()};

    @SyncField(units = 1L << 2)
    public final FluidTankNTM inputTank = new FluidTankNTM(FLUID_CAPACITY);

    @SyncField(units = 1L << 3)
    public final FluidTankNTM outputTank = new FluidTankNTM(FLUID_CAPACITY);

    private final FluidTankNTM[] receiving = {inputTank};
    private final FluidTankNTM[] sending = {outputTank};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final Random animRand = new Random();

    @SyncField(units = 1L << 1)
    public final ModuleMachineAssembler recipeModule =
            new ModuleMachineAssembler(
                    0,
                    this,
                    AssemblyMachineRecipes.INSTANCE,
                    this,
                    INPUT_SLOTS,
                    OUTPUT_SLOTS,
                    new FluidTankNTM[] {inputTank},
                    new FluidTankNTM[] {outputTank});

    private final UpgradeManager upgradeManager = new UpgradeManager(this);
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
    private boolean lastProgressingClient;

    public BlockEntityMachineAssemblyMachine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLYMACHINE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickClient() {

        if (TickPhase.every(this, 20)) frame = !level.getBlockState(worldPosition.above(3)).isAir();

        boolean wasProgressing = lastProgressingClient;
        lastProgressingClient = isProgressing;
        if (wasProgressing && !isProgressing) CLIENT_SOUND_STOP.accept(this);

        CLIENT_SOUND.accept(this);
        animateClient();
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

    private void animateClient() {
        for (AssemblerArm arm : arms) {
            arm.updateInterp();
            if (isProgressing) {
                arm.updateArm();
                if (arm.prevAngles[3] != arm.angles[3] && arm.angles[3] == -0.75D) {
                    CLIENT_SOUND_STRIKE.accept(this);
                }
            } else {
                arm.returnToNullPos();
            }
        }

        prevRing = ring;
        if (!isProgressing) return;

        if (ring != ringTarget) {
            double delta = Math.abs(ringTarget - ring);
            if (delta <= ringSpeed) ring = ringTarget;
            if (ringTarget > ring) ring += ringSpeed;
            if (ringTarget < ring) ring -= ringSpeed;
            if (ringTarget == ring) {

                if (ringTarget >= 360) {
                    ringTarget -= 360;
                    ring -= 360;
                    prevRing -= 360;
                }
                if (ringTarget <= -360) {
                    ringTarget += 360;
                    ring += 360;
                    prevRing += 360;
                }
                ringDelay = 20 + animRand.nextInt(21);
            }
        } else {

            if (ringDelay > 0) ringDelay--;
            if (ringDelay <= 0) {
                ringTarget += (animRand.nextDouble() * 2 - 1) * 135;
                ringSpeed = 10D + animRand.nextDouble() * 5D;
                CLIENT_SOUND_START.accept(this);
            }
        }
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
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof ItemBlueprints;
        if (slot >= SLOT_INPUT_START && slot <= SLOT_INPUT_END) {
            return recipeModule.isItemValid(slot, stack);
        }
        return slot != SLOT_OUTPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT || recipeModule.isSlotClogged(slot);
    }

    @Override
    public void tickServer() {
        long prevPower = power;

        GenericRecipe selected = recipeModule.getRecipe();
        if (selected != null) maxPower = selected.power * 100L;
        maxPower = Math.max(Math.max(power, maxPower), MAX_POWER);

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);

        flush.provide((ServerLevel) level, this);

        int speedLevel = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
        int overdriveLevel = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        double speed = 1D + speedLevel / 3D + overdriveLevel;
        double pow = 1D - powerLevel * 0.25D + speedLevel + overdriveLevel * 10D / 3D;

        recipeModule.update(speed, pow, true, inventory.get(SLOT_BLUEPRINT));
        isProgressing = recipeModule.didProcess;

        if (recipeModule.didProcess) {
            ItemSwordMeteorite.upgrade(
                    inventory,
                    SLOT_BATTERY,
                    ModItems.METEORITE_SWORD_ALLOYED,
                    ModItems.METEORITE_SWORD_MACHINED);
        }

        if (recipeModule.markDirty || power != prevPower) setChanged();
        networkPackNT(100);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "progress").equals(name))
            return "" + (int) Math.round(recipeModule.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return recipeModule.legacyRecipeName();
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (recipeModule.didProcess ? 1 : 0);
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrecipe").equals(name) && params.length == 1) {
            recipeModule.setRecipe(params[0], true);
            setChanged();
        }
        return null;
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
    protected Component getDefaultName() {
        return Component.translatable("container.machineAssemblyMachine");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineAssemblyMachine(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getLong("power").ifPresent(v -> power = v);
        recipeModule.load(input);
        input.child("tankIn").ifPresent(inputTank::deserialize);
        input.child("tankOut").ifPresent(outputTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        recipeModule.save(output);
        inputTank.serialize(output.child("tankIn"));
        outputTank.serialize(output.child("tankOut"));
    }

    public static final class AssemblerArm {

        private static final double[][] POSITIONS = {
            {45, -15, -5}, {15, 15, -15}, {25, 10, -15}, {30, 0, -10}, {70, -10, -25},
        };
        public final double[] angles = new double[4];
        public final double[] prevAngles = new double[4];
        private final double[] targetAngles = new double[4];
        private final double[] speed = new double[4];
        private final Random rand = new Random();
        private ArmActionState state = ArmActionState.ASSUME_POSITION;
        private int actionDelay;

        private static double lerp(double prev, double now, float f) {
            return Mth.lerp(f, prev, now);
        }

        public void updateInterp() {
            System.arraycopy(angles, 0, prevAngles, 0, angles.length);
        }

        private void resetSpeed() {
            speed[0] = 15;
            speed[1] = 15;
            speed[2] = 15;
            speed[3] = 0.5;
        }

        private void returnToNullPos() {
            for (int i = 0; i < 4; i++) targetAngles[i] = 0;
            speed[0] = 3;
            speed[1] = 3;
            speed[2] = 3;
            speed[3] = 0.25;
            state = ArmActionState.RETRACT_STRIKER;
            move();
        }

        private void updateArm() {
            resetSpeed();
            if (actionDelay > 0) {
                actionDelay--;
                return;
            }

            switch (state) {
                case ASSUME_POSITION -> {
                    if (move()) {
                        actionDelay = 2;
                        state = ArmActionState.EXTEND_STRIKER;
                        targetAngles[3] = -0.75D;
                    }
                }
                case EXTEND_STRIKER -> {
                    if (move()) {
                        state = ArmActionState.RETRACT_STRIKER;
                        targetAngles[3] = 0D;
                    }
                }
                case RETRACT_STRIKER -> {
                    if (move()) {
                        actionDelay = 2 + rand.nextInt(5);
                        chooseNewArmPosition();
                        state = ArmActionState.ASSUME_POSITION;
                    }
                }
            }
        }

        private void chooseNewArmPosition() {
            double[] chosen = POSITIONS[rand.nextInt(POSITIONS.length)];
            targetAngles[0] = chosen[0];
            targetAngles[1] = chosen[1];
            targetAngles[2] = chosen[2];
        }

        private boolean move() {
            boolean didMove = false;
            for (int i = 0; i < angles.length; i++) {
                if (angles[i] == targetAngles[i]) continue;
                didMove = true;
                double angle = angles[i], target = targetAngles[i], turn = speed[i];
                if (Math.abs(angle - target) <= turn) {
                    angles[i] = target;
                    continue;
                }
                angles[i] += angle < target ? turn : -turn;
            }
            return !didMove;
        }

        public void getPositions(float interp, double[] out) {
            out[0] = lerp(prevAngles[0], angles[0], interp);
            out[1] = lerp(prevAngles[1], angles[1], interp);
            out[2] = lerp(prevAngles[2], angles[2], interp);
            out[3] = lerp(prevAngles[3], angles[3], interp);
        }

        private enum ArmActionState {
            ASSUME_POSITION,
            EXTEND_STRIKER,
            RETRACT_STRIKER
        }
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
